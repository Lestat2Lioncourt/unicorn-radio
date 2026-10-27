package org.unicornradio.app.podcasts

/** Un podcast tel que l'annuaire ou le flux le décrit ; l'adresse du flux est la clef, comme dans le fichier d'échange. */
data class Podcast(
    val flux: String,
    val nom: String,
    val auteur: String?,
    val image: String?,
    val nbEpisodes: Int?,
) {
    val initiales: String
        get() = nom.split(Regex("\\s+")).filter { it.isNotEmpty() }
            .take(2).joinToString("") { it.first().uppercaseChar().toString() }
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
    val image: String?,
    /** Le résumé, en texte nu, pour la recherche dans les épisodes. */
    val description: String? = null,
) {
    /** Ce que le lecteur porte comme identifiant, pour reconnaître l'épisode en cours dans la liste. */
    val mediaId: String
        get() = "podcast:$flux#$guid"

    /** Le titre et le résumé, sans accents ni majuscules, pour le filtre. */
    val texteDeRecherche: String by lazy { normaliser(titre + " " + (description ?: "")) }

    companion object {
        /** Sans accents ni majuscules : « Été » et « ete » se valent. */
        fun normaliser(texte: String): String =
            java.text.Normalizer.normalize(texte, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "").lowercase()
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

/** « 1:02:03 » ou « 12:34 », pour une durée en secondes. */
fun formaterDuree(secondes: Long): String {
    val s = secondes.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, sec)
    else String.format(java.util.Locale.ROOT, "%d:%02d", m, sec)
}
