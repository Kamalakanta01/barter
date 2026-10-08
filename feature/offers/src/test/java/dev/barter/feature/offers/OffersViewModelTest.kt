package dev.barter.feature.offers

import app.cash.turbine.test
import dev.barter.core.domain.testing.FakeBarterRepository
import dev.barter.feature.offers.ui.OfferNote
import dev.barter.feature.offers.ui.OfferOutcome
import dev.barter.feature.offers.ui.OffersViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OffersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBarterRepository
    private lateinit var viewModel: OffersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBarterRepository()
        viewModel = OffersViewModel(repository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an unanswered incoming offer names who sent it and can be responded to`() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitLoaded()

            val row = viewModel.uiState.value.incoming.single()
            assertEquals("Mechanical keyboard", row.mineTitle)
            assertEquals("Ceramic mug set", row.theirsTitle)
            assertEquals("Mei", row.theirsOwnerName)
            assertEquals(OfferNote.SENT_TO_YOU, row.note)
            assertTrue(row.canRespond)
            assertFalse(row.canWithdraw)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sending an offer adds it to the outgoing group awaiting a reply`() = runTest(dispatcher) {
        repository.sendOffer(mineListingId = 102, theirsListingId = 1)

        viewModel.uiState.test {
            awaitLoaded()

            val row = viewModel.uiState.value.outgoing.single()
            assertEquals("Ana", row.theirsOwnerName)
            assertEquals(OfferNote.WAITING_ON_THEM, row.note)
            assertTrue(row.canWithdraw)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `accepting marks the offer swapped and stops it being answerable`() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.onAccept(201)
            advanceUntilIdle()

            val row = viewModel.uiState.value.incoming.single()
            assertEquals(OfferNote.SWAPPED, row.note)
            assertFalse(row.canRespond)
            assertTrue(repository.state.value.listing(101)!!.isTraded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `declining leaves both listings available`() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.onDecline(201)
            advanceUntilIdle()

            assertEquals(OfferNote.DECLINED, viewModel.uiState.value.incoming.single().note)
            assertFalse(repository.state.value.listing(101)!!.isTraded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `withdrawing removes the outgoing offer entirely`() = runTest(dispatcher) {
        repository.sendOffer(mineListingId = 102, theirsListingId = 1)

        viewModel.uiState.test {
            awaitLoaded()
            val outgoingId = viewModel.uiState.value.outgoing.single().id

            viewModel.onWithdraw(outgoingId)
            advanceUntilIdle()

            assertEquals(1, repository.state.value.offers.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `answering an offer reports the outcome`() = runTest(dispatcher) {
        viewModel.offerOutcomes.test {
            viewModel.onAccept(201)
            advanceUntilIdle()

            assertEquals(OfferOutcome.ACCEPTED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `answering an offer that no longer needs an answer reports nothing`() = runTest(dispatcher) {
        viewModel.offerOutcomes.test {
            viewModel.onAccept(201)
            advanceUntilIdle()
            assertEquals(OfferOutcome.ACCEPTED, awaitItem())

            viewModel.onAccept(201)
            advanceUntilIdle()

            expectNoEvents()
        }
    }
}

private suspend fun app.cash.turbine.ReceiveTurbine<dev.barter.feature.offers.ui.OffersUiState>.awaitLoaded() {
    awaitItem() // the placeholder stateIn starts with
    awaitItem()
}
