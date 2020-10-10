package li.klass.photo_copy.files.usb

import android.content.ContentResolver
import android.media.ExifInterface
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.model.ExifData
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class FileSystemExifDataProvider(private val contentResolver: ContentResolver) {

    fun exifDataFor(file: DocumentFile): ExifData =
        contentResolver.openFileDescriptor(file.uri, "r").use { fileDescriptor ->
            val exif = ExifInterface(fileDescriptor?.fileDescriptor!!)
            val captureDate = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?.let { exifDate.parseDateTime(it) }
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.replace("NIKON ", "")

            ExifData(
                captureDate = captureDate,
                model = model
            )
        }

    companion object {
        val exifDate: DateTimeFormatter = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss")
    }
}