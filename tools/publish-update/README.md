# Publicar una actualización

Automatiza: subir versión → compilar APK firmado → desplegar a Firebase Hosting → avisar a la app (Firestore).

Se usa Firebase Hosting (no Storage) para no depender del plan Blaze/tarjeta de crédito: Hosting
funciona en el plan gratuito Spark.

## Configuración (una sola vez)

1. Instalar dependencias:
   ```
   cd tools\publish-update
   npm install
   ```
2. Generar la clave de servicio de Firebase: consola de Firebase → ⚙️ **Configuración del proyecto** →
   pestaña **Cuentas de servicio** → **Generar nueva clave privada**. Se descarga un `.json`.
3. Guardar ese archivo como:
   ```
   C:\Users\<tu usuario>\keystores\mispantallas-firebase-adminsdk.json
   ```
   (o pasarle otra ruta con `--service-account`, o definir la variable de entorno
   `MISPANTALLAS_SERVICE_ACCOUNT`).

   **No subir nunca ese archivo a git** — da acceso administrador a Firestore.
4. Iniciar sesión con la CLI de Firebase (una sola vez, abre el navegador):
   ```
   firebase login
   ```

## Uso

Desde la raíz del proyecto:

```
node tools\publish-update\publish.js --version-name "1.1" --changelog "Corrige el recordatorio de vencimiento"
```

Esto:
- Sube `versionCode` (+1 automático) y `versionName` en `app/build.gradle.kts`.
- Corre `gradlew assembleRelease` (usa la keystore ya configurada en `~/.gradle/gradle.properties`).
- Despliega el APK a Firebase Hosting (`https://mispantallas.web.app/updates/mispantallas-v<versionCode>.apk`, público).
- Escribe `app_config/update` en Firestore con versionCode, versionName, apkUrl y changelog.

Cuando cada cliente abra o reabra la app, va a ver el aviso de actualización.

### Opciones útiles

- `--version-code 5` — fijar un número de versión en vez de autoincrementar.
- `--skip-build` — si ya compilaste el APK y solo querés resubirlo/actualizar Firestore.
