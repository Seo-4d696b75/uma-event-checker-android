package jp.seo.uma.eventchecker.ui.inspector

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InspectorDialogLaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dialog = InspectorDialog()
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