package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.core.getBitmap
import jp.seo.uma.eventchecker.core.readFloat
import jp.seo.uma.eventchecker.core.toBitmap
import jp.seo.uma.eventchecker.core.toMat
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
    open fun crop(img: Bitmap): Mat {
        val src = img.toMat()
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

class EventTitleProcess(context: Context): ScreenCropper(
    context.resources.readFloat(R.dimen.ocr_title_sampling_x),
    context.resources.readFloat(R.dimen.ocr_title_sampling_y),
    context.resources.readFloat(R.dimen.ocr_title_sampling_width),
    context.resources.readFloat(R.dimen.ocr_title_sampling_height)
) {
    fun preProcess(img: Bitmap): Bitmap {
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

abstract class TemplateDetector(
    private val template: Mat,
    samplingX: Float,
    samplingY: Float,
    samplingW: Float,
    samplingH: Float,
    private val originW: Float,
    private val threshold: Float
) : ScreenCropper(
    samplingX, samplingY, samplingW, samplingH
) {

    open fun convertColor(src: Mat): Mat {
        return src
    }

    fun detect(img: Bitmap): Boolean {
        val width = img.width.toFloat()
        val crop = crop(img)
        val scale = originW / width
        val size = Size(
            round(crop.width() * scale).toDouble(),
            round(crop.height() * scale).toDouble()
        )
        val resized = Mat()
        Imgproc.resize(crop, resized, size)
        val result = Mat(
            resized.width() - template.width() + 1,
            resized.height() - template.height() + 1,
            CvType.CV_32FC1
        )
        Imgproc.matchTemplate(convertColor(resized), template, result, Imgproc.TM_CCOEFF_NORMED)
        val mm = Core.minMaxLoc(result)
        return mm.maxVal > threshold
    }
}

class GameHeaderDetector(context: Context) : TemplateDetector(
    context.assets.getBitmap("template/game_header.png").toMat(),
    context.resources.readFloat(R.dimen.template_game_header_sampling_x),
    context.resources.readFloat(R.dimen.template_game_header_sampling_y),
    context.resources.readFloat(R.dimen.template_game_header_sampling_width),
    context.resources.readFloat(R.dimen.template_game_header_sampling_height),
    context.resources.readFloat(R.dimen.template_game_header_origin),
    context.resources.readFloat(R.dimen.template_game_header_threshold)
)

class EventTypeDetector(template: Mat, context: Context) : TemplateDetector(
    template,
    context.resources.readFloat(R.dimen.template_event_type_sampling_x),
    context.resources.readFloat(R.dimen.template_event_type_sampling_y),
    context.resources.readFloat(R.dimen.template_event_type_sampling_width),
    context.resources.readFloat(R.dimen.template_event_type_sampling_height),
    context.resources.readFloat(R.dimen.template_event_type_origin),
    context.resources.readFloat(R.dimen.template_event_type_threshold)
) {
    override fun convertColor(src: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        val th = Mat()
        Imgproc.threshold(gray, th, 220.0, 255.0, Imgproc.THRESH_BINARY_INV)
        return th
    }
}
