package li.klass.photo_copy.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import li.klass.photo_copy.R
import li.klass.photo_copy.databinding.ExternalDriveDropdownItemBinding
import li.klass.photo_copy.model.ExternalVolume
import li.klass.photo_copy.model.FileContainer
import li.klass.photo_copy.model.FileContainer.SourceContainer.SourcePtp

class ExternalDriveAdapter<T : FileContainer>(
    private val context: Context,
    private val objects: List<T>
) : BaseAdapter() {
    override fun getItem(position: Int) = objects[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = objects.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val binding = when {
            convertView != null -> ExternalDriveDropdownItemBinding.bind(convertView)
            else -> ExternalDriveDropdownItemBinding.inflate(LayoutInflater.from(context), parent, false)
        }

        binding.name.text = textFor(item)
        binding.space.text = spaceTextFor(item)
        binding.root.tag = item

        return binding.root
    }

    private fun textFor(item: T): String = when (item) {
        is SourcePtp -> with(item.deviceInformation) { "$manufacturer $model" }
        is ExternalVolume -> item.volume.volume.getDescription(context)
        else -> "???"
    }

    private fun spaceTextFor(item: T) = when (item) {
        is ExternalVolume -> {
            val stats = item.volume.stats
            context.getString(
                R.string.external_drive_dropdown_space,
                sizeTextFor(stats.availableBytes), sizeTextFor(stats.usedBytes)
            )
        }
        else -> ""
    }

    private fun sizeTextFor(bytes: Long): String {
        if (bytes / GB > 1) return "${(bytes / GB).toInt()} GB"
        return "${(bytes / MB).toInt()} MB"
    }

    companion object {
        const val MB: Float = (1024 * 1024).toFloat()
        const val GB: Float = (MB * 1024)
    }
}