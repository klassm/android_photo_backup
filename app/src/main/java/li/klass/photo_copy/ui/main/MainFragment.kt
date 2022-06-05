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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import li.klass.photo_copy.R
import li.klass.photo_copy.databinding.MainFragmentBinding
import li.klass.photo_copy.debounce
import li.klass.photo_copy.model.FileContainer
import li.klass.photo_copy.nullableCombineLatest
import li.klass.photo_copy.ui.copy.CopyProgressFragment

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        const val RELOAD_SD_CARDS = "reload_sd_cards"
    }

    private lateinit var binding: MainFragmentBinding
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
        binding = MainFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateDataVolumes()
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
                binding.sourceCard.onItemSelectedListener =
                    spinnerListenerFor(sources, viewModel.selectedSourceDrive)
                binding.sourceCard.adapter = ExternalDriveAdapter(context, sources)
                if (sources.isEmpty()) {
                    binding.sourceCard.visibility = View.GONE
                    binding.sourceCardEmpty.visibility = View.VISIBLE
                } else {
                    binding.sourceCard.visibility = View.VISIBLE
                    binding.sourceCardEmpty.visibility = View.GONE
                }
            }
        }

        viewModel.targetContainers.observe(viewLifecycleOwner) { targets ->
            context?.let { context ->
                binding.targetCard.onItemSelectedListener =
                    spinnerListenerFor(targets, viewModel.selectedTargetDrive)
                binding.targetCard.adapter = ExternalDriveAdapter(context, targets)
                if (targets.isEmpty()) {
                    binding.targetCard.visibility = View.GONE
                    binding.targetCardEmpty.visibility = View.VISIBLE
                } else {
                    binding.targetCard.visibility = View.VISIBLE
                    binding.targetCardEmpty.visibility = View.GONE
                }
            }
        }


        viewModel.transferListOnly.observe(viewLifecycleOwner) { transferOnly ->
            binding.transferListOnly.visibility = if (transferOnly == null) View.GONE else View.VISIBLE
            binding.transferListOnly.isChecked = transferOnly ?: false
        }

        binding.transferListOnly.setOnCheckedChangeListener { _, isChecked ->
            viewModel.handleTransferListOnlyChange(isChecked)
        }

        viewModel.status.observe(viewLifecycleOwner) {
            updateImageAndErrorMessage(it)
        }

        nullableCombineLatest(
            viewModel.startCopyButtonVisible,
            viewModel.filesToCopy
        ) { a, b -> a to b?.size }
            .observe(viewLifecycleOwner) { (visible, filesToCopy) ->
                binding.startCopying.visibility = if (visible == true) View.VISIBLE else View.GONE
                if (filesToCopy == null) {
                    binding.startCopying.isEnabled = false
                    binding.startCopying.startAnimation()
                }
            }
        viewModel.filesToCopy.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.startCopying.revertAnimation {
                    binding.startCopying.isEnabled = it.isNotEmpty()
                    binding.startCopying.text = resources.getQuantityString(
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

        binding.startCopying.setOnClickListener { startCopying() }

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

    private fun updateImageAndErrorMessage(status: ViewStatus?) {
        when (status) {
            ViewStatus.NO_DRIVES, null -> {
                binding.statusImage.setImageResource(R.drawable.ic_cross_red)
                binding.errorMessage.text = getString(R.string.no_external_drives_cards)
                return
            }
            ViewStatus.INPUT_REQUIRED -> {
                binding.statusImage.setImageResource(R.drawable.ic_question_answer_blue)
                binding.errorMessage.text = getString(R.string.no_source_or_target)
                return
            }
            ViewStatus.RELOADING_VOLUMES -> {
                binding.statusImage.setImageResource(R.drawable.ic_loading)
                binding.errorMessage.text = getString(R.string.updating_data_volumes)
                return
            }
            else -> {
                binding.errorMessage.text = ""
                binding.statusImage.setImageResource(R.drawable.ic_check_green)
            }
        }
    }
}

