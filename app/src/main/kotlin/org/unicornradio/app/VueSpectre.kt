package org.unicornradio.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import org.unicornradio.app.lecture.Spectre
import kotlin.math.max

/**
 * Le spectre à l'écran, dans la forme et la palette choisies. Il ne
 * s'anime que visible ; sans son depuis un moment (pause, coupure), il
 * retombe puis s'arrête de dessiner.
 */
class VueSpectre @JvmOverloads constructor(contexte: Context, attrs: AttributeSet? = null) : View(contexte, attrs) {

    private val echantillons = FloatArray(Spectre.N)
    private val re = FloatArray(Spectre.N)
    private val im = FloatArray(Spectre.N)
    private var brut = FloatArray(60)
    private var niveaux = FloatArray(60)
    private var cretes = FloatArray(60)
    private var cretesAge = IntArray(60)
    private var regarde = false

    private val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trait = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val chemin = Path()

    override fun onVisibilityAggregated(visible: Boolean) {
        super.onVisibilityAggregated(visible)
        if (visible == regarde) return
        regarde = visible
        if (visible) { Spectre.regarder(); postInvalidateOnAnimation() } else Spectre.neplusRegarder()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(reveil)
        if (regarde) { regarde = false; Spectre.neplusRegarder() }
        super.onDetachedFromWindow()
    }

