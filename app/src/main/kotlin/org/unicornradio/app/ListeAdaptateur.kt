package org.unicornradio.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station

/** Une ligne de la liste : un sous-dossier (ou un fichier), ou une station. */
sealed class Ligne {
    class Dossier(val noeud: Noeud) : Ligne()
    class Station(val station: org.unicornradio.app.modele.Station) : Ligne()
}

/**
 * La liste d'un noeud : ses sous-dossiers, puis ses stations, une par ligne
 * et dans l'ordre du fichier, comme sur le bureau. La station en cours est
 * marquée. Sur les résultats de l'annuaire, chaque station porte une étoile.
 */
class ListeAdaptateur(
    private val surDossier: (Noeud) -> Unit,
    private val surDossierLong: (Noeud) -> Boolean,
    private val surStation: (Station) -> Unit,
    private val surStationLong: (Station) -> Boolean,
    private val estFavorite: (Station) -> Boolean,
    private val surEtoile: (Station) -> Unit,
) : RecyclerView.Adapter<ListeAdaptateur.Vue>() {

    private var lignes: List<Ligne> = emptyList()
    private var avecEtoiles = false

    var enCours: String? = null
        set(valeur) {
            if (field == valeur) return
            field = valeur
            notifyDataSetChanged()
        }

    fun montrer(noeud: Noeud, etoiles: Boolean = false) {
        lignes = noeud.sousDossiers.map { Ligne.Dossier(it) } + noeud.stations.map { Ligne.Station(it) }
        avecEtoiles = etoiles
        notifyDataSetChanged()
    }

    /** Après une étoile : redessiner la ligne de cette station. */
    fun rafraichir(station: Station) {
        val i = lignes.indexOfFirst { it is Ligne.Station && it.station.uuid == station.uuid }
        if (i >= 0) notifyItemChanged(i)
    }

    val stations: List<Station>
        get() = lignes.filterIsInstance<Ligne.Station>().map { it.station }

    class Vue(v: View) : RecyclerView.ViewHolder(v) {
        val logo: ImageView? = v.findViewById(R.id.logo)
        val initiales: TextView? = v.findViewById(R.id.initiales)
        val icone: ImageView? = v.findViewById(R.id.icone)
        val etoile: ImageButton? = v.findViewById(R.id.etoile)
        val nom: TextView = v.findViewById(R.id.nom)
        val detail: TextView = v.findViewById(R.id.detail)
    }

    override fun getItemViewType(position: Int) = when (lignes[position]) {
        is Ligne.Dossier -> R.layout.ligne_dossier
        is Ligne.Station -> R.layout.ligne_station
    }

    override fun getItemCount() = lignes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Vue(LayoutInflater.from(parent.context).inflate(viewType, parent, false))

    override fun onBindViewHolder(vue: Vue, position: Int) {
        val c = vue.itemView.context
        when (val ligne = lignes[position]) {
            is Ligne.Dossier -> {
                val n = ligne.noeud
                vue.icone?.setImageResource(
                    when {
                        n.mesFavoris -> R.drawable.ic_etoile
                        n.fichier != null -> R.drawable.ic_fichier
                        else -> R.drawable.ic_dossier
                    },
                )
                vue.nom.text = n.titre
                vue.detail.text = c.resources.getQuantityString(R.plurals.nombre_de_stations, n.nombreDeStations, n.nombreDeStations)
                vue.itemView.isActivated = false
                vue.itemView.setOnClickListener { surDossier(n) }
                vue.itemView.setOnLongClickListener { surDossierLong(n) }
            }
            is Ligne.Station -> {
                val s = ligne.station
                vue.nom.text = s.nom
                vue.detail.text = listOfNotNull(s.pays?.takeIf { it.isNotBlank() }, s.pastille.takeIf { it.isNotBlank() })
                    .joinToString(" · ")
                vue.itemView.isActivated = s.uuid == enCours
                vue.initiales?.text = s.initiales
                vue.initiales?.visibility = View.VISIBLE
                // Le chargement en cours pour la station d'avant est annulé : sinon son logo arrive sur cette ligne.
                vue.logo?.dispose()
                vue.logo?.setImageDrawable(null)
                if (!s.logo.isNullOrBlank()) {
                    vue.logo?.load(s.logo) {
                        crossfade(true)
                        listener(onSuccess = { _, _ -> vue.initiales?.visibility = View.GONE })
                    }
                }
                vue.etoile?.let { e ->
                    if (avecEtoiles) {
                        e.visibility = View.VISIBLE
                        val favorite = estFavorite(s)
                        e.setImageResource(if (favorite) R.drawable.ic_etoile else R.drawable.ic_etoile_vide)
                        e.contentDescription = c.getString(if (favorite) R.string.retirer_des_favoris else R.string.ajouter_aux_favoris)
                        e.setOnClickListener { surEtoile(s) }
                    } else {
                        e.visibility = View.GONE
                        e.setOnClickListener(null)
                    }
                }
                vue.itemView.setOnClickListener { surStation(s) }
                vue.itemView.setOnLongClickListener { surStationLong(s) }
            }
        }
    }
}
