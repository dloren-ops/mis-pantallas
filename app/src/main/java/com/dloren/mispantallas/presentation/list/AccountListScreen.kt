package com.dloren.mispantallas.presentation.list

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dloren.mispantallas.R
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.presentation.AppViewModelProvider
import com.dloren.mispantallas.presentation.backup.BackupIo
import com.dloren.mispantallas.presentation.platform.Platforms
import com.dloren.mispantallas.presentation.whatsapp.WhatsAppLauncher
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    onAccountClick: (Long) -> Unit,
    onOpenRenewals: () -> Unit,
    onOpenNuevas: () -> Unit,
    viewModel: AccountListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = BackupIo.readText(context, uri)
                    val n = viewModel.restoreBackup(json)
                    Toast.makeText(
                        context,
                        context.getString(R.string.import_done, n),
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.import_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Escuchar eventos puntuales (Toasts).
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                is UpdateEvent.UpToDate -> context.getString(R.string.up_to_date)
                is UpdateEvent.NoReleases -> context.getString(R.string.no_releases)
                is UpdateEvent.PermissionRequested ->
                    context.getString(R.string.install_permission_needed)
                is UpdateEvent.Error ->
                    context.getString(R.string.update_error, event.message)
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_accounts)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(
                        onClick = onOpenNuevas,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.open_nuevas))
                    }
                    IconButton(onClick = onOpenRenewals) {
                        Icon(
                            Icons.Filled.Autorenew,
                            contentDescription = stringResource(R.string.open_renewals)
                        )
                    }
                    if (updateState is UpdateUiState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.checkForUpdates() }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.check_updates)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backup_export)) },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        try {
                                            BackupIo.shareJson(context, viewModel.buildBackupJson())
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.backup_error),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backup_import)) },
                                onClick = {
                                    menuExpanded = false
                                    importLauncher.launch("*/*")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SummaryBar(
                summary = summary,
                selected = filter,
                onSelect = viewModel::onFilterChange
            )
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            if (query.isBlank()) R.string.no_sales_yet else R.string.no_results
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            onClick = { onAccountClick(account.id) },
                            onSend = {
                                WhatsAppLauncher.send(context, account)
                                viewModel.markSoldIfNeeded(account)
                            },
                            onMoveToNotSold = { viewModel.markNotSold(account) }
                        )
                    }
                }
            }
        }
    }

    val state = updateState
    if (state is UpdateUiState.Available) {
        AlertDialog(
            onDismissRequest = { if (!state.downloading) viewModel.dismissUpdate() },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                if (state.downloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("  " + stringResource(R.string.downloading_update))
                    }
                } else {
                    Text(stringResource(R.string.update_available_body, state.release.tag))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.downloading,
                    onClick = { viewModel.downloadAndInstall() }
                ) { Text(stringResource(R.string.update_now)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.downloading,
                    onClick = { viewModel.dismissUpdate() }
                ) { Text(stringResource(R.string.later)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    onSend: () -> Unit,
    onMoveToNotSold: () -> Unit
) {
    val platformColor = Platforms.colorFor(account.platform)
    val title = account.platform.ifBlank { account.email.ifBlank { "Cuenta" } }
    val subtitle = account.profileName.ifBlank { account.email }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(platformColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    CountdownText(account)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    StatusBadge(account)
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = "  $subtitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = onMoveToNotSold) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.move_to_not_sold),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onSend) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_whatsapp),
                    tint = platformColor
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(account: Account) {
    val sold = account.isSold
    val bg = if (sold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Text(
        text = stringResource(if (sold) R.string.status_sold else R.string.status_not_sold),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun CountdownText(account: Account) {
    if (!account.isSold) return
    val remaining = account.remainingClientDays()
    val (text, color) = when {
        remaining < 0L -> stringResource(R.string.expired_ago, -remaining) to Color(0xFFB3261E)
        remaining == 0L -> stringResource(R.string.last_day) to Color(0xFFB3261E)
        remaining == 1L -> stringResource(R.string.one_day_left) to Color(0xFFE65100)
        remaining <= 3L -> stringResource(R.string.days_left, remaining) to Color(0xFFE65100)
        else -> stringResource(R.string.days_left, remaining) to Color(0xFF2E7D32)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1
    )
}

@Composable
private fun SummaryBar(
    summary: ListSummary,
    selected: ListFilter,
    onSelect: (ListFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            label = stringResource(R.string.summary_sold),
            count = summary.sold,
            color = Color(0xFF2E7D32),
            selected = selected == ListFilter.SOLD,
            onClick = { onSelect(ListFilter.SOLD) }
        )
        SummaryChip(
            label = stringResource(R.string.summary_due_soon),
            count = summary.dueSoon,
            color = Color(0xFFE65100),
            selected = selected == ListFilter.DUE_SOON,
            onClick = { onSelect(ListFilter.DUE_SOON) }
        )
        SummaryChip(
            label = stringResource(R.string.summary_expired),
            count = summary.expired,
            color = Color(0xFFB3261E),
            selected = selected == ListFilter.EXPIRED,
            onClick = { onSelect(ListFilter.EXPIRED) }
        )
        SummaryChip(
            label = stringResource(R.string.summary_not_sold),
            count = summary.notSold,
            color = Color(0xFF555555),
            selected = selected == ListFilter.NOT_SOLD,
            onClick = { onSelect(ListFilter.NOT_SOLD) }
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) color else color.copy(alpha = 0.12f)
    val countColor = if (selected) Color.White else color
    val labelColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = countColor
        )
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
    }
}
