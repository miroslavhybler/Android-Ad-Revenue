package mir.oslav.adrevenue.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.SignedInUser
import mir.oslav.adrevenue.ui.PreviewUI
import mir.oslav.adrevenue.ui.Destination
import mir.oslav.adrevenue.ui.UiMessage
import mir.oslav.adrevenue.ui.UiMessageKind
import mir.oslav.adrevenue.ui.AdRevenueTheme
import mir.oslav.adrevenue.ui.PreviewContainer

@Composable
fun AuthPane(
    onNavigate: (Destination) -> Unit,
    onSignInClick: () -> Unit,
    onUseAnotherGoogleAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                AuthNavigationEvent.Home -> onNavigate(Destination.Home)
            }
        }
    }

    AuthPaneImpl(
        state = state.value,
        onSignInClick = onSignInClick,
        onAccountSelected = viewModel::selectAccount,
        onContinueClick = viewModel::continueToDashboard,
        onRetryClick = viewModel::retryAccountLoading,
        onUseAnotherGoogleAccountClick = onUseAnotherGoogleAccountClick,
        modifier = modifier,
    )
}

@Composable
private fun AuthPaneImpl(
    state: AuthUiState,
    onSignInClick: () -> Unit,
    onAccountSelected: (String) -> Unit,
    onContinueClick: () -> Unit,
    onRetryClick: () -> Unit,
    onUseAnotherGoogleAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AuthHeader(step = state.step)
        },
        bottomBar = {
            AuthBottomAction(
                state = state,
                onSignInClick = onSignInClick,
                onContinueClick = onContinueClick,
                onRetryClick = onRetryClick,
                onUseAnotherGoogleAccountClick = onUseAnotherGoogleAccountClick,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = 28.dp,
                        end = 20.dp,
                        bottom = 32.dp,
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = 220)) +
                                slideInVertically(
                                    animationSpec = tween(durationMillis = 220),
                                    initialOffsetY = { height -> height / 12 },
                                ))
                            .togetherWith(
                               exit= fadeOut(animationSpec = tween(durationMillis = 140)),
                            )
                            .using(sizeTransform= SizeTransform(clip = false))
                    },
                    label = "auth-step",
                ) { step ->
                    when (step) {
                        AuthStep.SignIn -> SignInStep(state = state)
                        AuthStep.AccountSelection -> AccountSelectionStep(
                            state = state,
                            onAccountSelected = onAccountSelected,
                            onRetryClick = onRetryClick,
                            onUseAnotherGoogleAccountClick = onUseAnotherGoogleAccountClick,
                        )
                    }
                }
            }
        }
    }
}

@PreviewUI
@Composable
private fun AuthPanePreview(
    @PreviewParameter(provider = AuthPanePreviewProvider::class) state: AuthUiState,
) {
    PreviewContainer() {
        AuthPaneImpl(
            state = state,
            onSignInClick = {},
            onAccountSelected = {},
            onContinueClick = {},
            onRetryClick = {},
            onUseAnotherGoogleAccountClick = {},
        )
    }
}

private class AuthPanePreviewProvider : PreviewParameterProvider<AuthUiState> {
    private val signedInUser = SignedInUser(
        id = "developer@example.com",
        displayName = "Developer",
    )
    private val usdAccount = AdMobAccount(
        name = "accounts/pub-1234567890123456",
        publisherId = "pub-1234567890123456",
        reportingTimeZone = "Europe/Prague",
        currencyCode = "USD",
    )
    private val eurAccount = AdMobAccount(
        name = "accounts/pub-6543210987654321",
        publisherId = "pub-6543210987654321",
        reportingTimeZone = "Europe/Berlin",
        currencyCode = "EUR",
    )

    override val values: Sequence<AuthUiState> = sequenceOf(
        AuthUiState(isRestoring = false),
        AuthUiState(
            isRestoring = false,
            step = AuthStep.AccountSelection,
            operation = AuthOperation.LoadingAccounts,
            signedInUser = signedInUser,
        ),
        AuthUiState(
            isRestoring = false,
            step = AuthStep.AccountSelection,
            signedInUser = signedInUser,
            accounts = listOf(usdAccount),
            selectedAccountName = usdAccount.name,
        ),
        AuthUiState(
            isRestoring = false,
            step = AuthStep.AccountSelection,
            signedInUser = signedInUser,
            accounts = listOf(usdAccount, eurAccount),
        ),
        AuthUiState(
            isRestoring = false,
            step = AuthStep.AccountSelection,
            signedInUser = signedInUser,
            message = UiMessage(UiMessageKind.NoAdMobAccounts),
        ),
        AuthUiState(
            isRestoring = false,
            step = AuthStep.AccountSelection,
            signedInUser = signedInUser,
            message = UiMessage(UiMessageKind.LoadAccountsFailed),
        ),
    )
}
