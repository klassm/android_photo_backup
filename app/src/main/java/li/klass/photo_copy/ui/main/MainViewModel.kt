package li.klass.photo_copy.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.storage.StorageVolume
import android.util.Log
import android.util.Log.INFO
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.klass.photo_copy.AppDatabase
import li.klass.photo_copy.Constants.prefDidUserSeeExternalAccessInfoMessage
import li.klass.photo_copy.R
import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.files.FilesToCopyProvider
import li.klass.photo_copy.files.ptp.PtpFileProvider
import li.klass.photo_copy.files.ptp.PtpService
import li.klass.photo_copy.files.ptp.database.PtpItemDao
import li.klass.photo_copy.files.ptp.database.UsbItemExifDataDao
import li.klass.photo_copy.files.usb.ExternalDriveDocumentDivider
import li.klass.photo_copy.files.usb.FileSystemExifDataProvider
import li.klass.photo_copy.files.usb.FileSystemFileCreator
import li.klass.photo_copy.files.usb.UsbService
import li.klass.photo_copy.model.DataVolume
import li.klass.photo_copy.model.DataVolume.MountedVolume
import li.klass.photo_copy.model.DataVolume.PtpVolume
import li.klass.photo_copy.model.FileContainer.SourceContainer
import li.klass.photo_copy.model.FileContainer.TargetContainer
import li.klass.photo_copy.service.DataVolumesProvider

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val allVolumes: MutableLiveData<List<DataVolume>> = MutableLiveData(emptyList())
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
    val filesToCopy: MutableLiveData<Collection<CopyableFile>?> = MutableLiveData(null)
    val transferListOnly: MutableLiveData<Boolean?> = MutableLiveData(null)

    val status: MutableLiveData<ViewStatus?> = MutableLiveData(null)

    private val database = AppDatabase.getInstance(app)
    private val ptpItemDao: PtpItemDao = database.ptpItemDao()
    private val usbItemExifDataDao: UsbItemExifDataDao = database.usbItemExifDataDao()

    private fun updateStatusBasedOnVolumes() {
        val calculatedStatus= when {
            allVolumes.value.isNullOrEmpty() -> ViewStatus.NO_DRIVES
            selectedSourceDrive.value == null || selectedTargetDrive.value == null -> ViewStatus.INPUT_REQUIRED
            else -> ViewStatus.READY
        }
        Log.i(logTag, "setting status to $calculatedStatus")
        status.value = calculatedStatus

    }

    fun handleSourceTargetChange(source: SourceContainer?, target: TargetContainer?) {
        if (status.value == ViewStatus.RELOADING_VOLUMES) {
            return
        }
        transferListOnly.value =
            if (source != null && source is SourceContainer.SourcePtp) true else null

        updateStatusBasedOnVolumes()
        updateFilesToCopy(source, target)
    }

    fun handleTransferListOnlyChange(transfer: Boolean) {
        transferListOnly.value = transfer
        updateFilesToCopy(selectedSourceDrive.value, selectedTargetDrive.value, transfer)
    }

    @Synchronized
    private fun updateFilesToCopy(
        source: SourceContainer?,
        target: TargetContainer?,
        transferListOnly: Boolean = true
    ) {
        Log.i(logTag, "handleSourceTargetChange(source=$source, target=$target)")

        val canCopy = source != null && target != null
        startCopyButtonVisible.value = canCopy

        if (canCopy) {
            filesToCopy.value = null
            viewModelScope.launch {
                filesToCopy.value =
                    withContext(Dispatchers.IO) {
                        val usbService = UsbService(
                            FileSystemFileCreator(
                                FileSystemExifDataProvider(app.contentResolver),
                                usbItemExifDataDao
                            )
                        )
                        FilesToCopyProvider(usbService, PtpFileProvider(PtpService(), ptpItemDao))
                            .calculateFilesToCopy(target!!, source!!, transferListOnly)
                    }
            }
        }
    }

    private fun updateSourceAndTargetContainers() {
        val allVolumes = allVolumes.value ?: emptyList()
        val sortedVolumes = allVolumes.sortedWith(
            compareBy({ it is PtpVolume }, { it is MountedVolume && it.volume.isRemovable })
        ).reversed()
        val result = ExternalDriveDocumentDivider(
            app
        ).divide(sortedVolumes)
        sourceContainers.value = result.filterIsInstance<SourceContainer>()
        targetContainers.value = result.filterIsInstance<TargetContainer>()
        updateStatusBasedOnVolumes()
    }

    fun didUserAlreadySeeExternalDriveAccessInfo(): Boolean =
        PreferenceManager.getDefaultSharedPreferences(app)
            .getBoolean(prefDidUserSeeExternalAccessInfoMessage, false)

    fun userSawExternalDriveAccessInfo() =
        PreferenceManager.getDefaultSharedPreferences(app)
            .edit().putBoolean(prefDidUserSeeExternalAccessInfoMessage, true).apply()

    fun updateDataVolumes() {
        status.value = ViewStatus.RELOADING_VOLUMES
        allVolumes.value = null
        selectedSourceDrive.value = null
        selectedTargetDrive.value = null
        filesToCopy.value = null
        startCopyButtonVisible.value = false

        val updateVolumesJob = viewModelScope.launch {
            val dataVolumes = withContext(Dispatchers.IO) {
                DataVolumesProvider(app).getDataVolumes()
            }
            allVolumes.value = (allVolumes.value ?: emptyList())
                .filterIsInstance<PtpVolume>() + dataVolumes.available
            missingExternalDrives.value = dataVolumes.missingExternalDrives
        }
        val updatePtpJob = viewModelScope.launch {
            val ptpVolume = withContext(Dispatchers.IO) {
                PtpService().getDeviceInformation()?.let { PtpVolume(it) }
            }
            ptpVolume?.let { volume ->
                allVolumes.value = (allVolumes.value ?: emptyList())
                    .filterNot { it is PtpVolume } + volume
            }
        }

        viewModelScope.launch {
            updateVolumesJob.join()
            updatePtpJob.join()
            updateSourceAndTargetContainers()
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
