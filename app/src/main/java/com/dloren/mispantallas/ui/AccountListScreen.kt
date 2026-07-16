package com.dloren.mispantallas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dloren.mispantallas.R
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.util.DAY_PRESETS
import com.dloren.mispantallas.util.WhatsAppHelper
import com.dloren.mispantallas.util.DateUtils
import com.dloren.mispantallas.util.colorForPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    viewModel: AccountViewModel,
    settingsViewModel: SettingsViewModel,
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit,
    isReadOnly: Boolean = false,
    isAdmin: Boolean = false,
    onOpenAdmin: () -> Unit = {},
    onOpenNewProfiles: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRenewals: () -> Unit = {},
    onOpenClients: () -> Unit = {},
    onOpenIncome: () -> Unit = {},
    onOpenSavedCredentials: () -> Unit = {}
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val pendingNewProfiles = remember(accounts) { accounts.count { it.clientPhone.isBlank() } }
    val rentedAccounts = remember(accounts) { accounts.filter { it.clientPhone.isNotBlank() } }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf<String?>(null) }
    val filteredAccounts = remember(rentedAccounts, searchQuery, selectedPlatform) {
        val query = searchQuery.trim()
        val byPlatform = if (selectedPlatform == null) {
            rentedAccounts
        } else {
            rentedAccounts.filter { it.platform == selectedPlatform }
        }
        if (query.isEmpty()) {
            byPlatform
        } else {
            val digitsQuery = query.filter { it.isDigit() }
            byPlatform.filter { account ->
                account.email.contains(query, ignoreCase = true) ||
                    account.clientPhone.contains(query, ignoreCase = true) ||
                    (digitsQuery.isNotEmpty() && account.clientPhone.filter { it.isDigit() }.contains(digitsQuery))
            }
        }
    }
    val defaultNames by settingsViewModel.defaultNames.collectAsStateWithLifecycle()
    val defaultPins by settingsViewModel.defaultPins.collectAsStateWithLifecycle()
    val whatsappMessage by settingsViewModel.whatsappMessage.collectAsStateWithLifecycle()
    val whatsappFooter by settingsViewModel.whatsappFooter.collectAsStateWithLifecycle()
    val renewalReminderMessage by settingsViewModel.renewalReminderMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showCreateProfilesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.title_accounts))
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge { Text(rentedAccounts.size.toString()) }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRenewals) {
                        Icon(Icons.Filled.Autorenew, contentDescription = stringResource(R.string.renewals_title))
                    }
                    BadgedBox(
                        badge = {
                            if (pendingNewProfiles > 0) {
                                Badge { Text(pendingNewProfiles.toString()) }
                            }
                        }
                    ) {
                        AssistChip(
                            onClick = onOpenNewProfiles,
                            leadingIcon = {
                                Icon(Icons.Filled.Inbox, contentDescription = null)
                            },
                            label = { Text(stringResource(R.string.new_profiles_title)) }
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clients_title)) },
                            onClick = {
                                showMenu = false
                                onOpenClients()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.income_title)) },
                            onClick = {
                                showMenu = false
                                onOpenIncome()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.saved_credentials_title)) },
                            onClick = {
                                showMenu = false
                                onOpenSavedCredentials()
                            }
                        )
                        if (!isReadOnly) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.create_profiles)) },
                                onClick = {
                                    showMenu = false
                                    showCreateProfilesDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_menu_item)) },
                            onClick = {
                                showMenu = false
                                onOpenSettings()
                            }
                        )
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.admin_title)) },
                                onClick = {
                                    showMenu = false
                                    onOpenAdmin()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(onClick = onAddAccount) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_account))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isReadOnly) {
                ReadOnlyBanner()
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                }
            )
            PlatformFilterRow(
                accounts = rentedAccounts,
                selectedPlatform = selectedPlatform,
                onSelectPlatform = { selectedPlatform = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            AccountsColumn(
                accounts = filteredAccounts,
                isReadOnly = isReadOnly,
                onAccountClick = onAccountClick,
                onSend = { account -> WhatsAppHelper.sendToWhatsApp(context, account, whatsappMessage, whatsappFooter) },
                onAssignPhoneAndSend = { account, phone ->
                    val updated = account.copy(clientPhone = phone, startDateMillis = DateUtils.nowStartOfDayMillis())
                    viewModel.save(updated) { WhatsAppHelper.sendToWhatsApp(context, updated, whatsappMessage, whatsappFooter) }
                },
                onSendRenewalReminder = { account ->
                    WhatsAppHelper.sendRenewalReminder(context, account, renewalReminderMessage)
                },
                allowStatusToggle = true,
                onMarkAsRented = { account, phone ->
                    viewModel.save(account.copy(clientPhone = phone, startDateMillis = DateUtils.nowStartOfDayMillis()))
                },
                onMarkAsNew = { account -> viewModel.save(account.copy(clientPhone = "")) },
                onDeleteSelected = if (isReadOnly) null else { toDelete -> viewModel.deleteAll(toDelete) },
                emptyMessage = if (searchQuery.isBlank()) {
                    stringResource(R.string.no_accounts)
                } else {
                    stringResource(R.string.no_search_results)
                }
            )
        }
    }

    if (showCreateProfilesDialog) {
        CreateProfilesDialog(
            onDismiss = { showCreateProfilesDialog = false },
            onCreate = { platform, email, password, durationDays, profileCount ->
                val now = DateUtils.nowStartOfDayMillis()
                val generated = (1..profileCount).map { index ->
                    val customName = defaultNames.getOrNull(index - 1).orEmpty()
                    val customPin = defaultPins.getOrNull(index - 1).orEmpty()
                    Account(
                        platform = platform,
                        email = email,
                        password = password,
                        profileName = customName.ifBlank { "Perfil $index" },
                        pin = customPin,
                        durationDays = durationDays,
                        startDateMillis = now
                    )
                }
                viewModel.importAccounts(generated)
                showCreateProfilesDialog = false
            }
        )
    }
}

