package mir.oslav.adrevenue.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.SignedInUser
import mir.oslav.adrevenue.ui.AdRevenueTheme
import mir.oslav.adrevenue.ui.PreviewUI
import mir.oslav.adrevenue.ui.UiMessageText

@Composable
fun AccountPane(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    AccountPaneImpl(
        state = state.value,
        onAccountSelected = viewModel::selectAccount,
        modifier = modifier,
    )
}

@Composable
private fun AccountPaneImpl(
    state: AccountUiState,
    onAccountSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val signedInUser = state.signedInUser

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.account_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                end = 20.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            state.message?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        UiMessageText(
                            message = message,
                            modifier = Modifier.padding(all = 16.dp),
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    Text(
                        text = stringResource(id = R.string.account_google_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            supportingContent = signedInUser?.displayName?.let {
                                { Text(text = signedInUser.id) }
                            },
                        ) {
                            Text(
                                text = signedInUser?.displayName
                                    ?: signedInUser?.id
                                    ?: stringResource(id = R.string.account_not_available),
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    Text(
                        text = stringResource(id = R.string.account_admob_account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        state.selectedAccount?.let { account ->
                            Column {
                                ListItem(
                                    overlineContent = {
                                        Text(text = stringResource(id = R.string.account_publisher_id))
                                    },
                                ) {
                                    Text(text = account.publisherId)
                                }
                                HorizontalDivider()
                                ListItem(
                                    overlineContent = {
                                        Text(text = stringResource(id = R.string.account_currency))
                                    },
                                ) {
                                    Text(text = account.currencyCode)
                                }
                                HorizontalDivider()
                                ListItem(
                                    overlineContent = {
                                        Text(text = stringResource(id = R.string.account_reporting_time_zone))
                                    },
                                ) {
                                    Text(text = account.reportingTimeZone)
                                }
                            }
                        } ?: ListItem(
                            content = {
                                Text(text = stringResource(id = R.string.account_no_account))
                            },
                        )
                    }
                }
            }

            if (state.accounts.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(weight = 1f),
                            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.account_choose_account),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!state.canSelectAccount) {
                                Text(
                                    text = stringResource(id = R.string.account_refresh_to_switch),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (state.isSelectingAccount) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(size = 24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.accounts.forEachIndexed { index, account ->
                                val selected = account.name == state.selectedAccountName
                                val enabled = state.canSelectAccount && !state.isSelectingAccount

                                ListItem(
                                    selected = selected,
                                    onClick = { onAccountSelected(account.name) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = enabled,
                                    supportingContent = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.account_metadata,
                                                account.currencyCode,
                                                account.reportingTimeZone,
                                            ),
                                        )
                                    },
                                    leadingContent = {
                                        RadioButton(
                                            selected = selected,
                                            onClick = null,
                                            enabled = enabled,
                                        )
                                    },
                                ) {
                                    Text(text = account.publisherId)
                                }
                                if (index != state.accounts.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@PreviewUI
@Composable
private fun AccountPanePreview() {
    AdRevenueTheme {
        AccountPaneImpl(
            state = AccountUiState(
                snapshot = CachedAdMobSnapshot(
                    signedInUser = SignedInUser(
                        id = "JohnDoe@google.com",
                        displayName = "John Doe",
                    ),
                    accounts = listOf(
                        AdMobAccount(
                            name = "accounts/pub-9876543210987654",
                            publisherId = "pub-9876543210987654",
                            reportingTimeZone = "America/Los_Angeles",
                            currencyCode = "USD",
                        ),
                        AdMobAccount(
                            name = "accounts/pub-1234567890123456",
                            publisherId = "pub-1234567890123456",
                            reportingTimeZone = "Europe/Prague",
                            currencyCode = "CZK",
                        ),
                    ),
                    selectedAccountName = "accounts/pub-1234567890123456",
                ),
                canSelectAccount = true,
            ),
            onAccountSelected = {},
        )
    }
}
