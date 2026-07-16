#!/usr/bin/env node
/**
 * Publica una actualización de Mis Pantallas de punta a punta:
 *   1. Sube versionCode/versionName en app/build.gradle.kts
 *   2. Compila el APK de release firmado (gradlew assembleRelease)
 *   3. Lo despliega a Firebase Hosting (no requiere plan Blaze/tarjeta)
 *   4. Actualiza el documento app_config/update en Firestore
 *
 * Uso:
 *   node publish.js --version-name "1.1" --changelog "Corrige el recordatorio de vencimiento"
 *
 * Opciones:
 *   --version-name <texto>   Requerido. Nombre de versión visible (ej: "1.1").
 *   --changelog <texto>      Requerido. Qué cambió, se muestra en la app.
 *   --version-code <numero>  Opcional. Si no se pasa, se autoincrementa +1.
 *   --service-account <ruta> Opcional. Por defecto usa la variable de entorno
 *                             MISPANTALLAS_SERVICE_ACCOUNT o
 *                             C:\\Users\\<usuario>\\keystores\\mispantallas-firebase-adminsdk.json
 *   --skip-build              Salta el paso de compilar (usa el APK ya generado).
 */

const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const TOOL_DIR = __dirname;
const BUILD_GRADLE_PATH = path.join(REPO_ROOT, "app", "build.gradle.kts");
const APK_PATH = path.join(
  REPO_ROOT,
  "app",
  "build",
  "outputs",
  "apk",
  "release",
  "app-release.apk"
);

// Carpeta pública que se despliega a Firebase Hosting (ver tools/publish-update/firebase.json).
// Firebase Hosting no requiere plan Blaze/tarjeta, a diferencia de Firebase Storage.
const HOSTING_PUBLIC_DIR = path.join(TOOL_DIR, "hosting_public");
const HOSTING_SITE_URL = "https://mispantallas.web.app";

function parseArgs(argv) {
  const args = { skipBuild: false };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    switch (arg) {
      case "--version-name":
        args.versionName = argv[++i];
        break;
      case "--version-code":
        args.versionCode = parseInt(argv[++i], 10);
        break;
      case "--changelog":
        args.changelog = argv[++i];
        break;
      case "--service-account":
        args.serviceAccount = argv[++i];
        break;
      case "--skip-build":
        args.skipBuild = true;
        break;
      default:
        console.warn(`Argumento desconocido, se ignora: ${arg}`);
    }
  }
  return args;
}

function fail(message) {
  console.error(`\n❌ ${message}\n`);
  process.exit(1);
}

function readBuildGradle() {
  return fs.readFileSync(BUILD_GRADLE_PATH, "utf8");
}

function currentVersionCode(content) {
  const match = content.match(/versionCode\s*=\s*(\d+)/);
  if (!match) fail("No se encontró versionCode en app/build.gradle.kts");
  return parseInt(match[1], 10);
}

function writeVersion(content, versionCode, versionName) {
  let updated = content.replace(/versionCode\s*=\s*\d+/, `versionCode = ${versionCode}`);
  updated = updated.replace(/versionName\s*=\s*"[^"]*"/, `versionName = "${versionName}"`);
  fs.writeFileSync(BUILD_GRADLE_PATH, updated, "utf8");
}

function resolveServiceAccountPath(args) {
  const fromArg = args.serviceAccount;
  const fromEnv = process.env.MISPANTALLAS_SERVICE_ACCOUNT;
  const fallback = path.join(os.homedir(), "keystores", "mispantallas-firebase-adminsdk.json");
  const resolved = fromArg || fromEnv || fallback;
  if (!fs.existsSync(resolved)) {
    fail(
      `No encontré la clave de servicio de Firebase en:\n  ${resolved}\n\n` +
        "Generála desde la consola de Firebase (⚙️ Configuración del proyecto → " +
        "Cuentas de servicio → Generar nueva clave privada) y guardala en esa ruta " +
        "(o pasá --service-account <ruta> / seteá MISPANTALLAS_SERVICE_ACCOUNT)."
    );
  }
  return resolved;
}

