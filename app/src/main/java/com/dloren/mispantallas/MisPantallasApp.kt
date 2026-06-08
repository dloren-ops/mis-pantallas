package com.dloren.mispantallas

import android.app.Application
import com.dloren.mispantallas.di.AppContainer

/** Clase Application: punto único donde vive el contenedor de dependencias. */
class MisPantallasApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
