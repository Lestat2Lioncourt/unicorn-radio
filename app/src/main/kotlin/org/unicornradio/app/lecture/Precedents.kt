package org.unicornradio.app.lecture

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.fichier.Fichiers
import java.io.File
import kotlin.math.min

/**
 * Les titres passés d'une station, « Précédemment » : ce que son flux a
 * annoncé pendant qu'on l'écoutait, du plus récent au plus ancien, vingt
 * par station, dans `precedents.json`. Le service note à chaque titre, et
 * l'écran montre ; apparié, ce que le serveur de la maison a vu de son côté
 * s'y ajoute au moment de montrer.
 */
object Precedents {

    class Titre(val titre: String, val le: Long)

    private const val MAX = 20
    private val verrou = Any()

    /** Le titre que la station annonce : en tête de ses précédents, s'il change. */
    fun noter(contexte: Context, uuid: String, titre: String): Unit = synchronized(verrou) {
        val f = fichier(contexte)
        val table = lire(f)
        val liste = table.optJSONArray(uuid) ?: JSONArray()
        if (liste.length() > 0 && liste.optJSONObject(0)?.optString("titre") == titre) return
        val nouvelle = JSONArray().put(JSONObject().put("titre", titre).put("le", System.currentTimeMillis()))
        for (i in 0 until min(liste.length(), MAX - 1)) nouvelle.put(liste.get(i))
        table.put(uuid, nouvelle)
        try {
            f.writeText(table.toString())
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "précédents : pas écrits : ${e.message}")
        }
    }

    /** Les titres passés d'une station, le plus récent en tête. */
    fun pour(contexte: Context, uuid: String): List<Titre> = synchronized(verrou) {
        val liste = lire(fichier(contexte)).optJSONArray(uuid) ?: return emptyList()
        (0 until liste.length()).mapNotNull { i ->
            val o = liste.optJSONObject(i) ?: return@mapNotNull null
            val t = o.optString("titre").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Titre(t, o.optLong("le"))
        }
    }

    /** Plusieurs sources réunies : le même titre une fois, à sa date la plus récente, les plus récents d'abord. */
    fun reunir(vararg listes: List<Titre>): List<Titre> {
        val parTitre = LinkedHashMap<String, Titre>()
        for (l in listes) for (t in l) {
            val connu = parTitre[t.titre]
            if (connu == null || t.le > connu.le) parTitre[t.titre] = t
        }
        return parTitre.values.sortedByDescending { it.le }.take(MAX)
    }

    private fun fichier(contexte: Context) = File(contexte.filesDir, "precedents.json")

    private fun lire(f: File): JSONObject = try {
        if (f.exists()) JSONObject(f.readText()) else JSONObject()
    } catch (e: Exception) {
        Log.w(Fichiers.JOURNAL, "précédents illisibles, repris à vide : ${e.message}")
        JSONObject()
    }
}
