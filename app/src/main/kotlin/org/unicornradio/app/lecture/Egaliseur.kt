package org.unicornradio.app.lecture

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * L'égaliseur : dix bandes, de 31 Hz à 16 kHz, de −12 à +12 dB, des
 * préréglages et les siens sous un nom. Le son est traité dans le lecteur
 * de l'application (`Processeur`, posé dans la chaîne audio de Media3 par
 * le service), pas par l'égaliseur d'Android, dont le nombre de bandes
 * change d'un téléphone à l'autre.
 *
 * Le service et l'écran vivent dans le même processus : ce qu'on règle
 * ici est lu par le fil audio à la mémoire tampon suivante.
 */
object Egaliseur {

    /** Les fréquences centrales, en Hz. */
    val FREQUENCES = intArrayOf(31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)
    const val MAX_DB = 12

    /** Un préréglage : le nom (une clef pour ceux de l'application, le nom tapé pour les siens) et les gains. */
    class Prereglage(val nom: String, val gains: IntArray, val aSoi: Boolean)

    /** Ceux de l'application ; « perso » est le dernier réglage à la main. */
    val PREREGLAGES = listOf(
        Prereglage("plat", intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), false),
        Prereglage("rock", intArrayOf(5, 4, 3, 1, -1, -1, 1, 3, 4, 5), false),
        Prereglage("pop", intArrayOf(-1, 1, 3, 4, 3, 1, -1, -1, 1, 2), false),
        Prereglage("jazz", intArrayOf(3, 2, 1, 2, -1, -1, 0, 1, 2, 3), false),
        Prereglage("classique", intArrayOf(4, 3, 2, 1, -1, -1, 0, 2, 3, 4), false),
        Prereglage("basses", intArrayOf(7, 6, 5, 3, 1, 0, 0, 0, 0, 0), false),
        Prereglage("voix", intArrayOf(-3, -2, -1, 1, 3, 4, 4, 3, 1, -1), false),
    )
    const val PERSO = "perso"

    @Volatile var actif = false
        private set
    @Volatile var gains = IntArray(FREQUENCES.size)
        private set
    /** Le préréglage choisi : une clef de `PREREGLAGES`, `PERSO`, ou le nom d'un des siens. */
    @Volatile var choisi = "plat"
        private set
    /** Monte à chaque changement : le fil audio recalcule ses filtres. */
    @Volatile var version = 0
        private set

    private var charge = false

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("egaliseur", Context.MODE_PRIVATE)

    @Synchronized
    fun charger(ctx: Context) {
        if (charge) return
        val p = prefs(ctx)
        actif = p.getBoolean("actif", false)
        gains = lireGains(p.getString("gains", null)) ?: IntArray(FREQUENCES.size)
        choisi = p.getString("choisi", "plat") ?: "plat"
        charge = true
        version++
    }

    private fun lireGains(texte: String?): IntArray? {
        val morceaux = texte?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: return null
        if (morceaux.size != FREQUENCES.size) return null
        return morceaux.map { it.coerceIn(-MAX_DB, MAX_DB) }.toIntArray()
    }

    private fun enregistrer(ctx: Context) {
        prefs(ctx).edit()
            .putBoolean("actif", actif)
            .putString("gains", gains.joinToString(","))
            .putString("choisi", choisi)
            .apply()
        version++
    }

    fun activer(ctx: Context, oui: Boolean) {
        actif = oui
        enregistrer(ctx)
    }

    /** Une bande bougée à la main : le réglage devient « perso », gardé comme tel. */
    fun regler(ctx: Context, bande: Int, db: Int) {
        val g = gains.copyOf()
        g[bande] = db.coerceIn(-MAX_DB, MAX_DB)
        gains = g
        choisi = PERSO
        prefs(ctx).edit().putString("perso", g.joinToString(",")).apply()
        enregistrer(ctx)
    }

    fun choisir(ctx: Context, p: Prereglage) {
        gains = p.gains.copyOf()
        choisi = p.nom
        enregistrer(ctx)
    }

    /** « Perso » : le dernier réglage fait à la main, plat s'il n'y en a pas. */
    fun perso(ctx: Context): Prereglage =
        Prereglage(PERSO, lireGains(prefs(ctx).getString("perso", null)) ?: IntArray(FREQUENCES.size), false)

    /** Les siens, dans l'ordre où ils ont été enregistrés. */
    fun lesSiens(ctx: Context): List<Prereglage> {
        val a = try { JSONArray(prefs(ctx).getString("siens", "[]")) } catch (e: Exception) { JSONArray() }
        return (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val g = lireGains(o.optString("gains")) ?: return@mapNotNull null
            Prereglage(o.optString("nom").ifBlank { return@mapNotNull null }, g, true)
        }
    }

    /** Enregistrer le réglage actuel sous un nom ; le même nom remplace. */
    fun enregistrerSous(ctx: Context, nom: String) {
        val siens = lesSiens(ctx).filter { it.nom != nom } + Prereglage(nom, gains.copyOf(), true)
        ecrireLesSiens(ctx, siens)
        choisi = nom
        enregistrer(ctx)
    }

    fun supprimer(ctx: Context, nom: String) {
        ecrireLesSiens(ctx, lesSiens(ctx).filter { it.nom != nom })
        if (choisi == nom) { choisi = PERSO; prefs(ctx).edit().putString("perso", gains.joinToString(",")).apply(); enregistrer(ctx) }
    }

    private fun ecrireLesSiens(ctx: Context, siens: List<Prereglage>) {
        val a = JSONArray()
        siens.forEach { a.put(JSONObject().put("nom", it.nom).put("gains", it.gains.joinToString(","))) }
        prefs(ctx).edit().putString("siens", a.toString()).apply()
    }

    // ---- Le calcul ----

    /** La largeur des bandes : une octave chacune, qui se recouvrent juste assez. */
    private const val Q = 1.41

    /**
     * Les coefficients d'un filtre en cloche (« peaking »), normalisés :
     * b0, b1, b2, a1, a2. Formules classiques du « cookbook » de R. Bristow-Johnson.
     */
    fun coefficients(frequence: Double, db: Double, tauxHz: Int): DoubleArray {
        val a = 10.0.pow(db / 40.0)
        val w0 = 2 * PI * frequence / tauxHz
        val alpha = sin(w0) / (2 * Q)
        val cw = cos(w0)
        val a0 = 1 + alpha / a
        return doubleArrayOf(
            (1 + alpha * a) / a0,
            (-2 * cw) / a0,
            (1 - alpha * a) / a0,
            (-2 * cw) / a0,
            (1 - alpha / a) / a0,
        )
    }

    /** La réponse de l'ensemble, en dB, à une fréquence : pour dessiner la courbe. */
    fun reponse(gains: IntArray, frequence: Double, tauxHz: Int = 48_000): Double {
        val w = 2 * PI * frequence / tauxHz
        var total = 0.0
        for (i in FREQUENCES.indices) {
            if (gains[i] == 0 || FREQUENCES[i] >= tauxHz / 2) continue
            val c = coefficients(FREQUENCES[i].toDouble(), gains[i].toDouble(), tauxHz)
            // |H(e^jw)|² d'un biquad.
            val cr = cos(w); val sr = sin(w); val c2 = cos(2 * w); val s2 = sin(2 * w)
            val nr = c[0] + c[1] * cr + c[2] * c2; val ni = -(c[1] * sr + c[2] * s2)
            val dr = 1 + c[3] * cr + c[4] * c2; val di = -(c[3] * sr + c[4] * s2)
            total += 10 * kotlin.math.log10((nr * nr + ni * ni) / (dr * dr + di * di))
        }
        return total
    }

    /**
     * Dans la chaîne audio du lecteur : du PCM 16 bits, filtré bande par
     * bande, canal par canal. Coupé, il recopie tel quel. Un gain d'entrée
     * baisse le tout d'autant que la bande la plus poussée, pour ne pas
     * saturer ; ce qui dépasserait encore est écrêté.
     */
    @UnstableApi
    class Processeur : BaseAudioProcessor() {
        private var vue = -1
        private var filtres: Array<DoubleArray> = emptyArray()
        private var preampli = 1.0
        private var enService = false
        /** L'état des filtres : par canal, par bande, x1 x2 y1 y2. */
        private var etat = Array(0) { DoubleArray(0) }

        override fun onConfigure(entree: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            // Autre chose que du 16 bits (rare : la sortie en flottants est coupée) : on se met de côté.
            if (entree.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
            etat = Array(entree.channelCount * FREQUENCES.size) { DoubleArray(4) }
            vue = -1
            return entree
        }

        private fun preparer() {
            if (vue == version) return
            vue = version
            val taux = inputAudioFormat.sampleRate
            val g = gains
            enService = actif && g.any { it != 0 }
            filtres = Array(FREQUENCES.size) { i ->
                if (g[i] == 0 || FREQUENCES[i] >= taux / 2) doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0)
                else coefficients(FREQUENCES[i].toDouble(), g[i].toDouble(), taux)
            }
            preampli = 10.0.pow(-max(0, g.maxOrNull() ?: 0) / 20.0)
        }

        override fun queueInput(entree: ByteBuffer) {
            val reste = entree.remaining()
            if (reste == 0) return
            preparer()
            val sortie = replaceOutputBuffer(reste)
            if (!enService) {
                sortie.put(entree)
                sortie.flip()
                return
            }
            val canaux = inputAudioFormat.channelCount
            val source = entree.order(ByteOrder.nativeOrder())
            var canal = 0
            val nb = FREQUENCES.size
            while (source.remaining() >= 2) {
                var x = source.short.toDouble() * preampli
                val base = canal * nb
                for (b in 0 until nb) {
                    val c = filtres[b]
                    if (c[1] == 0.0 && c[0] == 1.0) continue
                    val s = etat[base + b]
                    val y = c[0] * x + c[1] * s[0] + c[2] * s[1] - c[3] * s[2] - c[4] * s[3]
                    s[1] = s[0]; s[0] = x; s[3] = s[2]; s[2] = y
                    x = y
                }
                val v = x.coerceIn(-32768.0, 32767.0).toInt().toShort()
                sortie.putShort(v)
                canal = if (canal + 1 == canaux) 0 else canal + 1
            }
            sortie.flip()
        }

        override fun onFlush(metadonnees: AudioProcessor.StreamMetadata) {
            for (s in etat) s.fill(0.0)
        }

        @Deprecated("Media3 appelle l'une ou l'autre selon sa version")
        override fun onFlush() {
            for (s in etat) s.fill(0.0)
        }

        override fun onReset() {
            etat = Array(0) { DoubleArray(0) }
            vue = -1
        }
    }

    /** « +3 », « 0 », « −2 » : la valeur d'une bande. */
    fun libelle(db: Int): String = if (db > 0) "+$db" else "$db"
}
