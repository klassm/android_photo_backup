package li.klass.photo_copy.service

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.model.DataVolume
import li.klass.photo_copy.model.DataVolume.MountedVolume
import li.klass.photo_copy.volumeStats

data class DataVolumes(
    val available: List<DataVolume>,
    val missingExternalDrives: List<StorageVolume>
)

class DataVolumesProvider(private val context: Context) {
    private val storageManager =
        ContextCompat.getSystemService(context, StorageManager::class.java)!!
    private val contentResolver = context.contentResolver!!

    fun getDataVolumes(): DataVolumes {
        // we only take external volumes
        val alreadyMountedAndAvailableVolumes = contentResolver.persistedUriPermissions
            .asSequence()
            .filter { it.isReadPermission && it.isWritePermission }
            .map { DocumentFile.fromTreeUri(context, it.uri) }
            .filter { it?.name != null }
            .filterNotNull()
            .map { it to findVolumeFor(it) }
            .filter { (_, volume) -> volume != null }
            .map { (file, volume) -> mountedVolumeFor(file, volume!!) }
            .toList()
        val missingVolumes = removableVolumes().filterNot { volume ->
            alreadyMountedAndAvailableVolumes.any { it.volume == volume }
        }
        val volumes = DataVolumes(
            available = alreadyMountedAndAvailableVolumes,
            missingExternalDrives = missingVolumes
        )
        Log.i(logTag, "getDataVolumes() - found volumes: $volumes")
        return volumes
    }

    fun findVolumeFor(file: DocumentFile): StorageVolume? {
        val removableVolumes = removableVolumes()
        if(file.uri.path?.startsWith("/tree/primary") == true) {
            return removableVolumes.find { !it.isRemovable }
        }

        return removableVolumes.find { it.uuid == file.name }
    }

    private fun removableVolumes() = storageManager.storageVolumes

    fun mountedVolumeFor(file: DocumentFile, volume: StorageVolume) =
        MountedVolume(file, volume, contentResolver.volumeStats(file))

    companion object {
        private val logTag = DataVolumesProvider::class.java.name
    }
}