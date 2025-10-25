package com.twinmind.voicerecorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.twinmind.voicerecorder.service.RecordingService

object RecordingStatusHolder {
    val status = mutableStateOf("Ready to Record")
    val transcript = mutableStateOf("")
    val summary = mutableStateOf("")
}

class MainActivity : ComponentActivity() {

    private var statusReceiver: BroadcastReceiver? = null
    private var transcriptReceiver: BroadcastReceiver? = null
    private var summaryReceiver: BroadcastReceiver? = null

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) startRecordingService()
            else RecordingStatusHolder.status.value = "Permission Denied"
        }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceivers()

        setContent {
            var selectedTab by remember { mutableStateOf(0) }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Filled.Mic, contentDescription = "Record") },
                            label = { Text("Record") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Filled.TextSnippet, contentDescription = "Transcript") },
                            label = { Text("Transcript") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Filled.Summarize, contentDescription = "Summary") },
                            label = { Text("Summary") }
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (selectedTab) {
                        0 -> RecordingScreen(::checkAndStartRecording, ::stopRecordingService)
                        1 -> TranscriptScreen()
                        2 -> SummaryScreen()
                    }
                }
            }
        }
    }

    private fun registerReceivers() {
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val msg = intent?.getStringExtra("status") ?: return
                RecordingStatusHolder.status.value = msg
                Log.d("MainActivity", "Status update: $msg")
            }
        }

        transcriptReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val text = intent?.getStringExtra("text") ?: "Transcript ready ✓"
                RecordingStatusHolder.status.value = "Transcript ready ✓"
                RecordingStatusHolder.transcript.value = text
            }
        }

        summaryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val text = intent?.getStringExtra("summary") ?: "Summary ready ✓"
                RecordingStatusHolder.summary.value = text
            }
        }

        val statusFilter = IntentFilter("com.twinmind.voicerecorder.STATUS_UPDATE")
        val transcriptFilter = IntentFilter("com.twinmind.voicerecorder.TRANSCRIPT_READY")
        val summaryFilter = IntentFilter("com.twinmind.voicerecorder.SUMMARY_READY")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, statusFilter, Context.RECEIVER_EXPORTED)
            registerReceiver(transcriptReceiver, transcriptFilter, Context.RECEIVER_EXPORTED)
            registerReceiver(summaryReceiver, summaryFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(statusReceiver, statusFilter)
            registerReceiver(transcriptReceiver, transcriptFilter)
            registerReceiver(summaryReceiver, summaryFilter)
        }
    }

    private fun checkAndStartRecording() {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) startRecordingService() else launcher.launch(permissions)
    }

    private fun startRecordingService() {
        val intent = Intent(this, RecordingService::class.java).apply { action = "START" }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopRecordingService() {
        val intent = Intent(this, RecordingService::class.java).apply { action = "STOP" }
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(statusReceiver)
            unregisterReceiver(transcriptReceiver)
            unregisterReceiver(summaryReceiver)
        } catch (e: Exception) {
            Log.w("MainActivity", "Receiver already unregistered: ${e.message}")
        }
    }
}

@Composable
fun RecordingScreen(onStart: () -> Unit, onStop: () -> Unit) {
    var isRecording by remember { mutableStateOf(false) }
    val status by RecordingStatusHolder.status

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(text = status, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (isRecording) onStop() else onStart()
                isRecording = !isRecording
            },
            modifier = Modifier.width(180.dp)
        ) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}

@Composable fun TranscriptScreen() {
    val text by RecordingStatusHolder.transcript
    Text(
        text = if (text.isEmpty()) "📝 Transcript will appear here once generated." else text,
        modifier = Modifier.padding(24.dp)
    )
}

@Composable fun SummaryScreen() {
    val summary by RecordingStatusHolder.summary
    Text(
        text = if (summary.isEmpty()) "📊 Summary will appear here after processing." else summary,
        modifier = Modifier.padding(24.dp)
    )
}
