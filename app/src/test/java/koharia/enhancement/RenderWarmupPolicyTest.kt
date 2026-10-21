package koharia.enhancement

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RenderWarmupPolicyTest {

    private fun image(sizeMb: Double, width: Int = 2000, height: Int = 3000) =
        RenderWarmupPolicy.ImageMetrics(
            compressedSizeBytes = (sizeMb * 1024 * 1024).toLong(),
            width = width,
            height = height,
        )

    private fun costs(sizeMb: Double, count: Int = 30) =
        List(count) { RenderWarmupPolicy.pageCost(image(sizeMb)) }

    @Test
    fun largePagesAlwaysKeepManualForwardBaseline() {
        listOf(10, 50, 100, 150, 200).forEach { strength ->
            RenderWarmupPolicy.forwardSlotCount(
                slotCosts = costs(10.0),
                strengthPercent = strength,
                baselineSlots = 5,
                maxSlots = 20,
            ) shouldBe 5
        }

        RenderWarmupPolicy.forwardSlotCount(
            slotCosts = costs(15.0),
            strengthPercent = 200,
            baselineSlots = 5,
            maxSlots = 20,
        ) shouldBe 5
    }

    @Test
    fun smallPagesAddForwardSlotsAboveTheBaseline() {
        RenderWarmupPolicy.forwardSlotCount(costs(5.0), 100, 5, 20) shouldBe 7
        RenderWarmupPolicy.forwardSlotCount(costs(2.5), 100, 5, 20) shouldBe 10
        RenderWarmupPolicy.forwardSlotCount(costs(1.0), 100, 5, 20) shouldBe 15
    }

    @Test
    fun forwardStrengthScalesOnlyTheAdaptiveBonus() {
        RenderWarmupPolicy.forwardSlotCount(costs(5.0), 10, 5, 20) shouldBe 5
        RenderWarmupPolicy.forwardSlotCount(costs(5.0), 50, 5, 20) shouldBe 6
        RenderWarmupPolicy.forwardSlotCount(costs(5.0), 100, 5, 20) shouldBe 7
        RenderWarmupPolicy.forwardSlotCount(costs(5.0), 150, 5, 20) shouldBe 8
        RenderWarmupPolicy.forwardSlotCount(costs(5.0), 200, 5, 20) shouldBe 9
    }

    @Test
    fun backwardAdaptationKeepsLargePagesAtBaselineAndExpandsSmallPages() {
        RenderWarmupPolicy.backwardSlotCount(costs(10.0), 200, 1) shouldBe 1
        RenderWarmupPolicy.backwardSlotCount(costs(5.0), 100, 1) shouldBe 3
        RenderWarmupPolicy.backwardSlotCount(costs(2.0), 100, 1) shouldBe 5
        RenderWarmupPolicy.backwardSlotCount(costs(1.0), 100, 1) shouldBe 5
    }

    @Test
    fun backwardStrengthScalesOnlyExtraRetention() {
        RenderWarmupPolicy.backwardSlotCount(costs(5.0), 50, 1) shouldBe 2
        RenderWarmupPolicy.backwardSlotCount(costs(5.0), 100, 1) shouldBe 3
        RenderWarmupPolicy.backwardSlotCount(costs(5.0), 200, 1) shouldBe 5
        RenderWarmupPolicy.backwardSlotCount(costs(5.0), 200, 0) shouldBe 0
    }

    @Test
    fun adaptiveConcurrencyTreatsManualValueAsFloor() {
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(10.0) }, 200, 5) shouldBe 5
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(15.0) }, 200, 5) shouldBe 5
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(5.0) }, 100, 5) shouldBe 6
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(2.5) }, 100, 5) shouldBe 7
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(1.0) }, 100, 5) shouldBe 9
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(5.0) }, 200, 5) shouldBe 7
    }

    @Test
    fun unknownSizesStayAtManualBaseline() {
        val unknown = RenderWarmupPolicy.ImageMetrics(null, null, null)

        RenderWarmupPolicy.pageCost(unknown) shouldBe 1.0
        RenderWarmupPolicy.forwardSlotCount(
            slotCosts = List(20) { RenderWarmupPolicy.pageCost(unknown) },
            strengthPercent = 200,
            baselineSlots = 5,
            maxSlots = 20,
        ) shouldBe 5
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { unknown }, 200, 5) shouldBe 5
    }

    @Test
    fun extremePixelDimensionsCanBlockBonusButNeverReduceBaseline() {
        val huge = image(2.0, width = 8000, height = 8000)
        val hugeCosts = List(20) { RenderWarmupPolicy.pageCost(huge) }

        RenderWarmupPolicy.forwardSlotCount(hugeCosts, 200, 5, 20) shouldBe 5
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { huge }, 200, 5) shouldBe 5
    }
}
