package li.klass.photo_copy.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.getDrawable
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.copying_progress.view.*
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.klass.photo_copy.R
import li.klass.photo_copy.model.MountedVolume
import li.klass.photo_copy.model.SdCardDocument
import li.klass.photo_copy.service.ExternalStorageHandler
import li.klass.photo_copy.service.SdCardCopier
import li.klass.photo_copy.service.SdCardDocumentDivider


class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        const val RELOAD_SD_CARDS = "reload_sd_cards"
    }

    private lateinit var viewModel: MainViewModel

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return

            if (intent.action == RELOAD_SD_CARDS) {
                queryUsbDevices()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(RELOAD_SD_CARDS)
        })
    }

    override fun onPause() {
        activity?.unregisterReceiver(broadcastReceiver)
        super.onPause()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        val activity = activity ?: return
        val externalStorageHandler = ExternalStorageHandler(activity)

        val treeUri: Uri = data.data as Uri
        val mountedVolume = externalStorageHandler.onAccessGranted(treeUri)
        if (mountedVolume != null) {
            val list = viewModel.externalStorageDrives.value ?: emptyList()
            viewModel.externalStorageDrives.value = list + mountedVolume
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.externalStorageDrives.observe(this, Observer(::updateUI))

        queryUsbDevices()
    }

    private fun queryUsbDevices() {
        val myContext = activity ?: return
        val externalStorageHandler = ExternalStorageHandler(myContext)
        val externalStorage = externalStorageHandler.getExternalVolumes()
        viewModel.externalStorageDrives.value = externalStorage.available
        externalStorageHandler.requestAccessFor(externalStorage)
    }

    private fun updateUI(files: List<MountedVolume>) {
        val activity = activity ?: return
        val result = SdCardDocumentDivider().divide(files)
        val sources = result.filterIsInstance<SdCardDocument.SourceSdCard>()
        update(
            list = sources,
            imageView = sourceImage, textView = sourceText
        )
        val targets = result.filterIsInstance<SdCardDocument.PossibleTargetSdCard>()
        update(
            list = targets,
            imageView = targetImage, textView = targetText
        )

        val canCopy = sources.size == 1 && targets.size == 1
        start_copying.visibility = if (canCopy) View.VISIBLE else View.GONE
        if (canCopy) {
            val filesToCopy = SdCardCopier(activity.contentResolver).getFilesToCopy(sources[0], targets[0])
            start_copying.text = resources.getQuantityString(
                R.plurals.start_copying,
                filesToCopy.size,
                filesToCopy.size
            )
            start_copying.setOnClickListener {
                startCopying()
            }
        }
    }

    private fun update(list: List<SdCardDocument>, imageView: ImageView, textView: TextView) {
        val context = imageView.context
        val text = list.joinToString(separator = ",") { it.volume.volume.getDescription(context) }

        textView.text = text
        imageView.setImageDrawable(
            getDrawable(
                context,
                if (list.size != 1) R.drawable.ic_cross_red else R.drawable.ic_check_green
            )
        )
    }

    @SuppressLint("SetTextI18n")
    fun startCopying() {
        val result =
            SdCardDocumentDivider().divide(viewModel.externalStorageDrives.value ?: emptyList())
        val source = result.filterIsInstance<SdCardDocument.SourceSdCard>()[0]
        val target = result.filterIsInstance<SdCardDocument.PossibleTargetSdCard>()[0]
        val activity = activity ?: return

        val view = LayoutInflater.from(activity).inflate(R.layout.copying_progress, null)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.copying_dialog_title)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.show()

        GlobalScope.launch {
            SdCardCopier(activity.contentResolver).copy(
                source,
                target
            ) { currentIndex: Int, totalAmount: Int, file: DocumentFile ->
                launch(Dispatchers.Main) {
                    val progress = ((currentIndex / totalAmount.toFloat()) * 100).toInt()
                    view.progress.setProgress(progress, true)
                    view.current_file.text = file.name
                    view.current_file_index.text = "${currentIndex + 1}"
                    view.total_file_index.text = "$totalAmount"
                }
            }
            launch(Dispatchers.Main) {
                dialog.hide()
            }
        }
    }
}

