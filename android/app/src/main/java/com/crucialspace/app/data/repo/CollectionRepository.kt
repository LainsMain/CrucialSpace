package com.crucialspace.app.data.repo

import com.crucialspace.app.data.local.AppDatabase
import com.crucialspace.app.data.local.CollectionEntity
import com.crucialspace.app.data.local.MemoryCollectionCrossRef
import java.util.UUID

class CollectionRepository(private val db: AppDatabase) {
    suspend fun listAll(): List<CollectionEntity> = db.memoryDao().listCollections()

    fun observeAll() = db.memoryDao().observeCollections()

    suspend fun create(name: String, description: String? = null, colorHex: String? = null): CollectionEntity {
        val existing = db.memoryDao().getCollectionByName(name)
        if (existing != null) return existing
        val entity = CollectionEntity(id = UUID.randomUUID().toString(), name = name.trim(), description = description, colorHex = colorHex)
        db.memoryDao().insertCollection(entity)
        return entity
    }

    suspend fun rename(id: String, name: String, description: String?, colorHex: String?) {
        db.memoryDao().updateCollection(id, name.trim(), description, colorHex)
    }

    suspend fun delete(id: String) {
        // Remove mappings first, then delete collection
        db.memoryDao().removeAllCrossRefsForCollection(id)
        db.memoryDao().deleteCollection(id)
    }

    suspend fun addMemoryToCollection(collectionId: String, memoryId: String) {
        db.memoryDao().addCrossRef(MemoryCollectionCrossRef(collectionId, memoryId))
    }

    suspend fun removeMemoryFromCollection(collectionId: String, memoryId: String) {
        db.memoryDao().removeCrossRef(collectionId, memoryId)
    }

    suspend fun listMemories(collectionId: String) = db.memoryDao().listMemoriesForCollection(collectionId)
}


