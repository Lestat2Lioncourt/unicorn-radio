package org.unicornradio.app.serveur

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.fichier.EcritureFichier
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.modele.Station
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Le journal des gestes, selon `docs/format-d-echange.md`, « Renvoyer » :
 * tout ce que l'utilisateur fait sur le téléphone, écrit sur le disque à
 * l'instant même, dans l'ordre, avec un identifiant unique et l'horodatage
 * UTC. Envoyé dès qu'un serveur répond, effacé selon sa réponse. Jamais la
 * liste entière : elle écraserait ce que la télé a fait entre-temps.
 */
class Journal(contexte: Context) {

    private val fichier: File = File(contexte.filesDir, "gestes.json")
    private var gestes: MutableList<JSONObject>? = null

    /** Les gestes en attente, dans l'ordre. */
    @Synchronized
    fun charger(): List<JSONObject> = lire().map { JSONObject(it.toString()) }

    val nombre: Int
        @Synchronized get() = lire().size

    @Synchronized
    fun favoriAjouter(station: Station): JSONObject =
        ajouter("favori.ajouter", JSONObject().put("station", EcritureFichier.fiche(station, 0)))

    @Synchronized
    fun favoriRetirer(uuid: String): JSONObject =
        ajouter("favori.retirer", JSONObject().put("station_uuid", uuid))

    @Synchronized
    fun favorisOrdonner(uuids: List<String>): JSONObject =
        ajouter("favoris.ordonner", JSONObject().put("station_uuids", JSONArray(uuids)))

    @Synchronized
    fun dossierCreer(chemin: List<String>): JSONObject =
        ajouter("dossier.creer", JSONObject().put("chemin", JSONArray(chemin)))

    @Synchronized
    fun dossierRenommer(chemin: List<String>, nom: String): JSONObject =
        ajouter("dossier.renommer", JSONObject().put("chemin", JSONArray(chemin)).put("nom", nom))

    @Synchronized
    fun dossierSupprimer(chemin: List<String>): JSONObject =
        ajouter("dossier.supprimer", JSONObject().put("chemin", JSONArray(chemin)))

    @Synchronized
    fun dossierAjouter(chemin: List<String>, station: Station): JSONObject =
        ajouter("dossier.ajouter", JSONObject().put("chemin", JSONArray(chemin)).put("station", EcritureFichier.fiche(station, 0)))

    @Synchronized
    fun dossierRetirer(chemin: List<String>, uuid: String): JSONObject =
        ajouter("dossier.retirer", JSONObject().put("chemin", JSONArray(chemin)).put("station_uuid", uuid))

    @Synchronized
    fun dossierDeplacer(chemin: List<String>, uuid: String, vers: List<String>): JSONObject =
        ajouter("dossier.deplacer", JSONObject().put("chemin", JSONArray(chemin)).put("station_uuid", uuid).put("vers", JSONArray(vers)))

    @Synchronized
    fun dossierOrdonner(chemin: List<String>, uuids: List<String>): JSONObject =
        ajouter("dossier.ordonner", JSONObject().put("chemin", JSONArray(chemin)).put("station_uuids", JSONArray(uuids)))

    /** Les gestes que le serveur a traités, acceptés, déjà vus ou refusés : sortis du journal. */
    @Synchronized
    fun retirer(ids: Collection<String>) {
        val l = lire()
        l.removeAll { it.optString("id") in ids }
        ecrire(l)
    }

    @Synchronized
    fun effacer() {
        gestes = ArrayList()
        fichier.delete()
    }

    private fun ajouter(type: String, champs: JSONObject): JSONObject {
        val geste = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("quand", Instant.now().toString())
            .put("type", type)
        for (clef in champs.keys()) geste.put(clef, champs.get(clef))
        val l = lire()
        l += geste
        ecrire(l)
        Log.i(Fichiers.JOURNAL, "journal : $type noté, ${l.size} en attente")
        return geste
    }

    private fun lire(): MutableList<JSONObject> {
        gestes?.let { return it }
        val l = ArrayList<JSONObject>()
        try {
            if (fichier.exists()) {
                val tableau = JSONArray(fichier.readText())
                for (i in 0 until tableau.length()) tableau.optJSONObject(i)?.let { l += it }
            }
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "journal illisible, repris à vide : ${e.message}")
        }
        gestes = l
        return l
    }

    private fun ecrire(l: List<JSONObject>) {
        if (l.isEmpty()) fichier.delete() else fichier.writeText(JSONArray(l).toString(2))
    }
}
