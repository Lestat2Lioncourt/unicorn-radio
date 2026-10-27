package org.unicornradio.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.serveur.ClientServeur
import org.unicornradio.app.serveur.Journal
import org.unicornradio.app.serveur.MesRadios
import org.unicornradio.app.serveur.Serveur
import org.unicornradio.app.serveur.Synchro
import java.text.DateFormat
import java.util.Date

/**
 * L'écran Serveur : apparier ce téléphone à un Unicorn-TV, par le QR code
 * ou l'adresse et le code tapés ; puis, apparié, ce qu'on sait du serveur,
 * « Mettre à jour » et « Se détacher ». L'écran principal recharge au
 * retour, quoi qu'il se soit passé ici.
 */
class ServeurActivity : AppCompatActivity() {

    private lateinit var serveur: Serveur
    private lateinit var mesRadios: MesRadios
    private lateinit var journal: Journal
    private lateinit var synchro: Synchro
    private lateinit var apparieGestes: TextView
    private lateinit var nonApparie: View
    private lateinit var apparie: View
    private lateinit var adresse: EditText
    private lateinit var code: EditText
    private lateinit var etat: TextView
    private lateinit var apparieTitre: TextView
    private lateinit var apparieUtilisateur: TextView
    private lateinit var apparieAdresse: TextView
    private lateinit var apparieCopie: TextView
    private var enCours = false

    private val scanner = registerForActivityResult(ScanContract()) { resultat ->
        val texte = resultat.contents
        if (texte == null) {
            // Scan abandonné, ou caméra refusée : la saisie à la main reste.
            Toast.makeText(this, R.string.camera_refusee, Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        val invitation = ClientServeur.Invitation.depuisQr(texte)
        if (invitation == null) {
            Toast.makeText(this, R.string.qr_pas_un_appariement, Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        apparier(invitation)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serveur)
        serveur = Serveur(this)
        mesRadios = MesRadios(this, MesRadios.COPIE)
        journal = Journal(this)
        synchro = Synchro(serveur, mesRadios, journal)
        setResult(RESULT_OK)

        val barre = findViewById<MaterialToolbar>(R.id.barre)
        barre.setNavigationOnClickListener { finish() }
        nonApparie = findViewById(R.id.nonApparie)
        apparie = findViewById(R.id.apparie)
        adresse = findViewById(R.id.adresse)
        code = findViewById(R.id.code)
        etat = findViewById(R.id.etat)
        apparieTitre = findViewById(R.id.apparieTitre)
        apparieUtilisateur = findViewById(R.id.apparieUtilisateur)
        apparieAdresse = findViewById(R.id.apparieAdresse)
        apparieCopie = findViewById(R.id.apparieCopie)
        apparieGestes = findViewById(R.id.apparieGestes)

        findViewById<View>(R.id.scanner).setOnClickListener {
            scanner.launch(ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("")
                setBeepEnabled(false)
                setOrientationLocked(false)
            })
        }
        findViewById<View>(R.id.apparierBouton).setOnClickListener {
            val a = adresse.text.toString().trim()
            val c = code.text.toString().trim()
            if (a.isEmpty() || c.isEmpty()) {
                etat.text = getString(R.string.champs_vides)
                return@setOnClickListener
            }
            apparier(ClientServeur.Invitation(null, null, listOf(ClientServeur.Invitation.normaliser(a)), c))
        }
        findViewById<View>(R.id.mettreAJour).setOnClickListener { mettreAJour() }
        findViewById<View>(R.id.detacher).setOnClickListener { proposerDeSeDetacher() }
        montrer()
    }

    private fun montrer() {
        val a = serveur.apparie
        nonApparie.visibility = if (a) View.GONE else View.VISIBLE
        apparie.visibility = if (a) View.VISIBLE else View.GONE
        if (a) {
            apparieTitre.text = getString(R.string.serveur_apparie_titre, serveur.nom)
            apparieUtilisateur.text = getString(R.string.serveur_utilisateur, serveur.utilisateur)
            apparieAdresse.text = getString(R.string.serveur_adresse, serveur.adresseActive ?: serveur.adresses.firstOrNull() ?: "")
            val quand = serveur.derniereCopie
            apparieCopie.text = if (quand == 0L) getString(R.string.serveur_copie_jamais)
            else getString(R.string.serveur_copie, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(quand)))
            val attente = journal.nombre
            apparieGestes.text = if (attente == 0) getString(R.string.gestes_aucun)
            else resources.getQuantityString(R.plurals.gestes_en_attente, attente, attente)
        }
    }

    /** Trouver le serveur, échanger le code, puis tirer la première copie. */
    private fun apparier(invitation: ClientServeur.Invitation) {
        if (enCours) return
        enCours = true
        etat.text = getString(R.string.serveur_recherche)
        lifecycleScope.launch {
            try {
                val racine = withContext(Dispatchers.IO) {
                    val (adresse, identite) = ClientServeur.trouver(invitation.adresses, invitation.serveur)
                    val r = ClientServeur.apparier(adresse, invitation.code, serveur.appareilId, serveur.appareilNom)
                    serveur.enregistrer(
                        id = r.serveur, nom = r.nom.ifEmpty { identite.nom }, adresses = invitation.adresses,
                        adresseActive = adresse, jeton = r.jeton, utilisateur = r.utilisateur,
                    )
                    Log.i(Fichiers.JOURNAL, "serveur : apparié à « ${r.nom} » ($adresse), utilisateur ${r.utilisateur}")
                    // Les favoris locaux versés au serveur, comme autant de gestes ; le fichier local reste, pour le détachement.
                    val locaux = MesRadios(this@ServeurActivity, MesRadios.LOCALE).charger()?.stations ?: emptyList()
                    for (s in locaux) journal.favoriAjouter(s)
                    if (locaux.isNotEmpty()) Log.i(Fichiers.JOURNAL, "serveur : ${locaux.size} favoris locaux versés au journal")
                    synchro.executer()
                }
                etat.text = resultatTexte(racine)
                code.setText("")
                montrer()
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "serveur : appariement : ${e.message}")
                etat.text = when (e) {
                    is ClientServeur.CodeInconnu -> getString(R.string.code_inconnu)
                    is ClientServeur.TropDEssais -> getString(R.string.trop_d_essais)
                    is ClientServeur.Revoque -> getString(R.string.revoque, serveur.nom)
                    else -> injoignable(e)
                }
            } finally {
                enCours = false
            }
        }
    }

