package app.techdox.pause

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.techdox.container
import app.techdox.data.Reflections
import app.techdox.data.Stats
import app.techdox.service.PauseSession
import java.time.LocalDate

/**
 * The pause: a card with a short reflection. The choice buttons appear once the card has been on
 * screen for the session's pause length.
 */
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

    // Saved so a rotation keeps the same line and doesn't restart the wait.
    val reflectionIndex = rememberSaveable { Reflections.pickIndex() }
    var ready by rememberSaveable { mutableStateOf(false) }
    var intention by rememberSaveable { mutableStateOf("") }

    val haptics = remember { Haptics(context) }
    val wait = remember { Animatable(if (ready) 1f else 0f) }
    val cardIn = remember { Animatable(if (ready) 1f else 0.85f) }

    LaunchedEffect(Unit) {
        cardIn.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        if (ready) return@LaunchedEffect
        wait.animateTo(1f, tween(session.pauseLength.seconds * 1000, easing = LinearEasing))
        ready = true
        if (session.haptics) haptics.tick()
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .graphicsLayer {
                            scaleX = cardIn.value
                            scaleY = cardIn.value
                        },
                    shape = RoundedCornerShape(topStart = 56.dp, topEnd = 20.dp, bottomEnd = 56.dp, bottomStart = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(horizontal = 28.dp, vertical = 32.dp)) {
                        Text(
                            "“",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(48.dp),
                        )
                        Text(
                            Reflections.text(reflectionIndex, label),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(28.dp))
                        LinearProgressIndicator(
                            progress = { wait.value },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = ready,
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
