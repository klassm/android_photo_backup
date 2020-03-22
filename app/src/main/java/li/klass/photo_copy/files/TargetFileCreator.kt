package li.klass.photo_copy.files

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.createDirIfNotExists
import li.klass.photo_copy.model.ExifData
import li.klass.photo_copy.model.FileContainer

class TargetFileCreator(
    private val context: Context
) {
    private fun getCaptureDateFor(exifData: ExifData) = exifData
        .captureDate
        ?.toLocalDate()?.toString("yyyy-MM-dd") ?: "???"

    fun createTargetFileFor(
        targetRoot: DocumentFile,
        inFile: CopyableFile,
        targetFileName: String,
        extension: String = inFile.extension,
        mimeType: String = inFile.mimeType
    ): DocumentFile? {
        val baseDir = targetRoot.createDirIfNotExists(extension)
        val exifData = inFile.exifData

        val targetDirectory = baseDir.createDirIfNotExists(getCaptureDateFor(exifData))
        return targetDirectory.createFile(mimeType, targetFileName)
    }

    fun getTargetDirectory(target: FileContainer.TargetContainer): DocumentFile = when (target) {
        is FileContainer.TargetContainer.TargetExternalDrive -> target.targetDirectory
        is FileContainer.TargetContainer.UnknownExternalDrive -> target.toTargetDrive(context).targetDirectory
    }
}