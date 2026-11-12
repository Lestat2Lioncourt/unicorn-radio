package org.unicornradio.app.serveur

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import org.unicornradio.app.fichier.Fichiers

/**
 * Le signal d'Android quand le téléphone change de réseau : un autre wifi,
 * les données mobiles, la box qui redémarre. À ce signal, l'adresse gardée
 * du serveur est à retrouver (`Serveur.aRetrouver`) : la prochaine demande
 * refait la recherche sans attendre un échec. L'écran et le service de
 * lecture s'abonnent tous deux ; un seul abonnement au système.
 *
 * Le retour du réseau, quand Android l'a vérifié (`VALIDATED`), est aussi
 * dit à `surRetour`, sur le fil du système : le service de lecture y
 * relance une lecture tombée sur la coupure.
 */
object Reseau {

    private var gestionnaire: ConnectivityManager? = null
    private var abonnes = 0
    /** L'abonnement signale d'abord le réseau courant : celui-là n'est pas un changement. */
    private var premier = true
    /** Le réseau par défaut est vérifié : Internet répond. */
    @Volatile private var valide = false

    /** Appelé quand un réseau vérifié revient, ou est là à l'abonnement ; hors du fil principal. */
    @Volatile var surRetour: (() -> Unit)? = null

    /** Vrai si le téléphone a en ce moment un réseau vérifié ; dans le doute, vrai. */
    fun enLigne(contexte: Context): Boolean = try {
        val g = contexte.applicationContext.getSystemService(ConnectivityManager::class.java)
        val capacites = g.getNetworkCapabilities(g.activeNetwork)
        capacites?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    } catch (e: Exception) {
        true
    }

    private val rappel = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capacites: NetworkCapabilities) {
            val v = capacites.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (v && !valide) {
                valide = true
                surRetour?.invoke()
            } else if (!v) {
                valide = false
            }
        }

        override fun onAvailable(network: Network) {
            if (premier) {
                premier = false
                return
            }
            Log.i(Fichiers.JOURNAL, "réseau : un autre réseau, le serveur sera recherché")
            Serveur.aRetrouver = true
        }

        override fun onLost(network: Network) {
            valide = false
            Serveur.aRetrouver = true
        }
    }

    @Synchronized
    fun surveiller(contexte: Context) {
        if (abonnes++ > 0) return
        try {
            val g = contexte.applicationContext.getSystemService(ConnectivityManager::class.java)
            premier = true
            valide = false
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
