package li.klass.photo_copy.service

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.files.ptp.PtpService
import li.klass.photo_copy.model.DataVolume
import li.klass.photo_copy.model.DataVolume.MountedVolume
import li.klass.photo_copy.volumeStats

data class DataVolumes(
    val available: List<DataVolume>,
    val missingExternalDrives: List<StorageVolume>
)

class DataVolumesProvider(private val context: Context, private val ptpService: PtpService) {
    private val storageManager =
        ContextCompat.getSystemService(context, StorageManager::class.java)!!
    private val contentResolver = context.contentResolver!!

    fun getDataVolumes(): DataVolumes {
        // we only take external volumes
        val alreadyMountedAndAvailableVolumes = contentResolver.persistedUriPermissions
            .asSequence()
            .filter { it.isReadPermission && it.isWritePermission }
            .map { DocumentFile.fromTreeUri(context, it.uri) }
            .filterNotNull()
            .map { it to findVolumeFor(it) }
            .filter { (_, volume) -> volume != null }
            .map { (file, volume) -> mountedVolumeFor(file, volume!!) }
            .toList()
        val missingVolumes = removableVolumes().filterNot { volume ->
            alreadyMountedAndAvailableVolumes.any { it.volume == volume }
        }
        return DataVolumes(
            available = alreadyMountedAndAvailableVolumes,
            missingExternalDrives = missingVolumes
        )
    }

    fun findVolumeFor(file: DocumentFile): StorageVolume? =
        removableVolumes().find { it.uuid == file.name }

    private fun removableVolumes() = storageManager.storageVolumes
        .filter { it.uuid != null }
        .filter { it.isRemovable }

    fun mountedVolumeFor(file: DocumentFile, volume: StorageVolume) =
        MountedVolume(file, volume, contentResolver.volumeStats(file))
}