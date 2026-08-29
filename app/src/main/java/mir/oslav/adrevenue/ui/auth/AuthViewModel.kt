package mir.oslav.adrevenue.ui.auth

import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mir.oslav.adrevenue.data.AdMobRepository
import mir.oslav.adrevenue.data.auth.AdMobAuthSession
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.SignedInUser
import mir.oslav.adrevenue.ui.UiMessage
import mir.oslav.adrevenue.ui.UiMessageKind

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AdMobRepository,
    private val authSession: AdMobAuthSession,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val selectedAccountName = savedStateHandle.getStateFlow<String?>(
        key = SelectedAccountKey,
        initialValue = null,
    )
    private val operationState = MutableStateFlow(AuthOperationState())
    private val _navigationEvents = MutableSharedFlow<AuthNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    val uiState = combine(
        flow = repository.snapshot,
        flow2 = selectedAccountName,
        flow3 = operationState,
    ) { snapshot, draftAccountName, operation ->
        val signedInUser = operation.pendingSignedInUser ?: snapshot.signedInUser
        val step = operation.stepOverride ?: if (signedInUser != null) {
            AuthStep.AccountSelection
        } else {
            AuthStep.SignIn
        }

        AuthUiState(
            isRestoring = operation.isRestoring,
            step = step,
            operation = operation.operation,
            signedInUser = signedInUser,
            accounts = snapshot.accounts,
            selectedAccountName = draftAccountName,
            message = operation.message,
            isStepForced = operation.stepOverride != null,
            hasConfirmedAccount = snapshot.selectedAccountName != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AuthUiState(),
    )

    init {
        viewModelScope.launch {
            val restoreFailure = runCatching {
                repository.restore()
            }.exceptionOrNull()
            val snapshot = repository.snapshot.value

            setSelectedAccountName(
                snapshot.selectedAccountName ?: snapshot.accounts.singleOrNull()?.name,
            )

            operationState.update {
                it.copy(
                    isRestoring = false,
                    message = when {
                        restoreFailure != null -> UiMessage(
                            UiMessageKind.CacheRestoreFailed,
                            restoreFailure.message,
                        )

                        snapshot.signedInUser != null &&
                                snapshot.selectedAccountName == null &&
                                snapshot.accounts.isEmpty() -> UiMessage(UiMessageKind.NoAdMobAccounts)

                        else -> null
                    },
                )
            }
        }
    }

    fun signInStarted() {
        operationState.update {
            it.copy(
                operation = AuthOperation.SigningIn,
                message = null,
                stepOverride = AuthStep.SignIn,
                pendingSignedInUser = null,
            )
        }
    }

    fun signInCanceled() {
        operationState.update {
            it.copy(
                operation = AuthOperation.Idle,
                message = null,
                stepOverride = AuthStep.SignIn,
                pendingSignedInUser = null,
            )
        }
    }

    fun signInFailed(throwable: Throwable) {
        operationState.update {
            it.copy(
                operation = AuthOperation.Idle,
                message = UiMessage.fromThrowable(throwable, UiMessageKind.SignInFailed),
                stepOverride = AuthStep.SignIn,
                pendingSignedInUser = null,
            )
        }
    }

    fun authorizationFailed(throwable: Throwable) {
        operationState.update {
            it.copy(
                operation = AuthOperation.Idle,
                message = UiMessage.fromThrowable(throwable, UiMessageKind.AuthorizationFailed),
                stepOverride = AuthStep.SignIn,
                pendingSignedInUser = null,
            )
        }
    }

    fun accessTokenAvailable(token: String) {
        val signedInUser = operationState.value.pendingSignedInUser
        if (signedInUser == null) {
            authorizationFailed(
                IllegalStateException("AdMob authorization finished without a signed-in user."),
            )
            return
        }

        authSession.setAccessToken(token)
        loadAccounts(
            signedInUser = signedInUser,
            accessToken = token,
        )
    }

    fun googleAccountSelected(signedInUser: SignedInUser) {
        operationState.update {
            it.copy(
                operation = AuthOperation.SigningIn,
                message = null,
                stepOverride = AuthStep.SignIn,
                pendingSignedInUser = signedInUser,
            )
        }
    }

    fun selectAccount(accountName: String) {
        if (uiState.value.accounts.none { it.name == accountName }) return
        setSelectedAccountName(accountName)
    }

    fun continueToDashboard() {
        val accountName = selectedAccountName.value ?: return
        val token = authSession.accessToken.value
        if (token.isNullOrBlank()) {
            operationState.update {
                it.copy(message = UiMessage(UiMessageKind.AccountSelectionNeedsAuthorization))
            }
            return
        }

        viewModelScope.launch {
            operationState.update {
                it.copy(
                    operation = AuthOperation.OpeningDashboard,
                    message = null,
                )
            }

            runCatching {
                repository.selectAccount(
                    accountName = accountName,
                    accessToken = token,
                )
            }.onSuccess {
                operationState.update {
                    it.copy(
                        operation = AuthOperation.Idle,
                        message = null,
                        stepOverride = null,
                        pendingSignedInUser = null,
                    )
                }
                _navigationEvents.tryEmit(AuthNavigationEvent.Home)
            }.onFailure { throwable ->
                operationState.update {
                    it.copy(
                        operation = AuthOperation.Idle,
                        message = UiMessage.fromThrowable(throwable, UiMessageKind.RefreshFailed),
                    )
                }
            }
        }
    }

    fun retryAccountLoading() {
        val signedInUser = uiState.value.signedInUser ?: return
        val token = authSession.accessToken.value
        if (token.isNullOrBlank()) {
            operationState.update {
                it.copy(message = UiMessage(UiMessageKind.AccountSelectionNeedsAuthorization))
            }
            return
        }

        loadAccounts(
            signedInUser = signedInUser,
            accessToken = token,
        )
    }

    suspend fun resetForAnotherGoogleAccount() {
        authSession.clear()
        setSelectedAccountName(null)
        operationState.value = AuthOperationState(
            isRestoring = false,
            stepOverride = AuthStep.SignIn,
        )
        repository.signOut()
    }

    private fun loadAccounts(
        signedInUser: SignedInUser,
        accessToken: String,
    ) {
        viewModelScope.launch {
            setSelectedAccountName(null)
            operationState.update {
                it.copy(
                    operation = AuthOperation.LoadingAccounts,
                    message = null,
                    stepOverride = AuthStep.AccountSelection,
                    pendingSignedInUser = signedInUser,
                )
            }

            runCatching {
                repository.discoverAccounts(
                    signedInUser = signedInUser,
                    accessToken = accessToken,
                )
            }.onSuccess {
                val accounts = repository.snapshot.value.accounts
                setSelectedAccountName(accounts.singleOrNull()?.name)
                operationState.update {
                    it.copy(
                        operation = AuthOperation.Idle,
                        message = if (accounts.isEmpty()) {
                            UiMessage(UiMessageKind.NoAdMobAccounts)
                        } else {
                            null
                        },
                        pendingSignedInUser = null,
                    )
                }
            }.onFailure { throwable ->
                operationState.update {
                    it.copy(
                        operation = AuthOperation.Idle,
                        message = UiMessage.fromThrowable(
                            throwable = throwable,
                            fallbackKind = UiMessageKind.LoadAccountsFailed,
                        ),
                    )
                }
            }
        }
    }

    private fun setSelectedAccountName(accountName: String?) {
        savedStateHandle[SelectedAccountKey] = accountName
    }

    private companion object {
        const val SelectedAccountKey = "auth.selected_account_name"
    }
}

@Keep
enum class AuthStep {
    SignIn,
    AccountSelection,
}

@Keep
enum class AuthOperation {
    Idle,
    SigningIn,
    LoadingAccounts,
    OpeningDashboard,
}

@Keep
enum class AuthNavigationEvent {
    Home,
}

@Keep
data class AuthUiState(
    val isRestoring: Boolean = true,
    val step: AuthStep = AuthStep.SignIn,
    val operation: AuthOperation = AuthOperation.Idle,
    val signedInUser: SignedInUser? = null,
    val accounts: List<AdMobAccount> = emptyList(),
    val selectedAccountName: String? = null,
    val message: UiMessage? = null,
    private val isStepForced: Boolean = false,
    internal val hasConfirmedAccount: Boolean = false,
) {
    val shouldShowAuth: Boolean
        get() = !isRestoring && (
                isStepForced ||
                        signedInUser == null ||
                        !hasConfirmedAccount
                )
}

private data class AuthOperationState(
    val isRestoring: Boolean = true,
    val operation: AuthOperation = AuthOperation.Idle,
    val message: UiMessage? = null,
    val stepOverride: AuthStep? = null,
    val pendingSignedInUser: SignedInUser? = null,
)
