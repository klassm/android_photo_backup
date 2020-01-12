package li.klass.photo_copy.files.ptp

import android.util.Log
import androidx.room.RoomDatabase
import com.fimagena.libptp.PtpConnection
import com.fimagena.libptp.PtpDataType
import com.fimagena.libptp.PtpDataType.StorageID
import com.fimagena.libptp.PtpSession
import com.fimagena.libptp.ptpip.PtpIpConnection
import com.fimagena.libptp.ptpip.PtpIpConnection.PtpIpAddress
import com.fimagena.libptp.ptpip.PtpIpConnection.PtpIpHostId
import li.klass.photo_copy.files.CopyableFile
import li.klass.photo_copy.files.ptp.database.PtpItemDao
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.net.InetAddress

object PtpConnectionSingleton {
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
    private const val ipAddress = "192.168.1.1"
    private val logTag = PtpConnectionSingleton::class.java.name
    private var connection: PtpConnection? = null

    @Synchronized
    private fun connect(): PtpConnection {
        with(connection) {
            if (this != null && this.isConnected) {
                return this
            }

            val address = PtpIpAddress(InetAddress.getByName(ipAddress))
            val hostId = PtpIpHostId(guid, friendlyName, 1, 1)
            val transport = PtpIpConnection()
            val ptpConnection = PtpConnection(transport)
            ptpConnection.connect(address, hostId, 2000)
            connection = ptpConnection
            return ptpConnection
        }
    }

    fun <T> runConnected(
        function: (ptpSession: PtpSession) -> T
    ): T? {
        return try {
            connect().openSession(2000).use {
                function(it)
            }
        } catch (e: Exception) {
            Log.i(logTag, "runConnected - could not execute command", e)
            null
        }
    }
}

class PtpService {
    fun getDeviceInformation() =
        runConnected {
            Log.i(logTag, "getDeviceInformation()")
            it.connection.deviceInfo
        }?.let {
            DeviceInformation(
                model = it.mModel.mString,
                manufacturer = it.mManufacturer.mString,
                serialNumber = it.mSerialNumber.mString
            )
        }

    fun fetchFile(handle: PtpDataType.ObjectHandle) =
        runConnected {
            Log.i(logTag, "fetchFile(handle=$handle)")
            it.notifyFileAcquisitionStart(handle)
            val result = it.getObject(handle)
            it.notifyFileAcquisitionEnd(handle)
            result
        }

    fun getAllFiles(): Array<PtpDataType.ObjectHandle>? = runConnected {
        Log.i(logTag, "getAvailableFiles()")
        it.getObjectHandles(allStorageId, typeImage)
    }

    fun getTransferList(): Array<PtpDataType.ObjectHandle>? = runConnected {
        Log.i(logTag, "getTransferList()")
        it.transferList
    }

    fun getObjectInfoFor(handle: PtpDataType.ObjectHandle) = runConnected {
        it.getObjectInfo(handle)
    }

    private fun <T> runConnected(
        function: (ptpSession: PtpSession) -> T
    ): T? = PtpConnectionSingleton.runConnected(function)

    companion object {
        private val allStorageId = StorageID(0xFFFFFFFF)
        private val typeImage = PtpDataType.ObjectFormatCode(0x3000)
        private val logTag = PtpService::class.java.name
    }
}