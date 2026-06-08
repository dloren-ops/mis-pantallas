plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dloren.mispantallas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dloren.mispantallas"
        minSdk = 24
        targetSdk = 34
        // versionCode se puede sobrescribir desde CI con -PversionCode=<n>
        // (usamos el numero de ejecucion de GitHub Actions para que siempre suba).
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Datos del repositorio para el buscador de actualizaciones.
        buildConfigField("String", "GITHUB_OWNER", "\"dloren-ops\"")
        buildConfigField("String", "GITHUB_REPO", "\"mis-pantallas\"")
    }

    signingConfigs {
        create("release") {
            // Keystore versionado en el repo para que todas las builds (locales y
            // de CI) tengan la MISMA firma y las actualizaciones se instalen encima.
            storeFile = file("keystore/mispantallas.jks")
            storePassword = "mispantallas"
            keyAlias = "mispantallas"
            keyPassword = "mispantallas"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Misma firma que release para poder actualizar entre ambas.
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // No abortar la build de release por advertencias de lint en CI.
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
}
