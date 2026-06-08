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

## Arquitectura

- **UI**: Jetpack Compose + Material 3, navegación con Navigation Compose.
- **Estado**: `AccountViewModel` (AndroidViewModel) expone un `StateFlow` con la
  lista de cuentas.
- **Persistencia**: Room (`Account`, `AccountDao`, `AppDatabase`,
  `AccountRepository`).
- **WhatsApp**: `util/WhatsAppHelper` arma y parsea el mensaje de texto.

```
app/src/main/java/com/dloren/mispantallas/
├── MainActivity.kt          # Navegación + manejo del intent de "Compartir"
├── data/                    # Room: entidad, DAO, base de datos, repositorio
├── ui/                      # Pantallas Compose + ViewModel + tema
└── util/WhatsAppHelper.kt   # Envío y parseo de mensajes de WhatsApp
```

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
