package li.klass.photo_copy.files.usb

import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.model.FileContainer

class UsbService(private val exifDataProvider: FileSystemExifDataProvider) {
    fun listFiles(drive: FileContainer.SourceContainer.SourceExternalDrive) =
        drive.sourceDirectory.listAllFiles().map {
            CopyableFile.FileSystemFile(it, exifData = exifDataProvider.exifDataFor(it))
        }
}