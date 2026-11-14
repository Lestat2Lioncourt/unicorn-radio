package org.unicornradio.app.proximite

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.R
import org.unicornradio.app.fichier.EcritureFichier
import org.unicornradio.app.fichier.LectureFichier
import org.unicornradio.app.modele.Noeud
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Échanger ses radios avec un téléphone proche, en Bluetooth, d'une
 * application à l'autre, sans Internet ni fichier à transmettre.
 *
 * Celui qui reçoit choisit « Recevoir » : son téléphone devient visible et
 * attend, cinq minutes au plus. Celui qui envoie choisit « Envoyer » et
 * touche le téléphone de l'autre dans la liste de ceux qui sont proches.
 * Le receveur voit d'où viennent les radios et combien, et décide de les
 * ajouter ; elles sont versées dans sa liste comme un import, rien
 * d'effacé, rien doublé. Rien n'arrive sans qu'on ait choisi « Recevoir ».
 *
 * La liaison est « non sécurisée » au sens du Bluetooth : pas
 * d'appairage à faire entre deux inconnus. Ce qui passe est une liste de
 * radios publiques, et le receveur la voit avant de l'accepter.
 *
 * Sur le fil : un nombre magique, la longueur, puis le fichier d'échange
 * en UTF-8, sans les podcasts ; le receveur répond d'un octet quand il a
 * tout lu, pour que l'envoyeur sache que c'est arrivé.
 */
