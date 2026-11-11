package org.unicornradio.app.annuaire

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
import org.unicornradio.app.serveur.Journal
import org.unicornradio.app.serveur.MesRadios
import org.unicornradio.app.serveur.Serveur

/**
 * Une station de mes listes que l'annuaire a refaite : son adresse de flux a
 * changé, ou son identifiant. Le pendant, sur le téléphone, du 232 du
 * bureau, pour qui n'a pas de serveur. `chercher` demande à l'annuaire ce
 * qu'il sait encore ; `appliquer` remplace la fiche partout dans mes listes,
 * favoris et dossiers, et, apparié, le dit au serveur par les gestes du
 * contrat. Tout ici bloque, sauf `appliquer` qui finit par prévenir l'écran.
 */
object Retrouver {

    /** Ce que l'annuaire dit d'une station qui ne joue pas. */
    sealed class Verdict {
        /** Même identifiant, une autre adresse de flux : la fiche à jour. */
        class Adresse(val nouvelle: Station) : Verdict()
        /** L'identifiant n'y est plus, une seule station de ce nom : c'est elle. */
        class Identifiant(val nouvelle: Station) : Verdict()
        /** Encore là, à la même adresse : l'annuaire ne sait rien de plus. */
        object MemeAdresse : Verdict()
        /** Plus là, et zéro ou plusieurs homonymes. */
        class Disparue(val homonymes: Int) : Verdict()
        class Injoignable(val message: String?) : Verdict()
    }

    /** L'écran s'y accroche pour relire ses listes quand le service a remplacé une fiche. Appelé sur le fil principal. */
    @Volatile
    var surRemplacement: ((Station) -> Unit)? = null

    /**
     * La fiche par l'identifiant d'abord ; sans elle, les stations de même
     * nom, dans le même pays quand il est connu et qu'il départage.
     */
    fun chercher(uuid: String, nom: String, url: String, paysCode: String?): Verdict = try {
        val fiche = Annuaire.fiche(uuid)
        if (fiche != null) {
            if (fiche.url != url) Verdict.Adresse(fiche) else Verdict.MemeAdresse
        } else {
            val homonymes = homonymes(nom, paysCode)
            if (homonymes.size == 1) Verdict.Identifiant(homonymes.first()) else Verdict.Disparue(homonymes.size)
        }
    } catch (e: Exception) {
        Log.w(Fichiers.JOURNAL, "retrouver « $nom » : annuaire injoignable : ${e.message}")
        Verdict.Injoignable(e.message)
    }

    /** Les stations de ce nom exact ; s'il y en a plusieurs et qu'un pays est connu, celles du pays quand il en reste. */
    fun homonymes(nom: String, paysCode: String?): List<Station> {
        val toutes = Annuaire.chercher(nom, nomExact = true)
        if (toutes.size <= 1 || paysCode.isNullOrBlank() || paysCode == "??") return toutes
        val duPays = toutes.filter { it.paysCode == paysCode }
        return if (duPays.isEmpty()) toutes else duPays
    }

    /** Pour choisir soi-même : le nom exact d'abord, puis les stations dont le nom le contient. */
    fun candidates(nom: String): List<Station> {
        val exactes = Annuaire.chercher(nom, nomExact = true)
        val vues = exactes.map { it.uuid }.toHashSet()
        return exactes + Annuaire.chercher(nom).filter { vues.add(it.uuid) }
    }

    /**
     * La nouvelle fiche à la place de l'ancienne, partout où l'ancienne était,
     * dans la copie du serveur comme dans la liste locale ; apparié, les
     * gestes du contrat portent le remplacement au serveur, retirer puis
     * ajouter, là où l'identifiant change. Puis l'écran est prévenu.
     */
    fun appliquer(contexte: Context, ancien: String, nouvelle: Station) {
        val serveur = Serveur(contexte)
        for (nom in listOf(MesRadios.COPIE, MesRadios.LOCALE)) {
            val liste = MesRadios(contexte, nom)
            if (!liste.existe) continue
            val ou = liste.remplacerLaStation(ancien, nouvelle)
            if (nom == MesRadios.COPIE && serveur.apparie && ancien != nouvelle.uuid) {
                // Retirer puis ajouter met la nouvelle à la fin : un « ordonner » la remet au rang de l'ancienne.
                val journal = Journal(contexte)
                val racine = liste.charger()
                if (ou.favorite) {
                    journal.favoriRetirer(ancien)
                    journal.favoriAjouter(nouvelle)
                    racine?.let { r -> journal.favorisOrdonner(r.stations.map { it.uuid }) }
                }
                for (chemin in ou.dossiers) {
                    journal.dossierRetirer(chemin, ancien)
                    journal.dossierAjouter(chemin, nouvelle)
                    racine?.dossier(chemin)?.let { d -> journal.dossierOrdonner(chemin, d.stations.map { it.uuid }) }
                }
            }
        }
        Log.i(Fichiers.JOURNAL, "retrouver : $ancien remplacée par ${nouvelle.uuid} « ${nouvelle.nom} » ${nouvelle.url}")
        Handler(Looper.getMainLooper()).post { surRemplacement?.invoke(nouvelle) }
    }

    /** Le dossier de ce chemin dans l'arbre, à n'importe quelle profondeur. */
    private fun Noeud.dossier(chemin: List<String>): Noeud? {
        for (d in sousDossiers) {
            if (d.chemin == chemin) return d
            d.dossier(chemin)?.let { return it }
        }
        return null
    }
}
