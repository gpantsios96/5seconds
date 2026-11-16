package app.techdox

import app.techdox.data.Stats
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

class ReflectionsTest {
    private val r = app.techdox.data.Reflections
    private val mine = listOf("Read more", "Sleep earlier")

    @Test fun builtInWhenNoReasons() {
        repeat(50) { assert(!r.choose(emptyList(), mixBuiltIn = true).own) }
    }

    @Test fun onlyOwnWhenMixIsOff() {
        repeat(200) {
            val c = r.choose(mine, mixBuiltIn = false)
            assert(c.own && c.text in mine)
        }
    }

    @Test fun mixShowsBoth() {
        val cards = List(300) { r.choose(mine, mixBuiltIn = true) }
        assert(cards.any { it.own } && cards.any { !it.own })
    }

    @Test fun neverRepeatsTwiceInARow() {
        var prev: String? = null
        repeat(500) {
            val c = r.choose(mine, mixBuiltIn = true)
            assert(c.text != prev)
            prev = c.text
        }
    }

    @Test fun ownWordsAreShownAsWritten() {
        val own = app.techdox.data.Reflections.Card("100%s mine", own = true)
        assertEquals("100%s mine", r.display(own, "Instagram"))
        r.all.forEach { assert("%s" !in r.display(app.techdox.data.Reflections.Card(it, false), "Instagram")) }
    }

    @Test fun sanitizeCleansInput() {
        assertEquals(listOf("a b", "c"), r.sanitize(listOf(" a\nb ", "", "c", "c")))
        assertEquals(r.MAX_REASONS, r.sanitize(List(9) { "r$it" }).size)
    }
}
