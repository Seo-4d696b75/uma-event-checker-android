package jp.seo.uma.eventchecker.img

import android.graphics.Bitmap
import jp.seo.uma.eventchecker.model.Partner
import jp.seo.uma.eventchecker.model.SupportCard

sealed interface EventOwnerDetectResult {
    val name: String
    val score: Double
    val img: Bitmap

    data class Support(
        val uma: SupportCard,
        override val score: Double,
        override val img: Bitmap,
    ) : EventOwnerDetectResult {
        override val name = uma.name
    }

    data class Chara(
        val uma: Partner,
        override val score: Double,
        override val img: Bitmap,
    ) : EventOwnerDetectResult {
        override val name = uma.name
    }
}