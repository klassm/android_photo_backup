package li.klass.photo_copy.files

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.files.ptp.PtpFileProvider
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.model.FileContainer
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourceExternalDrive
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourcePtp
import li.klass.photo_copy.model.FileContainer.TargetContainer.TargetExternalDrive

class FilesToCopyProvider(private val usbService: UsbService, private val ptpFileProvider: PtpFileProvider) {
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

    private fun calculateFilesToCopy(
        targetDirectory: DocumentFile?,
        source: FileContainer.SourceContainer,
        transferListOnly: Boolean
    ): Collection<CopyableFile> {
        Log.i(logTag, "determining files to copy")
        val copyableFiles = when (source) {
            is SourceExternalDrive -> usbService.listFiles(source)
            is SourcePtp -> ptpFileProvider.getFilesFor(transferListOnly)
        }
        val copyableFileNames = copyableFiles.map { it.targetFileName }.toSet()

        Log.i(logTag, "finding existing files")
        val allTargetFiles = targetDirectory?.listAllFiles { file -> copyableFileNames.contains(file.name) }
            ?: emptyList()

        Log.i(logTag, "mapping target file names")
        val allTargetFileNames = allTargetFiles.map { it.name }

        Log.i(logTag, "removing existing files")
        val toCopy = copyableFiles.filterNot { allTargetFileNames.contains(it.targetFileName) }

        Log.i(
            logTag,
            "calculateFilesToCopy - targetDirectory=${targetDirectory?.uri?.path}" +
                    "\r\ncopyableFiles: " + copyableFiles.size +
                    "\r\nexisting files:" + allTargetFileNames.size +
                    "\r\ntoCopy: " + toCopy.size
        )

        return toCopy
    }

    companion object {
        private val logTag = FilesToCopyProvider::class.java.name
    }
}