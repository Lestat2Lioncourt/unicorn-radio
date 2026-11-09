package org.unicornradio.app.fichier

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import org.unicornradio.app.modele.Noeud
import java.io.File

/**
 * Lire un fichier d'échange qu'on nous donne : « Ouvrir avec », ou le
 * sélecteur de documents. Depuis le 015, rien n'est gardé tel quel, le
 * fichier est versé dans ma liste ; les versions d'avant gardaient une copie
 * de chaque fichier ouvert dans `fichiers/`, à verser une fois puis oublier.
 */
class Fichiers(private val contexte: Context) {

    private val dossier: File
        get() = File(contexte.filesDir, "fichiers")

    /** Les fichiers gardés par les versions d'avant, par nom ; vide ensuite. Un fichier illisible est jeté. */
    fun anciens(): List<Noeud> =
        (dossier.listFiles() ?: emptyArray())
            .filter { it.isFile }
            .sortedBy { it.name.lowercase() }
            .mapNotNull { f ->
                try {
                    LectureFichier.lire(f.readText(), f.name)
                } catch (e: Exception) {
                    Log.w(JOURNAL, "ancien fichier ${f.name} illisible, jeté : ${e.message}")
                    f.delete()
                    null
                }
            }

    /** L'ancien fichier versé : effacé, et le dossier avec lui quand il est vide. */
    fun oublier(nom: String) {
        File(dossier, nom).delete()
        dossier.delete()
    }

    /**
     * Le fichier désigné par l'URI, lu et vérifié ; rien n'est gardé. Rend sa
     * racine, ou lève `LectureFichier.FichierIllisible`.
     */
    fun lire(uri: Uri): Noeud {
        val texte = contexte.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw LectureFichier.FichierIllisible("impossible d'ouvrir $uri")
        val nom = nomDe(uri)
        val racine = LectureFichier.lire(texte, nom)
        Log.i(JOURNAL, "fichier $nom lu : ${racine.stations.size} favoris, ${racine.sousDossiers.size} dossiers de premier niveau, ${racine.nombreDeStations} stations, ${racine.podcasts.size} podcasts")
        return racine
    }

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
