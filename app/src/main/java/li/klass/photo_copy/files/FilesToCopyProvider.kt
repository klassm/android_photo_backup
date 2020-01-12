package li.klass.photo_copy.files

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.files.ptp.PtpService
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.model.FileContainer
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourceExternalDrive
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourcePtp
import li.klass.photo_copy.model.FileContainer.TargetContainer.TargetExternalDrive

class FilesToCopyProvider(private val usbService: UsbService, private val ptpService: PtpService) {
    fun calculateFilesToCopy(
        target: FileContainer.TargetContainer,
        source: FileContainer.SourceContainer,
        transferListOnly: Boolean
    ): Collection<CopyableFile> {
        val targetDirectory = when (target) {
            is TargetExternalDrive -> target.targetDirectory
            else -> null
        }
        return calculateFilesToCopy(targetDirectory, source, transferListOnly)
    }

    fun calculateFilesToCopy(
        targetDirectory: DocumentFile?,
        source: FileContainer.SourceContainer,
        transferListOnly: Boolean
    ): Collection<CopyableFile> {
        val copyableFiles = when (source) {
            is SourceExternalDrive -> usbService.listFiles(source)
            is SourcePtp -> ptpService.getAvailableFiles(transferListOnly)
        } ?: emptyList()
        val allTargetFiles = targetDirectory?.listAllFiles() ?: emptyList()
        val allTargetFileNames = allTargetFiles.map { it.name }
        val toCopy = copyableFiles.filterNot { allTargetFileNames.contains(it.filename) }

        Log.i(
            logTag,
            "calculateFilesToCopy - targetDirectory=${targetDirectory?.uri?.path}" +
                    "\r\ncopyableFiles: " + copyableFiles.joinToString(separator = ",") { it.filename } +
                    "\r\nexisting files:" + allTargetFileNames.size +
                    "\r\ntoCopy: " + toCopy.joinToString(separator = ",") { it.filename })

        return toCopy
    }

    companion object {
        private val logTag = FilesToCopyProvider::class.java.name
    }
}