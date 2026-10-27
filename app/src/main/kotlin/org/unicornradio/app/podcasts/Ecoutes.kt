package org.unicornradio.app.podcasts

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.serveur.ClientServeur
import org.unicornradio.app.serveur.Serveur
import java.io.File

/**
 * Où l'on en est de chaque épisode : gardé sur le téléphone, dans
 * `ecoutes.json`, et, apparié, poussé au serveur, qui le garde par
 * utilisateur pour que la reprise soit la même sur le PC. Hors ligne, la
 * position attend ; au retour, le dernier écrit gagne, à l'horodatage en
 * millisecondes, le même des deux côtés. Le service de lecture note,
 * l'écran lit.
 */
class Ecoutes(contexte: Context) {

    private val fichier = File(contexte.filesDir, "ecoutes.json")
    private var table: JSONObject? = null

    /** Les écoutes d'un flux, par identifiant d'épisode. */
    @Synchronized
    fun pour(flux: String): Map<String, Ecoute> {
        val t = lire()
        val resultat = HashMap<String, Ecoute>()
        for (clef in t.keys()) {
            val e = ecoute(clef, t.optJSONObject(clef) ?: continue) ?: continue
            if (e.flux == flux) resultat[e.guid] = e
        }
        return resultat
    }

    /** Ce que le lecteur vient de voir : écrit tout de suite, à pousser. */
    @Synchronized
    fun noter(flux: String, guid: String, position: Long, duree: Long?, termine: Boolean) {
        val t = lire()
        t.put(clef(flux, guid), objet(Ecoute(flux, guid, position, duree, termine, System.currentTimeMillis(), aPousser = true)))
        ecrire(t)
    }

    /**
     * Apparié : pousser ce qui attend, puis prendre ce que le serveur sait de
     * ce flux, le plus récent gagnant. Sans serveur qui répond, rien ne change.
     */
    fun synchroniser(flux: String, serveur: Serveur) {
        val jeton = serveur.jeton ?: return
        val (adresse, _) = ClientServeur.trouver(serveur.adresses, serveur.id, serveur.adresseActive)
        pousser(adresse, jeton)
        val duServeur = ClientServeur.ecoutes(adresse, jeton, flux)
        synchronized(this) {
            val t = lire()
            for (e in duServeur) {
                val locale = t.optJSONObject(clef(flux, e.guid))?.let { ecoute(clef(flux, e.guid), it) }
                if (locale == null || (!locale.aPousser && e.le > locale.le)) t.put(clef(flux, e.guid), objet(e))
            }
            ecrire(t)
        }
    }

    /** Pousse au serveur tout ce qui attend ; ce qui est passé n'attend plus. Bloque. */
    fun pousserEnAttente(serveur: Serveur) {
        val jeton = serveur.jeton ?: return
        if (synchronized(this) { enAttente().isEmpty() }) return
        val (adresse, _) = ClientServeur.trouver(serveur.adresses, serveur.id, serveur.adresseActive)
        pousser(adresse, jeton)
    }

    private fun pousser(adresse: String, jeton: String) {
        for (e in synchronized(this) { enAttente() }) {
            try {
                ClientServeur.noterEcoute(adresse, jeton, e)
                synchronized(this) {
                    val t = lire()
                    val actuelle = t.optJSONObject(clef(e.flux, e.guid))?.let { ecoute(clef(e.flux, e.guid), it) }
                    if (actuelle != null && actuelle.le == e.le) {
                        t.put(clef(e.flux, e.guid), objet(e.copy(aPousser = false)))
                        ecrire(t)
                    }
                }
            } catch (ex: ClientServeur.Revoque) {
                throw ex
            } catch (ex: Exception) {
                Log.w(Fichiers.JOURNAL, "écoutes : ${e.guid} non poussée : ${ex.message}")
                return
            }
        }
    }

    private fun enAttente(): List<Ecoute> {
        val t = lire()
        return t.keys().asSequence().mapNotNull { k -> ecoute(k, t.optJSONObject(k) ?: return@mapNotNull null) }
            .filter { it.aPousser }.sortedBy { it.le }.toList()
    }

    private fun clef(flux: String, guid: String) = "$flux\n$guid"

    private fun ecoute(clef: String, o: JSONObject): Ecoute? {
        val flux = clef.substringBefore('\n')
        val guid = clef.substringAfter('\n', "")
        if (flux.isEmpty() || guid.isEmpty()) return null
        return Ecoute(
            flux, guid,
            position = o.optLong("position"),
            duree = if (o.isNull("duree")) null else o.optLong("duree"),
            termine = o.optBoolean("termine"),
            le = o.optLong("le"),
            aPousser = o.optBoolean("a_pousser"),
        )
    }

    private fun objet(e: Ecoute): JSONObject = JSONObject()
        .put("position", e.position)
        .put("duree", e.duree ?: JSONObject.NULL)
        .put("termine", e.termine)
        .put("le", e.le)
        .put("a_pousser", e.aPousser)

    private fun lire(): JSONObject {
        table?.let { return it }
        val t = try {
            if (fichier.exists()) JSONObject(fichier.readText()) else JSONObject()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "écoutes illisibles, reprises à vide : ${e.message}")
            JSONObject()
        }
        table = t
        return t
    }

    private fun ecrire(t: JSONObject) {
        table = t
        fichier.writeText(t.toString())
    }
}
