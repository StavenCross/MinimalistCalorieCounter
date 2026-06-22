package com.makstuff.minimalistcaloriecounter.ui.reused

import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp


@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<DropdownMenuItemData>,
    offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset
    ) {
        items.forEach { itemData ->
            DropdownMenuItem(
                text = itemData.text,
                onClick = {
                    itemData.onClick()
                    if (itemData.dismissOnClick) {
                        onDismissRequest()
                    }
                }
            )
        }
    }
}