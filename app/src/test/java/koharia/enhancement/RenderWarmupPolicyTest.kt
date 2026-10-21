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

    @Test
    fun `100 percent forward budget matches the device calibration points`() {
        RenderWarmupPolicy.forwardSlotCount(List(20) { RenderWarmupPolicy.pageCost(image(10.0)) }, 100, 20) shouldBe 5
        RenderWarmupPolicy.forwardSlotCount(List(20) { RenderWarmupPolicy.pageCost(image(5.0)) }, 100, 20) shouldBe 7
        RenderWarmupPolicy.forwardSlotCount(List(20) { RenderWarmupPolicy.pageCost(image(2.5)) }, 100, 20) shouldBe 10
    }

    @Test
    fun `forward strength scales the cost budget rather than the final page count`() {
        val costs = List(30) { RenderWarmupPolicy.pageCost(image(10.0)) }

        RenderWarmupPolicy.forwardSlotCount(costs, 80, 30) shouldBe 4
        RenderWarmupPolicy.forwardSlotCount(costs, 150, 30) shouldBe 7
        RenderWarmupPolicy.forwardSlotCount(costs, 200, 30) shouldBe 10
    }

    @Test
    fun `100 percent backward budget matches requested retention curve`() {
        RenderWarmupPolicy.backwardSlotCount(List(10) { RenderWarmupPolicy.pageCost(image(10.0)) }, 100) shouldBe 2
        RenderWarmupPolicy.backwardSlotCount(List(10) { RenderWarmupPolicy.pageCost(image(5.0)) }, 100) shouldBe 3
        RenderWarmupPolicy.backwardSlotCount(List(10) { RenderWarmupPolicy.pageCost(image(2.0)) }, 100) shouldBe 5
        RenderWarmupPolicy.backwardSlotCount(List(10) { RenderWarmupPolicy.pageCost(image(1.0)) }, 100) shouldBe 5
    }

    @Test
    fun `backward cap grows with strength`() {
        RenderWarmupPolicy.adaptiveBackwardPageCap(100) shouldBe 5
        RenderWarmupPolicy.adaptiveBackwardPageCap(150) shouldBe 8
        RenderWarmupPolicy.adaptiveBackwardPageCap(200) shouldBe 10
    }

    @Test
    fun `adaptive concurrency follows the agreed gradual curve`() {
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(10.0) }, 100) shouldBe 5
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(5.0) }, 100) shouldBe 6
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(2.5) }, 100) shouldBe 7
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(1.0) }, 100) shouldBe 9
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { image(10.0) }, 200) shouldBe 10
    }

    @Test
    fun `unknown sizes stay conservative at the 10 MB reference`() {
        val unknown = RenderWarmupPolicy.ImageMetrics(null, null, null)

        RenderWarmupPolicy.pageCost(unknown) shouldBe 1.0
        RenderWarmupPolicy.adaptiveConcurrency(List(5) { unknown }, 100) shouldBe 5
    }

    @Test
    fun `extreme pixel dimensions trigger the safety penalty`() {
        val normal = RenderWarmupPolicy.pageCost(image(2.0, width = 2000, height = 3000))
        val huge = RenderWarmupPolicy.pageCost(image(2.0, width = 8000, height = 8000))

        (huge > normal) shouldBe true
    }
}
