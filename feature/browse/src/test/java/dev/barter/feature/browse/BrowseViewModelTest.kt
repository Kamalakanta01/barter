package dev.barter.feature.browse

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.barter.core.domain.testing.FakeBarterRepository
import dev.barter.feature.browse.ui.BrowseUiState
import dev.barter.feature.browse.ui.BrowseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBarterRepository
    private lateinit var viewModel: BrowseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBarterRepository()
        viewModel = BrowseViewModel(repository, repository.me)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `browse lists other traders' listings with their names`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitLoaded()

            assertEquals(listOf("Film camera", "Ceramic mug set"), state.listings.map { it.title })
            assertEquals(listOf("Ana", "Mei"), state.listings.map { it.ownerName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search matches title and wants, case insensitively`() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.onQueryChange("HOUSEPLANTS")
            assertEquals(listOf("Ceramic mug set"), awaitItem().listings.map { it.title })

            viewModel.onQueryChange("bike")
            assertEquals(listOf("Film camera"), awaitItem().listings.map { it.title })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a search with no matches yields an empty list rather than everything`() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.onQueryChange("zzz")

            assertEquals(emptyList<String>(), awaitItem().listings.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `offerable lists only my untraded listings`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitLoaded()

            assertEquals(listOf("Mechanical keyboard", "Road bike"), state.offerable.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clicking a listing opens the swap dialog and dismissing closes it`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitLoaded()

            viewModel.onListingClick(state.listings.first())
            assertEquals("Film camera", awaitItem().swapTarget?.title)

            viewModel.onSwapDismissed()
            assertNull(awaitItem().swapTarget)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sending an offer records it and closes the dialog`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitLoaded()
            viewModel.onListingClick(state.listings.first())
            awaitItem() // dialog open

            viewModel.onSendOffer(mineListingId = 101)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.swapTarget)
            val offer = repository.state.value.offers.last()
            assertEquals(101L, offer.mineListingId)
            assertEquals(1L, offer.theirsListingId)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * `stateIn` replays its placeholder before the repository has produced anything, so the first item
 * is always the empty default.
 */
private suspend fun ReceiveTurbine<BrowseUiState>.awaitLoaded(): BrowseUiState {
    assertEquals(BrowseUiState(), awaitItem())
    return awaitItem()
}
