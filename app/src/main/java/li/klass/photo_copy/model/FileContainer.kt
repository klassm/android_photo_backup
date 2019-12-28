package li.klass.photo_copy.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import li.klass.photo_copy.Constants
import li.klass.photo_copy.createDirIfNotExists
import li.klass.photo_copy.files.ptp.DeviceInformation
import li.klass.photo_copy.model.DataVolume.*
import java.io.Serializable

interface ExternalVolume {
    val volume: MountedVolume
}

sealed class FileContainer : Serializable {
    sealed class SourceContainer : FileContainer() {
        data class SourceExternalDrive(
            val sourceDirectory: DocumentFile,
            override val volume: MountedVolume
        ) : ExternalVolume, SourceContainer()

        data class SourcePtp(val deviceInformation: DeviceInformation) : SourceContainer()
    }

    sealed class TargetContainer : FileContainer() {
        data class TargetExternalDrive(
            val targetDirectory: DocumentFile,
            override val volume: MountedVolume
        ) : ExternalVolume, TargetContainer()

        data class UnknownExternalDrive(override val volume: MountedVolume) :
            ExternalVolume, TargetContainer() {
            fun toTargetDrive(context: Context): TargetExternalDrive {
                val targetDirectoryName = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.prefTargetDirectoryName, "backup")!!
                val target = volume.file.createDirIfNotExists(targetDirectoryName)
                return TargetExternalDrive(target, volume)
            }
        }
    }
}