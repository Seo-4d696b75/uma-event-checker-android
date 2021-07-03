package jp.seo.uma.eventchecker.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.googlecode.tesseract.android.TessBaseAPI
import jp.seo.uma.eventchecker.img.EventTypeDetector
import jp.seo.uma.eventchecker.img.GameHeaderDetector
import jp.seo.uma.eventchecker.img.TemplateDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
class MainViewModel : ViewModel() {

    companion object {
        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"


        fun getInstance(store: ViewModelStore): MainViewModel {
            val factory = object : ViewModelProvider.Factory {
                @SuppressWarnings("unchecked_cast")
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    val obj = MainViewModel()
                    return obj as T
                }
            }
            return ViewModelProvider({ store }, factory).get(MainViewModel::class.java)
        }
    }

    private val _loading = MutableLiveData(false)
    private val _ocrText = MutableLiveData<String>()
    private val _bitmap = MutableLiveData<Bitmap>()

    val loading: LiveData<Boolean> = _loading
    val ocrText: LiveData<String> = _ocrText
    val bitmap: LiveData<Bitmap> = _bitmap

    private var hasInitialized = false
    private lateinit var ocrApi: TessBaseAPI

    private lateinit var headerDetector: TemplateDetector
    private lateinit var charaEventDetector: EventTypeDetector
    private lateinit var supportEventDetector: EventTypeDetector
    private lateinit var mainEventTypeDetector: EventTypeDetector

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        if (hasInitialized) return@launch
        _loading.value = true
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
        hasInitialized = true
        _loading.value = false
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

    private fun testImage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val src = context.assets.getBitmap("test_game_2.jpg")
            val detect = headerDetector.detect(src)
            Log.d("Header", "detect: $detect")
        }

    }

    private val updateMutex = Mutex()
    private var latestBitmap: Bitmap? = null
    private var processRunning: Boolean = false

    fun updateScreen(img: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        if (processRunning) {
            latestBitmap = img
            return@launch
        }
        processRunning = true
        var bitmap: Bitmap? = img
        while (bitmap != null) {
            update(img)
            delay(500L)
            bitmap = latestBitmap
            latestBitmap = null
        }
        processRunning = false
    }

    private fun update(img: Bitmap) {
        Log.d("update", "start...")
        val isGame = headerDetector.detect(img)
        Log.d("update", "isGame $isGame")
        if (!isGame) return
        val type = detectEventType(img)
        Log.d("update", "event type is '${type.toString()}'")
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

    fun setOcrImage(img: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        _ocrText.postValue(text.replace(Regex("[\\s　]+"), ""))
    }


}
