package app.anasa.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import app.anasa.data.InstalledApp
import app.anasa.ui.AppIcon
import kotlinx.coroutines.launch

/** Commonly distracting apps, ticked by default during onboarding if they are installed. */
val SUGGESTED_PACKAGES = listOf(
    "com.instagram.android",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.google.android.youtube",
    "com.twitter.android",
    "com.facebook.katana",
    "com.reddit.frontpage",
    "com.snapchat.android",
)

/** Searchable list of installed apps; ticking an app guards it immediately. */
@Composable
fun AppPickerList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val apps by produceState<List<InstalledApp>?>(null) { value = c.installedApps.launchableApps() }
    val guarded by remember { c.database.appConfigs().all() }.collectAsState(initial = emptyList())
    val guardedSet = guarded.map { it.packageName }.toSet()
    var query by rememberSaveable { mutableStateOf("") }

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        val shown = list.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            // Guarded first, then alphabetical.
            .sortedWith(compareByDescending<InstalledApp> { it.packageName in guardedSet }.thenBy { it.label.lowercase() })
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.packageName }) { app ->
                val checked = app.packageName in guardedSet
                val toggle = {
                    scope.launch {
                        val dao = c.database.appConfigs()
                        if (checked) dao.delete(app.packageName) else dao.insertIfAbsent(AppConfig(app.packageName))
                    }
                    Unit
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = toggle)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.packageName)
                    Spacer(Modifier.width(16.dp))
                    Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Checkbox(checked = checked, onCheckedChange = { toggle() })
                }
            }
        }
    }
}

@Composable
fun AppPickerScreen(onDone: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Text(
                "Choose apps to pause",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
            )
            AppPickerList(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDone) { Text("Done") }
            }
        }
    }
}
