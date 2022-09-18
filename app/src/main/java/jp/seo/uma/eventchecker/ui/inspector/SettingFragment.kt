package jp.seo.uma.eventchecker.ui.inspector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.databinding.FragmentSettingBinding

@AndroidEntryPoint
class SettingFragment : Fragment() {

    private val viewModel: SettingViewModel by viewModels()

    private lateinit var binding: FragmentSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.sliderSettingInterval.also {
            it.value = viewModel.minInterval.value.toFloat()
            it.setLabelFormatter {
                val value = it.toLong()
                requireContext().getString(R.string.setting_interval_format, value)
            }
            it.addOnChangeListener { _, value, _ ->
                viewModel.setMinInterval(value.toLong())
            }
        }
        binding.sliderSettingThreshold.also {
            it.value = viewModel.ocrThreshold.value
            it.addOnChangeListener { _, value, _ ->
                viewModel.setOcrThreshold(value)
            }
        }
    }
}