package com.twinmind.voicerecorder.repo

import com.twinmind.voicerecorder.data.db.*
import com.twinmind.voicerecorder.data.summaries.SummaryApiLike
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecorderRepository @Inject constructor(
    private val dao: RecordingDao,
    private val summaryApi: SummaryApiLike
) {
    suspend fun startSession(sessionId: String, t0: Long) {
        dao.insertSession(SessionEntity(sessionId, t0))
        dao.upsertSummary(SummaryEntity(sessionId = sessionId))
    }

    suspend fun endSession(sessionId: String, t1: Long) =
        dao.endSession(sessionId, t1)

    suspend fun addPendingChunk(
        sessionId: String,
        idx: Int,
        path: String,
        start: Long,
        end: Long
    ) {
        dao.upsertChunk(
            ChunkEntity(sessionId, idx, path, start, end, status = ChunkStatus.PENDING)
        )
    }

    fun observeChunksOrdered(sessionId: String): Flow<List<ChunkEntity>> =
        dao.observeChunks(sessionId)

    suspend fun setChunkUploading(sid: String, idx: Int) =
        dao.updateChunkStatus(sid, idx, ChunkStatus.UPLOADING)

    suspend fun setChunkFailed(sid: String, idx: Int) =
        dao.updateChunkStatus(sid, idx, ChunkStatus.FAILED)

    suspend fun setChunkTranscript(sid: String, idx: Int, text: String) =
        dao.setChunkTranscript(sid, idx, text, ChunkStatus.DONE)

    fun observeSummary(sessionId: String) = dao.observeSummary(sessionId)

    suspend fun markSummaryGenerating(sid: String) =
        dao.updateSummaryStatus(sid, SummaryStatus.GENERATING)

    suspend fun writeSummary(
        sid: String,
        title: String, sum: String, actions: String, keys: String
    ) {
        dao.upsertSummary(
            SummaryEntity(
                sessionId = sid,
                title = title,
                summary = sum,
                actionItems = actions,
                keyPoints = keys,
                status = SummaryStatus.DONE
            )
        )
    }

    suspend fun runSummary(sid: String) {
        markSummaryGenerating(sid)
        val chunks = dao.getChunksOnce(sid).sortedBy { it.chunkIndex }
        val full = chunks.mapNotNull { it.transcript }.joinToString("\n")
        val res = summaryApi.summarize(full)
        writeSummary(sid, res.title, res.summary, res.actionItems, res.keyPoints)
    }
}
