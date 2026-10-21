package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterGap
import eu.kanade.tachiyomi.util.system.createReaderThemeContext
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import koharia.connection.SharedAppPreferences
import koharia.enhancement.KohariaEnhancementPreferences
import koharia.enhancement.RenderWarmupPolicy
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PagerViewerAdapter(private val viewer: PagerViewer) : ViewPagerAdapter() {

    var slots: MutableList<PagerSlot> = mutableListOf()
        private set

    private var sourceItems: MutableList<SourceItem> = mutableListOf()
    private val splitPages = mutableMapOf<ReaderPage, InsertPage>()

    var nextTransition: ChapterTransition.Next? = null
        private set

    var currentChapter: ReaderChapter? = null

    private var readerThemedContext = viewer.activity.createReaderThemeContext()
    private val enhancementPreferences by lazy {
        Injekt.get<SharedAppPreferences>().enhancementPreferences()
    }
    private val renderWarmHolders = linkedMapOf<PagerSlot.Pages, PagerPageHolder>()
    private val activeRenderWarmups = linkedSetOf<PagerSlot.Pages>()
    private var desiredForwardWarmSlots = emptyList<PagerSlot.Pages>()
    private var desiredBackwardWarmSlots = emptyList<PagerSlot.Pages>()
    private var desiredRenderWarmSlots = emptyList<PagerSlot.Pages>()
    private var renderWarmupConcurrency = KohariaEnhancementPreferences.DEFAULT_RENDER_WARMUP_CONCURRENCY

    fun setChapters(
        chapters: ViewerChapters,
        forceTransition: Boolean,
        anchor: ReaderPage?,
    ) {
        val newItems = mutableListOf<SourceItem>()
        val activePages = listOfNotNull(chapters.prevChapter, chapters.currChapter, chapters.nextChapter)
            .flatMap { it.pages.orEmpty() }
            .toSet()
        splitPages.keys.retainAll(activePages)

        fun appendPages(chapter: ReaderChapter?) {
            chapter?.pages?.forEach { page ->
                newItems.add(SourceItem.Page(page))
                splitPages[page]?.let { newItems.add(SourceItem.Page(it)) }
            }
        }

        val prevHasMissingChapters = calculateChapterGap(chapters.currChapter, chapters.prevChapter) > 0
        val nextHasMissingChapters = calculateChapterGap(chapters.nextChapter, chapters.currChapter) > 0

        appendPages(chapters.prevChapter)
        if (prevHasMissingChapters || forceTransition || chapters.prevChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(SourceItem.Transition(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter)))
        }

        appendPages(chapters.currChapter)

        currentChapter = chapters.currChapter
        nextTransition = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter).also { transition ->
            if (nextHasMissingChapters || forceTransition ||
                chapters.nextChapter?.state !is ReaderChapter.State.Loaded
            ) {
                newItems.add(SourceItem.Transition(transition))
            }
        }
        appendPages(chapters.nextChapter)

        sourceItems = newItems
        rebuildSlots(anchor)
    }

    override fun getCount(): Int = slots.size

    override fun createView(container: ViewGroup, position: Int): View {
        return when (val slot = slots[position]) {
            is PagerSlot.Pages -> takeRenderWarmHolder(slot)
                ?: PagerPageHolder(readerThemedContext, viewer, slot)
            is PagerSlot.Transition -> PagerTransitionHolder(readerThemedContext, viewer, slot.transition)
        }
    }

    override fun getItemPosition(view: Any): Int {
        if (view is PagerPageHolder && view.renderedLayout != viewer.config.resolvedLayout) return POSITION_NONE
        if (view is PositionableView) {
            val position = when (val item = view.item) {
                is PagerSlot -> slots.indexOf(item)
                is ChapterTransition -> slots.indexOfFirst { it is PagerSlot.Transition && it.transition == item }
                else -> -1
            }
            if (position != -1) return position
            logcat { "Position for ${view.item} not found" }
        }
        return POSITION_NONE
    }

    fun currentSlot(): PagerSlot? = slots.getOrNull(viewer.pager.currentItem)

    fun positionOf(page: ReaderPage): Int = slots.indexOfFirst { slot ->
        slot is PagerSlot.Pages && slot.contains(page)
    }

    fun rebuildSlots(anchor: ReaderPage? = currentSlot()?.let(::anchorPage)) {
        clearRenderWarmups()
        slots = resolvedSlots().toMutableList()
        notifyDataSetChanged()

        anchor?.let(::positionOf)
            ?.takeIf { it >= 0 }
            ?.let { viewer.pager.setCurrentItem(it, false) }
    }

    fun onPagesClassified(
        classifications: Map<ReaderPage, ReaderPage.SpreadInfo>,
        anchor: ReaderPage,
    ): Boolean {
        var changed = false
        classifications.forEach { (page, info) ->
            if (page.spreadInfo != info) {
                page.spreadInfo = info
                changed = true
            }
        }
        if (!changed || resolvedSlots() == slots) return false
        viewer.requestSlotRebuild(anchor)
        return true
    }

    fun onPageSplit(currentPage: Any?, newPage: InsertPage) {
        if (currentPage !is ReaderPage || currentPage is InsertPage || newPage.parent !== currentPage) return
        val currentIndex = sourceItems.indexOfFirst { it is SourceItem.Page && it.page === currentPage }
        if (currentIndex < 0) return
        if (currentPage in splitPages) return
        splitPages[currentPage] = newPage
        sourceItems.add(currentIndex + 1, SourceItem.Page(newPage))
        // Decoding may finish for a prefetched page; keep the visible half-page selected.
        rebuildSlots()
    }

    fun removePageSplitItems() {
        sourceItems.removeAll { it is SourceItem.Page && it.page is InsertPage }
        splitPages.clear()
    }

    fun refresh() {
        clearRenderWarmups()
        readerThemedContext = viewer.activity.createReaderThemeContext()
    }

    fun updateRenderWarmupWindow(currentPosition: Int) {
        val current = slots.getOrNull(currentPosition) as? PagerSlot.Pages ?: run {
            clearRenderWarmups()
            return
        }
        val mode = KohariaEnhancementPreferences.normalizedRenderWarmupMode(
            enhancementPreferences.renderWarmupMode.get(),
        )
        val chapter = current.first.chapter
        val forwardStep = if (viewer is R2LPagerViewer) -1 else 1

        fun collect(step: Int, count: Int): List<PagerSlot.Pages> {
            if (count <= 0) return emptyList()
            val result = mutableListOf<PagerSlot.Pages>()
            var position = currentPosition + step
            while (position in slots.indices && result.size < count) {
                val candidate = slots[position] as? PagerSlot.Pages ?: break
                if (candidate.pages.any { it.chapter !== chapter }) break
                if (
                    viewer.config.doublePages &&
                    candidate.pages.any { it.spreadKind == ReaderPage.SpreadKind.UNKNOWN }
                ) {
                    break
                }
                result += candidate
                position += step
            }
            return result
        }

        when (mode) {
            KohariaEnhancementPreferences.RENDER_WARMUP_MODE_FIXED -> {
                val forwardCount = KohariaEnhancementPreferences.normalizedRenderWarmupForward(
                    enhancementPreferences.renderWarmupForwardPages.get(),
                )
                val backwardCount = KohariaEnhancementPreferences.normalizedRenderWarmupBackward(
                    enhancementPreferences.renderWarmupBackwardPages.get(),
                )
                desiredForwardWarmSlots = collect(forwardStep, forwardCount)
                desiredBackwardWarmSlots = collect(-forwardStep, backwardCount)
                renderWarmupConcurrency = KohariaEnhancementPreferences.normalizedRenderWarmupConcurrency(
                    enhancementPreferences.renderWarmupConcurrency.get(),
                )
            }
            else -> {
                val forwardStrength = KohariaEnhancementPreferences.normalizedAdaptiveRenderStrengthPercent(
                    enhancementPreferences.adaptiveRenderForwardStrengthPercent.get(),
                )
                val backwardStrength = KohariaEnhancementPreferences.normalizedAdaptiveRenderStrengthPercent(
                    enhancementPreferences.adaptiveRenderBackwardStrengthPercent.get(),
                )
                val concurrencyStrength = KohariaEnhancementPreferences.normalizedAdaptiveRenderStrengthPercent(
                    enhancementPreferences.adaptiveRenderConcurrencyStrengthPercent.get(),
                )
                val baselineForward = KohariaEnhancementPreferences.normalizedRenderWarmupForward(
                    enhancementPreferences.renderWarmupForwardPages.get(),
                )
                val baselineBackward = KohariaEnhancementPreferences.normalizedRenderWarmupBackward(
                    enhancementPreferences.renderWarmupBackwardPages.get(),
                )
                val baselineConcurrency = KohariaEnhancementPreferences.normalizedRenderWarmupConcurrency(
                    enhancementPreferences.renderWarmupConcurrency.get(),
                )
                val configuredMaxForward = KohariaEnhancementPreferences.normalizedAdaptiveRenderMaxForwardPages(
                    enhancementPreferences.adaptiveRenderMaxForwardPages.get(),
                )
                val maxForward = maxOf(configuredMaxForward, baselineForward)

                val forwardCandidates = collect(forwardStep, maxForward)
                val backwardCandidates = collect(
                    -forwardStep,
                    RenderWarmupPolicy.ADAPTIVE_BACKWARD_HARD_CAP,
                )
                val forwardCount = RenderWarmupPolicy.forwardSlotCount(
                    slotCosts = forwardCandidates.map(::renderWarmupCost),
                    strengthPercent = forwardStrength,
                    baselineSlots = baselineForward,
                    maxSlots = maxForward,
                )
                val backwardCount = RenderWarmupPolicy.backwardSlotCount(
                    slotCosts = backwardCandidates.map(::renderWarmupCost),
                    strengthPercent = backwardStrength,
                    baselineSlots = baselineBackward,
                )
                desiredForwardWarmSlots = forwardCandidates.take(forwardCount)
                desiredBackwardWarmSlots = backwardCandidates.take(backwardCount)
                renderWarmupConcurrency = RenderWarmupPolicy.adaptiveConcurrency(
                    metrics = forwardCandidates
                        .asSequence()
                        .flatMap { it.pages.asSequence() }
                        .map(::renderWarmupMetrics)
                        .take(RenderWarmupPolicy.CONCURRENCY_SAMPLE_PAGES)
                        .toList(),
                    strengthPercent = concurrencyStrength,
                    baselineConcurrency = baselineConcurrency,
                )
            }
        }

        desiredRenderWarmSlots = desiredForwardWarmSlots + desiredBackwardWarmSlots
        val desired = desiredRenderWarmSlots.toSet()
        renderWarmHolders.keys
            .filterNot(desired::contains)
            .toList()
            .forEach(::releaseRenderWarmHolder)

        pumpRenderWarmups()
        viewer.pager.post(::pumpRenderWarmups)
    }

    fun isWarmSlotReady(slot: PagerSlot.Pages): Boolean =
        renderWarmHolders[slot]?.isTransitionTargetReady() == true

    fun onAttachedRenderStateChanged() {
        viewer.pager.post(::pumpRenderWarmups)
    }

    fun clearRenderWarmups() {
        desiredForwardWarmSlots = emptyList()
        desiredBackwardWarmSlots = emptyList()
        desiredRenderWarmSlots = emptyList()
        activeRenderWarmups.clear()
        renderWarmHolders.values.toList().forEach(viewer::releaseRenderWarmupHolder)
        renderWarmHolders.clear()
    }

    private fun takeRenderWarmHolder(slot: PagerSlot.Pages): PagerPageHolder? {
        val holder = renderWarmHolders.remove(slot) ?: return null
        activeRenderWarmups.remove(slot)
        viewer.detachRenderWarmupHolderForPromotion(holder)
        holder.promoteFromWarmup()
        viewer.pager.post(::pumpRenderWarmups)
        return holder
    }

    private fun releaseRenderWarmHolder(slot: PagerSlot.Pages) {
        activeRenderWarmups.remove(slot)
        renderWarmHolders.remove(slot)?.let(viewer::releaseRenderWarmupHolder)
    }

    private fun pumpRenderWarmups() {
        if (desiredRenderWarmSlots.isEmpty()) return

        val attached = viewer.attachedPageSlots()
        val attachedPreparing = viewer.attachedPreparingRenderCount()
        val hiddenCapacity = (renderWarmupConcurrency - attachedPreparing).coerceAtLeast(0)

        val immediateForward = desiredForwardWarmSlots.firstOrNull()
        val immediateForwardReady = immediateForward == null ||
            viewer.isAttachedSlotReady(immediateForward) ||
            isWarmSlotReady(immediateForward)
        val eligible = if (immediateForwardReady) {
            desiredRenderWarmSlots
        } else {
            desiredForwardWarmSlots.take(NEAR_PAGE_WINDOW_WHILE_NEXT_PENDING)
        }
        val eligibleSet = eligible.toSet()

        activeRenderWarmups
            .filterNot(eligibleSet::contains)
            .toList()
            .forEach(::releaseRenderWarmHolder)

        while (activeRenderWarmups.size > hiddenCapacity) {
            val farthest = activeRenderWarmups.maxByOrNull { slot ->
                desiredRenderWarmSlots.indexOf(slot).takeIf { it >= 0 } ?: Int.MAX_VALUE
            } ?: break
            releaseRenderWarmHolder(farthest)
        }

        if (hiddenCapacity <= activeRenderWarmups.size) return

        val availableSlots = eligible.asSequence()
            .filterNot(attached::contains)
            .filterNot(renderWarmHolders::containsKey)
            .iterator()

        while (activeRenderWarmups.size < hiddenCapacity && availableSlots.hasNext()) {
            val slot = availableSlots.next()
            val holder = PagerPageHolder(
                readerThemedContext = readerThemedContext,
                viewer = viewer,
                slot = slot,
                warmupOnly = true,
                onWarmupFinished = ::onRenderWarmupFinished,
            )
            renderWarmHolders[slot] = holder
            activeRenderWarmups += slot
            viewer.attachRenderWarmupHolder(holder)
        }
    }

    private fun onRenderWarmupFinished(holder: PagerPageHolder) {
        val slot = holder.slot
        if (renderWarmHolders[slot] !== holder) return
        activeRenderWarmups.remove(slot)
        pumpRenderWarmups()
    }

    private fun renderWarmupCost(slot: PagerSlot.Pages): Double =
        slot.pages.sumOf { page -> RenderWarmupPolicy.pageCost(renderWarmupMetrics(page)) }

    private fun renderWarmupMetrics(page: ReaderPage): RenderWarmupPolicy.ImageMetrics {
        val sourcePage = (page as? InsertPage)?.parent ?: page
        return RenderWarmupPolicy.ImageMetrics(
            compressedSizeBytes = sourcePage.compressedSizeBytes,
            width = sourcePage.spreadInfo.width,
            height = sourcePage.spreadInfo.height,
        )
    }

    private fun buildSinglePageSlots(): List<PagerSlot> = sourceItems.map { item ->
        when (item) {
            is SourceItem.Page -> PagerSlot.Pages(item.page)
            is SourceItem.Transition -> PagerSlot.Transition(item.transition)
        }
    }

    private fun buildDoublePageSlots(): List<PagerSlot> {
        val result = mutableListOf<PagerSlot>()
        val segment = mutableListOf<ReaderPage>()

        fun flushSegment() {
            if (segment.isEmpty()) return
            val layout = DoublePagePairer.pair(
                soloPages = segment.map { it.spreadKind.occupiesFullSlot },
                shift = viewer.config.shiftDoublePages,
                preservePairBoundaries = viewer.config.usesContentAwarePairing,
                canPair = { first, second ->
                    !viewer.config.usesContentAwarePairing ||
                        DoublePageCompatibilityPolicy.canPair(
                            segment[first].spreadInfo,
                            segment[second].spreadInfo,
                        )
                },
            )
            layout.forEach { slot ->
                result += PagerSlot.Pages(segment[slot.first], slot.second?.let(segment::get))
            }
            segment.clear()
        }

        sourceItems.forEach { item ->
            when (item) {
                is SourceItem.Page -> {
                    val page = item.page
                    if (segment.lastOrNull()?.chapter?.chapter?.id != null &&
                        segment.last().chapter.chapter.id != page.chapter.chapter.id
                    ) {
                        flushSegment()
                    }
                    segment += page
                }
                is SourceItem.Transition -> {
                    flushSegment()
                    result += PagerSlot.Transition(item.transition)
                }
            }
        }
        flushSegment()
        return result
    }

    fun isSlotPlanned(slot: PagerSlot.Pages): Boolean = slot in resolvedSlots()

    private fun resolvedSlots(): List<PagerSlot> {
        val forwardSlots = if (viewer.config.doublePages) buildDoublePageSlots() else buildSinglePageSlots()
        return if (viewer is R2LPagerViewer) forwardSlots.asReversed() else forwardSlots
    }

    private fun anchorPage(slot: PagerSlot): ReaderPage? = (slot as? PagerSlot.Pages)?.progressPage

    private companion object {
        const val NEAR_PAGE_WINDOW_WHILE_NEXT_PENDING = 3
    }

    private sealed interface SourceItem {
        data class Page(val page: ReaderPage) : SourceItem
        data class Transition(val transition: ChapterTransition) : SourceItem
    }
}
