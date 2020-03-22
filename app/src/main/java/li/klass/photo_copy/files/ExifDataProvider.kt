package li.klass.photo_copy.files

import android.content.ContentResolver
import android.media.ExifInterface
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.model.ExifData
import java.io.FileDescriptor

class ExifDataProvider(private val contentResolver: ContentResolver) {

    private fun exifDataFor(file: CopyableFile.FileSystemFile): ExifData {
        val fileDescriptor: FileDescriptor =
            contentResolver.openFileDescriptor(file.documentFile.uri, "r")
                ?.fileDescriptor!!
        val exif = ExifInterface(fileDescriptor)
        val captureDate = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?.let { UsbService.exifDate.parseDateTime(it) }
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)

        return ExifData(
            captureDate = captureDate,
            model = model
        )
    }
    private fun exifDataFor(file: CopyableFile.PtpFile): ExifData = file.exifData

    fun exifDataFor(file: CopyableFile): ExifData = when(file) {
        is CopyableFile.PtpFile -> exifDataFor(file)
        is CopyableFile.FileSystemFile -> exifDataFor(file)
    }
}