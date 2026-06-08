package com.dloren.mispantallas.data.util

import com.dloren.mispantallas.BuildConfig
import com.dloren.mispantallas.domain.util.VersionProvider

/** Implementación del proveedor de versión basada en BuildConfig. */
class BuildConfigVersionProvider : VersionProvider {
    override val versionCode: Int = BuildConfig.VERSION_CODE
}
