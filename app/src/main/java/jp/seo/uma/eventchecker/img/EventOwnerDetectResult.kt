package jp.seo.uma.eventchecker.img

import android.graphics.Bitmap
import jp.seo.uma.eventchecker.model.Uma

data class EventOwnerDetectResult(
    val uma: Uma,
    val score: Double,
    val img: Bitmap,
)