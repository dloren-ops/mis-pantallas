package com.dloren.mispantallas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.dloren.mispantallas.presentation.navigation.AppNavHost
import com.dloren.mispantallas.presentation.navigation.Routes
import com.dloren.mispantallas.presentation.theme.MisPantallasTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as MisPantallasApp).container }

    // Señal de que llegó un "Compartir" pendiente de abrir en el formulario.
    private var shareTrigger by mutableStateOf(0)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sin acción */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        handleShareIntent(intent)

        setContent {
            MisPantallasTheme {
                val navController = rememberNavController()

                // Cuando llega un borrador compartido, navegamos al formulario nuevo.
                LaunchedEffect(shareTrigger) {
                    if (shareTrigger > 0) {
                        navController.navigate(Routes.FORM_NEW)
                    }
                }

                AppNavHost(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** Procesa un ACTION_SEND de texto plano (compartir desde WhatsApp). */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val parsed = container.parseSharedAccount(text)
            if (parsed != null) {
                container.setSharedDraft(parsed)
                shareTrigger++
                Toast.makeText(this, R.string.imported_from_share, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Pide el permiso de notificaciones en Android 13+ si aún no se concedió. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            val granted = checkSelfPermission(permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(permission)
            }
        }
    }
}
