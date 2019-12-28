package li.klass.photo_copy.files

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import li.klass.photo_copy.Constants
import li.klass.photo_copy.extension
import li.klass.photo_copy.files.CopyableFile.FileSystemFile
import li.klass.photo_copy.service.CopyResult
import li.klass.photo_copy.service.Copier

class JpgFromNefExtractor(
    private val targetFileCreator: TargetFileCreator,
    private val context: Context
) {
    fun extractTargetFileFrom(file: DocumentFile, baseDir: DocumentFile): Either<CopyResult, DocumentFile> {
        if (file.extension != "NEF" || !shouldExtractJpeg) {
            return right(file)
        }

        val targetFile: DocumentFile =
            targetFileCreator.createTargetFileFor(baseDir, FileSystemFile(file), "JPG")
                ?: return left(CopyResult.JPG_CREATION_FOR_NEF_FAILED)

        return try {
            val contentResolver = context.contentResolver
            val (offset, length) = contentResolver.openInputStream(file.uri).use { source ->
                source ?: return left(CopyResult.JPG_COULD_NOT_READ_NEF_INPUT_FILE)
                val metadata = ImageMetadataReader.readMetadata(source)
                val directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                val offset = directory.getInt(0x0201)
                val length = directory.getInt(0x0202)

                offset to length
            }

            contentResolver.openInputStream(file.uri).use { source ->
                source!!.skip(offset.toLong())

                contentResolver.openOutputStream(targetFile.uri).use { target ->
                    source.copyTo(target!!, length)
                }
            }
            return left(CopyResult.SUCCESS)
        } catch (e: Exception) {
            Log.e(
                Copier.logTag,
                "extractJpgFromNef(baseDir=${baseDir.name},nef=${file.name}) - Could not extract jpg from nef",
                e
            )
            return left(CopyResult.JPG_COULD_NOT_EXTRACT_JPG_FROM_NEF)
        }
    }

    private val shouldExtractJpeg get() =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            Constants.prefExtractJpgFromNef, true
        )
}