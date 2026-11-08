package app.anasa

import app.anasa.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsTest {
    private val today = LocalDate.of(2026, 11, 8)
    private fun d(daysAgo: Long) = today.minusDays(daysAgo)

    @Test fun emptyHasNoStreak() = assertEquals(Stats.Streak(0, 0), Stats.streak(emptySet(), today))

    @Test fun streakIncludingToday() =
        assertEquals(Stats.Streak(3, 3), Stats.streak(setOf(d(0), d(1), d(2)), today))

    @Test fun streakStillAliveIfOnlyYesterday() =
        assertEquals(Stats.Streak(2, 2), Stats.streak(setOf(d(1), d(2)), today))

    @Test fun gapBreaksCurrentButKeepsBest() =
        assertEquals(Stats.Streak(1, 4), Stats.streak(setOf(d(0), d(3), d(4), d(5), d(6)), today))

    @Test fun twoDayGapMeansZeroCurrent() =
        assertEquals(Stats.Streak(0, 1), Stats.streak(setOf(d(2)), today))
}
