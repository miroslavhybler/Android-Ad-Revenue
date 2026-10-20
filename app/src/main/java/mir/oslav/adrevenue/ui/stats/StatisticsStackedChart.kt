package mir.oslav.adrevenue.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/** Draws true stacked columns while retaining themed labels and touch tooltips. */
@Composable
internal fun StatisticsStackedColumnChart(
    breakdown: StatisticsChartBreakdown,
    timeRange: StatisticsTimeRange,
    currencyCode: String,
    minValue: Double,
    maxValue: Double,
    gridColor: Color,
    contentColor: Color,
    popupContainerColor: Color,
    popupContentColor: Color,
    labelTextStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val animationProgress = remember { Animatable(initialValue = 1f) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedSegment by remember { mutableStateOf<StackedSegmentTarget?>(null) }

    val fullSizeTargets = remember(breakdown, chartSize, minValue, maxValue, density) {
        buildStackedSegmentTargets(
            breakdown = breakdown,
            chartSize = chartSize,
            minValue = minValue,
            maxValue = maxValue,
            density = density.density,
            progress = 1f,
        )
    }

    LaunchedEffect(breakdown) {
        selectedSegment = null
        animationProgress.snapTo(targetValue = 0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = STACKED_CHART_ANIMATION_MILLIS),
        )
    }
    LaunchedEffect(selectedSegment) {
        if (selectedSegment != null) {
            delay(timeMillis = STACKED_CHART_POPUP_DURATION_MILLIS)
            selectedSegment = null
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { size -> chartSize = size }
            .pointerInput(fullSizeTargets) {
                detectTapGestures { position ->
                    selectedSegment = fullSizeTargets.lastOrNull { target ->
                        target.rect.contains(position)
                    }
                }
            },
    ) {
        val layout = stackedChartLayout(
            width = size.width,
            height = size.height,
            barCount = breakdown.bars.size,
            density = density.density,
        )
        val valueRange = (maxValue - minValue).takeUnless { range -> range == 0.0 } ?: 1.0
        val valueToY: (Double) -> Float = { value ->
            layout.plotBottom -
                    (((value - minValue) / valueRange) * layout.plotHeight).toFloat()
        }
        val indicatorTextStyle = labelTextStyle.copy(color = contentColor)

        repeat(STACKED_CHART_INDICATOR_COUNT) { index ->
            val fraction = index.toDouble() / (STACKED_CHART_INDICATOR_COUNT - 1)
            val value = maxValue - valueRange * fraction
            val y = valueToY(value)
            val measured = textMeasurer.measure(
                text = formatStatisticsChartAmount(value = value),
                style = indicatorTextStyle,
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = 0f,
                    y = (y - measured.size.height / 2f)
                        .coerceIn(0f, layout.plotBottom - measured.size.height),
                ),
            )
            drawLine(
                color = gridColor,
                start = Offset(x = layout.plotLeft, y = y),
                end = Offset(x = layout.plotRight, y = y),
            )
        }

        val animatedTargets = buildStackedSegmentTargets(
            breakdown = breakdown,
            chartSize = chartSize,
            minValue = minValue,
            maxValue = maxValue,
            density = density.density,
            progress = animationProgress.value,
        )
        animatedTargets.forEach { target ->
            val color = statisticsChartSeriesColor(index = target.seriesIndex)
            drawRect(
                color = color,
                topLeft = target.rect.topLeft,
                size = target.rect.size,
            )
        }

        breakdown.bars.forEachIndexed { index, bar ->
            val label = formatStatisticsBarLabel(
                bar = StatisticsChartBar(
                    startDateEpochMillis = bar.startDateEpochMillis,
                    endDateEpochMillis = bar.endDateEpochMillis,
                    estimatedEarningsMicros = 0L,
                ),
                timeRange = timeRange,
            )
            val measured = textMeasurer.measure(text = label, style = indicatorTextStyle)
            val centerX = layout.barCenter(index = index, barCount = breakdown.bars.size)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (centerX - measured.size.width / 2f)
                        .coerceIn(
                            minimumValue = 0f,
                            maximumValue = (size.width - measured.size.width).coerceAtLeast(0f),
                        ),
                    y = layout.plotBottom + STACKED_CHART_LABEL_TOP_PADDING.toPx(),
                ),
            )
        }

        selectedSegment?.let { selected ->
            val selectedRect = fullSizeTargets.firstOrNull { target ->
                target.dataIndex == selected.dataIndex && target.seriesIndex == selected.seriesIndex
            }?.rect ?: return@let
            drawRect(
                color = popupContentColor.copy(alpha = STACKED_CHART_SELECTION_ALPHA),
                topLeft = selectedRect.topLeft,
                size = selectedRect.size,
                style = Stroke(width = STACKED_CHART_SELECTION_WIDTH.toPx()),
            )

            val series = breakdown.series[selected.seriesIndex]
            val popupText = "${series.label}: ${formatStatisticsMoney(selected.valueMicros, currencyCode)}"
            val popupStyle = labelTextStyle.copy(color = popupContentColor)
            val horizontalPadding = STACKED_CHART_POPUP_HORIZONTAL_PADDING.toPx()
            val verticalPadding = STACKED_CHART_POPUP_VERTICAL_PADDING.toPx()
            val measured = textMeasurer.measure(
                text = popupText,
                style = popupStyle,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(
                    maxWidth = (size.width - horizontalPadding * 2f)
                        .toInt()
                        .coerceAtLeast(0),
                ),
            )
            val popupSize = Size(
                width = measured.size.width + horizontalPadding * 2f,
                height = measured.size.height + verticalPadding * 2f,
            )
            val popupX = (selectedRect.center.x - popupSize.width / 2f)
                .coerceIn(
                    minimumValue = 0f,
                    maximumValue = (size.width - popupSize.width).coerceAtLeast(0f),
                )
            val preferredY = selectedRect.top - popupSize.height - STACKED_CHART_POPUP_GAP.toPx()
            val popupY = if (preferredY >= 0f) {
                preferredY
            } else {
                (selectedRect.bottom + STACKED_CHART_POPUP_GAP.toPx())
                    .coerceAtMost(size.height - popupSize.height)
            }
            drawRoundRect(
                color = popupContainerColor,
                topLeft = Offset(x = popupX, y = popupY),
                size = popupSize,
                cornerRadius = CornerRadius(
                    x = STACKED_CHART_POPUP_CORNER_RADIUS.toPx(),
                    y = STACKED_CHART_POPUP_CORNER_RADIUS.toPx(),
                ),
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = popupX + horizontalPadding,
                    y = popupY + verticalPadding,
                ),
            )
        }
    }
}