@SuppressLint("MissingPermission")
class Proximite(
    private val activite: AppCompatActivity,
    /** Ma liste, la section du fichier d'échange, ou celle d'un dossier et de ses sous-dossiers ; appelé hors du fil principal. */
    private val section: (List<String>?) -> JSONObject?,
    /** Des radios reçues et acceptées, à verser dans ma liste ; sur le fil principal. */
    private val recues: (Noeud) -> Unit,
) {
    private val adaptateur: BluetoothAdapter?
        get() = (activite.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /** Ce qu'il reste à faire une fois les permissions accordées et le Bluetooth allumé. */
    private var ensuite: (() -> Unit)? = null

    private val permissions = activite.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reponses ->
        val suite = ensuite
        ensuite = null
        if (reponses.values.all { it }) suite?.invoke()
        else Toast.makeText(activite, R.string.proche_permission_refusee, Toast.LENGTH_LONG).show()
    }

    private val allumer = activite.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val suite = ensuite
        ensuite = null
        if (adaptateur?.isEnabled == true) suite?.invoke()
    }

    private val rendreVisible = activite.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultat ->
        // Le résultat est la durée accordée, en secondes ; refusé, c'est « annulé ».
        if (resultat.resultCode != AppCompatActivity.RESULT_CANCELED) attendre()
    }

    /** Le choix : envoyer ou recevoir. */
    fun proposer() {
        if (adaptateur == null) {
            Toast.makeText(activite, R.string.proche_pas_de_bluetooth, Toast.LENGTH_LONG).show()
            return
        }
        val choix = arrayOf(activite.getString(R.string.proche_envoyer), activite.getString(R.string.proche_recevoir))
        MaterialAlertDialogBuilder(activite)
            .setTitle(R.string.proche_titre)
            .setItems(choix) { _, i -> if (i == 0) envoyer(null) else recevoir() }
            .setNegativeButton(R.string.annuler, null)
            .show()
    }

    // ---- Les permissions ----

    private fun manquantes(voulues: List<String>): List<String> =
        voulues.filter { ContextCompat.checkSelfPermission(activite, it) != PackageManager.PERMISSION_GRANTED }

    private fun avecPermissions(voulues: List<String>, action: () -> Unit) {
        val manque = manquantes(voulues)
        if (manque.isEmpty()) { action(); return }
        ensuite = action
        permissions.launch(manque.toTypedArray())
    }

    // ---- Envoyer ----

    /** Envoyer toute ma liste (`null`), ou un dossier avec ses sous-dossiers. */
    fun envoyer(chemin: List<String>?) {
        if (adaptateur == null) {
            Toast.makeText(activite, R.string.proche_pas_de_bluetooth, Toast.LENGTH_LONG).show()
            return
        }
        val voulues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        avecPermissions(voulues) {
            if (adaptateur?.isEnabled == true) chercher(chemin)
            else {
                ensuite = { chercher(chemin) }
                allumer.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }

    /** Les téléphones proches, au fil de la recherche, dans une liste qu'on touche. */
    private fun chercher(chemin: List<String>?) {
        val bt = adaptateur ?: return
        // Avant Android 12, la recherche ne trouve rien si la localisation est coupée.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val lieu = activite.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val actif = lieu != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || lieu.isLocationEnabled)
            if (!actif) {
                Toast.makeText(activite, R.string.proche_localisation, Toast.LENGTH_LONG).show()
                return
            }
        }
        val trouves = ArrayList<BluetoothDevice>()
        val noms = ArrayAdapter<String>(activite, android.R.layout.simple_list_item_1, ArrayList())
        val marge = (20 * activite.resources.displayMetrics.density).toInt()
        val aide = TextView(activite).apply {
            setText(R.string.proche_aide_envoyer)
            setPadding(marge, marge / 2, marge, marge / 2)
        }
        val roue = ProgressBar(activite, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            setPadding(marge, 0, marge, 0)
        }
        val liste = ListView(activite).apply { adapter = noms }
        val vue = LinearLayout(activite).apply {
            orientation = LinearLayout.VERTICAL
            addView(aide)
            addView(roue, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(liste, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        var ouverte = true
        val ecoute = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val appareil = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        appareil ?: return
                        val nom = try { appareil.name } catch (e: SecurityException) { null } ?: return
                        // Les casques, enceintes, montres et voitures n'ont rien à recevoir : téléphones et tablettes seulement.
                        val genre = appareil.bluetoothClass?.majorDeviceClass
                        if (genre != null && genre != BluetoothClass.Device.Major.PHONE && genre != BluetoothClass.Device.Major.COMPUTER) return
                        if (trouves.any { it.address == appareil.address }) return
                        trouves += appareil
                        noms.add(nom)
                    }
                    // Une recherche dure une douzaine de secondes : on relance tant que la fenêtre est ouverte.
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> if (ouverte) try { bt.startDiscovery() } catch (e: SecurityException) { }
                }
            }
        }
        val filtre = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        ContextCompat.registerReceiver(activite, ecoute, filtre, ContextCompat.RECEIVER_EXPORTED)
        lateinit var fenetre: AlertDialog
        fenetre = MaterialAlertDialogBuilder(activite)
            .setTitle(R.string.proche_choisir)
            .setView(vue)
            .setNegativeButton(R.string.annuler, null)
            .setOnDismissListener {
                ouverte = false
                try { bt.cancelDiscovery() } catch (e: SecurityException) { }
                try { activite.unregisterReceiver(ecoute) } catch (e: IllegalArgumentException) { }
            }
            .show()
        liste.setOnItemClickListener { _, _, i, _ ->
            val appareil = trouves[i]
            fenetre.dismiss()
            transmettre(appareil, chemin, noms.getItem(i) ?: appareil.address)
        }
        try { bt.startDiscovery() } catch (e: SecurityException) { fenetre.dismiss() }
    }

    private fun transmettre(appareil: BluetoothDevice, chemin: List<String>?, nom: String) {
        Toast.makeText(activite, activite.getString(R.string.proche_envoi_en_cours, nom), Toast.LENGTH_SHORT).show()
        activite.lifecycleScope.launch {
            val n = try {
                withContext(Dispatchers.IO) {
                    val s = section(chemin) ?: throw IOException("rien à envoyer")
                    // Les radios seulement : les podcasts restent chez soi.
                    s.put("podcasts", JSONArray())
                    val n = LectureFichier.lire(s.toString(), "").nombreDeStations
                    if (n == 0) throw IOException("rien à envoyer")
                    val octets = EcritureFichier.enveloppe(s).toByteArray(Charsets.UTF_8)
                    adaptateur?.cancelDiscovery()
                    appareil.createInsecureRfcommSocketToServiceRecord(SERVICE).use { prise ->
                        prise.connect()
                        val sortie = DataOutputStream(prise.outputStream)
                        sortie.writeInt(MAGIQUE)
                        sortie.writeInt(octets.size)
                        sortie.write(octets)
                        sortie.flush()
                        if (prise.inputStream.read() != 1) throw IOException("pas d'accusé de réception")
                    }
                    n
                }
            } catch (e: Exception) {
                Log.w(JOURNAL, "envoi à $nom : ${e.message}")
                val message = if (e.message == "rien à envoyer") activite.getString(R.string.rien_a_exporter)
                else activite.getString(R.string.proche_envoi_echoue, nom)
                Toast.makeText(activite, message, Toast.LENGTH_LONG).show()
                return@launch
            }
            Toast.makeText(activite, activite.resources.getQuantityString(R.plurals.proche_envoyees, n, n, nom), Toast.LENGTH_LONG).show()
        }
    }

    // ---- Recevoir ----

    private fun recevoir() {
        val voulues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        else emptyList()
        avecPermissions(voulues) {
            // La demande de visibilité allume aussi le Bluetooth s'il est éteint.
            rendreVisible.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DUREE_S))
        }
    }

    /** Visible : on attend un envoyeur, cinq minutes au plus, ou jusqu'à « Annuler ». */
    private fun attendre() {
        val bt = adaptateur ?: return
        val serveur: BluetoothServerSocket = try {
            bt.listenUsingInsecureRfcommWithServiceRecord("Unicorn-Radio", SERVICE)
        } catch (e: Exception) {
            Log.w(JOURNAL, "écoute impossible : ${e.message}")
            Toast.makeText(activite, R.string.proche_reception_echouee, Toast.LENGTH_LONG).show()
            return
        }
        val moi = try { bt.name } catch (e: SecurityException) { null } ?: "?"
        var annulee = false
        val fenetre = MaterialAlertDialogBuilder(activite)
            .setTitle(R.string.proche_recevoir)
            .setMessage(activite.getString(R.string.proche_en_attente, moi))
            .setNegativeButton(R.string.annuler) { _, _ -> annulee = true }
            .setOnDismissListener { try { serveur.close() } catch (e: IOException) { } }
            .show()
        activite.lifecycleScope.launch {
            val recu = try {
                withContext(Dispatchers.IO) {
                    val prise: BluetoothSocket = serveur.accept(DUREE_S * 1_000)
                    serveur.close()
                    prise.use {
                        val entree = DataInputStream(it.inputStream)
                        if (entree.readInt() != MAGIQUE) throw IOException("pas Unicorn-Radio")
                        val taille = entree.readInt()
                        if (taille <= 0 || taille > TAILLE_MAX) throw IOException("taille $taille")
                        val octets = ByteArray(taille)
                        entree.readFully(octets)
                        it.outputStream.write(1)
                        it.outputStream.flush()
                        val de = try { it.remoteDevice.name } catch (e: SecurityException) { null } ?: it.remoteDevice.address
                        de to LectureFichier.lire(octets.toString(Charsets.UTF_8), de)
                    }
                }
            } catch (e: Exception) {
                fenetre.dismiss()
                if (!annulee) {
                    Log.w(JOURNAL, "réception : ${e.message}")
                    Toast.makeText(activite, R.string.proche_reception_echouee, Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            fenetre.dismiss()
            val (de, racine) = recu
            val n = racine.nombreDeStations
            val dossiers = racine.tousLesDossiers().size
            MaterialAlertDialogBuilder(activite)
                .setTitle(R.string.proche_recevoir)
                .setMessage(
                    activite.resources.getQuantityString(R.plurals.proche_recues, n, de, n) +
                        (if (dossiers > 0) activite.resources.getQuantityString(R.plurals.proche_dans_dossiers, dossiers, dossiers) else "") +
                        "\n\n" + activite.getString(R.string.proche_ajouter_question),
                )
                .setNegativeButton(R.string.proche_refuser, null)
                .setPositiveButton(R.string.proche_ajouter) { _, _ -> recues(racine) }
                .setCancelable(false)
                .show()
        }
    }

    companion object {
        private const val JOURNAL = "UnicornProche"
        /** Le même identifiant des deux côtés : celui du service d'Unicorn-Radio. */
        private val SERVICE: UUID = UUID.fromString("6f1c2b7e-4a5d-4c3e-9b8a-2d7e1f0a5c39")
        /** « URAD », pour ne pas prendre n'importe quoi pour une liste. */
        private const val MAGIQUE = 0x55524144
        private const val TAILLE_MAX = 10 * 1024 * 1024
        private const val DUREE_S = 300
    }
}
