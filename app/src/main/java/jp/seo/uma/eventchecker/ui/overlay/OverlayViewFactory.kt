package jp.seo.uma.eventchecker.ui.overlay

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LifecycleOwner
import jp.seo.uma.eventchecker.core.DataRepository
import jp.seo.uma.eventchecker.core.SettingRepository
import jp.seo.uma.eventchecker.databinding.OverlayMainBinding


fun Context.createOverlayView(
    data: DataRepository,
    setting: SettingRepository,
    lifecycleOwner: LifecycleOwner,
): View {
    val binding = OverlayMainBinding.inflate(LayoutInflater.from(applicationContext))
    val viewModel = OverlayViewModel(data, setting)
    binding.viewModel = viewModel
    binding.lifecycleOwner = lifecycleOwner
    binding.listOverlayChoices.apply {
        divider = null
        dividerHeight = 0
    }
    return binding.root
}