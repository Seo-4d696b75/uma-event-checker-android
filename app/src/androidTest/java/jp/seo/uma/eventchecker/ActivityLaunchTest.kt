package jp.seo.uma.eventchecker

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import jp.seo.uma.eventchecker.core.DataRepository
import jp.seo.uma.eventchecker.core.EventDataInfo
import jp.seo.uma.eventchecker.core.NetworkClient
import jp.seo.uma.eventchecker.ui.MainActivity
import kotlinx.coroutines.delay
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.io.IOException

/**
 * @author Seo-4d696b75
 * @version 2021/08/09.
 */
@HiltAndroidTest
class ActivityLaunchTest {

    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val rule: RuleChain = RuleChain.outerRule(HiltAndroidRule(this))
        .around(activityRule)

    @BindValue
    val network: NetworkClient = mockk()

    private val _repository: DataRepository

    @BindValue
    val repository: DataRepository

    private lateinit var decor: View

    init {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        _repository = DataRepository(context, network)
        repository = spyk(_repository)
        coEvery { network.getDataInfo() } coAnswers {
            delay(1000L)
            EventDataInfo(20990101L, 1024L)
        }
        coEvery { repository.checkUpdate() } coAnswers {
            _repository.clearData()
            _repository.checkUpdate()
        }
    }

    @Before
    fun setup() {
        activityRule.scenario.onActivity {
            decor = it.window.decorView
        }
    }

    @Test
    fun testLaunch_cancelUpdate() {
        // wait for dialog show
        waitForDialogWithView(withText(R.string.data_update_title))
        // click dialog 'negative' button
        onView(withText(R.string.data_update_dialog_button_negative)).perform(click())
        // check app fin
        Thread.sleep(1000L)
        assertThat(activityRule.scenario.state, Matchers.`is`(Lifecycle.State.DESTROYED))
        // verify
        coVerify(exactly = 1) {
            network.getDataInfo()
        }
        confirmVerified(network)
    }

    @Test
    fun testLaunch_updateFailure() {
        coEvery { network.getData(any()) } coAnswers {
            delay(100L)
            throw IOException("test")
        }
        // wait for dialog show
        waitForDialogWithView(withText(R.string.data_update_title))
        // click dialog 'negative' button
        onView(withText(R.string.data_update_dialog_button_positive)).perform(click())
        // check app fin
        Thread.sleep(1000L)
        assertThat(activityRule.scenario.state, Matchers.`is`(Lifecycle.State.DESTROYED))
        // verify
        coVerify(exactly = 1) {
            network.getDataInfo()
            network.getData(any())
        }
        confirmVerified(network)
    }

    @Test
    fun testLaunch_updateSuccess() {
        val statusCallbackSlot = slot<(String) -> Unit>()
        val progressCallbackSlot = slot<(Int) -> Unit>()
        coEvery {
            repository.updateData(
                any(),
                statusCallback = capture(statusCallbackSlot),
                progressCallback = capture(progressCallbackSlot)
            )
        } coAnswers {
            statusCallbackSlot.captured.invoke("test-message")
            progressCallbackSlot.captured.invoke(50)
            delay(1000L)
        }
        // wait for dialog show
        waitForDialogWithView(withText(R.string.data_update_title))
        // click dialog 'positive' button
        onView(withText(R.string.data_update_dialog_button_positive)).perform(click())
        // wait for progress dialog show
        waitForDialogWithView(withId(R.id.text_data_update_status))
        // check progress UI on dialog
        onView(withId(R.id.text_data_update_status))
            .check(ViewAssertions.matches(withText("test-message")))
        onView(withId(R.id.text_data_update_progress))
            .check(ViewAssertions.matches(withText(Matchers.containsString("50"))))
        // wait
        waitWhileDialogWithView(withId(R.id.text_data_update_status), 4000L)
        onView(withId(R.id.progress_main)).perform(waitWhileDisplayed(100L))
        // check UI on activity
        onView(withId(R.id.button_start))
            .check(ViewAssertions.matches(isEnabled()))
            .check(ViewAssertions.matches(withText(R.string.button_start)))
        onView(withId(R.id.text_main))
            .check(ViewAssertions.matches(withText(R.string.message_main_idle)))
        // verify
        coVerify(exactly = 1) {
            repository.checkUpdate()
            repository.updateData(any(), any(), any())
            repository.currentEvent
            repository.eventOwners
        }
        confirmVerified(repository)
    }

}
