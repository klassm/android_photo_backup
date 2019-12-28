package li.klass.photo_copy.files.usb

import android.content.Context
import androidx.preference.PreferenceManager
import li.klass.photo_copy.Constants
import li.klass.photo_copy.model.DataVolume
import li.klass.photo_copy.model.DataVolume.MountedVolume
import li.klass.photo_copy.model.DataVolume.PtpVolume
import li.klass.photo_copy.model.FileContainer
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourceExternalDrive
import li.klass.photo_copy.model.FileContainer.TargetContainer.TargetExternalDrive
import li.klass.photo_copy.model.FileContainer.TargetContainer.UnknownExternalDrive

class ExternalDriveDocumentDivider(private val context: Context) {
    fun divide(volumes: List<DataVolume>) = volumes.map { volume ->
        when (volume) {
            is PtpVolume -> FileContainer.SourceContainer.SourcePtp(volume.deviceInformation)
            is MountedVolume -> fileContainerFrom(volume)
        }
    }

    private fun fileContainerFrom(volume: MountedVolume): FileContainer {
        val directories = volume.file.listFiles().filter { it.isDirectory }
        val dcimDirectory = directories.find { it.name == "DCIM" }
        val targetDirectory = directories.find { it.name == targetDirectoryName }

        return when {
            dcimDirectory != null -> SourceExternalDrive(dcimDirectory, volume)
            targetDirectory != null -> TargetExternalDrive(targetDirectory, volume)
            else -> UnknownExternalDrive(volume)
        }
    }

    private val targetDirectoryName: String
        get() =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.prefTargetDirectoryName, "backup")!!
}