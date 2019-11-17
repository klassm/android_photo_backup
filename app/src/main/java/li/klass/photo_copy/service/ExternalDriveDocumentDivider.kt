package li.klass.photo_copy.service

import android.content.Context
import androidx.preference.PreferenceManager
import li.klass.photo_copy.Constants
import li.klass.photo_copy.model.MountedVolume
import li.klass.photo_copy.model.ExternalDriveDocument
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive.TargetExternalDrive
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive.UnknownExternalDrive

class ExternalDriveDocumentDivider(private val context: Context) {
    fun divide(volumes: List<MountedVolume>): List<ExternalDriveDocument> {
        return volumes.map { volume ->
            val targetDirectoryName = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.prefTargetDirectoryName, "backup")!!
            val directories = volume.file.listFiles().filter { it.isDirectory }
            val dcimDirectory = directories.find { it.name == "DCIM" }
            val targetDirectory = directories.find { it.name == targetDirectoryName }

            when {
                dcimDirectory != null -> ExternalDriveDocument.SourceExternalDrive(dcimDirectory, volume)
                targetDirectory != null -> TargetExternalDrive(targetDirectory, volume)
                else -> UnknownExternalDrive(volume)
            }
        }
    }
}