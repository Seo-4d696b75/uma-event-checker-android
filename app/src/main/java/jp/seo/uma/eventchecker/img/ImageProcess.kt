package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import com.googlecode.tesseract.android.TessBaseAPI
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.uma.eventchecker.model.Partner
import jp.seo.uma.eventchecker.model.SupportCard
import jp.seo.uma.eventchecker.repository.DataRepository
import jp.seo.uma.eventchecker.repository.SettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Seo-4d696b75
 * @version 2021/07/06.
 */
@Singleton
class ImageProcess @Inject constructor(
    private val repository: SettingRepository,
    private val data: DataRepository,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val OCR_DATA_DIR = "tessdata"
        const val OCR_TRAINED_DATA = "jpn.traineddata"
    }

    private val initialized = MutableStateFlow(false)
    private var _initialized = false
    val hasInitialized: StateFlow<Boolean> = initialized

    private lateinit var ocrApi: TessBaseAPI

    private lateinit var headerDetector: GameHeaderDetector
    private lateinit var eventTypeDetector: EventTypeDetector
    private lateinit var eventTitleCropper: EventTitleProcess
    private lateinit var charaEventOwnerDetector: TemplatesMatcher<Partner>
    private lateinit var supportEventOwnerDetector: TemplatesMatcher<SupportCard>

    @MainThread
    suspend fun init() {
        if (_initialized) return
        loadData()
        headerDetector = GameHeaderDetector(context)
        eventTypeDetector = EventTypeDetector(context)
        eventTitleCropper = EventTitleProcess(context)
        val iconData = data.eventOwners
        charaEventOwnerDetector = getCharaEventOwnerDetector(context, iconData)
        supportEventOwnerDetector = getSupportEventOwnerDetector(context, iconData)
        initialized.value = true
        _initialized = true
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {
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

    fun copyToBitmap(img: Image): Bitmap {
        val plane = img.planes[0]
        val bitmap = Bitmap.createBitmap(
            plane.rowStride / plane.pixelStride,
            repository.capturedScreenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)// Remove area of status-bar and navigation-bar
        val crop = Bitmap.createBitmap(
            bitmap,
            0,
            repository.capturedStatusBarHeight,
            repository.capturedScreenWidth,
            repository.capturedContentHeight
        )
        bitmap.recycle()
        return crop
    }

    /**
     * 現在の画面がイベント検索対象のゲーム画面か判定する
     */
    fun isGameScreen(img: Mat): Boolean = if (_initialized) {
        headerDetector.detect(img)
    } else throw IllegalStateException("not initialized")

    /**
     * イベントタイプを検出する
     */
    fun getEventType(img: Mat): EventType? = if (_initialized) {
        eventTypeDetector.detect(img)
    } else throw IllegalStateException("not initialized")

    /**
     * ゲームイベントの所有者を検出する
     *
     * **Note** Be sure to call after [getEventTitle] returns non null value, or an exception will be thrown
     */
    suspend fun getEventOwner(img: Mat, type: EventType) = if (_initialized) {
        when (type) {
            EventType.Scenario -> throw IllegalStateException("cannot to detect uma of the event owner, because current detected type is Scenario")
            EventType.SupportCard -> {
                val result = supportEventOwnerDetector.find(img)
                EventOwnerDetectResult(
                    result.data,
                    result.score,
                    result.img,
                )
            }
            EventType.Partner -> {
                val result = charaEventOwnerDetector.find(img)
                EventOwnerDetectResult(
                    result.data,
                    result.score,
                    result.img,
                )
            }
        }
    } else throw IllegalStateException("not initialized")

    /**
     * ゲーム画像からイベントタイトルを検出する
     */
    fun getEventTitle(img: Mat, type: EventType): Pair<Bitmap, String> {
        if (!_initialized) {
            throw IllegalStateException("not initialized")
        }
        val start = SystemClock.uptimeMillis()
        val target = eventTitleCropper.preProcess(img, type)
        val title = extractText(target)
        Log.d("OCR", "title: $title time: ${SystemClock.uptimeMillis() - start}ms")
        return target to title
    }

    private fun extractText(img: Bitmap): String {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        return text.replace(Regex("\\s+"), "")
    }
}
