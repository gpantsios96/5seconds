package app.anasa.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.anasa.container
import app.anasa.data.Settings
import app.anasa.ui.screens.AppPickerScreen
import app.anasa.ui.screens.AppsScreen
import app.anasa.ui.screens.HomeScreen
import app.anasa.ui.screens.OnboardingScreen
import app.anasa.ui.screens.SettingsScreen
import app.anasa.ui.screens.StatsScreen
import app.anasa.ui.theme.AnasaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsFlow = container.settings.settings
        setContent {
            AnasaTheme {
                val settings by settingsFlow.collectAsState(initial = null)
                when (val s = settings) {
                    null -> Unit // DataStore loads in a few ms; avoid flashing onboarding.
                    else -> if (!s.onboardingDone) OnboardingScreen() else MainScaffold(s)
                }
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.Home),
    STATS("Stats", Icons.Filled.Star),
    APPS("Apps", Icons.Filled.List),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
private fun MainScaffold(settings: Settings) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var picking by rememberSaveable { mutableStateOf(false) }

    if (picking) {
        BackHandler { picking = false }
        AppPickerScreen(onDone = { picking = false })
        return
    }
    BackHandler(enabled = tab != Tab.TODAY) { tab = Tab.TODAY }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(targetState = tab, modifier = Modifier.padding(padding), label = "tab") { t ->
            when (t) {
                Tab.TODAY -> HomeScreen(onPickApps = { picking = true })
                Tab.STATS -> StatsScreen()
                Tab.APPS -> AppsScreen(settings = settings, onAddApps = { picking = true })
                Tab.SETTINGS -> SettingsScreen(settings = settings)
            }
        }
    }
}
