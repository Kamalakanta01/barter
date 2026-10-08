package dev.barter.feature.myitems.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import dev.barter.feature.myitems.R

@Composable
fun MyItemsRoute(viewModel: MyItemsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val removedTemplate = stringResource(R.string.my_items_removed)
    val undo = stringResource(R.string.action_undo)

    // Taking down a listing is the one destructive action here, so it is undoable rather than final.
    LaunchedEffect(viewModel) {
        viewModel.removedListings.collect { listing ->
            val result = snackbarHostState.showSnackbar(
                message = String.format(removedTemplate, listing.title),
                actionLabel = undo,
                withDismissAction = false,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.onUndoRemove(listing)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { scaffoldPadding ->
        MyItemsScreen(
            uiState = uiState,
            onAddClicked = viewModel::onAddClicked,
            onRemove = viewModel::onRemove,
            modifier = Modifier.padding(scaffoldPadding),
        )
    }

    if (uiState.isAddDialogVisible) {
        AddListingDialog(
            onConfirm = viewModel::onAddConfirmed,
            onDismiss = viewModel::onAddDismissed,
        )
    }

    uiState.removalToConfirm?.let { listing ->
        RemoveListingDialog(
            listing = listing,
            onConfirm = viewModel::onRemoveConfirmed,
            onDismiss = viewModel::onRemoveDismissed,
        )
    }
}

@Composable
fun MyItemsScreen(
    uiState: MyItemsUiState,
    onAddClicked: () -> Unit,
    onRemove: (MyListing) -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.my_items_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Button(onClick = onAddClicked, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.my_items_add))
                }
            }
        }

        if (uiState.listings.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.my_items_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(uiState.listings, key = MyListing::id) { listing ->
            Box(Modifier.height(IntrinsicSize.Min)) {
                MyListingCard(
                    listing = listing,
                    onRemove = { onRemove(listing) },
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun MyListingCard(
    listing: MyListing,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EvenCard(modifier = modifier.fillMaxHeight()) {
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
            text = stringResource(R.string.my_items_wants, listing.wants),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.padding(top = 10.dp)) {
            if (listing.isTraded) {
                Text(
                    text = stringResource(R.string.my_items_swapped),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedButton(onClick = onRemove, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.my_items_remove))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun MyItemsScreenPreview() {
    EvenTheme {
        MyItemsScreen(
            uiState = MyItemsUiState(
                listings = listOf(
                    MyListing(101, "Mechanical keyboard", "Plants or kitchenware", isTraded = false),
                    MyListing(102, "Road bike", "Camera gear", isTraded = true),
                ),
            ),
            onAddClicked = {},
            onRemove = {},
        )
    }
}
