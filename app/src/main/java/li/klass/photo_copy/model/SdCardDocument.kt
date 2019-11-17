package li.klass.photo_copy.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import li.klass.photo_copy.Constants
import li.klass.photo_copy.createDirIfNotExists

sealed class SdCardDocument {
    abstract val volume: MountedVolume

    data class SourceSdCard(val sourceDirectory: DocumentFile, override val volume: MountedVolume) :
        SdCardDocument()

    sealed class PossibleTargetSdCard: SdCardDocument() {
        data class TargetSdCard(val targetDirectory: DocumentFile, override val volume: MountedVolume) :
            PossibleTargetSdCard()

        data class UnknownSdCard(override val volume: MountedVolume) : PossibleTargetSdCard() {
            fun toTargetSdCard(context: Context): TargetSdCard {
                val targetDirectoryName = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.prefTargetDirectoryName, "backup")!!
                val target = volume.file.createDirIfNotExists(targetDirectoryName)
                return TargetSdCard(target, volume)
            }
        }
    }
}