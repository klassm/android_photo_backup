package li.klass.photo_copy.files.usb

import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.model.FileContainer
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class UsbService {
    fun listFiles(drive: FileContainer.SourceContainer.SourceExternalDrive) =
        drive.sourceDirectory.listAllFiles().map {
            CopyableFile.FileSystemFile(it)
        }

    companion object {
        val exifDate: DateTimeFormatter = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss")
    }
}