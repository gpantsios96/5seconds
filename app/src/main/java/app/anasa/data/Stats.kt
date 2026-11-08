package app.anasa.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure stats helpers, kept free of Android types so they are unit-testable. */
object Stats {

    data class Streak(val current: Int, val best: Int)

    /**
     * Avoided streak: consecutive days with at least one "Not now".
     * The current streak ends today, or yesterday if today has none yet
     * (so a streak isn't "broken" first thing in the morning).
     */
    fun streak(days: Set<LocalDate>, today: LocalDate): Streak {
        if (days.isEmpty()) return Streak(0, 0)

        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days.sorted()) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }

        var cursor = if (today in days) today else today.minusDays(1)
        var current = 0
        while (cursor in days) {
            current++
            cursor = cursor.minusDays(1)
        }
        return Streak(current, best)
    }

    fun toDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun startOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    data class DayCounts(val date: LocalDate, val attempts: Int, val opened: Int, val avoided: Int, val graceOpens: Int = 0)

    /** Buckets events into one [DayCounts] per day, for every day in [from]..[to] (inclusive). */
    fun perDay(events: List<Event>, from: LocalDate, to: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<DayCounts> {
        val grouped = events.groupBy { toDate(it.timestamp, zone) }
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { d ->
                val e = grouped[d].orEmpty()
                DayCounts(
                    date = d,
                    attempts = e.count { it.type == EventType.ATTEMPT },
                    opened = e.count { it.type == EventType.OPENED },
                    avoided = e.count { it.type == EventType.AVOIDED },
                    graceOpens = e.count { it.type == EventType.GRACE_OPEN },
                )
            }
            .toList()
    }
}
