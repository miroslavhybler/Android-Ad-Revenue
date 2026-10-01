package mir.oslav.adrevenue.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import mir.oslav.adrevenue.data.auth.AdMobAuthorizationResult
import mir.oslav.adrevenue.data.auth.GoogleAuthClient
import mir.oslav.adrevenue.ui.auth.AuthViewModel
import mir.oslav.adrevenue.ui.home.HomeViewModel
import mir.oslav.adrevenue.ui.stats.StatsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var googleAuthClient: GoogleAuthClient

    private val authViewModel: AuthViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val statsViewModel: StatsViewModel by viewModels()

    private val signInAuthorizationLauncher = registerForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        handleAuthorizationActivityResult(
            resultCode = result.resultCode,
            data = result.data,
            target = AuthorizationTarget.SignIn,
        )
    }

    private val refreshAuthorizationLauncher = registerForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        handleAuthorizationActivityResult(
            resultCode = result.resultCode,
            data = result.data,
            target = AuthorizationTarget.Refresh,
        )
    }

    private val statisticsAuthorizationLauncher = registerForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        handleAuthorizationActivityResult(
            resultCode = result.resultCode,
            data = result.data,
            target = AuthorizationTarget.Statistics,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdRevenueTheme {
                MainScreen(
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,
                    statsViewModel = statsViewModel,
                    onSignInClick = ::startSignIn,
                    onUseAnotherGoogleAccountClick = ::useAnotherGoogleAccount,
                    onRefreshClick = ::requestRefreshAuthorization,
                    onStatisticsAuthorizationRequested = ::requestStatisticsAuthorization,
                )
            }
        }
    }

    private fun startSignIn() {
        authViewModel.signInStarted()
        lifecycleScope.launch {
            runCatching {
                googleAuthClient.signIn(
                    activity = this@MainActivity,
                    preferAuthorizedAccounts = false,
                )
            }.onSuccess { signedInUser ->
                authViewModel.googleAccountSelected(signedInUser = signedInUser)
                requestAdMobAuthorization(target = AuthorizationTarget.SignIn)
            }.onFailure { throwable ->
                if (throwable.isUserCancellation()) {
                    authViewModel.signInCanceled()
                } else {
                    authViewModel.signInFailed(throwable = throwable)
                }
            }
        }
    }

    private fun requestRefreshAuthorization() {
        homeViewModel.authorizationStartedForRefresh()
        lifecycleScope.launch {
            requestAdMobAuthorization(target = AuthorizationTarget.Refresh)
        }
    }

    private fun requestStatisticsAuthorization() {
        statsViewModel.authorizationStarted()
        lifecycleScope.launch {
            requestAdMobAuthorization(target = AuthorizationTarget.Statistics)
        }
    }

    private suspend fun requestAdMobAuthorization(target: AuthorizationTarget) {
        runCatching {
            googleAuthClient.requestAdMobAuthorization(this@MainActivity)
        }.onSuccess { authorizationResult ->
            when (authorizationResult) {
                is AdMobAuthorizationResult.AccessToken -> {
                    handleAccessToken(
                        accessToken = authorizationResult.value,
                        target = target,
                    )
                }

                is AdMobAuthorizationResult.ResolutionRequired -> {
                    val request = IntentSenderRequest.Builder(
                        authorizationResult.pendingIntent.intentSender,
                    ).build()
                    when (target) {
                        AuthorizationTarget.SignIn -> signInAuthorizationLauncher.launch( input=request)
                        AuthorizationTarget.Refresh -> refreshAuthorizationLauncher.launch( input=request)
                        AuthorizationTarget.Statistics -> statisticsAuthorizationLauncher.launch(
                         input=    request
                        )
                    }
                }
            }
        }.onFailure { throwable ->
            dispatchAuthorizationFailure(
                throwable = throwable,
                target = target,
            )
        }
    }

    private fun handleAuthorizationActivityResult(
        resultCode: Int,
        data: Intent?,
        target: AuthorizationTarget,
    ) {
        if (resultCode != RESULT_OK || data == null) {
            dispatchAuthorizationCancellation(target = target)
            return
        }

        runCatching {
            googleAuthClient.readAuthorizationResult(data)
        }.onSuccess { token ->
            handleAccessToken(
                accessToken = token,
                target = target,
            )
        }.onFailure { throwable ->
            dispatchAuthorizationFailure(
                throwable = throwable,
                target = target,
            )
        }
    }

    private fun handleAccessToken(
        accessToken: String,
        target: AuthorizationTarget,
    ) {
        when (target) {
            AuthorizationTarget.SignIn -> authViewModel.accessTokenAvailable(token = accessToken)
            AuthorizationTarget.Refresh -> homeViewModel.accessTokenAvailable(token = accessToken)
            AuthorizationTarget.Statistics -> statsViewModel.accessTokenAvailable(token = accessToken)
        }
    }

    private fun dispatchAuthorizationFailure(
        throwable: Throwable,
        target: AuthorizationTarget,
    ) {
        if (throwable.isUserCancellation()) {
            dispatchAuthorizationCancellation(target = target)
            return
        }

        when (target) {
            AuthorizationTarget.SignIn -> authViewModel.authorizationFailed(throwable = throwable)
            AuthorizationTarget.Refresh -> homeViewModel.authorizationFailed(throwable = throwable)
            AuthorizationTarget.Statistics -> statsViewModel.authorizationFailed(throwable = throwable)
        }
    }

    private fun dispatchAuthorizationCancellation(target: AuthorizationTarget) {
        when (target) {
            AuthorizationTarget.SignIn -> authViewModel.signInCanceled()
            AuthorizationTarget.Refresh -> homeViewModel.authorizationFailed(
                IllegalStateException("AdMob authorization was canceled."),
            )

            AuthorizationTarget.Statistics -> statsViewModel.authorizationFailed(
                IllegalStateException("AdMob authorization was canceled."),
            )
        }
    }

    private fun useAnotherGoogleAccount() {
        lifecycleScope.launch {
            runCatching { googleAuthClient.signOut() }
            runCatching { authViewModel.resetForAnotherGoogleAccount() }
            startSignIn()
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            runCatching {
                googleAuthClient.signOut()
            }
            homeViewModel.signOutFinished()
        }
    }
}

private enum class AuthorizationTarget {
    SignIn,
    Refresh,
    Statistics,
}

private fun Throwable.isUserCancellation(): Boolean =
    this is CancellationException ||
            this is GetCredentialCancellationException
