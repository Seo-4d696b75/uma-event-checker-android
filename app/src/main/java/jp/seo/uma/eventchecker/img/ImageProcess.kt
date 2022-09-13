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
import kotlinx.coroutines.flow.update
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

    private val _title = MutableStateFlow<String?>(null)

    /**
     * 検出したイベントタイトル文字列
     */
    val title: StateFlow<String?> = _title

    private val _textImage = MutableStateFlow<Bitmap?>(null)

    /**
     * イベントタイトルを検出した元画像（白黒変換済み）
     */
    val textImage: StateFlow<Bitmap?> = _textImage

    private val _eventType = MutableStateFlow<EventType?>(null)

    /**
     * 検出されたイベントタイプ
     */
    val currentEventType: StateFlow<EventType?> = _eventType

    private val _isGameScreen = MutableStateFlow<Boolean>(false)

    /**
     * ゲーム画面の検出結果
     */
    val isGameScreen: StateFlow<Boolean> = _isGameScreen

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

    private var eventType: EventType? = null

    /**
     * Gets event title from the screen image
     *
     * @param img bitmap of the screen without status-bar nor navigation-bar
     */
    fun getEventTitle(img: Mat): Pair<EventType, String>? {
        if (!_initialized) return null
        val isGame = headerDetector.detect(img)
        _isGameScreen.update { isGame }
        Log.d("Img", "check is-target $isGame")
        if (isGame) {
            val type = eventTypeDetector.detect(img)
            Log.d("Img", "event type '${type.toString()}'")
            if (type != null) {
                val title = extractEventTitle(img, type)
                _title.update { title }
                _eventType.update { type }
                eventType = type
                return type to title
            }
        }
        eventType = null
        _textImage.update { null }
        _title.update { null }
        _eventType.update { null }
        return null
    }

    /**
     * Gets what type event is now shown on the screen
     *
     * **Note** Be sure to call after [getEventTitle] returns non null value, or an exception will be thrown
     */
    suspend fun getEventOwner(img: Mat): EventOwnerDetectResult {
        return when (eventType) {
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
            null -> throw IllegalStateException("event type not found")
        }
    }

    private fun extractEventTitle(img: Mat, type: EventType): String {
        val start = SystemClock.uptimeMillis()
        val target = eventTitleCropper.preProcess(img, type)
        _textImage.update { target }
        val title = extractText(target)
        Log.d("OCR", "title: $title time: ${SystemClock.uptimeMillis() - start}ms")
        return title
    }

    private fun extractText(img: Bitmap): String {
        ocrApi.setImage(img)
        val text = ocrApi.utF8Text
        return text.replace(Regex("\\s+"), "")
    }


}
