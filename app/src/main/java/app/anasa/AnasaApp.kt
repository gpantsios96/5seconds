package app.anasa

import android.app.Application
import android.content.Context
import app.anasa.data.AnasaDatabase
import app.anasa.data.InstalledApps
import app.anasa.data.SettingsRepository
import app.anasa.service.PauseCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AnasaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container: one instance of each long-lived object. */
class AppContainer(context: Context) {
    /** App-wide scope on the main thread; Room/DataStore do their own IO dispatching. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database = AnasaDatabase.create(context)
    val settings = SettingsRepository(context)
    val installedApps = InstalledApps(context)
    val pauseCoordinator = PauseCoordinator(scope, database.events(), database.appConfigs(), settings)
}

val Context.container: AppContainer get() = (applicationContext as AnasaApp).container

/** Display name used in UI copy. Keep in sync with `app_name` in res/values/strings.xml. */
const val APP_NAME = "Anasa"
