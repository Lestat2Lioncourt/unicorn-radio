package org.unicornradio.app

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import org.unicornradio.app.lecture.Egaliseur
import org.unicornradio.app.lecture.Spectre
import kotlin.math.ln
import kotlin.math.pow

/** Les réglages du son : l'égaliseur, le spectre, et le spectre en grand. */
object ReglagesSon {

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    // ---- L'égaliseur ----

    private fun nomDuPrereglage(ctx: Context, nom: String): String = when (nom) {
        "plat" -> ctx.getString(R.string.eq_plat)
        "rock" -> ctx.getString(R.string.eq_rock)
        "pop" -> ctx.getString(R.string.eq_pop)
        "jazz" -> ctx.getString(R.string.eq_jazz)
        "classique" -> ctx.getString(R.string.eq_classique)
        "basses" -> ctx.getString(R.string.eq_basses)
        "voix" -> ctx.getString(R.string.eq_voix)
        Egaliseur.PERSO -> ctx.getString(R.string.eq_perso)
        else -> nom
    }

    private fun frequence(f: Int) = if (f >= 1_000) "${f / 1_000} k" else "$f"

    fun egaliseur(a: AppCompatActivity) {
        Egaliseur.charger(a)
        val marge = dp(a, 20)
        val paysage = enPaysage(a)
        val boite = LinearLayout(a).apply {
            orientation = if (paysage) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(marge, dp(a, 8), marge, 0)
        }
        // En paysage : à gauche l'interrupteur, le préréglage, la courbe, les boutons ; à droite les bandes, en deux colonnes de cinq.
        val gauche: LinearLayout
        val colonnes: List<LinearLayout>
        if (paysage) {
            gauche = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            colonnes = List(2) { LinearLayout(a).apply { orientation = LinearLayout.VERTICAL } }
            val droite = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
            colonnes.forEach { droite.addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) }
            boite.addView(gauche, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = marge })
            boite.addView(droite, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f))
        } else {
            gauche = boite
            colonnes = listOf(boite)
        }

        val interrupteur = MaterialSwitch(a).apply {
            setText(R.string.eq_activer)
            isChecked = Egaliseur.actif
        }
        gauche.addView(interrupteur)

        val courbe = VueCourbe(a)
        gauche.addView(courbe, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, if (paysage) 90 else 110)).apply { topMargin = dp(a, 8) })

        val prereglage = MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        gauche.addView(prereglage, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(a, 8) })

        val curseurs = ArrayList<SeekBar>()
        val valeurs = ArrayList<TextView>()
        var enCours = false
        val supprimer = MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply { setText(R.string.eq_supprimer) }

        fun rafraichir() {
            enCours = true
            interrupteur.isChecked = Egaliseur.actif
            prereglage.text = a.getString(R.string.eq_prereglage, nomDuPrereglage(a, Egaliseur.choisi))
            val g = Egaliseur.gains
            for (i in g.indices) {
                curseurs[i].progress = g[i] + Egaliseur.MAX_DB
                valeurs[i].text = Egaliseur.libelle(g[i])
            }
            supprimer.visibility = if (Egaliseur.lesSiens(a).any { it.nom == Egaliseur.choisi }) View.VISIBLE else View.GONE
            courbe.invalidate()
            enCours = false
        }

        for (i in Egaliseur.FREQUENCES.indices) {
            val ligne = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            ligne.addView(TextView(a).apply {
                text = frequence(Egaliseur.FREQUENCES[i])
                minWidth = dp(a, 36)
                textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            })
            val curseur = SeekBar(a).apply { max = 2 * Egaliseur.MAX_DB }
            ligne.addView(curseur, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val valeur = TextView(a).apply { minWidth = dp(a, 32) }
            ligne.addView(valeur)
            curseur.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, parUtilisateur: Boolean) {
                    if (!parUtilisateur || enCours) return
                    // Bouger une bande allume l'égaliseur : on veut l'entendre.
                    if (!Egaliseur.actif) Egaliseur.activer(a, true)
                    Egaliseur.regler(a, i, p - Egaliseur.MAX_DB)
                    rafraichir()
                }
                override fun onStartTrackingTouch(s: SeekBar) = Unit
                override fun onStopTrackingTouch(s: SeekBar) = Unit
            })
            curseurs += curseur
            valeurs += valeur
            colonnes[if (colonnes.size == 2 && i >= Egaliseur.FREQUENCES.size / 2) 1 else 0].addView(ligne)
        }

        // En paysage, les deux boutons l'un sous l'autre : la colonne est étroite.
        val bas = LinearLayout(a).apply {
            orientation = if (paysage) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val enregistrer = MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply { setText(R.string.eq_enregistrer_sous) }
        val pleine = if (paysage) LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        else LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bas.addView(supprimer, LinearLayout.LayoutParams(pleine))
        bas.addView(enregistrer, LinearLayout.LayoutParams(pleine))
        gauche.addView(bas)

        interrupteur.setOnCheckedChangeListener { _, oui -> if (!enCours) { Egaliseur.activer(a, oui); rafraichir() } }
        prereglage.setOnClickListener {
            val tous = Egaliseur.PREREGLAGES + Egaliseur.perso(a) + Egaliseur.lesSiens(a)
            MaterialAlertDialogBuilder(a)
                .setTitle(R.string.eq_prereglages)
                .setSingleChoiceItems(tous.map { nomDuPrereglage(a, it.nom) }.toTypedArray(), tous.indexOfFirst { it.nom == Egaliseur.choisi }) { d, i ->
                    if (!Egaliseur.actif) Egaliseur.activer(a, true)
                    Egaliseur.choisir(a, tous[i])
                    rafraichir()
                    d.dismiss()
                }
                .setNegativeButton(R.string.annuler, null)
                .show()
        }
        enregistrer.setOnClickListener {
            val champ = EditText(a).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setHint(R.string.eq_nom)
                if (Egaliseur.lesSiens(a).any { it.nom == Egaliseur.choisi }) { setText(Egaliseur.choisi); setSelection(text.length) }
            }
            val cadre = FrameLayout(a).apply { setPadding(marge, 0, marge, 0); addView(champ) }
            MaterialAlertDialogBuilder(a)
                .setTitle(R.string.eq_enregistrer_sous)
                .setView(cadre)
                .setNegativeButton(R.string.annuler, null)
                .setPositiveButton(R.string.valider) { _, _ ->
                    val nom = champ.text.toString().trim()
                    // Les noms de l'application sont des clefs : un des siens ne peut pas s'appeler comme eux.
                    val pris = Egaliseur.PREREGLAGES.any { it.nom == nom } || nom == Egaliseur.PERSO
                    if (nom.isNotEmpty() && !pris) { Egaliseur.enregistrerSous(a, nom); rafraichir() }
                }
                .show()
            champ.requestFocus()
        }
        supprimer.setOnClickListener {
            val nom = Egaliseur.choisi
            MaterialAlertDialogBuilder(a)
                .setTitle(R.string.eq_supprimer)
                .setMessage(a.getString(R.string.eq_supprimer_texte, nom))
                .setNegativeButton(R.string.annuler, null)
                .setPositiveButton(R.string.supprimer) { _, _ -> Egaliseur.supprimer(a, nom); rafraichir() }
                .show()
        }

        rafraichir()
        val fenetre = MaterialAlertDialogBuilder(a)
            .setTitle(R.string.eq_titre)
            .setView(ScrollView(a).apply { addView(boite) })
            .setPositiveButton(R.string.invitation_fermer, null)
            .show()
        if (paysage) elargir(a, fenetre)
    }

    private fun enPaysage(a: Context) =
        a.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    /** Android garde les boîtes de dialogue étroites : en paysage, presque toute la largeur, pour les deux colonnes. */
    private fun elargir(a: Context, fenetre: android.app.Dialog) {
        fenetre.window?.setLayout((a.resources.displayMetrics.widthPixels * 0.92).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /** La courbe de réponse de l'égaliseur, de 20 Hz à 20 kHz, de −12 à +12 dB. */
    private class VueCourbe(ctx: Context) : View(ctx) {
        private val trait = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val grille = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val chemin = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            if (w == 0f) return
            val d = resources.displayMetrics.density
            grille.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
            grille.strokeWidth = d
            canvas.drawLine(0f, h / 2, w, h / 2, grille)
            canvas.drawRect(0f, 0f, w, h, grille)
            val lnBas = ln(20.0); val lnHaut = ln(20_000.0)
            for (f in Egaliseur.FREQUENCES) {
                val x = ((ln(f.toDouble()) - lnBas) / (lnHaut - lnBas) * w).toFloat()
                canvas.drawLine(x, 0f, x, h, grille)
            }
            trait.color = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)
            trait.alpha = if (Egaliseur.actif) 255 else 100
            trait.strokeWidth = 2.5f * d
            chemin.reset()
            val g = Egaliseur.gains
            val points = 120
            for (i in 0..points) {
                val f = 20.0 * (1_000.0).pow(i / points.toDouble())
                val db = Egaliseur.reponse(g, f).coerceIn(-Egaliseur.MAX_DB - 3.0, Egaliseur.MAX_DB + 3.0)
                val x = (i / points.toDouble() * w).toFloat()
                val y = (h / 2 - db / (Egaliseur.MAX_DB + 3.0) * (h / 2)).toFloat()
                if (i == 0) chemin.moveTo(x, y) else chemin.lineTo(x, y)
            }
            canvas.drawPath(chemin, trait)
        }
    }

    // ---- Le spectre ----

    /** La forme, la palette et le nombre de bandes ; chaque choix vaut tout de suite. */
    fun spectre(a: AppCompatActivity, apres: () -> Unit) {
        Spectre.charger(a)
        val marge = dp(a, 20)
        val paysage = enPaysage(a)
        val boite = LinearLayout(a).apply {
            orientation = if (paysage) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(marge, dp(a, 8), marge, 0)
        }
        // En paysage : la forme à gauche ; l'aperçu, les couleurs et les bandes à droite.
        val gauche: LinearLayout
        val droite: LinearLayout
        if (paysage) {
            gauche = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            droite = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            boite.addView(gauche, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = marge })
            boite.addView(droite, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f))
        } else {
            gauche = boite
            droite = boite
        }
        val apercu = VueSpectre(a)
        droite.addView(apercu, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, if (paysage) 56 else 64)).apply { if (paysage) topMargin = dp(a, 12) })

        fun section(ou: LinearLayout, titre: Int, choix: List<String>, coche: Int, horizontal: Boolean, surChoix: (Int) -> Unit) {
            ou.addView(TextView(a).apply {
                setText(titre)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
                setPadding(0, dp(a, 12), 0, 0)
            })
            val groupe = RadioGroup(a).apply { orientation = if (horizontal) RadioGroup.HORIZONTAL else RadioGroup.VERTICAL }
            choix.forEachIndexed { i, texte ->
                groupe.addView(RadioButton(a).apply { id = View.generateViewId(); text = texte; isChecked = i == coche; setOnClickListener { surChoix(i); apercu.relancer(); apres() } })
            }
            ou.addView(groupe)
        }
        val formes = Spectre.Forme.entries
        section(
            gauche,
            R.string.spectre_forme,
            listOf(R.string.spectre_miroir, R.string.spectre_barres, R.string.spectre_courbe, R.string.spectre_onde, R.string.spectre_aucune).map { a.getString(it) },
            formes.indexOf(Spectre.forme), false,
        ) { Spectre.regler(a, forme = formes[it]) }
        val palettes = Spectre.Palette.entries
        section(
            droite,
            R.string.spectre_palette,
            listOf(R.string.spectre_arc, R.string.spectre_unie, R.string.spectre_vumetre).map { a.getString(it) },
            palettes.indexOf(Spectre.palette), false,
        ) { Spectre.regler(a, palette = palettes[it]) }
        section(droite, R.string.spectre_bandes, Spectre.BANDES.map { it.toString() }, Spectre.BANDES.indexOf(Spectre.bandes), true) {
            Spectre.regler(a, bandes = Spectre.BANDES[it])
        }
        val fenetre = MaterialAlertDialogBuilder(a)
            .setTitle(R.string.spectre_titre)
            .setView(ScrollView(a).apply { addView(boite) })
            .setPositiveButton(R.string.invitation_fermer, null)
            .show()
        if (paysage) elargir(a, fenetre)
    }

    /**
     * Le spectre en grand, sur fond noir, avec la station et le titre en
     * bas, l'écran gardé allumé. Un appui ferme, un appui long règle.
     */
    fun pleinEcran(a: AppCompatActivity, textes: () -> Pair<String, String?>) {
        Spectre.charger(a)
        val fenetre = Dialog(a, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val marge = dp(a, 24)
        val fond = FrameLayout(a).apply { setBackgroundColor(Color.BLACK) }
        val vue = VueSpectre(a)
        fond.addView(vue, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            setMargins(marge, marge * 2, marge, dp(a, 120))
        })
        val nom = TextView(a).apply { setTextColor(Color.WHITE); textSize = 24f; setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END }
        val titre = TextView(a).apply { setTextColor(Color.argb(200, 255, 255, 255)); textSize = 16f; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
        val bas = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(marge, 0, marge, marge)
            addView(nom); addView(titre)
        }
        fond.addView(bas, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        fond.setOnClickListener { fenetre.dismiss() }
        fond.setOnLongClickListener { spectre(a) { vue.relancer() }; true }
        fenetre.setContentView(fond)
        fenetre.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val minuterie = Handler(Looper.getMainLooper())
        val ecrire = object : Runnable {
            override fun run() {
                val (n, t) = textes()
                nom.text = n
                titre.text = t ?: ""
                titre.visibility = if (t.isNullOrBlank()) View.GONE else View.VISIBLE
                minuterie.postDelayed(this, 1_000)
            }
        }
        fenetre.setOnDismissListener { minuterie.removeCallbacks(ecrire) }
        ecrire.run()
        fenetre.show()
    }
}
