package jp.seo.uma.eventchecker.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DebugDialogLaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dialog = DebugDialog()
        dialog.show(supportFragmentManager, null)

        supportFragmentManager.setFragmentResultListener(FRAGMENT_CLOSE_KEY, this) { _, _ ->
            Log.d("DebugDialog", "closed")
            finish()
        }
    }

    companion object {
        const val FRAGMENT_CLOSE_KEY = "onDebugDialogClosed"
    }
}