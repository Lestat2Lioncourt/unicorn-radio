package org.unicornradio.app.serveur

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.fichier.EcritureFichier
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
import org.unicornradio.app.podcasts.Podcast
import java.io.File

/**
 * Une liste qu'on modifie, au format d'échange, dans le stockage privé :
 * la copie du serveur (`mes-radios.json`), remplacée à chaque tirage, ou
 * la liste locale de qui n'a pas de serveur (`mes-favoris.json`). Les
 * mêmes gestes sur les deux, appliqués sur place comme le serveur le fait,
 * pour que l'écran suive ; apparié, le journal les porte au serveur avant
 * le tirage suivant.
 */
class MesRadios(private val contexte: Context, private val nom: String) {

    private val fichier: File
        get() = File(contexte.filesDir, nom)

    private var racine: Noeud? = null
    private var favorisUuid: Set<String>? = null
    private var podcastsFlux: Set<String>? = null

    val existe: Boolean
        get() = fichier.exists()

    /** La racine, favoris en stations et dossiers en sous-dossiers ; `null` sans fichier ou illisible. */
    @Synchronized
    fun charger(): Noeud? {
        racine?.let { return it }
        val lu = try {
            if (fichier.exists()) LectureFichier.lire(fichier.readText(), nom) else null
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "$nom illisible : ${e.message}")
            null
        }
        racine = lu
        favorisUuid = lu?.stations?.map { it.uuid }?.toSet() ?: emptySet()
        podcastsFlux = lu?.podcasts?.map { it.flux }?.toSet() ?: emptySet()
        return lu
    }

    @Synchronized
    fun estSuivi(flux: String): Boolean {
        charger()
        return podcastsFlux?.contains(flux) == true
    }

    @Synchronized
    fun estFavorite(uuid: String): Boolean {
        charger()
        return favorisUuid?.contains(uuid) == true
    }

    /** Ce que le serveur vient de rendre, vérifié lisible avant d'écraser l'ancienne copie. Rend sa racine. */
    @Synchronized
    fun remplacer(texte: String): Noeud {
        val lu = LectureFichier.lire(texte, nom)
        fichier.writeText(texte)
        oublier()
        Log.i(Fichiers.JOURNAL, "$nom : ${lu.stations.size} favoris, ${lu.nombreDeStations} stations")
        return lu
    }

    @Synchronized
    fun effacer() {
        fichier.delete()
        oublier()
    }

    // ---- Les favoris ----

    /** À la fin des favoris ; déjà là, rien. */
    @Synchronized
    fun ajouterFavori(station: Station) = modifier { s ->
        val favoris = s.favoris()
        if (favoris.indexDeStation(station.uuid) < 0) favoris.put(EcritureFichier.fiche(station, favoris.length()))
    }

    /** Retiré des favoris ; absent, rien. */
    @Synchronized
    fun retirerFavori(uuid: String) = modifier { s -> s.favoris().retirerStation(uuid) }

    /** Dans cet ordre celles qui y sont ; les autres suivent. */
    @Synchronized
    fun ordonnerFavoris(uuids: List<String>) = modifier { s -> s.put("favoris", s.favoris().ordonnee(uuids)) }

    // ---- Les dossiers ----

    /** Le dossier et ses parents manquants ; existe, rien. */
    @Synchronized
    fun creerDossier(chemin: List<String>) = modifier { s -> s.dossiers().assurer(chemin) }

    /** Le dernier élément du chemin renommé, pour le dossier et tous ses descendants. */
    @Synchronized
    fun renommerDossier(chemin: List<String>, nom: String) = modifier { s ->
        val dossiers = s.dossiers()
        for (i in 0 until dossiers.length()) {
            val d = dossiers.optJSONObject(i) ?: continue
            val c = d.cheminDe() ?: continue
            if (!c.commencePar(chemin)) continue
            val nouveau = c.toMutableList().also { it[chemin.size - 1] = nom }
            d.put("chemin", JSONArray(nouveau))
        }
    }

    /** Le dossier et tout ce qu'il contient, sous-dossiers compris ; absent, rien. */
    @Synchronized
    fun supprimerDossier(chemin: List<String>) = modifier { s ->
        val dossiers = s.dossiers()
        for (i in dossiers.length() - 1 downTo 0) {
            val c = dossiers.optJSONObject(i)?.cheminDe() ?: continue
            if (c.commencePar(chemin)) dossiers.remove(i)
        }
    }

    /** À la fin du dossier, créé au besoin ; déjà là, rien. */
    @Synchronized
    fun ajouterAuDossier(chemin: List<String>, station: Station) = modifier { s ->
        val stations = s.dossiers().assurer(chemin).stationsDe()
        if (stations.indexDeStation(station.uuid) < 0) stations.put(EcritureFichier.fiche(station, stations.length()))
    }

    /** Retirée du dossier ; absent, rien. */
    @Synchronized
    fun retirerDuDossier(chemin: List<String>, uuid: String) = modifier { s ->
        val dossiers = s.dossiers()
        val i = dossiers.indexDeDossier(chemin)
        if (i >= 0) dossiers.getJSONObject(i).stationsDe().retirerStation(uuid)
    }

    /** Retirée d'un dossier, ajoutée à la fin de l'autre, avec sa fiche telle quelle. */
    @Synchronized
    fun deplacer(chemin: List<String>, uuid: String, vers: List<String>) = modifier { s ->
        val dossiers = s.dossiers()
        val i = dossiers.indexDeDossier(chemin)
        if (i < 0) return@modifier
        val source = dossiers.getJSONObject(i).stationsDe()
        val j = source.indexDeStation(uuid)
        if (j < 0) return@modifier
        val fiche = JSONObject(source.getJSONObject(j).toString())
        source.remove(j)
        val cible = dossiers.assurer(vers).stationsDe()
        if (cible.indexDeStation(uuid) < 0) cible.put(fiche.put("ordre", cible.length()))
    }

    /** Comme `ordonnerFavoris`, dans ce dossier. */
    @Synchronized
    fun ordonnerDossier(chemin: List<String>, uuids: List<String>) = modifier { s ->
        val dossiers = s.dossiers()
        val i = dossiers.indexDeDossier(chemin)
        if (i >= 0) dossiers.getJSONObject(i).let { it.put("stations", it.stationsDe().ordonnee(uuids)) }
    }

    /** Les dossiers frères sous ce parent (vide, la racine) dans cet ordre, les autres à la suite ; `ordre` réécrit. */
    @Synchronized
    fun ordonnerDossiers(parent: List<String>, noms: List<String>) = modifier { s ->
        val dossiers = s.dossiers()
        val rang = noms.withIndex().associate { it.value to it.index }
        val freres = (0 until dossiers.length()).mapNotNull { i ->
            val d = dossiers.optJSONObject(i) ?: return@mapNotNull null
            val c = d.cheminDe() ?: return@mapNotNull null
            if (c.size == parent.size + 1 && c.dropLast(1) == parent) d to d.optInt("ordre", i) else null
        }
        val tries = freres.withIndex().sortedWith(
            compareBy({ rang[it.value.first.cheminDe()!!.last()] ?: Int.MAX_VALUE }, { it.value.second }, { it.index }),
        )
        tries.forEachIndexed { i, f -> f.value.first.put("ordre", i) }
    }

    // ---- Les podcasts ----

    /** À la fin des podcasts suivis ; déjà là, rien, sa place est gardée. */
    @Synchronized
    fun ajouterPodcast(p: Podcast) = modifier { s ->
        val liste = s.podcastsJson()
        if (liste.indexDeFlux(p.flux) < 0) {
            liste.put(
                JSONObject().put("flux", p.flux).put("nom", p.nom)
                    .put("auteur", p.auteur ?: JSONObject.NULL).put("image", p.image ?: JSONObject.NULL)
                    .put("ordre", liste.length()),
            )
        }
    }

    /** Retiré des podcasts suivis ; absent, rien. */
    @Synchronized
    fun retirerPodcast(flux: String) = modifier { s ->
        val liste = s.podcastsJson()
        for (i in liste.length() - 1 downTo 0) if (liste.optJSONObject(i)?.optString("flux") == flux) liste.remove(i)
    }

    /** Dans cet ordre ceux qui y sont ; les autres suivent. */
    @Synchronized
    fun ordonnerPodcasts(fluxs: List<String>) = modifier { s -> s.put("podcasts", s.podcastsJson().ordonnee(fluxs, "flux")) }

    private fun JSONObject.podcastsJson(): JSONArray = optJSONArray("podcasts") ?: JSONArray().also { put("podcasts", it) }

    private fun JSONArray.indexDeFlux(flux: String): Int {
        for (i in 0 until length()) if (optJSONObject(i)?.optString("flux") == flux) return i
        return -1
    }

    // ---- Le fichier ----

    private fun oublier() {
        racine = null
        favorisUuid = null
        podcastsFlux = null
    }

    /** La section lue, modifiée, réécrite ; sans fichier, une section vide pour commencer. */
    private fun modifier(action: (JSONObject) -> Unit) {
        try {
            val section = if (fichier.exists()) JSONObject(fichier.readText()) else JSONObject(EcritureFichier.fichier(emptyList()))
            action(section)
            fichier.writeText(section.toString(2))
            oublier()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "$nom : modification impossible : ${e.message}")
        }
    }

    private fun JSONObject.favoris(): JSONArray = optJSONArray("favoris") ?: JSONArray().also { put("favoris", it) }
    private fun JSONObject.dossiers(): JSONArray = optJSONArray("dossiers") ?: JSONArray().also { put("dossiers", it) }
    private fun JSONObject.stationsDe(): JSONArray = optJSONArray("stations") ?: JSONArray().also { put("stations", it) }
    private fun JSONObject.cheminDe(): List<String>? = optJSONArray("chemin")?.let { c -> (0 until c.length()).map { c.optString(it) } }

    private fun List<String>.commencePar(prefixe: List<String>) = size >= prefixe.size && subList(0, prefixe.size) == prefixe

    private fun JSONArray.indexDeStation(uuid: String): Int {
        for (i in 0 until length()) if (optJSONObject(i)?.optString("station_uuid") == uuid) return i
        return -1
    }

    private fun JSONArray.retirerStation(uuid: String) {
        for (i in length() - 1 downTo 0) if (optJSONObject(i)?.optString("station_uuid") == uuid) remove(i)
    }

    /** Ceux de la liste dans cet ordre, puis les autres dans leur ordre d'avant, `ordre` réécrit ; la clef dit quel champ compare. */
    private fun JSONArray.ordonnee(clefs: List<String>, clef: String = "station_uuid"): JSONArray {
        val toutes = (0 until length()).mapNotNull { optJSONObject(it) }
        val rang = clefs.withIndex().associate { it.value to it.index }
        val triees = toutes.withIndex().sortedWith(compareBy({ rang[it.value.optString(clef)] ?: Int.MAX_VALUE }, { it.index })).map { it.value }
        return JSONArray().apply { triees.forEachIndexed { i, o -> put(o.put("ordre", i)) } }
    }

    private fun JSONArray.indexDeDossier(chemin: List<String>): Int {
        for (i in 0 until length()) if (optJSONObject(i)?.cheminDe() == chemin) return i
        return -1
    }

    /** Le dossier de ce chemin, créé avec ses parents manquants, chacun à la fin de ses frères. */
    private fun JSONArray.assurer(chemin: List<String>): JSONObject {
        var dernier: JSONObject? = null
        for (k in 1..chemin.size) {
            val prefixe = chemin.take(k)
            val i = indexDeDossier(prefixe)
            dernier = if (i >= 0) getJSONObject(i) else {
                val parent = prefixe.dropLast(1)
                val freres = (0 until length()).count { optJSONObject(it)?.cheminDe()?.let { c -> c.size == prefixe.size && c.dropLast(1) == parent } == true }
                JSONObject().put("chemin", JSONArray(prefixe)).put("ordre", freres).put("stations", JSONArray()).also { put(it) }
            }
        }
        return dernier!!
    }

    companion object {
        /** La copie du serveur. */
        const val COPIE = "mes-radios.json"
        /** La liste locale de qui n'a pas de serveur : ses favoris et, depuis le 007, ses dossiers. */
        const val LOCALE = "mes-favoris.json"
    }
}
