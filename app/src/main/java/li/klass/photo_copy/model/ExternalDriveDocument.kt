package li.klass.photo_copy.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import li.klass.photo_copy.Constants
import li.klass.photo_copy.createDirIfNotExists

sealed class ExternalDriveDocument {
    abstract val volume: MountedVolume

    data class SourceExternalDrive(val sourceDirectory: DocumentFile, override val volume: MountedVolume) :
        ExternalDriveDocument()

    sealed class PossibleTargetExternalDrive: ExternalDriveDocument() {
        data class TargetExternalDrive(val targetDirectory: DocumentFile, override val volume: MountedVolume) :
            PossibleTargetExternalDrive()

        data class UnknownExternalDrive(override val volume: MountedVolume) : PossibleTargetExternalDrive() {
            fun toTargetDrive(context: Context): TargetExternalDrive {
                val targetDirectoryName = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.prefTargetDirectoryName, "backup")!!
                val target = volume.file.createDirIfNotExists(targetDirectoryName)
                return TargetExternalDrive(target, volume)
            }
        }
    }
}