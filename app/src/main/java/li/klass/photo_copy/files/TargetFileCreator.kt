package li.klass.photo_copy.files

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.createDirIfNotExists
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.model.FileContainer

class TargetFileCreator(
    private val usbService: UsbService,
    private val context: Context
) {
    private fun getCaptureDateFor(file: CopyableFile) = when (file) {
        is CopyableFile.PtpFile -> file.captureDate
        is CopyableFile.FileSystemFile -> usbService.captureDate(file)
    }?.toLocalDate()?.toString("yyyy-MM-dd") ?: "???"


    fun createTargetFileFor(
        targetRoot: DocumentFile,
        inFile: CopyableFile,
        extension: String = inFile.extension
    ): DocumentFile? {
        val baseDir = targetRoot.createDirIfNotExists(extension)
        val targetDirectory = baseDir.createDirIfNotExists(getCaptureDateFor(inFile))
        val targetFileName = inFile.filename.replace("${inFile.extension}$".toRegex(), extension)

        return targetDirectory.createFile(inFile.mimeType, targetFileName)
    }


    fun getTargetDirectory(target: FileContainer.TargetContainer): DocumentFile {
        return when (target) {
            is FileContainer.TargetContainer.TargetExternalDrive -> target.targetDirectory
            is FileContainer.TargetContainer.UnknownExternalDrive -> target.toTargetDrive(context).targetDirectory
        }
    }
}