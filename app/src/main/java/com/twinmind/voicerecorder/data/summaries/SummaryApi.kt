package com.twinmind.voicerecorder.data.summaries

import kotlinx.coroutines.delay

interface SummaryApiLike {
    suspend fun summarize(fullTranscript: String): SummaryResponse
}

data class SummaryResponse(
    val title: String,
    val summary: String,
    val actionItems: String,
    val keyPoints: String
)

// Mock – replace with Gemini/OpenAI later
class MockSummaryApi: SummaryApiLike {
    override suspend fun summarize(fullTranscript: String): SummaryResponse {
        delay(800)
        val preview = fullTranscript.take(200).ifBlank { "No speech detected." }
        return SummaryResponse(
            title = "Meeting Summary",
            summary = preview,
            actionItems = "• Follow up with client\n• Send notes",
            keyPoints = "• Budget approved\n• Timeline agreed"
        )
    }
}