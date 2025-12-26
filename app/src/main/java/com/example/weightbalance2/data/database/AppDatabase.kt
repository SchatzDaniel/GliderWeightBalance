package com.example.weightbalance2.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weightbalance2.data.dao.AircraftDao
import com.example.weightbalance2.data.model.Aircraft

@Database(entities = [Aircraft::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun aircraftDao(): AircraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aircraft_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
