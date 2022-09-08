package jp.seo.uma.eventchecker.img

import android.graphics.Bitmap
import jp.seo.uma.eventchecker.model.CharaEventOwner
import jp.seo.uma.eventchecker.model.SupportEventOwner

sealed interface EventOwnerDetectResult {
    val name: String
    val score: Double
    val img: Bitmap

    data class Support(
        val uma: SupportEventOwner,
        override val score: Double,
        override val img: Bitmap,
    ) : EventOwnerDetectResult {
        override val name = uma.name
    }

    data class Chara(
        val uma: CharaEventOwner,
        override val score: Double,
        override val img: Bitmap,
    ) : EventOwnerDetectResult {
        override val name = uma.name
    }
}