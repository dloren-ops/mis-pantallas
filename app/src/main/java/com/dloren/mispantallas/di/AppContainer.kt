package com.dloren.mispantallas.di

import android.content.Context
import com.dloren.mispantallas.BuildConfig
import com.dloren.mispantallas.data.installer.ApkInstaller
import com.dloren.mispantallas.data.local.AppDatabase
import com.dloren.mispantallas.data.remote.GithubReleaseDataSource
import com.dloren.mispantallas.data.reminder.WorkManagerReminderScheduler
import com.dloren.mispantallas.data.repository.AccountRepositoryImpl
import com.dloren.mispantallas.data.repository.UpdateRepositoryImpl
import com.dloren.mispantallas.data.util.BuildConfigVersionProvider
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.reminder.ReminderScheduler
import com.dloren.mispantallas.domain.repository.AccountRepository
import com.dloren.mispantallas.domain.repository.UpdateRepository
import com.dloren.mispantallas.domain.usecase.CheckForUpdateUseCase
import com.dloren.mispantallas.domain.usecase.DeleteAccountUseCase
import com.dloren.mispantallas.domain.usecase.GetAccountUseCase
import com.dloren.mispantallas.domain.usecase.MarkAsSoldUseCase
import com.dloren.mispantallas.domain.usecase.ObserveAccountsUseCase
import com.dloren.mispantallas.domain.usecase.ParseSharedAccountUseCase
import com.dloren.mispantallas.domain.usecase.SaveAccountUseCase
import com.dloren.mispantallas.domain.util.VersionProvider

/**
 * Contenedor de inyección de dependencias manual (sin librerías externas, para
 * mantener la app liviana y el arranque rápido).
 *
 * Construye y cablea las dependencias de cada capa: data -> domain -> presentation.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // --- Data ---
    private val database: AppDatabase = AppDatabase.getInstance(appContext)

    private val accountRepository: AccountRepository =
        AccountRepositoryImpl(database.accountDao())

    private val updateRepository: UpdateRepository = UpdateRepositoryImpl(
        GithubReleaseDataSource(BuildConfig.GITHUB_OWNER, BuildConfig.GITHUB_REPO)
    )

    private val versionProvider: VersionProvider = BuildConfigVersionProvider()

    private val reminderScheduler: ReminderScheduler = WorkManagerReminderScheduler(appContext)

    /** Infraestructura Android para actualizar la app. */
    val apkInstaller: ApkInstaller = ApkInstaller(appContext)

    // --- Domain (casos de uso) ---
    val observeAccounts = ObserveAccountsUseCase(accountRepository)
    val getAccount = GetAccountUseCase(accountRepository)
    val saveAccount = SaveAccountUseCase(accountRepository, reminderScheduler)
    val deleteAccount = DeleteAccountUseCase(accountRepository, reminderScheduler)
    val markAsSold = MarkAsSoldUseCase(accountRepository, reminderScheduler)
    val parseSharedAccount = ParseSharedAccountUseCase()
    val checkForUpdate = CheckForUpdateUseCase(updateRepository, versionProvider)

    /**
     * Borrador recibido por "Compartir" (WhatsApp) pendiente de mostrar en el
     * formulario. Se consume una sola vez.
     */
    @Volatile
    private var sharedDraft: Account? = null

    fun setSharedDraft(account: Account?) {
        sharedDraft = account
    }

    fun consumeSharedDraft(): Account? {
        val draft = sharedDraft
        sharedDraft = null
        return draft
    }
}
