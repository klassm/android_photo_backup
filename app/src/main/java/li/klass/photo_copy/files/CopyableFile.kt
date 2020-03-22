package li.klass.photo_copy.files

import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.model.ExifData
import java.lang.RuntimeException
import java.util.*

val extensionToMimeType = mapOf(
    "JPG" to "image/jpeg",
    "JPEG" to "image/jpeg",
    "ARW" to "image/x-sony-arw",
    "CR2" to "image/x-canon-cr2",
    "CRW" to "image/x-canon-crw",
    "DCR" to "image/x-kodak-dcr",
    "DNG" to "image/x-adobe-dng",
    "ERF" to "image/x-epson-erf",
    "K25" to "image/x-kodak-k25",
    "KDC" to "image/x-kodak-kdc",
    "MRW" to "image/x-minolta-mrw",
    "NEF" to "image/x-nikon-nef",
    "ORF" to "image/x-olympus-orf",
    "PEF" to "image/x-pentax-pef",
    "RAF" to "image/x-fuji-raf",
    "RAW" to "image/x-panasonic-raw",
    "SR2" to "image/x-sony-sr2",
    "SRF" to "image/x-sony-srf",
    "X3F" to "image/x-sigma-x3f"
)

fun mimeTypeFor(extension: String) =
    extensionToMimeType.getOrElse(extension) { throw RuntimeException("Cannot find mime type for $extension") }

sealed class CopyableFile {
    abstract val filename: String
    abstract val mimeType: String
    abstract val exifData: ExifData
    val extension get() = filename.split(".").last().toUpperCase(Locale.getDefault())

    data class PtpFile(
        override val filename: String,
        val uid: Long,
        override val exifData: ExifData
    ) :
        CopyableFile() {
        override val mimeType: String
            get() = mimeTypeFor(extension)
    }

    data class FileSystemFile(val documentFile: DocumentFile, override val exifData: ExifData) : CopyableFile() {
        override val filename = documentFile.name ?: "???"
        override val mimeType = documentFile.type ?: mimeTypeFor(extension)
    }

    val targetFileName: String get() {
        val targetFileName = filename
        val prefix = exifData.model ?.let { it + "_" }

        return prefix + targetFileName
    }
}