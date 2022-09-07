package jp.seo.uma.eventchecker.ui.inspector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.databinding.FragmentMonitorBinding

/**
 * 現在のゲーム画面監視の状態を表示する
 */
@AndroidEntryPoint
class MonitorFragment : Fragment() {

    private lateinit var binding: FragmentMonitorBinding

    private val viewModel: MonitorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMonitorBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.setUiState()
        binding.viewModel = viewModel
    }
}