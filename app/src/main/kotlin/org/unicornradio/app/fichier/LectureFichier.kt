package org.unicornradio.app.fichier

import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
import org.unicornradio.app.podcasts.Podcast

/**
 * Lit un fichier d'échange, selon `docs/format-d-echange.md` : le fichier de
 * configuration complet, dont seule la section `radio` compte, ou la
 * section seule. `format` absent vaut 1 ; un autre numéro est refusé.
 */
object LectureFichier {

    class FichierIllisible(message: String) : Exception(message)

    const val FORMAT_CONNU = 1

    /** Le texte du fichier, le nom à donner à sa racine ; le noeud racine en retour. */
    fun lire(texte: String, nom: String): Noeud {
        val objet = try {
            JSONObject(texte)
        } catch (e: Exception) {
            throw FichierIllisible("pas du JSON : ${e.message}")
        }
        val radio = when {
            objet.has("radio") && objet.optJSONObject("radio") != null -> objet.getJSONObject("radio")
            objet.has("favoris") && objet.has("dossiers") -> objet
            else -> throw FichierIllisible("ni section « radio », ni « favoris » et « dossiers »")
        }
        val format = radio.optInt("format", FORMAT_CONNU)
        if (format != FORMAT_CONNU) throw FichierIllisible("format $format inconnu, je connais le $FORMAT_CONNU")

        val favoris = stations(radio.optJSONArray("favoris"))
        val dossiers = dossiers(radio.optJSONArray("dossiers"))
        val podcasts = podcasts(radio.optJSONArray("podcasts"))
        return Noeud(titre = nom, sousDossiers = dossiers, stations = favoris, fichier = nom, podcasts = podcasts)
    }

    /** Les podcasts suivis, dans l'ordre ; une ligne sans adresse de flux est passée. */
    private fun podcasts(tableau: JSONArray?): List<Podcast> {
        if (tableau == null) return emptyList()
        val liste = ArrayList<Pair<Int, Podcast>>(tableau.length())
        for (i in 0 until tableau.length()) {
            val o = tableau.optJSONObject(i) ?: continue
            val flux = o.chaine("flux") ?: continue
            liste += o.optInt("ordre", i) to Podcast(
                flux = flux,
                nom = o.chaine("nom") ?: flux,
                auteur = o.chaine("auteur"),
                image = o.chaine("image"),
                nbEpisodes = null,
            )
        }
        return liste.sortedBy { it.first }.map { it.second }
    }

    /** Les stations d'une liste, dans l'ordre ; une ligne sans identifiant ou sans adresse est passée. */
    private fun stations(tableau: JSONArray?): List<Station> {
        if (tableau == null) return emptyList()
        val liste = ArrayList<Station>(tableau.length())
        for (i in 0 until tableau.length()) {
            val o = tableau.optJSONObject(i) ?: continue
            val uuid = o.chaine("station_uuid") ?: continue
            val url = o.chaine("url") ?: continue
            liste += Station(
                uuid = uuid,
                nom = o.chaine("nom") ?: uuid,
                url = url,
                paysCode = o.chaine("pays_code"),
                pays = o.chaine("pays"),
                page = o.chaine("page"),
                logo = o.chaine("logo"),
                etiquettes = o.chaine("etiquettes"),
                langue = o.chaine("langue"),
                codec = o.chaine("codec"),
                debit = o.entier("debit"),
                votes = o.entier("votes"),
                hls = o.optInt("hls", 0) == 1 || Regex("\\.m3u8(\\?|$)", RegexOption.IGNORE_CASE).containsMatchIn(url),
                ordre = o.optInt("ordre", i),
            )
        }
        return liste.sortedBy { it.ordre }
    }

    private class DossierBrut(val chemin: List<String>) {
        var ordre = Int.MAX_VALUE
        val stations = ArrayList<Station>()
        val enfants = ArrayList<DossierBrut>()
        fun versNoeud(): Noeud = Noeud(
            titre = chemin.last(),
            chemin = chemin,
            sousDossiers = enfants.sortedWith(compareBy({ it.ordre }, { it.chemin.last() })).map { it.versNoeud() },
            stations = stations,
        )
    }

    /**
     * L'arbre depuis les chemins complets : chaque dossier s'accroche à son
     * parent, créé vide s'il manque. Le même chemin deux fois est un seul
     * dossier, ses stations à la suite.
     */
    private fun dossiers(tableau: JSONArray?): List<Noeud> {
        if (tableau == null) return emptyList()
        val parChemin = HashMap<List<String>, DossierBrut>()
        val racines = ArrayList<DossierBrut>()
        fun dossier(chemin: List<String>): DossierBrut = parChemin.getOrPut(chemin) {
            DossierBrut(chemin).also { d ->
                if (chemin.size == 1) racines += d else dossier(chemin.dropLast(1)).enfants += d
            }
        }
        for (i in 0 until tableau.length()) {
            val o = tableau.optJSONObject(i) ?: continue
            val cheminJson = o.optJSONArray("chemin") ?: continue
            val chemin = (0 until cheminJson.length()).map { cheminJson.optString(it) }.filter { it.isNotEmpty() }
            if (chemin.isEmpty()) continue
            val d = dossier(chemin)
            if (d.ordre == Int.MAX_VALUE) d.ordre = o.optInt("ordre", i)
            d.stations += stations(o.optJSONArray("stations"))
        }
        return racines.sortedWith(compareBy({ it.ordre }, { it.chemin.last() })).map { it.versNoeud() }
    }

    private fun JSONObject.chaine(clef: String): String? =
        if (isNull(clef)) null else optString(clef).takeIf { it.isNotEmpty() }

    private fun JSONObject.entier(clef: String): Int? =
        if (isNull(clef)) null else optInt(clef, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
}
