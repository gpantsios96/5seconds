package app.techdox.ui.screens

import app.techdox.ui.centeredContent
import app.techdox.APP_NAME
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.techdox.BuildConfigInfo
import app.techdox.container
import app.techdox.data.PauseLength
import app.techdox.data.GRACE_OPTIONS
import app.techdox.data.graceLabel
import app.techdox.data.Settings
import app.techdox.ui.ReasonsEditor
import app.techdox.ui.SetupChecklist
import app.techdox.ui.rememberAccessibilityEnabled
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: Settings) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val a11y = rememberAccessibilityEnabled()
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).centeredContent(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(8.dp))

        Section("Default pause length", "How long the reflection card stays before you can choose.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PauseLength.entries.forEach { l ->
                    FilterChip(
                        selected = settings.pauseLength == l,
                        onClick = { scope.launch { c.settings.setPauseLength(l) } },
                        label = { Text(l.describe()) },
                    )
                }
            }
        }
        Section("Default grace period", "After you open an app, leave and come back within this time without another pause.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GRACE_OPTIONS.forEach { m ->
                    FilterChip(
                        selected = settings.graceSeconds == m,
                        onClick = { scope.launch { c.settings.setGraceSeconds(m) } },
                        label = { Text(graceLabel(m)) },
                    )
                }
            }
        }
        Section("Your reasons", "The pause card shows these, in your own words.") {
            ReasonsEditor(settings.reasons)
            ToggleRow("Mix in $APP_NAME's own lines", settings.mixBuiltIn) { scope.launch { c.settings.setMixBuiltIn(it) } }
        }
        Section("Pause screen") {
            ToggleRow("Vibrate when the pause ends", settings.haptics) { scope.launch { c.settings.setHaptics(it) } }
            ToggleRow("Ask “What do you want to do?”", settings.intentionPrompt) { scope.launch { c.settings.setIntentionPrompt(it) } }
        }
        Section("Phone setup", if (a11y) "The service is on." else "The service is off, so guarded apps open without a pause.") {
            SetupChecklist()
        }
        Section("Your data", "Everything is stored only on this phone. Uninstalling $APP_NAME deletes it.") {
            OutlinedButton(onClick = { confirmReset = true }) { Text("Reset all stats") }
        }
        Section("About", "$APP_NAME ${BuildConfigInfo.versionName(context)} · Free and offline. No accounts, no ads, no analytics, no internet permission.") {}
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all stats?") },
            text = { Text("This deletes every pause, open and “not now” record. Guarded apps and settings stay.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { c.database.events().deleteAll() }
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
