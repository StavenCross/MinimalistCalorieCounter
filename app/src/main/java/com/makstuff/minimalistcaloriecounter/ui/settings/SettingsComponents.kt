package com.makstuff.minimalistcaloriecounter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsHubCard(
    title: String,
    subtitle: String,
    meta: String,
    emphasized: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f) else MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 10.dp, top = 2.dp),
            )
        }
        content()
    }
}

@Composable
fun OptionsSectionHeader(text: String, isExpanded: Boolean? = null, onToggle: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f))
            .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (isExpanded != null) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun OptionsItem(
    text: String,
    trailingText: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        },
        trailingContent = trailingContent ?: trailingText?.let {
            {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
fun SelectableOptionsItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OptionsItem(
        text = text,
        trailingContent = if (selected) {
            {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            null
        },
        onClick = onClick,
    )
}
