package jp.seo.uma.eventchecker.img.detect

import android.content.Context
import jp.seo.uma.eventchecker.img.R
import jp.seo.uma.eventchecker.img.getBitmap
import jp.seo.uma.eventchecker.img.readFloat
import jp.seo.uma.eventchecker.img.toGrayMat
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

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