package org.unicornradio.app.podcasts

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.fichier.Fichiers
import java.io.File
import java.security.MessageDigest

/**
 * Les épisodes téléchargés sur le téléphone, pour les écouter sans réseau :
 * Android télécharge (`DownloadManager`, avec sa notification), le fichier
 * va dans le dossier Podcasts privé de l'application, et `telechargements.json`
 * retient, par épisode, le numéro du téléchargement et le chemin. L'écran
 * demande, apprend la fin par la diffusion d'Android, et joue le fichier
 * local à la place de l'adresse en ligne.
 */
class Telechargements(private val contexte: Context) {

    enum class Sort { AUCUN, EN_COURS, FINI }

    private class Etat(val id: Long, val chemin: String, val fini: Boolean)

    private val fichier = File(contexte.filesDir, "telechargements.json")
    private val gestionnaire: DownloadManager
        get() = contexte.getSystemService(DownloadManager::class.java)

    fun sort(e: Episode): Sort {
        val etat = etat(e) ?: return Sort.AUCUN
        return if (etat.fini) (if (File(etat.chemin).exists()) Sort.FINI else Sort.AUCUN) else Sort.EN_COURS
    }

    /** L'état de tous les épisodes notés, par clef, pour marquer les lignes d'une liste d'un coup. */
    fun tous(): Map<String, Sort> {
        val table = lire()
        val resultat = HashMap<String, Sort>()
        for (clef in table.keys()) {
            val etat = etat(table.optJSONObject(clef) ?: continue)
            resultat[clef] = if (etat.fini) (if (File(etat.chemin).exists()) Sort.FINI else Sort.AUCUN) else Sort.EN_COURS
        }
        return resultat
    }

    /** Le fichier local, s'il est téléchargé et toujours là. */
    fun fichierDe(e: Episode): File? = etat(e)?.takeIf { it.fini }?.let { File(it.chemin) }?.takeIf { it.exists() }

    /** Lance le téléchargement ; lève si le téléphone n'a pas de stockage pour le recevoir. */
    fun lancer(e: Episode) {
        val dossier = contexte.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
            ?: throw IllegalStateException("pas de stockage")
        val nom = nomDeFichier(e)
        File(dossier, nom).delete()
        etat(e)?.let { if (!it.fini) runCatching { gestionnaire.remove(it.id) } }
        val demande = DownloadManager.Request(Uri.parse(e.url))
            .setTitle(e.titre)
            .setDescription(e.podcast)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(contexte, Environment.DIRECTORY_PODCASTS, nom)
            .addRequestHeader("User-Agent", "Unicorn-Radio/${BuildConfig.VERSION_NAME}")
        val id = gestionnaire.enqueue(demande)
        poser(e.clef, Etat(id, File(dossier, nom).path, fini = false))
        Log.i(Fichiers.JOURNAL, "téléchargement $id lancé : « ${e.titre} »")
    }

    /**
     * Android dit qu'un téléchargement est fini : s'il est à nous, noté
     * réussi, ou oublié s'il a échoué. Rend `true` si c'était l'un des nôtres.
     */
    fun terminer(id: Long): Boolean {
        val table = lire()
        val clef = table.keys().asSequence().firstOrNull { table.optJSONObject(it)?.optLong("id") == id } ?: return false
        val etat = etat(table.getJSONObject(clef))
        val reussi = reussi(id)
        if (reussi == true) {
            poser(clef, Etat(id, etat.chemin, fini = true))
            Log.i(Fichiers.JOURNAL, "téléchargement $id fini : ${etat.chemin}")
        } else {
            retirer(clef)
            Log.w(Fichiers.JOURNAL, "téléchargement $id échoué")
        }
        return true
    }

    /** Les téléchargements notés en cours, revus auprès d'Android : l'écran qui se rouvre après coup se met à jour. */
    fun rafraichir() {
        val table = lire()
        for (clef in table.keys().asSequence().toList()) {
            val etat = etat(table.getJSONObject(clef))
            if (etat.fini) continue
            when (reussi(etat.id)) {
                true -> poser(clef, Etat(etat.id, etat.chemin, fini = true))
                false -> retirer(clef)
                null -> Unit
            }
        }
    }

    /** Le fichier et la note ; un téléchargement en cours est annulé. */
    fun supprimer(e: Episode) {
        val etat = etat(e) ?: return
        if (!etat.fini) runCatching { gestionnaire.remove(etat.id) }
        File(etat.chemin).delete()
        retirer(e.clef)
    }

    /** `true` réussi, `false` échoué ou inconnu d'Android, `null` encore en route. */
    private fun reussi(id: Long): Boolean? {
        gestionnaire.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) return false
            return when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> true
                DownloadManager.STATUS_FAILED -> false
                else -> null
            }
        }
    }

    /** Un nom sûr et stable par épisode : l'empreinte de sa clef, et l'extension du fichier en ligne. */
    private fun nomDeFichier(e: Episode): String {
        val empreinte = MessageDigest.getInstance("SHA-1").digest(e.clef.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(24)
        val chemin = e.url.substringBefore('?').substringAfterLast('/')
        val extension = chemin.substringAfterLast('.', "").takeIf { it.length in 2..4 && it.all { c -> c.isLetterOrDigit() } } ?: "mp3"
        return "$empreinte.$extension"
    }

    private fun etat(e: Episode): Etat? = lire().optJSONObject(e.clef)?.let { etat(it) }

    private fun etat(o: JSONObject) = Etat(o.optLong("id"), o.optString("chemin"), o.optBoolean("fini"))

    private fun poser(clef: String, etat: Etat) = synchronized(VERROU) {
        val t = lire()
        t.put(clef, JSONObject().put("id", etat.id).put("chemin", etat.chemin).put("fini", etat.fini))
        fichier.writeText(t.toString())
    }

    private fun retirer(clef: String) = synchronized(VERROU) {
        val t = lire()
        t.remove(clef)
        fichier.writeText(t.toString())
    }

    private fun lire(): JSONObject = synchronized(VERROU) {
        try {
            if (fichier.exists()) JSONObject(fichier.readText()) else JSONObject()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "téléchargements illisibles, repris à vide : ${e.message}")
            JSONObject()
        }
    }

    companion object {
        private val VERROU = Any()
    }
}
