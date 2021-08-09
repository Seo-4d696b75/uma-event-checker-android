package jp.seo.uma.eventchecker

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * https://developer.android.com/training/dependency-injection/hilt-testing?hl=ja#instrumented-tests
 * @author Seo-4d696b75
 * @version 2021/08/09.
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
