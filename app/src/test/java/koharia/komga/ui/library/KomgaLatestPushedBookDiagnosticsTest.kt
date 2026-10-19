package koharia.komga.ui.library

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KomgaLatestPushedBookDiagnosticsTest {

    @Test
    fun `failure summary includes stage exception type and message`() {
        val error = KomgaLatestBookException(
            stage = KomgaLatestBookStage.PREPARE,
            bookUrl = "/api/v1/books/test",
            cause = IllegalStateException("chapter missing"),
        )

        val summary = komgaLatestBookFailureSummary(error)

        assertTrue(summary.contains("PREPARE"))
        assertTrue(summary.contains("IllegalStateException"))
        assertTrue(summary.contains("chapter missing"))
    }
}
