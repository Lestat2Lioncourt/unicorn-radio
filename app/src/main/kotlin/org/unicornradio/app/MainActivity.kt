package org.unicornradio.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.lecture.ServiceLecture
import org.unicornradio.app.lecture.versElement
import org.unicornradio.app.modele.Noeud
import org.unicornradio.app.modele.Station

/**
 * L'écran unique : la liste du noeud courant (fichiers, dossiers, stations)
 * sous une barre qui dit où l'on est, et en bas la ligne de lecture, qui
 * reste pendant qu'on navigue. Le lecteur lui-même est dans
 * `ServiceLecture` ; ici, un `MediaController` lui parle.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var fichiers: Fichiers
    private lateinit var barre: MaterialToolbar
    private lateinit var liste: RecyclerView
    private lateinit var vide: View
    private lateinit var lecture: View
    private lateinit var lectureLogo: ImageView
    private lateinit var lectureInitiales: TextView
    private lateinit var lectureNom: TextView
    private lateinit var lectureEtat: TextView
    private lateinit var lectureBouton: ImageButton
    private lateinit var adaptateur: ListeAdaptateur

    /** Le chemin parcouru, la racine en premier. Vide tant qu'aucun fichier n'est chargé. */
    private val pile = ArrayDeque<Noeud>()

    private var controleurFutur: ListenableFuture<MediaController>? = null
    private var controleur: MediaController? = null

    private val retour = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            pile.removeLast()
            afficher()
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

        barre = findViewById(R.id.barre)
        liste = findViewById(R.id.liste)
        vide = findViewById(R.id.vide)
        lecture = findViewById(R.id.lecture)
        lectureLogo = findViewById(R.id.lectureLogo)
        lectureInitiales = findViewById(R.id.lectureInitiales)
        lectureNom = findViewById(R.id.lectureNom)
        lectureEtat = findViewById(R.id.lectureEtat)
        lectureBouton = findViewById(R.id.lectureBouton)

        adaptateur = ListeAdaptateur(
            surDossier = { pile.addLast(it); afficher() },
            surDossierLong = { proposerDeRetirer(it) },
            surStation = { jouer(it) },
        )
        liste.layoutManager = LinearLayoutManager(this)
        liste.adapter = adaptateur

        barre.inflateMenu(R.menu.principal)
        barre.setOnMenuItemClickListener {
            if (it.itemId == R.id.ouvrir) { ouvrirUnFichier(); true } else false
        }
        barre.setNavigationOnClickListener { retour.handleOnBackPressed() }
        findViewById<View>(R.id.videBouton).setOnClickListener { ouvrirUnFichier() }
        lectureBouton.setOnClickListener { basculerLecture() }
        onBackPressedDispatcher.addCallback(this, retour)

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

    private fun charger(versFichier: String? = null) {
        lifecycleScope.launch {
            val racines = withContext(Dispatchers.IO) { fichiers.charger() }
            pile.clear()
            when (racines.size) {
                0 -> Unit
                1 -> pile.addLast(racines[0])
                else -> {
                    pile.addLast(Noeud(getString(R.string.app_name), racines, emptyList()))
                    racines.firstOrNull { it.fichier == versFichier }?.let { pile.addLast(it) }
                }
            }
            afficher()
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
                charger()
            }
            .show()
        return true
    }

    // ---- La liste ----

    private fun afficher() {
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
