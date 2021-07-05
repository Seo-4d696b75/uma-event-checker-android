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
import android.os.Handler
import android.os.HandlerThread
import android.view.WindowManager
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

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null

    var callback: ((Bitmap) -> Unit)? = null

    val initialized: Boolean
        get() = display != null

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var statusBarHeight: Int = 0
    private var navigationBarHeight: Int = 0

    private val scale = context.resources.readFloat(R.dimen.screen_capture_scale)

    private var thread: HandlerThread? = null

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
            // Prepare another thread than Main one to process image
            val thread = this.thread ?: kotlin.run {
                val t = HandlerThread("screen-capture")
                t.start()
                this.thread = t
                t
            }
            val handler = Handler(thread.looper)
            this.projection = projection
            display = createDisplay(projection, handler)
        }
    }

    @SuppressLint("WrongConstant")
    private fun createDisplay(projection: MediaProjection, handler: Handler): VirtualDisplay {
        val width = (screenWidth * scale).toInt()
        val height = (screenHeight * scale).toInt()
        context.resources.displayMetrics.let {
            val reader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                4
            )
            reader.setOnImageAvailableListener(this, handler)
            this.reader = reader
            return projection.createVirtualDisplay(
                "CapturedDisplay",
                width,
                height,
                it.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null, handler
            )
        }
    }

    override fun onImageAvailable(reader: ImageReader) {
        val img = reader.acquireLatestImage() ?: return
        kotlin.runCatching {
            val plane = img.planes[0]
            val bitmap = Bitmap.createBitmap(
                plane.rowStride / plane.pixelStride,
                (screenHeight * scale).toInt(),
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(plane.buffer)
            // Remove are of status-bar and navigation-bar
            val crop = Bitmap.createBitmap(
                bitmap,
                0,
                (statusBarHeight * scale).toInt(),
                (screenWidth * scale).toInt(),
                ((screenHeight - statusBarHeight - navigationBarHeight) * scale).toInt()
            )
            bitmap.recycle()
            //Log.d("Screen", "captured")
            callback?.invoke(crop)
        }
        img.close()
    }

    fun release() {
        display?.release()
        display = null
        reader?.close()
        reader = null
        projection?.stop()
        projection = null
        callback = null
        thread?.quit()
        thread = null
    }

}
