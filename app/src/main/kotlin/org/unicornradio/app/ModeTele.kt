package org.unicornradio.app

import android.app.Dialog
import android.app.Presentation
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.load
import org.unicornradio.app.lecture.Spectre

/**
 * Le mode télé : de quoi être vu de loin. Le spectre en grand, le logo, le
 * nom et le titre en gros, l'heure dans un coin, sur fond noir, l'écran
 * gardé allumé.
 *
 * Sur le téléphone, il prend tout l'écran, barres d'Android cachées, en
 * paysage. Avec un second écran branché (câble USB-C vers HDMI, Miracast),
 * il s'y affiche, et le téléphone reste libre ; un écran recopié (un
 * Chromecast) n'est pas un second écran, il montre ce que montre le
 * téléphone. La même vue servira sur une box Android TV.
 */
object ModeTele {

    /** Ce qui joue, relu chaque seconde. */
    class Affiche(val nom: String, val titre: String?, val logo: Uri?)

    /** Le mode télé sur un second écran, s'il est ouvert. */
    private var surEcran: Presentation? = null

    val surUnSecondEcran: Boolean
        get() = surEcran?.isShowing == true

    private fun secondEcran(ctx: Context): Display? =
        (ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
            .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()

    /** Ouvrir : sur le second écran s'il y en a un, sinon sur le téléphone. */
    fun ouvrir(a: AppCompatActivity, affiche: () -> Affiche) {
        Spectre.charger(a)
        val ecran = secondEcran(a)
        if (ecran != null) {
            fermerLeSecondEcran()
            val p = Presentation(a, ecran)
            val vue = construire(p.context, affiche) { }
            p.setContentView(vue.racine)
            p.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            p.setOnDismissListener { vue.arreter(); if (surEcran === p) surEcran = null }
            try {
                p.show()
                surEcran = p
                Toast.makeText(a, a.getString(R.string.tele_sur_l_ecran, ecran.name), Toast.LENGTH_SHORT).show()
                return
            } catch (e: WindowManager.InvalidDisplayException) {
                vue.arreter()
            }
        }
        surLeTelephone(a, affiche)
    }

    fun fermerLeSecondEcran() {
        surEcran?.dismiss()
        surEcran = null
    }

    private fun surLeTelephone(a: AppCompatActivity, affiche: () -> Affiche) {
        val fenetre = Dialog(a, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val vue = construire(a, affiche) { fenetre.dismiss() }
        vue.racine.setOnLongClickListener { ReglagesSon.spectre(a) { vue.spectre.relancer() }; true }
        fenetre.setContentView(vue.racine)
        val w = fenetre.window
        w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // En paysage, le temps du mode télé ; l'orientation d'avant revient en sortant.
        val avant = a.requestedOrientation
        a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        fenetre.setOnDismissListener {
            vue.arreter()
            a.requestedOrientation = avant
        }
        fenetre.show()
        // Les barres d'Android cachées ; un glissement du bord les montre un instant.
        if (w != null) {
            WindowCompat.setDecorFitsSystemWindows(w, false)
            WindowInsetsControllerCompat(w, w.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    internal class Vue(val racine: View, val spectre: VueSpectre, val arreter: () -> Unit)

    /** La vue du mode télé ; l'écran du réveil la reprend, ses boutons par-dessus. */
    internal fun construire(ctx: Context, affiche: () -> Affiche, surAppui: () -> Unit): Vue {
        fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
        val marge = dp(32)
        val fond = FrameLayout(ctx).apply { setBackgroundColor(Color.BLACK) }

        val spectre = VueSpectre(ctx)
        fond.addView(spectre, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            setMargins(marge, dp(72), marge, dp(170))
        })

        val heure = TextClock(ctx).apply {
            format12Hour = "h:mm"
            format24Hour = "HH:mm"
            setTextColor(Color.argb(220, 255, 255, 255))
            textSize = 30f
        }
        fond.addView(heure, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            setMargins(0, dp(16), marge, 0)
        })

        // En bas : le logo (ou les initiales), le nom, le titre.
        val cadreLogo = FrameLayout(ctx)
        val initiales = TextView(ctx).apply {
            setBackgroundResource(R.drawable.cercle)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 40f
        }
        val logo = ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE }
        cadreLogo.addView(initiales, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        cadreLogo.addView(logo, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val nom = TextView(ctx).apply {
            setTextColor(Color.WHITE); textSize = 36f; setSingleLine(); ellipsize = TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val titre = TextView(ctx).apply {
            setTextColor(Color.argb(210, 255, 255, 255)); textSize = 24f; maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        }
        val textes = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), 0, 0, 0)
            addView(nom); addView(titre)
        }
        val bas = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(marge, 0, marge, dp(24))
            addView(cadreLogo, LinearLayout.LayoutParams(dp(120), dp(120)))
            addView(textes, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        fond.addView(bas, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        fond.setOnClickListener { surAppui() }

        val minuterie = Handler(Looper.getMainLooper())
        var logoMontre: Uri? = null
        var premier = true
        val ecrire = object : Runnable {
            override fun run() {
                val a = affiche()
                nom.text = a.nom
                titre.text = a.titre ?: ""
                titre.visibility = if (a.titre.isNullOrBlank()) View.GONE else View.VISIBLE
                initiales.text = a.nom.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    .take(2).joinToString("") { it.first().uppercaseChar().toString() }
                if (premier || a.logo != logoMontre) {
                    premier = false
                    logoMontre = a.logo
                    initiales.visibility = View.VISIBLE
                    if (a.logo != null) logo.load(a.logo) { listener(onSuccess = { _, _ -> initiales.visibility = View.GONE }) }
                    else logo.setImageDrawable(null)
                }
                minuterie.postDelayed(this, 1_000)
            }
        }
        ecrire.run()
        return Vue(fond, spectre) { minuterie.removeCallbacks(ecrire) }
    }
}
