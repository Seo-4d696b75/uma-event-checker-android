package jp.seo.uma.eventchecker.img

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.core.getBitmap
import jp.seo.uma.eventchecker.core.readFloat
import jp.seo.uma.eventchecker.core.toMat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * @author Seo-4d696b75
 * @version 2021/07/08.
 */
class TemplatesMatcher(
    samplingX: Float,
    samplingY: Float,
    samplingW: Float,
    samplingH: Float,
    private val templates: Array<TemplateHolder>,
    originW: Float
) : TemplateMatcher(samplingX, samplingY, samplingW, samplingH, originW) {

    suspend fun find(src: Mat): TemplateHolder {
        val start = SystemClock.uptimeMillis()
        val target = preProcess(src)
        val scores = Array<Double>(templates.size) { 0.0 }
        calcMatchScore(0, scores.size, target, scores)
        val idx = scores.indices.maxByOrNull { scores[it] } ?: throw NoSuchElementException()
        val template = templates[idx]
        val score = scores[idx]
        Log.d(
            "Templates",
            "score: $score name: ${template.name} time: ${SystemClock.uptimeMillis() - start}ms"
        )
        return template
    }

    private suspend fun calcMatchScore(
        start: Int,
        end: Int,
        target: Mat,
        dst: Array<Double>
    ): Unit = withContext(Dispatchers.IO) {
        if (start + 8 < end) {
            val mid = start + (end - start) / 2
            val left = async { calcMatchScore(start, mid, target, dst) }
            val right = async { calcMatchScore(mid, end, target, dst) }
            left.await()
            right.await()
        } else {
            for (idx in start until end) {
                val scores = templates[idx].images.map { template ->
                    match(target, template)
                }
                dst[idx] = scores.maxOrNull() ?: 0.0
            }
        }
    }
}

class TemplateHolder(
    val name: String,
    src: List<Bitmap>,
    width: Int
) {
    val images: Array<Mat> = Array(src.size) { idx ->
        val img = src[idx].toMat()
        val size = Size(
            width.toDouble(),
            img.height() * width.toDouble() / img.width()
        )
        Imgproc.resize(img, img, size)
        img
    }

}

fun getCharaEventOwnerDetector(context: Context, data: EventOwners): TemplatesMatcher {
    val manager = context.resources.assets
    val resizedWidth =
        context.resources.getInteger(R.integer.template_event_owner_chara_resized_width)
    val files = manager.list("icon") ?: throw RuntimeException("fail to grab icon template files")
    val templates = data.charaEventOwners.map { owner ->
        val icon1 = manager.getBitmap("icon/${owner.icon}")
        val idx = owner.icon.lastIndexOf(".")
        val name = owner.icon.substring(0, idx)
        val extension = owner.icon.substring(idx, owner.icon.length)
        val file = "${name}_e${extension}"
        if (files.contains(file)) {
            val icon2 = manager.getBitmap("icon/$file")
            TemplateHolder(owner.name, listOf(icon1, icon2), resizedWidth)

        } else {
            TemplateHolder(owner.name, listOf(icon1), resizedWidth)
        }
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

fun getSupportEventOwnerDetector(context: Context, data: EventOwners): TemplatesMatcher {
    val manager = context.resources.assets
    val resizedWidth =
        context.resources.getInteger(R.integer.template_event_owner_support_resized_width)
    val templates = data.supportEventOwners.map { owner ->
        val file = "icon/${owner.icon}"
        val icon = manager.getBitmap(file)
        TemplateHolder(owner.name, listOf(icon), resizedWidth)
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

fun loadEventOwners(context: Context): EventOwners {
    val manager = context.resources.assets
    return manager.open("icon.json").use { reader ->
        val str = reader.readBytes().toString(Charsets.UTF_8)
        Json { ignoreUnknownKeys = true }.decodeFromString(str)
    }
}

@Serializable
data class SupportEventOwner(
    @SerialName("n")
    val name: String,
    @SerialName("l")
    val type: String,
    @SerialName("i")
    val icon: String
)

@Serializable
data class CharaEventOwner(
    @SerialName("n")
    val name: String,
    @SerialName("i")
    val icon: String
)

@Serializable
class EventOwners(
    @SerialName("chara")
    val charaEventOwners: Array<CharaEventOwner>,
    @SerialName("support")
    val supportEventOwners: Array<SupportEventOwner>
)
