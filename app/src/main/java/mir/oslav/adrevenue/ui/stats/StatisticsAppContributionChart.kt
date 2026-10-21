package mir.oslav.adrevenue.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.Pie
import mir.oslav.adrevenue.R

@Composable
internal fun StatisticsContributionCard(
    contributions: List<StatisticsContribution>,
    dimension: StatisticsBreakdownDimension,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val totalMicros = contributions.sumOf { it.estimatedEarningsMicros }
    val colors = remember(contributions) {
        contributions.map { contribution ->
            statisticsChartSeriesColor(index = contribution.colorIndex)
        }
    }
    val animatedEarnings = contributions.map { contribution ->
        animateFloatAsState(
            targetValue = contribution.estimatedEarningsMicros.toFloat(),
            animationSpec = tween(durationMillis = 450),
            label = "${contribution.label} earnings",
        ).value
    }
    val pieData = contributions.mapIndexed { index, contribution ->
        Pie(
            label = contribution.label,
            data = animatedEarnings[index].coerceAtLeast(0f).toDouble(),
            color = colors[index],
        )
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Text(
                text = stringResource(
                    id = when (dimension) {
                        StatisticsBreakdownDimension.App ->
                            R.string.statistics_app_contribution_title

                        StatisticsBreakdownDimension.AdFormat ->
                            R.string.statistics_ad_format_contribution_title
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            PieChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 190.dp),
                data = pieData,
                startDegree = -90f,
                selectedScale = 1f,
                selectedPaddingDegree = 0f,
                labelHelperProperties = LabelHelperProperties(enabled = false),
                style = Pie.Style.Stroke(width = 30.dp),
            )
            contributions.forEachIndexed { index, contribution ->
                val percentage =
                    contribution.estimatedEarningsMicros.toDouble() / totalMicros * 100.0
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (
                        dimension == StatisticsBreakdownDimension.App &&
                        contribution.iconUrl != null
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(size = 28.dp)
                                .clip(shape = MaterialTheme.shapes.small),
                            model = contribution.iconUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.ic_jetpack_compose_logo),
                            error = painterResource(id = R.drawable.ic_jetpack_compose_logo),
                        )
                    } else if (dimension == StatisticsBreakdownDimension.App) {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = null,
                            tint = colors[index],
                            modifier = Modifier.size(size = 28.dp)
                        )
                    } else {
                        Canvas(modifier = Modifier.size(size = 28.dp)) {
                            drawCircle(
                                color = colors[index],
                                radius = 7.dp.toPx(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    Text(
                        text = contribution.label,
                        modifier = Modifier.weight(weight = 1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "%.1f%%  %s".format(
                            percentage,
                            formatStatisticsMoney(
                                contribution.estimatedEarningsMicros,
                                currencyCode
                            ),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
