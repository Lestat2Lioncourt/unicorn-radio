package org.unicornradio.app.podcasts

import android.util.Log
import android.util.Xml
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.fichier.Fichiers
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Un flux RSS de podcast, lu et analysé sur le téléphone : l'en-tête du
 * podcast et ses épisodes, du plus récent au plus ancien, chacun avec son
 * fichier audio. Un flux sans `<enclosure>` audio liste des articles, pas
 * des épisodes, et le dit. Bloque : à appeler hors du fil principal.
 */
object Flux {

    class FluxIllisible(message: String) : Exception(message)
    class Lu(val podcast: Podcast, val episodes: List<Episode>, val sansSon: Boolean)

    private const val USER_AGENT = "Unicorn-Radio/${BuildConfig.VERSION_NAME}"
    private const val DELAI_MS = 15_000
    private const val ITUNES = "http://www.itunes.com/dtds/podcast-1.0.dtd"
    private val EXTENSIONS_AUDIO = listOf(".mp3", ".m4a", ".aac", ".ogg", ".oga", ".opus", ".wav", ".flac")

    /** L'adresse du flux ; ce qu'on savait déjà du podcast sert quand le flux ne le dit pas. */
    fun lire(adresse: String, connu: Podcast? = null): Lu {
        val texte = telecharger(adresse)
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        try {
            p.setInput(StringReader(texte))
        } catch (e: Exception) {
            throw FluxIllisible("pas du XML : ${e.message}")
        }

        var titre: String? = null
        var auteur: String? = null
        var image: String? = null
        var dansItem = false
        var dansImage = false
        var items = 0
        var courant: Brouillon? = null
        val episodes = ArrayList<Episode>()
        try {
            var type = p.eventType
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    val n = p.name
                    val ns = p.namespace ?: ""
                    if (!dansItem && n == "item") {
                        dansItem = true
                        items++
                        courant = Brouillon()
                    } else if (dansItem) {
                        val b = courant!!
                        when {
                            n == "title" && b.titre == null -> b.titre = p.nextText().trim()
                            n == "guid" -> b.guid = p.nextText().trim()
                            n == "pubDate" -> b.date = p.nextText().trim()
                            n == "duration" && ns == ITUNES -> b.duree = p.nextText().trim()
                            n == "description" && b.description == null -> b.description = p.nextText()
                            n == "summary" && ns == ITUNES && b.description == null -> b.description = p.nextText()
                            n == "encoded" && b.description == null -> b.description = p.nextText()
                            n == "enclosure" -> {
                                val url = p.getAttributeValue(null, "url")?.trim()
                                val mime = p.getAttributeValue(null, "type")?.trim()
                                if (!url.isNullOrEmpty() && (b.url == null || estAudio(url, mime))) {
                                    if (b.url == null || estAudio(url, mime)) { b.url = url; b.type = mime }
                                }
                            }
                        }
                    } else {
                        when {
                            n == "image" && ns == ITUNES -> p.getAttributeValue(null, "href")?.let { if (image == null) image = it.trim() }
                            n == "image" && ns.isEmpty() -> dansImage = true
                            n == "url" && dansImage -> { val u = p.nextText().trim(); if (image == null && u.isNotEmpty()) image = u }
                            n == "title" && titre == null && !dansImage -> titre = p.nextText().trim()
                            n == "author" && ns == ITUNES && auteur == null -> auteur = p.nextText().trim()
                        }
                    }
                } else if (type == XmlPullParser.END_TAG) {
                    val n = p.name
                    if (dansItem && n == "item") {
                        dansItem = false
                        val b = courant!!
                        val url = b.url
                        if (url != null && estAudio(url, b.type)) {
                            episodes += Episode(
                                guid = b.guid?.takeIf { it.isNotEmpty() } ?: url,
                                titre = b.titre?.takeIf { it.isNotEmpty() } ?: url.substringAfterLast('/'),
                                date = dateDe(b.date),
                                duree = dureeDe(b.duree),
                                url = url,
                                type = b.type,
                                flux = adresse,
                                podcast = titre ?: connu?.nom ?: adresse,
                                image = image ?: connu?.image,
                                description = b.description?.let { texteNu(it) }?.takeIf { it.isNotEmpty() },
                            )
                        }
                        courant = null
                    } else if (!dansItem && n == "image" && (p.namespace ?: "").isEmpty()) {
                        dansImage = false
                    }
                }
                type = p.next()
            }
        } catch (e: FluxIllisible) {
            throw e
        } catch (e: Exception) {
            if (episodes.isEmpty()) throw FluxIllisible("XML cassé : ${e.message}")
            Log.w(Fichiers.JOURNAL, "flux $adresse : XML cassé après ${episodes.size} épisodes : ${e.message}")
        }

        // Le nom du podcast vient de l'en-tête, lu après coup pour les premiers épisodes si le flux le met tard.
        val nom = titre?.takeIf { it.isNotEmpty() } ?: connu?.nom ?: adresse
        val podcast = Podcast(adresse, nom, auteur?.takeIf { it.isNotEmpty() } ?: connu?.auteur, image ?: connu?.image, episodes.size)
        val tries = episodes.map { if (it.podcast == nom && it.image == podcast.image) it else it.copy(podcast = nom, image = podcast.image) }
            .withIndex()
            .sortedWith(compareByDescending<IndexedValue<Episode>> { it.value.date ?: Long.MIN_VALUE }.thenBy { it.index })
            .map { it.value }
        Log.i(Fichiers.JOURNAL, "flux « $nom » : ${tries.size} épisodes sur $items éléments")
        return Lu(podcast, tries, sansSon = items > 0 && tries.isEmpty())
    }

    private class Brouillon {
        var titre: String? = null
        var guid: String? = null
        var date: String? = null
        var duree: String? = null
        var url: String? = null
        var type: String? = null
        var description: String? = null
    }

    /** Le HTML d'un résumé ramené à du texte, pour la recherche. */
    private fun texteNu(html: String): String =
        android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ").trim()

    private fun estAudio(url: String, mime: String?): Boolean {
        if (mime != null) return mime.startsWith("audio/", ignoreCase = true)
        val chemin = url.substringBefore('?').lowercase(Locale.ROOT)
        return EXTENSIONS_AUDIO.any { chemin.endsWith(it) }
    }

    private val FORMATS_DE_DATE = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, d MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd",
    )

    private fun dateDe(texte: String?): Long? {
        if (texte.isNullOrBlank()) return null
        for (f in FORMATS_DE_DATE) {
            try {
                return SimpleDateFormat(f, Locale.US).parse(texte)?.time
            } catch (e: Exception) {
                // le format suivant
            }
        }
        return null
    }

    /** « 1:02:03 », « 62:03 », « 3723 », parfois avec des décimales. */
    private fun dureeDe(texte: String?): Long? {
        if (texte.isNullOrBlank()) return null
        val parts = texte.trim().split(':')
        return try {
            when (parts.size) {
                1 -> parts[0].toDouble().toLong()
                2 -> parts[0].toLong() * 60 + parts[1].toDouble().toLong()
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toDouble().toLong()
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Le flux, en suivant jusqu'à cinq redirections, http vers https compris. */
    private fun telecharger(adresse: String): String {
        var url = adresse
        repeat(6) {
            val c = try {
                URL(url).openConnection() as HttpURLConnection
            } catch (e: Exception) {
                throw FluxIllisible("adresse invalide : ${e.message}")
            }
            try {
                c.connectTimeout = DELAI_MS
                c.readTimeout = DELAI_MS
                c.instanceFollowRedirects = false
                c.setRequestProperty("User-Agent", USER_AGENT)
                c.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
                val code = c.responseCode
                if (code in 300..399) {
                    val vers = c.getHeaderField("Location") ?: throw FluxIllisible("HTTP $code sans destination")
                    url = URL(URL(url), vers).toString()
                    return@repeat
                }
                if (code != 200) throw FluxIllisible("HTTP $code")
                val charset = c.contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.takeIf { it.isNotEmpty() }
                return c.inputStream.bufferedReader(charset?.let { runCatching { charset(it) }.getOrNull() } ?: Charsets.UTF_8).use { it.readText() }
            } catch (e: IOException) {
                throw FluxIllisible(e.message ?: e.javaClass.simpleName)
            } finally {
                c.disconnect()
            }
        }
        throw FluxIllisible("trop de redirections")
    }
}
