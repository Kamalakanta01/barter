package dev.barter.feature.myitems

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.barter.core.domain.testing.FakeBarterRepository
import dev.barter.feature.myitems.ui.MyItemsUiState
import dev.barter.feature.myitems.ui.MyListing
import dev.barter.feature.myitems.ui.MyItemsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyItemsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBarterRepository
    private lateinit var viewModel: MyItemsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBarterRepository()
        viewModel = MyItemsViewModel(repository, repository.me, SavedStateHandle())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val keyboard = MyListing(101, "Mechanical keyboard", "Plants", isTraded = false)
    private val bike = MyListing(102, "Road bike", "Camera gear", isTraded = false)

    @Test
    fun `lists only my own listings`() = runTest(dispatcher) {
        viewModel.uiState.test {
            assertEquals(listOf("Mechanical keyboard", "Road bike"), awaitLoaded().listings.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming the add dialog lists the item and closes the dialog`() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.onAddClicked()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isAddDialogVisible)

            viewModel.onAddConfirmed(title = "Drill", wants = "A lamp")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isAddDialogVisible)
            assertEquals("Drill", state.listings.last().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removing a listing drops it and any offer that used it`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitLoaded()

            viewModel.onRemove(state.listings.first())
            viewModel.onRemoveConfirmed()
            advanceUntilIdle()

            assertEquals(listOf("Road bike"), viewModel.uiState.value.listings.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a listing counts the pending offers that involve it`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitLoaded()

            assertEquals(listOf(1, 0), state.listings.map { it.pendingOffers })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removing a listing with a pending offer asks first and removes nothing yet`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val keyboardRow = awaitLoaded().listings.first()

            viewModel.onRemove(keyboardRow)

            assertEquals(keyboardRow, awaitItem().removalToConfirm)
            assertEquals(2, repository.state.value.myListings(repository.me).size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissing the warning keeps the listing and its offer`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val keyboardRow = awaitLoaded().listings.first()
            viewModel.onRemove(keyboardRow)
            awaitItem()

            viewModel.onRemoveDismissed()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.removalToConfirm)
            assertEquals(2, repository.state.value.myListings(repository.me).size)
            assertEquals(1, repository.state.value.offers.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a listing with no pending offers is removed without asking`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val bikeRow = awaitLoaded().listings.last()

            viewModel.onRemove(bikeRow)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.removalToConfirm)
            assertEquals(listOf("Mechanical keyboard"), viewModel.uiState.value.listings.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removing a listing reports which one so it can be undone`() = runTest(dispatcher) {
        viewModel.removedListings.test {
            viewModel.onRemove(keyboard)

            assertEquals("Mechanical keyboard", awaitItem().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo puts the listing back under its original id`() = runTest(dispatcher) {
        val original = repository.state.value.listing(101)!!
        viewModel.onRemove(keyboard)
        advanceUntilIdle()
        assertEquals(1, repository.state.value.myListings(repository.me).size)

        viewModel.onUndoRemove(original)
        advanceUntilIdle()

        val restored = repository.state.value.myListings(repository.me)
        assertEquals(listOf("Mechanical keyboard", "Road bike"), restored.map { it.title })
        assertEquals(101L, restored.first().id)
    }

    @Test
    fun `undo restores the listing it was offered for, not the latest removal`() = runTest(dispatcher) {
        val firstRemoved = repository.state.value.listing(101)!!
        viewModel.onRemove(keyboard)
        viewModel.onRemove(bike)
        advanceUntilIdle()

        viewModel.onUndoRemove(firstRemoved)
        advanceUntilIdle()

        assertEquals(listOf(101L), repository.state.value.myListings(repository.me).map { it.id })
    }

    @Test
    fun `the add dialog reopens after process death because the flag is saved`() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        MyItemsViewModel(repository, repository.me, savedState).onAddClicked()
        advanceUntilIdle()

        val afterRestart = MyItemsViewModel(repository, repository.me, savedState)
        afterRestart.uiState.test {
            assertTrue(awaitLoaded().isAddDialogVisible)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun ReceiveTurbine<MyItemsUiState>.awaitLoaded(): MyItemsUiState {
    assertEquals(MyItemsUiState(), awaitItem())
    return awaitItem()
}
