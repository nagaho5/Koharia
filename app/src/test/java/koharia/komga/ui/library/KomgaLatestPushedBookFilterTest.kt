package koharia.komga.ui.library

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import koharia.source.komga.InProgressFilter
import koharia.source.komga.OneshotFilter
import koharia.source.komga.RECENTLY_READ_SORT_INDEX
import koharia.source.komga.ReadFilter
import koharia.source.komga.ReadingStateGroup
import koharia.source.komga.SeriesSort
import koharia.source.komga.TYPE_BOOKS_INDEX
import koharia.source.komga.TypeSelect
import koharia.source.komga.UnreadFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KomgaLatestPushedBookFilterTest {

    @Test
    fun `latest pushed query targets recently read books without changing library selection`() {
        val filters = FilterList(
            TypeSelect(),
            SeriesSort(),
            ReadingStateGroup(),
        )

        filters.configureLatestPushedBookQuery()

        assertEquals(TYPE_BOOKS_INDEX, filters.filterIsInstance<TypeSelect>().single().state)
        assertEquals(
            Filter.Sort.Selection(RECENTLY_READ_SORT_INDEX, false),
            filters.filterIsInstance<SeriesSort>().single().state,
        )
        val reading = filters.filterIsInstance<ReadingStateGroup>().single().state
        assertFalse(reading.filterIsInstance<UnreadFilter>().single().state)
        assertTrue(reading.filterIsInstance<InProgressFilter>().single().state)
        assertTrue(reading.filterIsInstance<ReadFilter>().single().state)
        assertFalse(reading.filterIsInstance<OneshotFilter>().single().state)
    }
}
