package org.unicornradio.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
import org.unicornradio.app.podcasts.Ecoute
import org.unicornradio.app.podcasts.Episode
import org.unicornradio.app.podcasts.Podcast
import org.unicornradio.app.podcasts.formaterDuree
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Une ligne de la liste : un sous-dossier (ou un fichier), une station, un podcast, une date, un épisode. */
sealed class Ligne {
    class Dossier(val noeud: Noeud) : Ligne()
    class Station(val station: org.unicornradio.app.modele.Station) : Ligne()
    class Podcast(val podcast: org.unicornradio.app.podcasts.Podcast) : Ligne()
    /** L'en-tête d'une date ; `clef` vide pour les épisodes sans date. */
    class Date(val clef: String, val nombre: Int, val repliee: Boolean) : Ligne()
    class Episode(val episode: org.unicornradio.app.podcasts.Episode, val ecoute: Ecoute?) : Ligne()
}

/**
 * La liste d'un noeud : ses sous-dossiers, puis ses stations, une par ligne
 * et dans l'ordre du fichier, comme sur le bureau. Ou des podcasts, les
 * miens ou ceux trouvés, avec une étoile pour suivre. Ou les épisodes d'un
 * podcast, groupés par date, chaque date repliable. Ce qui joue est marqué.
 */
