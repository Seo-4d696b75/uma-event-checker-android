package jp.seo.uma.eventchecker.ui.inspector

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class InspectorDialogLaunchActivity : AppCompatActivity() {

    private val viewModel: InspectorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspector)

        viewModel.dismiss
            .flowWithLifecycle(lifecycle)
            .filter { it }
            .onEach {
                Log.d("InspectorDialog", "closed")
                finish()
            }
            .launchIn(lifecycleScope)
    }
}