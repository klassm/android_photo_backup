package li.klass.photo_copy.model

import android.os.storage.StorageVolume
import androidx.documentfile.provider.DocumentFile

data class StorageVolumeStats(val totalBytes: Long, val availableBytes: Long) {
    val usedBytes: Long = totalBytes - availableBytes
}

data class MountedVolume(val file: DocumentFile, val volume: StorageVolume, val stats: StorageVolumeStats)