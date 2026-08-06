package com.kobe.warehouse.inventory.utils

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Gestion globale de la session.
 *
 * Aligné sur `sales-android/utils/SessionManager` : événements diffusés par
 * `SharedFlow` (le `LocalBroadcastManager` précédent est déprécié).
 *
 * **Une différence assumée avec le module de vente** : une perte de connexion ne
 * déconnecte pas et n'efface pas les jetons. Le comptage d'inventaire fonctionne hors
 * ligne (saisies en base locale, synchronisation différée) ; déconnecter l'opérateur
 * parce que le Wi-Fi a lâché en réserve lui ferait perdre l'accès à son travail en
 * cours. L'écran de comptage signale l'état hors ligne par un bandeau.
 */
class SessionManager(context: Context) {

    companion object {
        /** Intervalle minimal entre deux signalements de perte de connexion */
        private const val CONNECTION_LOST_THROTTLE_MS = 30_000L

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    enum class SessionEvent {
        SESSION_EXPIRED,
        UNAUTHORIZED,
        CONNECTION_LOST
    }

    /**
     * `context` est un simple paramètre de constructeur, **pas une propriété** :
     * l'instance étant retenue dans un champ statique ([INSTANCE]), conserver une
     * référence au Context provoquerait une fuite mémoire (lint `StaticFieldLeak`).
     * Il n'est de toute façon utile qu'ici.
     */
    private val tokenManager = TokenManager(context)
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    fun handleSessionExpired(reason: String = "Session expirée") {
        clearSession()
        _sessionEvents.tryEmit(SessionEvent.SESSION_EXPIRED)
    }

    fun handleUnauthorized(reason: String = "Accès non autorisé") {
        clearSession()
        _sessionEvents.tryEmit(SessionEvent.UNAUTHORIZED)
    }

    /**
     * Perte de connexion : signalée mais **sans effacer la session** — voir la note de
     * classe. L'utilisateur reste connecté et continue de compter hors ligne.
     *
     * L'événement est émis au plus une fois par [CONNECTION_LOST_THROTTLE_MS] : hors
     * ligne, chaque requête échoue (liste, progression, sauvegardes, synchronisation
     * périodique), et une notification par échec noyait l'écran sous les messages
     * sans rien apprendre de plus à l'opérateur.
     */
    fun handleConnectionLost(reason: String = "Connexion perdue") {
        val now = SystemClock.elapsedRealtime()
        if (now - lastConnectionLostAt < CONNECTION_LOST_THROTTLE_MS) return
        lastConnectionLostAt = now
        _sessionEvents.tryEmit(SessionEvent.CONNECTION_LOST)
    }

    @Volatile
    private var lastConnectionLostAt = 0L

    private fun clearSession() {
        tokenManager.clearTokens()
    }
}