class ListeAdaptateur(
    private val surDossier: (Noeud) -> Unit,
    private val surDossierLong: (Noeud) -> Boolean,
    private val surStation: (Station) -> Unit,
    private val surStationLong: (Station) -> Boolean,
    private val estFavorite: (Station) -> Boolean,
    private val surEtoile: (Station) -> Unit,
    private val surPodcast: (Podcast) -> Unit,
    private val surPodcastLong: (Podcast) -> Boolean,
    private val estSuivi: (Podcast) -> Boolean,
    private val surEtoilePodcast: (Podcast) -> Unit,
    private val surDate: (String) -> Unit,
    private val surEpisode: (Episode) -> Unit,
) : RecyclerView.Adapter<ListeAdaptateur.Vue>() {

    private var lignes: List<Ligne> = emptyList()
    private var avecEtoiles = false
    /** Pour mes podcasts : le titre du dernier épisode par flux, `null` quand le flux ne répond pas ; absent, pas encore lu. */
    private var derniers: Map<String, String?> = emptyMap()

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

    fun montrerPodcasts(podcasts: List<Podcast>, derniersEpisodes: Map<String, String?> = emptyMap()) {
        lignes = podcasts.map { Ligne.Podcast(it) }
        avecEtoiles = true
        derniers = derniersEpisodes
        notifyDataSetChanged()
    }

    /** Les clefs des jours affichés par `montrerEpisodes`, dans l'ordre, et combien sont repliés : pour le bouton « tout déplier ». */
    var jours: List<String> = emptyList()
        private set
    var joursReplies = 0
        private set

    /**
     * Les épisodes groupés par jour, « lundi 27/10/2026 », du plus récent au
     * plus ancien, les sans-date en bas. `depliee` dit, pour chaque clef de
     * jour, s'il est ouvert.
     */
    fun montrerEpisodes(episodes: List<Episode>, ecoutes: Map<String, Ecoute>, depliee: (String) -> Boolean) {
        val format = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val groupes = LinkedHashMap<String, MutableList<Episode>>()
        val sansDate = ArrayList<Episode>()
        for (e in episodes) {
            if (e.date == null) sansDate += e else groupes.getOrPut(format.format(Date(e.date))) { ArrayList() } += e
        }
        if (sansDate.isNotEmpty()) groupes[""] = sansDate
        val l = ArrayList<Ligne>()
        var replies = 0
        for ((clef, liste) in groupes) {
            val repliee = !depliee(clef)
            if (repliee) replies++
            l += Ligne.Date(clef, liste.size, repliee)
            if (!repliee) liste.forEach { l += Ligne.Episode(it, ecoutes[it.guid]) }
        }
        jours = groupes.keys.toList()
        joursReplies = replies
        lignes = l
        avecEtoiles = false
        notifyDataSetChanged()
    }

    /** Après une étoile : redessiner la ligne de cette station. */
    fun rafraichir(station: Station) {
        val i = lignes.indexOfFirst { it is Ligne.Station && it.station.uuid == station.uuid }
        if (i >= 0) notifyItemChanged(i)
    }

    fun rafraichir(podcast: Podcast) {
        val i = lignes.indexOfFirst { it is Ligne.Podcast && it.podcast.flux == podcast.flux }
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
        val avancement: ProgressBar? = v.findViewById(R.id.avancement)
        val etat: TextView? = v.findViewById(R.id.etat)
    }

    override fun getItemViewType(position: Int) = when (lignes[position]) {
        is Ligne.Dossier -> R.layout.ligne_dossier
        is Ligne.Station, is Ligne.Podcast -> R.layout.ligne_station
        is Ligne.Date -> R.layout.ligne_date
        is Ligne.Episode -> R.layout.ligne_episode
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
                image(vue, s.initiales, s.logo)
                etoile(vue, avecEtoiles, estFavorite(s), R.string.ajouter_aux_favoris, R.string.retirer_des_favoris) { surEtoile(s) }
                vue.itemView.setOnClickListener { surStation(s) }
                vue.itemView.setOnLongClickListener { surStationLong(s) }
            }
            is Ligne.Podcast -> {
                val p = ligne.podcast
                vue.nom.text = p.nom
                val dernier = derniers[p.flux]
                vue.detail.text = when {
                    derniers.containsKey(p.flux) && dernier == null -> c.getString(R.string.flux_muet)
                    !dernier.isNullOrEmpty() -> c.getString(R.string.dernier_episode, dernier)
                    else -> listOfNotNull(
                        p.auteur?.takeIf { it.isNotBlank() },
                        p.nbEpisodes?.let { c.resources.getQuantityString(R.plurals.nombre_d_episodes, it, it) },
                    ).joinToString(" · ")
                }
                vue.itemView.isActivated = false
                image(vue, p.initiales, p.image)
                etoile(vue, avecEtoiles, estSuivi(p), R.string.suivre, R.string.ne_plus_suivre) { surEtoilePodcast(p) }
                vue.itemView.setOnClickListener { surPodcast(p) }
                vue.itemView.setOnLongClickListener { surPodcastLong(p) }
            }
            is Ligne.Date -> {
                vue.nom.text = ligne.clef.ifEmpty { c.getString(R.string.sans_date) }
                vue.detail.text = c.resources.getQuantityString(R.plurals.nombre_d_episodes, ligne.nombre, ligne.nombre)
                vue.icone?.rotation = if (ligne.repliee) -90f else 0f
                vue.itemView.isActivated = false
                vue.itemView.setOnClickListener { surDate(ligne.clef) }
                vue.itemView.setOnLongClickListener(null)
            }
            is Ligne.Episode -> {
                val e = ligne.episode
                val ecoute = ligne.ecoute
                vue.nom.text = e.titre
                vue.detail.text = e.duree?.let { formaterDuree(it) } ?: ""
                vue.detail.visibility = if (e.duree != null) View.VISIBLE else View.GONE
                vue.itemView.isActivated = e.mediaId == enCours
                val duree = ecoute?.duree ?: e.duree
                when {
                    ecoute == null || (ecoute.position <= 0 && !ecoute.termine) -> {
                        vue.avancement?.visibility = View.GONE
                        vue.etat?.visibility = View.GONE
                    }
                    ecoute.termine -> {
                        vue.avancement?.visibility = View.GONE
                        vue.etat?.visibility = View.VISIBLE
                        vue.etat?.text = c.getString(R.string.ecoute)
                    }
                    else -> {
                        vue.avancement?.visibility = if (duree != null && duree > 0) View.VISIBLE else View.GONE
                        if (duree != null && duree > 0) vue.avancement?.progress = (ecoute.position * 1000 / duree).toInt().coerceIn(0, 1000)
                        vue.etat?.visibility = View.VISIBLE
                        vue.etat?.text = c.getString(R.string.reprendre_a, formaterDuree(ecoute.position))
                    }
                }
                vue.itemView.setOnClickListener { surEpisode(e) }
                vue.itemView.setOnLongClickListener(null)
            }
        }
    }

    /** L'étoile d'une ligne : pleine ou vide selon l'état, ou cachée. */
    private fun etoile(vue: Vue, visible: Boolean, pleine: Boolean, ajouter: Int, retirer: Int, action: () -> Unit) {
        val e = vue.etoile ?: return
        if (!visible) {
            e.visibility = View.GONE
            e.setOnClickListener(null)
            return
        }
        e.visibility = View.VISIBLE
        e.setImageResource(if (pleine) R.drawable.ic_etoile else R.drawable.ic_etoile_vide)
        e.contentDescription = vue.itemView.context.getString(if (pleine) retirer else ajouter)
        e.setOnClickListener { action() }
    }

    /** Les initiales d'abord, le logo par-dessus quand il charge ; le chargement d'avant annulé, sinon il arrive sur cette ligne. */
    private fun image(vue: Vue, initiales: String, adresse: String?) {
        vue.initiales?.text = initiales
        vue.initiales?.visibility = View.VISIBLE
        vue.logo?.dispose()
        vue.logo?.setImageDrawable(null)
        if (!adresse.isNullOrBlank()) {
            vue.logo?.load(adresse) {
                crossfade(true)
                listener(onSuccess = { _, _ -> vue.initiales?.visibility = View.GONE })
            }
        }
    }
}
