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
        preferences.renderWarmupMode.get() shouldBe KohariaEnhancementPreferences.RENDER_WARMUP_MODE_ADAPTIVE
        preferences.renderWarmupForwardPages.get() shouldBe 5
        preferences.renderWarmupBackwardPages.get() shouldBe 1
        preferences.renderWarmupConcurrency.get() shouldBe 5
        preferences.adaptiveRenderForwardStrengthPercent.get() shouldBe 100
        preferences.adaptiveRenderBackwardStrengthPercent.get() shouldBe 100
        preferences.adaptiveRenderConcurrencyStrengthPercent.get() shouldBe 100
        preferences.adaptiveRenderMaxForwardPages.get() shouldBe 20
        preferences.rapidTapPaging.get() shouldBe false
        preferences.readerOverlayBackgroundOpacityPercent.get() shouldBe 50
        preferences.readerOverlayContentPaddingDp.get() shouldBe 1
        preferences.bottomSeekEnabled.get() shouldBe true
        preferences.bottomSeekTriggerHeightDp.get() shouldBe 40
        preferences.bottomSeekSensitivityPercent.get() shouldBe 300
        preferences.bottomSeekMinimumPagesPerScreen.get() shouldBe 50
    }

    @Test
    fun `unsupported stored values fall back to safe defaults`() {
        KohariaEnhancementPreferences.normalizedPageCacheSizeMb(777) shouldBe 500
        KohariaEnhancementPreferences.normalizedPrefetchNext(0) shouldBe 5
        KohariaEnhancementPreferences.normalizedPrefetchNext(101) shouldBe 5
        KohariaEnhancementPreferences.normalizedPrefetchPrevious(-1) shouldBe 3
        KohariaEnhancementPreferences.normalizedPrefetchPrevious(51) shouldBe 3
        KohariaEnhancementPreferences.normalizedRenderWarmupMode(-1) shouldBe
            KohariaEnhancementPreferences.RENDER_WARMUP_MODE_ADAPTIVE
        KohariaEnhancementPreferences.normalizedRenderWarmupForward(0) shouldBe 1
        KohariaEnhancementPreferences.normalizedRenderWarmupForward(31) shouldBe 30
        KohariaEnhancementPreferences.normalizedRenderWarmupBackward(-1) shouldBe 0
        KohariaEnhancementPreferences.normalizedRenderWarmupBackward(11) shouldBe 10
        KohariaEnhancementPreferences.normalizedRenderWarmupConcurrency(0) shouldBe 1
        KohariaEnhancementPreferences.normalizedRenderWarmupConcurrency(21) shouldBe 20
        KohariaEnhancementPreferences.normalizedAdaptiveRenderStrengthPercent(9) shouldBe 10
        KohariaEnhancementPreferences.normalizedAdaptiveRenderStrengthPercent(201) shouldBe 200
        KohariaEnhancementPreferences.normalizedAdaptiveRenderMaxForwardPages(4) shouldBe 5
        KohariaEnhancementPreferences.normalizedAdaptiveRenderMaxForwardPages(31) shouldBe 30
        KohariaEnhancementPreferences.normalizedReaderOverlayBackgroundOpacityPercent(-1) shouldBe 0
        KohariaEnhancementPreferences.normalizedReaderOverlayBackgroundOpacityPercent(101) shouldBe 100
        KohariaEnhancementPreferences.normalizedReaderOverlayContentPaddingDp(-1) shouldBe 0
        KohariaEnhancementPreferences.normalizedReaderOverlayContentPaddingDp(11) shouldBe 10
        KohariaEnhancementPreferences.normalizedBottomSeekTriggerHeightDp(9) shouldBe 10
        KohariaEnhancementPreferences.normalizedBottomSeekTriggerHeightDp(201) shouldBe 200
        KohariaEnhancementPreferences.normalizedBottomSeekSensitivityPercent(0) shouldBe 1
        KohariaEnhancementPreferences.normalizedBottomSeekSensitivityPercent(1001) shouldBe 1000
        KohariaEnhancementPreferences.normalizedBottomSeekMinimumPagesPerScreen(0) shouldBe 1
        KohariaEnhancementPreferences.normalizedBottomSeekMinimumPagesPerScreen(10_001) shouldBe 10_000
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
