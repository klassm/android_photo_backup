package li.klass.photo_copy.ui.main

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.klass.photo_copy.Constants.prefDidUserSeeExternalAccessInfoMessage
import li.klass.photo_copy.R
import li.klass.photo_copy.model.MountedVolume
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive
import li.klass.photo_copy.model.ExternalDriveDocument.SourceExternalDrive
import li.klass.photo_copy.service.ExternalDriveFileCopier
import li.klass.photo_copy.service.ExternalDriveDocumentDivider

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val externalStorageDrives: MutableLiveData<List<MountedVolume>> = MutableLiveData()

    val selectedSourceDrive: MutableLiveData<SourceExternalDrive?> = MutableLiveData()
    val selectedTargetDrive: MutableLiveData<PossibleTargetExternalDrive?> = MutableLiveData()
    val sourceDrives: MutableLiveData<List<SourceExternalDrive>> = MutableLiveData(
        emptyList())
    val targetDrives: MutableLiveData<List<PossibleTargetExternalDrive>> = MutableLiveData(
        emptyList())
    val startCopyButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val filesToCopy: MutableLiveData<Int?> = MutableLiveData(null)

    val errorMessage: MutableLiveData<String> = MutableLiveData("")
    val statusImage: MutableLiveData<Int> = MutableLiveData(R.drawable.ic_cross_red)

    private fun updateImageAndErrorMessage() {
        if (externalStorageDrives.value.isNullOrEmpty()) {
            statusImage.value = R.drawable.ic_cross_red
            errorMessage.value = app.getString(R.string.no_external_drives_cards)
            return
        }

        if (sourceDrives.value.isNullOrEmpty()) {
            statusImage.value = R.drawable.ic_cross_red
            errorMessage.value = app.getString(R.string.no_source_drives_found)
            return
        }

        if (targetDrives.value.isNullOrEmpty()) {
            statusImage.value = R.drawable.ic_cross_red
            errorMessage.value = app.getString(R.string.no_target_drives_found)
            return
        }

        if (selectedSourceDrive.value == null || selectedTargetDrive.value == null) {
            statusImage.value = R.drawable.ic_question_answer_blue
            errorMessage.value = app.getString(R.string.no_source_or_target)
            return
        }

        errorMessage.value = ""
        statusImage.value = R.drawable.ic_check_green
    }

    fun handleSourceTargetChange(source: SourceExternalDrive?, target: PossibleTargetExternalDrive?) {
        val canCopy = source != null && target != null
        filesToCopy.value = null
        startCopyButtonVisible.value = canCopy

        if (canCopy) {
            viewModelScope.launch {
                filesToCopy.value =
                    withContext(Dispatchers.IO) {
                        ExternalDriveFileCopier(app)
                            .getFilesToCopy(source!!, target!!).size
                    }
            }
        }

        updateImageAndErrorMessage()
    }

    fun handleExternalStorageChange(volumes: List<MountedVolume>) {
        selectedSourceDrive.value = null
        selectedTargetDrive.value = null
        val result = ExternalDriveDocumentDivider(app).divide(volumes)
        sourceDrives.value = result.filterIsInstance<SourceExternalDrive>()
        targetDrives.value = result.filterIsInstance<PossibleTargetExternalDrive>()
        updateImageAndErrorMessage()
    }

    fun didUserAlreadySeeExternalDriveAccessInfo(): Boolean =
        PreferenceManager.getDefaultSharedPreferences(app)
            .getBoolean(prefDidUserSeeExternalAccessInfoMessage, false)

    fun userSawExternalDriveAccessInfo() =
        PreferenceManager.getDefaultSharedPreferences(app)
            .edit().putBoolean(prefDidUserSeeExternalAccessInfoMessage, true).apply()

    private val app: Application
        get() = getApplication()
}
