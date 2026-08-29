package mir.oslav.adrevenue.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mir.oslav.adrevenue.data.AdMobRepository
import mir.oslav.adrevenue.data.auth.AdMobAuthSession
import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.SignedInUser
import mir.oslav.adrevenue.ui.UiMessage
import mir.oslav.adrevenue.ui.UiMessageKind

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: AdMobRepository,
    private val authSession: AdMobAuthSession,
) : ViewModel() {
    private val operationState = MutableStateFlow(AccountOperationState())

    val uiState = combine(
        repository.snapshot,
        authSession.accessToken,
        operationState,
    ) { snapshot, accessToken, operation ->
        AccountUiState(
            snapshot = snapshot,
            canSelectAccount = !accessToken.isNullOrBlank(),
            selectingAccountName = operation.selectingAccountName,
            message = operation.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AccountUiState(),
    )

    fun selectAccount(accountName: String) {
        if (accountName == uiState.value.selectedAccountName || uiState.value.isSelectingAccount) {
            return
        }

        val accessToken = authSession.accessToken.value
        if (accessToken.isNullOrBlank()) {
            operationState.update {
                it.copy(message = UiMessage(UiMessageKind.AccountSelectionNeedsAuthorization))
            }
            return
        }

        viewModelScope.launch {
            operationState.update {
                it.copy(
                    selectingAccountName = accountName,
                    message = null,
                )
            }

            runCatching {
                repository.selectAccount(
                    accountName = accountName,
                    accessToken = accessToken,
                )
            }.onSuccess {
                operationState.update {
                    it.copy(
                        selectingAccountName = null,
                        message = null,
                    )
                }
            }.onFailure { throwable ->
                operationState.update {
                    it.copy(
                        selectingAccountName = null,
                        message = UiMessage.fromThrowable(
                            throwable = throwable,
                            fallbackKind = UiMessageKind.RefreshFailed,
                        ),
                    )
                }
            }
        }
    }
}

data class AccountUiState(
    val snapshot: CachedAdMobSnapshot = CachedAdMobSnapshot(),
    val canSelectAccount: Boolean = false,
    val selectingAccountName: String? = null,
    val message: UiMessage? = null,
) {
    val signedInUser: SignedInUser?
        get() = snapshot.signedInUser

    val accounts: List<AdMobAccount>
        get() = snapshot.accounts

    val selectedAccountName: String?
        get() = snapshot.selectedAccountName

    val selectedAccount: AdMobAccount?
        get() = snapshot.selectedAccount

    val isSelectingAccount: Boolean
        get() = selectingAccountName != null
}

private data class AccountOperationState(
    val selectingAccountName: String? = null,
    val message: UiMessage? = null,
)
