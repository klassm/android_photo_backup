package li.klass.photo_copy.files.usb

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.files.FileCopier
import li.klass.photo_copy.files.ptp.database.UsbItemExifDataDao
import li.klass.photo_copy.files.ptp.database.UsbItemExifDataEntity
import li.klass.photo_copy.model.ExifData
import org.joda.time.DateTime

class FileSystemFileCreator(
    private val exifDataProvider: FileSystemExifDataProvider,
    private val usbItemExifDataDao: UsbItemExifDataDao
) {
    fun fileSystemFilesFor(files: List<DocumentFile>): List<CopyableFile.FileSystemFile> {
        val toLookup = files.map {
            UsbItemExifDataEntity.identifierFor(it.uri, it.lastModified()) to it
        }.toMap()

        val foundItems = usbItemExifDataDao.getAllBy(toLookup.keys.toList())
        val foundIdentifiers = foundItems.map { it.identifier }.toSet()
        val missingItems = toLookup.keys.filter { !foundIdentifiers.contains(it) }
        val cachedFiles = foundItems.map {
            CopyableFile.FileSystemFile(
                toLookup[it.identifier]
                    ?: error("did not find identifier for file we just looked up"),
                ExifData(DateTime.parse(it.captureDate), it.model)
            )
        }

        val newExifData = missingItems.map {
            val documentFile =
                toLookup[it] ?: error("did not find identifier for file we just looked up")
            it to CopyableFile.FileSystemFile(
                documentFile, exifDataProvider.exifDataFor(
                    documentFile
                )
            )
        }.toMap()

        usbItemExifDataDao.saveAll(
            newExifData.map { (key, value) ->
                UsbItemExifDataEntity(
                    key, value.exifData.model, value.exifData.captureDate.toString()
                )
            }
        )

        Log.i(logTag, "found exif data: cache=${cachedFiles.size}, new=${newExifData.size}")

        return newExifData.values + cachedFiles


    }

    companion object {
        private val logTag = FileSystemFileCreator::class.java.name
    }
}