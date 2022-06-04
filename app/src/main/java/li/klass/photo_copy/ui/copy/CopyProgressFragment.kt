package li.klass.photo_copy.ui.copy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import li.klass.photo_copy.R
import li.klass.photo_copy.databinding.CopyingProgressBinding
import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.model.FileContainer.*

class CopyProgressFragment : Fragment() {
    private val viewModel: CopyProgressViewModel by viewModels()
    private lateinit var binding: CopyingProgressBinding

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: Bundle()
        viewModel.apply {
            source = args.get("source") as SourceContainer
            target = args.get("target") as TargetContainer
            transferListOnly = args.getBoolean("transferListOnly")
            filesToCopy = args.get("filesToCopy") as List<CopyableFile>
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = CopyingProgressBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val copyFileResultAdapter = context?.let { CopyFileResultAdapter(it, emptyList()) }
        binding.fileProgressList.adapter = copyFileResultAdapter

        viewModel.copyProgress.observe(viewLifecycleOwner, Observer { copyProgress ->
            if (copyProgress != null) {
                binding.progress.setProgress(copyProgress.progress, true)
                binding.currentFileIndex.text = copyProgress.currentIndex.toString()
                binding.totalFileIndex.text = copyProgress.totalAmount.toString()
                binding.back.visibility =
                    if (copyProgress.currentIndex == copyProgress.totalAmount) View.VISIBLE else View.GONE
                copyFileResultAdapter?.objects = copyProgress.results.sortedBy { it.date }.reversed()
                binding.progressData.visibility = View.VISIBLE
                binding.fileProgressList.visibility = View.VISIBLE
            } else {
                binding.back.visibility = View.GONE
                binding.progressData.visibility = View.GONE
                binding.fileProgressList.visibility = View.GONE
            }
        })

        binding.back.setOnClickListener {
            activity?.supportFragmentManager?.popBackStackImmediate()
        }

        viewModel.startCopying()
    }

    companion object {
        fun newInstance(
            source: SourceContainer,
            target: TargetContainer,
            transferListOnly: Boolean,
            filesToCopy: Collection<CopyableFile>
        ): CopyProgressFragment =
            CopyProgressFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("source", source)
                    putSerializable("target", target)
                    putSerializable("transferListOnly", transferListOnly)
                    putSerializable("filesToCopy", ArrayList<CopyableFile>(filesToCopy))
                }
            }
    }
}