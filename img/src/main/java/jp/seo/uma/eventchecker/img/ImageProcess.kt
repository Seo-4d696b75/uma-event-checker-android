package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.MainThread
import com.googlecode.tesseract.android.TessBaseAPI
import jp.seo.uma.eventchecker.img.crop.EventTitleCropper
import jp.seo.uma.eventchecker.img.detect.EventTypeDetector
import jp.seo.uma.eventchecker.img.detect.GameHeaderDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author Seo-4d696b75
 * @version 2021/07/06.
 */
class ImageProcess {

    companion object {
        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"
    }

    private val _initialized = MutableStateFlow(false)
    val initialized = _initialized.asStateFlow()

    private lateinit var ocrApi: TessBaseAPI

    private lateinit var headerDetector: GameHeaderDetector
    private lateinit var eventTypeDetector: EventTypeDetector
    private lateinit var eventTitleCropper: EventTitleCropper

    @MainThread
    suspend fun init(context: Context) {
        if (_initialized.value) return
        loadData(context)
        initOpenCV(context)
        headerDetector = GameHeaderDetector(context)
        eventTypeDetector = EventTypeDetector(context)
        eventTitleCropper = EventTitleCropper(context)
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
            context.copyAssetsToFiles(OCR_TRAINED_DATA, file)
        }
        ocrApi = TessBaseAPI()
        if (!ocrApi.init(context.filesDir.toString(), "jpn")) {
            throw RuntimeException("fail to ocr client")
        }
    }

    private suspend fun initOpenCV(context: Context) = suspendCoroutine {
        if (OpenCVLoader.initDebug()) {
            it.resume(Unit)
        } else {
            val callback = object : BaseLoaderCallback(context) {
                override fun onManagerConnected(status: Int) {
                    if (status == LoaderCallbackInterface.SUCCESS) {
                        it.resume(Unit)
                    } else {
                        it.resumeWithException(RuntimeException("fail to init OpenCV"))
                    }
                }
            }
            OpenCVLoader.initAsync("4.5.2", context, callback)
        }
    }

    fun getEventTitle(src: Bitmap): String? {
        val img = src.toMat()
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
