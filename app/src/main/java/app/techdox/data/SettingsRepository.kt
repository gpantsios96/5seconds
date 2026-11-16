package app.techdox.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Global settings. Per-app overrides live in Room ([AppConfig]). */
data class Settings(
    val pauseLength: PauseLength = PauseLength.SHORT,
    val graceSeconds: Int = DEFAULT_GRACE_SECONDS,
    val haptics: Boolean = true,
    val intentionPrompt: Boolean = false,
    val onboardingDone: Boolean = false,
    /** The user's own reasons for cutting back, shown on the pause card. */
    val reasons: List<String> = emptyList(),
    /** When the user has written reasons, still show a built-in line now and then. */
    val mixBuiltIn: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val level = stringPreferencesKey("pause_length")
        val grace = intPreferencesKey("grace_seconds")
        val haptics = booleanPreferencesKey("haptics")
        val intention = booleanPreferencesKey("intention_prompt")
        val onboarding = booleanPreferencesKey("onboarding_done")
        /** One reason per line. */
        val reasons = stringPreferencesKey("reasons")
        val mixBuiltIn = booleanPreferencesKey("mix_built_in")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            pauseLength = p[Keys.level]?.let { runCatching { PauseLength.valueOf(it) }.getOrNull() }
                ?: PauseLength.SHORT,
            graceSeconds = (p[Keys.grace] ?: DEFAULT_GRACE_SECONDS)
                .coerceAtLeast(MIN_GRACE_SECONDS),
            haptics = p[Keys.haptics] ?: true,
            intentionPrompt = p[Keys.intention] ?: false,
            onboardingDone = p[Keys.onboarding] ?: false,
            reasons = p[Keys.reasons]?.split('\n')?.filter { it.isNotBlank() }.orEmpty(),
            mixBuiltIn = p[Keys.mixBuiltIn] ?: true,
        )
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setPauseLength(level: PauseLength) = set(Keys.level, level.name)
    suspend fun setGraceSeconds(seconds: Int) = set(Keys.grace, seconds.coerceAtLeast(MIN_GRACE_SECONDS))
    suspend fun setHaptics(on: Boolean) = set(Keys.haptics, on)
    suspend fun setIntentionPrompt(on: Boolean) = set(Keys.intention, on)
    suspend fun setReasons(reasons: List<String>) =
        set(Keys.reasons, Reflections.sanitize(reasons).joinToString("\n"))
    suspend fun setMixBuiltIn(on: Boolean) = set(Keys.mixBuiltIn, on)
    suspend fun setOnboardingDone(done: Boolean) = set(Keys.onboarding, done)
}
