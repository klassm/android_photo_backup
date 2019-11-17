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
import android.widget.AdapterView
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.copying_progress.view.*
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import li.klass.photo_copy.R
import li.klass.photo_copy.model.SdCardDocument
import li.klass.photo_copy.model.SdCardDocument.PossibleTargetSdCard
import li.klass.photo_copy.model.SdCardDocument.SourceSdCard
import li.klass.photo_copy.nullableCombineLatest
import li.klass.photo_copy.service.ExternalStorageHandler
import li.klass.photo_copy.service.SdCardCopier

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
                updateSdCards()
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

        val sdCardSelection = nullableCombineLatest(
            viewModel.selectedSourceCard,
            viewModel.selectedTargetCard
        ) { source, target -> source to target }
        sdCardSelection.observe(
            this,
            Observer<Pair<SourceSdCard?, PossibleTargetSdCard?>> { (source, target) ->
                viewModel.handleSourceTargetChange(source, target)
            })

        viewModel.sourceSdCards.observe(this, Observer { sources ->
            context?.let { context ->
                sourceCard.adapter = SdCardAdapter(context, sources)
                sourceCard.onItemSelectedListener =
                    spinnerListenerFor(sources, viewModel.selectedSourceCard)
            }
        })

        viewModel.targetSdCards.observe(this, Observer { targets ->
            context?.let { context ->
                targetCard.adapter = SdCardAdapter(context, targets)
                targetCard.onItemSelectedListener =
                    spinnerListenerFor(targets, viewModel.selectedTargetCard)
            }
        })

        viewModel.externalStorageDrives.observe(this, Observer {
            viewModel.handleExternalStorageChange(it)
        })

        viewModel.errorMessage.observe(this, Observer {
            errorMessage.text = it
        })

        viewModel.statusImage.observe(this, Observer {
            statusImage.setImageDrawable(context?.getDrawable(it))
        })

        nullableCombineLatest(viewModel.startCopyButtonVisible, viewModel.filesToCopy) { a, b -> a to b}
            .observe(this, Observer<Pair<Boolean?, Int?>> { (visible, filesToCopy) ->
                start_copying.visibility = if (visible == true) View.VISIBLE else View.GONE
                if (filesToCopy == null) {
                    start_copying.isEnabled = false
                    start_copying.startAnimation()
                }
            })
        viewModel.filesToCopy.observe(this, Observer {
            if (it != null) {
                start_copying.revertAnimation {
                    start_copying.isEnabled = it > 0
                    start_copying.text = resources.getQuantityString(
                        R.plurals.start_copying,
                        it,
                        it
                    )
                }
            }
        })

        start_copying.setOnClickListener { startCopying() }

        updateSdCards()
    }

    private fun updateSdCards() {
        val myContext = activity ?: return
        val externalStorageHandler = ExternalStorageHandler(myContext)
        val externalStorage = externalStorageHandler.getExternalVolumes()

        viewModel.externalStorageDrives.value = externalStorage.available
        externalStorageHandler.accessIntentsFor(externalStorage)
            .forEach { startActivityForResult(it, 0) }
    }

    private fun <T : SdCardDocument> spinnerListenerFor(
        items: List<T>, toUpdate: MutableLiveData<T?>
    ) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            toUpdate.value = items[position]
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            toUpdate.value = null
        }
    }

    @SuppressLint("SetTextI18n")
    fun startCopying() {
        val activity = activity ?: return
        val source = viewModel.selectedSourceCard.value ?: return
        val target = viewModel.selectedTargetCard.value ?: return

        val view = LayoutInflater.from(activity).inflate(R.layout.copying_progress, null)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.copying_dialog_title)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.show()

        GlobalScope.launch {
            SdCardCopier(activity).copy(
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
                updateSdCards()
            }
        }
    }
}

