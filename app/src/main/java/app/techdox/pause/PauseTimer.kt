package app.techdox.pause

/**
 * The pause countdown, computed from when the pause started rather than from a running counter.
 * Recreating the screen (rotation, fold/unfold, window resize) therefore neither restarts nor
 * skips the wait. Times are from a monotonic clock (`SystemClock.elapsedRealtime()`).
 */
object PauseTimer {
    /** Fraction of the wait that has passed, from 0 to 1. */
    fun progress(startedAt: Long, now: Long, durationMillis: Long): Float {
        if (durationMillis <= 0) return 1f
        return ((now - startedAt).toFloat() / durationMillis).coerceIn(0f, 1f)
    }

    /** Whether the choice buttons may be shown. */
    fun isDone(startedAt: Long, now: Long, durationMillis: Long): Boolean =
        now - startedAt >= durationMillis
}
