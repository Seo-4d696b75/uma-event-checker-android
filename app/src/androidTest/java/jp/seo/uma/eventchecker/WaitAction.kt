package jp.seo.uma.eventchecker

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.util.HumanReadables
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.core.IsAnything
import org.hamcrest.core.IsNot
import java.util.concurrent.TimeoutException

/**
 * あるViewが指定された条件を満たすまで待機するようなViewAction
 *
 * `timeout`を超える場合は例外を投げる
 * [参考](https://qiita.com/mechamogera/items/c6f2999f7e9c906b483c)
 */
class WaitAction(
    private val resumePredicate: Matcher<View>,
    private val timeout: Long
) : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return IsAnything()
    }

    override fun getDescription(): String {
        return "wait(timeout=$timeout ms)"
    }

    override fun perform(uiController: UiController, view: View) {
        uiController.loopMainThreadUntilIdle()
        val start = SystemClock.uptimeMillis()
        val limit = start + timeout
        do {
            if (resumePredicate.matches(view)) return
            uiController.loopMainThreadForAtLeast(100L)
        } while (SystemClock.uptimeMillis() < limit)
        throw PerformException.Builder()
            .withActionDescription(this.description)
            .withViewDescription(HumanReadables.describe(view))
            .withCause(TimeoutException())
            .build()
    }
}

/**
 * Viewが表示されている間は待って欲しい時に使う
 */
fun waitWhileDisplayed(timeout: Long = 5000L) = WaitAction(
    IsNot(ViewMatchers.isDisplayed()),
    timeout
)

/**
 * Viewが表示されるまで待機する
 */
fun waitUntilDisplayed(timeout: Long = 5000L) = WaitAction(
    ViewMatchers.isDisplayed(),
    timeout
)

fun waitForDialogWithView(viewMatcher: Matcher<View>, timeout: Long = 5000L) = runBlocking {
    val limit = System.currentTimeMillis() + timeout
    while (System.currentTimeMillis() < limit) {
        delay(100L)
        try {
            onView(viewMatcher)
                .inRoot(isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            return@runBlocking
        } catch (e: RuntimeException) {

        }
    }
    throw RuntimeException("dialog not found")
}

fun waitWhileDialogWithView(viewMatcher: Matcher<View>, timeout: Long = 5000L) = runBlocking {
    val limit = System.currentTimeMillis() + timeout
    while (System.currentTimeMillis() < limit) {
        delay(100L)
        try {
            onView(viewMatcher)
                .inRoot(isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        } catch (e: RuntimeException) {
            return@runBlocking
        }
    }
    throw RuntimeException("dialog still displayed")
}
