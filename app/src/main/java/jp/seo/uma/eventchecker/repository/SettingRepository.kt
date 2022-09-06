package jp.seo.uma.eventchecker.repository

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.img.readFloat
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/08.
 */
@Singleton
class SettingRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    val isDebugDialogShown = MutableStateFlow(false)

    var screenWidth: Int = 0
    var screenHeight: Int = 0
    var statusBarHeight: Int = 0
    var navigationBarHeight: Int = 0

    var screenCaptureScale = context.resources.readFloat(R.dimen.screen_capture_scale)

    var minUpdateInterval = context.resources.getInteger(R.integer.min_update_interval_ms).toLong()

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
