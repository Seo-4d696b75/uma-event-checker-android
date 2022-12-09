package jp.seo.uma.eventchecker.viewmodel

import android.content.Context
import android.media.Image
import android.media.projection.MediaProjection
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.model.DataRepository
import jp.seo.uma.eventchecker.model.SearchRepository
import jp.seo.uma.eventchecker.model.SettingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val search: SearchRepository,
    private val dataRepository: DataRepository,
    private val imgProcess: ImageProcess,
    private val setting: SettingRepository,
    private val capture: ScreenCapture,
) : ViewModel() {

    companion object {

        fun getInstance(
            store: ViewModelStore,
            repository: DataRepository,
            process: ImageProcess,
            setting: SettingRepository,
            capture: ScreenCapture
        ): MainViewModel {
            val factory = object : ViewModelProvider.Factory {
                @SuppressWarnings("unchecked_cast")
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    val obj = MainViewModel(repository, process, setting, capture)
                    return obj as T
                }
            }
            return ViewModelProvider({ store }, factory).get(MainViewModel::class.java)
        }
    }

    val loading = combine(
        dataRepository.initialized,
        imgProcess.initialized,
    ) { v1, v2 -> !v1 || !v2 }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            true,
        )

    val currentEvent = search.currentEvent

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        imgProcess.init(context)
        dataRepository.init(context)
    }

    fun setMetrics(manager: WindowManager) = setting.setMetrics(manager)

    /**
     * Note: this func is supposed to be called from a callback set at ImageReader,
     * which is running NOT on main thread
     */
    fun updateScreen(img: Image) = runBlocking {
        /*
        ImageReader provides image data via a callback, whereas
        this ViewModel uses coroutine.
        runBlocking is used in order to call suspending style functions in blocking style
         */
        val start = SystemClock.uptimeMillis()
        search.update(img)
        val wait = start + setting.minUpdateInterval - SystemClock.uptimeMillis()
        if (wait > 0L) {
            Log.d("update", "wait $wait ms")
            delay(wait)
        }
    }

    fun startCapture(projection: MediaProjection) = capture.start(projection)

    fun stopCapture() = capture.stop()

    val runningCapture = capture.running

    fun setScreenCallback(callback: ((Image) -> Unit)) {
        capture.callback = callback
    }

}
