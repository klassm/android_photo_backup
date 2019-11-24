package li.klass.photo_copy.ui.copy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.copying_progress.*
import li.klass.photo_copy.R
import li.klass.photo_copy.model.ExternalDriveDocument.PossibleTargetExternalDrive
import li.klass.photo_copy.model.ExternalDriveDocument.SourceExternalDrive

class CopyProgressFragment : Fragment() {
    private lateinit var viewModel: CopyProgressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: Bundle()
        viewModel = ViewModelProviders.of(this).get(CopyProgressViewModel::class.java)
        viewModel.apply {
            source = args.get("source") as SourceExternalDrive
            target = args.get("target") as PossibleTargetExternalDrive
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.copying_progress, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val copyFileResultAdapter = context?.let { CopyFileResultAdapter(it, emptyList()) }
        file_progress_list.adapter = copyFileResultAdapter

        viewModel.copyProgress.observe(this, Observer { copyProgress ->
            if (copyProgress != null) {
                progress.setProgress(copyProgress.progress, true)
                current_file_index.text = copyProgress.currentIndex.toString()
                total_file_index.text = copyProgress.totalAmount.toString()
                back.visibility =
                    if (copyProgress.currentIndex == copyProgress.totalAmount) View.VISIBLE else View.GONE
                copyFileResultAdapter?.objects = copyProgress.results.sortedBy { it.date }.reversed()
                progress_data.visibility = View.VISIBLE
                file_progress_list.visibility = View.VISIBLE
            } else {
                back.visibility = View.GONE
                progress_data.visibility = View.GONE
                file_progress_list.visibility = View.GONE
            }
        })

        back.setOnClickListener {
            activity?.supportFragmentManager?.popBackStackImmediate()
        }

        viewModel.startCopying()
    }

    companion object {
        fun newInstance(
            source: SourceExternalDrive,
            target: PossibleTargetExternalDrive
        ): CopyProgressFragment =
            CopyProgressFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("source", source)
                    putSerializable("target", target)
                }
            }
    }
}