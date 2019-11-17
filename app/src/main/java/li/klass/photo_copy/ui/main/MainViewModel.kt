package li.klass.photo_copy.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.klass.photo_copy.R
import li.klass.photo_copy.model.MountedVolume
import li.klass.photo_copy.model.SdCardDocument.PossibleTargetSdCard
import li.klass.photo_copy.model.SdCardDocument.SourceSdCard
import li.klass.photo_copy.service.SdCardCopier
import li.klass.photo_copy.service.SdCardDocumentDivider

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val externalStorageDrives: MutableLiveData<List<MountedVolume>> = MutableLiveData()

    val selectedSourceCard: MutableLiveData<SourceSdCard?> = MutableLiveData()
    val selectedTargetCard: MutableLiveData<PossibleTargetSdCard?> = MutableLiveData()
    val sourceSdCards: MutableLiveData<List<SourceSdCard>> = MutableLiveData(
        emptyList())
    val targetSdCards: MutableLiveData<List<PossibleTargetSdCard>> = MutableLiveData(
        emptyList())
    val startCopyButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val filesToCopy: MutableLiveData<Int?> = MutableLiveData(null)

    val errorMessage: MutableLiveData<String> = MutableLiveData("")
    val statusImage: MutableLiveData<Int> = MutableLiveData(R.drawable.ic_cross_red)


    fun updateImageAndErrorMessage() {
        if (externalStorageDrives.value.isNullOrEmpty()) {
            statusImage.value = R.drawable.ic_cross_red
            errorMessage.value = app.getString(R.string.no_sd_cards)
            return
        }

        if (selectedSourceCard.value == null || selectedTargetCard.value == null) {
            statusImage.value = R.drawable.ic_question_answer_blue
            errorMessage.value = app.getString(R.string.no_source_or_target)
            return
        }

        errorMessage.value = ""
        statusImage.value = R.drawable.ic_check_green
    }

    fun handleSourceTargetChange(source: SourceSdCard?, target: PossibleTargetSdCard?) {
        val canCopy = source != null && target != null
        filesToCopy.value = null
        startCopyButtonVisible.value = canCopy

        if (canCopy) {
            viewModelScope.launch {
                filesToCopy.value =
                    withContext(Dispatchers.IO) {
                        SdCardCopier(app)
                            .getFilesToCopy(source!!, target!!).size
                    }
            }
        }

        updateImageAndErrorMessage()
    }

    fun handleExternalStorageChange(volumes: List<MountedVolume>) {
        selectedSourceCard.value = null
        selectedTargetCard.value = null
        val result = SdCardDocumentDivider(app).divide(volumes)
        sourceSdCards.value = result.filterIsInstance<SourceSdCard>()
        targetSdCards.value = result.filterIsInstance<PossibleTargetSdCard>()
        updateImageAndErrorMessage()
    }

    private val app: Application
        get() = getApplication()
}
