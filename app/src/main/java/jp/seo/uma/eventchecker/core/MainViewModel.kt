package jp.seo.uma.eventchecker.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.*
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataRepository,
    private val imgProcess: ImageProcess
) : ViewModel() {

    companion object {
        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"


        fun getInstance(
            store: ViewModelStore,
            repository: DataRepository,
            process: ImageProcess
        ): MainViewModel {
            val factory = object : ViewModelProvider.Factory {
                @SuppressWarnings("unchecked_cast")
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    val obj = MainViewModel(repository, process)
                    return obj as T
                }
            }
            return ViewModelProvider({ store }, factory).get(MainViewModel::class.java)
        }
    }

    private val _loading = MutableLiveData(false)

    val loading: LiveData<Boolean> = _loading

    val ocrText = imgProcess.title

    val currentEvent = repository.currentEvent

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        if (imgProcess.hasInitialized) return@launch
        _loading.value = true
        imgProcess.init(context)
        _loading.value = false
    }


    @Volatile
    private var processRunning: Boolean = false

    fun updateScreen(img: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        if (processRunning) {
            Log.d("update", "skip")
            return@launch
        }
        processRunning = true
        val title = imgProcess.process(img)
        repository.setEventTitle(title)
        processRunning = false
    }

}
