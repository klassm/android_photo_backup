package li.klass.photo_copy.files.usb

import android.content.ContentResolver
import android.media.ExifInterface
import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.model.FileContainer
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.io.FileDescriptor

class UsbService(private val contentResolver: ContentResolver) {
    fun listFiles(drive: FileContainer.TargetContainer) =
        when (drive) {
            is FileContainer.TargetContainer.TargetExternalDrive -> drive.targetDirectory.listAllFiles()
            is FileContainer.TargetContainer.UnknownExternalDrive -> emptyList()
        }.map {
            CopyableFile.FileSystemFile(it)
        }

    fun listFiles(drive: FileContainer.SourceContainer.SourceExternalDrive) =
        drive.sourceDirectory.listAllFiles().map {
            CopyableFile.FileSystemFile(it)
        }

    fun captureDate(file: CopyableFile.FileSystemFile): DateTime? {
        val fileDescriptor: FileDescriptor =
            contentResolver.openFileDescriptor(file.documentFile.uri, "r")
                ?.fileDescriptor!!
        val exif = ExifInterface(fileDescriptor)
        val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        return date?.let { exifDate.parseDateTime(it) }
    }

    companion object {
        val exifDate: DateTimeFormatter = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss")
    }
}