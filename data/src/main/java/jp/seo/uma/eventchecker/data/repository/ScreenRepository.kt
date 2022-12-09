package jp.seo.uma.eventchecker.data.repository

import android.media.Image
import android.media.projection.MediaProjection
import kotlinx.coroutines.flow.StateFlow

/**
 * スクリーン画像をキャプチャする
 */
interface ScreenRepository {
    val running: StateFlow<Boolean>
    fun setCallback(callback: ((Image) -> Unit)?)
    fun start(projection: MediaProjection)
    fun stop()
}