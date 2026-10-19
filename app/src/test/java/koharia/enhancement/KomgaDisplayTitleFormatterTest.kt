package koharia.enhancement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KomgaDisplayTitleFormatterTest {

    @Test
    fun `removes event and author prefixes but keeps trailing metadata`() {
        assertEquals(
            "Real Title (Original) [DL版]",
            simplifyKomgaDisplayTitle("(C108) [Circle (Author)] Real Title (Original) [DL版]"),
        )
    }

    @Test
    fun `removes a leading author block without an event prefix`() {
        assertEquals(
            "Real Title [DL版]",
            simplifyKomgaDisplayTitle("[Author] Real Title [DL版]"),
        )
    }

    @Test
    fun `preserves corner bracket titles and trailing tags`() {
        val title = "【Real Title】 [AI Generated]"
        assertEquals(title, simplifyKomgaDisplayTitle(title))
    }

    @Test
    fun `does not keep stripping after the first square prefix`() {
        assertEquals(
            "(Notice)Real Title",
            simplifyKomgaDisplayTitle("[123] (Notice)Real Title"),
        )
    }

    @Test
    fun `falls back to the original when prefixes consume the whole title`() {
        val title = "(C108) [Author]"
        assertEquals(title, simplifyKomgaDisplayTitle(title))
    }
}
