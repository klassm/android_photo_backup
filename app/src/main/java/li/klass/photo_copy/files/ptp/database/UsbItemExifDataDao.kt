package li.klass.photo_copy.files.ptp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsbItemExifDataDao {
    @Query("SELECT * FROM usbItemExifData WHERE identifier in (:identifiers)")
    fun getAllBy(identifiers: List<String>): List<UsbItemExifDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAll(entities: List<UsbItemExifDataEntity>)
}