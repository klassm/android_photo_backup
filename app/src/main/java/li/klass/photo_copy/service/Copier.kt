package li.klass.photo_copy.service

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import arrow.core.Either
import arrow.core.continuations.either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.klass.photo_copy.Constants.prefVerifyMd5HashOfCopiedFiles
import li.klass.photo_copy.files.*
import li.klass.photo_copy.md5Hash
import li.klass.photo_copy.model.FileContainer.TargetContainer

enum class CopyResult {
    SUCCESS,
    COPY_FAILURE,
    TARGET_FILE_CREATION_FAILED,
    JPG_CREATION_FOR_NEF_FAILED,
    JPG_COULD_NOT_READ_NEF_INPUT_FILE,
    JPG_COULD_NOT_EXTRACT_JPG_FROM_NEF,
    INTEGRITY_CHECK_FAILED,
    ERROR
}

interface CopyListener {
    fun onFileFinished(
        copiedFileIndex: Int,
        totalNumberOfFiles: Int,
        copiedFile: CopyableFile,
        copyResult: CopyResult
    )

    fun onCopyStarted(totalNumberOfFiles: Int)
}

class Copier(
    private val context: Context,
    private val fileCopier: FileCopier,
    private val jpgFromNefExtractor: JpgFromNefExtractor,
    private val targetFileCreator: TargetFileCreator
) {
    suspend fun copy(
        target: TargetContainer,
        listener: CopyListener,
        toCopy: List<CopyableFile>
    ) = withContext(Dispatchers.IO) {
        val targetDirectory = targetFileCreator.getTargetDirectory(target)
        listener.onCopyStarted(toCopy.size)

        toCopy.forEachIndexed { index, file ->
            val result = copy(file, targetDirectory)
            listener.onFileFinished(index + 1, toCopy.size, file, result)
        }
    }

    private fun copy(toCopy: CopyableFile, targetRoot: DocumentFile): CopyResult {
        return try {
            either.eager<CopyResult, CopyResult> {
                val targetFile = fileCopier.copy(toCopy, targetRoot).bind()
                verifyHash(toCopy, targetFile)
                jpgFromNefExtractor.extractTargetFileFrom(
                    CopyableFile.FileSystemFile(
                        targetFile,
                        toCopy.exifData
                    ), targetRoot
                )

                CopyResult.SUCCESS
            }.fold({ it }, { it })
        } catch (e: Exception) {
            Log.e(
                logTag,
                "copy(toCopy=${toCopy.filename},targetRoot=${targetRoot.name}) - failed to copy due to ${e.message}",
                e
            )
            CopyResult.ERROR
        }
    }

    private fun verifyHash(
        toCopy: CopyableFile,
        newFile: DocumentFile
    ): Either<CopyResult, CopyResult> {
        val shouldVerifyMd5Hash = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            prefVerifyMd5HashOfCopiedFiles, true
        )
        if (toCopy is CopyableFile.FileSystemFile
            && shouldVerifyMd5Hash
            && contentResolver.md5Hash(toCopy.documentFile) != contentResolver.md5Hash(newFile)
        ) {
            Log.e(
                logTag,
                "copy(toCopy=${toCopy.filename},newFile=${newFile.name}) - Deleting ${newFile.uri}, md5 hash did not match"
            )
            newFile.delete()
            return Either.Right(CopyResult.INTEGRITY_CHECK_FAILED)
        }
        return Either.Left(CopyResult.SUCCESS)
    }

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    companion object {
        val logTag: String = Copier::class.java.name
    }
}