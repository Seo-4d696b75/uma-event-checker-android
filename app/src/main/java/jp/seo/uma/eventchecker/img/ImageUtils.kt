package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.core.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.round

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */

open class ScreenCropper(
    private val samplingX: Float,
    private val samplingY: Float,
    private val samplingW: Float,
    private val samplingH: Float
) {
    open fun crop(src: Mat): Mat {
        val width = src.width().toFloat()
        val rect = Rect(
            (width * samplingX).toInt(),
            (width * samplingY).toInt(),
            (width * samplingW).toInt(),
            (width * samplingH).toInt()
        )
        return Mat(src, rect)
    }
}

class EventTitleProcess(context: Context) : ScreenCropper(
    context.resources.readFloat(R.dimen.ocr_title_sampling_x),
    context.resources.readFloat(R.dimen.ocr_title_sampling_y),
    context.resources.readFloat(R.dimen.ocr_title_sampling_width),
    context.resources.readFloat(R.dimen.ocr_title_sampling_height)
) {
    fun preProcess(img: Mat): Bitmap {
        val crop = crop(img)
        val gray = Mat()
        Imgproc.cvtColor(crop, gray, Imgproc.COLOR_BGR2GRAY)
        val size = Size(
            gray.width() * 2.0,
            gray.height() * 2.0
        )
        Imgproc.resize(gray, gray, size, 0.0, 0.0, Imgproc.INTER_CUBIC)
        Imgproc.threshold(gray, gray, 220.0, 255.0, Imgproc.THRESH_BINARY_INV)
        return gray.toBitmap()
    }
}

/**
 * @constructor
 *
 * @param originW テンプレート画像のスケールにおける全体画面の幅
 */
abstract class TemplateMatcher(
    samplingX: Float,
    samplingY: Float,
    samplingW: Float,
    samplingH: Float,
    private val originW: Float,
) : ScreenCropper(
    samplingX, samplingY, samplingW, samplingH
) {

    /**
     * Any operation at the last step of [preProcess]
     */
    open fun convertColor(src: Mat): Mat {
        return src
    }

    /**
     * Process src img before template matching
     */
    fun preProcess(src: Mat): Mat {
        val width = src.width().toFloat()
        val dst = crop(src)
        val scale = originW / width
        val size = Size(
            round(dst.width() * scale).toDouble(),
            round(dst.height() * scale).toDouble()
        )
        Imgproc.resize(dst, dst, size)
        return convertColor(dst)
    }

    /**
     * Run template matching and get score
     * @param src pre-processed src img
     * @param template
     * @return normalized value in range of [0,1]
     */
    fun match(src: Mat, template: Mat): Double {
        val result = Mat(
            src.width() - template.width() + 1,
            src.height() - template.height() + 1,
            CvType.CV_32FC1
        )
        Imgproc.matchTemplate(src, template, result, Imgproc.TM_CCOEFF_NORMED)
        val mm = Core.minMaxLoc(result)
        return mm.maxVal
    }
}

class GameHeaderDetector(context: Context) : TemplateMatcher(
    context.resources.readFloat(R.dimen.template_game_header_sampling_x),
    context.resources.readFloat(R.dimen.template_game_header_sampling_y),
    context.resources.readFloat(R.dimen.template_game_header_sampling_width),
    context.resources.readFloat(R.dimen.template_game_header_sampling_height),
    context.resources.readFloat(R.dimen.template_game_header_origin)
) {
    private val template = context.assets.getBitmap("template/game_header.png").toMat()
    private val threshold = context.resources.readFloat(R.dimen.template_game_header_threshold)

    fun detect(src: Mat): Boolean {
        val score = match(preProcess(src), template)
        return score > threshold
    }
}

enum class EventType {
    Main, Chara, Support
}

class EventTypeDetector(context: Context) : TemplateMatcher(
    context.resources.readFloat(R.dimen.template_event_type_sampling_x),
    context.resources.readFloat(R.dimen.template_event_type_sampling_y),
    context.resources.readFloat(R.dimen.template_event_type_sampling_width),
    context.resources.readFloat(R.dimen.template_event_type_sampling_height),
    context.resources.readFloat(R.dimen.template_event_type_origin),
) {

    private val threshold = context.resources.readFloat(R.dimen.template_event_type_threshold)
    private val templateChara = context.assets.getBitmap("template/event_chara.png").toGrayMat()
    private val templateSupport = context.assets.getBitmap("template/event_support.png").toGrayMat()
    private val templateMain = context.assets.getBitmap("template/event_main.png").toGrayMat()

    override fun convertColor(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        val th = Mat()
        Imgproc.threshold(gray, th, 220.0, 255.0, Imgproc.THRESH_BINARY_INV)
        return th
    }

    fun detect(src: Mat): EventType? {
        val img = preProcess(src)
        if (match(img, templateChara) > threshold) return EventType.Chara
        if (match(img, templateSupport) > threshold) return EventType.Support
        if (match(img, templateMain) > threshold) return EventType.Main
        return null
    }

}
