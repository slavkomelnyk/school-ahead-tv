// TvChip.kt
// =============================================================================
// Chip/Tag button for TV interfaces.
// =============================================================================

package com.ahead.school.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ahead.school.ui.theme.Dimens
import com.ahead.school.ui.theme.SchoolFocusOutline


/**
 * Chip/Tag button for TV interfaces.
 * 
 * This button is designed for selection UIs where users can choose between
 * multiple options. It has a distinct selected state.
 * 
 * @param label The chip label
 * @param selected Whether the chip is currently selected
 * @param onClick Callback when chip is clicked
 * @param modifier Modifier for the chip
 */
@Composable
fun TvChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        colors = ButtonDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = ButtonDefaults.border(
            border = androidx.tv.material3.Border.None,
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(3.dp, SchoolFocusOutline),
                inset = 0.dp,
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(
                horizontal = Dimens.ButtonPaddingHorizontal,
                vertical = Dimens.SpacingSmall
            ),
        )
    }
}