    private fun mettreAJour() {
        if (enCours) return
        val jeton = serveur.jeton ?: return
        enCours = true
        etat.text = getString(R.string.mise_a_jour_en_cours)
        lifecycleScope.launch {
            try {
                val resultat = withContext(Dispatchers.IO) { synchro.executer() }
                etat.text = resultatTexte(resultat)
            } catch (e: ClientServeur.Revoque) {
                val nom = serveur.nom
                serveur.oublier()
                mesRadios.effacer()
                journal.effacer()
                etat.text = getString(R.string.revoque, nom)
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "serveur : mise à jour : ${e.message}")
                etat.text = injoignable(e)
            } finally {
                enCours = false
                montrer()
            }
        }
    }

    /** « Copie à jour : N stations », et les gestes refusés s'il y en a. */
    private fun resultatTexte(r: Synchro.Resultat): String {
        val texte = getString(R.string.mise_a_jour_faite, r.racine.nombreDeStations)
        if (r.refuses.isEmpty()) return texte
        return texte + "\n" + resources.getQuantityString(R.plurals.gestes_refuses, r.refuses.size, r.refuses.size)
    }

    /** « Serveur injoignable : 192.168.0.25:3000, pas de réponse en cinq secondes » : l'adresse essayée et la raison. */
    private fun injoignable(e: Exception): String {
        if (e !is ClientServeur.Injoignable) return getString(R.string.serveur_injoignable_detail, "-", e.message ?: e.javaClass.simpleName)
        val raison = when (e.raison) {
            ClientServeur.Raison.DELAI -> getString(R.string.raison_delai)
            ClientServeur.Raison.REFUSEE -> getString(R.string.raison_refusee)
            ClientServeur.Raison.ADRESSE_INCONNUE -> getString(R.string.raison_adresse_inconnue)
            ClientServeur.Raison.HTTP -> "HTTP ${e.detail}"
            ClientServeur.Raison.PAS_UNICORN_TV -> getString(R.string.raison_pas_unicorn_tv)
            ClientServeur.Raison.AUTRE_SERVEUR -> getString(R.string.raison_autre_serveur, e.detail ?: "")
            ClientServeur.Raison.AUCUNE_ADRESSE -> getString(R.string.raison_aucune_adresse)
            ClientServeur.Raison.AUTRE -> e.detail ?: "?"
        }
        return getString(R.string.serveur_injoignable_detail, ClientServeur.Invitation.courte(e.adresse), raison)
    }

    private fun proposerDeSeDetacher() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.se_detacher)
            .setMessage(getString(R.string.se_detacher_texte, serveur.nom))
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.se_detacher) { _, _ -> seDetacher() }
            .show()
    }

    /** Prévenir le serveur si on le voit, puis oublier dans tous les cas. */
    private fun seDetacher() {
        if (enCours) return
        enCours = true
        val nom = serveur.nom
        val jeton = serveur.jeton
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (jeton != null) {
                        val (adresse, _) = ClientServeur.trouver(serveur.adresses, serveur.id, serveur.adresseActive)
                        ClientServeur.detacher(adresse, jeton)
                    }
                } catch (e: Exception) {
                    Log.w(Fichiers.JOURNAL, "serveur : détachement non signalé : ${e.message}")
                }
                serveur.oublier()
                mesRadios.effacer()
                journal.effacer()
            }
            enCours = false
            etat.text = getString(R.string.detache, nom)
            montrer()
        }
    }
}
