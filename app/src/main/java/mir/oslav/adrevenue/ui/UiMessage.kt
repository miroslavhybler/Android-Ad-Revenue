package mir.oslav.adrevenue.ui

import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot

data class UiMessage(
    val kind: UiMessageKind,
    val details: String? = null,
) {
    companion object {
        fun fromThrowable(
            throwable: Throwable,
            fallbackKind: UiMessageKind,
        ): UiMessage =
            UiMessage(
                kind = if (throwable.message?.contains("ADMOB_WEB_CLIENT_ID") == true) {
                    UiMessageKind.MissingWebClientId
                } else {
                    fallbackKind
                },
                details = throwable.message,
            )
    }
}

enum class UiMessageKind {
    AccountSelectionNeedsAuthorization,
    AuthorizationFailed,
    AuthorizationRequired,
    CacheRestoreFailed,
    LoadAccountsFailed,
    MissingWebClientId,
    NoAdMobAccounts,
    Offline,
    RefreshFailed,
    SelectAdMobAccount,
    SignedOut,
    SignInFailed,
}

fun CachedAdMobSnapshot.postRefreshMessage(): UiMessage? =
    when {
        accounts.isEmpty() -> UiMessage(UiMessageKind.NoAdMobAccounts)
        accounts.size > 1 && selectedAccountName == null -> UiMessage(UiMessageKind.SelectAdMobAccount)
        else -> null
    }
