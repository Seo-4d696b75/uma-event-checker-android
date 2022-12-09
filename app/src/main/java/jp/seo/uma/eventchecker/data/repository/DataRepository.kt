package jp.seo.uma.eventchecker.data.repository

import android.content.Context
import jp.seo.uma.eventchecker.data.model.GameEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * ゲーム中のイベント情報を管理・検索
 *
 * @author Seo-4d696b75
 * @version 2021/07/04.
 */
interface DataRepository {
    val initialized: StateFlow<Boolean>
    suspend fun init(context: Context)
    fun searchEvent(title: String): GameEvent?
}
