package li.klass.photo_copy.model

import android.os.storage.StorageVolume
import androidx.documentfile.provider.DocumentFile

data class MountedVolume(val file: DocumentFile, val volume: StorageVolume)