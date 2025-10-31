package com.crucialspace.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MemoryEntity::class, CollectionEntity::class, MemoryCollectionCrossRef::class],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}

private const val DB_NAME = "crucialspace.db"

@Volatile
private var INSTANCE: AppDatabase? = null

fun db(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(AppDatabase::class.java) {
        INSTANCE ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .enableMultiInstanceInvalidation()
            .addMigrations(MIGRATION_5_6)
            .build()
            .also { INSTANCE = it }
    }
}

val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS collections (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, description TEXT, colorHex TEXT, createdAt INTEGER NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS memory_collection_cross_ref (collectionId TEXT NOT NULL, memoryId TEXT NOT NULL, PRIMARY KEY(collectionId, memoryId))"
        )
        // Ensure index names match Room's expected default names
        database.execSQL("DROP INDEX IF EXISTS idx_mccr_collection")
        database.execSQL("DROP INDEX IF EXISTS idx_mccr_memory")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_collection_cross_ref_collectionId ON memory_collection_cross_ref(collectionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_collection_cross_ref_memoryId ON memory_collection_cross_ref(memoryId)")
    }
}
