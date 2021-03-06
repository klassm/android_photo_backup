package li.klass.photo_copy.ui.copy

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import li.klass.photo_copy.AppDatabase
import li.klass.photo_copy.files.*
import li.klass.photo_copy.files.ptp.PtpFileProvider
import li.klass.photo_copy.files.ptp.PtpService
import li.klass.photo_copy.files.ptp.database.PtpItemDao
import li.klass.photo_copy.model.FileContainer.SourceContainer
import li.klass.photo_copy.model.FileContainer.TargetContainer
import li.klass.photo_copy.service.Copier
import li.klass.photo_copy.service.CopyListener
import li.klass.photo_copy.service.CopyResult
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
    lateinit var source: SourceContainer
    lateinit var target: TargetContainer
    var transferListOnly: Boolean = false
    lateinit var filesToCopy: List<CopyableFile>

    val copyProgress: MutableLiveData<CopyProgress?> = MutableLiveData(null)
    private val database = AppDatabase.getInstance(app)
    private val ptpItemDao: PtpItemDao = database.ptpItemDao()

    fun startCopying() {

        viewModelScope.launch {
            val listener = object : CopyListener {
                override fun onFileFinished(
                    copiedFileIndex: Int,
                    totalNumberOfFiles: Int,
                    copiedFile: CopyableFile,
                    copyResult: CopyResult
                ) {
                    launch(Dispatchers.Main) {
                        val oldResults = copyProgress.value?.results ?: emptyList()
                        copyProgress.value = CopyProgress(
                            copiedFileIndex,
                            totalNumberOfFiles,
                            oldResults + FileResult(copiedFile.filename, copyResult)
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
                copier.copy(target, listener, filesToCopy)
            }
            app.sendBroadcast(Intent(RELOAD_SD_CARDS))
        }
    }

    private val copier: Copier
        get() {
            val ptpFileProvider = PtpFileProvider(PtpService(), ptpItemDao)
            val targetFileCreator = TargetFileCreator(app)
            val fileCopier = FileCopier(
                app.contentResolver,
                ptpFileProvider,
                targetFileCreator
            )
            val jpgFromNefExtractor = JpgFromNefExtractor(targetFileCreator, app)

            return Copier(
                context = app,
                fileCopier = fileCopier,
                jpgFromNefExtractor = jpgFromNefExtractor,
                targetFileCreator = targetFileCreator
            )
        }

    private val app: Application
        get() = getApplication()
}