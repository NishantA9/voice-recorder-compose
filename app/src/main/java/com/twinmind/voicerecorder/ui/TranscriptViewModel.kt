package com.twinmind.voicerecorder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.voicerecorder.data.db.ChunkEntity
import com.twinmind.voicerecorder.repo.RecorderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val repo: RecorderRepository
) : ViewModel() {
    fun transcriptFlow(sessionId: String): Flow<List<ChunkEntity>> =
        repo.observeChunksOrdered(sessionId)
}
