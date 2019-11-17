package li.klass.photo_copy.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import li.klass.photo_copy.model.MountedVolume
import li.klass.photo_copy.model.SdCardDocument

class MainViewModel : ViewModel() {
    val externalStorageDrives: MutableLiveData<List<MountedVolume>> = MutableLiveData()
    val selectedSourceCard: MutableLiveData<SdCardDocument.SourceSdCard?> = MutableLiveData()
    val selectedTargetCard: MutableLiveData<SdCardDocument.PossibleTargetSdCard?> = MutableLiveData()
}
