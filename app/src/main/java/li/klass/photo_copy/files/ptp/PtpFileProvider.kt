package li.klass.photo_copy.files.ptp

import android.util.Log
import com.fimagena.libptp.PtpDataType
import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.files.ptp.database.PtpItemDao
import li.klass.photo_copy.files.ptp.database.PtpItemEntity
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class PtpFileProvider(private val ptpService: PtpService, private val ptpItemDao: PtpItemDao) {
    fun getFilesFor(transferListOnly: Boolean): List<CopyableFile.PtpFile> {
        val deviceInfo = ptpService.getDeviceInformation() ?: return emptyList()
        val uids = getAvailableUidsFor(transferListOnly)
        val known = ptpItemDao.getAllBy(
            uids = uids.toLongArray(),
            serialNumber = deviceInfo.serialNumber,
            manufacturer = deviceInfo.manufacturer
        ).map { it.uid to it }.toMap()

        val unknownEntries = uids.asSequence()
            .filterNot { known.contains(it) }
            .map { it to ptpService.getObjectInfoFor(PtpDataType.ObjectHandle(it)) }
            .filterNot { it.second == null }
            .map { (uid, info) -> uid to info!! }
            .map { (uid, info) ->
                PtpItemEntity(
                    uid = uid,
                    manufacturer = deviceInfo.manufacturer,
                    serialNumber = deviceInfo.serialNumber,
                    captureDate = info.mCaptureDate.mString,
                    fileName = info.mFilename.mString
                )
            }.toList()

        ptpItemDao.saveAll(unknownEntries)

        Log.i(logTag, "getFilesFor(transferListOnly=$transferListOnly) - dbcache=${known.size}, unknown=${unknownEntries.size}")
        return (known.values + unknownEntries)
            .map {toCopyableFile(it) }
    }

    fun fetchFile(ptpFile: CopyableFile.PtpFile) = ptpService.fetchFile(
        PtpDataType.ObjectHandle(ptpFile.uid)
    )

    private fun getAvailableUidsFor(transferListOnly: Boolean): List<Long> {
        val handles = when (transferListOnly) {
            true -> ptpService.getTransferList()
            else -> ptpService.getAllFiles()
        }?.toList() ?: emptyList()
        return handles.map { it.mValue }
    }

    private fun toCopyableFile(
        entity: PtpItemEntity
    ) = CopyableFile.PtpFile(
        filename = entity.fileName,
        uid =  entity.uid,
        captureDate = captureDateTimeFormatter.parseDateTime(entity.captureDate)
    )

    companion object {
        val captureDateTimeFormatter: DateTimeFormatter =
            DateTimeFormat.forPattern("YYYYMMdd'T'HHmmss")
        private val logTag = PtpFileProvider::class.java.name
    }
}