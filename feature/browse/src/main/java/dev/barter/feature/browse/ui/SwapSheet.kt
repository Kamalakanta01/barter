package dev.barter.feature.browse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.barter.core.designsystem.component.LetterTile
import dev.barter.feature.browse.R

/** The prototype's `openSwap` dialog. */
@Composable
fun SwapSheet(
    target: BrowseListing,
    offerable: List<BrowseListing>,
    onSendOffer: (mineListingId: Long) -> Unit,
    onDismiss: () -> Unit,
    onListItem: () -> Unit,
) {
    var selectedId by rememberSaveable(target.id, offerable) {
        mutableStateOf(offerable.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LetterTile(text = target.title, fontSize = 17.sp, modifier = Modifier.size(40.dp))
                Column {
                    Text(target.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            R.string.browse_wants_from,
                            target.ownerName,
                            target.wants,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            if (offerable.isEmpty()) {
                Text(
                    text = stringResource(R.string.browse_no_items_yet),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(modifier = Modifier.selectableGroup()) {
                    Text(
                        text = stringResource(R.string.browse_offer_one),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    offerable.forEach { listing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = listing.id == selectedId,
                                    onClick = { selectedId = listing.id },
                                )
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = listing.id == selectedId, onClick = null)
                            LetterTile(text = listing.title, fontSize = 17.sp, modifier = Modifier.size(40.dp))
                            Text(listing.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (offerable.isEmpty()) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onListItem()
                    },
                ) {
                    Text(stringResource(R.string.browse_list_an_item))
                }
            } else {
                TextButton(
                    enabled = selectedId != null,
                    onClick = { selectedId?.let(onSendOffer) },
                ) {
                    Text(stringResource(R.string.browse_send_offer))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.browse_cancel))
            }
        },
    )
}
