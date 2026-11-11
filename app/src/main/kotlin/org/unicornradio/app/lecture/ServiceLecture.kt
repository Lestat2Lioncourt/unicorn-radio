package org.unicornradio.app.lecture

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.MainActivity
import org.unicornradio.app.R
import org.unicornradio.app.annuaire.Annuaire
import org.unicornradio.app.annuaire.Retrouver
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.podcasts.CLEF_DUREE
import org.unicornradio.app.podcasts.CLEF_FLUX
import org.unicornradio.app.podcasts.CLEF_GUID
import org.unicornradio.app.podcasts.Ecoutes
import org.unicornradio.app.podcasts.FileDEcoute
import org.unicornradio.app.podcasts.episode
import org.unicornradio.app.podcasts.estUnEpisode
import org.unicornradio.app.serveur.Reseau
import org.unicornradio.app.serveur.Serveur

/**
 * Le lecteur, en arrière-plan : un ExoPlayer dans une session Media3. La
 * session donne la notification avec lecture, pause et suivant, l'écran de
 * verrouillage, les boutons du casque et la pause quand on débranche les
 * écouteurs. Le service est en avant-plan pendant la lecture, avec un verrou
 * pour continuer écran éteint.
 */
@UnstableApi
class ServiceLecture : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Un user agent nommé, les redirections http vers https acceptées, et
        // douze secondes sans réponse ou sans octet valent un échec : l'essai
        // suivant plutôt qu'une « mise en mémoire » sans fin.
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Unicorn-Radio/${BuildConfig.VERSION_NAME}")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(12_000)
        // Le http pour les flux et les épisodes en ligne ; `DefaultDataSource` par-dessus lit aussi les fichiers téléchargés.
        val lecteur = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(DefaultDataSource.Factory(this, http)))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        lecteur.addListener(Journal)
        lecteur.addListener(suivi)
        lecteur.addListener(titres)
        lecteur.addListener(reprise)

        val ouvrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaSession.Builder(this, lecteur).setSessionActivity(ouvrir).build()
        // La minuterie d'extinction, programmée par l'écran : à l'échéance, la pause.
        Extinction.arret = { session?.player?.pause() }
        // Un autre réseau pendant la lecture : le serveur sera recherché à la poussée suivante.
        Reseau.surveiller(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        noterLaPosition()
        main.removeCallbacks(toutesLesDixSecondes)
        Extinction.annuler()
        Extinction.arret = null
        Reseau.laisser()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    // ---- Le titre en cours d'une station ----

    /**
     * Le titre que la station annonce dans son flux, les métadonnées ICY.
     * Le lecteur les lit, mais quand il combine la fiche de l'élément et ce
     * qui vient du flux, la fiche passe par-dessus : un titre posé dans la
     * fiche cache celui du flux. Alors on écoute les métadonnées nous-mêmes
     * et on pose le titre dans la fiche de l'élément en cours, remplacé sur
     * place, même adresse, sans couper le son. La station qui n'annonce plus
     * rien reprend son nom pour titre, et l'écran redit « En direct ».
     */
    private val titres = object : Player.Listener {
        override fun onMetadata(metadata: Metadata) {
            val p = session?.player ?: return
            val item = p.currentMediaItem ?: return
            if (item.estUnEpisode) return
            var vu = false
            var titre: String? = null
            for (i in 0 until metadata.length()) {
                val e = metadata.get(i) as? IcyInfo ?: continue
                vu = true
                titre = e.title?.trim()?.takeIf { it.isNotEmpty() }
            }
            if (!vu) return
            val fiche = item.mediaMetadata
            val nom = fiche.station?.toString() ?: return
            val nouveau = titre ?: nom
            if (fiche.title?.toString() == nouveau) return
            Log.i(Fichiers.JOURNAL, "titre du flux « $nouveau »")
            // Un vrai titre, pas le nom de la station entre deux morceaux : noté dans « Précédemment ».
            if (titre != null && titre != nom) Precedents.noter(this@ServiceLecture, item.mediaId, titre)
            p.replaceMediaItem(
                p.currentMediaItemIndex,
                item.buildUpon().setMediaMetadata(fiche.buildUpon().setTitle(nouveau).build()).build(),
            )
        }
    }

    // ---- Une station que l'annuaire a refaite ----

    /** Les stations déjà redemandées à l'annuaire depuis le dernier changement d'élément : une fois chacune, pas de boucle. */
    private val redemandees = HashSet<String>()

    /**
     * Une station qui ne démarre pas : l'annuaire est consulté une fois. Une
     * autre adresse, ou une seule station de même nom sous un autre
     * identifiant, et la lecture repart avec elle, la fiche remplacée dans
     * mes listes (`Retrouver`) ; sinon, ce que l'annuaire a dit, en message.
     */
    private val reprise = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            redemandees.clear()
        }

        override fun onPlayerError(error: PlaybackException) {
            val p = session?.player ?: return
            val item = p.currentMediaItem ?: return
            if (item.estUnEpisode) return
            val uuid = item.mediaId
            if (!redemandees.add(uuid)) return
            val nom = item.mediaMetadata.station?.toString() ?: return
            val url = item.localConfiguration?.uri?.toString() ?: return
            val pays = item.mediaMetadata.extras?.getString(CLEF_PAYS)
            Thread {
                val verdict = Retrouver.chercher(uuid, nom, url, pays)
                main.post {
                    val nouvelle = when (verdict) {
                        is Retrouver.Verdict.Adresse -> verdict.nouvelle
                        is Retrouver.Verdict.Identifiant -> verdict.nouvelle
                        else -> null
                    }
                    if (nouvelle != null) {
                        Thread { Retrouver.appliquer(applicationContext, uuid, nouvelle) }.start()
                        val lecteur = session?.player
                        if (lecteur != null && lecteur.currentMediaItem?.mediaId == uuid) {
                            Log.i(Fichiers.JOURNAL, "retrouver : « $nom » repart à ${nouvelle.url}")
                            lecteur.replaceMediaItem(lecteur.currentMediaItemIndex, nouvelle.versElement())
                            lecteur.prepare()
                            lecteur.play()
                        }
                    }
                    dire(
                        when (verdict) {
                            is Retrouver.Verdict.Adresse -> getString(R.string.station_adresse_suivie, nom)
                            is Retrouver.Verdict.Identifiant -> getString(R.string.station_identifiant_suivi, nom)
                            is Retrouver.Verdict.MemeAdresse -> getString(R.string.station_meme_adresse, nom)
                            is Retrouver.Verdict.Disparue ->
                                if (verdict.homonymes == 0) getString(R.string.station_disparue_aucune, nom)
                                else getString(R.string.station_disparue_plusieurs, nom, verdict.homonymes)
                            is Retrouver.Verdict.Injoignable -> getString(R.string.station_annuaire_injoignable, nom)
                        },
                    )
                }
            }.start()
        }
    }

    private fun dire(texte: String) {
        Toast.makeText(applicationContext, texte, Toast.LENGTH_LONG).show()
    }

    // ---- Où l'on en est d'un épisode ----

    private val main = Handler(Looper.getMainLooper())
    private val ecoutes by lazy { Ecoutes(this) }
    private val serveur by lazy { Serveur(this) }
    private val fileDEcoute by lazy { FileDEcoute(this) }
    private val toutesLesDixSecondes = object : Runnable {
        override fun run() {
            noterLaPosition()
            main.postDelayed(this, 10_000)
        }
    }

    /**
     * La position d'un épisode, écrite toutes les dix secondes pendant la
     * lecture, à la pause, à l'arrêt, au changement d'élément et à la fin ;
     * terminé à moins d'une minute de la fin. Poussée au serveur dans la
     * foulée s'il répond, gardée sinon.
     */
    private val suivi = object : Player.Listener {
        /** Un épisode qui démarre, à la main ou à la suite du précédent : la liste de lecture le note, tout de suite, avant que l'écran ne relise. */
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem?.episode == null) return
            val p = session?.player ?: return
            val elements = (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
            val ici = p.currentMediaItemIndex
            fileDEcoute.suivreLeLecteur(
                elements.take(ici + 1).mapNotNull { it.episode },
                elements.drop(ici + 1).mapNotNull { it.episode },
            )
            // Arrivé par « précédent », « suivant » ou à la fin du précédent : repris où on l'avait laissé, s'il n'est pas fini.
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val extras = mediaItem.mediaMetadata.extras ?: return
                val flux = extras.getString(CLEF_FLUX) ?: return
                val guid = extras.getString(CLEF_GUID) ?: return
                val ecoute = ecoutes.pour(flux)[guid] ?: return
                if (!ecoute.termine && ecoute.position > 0) p.seekTo(ecoute.position * 1_000)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            main.removeCallbacks(toutesLesDixSecondes)
            if (isPlaying) main.postDelayed(toutesLesDixSecondes, 10_000) else noterLaPosition()
        }

        override fun onPositionDiscontinuity(ancienne: Player.PositionInfo, nouvelle: Player.PositionInfo, reason: Int) {
            val avant = ancienne.mediaItem ?: return
            if (ancienne.mediaItemIndex != nouvelle.mediaItemIndex && avant.estUnEpisode) {
                noter(avant, ancienne.positionMs, reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val p = session?.player ?: return
                val item = p.currentMediaItem ?: return
                if (item.estUnEpisode) noter(item, p.duration.takeIf { it != C.TIME_UNSET } ?: p.currentPosition, fini = true)
            }
        }
    }

    private fun noterLaPosition() {
        val p = session?.player ?: return
        val item = p.currentMediaItem ?: return
        if (!item.estUnEpisode || p.playbackState == Player.STATE_IDLE) return
        noter(item, p.currentPosition, fini = false, dureeMs = p.duration)
    }

    private fun noter(item: MediaItem, positionMs: Long, fini: Boolean, dureeMs: Long = C.TIME_UNSET) {
        val extras = item.mediaMetadata.extras ?: return
        val flux = extras.getString(CLEF_FLUX) ?: return
        val guid = extras.getString(CLEF_GUID) ?: return
        val duree = when {
            dureeMs != C.TIME_UNSET && dureeMs > 0 -> dureeMs / 1000
            extras.getLong(CLEF_DUREE, -1L) > 0 -> extras.getLong(CLEF_DUREE)
            else -> null
        }
        val position = (positionMs / 1000).coerceAtLeast(0)
        val termine = fini || (duree != null && position >= duree - 60)
        Thread {
            try {
                ecoutes.noter(flux, guid, position, duree, termine)
                ecoutes.pousserEnAttente(serveur)
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "écoutes : position gardée, pas poussée : ${e.message}")
            }
        }.start()
    }

    /**
     * Ce qui se passe, dans Logcat : la station lancée, l'état, le titre,
     * l'échec et sa cause. Et à chaque station lancée, l'écoute est signalée
     * à l'annuaire, qui la compte ; l'adresse à jour qu'il rend est notée,
     * la lecture déjà partie n'est pas interrompue.
     */
    private object Journal : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return
            val uri = mediaItem.localConfiguration?.uri
            if (mediaItem.estUnEpisode) {
                Log.i(Fichiers.JOURNAL, "lecture de l'épisode « ${mediaItem.mediaMetadata.title} » $uri")
                return
            }
            Log.i(Fichiers.JOURNAL, "lecture « ${mediaItem.mediaMetadata.station} » $uri")
            val uuid = mediaItem.mediaId
            Thread {
                val aJour = Annuaire.signalerEcoute(uuid)
                if (aJour != null && aJour != uri.toString()) Log.i(Fichiers.JOURNAL, "l'annuaire donne une autre adresse pour $uuid : $aJour")
            }.start()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val etat = when (playbackState) {
                Player.STATE_IDLE -> "idle"
                Player.STATE_BUFFERING -> "connexion"
                Player.STATE_READY -> "prêt"
                Player.STATE_ENDED -> "fini"
                else -> "$playbackState"
            }
            Log.i(Fichiers.JOURNAL, "état $etat")
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Log.i(Fichiers.JOURNAL, "titre « ${mediaMetadata.title} »")
        }

        override fun onPlayerError(error: PlaybackException) {
            var cause: Throwable? = error.cause
            val causes = StringBuilder()
            while (cause != null) {
                causes.append(" ← ").append(cause.javaClass.simpleName).append(": ").append(cause.message)
                cause = cause.cause
            }
            Log.w(Fichiers.JOURNAL, "échec ${error.errorCodeName}$causes")
        }
    }
}
