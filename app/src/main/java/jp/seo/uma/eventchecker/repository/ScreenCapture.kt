package jp.seo.uma.eventchecker.repository

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@Singleton
class ScreenCapture @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val repository: SettingRepository
) : ImageReader.OnImageAvailableListener {

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null

    var callback: ((Image) -> Unit)? = null

    private val _running = MutableLiveData<Boolean>(false)

    val running: LiveData<Boolean> = _running

    private var thread: HandlerThread? = null


    fun start(projection: MediaProjection) {
        if (display == null) {
            // Prepare another thread than Main one to process image
            val thread = HandlerThread("screen-capture")
            thread.start()
            this.thread = thread
            val handler = Handler(thread.looper)
            this.projection = projection
            display = createDisplay(projection, handler)
            _running.value = true
        }
    }

    @SuppressLint("WrongConstant")
    private fun createDisplay(projection: MediaProjection, handler: Handler): VirtualDisplay {
        val width = repository.capturedScreenWidth
        val height = repository.capturedScreenHeight
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
            callback?.invoke(img)
        }
        img.close()
    }

    fun stop() {
        display?.release()
        display = null
        reader?.close()
        reader = null
        projection?.stop()
        projection = null
        thread?.quit()
        thread = null
        _running.value = false
        callback = null
    }

}
