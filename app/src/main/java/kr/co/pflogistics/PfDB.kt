package kr.co.pflogistics

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Data::class], version = 1)
abstract class PfDB : RoomDatabase() {
    abstract fun dao(): DataDao

    companion object {
        private var INSTANCE: PfDB? = null

        fun getInstance(context: Context): PfDB? {
            if (INSTANCE == null) {
                synchronized(PfDB::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        PfDB::class.java,
                        "pf.db"
                    )
                    .allowMainThreadQueries()
                    .build()
                }
            }
            return INSTANCE
        }
    }
}