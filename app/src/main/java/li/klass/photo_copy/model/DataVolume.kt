package li.klass.photo_copy.model

import android.os.storage.StorageVolume
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.files.ptp.DeviceInformation

data class MountedVolumeStats(val totalBytes: Long, val availableBytes: Long) {
    val usedBytes: Long = totalBytes - availableBytes
}


sealed class DataVolume {
    data class MountedVolume(val file: DocumentFile, val volume: StorageVolume, val stats: MountedVolumeStats): DataVolume()
    data class PtpVolume(val deviceInformation: DeviceInformation): DataVolume()
}