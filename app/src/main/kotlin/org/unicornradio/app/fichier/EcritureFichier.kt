package org.unicornradio.app.fichier

import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.modele.Station
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Écrit le fichier d'échange, selon `docs/format-d-echange.md` : la section
 * `radio` seule, avec `application` et `exportedAt` à côté d'elle pour
 * qu'on sache d'où vient le fichier.
 */
object EcritureFichier {

    /** La fiche d'une station, tous les champs présents, `null` quand inconnus, et sa place. */
    fun fiche(s: Station, ordre: Int): JSONObject = JSONObject().apply {
        put("station_uuid", s.uuid)
        put("pays_code", s.paysCode ?: "??")
        put("pays", s.pays ?: JSONObject.NULL)
        put("nom", s.nom)
        put("url", s.url)
        put("page", s.page ?: JSONObject.NULL)
        put("logo", s.logo ?: JSONObject.NULL)
        put("etiquettes", s.etiquettes ?: JSONObject.NULL)
        put("langue", s.langue ?: JSONObject.NULL)
        put("codec", s.codec ?: JSONObject.NULL)
        put("debit", s.debit ?: JSONObject.NULL)
        put("votes", s.votes ?: JSONObject.NULL)
        put("hls", if (s.hls) 1 else 0)
        put("ordre", ordre)
    }

    /** Le fichier entier, pour une liste de favoris ; les dossiers viendront avec le palier 3. */
    fun fichier(favoris: List<Station>, podcasts: JSONArray? = null): String {
        val horodatage = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val racine = JSONObject()
        racine.put("application", "unicorn-radio")
        racine.put("exportedAt", horodatage)
        racine.put("format", LectureFichier.FORMAT_CONNU)
        racine.put("favoris", JSONArray().apply { favoris.forEachIndexed { i, s -> put(fiche(s, i)) } })
        racine.put("dossiers", JSONArray())
        racine.put("podcasts", podcasts ?: JSONArray())
        return racine.toString(2)
    }
}
