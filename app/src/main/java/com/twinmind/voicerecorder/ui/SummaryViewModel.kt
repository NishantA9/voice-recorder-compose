package com.twinmind.voicerecorder.ui

import androidx.lifecycle.ViewModel
import com.twinmind.voicerecorder.data.db.SummaryEntity
import com.twinmind.voicerecorder.repo.RecorderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repo: RecorderRepository
) : ViewModel() {
    fun summaryFlow(sessionId: String): Flow<SummaryEntity?> =
        repo.observeSummary(sessionId)
}
