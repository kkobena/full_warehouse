package com.kobe.warehouse.inventory.utils

import android.util.Log
import com.kobe.warehouse.inventory.data.api.AuthApiService
import com.kobe.warehouse.inventory.data.model.auth.RefreshTokenRequest
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Renouvelle silencieusement l'access token quand le serveur répond 401, puis rejoue
 * la requête d'origine.
 *
 * <p>Sans cela, l'expiration du token (8 h) coupe l'opérateur en plein comptage. La
 * saisie n'est pas perdue — elle est en base locale (offline-first) — mais l'opérateur
 * doit se reconnecter manuellement.
 *
 * <p>Le client HTTP utilisé pour le refresh est volontairement **distinct** de celui
 * qui porte cet Authenticator : sinon un 401 sur l'appel de refresh relancerait
 * l'Authenticator en boucle.
 */
class TokenRefreshAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String
) : Authenticator {

    companion object {
        private const val TAG = "TokenRefreshAuth"
        private const val MAX_RETRY = 1
    }

    private val refreshApi: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Une seule tentative : si la requête rejouée échoue encore, la session est morte
        if (responseCount(response) > MAX_RETRY) {
            Log.w(TAG, "Refresh déjà tenté pour cette requête, abandon")
            return null
        }

        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return null
        }

        // Deux requêtes peuvent recevoir 401 en parallèle : la première rafraîchit,
        // les suivantes réutilisent le token déjà renouvelé.
        synchronized(this) {
            val currentHeader = tokenManager.getAuthorizationHeader()
            val staleHeader = response.request.header("Authorization")
            if (currentHeader != null && currentHeader != staleHeader) {
                Log.d(TAG, "Token déjà renouvelé par une autre requête")
                return response.request.newBuilder()
                    .header("Authorization", currentHeader)
                    .build()
            }

            return try {
                val refreshResponse = refreshApi
                    .refreshTokenSync(RefreshTokenRequest(refreshToken))
                    .execute()

                val body = refreshResponse.body()
                if (refreshResponse.isSuccessful && body != null) {
                    tokenManager.storeTokens(body)
                    Log.d(TAG, "Access token renouvelé, requête rejouée")
                    response.request.newBuilder()
                        .header("Authorization", "${body.tokenType} ${body.accessToken}")
                        .build()
                } else {
                    // Refresh token expiré ou compte désactivé : session terminée
                    Log.w(TAG, "Refresh refusé (${refreshResponse.code()}), session expirée")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Refresh impossible (réseau) : ${e.message}")
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
