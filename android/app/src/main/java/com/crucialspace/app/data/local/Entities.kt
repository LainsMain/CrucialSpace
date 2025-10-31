package com.crucialspace.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "memories")
data class MemoryEntity(
	@PrimaryKey val id: String = UUID.randomUUID().toString(),
	val createdAt: Long = System.currentTimeMillis(),
	val imageUri: String?,
	val noteText: String?,
	val audioUri: String?,
    val aiTitle: String? = null,
	val aiSummary: String? = null,
	val aiTodosJson: String? = null,
    val aiTodosDoneJson: String? = null,
    val aiUrlsJson: String? = null,
	val aiRemindersJson: String? = null,
    val embeddingJson: String? = null,
	val status: String = "PENDING", // PENDING|SYNCED|ERROR
	val error: String? = null,
)
