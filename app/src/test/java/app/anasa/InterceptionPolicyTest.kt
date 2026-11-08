package app.anasa

import app.anasa.service.InterceptionPolicy
import app.anasa.service.InterceptionPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class InterceptionPolicyTest {
    private val ig = "com.instagram.android"
    private val home = "launcher"
    private val self = "app.anasa"

    @Test fun guardedAppFromHomeStarts() {
        val p = InterceptionPolicy()
        p.onForeground(home, 0, false, null)
        assertEquals(Decision.START, p.onForeground(ig, 10_000, true, null))
    }

    @Test fun unguardedIgnored() {
        assertEquals(Decision.IGNORE, InterceptionPolicy().onForeground("x", 0, false, null))
    }

    @Test fun activeSessionResumes() {
        assertEquals(Decision.RESUME, InterceptionPolicy().onForeground(ig, 0, true, ig))
    }

    @Test fun ourRelaunchAfterOpenIsNotAnEntry() {
        val p = InterceptionPolicy()
        p.noteForeground(self)
        p.allow(ig, 0, graceSeconds = 180)
        assertEquals(Decision.RELAUNCH, p.onForeground(ig, 500, true, null))
    }

    @Test fun reopenDuringGraceIsRecordedNotPaused() {
        val p = InterceptionPolicy()
        p.noteForeground(self)
        p.allow(ig, 0, graceSeconds = 180)
        p.onForeground(ig, 500, true, null) // relaunch
        p.onForeground(home, 60_000, false, null)
        assertEquals(Decision.GRACE_OPEN, p.onForeground(ig, 120_000, true, null))
        p.onForeground(home, 170_000, false, null)
        assertEquals(Decision.START, p.onForeground(ig, 181_000, true, null))
    }

    @Test fun graceNeverShorterThan30Seconds() {
        val p = InterceptionPolicy()
        p.noteForeground(self)
        p.allow(ig, 0, graceSeconds = 0)
        p.onForeground(ig, 500, true, null)
        p.onForeground(home, 10_000, false, null)
        assertEquals(Decision.GRACE_OPEN, p.onForeground(ig, 20_000, true, null))
        p.onForeground(home, 25_000, false, null)
        assertEquals(Decision.START, p.onForeground(ig, 31_000, true, null))
    }

    @Test fun sameAppScreenChangeIgnored() {
        val p = InterceptionPolicy()
        p.noteForeground(self)
        p.allow(ig, 0, 30)
        p.onForeground(ig, 1_000, true, null)
        assertEquals(Decision.IGNORE, p.onForeground(ig, 60_000, true, null))
    }

    @Test fun debounceRepeatedStarts() {
        val p = InterceptionPolicy()
        assertEquals(Decision.START, p.onForeground(ig, 0, true, null))
        p.onForeground(home, 100, false, null)
        assertEquals(Decision.IGNORE, p.onForeground(ig, 500, true, null))
    }
}
