package org.unicornradio.app.annuaire

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.modele.Station
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder

/**
 * L'annuaire ouvert Radio Browser, communautaire, sans clef, en JSON. Ce
 * qu'il demande, et qu'on fait : un `User-Agent` qui nomme l'application ;
 * un serveur choisi par le DNS de `all.api.radio-browser.info` plutôt
 * qu'écrit en dur, chaque serveur essayé à son tour ; et signaler une écoute
 * par `/json/url/<uuid>`, qui compte le clic. Les identifiants de station
 * sont ceux du fichier d'échange. Tout ici bloque : à appeler hors du fil
 * principal.
 */
object Annuaire {

    class Pays(val code: String, val nom: String, val nombre: Int)
    class Etiquette(val nom: String, val nombre: Int)
    class AnnuaireInjoignable(message: String) : Exception(message)

    private const val USER_AGENT = "Unicorn-Radio/${BuildConfig.VERSION_NAME} (https://github.com/Lestat2Lioncourt/unicorn-radio)"
    private const val TOUS_LES_SERVEURS = "all.api.radio-browser.info"
    private const val SERVEUR_DE_SECOURS = "de1.api.radio-browser.info"
    private const val DELAI_MS = 20_000
    private const val LIMITE = 200

    private var serveurs: List<String>? = null
    private var serveursLe = 0L

    /** Les serveurs de l'annuaire, par le DNS, retenus une heure. */
    @Synchronized
    private fun lesServeurs(): List<String> {
        serveurs?.let { if (System.currentTimeMillis() - serveursLe < 3_600_000) return it }
        val noms = LinkedHashSet<String>()
        try {
            for (adresse in InetAddress.getAllByName(TOUS_LES_SERVEURS)) {
                // Sans entrée inverse, Java rend l'adresse elle-même, que le certificat https ne couvre pas : écartée.
                val nom = adresse.canonicalHostName
                if (nom.isNotBlank() && nom != adresse.hostAddress) noms += nom
            }
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "annuaire : pas de DNS (${e.message}), le secours")
        }
        // Après les serveurs nommés, le nom collectif lui-même, que le certificat couvre, puis le secours.
        noms += TOUS_LES_SERVEURS
        noms += SERVEUR_DE_SECOURS
        val liste = noms.toList()
        serveurs = liste
        serveursLe = System.currentTimeMillis()
        Log.i(Fichiers.JOURNAL, "annuaire : serveurs $liste")
        return liste
    }

    /** Une demande JSON à l'annuaire, en essayant chaque serveur à son tour. */
    private fun demander(chemin: String, params: Map<String, String> = emptyMap()): String {
        val requete = params.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        var derniere: Exception? = null
        for (serveur in lesServeurs()) {
            val url = "https://$serveur$chemin" + if (requete.isEmpty()) "" else "?$requete"
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = DELAI_MS
                c.readTimeout = DELAI_MS
                c.setRequestProperty("User-Agent", USER_AGENT)
                c.setRequestProperty("Accept", "application/json")
                try {
                    if (c.responseCode != 200) throw Exception("HTTP ${c.responseCode}")
                    return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally {
                    c.disconnect()
                }
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "annuaire : $serveur$chemin : ${e.message}")
                derniere = e
            }
        }
        throw AnnuaireInjoignable(derniere?.message ?: "aucun serveur")
    }

    /**
     * La recherche : par nom, pays et étiquette, chacun facultatif, triée par
     * votes, les stations cassées au dernier contrôle exclues.
     */
    fun chercher(nom: String = "", paysCode: String? = null, etiquette: String? = null): List<Station> {
        val params = LinkedHashMap<String, String>()
        if (nom.isNotBlank()) params["name"] = nom.trim()
        if (!paysCode.isNullOrBlank()) params["countrycode"] = paysCode
        if (!etiquette.isNullOrBlank()) {
            params["tag"] = etiquette
            params["tagExact"] = "true"
        }
        params["hidebroken"] = "true"
        params["order"] = "votes"
        params["reverse"] = "true"
        params["limit"] = LIMITE.toString()
        val tableau = JSONArray(demander("/json/stations/search", params))
        val liste = ArrayList<Station>(tableau.length())
        for (i in 0 until tableau.length()) {
            val o = tableau.optJSONObject(i) ?: continue
            liste += station(o, i) ?: continue
        }
        return liste
    }

    /** Les pays et leur nombre de stations, du plus fourni au moins. */
    fun pays(): List<Pays> {
        val tableau = JSONArray(demander("/json/countries", mapOf("hidebroken" to "true")))
        val liste = ArrayList<Pays>()
        for (i in 0 until tableau.length()) {
            val o = tableau.optJSONObject(i) ?: continue
            val code = o.optString("iso_3166_1").uppercase()
            val nom = o.optString("name")
            if (code.length != 2 || nom.isBlank()) continue
            liste += Pays(code, nom, o.optInt("stationcount"))
        }
        return liste.sortedByDescending { it.nombre }
    }

    /** Les étiquettes les plus portées. */
    fun etiquettes(limite: Int = 80): List<Etiquette> {
        val tableau = JSONArray(
            demander("/json/tags", mapOf("order" to "stationcount", "reverse" to "true", "hidebroken" to "true", "limit" to limite.toString())),
        )
        val liste = ArrayList<Etiquette>()
        for (i in 0 until tableau.length()) {
            val o = tableau.optJSONObject(i) ?: continue
            val nom = o.optString("name").trim()
            if (nom.isEmpty()) continue
            liste += Etiquette(nom, o.optInt("stationcount"))
        }
        return liste
    }

    /**
     * Signaler une écoute : l'annuaire compte le clic et rend l'adresse du flux
     * à jour, ou `null` s'il ne la donne pas. Rien n'est levé : une écoute
     * non comptée n'empêche pas d'écouter.
     */
    fun signalerEcoute(uuid: String): String? = try {
        val r = JSONObject(demander("/json/url/" + URLEncoder.encode(uuid, "UTF-8")))
        if (r.optString("ok") == "true") r.optString("url").takeIf { it.isNotBlank() } else null
    } catch (e: Exception) {
        Log.w(Fichiers.JOURNAL, "annuaire : écoute non comptée pour $uuid : ${e.message}")
        null
    }

    /** Une fiche de l'annuaire devient une station, avec les champs du fichier d'échange. */
    private fun station(o: JSONObject, ordre: Int): Station? {
        val uuid = o.optString("stationuuid").takeIf { it.isNotBlank() } ?: return null
        val url = o.optString("url_resolved").takeIf { it.isNotBlank() } ?: o.optString("url").takeIf { it.isNotBlank() } ?: return null
        return Station(
            uuid = uuid,
            nom = o.optString("name").trim().ifEmpty { uuid },
            url = url,
            paysCode = o.optString("countrycode").takeIf { it.isNotBlank() } ?: "??",
            pays = o.optString("country").takeIf { it.isNotBlank() },
            page = o.optString("homepage").takeIf { it.isNotBlank() },
            logo = o.optString("favicon").takeIf { it.isNotBlank() },
            etiquettes = o.optString("tags").takeIf { it.isNotBlank() },
            langue = o.optString("language").takeIf { it.isNotBlank() },
            codec = o.optString("codec").takeIf { it.isNotBlank() },
            debit = o.optInt("bitrate").takeIf { it > 0 },
            votes = o.optInt("votes"),
            hls = o.optInt("hls") == 1,
            ordre = ordre,
        )
    }
}
