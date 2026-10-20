package koharia.enhancement

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class KohariaEnhancementPreferences(
    preferenceStore: PreferenceStore,
) {
    val comicPageCacheSizeMb: Preference<Int> =
        preferenceStore.getInt("custom_comic_page_cache_mb", DEFAULT_PAGE_CACHE_SIZE_MB)

    val komgaPrefetchNextPages: Preference<Int> =
        preferenceStore.getInt("custom_komga_prefetch_next", DEFAULT_PREFETCH_NEXT)

    val komgaPrefetchPreviousPages: Preference<Int> =
        preferenceStore.getInt("custom_komga_prefetch_previous", DEFAULT_PREFETCH_PREVIOUS)

    val simplifyKomgaBookTitles: Preference<Boolean> =
        preferenceStore.getBoolean("custom_komga_simplify_book_titles", true)

    val rapidTapPaging: Preference<Boolean> =
        preferenceStore.getBoolean("custom_reader_rapid_tap", false)

    val readerOverlayBackgroundOpacityPercent: Preference<Int> =
        preferenceStore.getInt(
            "custom_reader_overlay_background_opacity_percent",
            DEFAULT_READER_OVERLAY_BACKGROUND_OPACITY_PERCENT,
        )

    val readerOverlayContentPaddingDp: Preference<Int> =
        preferenceStore.getInt(
            "custom_reader_overlay_content_padding_dp",
            DEFAULT_READER_OVERLAY_CONTENT_PADDING_DP,
        )

    val bottomSeekEnabled: Preference<Boolean> =
        preferenceStore.getBoolean("custom_reader_bottom_seek_enabled", true)

    val bottomSeekTriggerHeightDp: Preference<Int> =
        preferenceStore.getInt(
            "custom_reader_bottom_seek_trigger_height_dp",
            DEFAULT_BOTTOM_SEEK_TRIGGER_HEIGHT_DP,
        )

    val bottomSeekSensitivityPercent: Preference<Int> =
        preferenceStore.getInt(
            "custom_reader_bottom_seek_sensitivity_percent",
            DEFAULT_BOTTOM_SEEK_SENSITIVITY_PERCENT,
        )

    val bottomSeekMinimumPagesPerScreen: Preference<Int> =
        preferenceStore.getInt(
            "custom_reader_bottom_seek_minimum_pages_per_screen",
            DEFAULT_BOTTOM_SEEK_MINIMUM_PAGES_PER_SCREEN,
        )

    companion object {
        const val AUTO_PAGE_CACHE_SIZE_MB = 0
        const val DEFAULT_PAGE_CACHE_SIZE_MB = 500
        const val DEFAULT_PREFETCH_NEXT = 5
        const val DEFAULT_PREFETCH_PREVIOUS = 3
        const val DEFAULT_READER_OVERLAY_BACKGROUND_OPACITY_PERCENT = 50
        const val DEFAULT_READER_OVERLAY_CONTENT_PADDING_DP = 1
        const val DEFAULT_BOTTOM_SEEK_TRIGGER_HEIGHT_DP = 40
        const val DEFAULT_BOTTOM_SEEK_SENSITIVITY_PERCENT = 300
        const val DEFAULT_BOTTOM_SEEK_MINIMUM_PAGES_PER_SCREEN = 50

        val PAGE_CACHE_SIZE_OPTIONS_MB = setOf(0, 100, 300, 500, 1024, 2048)
        val PREFETCH_NEXT_OPTIONS = setOf(1, 3, 5, 10, 15, 20)
        val PREFETCH_PREVIOUS_OPTIONS = setOf(0, 1, 3, 5)

        fun normalizedPageCacheSizeMb(value: Int): Int =
            value.takeIf(PAGE_CACHE_SIZE_OPTIONS_MB::contains) ?: DEFAULT_PAGE_CACHE_SIZE_MB

        fun normalizedPrefetchNext(value: Int): Int =
            value.takeIf(PREFETCH_NEXT_OPTIONS::contains) ?: DEFAULT_PREFETCH_NEXT

        fun normalizedPrefetchPrevious(value: Int): Int =
            value.takeIf(PREFETCH_PREVIOUS_OPTIONS::contains) ?: DEFAULT_PREFETCH_PREVIOUS

        fun normalizedReaderOverlayBackgroundOpacityPercent(value: Int): Int = value.coerceIn(0, 100)

        fun normalizedReaderOverlayContentPaddingDp(value: Int): Int = value.coerceIn(0, 10)

        fun normalizedBottomSeekTriggerHeightDp(value: Int): Int = value.coerceIn(10, 200)

        fun normalizedBottomSeekSensitivityPercent(value: Int): Int = value.coerceIn(1, 1000)

        fun normalizedBottomSeekMinimumPagesPerScreen(value: Int): Int = value.coerceIn(1, 10_000)
    }
}
