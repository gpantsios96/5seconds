package app.anasa.pause

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.anasa.container
import app.anasa.data.BreathLevel
import app.anasa.data.Stats
import app.anasa.service.PauseSession
import java.time.LocalDate

private enum class Phase { IN, OUT, DONE }

@Composable
fun PauseScreen(
    session: PauseSession,
    onOpen: (intention: String?) -> Unit,
    onNotNow: (intention: String?) -> Unit,
) {
    val context = LocalContext.current
    val apps = context.container.installedApps
    val events = context.container.database.events()
    val label = remember(session.packageName) { apps.label(session.packageName) }
    val icon = remember(session.packageName) { apps.icon(session.packageName)?.asImageBitmap() }
    val attemptsToday by remember(session.packageName) {
        events.attemptsSince(session.packageName, Stats.startOfDay(LocalDate.now()))
    }.collectAsState(initial = null)

    val haptics = remember { Haptics(context) }
    val progress = remember { Animatable(0f) }
    var phase by rememberSaveable { mutableStateOf(Phase.IN) }
    var breath by remember { mutableIntStateOf(1) }
    var intention by rememberSaveable { mutableStateOf("") }

    val halfBreathMs = BreathLevel.BREATH_SECONDS * 1000 / 2

    LaunchedEffect(session.id) {
        if (phase == Phase.DONE) return@LaunchedEffect
        val ease = tween<Float>(halfBreathMs, easing = FastOutSlowInEasing)
        repeat(session.breathLevel.breaths) { i ->
            breath = i + 1
            phase = Phase.IN
            if (session.haptics) haptics.inhale(halfBreathMs.toLong())
            progress.animateTo(1f, ease)
            phase = Phase.OUT
            if (session.haptics) haptics.tick()
            progress.animateTo(0f, ease)
        }
        if (session.haptics) haptics.tick()
        phase = Phase.DONE
        // Settle into a calm, half-open shape with an expressive spring.
        progress.animateTo(0.6f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        // Stop any running waveform if the screen goes away mid-breath.
        try { kotlinx.coroutines.awaitCancellation() } finally { haptics.cancel() }
    }

    val trimmed = intention.trim().takeIf { it.isNotEmpty() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = attemptsLine(label, attemptsToday),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BreathingCircle(
                    progress = progress.value,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    haloColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f),
                )
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                    label = "phase",
                ) { p ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (p) {
                                Phase.IN -> "Breathe in"
                                Phase.OUT -> "Breathe out"
                                Phase.DONE -> "Still want to?"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (p != Phase.DONE && session.breathLevel.breaths > 1) {
                            Text(
                                "Breath $breath of ${session.breathLevel.breaths}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = phase == Phase.DONE,
                enter = fadeIn(tween(300)) +
                    slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) { it / 3 } +
                    scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), initialScale = 0.9f),
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (session.intentionPrompt) {
                        OutlinedTextField(
                            value = intention,
                            onValueChange = { intention = it.take(120) },
                            label = { Text("What do you want to do on $label?") },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = { onNotNow(trimmed) },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),
                    ) {
                        Text("Not now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onOpen(trimmed) },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Text("Open $label", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun attemptsLine(label: String, count: Int?): String = when {
    count == null -> " "
    count <= 1 -> "First time opening $label today."
    else -> "You've opened $label $count times today."
}
