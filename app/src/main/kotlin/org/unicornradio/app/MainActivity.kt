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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.unicornradio.app.annuaire.Annuaire
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.fichier.MesFavoris
import org.unicornradio.app.lecture.ServiceLecture
import org.unicornradio.app.lecture.versElement
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station
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
    private lateinit var mesFavoris: MesFavoris
    private lateinit var barre: MaterialToolbar
    private lateinit var liste: RecyclerView
    private lateinit var vide: View
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

    private var controleurFutur: ListenableFuture<MediaController>? = null
    private var controleur: MediaController? = null

    private val retour = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (modeRecherche) quitterLaRecherche() else { pile.removeLast(); afficher() }
        }
    }

    private val choisirFichier = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importer(uri)
    }

    private val demanderNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val surLecteur = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = rafraichirLecture()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fichiers = Fichiers(this)
        mesFavoris = MesFavoris(this)

        barre = findViewById(R.id.barre)
        liste = findViewById(R.id.liste)
        vide = findViewById(R.id.vide)
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

        adaptateur = ListeAdaptateur(
            surDossier = { pile.addLast(it); afficher() },
            surDossierLong = { proposerDeRetirer(it) },
            surStation = { jouer(it) },
            surStationLong = { proposerDeRetirerDesFavoris(it) },
            estFavorite = { mesFavoris.contient(it.uuid) },
            surEtoile = { etoiler(it) },
        )
        liste.layoutManager = LinearLayoutManager(this)
        liste.adapter = adaptateur

        barre.inflateMenu(R.menu.principal)
        barre.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.ouvrir -> { ouvrirUnFichier(); true }
                R.id.chercher -> { entrerDansLaRecherche(); true }
                else -> false
            }
        }
        barre.setNavigationOnClickListener { retour.handleOnBackPressed() }
        findViewById<View>(R.id.videBouton).setOnClickListener { ouvrirUnFichier() }
        findViewById<View>(R.id.videChercher).setOnClickListener { entrerDansLaRecherche() }
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
    }

    override fun onStop() {
        controleur?.removeListener(surLecteur)
        controleurFutur?.let { MediaController.releaseFuture(it) }
        controleur = null
        controleurFutur = null
        super.onStop()
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
                val favoris = mesFavoris.charger()
                val liste = ArrayList<Noeud>()
                if (favoris.isNotEmpty()) liste += Noeud(getString(R.string.mes_favoris), emptyList(), favoris, mesFavoris = true)
                liste += fichiers.charger()
                liste
            }
            pile.clear()
            when (racines.size) {
                0 -> Unit
                1 -> pile.addLast(racines[0])
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

    // ---- La liste ----

    private fun afficher() {
        if (modeRecherche) {
            retour.isEnabled = true
            barre.navigationIcon = getDrawable(R.drawable.ic_retour)
            barre.title = getString(R.string.annuaire)
            barre.subtitle = null
            recherche.visibility = View.VISIBLE
            vide.visibility = View.GONE
            liste.visibility = View.VISIBLE
            adaptateur.montrer(Noeud(getString(R.string.annuaire), emptyList(), resultats), etoiles = true)
            return
        }
        recherche.visibility = View.GONE
        val noeud = pile.lastOrNull()
        val racine = pile.size <= 1
        retour.isEnabled = !racine
        barre.navigationIcon = if (racine) null else getDrawable(R.drawable.ic_retour)
        barre.title = noeud?.titre ?: getString(R.string.app_name)
        barre.subtitle = if (racine) BuildConfig.VERSION_NAME else null
        if (noeud == null) {
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

    /** Un appui long sur une station de « Mes favoris » propose de la retirer, sans repasser par l'annuaire. */
    private fun proposerDeRetirerDesFavoris(station: Station): Boolean {
        if (modeRecherche || pile.lastOrNull()?.mesFavoris != true) return false
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.retirer_des_favoris)
            .setMessage(station.nom)
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.retirer) { _, _ -> etoiler(station) }
            .show()
        return true
    }

    /** L'étoile : la station entre dans « Mes favoris » ou en sort ; la racine suit. */
    private fun etoiler(station: Station) {
        lifecycleScope.launch {
            val favorite = withContext(Dispatchers.IO) { mesFavoris.basculer(station) }
            Toast.makeText(
                this@MainActivity,
                getString(if (favorite) R.string.favori_ajoute else R.string.favori_retire, station.nom),
                Toast.LENGTH_SHORT,
            ).show()
            adaptateur.rafraichir(station)
            charger()
        }
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
        val nom = (fiche.station ?: fiche.title ?: "").toString()
        lectureNom.text = nom
        // Le titre du flux : ce que le lecteur combine, quand il diffère du nom de la station.
        val titre = c.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() && it != nom }
        val erreur = c.playerError
        lectureEtat.text = when {
            erreur != null -> getString(R.string.injoignable, erreur.errorCodeName.removePrefix("ERROR_CODE_"))
            c.playbackState == Player.STATE_BUFFERING -> getString(R.string.connexion)
            c.isPlaying -> titre ?: getString(R.string.en_direct)
            c.playbackState == Player.STATE_READY -> getString(R.string.en_pause)
            else -> ""
        }
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