    private fun couleurUnie(): Int = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)

    /** La couleur d'une bande pour l'arc-en-ciel : du violet au rouge, de la gauche à la droite. */
    private fun arc(i: Int, n: Int): Int {
        val teinte = 265f - i * 250f / max(1, n - 1)
        return hsl((teinte + 360f) % 360f, 0.85f, 0.55f)
    }

    private fun hsl(h: Float, s: Float, l: Float): Int {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
        val m = l - c / 2
        val (r, g, b) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        return Color.rgb(((r + m) * 255).toInt(), ((g + m) * 255).toInt(), ((b + m) * 255).toInt())
    }

    /** Le vu-mètre : vert en bas, jaune, rouge en haut ; `miroir`, du centre vers les bords. */
    private fun vumetre(miroir: Boolean): Shader {
        val couleurs = intArrayOf(Color.rgb(46, 204, 64), Color.rgb(255, 220, 0), Color.rgb(255, 65, 54))
        return if (miroir) {
            LinearGradient(0f, height / 2f, 0f, 0f, couleurs, floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.MIRROR)
        } else {
            LinearGradient(0f, height.toFloat(), 0f, 0f, couleurs, floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        }
    }

    /** L'arc-en-ciel en dégradé horizontal, pour la courbe et l'onde. */
    private fun arcHorizontal(): Shader {
        val couleurs = IntArray(8) { arc(it, 8) }
        return LinearGradient(0f, 0f, width.toFloat(), 0f, couleurs, null, Shader.TileMode.CLAMP)
    }

    private fun preparerLesTableaux(n: Int) {
        if (brut.size == n) return
        brut = FloatArray(n); niveaux = FloatArray(n); cretes = FloatArray(n); cretesAge = IntArray(n)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val forme = Spectre.forme
        if (forme == Spectre.Forme.AUCUNE) return
        val n = Spectre.bandes
        preparerLesTableaux(n)
        val vivant = System.currentTimeMillis() - Spectre.dernierSon < 400
        val taux = Spectre.lire(echantillons)
        var encore = vivant
        if (forme == Spectre.Forme.ONDE) {
            dessinerLOnde(canvas, vivant)
        } else {
            if (vivant) Spectre.analyser(echantillons, taux, n, re, im, brut) else brut.fill(0f)
            for (i in 0 until n) {
                // Monte d'un coup, retombe en douceur ; les crêtes tiennent un instant puis tombent.
                niveaux[i] = if (brut[i] > niveaux[i]) brut[i] else niveaux[i] * 0.85f + brut[i] * 0.15f
                if (niveaux[i] >= cretes[i]) { cretes[i] = niveaux[i]; cretesAge[i] = 0 }
                else if (++cretesAge[i] > 20) cretes[i] = max(0f, cretes[i] - 0.015f)
                if (niveaux[i] > 0.01f || cretes[i] > 0.01f) encore = true
            }
            when (forme) {
                Spectre.Forme.MIROIR -> dessinerLesBarres(canvas, n, miroir = true)
                Spectre.Forme.BARRES -> dessinerLesBarres(canvas, n, miroir = false)
                Spectre.Forme.COURBE -> dessinerLaCourbe(canvas, n)
                else -> Unit
            }
        }
        // Au repos, un coup d'œil de temps en temps, pour repartir quand le son revient.
        if (regarde) { if (encore) postInvalidateOnAnimation() else postDelayed(reveil, 300) }
    }

    private val reveil = Runnable { invalidate() }

    private fun dessinerLesBarres(canvas: Canvas, n: Int, miroir: Boolean) {
        val pas = width.toFloat() / n
        val ecart = max(1f, pas * 0.2f)
        val palette = Spectre.palette
        pinceau.shader = if (palette == Spectre.Palette.VUMETRE) vumetre(miroir) else null
        val unie = couleurUnie()
        val milieu = height / 2f
        for (i in 0 until n) {
            if (palette != Spectre.Palette.VUMETRE) pinceau.color = if (palette == Spectre.Palette.ARC) arc(i, n) else unie
            val x = i * pas + ecart / 2
            val h = niveaux[i]
            if (miroir) {
                val demi = h * milieu
                canvas.drawRect(x, milieu - demi, x + pas - ecart, milieu + demi, pinceau)
            } else {
                canvas.drawRect(x, height * (1 - h), x + pas - ecart, height.toFloat(), pinceau)
                // La crête : un trait fin au plus haut récent.
                val y = height * (1 - cretes[i])
                canvas.drawRect(x, y - 2f * resources.displayMetrics.density, x + pas - ecart, y, pinceau)
            }
        }
        pinceau.shader = null
    }

    private fun dessinerLaCourbe(canvas: Canvas, n: Int) {
        chemin.reset()
        val pas = width.toFloat() / max(1, n - 1)
        chemin.moveTo(0f, height.toFloat())
        var px = 0f; var py = height * (1 - niveaux[0])
        chemin.lineTo(px, py)
        for (i in 1 until n) {
            val x = i * pas; val y = height * (1 - niveaux[i])
            chemin.quadTo(px, py, (px + x) / 2, (py + y) / 2)
            px = x; py = y
        }
        chemin.lineTo(width.toFloat(), py)
        chemin.lineTo(width.toFloat(), height.toFloat())
        chemin.close()
        appliquerLaPalette(pinceau)
        pinceau.alpha = 170
        canvas.drawPath(chemin, pinceau)
        pinceau.alpha = 255
        pinceau.shader = null
    }

    private fun dessinerLOnde(canvas: Canvas, vivant: Boolean) {
        val points = 512
        val debut = echantillons.size - points
        chemin.reset()
        val milieu = height / 2f
        for (i in 0 until points) {
            val v = if (vivant) echantillons[debut + i].coerceIn(-1f, 1f) else 0f
            val x = i * width.toFloat() / (points - 1)
            val y = milieu - v * milieu * 0.95f
            if (i == 0) chemin.moveTo(x, y) else chemin.lineTo(x, y)
        }
        trait.strokeWidth = max(1.5f, 2f * resources.displayMetrics.density)
        appliquerLaPalette(trait)
        canvas.drawPath(chemin, trait)
        trait.shader = null
    }

    private fun appliquerLaPalette(p: Paint) {
        when (Spectre.palette) {
            Spectre.Palette.ARC -> p.shader = arcHorizontal()
            Spectre.Palette.VUMETRE -> p.shader = vumetre(Spectre.forme == Spectre.Forme.ONDE)
            Spectre.Palette.UNIE -> { p.shader = null; p.color = couleurUnie() }
        }
    }

    /** Un autre réglage : redessiner tout de suite. */
    fun relancer() {
        removeCallbacks(reveil)
        postInvalidateOnAnimation()
    }
}
