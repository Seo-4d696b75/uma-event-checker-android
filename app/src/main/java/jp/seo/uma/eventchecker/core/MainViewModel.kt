package jp.seo.uma.eventchecker.core

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
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

        const val MIN_UPDATE_INTERVAL = 500L
    }

    val loading: LiveData<Boolean> = imgProcess.hasInitialized.map { !it }

    val ocrText = imgProcess.title

    val currentEvent = repository.currentEvent

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        imgProcess.init(context)
    }

    @Volatile
    private var processRunning: Boolean = false

    fun updateScreen(img: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        if (processRunning) {
            Log.d("update", "skip")
            return@launch
        }
        val start = SystemClock.uptimeMillis()
        processRunning = true
        val title = imgProcess.process(img)
        repository.setEventTitle(title)
        val wait = start + MIN_UPDATE_INTERVAL - SystemClock.uptimeMillis()
        if (wait > 0L) {
            Log.d("update", "wait $wait ms")
            delay(wait)
        }
        processRunning = false
    }

}
