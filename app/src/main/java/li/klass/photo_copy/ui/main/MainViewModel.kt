package li.klass.photo_copy.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.storage.StorageVolume
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.klass.photo_copy.Constants.prefDidUserSeeExternalAccessInfoMessage
import li.klass.photo_copy.R
import li.klass.photo_copy.files.FilesToCopyProvider
import li.klass.photo_copy.files.TargetFileCreator
import li.klass.photo_copy.files.ptp.PtpService
import li.klass.photo_copy.files.usb.ExternalDriveDocumentDivider
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.model.DataVolume
import li.klass.photo_copy.model.FileContainer.SourceContainer
import li.klass.photo_copy.model.FileContainer.TargetContainer
import li.klass.photo_copy.service.DataVolumesProvider

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val allVolumes: MutableLiveData<List<DataVolume>> = MutableLiveData()
    val missingExternalDrives: MutableLiveData<List<StorageVolume>> = MutableLiveData()

    val selectedSourceDrive: MutableLiveData<SourceContainer?> = MutableLiveData()
    val selectedTargetDrive: MutableLiveData<TargetContainer?> = MutableLiveData()
    val sourceContainers: MutableLiveData<List<SourceContainer>> = MutableLiveData(
        emptyList()
    )
    val targetContainers: MutableLiveData<List<TargetContainer>> = MutableLiveData(
        emptyList()
    )
    val startCopyButtonVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val filesToCopy: MutableLiveData<Int?> = MutableLiveData(null)

    val errorMessage: MutableLiveData<String> = MutableLiveData("")
    val statusImage: MutableLiveData<Int> = MutableLiveData(R.drawable.ic_cross_red)

    private fun updateImageAndErrorMessage() {
        if (allVolumes.value.isNullOrEmpty()) {
            statusImage.value = R.drawable.ic_cross_red
            errorMessage.value = app.getString(R.string.no_external_drives_cards)
            return
        }

        if (sourceContainers.value.isNullOrEmpty()) {
            statusImage.value = R.drawable.ic_cross_red
            errorMessage.value = app.getString(R.string.no_source_drives_found)
            return
        }

        if (targetContainers.value.isNullOrEmpty()) {
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

    fun handleSourceTargetChange(source: SourceContainer?, target: TargetContainer?) {
        val canCopy = source != null && target != null
        filesToCopy.value = null
        startCopyButtonVisible.value = canCopy

        if (canCopy) {
            viewModelScope.launch {
                val targetDirectory =
                    TargetFileCreator(UsbService(app.contentResolver), app).getTargetDirectory(
                        target!!
                    )
                filesToCopy.value =
                    withContext(Dispatchers.IO) {
                        FilesToCopyProvider(UsbService(app.contentResolver), PtpService())
                            .calculateFilesToCopy(targetDirectory, source!!).size
                    }
            }
        }

        updateImageAndErrorMessage()
    }

    fun handleExternalStorageChange(volumes: List<DataVolume>) {
        selectedSourceDrive.value = null
        selectedTargetDrive.value = null
        val result = ExternalDriveDocumentDivider(
            app
        ).divide(volumes)
        sourceContainers.value = result.filterIsInstance<SourceContainer>()
        targetContainers.value = result.filterIsInstance<TargetContainer>()
        updateImageAndErrorMessage()
    }

    fun didUserAlreadySeeExternalDriveAccessInfo(): Boolean =
        PreferenceManager.getDefaultSharedPreferences(app)
            .getBoolean(prefDidUserSeeExternalAccessInfoMessage, false)

    fun userSawExternalDriveAccessInfo() =
        PreferenceManager.getDefaultSharedPreferences(app)
            .edit().putBoolean(prefDidUserSeeExternalAccessInfoMessage, true).apply()

    fun updateDataVolumes() {
        allVolumes.value = null
        missingExternalDrives.value = null

        viewModelScope.launch {
            val dataVolumes = withContext(Dispatchers.IO) {
                DataVolumesProvider(app, PtpService()).getDataVolumes()
            }
            allVolumes.value = (allVolumes.value ?: emptyList()) + dataVolumes.available
            missingExternalDrives.value = dataVolumes.missingExternalDrives
        }
        viewModelScope.launch {
            val ptpVolume = withContext(Dispatchers.IO) {
                PtpService().getDeviceInformation()?.let { DataVolume.PtpVolume(it) }
            }
            ptpVolume?.let {
                allVolumes.value = (allVolumes.value ?: emptyList()) + it
            }
        }
    }

    fun accessIntentsFor(missingDrives: List<StorageVolume>) =
        missingDrives.map { it.createOpenDocumentTreeIntent() }


    fun onAccessGranted(uri: Uri) {
        app.contentResolver?.apply {
            takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val documentFile = DocumentFile.fromTreeUri(app, uri) ?: return
        val volume = dataVolumesProvider.findVolumeFor(documentFile)

        val dataVolume = volume?.let { dataVolumesProvider.mountedVolumeFor(documentFile, it) }
        if (dataVolume != null) {
            val list = allVolumes.value ?: emptyList()
            allVolumes.value = list + dataVolume
        }
    }

    private val dataVolumesProvider = DataVolumesProvider(app, PtpService())

    private val app: Application
        get() = getApplication()
}
