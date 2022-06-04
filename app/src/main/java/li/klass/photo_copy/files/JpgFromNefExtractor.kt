package li.klass.photo_copy.files

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import androidx.preference.PreferenceManager
import arrow.core.Either
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifDirectoryBase
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.exif.GpsDirectory.TAG_ALTITUDE
import com.drew.metadata.exif.GpsDirectory.TAG_DATE_STAMP
import li.klass.photo_copy.Constants
import li.klass.photo_copy.files.CopyableFile.FileSystemFile
import li.klass.photo_copy.service.CopyResult
import li.klass.photo_copy.service.Copier
import java.lang.IllegalStateException

class JpgFromNefExtractor(
    private val targetFileCreator: TargetFileCreator,
    private val context: Context
) {
    fun extractTargetFileFrom(
        file: FileSystemFile,
        baseDir: DocumentFile
    ): Either<CopyResult, CopyableFile> {
        if (file.extension != "NEF" || !shouldExtractJpeg) {
            return Either.Right(file)
        }

        val targetFile: DocumentFile =
            targetFileCreator.createTargetFileFor(
                baseDir, file,
                targetFileName = file.filename + ".JPG",
                extension = "JPG",
                mimeType = mimeTypeFor("JPG")
            )
                ?: return Either.Left(CopyResult.JPG_CREATION_FOR_NEF_FAILED)

        @Suppress("UNREACHABLE_CODE")
        return try {
            val contentResolver = context.contentResolver
            val sourceMetadata = metadataFrom(file.documentFile)
                ?: return Either.Left(CopyResult.JPG_COULD_NOT_EXTRACT_JPG_FROM_NEF)
            val (offset, length) = sourceMetadata.let { source ->
                val directory = source.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                val offset = directory.getInt(0x0201)
                val length = directory.getInt(0x0202)

                offset to length
            }

            contentResolver.openInputStream(file.documentFile.uri).use { source ->
                source!!.skip(offset.toLong())

                contentResolver.openOutputStream(targetFile.uri).use { target ->
                    source.copyTo(target!!, length)
                }
            }

            val targetMetadata = exifDataFrom(targetFile) ?: return Either.Left(CopyResult.ERROR)
            copyExifData(sourceMetadata, targetMetadata)

            return Either.Right(FileSystemFile(targetFile, file.exifData))
        } catch (e: Exception) {
            Log.e(
                Copier.logTag,
                "extractJpgFromNef(baseDir=${baseDir.name},nef=${file.filename}) - Could not extract jpg from nef",
                e
            )
            return Either.Left(CopyResult.JPG_COULD_NOT_EXTRACT_JPG_FROM_NEF)
        }
    }

    private fun copyExifData(source: Metadata, target: ExifInterface) {
        try {
            val directory = source.getFirstDirectoryOfType(ExifDirectoryBase::class.java) ?: return
            val orientation = directory.getInt(ExifDirectoryBase.TAG_ORIENTATION)
            target.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            target.setAttribute("ThumbnailOrientation", orientation.toString())

            source.getFirstDirectoryOfType(GpsDirectory::class.java) ?.let { gps ->
                if (gps.geoLocation != null) {
                    target.setLatLong(gps.geoLocation.latitude, gps.geoLocation.longitude)
                }

                target.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, gps.getString(TAG_DATE_STAMP))
                target.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, gps.getString(TAG_ALTITUDE))
            }

            target.saveAttributes()
        } catch (e: Exception) {
            throw IllegalStateException("could not save", e)
        }
    }

    private fun metadataFrom(file: DocumentFile): Metadata? =
        context.contentResolver.openInputStream(file.uri)?.use {
            ImageMetadataReader.readMetadata(it)
        }

    private fun exifDataFrom(file: DocumentFile): ExifInterface? =
        context.contentResolver.openFileDescriptor(file.uri, "rw")
            ?.fileDescriptor
            ?.let { ExifInterface(it) }

    private val shouldExtractJpeg
        get() =
            PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.prefExtractJpgFromNef, true
            )
}