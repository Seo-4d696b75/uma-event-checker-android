package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.model.CharaEventOwner
import jp.seo.uma.eventchecker.model.EventOwners
import jp.seo.uma.eventchecker.model.SupportEventOwner
import jp.seo.uma.eventchecker.repository.mapParallel
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * @author Seo-4d696b75
 * @version 2021/07/08.
 */
class TemplatesMatcher<T>(
    samplingX: Float,
    samplingY: Float,
    samplingW: Float,
    samplingH: Float,
    private val templates: Array<TemplateHolder<T>>,
    originW: Float
) : TemplateMatcher(samplingX, samplingY, samplingW, samplingH, originW) {

    suspend fun find(src: Mat): TemplateResult<T> {
        val start = SystemClock.uptimeMillis()
        val target = preProcess(src)
        val results = templates.mapParallel(
            process = {
                val scores = it.images.map { template -> match(target, template) }
                val idx =
                    scores.indices.maxByOrNull { i -> scores[i] } ?: throw NoSuchElementException()
                TemplateResult(
                    it.data,
                    it.src[idx],
                    scores[idx],
                )
            },
        )
        val max = results.maxByOrNull { it.score } ?: throw NoSuchElementException()
        Log.d(
            "Templates",
            "score: ${max.score} name: ${max.data} time: ${SystemClock.uptimeMillis() - start}ms"
        )
        return max
    }
}

class TemplateHolder<T>(
    val data: T,
    val src: List<Bitmap>,
    width: Int
) {
    val images: Array<Mat> by lazy {
        Array(src.size) { idx ->
            val img = src[idx].toMat()
            val size = Size(
                width.toDouble(),
                img.height() * width.toDouble() / img.width()
            )
            Imgproc.resize(img, img, size)
            img
        }
    }
}

data class TemplateResult<T>(
    val data: T,
    val img: Bitmap,
    val score: Double,
)

fun getCharaEventOwnerDetector(
    context: Context,
    data: EventOwners
): TemplatesMatcher<CharaEventOwner> {
    val resizedWidth =
        context.resources.getInteger(R.integer.template_event_owner_chara_resized_width)
    val templates = data.charaEventOwners.map { owner ->
        val icons = owner.icon.map {
            readBitmap(context.filesDir, "icon/${it}")
        }.toList()
        TemplateHolder(owner, icons, resizedWidth)
    }
    return TemplatesMatcher(
        context.resources.readFloat(R.dimen.template_event_owner_chara_sampling_x),
        context.resources.readFloat(R.dimen.template_event_owner_chara_sampling_y),
        context.resources.readFloat(R.dimen.template_event_owner_chara_sampling_width),
        context.resources.readFloat(R.dimen.template_event_owner_chara_sampling_height),
        templates.toTypedArray(),
        resizedWidth / context.resources.readFloat(R.dimen.template_event_owner_chara_width)
    )
}

fun getSupportEventOwnerDetector(
    context: Context,
    data: EventOwners
): TemplatesMatcher<SupportEventOwner> {
    val resizedWidth =
        context.resources.getInteger(R.integer.template_event_owner_support_resized_width)
    val templates = data.supportEventOwners.map { owner ->
        val file = "icon/${owner.icon}"
        val icon = readBitmap(context.filesDir, file)
        TemplateHolder(owner, listOf(icon), resizedWidth)
    }
    return TemplatesMatcher(
        context.resources.readFloat(R.dimen.template_event_owner_support_sampling_x),
        context.resources.readFloat(R.dimen.template_event_owner_support_sampling_y),
        context.resources.readFloat(R.dimen.template_event_owner_support_sampling_width),
        context.resources.readFloat(R.dimen.template_event_owner_support_sampling_height),
        templates.toTypedArray(),
        resizedWidth / context.resources.readFloat(R.dimen.template_event_owner_support_width)
    )
}


