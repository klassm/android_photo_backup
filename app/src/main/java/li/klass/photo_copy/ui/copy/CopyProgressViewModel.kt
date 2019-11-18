package li.klass.photo_copy.ui.copy

import android.app.Application
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive
import li.klass.photo_copy.model.ExternalDriveDocument.SourceExternalDrive
import li.klass.photo_copy.service.ExternalDriveFileCopier
import li.klass.photo_copy.ui.main.MainFragment.Companion.RELOAD_SD_CARDS

data class CopyProgress(val currentIndex: Int, val totalAmount: Int, val currentFile: String) {
    val progress = (((currentIndex + 1) / totalAmount.toFloat()) * 100).toInt()
}

class CopyProgressViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var source: SourceExternalDrive
    lateinit var target: PossibleTargetExternalDrive
    val copyProgress: MutableLiveData<CopyProgress?> = MutableLiveData()

    fun startCopying() {
        GlobalScope.launch {
            launch(Dispatchers.Main) {
                ExternalDriveFileCopier(app).copy(
                    source,
                    target
                ) { currentIndex: Int, totalAmount: Int, file: DocumentFile ->
                    launch(Dispatchers.Main) {
                        copyProgress.value = CopyProgress(
                            currentIndex, totalAmount, file.name ?: "???"
                        )
                    }
                }
            }
            app.sendBroadcast(Intent(RELOAD_SD_CARDS))
        }
    }

    private val app: Application
        get() = getApplication()
}