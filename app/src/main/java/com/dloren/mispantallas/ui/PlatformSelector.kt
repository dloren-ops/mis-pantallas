package com.dloren.mispantallas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dloren.mispantallas.R
import com.dloren.mispantallas.util.STREAMING_PLATFORMS

/**
 * Lista desplegable con las plataformas de streaming más comunes (cada una con su
 * color) más una opción "Otra" que revela un campo libre para una plataforma nueva.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformSelector(
    platform: String,
    onPlatformChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var isCustom by remember(platform) {
        mutableStateOf(platform.isNotBlank() && STREAMING_PLATFORMS.none { it.name == platform })
    }
    val selected = STREAMING_PLATFORMS.find { it.name == platform }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = if (isCustom) stringResource(R.string.platform_other) else platform,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(stringResource(R.string.field_platform)) },
                leadingIcon = selected?.let { p -> { PlatformDot(p.color) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                STREAMING_PLATFORMS.forEach { p ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlatformDot(p.color)
                                Text(text = "  " + p.name)
                            }
                        },
                        onClick = {
                            isCustom = false
                            onPlatformChange(p.name)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.platform_other)) },
                    onClick = {
                        isCustom = true
                        onPlatformChange("")
                        expanded = false
                    }
                )
            }
        }
        if (isCustom) {
            OutlinedTextField(
                value = platform,
                onValueChange = onPlatformChange,
                readOnly = !enabled,
                label = { Text(stringResource(R.string.platform_other)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PlatformDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}
