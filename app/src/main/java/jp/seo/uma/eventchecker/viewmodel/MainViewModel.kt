package jp.seo.uma.eventchecker.viewmodel

import android.content.Context
import android.media.Image
import android.media.projection.MediaProjection
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.uma.eventchecker.core.DataRepository
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.model.SettingRepository
import jp.seo.uma.eventchecker.toMat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataRepository,
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

    val loading: LiveData<Boolean> = imgProcess.hasInitialized.map { !it }

    val ocrText = imgProcess.title

    val currentEvent = repository.currentEvent

    @MainThread
    fun init(context: Context) = viewModelScope.launch {
        imgProcess.init(context)
        repository.init(context)
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
        val mat = imgProcess.copyToBitmap(img).toMat()
        val title = imgProcess.getEventTitle(mat)
        repository.setEventTitle(title)
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
