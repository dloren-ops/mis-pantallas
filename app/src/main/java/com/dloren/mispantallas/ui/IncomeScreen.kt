package com.dloren.mispantallas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.util.formatCurrency

/**
 * Resumen de ingresos a partir del precio cargado en cada pantalla alquilada.
 * El "estimado mensual" normaliza cada precio a un ciclo de 30 días, para poder
 * sumar alquileres con duraciones distintas (semanal, quincenal, mensual, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val otherPlatformLabel = stringResource(R.string.platform_other)
    val rented = remember(accounts) { accounts.filter { it.clientPhone.isNotBlank() } }
    val activeTotal = remember(rented) { rented.sumOf { it.price } }
    val monthlyEstimate = remember(rented) {
        rented.sumOf { account ->
            if (account.durationDays > 0) account.price / account.durationDays * 30 else 0.0
        }
    }
    val byPlatform = remember(rented, otherPlatformLabel) {
        rented
            .filter { it.price > 0.0 }
            .groupBy { it.platform.ifBlank { otherPlatformLabel } }
            .mapValues { (_, accs) -> accs.sumOf { it.price } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.income_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (rented.none { it.price > 0.0 }) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_income),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(
                    label = stringResource(R.string.income_active_total),
                    amount = activeTotal
                )
            }
            item {
                SummaryCard(
                    label = stringResource(R.string.income_monthly_estimate),
                    amount = monthlyEstimate
                )
            }
            item {
                Text(
                    text = stringResource(R.string.income_by_platform),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(byPlatform, key = { it.first }) { (platform, total) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(platform, style = MaterialTheme.typography.bodyLarge)
                        Text(formatCurrency(total), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
