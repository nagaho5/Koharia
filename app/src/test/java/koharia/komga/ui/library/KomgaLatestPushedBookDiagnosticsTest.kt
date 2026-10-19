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
    @Test
    fun `current Komga book URL is excluded from reader latest-book selection`() {
        assertTrue(
            sameKomgaBookUrl(
                candidateUrl = "http://server/api/v1/books/current/",
                excludedBookUrl = "http://server/api/v1/books/current#reader",
            ),
        )
        assertTrue(
            !sameKomgaBookUrl(
                candidateUrl = "http://server/api/v1/books/pushed",
                excludedBookUrl = "http://server/api/v1/books/current",
            ),
        )
    }

}
