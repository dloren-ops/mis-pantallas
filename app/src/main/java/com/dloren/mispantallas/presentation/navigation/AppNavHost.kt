package com.dloren.mispantallas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dloren.mispantallas.presentation.form.AccountFormScreen
import com.dloren.mispantallas.presentation.list.AccountListScreen
import com.dloren.mispantallas.presentation.renewals.RenewalsScreen

/** Rutas de navegación de la app. */
object Routes {
    const val LIST = "list"
    const val FORM = "form"
    const val FORM_NEW = "form/new"
    const val RENEWALS = "renewals"
    const val ARG_ACCOUNT_ID = "accountId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            AccountListScreen(
                onAddAccount = { navController.navigate(Routes.FORM_NEW) },
                onAccountClick = { id -> navController.navigate("${Routes.FORM}/$id") },
                onOpenRenewals = { navController.navigate(Routes.RENEWALS) }
            )
        }

        composable(Routes.RENEWALS) {
            RenewalsScreen(
                onBack = { navController.popBackStack() },
                onAccountClick = { id -> navController.navigate("${Routes.FORM}/$id") }
            )
        }

        // Alta de cuenta (id = 0).
        composable(Routes.FORM_NEW) {
            AccountFormScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // Edición de cuenta existente.
        composable(
            route = "${Routes.FORM}/{${Routes.ARG_ACCOUNT_ID}}",
            arguments = listOf(navArgument(Routes.ARG_ACCOUNT_ID) { type = NavType.LongType })
        ) {
            AccountFormScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
