package li.klass.photo_copy.files.ptp.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import java.security.MessageDigest

@Entity(tableName = "usbItemExifData", primaryKeys = ["identifier"])
data class UsbItemExifDataEntity(
    @ColumnInfo(name = "identifier") val identifier: String,
    @ColumnInfo(name = "model") val model: String?,
    @ColumnInfo(name = "captureDate") val captureDate: String
) {
    companion object {
        fun identifierFor(fileUri: Uri, lastModified: Long): String {
            val algorithm = MessageDigest.getInstance("SHA-512")
            val digest = algorithm.digest("${fileUri}_$lastModified".toByteArray(Charsets.UTF_8))
            return digest.fold("", { str, it -> str + "%02x".format(it) })
        }
    }
}