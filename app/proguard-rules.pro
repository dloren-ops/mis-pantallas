# Reglas de R8/ProGuard para Mis Pantallas.
# Las librerías (Room, Compose, Navigation, Coroutines) ya traen sus propias
# reglas "consumer"; aquí agregamos protecciones puntuales del proyecto.

# Mantener números de línea para que los reportes de error sean legibles.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Entidades de Room: la librería accede a estos campos por su código generado.
-keep class com.dloren.mispantallas.data.local.** { *; }

# Workers de WorkManager (instanciados por reflexión).
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Modelos de dominio (incluye los que se construyen al parsear JSON de GitHub).
-keep class com.dloren.mispantallas.domain.model.** { *; }

# org.json forma parte del framework de Android (no requiere reglas), pero
# evitamos advertencias por si alguna dependencia lo referencia.
-dontwarn org.json.**
