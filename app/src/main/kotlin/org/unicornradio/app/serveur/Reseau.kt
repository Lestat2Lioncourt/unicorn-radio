package org.unicornradio.app.serveur

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import org.unicornradio.app.fichier.Fichiers

/**
 * Le signal d'Android quand le téléphone change de réseau : un autre wifi,
 * les données mobiles, la box qui redémarre. À ce signal, l'adresse gardée
 * du serveur est à retrouver (`Serveur.aRetrouver`) : la prochaine demande
 * refait la recherche sans attendre un échec. L'écran et le service de
 * lecture s'abonnent tous deux ; un seul abonnement au système.
 */
object Reseau {

    private var gestionnaire: ConnectivityManager? = null
    private var abonnes = 0
    /** L'abonnement signale d'abord le réseau courant : celui-là n'est pas un changement. */
    private var premier = true

    private val rappel = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (premier) {
                premier = false
                return
            }
            Log.i(Fichiers.JOURNAL, "réseau : un autre réseau, le serveur sera recherché")
            Serveur.aRetrouver = true
        }

        override fun onLost(network: Network) {
            Serveur.aRetrouver = true
        }
    }

    @Synchronized
    fun surveiller(contexte: Context) {
        if (abonnes++ > 0) return
        try {
            val g = contexte.applicationContext.getSystemService(ConnectivityManager::class.java)
            premier = true
            g.registerDefaultNetworkCallback(rappel)
            gestionnaire = g
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "réseau : pas de signal de changement : ${e.message}")
        }
    }

    @Synchronized
    fun laisser() {
        if (abonnes == 0 || --abonnes > 0) return
        try {
            gestionnaire?.unregisterNetworkCallback(rappel)
        } catch (e: Exception) {
            // Déjà désabonné : rien à faire.
        }
        gestionnaire = null
    }
}
