package jp.seo.uma.eventchecker.ui.inspector

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.databinding.DialogInspectorBinding

@AndroidEntryPoint
class InspectorDialog : DialogFragment() {

    private val viewModel: MonitorViewModel by viewModels()

    override fun onStart() {
        super.onStart()

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        viewModel.setUiState()

        val binding = DataBindingUtil.inflate<DialogInspectorBinding>(
            layoutInflater,
            R.layout.dialog_inspector,
            null,
            false
        ).also {
            it.inspectorViewpager.adapter = InspectorPageAdapter(this)
            TabLayoutMediator(it.inspectorTab, it.inspectorViewpager) { tab, position ->
                tab.text = context.getString(
                    when (position) {
                        0 -> R.string.inspector_tab_monitor
                        1 -> R.string.inspector_tab_search
                        else -> throw IndexOutOfBoundsException()
                    }
                )
            }.attach()
        }

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.inspector_dialog_title)
            setView(binding.root)
            setPositiveButton(R.string.inspector_dialog_close) { _, _ ->
                dismiss()
                viewModel.onDismiss()
                setFragmentResult(InspectorDialogLaunchActivity.FRAGMENT_CLOSE_KEY, bundleOf())
            }
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }
}

class InspectorPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 2

    override fun createFragment(position: Int) = when (position) {
        0 -> MonitorFragment()
        1 -> MonitorFragment()
        else -> throw IndexOutOfBoundsException()
    }
}