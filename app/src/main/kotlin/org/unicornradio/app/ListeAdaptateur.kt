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
import coil.transform.RoundedCornersTransformation
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
import org.unicornradio.app.podcasts.Ecoute
import org.unicornradio.app.podcasts.Episode
import org.unicornradio.app.podcasts.Podcast
import org.unicornradio.app.podcasts.formaterDuree
import org.unicornradio.app.podcasts.jourDEpisode
import java.text.SimpleDateFormat
import java.util.Locale

/** Une ligne de la liste : un sous-dossier, une station, un podcast, la liste de lecture, une date, un titre de section, un épisode. */
sealed class Ligne {
    class Dossier(val noeud: Noeud) : Ligne()
    class Station(val station: org.unicornradio.app.modele.Station) : Ligne()
    class Podcast(val podcast: org.unicornradio.app.podcasts.Podcast) : Ligne()
    /** « Liste de lecture », en tête de mes podcasts, avec ce qu'elle compte. */
    class Liste(val nombre: Int) : Ligne()
    /** L'en-tête d'une date ; `clef` vide pour les épisodes sans date. */
    class Date(val clef: String, val nombre: Int, val repliee: Boolean) : Ligne()
    /** Un simple titre de section, « Précédents », « À suivre », ou un mot quand il n'y a rien. */
    class Titre(val texte: String) : Ligne()
    class Episode(val episode: org.unicornradio.app.podcasts.Episode, val ecoute: Ecoute?, val passe: Boolean = false) : Ligne()
}

