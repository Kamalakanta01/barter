package dev.barter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.barter.core.domain.BarterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-level state the prototype kept outside any screen: the pending-offer count on the tab. */
@HiltViewModel
class EvenViewModel @Inject constructor(
    repository: BarterRepository,
) : ViewModel() {

    val pendingIncoming: StateFlow<Int> = repository.state
        .map { it.pendingIncoming().size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
