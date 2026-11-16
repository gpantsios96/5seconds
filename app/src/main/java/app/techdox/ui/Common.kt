package app.techdox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.techdox.container
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Re-evaluates [read] every time the screen resumes (e.g. coming back from system settings). */
@Composable
fun <T> rememberOnResume(read: () -> T): T {
    val owner = LocalLifecycleOwner.current
    var value by remember { mutableStateOf(read()) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) value = read()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return value
}

/** Accessibility-service state, re-checked every time the screen resumes. */
@Composable
fun rememberAccessibilityEnabled(): Boolean {
    val context = LocalContext.current
    return rememberOnResume { SetupActions.isAccessibilityEnabled(context) }
}

/** Today's date, refreshed on resume so screens left open overnight roll over. */
@Composable
fun rememberToday(): java.time.LocalDate = rememberOnResume { java.time.LocalDate.now() }

@Composable
fun AppIcon(packageName: String, size: Dp = 40.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) { context.container.installedApps.icon(packageName)?.asImageBitmap() }
    }
    val b = bitmap
    if (b != null) {
        Image(b, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}
