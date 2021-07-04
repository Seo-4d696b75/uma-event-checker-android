package jp.seo.uma.eventchecker.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@Singleton
class ScreenCapture @Inject constructor(
    @ApplicationContext
    private val context: Context
) : ImageReader.OnImageAvailableListener {

    private var display: VirtualDisplay? = null
    private val _screen = MutableLiveData<Bitmap>()

    var callback: ((Bitmap) -> Unit)? = null

    val screen: LiveData<Bitmap> = _screen
    val initialized: Boolean
        get() = display != null

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var statusBarHeight: Int = 0
    private var navigationBarHeight: Int = 0

    private val scale = context.resources.readFloat(R.dimen.screen_capture_scale)

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
        var resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        navigationBarHeight = context.resources.getDimensionPixelSize(resourceId)

    }

    fun setMediaProjection(projection: MediaProjection) {
        if (display == null) {
            display = createDisplay(projection)
        }
    }

    @SuppressLint("WrongConstant")
    private fun createDisplay(projection: MediaProjection): VirtualDisplay {
        val width = (screenWidth * scale).toInt()
        val height = (screenHeight * scale).toInt()
        context.resources.displayMetrics.let {
            val reader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )
            reader.setOnImageAvailableListener(this, null)
            return projection.createVirtualDisplay(
                "CapturedDisplay",
                width,
                height,
                it.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null, null
            )
        }
    }

    override fun onImageAvailable(reader: ImageReader) {
        val img = reader.acquireLatestImage() ?: return
        val plane = img.planes[0]
        val bitmap = Bitmap.createBitmap(
            plane.rowStride / plane.pixelStride,
            (screenHeight * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)
        img.close()
        val crop = Bitmap.createBitmap(
            bitmap,
            0,
            (statusBarHeight * scale).toInt(),
            (screenWidth * scale).toInt(),
            ((screenHeight - statusBarHeight - navigationBarHeight) * scale).toInt()
        )
        bitmap.recycle()
        Log.d("Screen", "captured")
        _screen.postValue(crop)
        callback?.invoke(crop)
    }

    fun release() {
        display?.release()
        display = null
        callback = null
    }

}
