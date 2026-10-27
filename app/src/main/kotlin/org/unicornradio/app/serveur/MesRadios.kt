package org.unicornradio.app.serveur

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.fichier.EcritureFichier
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
import java.io.File

/**
 * La copie locale de la liste du serveur : la section seule du format
 * d'échange, dans `mes-radios.json`, remplacée à chaque tirage, relue au
 * démarrage. Le serveur en est le seul maître ; ce qu'on change ici est
 * appliqué tout de suite pour que l'écran suive, et noté dans le journal
 * des gestes, qui part au serveur avant le tirage suivant.
 */
class MesRadios(private val contexte: Context) {

    private val fichier: File
        get() = File(contexte.filesDir, "mes-radios.json")

    private var racine: Noeud? = null
    private var favorisUuid: Set<String>? = null

    val existe: Boolean
        get() = fichier.exists()

    /** La racine de la copie, favoris en stations et dossiers en sous-dossiers ; `null` sans copie ou illisible. */
    @Synchronized
    fun charger(): Noeud? {
        racine?.let { return it }
        val lu = try {
            if (fichier.exists()) LectureFichier.lire(fichier.readText(), fichier.name) else null
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "copie du serveur illisible : ${e.message}")
            null
        }
        racine = lu
        favorisUuid = lu?.stations?.map { it.uuid }?.toSet() ?: emptySet()
        return lu
    }

    @Synchronized
    fun estFavorite(uuid: String): Boolean {
        charger()
        return favorisUuid?.contains(uuid) == true
    }

    /** Ce que le serveur vient de rendre, vérifié lisible avant d'écraser l'ancienne copie. Rend sa racine. */
    @Synchronized
    fun remplacer(texte: String): Noeud {
        val lu = LectureFichier.lire(texte, fichier.name)
        fichier.writeText(texte)
        oublier()
        Log.i(Fichiers.JOURNAL, "copie du serveur : ${lu.stations.size} favoris, ${lu.nombreDeStations} stations")
        return lu
    }

    /** Appliqué sur place, comme le serveur le fera : à la fin des favoris, rien si déjà là. */
    @Synchronized
    fun ajouterFavori(station: Station) {
        modifier { favoris ->
            for (i in 0 until favoris.length()) if (favoris.optJSONObject(i)?.optString("station_uuid") == station.uuid) return@modifier
            favoris.put(EcritureFichier.fiche(station, favoris.length()))
        }
    }

    /** Appliqué sur place : retiré des favoris, rien si absent. */
    @Synchronized
    fun retirerFavori(uuid: String) {
        modifier { favoris ->
            for (i in favoris.length() - 1 downTo 0) {
                if (favoris.optJSONObject(i)?.optString("station_uuid") == uuid) favoris.remove(i)
            }
        }
    }

    @Synchronized
    fun effacer() {
        fichier.delete()
        oublier()
    }

    private fun oublier() {
        racine = null
        favorisUuid = null
    }

    /** La section telle que le serveur l'a rendue, ses favoris modifiés, réécrite ; sans copie, rien. */
    private fun modifier(action: (JSONArray) -> Unit) {
        if (!fichier.exists()) return
        try {
            val section = JSONObject(fichier.readText())
            val favoris = section.optJSONArray("favoris") ?: JSONArray().also { section.put("favoris", it) }
            action(favoris)
            fichier.writeText(section.toString(2))
            oublier()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "copie du serveur : modification impossible : ${e.message}")
        }
    }
}
