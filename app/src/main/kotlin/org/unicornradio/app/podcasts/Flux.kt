package org.unicornradio.app.podcasts

import android.os.SystemClock
import android.util.Log
import android.util.Xml
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.fichier.Fichiers
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Un flux RSS de podcast, lu et analysé sur le téléphone : l'en-tête du
 * podcast et ses épisodes, du plus récent au plus ancien, chacun avec son
 * fichier audio. Un flux sans `<enclosure>` audio liste des articles, pas
 * des épisodes, et le dit. Bloque : à appeler hors du fil principal.
 *
 * Le flux est analysé au fil du téléchargement : `auFil`, s'il est donné,
 * reçoit les épisodes déjà lus, d'abord les vingt premiers puis par
 * paquets, avant la liste complète que rend `lire`. Seulement quand le flux
 * va du plus récent au plus ancien, le cas presque toujours : dans l'autre
 * sens, les premiers lus seraient les plus vieux, et l'on attend la fin.
 */
object Flux {

    class FluxIllisible(message: String) : Exception(message)
    class Lu(val podcast: Podcast, val episodes: List<Episode>, val sansSon: Boolean, val complet: Boolean = true)

    private const val USER_AGENT = "Unicorn-Radio/${BuildConfig.VERSION_NAME}"
    private const val DELAI_MS = 15_000
    private const val ITUNES = "http://www.itunes.com/dtds/podcast-1.0.dtd"
    private val EXTENSIONS_AUDIO = listOf(".mp3", ".m4a", ".aac", ".ogg", ".oga", ".opus", ".wav", ".flac")
    private const val PREMIER_PAQUET = 20
    private const val PAQUET = 200

    /** L'adresse du flux ; ce qu'on savait déjà du podcast sert quand le flux ne le dit pas. */
    fun lire(adresse: String, connu: Podcast? = null, auFil: ((Lu) -> Unit)? = null): Lu {
        val debut = SystemClock.elapsedRealtime()
        var premierPaquet = -1L
        val c = ouvrir(adresse)
        var titre: String? = null
        var auteur: String? = null
        var image: String? = null
        var items = 0
        val episodes = ArrayList<Episode>()
        // Au premier paquet, le sens du flux : `null` tant qu'on ne sait pas.
        var duRecent: Boolean? = null
        try {
            val p = Xml.newPullParser()
            p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            // Sans charset dans l'en-tête HTTP, l'analyseur suit la déclaration XML.
            val charset = c.contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.trim('"')?.takeIf { it.isNotEmpty() }
            try {
                p.setInput(c.inputStream, charset)
            } catch (e: IOException) {
                throw FluxIllisible(e.message ?: e.javaClass.simpleName)
            } catch (e: Exception) {
                throw FluxIllisible("pas du XML : ${e.message}")
            }

            val dates = Dates()
            var dansItem = false
            var dansImage = false
            var courant: Brouillon? = null
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
                                // L'image propre à l'épisode, quand le flux en donne une.
                                n == "image" && ns == ITUNES -> p.getAttributeValue(null, "href")?.trim()?.takeIf { it.isNotEmpty() }?.let { b.image = it }
                                n == "enclosure" -> {
                                    val url = p.getAttributeValue(null, "url")?.trim()
                                    val mime = p.getAttributeValue(null, "type")?.trim()
                                    if (!url.isNullOrEmpty() && (b.url == null || estAudio(url, mime))) {
                                        b.url = url; b.type = mime
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
                                    date = dates.lire(b.date),
                                    duree = dureeDe(b.duree),
                                    url = url,
                                    type = b.type,
                                    flux = adresse,
                                    podcast = titre ?: connu?.nom ?: adresse,
                                    image = image ?: connu?.image,
                                    description = b.description?.takeIf { it.isNotBlank() },
                                    imagePropre = b.image,
                                )
                                val k = episodes.size
                                if (auFil != null && (k == PREMIER_PAQUET || (k > PREMIER_PAQUET && (k - PREMIER_PAQUET) % PAQUET == 0))) {
                                    if (duRecent == null) duRecent = vaDuPlusRecent(episodes)
                                    if (duRecent == true) {
                                        if (premierPaquet < 0) premierPaquet = SystemClock.elapsedRealtime() - debut
                                        auFil(assembler(adresse, connu, titre, auteur, image, episodes, items, complet = false))
                                    }
                                }
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
            } catch (e: IOException) {
                if (episodes.isEmpty()) throw FluxIllisible(e.message ?: e.javaClass.simpleName)
                Log.w(Fichiers.JOURNAL, "flux $adresse : coupé après ${episodes.size} épisodes : ${e.message}")
            } catch (e: Exception) {
                if (episodes.isEmpty()) throw FluxIllisible("XML cassé : ${e.message}")
                Log.w(Fichiers.JOURNAL, "flux $adresse : XML cassé après ${episodes.size} épisodes : ${e.message}")
            }
        } finally {
            c.disconnect()
        }

        val lu = assembler(adresse, connu, titre, auteur, image, episodes, items, complet = true)
        val tout = SystemClock.elapsedRealtime() - debut
        Log.i(Fichiers.JOURNAL, "flux « ${lu.podcast.nom} » : ${lu.episodes.size} épisodes sur $items éléments, " +
            (if (premierPaquet >= 0) "les $PREMIER_PAQUET premiers en $premierPaquet ms, " else "") + "tout en $tout ms")
        return lu
    }

