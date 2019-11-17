package li.klass.photo_copy

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest
import java.util.*

fun DocumentFile.listAllFiles(): List<DocumentFile> =
    listFiles()
        .flatMap { if (it.isDirectory) it.listAllFiles() else listOf(it) }

fun DocumentFile.createDirIfNotExists(dirName: String): DocumentFile {
    return listFiles().find { it.isDirectory && it.name == dirName }
        ?: createDirectory(dirName)!!
}

fun ContentResolver.md5Hash(documentFile: DocumentFile): String {
    val md5 = MessageDigest.getInstance("MD5")
    val bytes: ByteArray = openInputStream(documentFile.uri).use { it?.readBytes() } ?: ByteArray(0)
    md5.update(bytes)
    return String(md5.digest()).toUpperCase(Locale.getDefault())
}