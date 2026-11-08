package app.anasa.pause

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.anasa.container
import app.anasa.data.EventType
import app.anasa.service.PauseCoordinator
import app.anasa.ui.theme.AnasaTheme

/**
 * Full-screen pause shown instead of a guarded app. Its UI is driven by [PauseCoordinator.session];
 * when the session ends (or is replaced) the activity finishes or recomposes.
 */
class PauseActivity : ComponentActivity() {

    private val coordinator: PauseCoordinator by lazy { container.pauseCoordinator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Back = "Not now": the user never lands in the guarded app by accident.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = notNow(null)
        })

        setContent {
            AnasaTheme {
                val session by coordinator.session.collectAsStateWithLifecycle(
                    minActiveState = androidx.lifecycle.Lifecycle.State.CREATED,
                )
                LaunchedEffect(session) { if (session == null) finish() }
                session?.let { s ->
                    key(s.id) {
                        PauseScreen(
                            session = s,
                            onOpen = { intention -> open(intention) },
                            onNotNow = { intention -> notNow(intention) },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        coordinator.session.value?.let { coordinator.onPauseVisible(it.id) }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) {
            coordinator.session.value?.let { coordinator.onPauseInterrupted(it.id) }
        }
    }

    private fun open(intention: String?) {
        val s = coordinator.session.value ?: return finish()
        val launch = packageManager.getLaunchIntentForPackage(s.packageName)
        coordinator.resolve(s.id, EventType.OPENED, intention)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }
        finish()
    }

    private fun notNow(intention: String?) {
        coordinator.session.value?.let { coordinator.resolve(it.id, EventType.AVOIDED, intention) }
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }
}
