package li.klass.photo_copy.service

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import li.klass.photo_copy.model.MountedVolume

data class ExternalVolumes(
    val available: List<MountedVolume>,
    val missing: List<StorageVolume>
) {

}

class ExternalStorageHandler(private val activity: Activity) {
    private val storageManager =
        ContextCompat.getSystemService(activity, StorageManager::class.java)!!
    private val contentResolver = activity.contentResolver!!

    fun getExternalVolumes(): ExternalVolumes {

        val alreadyMountedAndAvailableVolumes = contentResolver.persistedUriPermissions
            .asSequence()
            .filter { it.isReadPermission && it.isWritePermission }
            .map { DocumentFile.fromTreeUri(activity, it.uri) }
            .filterNotNull()
            .map { it to findVolumeFor(it) }
            .filter { (_, volume) -> volume != null }
            .map { (file, volume) -> MountedVolume(file, volume!!) }
            .toList()

        val missingVolumes = removableVolumes().filterNot { volume ->
            alreadyMountedAndAvailableVolumes.any { it.volume == volume }
        }
        return ExternalVolumes(
            available = alreadyMountedAndAvailableVolumes,
            missing = missingVolumes
        )
    }

    fun requestAccessFor(externalVolumes: ExternalVolumes) {
        externalVolumes.missing
            .forEach {
                activity.startActivityForResult(it.createOpenDocumentTreeIntent(), 1)
            }
    }

    fun onAccessGranted(uri: Uri): MountedVolume? {
        activity.contentResolver?.apply {
            takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val documentFile = DocumentFile.fromTreeUri(activity, uri) ?: return null
        val volume = findVolumeFor(documentFile)

        return volume?.let { MountedVolume(documentFile, it) }
    }

    private fun findVolumeFor(file: DocumentFile): StorageVolume? =
        removableVolumes().find { it.uuid == file.name }

    private fun removableVolumes() = storageManager.storageVolumes
        .filter { it.uuid != null }
        .filter { it.isRemovable }
}