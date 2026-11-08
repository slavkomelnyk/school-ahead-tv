// TvPrimaryButton.kt
// =============================================================================
// Primary button optimized for TV interfaces.
// =============================================================================

package com.ahead.school.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ahead.school.ui.theme.Dimens
import com.ahead.school.ui.theme.SchoolFocusOutline


/**
 * Primary button optimized for TV interfaces.
 * 
 * This button has:
 * - Large touch target for 10-foot UI
 * - Custom focus styling with outline
 * - Support for icons and text
 * - Compact mode for secondary buttons
 * 
 * @param text The button text
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the button
 * @param icon Optional icon to display before the text
 * @param focusRequester Optional focus requester for programmatic focus
 * @param compact Whether to use compact sizing
 */
@Composable
fun TvPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    compact: Boolean = false,
) {
    // Apply focus requester if provided
    val baseModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }
    
    var isFocused by remember { mutableStateOf(false) }
    
    val focusModifier = baseModifier.onFocusChanged { isFocused = it.isFocused }

    Button(
        onClick = onClick,
        modifier = focusModifier
            .height(if (compact) Dimens.ButtonHeightCompact else Dimens.ButtonHeight)
            .defaultMinSize(minWidth = Dimens.ButtonMinWidth),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = ButtonDefaults.border(
            border = androidx.tv.material3.Border.None,
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(3.dp, SchoolFocusOutline),
                inset = 0.dp,
                shape = CircleShape
            ),
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (compact) 16.dp else Dimens.ButtonPaddingHorizontal,
            ),
        ) {
            // Optional icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconMedium),
                )
            }
            
            // Text
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
