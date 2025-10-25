package com.twinmind.voicerecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.twinmind.voicerecorder.R
import com.twinmind.voicerecorder.repo.RecorderRepository
import com.twinmind.voicerecorder.workers.TranscriptionWorker
import com.twinmind.voicerecorder.workers.SummaryWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var repo: RecorderRepository
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var telephonyManager: TelephonyManager? = null
    private var isRecording = false

    private var sessionId = ""
    private var chunkIndex = 0
    private var rotateHandler: Handler? = null
    private var activeRecorder: MediaRecorder? = null
    private var nextRecorder: MediaRecorder? = null
    private var activeStartTs: Long = 0L

    companion object {
        const val CHANNEL_ID = "RecordingChannel"
        const val NOTIFICATION_ID = 101
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK,
                TelephonyManager.CALL_STATE_RINGING -> pauseRecording("Paused – Phone call")
                TelephonyManager.CALL_STATE_IDLE -> resumeRecording()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        createNotificationChannel()
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (_: SecurityException) {}
        Log.d("RecordingService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startRecording()
            "STOP" -> stopRecording()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording() {
        if (isRecording) return
        isRecording = true
        sessionId = "S" + System.currentTimeMillis()
        chunkIndex = 0
        rotateHandler = Handler(Looper.getMainLooper())
        ioScope.launch { repo.startSession(sessionId, System.currentTimeMillis()) }

        startNewActiveChunk()

        val notif = buildNotification("Recording...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else startForeground(NOTIFICATION_ID, notif)

        broadcastStatus("Recording...")
    }

    private fun startNewActiveChunk() {
        val filePath = "${externalCacheDir?.absolutePath}/${sessionId}_$chunkIndex.3gp"
        activeRecorder = newConfiguredRecorder(filePath).also { it.start() }
        activeStartTs = System.currentTimeMillis()

        ioScope.launch {
            repo.addPendingChunk(sessionId, chunkIndex, filePath, activeStartTs, activeStartTs + 30_000)
        }
        rotateHandler?.postDelayed({ armNextRecorder() }, 28_000)
        rotateHandler?.postDelayed({ swapRecorders() }, 30_000)
    }

    private fun armNextRecorder() {
        val nextPath = "${externalCacheDir?.absolutePath}/${sessionId}_${chunkIndex + 1}.3gp"
        nextRecorder = newConfiguredRecorder(nextPath).also { it.prepare() }
    }

    private fun swapRecorders() {
        try { activeRecorder?.stop() } catch (_: Exception) {}
        activeRecorder?.release()
        enqueueChunkTranscription(sessionId, chunkIndex)

        nextRecorder?.start()
        activeRecorder = nextRecorder
        nextRecorder = null
        chunkIndex++

        rotateHandler?.postDelayed({ armNextRecorder() }, 28_000)
        rotateHandler?.postDelayed({ swapRecorders() }, 30_000)
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        rotateHandler?.removeCallbacksAndMessages(null)
        try { activeRecorder?.stop() } catch (_: Exception) {}
        activeRecorder?.release()
        nextRecorder?.release()
        activeRecorder = null
        nextRecorder = null
        enqueueChunkTranscription(sessionId, chunkIndex)
        ioScope.launch { repo.endSession(sessionId, System.currentTimeMillis()) }
        broadcastStatus("Stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        enqueueSummary(sessionId)
    }

    private fun newConfiguredRecorder(path: String) = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        setOutputFile(path)
        prepare()
    }

    private fun pauseRecording(reason: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activeRecorder?.pause()
            broadcastStatus(reason)
        } catch (_: Exception) {}
    }

    private fun resumeRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activeRecorder?.resume()
            broadcastStatus("Recording...")
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        super.onDestroy()
    }

    // --- WorkManager integration ---
    private fun enqueueChunkTranscription(sid: String, idx: Int) {
        val audioPath = "${externalCacheDir?.absolutePath}/${sid}_$idx.3gp"
        val req = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf(
                TranscriptionWorker.KEY_SESSION_ID to sid,
                TranscriptionWorker.KEY_CHUNK_INDEX to idx,
                TranscriptionWorker.KEY_AUDIO_PATH to audioPath
            ))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(req)
    }

    private fun enqueueSummary(sid: String) {
        val req = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(workDataOf(SummaryWorker.KEY_SESSION_ID to sid))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(req)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Voice Recorder", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply { action = "STOP" }
        val pending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Recorder")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(android.R.drawable.ic_media_pause, "Stop", pending)
            .setOngoing(true)
            .build()
    }

    private fun broadcastStatus(status: String) {
        sendBroadcast(Intent("com.twinmind.voicerecorder.STATUS_UPDATE").apply {
            putExtra("status", status)
        })
    }
}