private fun buildStackedSegmentTargets(
    breakdown: StatisticsChartBreakdown,
    chartSize: IntSize,
    minValue: Double,
    maxValue: Double,
    density: Float,
    progress: Float,
): List<StackedSegmentTarget> {
    if (chartSize == IntSize.Zero || breakdown.bars.isEmpty()) return emptyList()
    val layout = stackedChartLayout(
        width = chartSize.width.toFloat(),
        height = chartSize.height.toFloat(),
        barCount = breakdown.bars.size,
        density = density,
    )
    val valueRange = (maxValue - minValue).takeUnless { range -> range == 0.0 } ?: 1.0
    fun valueToY(value: Double): Float = layout.plotBottom -
            (((value - minValue) / valueRange) * layout.plotHeight).toFloat()

    return buildList {
        breakdown.bars.forEachIndexed { dataIndex, bar ->
            var positiveBase = 0.0
            var negativeBase = 0.0
            val centerX = layout.barCenter(index = dataIndex, barCount = breakdown.bars.size)
            breakdown.series.forEachIndexed seriesLoop@{ seriesIndex, series ->
                val valueMicros = bar.earningsMicrosBySeries[series.key] ?: 0L
                if (valueMicros == 0L) return@seriesLoop
                val value = valueMicros / MICROS_PER_CURRENCY_UNIT
                val startValue: Double
                val endValue: Double
                if (value >= 0.0) {
                    startValue = positiveBase
                    positiveBase += value
                    endValue = positiveBase
                } else {
                    startValue = negativeBase
                    negativeBase += value
                    endValue = negativeBase
                }
                val startY = valueToY(value = startValue * progress)
                val endY = valueToY(value = endValue * progress)
                val top = min(startY, endY)
                val bottom = max(startY, endY)
                add(
                    StackedSegmentTarget(
                        rect = Rect(
                            left = centerX - layout.barWidth / 2f,
                            top = top,
                            right = centerX + layout.barWidth / 2f,
                            bottom = bottom,
                        ),
                        dataIndex = dataIndex,
                        seriesIndex = seriesIndex,
                        valueMicros = valueMicros,
                    ),
                )
            }
        }
    }
}

