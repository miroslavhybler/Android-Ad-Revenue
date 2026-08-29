package mir.oslav.adrevenue.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.ui.UiMessageKind
import mir.oslav.adrevenue.ui.UiMessageText


@Composable
internal fun SignInStep(
    modifier: Modifier = Modifier,
    state: AuthUiState,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 30.dp),
        verticalArrangement = Arrangement.spacedBy(
            20.dp,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 10.dp)) {
            Text(
                text = stringResource(id = R.string.auth_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(id = R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.isRestoring) {
            LoadingLabel(text = stringResource(id = R.string.auth_loading_cache))
        } else {
            state.message?.let { message ->
                FailureMessage {
                    UiMessageText(
                        message = message,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccountSelectionStep(
    state: AuthUiState,
    onAccountSelected: (String) -> Unit,
    onRetryClick: () -> Unit,
    onUseAnotherGoogleAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = state.operation == AuthOperation.LoadingAccounts
    val hasLoadFailure = state.message?.kind == UiMessageKind.LoadAccountsFailed
    val hasNoAccounts = !isLoading && !hasLoadFailure && state.accounts.isEmpty()
    val isOpeningDashboard = state.operation == AuthOperation.OpeningDashboard

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 30.dp),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 10.dp)) {
            Text(
                text = stringResource(id = R.string.auth_accounts_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(id = R.string.auth_accounts_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }


        state.signedInUser?.id?.takeIf(predicate = String::isNotBlank)?.let { email ->
            Text(
                text = stringResource(id = R.string.auth_signed_in_as, email),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            isLoading -> AccountLoadingContent()

            hasLoadFailure -> {
                AccountProblemCard(
                    title = stringResource(id = R.string.auth_load_accounts_failed_title),
                    body = stringResource(id = R.string.auth_load_accounts_failed_body),
                    isFailure = true,
                )
                TextButton(
                    onClick = onUseAnotherGoogleAccountClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.auth_use_another_account))
                }
            }

            hasNoAccounts -> {
                AccountProblemCard(
                    title = stringResource(id = R.string.auth_no_account_title),
                    body = stringResource(id = R.string.auth_no_account_body),
                    isFailure = false,
                )
                TextButton(
                    onClick = onRetryClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.auth_try_again))
                }
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                    state.accounts.forEach { account ->
                        AccountOptionCard(
                            account = account,
                            selected = account.name == state.selectedAccountName,
                            enabled = !isOpeningDashboard,
                            onClick = { onAccountSelected(account.name) },
                        )
                    }
                }

                state.message?.let { message ->
                    FailureMessage {
                        UiMessageText(
                            message = message,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                TextButton(
                    onClick = onUseAnotherGoogleAccountClick,
                    enabled = !isOpeningDashboard,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(text = stringResource(id = R.string.auth_use_another_account))
                }
            }
        }
    }
}

@Composable
internal fun AuthBottomAction(
    state: AuthUiState,
    onSignInClick: () -> Unit,
    onContinueClick: () -> Unit,
    onRetryClick: () -> Unit,
    onUseAnotherGoogleAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 32.dp)
    ) {
        AnimatedContent(
            modifier = Modifier
                .fillMaxWidth(),
            targetState = state.step,
            transitionSpec = {
                val enter = fadeIn(animationSpec = tween(durationMillis = 180))
                val exit = fadeOut(animationSpec = tween(durationMillis = 120))
                enter togetherWith exit
            },
            label = "auth-bottom-action",
        ) { step ->
            when (step) {
                AuthStep.SignIn -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                    ) {
                        AnimatedVisibility(
                            visible = state.operation == AuthOperation.SigningIn,
                        ) {
                            LoadingLabel(text = stringResource(id = R.string.auth_signing_in))
                        }

                        GoogleSignInButton(
                            onClick = onSignInClick,
                            enabled = !state.isRestoring &&
                                    state.operation != AuthOperation.SigningIn,
                        )
                    }
                }

                AuthStep.AccountSelection -> {
                    val hasLoadFailure = state.message?.kind == UiMessageKind.LoadAccountsFailed
                    val hasNoAccounts = state.operation != AuthOperation.LoadingAccounts &&
                            !hasLoadFailure &&
                            state.accounts.isEmpty()

                    when {
                        hasLoadFailure -> PrimaryActionButton(
                            text = stringResource(id = R.string.auth_try_again),
                            onClick = onRetryClick,
                        )

                        hasNoAccounts -> PrimaryActionButton(
                            text = stringResource(id = R.string.auth_use_another_account),
                            onClick = onUseAnotherGoogleAccountClick,
                        )

                        else -> PrimaryActionButton(
                            text = if (state.operation == AuthOperation.OpeningDashboard) {
                                stringResource(id = R.string.auth_opening_dashboard)
                            } else {
                                stringResource(id = R.string.auth_continue)
                            },
                            onClick = onContinueClick,
                            enabled = state.selectedAccountName != null &&
                                    state.operation == AuthOperation.Idle,
                            showProgress = state.operation == AuthOperation.OpeningDashboard,
                        )
                    }
                }
            }
        }


        Text(
            modifier = Modifier,
            text = stringResource(id = R.string.not_affiliated_with_google),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

}



@Composable
private fun AccountLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LoadingLabel(text = stringResource(R.string.auth_finding_accounts))
        repeat(times = 2) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = AuthCardShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {}
        }
    }
}

@Composable
private fun AccountProblemCard(
    title: String,
    body: String,
    isFailure: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isFailure) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isFailure) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AuthCardShape,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun AccountOptionCard(
    account: AdMobAccount,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(durationMillis = 180),
        label = "account-container",
    )
    val outlineColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 180),
        label = "account-outline",
    )
    val outlineWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = 180),
        label = "account-outline-width",
    )
    val publisherId = account.publisherId.ifBlank { account.name }
    val metadata = stringResource(
        id = R.string.auth_account_metadata,
        account.currencyCode,
        account.reportingTimeZone,
    )
    val accessibilityLabel = stringResource(
        id = R.string.auth_account_accessibility,
        publisherId,
        account.currencyCode,
        account.reportingTimeZone,
    )
    val selectionState = stringResource(
        id = if (selected) R.string.auth_account_selected
        else R.string
            .auth_account_not_selected,
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
                stateDescription = selectionState
            },
        shape = AuthCardShape,
        color = containerColor,
        border = BorderStroke(outlineWidth, outlineColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = publisherId,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FailureMessage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AuthCardShape,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun LoadingLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val containerColor = if (darkTheme) Color(0xFF131314) else Color.White
    val contentColor = if (darkTheme) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
    val borderColor = if (darkTheme) Color(0xFF8E918F) else Color(0xFF747775)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.62f),
            disabledContentColor = contentColor.copy(alpha = 0.62f),
        ),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo_google),
            contentDescription = stringResource(R.string.auth_google_logo_content_description),
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.auth_sign_in),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val AuthCardShape = RoundedCornerShape(size = 8.dp)

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showProgress: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(size = 20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(width = 10.dp))
        }
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
