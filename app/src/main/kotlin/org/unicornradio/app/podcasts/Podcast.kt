package org.unicornradio.app.podcasts

import org.json.JSONObject
import java.util.Calendar
import java.util.Date

/** Un podcast tel que l'annuaire ou le flux le décrit ; l'adresse du flux est la clef, comme dans le fichier d'échange. */
data class Podcast(
    val flux: String,
    val nom: String,
    val auteur: String?,
    val image: String?,
    val nbEpisodes: Int?,
) {
    val initiales: String
        get() = initialesDe(nom)
}

/** Un épisode d'un flux : son fichier audio, et de quoi l'afficher et le retrouver. */
data class Episode(
    /** L'identifiant dans le flux, sinon l'adresse du fichier. */
    val guid: String,
    val titre: String,
    /** La date de publication, millisecondes depuis l'époque, ou `null`. */
    val date: Long?,
    /** La durée annoncée, en secondes, ou `null`. */
    val duree: Long?,
    val url: String,
    val type: String?,
    val flux: String,
    val podcast: String,
    /** L'image du podcast. */
    val image: String?,
    /** Le résumé, en texte nu, pour la recherche dans les épisodes. */
    val description: String? = null,
    /** L'image propre à l'épisode, quand le flux en donne une (`itunes:image` de l'élément). */
    val imagePropre: String? = null,
) {
    /** Ce que le lecteur porte comme identifiant, pour reconnaître l'épisode en cours dans la liste. */
    val mediaId: String
        get() = "podcast:$flux#$guid"

    /** La clef d'un épisode dans les listes gardées sur le téléphone : le flux, puis l'identifiant. */
    val clef: String
        get() = "$flux\n$guid"

    /** La vignette : l'image de l'épisode, sinon celle du podcast. */
    val vignette: String?
        get() = imagePropre ?: image

    val initiales: String
        get() = initialesDe(titre)

    /** Le titre et le résumé, sans accents ni majuscules, pour le filtre. */
    val texteDeRecherche: String by lazy { normaliser(titre + " " + (description ?: "")) }

    /** De quoi retrouver et afficher l'épisode plus tard : tout sauf le résumé. */
    fun versJson(): JSONObject = JSONObject()
        .put("guid", guid)
        .put("titre", titre)
        .put("date", date ?: JSONObject.NULL)
        .put("duree", duree ?: JSONObject.NULL)
        .put("url", url)
        .put("type", type ?: JSONObject.NULL)
        .put("flux", flux)
        .put("podcast", podcast)
        .put("image", image ?: JSONObject.NULL)
        .put("image_propre", imagePropre ?: JSONObject.NULL)

    companion object {
        /** Sans accents ni majuscules : « Été » et « ete » se valent. */
        fun normaliser(texte: String): String =
            java.text.Normalizer.normalize(texte, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "").lowercase()

        /** L'inverse de `versJson` ; `null` s'il manque l'essentiel. */
        fun depuisJson(o: JSONObject): Episode? {
            val guid = o.optString("guid").takeIf { it.isNotEmpty() } ?: return null
            val url = o.optString("url").takeIf { it.isNotEmpty() } ?: return null
            val flux = o.optString("flux").takeIf { it.isNotEmpty() } ?: return null
            return Episode(
                guid = guid,
                titre = o.optString("titre").ifEmpty { url.substringAfterLast('/') },
                date = if (o.isNull("date")) null else o.optLong("date"),
                duree = if (o.isNull("duree")) null else o.optLong("duree"),
                url = url,
                type = if (o.isNull("type")) null else o.optString("type"),
                flux = flux,
                podcast = o.optString("podcast").ifEmpty { flux },
                image = if (o.isNull("image")) null else o.optString("image"),
                imagePropre = if (o.isNull("image_propre")) null else o.optString("image_propre"),
            )
        }
    }
}

/**
 * Où l'on en est d'un épisode : en secondes entières, avec l'instant de
 * l'écoute en millisecondes depuis l'époque, comme le serveur le garde, et
 * si elle reste à pousser au serveur.
 */
data class Ecoute(
    val flux: String,
    val guid: String,
    val position: Long,
    val duree: Long?,
    val termine: Boolean,
    val le: Long,
    val aPousser: Boolean = false,
)

/** Les deux premières initiales d'un nom, pour tenir lieu d'image. */
fun initialesDe(nom: String): String = nom.split(Regex("\\s+")).filter { it.isNotEmpty() }
    .take(2).joinToString("") { it.first().uppercaseChar().toString() }

/** « 1:02:03 » ou « 12:34 », pour une durée en secondes. */
fun formaterDuree(secondes: Long): String {
    val s = secondes.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, sec)
    else String.format(java.util.Locale.ROOT, "%d:%02d", m, sec)
}

/** Jusqu'à cette minute du jour, heure locale, un épisode appartient à la veille : 1 h 30. */
const val MINUTES_DE_LA_VEILLE = 90

/**
 * Le jour auquel un épisode se rattache : **la nuit appartient à la veille**.
 * Le flux ne porte qu'une date de publication, en temps universel ; une
 * émission du soir finit à minuit et son fichier est publié dans le quart
 * d'heure qui suit, donc au lendemain. Publié avant 1 h 30, heure locale,
 * l'épisode est rangé au jour d'avant, pris à midi pour que le changement
 * d'heure ne fasse pas glisser la date. La date gardée ne change pas, seul
 * l'affichage se déplace. La même règle que le bureau (234 là-bas).
 */
fun jourDEpisode(ms: Long): Date {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    if (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE) < MINUTES_DE_LA_VEILLE) {
        c.add(Calendar.DAY_OF_MONTH, -1)
        c.set(Calendar.HOUR_OF_DAY, 12)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
    }
    return c.time
}
