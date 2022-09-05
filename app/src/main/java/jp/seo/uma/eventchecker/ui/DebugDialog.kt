package jp.seo.uma.eventchecker.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.databinding.DialogDebugBinding

@AndroidEntryPoint
class DebugDialog : DialogFragment() {

    private val viewModel: DebugDialogViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        viewModel.setUiState()

        val binding = DataBindingUtil.inflate<DialogDebugBinding>(
            layoutInflater,
            R.layout.dialog_debug,
            null,
            false
        ).also {
            it.viewModel = viewModel
        }

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.debug_dialog_title)
            setView(binding.root)
            setPositiveButton(R.string.debug_dialog_close) { _, _ ->
                dismiss()
                viewModel.onDismiss()
                setFragmentResult(DebugDialogLaunchActivity.FRAGMENT_CLOSE_KEY, bundleOf())
            }
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }
}