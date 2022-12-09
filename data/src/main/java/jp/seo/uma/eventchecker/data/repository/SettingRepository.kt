package jp.seo.uma.eventchecker.data.repository

import android.view.WindowManager

/**
 * スクリーンキャプチャに関する設定値
 *
 * @author Seo-4d696b75
 * @version 2021/07/08.
 */
interface SettingRepository {

    /**
     * キャプチャした画像からイベントを検索する最短間隔 ms
     */
    val minUpdateInterval: Long

    fun setMetrics(windowManager: WindowManager)

    val capturedScreenWidth: Int
    val capturedScreenHeight: Int
    val capturedStatusBarHeight: Int
    val capturedContentHeight: Int
}
