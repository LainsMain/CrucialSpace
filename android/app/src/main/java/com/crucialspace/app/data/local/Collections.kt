package com.crucialspace.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "memory_collection_cross_ref",
    primaryKeys = ["collectionId", "memoryId"],
    indices = [Index("collectionId"), Index("memoryId")]
)
data class MemoryCollectionCrossRef(
    val collectionId: String,
    val memoryId: String,
)


