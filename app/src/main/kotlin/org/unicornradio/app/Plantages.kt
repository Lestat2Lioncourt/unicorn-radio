package org.unicornradio.app

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * L'application : elle installe, avant tout le reste, le gardien des
 * plantages, pour l'écran comme pour le service de lecture.
 */
class UnicornRadio : Application() {
    override fun onCreate() {
        super.onCreate()
        Plantages.installer(this)
        // Les notifications de ce qui est programmé, à jour dès le lancement (après une mise à jour aussi).
        try { org.unicornradio.app.lecture.Annonces.rafraichir(this) } catch (e: Exception) { }
    }
}

/**
 * Un plantage n'est pas toujours annoncé par Android. Celui-ci est écrit
 * dans un fichier, la date, la version, le téléphone, la pile, puis Android
 * fait comme d'habitude. À l'ouverture suivante, l'écran le montre une fois,
 * à copier pour le signaler (`MainActivity.montrerLePlantage`).
 */
object Plantages {

    private fun fichier(ctx: Context) = File(ctx.filesDir, "plantage.txt")

    fun installer(ctx: Context) {
        val app = ctx.applicationContext
        val avant = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { fil, erreur ->
            try {
                val pile = StringWriter().also { erreur.printStackTrace(PrintWriter(it)) }.toString()
                val quand = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
                fichier(app).writeText(
                    "Unicorn-Radio ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                        "$quand, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "fil « ${fil.name} »\n\n$pile",
                )
            } catch (e: Throwable) {
                // Rien à faire : au moins, ne pas cacher le plantage d'origine.
            }
            avant?.uncaughtException(fil, erreur)
        }
    }

    /** Le dernier plantage pas encore montré, effacé en le lisant ; `null` s'il n'y en a pas. */
    fun prendre(ctx: Context): String? {
        val f = fichier(ctx)
        if (!f.exists()) return null
        return try { f.readText() } catch (e: Exception) { null }.also { f.delete() }
    }
}