private fun stackedChartLayout(
    width: Float,
    height: Float,
    barCount: Int,
    density: Float,
): StackedChartLayout {
    val plotLeft = STACKED_CHART_INDICATOR_WIDTH.value * density
    val plotRight = max(plotLeft, width - STACKED_CHART_END_PADDING.value * density)
    val plotBottom = min(height, STACKED_CHART_PLOT_HEIGHT.value * density)
    val availableBarWidth = if (barCount == 0) {
        STACKED_CHART_MAX_BAR_WIDTH.value * density
    } else {
        (plotRight - plotLeft) / max(1, barCount) * STACKED_CHART_BAR_WIDTH_FRACTION
    }
    return StackedChartLayout(
        plotLeft = plotLeft,
        plotRight = plotRight,
        plotBottom = plotBottom,
        barWidth = min(STACKED_CHART_MAX_BAR_WIDTH.value * density, availableBarWidth),
    )
}

private data class StackedChartLayout(
    val plotLeft: Float,
    val plotRight: Float,
    val plotBottom: Float,
    val barWidth: Float,
) {
    val plotHeight: Float
        get() = plotBottom

    fun barCenter(index: Int, barCount: Int): Float {
        if (barCount <= 1) return (plotLeft + plotRight) / 2f
        val usableWidth = (plotRight - plotLeft - barWidth).coerceAtLeast(0f)
        return plotLeft + barWidth / 2f + usableWidth * index / (barCount - 1)
    }
}

private data class StackedSegmentTarget(
    val rect: Rect,
    val dataIndex: Int,
    val seriesIndex: Int,
    val valueMicros: Long,
)

private const val STACKED_CHART_INDICATOR_COUNT = 4
private const val STACKED_CHART_ANIMATION_MILLIS = 500
private const val STACKED_CHART_POPUP_DURATION_MILLIS = 1_500L
private const val STACKED_CHART_BAR_WIDTH_FRACTION = 0.55f
private const val STACKED_CHART_SELECTION_ALPHA = 0.8f
private val STACKED_CHART_PLOT_HEIGHT = 210.dp
private val STACKED_CHART_INDICATOR_WIDTH = 48.dp
private val STACKED_CHART_END_PADDING = 8.dp
private val STACKED_CHART_MAX_BAR_WIDTH = 22.dp
private val STACKED_CHART_LABEL_TOP_PADDING = 8.dp
private val STACKED_CHART_SELECTION_WIDTH = 1.dp
private val STACKED_CHART_POPUP_HORIZONTAL_PADDING = 6.dp
private val STACKED_CHART_POPUP_VERTICAL_PADDING = 3.dp
private val STACKED_CHART_POPUP_GAP = 4.dp
private val STACKED_CHART_POPUP_CORNER_RADIUS = 6.dp
