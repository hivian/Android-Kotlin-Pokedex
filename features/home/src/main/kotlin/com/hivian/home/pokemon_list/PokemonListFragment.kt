package com.hivian.home.pokemon_list

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import com.hivian.common.base.BaseFragment
import com.hivian.common.base.BaseViewEvent
import com.hivian.common.base.BaseViewModel
import com.hivian.common.base.BaseViewState
import com.hivian.common.extension.gridLayoutManager
import com.hivian.common.extension.hideKeyboard
import com.hivian.common.extension.observe
import com.hivian.common.extension.showCustomDialog
import com.hivian.home.R
import com.hivian.home.databinding.FragmentPokemonListBinding
import com.hivian.home.pokemon_list.views.adapter.PokemonListAdapter
import com.hivian.home.pokemon_list.views.adapter.PokemonListAdapterState
import com.hivian.model.domain.Pokemon
import org.koin.android.viewmodel.ext.android.viewModel


class PokemonListFragment : BaseFragment<FragmentPokemonListBinding, PokemonListViewModel> (
    layoutId = R.layout.fragment_pokemon_list
) {

    private val viewModel: PokemonListViewModel by viewModel()
    private val viewAdapter: PokemonListAdapter by lazy {
        PokemonListAdapter(viewModel)
    }
    private var menu: Menu? = null
    private lateinit var parentToolbar: Toolbar


    override fun onInitDataBinding() {
        viewBinding.viewModel = viewModel
        with(viewBinding.includeList.pokemonList) {
            adapter = viewAdapter
            gridLayoutManager?.run {
                spanSizeLookup = viewAdapter.getSpanSizeLookup()
            }
        }
    }

    override fun getViewModel(): BaseViewModel = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.loadPokemonsRemote()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        parentToolbar = requireCompatActivity().findViewById(R.id.toolbar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(viewModel.dataFilter, ::onFilterEvent)
        observe(viewModel.event, ::onViewEvent)
        observe(viewModel.data, ::onViewDataChange)
        observe(viewModel.state, ::onViewStateChange)
    }

    /**
     * Observer view event change on [FilterType].
     *
     * @param filterType Event on characters list.
     */
    private fun onFilterEvent(filterType: FilterType) {
        when (filterType) {
            is FilterType.All -> {
                setToolbarTitle(null)
                handleActionViewVisibility(filterType)
            }
            is FilterType.Favorite -> {
                setToolbarTitle(R.string.pokemon_list_favorite_title)
                handleActionViewVisibility(filterType)
                hideKeyboard()
            }
            is FilterType.Caught -> {
                setToolbarTitle(R.string.pokemon_list_caught_title)
                handleActionViewVisibility(filterType)
                hideKeyboard()
            }
        }
    }

    /**
     * Observer view data change on [PokemonListViewModel].
     *
     * @param viewData Paged list of characters.
     */
    private fun onViewDataChange(viewData: List<Pokemon>) {
        viewAdapter.submitList(viewData)
    }

    /**
     * Observer view state change on [PokemonListViewModel].
     *
     * @param viewState State of characters list.
     */
    private fun onViewStateChange(viewState: BaseViewState) {
        when (viewState) {
            is PokemonListViewState.Loaded ->
                viewAdapter.submitState(PokemonListAdapterState.Added)
            is PokemonListViewState.AddLoading ->
                viewAdapter.submitState(PokemonListAdapterState.AddLoading)
            is PokemonListViewState.AddError ->
                viewAdapter.submitState(PokemonListAdapterState.AddError)
            is PokemonListViewState.NoMoreElements ->
                viewAdapter.submitState(PokemonListAdapterState.NoMore)
        }
    }

    /**
     * Observer view event change on [PokemonListViewModel].
     *
     * @param viewEvent Event on characters list.
     */
    private fun onViewEvent(viewEvent: BaseViewEvent) {
        when (viewEvent) {
            is PokemonListViewEvent.OpenPokemonDetailView -> {
                hideKeyboard()
                findNavController().navigate(
                    PokemonListFragmentDirections.actionPokemonListFragmentToPokemonDetailFragment(
                        viewEvent.name
                    )
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_pokemon_list, menu)

        this.menu = menu
        configureSearchView(menu)
        handleActionViewVisibility(viewModel.dataFilter.value)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        updateItemOptionsRefresh(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_display_all -> {
            item.isChecked = !item.isChecked
            viewModel.loadAllPokemons()
            true
        }
        R.id.action_display_favorites -> {
            item.isChecked = !item.isChecked
            viewModel.loadPokemonFavorites()
            true
        }
        R.id.action_display_caught -> {
            item.isChecked = !item.isChecked
            viewModel.loadPokemonCaught()
            true
        }
        R.id.action_jump_to_top -> {
            viewAdapter.scrollTo(0)
            true
        }
        R.id.action_refresh -> {
            showCustomDialog(
                title = R.string.dialog_title,
                message = R.string.dialog_description) {
                    viewModel.forceRefreshItems()
            }
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun updateItemOptionsRefresh(menu: Menu) {
        when (viewModel.dataFilter.value) {
            is FilterType.All -> menu.findItem(R.id.action_display_all).isChecked = true
            is FilterType.Favorite -> menu.findItem(R.id.action_display_favorites).isChecked = true
            is FilterType.Caught -> menu.findItem(R.id.action_display_caught).isChecked = true
        }
    }

    private fun configureSearchView(menu: Menu) {
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.queryHint = getString(R.string.pokemon_list_search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(query: String?): Boolean {
                query?.let { viewModel.loadPokemonListByPattern(it) }
                return true
            }
        })
    }

    private fun setToolbarTitle(@StringRes title: Int?) {
        title?.run {
            parentToolbar.setTitle(this)
        } ?: run {
            parentToolbar.title = ""
        }
    }

    private fun handleActionViewVisibility(filterType: FilterType?) {
        val searchItem = parentToolbar.menu.findItem(R.id.action_search)
        searchItem?.let { search ->
            when (filterType) {
                is FilterType.All -> search.isVisible = true
                is FilterType.Favorite, FilterType.Caught -> {
                    search.isVisible = false
                    search.collapseActionView()
                }
                else -> {}
            }
        }
    }

}