package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.googlecode.tesseract.android.TessBaseAPI
import jp.seo.uma.eventchecker.core.MainViewModel
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
class ImageProcess @Inject constructor() {

    private val _title = MutableLiveData<String?>(null)
    val title: LiveData<String?> = _title

    var hasInitialized = false
    private lateinit var ocrApi: TessBaseAPI

    private lateinit var headerDetector: TemplateDetector
    private lateinit var charaEventDetector: EventTypeDetector
    private lateinit var supportEventDetector: EventTypeDetector
    private lateinit var mainEventTypeDetector: EventTypeDetector
    private lateinit var eventTitleCropper: EventTitleProcess

    suspend fun init(context: Context) {
        if (hasInitialized) return
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

        hasInitialized = true
    }

    private suspend fun loadData(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, MainViewModel.OCR_DATA_DIR)
        if (!dir.exists() || !dir.isDirectory) {
            if (!dir.mkdir()) {
                throw RuntimeException("fail to mkdir: $dir")
            }
        }
        val file = File(dir, MainViewModel.OCR_TRAINED_DATA)
        if (!file.exists()) {
            copyAssetsToFiles(context, MainViewModel.OCR_TRAINED_DATA, file)
        }
        ocrApi = TessBaseAPI()
        if (!ocrApi.init(context.filesDir.toString(), "jpn")) {
            throw RuntimeException("fail to ocr client")
        }
    }

    fun process(img: Bitmap): String? {
        val isGame = headerDetector.detect(img)
        Log.d("update", "target $isGame")
        if (isGame) {
            val type = detectEventType(img)
            Log.d("update", "event type '${type.toString()}'")
            if (type != null) {
                val title = getEventTitle(img)
                Log.d("update", "event title '$title'")
                _title.postValue(title)
                return title
            }
        }
        _title.postValue(null)
        return null
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

    private fun getEventTitle(img: Bitmap): String {
        val target = eventTitleCropper.preProcess(img)
        return extractText(target)
    }

    private fun extractText(img: Bitmap): String {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        return text.replace(Regex("[\\s　]+"), "")
    }


}
