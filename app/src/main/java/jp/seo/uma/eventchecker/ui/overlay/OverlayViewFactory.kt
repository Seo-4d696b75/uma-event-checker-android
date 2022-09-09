package jp.seo.uma.eventchecker.ui.overlay

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LifecycleOwner
import jp.seo.uma.eventchecker.databinding.OverlayMainBinding
import jp.seo.uma.eventchecker.repository.SearchRepository
import jp.seo.uma.eventchecker.repository.SettingRepository


fun Context.createOverlayView(
    searchRepository: SearchRepository,
    setting: SettingRepository,
    lifecycleOwner: LifecycleOwner,
): View {
    val binding = OverlayMainBinding.inflate(LayoutInflater.from(applicationContext))
    val viewModel = OverlayViewModel(searchRepository, setting)
    binding.viewModel = viewModel
    binding.lifecycleOwner = lifecycleOwner
    binding.includeEventChoice.listGameEvent.apply {
        divider = null
        dividerHeight = 0
    }
    return binding.root
}