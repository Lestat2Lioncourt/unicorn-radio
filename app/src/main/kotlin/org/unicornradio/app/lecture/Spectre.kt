package org.unicornradio.app.lecture

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Le spectre : le son pris dans le lecteur de l'application, après
 * l'égaliseur, par une dérivation de la chaîne audio de Media3. Pas de
 * permission du micro, que demanderait le `Visualizer` d'Android.
 *
 * Rien n'est gardé ni calculé tant qu'aucun spectre n'est à l'écran
 * (`ecoutes`) : écran éteint, application cachée, la dérivation ne fait
 * que passer.
 */
object Spectre {

    enum class Forme { MIROIR, BARRES, COURBE, ONDE, AUCUNE }
    enum class Palette { ARC, UNIE, VUMETRE }
    val BANDES = intArrayOf(10, 20, 30, 60)

    @Volatile var forme = Forme.MIROIR
        private set
    @Volatile var palette = Palette.ARC
        private set
    @Volatile var bandes = 30
        private set
    /** La bande sous la ligne de lecture, masquée par l'appui long sur le logo ; les réglages restent. */
    @Volatile var masque = false
        private set
    private var charge = false

    /** La bande est-elle à l'écran : pas masquée, et une forme choisie. */
    val visible: Boolean
        get() = !masque && forme != Forme.AUCUNE

    /** Afficher ou masquer la bande ; l'afficher depuis « aucune » remet le miroir. */
    fun montrer(ctx: Context, oui: Boolean) {
        masque = !oui
        if (oui && forme == Forme.AUCUNE) forme = Forme.MIROIR
        prefs(ctx).edit().putBoolean("masque", masque).putString("forme", forme.name).apply()
    }

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("spectre", Context.MODE_PRIVATE)

    @Synchronized
    fun charger(ctx: Context) {
        if (charge) return
        val p = prefs(ctx)
        forme = Forme.entries.firstOrNull { it.name == p.getString("forme", null) } ?: Forme.MIROIR
        palette = Palette.entries.firstOrNull { it.name == p.getString("palette", null) } ?: Palette.ARC
        bandes = p.getInt("bandes", 30).takeIf { it in BANDES } ?: 30
        masque = p.getBoolean("masque", false)
        charge = true
    }

    fun regler(ctx: Context, forme: Forme = this.forme, palette: Palette = this.palette, bandes: Int = this.bandes) {
        this.forme = forme
        this.palette = palette
        this.bandes = bandes
        prefs(ctx).edit().putString("forme", forme.name).putString("palette", palette.name).putInt("bandes", bandes).apply()
    }

    // ---- Les échantillons ----

    /** Combien de vues de spectre sont à l'écran : à zéro, rien n'est copié. */
    @Volatile var ecoutes = 0
        private set

    @Synchronized fun regarder() { ecoutes++ }
    @Synchronized fun neplusRegarder() { ecoutes = max(0, ecoutes - 1) }

    /** Une seconde et plus de son, en mono, de −1 à 1. */
    private const val TAILLE = 1 shl 16
    private val anneau = FloatArray(TAILLE)
    private var ecrit = 0L
    @Volatile private var taux = 44_100
    /** Quand le dernier son est arrivé : sans rien depuis un moment, le spectre retombe. */
    @Volatile var dernierSon = 0L
        private set

    /**
     * Le son arrive au lecteur avant d'être entendu, d'un quart de seconde
     * environ (la mémoire de la sortie audio) : on lit d'autant en arrière.
     */
    private const val RETARD_S = 0.25

    /** La dérivation, dans la chaîne audio du lecteur. */
    @UnstableApi
    val capteur = object : TeeAudioProcessor.AudioBufferSink {
        @Volatile private var canaux = 2
        @Volatile private var seize = true

        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
            taux = sampleRateHz
            canaux = max(1, channelCount)
            seize = encoding == C.ENCODING_PCM_16BIT
        }

        override fun handleBuffer(buffer: ByteBuffer) {
            if (ecoutes == 0 || !seize) return
            val b = buffer.duplicate().order(ByteOrder.nativeOrder())
            val n = canaux
            synchronized(anneau) {
                while (b.remaining() >= 2 * n) {
                    var somme = 0
                    for (c in 0 until n) somme += b.short
                    anneau[(ecrit and (TAILLE - 1).toLong()).toInt()] = somme / (32768f * n)
                    ecrit++
                }
            }
            dernierSon = System.currentTimeMillis()
        }
    }

    /** Les `dest.size` derniers échantillons entendus, à peu près ; rend le taux. */
    fun lire(dest: FloatArray): Int {
        val t = taux
        synchronized(anneau) {
            val fin = ecrit - (RETARD_S * t).toLong()
            for (i in dest.indices) {
                val k = fin - dest.size + i
                dest[i] = if (k < 0 || ecrit - k > TAILLE) 0f else anneau[(k and (TAILLE - 1).toLong()).toInt()]
            }
        }
        return t
    }

    // ---- L'analyse ----

    const val N = 2048
    private val fenetre = FloatArray(N) { (0.5 - 0.5 * cos(2 * PI * it / (N - 1))).toFloat() }

    /**
     * Les bandes, de 0 à 1, en échelle logarithmique de 31 Hz à 16 kHz,
     * depuis `echantillons` (N valeurs) au taux donné. `re` et `im` sont des
     * tableaux de travail de N valeurs.
     */
    fun analyser(echantillons: FloatArray, tauxHz: Int, nb: Int, re: FloatArray, im: FloatArray, sortie: FloatArray) {
        for (i in 0 until N) { re[i] = echantillons[i] * fenetre[i]; im[i] = 0f }
        fft(re, im)
        val bas = 31.0; val haut = minOf(16_000.0, tauxHz / 2.0)
        val pas = ln(haut / bas) / nb
        for (b in 0 until nb) {
            val f1 = bas * kotlin.math.exp(pas * b)
            val f2 = bas * kotlin.math.exp(pas * (b + 1))
            var i1 = (f1 * N / tauxHz).toInt().coerceIn(1, N / 2 - 1)
            val i2 = (f2 * N / tauxHz).toInt().coerceIn(i1, N / 2 - 1)
            var m = 0f
            while (i1 <= i2) {
                val a = sqrt(re[i1] * re[i1] + im[i1] * im[i1])
                if (a > m) m = a
                i1++
            }
            // En dB, de −70 à 0 ramené à 0..1 ; la fenêtre de Hann divise par deux.
            val db = 20 * log10((m * 4f / N).coerceAtLeast(1e-6f))
            sortie[b] = ((db + 70f) / 70f).coerceIn(0f, 1f)
        }
    }

    /** FFT radix 2, sur place. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var longueur = 2
        while (longueur <= n) {
            val angle = -2 * PI / longueur
            val wr = cos(angle).toFloat(); val wi = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var cr = 1f; var ci = 0f
                for (k in 0 until longueur / 2) {
                    val a = i + k; val b = a + longueur / 2
                    val tr = re[b] * cr - im[b] * ci
                    val ti = re[b] * ci + im[b] * cr
                    re[b] = re[a] - tr; im[b] = im[a] - ti
                    re[a] += tr; im[a] += ti
                    val ncr = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = ncr
                }
                i += longueur
            }
            longueur = longueur shl 1
        }
    }
}
