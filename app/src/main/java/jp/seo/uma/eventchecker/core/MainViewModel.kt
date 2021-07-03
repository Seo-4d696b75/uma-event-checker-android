package jp.seo.uma.eventchecker.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.googlecode.tesseract.android.TessBaseAPI
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

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        if (hasInitialized) return@launch
        _loading.value = true
        loadData(context)
        headerDetector = GameHeaderDetector(context)
        hasInitialized = true
        _loading.value = false
        testImage(context)
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
            val src = context.assets.getBitmap("test_game_1.jpg")
            val detect = headerDetector.detect(src)
            Log.d("Header", "detect: $detect")
        }

    }

    private val updateMutex = Mutex()
    private var latestBitmap: Bitmap? = null
    private var processRunning: Boolean = false

    fun updateScreen(img: Bitmap) = viewModelScope.launch {
        var run = updateMutex.withLock {
            if (processRunning) {
                Log.d("ViewModel", "update running")
                latestBitmap = img
                false
            } else {
                Log.d("ViewModel", "update start")
                processRunning = true
                latestBitmap = null
                true
            }
        }
        var bitmap = img
        while (run) {
            update(bitmap)
            // keep min interval
            delay(1000L)
            run = updateMutex.withLock {
                val latest = latestBitmap
                if (latest == null) {
                    Log.d("ViewModel", "update stop")
                    processRunning = false
                    false
                } else {
                    Log.d("ViewModel", "update continue")
                    bitmap = latest
                    latestBitmap = null
                    true
                }
            }
        }

    }

    private suspend fun update(img: Bitmap) = withContext(Dispatchers.IO) {
        Log.d("ViewModel", "update")
        delay(100L)
    }

    fun setOcrImage(img: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        _ocrText.postValue(text.replace(Regex("[\\s　]+"), ""))
    }


}
