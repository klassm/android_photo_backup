package li.klass.photo_copy.files.ptp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PtpItemDao {
    @Query("SELECT * from ptpItem WHERE serialNumber=:serialNumber AND manufacturer=:manufacturer AND uid in (:uids)")
    fun getAllBy(uids: LongArray, serialNumber: String, manufacturer: String): List<PtpItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAll(entities: List<PtpItemEntity>)
}