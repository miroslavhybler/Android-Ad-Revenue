package mir.oslav.adrevenue.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.ui.PreviewContainer

private val AppLogoShape = RoundedCornerShape(size = 16.dp)


@Composable
internal fun AuthHeader(
    modifier: Modifier = Modifier,
    step: AuthStep,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLogoPlaceholder()
        AuthStepIndicator(step = step)
    }
}



@Composable
private fun AppLogoPlaceholder(
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(id = R.string.auth_app_logo_content_description)
    val chartColor = MaterialTheme.colorScheme.onPrimaryContainer
    val accentColor = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = modifier
            .size(68.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = AppLogoShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Canvas(modifier = Modifier.padding(15.dp)) {
            val baseline = size.height * 0.78f
            val barWidth = size.width * 0.13f
            val gap = size.width * 0.10f
            val barHeights = listOf(0.30f, 0.48f, 0.68f)

            barHeights.fastForEachIndexed { index, heightFraction ->
                val left = size.width * 0.08f + index * (barWidth + gap)
                val top = baseline - size.height * heightFraction
                drawRoundRect(
                    color = chartColor.copy(alpha = 0.72f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, baseline - top),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }

            val path = Path().apply {
                moveTo(size.width * 0.10f, size.height * 0.58f)
                lineTo(size.width * 0.38f, size.height * 0.46f)
                lineTo(size.width * 0.60f, size.height * 0.52f)
                lineTo(size.width * 0.88f, size.height * 0.20f)
            }
            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = size.width * 0.08f, cap = StrokeCap.Round),
            )
            drawCircle(
                color = accentColor,
                radius = size.width * 0.07f,
                center = Offset(size.width * 0.88f, size.height * 0.20f),
            )
        }
    }
}

@Composable
private fun AuthStepIndicator(
    step: AuthStep,
    modifier: Modifier = Modifier,
) {
    val activeSteps = if (step == AuthStep.SignIn) 1 else 2

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(times = 2) { index ->
                val targetAlpha = if (index < activeSteps) 1f else 0.28f
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 180),
                    label = "auth-step-dot",
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            shape = CircleShape,
                        ),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.auth_step_indicator,
                if (step == AuthStep.SignIn) 1 else 2,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


@Composable
@PreviewLightDark
private fun AuthHeaderPreview() {
    PreviewContainer {
        AuthHeader(step = AuthStep.SignIn)
    }
}

@Composable
@PreviewLightDark
private fun AuthHeaderPreview2() {
    PreviewContainer {
        AuthHeader(step = AuthStep.AccountSelection)
    }
}

