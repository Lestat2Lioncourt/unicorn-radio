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
        return lu
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

    // ---- Le fichier ----

    private fun oublier() {
        racine = null
        favorisUuid = null
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

    /** Celles de la liste dans cet ordre, puis les autres dans leur ordre d'avant, `ordre` réécrit. */
    private fun JSONArray.ordonnee(uuids: List<String>): JSONArray {
        val toutes = (0 until length()).mapNotNull { optJSONObject(it) }
        val rang = uuids.withIndex().associate { it.value to it.index }
        val triees = toutes.withIndex().sortedWith(compareBy({ rang[it.value.optString("station_uuid")] ?: Int.MAX_VALUE }, { it.index })).map { it.value }
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
