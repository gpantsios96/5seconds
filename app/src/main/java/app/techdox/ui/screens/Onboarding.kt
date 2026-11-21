package app.techdox.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import app.techdox.ui.centeredContent
import app.techdox.APP_NAME
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.techdox.container
import app.techdox.data.AppConfig
import app.techdox.pause.PulseShape
import app.techdox.ui.ReasonsEditor
import app.techdox.ui.SetupChecklist
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.first
import app.techdox.ui.rememberAccessibilityEnabled
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var suggestionsApplied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(page) {
        if (page == 1 && !suggestionsApplied) {
            suggestionsApplied = true
            val installed = c.installedApps.launchableApps().map { it.packageName }.toSet()
            SUGGESTED_PACKAGES.filter { it in installed }
                .forEach { c.database.appConfigs().insertIfAbsent(AppConfig(it)) }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                slideInHorizontally(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) { it * dir } togetherWith
                    slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it * dir }
            },
            modifier = Modifier.safeDrawingPadding(),
            label = "onboarding",
        ) { p ->
            when (p) {
                0 -> WelcomePage(onNext = { page = 1 })
                1 -> Column(Modifier.fillMaxSize()) {
                    Header("Which apps pull you in?", "$APP_NAME will pause before these. You can change this any time.")
                    AppPickerList(Modifier.weight(1f))
                    NavRow(onBack = { page = 0 }, onNext = { page = 2 })
                }
                2 -> ReasonsPage(onBack = { page = 1 }, onNext = { page = 3 })
                else -> SetupPage(
                    onBack = { page = 2 },
                    onFinish = { scope.launch { c.settings.setOnboardingDone(true) } },
                )
            }
        }
    }
}

@Composable
private fun Header(title: String, body: String) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).centeredContent()) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NavRow(onBack: (() -> Unit)?, onNext: () -> Unit, nextLabel: String = "Next", nextEnabled: Boolean = true) {
    Row(
        Modifier.padding(16.dp).centeredContent(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) TextButton(onClick = onBack) { Text("Back") } else Spacer(Modifier)
        Button(onClick = onNext, enabled = nextEnabled, shape = RoundedCornerShape(28.dp), modifier = Modifier.heightIn(min = 52.dp)) {
            Text(nextLabel)
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "demo").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "demo",
    )
    // Scrolls when the window is too short (landscape, split-screen); the shape is sized from
    // the smaller side of the window so it never pushes the rest off-screen.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pulseSize = minOf(maxWidth * 0.7f, maxHeight * 0.4f, 360.dp)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(24.dp)
                .centeredContent(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(0.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PulseShape(
                    progress = pulse,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    haloColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(pulseSize),
                )
                Spacer(Modifier.height(32.dp))
                Text("$APP_NAME", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    "A moment to think before the apps you open on autopilot. Then you decide.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Free. Offline. No account. Nothing leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))
            }
            Button(onClick = onNext, shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                Text("Get started", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ReasonsPage(onBack: () -> Unit, onNext: () -> Unit) {
    val settings = LocalContext.current.container.settings
    // Load once; the editor saves every change itself.
    val saved by produceState<List<String>?>(null) { value = settings.settings.first().reasons }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).centeredContent()) {
            Header(
                "Why are you doing this?",
                "Write it in your own words. $APP_NAME will show you these lines when you reach for a guarded app. You can change them any time.",
            )
            saved?.let { ReasonsEditor(it, Modifier.padding(horizontal = 16.dp)) }
            Spacer(Modifier.height(16.dp))
        }
        NavRow(onBack = onBack, onNext = onNext)
    }
}

@Composable
private fun SetupPage(onBack: () -> Unit, onFinish: () -> Unit) {
    val enabled = rememberAccessibilityEnabled()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).centeredContent()) {
            Header(
                "Set up your phone",
                "$APP_NAME needs Accessibility access to notice when you open a guarded app. Each button below opens the right settings page; come back here afterwards.",
            )
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What $APP_NAME can see", style = MaterialTheme.typography.titleMedium)
                    Text("• Which app just came to the front. That's it.")
                    Text("What it can't see", style = MaterialTheme.typography.titleMedium)
                    Text("• Screen content, what you type, messages or notifications.")
                    Text("• And there's no internet permission, so nothing can leave your phone.")
                }
            }
            Spacer(Modifier.height(12.dp))
            SetupChecklist(Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
        }
        NavRow(onBack = onBack, onNext = onFinish, nextLabel = if (enabled) "Finish" else "Skip for now")
    }
}
