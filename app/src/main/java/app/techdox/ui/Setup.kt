package app.techdox.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.techdox.APP_NAME
import app.techdox.service.TechdoxAccessibilityService

/** Deep links into the system settings pages the app needs, with safe fallbacks. */
object SetupActions {
    private val maker = Build.MANUFACTURER.lowercase()
    val isXiaomi = maker in setOf("xiaomi", "redmi", "poco")
    val isSamsung = maker == "samsung"

    private fun serviceComponent(context: Context) =
        ComponentName(context, TechdoxAccessibilityService::class.java)

    /** True if the user has switched on the accessibility service in system settings. */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val me = serviceComponent(context)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun isBatteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true

    /** Tries each intent in turn; the last resort is always the app's own App info page. */
    private fun start(context: Context, vararg intents: Intent) {
        for (intent in intents.toList() + appInfoIntent(context)) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {
                // Not available on this phone; try the next one.
            }
        }
    }

    private fun appInfoIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))

    /** Accessibility settings; on most phones our entry is scrolled to and highlighted. */
    fun openAccessibility(context: Context) {
        val key = serviceComponent(context).flattenToString()
        val highlight = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(":settings:fragment_args_key", key)
            .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", key) })
        start(context, highlight, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** App info: "Allow restricted settings", battery, and (Xiaomi) autostart all live here. */
    fun openAppInfo(context: Context) = start(context)

    fun openBattery(context: Context) {
        // Samsung and Xiaomi keep the per-app battery switch on the App info page.
        if (isSamsung || isXiaomi) start(context)
        else start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    fun openXiaomiAutostart(context: Context) = start(
        context,
        Intent().setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        ),
    )

    /** MIUI/HyperOS "Other permissions" page, which holds the background pop-up permission. */
    fun openXiaomiPermissions(context: Context) {
        fun editor() = Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", context.packageName)
        start(
            context,
            editor().setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"),
            editor(),
        )
    }
}

/**
 * The first-run checklist: one card per system setting, each with a button that opens the right
 * settings page. Steps that can be detected show a tick once done.
 */
@Composable
fun SetupChecklist(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val a11y = rememberAccessibilityEnabled()
    val battery = rememberOnResume { SetupActions.isBatteryUnrestricted(context) }
    var n = 0

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SetupStep(
            number = ++n,
            title = "Turn on the $APP_NAME service",
            body = "In the list, tap $APP_NAME (it may be under \"Downloaded apps\" or \"Installed apps\") and switch it on.",
            done = a11y,
            button = "Open Accessibility settings",
            onClick = { SetupActions.openAccessibility(context) },
        )
        if (!a11y && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SetupStep(
                number = ++n,
                title = "Switch greyed out, or \"Restricted setting\"?",
                body = "Android blocks this for apps installed from a file. After you've tapped the switch once, " +
                    "open App info, tap ⋮ at the top right, choose \"Allow restricted settings\", then repeat step 1.",
                done = null,
                button = "Open App info",
                onClick = { SetupActions.openAppInfo(context) },
            )
        }
        SetupStep(
            number = ++n,
            title = "Let it run in the background",
            body = when {
                SetupActions.isXiaomi -> "In App info: Battery saver → No restrictions."
                SetupActions.isSamsung -> "In App info: Battery → Unrestricted. Also check it isn't listed under Sleeping apps."
                else -> "Choose \"All apps\", find $APP_NAME and select \"Don't optimise\"."
            },
            done = battery,
            button = "Open battery settings",
            onClick = { SetupActions.openBattery(context) },
        )
        if (SetupActions.isXiaomi) {
            SetupStep(
                number = ++n,
                title = "Allow pop-ups in the background",
                body = "Turn on \"Display pop-up windows while running in the background\". " +
                    "Without it, Xiaomi stops the pause screen from appearing.",
                done = null,
                button = "Open permissions",
                onClick = { SetupActions.openXiaomiPermissions(context) },
            )
            SetupStep(
                number = ++n,
                title = "Allow autostart",
                body = "Switch on $APP_NAME so it keeps working after a restart.",
                done = null,
                button = "Open Autostart",
                onClick = { SetupActions.openXiaomiAutostart(context) },
            )
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    body: String,
    /** true/false when the app can detect the state; null when it can't. */
    done: Boolean?,
    button: String,
    onClick: () -> Unit,
) {
    val container =
        if (done == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (done == true) "✓" else "$number",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(14.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            if (done == true) {
                Text("Done", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (done == null) FilledTonalButton(onClick = onClick) { Text(button) }
                else Button(onClick = onClick) { Text(button) }
            }
        }
    }
}
