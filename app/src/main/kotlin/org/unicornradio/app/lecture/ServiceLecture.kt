package org.unicornradio.app.lecture

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.MainActivity
import org.unicornradio.app.annuaire.Annuaire
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.podcasts.CLEF_DUREE
import org.unicornradio.app.podcasts.CLEF_FLUX
import org.unicornradio.app.podcasts.CLEF_GUID
import org.unicornradio.app.podcasts.Ecoutes
import org.unicornradio.app.podcasts.estUnEpisode
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
        val lecteur = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(http))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        lecteur.addListener(Journal)
        lecteur.addListener(suivi)

        val ouvrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaSession.Builder(this, lecteur).setSessionActivity(ouvrir).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        noterLaPosition()
        main.removeCallbacks(toutesLesDixSecondes)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    // ---- Où l'on en est d'un épisode ----

    private val main = Handler(Looper.getMainLooper())
    private val ecoutes by lazy { Ecoutes(this) }
    private val serveur by lazy { Serveur(this) }
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
