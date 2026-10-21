package koharia.enhancement

import kotlin.math.ceil
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
    private const val REFERENCE_SIZE_BYTES = 10L * 1024L * 1024L
    private const val FORWARD_BUDGET_AT_100 = 5.0
    private const val BACKWARD_BUDGET_AT_100 = 2.35
    private const val BACKWARD_CAP_AT_100 = 5
    private const val ADAPTIVE_CONCURRENCY_AT_10_MB = 5.0
    private const val ADAPTIVE_CONCURRENCY_HARD_CAP = 20
    private const val PIXEL_SAFETY_THRESHOLD = 24_000_000L

    fun pageCost(metrics: ImageMetrics): Double =
        sqrt(effectiveSizeBytes(metrics).toDouble() / REFERENCE_SIZE_BYTES.toDouble())

    fun forwardSlotCount(
        slotCosts: List<Double>,
        strengthPercent: Int,
        maxSlots: Int,
    ): Int {
        val budget = FORWARD_BUDGET_AT_100 * normalizedStrength(strengthPercent) / 100.0
        return fitWithinBudget(slotCosts, budget, maxSlots.coerceAtLeast(0))
    }

    fun backwardSlotCount(
        slotCosts: List<Double>,
        strengthPercent: Int,
    ): Int {
        val strength = normalizedStrength(strengthPercent)
        val budget = BACKWARD_BUDGET_AT_100 * strength / 100.0
        return fitWithinBudget(slotCosts, budget, adaptiveBackwardPageCap(strength))
    }

    fun adaptiveBackwardPageCap(strengthPercent: Int): Int {
        val strength = normalizedStrength(strengthPercent)
        return ceil(BACKWARD_CAP_AT_100 * strength / 100.0).toInt().coerceAtLeast(1)
    }

    fun adaptiveConcurrency(
        metrics: List<ImageMetrics>,
        strengthPercent: Int,
    ): Int {
        val sample = metrics.take(CONCURRENCY_SAMPLE_PAGES)
        val averageSize = if (sample.isEmpty()) {
            REFERENCE_SIZE_BYTES.toDouble()
        } else {
            sample.map(::effectiveSizeBytes).average()
        }
        val sizeFactor = (REFERENCE_SIZE_BYTES / averageSize).pow(0.25)
        val scaled = ADAPTIVE_CONCURRENCY_AT_10_MB *
            sizeFactor *
            normalizedStrength(strengthPercent) /
            100.0
        return scaled.roundToInt().coerceIn(1, ADAPTIVE_CONCURRENCY_HARD_CAP)
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
