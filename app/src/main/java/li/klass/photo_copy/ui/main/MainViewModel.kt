package li.klass.photo_copy.ui.main

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import li.klass.photo_copy.model.MountedVolume
import li.klass.photo_copy.model.SdCardDocument
import li.klass.photo_copy.service.SdCardCopier
import li.klass.photo_copy.service.SdCardDocumentDivider

class MainViewModel : ViewModel() {
    val externalStorageDrives: MutableLiveData<List<MountedVolume>> = MutableLiveData()

}
