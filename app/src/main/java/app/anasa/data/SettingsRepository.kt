package app.anasa.data

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
    val breathLevel: BreathLevel = BreathLevel.GENTLE,
    val graceSeconds: Int = DEFAULT_GRACE_SECONDS,
    val haptics: Boolean = true,
    val intentionPrompt: Boolean = false,
    val onboardingDone: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val level = stringPreferencesKey("breath_level")
        val grace = intPreferencesKey("grace_seconds")
        val haptics = booleanPreferencesKey("haptics")
        val intention = booleanPreferencesKey("intention_prompt")
        val onboarding = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            breathLevel = p[Keys.level]?.let { runCatching { BreathLevel.valueOf(it) }.getOrNull() }
                ?: BreathLevel.GENTLE,
            graceSeconds = (p[Keys.grace] ?: DEFAULT_GRACE_SECONDS)
                .coerceAtLeast(MIN_GRACE_SECONDS),
            haptics = p[Keys.haptics] ?: true,
            intentionPrompt = p[Keys.intention] ?: false,
            onboardingDone = p[Keys.onboarding] ?: false,
        )
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setBreathLevel(level: BreathLevel) = set(Keys.level, level.name)
    suspend fun setGraceSeconds(seconds: Int) = set(Keys.grace, seconds.coerceAtLeast(MIN_GRACE_SECONDS))
    suspend fun setHaptics(on: Boolean) = set(Keys.haptics, on)
    suspend fun setIntentionPrompt(on: Boolean) = set(Keys.intention, on)
    suspend fun setOnboardingDone(done: Boolean) = set(Keys.onboarding, done)
}
