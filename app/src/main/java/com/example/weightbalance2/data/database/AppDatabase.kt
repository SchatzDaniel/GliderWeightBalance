package com.example.weightbalance2.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.weightbalance2.data.dao.AircraftDao
import com.example.weightbalance2.data.dao.AircraftProfileDao
import com.example.weightbalance2.data.dao.PayloadStationDao
import com.example.weightbalance2.data.model.Aircraft
import com.example.weightbalance2.data.model.PayloadStation

@Database(
    entities = [Aircraft::class, PayloadStation::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aircraftDao(): AircraftDao
    abstract fun payloadStationDao(): PayloadStationDao
    abstract fun aircraftProfileDao(): AircraftProfileDao

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
                    .addMigrations(MIGRATION_1_2)
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
    }

}


