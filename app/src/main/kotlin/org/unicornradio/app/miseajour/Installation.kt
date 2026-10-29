package org.unicornradio.app.miseajour

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat

/**
 * Télécharger l'APK d'une release par le gestionnaire de téléchargements
 * d'Android, avec sa notification, puis ouvrir l'installeur à la fin.
 * Rien ne s'installe sans l'utilisateur : Android lui demande d'autoriser
 * l'application à installer des paquets, une fois, puis confirme encore.
 * Si l'écran est parti entre-temps, la notification du téléchargement
 * fini ouvre l'installeur elle-même.
 */
class Installation(private val contexte: Context, private val surEchec: (String) -> Unit) {

    private val gestionnaire = contexte.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var enCours = -1L
    private var recepteur: BroadcastReceiver? = null

    fun lancer(version: MiseAJour.Version) {
        val apk = version.apk ?: return
        val nom = "unicorn-radio-${version.numero}.apk"
        // Un téléchargement précédent du même fichier, fini ou non, est écarté : le gestionnaire ne remplace pas.
        contexte.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve(nom)?.delete()
        val demande = DownloadManager.Request(Uri.parse(apk))
            .setTitle("Unicorn-Radio ${version.numero}")
            .setMimeType(MIME_APK)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(contexte, Environment.DIRECTORY_DOWNLOADS, nom)
        ecouter()
        enCours = gestionnaire.enqueue(demande)
        tracer("téléchargement $enCours de $apk")
    }

    private fun ecouter() {
        if (recepteur != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != enCours) return
                var statut = -1
                var raison = -1
                gestionnaire.query(DownloadManager.Query().setFilterById(id)).use { curseur ->
                    if (curseur.moveToFirst()) {
                        statut = curseur.getInt(curseur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        raison = curseur.getInt(curseur.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    }
                }
                val fichier = gestionnaire.getUriForDownloadedFile(id)
                if (statut == DownloadManager.STATUS_SUCCESSFUL && fichier != null) {
                    tracer("téléchargement $id fini, l'installeur")
                    contexte.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(fichier, MIME_APK)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    )
                } else {
                    tracer("téléchargement $id manqué, statut $statut, raison $raison")
                    surEchec("$statut/$raison")
                }
            }
        }
        ContextCompat.registerReceiver(contexte, r, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
        recepteur = r
    }

    /** À la fin de l'écran : on n'écoute plus, le téléchargement continue et sa notification fera le reste. */
    fun fermer() {
        recepteur?.let { contexte.unregisterReceiver(it) }
        recepteur = null
    }

    private companion object {
        const val MIME_APK = "application/vnd.android.package-archive"
    }
}
