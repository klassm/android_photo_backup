package li.klass.photo_copy.ui.copy

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.copy_file_result_item.view.*
import li.klass.photo_copy.R
import li.klass.photo_copy.service.CopyResult

class CopyFileResultAdapter(
    private val context: Context,
    objects: List<FileResult>
) : BaseAdapter() {
    var objects: List<FileResult> = objects
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItem(position: Int) = objects[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = objects.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.copy_file_result_item,
            parent,
            false
        )

        view.filename.text = item.fileName
        view.statusImage.setImageDrawable(view.context.getDrawable(when(item.result) {
            CopyResult.SUCCESS -> R.drawable.ic_check_green
            else -> R.drawable.ic_cross_red
        }))

        val errorMessageStringId = when(item.result) {
            CopyResult.SUCCESS -> null
            CopyResult.COPY_FAILURE -> R.string.copy_error_could_not_copy
            CopyResult.INTEGRITY_CHECK_FAILED -> R.string.copy_error_integrity_check_failed
            CopyResult.ERROR -> R.string.copy_error_unknown_error
            CopyResult.TARGET_FILE_CREATION_FAILED -> R.string.copy_error_target_file_creation_failed
            CopyResult.JPG_CREATION_FOR_NEF_FAILED -> R.string.copy_error_jpg_target_file_creation_for_nef_failed
            CopyResult.JPG_COULD_NOT_EXTRACT_JPG_FROM_NEF -> R.string.copy_error_jpg_could_not_extract_jpg_from_nef
            CopyResult.JPG_COULD_NOT_READ_NEF_INPUT_FILE -> R.string.copy_error_jpg_could_not_read_nef_input_file
        }
        view.errorMessage.visibility = if (errorMessageStringId == null) View.GONE else View.VISIBLE
        view.errorMessage.text = errorMessageStringId ?.let { context.getString(it) }

        return view
    }
}