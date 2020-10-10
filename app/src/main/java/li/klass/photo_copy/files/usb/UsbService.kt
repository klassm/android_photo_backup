package li.klass.photo_copy.files.usb

import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.model.FileContainer

class UsbService(private val fileSystemFileCreator: FileSystemFileCreator) {
    fun listFiles(drive: FileContainer.SourceContainer.SourceExternalDrive): List<CopyableFile.FileSystemFile> {
        val allFiles = drive.sourceDirectory.listAllFiles()
        return fileSystemFileCreator.fileSystemFilesFor(allFiles)
    }
}