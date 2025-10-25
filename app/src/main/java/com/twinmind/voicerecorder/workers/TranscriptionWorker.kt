package com.twinmind.voicerecorder.workers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TranscriptionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sid = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val path = inputData.getString(KEY_AUDIO_PATH) ?: return Result.failure()
        val idx = inputData.getInt(KEY_CHUNK_INDEX, 0)

        Log.d(TAG, "Running speech recognition for $path")

        val transcript = recognizeSpeech() ?: "Unable to transcribe audio."

        val intent = Intent("com.twinmind.voicerecorder.TRANSCRIPT_READY").apply {
            putExtra("sessionId", sid)
            putExtra("chunkIndex", idx)
            putExtra("text", transcript)
        }
        applicationContext.sendBroadcast(intent)

        return Result.success(
            Data.Builder()
                .putString(KEY_SESSION_ID, sid)
                .putString("transcript", transcript)
                .build()
        )
    }

    private suspend fun recognizeSpeech(): String? =
        suspendCancellableCoroutine { cont ->
            // Run on main thread
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                try {
                    if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
                        Log.e(TAG, "Speech recognition not available on this device")
                        cont.resume(null)
                        return@post
                    }

                    val recognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_CALLING_PACKAGE,
                            applicationContext.packageName
                        )
                    }

                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            Log.e(TAG, "SpeechRecognizer error code: $error")
                            cont.resume(null)
                            recognizer.destroy()
                        }

                        override fun onResults(results: Bundle?) {
                            val matches =
                                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.joinToString(" ") ?: ""
                            cont.resume(text)
                            recognizer.destroy()
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    recognizer.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Speech recognition failed", e)
                    cont.resume(null)
                }
            }
        }


    companion object {
        private const val TAG = "TranscriptionWorker"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_CHUNK_INDEX = "chunk_index"
        const val KEY_AUDIO_PATH = "audio_path"
    }
}
