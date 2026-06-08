package com.dloren.mispantallas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dloren.mispantallas.data.Account
import com.dloren.mispantallas.ui.AccountFormScreen
import com.dloren.mispantallas.ui.AccountListScreen
import com.dloren.mispantallas.ui.AccountViewModel
import com.dloren.mispantallas.ui.theme.MisPantallasTheme
import com.dloren.mispantallas.util.WhatsAppHelper

class MainActivity : ComponentActivity() {

    private val viewModel: AccountViewModel by viewModels { AccountViewModel.Factory }

    // Cuenta recibida desde un "Compartir" de WhatsApp, pendiente de mostrar.
    private var pendingShared by mutableStateOf<Account?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            MisPantallasTheme {
                val navController = rememberNavController()

                // Si llegó algo por "Compartir", navegamos al formulario prellenado.
                LaunchedEffect(pendingShared) {
                    val shared = pendingShared
                    if (shared != null) {
                        viewModel.setSharedDraft(shared)
                        pendingShared = null
                        navController.navigate(Routes.FORM_NEW)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.LIST
                ) {
                    composable(Routes.LIST) {
                        AccountListScreen(
                            viewModel = viewModel,
                            onAddAccount = { navController.navigate(Routes.FORM_NEW) },
                            onAccountClick = { id ->
                                navController.navigate("${Routes.FORM}/$id")
                            }
                        )
                    }
                    composable(Routes.FORM_NEW) {
                        AccountFormScreen(
                            accountId = 0L,
                            prefill = viewModel.consumeSharedDraft(),
                            viewModel = viewModel,
                            onDone = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "${Routes.FORM}/{accountId}",
                        arguments = listOf(navArgument("accountId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getLong("accountId") ?: 0L
                        AccountFormScreen(
                            accountId = id,
                            prefill = null,
                            viewModel = viewModel,
                            onDone = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** Procesa un intent ACTION_SEND con texto plano (compartir desde WhatsApp). */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val parsed = WhatsAppHelper.parseSharedText(text)
            if (parsed != null) {
                pendingShared = parsed
                Toast.makeText(this, R.string.imported_from_share, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private object Routes {
        const val LIST = "list"
        const val FORM = "form"
        const val FORM_NEW = "form/new"
    }
}
