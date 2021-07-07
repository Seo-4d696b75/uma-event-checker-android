package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.googlecode.tesseract.android.TessBaseAPI
import jp.seo.uma.eventchecker.core.SettingRepository
import jp.seo.uma.eventchecker.core.copyAssetsToFiles
import jp.seo.uma.eventchecker.core.getBitmap
import jp.seo.uma.eventchecker.core.toGrayMat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/06.
 */
@Singleton
class ImageProcess @Inject constructor(
    private val repository: SettingRepository
) {

    companion object {

        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"
    }

    private val _title = MutableLiveData<String?>(null)
    val title: LiveData<String?> = _title

    private val initialized = MutableLiveData(false)
    private var _initialized = false
    var hasInitialized: LiveData<Boolean> = initialized
    private lateinit var ocrApi: TessBaseAPI

    private lateinit var headerDetector: TemplateDetector
    private lateinit var charaEventDetector: EventTypeDetector
    private lateinit var supportEventDetector: EventTypeDetector
    private lateinit var mainEventTypeDetector: EventTypeDetector
    private lateinit var eventTitleCropper: EventTitleProcess

    @MainThread
    suspend fun init(context: Context) {
        if (_initialized) return
        loadData(context)
        headerDetector = GameHeaderDetector(context)
        charaEventDetector = EventTypeDetector(
            context.assets.getBitmap("template/event_chara.png").toGrayMat(),
            context
        )
        supportEventDetector = EventTypeDetector(
            context.assets.getBitmap("template/event_support.png").toGrayMat(),
            context
        )
        mainEventTypeDetector = EventTypeDetector(
            context.assets.getBitmap("template/event_main.png").toGrayMat(),
            context
        )
        eventTitleCropper = EventTitleProcess(context)

        initialized.value = true
        _initialized = true
    }

    private suspend fun loadData(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, OCR_DATA_DIR)
        if (!dir.exists() || !dir.isDirectory) {
            if (!dir.mkdir()) {
                throw RuntimeException("fail to mkdir: $dir")
            }
        }
        val file = File(dir, OCR_TRAINED_DATA)
        if (!file.exists()) {
            copyAssetsToFiles(context, OCR_TRAINED_DATA, file)
        }
        ocrApi = TessBaseAPI()
        if (!ocrApi.init(context.filesDir.toString(), "jpn")) {
            throw RuntimeException("fail to ocr client")
        }
    }

    fun copyToBitmap(img: Image): Bitmap {
        val plane = img.planes[0]
        val bitmap = Bitmap.createBitmap(
            plane.rowStride / plane.pixelStride,
            repository.capturedScreenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)
        return bitmap
    }

    fun getEventTitle(screen: Bitmap): String? {
        if (!_initialized) return null
        val bitmap = cropContentArea(screen)
        val isGame = headerDetector.detect(bitmap)
        Log.d("update", "target $isGame")
        if (isGame) {
            val type = detectEventType(bitmap)
            Log.d("update", "event type '${type.toString()}'")
            if (type != null) {
                val title = extranctEventTitle(bitmap)
                Log.d("update", "event title '$title'")
                _title.postValue(title)
                return title
            }
        }
        _title.postValue(null)
        return null
    }

    private fun cropContentArea(bitmap: Bitmap): Bitmap {
        // Remove area of status-bar and navigation-bar
        val crop = Bitmap.createBitmap(
            bitmap,
            0,
            repository.capturedStatusBarHeight,
            repository.capturedScreenWidth,
            repository.capturedContentHeight
        )
        bitmap.recycle()
        return crop
    }

    private enum class EventType {
        Main, Chara, Support
    }

    private fun detectEventType(img: Bitmap): EventType? {
        if (charaEventDetector.detect(img)) return EventType.Chara
        if (supportEventDetector.detect(img)) return EventType.Support
        if (mainEventTypeDetector.detect(img)) return EventType.Main
        return null
    }

    private fun extranctEventTitle(img: Bitmap): String {
        val target = eventTitleCropper.preProcess(img)
        return extractText(target)
    }

    private fun extractText(img: Bitmap): String {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        return text.replace(Regex("[\\s　]+"), "")
    }


}
