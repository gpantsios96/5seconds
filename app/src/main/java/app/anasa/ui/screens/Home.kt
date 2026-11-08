package app.anasa.ui.screens

import app.anasa.APP_NAME
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.anasa.container
import app.anasa.data.EventType
import app.anasa.data.Stats
import app.anasa.ui.AccessibilityStatus
import app.anasa.ui.AppIcon
import app.anasa.ui.rememberAccessibilityEnabled
import java.time.LocalDate

@Composable
fun HomeScreen(onPickApps: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val today = remember { LocalDate.now() }
    val events by remember {
        c.database.events().eventsBetween(Stats.startOfDay(today), Stats.startOfDay(today.plusDays(1)))
    }.collectAsState(initial = emptyList())
    val avoidedTimes by remember { c.database.events().timestampsOf(EventType.AVOIDED, null) }
        .collectAsState(initial = emptyList())
    val guarded by remember { c.database.appConfigs().all() }.collectAsState(initial = null)
    val a11y = rememberAccessibilityEnabled()

    val streak = remember(avoidedTimes) { Stats.streak(avoidedTimes.map { Stats.toDate(it) }.toSet(), today) }
    val attempts = events.count { it.type == EventType.ATTEMPT }
    val opened = events.count { it.type == EventType.OPENED }
    val avoided = events.count { it.type == EventType.AVOIDED }
    val perApp = events.groupBy { it.packageName }.map { (pkg, e) ->
        AppDay(
            pkg,
            opened = e.count { it.type == EventType.OPENED },
            avoided = e.count { it.type == EventType.AVOIDED },
            graceOpens = e.count { it.type == EventType.GRACE_OPEN },
        )
    }.sortedByDescending { it.opened + it.avoided + it.graceOpens }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Today", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(8.dp))
        }
        if (!a11y) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("$APP_NAME is paused", style = MaterialTheme.typography.titleMedium)
                        Text("The Accessibility service is off, so guarded apps open without a breath.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { AccessibilityStatus.openSettings(context) }) { Text("Turn on") }
                    }
                }
            }
        }
        if (guarded?.isEmpty() == true) {
            item {
                Card(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("No apps guarded yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onPickApps) { Text("Choose apps") }
                    }
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 48.dp, topEnd = 16.dp, bottomEnd = 48.dp, bottomStart = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${streak.current}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            if (streak.current == 1) "day avoided streak" else "days avoided streak",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Best: ${streak.best}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Pauses", attempts, MaterialTheme.colorScheme.surfaceContainerHigh, Modifier.weight(1f))
                StatTile("Not now", avoided, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
                StatTile("Opened", opened, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
            }
        }
        if (perApp.isNotEmpty()) {
            item { Text("By app", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) }
            items(perApp, key = { it.pkg }) { (pkg, o, a, g) ->
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(pkg)
                        Spacer(Modifier.width(16.dp))
                        Text(c.installedApps.label(pkg), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            buildString {
                                append("$a not now · $o opened")
                                if (g > 0) append(" · $g in grace")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private data class AppDay(val pkg: String, val opened: Int, val avoided: Int, val graceOpens: Int)
