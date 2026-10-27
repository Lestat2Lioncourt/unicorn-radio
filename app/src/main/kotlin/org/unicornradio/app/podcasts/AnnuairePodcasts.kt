package org.unicornradio.app.podcasts

import org.json.JSONObject
import org.unicornradio.app.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * L'annuaire d'Apple Podcasts, par nom, comme sur le bureau : il ne sert
 * qu'à trouver l'adresse du flux RSS, qui fait foi ensuite. Sans clef, en
 * JSON. Bloque : à appeler hors du fil principal.
 */
object AnnuairePodcasts {

    class AnnuaireInjoignable(message: String) : Exception(message)

    private const val USER_AGENT = "Unicorn-Radio/${BuildConfig.VERSION_NAME}"
    private const val DELAI_MS = 15_000

    fun chercher(texte: String): List<Podcast> {
        val url = "https://itunes.apple.com/search?media=podcast&limit=50&term=" + URLEncoder.encode(texte.trim(), "UTF-8")
        val reponse = try {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = DELAI_MS
                c.readTimeout = DELAI_MS
                c.setRequestProperty("User-Agent", USER_AGENT)
                c.setRequestProperty("Accept", "application/json")
                if (c.responseCode != 200) throw AnnuaireInjoignable("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                c.disconnect()
            }
        } catch (e: IOException) {
            throw AnnuaireInjoignable(e.message ?: e.javaClass.simpleName)
        }
        val resultats = JSONObject(reponse).optJSONArray("results") ?: return emptyList()
        val liste = ArrayList<Podcast>()
        for (i in 0 until resultats.length()) {
            val o = resultats.optJSONObject(i) ?: continue
            val flux = o.optString("feedUrl").trim()
            if (flux.isEmpty()) continue
            liste += Podcast(
                flux = flux,
                nom = o.optString("collectionName").ifEmpty { o.optString("trackName") }.ifEmpty { flux },
                auteur = o.optString("artistName").takeIf { it.isNotBlank() },
                image = o.optString("artworkUrl600").ifEmpty { o.optString("artworkUrl100") }.takeIf { it.isNotBlank() },
                nbEpisodes = if (o.has("trackCount")) o.optInt("trackCount") else null,
            )
        }
        return liste
    }
}
