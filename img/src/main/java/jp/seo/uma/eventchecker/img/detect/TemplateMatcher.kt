package jp.seo.uma.eventchecker.img.detect

import jp.seo.uma.eventchecker.img.crop.ScreenCropper
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.round


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
