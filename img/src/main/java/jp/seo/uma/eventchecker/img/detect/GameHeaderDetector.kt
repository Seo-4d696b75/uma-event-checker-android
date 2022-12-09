package jp.seo.uma.eventchecker.img.detect

import android.content.Context
import jp.seo.uma.eventchecker.img.R
import jp.seo.uma.eventchecker.img.getBitmap
import jp.seo.uma.eventchecker.img.readFloat
import jp.seo.uma.eventchecker.img.toMat
import org.opencv.core.Mat

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