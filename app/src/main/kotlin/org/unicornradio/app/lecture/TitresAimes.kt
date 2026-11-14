package org.unicornradio.app.lecture

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.fichier.Fichiers
import java.io.File

/**
 * Les titres aimés : un morceau entendu à la radio, gardé d'un appui sur le
 * cœur, avec la station et l'heure, le plus récent en tête, dans
 * `titres-aimes.json`. Sur le téléphone seulement : ni dans le fichier
 * d'export, ni au serveur.
 */
object TitresAimes {

    class Titre(val titre: String, val station: String, val uuid: String, val le: Long)

    private val verrou = Any()

    /** Aimé, sur cette station ? Le même titre sur une autre station est un autre souvenir. */
    fun estAime(contexte: Context, titre: String, uuid: String): Boolean = synchronized(verrou) {
        tous(contexte).any { it.titre == titre && it.uuid == uuid }
    }

    /** Aimer ou ne plus aimer ; rend `true` s'il est aimé après. */
    fun basculer(contexte: Context, titre: String, station: String, uuid: String): Boolean = synchronized(verrou) {
        val liste = tous(contexte).toMutableList()
        val avant = liste.size
        liste.removeAll { it.titre == titre && it.uuid == uuid }
        val aime = liste.size == avant
        if (aime) liste.add(0, Titre(titre, station, uuid, System.currentTimeMillis()))
        ecrire(contexte, liste)
        aime
    }

    fun retirer(contexte: Context, t: Titre): Unit = synchronized(verrou) {
        ecrire(contexte, tous(contexte).filterNot { it.titre == t.titre && it.uuid == t.uuid && it.le == t.le })
    }

    /** Tous, le plus récent en tête. */
    fun tous(contexte: Context): List<Titre> = synchronized(verrou) {
        val f = fichier(contexte)
        val tableau = try {
            if (f.exists()) JSONArray(f.readText()) else JSONArray()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "titres aimés illisibles : ${e.message}")
            JSONArray()
        }
        (0 until tableau.length()).mapNotNull { i ->
            val o = tableau.optJSONObject(i) ?: return@mapNotNull null
            val titre = o.optString("titre").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Titre(titre, o.optString("station"), o.optString("uuid"), o.optLong("le"))
        }
    }

    private fun ecrire(contexte: Context, liste: List<Titre>) {
        val tableau = JSONArray()
        for (t in liste) tableau.put(JSONObject().put("titre", t.titre).put("station", t.station).put("uuid", t.uuid).put("le", t.le))
        try {
            fichier(contexte).writeText(tableau.toString())
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "titres aimés : pas écrits : ${e.message}")
        }
    }

    private fun fichier(contexte: Context) = File(contexte.filesDir, "titres-aimes.json")
}