    /** Le podcast et ses épisodes, du plus récent au plus ancien, à partir de ce qui est lu. */
    private fun assembler(adresse: String, connu: Podcast?, titre: String?, auteur: String?, image: String?,
                          episodes: List<Episode>, items: Int, complet: Boolean): Lu {
        // Le nom du podcast vient de l'en-tête, lu après coup pour les premiers épisodes si le flux le met tard.
        val nom = titre?.takeIf { it.isNotEmpty() } ?: connu?.nom ?: adresse
        val podcast = Podcast(adresse, nom, auteur?.takeIf { it.isNotEmpty() } ?: connu?.auteur, image ?: connu?.image, episodes.size)
        val tries = episodes.map { if (it.podcast == nom && it.image == podcast.image) it else it.copy(podcast = nom, image = podcast.image) }
            .withIndex()
            .sortedWith(compareByDescending<IndexedValue<Episode>> { it.value.date ?: Long.MIN_VALUE }.thenBy { it.index })
            .map { it.value }
        return Lu(podcast, tries, sansSon = complet && items > 0 && tries.isEmpty(), complet = complet)
    }

    /** Les premiers épisodes datés vont-ils du plus récent au plus ancien ? Sans dates, on ne peut pas dire mieux. */
    private fun vaDuPlusRecent(episodes: List<Episode>): Boolean {
        val d = episodes.mapNotNull { it.date }
        return d.size < 2 || d.first() >= d.last()
    }

    private class Brouillon {
        var titre: String? = null
        var guid: String? = null
        var date: String? = null
        var duree: String? = null
        var url: String? = null
        var type: String? = null
        var description: String? = null
        var image: String? = null
    }

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

    /**
     * Les dates d'un flux : les formats préparés une fois par lecture, sans
     * exception à chaque essai manqué. Un flux écrit toutes ses dates de la
     * même façon : le format qui a réussi est essayé d'abord, et retenu s'il
     * lit la date jusqu'au bout ; sinon, les formats dans l'ordre, comme avant.
     */
    private class Dates {
        private val formats = FORMATS_DE_DATE.map { SimpleDateFormat(it, Locale.US) }
        private var dernier = -1

        fun lire(texte: String?): Long? {
            if (texte.isNullOrBlank()) return null
            val t = texte.trim()
            if (dernier >= 0) {
                val pos = ParsePosition(0)
                val d = formats[dernier].parse(t, pos)
                if (d != null && pos.index == t.length) return d.time
            }
            for (i in formats.indices) {
                val d = formats[i].parse(t, ParsePosition(0)) ?: continue
                dernier = i
                return d.time
            }
            return null
        }
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

    /** La connexion au flux, en suivant jusqu'à cinq redirections, http vers https compris ; à fermer par l'appelant. */
    private fun ouvrir(adresse: String): HttpURLConnection {
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
                    c.disconnect()
                    return@repeat
                }
                if (code != 200) throw FluxIllisible("HTTP $code")
                return c
            } catch (e: IOException) {
                c.disconnect()
                throw FluxIllisible(e.message ?: e.javaClass.simpleName)
            } catch (e: FluxIllisible) {
                c.disconnect()
                throw e
            }
        }
        throw FluxIllisible("trop de redirections")
    }
}
