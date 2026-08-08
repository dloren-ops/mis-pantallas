package com.dloren.mispantallas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.ui.AccountFormScreen
import com.dloren.mispantallas.ui.AccountListScreen
import com.dloren.mispantallas.ui.AccountViewModel
import com.dloren.mispantallas.ui.AdminScreen
import com.dloren.mispantallas.ui.AppUpdateViewModel
import com.dloren.mispantallas.ui.UpdateAvailableDialog
import com.dloren.mispantallas.ui.BackupScreen
import com.dloren.mispantallas.ui.ClientsScreen
import com.dloren.mispantallas.ui.EmailChangeNoticesScreen
import com.dloren.mispantallas.ui.IncomeScreen
import com.dloren.mispantallas.ui.LicenseGateScreen
import com.dloren.mispantallas.ui.LicenseState
import com.dloren.mispantallas.ui.LicenseViewModel
import com.dloren.mispantallas.ui.NewProfilesScreen
import com.dloren.mispantallas.ui.PaymentViewModel
import com.dloren.mispantallas.ui.ProfileDefaultsScreen
import com.dloren.mispantallas.ui.ReminderSettingsScreen
import com.dloren.mispantallas.ui.PausedAccountsScreen
import com.dloren.mispantallas.ui.PausedAccountsViewModel
import com.dloren.mispantallas.ui.RenewalEditScreen
import com.dloren.mispantallas.ui.RenewalsScreen
import com.dloren.mispantallas.ui.SavedCredentialsScreen
import com.dloren.mispantallas.ui.SavedCredentialsViewModel
import com.dloren.mispantallas.ui.SettingsScreen
import com.dloren.mispantallas.ui.SettingsViewModel
import com.dloren.mispantallas.ui.WhatsappMessageScreen
import com.dloren.mispantallas.ui.theme.MisPantallasTheme
import com.dloren.mispantallas.util.AccountBackup
import com.dloren.mispantallas.util.WhatsAppHelper
import com.dloren.mispantallas.worker.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: AccountViewModel by viewModels { AccountViewModel.Factory }
    private val licenseViewModel: LicenseViewModel by viewModels { LicenseViewModel.Factory }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }
    private val savedCredentialsViewModel: SavedCredentialsViewModel by viewModels { SavedCredentialsViewModel.Factory }
    private val pausedAccountsViewModel: PausedAccountsViewModel by viewModels { PausedAccountsViewModel.Factory }
    private val paymentViewModel: PaymentViewModel by viewModels { PaymentViewModel.Factory }
    private val appUpdateViewModel: AppUpdateViewModel by viewModels { AppUpdateViewModel.Factory }

    // Cuenta recibida desde un "Compartir" de WhatsApp, pendiente de mostrar.
    private var pendingShared by mutableStateOf<Account?>(null)

    // true si la app se abrió tocando el recordatorio de vencimientos.
    private var pendingOpenRenewals by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solo procesamos el intent de arranque en un inicio "en frío": si savedInstanceState
        // no es null, esta Activity se está recreando (rotación, o Android la restauró tras
        // matarla en segundo plano) con el mismo intent original, y reprocesarlo volvería a
        // importar/duplicar cuentas que ya se agregaron (o incluso que ya borraste).
        if (savedInstanceState == null) {
            handleShareIntent(intent)
        }
        pendingOpenRenewals = intent?.getBooleanExtra(EXTRA_OPEN_RENEWALS, false) == true
        requestNotificationPermissionIfNeeded()
        ReminderScheduler.schedule(this)

        setContent {
            MisPantallasTheme {
                val navController = rememberNavController()
                val licenseState by licenseViewModel.licenseState.collectAsStateWithLifecycle()

                // Revalida la licencia contra Firestore cada vez que la app vuelve a primer plano.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            licenseViewModel.revalidate()
                            appUpdateViewModel.checkForUpdate()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Si llegó algo por "Compartir", navegamos al formulario prellenado.
                LaunchedEffect(pendingShared) {
                    val shared = pendingShared
                    if (shared != null) {
                        viewModel.setSharedDraft(shared)
                        pendingShared = null
                        navController.navigate(Routes.FORM_NEW)
                    }
                }

                // Una vez resuelta la licencia (cliente activo/vencido o admin), salimos del
                // gate hacia la lista. Solo dispara mientras seguimos en el gate: cambios
                // posteriores del estado (p. ej. se vence la licencia estando en la lista) no
                // deben forzar una navegación adicional.
                LaunchedEffect(licenseState) {
                    val isResolved = licenseState is LicenseState.Client || licenseState is LicenseState.Admin
                    val onGate = navController.currentDestination?.route == Routes.GATE
                    if (isResolved && onGate) {
                        navController.navigate(Routes.LIST) {
                            popUpTo(Routes.GATE) { inclusive = true }
                        }
                    }
                }

                // Si se abrió la app desde el recordatorio de vencimientos, vamos directo
                // a Renovaciones una vez resuelta la licencia.
                LaunchedEffect(licenseState, pendingOpenRenewals) {
                    val isResolved = licenseState is LicenseState.Client || licenseState is LicenseState.Admin
                    if (isResolved && pendingOpenRenewals) {
                        pendingOpenRenewals = false
                        navController.navigate(Routes.RENEWALS)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.GATE
                ) {
                    composable(Routes.GATE) {
                        LicenseGateScreen(licenseViewModel)
                    }
                    composable(Routes.LIST) {
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        val isAdmin = licenseState is LicenseState.Admin
                        AccountListScreen(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            onAddAccount = { navController.navigate(Routes.FORM_NEW) },
                            onAccountClick = { id ->
                                navController.navigate("${Routes.FORM}/$id")
                            },
                            isReadOnly = isReadOnly,
                            isAdmin = isAdmin,
                            onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                            onOpenNewProfiles = { navController.navigate(Routes.NEW_PROFILES) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenRenewals = { navController.navigate(Routes.RENEWALS) },
                            onOpenClients = { navController.navigate(Routes.CLIENTS) },
                            onOpenIncome = { navController.navigate(Routes.INCOME) },
                            onOpenSavedCredentials = { navController.navigate(Routes.SAVED_CREDENTIALS) },
                            onOpenEmailChangeNotices = { navController.navigate(Routes.EMAIL_CHANGE_NOTICES) }
                        )
                    }
                    composable(Routes.EMAIL_CHANGE_NOTICES) {
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        EmailChangeNoticesScreen(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            onAccountClick = { id -> navController.navigate("${Routes.FORM}/$id") },
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(Routes.CLIENTS) {
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        ClientsScreen(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            onAccountClick = { id -> navController.navigate("${Routes.FORM}/$id") },
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(Routes.INCOME) {
                        IncomeScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.RENEWALS) {
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        RenewalsScreen(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            onAccountClick = { id -> navController.navigate("${Routes.RENEWAL_EDIT}/$id") },
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(
                        route = "${Routes.RENEWAL_EDIT}/{accountId}",
                        arguments = listOf(navArgument("accountId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("accountId") ?: 0L
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        RenewalEditScreen(
                            accountId = id,
                            viewModel = viewModel,
                            pausedAccountsViewModel = pausedAccountsViewModel,
                            onDone = { navController.popBackStack() },
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(Routes.NEW_PROFILES) {
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        NewProfilesScreen(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            onAccountClick = { id -> navController.navigate("${Routes.FORM}/$id") },
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(Routes.FORM_NEW) {
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        AccountFormScreen(
                            accountId = 0L,
                            prefill = viewModel.consumeSharedDraft(),
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            paymentViewModel = paymentViewModel,
                            onDone = { navController.popBackStack() },
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(
                        route = "${Routes.FORM}/{accountId}",
                        arguments = listOf(navArgument("accountId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("accountId") ?: 0L
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        AccountFormScreen(
                            accountId = id,
                            prefill = null,
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            paymentViewModel = paymentViewModel,
                            onDone = { navController.popBackStack() },
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(Routes.ADMIN) {
                        AdminScreen(
                            viewModel = licenseViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            appUpdateViewModel = appUpdateViewModel,
                            onOpenProfileDefaults = { navController.navigate(Routes.PROFILE_DEFAULTS) },
                            onOpenWhatsappMessage = { navController.navigate(Routes.WHATSAPP_MESSAGE) },
                            onOpenReminder = { navController.navigate(Routes.REMINDER_SETTINGS) },
                            onOpenBackup = { navController.navigate(Routes.BACKUP) },
                            onOpenPausedAccounts = { navController.navigate(Routes.PAUSED_ACCOUNTS) }
                        )
                    }
                    composable(Routes.PROFILE_DEFAULTS) {
                        ProfileDefaultsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.WHATSAPP_MESSAGE) {
                        WhatsappMessageScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.REMINDER_SETTINGS) {
                        ReminderSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.BACKUP) {
                        val isReadOnly = (licenseState as? LicenseState.Client)?.isReadOnly ?: false
                        BackupScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            isReadOnly = isReadOnly
                        )
                    }
                    composable(Routes.PAUSED_ACCOUNTS) {
                        PausedAccountsScreen(
                            viewModel = pausedAccountsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.SAVED_CREDENTIALS) {
                        SavedCredentialsScreen(
                            viewModel = savedCredentialsViewModel,
                            onBack = { navController.popBackStack() },
                            onUseCredential = { credential ->
                                viewModel.setSharedDraft(
                                    Account(email = credential.email, password = credential.password)
                                )
                                navController.navigate(Routes.FORM_NEW)
                            }
                        )
                    }
                }

                UpdateAvailableDialog(appUpdateViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_RENEWALS, false)) {
            pendingOpenRenewals = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Procesa un intent de entrada: texto plano compartido (ACTION_SEND, p. ej. desde
     * WhatsApp), un archivo .json compartido (ACTION_SEND) o abierto directamente
     * (ACTION_VIEW, p. ej. al tocar "Abrir" sobre un adjunto de WhatsApp).
     */
    private fun handleShareIntent(intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_VIEW) return
        val isReadOnly = (licenseViewModel.licenseState.value as? LicenseState.Client)?.isReadOnly ?: false
        if (isReadOnly) {
            Toast.makeText(this, R.string.license_readonly_banner, Toast.LENGTH_LONG).show()
            return
        }

        val streamUri = if (action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
        if (streamUri != null) {
            handleSharedFile(streamUri)
            return
        }

        if (action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val parsed = WhatsAppHelper.parseSharedText(text)
            if (parsed != null) {
                pendingShared = parsed
                Toast.makeText(this, R.string.imported_from_share, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Lee un archivo de respaldo (.json) compartido/abierto directamente. A diferencia
     * del texto suelto de WhatsApp, un archivo SIEMPRE debe ser el JSON de respaldo
     * completo: si no se puede parsear no hay que intentar leerlo como una cuenta
     * suelta (eso mezclaría datos de varias cuentas del archivo en una sola).
     */
    private fun handleSharedFile(uri: Uri) {
        lifecycleScope.launch {
            val backup = withContext(Dispatchers.IO) {
                val text = runCatching {
                    contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                }.getOrNull()
                text?.let { runCatching { AccountBackup.fromJson(it) }.getOrNull() }
            }
            if (backup != null) {
                viewModel.importAccounts(backup)
                Toast.makeText(this@MainActivity, R.string.import_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, R.string.import_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private object Routes {
        const val GATE = "gate"
        const val LIST = "list"
        const val FORM = "form"
        const val FORM_NEW = "form/new"
        const val ADMIN = "admin"
        const val NEW_PROFILES = "new_profiles"
        const val SETTINGS = "settings"
        const val PROFILE_DEFAULTS = "profile_defaults"
        const val WHATSAPP_MESSAGE = "whatsapp_message"
        const val REMINDER_SETTINGS = "reminder_settings"
        const val BACKUP = "backup"
        const val RENEWALS = "renewals"
        const val RENEWAL_EDIT = "renewal_edit"
        const val CLIENTS = "clients"
        const val INCOME = "income"
        const val SAVED_CREDENTIALS = "saved_credentials"
        const val PAUSED_ACCOUNTS = "paused_accounts"
        const val EMAIL_CHANGE_NOTICES = "email_change_notices"
    }

    companion object {
        const val EXTRA_OPEN_RENEWALS = "extra_open_renewals"
    }
}
