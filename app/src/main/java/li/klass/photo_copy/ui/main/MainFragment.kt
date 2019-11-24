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
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.main_fragment.*
import li.klass.photo_copy.R
import li.klass.photo_copy.model.ExternalDriveDocument
import li.klass.photo_copy.nullableCombineLatest
import li.klass.photo_copy.service.ExternalStorageHandler
import li.klass.photo_copy.ui.copy.CopyProgressFragment

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
                updateDrives()
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

        nullableCombineLatest(
            viewModel.selectedSourceDrive,
            viewModel.selectedTargetDrive
        ) { source, target -> source to target }
            .observe(
                this,
                Observer { (source, target) ->
                    viewModel.handleSourceTargetChange(source, target)
                })

        nullableCombineLatest(
            viewModel.sourceDrives,
            viewModel.targetDrives
        ) { source, target -> source to target }
            .observe(
                this,
                Observer { (source, target) ->
                    val visible = source?.size ?: 0 > 0 && target?.size ?: 0 > 0
                    selectSourceTarget.visibility = if (visible) View.VISIBLE else View.GONE
                })

        viewModel.sourceDrives.observe(this, Observer { sources ->
            context?.let { context ->
                sourceCard.adapter = ExternalDriveAdapter(context, sources)
                sourceCard.onItemSelectedListener =
                    spinnerListenerFor(sources, viewModel.selectedSourceDrive)
            }
        })

        viewModel.targetDrives.observe(this, Observer { targets ->
            context?.let { context ->
                targetCard.adapter = ExternalDriveAdapter(context, targets)
                targetCard.onItemSelectedListener =
                    spinnerListenerFor(targets, viewModel.selectedTargetDrive)
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

        nullableCombineLatest(
            viewModel.startCopyButtonVisible,
            viewModel.filesToCopy
        ) { a, b -> a to b }
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

        updateDrives()
    }

    private fun updateDrives() {
        val myContext = activity ?: return
        val externalStorageHandler = ExternalStorageHandler(myContext)
        val externalStorage = externalStorageHandler.getExternalVolumes()
        val requestAccess = {
            externalStorageHandler.accessIntentsFor(externalStorage)
                .forEach { startActivityForResult(it, 0) }
        }

        viewModel.externalStorageDrives.value = externalStorage.available
        if (externalStorage.missing.isEmpty()) {
            return
        }

        if (viewModel.didUserAlreadySeeExternalDriveAccessInfo()) {
            requestAccess()
        } else {
            AlertDialog.Builder(context)
                .setMessage(R.string.external_drive_access_content)
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewModel.userSawExternalDriveAccessInfo()
                    requestAccess()
                }
                .show()
        }
    }

    private fun <T : ExternalDriveDocument> spinnerListenerFor(
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
        val source = viewModel.selectedSourceDrive.value ?: return
        val target = viewModel.selectedTargetDrive.value ?: return
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.container, CopyProgressFragment.newInstance(source, target))
            ?.addToBackStack(null)
            ?.commit()
    }
}

