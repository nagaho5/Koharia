package koharia.enhancement

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class KohariaEnhancementPreferencesTest {

    @Test
    fun `custom reader preferences use the agreed defaults`() {
        val preferences = KohariaEnhancementPreferences(InMemoryPreferenceStore())

        preferences.comicPageCacheSizeMb.get() shouldBe 500
        preferences.komgaPrefetchNextPages.get() shouldBe 5
        preferences.komgaPrefetchPreviousPages.get() shouldBe 3
        preferences.rapidTapPaging.get() shouldBe false
    }

    @Test
    fun `unsupported stored values fall back to safe defaults`() {
        KohariaEnhancementPreferences.normalizedPageCacheSizeMb(777) shouldBe 500
        KohariaEnhancementPreferences.normalizedPrefetchNext(9) shouldBe 5
        KohariaEnhancementPreferences.normalizedPrefetchPrevious(2) shouldBe 3
    }

    @Test
    fun `all exposed values survive normalization`() {
        KohariaEnhancementPreferences.PAGE_CACHE_SIZE_OPTIONS_MB.forEach {
            KohariaEnhancementPreferences.normalizedPageCacheSizeMb(it) shouldBe it
        }
        KohariaEnhancementPreferences.PREFETCH_NEXT_OPTIONS.forEach {
            KohariaEnhancementPreferences.normalizedPrefetchNext(it) shouldBe it
        }
        KohariaEnhancementPreferences.PREFETCH_PREVIOUS_OPTIONS.forEach {
            KohariaEnhancementPreferences.normalizedPrefetchPrevious(it) shouldBe it
        }
    }
}
