package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.MainThread
import com.googlecode.tesseract.android.TessBaseAPI
import jp.seo.uma.eventchecker.copyAssetsToFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
class ImageProcess @Inject constructor() {

    companion object {
        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"
    }

    private val _initialized = MutableStateFlow(false)
    val initialized = _initialized.asStateFlow()

    private lateinit var ocrApi: TessBaseAPI

    private lateinit var headerDetector: GameHeaderDetector
    private lateinit var eventTypeDetector: EventTypeDetector
    private lateinit var eventTitleCropper: EventTitleProcess

    @MainThread
    suspend fun init(context: Context) {
        if (_initialized.value) return
        loadData(context)
        headerDetector = GameHeaderDetector(context)
        eventTypeDetector = EventTypeDetector(context)
        eventTitleCropper = EventTitleProcess(context)
        _initialized.update { true }
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

    fun getEventTitle(img: Mat): String? {
        if (!_initialized.value) return null
        val isGame = headerDetector.detect(img)
        Log.d("update", "target $isGame")
        if (isGame) {
            val type = eventTypeDetector.detect(img)
            Log.d("update", "event type '${type.toString()}'")
            if (type != null) {
                val title = extractEventTitle(img)
                Log.d("update", "event title '$title'")
                return title
            }
        }
        return null
    }

    private fun extractEventTitle(img: Mat): String {
        val target = eventTitleCropper.preProcess(img)
        return extractText(target)
    }

    private fun extractText(img: Bitmap): String {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        return text.replace(Regex("[\\s　]+"), "")
    }
}
