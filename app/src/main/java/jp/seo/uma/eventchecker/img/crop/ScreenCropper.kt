package jp.seo.uma.eventchecker.img.crop

import org.opencv.core.Mat
import org.opencv.core.Rect

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