package mir.oslav.adrevenue.ui.stats

import java.util.Calendar
import java.util.TimeZone
import mir.oslav.adrevenue.core.AdMobApiConstants
import mir.oslav.adrevenue.data.cache.CachedRevenueTarget
import mir.oslav.adrevenue.data.model.AdMobReportRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsPeriodTest {
    @Test
    fun reportDimensions_keepHomeTotalsSeparateFromStatisticsBreakdowns() {
        assertEquals(listOf("DATE"), AdMobApiConstants.NetworkReportDimensions)
        assertEquals(
            listOf("DATE", "APP", "FORMAT"),
            AdMobApiConstants.StatisticsReportDimensions,
        )
    }

    @Test
    fun currentWeek_startsOnMondayAndEndsToday() {
        val period = calculateStatisticsPeriod(
            timeRange = StatisticsTimeRange.Week,
            periodOffset = 0,
            anchorEpochMillis = reportDate(year = 2026, month = Calendar.AUGUST, day = 27),
        )

        assertEquals(
            reportDate(year = 2026, month = Calendar.AUGUST, day = 24),
            period.startDateEpochMillis,
        )
        assertEquals(
            reportDate(year = 2026, month = Calendar.AUGUST, day = 27),
            period.endDateEpochMillis,
        )
    }

    @Test
    fun previousQuarter_returnsCompleteCalendarQuarter() {
        val period = calculateStatisticsPeriod(
            timeRange = StatisticsTimeRange.Quarter,
            periodOffset = -1,
            anchorEpochMillis = reportDate(year = 2026, month = Calendar.AUGUST, day = 27),
        )

        assertEquals(
            reportDate(year = 2026, month = Calendar.APRIL, day = 1),
            period.startDateEpochMillis,
        )
        assertEquals(
            reportDate(year = 2026, month = Calendar.JUNE, day = 30),
            period.endDateEpochMillis,
        )
    }

    @Test
    fun yearRange_isCappedAtTodayForCurrentYear() {
        val period = calculateStatisticsPeriod(
            timeRange = StatisticsTimeRange.Year,
            periodOffset = 0,
            anchorEpochMillis = reportDate(year = 2026, month = Calendar.AUGUST, day = 27),
        )

        assertEquals(
            reportDate(year = 2026, month = Calendar.JANUARY, day = 1),
            period.startDateEpochMillis,
        )
        assertEquals(
            reportDate(year = 2026, month = Calendar.AUGUST, day = 27),
            period.endDateEpochMillis,
        )
    }

    @Test
    fun currentRanges_fillBothChartsWithEmptyBucketsThroughTheSelectedPeriod() {
        data class ExpectedChartRange(
            val timeRange: StatisticsTimeRange,
            val expectedBarCount: Int,
            val expectedEndEpochMillis: Long,
        )

        val anchorEpochMillis = reportDate(2026, Calendar.AUGUST, 25)
        val expectedRanges = listOf(
            ExpectedChartRange(
                timeRange = StatisticsTimeRange.Week,
                expectedBarCount = 7,
                expectedEndEpochMillis = reportDate(2026, Calendar.AUGUST, 30),
            ),
            ExpectedChartRange(
                timeRange = StatisticsTimeRange.Month,
                expectedBarCount = 5,
                expectedEndEpochMillis = reportDate(2026, Calendar.AUGUST, 31),
            ),
            ExpectedChartRange(
                timeRange = StatisticsTimeRange.Quarter,
                expectedBarCount = 14,
                expectedEndEpochMillis = reportDate(2026, Calendar.SEPTEMBER, 30),
            ),
            ExpectedChartRange(
                timeRange = StatisticsTimeRange.Year,
                expectedBarCount = 12,
                expectedEndEpochMillis = reportDate(2026, Calendar.DECEMBER, 31),
            ),
        )
        val rows = listOf(
            reportRow(
                day = 25,
                revenueMicros = 2_500_000L,
                appName = "Current app",
                appId = "app-current",
            ),
        )

        expectedRanges.forEach { expected ->
            val reportPeriod = calculateStatisticsPeriod(
                timeRange = expected.timeRange,
                periodOffset = 0,
                anchorEpochMillis = anchorEpochMillis,
            )
            val chartPeriod = expandStatisticsPeriodForChart(
                timeRange = expected.timeRange,
                period = reportPeriod,
            )
            val bars = buildStatisticsChartBars(
                rows = rows,
                timeRange = expected.timeRange,
                period = chartPeriod,
            )
            val breakdown = buildStatisticsChartBreakdown(
                rows = rows,
                timeRange = expected.timeRange,
                period = chartPeriod,
                dimension = StatisticsBreakdownDimension.App,
            )

            assertEquals(
                expected.timeRange.name,
                expected.expectedEndEpochMillis,
                chartPeriod.endDateEpochMillis,
            )
            assertEquals(expected.timeRange.name, expected.expectedBarCount, bars.size)
            assertEquals(expected.timeRange.name, expected.expectedBarCount, breakdown.bars.size)
            assertEquals(
                expected.timeRange.name,
                expected.expectedEndEpochMillis,
                bars.last().endDateEpochMillis,
            )
            assertEquals(expected.timeRange.name, 0L, bars.last().estimatedEarningsMicros)
            assertTrue(
                expected.timeRange.name,
                breakdown.bars.last().earningsMicrosBySeries.isEmpty(),
            )
            assertEquals(
                expected.timeRange.name,
                rows.single().estimatedEarningsMicros,
                bars.sumOf { bar -> bar.estimatedEarningsMicros },
            )
        }
    }

    @Test
    fun trendAverage_excludesFutureEmptyBuckets() {
        val reportPeriod = calculateStatisticsPeriod(
            timeRange = StatisticsTimeRange.Week,
            periodOffset = 0,
            anchorEpochMillis = reportDate(2026, Calendar.AUGUST, 25),
        )
        val chartPeriod = expandStatisticsPeriodForChart(
            timeRange = StatisticsTimeRange.Week,
            period = reportPeriod,
        )
        val bars = buildStatisticsChartBars(
            rows = listOf(
                reportRow(day = 24, revenueMicros = 1_000_000L),
                reportRow(day = 25, revenueMicros = 3_000_000L),
            ),
            timeRange = StatisticsTimeRange.Week,
            period = chartPeriod,
        )

        assertEquals(7, bars.size)
        assertEquals(
            2.0,
            calculateStatisticsTrendAmount(
                bars = bars,
                dataEndDateEpochMillis = reportPeriod.endDateEpochMillis,
            ),
            0.0,
        )
    }

    @Test
    fun weekAggregation_fillsMissingDaysAndSumsRevenue() {
        val period = StatisticsPeriod(
            startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 24),
            endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 30),
        )
        val rows = listOf(
            reportRow(day = 24, revenueMicros = 1_000_000),
            reportRow(day = 26, revenueMicros = 2_500_000),
        )

        val bars = buildStatisticsChartBars(
            rows = rows,
            timeRange = StatisticsTimeRange.Week,
            period = period,
        )

        assertEquals(7, bars.size)
        assertEquals(1_000_000L, bars[0].estimatedEarningsMicros)
        assertEquals(0L, bars[1].estimatedEarningsMicros)
        assertEquals(2_500_000L, bars[2].estimatedEarningsMicros)
    }

    @Test
    fun monthAggregation_usesWeeklyBuckets() {
        val period = StatisticsPeriod(
            startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 1),
            endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 31),
        )
        val rows = listOf(
            reportRow(day = 1, revenueMicros = 1_000_000),
            reportRow(day = 7, revenueMicros = 2_000_000),
            reportRow(day = 8, revenueMicros = 4_000_000),
        )

        val bars = buildStatisticsChartBars(
            rows = rows,
            timeRange = StatisticsTimeRange.Month,
            period = period,
        )

        assertEquals(5, bars.size)
        assertEquals(3_000_000L, bars[0].estimatedEarningsMicros)
        assertEquals(4_000_000L, bars[1].estimatedEarningsMicros)
    }

    @Test
    fun appBreakdown_preservesBucketTotalsAndOrdersSeriesByRevenue() {
        val period = StatisticsPeriod(
            startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 24),
            endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 25),
        )
        val rows = listOf(
            reportRow(
                day = 24,
                revenueMicros = 1_000_000L,
                appName = "First app",
                appId = "app-1",
            ),
            reportRow(
                day = 24,
                revenueMicros = 2_000_000L,
                appName = "Second app",
                appId = "app-2",
            ),
            reportRow(
                day = 25,
                revenueMicros = 3_000_000L,
                appName = "First app",
                appId = "app-1",
            ),
        )

        val breakdown = buildStatisticsChartBreakdown(
            rows = rows,
            timeRange = StatisticsTimeRange.Week,
            period = period,
            dimension = StatisticsBreakdownDimension.App,
        )

        assertEquals(listOf("First app", "Second app"), breakdown.series.map { it.label })
        assertEquals(3_000_000L, breakdown.bars[0].earningsMicrosBySeries.values.sum())
        assertEquals(3_000_000L, breakdown.bars[1].earningsMicrosBySeries.values.sum())
    }

    @Test
    fun adFormatBreakdown_groupsApiValuesAndFormatsTheirLabels() {
        val period = StatisticsPeriod(
            startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 24),
            endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 24),
        )
        val rows = listOf(
            reportRow(
                day = 24,
                revenueMicros = 2_500_000L,
                adFormat = "APP_OPEN",
            ),
            reportRow(
                day = 24,
                revenueMicros = 1_500_000L,
                adFormat = "BANNER",
            ),
        )

        val breakdown = buildStatisticsChartBreakdown(
            rows = rows,
            timeRange = StatisticsTimeRange.Week,
            period = period,
            dimension = StatisticsBreakdownDimension.AdFormat,
        )

        assertEquals(listOf("App open", "Banner"), breakdown.series.map { it.label })
        assertEquals(4_000_000L, breakdown.bars.single().earningsMicrosBySeries.values.sum())
    }

    @Test
    fun revenueTarget_recalculatesForWeekAndDailyBars() {
        val projection = projectRevenueTarget(
            target = CachedRevenueTarget(
                accountName = "accounts/pub-1",
                amountMicros = 30_000_000L,
                rangeDays = 30,
            ),
            timeRange = StatisticsTimeRange.Week,
            period = StatisticsPeriod(
                startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 24),
                endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 27),
            ),
            barCount = 4,
        )

        requireNotNull(projection)
        assertEquals(7_000_000.0, projection.selectedRangeAmountMicros, 0.0)
        assertEquals(1_000_000.0, projection.chartLineAmountMicros, 0.0)
    }

    @Test
    fun revenueTarget_usesCalendarMonthLength() {
        val projection = projectRevenueTarget(
            target = CachedRevenueTarget(
                accountName = "accounts/pub-1",
                amountMicros = 30_000_000L,
                rangeDays = 30,
            ),
            timeRange = StatisticsTimeRange.Month,
            period = StatisticsPeriod(
                startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 1),
                endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 31),
            ),
            barCount = 5,
        )

        requireNotNull(projection)
        assertEquals(31_000_000.0, projection.selectedRangeAmountMicros, 0.0)
        assertEquals(6_200_000.0, projection.chartLineAmountMicros, 0.0)
    }

    @Test
    fun loadingNewPeriod_retainsPreviousChartForTheSameAccount() {
        val previousPeriod = StatisticsPeriod(
            startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 24),
            endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 30),
        )
        val previous = StatsUiState(
            period = previousPeriod,
            accountName = "accounts/pub-1",
            chartBars = listOf(
                StatisticsChartBar(
                    startDateEpochMillis = previousPeriod.startDateEpochMillis,
                    endDateEpochMillis = previousPeriod.startDateEpochMillis,
                    estimatedEarningsMicros = 1_000_000L,
                ),
            ),
            chartTimeRange = StatisticsTimeRange.Week,
            chartPeriod = previousPeriod,
            chartDataEndDateEpochMillis = previousPeriod.endDateEpochMillis,
            chartCurrencyCode = "USD",
        )
        val current = StatsUiState(
            period = StatisticsPeriod(
                startDateEpochMillis = reportDate(2026, Calendar.JULY, 1),
                endDateEpochMillis = reportDate(2026, Calendar.JULY, 31),
            ),
            timeRange = StatisticsTimeRange.Month,
            accountName = "accounts/pub-1",
            isLoading = true,
        )

        val retained = current.retainChartWhileLoading(previous = previous)

        assertTrue(retained.hasVisibleChart)
        assertTrue(retained.isShowingPreviousChart)
        assertEquals(previous.chartBars, retained.chartBars)
        assertEquals(StatisticsTimeRange.Week, retained.chartTimeRange)
        assertEquals(
            previous.chartDataEndDateEpochMillis,
            retained.chartDataEndDateEpochMillis,
        )
    }

    @Test
    fun finishedRefresh_doesNotRetainPreviousChart() {
        val period = StatisticsPeriod(
            startDateEpochMillis = reportDate(2026, Calendar.AUGUST, 24),
            endDateEpochMillis = reportDate(2026, Calendar.AUGUST, 30),
        )
        val previous = StatsUiState(
            period = period,
            accountName = "accounts/pub-1",
            chartBars = listOf(
                StatisticsChartBar(
                    startDateEpochMillis = period.startDateEpochMillis,
                    endDateEpochMillis = period.startDateEpochMillis,
                    estimatedEarningsMicros = 1_000_000L,
                ),
            ),
            chartTimeRange = StatisticsTimeRange.Week,
            chartPeriod = period,
            chartDataEndDateEpochMillis = period.endDateEpochMillis,
        )
        val current = StatsUiState(
            period = period,
            accountName = "accounts/pub-1",
            isLoading = false,
        )

        val retained = current.retainChartWhileLoading(previous = previous)

        assertFalse(retained.hasVisibleChart)
        assertFalse(retained.isShowingPreviousChart)
    }

    private fun reportRow(
        day: Int,
        revenueMicros: Long,
        appName: String? = null,
        appId: String? = null,
        adFormat: String? = null,
    ): AdMobReportRow = AdMobReportRow(
        dateLabel = "Aug $day",
        appName = appName,
        appId = appId,
        estimatedEarningsMicros = revenueMicros,
        impressions = null,
        clicks = null,
        matchRate = null,
        impressionRpmMicros = null,
        dateEpochMillis = reportDate(2026, Calendar.AUGUST, day),
        adFormat = adFormat,
    )

    private fun reportDate(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis
}
