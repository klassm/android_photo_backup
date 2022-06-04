package li.klass.photo_copy.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import kotlinx.android.synthetic.main.main_fragment.*
import li.klass.photo_copy.R
import li.klass.photo_copy.debounce
import li.klass.photo_copy.model.FileContainer
import li.klass.photo_copy.nullableCombineLatest
import li.klass.photo_copy.ui.copy.CopyProgressFragment

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        const val RELOAD_SD_CARDS = "reload_sd_cards"
    }

    private val viewModel: MainViewModel by viewModels()
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val treeUri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && treeUri != null) {
            viewModel.onAccessGranted(treeUri)
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return

            if (intent.action == RELOAD_SD_CARDS) {
                viewModel.updateDataVolumes()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nullableCombineLatest(
            viewModel.selectedSourceDrive,
            viewModel.selectedTargetDrive
        ) { source, target -> source to target }
            .debounce(1000)
            .observe(
                viewLifecycleOwner
            ) { (source, target) ->
                viewModel.handleSourceTargetChange(source, target)
            }

        viewModel.sourceContainers.observe(viewLifecycleOwner) { sources ->
            context?.let { context ->
                sourceCard.onItemSelectedListener =
                    spinnerListenerFor(sources, viewModel.selectedSourceDrive)
                sourceCard.adapter = ExternalDriveAdapter(context, sources)
                if (sources.isEmpty()) {
                    sourceCard.visibility = View.GONE
                    sourceCardEmpty.visibility = View.VISIBLE
                } else {
                    sourceCard.visibility = View.VISIBLE
                    sourceCardEmpty.visibility = View.GONE
                }
            }
        }

        viewModel.targetContainers.observe(viewLifecycleOwner) { targets ->
            context?.let { context ->
                targetCard.onItemSelectedListener =
                    spinnerListenerFor(targets, viewModel.selectedTargetDrive)
                targetCard.adapter = ExternalDriveAdapter(context, targets)
                if (targets.isEmpty()) {
                    targetCard.visibility = View.GONE
                    targetCardEmpty.visibility = View.VISIBLE
                } else {
                    targetCard.visibility = View.VISIBLE
                    targetCardEmpty.visibility = View.GONE
                }
            }
        }

        viewModel.updatingDataVolumes
            .observe(viewLifecycleOwner) { updating ->
            updatingDataVolumes.visibility = if (updating) View.VISIBLE else View.GONE
        }

        viewModel.transferListOnly.observe(viewLifecycleOwner) { transferOnly ->
            transferListOnly.visibility = if (transferOnly == null) View.GONE else View.VISIBLE
            transferListOnly.isChecked = transferOnly ?: false
        }

        transferListOnly.setOnCheckedChangeListener { _, isChecked ->
            viewModel.handleTransferListOnlyChange(isChecked)
        }

        viewModel.allVolumes.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.handleExternalStorageChange(it)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            errorMessage.text = it
        }

        viewModel.statusImage.observe(viewLifecycleOwner) { drawable ->
            statusImage.setImageDrawable(context?.let { ContextCompat.getDrawable(it, drawable) })
        }

        nullableCombineLatest(
            viewModel.startCopyButtonVisible,
            viewModel.filesToCopy
        ) { a, b -> a to b?.size }
            .observe(viewLifecycleOwner) { (visible, filesToCopy) ->
                start_copying.visibility = if (visible == true) View.VISIBLE else View.GONE
                if (filesToCopy == null) {
                    start_copying.isEnabled = false
                    start_copying.startAnimation()
                }
            }
        viewModel.filesToCopy.observe(viewLifecycleOwner) {
            if (it != null) {
                start_copying.revertAnimation {
                    start_copying.isEnabled = it.isNotEmpty()
                    start_copying.text = resources.getQuantityString(
                        R.plurals.start_copying,
                        it.size,
                        it.size
                    )
                }
            }
        }

        viewModel.missingExternalDrives
            .observe(viewLifecycleOwner) { missing ->
                if (missing != null && missing.isNotEmpty()) {
                    val requestAccess = {
                        viewModel.accessIntentsFor(missing)
                            .forEach { resultLauncher.launch(it) }
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
            }

        start_copying.setOnClickListener { startCopying() }

        viewModel.updateDataVolumes()
    }

    private fun <T : FileContainer> spinnerListenerFor(
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
        val transferListOnly = viewModel.transferListOnly.value ?: false
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.container, CopyProgressFragment.newInstance(source, target, transferListOnly, viewModel.filesToCopy.value ?: emptyList()))
            ?.addToBackStack(null)
            ?.commit()
    }
}

