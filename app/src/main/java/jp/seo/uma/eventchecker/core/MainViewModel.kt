package jp.seo.uma.eventchecker.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val factory = object : ViewModelProvider.Factory {
            @SuppressWarnings("unchecked_cast")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                val obj = MainViewModel()
                return obj as T
            }
        }

        fun getInstance(store: ViewModelStore): MainViewModel {
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

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        if (hasInitialized) return@launch
        _loading.value = true
        loadData(context)
        testImage(context)
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
        val img = BitmapFactory.decodeStream(context.assets.open("test.png"))
        _bitmap.postValue(img)
        setOcrImage(img)

    }

    fun setOcrImage(img: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        _ocrText.postValue(text.replace(Regex("[\\s　]+"), ""))
    }

}
