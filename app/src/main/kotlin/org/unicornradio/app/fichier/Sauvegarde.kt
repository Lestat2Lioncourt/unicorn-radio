package org.unicornradio.app.fichier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import org.json.JSONObject
import java.io.IOException

/**
 * La sauvegarde automatique de ma liste : le fichier d'export, créé une
 * fois à l'emplacement que l'utilisateur a choisi dans le sélecteur
 * d'Android, de préférence un espace en ligne pour survivre au téléphone,
 * puis réécrit à chaque changement. Un fichier plutôt qu'un dossier : tous
 * les espaces en ligne savent créer un fichier, pas tous donner un dossier.
 * L'accès est gardé d'un lancement à l'autre (`takePersistableUriPermission`).
 * Rien n'est écrit quand la liste n'a pas changé depuis la dernière fois.
 */
class Sauvegarde(private val contexte: Context) {

    enum class Issue { RIEN, ECRITE, EN_PANNE }

    private val prefs = contexte.getSharedPreferences("sauvegarde", Context.MODE_PRIVATE)

    private val uri: Uri?
        get() = prefs.getString("uri", null)?.let { Uri.parse(it) }

    val reglee: Boolean
        get() = uri != null

    /** Le nom du fichier, tel que l'emplacement le donne. */
    val nom: String
        get() = prefs.getString("nom", null) ?: ""

    /** L'instant de la dernière écriture réussie, en millisecondes, ou 0. */
    val le: Long
        get() = prefs.getLong("le", 0L)

    /** La dernière écriture a échoué : l'emplacement est à régler de nouveau. */
    val enPanne: Boolean
        get() = prefs.getBoolean("panne", false)

    /** Le rappel discret, une seule fois : vrai la première fois qu'on le demande. */
    fun rappelAFaire(): Boolean {
        if (prefs.getBoolean("rappel", false)) return false
        prefs.edit().putBoolean("rappel", true).apply()
        return true
    }

    /** Le fichier que l'utilisateur vient de créer : l'accès est gardé, l'ancien rendu. */
    fun regler(nouveau: Uri) {
        liberer()
        try {
            contexte.contentResolver.takePersistableUriPermission(
                nouveau, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "sauvegarde : accès non gardé : ${e.message}")
        }
        val nom = try {
            contexte.contentResolver.query(nouveau, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        val ou = nouveau.authority?.takeIf { it.isNotBlank() }
        prefs.edit()
            .putString("uri", nouveau.toString())
            .putString("nom", listOfNotNull(nom ?: nouveau.lastPathSegment, ou).joinToString(" · "))
            .remove("empreinte").remove("panne").putLong("le", 0L)
            .apply()
    }

    /** Plus de sauvegarde automatique ; le fichier déjà écrit reste où il est. */
    fun arreter() {
        liberer()
        prefs.edit().remove("uri").remove("nom").remove("empreinte").remove("panne").remove("le").apply()
    }

    private fun liberer() {
        val ancien = uri ?: return
        try {
            contexte.contentResolver.releasePersistableUriPermission(
                ancien, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: Exception) {
            // Déjà rendu, ou jamais gardé.
        }
    }

    /**
     * La section écrite si elle a changé depuis la dernière écriture, ou si
     * on force. `EN_PANNE` seulement à la première écriture qui échoue, pour
     * ne le dire qu'une fois ; les suivantes rendent `RIEN`. Bloque.
     */
    @Synchronized
    fun ecrire(section: JSONObject?, force: Boolean = false): Issue {
        val ou = uri ?: return Issue.RIEN
        if (section == null) return Issue.RIEN
        val empreinte = section.toString().let { "${it.length}:${it.hashCode()}" }
        if (!force && !enPanne && prefs.getString("empreinte", null) == empreinte) return Issue.RIEN
        return try {
            val octets = EcritureFichier.enveloppe(section).toByteArray(Charsets.UTF_8)
            val flux = try {
                contexte.contentResolver.openOutputStream(ou, "wt")
            } catch (e: Exception) {
                // Certains emplacements ne connaissent pas « wt » : « w » tronque chez eux.
                contexte.contentResolver.openOutputStream(ou, "w")
            } ?: throw IOException("impossible d'écrire")
            flux.use { it.write(octets) }
            prefs.edit().putString("empreinte", empreinte).putLong("le", System.currentTimeMillis()).remove("panne").apply()
            Log.i(Fichiers.JOURNAL, "sauvegarde : ${octets.size} octets écrits dans $nom")
            Issue.ECRITE
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "sauvegarde : écriture impossible dans $nom : ${e.message}")
            val nouvelle = !enPanne
            prefs.edit().putBoolean("panne", true).apply()
            if (nouvelle || force) Issue.EN_PANNE else Issue.RIEN
        }
    }
}
