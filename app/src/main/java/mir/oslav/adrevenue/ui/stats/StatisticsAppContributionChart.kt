package mir.oslav.adrevenue.ui.stats

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mir.oslav.adrevenue.R

private val contributionColors = listOf(
    Color(0xFF5D54C5), Color(0xFF006875), Color(0xFF7B5800),
    Color(0xFFB04A67), Color(0xFF4D7C5A), Color(0xFF7A5C99),
)

@Composable
internal fun StatisticsAppContributionCard(
    contributions: List<StatisticsAppContribution>,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val totalMicros = contributions.sumOf { it.estimatedEarningsMicros }
    val colors = remember(contributions.size) {
        contributions.indices.map { index -> contributionColors[index % contributionColors.size] }
    }
    val animatedEarnings = contributions.map { contribution ->
        animateFloatAsState(
            targetValue = contribution.estimatedEarningsMicros.toFloat(),
            animationSpec = tween(durationMillis = 450),
            label = "${contribution.appName} earnings",
        ).value
    }
    val animatedTotal = animatedEarnings.sum().coerceAtLeast(1f)

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.statistics_app_contribution_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 190.dp),
            ) {
                val diameter = minOf(a = size.width, b = size.height)
                val topLeft = androidx.compose.ui.geometry.Offset(
                    x = (size.width - diameter) / 2f,
                    y = (size.height - diameter) / 2f,
                )
                var startAngle = -90f
                contributions.forEachIndexed { index, _ ->
                    val sweep = animatedEarnings[index] / animatedTotal * 360f
                    drawArc(
                        color = colors[index],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(
                            width = diameter,
                            height = diameter
                        ),
                        style = Stroke(width = 30.dp.toPx()),
                    )
                    startAngle += sweep
                }
            }
            contributions.forEachIndexed { index, contribution ->
                val percentage =
                    contribution.estimatedEarningsMicros.toDouble() / totalMicros * 100.0
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (contribution.iconUrl != null) {
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
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = null,
                            tint = colors[index],
                            modifier = Modifier.size(size = 28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    Text(
                        text = contribution.appName,
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
