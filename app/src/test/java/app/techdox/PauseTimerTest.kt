package app.techdox

import app.techdox.pause.PauseTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseTimerTest {
    private val start = 1_000_000L
    private val duration = 10_000L

    @Test fun progressFollowsElapsedTime() {
        assertEquals(0f, PauseTimer.progress(start, start, duration))
        assertEquals(0.5f, PauseTimer.progress(start, start + 5_000, duration))
        assertEquals(1f, PauseTimer.progress(start, start + 20_000, duration))
    }

    /** A recreated screen reads the same start time, so it resumes mid-way instead of restarting. */
    @Test fun recreationResumesWhereItWas() {
        val beforeRecreate = PauseTimer.progress(start, start + 4_000, duration)
        val afterRecreate = PauseTimer.progress(start, start + 4_050, duration)
        assertTrue(afterRecreate >= beforeRecreate)
        assertTrue(afterRecreate < 1f)
    }

    @Test fun notDoneBeforeTheFullDuration() {
        assertFalse(PauseTimer.isDone(start, start + duration - 1, duration))
        assertTrue(PauseTimer.isDone(start, start + duration, duration))
    }

    @Test fun clockGoingBackwardsNeverSkips() {
        assertEquals(0f, PauseTimer.progress(start, start - 5_000, duration))
    }
}
