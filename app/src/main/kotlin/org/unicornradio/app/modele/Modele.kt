package org.unicornradio.app.modele

/**
 * Une station telle que la porte le fichier d'échange (voir
 * `docs/format-d-echange.md`) : la fiche d'une station, champs à `null`
 * quand inconnus, et sa place dans la liste.
 */
data class Station(
    val uuid: String,
    val nom: String,
    val url: String,
    val paysCode: String?,
    val pays: String?,
    val page: String?,
    val logo: String?,
    val etiquettes: String?,
    val langue: String?,
    val codec: String?,
    val debit: Int?,
    val votes: Int?,
    val hls: Boolean,
    val ordre: Int,
) {
    /** « AAC+ 128 », « MP3 », ou rien : la pastille de qualité, comme sur le bureau. */
    val pastille: String
        get() = listOfNotNull(
            codec?.takeIf { it.isNotBlank() && it != "UNKNOWN" },
            debit?.takeIf { it > 0 }?.toString(),
        ).joinToString(" ")

    /** Les initiales, à la place du logo quand il manque ou ne charge pas. */
    val initiales: String
        get() = nom.split(Regex("\\s+")).filter { it.isNotEmpty() }
            .take(2).joinToString("") { it.first().uppercaseChar().toString() }
}

/**
 * Ce qu'on parcourt : un fichier, un dossier. Le même objet pour les deux,
 * seul le titre dit d'où il vient. À la racine d'un fichier, les stations
 * sont ses favoris et les sous-dossiers ses dossiers de premier niveau.
 */
class Noeud(
    val titre: String,
    val sousDossiers: List<Noeud>,
    val stations: List<Station>,
    /** Le nom du fichier dont ce noeud est la racine, sinon `null`. */
    val fichier: String? = null,
    /** La liste « Mes favoris » du téléphone. */
    val mesFavoris: Boolean = false,
) {
    val nombreDeStations: Int
        get() = stations.size + sousDossiers.sumOf { it.nombreDeStations }
}
