package com.crucialspace.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    suspend fun listAll(): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun listAllFlow(): Flow<List<MemoryEntity>>

	@Query("SELECT * FROM memories WHERE status = :status ORDER BY createdAt ASC")
	suspend fun listByStatus(status: String): List<MemoryEntity>

	@Query("SELECT * FROM memories WHERE id = :id")
	suspend fun getById(id: String): MemoryEntity?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(item: MemoryEntity)

	@Update
	suspend fun update(item: MemoryEntity)

    @Query("UPDATE memories SET aiTitle = :title, aiSummary = :summary, aiTodosJson = :todos, aiUrlsJson = :urls, aiRemindersJson = :reminders, embeddingJson = :embedding, status = :status, error = NULL WHERE id = :id")
    suspend fun markSuccess(id: String, title: String?, summary: String?, todos: String?, urls: String?, reminders: String?, embedding: String?, status: String)

    @Query("UPDATE memories SET noteText = :text WHERE id = :id")
    suspend fun updateNoteText(id: String, text: String?)

    @Query("UPDATE memories SET aiSummary = :summary WHERE id = :id")
    suspend fun updateAiSummary(id: String, summary: String?)

    @Query("UPDATE memories SET aiTodosJson = :todos WHERE id = :id")
    suspend fun updateTodosJson(id: String, todos: String?)

	@Query("UPDATE memories SET status = 'ERROR', error = :error WHERE id = :id")
	suspend fun markError(id: String, error: String)

	@Query("UPDATE memories SET status = 'PENDING', error = NULL WHERE id = :id")
	suspend fun markPending(id: String)

	@Query("DELETE FROM memories WHERE id = :id")
	suspend fun deleteById(id: String)

    @Query("UPDATE memories SET aiRemindersJson = :reminders WHERE id = :id")
    suspend fun updateReminders(id: String, reminders: String?)

    @Query("UPDATE memories SET aiTodosDoneJson = :done WHERE id = :id")
    suspend fun updateTodosDone(id: String, done: String?)

    @Query("DELETE FROM memories WHERE id IN(:ids)")
    suspend fun deleteByIds(ids: List<String>)

    // Collections
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    suspend fun listCollections(): List<CollectionEntity>

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun observeCollections(): kotlinx.coroutines.flow.Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(entity: CollectionEntity)

    @Query("UPDATE collections SET name = :name, description = :description, colorHex = :color WHERE id = :id")
    suspend fun updateCollection(id: String, name: String, description: String?, color: String?)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query("SELECT * FROM collections WHERE name = :name LIMIT 1")
    suspend fun getCollectionByName(name: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCrossRef(ref: MemoryCollectionCrossRef)

    @Query("DELETE FROM memory_collection_cross_ref WHERE collectionId = :collectionId AND memoryId = :memoryId")
    suspend fun removeCrossRef(collectionId: String, memoryId: String)

    @Query("DELETE FROM memory_collection_cross_ref WHERE memoryId = :memoryId")
    suspend fun removeAllCrossRefsForMemory(memoryId: String)

    @Query("SELECT m.* FROM memories m INNER JOIN memory_collection_cross_ref x ON x.memoryId = m.id WHERE x.collectionId = :collectionId ORDER BY m.createdAt DESC")
    suspend fun listMemoriesForCollection(collectionId: String): List<MemoryEntity>

    @Query("SELECT c.* FROM collections c INNER JOIN memory_collection_cross_ref x ON x.collectionId = c.id WHERE x.memoryId = :memoryId ORDER BY c.createdAt DESC")
    suspend fun listCollectionsForMemory(memoryId: String): List<CollectionEntity>

    @Query("SELECT COUNT(*) FROM memory_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun countMemoriesInCollection(collectionId: String): Int

    @Query("DELETE FROM memory_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun removeAllCrossRefsForCollection(collectionId: String)
}
