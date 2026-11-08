package app.anasa.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.anasa.container
import app.anasa.data.EventType
import app.anasa.data.Stats
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen() {
    val c = LocalContext.current.container
    val today = remember { LocalDate.now() }
    val from = today.minusDays(6)
    val weekEvents by remember {
        c.database.events().eventsBetween(Stats.startOfDay(from), Stats.startOfDay(today.plusDays(1)))
    }.collectAsState(initial = emptyList())
    val guarded by remember { c.database.appConfigs().all() }.collectAsState(initial = emptyList())
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    val avoidedTimes by remember(selected) { c.database.events().timestampsOf(EventType.AVOIDED, selected) }
        .collectAsState(initial = emptyList())
    val intentions by remember(selected) {
        selected?.let { c.database.events().recentIntentions(it, 5) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val filtered = weekEvents.filter { selected == null || it.packageName == selected }
    val days = Stats.perDay(filtered, from, today)
    val streak = Stats.streak(avoidedTimes.map { Stats.toDate(it) }.toSet(), today)
    // Apps that appear in this week's data or are guarded, for the filter chips.
    val appChoices = (guarded.map { it.packageName } + weekEvents.map { it.packageName }).distinct()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Last 7 days", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = selected == null, onClick = { selected = null }, label = { Text("All apps") })
            appChoices.forEach { pkg ->
                FilterChip(
                    selected = selected == pkg,
                    onClick = { selected = pkg },
                    label = { Text(c.installedApps.label(pkg)) },
                )
            }
        }

        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(20.dp)) {
                WeekChart(
                    days,
                    avoidedColor = MaterialTheme.colorScheme.primary,
                    openedColor = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Legend(MaterialTheme.colorScheme.primary, "Not now")
                    Legend(MaterialTheme.colorScheme.tertiary, "Opened")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Current streak", streak.current, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
            StatTile("Best streak", streak.best, MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Not now (7d)", days.sumOf { it.avoided }, MaterialTheme.colorScheme.surfaceContainerHigh, Modifier.weight(1f))
            StatTile("Opened (7d)", days.sumOf { it.opened }, MaterialTheme.colorScheme.surfaceContainerHigh, Modifier.weight(1f))
        }
        StatTile(
            "Re-opened during grace (7d)",
            days.sumOf { it.graceOpens },
            MaterialTheme.colorScheme.surfaceContainerHigh,
            Modifier.fillMaxWidth(),
        )

        if (selected != null && intentions.isNotEmpty()) {
            Card(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recent intentions", style = MaterialTheme.typography.titleMedium)
                    intentions.forEach { e ->
                        val verb = if (e.type == EventType.OPENED) "opened" else "not now"
                        Text("“${e.intention}” — $verb", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Stacked bars: "not now" at the bottom, "opened" on top. */
@Composable
private fun WeekChart(days: List<Stats.DayCounts>, avoidedColor: Color, openedColor: Color) {
    val max = (days.maxOfOrNull { it.avoided + it.opened } ?: 0).coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Column {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val slot = size.width / days.size
            val barW = slot * 0.55f
            val radius = CornerRadius(barW / 2, barW / 2)
            days.forEachIndexed { i, d ->
                val x = i * slot + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), radius)
                val total = d.avoided + d.opened
                if (total == 0) return@forEachIndexed
                val totalH = size.height * total / max
                val avoidedH = size.height * d.avoided / max
                drawRoundRect(openedColor, Offset(x, size.height - totalH), Size(barW, totalH), radius)
                if (d.avoided > 0) {
                    drawRoundRect(avoidedColor, Offset(x, size.height - avoidedH), Size(barW, avoidedH), radius)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach { d ->
                Text(
                    d.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
