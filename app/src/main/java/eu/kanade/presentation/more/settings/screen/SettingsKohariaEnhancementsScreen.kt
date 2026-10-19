package eu.kanade.presentation.more.settings.screen

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.cache.LocalCacheCleaner
import eu.kanade.tachiyomi.util.system.toast
import koharia.connection.SharedAppPreferences
import koharia.enhancement.KohariaEnhancementPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsKohariaEnhancementsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_koharia_enhancements

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val preferences = remember {
            Injekt.get<SharedAppPreferences>().enhancementPreferences()
        }
        val cacheCleaner = remember { Injekt.get<LocalCacheCleaner>() }
        val cacheRevision = remember { mutableIntStateOf(0) }
        val cacheScope = rememberCoroutineScope()
        val cacheBreakdown = remember(cacheRevision.intValue) { cacheCleaner.cacheBreakdown() }
        fun size(bytes: Long): String = Formatter.formatFileSize(context, bytes)
        val tapToClear = stringResource(MR.strings.pref_enhancement_cache_tap_clear)

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_enhancement_comic_cache),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.comicPageCacheSizeMb,
                        entries = persistentMapOf(
                            KohariaEnhancementPreferences.AUTO_PAGE_CACHE_SIZE_MB to
                                stringResource(MR.strings.pref_enhancement_cache_auto),
                            100 to "100 MB",
                            300 to "300 MB",
                            500 to "500 MB",
                            1024 to "1 GB",
                            2048 to "2 GB",
                        ),
                        title = stringResource(MR.strings.pref_enhancement_page_cache_size),
                        subtitle = "%s\n" + stringResource(MR.strings.pref_enhancement_page_cache_size_summary),
                        onValueChanged = {
                            context.toast(MR.strings.requires_app_restart)
                            true
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_enhancement_storage_cache),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_comic_pages),
                        subtitle = "${size(cacheBreakdown.comicPagesBytes)} / " +
                            "${size(cacheBreakdown.comicPagesLimitBytes)}\n$tapToClear",
                        onClick = {
                            cacheCleaner.clearChapterCache()
                            cacheRevision.intValue++
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_epub),
                        subtitle = "${size(cacheBreakdown.epubBytes)} / " +
                            "${size(cacheBreakdown.epubLimitBytes)}\n$tapToClear",
                        onClick = {
                            cacheScope.launch {
                                cacheCleaner.clearEpubCache()
                                cacheRevision.intValue++
                            }
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_covers),
                        subtitle = "${size(cacheBreakdown.coversBytes)}\n$tapToClear",
                        onClick = {
                            cacheCleaner.clearCoverCache()
                            cacheRevision.intValue++
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_network),
                        subtitle = "${size(cacheBreakdown.networkBytes)}\n$tapToClear",
                        onClick = {
                            cacheCleaner.clearNetworkCache()
                            cacheRevision.intValue++
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_metadata),
                        subtitle = "${size(cacheBreakdown.komgaMetadataBytes)}\n$tapToClear",
                        onClick = {
                            cacheCleaner.clearKomgaMetadataCache()
                            cacheRevision.intValue++
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_images),
                        subtitle = "${size(cacheBreakdown.imageBytes)}\n$tapToClear",
                        onClick = {
                            cacheCleaner.clearImageCache()
                            cacheRevision.intValue++
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_other),
                        subtitle = "${size(cacheBreakdown.otherTemporaryBytes)}\n$tapToClear",
                        onClick = {
                            cacheScope.launch {
                                cacheCleaner.clearOtherTemporaryCache()
                                cacheRevision.intValue++
                            }
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_total),
                        subtitle = size(cacheBreakdown.totalRebuildableBytes),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_refresh),
                        onClick = { cacheRevision.intValue++ },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_cache_clear_all),
                        subtitle = stringResource(MR.strings.pref_enhancement_cache_clear_all_summary),
                        onClick = {
                            cacheScope.launch {
                                cacheCleaner.clearAllRebuildableCache()
                                cacheRevision.intValue++
                            }
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_enhancement_komga_reader),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.komgaPrefetchNextPages,
                        entries = persistentMapOf(
                            1 to "1",
                            3 to "3",
                            5 to "5",
                            10 to "10",
                            15 to "15",
                            20 to "20",
                        ),
                        title = stringResource(MR.strings.pref_enhancement_prefetch_next),
                        subtitle = "%s\n" + stringResource(MR.strings.pref_enhancement_prefetch_next_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.komgaPrefetchPreviousPages,
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.off),
                            1 to "1",
                            3 to "3",
                            5 to "5",
                        ),
                        title = stringResource(MR.strings.pref_enhancement_prefetch_previous),
                        subtitle = "%s\n" + stringResource(MR.strings.pref_enhancement_prefetch_previous_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_enhancement_display),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.simplifyKomgaBookTitles,
                        title = stringResource(MR.strings.pref_enhancement_simplify_book_titles),
                        subtitle = stringResource(MR.strings.pref_enhancement_simplify_book_titles_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_enhancement_controls),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.rapidTapPaging,
                        title = stringResource(MR.strings.pref_enhancement_rapid_tap),
                        subtitle = stringResource(MR.strings.pref_enhancement_rapid_tap_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_enhancement_about),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_enhancement_build_identity),
                        subtitle = buildString {
                            append(BuildConfig.APPLICATION_ID)
                            append(" · ")
                            append(BuildConfig.VERSION_NAME)
                            BuildConfig.COMMIT_SHA.takeIf(String::isNotBlank)?.let {
                                append(" · ")
                                append(it.take(7))
                            }
                        },
                    ),
                ),
            ),
        )
    }
}
