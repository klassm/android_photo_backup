package li.klass.photo_copy.files

import android.content.ContentResolver
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import li.klass.photo_copy.files.ptp.PtpFileProvider
import li.klass.photo_copy.service.CopyResult

class FileCopier(
    private val contentResolver: ContentResolver,
    private val ptpFileProvider: PtpFileProvider,
    private val targetFileCreator: TargetFileCreator
) {

    fun copy(from: CopyableFile, targetDirectory: DocumentFile): Either<CopyResult, DocumentFile> {
        val targetFile =
            targetFileCreator.createTargetFileFor(targetDirectory, from, from.targetFileName)
                ?: return Either.Left(CopyResult.TARGET_FILE_CREATION_FAILED)
        return copyToFile(from, targetFile)
    }

    private fun copyToFile(from: CopyableFile, to: DocumentFile): Either<CopyResult, DocumentFile> =
        try {
            read(from)?.let {
                contentResolver.openOutputStream(to.uri).use { targetStream ->
                    targetStream?.write(it)
                }
                Either.Right(to)
            } ?: Either.Left(CopyResult.COPY_FAILURE)
        } catch (e: Exception) {
            Log.e(logTag, "could not copy - ${e.message}", e)
            Either.Left(CopyResult.COPY_FAILURE)
        }

    private fun read(file: CopyableFile): ByteArray? =
        when (file) {
            is CopyableFile.PtpFile -> ptpFileProvider.fetchFile(file)
            is CopyableFile.FileSystemFile -> contentResolver.openInputStream(file.documentFile.uri).use { it?.readBytes() }
        }

    companion object {
        private val logTag = FileCopier::class.java.name
    }
}