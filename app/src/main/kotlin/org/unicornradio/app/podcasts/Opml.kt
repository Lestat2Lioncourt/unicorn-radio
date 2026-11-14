package org.unicornradio.app.podcasts

import android.util.Log
import android.util.Xml
import org.unicornradio.app.fichier.Fichiers
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.io.StringWriter
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * L'OPML, la liste d'abonnements que toutes les applications de podcasts
 * savent lire et écrire : un `<outline>` par podcast, son nom et l'adresse
 * de son flux (`xmlUrl`). Seuls les abonnements voyagent : ni les
 * positions, ni les épisodes écoutés, ni les téléchargements.
 */
object Opml {

    /** Le texte ressemble-t-il à de l'OPML : du XML qui porte `<opml`. */
    fun enEst(texte: String): Boolean {
        val debut = texte.trimStart('﻿', ' ', '\n', '\r', '\t')
        return debut.startsWith("<") && texte.contains("<opml", ignoreCase = true)
    }

    /**
     * Les podcasts d'un fichier OPML, dans l'ordre, sans doublon ; les
     * dossiers que certaines applications y mettent sont aplatis. Un
     * `outline` sans `xmlUrl` est un dossier ou un titre, pas un podcast.
     */
    fun lire(texte: String): List<Podcast> {
        val p = Xml.newPullParser()
        p.setInput(StringReader(texte.trimStart('﻿')))
        val vus = HashSet<String>()
        val liste = ArrayList<Podcast>()
        var type = p.eventType
        while (type != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG && p.name.equals("outline", ignoreCase = true)) {
                val flux = attribut(p, "xmlUrl")
                if (flux != null && (flux.startsWith("http://") || flux.startsWith("https://")) && vus.add(flux)) {
                    liste += Podcast(
                        flux = flux,
                        nom = attribut(p, "text") ?: attribut(p, "title") ?: flux,
                        auteur = null,
                        image = attribut(p, "imageUrl"),
                        nbEpisodes = null,
                    )
                }
            }
            type = p.next()
        }
        return liste
    }

    /** L'attribut, quelle que soit sa casse (`xmlUrl`, `xmlurl`), sans blancs autour ; `null` s'il est vide. */
    private fun attribut(p: XmlPullParser, nom: String): String? {
        for (i in 0 until p.attributeCount) {
            if (p.getAttributeName(i).equals(nom, ignoreCase = true)) return p.getAttributeValue(i)?.trim()?.takeIf { it.isNotEmpty() }
        }
        return null
    }

    /**
     * L'OPML ne dit que le nom : chaque flux est lu, quatre à la fois, pour
     * l'image et l'auteur, et le nom que le flux se donne. Un flux qui ne
     * répond pas garde ce que le fichier disait. Bloque : hors du fil
     * principal.
     */
    fun completer(podcasts: List<Podcast>): List<Podcast> {
        if (podcasts.isEmpty()) return podcasts
        val ouvriers = Executors.newFixedThreadPool(4)
        try {
            val futurs = podcasts.map { p ->
                ouvriers.submit(Callable {
                    try {
                        val lu = Flux.lire(p.flux, p).podcast
                        p.copy(nom = lu.nom.ifBlank { p.nom }, auteur = lu.auteur ?: p.auteur, image = lu.image ?: p.image)
                    } catch (e: Exception) {
                        Log.w(Fichiers.JOURNAL, "opml : flux ${p.flux} non lu, gardé tel quel : ${e.message}")
                        p
                    }
                })
            }
            return futurs.mapIndexed { i, f ->
                try { f.get(30, TimeUnit.SECONDS) } catch (e: Exception) { podcasts[i] }
            }
        } finally {
            ouvriers.shutdownNow()
        }
    }

    /** Le fichier OPML des podcasts suivis, dans l'ordre. */
    fun ecrire(podcasts: List<Podcast>, titre: String): String {
        val sortie = StringWriter()
        val x = Xml.newSerializer()
        x.setOutput(sortie)
        x.startDocument("UTF-8", null)
        x.text("\n")
        x.startTag(null, "opml").attribute(null, "version", "2.0")
        x.text("\n  ")
        x.startTag(null, "head")
        x.startTag(null, "title").text(titre).endTag(null, "title")
        x.endTag(null, "head")
        x.text("\n  ")
        x.startTag(null, "body")
        for (p in podcasts) {
            x.text("\n    ")
            x.startTag(null, "outline")
                .attribute(null, "type", "rss")
                .attribute(null, "text", p.nom)
                .attribute(null, "title", p.nom)
                .attribute(null, "xmlUrl", p.flux)
            p.image?.let { x.attribute(null, "imageUrl", it) }
            x.endTag(null, "outline")
        }
        x.text("\n  ")
        x.endTag(null, "body")
        x.text("\n")
        x.endTag(null, "opml")
        x.text("\n")
        x.endDocument()
        return sortie.toString()
    }
}
