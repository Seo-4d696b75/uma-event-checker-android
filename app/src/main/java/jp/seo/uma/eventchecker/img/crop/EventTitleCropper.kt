package jp.seo.uma.eventchecker.img.crop

import android.content.Context
import android.graphics.Bitmap
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.img.readFloat
import jp.seo.uma.eventchecker.img.toBitmap
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class EventTitleCropper(context: Context) : ScreenCropper(
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