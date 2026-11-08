package app.anasa.service

import app.anasa.data.MIN_GRACE_SECONDS

/**
 * Pure decision logic for "what should happen when this app comes to the front?".
 * No Android types, so it can be unit-tested. Not thread-safe: call from one thread.
 *
 * Rules, in order, for a guarded app:
 * 1. A pause for this app is already open → bring it back ([Decision.RESUME]).
 * 2. Moving between screens inside the same app → nothing ([Decision.IGNORE]).
 * 3. Repeated window events within [debounceMillis] of starting a pause → nothing.
 * 4. The first entry after the user tapped "Open" is our own relaunch → [Decision.RELAUNCH].
 * 5. Inside the app's grace period → [Decision.GRACE_OPEN] (recorded, no pause).
 * 6. Otherwise → [Decision.START] a pause.
 */
class InterceptionPolicy(
    private val debounceMillis: Long = 1_500,
    /** How long after "Open" we expect our own relaunch of the app to arrive. */
    private val relaunchWindowMillis: Long = 5_000,
) {
    enum class Decision {
        /** Do nothing, record nothing. */
        IGNORE,
        /** Start a new pause for this app. */
        START,
        /** A pause for this app is already active: bring the pause screen back on top. */
        RESUME,
        /** Our own launch of the app right after "Open". Not a new entry. */
        RELAUNCH,
        /** A new entry into the app during its grace period: record it, but don't pause. */
        GRACE_OPEN,
    }

    private var lastForeground: String? = null
    private val graceUntil = HashMap<String, Long>()
    private val relaunchUntil = HashMap<String, Long>()
    private val lastStart = HashMap<String, Long>()

    /**
     * Called for every relevant foreground change.
     * @param guarded whether [pkg] is on the guarded list
     * @param activeSessionPkg package of the pause currently on screen (or just interrupted), if any
     */
    fun onForeground(pkg: String, now: Long, guarded: Boolean, activeSessionPkg: String?): Decision {
        val previous = lastForeground
        lastForeground = pkg

        if (!guarded) return Decision.IGNORE
        if (activeSessionPkg == pkg) return Decision.RESUME
        if (previous == pkg) return Decision.IGNORE
        val last = lastStart[pkg]
        if (last != null && now - last < debounceMillis) return Decision.IGNORE

        if ((relaunchUntil.remove(pkg) ?: 0L) > now) return Decision.RELAUNCH
        if ((graceUntil[pkg] ?: 0L) > now) return Decision.GRACE_OPEN

        lastStart[pkg] = now
        return Decision.START
    }

    /** Records that something that isn't a guarded app (e.g. this app itself) is in front. */
    fun noteForeground(pkg: String) {
        lastForeground = pkg
    }

    /** The user chose "Open": expect our relaunch, then give the app its grace period. */
    fun allow(pkg: String, now: Long, graceSeconds: Int) {
        relaunchUntil[pkg] = now + relaunchWindowMillis
        graceUntil[pkg] = now + graceSeconds.coerceAtLeast(MIN_GRACE_SECONDS) * 1_000L
    }
}
