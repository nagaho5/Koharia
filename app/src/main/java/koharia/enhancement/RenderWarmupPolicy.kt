package koharia.enhancement

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object RenderWarmupPolicy {

    data class ImageMetrics(
        val compressedSizeBytes: Long?,
        val width: Int?,
        val height: Int?,
    )

    const val CONCURRENCY_SAMPLE_PAGES = 5
    const val ADAPTIVE_CONCURRENCY_HARD_CAP = 20
    const val ADAPTIVE_BACKWARD_HARD_CAP = 10

    private const val REFERENCE_SIZE_BYTES = 10L * 1024L * 1024L
    private const val BACKWARD_BUDGET_AT_100_PER_BASELINE = 2.35
    private const val BACKWARD_BONUS_CAP_AT_100_PER_BASELINE = 4
    private const val PIXEL_SAFETY_THRESHOLD = 24_000_000L

    fun pageCost(metrics: ImageMetrics): Double =
        sqrt(effectiveSizeBytes(metrics).toDouble() / REFERENCE_SIZE_BYTES.toDouble())

    fun forwardSlotCount(
        slotCosts: List<Double>,
        strengthPercent: Int,
        baselineSlots: Int,
        maxSlots: Int,
    ): Int {
        val safeMax = maxSlots.coerceAtLeast(0)
        val baseline = baselineSlots.coerceIn(0, safeMax)
        if (baseline == 0 || baseline == safeMax) return baseline

        val targetAt100 = fitWithinBudget(
            costs = slotCosts,
            budget = baseline.toDouble(),
            maxCount = safeMax,
        ).coerceAtLeast(baseline)
        return scaleBonus(
            baseline = baseline,
            targetAt100 = targetAt100,
            strengthPercent = strengthPercent,
            hardCap = safeMax,
        )
    }

    fun backwardSlotCount(
        slotCosts: List<Double>,
        strengthPercent: Int,
        baselineSlots: Int,
        maxSlots: Int = ADAPTIVE_BACKWARD_HARD_CAP,
    ): Int {
        val safeMax = maxSlots.coerceIn(0, ADAPTIVE_BACKWARD_HARD_CAP)
        val baseline = baselineSlots.coerceIn(0, safeMax)
        if (baseline == 0 || baseline == safeMax) return baseline

        val nearestCost = slotCosts.firstOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        if (nearestCost >= 1.0) return baseline

        val targetCapAt100 = min(
            safeMax,
            baseline + BACKWARD_BONUS_CAP_AT_100_PER_BASELINE * baseline,
        )
        val targetAt100 = fitWithinBudget(
            costs = slotCosts,
            budget = BACKWARD_BUDGET_AT_100_PER_BASELINE * baseline,
            maxCount = targetCapAt100,
        ).coerceAtLeast(baseline)

        return scaleBonus(
            baseline = baseline,
            targetAt100 = targetAt100,
            strengthPercent = strengthPercent,
            hardCap = safeMax,
        )
    }

    fun adaptiveConcurrency(
        metrics: List<ImageMetrics>,
        strengthPercent: Int,
        baselineConcurrency: Int,
    ): Int {
        val baseline = baselineConcurrency.coerceIn(1, ADAPTIVE_CONCURRENCY_HARD_CAP)
        val sample = metrics.take(CONCURRENCY_SAMPLE_PAGES)
        val averageSize = if (sample.isEmpty()) {
            REFERENCE_SIZE_BYTES.toDouble()
        } else {
            sample.map(::effectiveSizeBytes).average()
        }
        val sizeFactor = (REFERENCE_SIZE_BYTES / averageSize).pow(0.25)
        val targetAt100 = (baseline * sizeFactor)
            .roundToInt()
            .coerceIn(baseline, ADAPTIVE_CONCURRENCY_HARD_CAP)

        return scaleBonus(
            baseline = baseline,
            targetAt100 = targetAt100,
            strengthPercent = strengthPercent,
            hardCap = ADAPTIVE_CONCURRENCY_HARD_CAP,
        )
    }

    private fun scaleBonus(
        baseline: Int,
        targetAt100: Int,
        strengthPercent: Int,
        hardCap: Int,
    ): Int {
        val bonusAt100 = (targetAt100 - baseline).coerceAtLeast(0)
        val scaledBonus = (bonusAt100 * normalizedStrength(strengthPercent) / 100.0).roundToInt()
        return (baseline + scaledBonus).coerceIn(baseline, hardCap)
    }

    private fun fitWithinBudget(
        costs: List<Double>,
        budget: Double,
        maxCount: Int,
    ): Int {
        var used = 0.0
        var count = 0
        for (cost in costs.take(maxCount)) {
            val safeCost = cost.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
            if (used + safeCost > budget + 1e-9) break
            used += safeCost
            count++
        }
        return count
    }

    private fun effectiveSizeBytes(metrics: ImageMetrics): Long {
        val compressed = metrics.compressedSizeBytes
            ?.takeIf { it > 0L }
            ?: REFERENCE_SIZE_BYTES
        val width = metrics.width?.takeIf { it > 0 }
        val height = metrics.height?.takeIf { it > 0 }
        if (width == null || height == null) return compressed

        val pixels = width.toLong() * height.toLong()
        if (pixels <= PIXEL_SAFETY_THRESHOLD) return compressed

        val pixelPenalty = REFERENCE_SIZE_BYTES *
            sqrt(pixels.toDouble() / PIXEL_SAFETY_THRESHOLD.toDouble())
        return maxOf(compressed, pixelPenalty.toLong())
    }

    private fun normalizedStrength(value: Int): Int = value.coerceIn(10, 200)
}
