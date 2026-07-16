package com.dloren.mispantallas.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.MAX_PROFILE_SLOTS

/** Nombre y PIN por defecto de cada perfil, usados al crear perfiles en bloque. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDefaultsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val storedNames by viewModel.defaultNames.collectAsStateWithLifecycle()
    val storedPins by viewModel.defaultPins.collectAsStateWithLifecycle()

    var names by remember { mutableStateOf(List(MAX_PROFILE_SLOTS) { "" }) }
    var pins by remember { mutableStateOf(List(MAX_PROFILE_SLOTS) { "" }) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(storedNames, storedPins) {
        if (!loaded) {
            names = storedNames
            pins = storedPins
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_profile_names_section)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            names.forEachIndexed { index, name ->
                ProfileDefaultCard(
                    slot = index + 1,
                    name = name,
                    pin = pins.getOrElse(index) { "" },
                    onNameChange = { new -> names = names.toMutableList().also { it[index] = new } },
                    onPinChange = { new -> pins = pins.toMutableList().also { it[index] = new } }
                )
            }
            Button(
                onClick = {
                    viewModel.saveNames(names)
                    viewModel.savePins(pins)
                    Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun ProfileDefaultCard(
    slot: Int,
    name: String,
    pin: String,
    onNameChange: (String) -> Unit,
    onPinChange: (String) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_profile_name_hint, slot),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.field_profile)) },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { new -> onPinChange(new.filter { it.isDigit() }) },
                    label = { Text(stringResource(R.string.field_pin)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
