package dev.barter.feature.myitems.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.barter.feature.myitems.R

/** Shown only for a listing with pending offers, because removing it cancels them for good. */
@Composable
fun RemoveListingDialog(
    listing: MyListing,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.my_items_remove_dialog_title, listing.title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = pluralStringResource(
                    R.plurals.my_items_remove_dialog_message,
                    listing.pendingOffers,
                    listing.pendingOffers,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.my_items_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.my_items_dialog_cancel))
            }
        },
    )
}
