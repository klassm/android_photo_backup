package li.klass.photo_copy.files.ptp

import java.io.Serializable

data class DeviceInformation(
    val model: String,
    val manufacturer: String,
    val serialNumber: String
): Serializable