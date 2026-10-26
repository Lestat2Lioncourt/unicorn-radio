package org.unicornradio.app.serveur

import android.content.Context
import android.util.Log
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.modele.Noeud
import java.io.File

/**
 * La copie locale de la liste du serveur : la section seule du format
 * d'échange, dans `mes-radios.json`, remplacée à chaque tirage, relue au
 * démarrage. Le serveur en est le seul maître ; ce qu'on y change sur le
 * téléphone passera par le journal des gestes (chantier suivant).
 */
class MesRadios(private val contexte: Context) {

    private val fichier: File
        get() = File(contexte.filesDir, "mes-radios.json")

    val existe: Boolean
        get() = fichier.exists()

    /** La racine de la copie, favoris en stations et dossiers en sous-dossiers ; `null` sans copie ou illisible. */
    fun charger(): Noeud? = try {
        if (fichier.exists()) LectureFichier.lire(fichier.readText(), fichier.name) else null
    } catch (e: Exception) {
        Log.w(Fichiers.JOURNAL, "copie du serveur illisible : ${e.message}")
        null
    }

    /** Ce que le serveur vient de rendre, vérifié lisible avant d'écraser l'ancienne copie. Rend sa racine. */
    fun remplacer(texte: String): Noeud {
        val racine = LectureFichier.lire(texte, fichier.name)
        fichier.writeText(texte)
        Log.i(Fichiers.JOURNAL, "copie du serveur : ${racine.stations.size} favoris, ${racine.nombreDeStations} stations")
        return racine
    }

    fun effacer() {
        fichier.delete()
    }
}
