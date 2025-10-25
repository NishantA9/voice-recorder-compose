package com.twinmind.voicerecorder.data.db

import androidx.room.*

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
)

@Entity(
    tableName = "chunks",
    primaryKeys = ["sessionId","chunkIndex"]
)
data class ChunkEntity(
    val sessionId: String,
    val chunkIndex: Int,
    val filePath: String,
    val startedAt: Long,
    val endedAt: Long,
    val status: ChunkStatus = ChunkStatus.PENDING,
    val transcript: String? = null
)

enum class ChunkStatus { PENDING, UPLOADING, DONE, FAILED, RETRYING }

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey val sessionId: String,
    val title: String? = null,
    val summary: String? = null,
    val actionItems: String? = null,
    val keyPoints: String? = null,
    val status: SummaryStatus = SummaryStatus.IDLE,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SummaryStatus { IDLE, GENERATING, DONE, ERROR }
