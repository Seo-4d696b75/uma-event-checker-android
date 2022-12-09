package jp.seo.uma.eventchecker.data

import android.graphics.Bitmap
import android.media.Image
import jp.seo.uma.eventchecker.data.model.GameEvent
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.toMat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val detector: ImageProcess,
    private val setting: SettingRepository,
    private val dataRepository: DataRepository,
) {

    private val _currentTitle = MutableStateFlow<String?>(null)
    val currentTitle = _currentTitle.asStateFlow()

    private val _currentEvent = MutableStateFlow<GameEvent?>(null)
    val currentEvent = _currentEvent.asStateFlow()

    fun update(img: Image) {
        val bitmap = img.cropScreenContent()
        val title = detector.getEventTitle(bitmap.toMat())
        if (title != _currentTitle.value) {
            _currentTitle.update { title }
            val event = title?.let {
                dataRepository.searchEvent(it)
            }
            _currentEvent.update { event }
        }
    }

    // システムUI（ステータスバー・ナビゲーションバー）の部分を削除
    private fun Image.cropScreenContent(): Bitmap {
        val plane = this.planes[0]
        val bitmap = Bitmap.createBitmap(
            plane.rowStride / plane.pixelStride,
            setting.capturedScreenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)
        // Remove area of status-bar and navigation-bar
        val crop = Bitmap.createBitmap(
            bitmap,
            0,
            setting.capturedStatusBarHeight,
            setting.capturedScreenWidth,
            setting.capturedContentHeight
        )
        bitmap.recycle()
        return crop
    }
}