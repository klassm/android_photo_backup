package li.klass.photo_copy

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import li.klass.photo_copy.files.ptp.database.PtpItemDao
import li.klass.photo_copy.files.ptp.database.PtpItemEntity

@Database(entities = [PtpItemEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ptpItemDao(): PtpItemDao

    companion object {
        fun getInstance(applicationContext: Context): AppDatabase =
            Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "photocopy"
            ).build()
    }
}