package org.unicornradio.app

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
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
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.unicornradio.app.annuaire.Annuaire
import org.unicornradio.app.annuaire.Retrouver
import org.unicornradio.app.fichier.EcritureFichier
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.lecture.Extinction
import org.unicornradio.app.lecture.Precedents
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
import org.unicornradio.app.podcasts.FileDEcoute
import org.unicornradio.app.podcasts.Flux
import org.unicornradio.app.podcasts.Podcast
import org.unicornradio.app.podcasts.Telechargements
import org.unicornradio.app.podcasts.episode
import org.unicornradio.app.podcasts.estUnEpisode
import org.unicornradio.app.podcasts.formaterDuree
import org.unicornradio.app.podcasts.versElement
import org.unicornradio.app.serveur.ClientServeur
import org.unicornradio.app.serveur.Journal
import org.unicornradio.app.serveur.MesRadios
import org.unicornradio.app.serveur.Reseau
import org.unicornradio.app.serveur.Serveur
import org.unicornradio.app.serveur.Synchro
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * L'écran unique : la liste du noeud courant (favoris, dossiers, stations)
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
    private lateinit var lecturePrecedent: ImageButton
    private lateinit var lectureSuivant: ImageButton
    private lateinit var lectureMinuterie: View
    private lateinit var lectureMinuterieIcone: ImageView
    private lateinit var lectureMinuterieEtat: TextView
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
    private lateinit var fileDEcoute: FileDEcoute
    private lateinit var telechargements: Telechargements
    private lateinit var adaptateur: ListeAdaptateur

    /** Le chemin parcouru, la racine en premier. Vide tant que la liste est vide. */
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

    // L'onglet Podcasts : la recherche dans l'annuaire, le podcast ouvert et ses épisodes, où l'on en est de chacun, la liste de lecture.
    private enum class Onglet { RADIO, PODCASTS }
    private var onglet = Onglet.RADIO
    private var podcastsResultats: List<Podcast> = emptyList()
    private var podcastOuvert: Podcast? = null
    private var episodes: List<Episode> = emptyList()
    private var ecoutesDuPodcast: Map<String, Ecoute> = emptyMap()
    private var generationPodcasts = 0
    private val lancerPodcasts = Runnable { chercherDesPodcasts() }
    private var barreTenue = false
    /** La liste de lecture ouverte, sous l'onglet Podcasts, à la place de mes podcasts. */
    private var fileOuverte = false
    /** Les clefs des épisodes à suivre et l'état des téléchargements, relus avant chaque affichage, pour les lignes. */
    private var aSuivreClefs: Set<String> = emptySet()
    private var sortsDeTelechargement: Map<String, Telechargements.Sort> = emptyMap()
    /**
     * Les plis des jours du podcast ouvert, rien n'est gardé d'un podcast à
     * l'autre. Hors recherche : tout replié à l'ouverture, « tout déplier »
     * retient le choix le temps du podcast, et `exceptions` liste les jours
     * pliés ou dépliés à la main contre ce choix. Pendant une recherche, les
     * jours trouvés sont dépliés, et `plisDeRecherche` ceux repliés là, le
     * temps de la recherche seulement.
     */
    private var toutDeplie = false

    /** Au lancement, l'écran s'ouvre dans « Mes favoris » quand il existe ; une fois, au premier chargement. */
    private var ouvrirLesFavoris = false
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
    /** Les minutes qui restent à la minuterie d'extinction, relues toutes les trente secondes tant qu'elle court. */
    private val ticMinuterie = object : Runnable {
        override fun run() {
            rafraichirLaMinuterie()
            if (Extinction.active) minuterie.postDelayed(this, 30_000)
        }
    }

    // La mise à jour proposée : une vérification à la fois, le téléchargement s'il est lancé.
    private val miseAJour by lazy { MiseAJour(this) }
    private var installation: Installation? = null
    private var verificationEnCours = false
    private var propositionOuverte = false

    // Le serveur apparié : un tirage à la fois, et la barre dit s'il ne répond pas, après deux tirages manqués de suite.
    private var tirageEnCours = false
    private var tirageARefaire = false
    private var tiragesManques = 0
    private val serveurInjoignable: Boolean
        get() = tiragesManques >= 2

    private var controleurFutur: ListenableFuture<MediaController>? = null
    private var controleur: MediaController? = null
    private var recepteurTelechargements: BroadcastReceiver? = null

    private val retour = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                modeRecherche -> quitterLaRecherche()
                onglet == Onglet.PODCASTS && podcastOuvert != null -> fermerLePodcast()
                onglet == Onglet.PODCASTS && fileOuverte -> fermerLaFile()
                onglet == Onglet.PODCASTS -> onglets.check(R.id.ongletRadio)
                else -> { pile.removeLast(); afficher() }
            }
        }
    }

    private val choisirUnFichier = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importer(uri)
    }

    private val creerLExport = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) ecrireLExport(uri)
    }
    private val sauvegarde by lazy { org.unicornradio.app.fichier.Sauvegarde(this) }
    private val creerLaSauvegarde = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            sauvegarde.regler(uri)
            sauvegarder(aLaMain = true)
        }
    }

    private val demanderNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Au retour de l'écran Serveur, quoi qu'il s'y soit passé : relire, et `onStart` tire. */
    private val ouvrirServeur = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        tiragesManques = 0
        pile.clear()
        charger()
    }

    private val surLecteur = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            rafraichirLecture()
            // Un épisode qui démarre : la liste de lecture a bougé, et son compte en tête de mes podcasts.
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) && onglet == Onglet.PODCASTS && podcastOuvert == null && !modeRecherche) afficher()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ouvrirLesFavoris = savedInstanceState == null
        setContentView(R.layout.activity_main)
        fichiers = Fichiers(this)
        serveur = Serveur(this)
        mesRadios = MesRadios(this, MesRadios.COPIE)
        locale = MesRadios(this, MesRadios.LOCALE)
        journal = Journal(this)
        synchro = Synchro(serveur, mesRadios, journal)
        Reseau.surveiller(this)

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
        // Le défilement d'un titre trop long ne part que sur une vue « sélectionnée ».
        lectureEtat.isSelected = true
        lectureBouton = findViewById(R.id.lectureBouton)
        lecturePrecedent = findViewById(R.id.lecturePrecedent)
        lectureSuivant = findViewById(R.id.lectureSuivant)
        lectureMinuterie = findViewById(R.id.lectureMinuterie)
        lectureMinuterieIcone = findViewById(R.id.lectureMinuterieIcone)
        lectureMinuterieEtat = findViewById(R.id.lectureMinuterieEtat)
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
        fileDEcoute = FileDEcoute(this)
        telechargements = Telechargements(this)

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
            surListe = { ouvrirLaFile() },
            surDate = { basculerLaDate(it) },
            surEpisode = { jouerLEpisode(it) },
            surEpisodeLong = { menuEpisode(it) },
            etiquettes = { e ->
                listOfNotNull(
                    getString(R.string.a_la_suite).takeIf { e.clef in aSuivreClefs },
                    when (sortsDeTelechargement[e.clef]) {
                        Telechargements.Sort.FINI -> getString(R.string.telecharge)
                        Telechargements.Sort.EN_COURS -> getString(R.string.telechargement_en_cours)
                        else -> null
                    },
                )
            },
        )
        liste.layoutManager = LinearLayoutManager(this)
        liste.adapter = adaptateur

        barre.inflateMenu(R.menu.principal)
        // Les trois points du menu, de la même teinte que les autres icônes de la barre.
        barre.overflowIcon?.setTint(getColor(R.color.icone_barre))
        barre.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.importer, R.id.importerMenu -> { choisirUnFichier.launch(arrayOf("application/json", "application/octet-stream", "text/plain")); true }
                R.id.exporter -> { exporter(); true }
                R.id.chercher -> { entrerDansLaRecherche(); true }
                R.id.serveur -> { ouvrirServeur.launch(Intent(this, ServeurActivity::class.java)); true }
                R.id.viderLaListe -> { proposerDeViderLaListe(); true }
                R.id.sauvegarde -> { montrerLaSauvegarde(); true }
                R.id.invitation -> { montrerLInvitation(); true }
                R.id.miseAJour -> { verifierLaMiseAJour(aLaMain = true); true }
                R.id.aPropos -> { montrerLAPropos(); true }
                else -> false
            }
        }
        barre.setNavigationOnClickListener { retour.handleOnBackPressed() }
        poserLesDrapeaux()
        videBouton.setOnClickListener { choisirUnFichier.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
        findViewById<View>(R.id.videChercher).setOnClickListener { entrerDansLaRecherche() }
        findViewById<View>(R.id.videServeur).setOnClickListener { ouvrirServeur.launch(Intent(this, ServeurActivity::class.java)) }
        lectureBouton.setOnClickListener { basculerLecture() }
        // L'élément d'avant ou d'après dans la file du lecteur : la liste de lecture pour un épisode, la liste d'où l'on vient pour une station.
        lecturePrecedent.setOnClickListener { controleur?.let { c -> if (c.hasPreviousMediaItem()) { c.seekToPreviousMediaItem(); c.play() } } }
        lectureSuivant.setOnClickListener { controleur?.let { c -> if (c.hasNextMediaItem()) { c.seekToNextMediaItem(); c.play() } } }
        lectureMinuterie.setOnClickListener { choisirLaMinuterie() }
        TooltipCompat.setTooltipText(lectureMinuterie, getString(R.string.minuterie))
        findViewById<View>(R.id.lectureTextes).setOnClickListener { surLaLigneDeLecture() }
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

        // Android dit quand un téléchargement finit : s'il est à nous, les lignes se relisent.
        recepteurTelechargements = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val id = i?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
                if (id < 0) return
                lifecycleScope.launch {
                    val notre = withContext(Dispatchers.IO) { telechargements.terminer(id) }
                    if (notre && onglet == Onglet.PODCASTS && !modeRecherche) afficher()
                }
            }
        }.also { ContextCompat.registerReceiver(this, it, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED) }

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

    /** « Ouvrir avec » : l'intention porte le fichier, à verser dans ma liste. */
    private fun traiter(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            intent.data = null
            importer(uri)
        }
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
        // Les téléchargements finis pendant qu'on n'était pas là.
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { telechargements.rafraichir() }
            if (onglet == Onglet.PODCASTS && !modeRecherche) afficher()
        }
        // Le service a remplacé une fiche que l'annuaire avait refaite : les listes se relisent, et le serveur l'apprend.
        Retrouver.surRemplacement = {
            charger()
            if (serveur.apparie) tirer()
        }
        tirer()
        verifierLaMiseAJour(aLaMain = false)
    }

    override fun onDestroy() {
        installation?.fermer()
        installation = null
        recepteurTelechargements?.let { unregisterReceiver(it) }
        recepteurTelechargements = null
        Reseau.laisser()
        super.onDestroy()
    }

    override fun onStop() {
        // Ce qui ne repasse pas par le rechargement de la liste, un podcast suivi par exemple, est sauvegardé en partant.
        sauvegarder(aLaMain = false)
        Retrouver.surRemplacement = null
        minuterie.removeCallbacks(avancement)
        minuterie.removeCallbacks(ticMinuterie)
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
     * reste et la barre le dit ; révoqué, on repasse en mode local et on
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

    // ---- Ma liste, et les fichiers ----

    /**
     * Relit ma liste, la copie du serveur ou la liste locale : « Mes
     * favoris » en tête, puis les dossiers, sous le nom de l'application. Le
     * chemin où l'on était est retrouvé par ses titres. Les fichiers que les
     * versions d'avant gardaient à côté sont d'abord versés dans la liste,
     * une fois.
     */
    private fun charger() {
        val chemin = pile.map { it.titre }
        lifecycleScope.launch {
            val verses = withContext(Dispatchers.IO) { verserLesAnciensFichiers() }
            if (verses > 0) Toast.makeText(this@MainActivity, getString(R.string.fichiers_verses, verses), Toast.LENGTH_LONG).show()
            val racines = withContext(Dispatchers.IO) {
                val mienne = maListe.charger()
                val liste = ArrayList<Noeud>()
                if (mienne != null) {
                    // Apparié, les favoris sont là même vides ; en local, seulement s'il y en a.
                    if (serveur.apparie || mienne.stations.isNotEmpty()) {
                        liste += Noeud(getString(R.string.mes_favoris), emptyList(), mienne.stations, mesFavoris = true, mien = true)
                    }
                    liste += mienne.sousDossiers.map { it.aMoi() }
                }
                liste
            }
            pile.clear()
            if (racines.isNotEmpty()) {
                pile.addLast(Noeud(getString(R.string.app_name), racines, emptyList()))
                for (titre in chemin.drop(1)) {
                    val suivant = pile.last().sousDossiers.firstOrNull { it.titre == titre } ?: break
                    pile.addLast(suivant)
                }
                if (ouvrirLesFavoris && pile.size == 1) racines.firstOrNull { it.mesFavoris }?.let { pile.addLast(it) }
            }
            ouvrirLesFavoris = false
            if (!modeRecherche) afficher()
            if (verses > 0 && serveur.apparie) tirer()
            sauvegarder(aLaMain = false)
        }
    }

    // ---- La sauvegarde automatique ----

    /**
     * Ma liste écrite à l'emplacement choisi, si elle a changé ; à chaque
     * rechargement de la liste, donc après chaque geste, import ou
     * synchronisation, et en quittant l'écran. Sans emplacement et sans
     * serveur, un rappel discret, une seule fois, dès que la liste existe.
     */
    private fun sauvegarder(aLaMain: Boolean) {
        lifecycleScope.launch {
            if (!sauvegarde.reglee) {
                if (!serveur.apparie && withContext(Dispatchers.IO) { maListe.charger()?.let { it.nombreDeStations > 0 || it.podcasts.isNotEmpty() } == true } &&
                    sauvegarde.rappelAFaire()
                ) Toast.makeText(this@MainActivity, R.string.sauvegarde_rappel, Toast.LENGTH_LONG).show()
                return@launch
            }
            when (withContext(Dispatchers.IO) { sauvegarde.ecrire(maListe.section(), force = aLaMain) }) {
                org.unicornradio.app.fichier.Sauvegarde.Issue.ECRITE ->
                    if (aLaMain) Toast.makeText(this@MainActivity, getString(R.string.sauvegarde_faite, sauvegarde.nom), Toast.LENGTH_LONG).show()
                org.unicornradio.app.fichier.Sauvegarde.Issue.EN_PANNE ->
                    Toast.makeText(this@MainActivity, getString(R.string.sauvegarde_en_panne, sauvegarde.nom), Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    /** « Sauvegarde automatique… » : choisir l'emplacement, ou voir où et quand, sauvegarder, changer, arrêter. */
    private fun montrerLaSauvegarde() {
        val boite = MaterialAlertDialogBuilder(this).setTitle(R.string.sauvegarde_automatique)
        if (!sauvegarde.reglee) {
            boite.setMessage(R.string.sauvegarde_explication)
                .setNegativeButton(R.string.invitation_fermer, null)
                .setPositiveButton(R.string.sauvegarde_choisir) { _, _ -> creerLaSauvegarde.launch("unicorn-radio-sauvegarde.json") }
                .show()
            return
        }
        val quand = if (sauvegarde.le > 0) java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(Date(sauvegarde.le))
        else getString(R.string.sauvegarde_jamais)
        val texte = getString(R.string.sauvegarde_etat, sauvegarde.nom, quand) +
            if (sauvegarde.enPanne) "\n\n" + getString(R.string.sauvegarde_a_regler) else ""
        boite.setMessage(texte)
            .setPositiveButton(R.string.sauvegarde_maintenant) { _, _ -> sauvegarder(aLaMain = true) }
            .setNegativeButton(R.string.sauvegarde_changer) { _, _ -> creerLaSauvegarde.launch("unicorn-radio-sauvegarde.json") }
            .setNeutralButton(R.string.sauvegarde_arreter) { _, _ ->
                sauvegarde.arreter()
                Toast.makeText(this, R.string.sauvegarde_arretee, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /** Les fichiers gardés par les versions d'avant la 1.1.0 : versés dans ma liste comme autant de gestes, puis oubliés. Rend les stations ajoutées. */
    private fun verserLesAnciensFichiers(): Int {
        val anciens = fichiers.anciens()
        if (anciens.isEmpty()) return 0
        var total = 0
        for (racine in anciens) {
            val (stations, _) = maListe.verser(racine, journal.takeIf { serveur.apparie })
            total += stations
            racine.fichier?.let { fichiers.oublier(it) }
        }
        Log.i(Fichiers.JOURNAL, "${anciens.size} anciens fichiers versés dans ma liste : $total stations ajoutées")
        return total
    }

    /**
     * Importer une liste : un fichier d'export, le mien ou celui de
     * quelqu'un, versé dans ma liste comme autant de gestes, rien d'effacé,
     * rien doublé. Apparié, les gestes partent au serveur dans la foulée.
     */
    private fun importer(uri: Uri) {
        lifecycleScope.launch {
            try {
                val (stations, podcasts) = withContext(Dispatchers.IO) {
                    val racine = fichiers.lire(uri)
                    maListe.verser(racine, journal.takeIf { serveur.apparie })
                }
                Toast.makeText(this@MainActivity, getString(R.string.liste_importee, stations, podcasts), Toast.LENGTH_LONG).show()
                if (modeRecherche) quitterLaRecherche(sansAfficher = true)
                charger()
                if (serveur.apparie) tirer()
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "import refusé : ${e.message}")
                val raison = if (e is LectureFichier.FichierIllisible) e.message else e.javaClass.simpleName
                Toast.makeText(this@MainActivity, getString(R.string.fichier_illisible, raison), Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Exporter mes radios : le fichier d'échange, favoris, dossiers et podcasts, là où l'on veut, nommé comme sur le bureau. */
    private fun exporter() {
        val racine = maListe.charger()
        if (racine == null || (racine.nombreDeStations == 0 && racine.podcasts.isEmpty())) {
            Toast.makeText(this, R.string.rien_a_exporter, Toast.LENGTH_SHORT).show()
            return
        }
        val jour = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        creerLExport.launch("unicorn-radios-$jour.json")
    }

    private fun ecrireLExport(uri: Uri) {
        lifecycleScope.launch {
            try {
                val n = withContext(Dispatchers.IO) {
                    val section = maListe.section() ?: throw IOException("rien à exporter")
                    val texte = EcritureFichier.enveloppe(section)
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(texte.toByteArray(Charsets.UTF_8)) }
                        ?: throw IOException("impossible d'écrire")
                    maListe.charger()?.nombreDeStations ?: 0
                }
                Toast.makeText(this@MainActivity, getString(R.string.fichier_exporte, n), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "export : ${e.message}")
                Toast.makeText(this@MainActivity, getString(R.string.export_echoue, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    /** L'appui long sur un de mes dossiers : son menu. */
    private fun menuDossier(noeud: Noeud): Boolean {
        val chemin = noeud.chemin
        if (!noeud.mien || chemin == null) return false
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

    // ---- La mise à jour proposée ----

    /**
     * La dernière release du dépôt GitHub, comparée à la version installée.
     * D'elle-même à chaque retour au premier plan, sans un mot si rien n'est
     * plus récent, si GitHub ne répond pas, ou si cette version a été écartée
     * par « Plus tard » il y a moins d'un jour ; à la main depuis le menu,
     * avec la réponse dans tous les cas. Une seule boîte à la fois.
     */
    private fun verifierLaMiseAJour(aLaMain: Boolean) {
        if (verificationEnCours || propositionOuverte) return
        verificationEnCours = true
        lifecycleScope.launch {
            try {
                val derniere = withContext(Dispatchers.IO) { miseAJour.derniere() }
                if (derniere != null && MiseAJour.plusRecente(derniere.numero, BuildConfig.VERSION_NAME)) {
                    if (aLaMain || !miseAJour.estEcartee(derniere.numero)) proposerLaVersion(derniere)
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

    /** La note dans la langue de l'application, et deux boutons : installer, ou plus tard, c'est-à-dire demain pour cette version. */
    private fun proposerLaVersion(version: MiseAJour.Version) {
        val langue = resources.configuration.locales[0].language
        propositionOuverte = true
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.version_disponible, version.numero))
            .setMessage(MiseAJour.noteDans(version.note, langue))
            .setNegativeButton(R.string.plus_tard) { _, _ -> miseAJour.ecarter(version.numero) }
            .setPositiveButton(R.string.installer) { _, _ -> installerLaVersion(version) }
            .setOnDismissListener { propositionOuverte = false }
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
        // La loupe, l'import et l'export sont de l'onglet Radio ; vider la liste, de la liste de lecture ouverte.
        barre.menu.findItem(R.id.importer).isVisible = onglet == Onglet.RADIO
        barre.menu.findItem(R.id.importerMenu).isVisible = onglet == Onglet.RADIO
        barre.menu.findItem(R.id.exporter).isVisible = onglet == Onglet.RADIO
        barre.menu.findItem(R.id.chercher).isVisible = onglet == Onglet.RADIO
        barre.menu.findItem(R.id.viderLaListe).isVisible = !modeRecherche && onglet == Onglet.PODCASTS && podcastOuvert == null && fileOuverte
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

    /** L'appui long sur une station : les favoris, le rangement, dans ma liste le retrait et l'ordre, le site et l'adresse du flux. */
    private fun menuStation(station: Station): Boolean {
        val ici = noeudAMoi
        val dossier = ici?.chemin
        val favorite = maListe.estFavorite(station.uuid)
        val actions = ArrayList<Pair<String, () -> Unit>>()
        actions += getString(if (favorite) R.string.retirer_des_favoris else R.string.ajouter_aux_favoris) to { etoiler(station) }
        actions += getString(R.string.ranger_dans) to {
            choisirUnDossier(R.string.ranger_dans, station) { chemin ->
                agir(
                    message = getString(R.string.rangee_dans, chemin.last(), station.nom),
                    surJournal = { it.dossierAjouter(chemin, station) },
                ) { it.ajouterAuDossier(chemin, station) }
            }
        }
        if (dossier != null) {
            actions += getString(R.string.deplacer_vers) to {
                choisirUnDossier(R.string.deplacer_vers, station) { vers ->
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
        // Le site de la station, et l'adresse de son flux pour l'écouter ailleurs ou la donner, comme sur le bureau.
        station.page?.takeIf { it.isNotBlank() }?.let { page -> actions += getString(R.string.ouvrir_le_site) to { ouvrirLaPage(page) } }
        actions += getString(R.string.copier_l_adresse) to { copier(station.url) }
        // Une station de mes listes que l'annuaire a refaite : la retrouver par son nom et choisir laquelle c'est.
        if (ici != null && (ici.mesFavoris || dossier != null)) actions += getString(R.string.retrouver_dans_l_annuaire) to { retrouver(station) }
        MaterialAlertDialogBuilder(this)
            .setTitle(station.nom)
            .setItems(actions.map { it.first }.toTypedArray()) { _, i -> actions[i].second() }
            .show()
        return true
    }

    /**
     * Les stations de l'annuaire qui portent ce nom, l'exact d'abord, chacune
     * avec son pays, son codec et son débit pour les distinguer ; celle qu'on
     * choisit remplace la fiche partout, favoris et dossiers.
     */
    private fun retrouver(station: Station) {
        lifecycleScope.launch {
            val candidates = try {
                withContext(Dispatchers.IO) { Retrouver.candidates(station.nom) }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.annuaire_injoignable, e.message), Toast.LENGTH_LONG).show()
                return@launch
            }
            if (candidates.isEmpty()) {
                Toast.makeText(this@MainActivity, getString(R.string.retrouver_rien, station.nom), Toast.LENGTH_LONG).show()
                return@launch
            }
            val lignes = candidates.map { c ->
                val details = listOfNotNull(c.pays ?: c.paysCode, c.codec?.let { k -> c.debit?.let { "$k $it kb/s" } ?: k })
                (if (c.uuid == station.uuid) "✓ " else "") + c.nom + if (details.isEmpty()) "" else "\n" + details.joinToString(" · ")
            }
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.retrouver_titre, station.nom))
                .setItems(lignes.toTypedArray()) { _, i -> remplacer(station, candidates[i]) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    /** La nouvelle fiche à la place de l'ancienne partout ; si c'est la station en cours, la lecture repart avec elle. */
    private fun remplacer(ancienne: Station, nouvelle: Station) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { Retrouver.appliquer(this@MainActivity, ancienne.uuid, nouvelle) }
            Toast.makeText(this@MainActivity, getString(R.string.station_remplacee, ancienne.nom, nouvelle.nom), Toast.LENGTH_LONG).show()
            val c = controleur ?: return@launch
            val i = (0 until c.mediaItemCount).firstOrNull { c.getMediaItemAt(it).mediaId == ancienne.uuid } ?: return@launch
            c.replaceMediaItem(i, nouvelle.versElement())
            if (i == c.currentMediaItemIndex) {
                c.prepare()
                c.play()
            }
        }
    }

    /**
     * « Invitation QRCode » : l'adresse de la dernière version, en QR code à
     * faire scanner par quelqu'un qu'on croise, sur fond blanc pour se lire
     * en thème sombre aussi ; « Envoyer le lien » pour qui n'est pas en face.
     */
    private fun montrerLInvitation() {
        val adresse = "https://github.com/Lestat2Lioncourt/unicorn-radio/releases/latest"
        val cote = (resources.displayMetrics.density * 260).toInt()
        val code = try {
            com.journeyapps.barcodescanner.BarcodeEncoder().encodeBitmap(adresse, com.google.zxing.BarcodeFormat.QR_CODE, cote, cote)
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "invitation : QR code impossible : ${e.message}")
            null
        }
        val marge = (resources.displayMetrics.density * 16).toInt()
        val image = ImageView(this).apply {
            setImageBitmap(code)
            setPadding(marge, marge, marge, 0)
            adjustViewBounds = true
        }
        val cadre = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(image)
            addView(ligneDesTelechargements())
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.invitation_qrcode)
            .setMessage(getString(R.string.invitation_texte, adresse))
            .setView(cadre)
            .setNegativeButton(R.string.invitation_fermer, null)
            .setPositiveButton(R.string.envoyer_le_lien) { _, _ ->
                val envoi = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, getString(R.string.invitation_message, adresse))
                startActivity(Intent.createChooser(envoi, getString(R.string.envoyer_le_lien)))
            }
            .show()
    }

    /**
     * « Déjà téléchargée N fois » : une ligne cachée, montrée quand GitHub a
     * donné le total ; sans réseau, elle reste cachée. Pour l'invitation et « À propos ».
     */
    private fun ligneDesTelechargements(): TextView {
        val marge = (resources.displayMetrics.density * 16).toInt()
        val compte = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(marge, marge / 2, marge, 0)
            visibility = View.GONE
        }
        lifecycleScope.launch {
            val total = withContext(Dispatchers.IO) {
                try { org.unicornradio.app.miseajour.MiseAJour(this@MainActivity).telechargements() } catch (e: Exception) { null }
            }
            if (total != null && total > 0) {
                compte.text = resources.getQuantityString(R.plurals.invitation_telechargements, total, total)
                compte.visibility = View.VISIBLE
            }
        }
        return compte
    }

    /** « À propos » : ce que fait l'application, sans compte, sans collecte, à qui elle parle, la licence, le compteur. */
    private fun montrerLAPropos() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME)
            .setMessage(R.string.a_propos_texte)
            .setView(ligneDesTelechargements())
            .setNegativeButton(R.string.invitation_fermer, null)
            .setPositiveButton(R.string.code_source) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lestat2Lioncourt/unicorn-radio")))
                } catch (e: Exception) {
                    Log.w(Fichiers.JOURNAL, "à propos : pas de navigateur : ${e.message}")
                }
            }
            .show()
    }

    /** Dans le presse-papiers ; depuis Android 13, le système le dit lui-même. */
    private fun copier(texte: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Unicorn-Radio", texte))
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(this, R.string.adresse_copiee, Toast.LENGTH_SHORT).show()
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
    /**
     * Tous mes dossiers, pour ranger ou déplacer une station : ceux qui la
     * contiennent déjà, celui où l'on est compris, en gris et sans effet.
     */
    private fun choisirUnDossier(titre: Int, station: Station, action: (List<String>) -> Unit) {
        val dossiers = (maListe.charger()?.tousLesDossiers() ?: emptyList()).filter { it.chemin != null }
        val dejaLa = dossiers.map { d -> d.stations.any { it.uuid == station.uuid } }
        val noms = dossiers.map { it.chemin!!.joinToString(" / ") } + getString(R.string.nouveau_dossier)
        val liste = object : ArrayAdapter<String>(this, android.R.layout.select_dialog_item, noms) {
            override fun areAllItemsEnabled() = false
            override fun isEnabled(position: Int) = !(dejaLa.getOrNull(position) ?: false)
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                super.getView(position, convertView, parent).apply { alpha = if (isEnabled(position)) 1f else 0.38f }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(titre)
            .setAdapter(liste) { _, i ->
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

    /** L'onglet Podcasts : la recherche et ses résultats, le podcast ouvert et ses épisodes, ou la liste de lecture. */
    private fun afficherLesPodcasts() {
        val p = podcastOuvert
        val file = p == null && fileOuverte
        val accueil = p == null && !file
        retour.isEnabled = true
        barre.navigationIcon = if (accueil) null else getDrawable(R.drawable.ic_retour)
        barre.logo = if (accueil) getDrawable(R.drawable.logo) else null
        barre.title = when {
            p != null -> p.nom
            file -> getString(R.string.liste_de_lecture)
            else -> getString(R.string.app_name)
        }
        barre.subtitle = when {
            p != null -> if (episodes.isEmpty()) getString(R.string.flux_en_cours) else p.auteur
            file -> null
            else -> sousTitreDeLaRacine()
        }
        onglets.visibility = if (accueil) View.VISIBLE else View.GONE
        podcastsPanneau.visibility = if (accueil) View.VISIBLE else View.GONE
        episodesPanneau.visibility = if (p == null) View.GONE else View.VISIBLE
        vide.visibility = View.GONE
        liste.visibility = View.VISIBLE
        when {
            p != null -> montrerLesEpisodes()
            file -> montrerLaFile()
            podcastsNom.text.isNullOrBlank() -> {
                // Le champ vide : la liste de lecture en tête, puis mes podcasts, dans l'ordre du serveur, avec leur dernier épisode quand il est lu.
                val miens = maListe.charger()?.podcasts ?: emptyList()
                podcastsEtat.text = if (miens.isEmpty()) getString(R.string.aucun_podcast_suivi) else getString(R.string.mes_podcasts)
                adaptateur.montrerPodcasts(miens, derniersEpisodes, liste = fileDEcoute.nombre)
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
                if (onglet == Onglet.PODCASTS && podcastOuvert == null && !fileOuverte && podcastsNom.text.isNullOrBlank()) adaptateur.rafraichir(p)
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
        relireLesMarques()
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

    /** Ce que les lignes d'épisodes disent en plus : à la suite, téléchargé. */
    private fun relireLesMarques() {
        aSuivreClefs = fileDEcoute.clefsASuivre()
        sortsDeTelechargement = telechargements.tous()
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

    // ---- La liste de lecture ----

    /** La liste de lecture, depuis mes podcasts ou depuis la ligne de lecture, d'où qu'on vienne. */
    private fun ouvrirLaFile() {
        if (modeRecherche) quitterLaRecherche(sansAfficher = true)
        if (podcastOuvert != null) {
            podcastOuvert = null
            episodes = emptyList()
            ecoutesDuPodcast = emptyMap()
            episodesFiltre.setText("")
        }
        fileOuverte = true
        if (onglet != Onglet.PODCASTS) {
            onglet = Onglet.PODCASTS
            onglets.check(R.id.ongletPodcasts)
        }
        afficher()
    }

    private fun fermerLaFile() {
        fileOuverte = false
        afficher()
    }

    /** Les passés, atténués, l'épisode en cours, ceux à suivre ; où l'on en est de chacun, flux par flux. */
    private fun montrerLaFile() {
        val enCours = controleur?.currentMediaItem?.takeIf { it.estUnEpisode }?.episode
        val passes = fileDEcoute.passes().filter { it.clef != enCours?.clef }
        val suite = fileDEcoute.aSuivre()
        relireLesMarques()
        val cache = HashMap<String, Map<String, Ecoute>>()
        adaptateur.montrerLaFile(
            passes, enCours, suite,
            ecoute = { e -> cache.getOrPut(e.flux) { ecoutes.pour(e.flux) }[e.guid] },
            titres = Triple(getString(R.string.precedents), getString(R.string.en_cours), getString(R.string.a_suivre)),
            vide = getString(R.string.liste_vide),
        )
    }

    /** « Vider la liste » : tout, l'épisode en cours compris, arrêté d'abord, comme sur le bureau. */
    private fun proposerDeViderLaListe() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vider_la_liste)
            .setMessage(R.string.vider_la_liste_texte)
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.vider_la_liste) { _, _ ->
                controleur?.let { c -> if (c.currentMediaItem?.estUnEpisode == true) { c.stop(); c.clearMediaItems() } }
                fileDEcoute.vider()
                afficher()
            }
            .show()
    }

    /** L'appui long sur un épisode : lire, à la suite ou retiré, téléchargé ou son téléchargement supprimé. */
    private fun menuEpisode(episode: Episode): Boolean {
        val actions = ArrayList<Pair<String, () -> Unit>>()
        actions += getString(R.string.lire) to { jouerLEpisode(episode) }
        when {
            fileOuverte && podcastOuvert == null -> actions += getString(R.string.retirer_de_la_liste) to { retirerDeLaListe(episode) }
            episode.clef in aSuivreClefs -> actions += getString(R.string.retirer_de_la_file) to { retirerDeLaListe(episode) }
            else -> actions += getString(R.string.lire_ensuite) to { lireEnsuite(episode) }
        }
        when (sortsDeTelechargement[episode.clef]) {
            Telechargements.Sort.FINI, Telechargements.Sort.EN_COURS ->
                actions += getString(R.string.supprimer_le_telechargement) to { supprimerLeTelechargement(episode) }
            else -> actions += getString(R.string.telecharger) to { telecharger(episode) }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(episode.titre)
            .setItems(actions.map { it.first }.toTypedArray()) { _, i -> actions[i].second() }
            .show()
        return true
    }

    /** À la suite de ce qui joue : dans la liste gardée, et dans la file du lecteur si un épisode joue déjà. */
    private fun lireEnsuite(episode: Episode) {
        if (fileDEcoute.ajouter(episode)) {
            controleur?.let { c -> if (c.currentMediaItem?.estUnEpisode == true) c.addMediaItem(elementDe(episode)) }
            Toast.makeText(this, getString(R.string.mis_a_la_suite, episode.titre), Toast.LENGTH_SHORT).show()
        }
        afficher()
    }

    /** Retiré de la liste gardée et de la file du lecteur ; l'épisode en cours, arrêté. */
    private fun retirerDeLaListe(episode: Episode) {
        fileDEcoute.retirer(episode.clef)
        controleur?.let { c ->
            val i = (0 until c.mediaItemCount).firstOrNull { c.getMediaItemAt(it).mediaId == episode.mediaId }
            if (i != null) {
                if (i == c.currentMediaItemIndex) { c.stop(); c.clearMediaItems() } else c.removeMediaItem(i)
            }
        }
        afficher()
    }

    private fun telecharger(episode: Episode) {
        try {
            telechargements.lancer(episode)
            Toast.makeText(this, getString(R.string.telechargement_episode, episode.titre), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "téléchargement impossible : ${e.message}")
            Toast.makeText(this, getString(R.string.telechargement_impossible, e.message), Toast.LENGTH_LONG).show()
        }
        afficher()
    }

    private fun supprimerLeTelechargement(episode: Episode) {
        telechargements.supprimer(episode)
        Toast.makeText(this, getString(R.string.telechargement_supprime, episode.titre), Toast.LENGTH_SHORT).show()
        afficher()
    }

    /** L'élément du lecteur pour un épisode : le fichier téléchargé s'il est là, l'adresse en ligne sinon. */
    private fun elementDe(episode: Episode) =
        episode.versElement(telechargements.fichierDe(episode)?.let { Uri.fromFile(it).toString() } ?: episode.url)

    /**
     * Un épisode dans le lecteur, repris où l'on en était s'il n'est pas
     * terminé ; toute la liste autour de lui, les passés devant pour que
     * « précédent » y revienne, ceux à suivre derrière.
     */
    private fun jouerLEpisode(episode: Episode) {
        val c = controleur
        if (c == null) {
            Toast.makeText(this, R.string.lecteur_pas_pret, Toast.LENGTH_SHORT).show()
            return
        }
        val ecoute = ecoutesDuPodcast[episode.guid] ?: ecoutes.pour(episode.flux)[episode.guid]
        val reprise = ecoute?.takeIf { !it.termine && it.position > 0 }?.position ?: 0L
        val passes = fileDEcoute.passes().filter { it.clef != episode.clef }
        val suite = fileDEcoute.aSuivre().filter { it.clef != episode.clef }
        c.setMediaItems((passes + episode + suite).map { elementDe(it) }, passes.size, reprise * 1_000)
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

    /** Un appui sur les textes de la ligne de lecture : les titres passés d'une station, la liste de lecture pour un épisode. */
    private fun surLaLigneDeLecture() {
        val c = controleur ?: return
        val element = c.currentMediaItem ?: return
        if (element.estUnEpisode) {
            ouvrirLaFile()
            return
        }
        val nom = element.mediaMetadata.station?.toString() ?: return
        val enCours = c.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() && it != nom }
        montrerLesPrecedents(element.mediaId, nom, enCours)
    }

    /**
     * « Précédemment » : les titres que la station a annoncés pendant qu'on
     * l'écoutait, notés par le service, et, apparié, ceux que le serveur de
     * la maison a vus s'il écoute la même station, sans attendre plus de
     * trois secondes. Le titre en cours n'y est pas, il est déjà sous le nom.
     */
    private fun montrerLesPrecedents(uuid: String, nom: String, enCours: String?) {
        lifecycleScope.launch {
            val tous = withContext(Dispatchers.IO) {
                val locaux = Precedents.pour(this@MainActivity, uuid)
                val adresse = serveur.adresseActive
                val jeton = serveur.jeton
                val duServeur = if (serveur.apparie && adresse != null && jeton != null) {
                    try {
                        ClientServeur.titres(adresse, jeton, uuid)
                    } catch (e: Exception) {
                        Log.i(Fichiers.JOURNAL, "précédents : rien du serveur : ${e.message}")
                        emptyList()
                    }
                } else emptyList()
                Precedents.reunir(locaux, duServeur).filter { it.titre != enCours }
            }
            val heure = SimpleDateFormat("HH:mm", Locale.getDefault())
            val boite = MaterialAlertDialogBuilder(this@MainActivity).setTitle(getString(R.string.precedemment, nom))
            if (tous.isEmpty()) boite.setMessage(R.string.precedemment_rien)
            else boite.setItems(tous.map { "${heure.format(Date(it.le))}   ${it.titre}" }.toTypedArray(), null)
            boite.setPositiveButton(android.R.string.ok, null).show()
        }
    }

    /** La minuterie d'extinction : un menu de durées, et « Annuler » quand elle court. */
    private fun choisirLaMinuterie() {
        val durees = listOf(15, 30, 45, 60, 90)
        val noms = durees.map { getString(R.string.minuterie_minutes, it) }.toMutableList()
        if (Extinction.active) noms += getString(R.string.minuterie_annuler)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.minuterie)
            .setItems(noms.toTypedArray()) { _, i ->
                if (i < durees.size) {
                    Extinction.programmer(durees[i])
                    Toast.makeText(this, getString(R.string.minuterie_arret_dans, durees[i]), Toast.LENGTH_SHORT).show()
                } else {
                    Extinction.annuler()
                }
                rafraichirLaMinuterie()
            }
            .setNegativeButton(R.string.annuler, null)
            .show()
    }

    /** L'icône de la minuterie, colorée quand elle court, et les minutes qui restent dessous. */
    private fun rafraichirLaMinuterie() {
        val restantes = Extinction.minutesRestantes
        lectureMinuterieEtat.visibility = if (restantes != null) View.VISIBLE else View.GONE
        lectureMinuterieEtat.text = restantes?.let { getString(R.string.minuterie_court, it) } ?: ""
        val couleur = MaterialColors.getColor(
            lectureMinuterieIcone,
            if (restantes != null) androidx.appcompat.R.attr.colorPrimary else com.google.android.material.R.attr.colorOnSurfaceVariant,
        )
        lectureMinuterieIcone.imageTintList = ColorStateList.valueOf(couleur)
        minuterie.removeCallbacks(ticMinuterie)
        if (restantes != null) minuterie.postDelayed(ticMinuterie, 30_000)
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
        // Grisés quand il n'y a rien derrière ou devant.
        lecturePrecedent.isEnabled = c.hasPreviousMediaItem()
        lecturePrecedent.alpha = if (lecturePrecedent.isEnabled) 1f else 0.3f
        lectureSuivant.isEnabled = c.hasNextMediaItem()
        lectureSuivant.alpha = if (lectureSuivant.isEnabled) 1f else 0.3f
        rafraichirLaMinuterie()

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
