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

    val komgaBookHistoryMode: Preference<Int> =
        preferenceStore.getInt("custom_komga_book_history_mode", BOOK_HISTORY_MODE_BOOK)

    val rapidTapPaging: Preference<Boolean> =
        preferenceStore.getBoolean("custom_reader_rapid_tap", false)

    companion object {
        const val AUTO_PAGE_CACHE_SIZE_MB = 0
        const val DEFAULT_PAGE_CACHE_SIZE_MB = 500
        const val DEFAULT_PREFETCH_NEXT = 5
        const val DEFAULT_PREFETCH_PREVIOUS = 3
        const val BOOK_HISTORY_MODE_BOOK = 0
        const val BOOK_HISTORY_MODE_SERIES = 1

        val PAGE_CACHE_SIZE_OPTIONS_MB = setOf(0, 100, 300, 500, 1024, 2048)
        val PREFETCH_NEXT_OPTIONS = setOf(1, 3, 5, 10, 15, 20)
        val PREFETCH_PREVIOUS_OPTIONS = setOf(0, 1, 3, 5)

        fun normalizedPageCacheSizeMb(value: Int): Int =
            value.takeIf(PAGE_CACHE_SIZE_OPTIONS_MB::contains) ?: DEFAULT_PAGE_CACHE_SIZE_MB

        fun normalizedPrefetchNext(value: Int): Int =
            value.takeIf(PREFETCH_NEXT_OPTIONS::contains) ?: DEFAULT_PREFETCH_NEXT

        fun normalizedPrefetchPrevious(value: Int): Int =
            value.takeIf(PREFETCH_PREVIOUS_OPTIONS::contains) ?: DEFAULT_PREFETCH_PREVIOUS
    }
}
