package org.unicornradio.app.lecture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import org.unicornradio.app.MainActivity
import org.unicornradio.app.R

/**
 * Ce qui est programmé, annoncé dans les notifications, discrètement (sans
 * son ni vibration) :
 *
 * - un compte à rebours lancé, avec le temps qui reste, qu'Android fait
 *   défiler lui-même (`setChronometerCountDown`), et « Annuler » ;
 * - le prochain réveil en marche, le plus proche, avec son jour et son
 *   heure, et « Ne pas sonner cette fois » : cette sonnerie sautée, le
 *   réveil garde les jours suivants.
 *
 * Un appui ouvre la liste dans l'application. Refaites à chaque changement
 * (`Reveil` appelle `rafraichir`) : rien ne tourne entre-temps.
 */
object Annonces {

    /** Dans l'intention qui ouvre l'application : « reveil » ou « compte », la liste à montrer. */
    const val OUVRIR = "ouvrir_alarmes"

    private const val CANAL = "programmes"
    private const val ETIQUETTE = "programme"
    private const val ID_REVEIL = 1
    private const val ANNULER = "org.unicornradio.app.ANNULER_LE_COMPTE"
    private const val SAUTER = "org.unicornradio.app.SAUTER_LE_REVEIL"

    fun rafraichir(ctx: Context) {
        val gestionnaire = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (gestionnaire.getNotificationChannel(CANAL) == null) {
            gestionnaire.createNotificationChannel(
                NotificationChannel(CANAL, ctx.getString(R.string.annonces_canal), NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                },
            )
        }
        val maintenant = System.currentTimeMillis()
        val alarmes = Reveil.alarmes(ctx).filter { it.actif }
        val voulues = HashSet<Int>()

        // Les comptes à rebours lancés, un par un ; leur numéro de notification est le leur, + 100.
        for (a in alarmes.filter { it.genre == Reveil.Genre.COMPTE && it.fin > maintenant }) {
            val id = 100 + a.id
            voulues += id
            val n = base(ctx, Reveil.Genre.COMPTE)
                .setContentTitle(a.nom ?: ctx.getString(R.string.compte_a_rebours))
                .setContentText(ctx.getString(R.string.annonce_compte, heure(ctx, a.fin), Reveil.sonTexte(a.son)))
                .setWhen(a.fin)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, ctx.getString(R.string.annonce_annuler), action(ctx, ANNULER, a.id))
                .build()
            gestionnaire.notify(ETIQUETTE, id, n)
        }

        // Le prochain réveil en marche, le plus proche.
        val prochain = alarmes.filter { it.genre == Reveil.Genre.REVEIL }
            .map { it to Reveil.quandApresLeSaut(it) }
            .filter { it.second > maintenant }
            .minByOrNull { it.second }
        if (prochain != null) {
            val (a, quand) = prochain
            voulues += ID_REVEIL
            val jour = DateUtils.formatDateTime(ctx, quand, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_TIME)
            val n = base(ctx, Reveil.Genre.REVEIL)
                .setContentTitle(ctx.getString(R.string.annonce_reveil, a.nom ?: ctx.getString(R.string.sans_nom)))
                .setContentText(ctx.getString(R.string.annonce_reveil_detail, jour, Reveil.sonTexte(a.son)))
                .setShowWhen(false)
                .addAction(0, ctx.getString(R.string.annonce_sauter), action(ctx, SAUTER, a.id))
                .build()
            gestionnaire.notify(ETIQUETTE, ID_REVEIL, n)
        }

        // Ce qui n'est plus programmé s'en va.
        for (s in gestionnaire.activeNotifications) {
            if (s.tag == ETIQUETTE && s.id !in voulues) gestionnaire.cancel(ETIQUETTE, s.id)
        }
    }

    private fun base(ctx: Context, genre: Reveil.Genre): NotificationCompat.Builder {
        val ouvrir = PendingIntent.getActivity(
            ctx, if (genre == Reveil.Genre.COMPTE) 4 else 5,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(OUVRIR, genre.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_licorne)
            .setColor(ctx.getColor(R.color.licorne))
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.logo))
            .setContentIntent(ouvrir)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun action(ctx: Context, quoi: String, id: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, id * 10 + (if (quoi == ANNULER) 1 else 2),
        Intent(ctx, Recepteur::class.java).setAction(quoi).putExtra(Reveil.ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun heure(ctx: Context, quand: Long): String =
        android.text.format.DateFormat.getTimeFormat(ctx).format(java.util.Date(quand))

    /** « Annuler » un compte à rebours, « Ne pas sonner cette fois » un réveil. */
    class Recepteur : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intention: Intent) {
            val id = intention.getIntExtra(Reveil.ID, -1)
            when (intention.action) {
                ANNULER -> Reveil.basculer(ctx, id, false)
                SAUTER -> Reveil.sauter(ctx, id)
            }
            rafraichir(ctx)
        }
    }
}
