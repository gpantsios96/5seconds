package app.anasa.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: Event): Long

    /** All events in [fromMillis, toMillis). */
    @Query("SELECT * FROM events WHERE timestamp >= :fromMillis AND timestamp < :toMillis ORDER BY timestamp")
    fun eventsBetween(fromMillis: Long, toMillis: Long): Flow<List<Event>>

    /** Every day that has at least one event of [type] — used for streaks. Small: one row per event. */
    @Query("SELECT timestamp FROM events WHERE type = :type AND (:pkg IS NULL OR packageName = :pkg)")
    fun timestampsOf(type: EventType, pkg: String?): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM events WHERE packageName = :pkg AND type = 'ATTEMPT' AND timestamp >= :fromMillis")
    fun attemptsSince(pkg: String, fromMillis: Long): Flow<Int>

    @Query("SELECT * FROM events WHERE packageName = :pkg AND intention IS NOT NULL AND intention != '' ORDER BY timestamp DESC LIMIT :limit")
    fun recentIntentions(pkg: String, limit: Int): Flow<List<Event>>

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config ORDER BY packageName")
    fun all(): Flow<List<AppConfig>>

    @Query("SELECT * FROM app_config WHERE packageName = :pkg")
    suspend fun get(pkg: String): AppConfig?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(config: AppConfig)

    @Upsert
    suspend fun upsert(config: AppConfig)

    @Query("DELETE FROM app_config WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

class Converters {
    @TypeConverter fun eventType(v: EventType): String = v.name
    @TypeConverter fun eventType(v: String): EventType = EventType.valueOf(v)
    @TypeConverter fun level(v: BreathLevel?): String? = v?.name
    @TypeConverter fun level(v: String?): BreathLevel? = v?.let { BreathLevel.valueOf(it) }
}

@Database(entities = [Event::class, AppConfig::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AnasaDatabase : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun appConfigs(): AppConfigDao

    companion object {
        fun create(context: Context): AnasaDatabase =
            Room.databaseBuilder(context, AnasaDatabase::class.java, "anasa.db").build()
    }
}
