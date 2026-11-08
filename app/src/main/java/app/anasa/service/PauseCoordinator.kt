package app.anasa.service

import android.content.Context
import android.content.Intent
import app.anasa.data.AppConfigDao
import app.anasa.data.BreathLevel
import app.anasa.data.Event
import app.anasa.data.EventDao
import app.anasa.data.EventType
import app.anasa.data.SettingsRepository
import app.anasa.pause.PauseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the pause screen needs, resolved (per-app overrides applied) when the session starts. */
data class PauseSession(
    val id: Long,
    val packageName: String,
    val breathLevel: BreathLevel,
    val graceSeconds: Int,
    val haptics: Boolean,
    val intentionPrompt: Boolean,
)

/**
 * Owns the lifecycle of pause sessions: decides when to show the pause screen, logs events
 * and keeps the in-memory allow windows. All methods must be called on the main thread.
 */
class PauseCoordinator(
    private val scope: CoroutineScope,
    private val events: EventDao,
    private val configs: AppConfigDao,
    private val settings: SettingsRepository,
) {
    private val policy = InterceptionPolicy()

    /** Guarded packages, kept hot so the service can check them synchronously. */
    private val guarded: StateFlow<Set<String>> = configs.all()
        .map { list -> list.map { it.packageName }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val _session = MutableStateFlow<PauseSession?>(null)
    /** The pause session on screen right now, or null. The pause screen finishes when this changes. */
    val session: StateFlow<PauseSession?> = _session.asStateFlow()

    private var nextId = 1L
    private var interruptJob: Job? = null

    /** How long a pause screen that was pushed to the background has to come back before it counts as "avoided". */
    private val interruptGraceMillis = 2_000L

    fun onForeground(context: Context, pkg: String, now: Long = System.currentTimeMillis()) {
        if (pkg == context.packageName) {
            policy.noteForeground(pkg)
            return
        }
        val active = _session.value
        when (policy.onForeground(pkg, now, pkg in guarded.value, active?.packageName)) {
            InterceptionPolicy.Decision.IGNORE,
            InterceptionPolicy.Decision.RELAUNCH,
            -> Unit
            InterceptionPolicy.Decision.GRACE_OPEN -> scope.launch {
                // Every entry into a guarded app is recorded, including ones without a pause.
                events.insert(Event(timestamp = now, packageName = pkg, type = EventType.GRACE_OPEN))
            }
            InterceptionPolicy.Decision.RESUME -> {
                interruptJob?.cancel()
                launchPause(context)
            }
            InterceptionPolicy.Decision.START -> {
                // A different app's pause was still pending: the user didn't open it.
                active?.let { resolve(it.id, EventType.AVOIDED, null) }
                startSession(context, pkg, now)
            }
        }
    }

    private fun startSession(context: Context, pkg: String, now: Long) {
        val id = nextId++
        scope.launch {
            events.insert(Event(timestamp = now, packageName = pkg, type = EventType.ATTEMPT))
        }
        scope.launch {
            val s = settings.settings.first()
            val c = configs.get(pkg)
            // Only publish if nothing replaced this session meanwhile.
            if (_session.value == null || _session.value?.id == id) {
                _session.value = PauseSession(
                    id = id,
                    packageName = pkg,
                    breathLevel = c?.breathLevel ?: s.breathLevel,
                    graceSeconds = c?.graceSeconds ?: s.graceSeconds,
                    haptics = s.haptics,
                    intentionPrompt = s.intentionPrompt,
                )
                launchPause(context)
            }
        }
    }

    private fun launchPause(context: Context) {
        val intent = Intent(context, PauseActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
        )
        context.startActivity(intent)
    }

    /**
     * Records the outcome of session [id] and ends it. Ignored if [id] is no longer the active session.
     * On [EventType.OPENED] the app gets its grace period.
     */
    fun resolve(id: Long, outcome: EventType, intention: String?, now: Long = System.currentTimeMillis()) {
        val s = _session.value ?: return
        if (s.id != id) return
        interruptJob?.cancel()
        if (outcome == EventType.OPENED) policy.allow(s.packageName, now, s.graceSeconds)
        _session.value = null
        scope.launch {
            events.insert(
                Event(
                    timestamp = now,
                    packageName = s.packageName,
                    type = outcome,
                    intention = intention?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
    }

    /**
     * The pause screen went to the background without a choice (home button, screen off, or the
     * guarded app briefly drew over it). If it isn't brought back quickly, count it as avoided.
     */
    fun onPauseInterrupted(id: Long) {
        if (_session.value?.id != id) return
        interruptJob?.cancel()
        interruptJob = scope.launch {
            delay(interruptGraceMillis)
            resolve(id, EventType.AVOIDED, null)
        }
    }

    /** The pause screen is visible again; cancel any pending "avoided" timeout. */
    fun onPauseVisible(id: Long) {
        if (_session.value?.id == id) interruptJob?.cancel()
    }
}
