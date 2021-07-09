package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.googlecode.tesseract.android.TessBaseAPI
import jp.seo.uma.eventchecker.core.SettingRepository
import jp.seo.uma.eventchecker.core.copyAssetsToFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
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

    private lateinit var headerDetector: GameHeaderDetector
    private lateinit var eventTypeDetector: EventTypeDetector
    private lateinit var eventTitleCropper: EventTitleProcess
    private lateinit var charaEventOwnerDetector: TemplatesMatcher
    private lateinit var supportEventOwnerDetector: TemplatesMatcher

    @MainThread
    suspend fun init(context: Context) {
        if (_initialized) return
        loadData(context)
        headerDetector = GameHeaderDetector(context)
        eventTypeDetector = EventTypeDetector(context)
        eventTitleCropper = EventTitleProcess(context)
        val iconData = loadEventOwners(context)
        charaEventOwnerDetector = getCharaEventOwnerDetector(context, iconData)
        supportEventOwnerDetector = getSupportEventOwnerDetector(context, iconData)
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
        bitmap.copyPixelsFromBuffer(plane.buffer)// Remove area of status-bar and navigation-bar
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

    private var eventType: EventType? = null

    /**
     * Gets event title from the screen image
     *
     * @param img bitmap of the screen without status-bar nor navigation-bar
     */
    fun getEventTitle(img: Mat): String? {
        if (!_initialized) return null
        val isGame = headerDetector.detect(img)
        Log.d("Img", "check is-target $isGame")
        if (isGame) {
            val type = eventTypeDetector.detect(img)
            Log.d("Img", "event type '${type.toString()}'")
            if (type != null) {
                val title = extractEventTitle(img)
                _title.postValue(title)
                eventType = type
                return title
            }
        }
        eventType = null
        _title.postValue(null)
        return null
    }

    /**
     * Gets what type event is now shown on the screen
     *
     * **Note** Be sure to call after [getEventTitle] returns non null value, or an exception will be thrown
     */
    suspend fun getEventOwner(img: Mat): String {
        return when (eventType) {
            EventType.Main -> "URA"
            EventType.Support -> supportEventOwnerDetector.find(img).name
            EventType.Chara -> charaEventOwnerDetector.find(img).name
            null -> throw IllegalStateException("event type not found")
        }
    }

    private fun extractEventTitle(img: Mat): String {
        val start = SystemClock.uptimeMillis()
        val target = eventTitleCropper.preProcess(img)
        val title = extractText(target)
        Log.d("OCR", "title: $title time: ${SystemClock.uptimeMillis() - start}ms")
        return title
    }

    private fun extractText(img: Bitmap): String {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        return text.replace(Regex("\\s+"), "")
    }


}
