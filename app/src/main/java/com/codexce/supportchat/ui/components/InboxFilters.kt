package com.codexce.supportchat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codexce.supportchat.data.model.InboxFilter

/**
 * Assigned / Unassigned / Closed / Pending, plus All. Only this row scrolls horizontally.
 * Every chip maps onto the existing `status` / `assignedAgentUid` fields, so the labels are
 * the real queue states rather than a decorative segmented control.
 */
@Composable
fun InboxFilters(
    selected: InboxFilter,
    counts: Map<InboxFilter, Int>,
    onSelect: (InboxFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = InboxFilter.entries, key = { it.name }) { filter ->
            val count = counts[filter] ?: 0
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = debounced { onSelect(filter) },
                label = {
                    Text(
                        text = if (count > 0) "${filter.label} $count" else filter.label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                shape = CircleShape,
                // Neutral tint at rest, ink when selected: no outlines, separation by tint.
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = null,
            )
        }
    }
}
