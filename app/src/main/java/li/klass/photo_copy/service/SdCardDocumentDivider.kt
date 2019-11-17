package li.klass.photo_copy.service

import li.klass.photo_copy.Constants
import li.klass.photo_copy.model.MountedVolume
import li.klass.photo_copy.model.SdCardDocument
import li.klass.photo_copy.model.SdCardDocument.PossibleTargetSdCard.TargetSdCard
import li.klass.photo_copy.model.SdCardDocument.PossibleTargetSdCard.UnknownSdCard

class SdCardDocumentDivider {
    fun divide(volumes: List<MountedVolume>): List<SdCardDocument> {
        return volumes.map { volume ->
            val directories = volume.file.listFiles().filter { it.isDirectory }
            val dcimDirectory = directories.find { it.name == "DCIM" }
            val targetDirectory = directories.find { it.name == Constants.targetDirectoryName }

            when {
                dcimDirectory != null -> SdCardDocument.SourceSdCard(dcimDirectory, volume)
                targetDirectory != null -> TargetSdCard(targetDirectory, volume)
                else -> UnknownSdCard(volume)
            }
        }
    }
}