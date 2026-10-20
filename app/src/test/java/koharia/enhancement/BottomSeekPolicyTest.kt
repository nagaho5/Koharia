package koharia.enhancement

import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BottomSeekPolicyTest {

    @Test
    fun `percentage sensitivity scales with book length`() {
        BottomSeekPolicy.pagesPerScreen(
            totalPages = 25,
            sensitivityPercent = 300,
            minimumPagesPerScreen = 50,
        ).shouldBeExactly(75f)

        BottomSeekPolicy.pagesPerScreen(
            totalPages = 2_000,
            sensitivityPercent = 300,
            minimumPagesPerScreen = 50,
        ).shouldBeExactly(6_000f)
    }

    @Test
    fun `minimum span keeps short books responsive`() {
        BottomSeekPolicy.pagesPerScreen(
            totalPages = 4,
            sensitivityPercent = 300,
            minimumPagesPerScreen = 50,
        ).shouldBeExactly(50f)
    }

    @Test
    fun `relative seek anchors at the current page and respects direction`() {
        BottomSeekPolicy.targetIndex(
            anchorIndex = 10,
            dragFraction = 0f,
            totalPages = 25,
            sensitivityPercent = 300,
            minimumPagesPerScreen = 50,
            rightToLeft = false,
        ) shouldBe 10

        BottomSeekPolicy.targetIndex(
            anchorIndex = 10,
            dragFraction = 0.04f,
            totalPages = 25,
            sensitivityPercent = 300,
            minimumPagesPerScreen = 50,
            rightToLeft = false,
        ) shouldBe 13

        BottomSeekPolicy.targetIndex(
            anchorIndex = 10,
            dragFraction = 0.04f,
            totalPages = 25,
            sensitivityPercent = 300,
            minimumPagesPerScreen = 50,
            rightToLeft = true,
        ) shouldBe 7
    }

    @Test
    fun `relative seek clamps to book boundaries`() {
        BottomSeekPolicy.targetIndex(
            anchorIndex = 0,
            dragFraction = -1f,
            totalPages = 25,
            sensitivityPercent = 1_000,
            minimumPagesPerScreen = 10_000,
            rightToLeft = false,
        ) shouldBe 0

        BottomSeekPolicy.targetIndex(
            anchorIndex = 24,
            dragFraction = 1f,
            totalPages = 25,
            sensitivityPercent = 1_000,
            minimumPagesPerScreen = 10_000,
            rightToLeft = false,
        ) shouldBe 24
    }
}
