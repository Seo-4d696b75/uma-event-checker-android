package jp.seo.uma.eventchecker.repository

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.img.readFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/08.
 */
@Singleton
class SettingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val isDebugDialogShown = MutableStateFlow(false)

    var screenWidth: Int = 0
    var screenHeight: Int = 0
    var statusBarHeight: Int = 0
    var navigationBarHeight: Int = 0

    var screenCaptureScale = context.resources.readFloat(R.dimen.screen_capture_scale)

    val minUpdateInterval = PreferenceFlow(
        longPreferencesKey("min_update_interval_ms"),
        context.dataStore,
        500L,
    )

    val ocrThreshold = PreferenceFlow(
        floatPreferencesKey("orc_threshold"),
        context.dataStore,
        0.5f,
    )

    init {
        var resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        navigationBarHeight = context.resources.getDimensionPixelSize(resourceId)
    }

    fun setMetrics(windowManager: WindowManager) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metric = windowManager.currentWindowMetrics
            screenWidth = metric.bounds.width()
            screenHeight = metric.bounds.height()
        } else {
            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)
            screenWidth = size.x
            screenHeight = size.y
        }

    }

    val capturedScreenWidth: Int
        get() = (screenWidth * screenCaptureScale).toInt()

    val capturedScreenHeight: Int
        get() = (screenHeight * screenCaptureScale).toInt()

    val capturedStatusBarHeight: Int
        get() = (statusBarHeight * screenCaptureScale).toInt()

    val capturedContentHeight: Int
        get() = ((screenHeight - statusBarHeight - navigationBarHeight) * screenCaptureScale).toInt()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("setting")

class PreferenceFlow<T> constructor(
    private val key: Preferences.Key<T>,
    private val dataStore: DataStore<Preferences>,
    private val defaultValue: T,
) : Flow<T> {

    private val flow = dataStore.data.map {
        it[key] ?: defaultValue
    }

    override suspend fun collect(collector: FlowCollector<T>) {
        flow.collect(collector)
    }

    private var runningUpdate: Job? = null

    fun update(scope: CoroutineScope, producer: suspend () -> T) = scope.launch {
        runningUpdate?.cancel()
        runningUpdate = launch {
            dataStore.edit { it[key] = producer() }
        }
    }

    fun stateIn(scope: CoroutineScope, started: SharingStarted) =
        flow.stateIn(scope, started, defaultValue)
}