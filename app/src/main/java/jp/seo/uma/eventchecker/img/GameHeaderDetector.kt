package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.core.getBitmap
import jp.seo.uma.eventchecker.core.readFloat
import jp.seo.uma.eventchecker.core.toMat
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.round

/**
 * @author Seo-4d696b75
 * @version 2021/07/03.
 */
abstract class TemplateDetector(
    private val template: Mat,
    private val samplingX: Float,
    private val samplingY: Float,
    private val samplingW: Float,
    private val samplingH: Float,
    private val originW: Float,
    private val threshold: Float
) {

    fun detect(img: Bitmap): Boolean {
        val src = img.toMat()
        val width = src.width().toFloat()
        val rect = Rect(
            (width * samplingX).toInt(),
            (width * samplingY).toInt(),
            (width * samplingW).toInt(),
            (width * samplingH).toInt()
        )
        val crop = Mat(src, rect)
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
        Imgproc.matchTemplate(resized, template, result, Imgproc.TM_CCOEFF_NORMED)
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
