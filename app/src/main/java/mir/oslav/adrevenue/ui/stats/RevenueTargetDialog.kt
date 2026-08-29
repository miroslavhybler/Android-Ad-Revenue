package mir.oslav.adrevenue.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.cache.CachedRevenueTarget

@Composable
internal fun RevenueTargetDialog(
    target: CachedRevenueTarget?,
    currencyCode: String,
    onSave: (amountMicros: Long, rangeDays: Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountInput by rememberSaveable(target?.amountMicros) {
        mutableStateOf(revenueTargetAmountInput(amountMicros = target?.amountMicros))
    }
    var daysInput by rememberSaveable(target?.rangeDays) {
        mutableStateOf(
            (target?.rangeDays ?: DEFAULT_REVENUE_TARGET_RANGE_DAYS).toString(),
        )
    }
    val amountMicros = remember(amountInput) {
        parseRevenueTargetAmountMicros(input = amountInput)
    }
    val rangeDays = remember(daysInput) { daysInput.toIntOrNull() }
    val amountHasError = amountInput.isNotBlank() && amountMicros == null
    val daysHaveError = daysInput.isNotBlank() && (
        rangeDays == null ||
            rangeDays !in MIN_REVENUE_TARGET_RANGE_DAYS..MAX_REVENUE_TARGET_RANGE_DAYS
        )
    val canSave = amountMicros != null && rangeDays != null &&
        rangeDays in MIN_REVENUE_TARGET_RANGE_DAYS..MAX_REVENUE_TARGET_RANGE_DAYS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.statistics_target_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                Text(text = stringResource(id = R.string.statistics_target_dialog_body))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input -> amountInput = input },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = if (currencyCode.isBlank()) {
                                stringResource(id = R.string.statistics_target_amount)
                            } else {
                                stringResource(
                                    id = R.string.statistics_target_amount_with_currency,
                                    currencyCode,
                                )
                            },
                        )
                    },
                    supportingText = if (amountHasError) {
                        { Text(text = stringResource(id = R.string.statistics_target_amount_error)) }
                    } else {
                        null
                    },
                    isError = amountHasError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = daysInput,
                    onValueChange = { input -> daysInput = input.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.statistics_target_range_days)) },
                    supportingText = if (daysHaveError) {
                        {
                            Text(
                                text = stringResource(
                                    id = R.string.statistics_target_range_error,
                                    MIN_REVENUE_TARGET_RANGE_DAYS,
                                    MAX_REVENUE_TARGET_RANGE_DAYS,
                                ),
                            )
                        }
                    } else {
                        null
                    },
                    isError = daysHaveError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        checkNotNull(amountMicros),
                        checkNotNull(rangeDays),
                    )
                },
                enabled = canSave,
            ) {
                Text(text = stringResource(id = R.string.statistics_target_save))
            }
        },
        dismissButton = {
            if (target == null) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.statistics_target_cancel))
                }
            } else {
                Row {
                    TextButton(onClick = onClear) {
                        Text(text = stringResource(id = R.string.statistics_target_remove))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.statistics_target_cancel))
                    }
                }
            }
        },
    )
}

private fun parseRevenueTargetAmountMicros(input: String): Long? {
    val amount = input.trim().replace(oldChar = ',', newChar = '.').toBigDecimalOrNull()
        ?: return null
    if (amount <= BigDecimal.ZERO) return null

    return runCatching {
        amount
            .multiply(BigDecimal.valueOf(MICROS_PER_CURRENCY_UNIT.toLong()))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
            .takeIf { micros -> micros > 0L }
    }.getOrNull()
}

private fun revenueTargetAmountInput(amountMicros: Long?): String = amountMicros
    ?.let { micros -> BigDecimal.valueOf(micros, REVENUE_TARGET_MICRO_SCALE) }
    ?.stripTrailingZeros()
    ?.toPlainString()
    .orEmpty()

private const val REVENUE_TARGET_MICRO_SCALE = 6
