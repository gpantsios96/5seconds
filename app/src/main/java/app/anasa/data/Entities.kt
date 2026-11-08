package app.anasa.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** What happened during one interception. */
enum class EventType {
    /** The pause screen was shown for a guarded app. */
    ATTEMPT,
    /** The user chose to continue into the app. */
    OPENED,
    /** The user chose "Not now" (or left the pause screen without opening the app). */
    AVOIDED,
    /**
     * The app was brought to the front again during its grace period, so no pause was shown.
     * Every entry into a guarded app is recorded, whether it was paused or not.
     */
    GRACE_OPEN,
}

@Entity(tableName = "events", indices = [Index("timestamp"), Index("packageName")])
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val type: EventType,
    /** Optional free-text intention typed on the pause screen. */
    val intention: String? = null,
)

/**
 * A guarded app. A row existing means the app is guarded.
 * Null overrides mean "use the global default".
 */
@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val packageName: String,
    @ColumnInfo(name = "breathLevel") val breathLevel: BreathLevel? = null,
    @ColumnInfo(name = "graceSeconds") val graceSeconds: Int? = null,
)

/** Breath presets. Each breath is [BREATH_SECONDS] long: half inhale, half exhale. */
enum class BreathLevel(val breaths: Int, val label: String) {
    GENTLE(1, "Gentle"),
    DEEPER(2, "Deeper"),
    EXTREME(3, "Extreme");

    val totalSeconds: Int get() = breaths * BREATH_SECONDS

    companion object {
        const val BREATH_SECONDS = 5
    }
}

/**
 * Allowed grace-period values, in seconds. The shortest is deliberately 30 s: a grace window is
 * never just a few seconds long, and it always covers our own relaunch of the app after "Open".
 */
val GRACE_OPTIONS = listOf(30, 60, 180, 300, 600)
const val MIN_GRACE_SECONDS = 30
const val DEFAULT_GRACE_SECONDS = 180

fun graceLabel(seconds: Int): String = if (seconds < 60) "$seconds s" else "${seconds / 60} min"
