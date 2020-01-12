package li.klass.photo_copy.files.ptp

import li.klass.photo_copy.files.ptp.PtpFileProvider.Companion.captureDateTimeFormatter
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.jupiter.api.Test

class PtpFileProviderTest {
    @Test
    fun should_parse_the_capture_date() {
        assertThat(captureDateTimeFormatter.parseDateTime("20191225T133209"))
            .isEqualTo(DateTime(2019, 12, 25, 13, 32, 9))
    }
}