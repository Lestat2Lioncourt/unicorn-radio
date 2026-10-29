package org.unicornradio.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.TooltipCompat
import androidx.core.os.LocaleListCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.unicornradio.app.annuaire.Annuaire
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.lecture.ServiceLecture
import org.unicornradio.app.lecture.versElement
import org.unicornradio.app.miseajour.Installation
import org.unicornradio.app.miseajour.MiseAJour
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
import org.unicornradio.app.podcasts.AnnuairePodcasts
import org.unicornradio.app.podcasts.Ecoute
import org.unicornradio.app.podcasts.Ecoutes
import org.unicornradio.app.podcasts.Episode
import org.unicornradio.app.podcasts.Flux
import org.unicornradio.app.podcasts.Podcast
import org.unicornradio.app.podcasts.estUnEpisode
import org.unicornradio.app.podcasts.formaterDuree
import org.unicornradio.app.podcasts.versElement
import org.unicornradio.app.serveur.ClientServeur
import org.unicornradio.app.serveur.Journal
import org.unicornradio.app.serveur.MesRadios
import org.unicornradio.app.serveur.Serveur
import org.unicornradio.app.serveur.Synchro
import java.util.Locale

/**
 * L'écran unique : la liste du noeud courant (fichiers, dossiers, stations)
 * sous une barre qui dit où l'on est, ou, par la loupe, la recherche dans
 * l'annuaire à la place de la liste ; et en bas la ligne de lecture, qui
 * reste dans les deux cas. Le lecteur lui-même est dans `ServiceLecture` ;
 * ici, un `MediaController` lui parle.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var fichiers: Fichiers
    private lateinit var serveur: Serveur
    /** La copie du serveur, et la liste locale de qui n'a pas de serveur. */
    private lateinit var mesRadios: MesRadios
    private lateinit var locale: MesRadios
    private lateinit var journal: Journal
    private lateinit var synchro: Synchro

    /** La liste qu'on modifie : la copie du serveur, apparié, sinon la liste locale. */
    private val maListe: MesRadios
        get() = if (serveur.apparie) mesRadios else locale
    private lateinit var barre: MaterialToolbar
    private lateinit var liste: RecyclerView
    private lateinit var vide: View
    private lateinit var videTexte: TextView
    private lateinit var videBouton: View
    private lateinit var lecture: View
    private lateinit var lectureLogo: ImageView
    private lateinit var lectureInitiales: TextView
    private lateinit var lectureNom: TextView
    private lateinit var lectureEtat: TextView
    private lateinit var lectureBouton: ImageButton
    private lateinit var recherche: View
    private lateinit var rechercheNom: EditText
    private lateinit var recherchePays: Spinner
    private lateinit var rechercheStyle: Spinner
    private lateinit var rechercheEtat: TextView
    private lateinit var onglets: MaterialButtonToggleGroup
    private lateinit var podcastsPanneau: View
    private lateinit var podcastsNom: EditText
    private lateinit var podcastsEtat: TextView
    private lateinit var lectureAvancement: View
    private lateinit var lectureBarre: SeekBar
    private lateinit var lectureReste: TextView
    private lateinit var ecoutes: Ecoutes
    private lateinit var adaptateur: ListeAdaptateur

    /** Le chemin parcouru, la racine en premier. Vide tant qu'aucun fichier n'est chargé. */
    private val pile = ArrayDeque<Noeud>()

    // La recherche : son mode, ses listes, ses résultats, et une génération pour ignorer une réponse dépassée.
    private var modeRecherche = false
    private var pays: List<Annuaire.Pays>? = null
    private var etiquettes: List<Annuaire.Etiquette>? = null
    private var filtresPrets = false
    private var resultats: List<Station> = emptyList()
    private var generation = 0
    private val minuterie = Handler(Looper.getMainLooper())
    private val lancer = Runnable { chercher() }

    // L'onglet Podcasts : la recherche dans l'annuaire, le podcast ouvert et ses épisodes, où l'on en est de chacun.
    private enum class Onglet { RADIO, PODCASTS }
    private var onglet = Onglet.RADIO
    private var podcastsResultats: List<Podcast> = emptyList()
    private var podcastOuvert: Podcast? = null
    private var episodes: List<Episode> = emptyList()
    private var ecoutesDuPodcast: Map<String, Ecoute> = emptyMap()
    private var generationPodcasts = 0
    private val lancerPodcasts = Runnable { chercherDesPodcasts() }
    private var barreTenue = false
    /**
     * Les plis des jours du podcast ouvert, rien n'est gardé d'un podcast à
     * l'autre. Hors recherche : tout replié à l'ouverture, « tout déplier »
     * retient le choix le temps du podcast, et `exceptions` liste les jours
     * pliés ou dépliés à la main contre ce choix. Pendant une recherche, les
     * jours trouvés sont dépliés, et `plisDeRecherche` ceux repliés là, le
     * temps de la recherche seulement.
     */
    private var toutDeplie = false
    private val exceptions = HashSet<String>()
    private val plisDeRecherche = HashSet<String>()
    private var enRecherche = false
    private lateinit var episodesPanneau: View
    private lateinit var episodesDeplier: ImageButton
    private lateinit var episodesFiltre: EditText
    private lateinit var episodesEtat: TextView
    /** Pour « Mes podcasts » : le dernier épisode par flux, `null` quand le flux ne répond pas ; lu en arrière-plan. */
    private val derniersEpisodes = HashMap<String, String?>()
    private val derniersEnCours = HashSet<String>()
    private val avancement = object : Runnable {
        override fun run() {
            rafraichirAvancement()
            minuterie.postDelayed(this, 1_000)
        }
    }

    // La mise à jour proposée : une vérification à la fois, le téléchargement s'il est lancé.
    private val miseAJour by lazy { MiseAJour(this) }
    private var installation: Installation? = null
    private var verificationEnCours = false

    // Le serveur apparié : un tirage à la fois, et la barre dit s'il ne répond pas, après deux tirages manqués de suite.
    private var tirageEnCours = false
    private var tirageARefaire = false
    private var tiragesManques = 0
    private val serveurInjoignable: Boolean
        get() = tiragesManques >= 2

    private var controleurFutur: ListenableFuture<MediaController>? = null
    private var controleur: MediaController? = null

    private val retour = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                modeRecherche -> quitterLaRecherche()
                onglet == Onglet.PODCASTS && podcastOuvert != null -> fermerLePodcast()
                onglet == Onglet.PODCASTS -> onglets.check(R.id.ongletRadio)
                else -> { pile.removeLast(); afficher() }
            }
        }
    }

    private val choisirFichier = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importer(uri)
    }

    private val demanderNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Au retour de l'écran Serveur, quoi qu'il s'y soit passé : relire, et `onStart` tire. */
    private val ouvrirServeur = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        tiragesManques = 0
        pile.clear()
        charger()
    }

    private val surLecteur = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = rafraichirLecture()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fichiers = Fichiers(this)
        serveur = Serveur(this)
        mesRadios = MesRadios(this, MesRadios.COPIE)
        locale = MesRadios(this, MesRadios.LOCALE)
        journal = Journal(this)
        synchro = Synchro(serveur, mesRadios, journal)

        barre = findViewById(R.id.barre)
        liste = findViewById(R.id.liste)
        vide = findViewById(R.id.vide)
        videTexte = findViewById(R.id.videTexte)
        videBouton = findViewById(R.id.videBouton)
        lecture = findViewById(R.id.lecture)
        lectureLogo = findViewById(R.id.lectureLogo)
        lectureInitiales = findViewById(R.id.lectureInitiales)
        lectureNom = findViewById(R.id.lectureNom)
        lectureEtat = findViewById(R.id.lectureEtat)
        lectureBouton = findViewById(R.id.lectureBouton)
        recherche = findViewById(R.id.recherche)
        rechercheNom = findViewById(R.id.rechercheNom)
        recherchePays = findViewById(R.id.recherchePays)
        rechercheStyle = findViewById(R.id.rechercheStyle)
        rechercheEtat = findViewById(R.id.rechercheEtat)
        onglets = findViewById(R.id.onglets)
        podcastsPanneau = findViewById(R.id.podcasts)
        podcastsNom = findViewById(R.id.podcastsNom)
        podcastsEtat = findViewById(R.id.podcastsEtat)
        episodesPanneau = findViewById(R.id.episodesPanneau)
        episodesDeplier = findViewById(R.id.episodesDeplier)
        episodesFiltre = findViewById(R.id.episodesFiltre)
        episodesEtat = findViewById(R.id.episodesEtat)
        lectureAvancement = findViewById(R.id.lectureAvancement)
        lectureBarre = findViewById(R.id.lectureBarre)
        lectureReste = findViewById(R.id.lectureReste)
        ecoutes = Ecoutes(this)

        adaptateur = ListeAdaptateur(
            surDossier = { pile.addLast(it); afficher() },
            surDossierLong = { menuDossier(it) },
            surStation = { jouer(it) },
            surStationLong = { menuStation(it) },
            estFavorite = { maListe.estFavorite(it.uuid) },
            surEtoile = { etoiler(it) },
            surPodcast = { ouvrirLePodcast(it) },
            surPodcastLong = { menuPodcast(it) },
            estSuivi = { maListe.estSuivi(it.flux) },
            surEtoilePodcast = { suivre(it) },
            surDate = { basculerLaDate(it) },
            surEpisode = { jouerLEpisode(it) },
        )
        liste.layoutManager = LinearLayoutManager(this)
        liste.adapter = adaptateur

        barre.inflateMenu(R.menu.principal)
        // Les trois points du menu, de la même teinte que les autres icônes de la barre.
        barre.overflowIcon?.setTint(getColor(R.color.icone_barre))
        barre.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.ouvrir -> { ouvrirUnFichier(); true }
                R.id.chercher -> { entrerDansLaRecherche(); true }
                R.id.serveur -> { ouvrirServeur.launch(Intent(this, ServeurActivity::class.java)); true }
                R.id.miseAJour -> { verifierLaMiseAJour(aLaMain = true); true }
                else -> false
            }
        }
        barre.setNavigationOnClickListener { retour.handleOnBackPressed() }
        poserLesDrapeaux()
        videBouton.setOnClickListener { ouvrirUnFichier() }
        findViewById<View>(R.id.videChercher).setOnClickListener { entrerDansLaRecherche() }
        findViewById<View>(R.id.videServeur).setOnClickListener { ouvrirServeur.launch(Intent(this, ServeurActivity::class.java)) }
        lectureBouton.setOnClickListener { basculerLecture() }
        onBackPressedDispatcher.addCallback(this, retour)

        rechercheNom.doAfterTextChanged { programmerLaRecherche() }
        val surFiltre = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (filtresPrets) programmerLaRecherche(0)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        recherchePays.onItemSelectedListener = surFiltre
        rechercheStyle.onItemSelectedListener = surFiltre

        onglets.addOnButtonCheckedListener { _, id, coche ->
            if (coche) choisirLOnglet(if (id == R.id.ongletPodcasts) Onglet.PODCASTS else Onglet.RADIO)
        }
        podcastsNom.doAfterTextChanged { programmerLaRecherchePodcasts() }
        findViewById<View>(R.id.podcastsColler).setOnClickListener { collerUneAdresse() }
        episodesFiltre.doAfterTextChanged {
            plisDeRecherche.clear()
            if (podcastOuvert != null) montrerLesEpisodes()
        }
        episodesDeplier.setOnClickListener { basculerTousLesJours() }
        findViewById<View>(R.id.lectureReculer).setOnClickListener { sauter(-15_000) }
        findViewById<View>(R.id.lectureAvancer).setOnClickListener { sauter(15_000) }
        lectureBarre.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(barre: SeekBar, progression: Int, parUtilisateur: Boolean) {
                if (parUtilisateur) lectureReste.text = "-" + formaterDuree((barre.max - progression).toLong())
            }
            override fun onStartTrackingTouch(barre: SeekBar) { barreTenue = true }
            override fun onStopTrackingTouch(barre: SeekBar) {
                barreTenue = false
                controleur?.seekTo(barre.progress * 1_000L)
            }
        })

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            demanderNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        charger()
        traiter(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        traiter(intent)
    }

    /** « Ouvrir avec » : l'intention porte le fichier. */
    private fun traiter(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            intent.data = null
            importer(uri)
        }
    }

    private fun ouvrirUnFichier() {
        choisirFichier.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
    }

    override fun onStart() {
        super.onStart()
        val jeton = SessionToken(this, ComponentName(this, ServiceLecture::class.java))
        controleurFutur = MediaController.Builder(this, jeton).buildAsync().also { futur ->
            futur.addListener({
                controleur = try { futur.get() } catch (e: Exception) {
                    Log.w(Fichiers.JOURNAL, "pas de contrôleur : ${e.message}")
                    null
                }
                controleur?.addListener(surLecteur)
                rafraichirLecture()
            }, MoreExecutors.directExecutor())
        }
        tirer()
        verifierLaMiseAJour(aLaMain = false)
    }

    override fun onDestroy() {
        installation?.fermer()
        installation = null
        super.onDestroy()
    }

    override fun onStop() {
        minuterie.removeCallbacks(avancement)
        controleur?.removeListener(surLecteur)
        controleurFutur?.let { MediaController.releaseFuture(it) }
        controleur = null
        controleurFutur = null
        super.onStop()
    }

    // ---- La langue ----

    /**
     * Les deux drapeaux de la barre, comme sur le bureau : celui de la langue
     * active est net, l'autre atténué, appuyer choisit. Le choix passe par
     * AppCompat, qui le garde et relance l'écran ; le son, dans le service,
     * continue. Sans choix, la langue du téléphone, et l'anglais sinon.
     */
    private fun poserLesDrapeaux() {
        val drapeaux = barre.menu.findItem(R.id.langues).actionView ?: return
        val fr = drapeaux.findViewById<View>(R.id.drapeauFr)
        val en = drapeaux.findViewById<View>(R.id.drapeauEn)
        val active = resources.configuration.locales[0].language
        fr.alpha = if (active == "fr") 1f else 0.4f
        en.alpha = if (active == "fr") 0.4f else 1f
        fr.setOnClickListener { choisirLaLangue("fr") }
        en.setOnClickListener { choisirLaLangue("en") }
    }

    private fun choisirLaLangue(langue: String) {
        if (resources.configuration.locales[0].language == langue) return
        if (modeRecherche) quitterLaRecherche(sansAfficher = true)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langue))
    }

    // ---- Le serveur ----

    /**
     * Apparié, se remettre d'accord avec le serveur : le journal des gestes
     * envoyé, puis la copie tirée. À chaque retour au premier plan, donc
     * aussi au retour de l'écran Serveur, et après chaque geste. Un tirage
     * demandé pendant un autre est refait à la fin. Injoignable, la copie
     * reste et la barre le dit ; révoqué, on repasse en mode fichiers et on
     * le dit.
     */
    private fun tirer() {
        if (!serveur.apparie) return
        if (tirageEnCours) { tirageARefaire = true; return }
        tirageEnCours = true
        lifecycleScope.launch {
            try {
                val resultat = withContext(Dispatchers.IO) { synchro.executer() }
                tiragesManques = 0
                if (resultat.refuses.isNotEmpty()) {
                    val n = resultat.refuses.size
                    Toast.makeText(this@MainActivity, resources.getQuantityString(R.plurals.gestes_refuses, n, n), Toast.LENGTH_LONG).show()
                }
                charger()
            } catch (e: ClientServeur.Revoque) {
                val nom = serveur.nom
                withContext(Dispatchers.IO) { serveur.oublier(); mesRadios.effacer(); journal.effacer() }
                tiragesManques = 0
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setMessage(getString(R.string.revoque, nom))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                pile.clear()
                charger()
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "serveur : tirage : ${e.message}")
                tiragesManques++
                if (!modeRecherche) afficher()
            } finally {
                tirageEnCours = false
                if (tirageARefaire) { tirageARefaire = false; tirer() }
            }
        }
    }

    // ---- Les fichiers ----

    /**
     * Relit tout : « Mes favoris » s'il y en a, puis les fichiers gardés. Un
     * seul, c'est la racine ; plusieurs, la racine les liste. Le chemin où
     * l'on était est retrouvé par ses titres ; `versFichier` ouvre un fichier
     * qu'on vient d'ajouter.
     */
    private fun charger(versFichier: String? = null) {
        val chemin = pile.map { it.titre }
        lifecycleScope.launch {
            val racines = withContext(Dispatchers.IO) {
                val mienne = maListe.charger()
                val liste = ArrayList<Noeud>()
                if (serveur.apparie) {
                    // Apparié : la copie du serveur tient lieu de tout, ses favoris en tête, puis ses dossiers.
                    if (mienne != null) {
                        liste += Noeud(getString(R.string.mes_favoris), emptyList(), mienne.stations, mesFavoris = true, mien = true)
                        liste += mienne.sousDossiers.map { it.aMoi() }
                    }
                } else {
                    // La liste locale, ses favoris s'il y en a et ses dossiers, puis les fichiers importés.
                    if (mienne != null && mienne.stations.isNotEmpty()) {
                        liste += Noeud(getString(R.string.mes_favoris), emptyList(), mienne.stations, mesFavoris = true, mien = true)
                    }
                    if (mienne != null) liste += mienne.sousDossiers.map { it.aMoi() }
                    liste += fichiers.charger()
                }
                liste
            }
            pile.clear()
            when {
                racines.isEmpty() -> Unit
                // Un seul fichier importé est la racine lui-même ; ma liste est toujours sous le nom de l'application.
                racines.size == 1 && !racines[0].mien -> pile.addLast(racines[0])
                else -> pile.addLast(Noeud(getString(R.string.app_name), racines, emptyList()))
            }
            if (versFichier != null) {
                if (pile.size == 1 && pile.first().fichier != versFichier) {
                    pile.first().sousDossiers.firstOrNull { it.fichier == versFichier }?.let { pile.addLast(it) }
                }
            } else if (pile.isNotEmpty()) {
                for (titre in chemin.drop(1)) {
                    val suivant = pile.last().sousDossiers.firstOrNull { it.titre == titre } ?: break
                    pile.addLast(suivant)
                }
            }
            if (!modeRecherche) afficher()
        }
    }

    private fun importer(uri: Uri) {
        lifecycleScope.launch {
            try {
                val racine = withContext(Dispatchers.IO) { fichiers.importer(uri) }
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.fichier_ajoute, racine.titre, racine.nombreDeStations),
                    Toast.LENGTH_LONG,
                ).show()
                if (modeRecherche) quitterLaRecherche(sansAfficher = true)
                charger(versFichier = racine.fichier)
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "import refusé : ${e.message}")
                val raison = if (e is LectureFichier.FichierIllisible) e.message else e.javaClass.simpleName
                Toast.makeText(this@MainActivity, getString(R.string.fichier_illisible, raison), Toast.LENGTH_LONG).show()
            }
        }
    }

    /** L'appui long sur un dossier : le mien, son menu ; un fichier importé, le retirer ; le reste, rien. */
    private fun menuDossier(noeud: Noeud): Boolean {
        val chemin = noeud.chemin
        if (!noeud.mien || chemin == null) return proposerDeRetirer(noeud)
        val actions = mutableListOf<Pair<String, () -> Unit>>(
            getString(R.string.nouveau_sous_dossier) to {
                demanderUnNom(R.string.nouveau_sous_dossier) { nom ->
                    agir(surJournal = { it.dossierCreer(chemin + nom) }) { it.creerDossier(chemin + nom) }
                }
            },
            getString(R.string.renommer) to {
                demanderUnNom(R.string.renommer, noeud.titre) { nom ->
                    if (nom != noeud.titre) agir(surJournal = { it.dossierRenommer(chemin, nom) }) { it.renommerDossier(chemin, nom) }
                }
            },
            getString(R.string.supprimer) to {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.supprimer_dossier_titre)
                    .setMessage(getString(R.string.supprimer_dossier_texte, noeud.titre))
                    .setNegativeButton(R.string.annuler, null)
                    .setPositiveButton(R.string.supprimer) { _, _ ->
                        agir(surJournal = { it.dossierSupprimer(chemin) }) { it.supprimerDossier(chemin) }
                    }
                    .show()
            },
        )
        // Monter et descendre parmi ses frères : un geste « dossiers.ordonner » avec tous les noms dans le nouvel ordre.
        val freres = pile.lastOrNull()?.sousDossiers?.filter { it.mien && it.chemin != null } ?: emptyList()
        val i = freres.indexOfFirst { it.chemin == chemin }
        val parent = chemin.dropLast(1)
        fun ordonner(vers: Int) {
            val noms = freres.map { it.titre }.toMutableList()
            noms.add(vers, noms.removeAt(i))
            agir(surJournal = { it.dossiersOrdonner(parent, noms) }) { it.ordonnerDossiers(parent, noms) }
        }
        if (i > 0) actions += getString(R.string.monter) to { ordonner(i - 1) }
        if (i in 0 until freres.size - 1) actions += getString(R.string.descendre) to { ordonner(i + 1) }
        MaterialAlertDialogBuilder(this)
            .setTitle(noeud.titre)
            .setItems(actions.map { it.first }.toTypedArray()) { _, i -> actions[i].second() }
            .show()
        return true
    }

    private fun proposerDeRetirer(noeud: Noeud): Boolean {
        val nom = noeud.fichier ?: return false
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.retirer_fichier_titre)
            .setMessage(getString(R.string.retirer_fichier_texte, nom))
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.retirer) { _, _ ->
                fichiers.supprimer(nom)
                pile.clear()
                charger()
            }
            .show()
        return true
    }

    // ---- La mise à jour proposée ----

    /**
     * La dernière release du dépôt GitHub, comparée à la version installée.
     * D'elle-même à l'ouverture, au plus une fois par jour et sans un mot si
     * rien n'est plus récent ou si GitHub ne répond pas ; à la main depuis le
     * menu, avec la réponse dans tous les cas.
     */
    private fun verifierLaMiseAJour(aLaMain: Boolean) {
        if (verificationEnCours) return
        if (!aLaMain && !miseAJour.aVerifier()) return
        verificationEnCours = true
        lifecycleScope.launch {
            try {
                val derniere = withContext(Dispatchers.IO) { miseAJour.derniere() }
                miseAJour.noterLaVerification()
                if (derniere != null && MiseAJour.plusRecente(derniere.numero, BuildConfig.VERSION_NAME)) {
                    proposerLaVersion(derniere)
                } else if (aLaMain) {
                    Toast.makeText(this@MainActivity, getString(R.string.a_jour, BuildConfig.VERSION_NAME), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "mise à jour : ${e.message}")
                if (aLaMain) Toast.makeText(this@MainActivity, getString(R.string.github_injoignable, e.message), Toast.LENGTH_LONG).show()
            } finally {
                verificationEnCours = false
            }
        }
    }

    /** La note dans la langue de l'application, et deux boutons : installer, ou plus tard, c'est-à-dire demain. */
    private fun proposerLaVersion(version: MiseAJour.Version) {
        val langue = resources.configuration.locales[0].language
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.version_disponible, version.numero))
            .setMessage(MiseAJour.noteDans(version.note, langue))
            .setNegativeButton(R.string.plus_tard, null)
            .setPositiveButton(R.string.installer) { _, _ -> installerLaVersion(version) }
            .show()
    }

    /** L'APK joint à la release, téléchargé par Android, puis son installeur ; sans APK joint, ou s'il échoue, la page de la version. */
    private fun installerLaVersion(version: MiseAJour.Version) {
        if (version.apk == null) {
            ouvrirLaPage(version.page)
            return
        }
        val i = installation ?: Installation(this) { raison ->
            MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.telechargement_echoue, raison))
                .setNegativeButton(R.string.annuler, null)
                .setPositiveButton(R.string.ouvrir_la_page) { _, _ -> ouvrirLaPage(version.page) }
                .show()
        }.also { installation = it }
        try {
            i.lancer(version)
            Toast.makeText(this, getString(R.string.telechargement_lance, version.numero), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "mise à jour : téléchargement impossible : ${e.message}")
            ouvrirLaPage(version.page)
        }
    }

    private fun ouvrirLaPage(page: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page)))
        } catch (e: Exception) {
            Toast.makeText(this, page, Toast.LENGTH_LONG).show()
        }
    }

    // ---- La liste ----

    /** Le sous-titre de la racine : le serveur apparié, s'il ne répond pas, et la version. */
    private fun sousTitreDeLaRacine(): String = listOfNotNull(
        serveur.nom.takeIf { serveur.apparie && it.isNotEmpty() },
        getString(R.string.injoignable_court).takeIf { serveur.apparie && serveurInjoignable },
        BuildConfig.VERSION_NAME,
    ).joinToString(" · ")

    private fun afficher() {
        val apparie = serveur.apparie
        // Apparié, les fichiers n'ont plus cours ; la loupe et le fichier sont de l'onglet Radio.
        barre.menu.findItem(R.id.ouvrir).isVisible = !apparie && onglet == Onglet.RADIO
        barre.menu.findItem(R.id.chercher).isVisible = onglet == Onglet.RADIO
        if (modeRecherche) {
            retour.isEnabled = true
            barre.navigationIcon = getDrawable(R.drawable.ic_retour)
            barre.logo = null
            barre.title = getString(R.string.annuaire)
            barre.subtitle = null
            onglets.visibility = View.GONE
            podcastsPanneau.visibility = View.GONE
            episodesPanneau.visibility = View.GONE
            recherche.visibility = View.VISIBLE
            vide.visibility = View.GONE
            liste.visibility = View.VISIBLE
            adaptateur.montrer(Noeud(getString(R.string.annuaire), emptyList(), resultats), etoiles = true)
            return
        }
        recherche.visibility = View.GONE
        if (onglet == Onglet.PODCASTS) {
            afficherLesPodcasts()
            return
        }
        podcastsPanneau.visibility = View.GONE
        episodesPanneau.visibility = View.GONE
        val noeud = pile.lastOrNull()
        val racine = pile.size <= 1
        retour.isEnabled = !racine
        onglets.visibility = if (racine) View.VISIBLE else View.GONE
        barre.navigationIcon = if (racine) null else getDrawable(R.drawable.ic_retour)
        // À la racine, le logo devant le nom ; dans un dossier, la flèche retour prend sa place.
        barre.logo = if (racine) getDrawable(R.drawable.logo) else null
        barre.title = noeud?.titre ?: getString(R.string.app_name)
        barre.subtitle = if (racine) sousTitreDeLaRacine() else null
        if (noeud == null) {
            videTexte.text = if (apparie) getString(R.string.copie_absente, serveur.nom) else getString(R.string.aucun_fichier)
            videBouton.visibility = if (apparie) View.GONE else View.VISIBLE
            vide.visibility = View.VISIBLE
            liste.visibility = View.GONE
        } else {
            vide.visibility = View.GONE
            liste.visibility = View.VISIBLE
            adaptateur.montrer(noeud)
            liste.scrollToPosition(0)
        }
    }

    // ---- La recherche dans l'annuaire ----

    private fun entrerDansLaRecherche() {
        if (modeRecherche) return
        modeRecherche = true
        afficher()
        if (pays == null) chargerLesFiltres() else rechercheEtat.text = etatDeDepart()
        rechercheNom.requestFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(rechercheNom, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun quitterLaRecherche(sansAfficher: Boolean = false) {
        minuterie.removeCallbacks(lancer)
        generation++
        modeRecherche = false
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(rechercheNom.windowToken, 0)
        if (!sansAfficher) afficher()
    }

    /** Les pays et les styles, une fois par vie de l'écran ; le pays du téléphone choisi d'avance. */
    private fun chargerLesFiltres() {
        rechercheEtat.text = getString(R.string.recherche_en_cours)
        lifecycleScope.launch {
            try {
                val (p, e) = withContext(Dispatchers.IO) {
                    Annuaire.pays().sortedBy { it.nom.lowercase(Locale.getDefault()) } to Annuaire.etiquettes()
                }
                pays = p
                etiquettes = e
                recherchePays.adapter = ArrayAdapter(
                    this@MainActivity, android.R.layout.simple_spinner_item,
                    listOf(getString(R.string.tous_les_pays)) + p.map { it.nom },
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                rechercheStyle.adapter = ArrayAdapter(
                    this@MainActivity, android.R.layout.simple_spinner_item,
                    listOf(getString(R.string.tous_les_styles)) + e.map { it.nom },
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                val ici = Locale.getDefault().country.uppercase()
                val position = p.indexOfFirst { it.code == ici }
                if (position >= 0) recherchePays.setSelection(position + 1, false)
                rechercheEtat.text = etatDeDepart()
                // Les choix faits avant que les listes arrivent comptent à partir d'ici.
                recherchePays.post { filtresPrets = true; programmerLaRecherche(0) }
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "annuaire : filtres : ${e.message}")
                rechercheEtat.text = getString(R.string.annuaire_injoignable, e.message)
            }
        }
    }

    private fun etatDeDepart() = getString(R.string.recherche_tapez)

    private fun paysChoisi(): String? = pays?.getOrNull(recherchePays.selectedItemPosition - 1)?.code

    private fun styleChoisi(): String? = etiquettes?.getOrNull(rechercheStyle.selectedItemPosition - 1)?.nom

    /** Taper relance au bout de trois cents millisecondes ; un filtre, tout de suite. */
    private fun programmerLaRecherche(delai: Long = 300) {
        if (!modeRecherche) return
        minuterie.removeCallbacks(lancer)
        minuterie.postDelayed(lancer, delai)
    }

    private fun chercher() {
        if (!modeRecherche) return
        val nom = rechercheNom.text.toString().trim()
        val codePays = paysChoisi()
        val style = styleChoisi()
        val g = ++generation
        if (nom.length < 2 && codePays == null && style == null) {
            resultats = emptyList()
            rechercheEtat.text = etatDeDepart()
            afficher()
            return
        }
        rechercheEtat.text = getString(R.string.recherche_en_cours)
        lifecycleScope.launch {
            try {
                val trouvees = withContext(Dispatchers.IO) { Annuaire.chercher(nom, codePays, style) }
                if (g != generation || !modeRecherche) return@launch
                resultats = trouvees
                rechercheEtat.text = if (trouvees.isEmpty()) getString(R.string.recherche_rien)
                else getString(R.string.recherche_nombre, trouvees.size)
                afficher()
                liste.scrollToPosition(0)
            } catch (e: Exception) {
                if (g != generation || !modeRecherche) return@launch
                Log.w(Fichiers.JOURNAL, "annuaire : recherche : ${e.message}")
                rechercheEtat.text = getString(R.string.annuaire_injoignable, e.message)
            }
        }
    }

    // ---- Les gestes sur ma liste ----

    /**
     * Un geste : appliqué à ma liste sur place, noté au journal si on est
     * apparié, l'écran relu, et le serveur prévenu dans la foulée s'il répond.
     */
    private fun agir(message: String? = null, apres: (() -> Unit)? = null, surJournal: (Journal) -> Unit, surListe: (MesRadios) -> Unit) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                surListe(maListe)
                if (serveur.apparie) surJournal(journal)
            }
            if (message != null) Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            apres?.invoke()
            charger()
            if (serveur.apparie) tirer()
        }
    }

    /** Le noeud où l'on est, s'il est à moi : « Mes favoris » ou un de mes dossiers. */
    private val noeudAMoi: Noeud?
        get() = if (modeRecherche) null else pile.lastOrNull()?.takeIf { it.mien }

    /** L'étoile : la station entre dans « Mes favoris » ou en sort ; la racine suit. */
    private fun etoiler(station: Station) {
        val etait = maListe.estFavorite(station.uuid)
        agir(
            message = getString(if (etait) R.string.favori_retire else R.string.favori_ajoute, station.nom),
            apres = { adaptateur.rafraichir(station) },
            surJournal = { if (etait) it.favoriRetirer(station.uuid) else it.favoriAjouter(station) },
        ) { if (etait) it.retirerFavori(station.uuid) else it.ajouterFavori(station) }
    }

    /** L'appui long sur une station : les favoris, le rangement, et dans ma liste, le retrait et l'ordre. */
    private fun menuStation(station: Station): Boolean {
        val ici = noeudAMoi
        val dossier = ici?.chemin
        val favorite = maListe.estFavorite(station.uuid)
        val actions = ArrayList<Pair<String, () -> Unit>>()
        actions += getString(if (favorite) R.string.retirer_des_favoris else R.string.ajouter_aux_favoris) to { etoiler(station) }
        actions += getString(R.string.ranger_dans) to {
            choisirUnDossier(R.string.ranger_dans, exclure = dossier) { chemin ->
                agir(
                    message = getString(R.string.rangee_dans, chemin.last(), station.nom),
                    surJournal = { it.dossierAjouter(chemin, station) },
                ) { it.ajouterAuDossier(chemin, station) }
            }
        }
        if (dossier != null) {
            actions += getString(R.string.deplacer_vers) to {
                choisirUnDossier(R.string.deplacer_vers, exclure = dossier) { vers ->
                    agir(
                        message = getString(R.string.rangee_dans, vers.last(), station.nom),
                        surJournal = { it.dossierDeplacer(dossier, station.uuid, vers) },
                    ) { it.deplacer(dossier, station.uuid, vers) }
                }
            }
            actions += getString(R.string.retirer_du_dossier) to {
                agir(surJournal = { it.dossierRetirer(dossier, station.uuid) }) { it.retirerDuDossier(dossier, station.uuid) }
            }
        }
        if (ici != null && (ici.mesFavoris || dossier != null)) {
            val i = ici.stations.indexOfFirst { it.uuid == station.uuid }
            if (i > 0) actions += getString(R.string.monter) to { decaler(ici, i, i - 1) }
            if (i in 0 until ici.stations.size - 1) actions += getString(R.string.descendre) to { decaler(ici, i, i + 1) }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(station.nom)
            .setItems(actions.map { it.first }.toTypedArray()) { _, i -> actions[i].second() }
            .show()
        return true
    }

    /** La station à la place `de` passe à la place `vers` : un geste « ordonner » sur toute la liste. */
    private fun decaler(noeud: Noeud, de: Int, vers: Int) {
        val uuids = noeud.stations.map { it.uuid }.toMutableList()
        uuids.add(vers, uuids.removeAt(de))
        val chemin = noeud.chemin
        if (chemin == null) {
            agir(surJournal = { it.favorisOrdonner(uuids) }) { it.ordonnerFavoris(uuids) }
        } else {
            agir(surJournal = { it.dossierOrdonner(chemin, uuids) }) { it.ordonnerDossier(chemin, uuids) }
        }
    }

    /**
     * Mes dossiers par leur chemin, « Musique / Jazz », et « Nouveau
     * dossier… » en bas, créé dans le dossier où l'on est, sinon à la racine.
     */
    private fun choisirUnDossier(titre: Int, exclure: List<String>?, action: (List<String>) -> Unit) {
        val dossiers = (maListe.charger()?.tousLesDossiers() ?: emptyList())
            .filter { it.chemin != null && it.chemin != exclure }
        val noms = dossiers.map { it.chemin!!.joinToString(" / ") } + getString(R.string.nouveau_dossier)
        MaterialAlertDialogBuilder(this)
            .setTitle(titre)
            .setItems(noms.toTypedArray()) { _, i ->
                if (i < dossiers.size) action(dossiers[i].chemin!!)
                else demanderUnNom(R.string.nouveau_dossier) { nom -> action((noeudAMoi?.chemin ?: emptyList()) + nom) }
            }
            .setNegativeButton(R.string.annuler, null)
            .show()
    }

    /** Un nom de dossier, dans une boîte : vide ou avec une barre oblique, rien ne se passe. */
    private fun demanderUnNom(titre: Int, initial: String = "", action: (String) -> Unit) {
        val champ = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = getString(R.string.nom_du_dossier)
            setText(initial)
            setSelection(text.length)
        }
        val marge = (20 * resources.displayMetrics.density).toInt()
        val cadre = FrameLayout(this).apply { setPadding(marge, 0, marge, 0); addView(champ) }
        MaterialAlertDialogBuilder(this)
            .setTitle(titre)
            .setView(cadre)
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.valider) { _, _ ->
                val nom = champ.text.toString().trim()
                if (nom.isNotEmpty() && !nom.contains('/')) action(nom)
            }
            .show()
        champ.requestFocus()
    }

    // ---- Les podcasts ----

    private fun choisirLOnglet(choix: Onglet) {
        if (onglet == choix) return
        if (modeRecherche) quitterLaRecherche(sansAfficher = true)
        onglet = choix
        afficher()
    }

    /** L'onglet Podcasts : la recherche et ses résultats, ou le podcast ouvert et ses épisodes. */
    private fun afficherLesPodcasts() {
        val p = podcastOuvert
        retour.isEnabled = true
        barre.navigationIcon = if (p == null) null else getDrawable(R.drawable.ic_retour)
        barre.logo = if (p == null) getDrawable(R.drawable.logo) else null
        barre.title = p?.nom ?: getString(R.string.app_name)
        barre.subtitle = if (p == null) sousTitreDeLaRacine() else if (episodes.isEmpty()) getString(R.string.flux_en_cours) else p.auteur
        onglets.visibility = if (p == null) View.VISIBLE else View.GONE
        podcastsPanneau.visibility = if (p == null) View.VISIBLE else View.GONE
        episodesPanneau.visibility = if (p == null) View.GONE else View.VISIBLE
        vide.visibility = View.GONE
        liste.visibility = View.VISIBLE
        when {
            p != null -> montrerLesEpisodes()
            podcastsNom.text.isNullOrBlank() -> {
                // Le champ vide : mes podcasts, dans l'ordre du serveur, avec leur dernier épisode quand il est lu.
                val miens = maListe.charger()?.podcasts ?: emptyList()
                podcastsEtat.text = if (miens.isEmpty()) getString(R.string.aucun_podcast_suivi) else getString(R.string.mes_podcasts)
                adaptateur.montrerPodcasts(miens, derniersEpisodes)
                lireLesDerniersEpisodes(miens)
            }
            else -> adaptateur.montrerPodcasts(podcastsResultats)
        }
    }

    /** Le dernier épisode de chacun de mes podcasts, flux par flux, en arrière-plan, une fois par vie de l'écran. */
    private fun lireLesDerniersEpisodes(miens: List<Podcast>) {
        for (p in miens) {
            if (derniersEpisodes.containsKey(p.flux) || !derniersEnCours.add(p.flux)) continue
            lifecycleScope.launch {
                val titre: String? = try {
                    withContext(Dispatchers.IO) { Flux.lire(p.flux, p).episodes.firstOrNull()?.titre ?: "" }
                } catch (e: Exception) {
                    Log.w(Fichiers.JOURNAL, "podcasts : dernier épisode de ${p.flux} : ${e.message}")
                    null
                }
                derniersEpisodes[p.flux] = titre?.takeIf { it.isNotEmpty() } ?: titre
                derniersEnCours.remove(p.flux)
                if (onglet == Onglet.PODCASTS && podcastOuvert == null && podcastsNom.text.isNullOrBlank()) adaptateur.rafraichir(p)
            }
        }
    }

    private fun programmerLaRecherchePodcasts() {
        if (onglet != Onglet.PODCASTS) return
        minuterie.removeCallbacks(lancerPodcasts)
        minuterie.postDelayed(lancerPodcasts, 400)
    }

    private fun chercherDesPodcasts() {
        if (onglet != Onglet.PODCASTS) return
        val texte = podcastsNom.text.toString().trim()
        val g = ++generationPodcasts
        if (texte.length < 2) {
            podcastsResultats = emptyList()
            if (podcastOuvert == null) afficher()
            if (texte.isNotEmpty()) podcastsEtat.text = getString(R.string.podcasts_tapez)
            return
        }
        podcastsEtat.text = getString(R.string.recherche_en_cours)
        lifecycleScope.launch {
            try {
                val trouves = withContext(Dispatchers.IO) { AnnuairePodcasts.chercher(texte) }
                if (g != generationPodcasts || onglet != Onglet.PODCASTS) return@launch
                podcastsResultats = trouves
                podcastsEtat.text = if (trouves.isEmpty()) getString(R.string.podcasts_rien)
                else resources.getQuantityString(R.plurals.podcasts_nombre, trouves.size, trouves.size)
                if (podcastOuvert == null) { afficher(); liste.scrollToPosition(0) }
            } catch (e: Exception) {
                if (g != generationPodcasts || onglet != Onglet.PODCASTS) return@launch
                Log.w(Fichiers.JOURNAL, "podcasts : recherche : ${e.message}")
                podcastsEtat.text = getString(R.string.podcasts_injoignable, e.message)
            }
        }
    }

    /** Le flux lu sur le téléphone, et, apparié, les positions d'écoute mises d'accord avec le serveur. */
    private fun ouvrirLePodcast(podcast: Podcast, proposerDeSuivre: Boolean = false) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(podcastsNom.windowToken, 0)
        podcastOuvert = podcast
        episodes = emptyList()
        ecoutesDuPodcast = emptyMap()
        toutDeplie = false
        exceptions.clear()
        plisDeRecherche.clear()
        episodesFiltre.setText("")
        afficher()
        lifecycleScope.launch {
            try {
                val lu = withContext(Dispatchers.IO) {
                    val lu = Flux.lire(podcast.flux, podcast)
                    try {
                        ecoutes.synchroniser(podcast.flux, serveur)
                    } catch (e: Exception) {
                        Log.w(Fichiers.JOURNAL, "écoutes : pas mises d'accord avec le serveur : ${e.message}")
                    }
                    lu
                }
                if (podcastOuvert?.flux != podcast.flux) return@launch
                podcastOuvert = lu.podcast
                episodes = lu.episodes
                ecoutesDuPodcast = withContext(Dispatchers.IO) { ecoutes.pour(podcast.flux) }
                if (lu.sansSon) Toast.makeText(this@MainActivity, R.string.flux_sans_son, Toast.LENGTH_LONG).show()
                else if (lu.episodes.isEmpty()) Toast.makeText(this@MainActivity, R.string.aucun_episode, Toast.LENGTH_SHORT).show()
                afficher()
                liste.scrollToPosition(0)
                // Un flux collé à la main, qui a du son et qu'on ne suit pas encore : proposer de le suivre.
                if (proposerDeSuivre && !lu.sansSon && !maListe.estSuivi(lu.podcast.flux)) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(R.string.suivre_ce_podcast)
                        .setMessage(lu.podcast.nom)
                        .setNegativeButton(R.string.pas_maintenant, null)
                        .setPositiveButton(R.string.suivre) { _, _ -> suivre(lu.podcast) }
                        .show()
                }
            } catch (e: Exception) {
                if (podcastOuvert?.flux != podcast.flux) return@launch
                Log.w(Fichiers.JOURNAL, "podcasts : flux ${podcast.flux} : ${e.message}")
                Toast.makeText(this@MainActivity, getString(R.string.flux_injoignable, e.message), Toast.LENGTH_LONG).show()
                fermerLePodcast()
            }
        }
    }

    /** L'étoile d'un podcast : suivi ou plus suivi, un geste comme les favoris. */
    private fun suivre(podcast: Podcast) {
        val etait = maListe.estSuivi(podcast.flux)
        agir(
            message = getString(if (etait) R.string.podcast_retire else R.string.podcast_suivi, podcast.nom),
            apres = { adaptateur.rafraichir(podcast) },
            surJournal = { if (etait) it.podcastRetirer(podcast.flux) else it.podcastAjouter(podcast) },
        ) { if (etait) it.retirerPodcast(podcast.flux) else it.ajouterPodcast(podcast) }
    }

    /** L'appui long sur un podcast : suivre ou non, et dans mes podcasts, monter et descendre. */
    private fun menuPodcast(podcast: Podcast): Boolean {
        val suivi = maListe.estSuivi(podcast.flux)
        val actions = ArrayList<Pair<String, () -> Unit>>()
        actions += getString(if (suivi) R.string.ne_plus_suivre else R.string.suivre) to { suivre(podcast) }
        val miens = maListe.charger()?.podcasts ?: emptyList()
        val i = miens.indexOfFirst { it.flux == podcast.flux }
        if (podcastsNom.text.isNullOrBlank() && i >= 0) {
            fun ordonner(vers: Int) {
                val fluxs = miens.map { it.flux }.toMutableList()
                fluxs.add(vers, fluxs.removeAt(i))
                agir(surJournal = { it.podcastsOrdonner(fluxs) }) { it.ordonnerPodcasts(fluxs) }
            }
            if (i > 0) actions += getString(R.string.monter) to { ordonner(i - 1) }
            if (i < miens.size - 1) actions += getString(R.string.descendre) to { ordonner(i + 1) }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(podcast.nom)
            .setItems(actions.map { it.first }.toTypedArray()) { _, k -> actions[k].second() }
            .show()
        return true
    }

    /**
     * Les épisodes du podcast ouvert, filtrés par le champ de recherche sur
     * le titre et le résumé, sans égard aux accents ni aux majuscules ; les
     * jours sans épisode retenu disparaissent. Tout replié par défaut, tout
     * déplié pendant une recherche. Puis le bouton « tout déplier » se relit :
     * il dit ce que le prochain appui fera, déplier tant qu'un jour est
     * replié, replier quand tout est ouvert ; sans jour, il est éteint.
     */
    private fun montrerLesEpisodes() {
        val filtre = Episode.normaliser(episodesFiltre.text.toString().trim())
        enRecherche = filtre.isNotEmpty()
        val retenus = if (enRecherche) episodes.filter { filtre in it.texteDeRecherche } else episodes
        adaptateur.montrerEpisodes(retenus, ecoutesDuPodcast) { clef ->
            if (enRecherche) clef !in plisDeRecherche else toutDeplie != (clef in exceptions)
        }
        episodesEtat.visibility = if (retenus.isEmpty() && episodes.isNotEmpty()) View.VISIBLE else View.GONE
        episodesEtat.text = getString(R.string.aucun_episode)
        val desJours = adaptateur.jours.isNotEmpty()
        val replier = desJours && adaptateur.joursReplies == 0
        episodesDeplier.isEnabled = desJours
        episodesDeplier.alpha = if (desJours) 1f else 0.38f
        episodesDeplier.setImageResource(if (replier) R.drawable.ic_replier else R.drawable.ic_deplier)
        episodesDeplier.contentDescription = getString(if (replier) R.string.tout_replier else R.string.tout_deplier)
        TooltipCompat.setTooltipText(episodesDeplier, episodesDeplier.contentDescription)
    }

    /** Un appui sur l'en-tête d'un jour le déplie ou le replie, contre le choix courant. */
    private fun basculerLaDate(clef: String) {
        val plis = if (enRecherche) plisDeRecherche else exceptions
        if (!plis.remove(clef)) plis += clef
        montrerLesEpisodes()
    }

    /** Le bouton à gauche du champ : s'il reste un jour replié, tout se déplie ; sinon tout se replie. */
    private fun basculerTousLesJours() {
        val deplier = adaptateur.joursReplies > 0
        if (enRecherche) {
            plisDeRecherche.clear()
            if (!deplier) plisDeRecherche += adaptateur.jours
        } else {
            toutDeplie = deplier
            exceptions.clear()
        }
        montrerLesEpisodes()
    }

    /** L'adresse d'un flux absent de l'annuaire, collée à la main : ouvert tel quel, et proposé à suivre. */
    private fun collerUneAdresse() {
        val champ = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            hint = "https://…/rss"
        }
        val marge = (20 * resources.displayMetrics.density).toInt()
        val cadre = FrameLayout(this).apply { setPadding(marge, 0, marge, 0); addView(champ) }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.adresse_du_flux)
            .setView(cadre)
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.valider) { _, _ ->
                var adresse = champ.text.toString().trim()
                if (adresse.isEmpty()) return@setPositiveButton
                if (!adresse.contains("://")) adresse = "https://$adresse"
                ouvrirLePodcast(Podcast(adresse, adresse, null, null, null), proposerDeSuivre = true)
            }
            .show()
        champ.requestFocus()
    }

    private fun fermerLePodcast() {
        podcastOuvert = null
        episodes = emptyList()
        ecoutesDuPodcast = emptyMap()
        episodesFiltre.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(episodesFiltre.windowToken, 0)
        afficher()
    }

    /** Un épisode seul dans le lecteur, repris où l'on en était s'il n'est pas terminé. */
    private fun jouerLEpisode(episode: Episode) {
        val c = controleur
        if (c == null) {
            Toast.makeText(this, R.string.lecteur_pas_pret, Toast.LENGTH_SHORT).show()
            return
        }
        val reprise = ecoutesDuPodcast[episode.guid]?.takeIf { !it.termine && it.position > 0 }?.position ?: 0L
        c.setMediaItem(episode.versElement(), reprise * 1_000)
        c.prepare()
        c.play()
    }

    private fun sauter(ms: Long) {
        val c = controleur ?: return
        val duree = c.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
        c.seekTo((c.currentPosition + ms).coerceIn(0, duree))
    }

    /** La barre et le temps restant de l'épisode en cours, sauf pendant que le doigt tient la barre. */
    private fun rafraichirAvancement() {
        val c = controleur ?: return
        if (c.currentMediaItem?.estUnEpisode != true) return
        val duree = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        val position = c.currentPosition.coerceAtLeast(0)
        if (barreTenue) return
        lectureBarre.max = (duree / 1_000).toInt().coerceAtLeast(1)
        lectureBarre.progress = (position / 1_000).toInt()
        lectureBarre.isEnabled = duree > 0
        lectureReste.text = if (duree > 0) "-" + formaterDuree((duree - position) / 1_000) else formaterDuree(position / 1_000)
    }

    // ---- La lecture ----

    /** La liste où l'on a appuyé devient la file du lecteur : « suivant » y passe à la station d'après. */
    private fun jouer(station: Station) {
        val c = controleur
        if (c == null) {
            Toast.makeText(this, R.string.lecteur_pas_pret, Toast.LENGTH_SHORT).show()
            return
        }
        val stations = adaptateur.stations
        val index = stations.indexOfFirst { it === station }.coerceAtLeast(0)
        c.setMediaItems(stations.map { it.versElement() }, index, C.TIME_UNSET)
        c.prepare()
        c.play()
    }

    private fun basculerLecture() {
        val c = controleur ?: return
        when {
            c.playerError != null -> { c.prepare(); c.play() }
            c.playWhenReady -> c.pause()
            else -> { if (c.playbackState == Player.STATE_IDLE) c.prepare(); c.play() }
        }
    }

    private fun rafraichirLecture() {
        val c = controleur
        val element = c?.currentMediaItem
        if (c == null || element == null) {
            lecture.visibility = View.GONE
            adaptateur.enCours = null
            return
        }
        lecture.visibility = View.VISIBLE
        val fiche = element.mediaMetadata
        val episode = element.estUnEpisode
        val nom = (fiche.station ?: fiche.title ?: "").toString()
        lectureNom.text = nom
        // Le titre du flux : ce que le lecteur combine, quand il diffère du nom de la station. Pour un épisode, le podcast.
        val titre = if (episode) fiche.artist?.toString() else c.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() && it != nom }
        val erreur = c.playerError
        lectureEtat.text = when {
            erreur != null -> getString(R.string.injoignable, erreur.errorCodeName.removePrefix("ERROR_CODE_"))
            c.playbackState == Player.STATE_BUFFERING -> getString(R.string.connexion)
            c.isPlaying -> titre ?: getString(R.string.en_direct)
            c.playbackState == Player.STATE_READY -> if (episode) titre ?: getString(R.string.en_pause) else getString(R.string.en_pause)
            c.playbackState == Player.STATE_ENDED -> titre ?: ""
            else -> ""
        }
        // Pour un épisode, la barre d'avancement, rafraîchie chaque seconde tant que l'écran est là.
        lectureAvancement.visibility = if (episode) View.VISIBLE else View.GONE
        minuterie.removeCallbacks(avancement)
        if (episode) { rafraichirAvancement(); minuterie.postDelayed(avancement, 1_000) }
        val enLecture = c.playWhenReady && erreur == null
        lectureBouton.setImageResource(if (enLecture) R.drawable.ic_pause else R.drawable.ic_lecture)
        lectureBouton.contentDescription = getString(if (enLecture) R.string.pause else R.string.lire)

        lectureInitiales.text = nom.split(Regex("\\s+")).filter { it.isNotEmpty() }
            .take(2).joinToString("") { it.first().uppercaseChar().toString() }
        lectureInitiales.visibility = View.VISIBLE
        val logo = fiche.artworkUri
        if (logo != null) {
            lectureLogo.load(logo) { listener(onSuccess = { _, _ -> lectureInitiales.visibility = View.GONE }) }
        } else {
            lectureLogo.setImageDrawable(null)
        }
        adaptateur.enCours = element.mediaId
    }
}
