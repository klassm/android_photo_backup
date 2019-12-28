package li.klass.photo_copy.files

import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.files.ptp.PtpService
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.model.FileContainer
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourceExternalDrive
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourcePtp

class FilesToCopyProvider(private val usbService: UsbService, private val ptpService: PtpService) {
    fun calculateFilesToCopy(
        targetDirectory: DocumentFile,
        source: FileContainer.SourceContainer
    ): Collection<CopyableFile> {
        val copyableFiles = when (source) {
            is SourceExternalDrive -> usbService.listFiles(source)
            is SourcePtp -> ptpService.getAvailableFiles()
        }?: emptyList()

        val allTargetFiles = targetDirectory.listAllFiles()
        val allTargetFileNames = allTargetFiles.map { it.name }
        return copyableFiles.filterNot { allTargetFileNames.contains(it.filename) }
    }
}