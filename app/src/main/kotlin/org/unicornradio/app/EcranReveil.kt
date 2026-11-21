package org.unicornradio.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import org.unicornradio.app.lecture.ServiceLecture
import org.unicornradio.app.lecture.Spectre

/**
 * L'écran du réveil, à l'heure dite : il allume le téléphone, par-dessus le
 * verrouillage, comme un réveil ordinaire. Le mode télé (spectre, heure,
 * logo, station et titre), et en haut les boutons : « Écouter » (une
 * station sonne : elle continue en écoute ordinaire, au casque seul s'il y
 * en a un), « Arrêter », « +1 min », « +10 min », en ligne en paysage,
 * empilés en portrait. Le retour referme l'écran : une station continue,
 * en écoute ordinaire ; une sonnerie s'arrête. Toucher l'écran ne fait rien,
 * pour qu'un geste ensommeillé ne coupe pas le réveil.
 *
 * Les boutons parlent au service ; l'écran se ferme de lui-même quand la
 * sonnerie est finie, d'où qu'elle finisse (la notification, le casque,
 * l'arrêt d'office).
 */
@UnstableApi
class EcranReveil : ComponentActivity() {

    private val minuterie = Handler(Looper.getMainLooper())
    private var arreterLaVue: (() -> Unit)? = null
    private var boutons: LinearLayout? = null
    private var ecouter: Button? = null

    private val surveiller = object : Runnable {
        override fun run() {
            if (!ServiceLecture.reveilAffiche) {
                finish()
                return
            }
            ecouter?.visibility = if (ServiceLecture.enCours?.stationAEcouter == true) android.view.View.VISIBLE else android.view.View.GONE
            minuterie.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Spectre.charger(this)
        val vue = ModeTele.construire(this, {
            ServiceLecture.enCours?.afficheDuReveil() ?: ModeTele.Affiche(getString(R.string.reveil_radio), null, null)
        }) { }
        arreterLaVue = vue.arreter

        fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
        fun bouton(texte: Int, quoi: () -> Unit) = Button(this).apply {
            setText(texte)
            isAllCaps = false
            textSize = 22f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.argb(60, 255, 255, 255))
                setStroke(dp(2), Color.argb(200, 255, 255, 255))
            }
            minWidth = dp(112)
            minHeight = dp(56)
            setPadding(dp(20), 0, dp(20), 0)
            setOnClickListener { quoi() }
        }
        val rangee = LinearLayout(this).apply {
            addView(bouton(R.string.reveil_ecouter) { commander(ServiceLecture.ACTION_CONTINUER) }.also { ecouter = it })
            addView(bouton(R.string.reveil_arreter) { commander(ServiceLecture.ACTION_ARRETER) })
            addView(bouton(R.string.reveil_plus_une) { commander(ServiceLecture.ACTION_REPORTER, 1) })
            addView(bouton(R.string.reveil_plus_dix) { commander(ServiceLecture.ACTION_REPORTER, 10) })
        }
        boutons = rangee
        val fond = FrameLayout(this)
        fond.addView(vue.racine, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        fond.addView(rangee)
        disposer(resources.configuration.orientation)
        setContentView(fond)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onBackPressedDispatcher.addCallback(this) { commander(ServiceLecture.ACTION_CONTINUER) }
    }

    override fun onConfigurationChanged(nouvelle: Configuration) {
        super.onConfigurationChanged(nouvelle)
        disposer(nouvelle.orientation)
    }

    /**
     * En paysage, les trois boutons en ligne, en haut à gauche, l'heure à
     * droite. En portrait, la largeur manque : empilés, de même largeur,
     * sous l'heure.
     */
    private fun disposer(orientation: Int) {
        val rangee = boutons ?: return
        fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
        val portrait = orientation == Configuration.ORIENTATION_PORTRAIT
        rangee.orientation = if (portrait) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        for (i in 0 until rangee.childCount) {
            rangee.getChildAt(i).layoutParams = if (portrait) {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) }
            } else {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(16) }
            }
        }
        rangee.layoutParams = if (portrait) {
            FrameLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(0, dp(88), 0, 0)
            }
        } else {
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
                setMargins(dp(32), dp(16), 0, 0)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        minuterie.post(surveiller)
    }

    override fun onStop() {
        minuterie.removeCallbacks(surveiller)
        super.onStop()
    }

    override fun onDestroy() {
        arreterLaVue?.invoke()
        super.onDestroy()
    }

    private fun commander(action: String, minutes: Int = 0) {
        try {
            startService(Intent(this, ServiceLecture::class.java).setAction(action).putExtra(ServiceLecture.MINUTES, minutes))
        } catch (e: Exception) {
            android.util.Log.w(org.unicornradio.app.fichier.Fichiers.JOURNAL, "écran du réveil : service injoignable : ${e.message}")
        }
        finish()
    }
}
