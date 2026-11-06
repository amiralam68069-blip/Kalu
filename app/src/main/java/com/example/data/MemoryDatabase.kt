package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Routine", "Preference", "Note", "Fact"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    suspend fun getAllMemoriesList(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Int)

    @Query("DELETE FROM memories WHERE content LIKE '%' || :keyword || '%' OR category LIKE '%' || :keyword || '%'")
    suspend fun deleteMemoriesMatching(keyword: String): Int

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()
}

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getDatabase(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "amancipher_memory_core.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun getAllList(): List<MemoryEntity> = memoryDao.getAllMemoriesList()

    suspend fun insert(category: String, content: String) {
        memoryDao.insertMemory(
            MemoryEntity(
                category = category,
                content = content
            )
        )
    }

    suspend fun deleteById(id: Int) = memoryDao.deleteMemoryById(id)

    suspend fun deleteMatching(keyword: String): Int = memoryDao.deleteMemoriesMatching(keyword)

    suspend fun clearAll() = memoryDao.clearAllMemories()
}
