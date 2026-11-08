package app.anasa.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.anasa.container
import app.anasa.data.AppConfig
import app.anasa.data.BreathLevel
import app.anasa.data.GRACE_OPTIONS
import app.anasa.data.graceLabel
import app.anasa.data.Settings
import app.anasa.ui.AppIcon
import kotlinx.coroutines.launch

fun BreathLevel.describe() = "$label · $breaths × ${BreathLevel.BREATH_SECONDS}s"

@Composable
fun AppsScreen(settings: Settings, onAddApps: () -> Unit) {
    val c = LocalContext.current.container
    val configs by remember { c.database.appConfigs().all() }.collectAsState(initial = emptyList())
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddApps,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add apps") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("Guarded apps", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(8.dp)) }
            if (configs.isEmpty()) {
                item { Text("Nothing guarded yet. Tap “Add apps”.", modifier = Modifier.padding(8.dp)) }
            }
            items(configs, key = { it.packageName }) { cfg ->
                val level = cfg.breathLevel ?: settings.breathLevel
                val grace = cfg.graceSeconds ?: settings.graceSeconds
                Card(
                    Modifier.fillMaxWidth().clickable { editing = cfg.packageName },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(cfg.packageName, 44.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.installedApps.label(cfg.packageName), style = MaterialTheme.typography.titleMedium)
                            Text(
                                buildString {
                                    append(level.describe())
                                    if (cfg.breathLevel == null) append(" (default)")
                                    append(" · grace ").append(graceLabel(grace))
                                    if (cfg.graceSeconds == null) append(" (default)")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    configs.firstOrNull { it.packageName == editing }?.let { cfg ->
        AppConfigSheet(cfg, settings, onDismiss = { editing = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppConfigSheet(cfg: AppConfig, settings: Settings, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val dao = c.database.appConfigs()
    fun save(new: AppConfig) { scope.launch { dao.upsert(new) } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(cfg.packageName, 48.dp)
                Spacer(Modifier.width(16.dp))
                Text(c.installedApps.label(cfg.packageName), style = MaterialTheme.typography.headlineSmall)
            }
            Text("Breath", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = cfg.breathLevel == null,
                    onClick = { save(cfg.copy(breathLevel = null)) },
                    label = { Text("Default (${settings.breathLevel.label})") },
                )
                BreathLevel.entries.forEach { l ->
                    FilterChip(
                        selected = cfg.breathLevel == l,
                        onClick = { save(cfg.copy(breathLevel = l)) },
                        label = { Text(l.describe()) },
                    )
                }
            }
            Text("Grace period after opening", style = MaterialTheme.typography.titleMedium)
            Text(
                "Leave and come back within this time without another pause.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = cfg.graceSeconds == null,
                    onClick = { save(cfg.copy(graceSeconds = null)) },
                    label = { Text("Default (${graceLabel(settings.graceSeconds)})") },
                )
                GRACE_OPTIONS.forEach { m ->
                    FilterChip(
                        selected = cfg.graceSeconds == m,
                        onClick = { save(cfg.copy(graceSeconds = m)) },
                        label = { Text(graceLabel(m)) },
                    )
                }
            }
            TextButton(onClick = {
                scope.launch { dao.delete(cfg.packageName) }
                onDismiss()
            }) { Text("Stop guarding this app", color = MaterialTheme.colorScheme.error) }
        }
    }
}
