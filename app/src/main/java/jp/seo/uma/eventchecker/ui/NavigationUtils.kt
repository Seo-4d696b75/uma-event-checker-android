package jp.seo.uma.eventchecker.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavDirections

/**
 * 現在一番上で表示されているダイアログが閉じるのを待ってからnavigateする
 *
 * ダイアログは同一のNavControllerを使用して表示されていること
 * [Navigation ComponentでDialogを扱う時の問題点](https://star-zero.medium.com/navigation-component%E3%81%A7dialog%E3%82%92%E6%89%B1%E3%81%86%E3%81%A8%E3%81%8D%E3%81%AE%E6%B3%A8%E6%84%8F-f84f3f5cbb8f)
 *
 * @param directions 待機してからnavigateするdirection
 * @param dialogId 指定されたIDと一致するdestinationを持つBackStackEntryがStackの一番上にある場合のみ実行
 * @param lifecycle BackStackEntryを監視するlifecycle
 */
fun NavController.navigateWhenDialogClosed(
    directions: NavDirections,
    dialogId: Int,
    lifecycle: Lifecycle,
) {
    // 参考：https://developer.android.com/guide/navigation/navigation-programmatic#additional_considerations
    currentBackStackEntry?.let { top ->
        if (top.destination.id == dialogId) {
            previousBackStackEntry?.let { entry ->
                var run = false
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (!run) {
                            navigate(directions)
                        }
                        run = true
                    }
                }
                entry.lifecycle.addObserver(observer)
                lifecycle.addObserver(
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            entry.lifecycle.removeObserver(observer)
                        }
                    }
                )
            }
        }
    }
}