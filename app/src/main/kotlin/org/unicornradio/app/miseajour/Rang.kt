package org.unicornradio.app.miseajour

import android.content.Context

/**
 * « Vous êtes le 37ᵉ utilisateur » : pour le plaisir, le total des
 * téléchargements que GitHub comptait au premier lancement, gardé une fois
 * pour toutes. Une mise à jour ne le touche pas (les préférences restent),
 * et il n'est jamais relu. Le chiffre est faux par nature : GitHub compte
 * aussi les mises à jour et les essais.
 */
object Rang {

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("rang", Context.MODE_PRIVATE)

    /** Le rang gardé, ou `null` s'il n'a pas encore pu être lu. */
    fun lu(ctx: Context): Int? = prefs(ctx).getInt("rang", 0).takeIf { it > 0 }

    /** Le garder, seulement s'il n'y en a pas déjà un. Vrai s'il vient d'être noté. */
    @Synchronized
    fun noter(ctx: Context, rang: Int): Boolean {
        if (rang <= 0 || lu(ctx) != null) return false
        prefs(ctx).edit().putInt("rang", rang).putLong("le", System.currentTimeMillis()).apply()
        return true
    }

    /** Le remplacer : seulement depuis une version d'essai, pour l'auteur qui veut être le premier. */
    @Synchronized
    fun changer(ctx: Context, rang: Int) {
        if (rang > 0) prefs(ctx).edit().putInt("rang", rang).apply()
    }

    /** « 1er », « 37e » ; « 1st », « 2nd », « 3rd », « 37th ». */
    fun ordinal(ctx: Context, n: Int): String {
        val langue = ctx.resources.configuration.locales[0].language
        if (langue == "fr") return if (n == 1) "1er" else "${n}e"
        val suffixe = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$suffixe"
    }
}
