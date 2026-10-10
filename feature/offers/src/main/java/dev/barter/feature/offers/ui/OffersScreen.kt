package dev.barter.feature.offers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.barter.core.designsystem.component.EmptyState
import dev.barter.core.designsystem.component.EvenIcons
import dev.barter.core.designsystem.component.LetterTile
import dev.barter.core.designsystem.theme.EvenTheme
import dev.barter.feature.offers.R

@Composable
private fun OfferOutcome.stringRes(): Int = when (this) {
    OfferOutcome.ACCEPTED -> R.string.offers_msg_accepted
    OfferOutcome.DECLINED -> R.string.offers_msg_declined
    OfferOutcome.WITHDRAWN -> R.string.offers_msg_withdrawn
}

@Composable
fun OffersRoute(viewModel: OffersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolved before the coroutine, which cannot call stringResource itself.
    val texts = OfferOutcome.entries.associateWith { stringResource(it.stringRes()) }

    LaunchedEffect(viewModel) {
        viewModel.offerOutcomes.collect { outcome ->
            snackbarHostState.showSnackbar(message = texts.getValue(outcome), withDismissAction = false)
        }
    }

    // Transparent: the app's one background is painted once, by the shell around every screen.
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { scaffoldPadding ->
        OffersScreen(
            uiState = uiState,
            onAccept = viewModel::onAccept,
            onDecline = viewModel::onDecline,
            onWithdraw = viewModel::onWithdraw,
            modifier = Modifier.padding(scaffoldPadding),
        )
    }
}

@Composable
fun OffersScreen(
    uiState: OffersUiState,
    onAccept: (Long) -> Unit,
    onDecline: (Long) -> Unit,
    onWithdraw: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.offers_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (uiState.isEmpty) {
            item {
                EmptyState(
                    icon = EvenIcons.Swap,
                    title = stringResource(R.string.offers_empty_title),
                    body = stringResource(R.string.offers_empty),
                )
            }
        }

        if (uiState.incoming.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.offers_incoming)) }
            items(uiState.incoming, key = OfferRow::id) { row ->
                OfferRowItem(row, onAccept, onDecline, onWithdraw)
            }
        }

        if (uiState.outgoing.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.offers_sent)) }
            items(uiState.outgoing, key = OfferRow::id) { row ->
                OfferRowItem(row, onAccept, onDecline, onWithdraw)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun OfferRowItem(
    row: OfferRow,
    onAccept: (Long) -> Unit,
    onDecline: (Long) -> Unit,
    onWithdraw: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LetterTile(text = row.mineTitle, fontSize = 17.sp, modifier = Modifier.size(40.dp))
            // A drawn arrow rather than a text glyph, which older phones may not have a font for.
            Icon(
                painter = painterResource(EvenIcons.Swap),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            LetterTile(text = row.theirsTitle, fontSize = 17.sp, modifier = Modifier.size(40.dp))
        }

        Text(
            text = stringResource(R.string.offers_for, row.mineTitle, row.theirsTitle),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = row.noteText(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (row.canRespond || row.canWithdraw) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (row.canRespond) {
                    Button(onClick = { onAccept(row.id) }, shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.offers_accept))
                    }
                    OutlinedButton(onClick = { onDecline(row.id) }, shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.offers_decline))
                    }
                }
                if (row.canWithdraw) {
                    OutlinedButton(onClick = { onWithdraw(row.id) }, shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.offers_withdraw))
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun OfferRow.noteText(): String = when (note) {
    OfferNote.SENT_TO_YOU -> stringResource(R.string.offers_note_sent_to_you, theirsOwnerName)
    OfferNote.WAITING_ON_THEM -> stringResource(R.string.offers_note_waiting_on, theirsOwnerName)
    OfferNote.SWAPPED -> stringResource(R.string.offers_note_swapped)
    OfferNote.DECLINED -> stringResource(R.string.offers_note_declined)
    OfferNote.NO_LONGER_AVAILABLE -> stringResource(R.string.offers_note_closed)
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun OffersScreenPreview() {
    EvenTheme {
        OffersScreen(
            uiState = OffersUiState(
                incoming = listOf(
                    OfferRow(
                        id = 201,
                        mineTitle = "Mechanical keyboard",
                        theirsTitle = "Ceramic mug set (4)",
                        theirsOwnerName = "Mei",
                        note = OfferNote.SENT_TO_YOU,
                        canRespond = true,
                        canWithdraw = false,
                    ),
                ),
                outgoing = listOf(
                    OfferRow(
                        id = 202,
                        mineTitle = "Road bike",
                        theirsTitle = "Film camera (Canon AE-1)",
                        theirsOwnerName = "Ana",
                        note = OfferNote.WAITING_ON_THEM,
                        canRespond = false,
                        canWithdraw = true,
                    ),
                ),
            ),
            onAccept = {},
            onDecline = {},
            onWithdraw = {},
        )
    }
}
