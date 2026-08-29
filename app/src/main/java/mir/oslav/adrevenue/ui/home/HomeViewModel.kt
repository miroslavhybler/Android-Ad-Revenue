package mir.oslav.adrevenue.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mir.oslav.adrevenue.ui.UiMessage
import mir.oslav.adrevenue.ui.UiMessageKind
import mir.oslav.adrevenue.ui.postRefreshMessage
import mir.oslav.adrevenue.core.network.NetworkMonitor
import mir.oslav.adrevenue.data.AdMobRepository
import mir.oslav.adrevenue.data.auth.AdMobAuthSession
import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.model.SignedInUser

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AdMobRepository,
    private val authSession: AdMobAuthSession,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    private val operationState = MutableStateFlow(HomeOperationState())

    val uiState = combine(
        repository.snapshot,
        networkMonitor.isOnline.catch { emit(false) },
        operationState,
    ) { snapshot, isOnline, operation ->
        HomeUiState(
            isOnline = isOnline,
            isRefreshing = operation.isRefreshing,
            message = operation.message,
            snapshot = snapshot,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun authorizationStartedForRefresh() {
        operationState.update {
            it.copy(
                isRefreshing = true,
                message = null,
            )
        }
    }

    fun authorizationFailed(throwable: Throwable) {
        operationState.update {
            it.copy(
                isRefreshing = false,
                message = UiMessage.fromThrowable(throwable, UiMessageKind.AuthorizationFailed),
            )
        }
    }

    fun accessTokenAvailable(token: String) {
        authSession.setAccessToken(token)
        refresh()
    }

    fun refresh() {
        val token = authSession.accessToken.value
        if (token.isNullOrBlank()) {
            operationState.update {
                it.copy(message = UiMessage(UiMessageKind.AuthorizationRequired))
            }
            return
        }

        if (!uiState.value.isOnline) {
            operationState.update {
                it.copy(message = UiMessage(UiMessageKind.Offline))
            }
            return
        }

        viewModelScope.launch {
            operationState.update {
                it.copy(
                    isRefreshing = true,
                    message = null,
                )
            }

            runCatching {
                repository.refresh(token)
            }.onSuccess {
                val snapshot = repository.snapshot.value
                operationState.update {
                    it.copy(
                        isRefreshing = false,
                        message = snapshot.postRefreshMessage(),
                    )
                }
            }.onFailure { throwable ->
                operationState.update {
                    it.copy(
                        isRefreshing = false,
                        message = UiMessage.fromThrowable(throwable, UiMessageKind.RefreshFailed),
                    )
                }
            }
        }
    }

    fun selectAccount(accountName: String) {
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
                    isRefreshing = true,
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
                        isRefreshing = false,
                        message = null,
                    )
                }
            }.onFailure { throwable ->
                operationState.update {
                    it.copy(
                        isRefreshing = false,
                        message = UiMessage.fromThrowable(throwable, UiMessageKind.RefreshFailed),
                    )
                }
            }
        }
    }

    fun signOutFinished() {
        authSession.clear()
        viewModelScope.launch {
            repository.signOut()
            operationState.update {
                it.copy(
                    isRefreshing = false,
                    message = UiMessage(UiMessageKind.SignedOut),
                )
            }
        }
    }
}

data class HomeUiState(
    val isOnline: Boolean = true,
    val isRefreshing: Boolean = false,
    val message: UiMessage? = null,
    val snapshot: CachedAdMobSnapshot = CachedAdMobSnapshot(),
) {
    val signedInUser: SignedInUser?
        get() = snapshot.signedInUser

    val accounts: List<AdMobAccount>
        get() = snapshot.accounts

    val selectedAccountName: String?
        get() = snapshot.selectedAccountName

    val selectedAccount: AdMobAccount?
        get() = snapshot.selectedAccount

    val report: AdMobNetworkReport?
        get() = snapshot.report

    val refreshedAtEpochMillis: Long?
        get() = snapshot.refreshedAtEpochMillis

    val hasCachedData: Boolean
        get() = snapshot.hasDisplayableData

    val requiresAccountSelection: Boolean
        get() = signedInUser != null && selectedAccountName == null

    val shouldShowHome: Boolean
        get() = signedInUser != null && selectedAccountName != null

    val shouldShowNoConnectionEmpty: Boolean
        get() = !isOnline && !hasCachedData

    val shouldShowCachedWarning: Boolean
        get() = !isOnline && hasCachedData
}

private data class HomeOperationState(
    val isRefreshing: Boolean = false,
    val message: UiMessage? = null,
)
