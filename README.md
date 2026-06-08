# Mis Pantallas

Aplicación Android (Kotlin + Jetpack Compose) para gestionar cuentas/perfiles de
plataformas de streaming en alquiler (Netflix, Disney+, etc.).

## Funcionalidades

- **Guardar datos de cuenta**: correo, contraseña, nombre de perfil, PIN,
  plataforma y duración del alquiler.
- **Cuenta regresiva**: cada cuenta muestra los días restantes hasta el
  vencimiento, con colores (verde / naranja / rojo) según cuánto falte.
- **Enviar por WhatsApp**: comparte los datos al cliente con un mensaje
  predeterminado. Si se carga el número del cliente abre el chat directo
  (`wa.me`), si no, abre el selector de WhatsApp.
- **Recibir por compartir**: desde WhatsApp (u otra app) podés usar "Compartir"
  hacia *Mis Pantallas* y la app intenta detectar los datos automáticamente y
  prellenar el formulario para guardarlos.

## Arquitectura (limpia, por capas)

La app sigue una arquitectura por capas con separación clara de responsabilidades
e inyección de dependencias **manual** (sin librerías de DI, para mantenerla liviana):

- **presentation**: Jetpack Compose + Material 3, `ViewModel`s con `UiState`
  (flujo de datos unidireccional), navegación con Navigation Compose y tema.
- **domain**: modelos puros, interfaces de repositorio y **casos de uso**. No
  depende de Android ni de Room (es Kotlin puro y testeable).
- **data**: Room (local), fuente remota de GitHub, implementaciones de los
  repositorios e infraestructura de Android (instalador de APK).

```
app/src/main/java/com/dloren/mispantallas/
├── MainActivity.kt              # Host de navegación + intent "Compartir"
├── MisPantallasApp.kt           # Application: crea el contenedor de DI
├── di/AppContainer.kt           # Inyección de dependencias manual
├── domain/                      # Kotlin puro (sin Android)
│   ├── model/                   # Account, AppRelease, UpdateResult
│   ├── repository/              # Interfaces (contratos)
│   ├── usecase/                 # Casos de uso (reglas de aplicación)
│   └── util/VersionProvider
├── data/                        # Implementaciones e infraestructura
│   ├── local/                   # Room: Entity, Dao, Database
│   ├── mapper/                  # Entity <-> modelo de dominio
│   ├── remote/                  # API de GitHub (releases)
│   ├── repository/              # Implementaciones de los repositorios
│   ├── installer/               # Descarga e instalación del APK
│   └── util/                    # VersionProvider basado en BuildConfig
└── presentation/                # UI (Compose)
    ├── list/                    # Pantalla de lista + ViewModel + UiState
    ├── form/                    # Pantalla de formulario + ViewModel + UiState
    ├── navigation/              # NavHost y rutas
    ├── whatsapp/                # Envío por WhatsApp (intents)
    └── theme/                   # Tema Material 3
```

> El flujo de dependencias va siempre hacia el dominio: `presentation -> domain`
> y `data -> domain`. El dominio no conoce a las otras capas.

## Cómo compilar

Requiere **Android Studio** (o el SDK de Android) y **JDK 17**.

1. Abrir el proyecto en Android Studio.
2. Dejar que sincronice Gradle.
3. Ejecutar en un emulador o dispositivo (minSdk 24, targetSdk 34).

Por línea de comandos:

```bash
./gradlew assembleDebug
```

> Nota: el formato del mensaje de WhatsApp usa pares `etiqueta: valor`. Eso hace
> que el mensaje sea legible para el cliente y, a la vez, permite que la app lo
> vuelva a interpretar cuando se comparte de regreso.

## Actualizaciones automáticas (sin Android Studio)

El proyecto compila el APK solo, mediante **GitHub Actions**, y lo publica como
**Release**. La app trae un botón **"Buscar actualizaciones"** (ícono de refrescar
en la barra superior) que descarga e instala la última versión.

### Cómo funciona

1. Cada `push` a `main` dispara el workflow `.github/workflows/build-apk.yml`.
2. El workflow compila un APK **firmado** (keystore fijo en `app/keystore/`) y crea
   un Release con tag `v1.0.<n>`, donde `<n>` es el `versionCode`.
3. En el teléfono, al tocar **Buscar actualizaciones**, la app consulta el último
   Release vía la API de GitHub, compara el `versionCode` y, si hay una versión
   nueva, descarga el APK y lanza el instalador.

### Instalación de la primera versión

Cuando termine el primer workflow, entrá a la pestaña **Releases** del repositorio
en GitHub desde el teléfono, descargá el `.apk` y abrilo para instalarlo. A partir
de ahí, las siguientes actualizaciones se hacen desde el botón dentro de la app.

> El teléfono pedirá permiso para **"instalar apps de orígenes desconocidos"** la
> primera vez. Es normal al instalar fuera de Play Store.

### ⚠️ Importante: el repositorio debe ser público

Para que la app pueda **descargar el APK sin pedir credenciales**, los Releases
tienen que ser accesibles públicamente, es decir, el repositorio debe ser
**público**.

- El código **no contiene datos sensibles**: las cuentas que cargás se guardan solo
  en tu teléfono (base de datos local), nunca en el repositorio.
- Si preferís mantenerlo **privado**, la descarga automática no funcionará sin
  incrustar un token de GitHub en la app (no recomendado). En ese caso conviene
  publicar los APK en un repositorio público aparte solo para releases.

### Firma

Se incluye un keystore (`app/keystore/mispantallas.jks`) para que todas las builds
tengan la **misma firma** y las actualizaciones se instalen sobre la versión
anterior. Es una firma de uso personal para distribución por fuera de Play Store;
no la uses para publicar en Google Play.

