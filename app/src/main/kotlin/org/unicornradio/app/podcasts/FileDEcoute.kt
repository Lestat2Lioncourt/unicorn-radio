package org.unicornradio.app.podcasts

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.fichier.Fichiers
import java.io.File

/**
 * La liste de lecture des podcasts, gardée sur le téléphone dans
 * `file-d-ecoute.json` : les épisodes passés, du plus ancien au plus
 * récent, vingt au plus, et ceux à suivre, dans l'ordre où ils joueront.
 * L'épisode en cours est celui du lecteur, pas d'ici. Le service note
 * chaque départ, l'écran ajoute et retire. Le fichier est relu à chaque
 * appel, il est petit, et le service comme l'écran en ont une instance.
 */
class FileDEcoute(contexte: Context) {

    private val fichier = File(contexte.filesDir, "file-d-ecoute.json")

    fun passes(): List<Episode> = synchronized(VERROU) { lire().first }

    fun aSuivre(): List<Episode> = synchronized(VERROU) { lire().second }

    /** Les clefs des épisodes à suivre, pour marquer les lignes. */
    fun clefsASuivre(): Set<String> = aSuivre().map { it.clef }.toSet()

    /** Combien la liste compte, passés et à suivre. */
    val nombre: Int
        get() = synchronized(VERROU) { lire().let { it.first.size + it.second.size } }

    /** À la fin de ceux à suivre ; déjà là, rien, et `false`. */
    fun ajouter(e: Episode): Boolean = synchronized(VERROU) {
        val (passes, suite) = lire()
        if (suite.any { it.clef == e.clef }) return false
        ecrire(passes, suite + e)
        true
    }

    /** Retiré d'où qu'il soit, passés ou à suivre. */
    fun retirer(clef: String) = synchronized(VERROU) {
        val (passes, suite) = lire()
        ecrire(passes.filter { it.clef != clef }, suite.filter { it.clef != clef })
    }

    /** Un épisode démarre : il sort de ceux à suivre et passe en fin des passés, une seule fois. */
    fun noterLeDepart(e: Episode) = synchronized(VERROU) {
        val (passes, suite) = lire()
        val nouveaux = (passes.filter { it.clef != e.clef } + e).takeLast(PASSES_MAX)
        ecrire(nouveaux, suite.filter { it.clef != e.clef })
    }

    fun vider() = synchronized(VERROU) { ecrire(emptyList(), emptyList()) }

    private fun lire(): Pair<List<Episode>, List<Episode>> {
        if (!fichier.exists()) return emptyList<Episode>() to emptyList()
        return try {
            val o = JSONObject(fichier.readText())
            liste(o.optJSONArray("passes")) to liste(o.optJSONArray("a_suivre"))
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "file d'écoute illisible, reprise à vide : ${e.message}")
            emptyList<Episode>() to emptyList()
        }
    }

    private fun liste(tableau: JSONArray?): List<Episode> {
        if (tableau == null) return emptyList()
        return (0 until tableau.length()).mapNotNull { i -> tableau.optJSONObject(i)?.let { Episode.depuisJson(it) } }
    }

    private fun ecrire(passes: List<Episode>, suite: List<Episode>) {
        val o = JSONObject()
            .put("passes", JSONArray().apply { passes.forEach { put(it.versJson()) } })
            .put("a_suivre", JSONArray().apply { suite.forEach { put(it.versJson()) } })
        fichier.writeText(o.toString())
    }

    companion object {
        const val PASSES_MAX = 20
        private val VERROU = Any()
    }
}
