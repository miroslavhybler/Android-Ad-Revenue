package mir.oslav.adrevenue.data.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import mir.oslav.adrevenue.BuildConfig
import mir.oslav.adrevenue.core.AdMobApiConstants
import mir.oslav.adrevenue.data.model.SignedInUser

@Singleton
class GoogleAuthClient @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    suspend fun signIn(
        activity: Activity,
        preferAuthorizedAccounts: Boolean,
    ): SignedInUser {
        val serverClientId = BuildConfig.ADMOB_WEB_CLIENT_ID
        if (serverClientId.isBlank()) {
            throw MissingWebClientIdException()
        }

        val request = if (preferAuthorizedAccounts) {
            GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setAutoSelectEnabled(true)
                        .setServerClientId(serverClientId)
                        .setNonce(generateSecureRandomNonce())
                        .build(),
                )
                .build()
        } else {
            GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(serverClientId)
                        .setNonce(generateSecureRandomNonce())
                        .build(),
                )
                .build()
        }

        val result = credentialManager.getCredential(
            context = MutableContextWrapper(activity),
            request = request,
        )
        val credential = result.credential
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return SignedInUser(
                id = googleCredential.id,
                displayName = googleCredential.displayName,
                profilePictureUri = googleCredential.profilePictureUri?.toString(),
            )
        }

        throw IllegalStateException("Google sign-in returned an unsupported credential.")
    }

    suspend fun requestAdMobAuthorization(activity: Activity): AdMobAuthorizationResult {
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(AdMobApiConstants.ReadonlyScope)))
            .build()

        val authorizationResult = Identity.getAuthorizationClient(activity)
            .authorize(authorizationRequest)
            .await()

        if (authorizationResult.hasResolution()) {
            val pendingIntent = authorizationResult.pendingIntent
                ?: throw IllegalStateException("AdMob authorization needs consent, but no prompt was returned.")
            return AdMobAuthorizationResult.ResolutionRequired(pendingIntent)
        }

        val accessToken = authorizationResult.accessToken
        if (accessToken.isNullOrBlank()) {
            throw IllegalStateException("AdMob authorization returned no access token.")
        }

        return AdMobAuthorizationResult.AccessToken(accessToken)
    }

    fun readAuthorizationResult(data: Intent): String {
        val authorizationResult = Identity.getAuthorizationClient(appContext)
            .getAuthorizationResultFromIntent(data)
        val accessToken = authorizationResult.accessToken
        if (accessToken.isNullOrBlank()) {
            throw IllegalStateException("AdMob authorization returned no access token.")
        }
        return accessToken
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    private fun generateSecureRandomNonce(byteLength: Int = 32): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(
            randomBytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
        )
    }
}

sealed interface AdMobAuthorizationResult {
    data class AccessToken(val value: String) : AdMobAuthorizationResult
    data class ResolutionRequired(val pendingIntent: PendingIntent) : AdMobAuthorizationResult
}

class MissingWebClientIdException : IllegalStateException(
    "Missing ADMOB_WEB_CLIENT_ID. Add it to local.properties before signing in.",
)