/** Chips de filtro por plataforma con la cantidad de cuentas de cada una, reutilizado por [AccountListScreen] y [NewProfilesScreen]. */
@Composable
internal fun PlatformFilterRow(
    accounts: List<Account>,
    selectedPlatform: String?,
    onSelectPlatform: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val platformCounts = remember(accounts) {
        accounts.filter { it.platform.isNotBlank() }.groupingBy { it.platform }.eachCount()
    }
    val availablePlatforms = remember(platformCounts) {
        platformCounts.keys.sortedWith(compareByDescending<String> { platformCounts[it] ?: 0 }.thenBy { it })
    }

    if (availablePlatforms.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedPlatform == null,
                onClick = { onSelectPlatform(null) },
                label = { Text("${stringResource(R.string.filter_all_platforms)} (${accounts.size})") }
            )
        }
        items(availablePlatforms) { platform ->
            FilterChip(
                selected = selectedPlatform == platform,
                onClick = { onSelectPlatform(if (selectedPlatform == platform) null else platform) },
                label = { Text("$platform (${platformCounts[platform] ?: 0})") }
            )
        }
    }
}

/**
 * Lista de cuentas reutilizada por [AccountListScreen], [NewProfilesScreen] y
 * [RenewalsScreen]. Si una cuenta todavía no tiene WhatsApp del cliente cargado,
 * enviar pide el número antes de abrir el chat (y lo guarda en la cuenta).
 */
