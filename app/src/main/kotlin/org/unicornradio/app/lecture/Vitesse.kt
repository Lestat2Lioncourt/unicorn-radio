package org.unicornradio.app.lecture

import android.content.Context
import java.text.NumberFormat

/**
 * La vitesse de lecture des épisodes : une seule pour tous, gardée d'un
 * lancement à l'autre. Les stations restent à 1× ; le service l'applique à
 * chaque changement d'élément, l'écran à chaque choix. Le lecteur garde la
 * hauteur de la voix.
 */
object Vitesse {

    val CHOIX = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("lecture", Context.MODE_PRIVATE)

    fun lue(ctx: Context): Float = prefs(ctx).getFloat("vitesse", 1f).takeIf { it in CHOIX } ?: 1f

    fun garder(ctx: Context, vitesse: Float) {
        prefs(ctx).edit().putFloat("vitesse", vitesse).apply()
    }

    /** « 1,25× » en français, « 1.25× » en anglais : la langue de l'application. */
    fun texte(ctx: Context, vitesse: Float): String =
        NumberFormat.getNumberInstance(ctx.resources.configuration.locales[0]).apply { minimumFractionDigits = 0; maximumFractionDigits = 2 }.format(vitesse) + "×"
}
