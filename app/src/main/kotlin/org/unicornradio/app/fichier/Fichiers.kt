package org.unicornradio.app.fichier

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import org.unicornradio.app.modele.Noeud
import java.io.File

/**
 * Les fichiers d'échange gardés par l'application : une copie de chacun dans
 * son stockage privé, sous son nom d'origine. Le même nom remplace. Relus à
 * chaque démarrage ; à deux cents stations, c'est instantané.
 */
class Fichiers(private val contexte: Context) {

    private val dossier: File
        get() = File(contexte.filesDir, "fichiers").apply { mkdirs() }

    /** Les racines de tous les fichiers gardés, par nom. */
    fun charger(): List<Noeud> =
        (dossier.listFiles() ?: emptyArray())
            .filter { it.isFile }
            .sortedBy { it.name.lowercase() }
            .mapNotNull { f ->
                try {
                    LectureFichier.lire(f.readText(), f.name)
                } catch (e: Exception) {
                    Log.w(JOURNAL, "fichier ${f.name} ignoré : ${e.message}")
                    null
                }
            }

    /**
     * Copie le fichier désigné par l'URI (« Ouvrir avec », sélecteur de
     * documents), après l'avoir lu pour être sûr qu'il est lisible. Rend sa
     * racine, ou lève `LectureFichier.FichierIllisible`.
     */
    fun importer(uri: Uri): Noeud {
        val texte = contexte.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw LectureFichier.FichierIllisible("impossible d'ouvrir $uri")
        val nom = nomDe(uri)
        val racine = LectureFichier.lire(texte, nom)
        File(dossier, nom).writeText(texte)
        Log.i(JOURNAL, "fichier $nom gardé : ${racine.stations.size} favoris, ${racine.sousDossiers.size} dossiers de premier niveau, ${racine.nombreDeStations} stations")
        return racine
    }

    fun supprimer(nom: String): Boolean = File(dossier, nom).delete()

    /** Le nom affiché par le fournisseur, sinon le dernier segment du chemin, sans caractère de chemin. */
    private fun nomDe(uri: Uri): String {
        var nom: String? = null
        if (uri.scheme == "content") {
            contexte.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) nom = c.getString(0)
            }
        }
        val brut = nom ?: uri.lastPathSegment ?: "fichier.json"
        return brut.substringAfterLast('/').substringAfterLast('\\').ifEmpty { "fichier.json" }
    }

    companion object {
        const val JOURNAL = "UnicornRadio"
    }
}
