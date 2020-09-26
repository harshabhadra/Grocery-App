package com.a99Spicy.a99spicy.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DatabaseProduct::class, DatabaseProductCategory::class,
        DatabaseCart::class, DatabaseShipping::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(DataConverter::class)
abstract class MyDatabase : RoomDatabase() {

    abstract val productDao: ProductDao
    abstract val categoryDao: CategoryDao
    abstract val cartDao: CartDao
    abstract val addressDao:AddressDao

    companion object {
        @Volatile
        private var INSTANCE: MyDatabase? = null

        fun getDatabase(context: Context): MyDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        MyDatabase::class.java,
                        "99spicy_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}