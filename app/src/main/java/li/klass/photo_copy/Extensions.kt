package li.klass.photo_copy

import android.content.ContentResolver
import android.os.Handler
import android.os.Looper
import android.system.Os
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import li.klass.photo_copy.model.MountedVolumeStats
import java.lang.IllegalStateException
import java.security.MessageDigest
import java.util.*
import java.util.function.Predicate

fun DocumentFile.listAllFiles(predicate: (file: DocumentFile) -> Boolean = { _ -> true }): List<DocumentFile> =
    listFiles()
        .filter { it.isDirectory || predicate(it) }
        .flatMap { if (it.isDirectory) it.listAllFiles(predicate) else listOf(it) }

fun DocumentFile.createDirIfNotExists(dirName: String): DocumentFile {
    return listFiles().find { it.isDirectory && it.name == dirName }
        ?: createDirectory(dirName) ?: throw IllegalStateException("cannot create directory $dirName")
}

fun ContentResolver.md5Hash(documentFile: DocumentFile): String {
    val md5 = MessageDigest.getInstance("MD5")
    val bytes: ByteArray = openInputStream(documentFile.uri).use { it?.readBytes() } ?: ByteArray(0)
    md5.update(bytes)
    return String(md5.digest()).uppercase(Locale.getDefault())
}

fun ContentResolver.volumeStats(documentFile: DocumentFile): MountedVolumeStats {
    val stats = Os.fstatvfs(openFileDescriptor(documentFile.uri, "r")?.fileDescriptor)

    return MountedVolumeStats(
        totalBytes = stats.f_blocks * stats.f_frsize,
        availableBytes = stats.f_bavail * stats.f_frsize
    )
}

fun <X, T, Z> nullableCombineLatest(first: LiveData<X?>, second: LiveData<T?>, combineFunction: (X?, T?) -> Z?): LiveData<Z> {
    val finalLiveData: MediatorLiveData<Z> = MediatorLiveData()

    var firstEmitted = false
    var firstValue: X? = null

    var secondEmitted = false
    var secondValue: T? = null
    finalLiveData.addSource(first) { value ->
        firstEmitted = true
        firstValue = value
        if (firstEmitted && secondEmitted) {
            finalLiveData.value = combineFunction(firstValue, secondValue)
        }
    }
    finalLiveData.addSource(second) { value ->
        secondEmitted = true
        secondValue = value
        if (firstEmitted && secondEmitted) {
            finalLiveData.value = combineFunction(firstValue, secondValue)
        }
    }
    return finalLiveData
}

fun <T> LiveData<T>.debounce(duration: Long = 1000L) = MediatorLiveData<T>().also { mld ->
    val source = this
    val handler = Handler(Looper.getMainLooper())

    val runnable = Runnable {
        mld.value = source.value
    }

    mld.addSource(source) {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, duration)
    }
}

