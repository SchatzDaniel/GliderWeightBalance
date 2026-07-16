package com.danielschatz.gliderweightbalance.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.danielschatz.gliderweightbalance.data.dao.AircraftDao
import com.danielschatz.gliderweightbalance.data.dao.AircraftProfileDao
import com.danielschatz.gliderweightbalance.data.dao.PayloadStationDao
import com.danielschatz.gliderweightbalance.data.dao.PresetDao
import com.danielschatz.gliderweightbalance.data.dao.ScenarioDao
import com.danielschatz.gliderweightbalance.data.model.Aircraft
import com.danielschatz.gliderweightbalance.data.model.PayloadStation
import com.danielschatz.gliderweightbalance.data.model.Preset
import com.danielschatz.gliderweightbalance.data.model.Scenario
import com.danielschatz.gliderweightbalance.data.model.ScenarioEntry

@Database(
    entities = [Aircraft::class, PayloadStation::class, Preset::class, Scenario::class, ScenarioEntry::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aircraftDao(): AircraftDao
    abstract fun payloadStationDao(): PayloadStationDao
    abstract fun aircraftProfileDao(): AircraftProfileDao
    abstract fun presetDao(): PresetDao
    abstract fun scenarioDao(): ScenarioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aircraft_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
            CREATE TABLE payload_stations (
                stationId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                aircraftOwnerId INTEGER NOT NULL,
                name TEXT NOT NULL,
                arm REAL NOT NULL,
                maxMass REAL
            )
        """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fügt die neue Spalte zur bestehenden Tabelle hinzu
                // WICHTIG: Prüfe ob deine Tabelle "PayloadStation" oder "payload_stations" heißt!
                // Laut deinem MIGRATION_1_2 Code heißt sie "payload_stations"
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fügt die Spalte isNonLifting hinzu.
                // In SQLite gibt es kein Boolean, daher nutzen wir INTEGER (0 = false, 1 = true).
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN isNonLifting INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN defaultValue REAL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Neue Spalten zur Tabelle payload_stations hinzufügen
                // In SQLite gibt es kein Boolean, daher INTEGER (0=false, 1=true)
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN hasSlider INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN hasPresets INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN hasAmountInput INTEGER NOT NULL DEFAULT 0")

                // 2. Neue Tabelle für Presets erstellen
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS station_presets (
                        presetId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        parentStationId INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        weight REAL NOT NULL,
                        FOREIGN KEY(parentStationId) REFERENCES payload_stations(stationId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // Index für Performance und Fremdschlüssel-Effizienz
                db.execSQL("CREATE INDEX IF NOT EXISTS index_station_presets_parentStationId ON station_presets (parentStationId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fügt die Spalte für das gewählte Preset-Label hinzu (Nullable TEXT)
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN selectedPresetLabel TEXT")

                // Fügt die Spalte für die Anzahl hinzu (INTEGER mit Default 1)
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN amount INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN fluidType TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE aircraft_table ADD COLUMN wingArea REAL")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN isConsumable INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scenarios (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        aircraftId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        FOREIGN KEY(aircraftId) REFERENCES aircraft_table(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scenarios_aircraftId ON scenarios (aircraftId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scenario_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scenarioId INTEGER NOT NULL,
                        stationId INTEGER NOT NULL,
                        value REAL,
                        selectedPresetLabel TEXT,
                        amount INTEGER NOT NULL,
                        FOREIGN KEY(scenarioId) REFERENCES scenarios(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(stationId) REFERENCES payload_stations(stationId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_entries_scenarioId ON scenario_entries (scenarioId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_entries_stationId ON scenario_entries (stationId)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN dumpTime REAL")
                db.execSQL("ALTER TABLE payload_stations ADD COLUMN couplingGroupId INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

}


