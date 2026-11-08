package app.anasa.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.Settings
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String)

/** Reads launchable apps and their icons. Visibility comes from the manifest's <queries>. */
class InstalledApps(private val context: Context) {
    private val pm: PackageManager get() = context.packageManager
    private val iconCache = LruCache<String, Bitmap>(64)
    private val labelCache = HashMap<String, String>()

    /** Packages that must never be guarded, or the user could lock themselves out. */
    fun neverGuard(): Set<String> = buildSet {
        add(context.packageName)
        addAll(homePackages())
        add("com.android.settings")
        Intent(Settings.ACTION_SETTINGS).resolveActivity(pm)?.packageName?.let(::add)
        add("com.android.systemui")
    }

    fun homePackages(): Set<String> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()

    suspend fun launchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val excluded = neverGuard()
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filterNot { it in excluded }
            .map { InstalledApp(it, label(it)) }
            .sortedBy { it.label.lowercase() }
    }

    fun label(pkg: String): String = synchronized(labelCache) {
        labelCache.getOrPut(pkg) {
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                .getOrDefault(pkg)
        }
    }

    fun icon(pkg: String): Bitmap? {
        iconCache.get(pkg)?.let { return it }
        return runCatching { pm.getApplicationIcon(pkg).toBitmap(144, 144) }
            .getOrNull()
            ?.also { iconCache.put(pkg, it) }
    }

    fun isLaunchable(pkg: String): Boolean = pm.getLaunchIntentForPackage(pkg) != null
}
