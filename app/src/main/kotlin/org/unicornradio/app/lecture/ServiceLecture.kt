package org.unicornradio.app.lecture

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.core.app.ServiceCompat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.unicornradio.app.BuildConfig
import org.unicornradio.app.MainActivity
import org.unicornradio.app.R
import org.unicornradio.app.annuaire.Annuaire
import org.unicornradio.app.annuaire.Retrouver
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.modele.Station
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

    companion object {
        /** L'écran du réveil : « Arrêter ». */
        const val ACTION_ARRETER = "org.unicornradio.app.REVEIL_ARRETER"
        /** L'écran du réveil : « +1 min », « +10 min », les minutes dans `MINUTES`. */
        const val ACTION_REPORTER = "org.unicornradio.app.REVEIL_REPORTER"
        /** L'écran du réveil refermé (retour) : la station continue, en écoute ordinaire ; une sonnerie s'arrête. */
        const val ACTION_CONTINUER = "org.unicornradio.app.REVEIL_CONTINUER"
        const val MINUTES = "minutes"

        /** Le service vivant, pour l'écran du réveil (même processus). */
        @Volatile
        var enCours: ServiceLecture? = null
            private set

        /** Vrai tant que l'écran du réveil doit rester : il se ferme de lui-même ensuite. */
        val reveilAffiche: Boolean
            get() = enCours?.ecranVoulu == true
    }

    override fun onCreate() {
        super.onCreate()
        enCours = this
        // Un user agent nommé, les redirections http vers https acceptées, et
        // douze secondes sans réponse ou sans octet valent un échec : l'essai
        // suivant plutôt qu'une « mise en mémoire » sans fin.
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Unicorn-Radio/${BuildConfig.VERSION_NAME}")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(12_000)
        // Le http pour les flux et les épisodes en ligne ; `DefaultDataSource` par-dessus lit aussi les fichiers téléchargés.
        // L'égaliseur, puis la dérivation du spectre, dans la chaîne audio du lecteur.
        Egaliseur.charger(this)
        Spectre.charger(this)
        val rendus = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: android.content.Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean) =
                androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(Egaliseur.Processeur(), androidx.media3.exoplayer.audio.TeeAudioProcessor(Spectre.capteur)))
                    .build()
        }
        val lecteur = ExoPlayer.Builder(this, rendus)
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
        lecteur.addListener(widget)
        lecteur.addListener(finDuReveil)
        lecteur.addListener(enBoucle)

        val ouvrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // La notification du lecteur porte la licorne dans la barre d'état, pas l'icône neutre de Media3.
        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_licorne) })
        session = MediaSession.Builder(this, lecteur)
            .setSessionActivity(ouvrir)
            .setCallback(reprendreLaDerniere)
            .build()
        // La minuterie d'extinction, programmée par l'écran : à l'échéance, la pause.
        Extinction.arret = { session?.player?.pause() }
        // Un autre réseau pendant la lecture : le serveur sera recherché à la poussée suivante.
        Reseau.surveiller(this)
        // Le réseau qui revient : une lecture tombée sur la coupure repart.
        Reseau.surRetour = { main.post { reprendreApresLaCoupure() } }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        enCours = null
        main.removeCallbacks(sonnerParDessus)
        arreterLaCloche(reprendre = false)
        cacherLEcran()
        noterLaPosition()
        main.removeCallbacks(toutesLesDixSecondes)
        Extinction.annuler()
        Extinction.arret = null
        Widget.mettreAJour(this, DerniereEcoute.nom(this), null, enLecture = false)
        Reseau.surRetour = null
        Reseau.laisser()
        relacherLeRenfort()
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
            // Une erreur de réseau : notée, le retour du réseau relancera.
            // Hors ligne, l'annuaire ne répondrait pas : on ne le consulte pas.
            if (error.errorCode in ERREURS_DE_RESEAU) {
                tombeeLe = SystemClock.elapsedRealtime()
                if (!Reseau.enLigne(this@ServiceLecture)) {
                    Log.i(Fichiers.JOURNAL, "réseau : lecture tombée sur une coupure, reprise au retour du réseau")
                    return
                }
            }
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

    // ---- Les réveils et les comptes à rebours ----

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ARRETER -> arreterLeReveil(continuer = false)
            ACTION_CONTINUER -> arreterLeReveil(continuer = true)
            ACTION_REPORTER -> reporter(intent.getIntExtra(MINUTES, 10))
        }
        if (intent?.action == Reveil.ACTION) {
            val alarme = Reveil.alarmeDe(intent.getStringExtra(Reveil.ALARME))
            passerAuPremierPlan(alarme)
            if (alarme == null || !sonner(alarme, intent.getLongExtra(Reveil.HEURE_DITE, System.currentTimeMillis()))) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Lancé par une alarme (`startForegroundService`), le service doit passer
     * au premier plan dans les secondes qui suivent, sinon Android l'arrête
     * (plantage d'un essai, `ForegroundServiceDidNotStartInTimeException`).
     * Media3 ne le fait qu'une fois son propre contrôleur connecté et la
     * station chargée : trop tard parfois. Alors tout de suite, une
     * notification sous le numéro de celle de Media3, qui la remplace dès
     * qu'elle vient.
     */
    private fun passerAuPremierPlan(alarme: Reveil.Alarme?) {
        val gestionnaire = getSystemService(android.app.NotificationManager::class.java)
        if (gestionnaire.getNotificationChannel(CANAL_REVEIL) == null) {
            gestionnaire.createNotificationChannel(
                android.app.NotificationChannel(CANAL_REVEIL, getString(R.string.reveil_radio), android.app.NotificationManager.IMPORTANCE_LOW),
            )
        }
        // Un appui ouvre l'application ; « Arrêter » met en pause, ce qui finit la sonnerie.
        // Cette notification peut rester celle qu'on voit (essai : un appui n'ouvrait rien).
        val ouvrir = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val arreter = PendingIntent.getBroadcast(
            this, 3,
            Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(android.content.ComponentName(this, androidx.media3.session.MediaButtonReceiver::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = androidx.core.app.NotificationCompat.Builder(this, CANAL_REVEIL)
            .setSmallIcon(R.drawable.ic_licorne)
            .setColor(getColor(R.color.licorne))
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.drawable.logo))
            .setContentTitle(alarme?.nom ?: getString(R.string.reveil_radio))
            .setContentText(alarme?.let { Reveil.sonTexte(it.son) })
            .setContentIntent(ouvrir)
            .addAction(0, getString(R.string.arreter_la_lecture), arreter)
            .setOngoing(true)
            .build()
        try {
            ServiceCompat.startForeground(
                this, androidx.media3.session.DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "alarme : premier plan refusé : ${e.message}")
        }
    }

    /** Vrai de la sonnerie jusqu'à la première pause ou à une autre écoute. */
    private var reveilEnCours = false

    /** L'identifiant de ce qui sonne dans le lecteur : la station, ou la sonnerie. */
    private var idQuiSonne: String? = null

    /** Le volume monte-t-il, ou part-il au plein à l'heure dite. */
    private var progressif = true

    /** Quand la montée du son a commencé (horloge du système depuis le démarrage), 0 avant l'heure dite. */
    private var monteeDepuis = 0L

    /**
     * La montée du son, une fois par seconde pendant une minute : le volume
     * est recalculé d'après le temps écoulé, au cube pour l'oreille, presque
     * rien les premières secondes, plein à la fin. Recalculé, pas ajouté :
     * rien ne peut le faire dériver.
     */
    private val monterLeSon = object : Runnable {
        override fun run() {
            val p = session?.player ?: return
            if (!reveilEnCours || monteeDepuis == 0L) return
            val f = ((SystemClock.elapsedRealtime() - monteeDepuis) / MONTEE_MS.toFloat()).coerceIn(0f, 1f)
            p.volume = (0.02f + 0.98f * f * f * f).coerceAtMost(1f)
            if (f < 1f) main.postDelayed(this, 1_000) else Log.i(Fichiers.JOURNAL, "alarme : plein son")
        }
    }

    /** Le son, à l'heure dite : il monte en une minute, ou part au plein, selon l'alarme. */
    private fun donnerLeSon() {
        val p = session?.player ?: return
        main.removeCallbacks(monterLeSon)
        if (progressif) {
            monteeDepuis = SystemClock.elapsedRealtime()
            main.post(monterLeSon)
        } else {
            p.volume = 1f
        }
    }

    /**
     * L'heure dite, pour une station : elle joue depuis deux minutes, en
     * muet, la publicité du début passée ; le son vient. Si elle ne joue pas,
     * la radio de secours, si l'alarme en a une et qu'elle n'a pas déjà été
     * essayée, avec `ATTENTE_SECOURS_MS` pour partir ; sinon, ou si elle ne
     * part pas, la sonnerie du téléphone.
     */
    private val aLHeureDite = Runnable {
        val p = session?.player ?: return@Runnable
        if (!reveilEnCours) return@Runnable
        if (!p.isPlaying) {
            if (passerAuSecours()) {
                main.postDelayed(apresLeSecours, ATTENTE_SECOURS_MS)
            } else {
                Log.w(Fichiers.JOURNAL, "alarme : la station ne joue pas, la sonnerie du téléphone à la place")
                jouerUneSonnerie(null)
            }
        }
        Log.i(Fichiers.JOURNAL, "alarme : l'heure dite")
        donnerLeSon()
        montrerLEcran()
    }

    /** La radio de secours de l'alarme qui sonne, `null` sans elle ; et si elle a déjà été lancée. */
    private var secours: Station? = null
    private var secoursEssaye = false

    /**
     * La station ne joue pas : la radio de secours prend sa place, au même
     * volume (muet avant l'heure dite, la montée ensuite). Faux s'il n'y en a
     * pas, ou si elle a déjà été essayée.
     */
    private fun passerAuSecours(): Boolean {
        val p = session?.player ?: return false
        val s = secours ?: return false
        if (secoursEssaye) return false
        secoursEssaye = true
        Log.w(Fichiers.JOURNAL, "alarme : la station ne joue pas, la radio de secours « ${s.nom} »")
        // Avant le changement : le lecteur qui passe à la radio de secours ne doit pas finir le réveil.
        idQuiSonne = s.uuid
        p.setMediaItem(s.versElement())
        p.prepare()
        p.play()
        return true
    }

    /** Après l'heure dite, la radio de secours a eu son temps : si elle ne joue pas, la sonnerie. */
    private val apresLeSecours = Runnable {
        val p = session?.player ?: return@Runnable
        if (!reveilEnCours || idQuiSonne == ID_SONNERIE || p.isPlaying) return@Runnable
        Log.w(Fichiers.JOURNAL, "alarme : la radio de secours ne joue pas non plus, la sonnerie du téléphone")
        jouerUneSonnerie(null)
    }

    /** Une sonnerie du téléphone en boucle, celle choisie, ou celle du réveil d'Android ; en muet, le son vient ensuite. */
    private fun jouerUneSonnerie(uri: String?) {
        val p = session?.player ?: return
        val adresse = uri?.let { android.net.Uri.parse(it) }
            ?: android.media.RingtoneManager.getActualDefaultRingtoneUri(this, android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: return
        idQuiSonne = ID_SONNERIE
        p.setMediaItem(
            MediaItem.Builder().setMediaId(ID_SONNERIE).setUri(adresse)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(getString(R.string.reveil_radio)).build())
                .build(),
        )
        p.repeatMode = Player.REPEAT_MODE_ONE
        p.volume = 0f
        renforcer()
        p.prepare()
        p.play()
        // La station est tombée l'écran montré : la notification perd « Écouter ».
        if (ecranVoulu) montrerLEcran()
    }

    /** Le renfort de la sonnerie du téléphone, posé sur la session audio du lecteur le temps qu'elle sonne. */
    private var renfort: LoudnessEnhancer? = null

    /**
     * Une sonnerie d'Android est enregistrée bien plus bas qu'une radio,
     * diffusée très compressée : au même volume, elle sonnait moins fort.
     * Le renfort d'Android la remonte, son limiteur évite la saturation ;
     * la montée progressive reste la même, elle finit au niveau d'une radio.
     */
    private fun renforcer() {
        if (renfort != null) return
        val exo = session?.player as? ExoPlayer ?: return
        renfort = renfortSur(exo.audioSessionId)
    }

    private fun renfortSur(sessionAudio: Int): LoudnessEnhancer? = try {
        LoudnessEnhancer(sessionAudio).apply {
            setTargetGain(GAIN_SONNERIE_MB)
            enabled = true
        }
    } catch (e: Exception) {
        Log.w(Fichiers.JOURNAL, "alarme : pas de renfort pour la sonnerie : ${e.message}")
        null
    }

    private fun relacherLeRenfort() {
        try {
            renfort?.release()
        } catch (e: Exception) {
            // Déjà libéré : rien à faire.
        }
        renfort = null
    }

    /**
     * Une alarme sonne, sur le volume du réveil d'Android (elle sonne même
     * en silencieux). Une station démarre deux minutes avant l'heure dite, en
     * muet, le temps que la publicité du début passe ; à l'heure dite, le son
     * vient (`aLHeureDite`), ou la sonnerie du téléphone si rien ne joue. Une
     * sonnerie choisie démarre à l'heure. Arrêt d'office au bout d'une heure
     * sans un geste.
     */
    private fun sonner(alarme: Reveil.Alarme, heureDite: Long): Boolean {
        val p = session?.player ?: return false
        Log.i(Fichiers.JOURNAL, "alarme « ${alarme.nom ?: "sans nom"} » : ${Reveil.sonTexte(alarme.son)}")
        // Une autre sonnerie en cours s'efface devant celle-ci ; ce qu'elle avait interrompu reste en pause.
        main.removeCallbacks(sonnerParDessus)
        parDessus = null
        arreterLaCloche(reprendre = false)
        // Quelque chose s'écoute déjà, hors réveil : on n'y touche pas avant l'heure, puis la sonnerie par-dessus.
        if (!reveilEnCours && p.currentMediaItem != null && p.playWhenReady && p.playbackState != Player.STATE_IDLE) {
            Log.i(Fichiers.JOURNAL, "alarme : une écoute en cours, la sonnerie du téléphone à l'heure dite, par-dessus")
            parDessus = alarme
            main.postDelayed(sonnerParDessus, (heureDite - System.currentTimeMillis()).coerceIn(0L, 2 * Reveil.AVANCE_MS))
            return true
        }
        alarmeQuiSonne = alarme
        reveilEnCours = true
        progressif = alarme.progressif
        monteeDepuis = 0L
        main.removeCallbacks(monterLeSon)
        main.removeCallbacks(aLHeureDite)
        main.removeCallbacks(apresLeSecours)
        val principale = (alarme.son as? Reveil.Son.DeStation)?.station
        secours = alarme.secours?.takeIf { principale != null && it.uuid != principale.uuid }
        secoursEssaye = false
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_ALARM).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        when (val son = alarme.son) {
            is Reveil.Son.DeStation -> {
                idQuiSonne = son.station.uuid
                p.repeatMode = Player.REPEAT_MODE_OFF
                p.volume = 0f
                p.setMediaItem(son.station.versElement())
                p.prepare()
                p.play()
                val attente = (heureDite - System.currentTimeMillis()).coerceIn(0L, 2 * Reveil.AVANCE_MS)
                main.postDelayed(aLHeureDite, attente)
            }
            is Reveil.Son.Sonnerie -> {
                jouerUneSonnerie(son.uri)
                donnerLeSon()
                montrerLEcran()
            }
        }
        Extinction.programmer(61)
        extinctionDuReveil = true
        return true
    }

    /** L'alarme qui sonne ou vient de sonner : son nom à l'écran, et ce que « +1 min » reprogramme. */
    private var alarmeQuiSonne: Reveil.Alarme? = null

    /** La minuterie d'une heure posée par le réveil : elle s'en va avec lui. */
    private var extinctionDuReveil = false

    // ---- La sonnerie par-dessus une écoute ----

    /** L'alarme qui attend son heure pendant une écoute ; `null` sinon. */
    private var parDessus: Reveil.Alarme? = null

    /** La sonnerie par-dessus, dans un lecteur à elle : celui de l'écoute garde sa file et sa position. */
    private var cloche: android.media.MediaPlayer? = null
    private var renfortCloche: LoudnessEnhancer? = null

    /** L'écoute jouait à l'heure dite : elle reprend quand la sonnerie s'arrête. */
    private var reprendreApres = false

    /**
     * L'heure dite pendant une écoute : elle se met en pause, la sonnerie du
     * téléphone part au plein (on est réveillé), l'écran s'allume. On ne la
     * confond pas avec une station de plus.
     */
    private val sonnerParDessus = Runnable {
        val a = parDessus ?: return@Runnable
        parDessus = null
        val p = session?.player ?: return@Runnable
        reprendreApres = p.currentMediaItem != null && p.playWhenReady && p.playbackState != Player.STATE_IDLE
        p.pause()
        val adresse = (a.son as? Reveil.Son.Sonnerie)?.uri?.let { android.net.Uri.parse(it) }
            ?: android.media.RingtoneManager.getActualDefaultRingtoneUri(this, android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        try {
            cloche = android.media.MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@ServiceLecture, adresse)
                isLooping = true
                prepare()
                start()
            }
            renfortCloche = cloche?.audioSessionId?.let(::renfortSur)
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "alarme : sonnerie par-dessus impossible : ${e.message}")
            cloche?.release()
            cloche = null
        }
        Log.i(Fichiers.JOURNAL, "alarme : l'heure dite, sonnerie par-dessus l'écoute")
        alarmeQuiSonne = a
        montrerLEcran()
        main.postDelayed(finDeLaCloche, DUREE_CLOCHE_MS)
    }

    /** Personne n'a répondu : la sonnerie s'arrête d'elle-même, l'écoute reprend. */
    private val finDeLaCloche = Runnable { arreterLaCloche(reprendre = true) }

    private fun arreterLaCloche(reprendre: Boolean) {
        main.removeCallbacks(finDeLaCloche)
        val c = cloche ?: return
        cloche = null
        try {
            c.stop()
        } catch (e: Exception) {
            // Pas démarrée : rien à arrêter.
        }
        c.release()
        try {
            renfortCloche?.release()
        } catch (e: Exception) {
            // Déjà libéré.
        }
        renfortCloche = null
        cacherLEcran()
        if (reprendre && reprendreApres) session?.player?.play()
        reprendreApres = false
    }

    // ---- L'écran du réveil, à l'heure dite ----

    /** Vrai de l'heure dite à la fin de la sonnerie : l'écran reste, sa notification aussi. */
    @Volatile
    private var ecranVoulu = false

    /**
     * À l'heure dite, pas au départ deux minutes avant : une notification
     * d'alarme qui allume l'écran, par-dessus le verrouillage, sur l'écran
     * du réveil (`EcranReveil`). Si Android ne laisse pas l'application
     * passer en plein écran, ou si le téléphone est en main, elle reste une
     * notification, avec les mêmes boutons ; un appui ouvre l'application.
     */
    private fun montrerLEcran() {
        ecranVoulu = true
        val gestionnaire = getSystemService(android.app.NotificationManager::class.java)
        if (gestionnaire.getNotificationChannel(CANAL_SONNERIE) == null) {
            gestionnaire.createNotificationChannel(
                android.app.NotificationChannel(CANAL_SONNERIE, getString(R.string.sonnerie_canal), android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                    // Le son est celui du lecteur ; la notification n'en ajoute pas.
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
        val ecran = PendingIntent.getActivity(
            this, 6,
            Intent(this, org.unicornradio.app.EcranReveil::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Un appui sur la notification : l'application, sur ce qui joue.
        val application = PendingIntent.getActivity(
            this, 10, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun commande(code: Int, action: String, minutes: Int = 0) = PendingIntent.getService(
            this, code, Intent(this, ServiceLecture::class.java).setAction(action).putExtra(MINUTES, minutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val a = alarmeQuiSonne
        val notification = androidx.core.app.NotificationCompat.Builder(this, CANAL_SONNERIE)
            .setSmallIcon(R.drawable.ic_licorne)
            .setColor(getColor(R.color.licorne))
            .setContentTitle(a?.nom ?: getString(R.string.reveil_radio))
            .setContentText(a?.let { Reveil.sonTexte(it.son) })
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(application)
            .setFullScreenIntent(ecran, true)
            .apply {
                // Trois boutons au plus dans une notification : avec une station, « Écouter » prend la place de « +1 min ».
                if (stationAEcouter) addAction(0, getString(R.string.reveil_ecouter), commande(11, ACTION_CONTINUER))
                addAction(0, getString(R.string.reveil_arreter), commande(7, ACTION_ARRETER))
                if (!stationAEcouter) addAction(0, getString(R.string.reveil_plus_une), commande(8, ACTION_REPORTER, 1))
                addAction(0, getString(R.string.reveil_plus_dix), commande(9, ACTION_REPORTER, 10))
            }
            .build()
        try {
            gestionnaire.notify(ID_NOTIFICATION_SONNERIE, notification)
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "alarme : notification de l'écran refusée : ${e.message}")
        }
    }

    /**
     * Une station sonne, pas une sonnerie : « Écouter » la garde, en écoute
     * ordinaire. Le son d'une alarme sort aussi par le haut-parleur, casque
     * branché ; celui d'une écoute, par le casque seul.
     */
    val stationAEcouter: Boolean
        get() = reveilEnCours && cloche == null && idQuiSonne != ID_SONNERIE

    private fun cacherLEcran() {
        ecranVoulu = false
        getSystemService(android.app.NotificationManager::class.java)?.cancel(ID_NOTIFICATION_SONNERIE)
    }

    /** Ce que montre l'écran du réveil : la station et son titre, ou le nom de l'alarme sur une sonnerie. */
    fun afficheDuReveil(): org.unicornradio.app.ModeTele.Affiche {
        val a = alarmeQuiSonne
        val nomAlarme = a?.nom ?: getString(R.string.reveil_radio)
        val item = session?.player?.currentMediaItem
        if (cloche != null || item == null || item.mediaId == ID_SONNERIE) {
            return org.unicornradio.app.ModeTele.Affiche(nomAlarme, (a?.son as? Reveil.Son.Sonnerie)?.titre, null)
        }
        val fiche = item.mediaMetadata
        val nom = (fiche.station ?: fiche.title)?.toString() ?: nomAlarme
        return org.unicornradio.app.ModeTele.Affiche(
            nom, fiche.title?.toString()?.takeIf { it != nom }, fiche.artworkUri?.takeIf { it != LOGO_PAR_DEFAUT },
        )
    }

    /**
     * « Arrêter » : la sonnerie par-dessus s'arrête et l'écoute reprend ; un
     * réveil se met en pause. Écran refermé (`continuer`) : une station
     * continue, en écoute ordinaire ; une sonnerie s'arrête.
     */
    private fun arreterLeReveil(continuer: Boolean) {
        Log.i(Fichiers.JOURNAL, "alarme : ${if (continuer) "écran refermé" else "arrêtée"}")
        if (cloche != null) {
            arreterLaCloche(reprendre = true)
        } else if (reveilEnCours) {
            val p = session?.player
            if (continuer && idQuiSonne != ID_SONNERIE) finirLeReveil() else p?.pause()
        }
        cacherLEcran()
    }

    /**
     * « +1 min », « +10 min ».
     *
     * Un réveil qui sonne (station, radio de secours ou sonnerie) : rien ne
     * s'arrête, le son est coupé, l'écran se ferme ; au bout du temps, c'est
     * de nouveau l'heure dite (`aLHeureDite`) : la montée du son, l'écran.
     * La station ne redémarre pas, elle n'a pas à se reconnecter. Mettre en
     * pause pendant ce temps annule tout.
     *
     * La sonnerie par-dessus une écoute : elle s'arrête, l'écoute reste en
     * pause, et la même alarme revient dans tant de minutes, en compte à
     * rebours sans nom (il paraît dans la liste, on peut l'annuler) : rien
     * ne jouant alors, avec la station de l'alarme, en avance et en muet.
     */
    private fun reporter(minutes: Int) {
        val a = alarmeQuiSonne
        val fin = System.currentTimeMillis() + minutes * 60_000L
        val p = session?.player
        if (cloche == null && reveilEnCours && p != null) {
            Log.i(Fichiers.JOURNAL, "alarme : reportée de $minutes min, en muet")
            main.removeCallbacks(monterLeSon)
            main.removeCallbacks(aLHeureDite)
            main.removeCallbacks(apresLeSecours)
            monteeDepuis = 0L
            p.volume = 0f
            cacherLEcran()
            main.postDelayed(aLHeureDite, minutes * 60_000L)
            // L'arrêt d'office, une heure après la nouvelle sonnerie.
            Extinction.programmer(minutes + 61)
            extinctionDuReveil = true
            dire(getString(R.string.reveil_reporte, android.text.format.DateFormat.getTimeFormat(this).format(java.util.Date(fin))))
            return
        }
        // La sonnerie par-dessus : elle s'arrête, l'écoute reste en pause, comme le silence d'un réveil ;
        // le report la trouvera arrêtée, et sonnera avec la station de l'alarme.
        if (cloche != null) {
            reprendreApres = false
            arreterLaCloche(reprendre = false)
        }
        arreterLeReveil(continuer = false)
        if (a == null) return
        val report = Reveil.Alarme(
            id = Reveil.nouvelId(this), genre = Reveil.Genre.COMPTE, nom = null, minutes = minutes,
            son = a.son, progressif = a.progressif, actif = true, fin = fin, secours = a.secours,
        )
        Log.i(Fichiers.JOURNAL, "alarme : reportée de $minutes min")
        if (Reveil.garder(this, report) != null) {
            dire(getString(R.string.reveil_reporte, android.text.format.DateFormat.getTimeFormat(this).format(java.util.Date(fin))))
        }
    }

    /** La sonnerie finie, par une pause ou une autre écoute : le lecteur redevient ordinaire. */
    private fun finirLeReveil() {
        if (!reveilEnCours) return
        reveilEnCours = false
        idQuiSonne = null
        main.removeCallbacks(monterLeSon)
        main.removeCallbacks(aLHeureDite)
        main.removeCallbacks(apresLeSecours)
        secours = null
        relacherLeRenfort()
        cacherLEcran()
        if (extinctionDuReveil) {
            extinctionDuReveil = false
            Extinction.annuler()
        }
        val p = session?.player ?: return
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        p.repeatMode = Player.REPEAT_MODE_OFF
        p.volume = 1f
        if (p.currentMediaItem?.mediaId == ID_SONNERIE) { p.stop(); p.clearMediaItems() }
    }

    /**
     * Une liste de stations tourne en boucle : « suivante » sur la dernière
     * revient à la première, « précédente » sur la première va à la
     * dernière, ici comme au casque, dans la notification ou le widget. Pas
     * les épisodes, qui ne recommencent pas une file finie ; pas une station
     * seule ; et le réveil règle lui-même sa répétition.
     */
    private val enBoucle = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (!events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) return
            if (reveilEnCours) return
            val item = player.currentMediaItem ?: return
            val voulu = if (!item.estUnEpisode && player.mediaItemCount > 1) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            if (player.repeatMode != voulu) player.repeatMode = voulu
        }
    }

    private val finDuReveil = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) finirLeReveil()
            // Lecture relancée à la main pendant la sonnerie par-dessus : c'est entendu, elle s'arrête.
            else if (cloche != null) arreterLaCloche(reprendre = false)
        }

        /**
         * Le flux d'une station tombe pendant le réveil : la radio de secours
         * tout de suite, sans attendre l'heure dite, pour qu'elle passe sa
         * publicité en muet. Sans elle, ou déjà essayée : avant l'heure dite,
         * on attend (`aLHeureDite` sonnera) ; après, la sonnerie.
         */
        override fun onPlayerError(error: PlaybackException) {
            if (!reveilEnCours || idQuiSonne == ID_SONNERIE) return
            main.post {
                if (!reveilEnCours || idQuiSonne == ID_SONNERIE) return@post
                if (passerAuSecours()) {
                    if (monteeDepuis != 0L) main.postDelayed(apresLeSecours, ATTENTE_SECOURS_MS)
                } else if (monteeDepuis != 0L) {
                    Log.w(Fichiers.JOURNAL, "alarme : le flux est tombé, la sonnerie du téléphone")
                    jouerUneSonnerie(null)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem?.mediaId != idQuiSonne && mediaItem?.mediaId != ID_SONNERIE) finirLeReveil()
        }
    }

    // ---- La dernière écoute, et le widget ----

    /**
     * « Lecture » sans rien dans le lecteur, depuis le widget, le casque ou
     * les commandes de lecture d'Android : la dernière écoute est remise.
     */
    private val reprendreLaDerniere = object : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val a = DerniereEcoute.aJouer(this@ServiceLecture)
                ?: return Futures.immediateFailedFuture(UnsupportedOperationException("rien à reprendre"))
            Log.i(Fichiers.JOURNAL, "reprise de la dernière écoute")
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(a.elements, a.index, a.positionMs))
        }
    }

    /** Le widget suit le lecteur : ce qui joue, le titre en cours, lecture ou pause. */
    private val widget = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (!events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED,
                )
            ) return
            val item = player.currentMediaItem
            val fiche = item?.mediaMetadata
            val nom = when {
                item == null -> DerniereEcoute.nom(this@ServiceLecture)
                item.estUnEpisode -> fiche?.title?.toString()
                else -> fiche?.station?.toString()
            }
            val titre = when {
                item == null -> null
                item.estUnEpisode -> fiche?.artist?.toString()
                else -> fiche?.title?.toString()?.takeIf { it != nom }
            }
            Widget.mettreAJour(this@ServiceLecture, nom, titre, enLecture = player.playWhenReady && player.playbackState != Player.STATE_IDLE)
        }
    }

    // ---- Reprendre au retour du réseau ----

    /** Quand la lecture est tombée sur une erreur de réseau ; 0 si rien n'est à reprendre. */
    private var tombeeLe = 0L

    /**
     * Le réseau est revenu : si la lecture est en erreur de réseau, toujours
     * voulue (pas de pause ni d'arrêt depuis) et tombée il y a moins de
     * trente minutes, elle repart, à la même position pour un épisode.
     */
    private fun reprendreApresLaCoupure() {
        val p = session?.player ?: return
        val quand = tombeeLe
        tombeeLe = 0L
        if (quand == 0L) return
        val erreur = p.playerError ?: return
        if (erreur.errorCode !in ERREURS_DE_RESEAU || !p.playWhenReady || p.currentMediaItem == null) return
        if (SystemClock.elapsedRealtime() - quand > TRENTE_MINUTES_MS) {
            Log.i(Fichiers.JOURNAL, "réseau : revenu, mais la coupure dure depuis plus de trente minutes, pas de reprise")
            return
        }
        val nom = (p.currentMediaItem?.mediaMetadata?.station ?: p.currentMediaItem?.mediaMetadata?.title ?: "").toString()
        Log.i(Fichiers.JOURNAL, "réseau : revenu, « $nom » reprend")
        p.prepare()
        p.play()
        dire(getString(R.string.reprise_au_retour_du_reseau, nom))
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
            // Ce qu'on écoute devient la dernière écoute, que l'ouverture, le widget et Android savent reprendre.
            if (mediaItem != null) DerniereEcoute.noter(this@ServiceLecture, mediaItem)
            // La vitesse : celle choisie pour un épisode, 1× pour une station.
            if (mediaItem != null) session?.player?.setPlaybackSpeed(if (mediaItem.episode != null) Vitesse.lue(this@ServiceLecture) else 1f)
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

/** Les erreurs d'une coupure : connexion impossible, délai dépassé, entrée-sortie sans détail. */
private val ERREURS_DE_RESEAU = setOf(
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
)

private const val TRENTE_MINUTES_MS = 30 * 60_000L

/** L'identifiant de la sonnerie de secours du réveil, dans le lecteur. */
private const val ID_SONNERIE = "reveil-sonnerie"

/** La montée du son du réveil, du silence au plein. */
private const val MONTEE_MS = 60_000L

/** Après l'heure dite, le temps laissé à la radio de secours pour partir avant la sonnerie. */
private const val ATTENTE_SECOURS_MS = 20_000L

/** Le renfort de la sonnerie, en millièmes de décibel : +10 dB. */
private const val GAIN_SONNERIE_MB = 1000

/** Le canal de la notification d'attente du réveil, le temps que celle de Media3 vienne. */
private const val CANAL_REVEIL = "reveil"

/** Le canal de l'alarme qui sonne, celle qui allume l'écran. */
private const val CANAL_SONNERIE = "sonnerie"

private const val ID_NOTIFICATION_SONNERIE = 2001

/** La sonnerie par-dessus une écoute s'arrête d'elle-même au bout de dix minutes. */
private const val DUREE_CLOCHE_MS = 10 * 60_000L
