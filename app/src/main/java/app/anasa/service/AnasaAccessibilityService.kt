package app.anasa.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import app.anasa.AnasaApp
import app.anasa.data.InstalledApps

/**
 * Watches which app is in the foreground and asks [PauseCoordinator] whether to show the pause.
 *
 * Only `TYPE_WINDOW_STATE_CHANGED` is subscribed to, and window content is never read
 * (`canRetrieveWindowContent=false` in the service config).
 */
class AnasaAccessibilityService : AccessibilityService() {

    private lateinit var apps: InstalledApps
    private lateinit var coordinator: PauseCoordinator

    /**
     * Cache of "is this package an app the user can be 'in'?". Keyboards, the notification shade and
     * system dialogs also emit window events; ignoring them avoids false "you just reopened Instagram"
     * triggers when a keyboard closes.
     */
    private val relevant = HashMap<String, Boolean>()
    private var homes: Set<String> = emptySet()

    override fun onServiceConnected() {
        val container = (application as AnasaApp).container
        apps = container.installedApps
        coordinator = container.pauseCoordinator
        homes = apps.homePackages()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != packageName && !isRelevant(pkg)) return
        coordinator.onForeground(this, pkg)
    }

    private fun isRelevant(pkg: String): Boolean =
        relevant.getOrPut(pkg) { pkg in homes || apps.isLaunchable(pkg) }

    override fun onInterrupt() = Unit
}
