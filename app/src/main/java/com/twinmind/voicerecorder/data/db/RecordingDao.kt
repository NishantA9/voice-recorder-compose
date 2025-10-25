package com.twinmind.voicerecorder.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    // Session
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE sessions SET endedAt=:endedAt WHERE sessionId=:sid")
    suspend fun endSession(sid: String, endedAt: Long)

    // Chunks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChunk(chunk: ChunkEntity)

    @Query("SELECT * FROM chunks WHERE sessionId=:sid ORDER BY chunkIndex ASC")
    fun observeChunks(sid: String): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE sessionId=:sid ORDER BY chunkIndex ASC")
    suspend fun getChunksOnce(sid: String): List<ChunkEntity>

    @Query("UPDATE chunks SET status=:status WHERE sessionId=:sid AND chunkIndex=:idx")
    suspend fun updateChunkStatus(sid: String, idx: Int, status: ChunkStatus)

    @Query("UPDATE chunks SET transcript=:text, status=:status WHERE sessionId=:sid AND chunkIndex=:idx")
    suspend fun setChunkTranscript(sid: String, idx: Int, text: String, status: ChunkStatus)

    // Summary
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(s: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE sessionId=:sid")
    fun observeSummary(sid: String): Flow<SummaryEntity?>

    @Query("UPDATE summaries SET status=:status WHERE sessionId=:sid")
    suspend fun updateSummaryStatus(sid: String, status: SummaryStatus)
}
