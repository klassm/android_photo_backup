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

        viewModel.copyProgress.observe(this, Observer {
            if (it != null) {
                progress.setProgress(it.progress, true)
                current_file.text = it.currentFile
                current_file_index.text = (it.currentIndex + 1).toString()
                total_file_index.text = it.totalAmount.toString()
                back.visibility =
                    if (it.currentIndex + 1 == it.totalAmount) View.VISIBLE else View.GONE
                progress_data.visibility = View.VISIBLE
            } else {
                back.visibility = View.GONE
                progress_data.visibility = View.GONE
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