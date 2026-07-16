package com.dloren.mispantallas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Account::class, SavedCredential::class, Payment::class, PausedAccount::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun savedCredentialDao(): SavedCredentialDao

    abstract fun paymentDao(): PaymentDao

    abstract fun pausedAccountDao(): PausedAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Agrega los campos de estado/renovación sin tocar las cuentas ya guardadas. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN status TEXT NOT NULL DEFAULT 'SOLD'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN soldDateMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN renewEveryDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN providerStartMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Agrega el precio del alquiler sin tocar las cuentas ya guardadas. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN price REAL NOT NULL DEFAULT 0")
            }
        }

        /** Agrega la tabla de correos guardados para reutilizar al crear cuentas nuevas. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_credentials` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`email` TEXT NOT NULL, " +
                        "`password` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL)"
                )
            }
        }

        /** Agrega la tabla de historial de pagos por cuenta. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`accountId` INTEGER NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_accountId` ON `payments` (`accountId`)")
            }
        }

        /** Agrega la tabla de cuentas en pausa (correo/contraseña guardados aparte). */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `paused_accounts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`email` TEXT NOT NULL, " +
                        "`password` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mis_pantallas.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
