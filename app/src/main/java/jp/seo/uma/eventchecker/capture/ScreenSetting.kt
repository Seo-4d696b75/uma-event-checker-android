package jp.seo.uma.eventchecker.capture

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.data.readFloat
import jp.seo.uma.eventchecker.data.repository.SettingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenSetting @Inject constructor(
    @ApplicationContext context: Context
) : SettingRepository {

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var statusBarHeight: Int = 0
    private var navigationBarHeight: Int = 0

    private var screenCaptureScale = context.resources.readFloat(R.dimen.screen_capture_scale)

    override val minUpdateInterval =
        context.resources.getInteger(R.integer.min_update_interval_ms).toLong()

    init {
        var resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        navigationBarHeight = context.resources.getDimensionPixelSize(resourceId)
    }

    override fun setMetrics(windowManager: WindowManager) {

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

    override val capturedScreenWidth: Int
        get() = (screenWidth * screenCaptureScale).toInt()

    override val capturedScreenHeight: Int
        get() = (screenHeight * screenCaptureScale).toInt()

    override val capturedStatusBarHeight: Int
        get() = (statusBarHeight * screenCaptureScale).toInt()

    override val capturedContentHeight: Int
        get() = ((screenHeight - statusBarHeight - navigationBarHeight) * screenCaptureScale).toInt()
}