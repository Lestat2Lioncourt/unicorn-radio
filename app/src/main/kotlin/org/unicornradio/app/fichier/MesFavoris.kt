package org.unicornradio.app.fichier

import android.content.Context
import android.util.Log
import org.unicornradio.app.modele.Station
import java.io.File

/**
 * Les favoris choisis sur le téléphone, à part des fichiers importés qu'on
 * ne modifie pas : un fichier du format d'échange dans le stockage privé,
 * relu au démarrage, réécrit à chaque étoile. C'est lui que le palier 3
 * saura rendre.
 */
class MesFavoris(private val contexte: Context) {

    private val fichier: File
        get() = File(contexte.filesDir, "mes-favoris.json")

    private var liste: MutableList<Station>? = null

    @Synchronized
    fun charger(): List<Station> {
        liste?.let { return it.toList() }
        val lu = try {
            if (fichier.exists()) LectureFichier.lire(fichier.readText(), fichier.name).stations else emptyList()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "mes favoris illisibles : ${e.message}")
            emptyList()
        }
        liste = lu.toMutableList()
        return lu
    }

    @Synchronized
    fun contient(uuid: String): Boolean = charger().any { it.uuid == uuid }

    /** Ajoute la station si elle manque, la retire sinon ; rend `true` si elle est maintenant favorite. */
    @Synchronized
    fun basculer(station: Station): Boolean {
        charger()
        val l = liste!!
        val presente = l.any { it.uuid == station.uuid }
        if (presente) l.removeAll { it.uuid == station.uuid } else l += station
        fichier.writeText(EcritureFichier.fichier(l))
        Log.i(Fichiers.JOURNAL, "mes favoris : « ${station.nom} » ${if (presente) "retirée" else "ajoutée"}, ${l.size} au total")
        return !presente
    }
}
