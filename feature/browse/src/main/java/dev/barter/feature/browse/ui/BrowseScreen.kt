package dev.barter.feature.browse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.barter.core.designsystem.component.EvenCard
import dev.barter.core.designsystem.component.LetterTile
import dev.barter.core.designsystem.theme.EvenTheme
import dev.barter.feature.browse.R

@Composable
fun BrowseRoute(
    onListItem: () -> Unit,
    onOfferSent: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BrowseScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onListingClick = viewModel::onListingClick,
    )

    uiState.swapTarget?.let { target ->
        SwapSheet(
            target = target,
            offerable = uiState.offerable,
            onSendOffer = { listingId ->
                viewModel.onSendOffer(listingId)
                onOfferSent()
            },
            onDismiss = viewModel::onSwapDismissed,
            onListItem = onListItem,
        )
    }
}

@Composable
fun BrowseScreen(
    uiState: BrowseUiState,
    onQueryChange: (String) -> Unit,
    onListingClick: (BrowseListing) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.browse_hero),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        if (uiState.listings.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (uiState.query.isBlank()) {
                        stringResource(R.string.browse_nothing_yet)
                    } else {
                        stringResource(R.string.browse_empty, uiState.query)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(uiState.listings, key = BrowseListing::id) { listing ->
            Box(Modifier.height(IntrinsicSize.Min)) {
                ListingCard(
                    listing = listing,
                    onClick = { onListingClick(listing) },
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ListingCard(
    listing: BrowseListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EvenCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        LetterTile(
            text = listing.title,
            fontSize = 48.sp,
            cornerRadius = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .padding(bottom = 10.dp),
        )
        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listing.ownerName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = stringResource(R.string.browse_wants),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = listing.wants,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun BrowseScreenPreview() {
    EvenTheme {
        BrowseScreen(
            uiState = BrowseUiState(
                listings = listOf(
                    BrowseListing(1, "Film camera (Canon AE-1)", "Ana", "Bike lights or vinyl records"),
                    BrowseListing(2, "Acoustic guitar", "Kofi", "A bookshelf"),
                ),
            ),
            onQueryChange = {},
            onListingClick = {},
        )
    }
}
