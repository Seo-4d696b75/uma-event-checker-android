package jp.seo.uma.eventchecker.ui.inspector

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.databinding.FragmentSearchBinding
import jp.seo.uma.eventchecker.databinding.ListEventItemBinding
import jp.seo.uma.eventchecker.repository.SearchResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.setQueryAsDetectedString()
        binding.viewModel = viewModel
        binding.inputTextEventTitle.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                viewModel.setQuery(text.toString())
            }
        )
        val context = requireContext()
        val adapter = GamaEventAdapter(context)
        binding.listEventSearch.also {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL,
                )
            )
        }
        viewModel.events
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach { adapter.submitList(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private class SearchResultViewHolder(val binding: ListEventItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class SearchResultComparator : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.event.title == newItem.event.title
                    && oldItem.event.ownerName == newItem.event.ownerName
                    && oldItem.score == newItem.score
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }

    private class GamaEventAdapter(context: Context) :
        ListAdapter<SearchResult, SearchResultViewHolder>(
            SearchResultComparator()
        ) {
        private val inflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
            val binding = ListEventItemBinding.inflate(inflater)
            return SearchResultViewHolder(binding)
        }

        override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
            val result = getItem(position)
            holder.binding.result = result
        }

    }
}
