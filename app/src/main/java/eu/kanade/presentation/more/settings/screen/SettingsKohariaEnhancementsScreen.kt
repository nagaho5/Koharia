package eu.kanade.presentation.more.settings.screen

import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
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
import tachiyomi.core.common.preference.Preference as PreferenceData

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
                title = stringResource(MR.strings.pref_enhancement_reader_overlay),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_enhancement_reader_overlay_opacity),
                        content = {
                            IntegerInputPreference(
                                preference = preferences.readerOverlayBackgroundOpacityPercent,
                                title = stringResource(MR.strings.pref_enhancement_reader_overlay_opacity),
                                summary = stringResource(MR.strings.pref_enhancement_reader_overlay_opacity_summary),
                                range = 0..100,
                                suffix = "%",
                            )
                        },
                    ),
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_enhancement_reader_overlay_padding),
                        content = {
                            IntegerInputPreference(
                                preference = preferences.readerOverlayContentPaddingDp,
                                title = stringResource(MR.strings.pref_enhancement_reader_overlay_padding),
                                summary = stringResource(MR.strings.pref_enhancement_reader_overlay_padding_summary),
                                range = 0..10,
                                suffix = " dp",
                            )
                        },
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
                        preference = preferences.bottomSeekEnabled,
                        title = stringResource(MR.strings.pref_enhancement_bottom_seek),
                        subtitle = stringResource(MR.strings.pref_enhancement_bottom_seek_summary),
                    ),
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_enhancement_bottom_seek_height),
                        content = {
                            IntegerInputPreference(
                                preference = preferences.bottomSeekTriggerHeightDp,
                                title = stringResource(MR.strings.pref_enhancement_bottom_seek_height),
                                summary = stringResource(MR.strings.pref_enhancement_bottom_seek_height_summary),
                                range = 10..200,
                                suffix = " dp",
                            )
                        },
                    ),
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_enhancement_bottom_seek_sensitivity),
                        content = {
                            IntegerInputPreference(
                                preference = preferences.bottomSeekSensitivityPercent,
                                title = stringResource(MR.strings.pref_enhancement_bottom_seek_sensitivity),
                                summary = stringResource(MR.strings.pref_enhancement_bottom_seek_sensitivity_summary),
                                range = 1..1000,
                                suffix = "% / screen",
                            )
                        },
                    ),
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_enhancement_bottom_seek_minimum_span),
                        content = {
                            IntegerInputPreference(
                                preference = preferences.bottomSeekMinimumPagesPerScreen,
                                title = stringResource(MR.strings.pref_enhancement_bottom_seek_minimum_span),
                                summary = stringResource(MR.strings.pref_enhancement_bottom_seek_minimum_span_summary),
                                range = 1..10_000,
                                suffix = " pages / screen",
                            )
                        },
                    ),
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

@Composable
private fun IntegerInputPreference(
    preference: PreferenceData<Int>,
    title: String,
    summary: String,
    range: IntRange,
    suffix: String,
) {
    val storedValue by preference.changes().collectAsState(initial = preference.get())
    val value = storedValue.coerceIn(range.first, range.last)
    var dialogVisible by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = "$value$suffix\n$summary",
        onPreferenceClick = { dialogVisible = true },
    )

    if (!dialogVisible) return

    var input by remember(dialogVisible, value) { mutableStateOf(value.toString()) }
    val parsed = input.toIntOrNull()
    val valid = parsed != null && parsed in range

    AlertDialog(
        onDismissRequest = { dialogVisible = false },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit) },
                singleLine = true,
                isError = input.isNotEmpty() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${range.first}–${range.last}$suffix") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    preference.set(checkNotNull(parsed))
                    dialogVisible = false
                },
            ) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { dialogVisible = false }) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
