package mir.oslav.adrevenue.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mir.oslav.adrevenue.R

/** Shared app/ad-format selector used by both statistics visualizations. */
@Composable
internal fun StatisticsBreakdownSelector(
    selectedDimension: StatisticsBreakdownDimension,
    onDimensionSelected: (StatisticsBreakdownDimension) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        StatisticsBreakdownDimension.entries.forEach { dimension ->
            FilterChip(
                selected = selectedDimension == dimension,
                onClick = { onDimensionSelected(dimension) },
                label = {
                    Text(
                        text = stringResource(
                            id = when (dimension) {
                                StatisticsBreakdownDimension.App ->
                                    R.string.statistics_group_apps

                                StatisticsBreakdownDimension.AdFormat ->
                                    R.string.statistics_group_ad_formats
                            },
                        ),
                    )
                },
            )
        }
    }
}
