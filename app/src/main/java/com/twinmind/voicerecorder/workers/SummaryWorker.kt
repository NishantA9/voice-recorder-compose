package com.twinmind.voicerecorder.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sid = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        Log.d(TAG, "Generating summary for session $sid")

        // Simple dummy summary generator for now
        val summaryText = "Summary: You spoke about your recording session. " +
                "Detailed transcript will appear in the Transcript tab."

        val intent = Intent("com.twinmind.voicerecorder.SUMMARY_READY").apply {
            putExtra("sessionId", sid)
            putExtra("summary", summaryText)
        }
        applicationContext.sendBroadcast(intent)

        return Result.success()
    }

    companion object {
        private const val TAG = "SummaryWorker"
        const val KEY_SESSION_ID = "session_id"
    }
}
