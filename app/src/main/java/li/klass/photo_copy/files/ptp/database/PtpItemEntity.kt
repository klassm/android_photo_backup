package li.klass.photo_copy.files.ptp.database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "ptpItem", primaryKeys = ["uid", "manufacturer", "serialNumber"])
data class PtpItemEntity(
    @ColumnInfo(name = "uid") val uid: Long,
    @ColumnInfo(name = "manufacturer") val manufacturer: String,
    @ColumnInfo(name = "serialNumber") val serialNumber: String,
    @ColumnInfo(name = "fileName") val fileName: String,
    @ColumnInfo(name = "captureDate") val captureDate: String,
    @ColumnInfo(name = "model") val model: String
)