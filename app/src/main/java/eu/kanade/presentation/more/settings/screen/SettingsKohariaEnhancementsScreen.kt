package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.toast
import koharia.connection.SharedAppPreferences
import koharia.enhancement.KohariaEnhancementPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
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
