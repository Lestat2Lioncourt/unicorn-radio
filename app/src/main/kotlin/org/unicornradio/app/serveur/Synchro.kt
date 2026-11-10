package org.unicornradio.app.serveur

import android.util.Log
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.modele.Noeud

/**
 * Se remettre d'accord avec le serveur : trouver celui qu'on attend, lui
 * envoyer le journal des gestes s'il y en a, puis tirer sa copie. Toujours
 * dans cet ordre, pour qu'un geste fait hors ligne ne soit jamais écrasé
 * par la copie. Bloque ; lève `ClientServeur.Injoignable` ou `Revoque`.
 */
class Synchro(private val serveur: Serveur, private val mesRadios: MesRadios, private val journal: Journal) {

    class Resultat(val racine: Noeud, val envoyes: Int, val refuses: List<ClientServeur.Refus>)

    fun executer(): Resultat {
        val jeton = serveur.jeton ?: throw IllegalStateException("pas apparié")
        var envoyes = 0
        val refuses = ArrayList<ClientServeur.Refus>()
        // À l'adresse gardée ; si elle ne répond pas, le serveur est recherché et tout est refait
        // une fois : les paquets déjà passés ont quitté le journal, ils ne repartent pas.
        val (adresse, export) = serveur.avecLAdresse { adresse ->
            // Au plus deux mille gestes par appel, dit le contrat.
            for (paquet in journal.charger().chunked(2000)) {
                val r = ClientServeur.gestes(adresse, jeton, serveur.appareilId, paquet)
                journal.retirer(paquet.map { it.optString("id") })
                envoyes += paquet.size
                refuses += r.refuses
                Log.i(Fichiers.JOURNAL, "journal : ${paquet.size} envoyés, ${r.acceptes} acceptés, ${r.dejaVus} déjà vus, ${r.refuses.size} refusés")
                for (refus in r.refuses) Log.w(Fichiers.JOURNAL, "journal : geste ${refus.id} refusé : ${refus.raison}")
            }
            adresse to ClientServeur.export(adresse, jeton)
        }
        val racine = mesRadios.remplacer(export)
        serveur.noterLaCopie(adresse)
        return Resultat(racine, envoyes, refuses)
    }
}
