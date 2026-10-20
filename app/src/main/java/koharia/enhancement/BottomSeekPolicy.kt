package koharia.enhancement

import kotlin.math.max
import kotlin.math.roundToInt

internal object BottomSeekPolicy {

    fun pagesPerScreen(
        totalPages: Int,
        sensitivityPercent: Int,
        minimumPagesPerScreen: Int,
    ): Float {
        val normalizedTotal = totalPages.coerceAtLeast(1)
        val normalizedSensitivity =
            KohariaEnhancementPreferences.normalizedBottomSeekSensitivityPercent(sensitivityPercent)
        val normalizedMinimum =
            KohariaEnhancementPreferences.normalizedBottomSeekMinimumPagesPerScreen(minimumPagesPerScreen)
        return max(
            normalizedTotal * normalizedSensitivity / 100f,
            normalizedMinimum.toFloat(),
        )
    }

    fun targetIndex(
        anchorIndex: Int,
        dragFraction: Float,
        totalPages: Int,
        sensitivityPercent: Int,
        minimumPagesPerScreen: Int,
        rightToLeft: Boolean,
    ): Int {
        if (totalPages <= 1) return 0
        val direction = if (rightToLeft) -1f else 1f
        val pageDelta = dragFraction *
            pagesPerScreen(totalPages, sensitivityPercent, minimumPagesPerScreen) *
            direction
        return (anchorIndex + pageDelta).roundToInt().coerceIn(0, totalPages - 1)
    }
}
