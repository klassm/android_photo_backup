package li.klass.photo_copy.ui.copy

import android.app.Application
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive
import li.klass.photo_copy.model.ExternalDriveDocument.SourceExternalDrive
import li.klass.photo_copy.service.CopyListener
import li.klass.photo_copy.service.CopyResult
import li.klass.photo_copy.service.ExternalDriveFileCopier
import li.klass.photo_copy.ui.main.MainFragment.Companion.RELOAD_SD_CARDS
import org.joda.time.DateTime

data class FileResult(
    val fileName: String,
    val result: CopyResult,
    val date: DateTime = DateTime.now()
)

data class CopyProgress(
    val currentIndex: Int,
    val totalAmount: Int,
    val results: List<FileResult>
) {
    val progress = ((currentIndex / totalAmount.toFloat()) * 100).toInt()
}

class CopyProgressViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var source: SourceExternalDrive
    lateinit var target: PossibleTargetExternalDrive
    val copyProgress: MutableLiveData<CopyProgress?> = MutableLiveData(null)

    fun startCopying() {

        viewModelScope.launch {
            val listener = object : CopyListener {
                override fun onFileFinished(
                    copiedFileIndex: Int,
                    totalNumberOfFiles: Int,
                    copiedFile: DocumentFile,
                    copyResult: CopyResult
                ) {
                    launch(Dispatchers.Main) {
                        val oldResults = copyProgress.value?.results ?: emptyList()
                        copyProgress.value = CopyProgress(
                            copiedFileIndex,
                            totalNumberOfFiles,
                            oldResults + FileResult(copiedFile.name ?: "???", copyResult)
                        )
                    }
                }

                override fun onCopyStarted(totalNumberOfFiles: Int) {
                    launch(Dispatchers.Main) {
                        copyProgress.value = CopyProgress(0, totalNumberOfFiles, emptyList())
                    }
                }
            }

            launch(Dispatchers.IO) {
                ExternalDriveFileCopier(app).copy(source, target, listener)
            }
            app.sendBroadcast(Intent(RELOAD_SD_CARDS))
        }
    }

    private val app: Application
        get() = getApplication()
}