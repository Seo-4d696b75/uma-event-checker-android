package jp.seo.uma.eventchecker.data.repository

import android.media.Image
import jp.seo.uma.eventchecker.data.model.GameEvent
import kotlinx.coroutines.flow.StateFlow

interface SearchRepository {
    val currentTitle: StateFlow<String?>
    val currentEvent: StateFlow<GameEvent?>
    fun update(img: Image)
}