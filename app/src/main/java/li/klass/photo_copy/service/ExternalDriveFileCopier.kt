package li.klass.photo_copy.service

import android.content.ContentResolver
import android.content.Context
import android.media.ExifInterface
import android.media.ExifInterface.TAG_DATETIME_ORIGINAL
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.klass.photo_copy.Constants.prefVerifyMd5HashOfCopiedFiles
import li.klass.photo_copy.createDirIfNotExists
import li.klass.photo_copy.listAllFiles
import li.klass.photo_copy.md5Hash
import li.klass.photo_copy.model.ExternalDriveDocument
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive.TargetExternalDrive
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive.UnknownExternalDrive
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.io.FileDescriptor
import java.lang.Exception
import java.util.*

enum class CopyResult {
    SUCCESS, COPY_FAILURE, INTEGRITY_CHECK_FAILED, ERROR
}

interface CopyListener {
    fun onFileFinished(copiedFileIndex: Int, totalNumberOfFiles: Int, copiedFile: DocumentFile, copyResult: CopyResult)
    fun onCopyStarted(totalNumberOfFiles: Int)
}

class ExternalDriveFileCopier(private val context: Context) {

    suspend fun getFilesToCopy(
        source: ExternalDriveDocument.SourceExternalDrive,
        target: PossibleTargetExternalDrive
    ): List<DocumentFile> = withContext(Dispatchers.IO) {
        val allSourceFiles = source.sourceDirectory.listAllFiles()
        val allTargetFiles = when (target) {
            is TargetExternalDrive -> target.targetDirectory.listAllFiles()
            is UnknownExternalDrive -> emptyList()
        }
        val allTargetFileNames = allTargetFiles.map { it.name }
        allSourceFiles.filterNot { allTargetFileNames.contains(it.name) }
    }

    suspend fun copy(
        source: ExternalDriveDocument.SourceExternalDrive,
        target: PossibleTargetExternalDrive,
        listener: CopyListener
    ) = withContext(Dispatchers.IO) {
        val targetFile = getTargetDirectory(target)
        val toCopy = getFilesToCopy(source, target)
        listener.onCopyStarted(toCopy.size)

        toCopy.forEachIndexed { index, documentFile ->
            val result = copy(documentFile, targetFile)
            listener.onFileFinished(index + 1, toCopy.size, documentFile, result)
        }
    }

    private fun copy(toCopy: DocumentFile, targetRoot: DocumentFile): CopyResult {
        try {
            val extension = (toCopy.name ?: "").split(".").last().toUpperCase(Locale.getDefault())
            val baseDir = targetRoot.createDirIfNotExists(extension)
            val targetDirectory = baseDir.createDirIfNotExists(getDateFolderNameFor(toCopy))

            val target = targetDirectory.createFile(toCopy.type!!, toCopy.name!!)
            val result = copyFile(toCopy, target!!)
            if (result != CopyResult.SUCCESS) return result

            return verifyHash(toCopy, target)
        } catch (e: Exception) {
            Log.e(
                logTag,
                "copy(toCopy=${toCopy.name},targetRoot=${targetRoot.name}) - failed to copy due to ${e.message}",
                e
            )
            return CopyResult.ERROR
        }
    }

    private fun copyFile(
        from: DocumentFile,
        to: DocumentFile
    ): CopyResult {
        try {
            contentResolver.openOutputStream(to.uri).use { targetStream ->
                contentResolver.openInputStream(from.uri).use { sourceStream ->
                    sourceStream!!.copyTo(targetStream!!)
                }
            }
            return CopyResult.SUCCESS
        } catch (e: Exception) {
            Log.e(
                logTag,
                "copy(from=${from.name},to=${to.name}) - failed to copy due to ${e.message}",
                e
            )
            return CopyResult.COPY_FAILURE
        }
    }

    private fun verifyHash(
        toCopy: DocumentFile,
        newFile: DocumentFile
    ): CopyResult {
        val shouldVerifyMd5Hash = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            prefVerifyMd5HashOfCopiedFiles, true
        )
        if (shouldVerifyMd5Hash && contentResolver.md5Hash(toCopy) != contentResolver.md5Hash(
                newFile
            )
        ) {
            Log.e(
                logTag,
                "copy(toCopy=${toCopy.name},newFile=${newFile.name}) - Deleting ${newFile.uri}, md5 hash did not match"
            )
            newFile.delete()
            return CopyResult.INTEGRITY_CHECK_FAILED
        }
        return CopyResult.SUCCESS
    }

    private fun getDateFolderNameFor(toCopy: DocumentFile): String {
        val fileDescriptor: FileDescriptor = contentResolver.openFileDescriptor(toCopy.uri, "r")
            ?.fileDescriptor!!
        val exif = ExifInterface(fileDescriptor)
        val date = exif.getAttribute(TAG_DATETIME_ORIGINAL)
        return date?.let {
            val localDate = exifDate.parseLocalDate(date)
            localDate.toString("yyyy-MM-dd")
        } ?: "unknown"
    }

    private fun getTargetDirectory(target: PossibleTargetExternalDrive): DocumentFile {
        return when (target) {
            is TargetExternalDrive -> target.targetDirectory
            is UnknownExternalDrive -> target.toTargetDrive(context).targetDirectory
        }
    }

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    companion object {
        val exifDate: DateTimeFormatter = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss")
        val logTag: String = ExternalDriveFileCopier::class.java.name
    }
}