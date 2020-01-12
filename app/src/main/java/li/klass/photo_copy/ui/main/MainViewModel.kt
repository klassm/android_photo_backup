package li.klass.photo_copy.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.storage.StorageVolume
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import arrow.core.extensions.list.functorFilter.filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.klass.photo_copy.AppDatabase
import li.klass.photo_copy.Constants.prefDidUserSeeExternalAccessInfoMessage
import li.klass.photo_copy.R
import li.klass.photo_copy.files.FilesToCopyProvider
import li.klass.photo_copy.files.ptp.PtpFileProvider
import li.klass.photo_copy.files.ptp.PtpService
import li.klass.photo_copy.files.ptp.database.PtpItemDao
import li.klass.photo_copy.files.usb.ExternalDriveDocumentDivider
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.model.DataVolume
import li.klass.photo_copy.model.DataVolume.MountedVolume
import li.klass.photo_copy.model.DataVolume.PtpVolume
import li.klass.photo_copy.model.FileContainer.SourceContainer
import li.klass.photo_copy.model.FileContainer.TargetContainer
import li.klass.photo_copy.service.DataVolumesProvider

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val allVolumes: MutableLiveData<List<DataVolume>> = MutableLiveData(emptyList())
    val missingExternalDrives: MutableLiveData<List<StorageVolume>> = MutableLiveData(emptyList())

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
    val transferListOnly: MutableLiveData<Boolean?> = MutableLiveData(null)

    val errorMessage: MutableLiveData<String> = MutableLiveData("")
    val statusImage: MutableLiveData<Int> = MutableLiveData(R.drawable.ic_cross_red)

    private val ptpItemDao: PtpItemDao = AppDatabase.getInstance(app).ptpItemDao()

    private fun updateImageAndErrorMessage() {
        if (allVolumes.value.isNullOrEmpty()) {
            statusImage.value = R.drawable.ic_cross_red
            errorMessage.value = app.getString(R.string.no_external_drives_cards)
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
        transferListOnly.value = if (source != null && source is SourceContainer.SourcePtp) true else null
        updateFilesToCopy(source, target)
    }

    fun handleTransferListOnlyChange(transfer: Boolean) {
        transferListOnly.value = transfer
        updateFilesToCopy(selectedSourceDrive.value, selectedTargetDrive.value, transfer)
    }

    @Synchronized
    private fun updateFilesToCopy(source: SourceContainer?, target: TargetContainer?, transferListOnly: Boolean = true) {
        Log.i(logTag, "handleSourceTargetChange(source=$source, target=$target)")

        val canCopy = source != null && target != null
        startCopyButtonVisible.value = canCopy

        if (canCopy) {
            filesToCopy.value = null
            viewModelScope.launch {
                filesToCopy.value =
                    withContext(Dispatchers.IO) {
                        FilesToCopyProvider(UsbService(app.contentResolver), PtpFileProvider(PtpService(), ptpItemDao))
                            .calculateFilesToCopy(target!!, source!!, transferListOnly).size
                    }
            }
        }

        updateImageAndErrorMessage()
    }

    fun handleExternalStorageChange(volumes: List<DataVolume>) {
        val sortedVolumes = volumes.sortedWith(
            compareBy({ it is PtpVolume }, { it is MountedVolume && it.volume.isRemovable })
        ).reversed()
        val result = ExternalDriveDocumentDivider(
            app
        ).divide(sortedVolumes)
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
        selectedSourceDrive.value = null
        selectedTargetDrive.value = null

        viewModelScope.launch {
            val dataVolumes = withContext(Dispatchers.IO) {
                DataVolumesProvider(app).getDataVolumes()
            }
            allVolumes.value = (allVolumes.value ?: emptyList())
                .filter { it is PtpVolume } + dataVolumes.available
            missingExternalDrives.value = dataVolumes.missingExternalDrives
        }
        viewModelScope.launch {
            val ptpVolume = withContext(Dispatchers.IO) {
                PtpService().getDeviceInformation()?.let { PtpVolume(it) }
            }
            ptpVolume?.let { volume ->
                allVolumes.value = (allVolumes.value ?: emptyList())
                    .filterNot { it is PtpVolume } + volume
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

    private val dataVolumesProvider = DataVolumesProvider(app)

    private val app: Application
        get() = getApplication()

    companion object {
        private val logTag = MainViewModel::class.java.name
    }
}
