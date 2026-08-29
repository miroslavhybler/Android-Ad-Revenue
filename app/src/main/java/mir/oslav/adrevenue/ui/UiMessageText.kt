package mir.oslav.adrevenue.ui

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mir.oslav.adrevenue.R

@Composable
fun UiMessageText(
    message: UiMessage,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildString {
            append(stringResource(message.kind.stringRes))
            message.details?.takeIf { it.isNotBlank() }?.let { details ->
                append('\n')
                append(stringResource(R.string.message_details, details))
            }
        },
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

private val UiMessageKind.stringRes: Int
    @StringRes
    get() = when (this) {
        UiMessageKind.AccountSelectionNeedsAuthorization -> R.string.message_account_selection_needs_authorization
        UiMessageKind.AuthorizationFailed -> R.string.message_authorization_failed
        UiMessageKind.AuthorizationRequired -> R.string.message_authorization_required
        UiMessageKind.CacheRestoreFailed -> R.string.message_cache_restore_failed
        UiMessageKind.LoadAccountsFailed -> R.string.message_load_accounts_failed
        UiMessageKind.MissingWebClientId -> R.string.message_missing_web_client_id
        UiMessageKind.NoAdMobAccounts -> R.string.message_no_admob_accounts
        UiMessageKind.Offline -> R.string.message_offline
        UiMessageKind.RefreshFailed -> R.string.message_refresh_failed
        UiMessageKind.SelectAdMobAccount -> R.string.message_select_admob_account
        UiMessageKind.SignedOut -> R.string.message_signed_out
        UiMessageKind.SignInFailed -> R.string.message_sign_in_failed
    }
