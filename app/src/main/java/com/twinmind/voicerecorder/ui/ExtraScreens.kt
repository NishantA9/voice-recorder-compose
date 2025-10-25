package com.twinmind.voicerecorder.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.twinmind.voicerecorder.RecordingStatusHolder


@Composable
fun TranscriptScreen() {
    val transcript by RecordingStatusHolder.transcript
    Text(
        text = if (transcript.isEmpty()) "📝 Transcript will appear here once generated." else transcript,
        modifier = Modifier.padding(24.dp)
    )
}

@Composable
fun SummaryScreen() {
    val summary by RecordingStatusHolder.summary
    Text(
        text = if (summary.isEmpty()) "📊 Summary will appear here after processing." else summary,
        modifier = Modifier.padding(24.dp)
    )
}

