package koharia.komga.ui.library

import android.content.Context
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.crash.CrashDiagnostics
import koharia.domain.manga.model.toDomainManga
import koharia.source.komga.KomgaLibraryScope
import koharia.source.komga.KomgaSource
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class PreparedKomgaLatestBook(
    val manga: Manga,
    val chapter: Chapter,
)

internal enum class KomgaLatestBookStage {
    LOOKUP,
    MATERIALIZE,
    PREPARE,
    LAUNCH,
}

internal class KomgaLatestBookException(
    val stage: KomgaLatestBookStage,
    val bookUrl: String?,
    cause: Throwable,
) : RuntimeException(
    "Latest pushed Komga book failed at $stage" +
        (bookUrl?.let { " for $it" } ?: ""),
    cause,
)

internal class KomgaLatestPushedBookResolver(
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val chapterRepository: ChapterRepository = Injekt.get(),
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get(),
) {

    suspend fun resolveAndPrepare(
        source: KomgaSource,
        libraryId: String? = null,
        allowedLibraryIds: Set<String>? = null,
        libraryScope: KomgaLibraryScope = KomgaLibraryScope.ALL,
    ): PreparedKomgaLatestBook? {
        val remote = atStage(KomgaLatestBookStage.LOOKUP, null) {
            val filters = source.buildFilterListForLibrary(
                libraryId = libraryId,
                allowedLibraryIds = allowedLibraryIds,
                libraryScope = libraryScope,
            ).configureLatestPushedBookQuery()
            source.getFreshSearchManga(1, "", filters).mangas.firstOrNull()
        } ?: return null

        val remoteManga = remote.toDomainManga(source.id)
        val localManga = atStage(KomgaLatestBookStage.MATERIALIZE, remoteManga.url) {
            mangaRepository.getMangaByUrlAndSourceId(remoteManga.url, source.id)
                ?: networkToLocalManga(remoteManga)
        }

        val chapter = atStage(KomgaLatestBookStage.PREPARE, localManga.url) {
            chapterRepository.getChapterByMangaId(localManga.id).singleOrNull()
                ?: run {
                    syncChaptersWithSource.await(
                        source.getChapterList(localManga.toSManga()),
                        localManga,
                        source,
                    )
                    chapterRepository.getChapterByMangaId(localManga.id).singleOrNull()
                        ?: error("Prepared Komga book does not have exactly one chapter")
                }
        }

        logcat(LogPriority.INFO) {
            "Komga latest-pushed prepared sourceId=${source.id} mangaId=${localManga.id} " +
                "chapterId=${chapter.id} bookUrl=${localManga.url}"
        }
        return PreparedKomgaLatestBook(localManga, chapter)
    }

    private suspend fun <T> atStage(
        stage: KomgaLatestBookStage,
        bookUrl: String?,
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw KomgaLatestBookException(stage, bookUrl, error)
        }
    }
}

internal fun komgaLatestBookLaunchException(bookUrl: String?, error: Throwable): Throwable {
    if (error is CancellationException) throw error
    return if (error is KomgaLatestBookException) {
        error
    } else {
        KomgaLatestBookException(KomgaLatestBookStage.LAUNCH, bookUrl, error)
    }
}

internal fun recordKomgaLatestBookFailure(
    context: Context,
    origin: String,
    sourceId: Long,
    error: Throwable,
) {
    val failure = error as? KomgaLatestBookException
    val stage = failure?.stage ?: KomgaLatestBookStage.LAUNCH
    val root = failure?.cause ?: error
    val details = buildString {
        append("origin=")
        append(origin)
        append(" sourceId=")
        append(sourceId)
        append(" stage=")
        append(stage.name)
        append(" bookUrl=")
        append(failure?.bookUrl ?: "unknown")
        append(" exception=")
        append(root::class.qualifiedName ?: root.javaClass.name)
        append(" message=")
        append(root.message ?: "<none>")
    }
    val diagnostic = IllegalStateException(details, root)
    context.logcat(LogPriority.ERROR, diagnostic) { "Komga latest-pushed failure: $details" }
    CrashDiagnostics.recordNonFatal(
        context,
        "komga.latest-pushed.$origin.${stage.name.lowercase()}",
        diagnostic,
    )
}

internal fun komgaLatestBookFailureSummary(error: Throwable): String {
    val failure = error as? KomgaLatestBookException
    val stage = failure?.stage ?: KomgaLatestBookStage.LAUNCH
    val root = failure?.cause ?: error
    val type = root::class.simpleName ?: root.javaClass.simpleName.ifBlank { "Throwable" }
    val message = root.message?.trim()?.takeIf(String::isNotBlank)?.take(180)
    return buildString {
        append(stage.name)
        append(" · ")
        append(type)
        if (message != null) {
            append(": ")
            append(message)
        }
    }
}