/**
 * La liste d'un noeud : ses sous-dossiers, puis ses stations, une par ligne
 * et dans l'ordre du fichier, comme sur le bureau. Ou des podcasts, les
 * miens ou ceux trouvés, avec une étoile pour suivre. Ou les épisodes d'un
 * podcast, groupés par date, chaque date repliable. Ou la liste de lecture,
 * passés, en cours, à suivre. Ce qui joue est marqué.
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
    private val surListe: () -> Unit,
    private val surDate: (String) -> Unit,
    private val surEpisode: (Episode) -> Unit,
    private val surEpisodeLong: (Episode) -> Boolean,
    /** Ce qu'une ligne d'épisode dit en plus de sa durée : « À la suite », « Téléchargé ». */
    private val etiquettes: (Episode) -> List<String>,
) : RecyclerView.Adapter<ListeAdaptateur.Vue>() {

    private var lignes: List<Ligne> = emptyList()
    private var avecEtoiles = false
    /** Pour mes podcasts : le titre du dernier épisode par flux, `null` quand le flux ne répond pas ; absent, pas encore lu. */
    private var derniers: Map<String, String?> = emptyMap()

    /** Dans la liste de lecture en mode sélection : les clefs des épisodes cochés ; `null` hors de ce mode. */
    var selection: Set<String>? = null

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

    /** Des podcasts ; avec `liste`, la liste de lecture en tête, comme sur le bureau. */
    fun montrerPodcasts(podcasts: List<Podcast>, derniersEpisodes: Map<String, String?> = emptyMap(), liste: Int? = null) {
        lignes = listOfNotNull(liste?.let { Ligne.Liste(it) }) + podcasts.map { Ligne.Podcast(it) }
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
     * plus ancien, les sans-date en bas ; la nuit appartient à la veille
     * (`jourDEpisode`). `depliee` dit, pour chaque clef de jour, s'il est
     * ouvert.
     */
    fun montrerEpisodes(episodes: List<Episode>, ecoutes: Map<String, Ecoute>, depliee: (String) -> Boolean) {
        val format = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val groupes = LinkedHashMap<String, MutableList<Episode>>()
        val sansDate = ArrayList<Episode>()
        for (e in episodes) {
            if (e.date == null) sansDate += e else groupes.getOrPut(format.format(jourDEpisode(e.date))) { ArrayList() } += e
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

    /**
     * La liste de lecture : les passés, atténués, l'épisode en cours, puis
     * ceux à suivre, chaque groupe sous son titre ; `vide` seul quand il n'y
     * a rien. `ecoute` donne où l'on en est de chacun.
     */
    fun montrerLaFile(passes: List<Episode>, enCours: Episode?, aSuivre: List<Episode>, ecoute: (Episode) -> Ecoute?, titres: Triple<String, String, String>, vide: String) {
        val l = ArrayList<Ligne>()
        if (passes.isNotEmpty()) {
            l += Ligne.Titre(titres.first)
            passes.forEach { l += Ligne.Episode(it, ecoute(it), passe = true) }
        }
        if (enCours != null) {
            l += Ligne.Titre(titres.second)
            l += Ligne.Episode(enCours, ecoute(enCours))
        }
        if (aSuivre.isNotEmpty()) {
            l += Ligne.Titre(titres.third)
            aSuivre.forEach { l += Ligne.Episode(it, ecoute(it)) }
        }
        if (l.isEmpty()) l += Ligne.Titre(vide)
        jours = emptyList()
        joursReplies = 0
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
        is Ligne.Dossier, is Ligne.Liste -> R.layout.ligne_dossier
        is Ligne.Station, is Ligne.Podcast -> R.layout.ligne_station
        is Ligne.Date, is Ligne.Titre -> R.layout.ligne_date
        is Ligne.Episode -> R.layout.ligne_episode
    }

    override fun getItemCount() = lignes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Vue(LayoutInflater.from(parent.context).inflate(viewType, parent, false))

    override fun onBindViewHolder(vue: Vue, position: Int) {
        val c = vue.itemView.context
        vue.itemView.alpha = 1f
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
            is Ligne.Liste -> {
                vue.icone?.setImageResource(R.drawable.ic_liste)
                vue.nom.text = c.getString(R.string.liste_de_lecture)
                vue.detail.text = c.resources.getQuantityString(R.plurals.nombre_d_episodes, ligne.nombre, ligne.nombre)
                vue.itemView.isActivated = false
                vue.itemView.setOnClickListener { surListe() }
                vue.itemView.setOnLongClickListener(null)
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
                vue.icone?.visibility = View.VISIBLE
                vue.icone?.rotation = if (ligne.repliee) -90f else 0f
                vue.itemView.isActivated = false
                vue.itemView.setOnClickListener { surDate(ligne.clef) }
                vue.itemView.setOnLongClickListener(null)
            }
            is Ligne.Titre -> {
                vue.nom.text = ligne.texte
                vue.detail.text = ""
                vue.icone?.visibility = View.GONE
                vue.itemView.isActivated = false
                vue.itemView.setOnClickListener(null)
                vue.itemView.setOnLongClickListener(null)
                vue.itemView.isClickable = false
                vue.itemView.isLongClickable = false
            }
            is Ligne.Episode -> {
                val e = ligne.episode
                val ecoute = ligne.ecoute
                vue.nom.text = e.titre
                val details = listOfNotNull(e.duree?.let { formaterDuree(it) }) + etiquettes(e)
                vue.detail.text = details.joinToString(" · ")
                vue.detail.visibility = if (details.isNotEmpty()) View.VISIBLE else View.GONE
                vue.itemView.isActivated = e.mediaId == enCours
                vue.itemView.alpha = if (ligne.passe) 0.6f else 1f
                image(vue, e.initiales, e.vignette, coins = 6f)
                // En mode sélection : un épisode coché est surligné, une coche à la place de sa vignette.
                val choix = selection
                if (choix != null) {
                    val coche = e.clef in choix
                    vue.itemView.isActivated = coche
                    if (coche) {
                        vue.logo?.dispose()
                        vue.logo?.setImageDrawable(null)
                        vue.initiales?.text = "✓"
                        vue.initiales?.visibility = View.VISIBLE
                    }
                }
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
                vue.itemView.setOnLongClickListener { surEpisodeLong(e) }
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

    /**
     * Les initiales d'abord, l'image par-dessus quand elle charge ; le
     * chargement d'avant annulé, sinon il arrive sur cette ligne. `coins`,
     * en dp, arrondit une vignette carrée.
     */
    private fun image(vue: Vue, initiales: String, adresse: String?, coins: Float = 0f) {
        vue.initiales?.text = initiales
        vue.initiales?.visibility = View.VISIBLE
        vue.logo?.dispose()
        vue.logo?.setImageDrawable(null)
        if (!adresse.isNullOrBlank()) {
            val rayon = coins * vue.itemView.resources.displayMetrics.density
            vue.logo?.load(adresse) {
                crossfade(true)
                if (coins > 0f) transformations(RoundedCornersTransformation(rayon))
                listener(onSuccess = { _, _ -> vue.initiales?.visibility = View.GONE })
            }
        }
    }
}