@Composable
internal fun AccountsColumn(
    accounts: List<Account>,
    isReadOnly: Boolean,
    onAccountClick: (Long) -> Unit,
    onSend: (Account) -> Unit,
    onAssignPhoneAndSend: (Account, String) -> Unit,
    emptyMessage: String,
    allowStatusToggle: Boolean = false,
    onMarkAsRented: (Account, String) -> Unit = { _, _ -> },
    onMarkAsNew: (Account) -> Unit = {},
    onDeleteSelected: ((List<Account>) -> Unit)? = null,
    onSendRenewalReminder: ((Account) -> Unit)? = null,
    useProviderCountdown: Boolean = false
) {
    var pendingPhoneFor by remember { mutableStateOf<Account?>(null) }
    var pendingRentedFor by remember { mutableStateOf<Account?>(null) }
    var pendingNewFor by remember { mutableStateOf<Account?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }
    val selectionEnabled = onDeleteSelected != null
    val selectionActive = selectionEnabled && selectedIds.isNotEmpty()

    fun toggleSelection(id: Long) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    if (selectionActive) {
        SelectionActionBar(
            count = selectedIds.size,
            onCancel = { selectedIds = emptySet() },
            onDelete = { showDeleteSelectedConfirm = true }
        )
    }

    if (accounts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    onClick = {
                        if (selectionActive) {
                            toggleSelection(account.id)
                        } else {
                            onAccountClick(account.id)
                        }
                    },
                    onLongClick = if (selectionEnabled) {
                        { toggleSelection(account.id) }
                    } else {
                        null
                    },
                    isSelected = selectedIds.contains(account.id),
                    selectionModeActive = selectionActive,
                    onSend = {
                        if (account.clientPhone.isBlank()) {
                            pendingPhoneFor = account
                        } else {
                            onSend(account)
                        }
                    },
                    onSendRenewalReminder = if (onSendRenewalReminder != null && account.clientPhone.isNotBlank()) {
                        { onSendRenewalReminder(account) }
                    } else {
                        null
                    },
                    useProviderCountdown = useProviderCountdown,
                    isReadOnly = isReadOnly,
                    onToggleRented = if (allowStatusToggle && !isReadOnly && !selectionActive) {
                        {
                            if (account.clientPhone.isBlank()) {
                                pendingRentedFor = account
                            } else {
                                pendingNewFor = account
                            }
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = { Text(stringResource(R.string.delete_selected_accounts_title)) },
            text = { Text(stringResource(R.string.delete_selected_accounts_confirm, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = accounts.filter { selectedIds.contains(it.id) }
                    onDeleteSelected?.invoke(toDelete)
                    selectedIds = emptySet()
                    showDeleteSelectedConfirm = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    pendingPhoneFor?.let { account ->
        PhoneNumberDialog(
            title = stringResource(R.string.send_whatsapp),
            confirmLabel = stringResource(R.string.send_whatsapp),
            onDismiss = { pendingPhoneFor = null },
            onConfirm = { phone ->
                onAssignPhoneAndSend(account, phone)
                pendingPhoneFor = null
            }
        )
    }

    pendingRentedFor?.let { account ->
        PhoneNumberDialog(
            title = stringResource(R.string.mark_as_rented),
            confirmLabel = stringResource(R.string.mark_as_rented),
            onDismiss = { pendingRentedFor = null },
            onConfirm = { phone ->
                onMarkAsRented(account, phone)
                pendingRentedFor = null
            }
        )
    }

    pendingNewFor?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingNewFor = null },
            title = { Text(stringResource(R.string.mark_as_new)) },
            text = { Text(stringResource(R.string.mark_as_new_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onMarkAsNew(account)
                    pendingNewFor = null
                }) { Text(stringResource(R.string.mark_as_new)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingNewFor = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
private fun PhoneNumberDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.field_client_phone)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(phone.trim()) },
                enabled = phone.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    onSend: () -> Unit,
    isReadOnly: Boolean = false,
    onToggleRented: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    selectionModeActive: Boolean = false,
    onSendRenewalReminder: (() -> Unit)? = null,
    useProviderCountdown: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionModeActive) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(4.dp))
            }
            PlatformAvatar(account.platform)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.platform.ifBlank { account.email.ifBlank { "Cuenta" } },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (account.profileName.isNotBlank()) {
                    Text(
                        text = "Perfil: ${account.profileName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (account.email.isNotBlank()) {
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val countdownDays = if (useProviderCountdown) {
                    account.providerRemainingDays() ?: account.remainingDays()
                } else {
                    account.remainingDays()
                }
                CountdownLabel(countdownDays, isProviderCountdown = useProviderCountdown)
            }
            if (onToggleRented != null) {
                IconButton(onClick = onToggleRented) {
                    Icon(
                        Icons.Filled.SwapHoriz,
                        contentDescription = stringResource(
                            if (account.clientPhone.isBlank()) R.string.mark_as_rented else R.string.mark_as_new
                        ),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (!isReadOnly && !selectionModeActive && onSendRenewalReminder != null) {
                IconButton(onClick = onSendRenewalReminder) {
                    Icon(
                        Icons.Filled.NotificationsActive,
                        contentDescription = stringResource(R.string.send_renewal_reminder),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (!isReadOnly && !selectionModeActive) {
                IconButton(onClick = onSend) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_whatsapp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** Barra contextual que aparece al seleccionar una o más cuentas para borrarlas en lote. */
@Composable
private fun SelectionActionBar(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel_selection))
        }
        Text(
            text = stringResource(R.string.selected_count, count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Círculo de color con la inicial de la plataforma, para reconocer cuentas de un vistazo. */
@Composable
private fun PlatformAvatar(platform: String) {
    val color = remember(platform) { colorForPlatform(platform) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = platform.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateProfilesDialog(
    onDismiss: () -> Unit,
    onCreate: (platform: String, email: String, password: String, durationDays: Int, profileCount: Int) -> Unit
) {
    var platform by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var profileCount by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_profiles_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformSelector(
                    platform = platform,
                    onPlatformChange = { platform = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.field_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.field_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.field_duration_quick),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val selectedDays = duration.toIntOrNull()
                    DAY_PRESETS.forEach { days ->
                        FilterChip(
                            selected = selectedDays == days,
                            onClick = { duration = days.toString() },
                            label = { Text("$days días") }
                        )
                    }
                }
                OutlinedTextField(
                    value = duration,
                    onValueChange = { new -> duration = new.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.field_duration)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.profile_count_label),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { count ->
                        FilterChip(
                            selected = profileCount == count,
                            onClick = { profileCount = count },
                            label = { Text(count.toString()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        platform.trim(),
                        email.trim(),
                        password.trim(),
                        duration.toIntOrNull() ?: 30,
                        profileCount
                    )
                },
                enabled = platform.isNotBlank() && email.isNotBlank()
            ) { Text(stringResource(R.string.admin_client_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
internal fun CountdownLabel(remaining: Long, isProviderCountdown: Boolean = false) {
    val (text, color) = when {
        remaining < 0L -> stringResource(
            if (isProviderCountdown) R.string.provider_expired_ago else R.string.expired_ago,
            -remaining
        ) to Color(0xFFB3261E)
        remaining == 0L -> stringResource(
            if (isProviderCountdown) R.string.provider_expired_today else R.string.expired_today
        ) to Color(0xFFB3261E)
        remaining == 1L -> stringResource(
            if (isProviderCountdown) R.string.provider_last_day else R.string.last_day
        ) to Color(0xFFB3261E)
        remaining <= 3L -> stringResource(
            if (isProviderCountdown) R.string.provider_days_left else R.string.days_left,
            remaining
        ) to Color(0xFFE65100)
        else -> stringResource(
            if (isProviderCountdown) R.string.provider_days_left else R.string.days_left,
            remaining
        ) to Color(0xFF2E7D32)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 6.dp)
    )
}
