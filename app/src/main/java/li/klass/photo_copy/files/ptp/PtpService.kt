package li.klass.photo_copy.files.ptp

import android.util.Log
import com.fimagena.libptp.PtpConnection
import com.fimagena.libptp.PtpDataType
import com.fimagena.libptp.PtpDataType.StorageID
import com.fimagena.libptp.PtpSession
import com.fimagena.libptp.ptpip.PtpIpConnection
import com.fimagena.libptp.ptpip.PtpIpConnection.PtpIpAddress
import com.fimagena.libptp.ptpip.PtpIpConnection.PtpIpHostId
import li.klass.photo_copy.files.CopyableFile
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.net.InetAddress

class PtpService {
    private val ipAddress = "192.168.1.1"

    fun getDeviceInformation() =
        runConnected {
            it.connection.deviceInfo
        }?.let {
            DeviceInformation(
                model = it.mModel.mString,
                manufacturer = it.mManufacturer.mString
            )
        }

    fun fetchFile(ptpFile: CopyableFile.PtpFile) =
        runConnected {
            it.getObject(ptpFile.objectHandle)
        }

    fun getAvailableFiles() =
        runConnected {
            it.getObjectHandles(allStorageId, typeImage).map { handle ->
                val info = it.getObjectInfo(handle)
                CopyableFile.PtpFile(
                    filename = info.mFilename.mString,
                    objectHandle = handle,
                    captureDate = captureDateTimeFormatter.parseDateTime(info.mCaptureDate.mString)
                )
            }
        }

    private fun <T> runConnected(
        function: (ptpSession: PtpSession) -> T
    ): T? {
        val address = PtpIpAddress(InetAddress.getByName(ipAddress))
        val hostId = PtpIpHostId(guid, friendlyName, 1, 1)
        val transport = PtpIpConnection()
        val ptpConnection = PtpConnection(transport)

        return try {
            ptpConnection.connect(address, hostId, 2000)
            function(ptpConnection.openSession(2000))
        } catch(e: Exception) {
            Log.e(logTag, "runConnected - could not execute command", e)
            null
        } finally {
            ptpConnection.close()
        }
    }

    companion object {
        private val guid = shortArrayOf(
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0xff,
            0xff,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        ) // adjust MAC

        private const val friendlyName = "PhotoCopy.ptp"
        private val allStorageId = StorageID(0xFFFFFFFF)
        private val typeImage = PtpDataType.ObjectFormatCode(0x3000)
        private val logTag = PtpService::class.java.name
        val captureDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("YYYYMMdd'T'HHmmss")
    }
}