function buildRelease() {
  console.log("\n📦 Compilando APK de release firmado (gradlew assembleRelease)...\n");
  const gradlew = path.join(REPO_ROOT, "gradlew.bat");
  execFileSync(gradlew, ["assembleRelease"], {
    cwd: REPO_ROOT,
    stdio: "inherit",
    shell: true
  });
  if (!fs.existsSync(APK_PATH)) {
    fail(`La build terminó pero no encontré el APK esperado en:\n  ${APK_PATH}`);
  }
}

function deployToHosting(versionCode) {
  // Firebase Hosting rechaza extensiones "ejecutables" (incluida .apk) en el plan Spark
  // gratuito. La app descarga el archivo como bytes crudos y lo guarda localmente con su
  // propio nombre (ver ApkInstaller.downloadApk), así que la extensión remota no importa.
  const relativeApkPath = path.join("updates", `mispantallas-v${versionCode}.dat`);
  const destination = path.join(HOSTING_PUBLIC_DIR, relativeApkPath);
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.copyFileSync(APK_PATH, destination);

  console.log(`\n☁️  Desplegando a Firebase Hosting (${relativeApkPath})...\n`);
  const firebaseCli = process.platform === "win32" ? "firebase.cmd" : "firebase";
  execFileSync(firebaseCli, ["deploy", "--only", "hosting", "--project", "mispantallas"], {
    cwd: TOOL_DIR,
    stdio: "inherit",
    shell: true
  });

  return `${HOSTING_SITE_URL}/${relativeApkPath.replace(/\\/g, "/")}`;
}

async function uploadAndPublish({ versionCode, versionName, changelog, serviceAccountPath, apkUrl }) {
  const admin = require("firebase-admin");
  const serviceAccount = require(serviceAccountPath);

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });

  console.log("\n📝 Actualizando app_config/update en Firestore...\n");
  const db = admin.firestore();
  await db.collection("app_config").doc("update").set(
    {
      versionCode,
      versionName,
      apkUrl,
      changelog,
      publishedAt: admin.firestore.FieldValue.serverTimestamp()
    },
    { merge: true }
  );

  console.log("✅ Listo. Resumen:");
  console.log(`   versionCode: ${versionCode}`);
  console.log(`   versionName: ${versionName}`);
  console.log(`   apkUrl:      ${apkUrl}`);
  console.log(`   changelog:   ${changelog}`);
  console.log("\nLos clientes van a ver el aviso la próxima vez que abran o reabran la app.\n");
}

async function main() {
  const args = parseArgs(process.argv.slice(2));

  if (!args.versionName) fail("Falta --version-name (ej: --version-name \"1.1\")");
  if (!args.changelog) fail("Falta --changelog (ej: --changelog \"Corrige tal cosa\")");

  const serviceAccountPath = resolveServiceAccountPath(args);

  const content = readBuildGradle();
  const newVersionCode = args.versionCode || currentVersionCode(content) + 1;

  console.log(`\nPublicando Mis Pantallas v${args.versionName} (versionCode ${newVersionCode})`);

  writeVersion(content, newVersionCode, args.versionName);
  console.log("✔️  app/build.gradle.kts actualizado.");

  if (!args.skipBuild) {
    buildRelease();
  } else {
    console.log("⏭️  Se salteó la compilación (--skip-build). Usando el APK ya existente.");
    if (!fs.existsSync(APK_PATH)) {
      fail(`No existe un APK previo en:\n  ${APK_PATH}\nQuitá --skip-build para compilarlo.`);
    }
  }

  const apkUrl = deployToHosting(newVersionCode);

  await uploadAndPublish({
    versionCode: newVersionCode,
    versionName: args.versionName,
    changelog: args.changelog,
    serviceAccountPath,
    apkUrl
  });
}

main().catch((error) => {
  fail(error.message || String(error));
});
