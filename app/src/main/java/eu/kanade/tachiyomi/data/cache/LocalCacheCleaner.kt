package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.network.NetworkHelper
import koharia.epub.cache.EpubCacheManager
import koharia.pdf.cache.PdfReflowCacheManager
import tachiyomi.core.common.storage.LocalTempCacheDirectoryProvider
import tachiyomi.domain.manga.model.Manga

data class LocalCacheBreakdown(
    val comicPagesBytes: Long,
    val comicPagesLimitBytes: Long,
    val epubBytes: Long,
    val epubLimitBytes: Long,
    val coversBytes: Long,
    val networkBytes: Long,
    val komgaMetadataBytes: Long,
    val imageBytes: Long,
    val otherTemporaryBytes: Long,
) {
    val totalRebuildableBytes: Long
        get() = comicPagesBytes + epubBytes + coversBytes + networkBytes +
            komgaMetadataBytes + imageBytes + otherTemporaryBytes
}

class LocalCacheCleaner(
    private val context: Context,
    private val chapterCache: ChapterCache,
    private val coverCache: CoverCache,
    private val downloadCache: DownloadCache,
    private val networkHelper: NetworkHelper,
    private val epubCacheManager: EpubCacheManager,
    private val pdfReflowCacheManager: PdfReflowCacheManager,
) {

    fun temporaryCacheReadableSize(): String {
        return Formatter.formatFileSize(
            context,
            LocalTempCacheDirectoryProvider.temporaryCacheSize(context) + koharia.smanga.SmangaPdfCache.size(context) +
                koharia.kavita.KavitaPdfCache.size(context),
        )
    }

    fun cacheBreakdown(): LocalCacheBreakdown {
        val comicPages = LocalTempCacheDirectoryProvider.chapterCacheSize(context)
        val epub = LocalTempCacheDirectoryProvider.epubCacheSize(context)
        val network = LocalTempCacheDirectoryProvider.networkCacheSize(context)
        val metadata = LocalTempCacheDirectoryProvider.metadataCacheSize(context)
        val image = LocalTempCacheDirectoryProvider.coilDiskCacheSize(context) +
            LocalTempCacheDirectoryProvider.sharedImageCacheSize(context)
        val knownTemporary = comicPages + epub + network + metadata + image
        val allTemporary = LocalTempCacheDirectoryProvider.temporaryCacheSize(context) +
            koharia.smanga.SmangaPdfCache.size(context) +
            koharia.kavita.KavitaPdfCache.size(context)

        return LocalCacheBreakdown(
            comicPagesBytes = comicPages,
            comicPagesLimitBytes = chapterCache.maxSizeBytes,
            epubBytes = epub,
            epubLimitBytes = epubCacheManager.maxSizeBytes(),
            coversBytes = coverCache.sizeBytes(),
            networkBytes = network,
            komgaMetadataBytes = metadata,
            imageBytes = image,
            otherTemporaryBytes = (allTemporary - knownTemporary).coerceAtLeast(0L),
        )
    }

    fun clearDeletedMangaCache(activeManga: Collection<Manga>): Int {
        return coverCache.deleteOrphaned(activeManga)
    }

    fun clearChapterCache(): Int {
        return chapterCache.clear()
    }

    fun clearCoverCache(): Int {
        return coverCache.clear()
    }

    fun clearNetworkCache(): Int = networkHelper.clearDiskCache()

    fun clearKomgaMetadataCache(): Int = LocalTempCacheDirectoryProvider.clearMetadataCache(context)

    fun clearImageCache(): Int {
        return LocalTempCacheDirectoryProvider.clearCoilDiskCache(context) +
            LocalTempCacheDirectoryProvider.clearSharedImageCache(context)
    }

    suspend fun clearEpubCache(): Int = epubCacheManager.clear()

    suspend fun clearAllRebuildableCache(): Int {
        return clearCoverCache() + clearAllTemporaryCache()
    }

    suspend fun clearAllTemporaryCache(): Int {
        var deleted = 0
        deleted += clearChapterCache()
        deleted += networkHelper.clearDiskCache()
        deleted += LocalTempCacheDirectoryProvider.clearMetadataCache(context)
        deleted += downloadCache.clearDiskCache()
        deleted += LocalTempCacheDirectoryProvider.clearCoilDiskCache(context)
        deleted += LocalTempCacheDirectoryProvider.clearSharedImageCache(context)
        deleted += epubCacheManager.clear()
        deleted += pdfReflowCacheManager.clear()
        deleted += koharia.smanga.SmangaPdfCache.clear(context)
        deleted += koharia.kavita.KavitaPdfCache.clear(context)
        return deleted
    }
}
