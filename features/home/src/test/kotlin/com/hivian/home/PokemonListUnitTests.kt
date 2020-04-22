package com.hivian.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.*
import androidx.test.filters.SmallTest
import com.hivian.common.extension.toLiveData
import com.hivian.common_test.datasets.FakeData
import com.hivian.common_test.extensions.blockingObserve
import com.hivian.home.domain.PokemonListUseCase
import com.hivian.home.pokemon_list.FilterType
import com.hivian.home.pokemon_list.PokemonListViewEvent
import com.hivian.home.pokemon_list.PokemonListViewModel
import com.hivian.home.pokemon_list.PokemonListViewState
import com.hivian.model.domain.Pokemon
import com.hivian.repository.AppDispatchers
import com.hivian.repository.utils.NetworkWrapper
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@ExperimentalCoroutinesApi
@SmallTest
class PokemonListUnitTests {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var pokemonListUseCase: PokemonListUseCase
    private lateinit var pokemonListViewModel: PokemonListViewModel
    private val appDispatchers = AppDispatchers(Dispatchers.Unconfined, Dispatchers.Unconfined)

    @Before
    fun setUp() {
        pokemonListUseCase = mockk()
    }

    @Test
    fun `Pokemons requested when ViewModel is created`() {
        val observer = mockk<Observer<List<Pokemon>>>(relaxed = true)
        val filterObserver = mockk<Observer<FilterType>>(relaxed = true)
        val result = NetworkWrapper.Success(FakeData.createFakePokemonsDomain(3))
        coEvery { pokemonListUseCase.getAllPokemonRemote(false) } returns result
        coEvery { pokemonListUseCase.getAllPokemonFilter("") } returns result.value.toLiveData()

        pokemonListViewModel = PokemonListViewModel(pokemonListUseCase, appDispatchers)
        pokemonListViewModel.dataFilter.observeForever(filterObserver)
        pokemonListViewModel.data.observeForever(observer)

        verify {
            filterObserver.onChanged(FilterType.All())
            observer.onChanged(result.value)
        }

        confirmVerified(filterObserver, observer)
    }

    @Test
    fun `Pokemons requested but failed when ViewModel is created`() {
        val filterObserver = mockk<Observer<FilterType>>(relaxed = true)
        val observerState = mockk<Observer<PokemonListViewState>>(relaxed = true)
        val result = NetworkWrapper.NetworkError
        coEvery { pokemonListUseCase.getAllPokemonRemote(any()) } returns result

        pokemonListViewModel = PokemonListViewModel(pokemonListUseCase, appDispatchers)
        pokemonListViewModel.dataFilter.observeForever(filterObserver)
        pokemonListViewModel.state.observeForever(observerState)

        verifySequence {
            filterObserver.onChanged(FilterType.All(""))
            observerState.onChanged(PokemonListViewState.Error)
        }

        confirmVerified(filterObserver, observerState)
    }

    @Test
    fun `Pokemon clicks on item on RecyclerView`() {
        val fakeDataSet = FakeData.createFakePokemonsDomain(3)
        val event = PokemonListViewEvent.OpenPokemonDetailView(fakeDataSet.first().name)
        coEvery { pokemonListUseCase.getAllPokemonRemote(false) } returns NetworkWrapper.Success(fakeDataSet)

        pokemonListViewModel = PokemonListViewModel(pokemonListUseCase, appDispatchers)
        pokemonListViewModel.openPokemonDetail(fakeDataSet.first().name)

        Assert.assertEquals(event.name,
            (pokemonListViewModel.event.blockingObserve()!! as PokemonListViewEvent.OpenPokemonDetailView).name)
    }
}