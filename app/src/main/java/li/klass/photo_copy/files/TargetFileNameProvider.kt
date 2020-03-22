package li.klass.photo_copy.files

class TargetFileNameProvider(private val exifDataProvider: ExifDataProvider) {
    fun getTargetFileName(inFile: CopyableFile): String {
        val exifData = exifDataProvider.exifDataFor(inFile)
        val targetFileName = inFile.filename
        val prefix = exifData.model ?.let { it + "_" }

        return prefix + targetFileName
    }
}