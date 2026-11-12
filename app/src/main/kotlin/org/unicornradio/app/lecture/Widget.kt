package org.unicornradio.app.lecture

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.media3.session.MediaButtonReceiver
import org.unicornradio.app.MainActivity
import org.unicornradio.app.R

/**
 * Le widget de l'écran d'accueil : ce qui joue, ou ce qu'on écoutait en
 * dernier, le titre en cours, et précédent, lecture ou pause, suivant,
 * arrêter (la lecture s'arrête, la notification s'en va).
 *
 * Les boutons envoient des touches multimédia au `MediaButtonReceiver` de
 * Media3, comme un casque : il réveille le service si besoin, et « lecture »
 * sans rien dans le lecteur reprend la dernière écoute
 * (`ServiceLecture.reprendreLaDerniere`). Un appui sur les textes ouvre
 * l'application. Le service redessine le widget à chaque changement.
 */
class Widget : AppWidgetProvider() {

    override fun onUpdate(contexte: Context, gestionnaire: AppWidgetManager, ids: IntArray) {
        gestionnaire.updateAppWidget(ids, vues(contexte, DerniereEcoute.nom(contexte), null, enLecture = enLecture))
    }

    companion object {
        /** L'état de lecture dernier connu, pour un widget ajouté pendant la lecture. */
        @Volatile private var enLecture = false

        /** Le numéro de l'intention qui ouvre l'application, distinct de celles des touches (leurs codes) et du service. */
        private const val OUVRIR = 7001

        fun mettreAJour(contexte: Context, nom: String?, titre: String?, enLecture: Boolean) {
            this.enLecture = enLecture
            val gestionnaire = AppWidgetManager.getInstance(contexte)
            val ids = gestionnaire.getAppWidgetIds(ComponentName(contexte, Widget::class.java))
            if (ids.isEmpty()) return
            gestionnaire.updateAppWidget(ids, vues(contexte, nom, titre, enLecture))
        }

        private fun vues(contexte: Context, nom: String?, titre: String?, enLecture: Boolean): RemoteViews =
            RemoteViews(contexte.packageName, R.layout.widget).apply {
                setTextViewText(R.id.widgetNom, nom ?: contexte.getString(R.string.widget_rien))
                setTextViewText(R.id.widgetTitre, titre ?: "")
                setViewVisibility(R.id.widgetTitre, if (titre.isNullOrBlank()) View.GONE else View.VISIBLE)
                setImageViewResource(R.id.widgetLecture, if (enLecture) R.drawable.ic_pause else R.drawable.ic_lecture)
                setContentDescription(R.id.widgetLecture, contexte.getString(if (enLecture) R.string.pause else R.string.lire))
                setOnClickPendingIntent(R.id.widgetPrecedent, touche(contexte, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                setOnClickPendingIntent(R.id.widgetLecture, touche(contexte, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                setOnClickPendingIntent(R.id.widgetSuivant, touche(contexte, KeyEvent.KEYCODE_MEDIA_NEXT))
                setOnClickPendingIntent(R.id.widgetArret, touche(contexte, KeyEvent.KEYCODE_MEDIA_STOP))
                // Ouvrir l'application : partout hors des boutons, le fond, la zone des textes et chaque texte ;
                // certains écrans d'accueil ne transmettent l'appui qu'à la vue touchée.
                val ouvrir = PendingIntent.getActivity(
                    contexte, OUVRIR,
                    Intent(contexte, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                for (vue in listOf(R.id.widgetFond, R.id.widgetTextes, R.id.widgetNom, R.id.widgetTitre)) setOnClickPendingIntent(vue, ouvrir)
            }

        /** Une touche multimédia, comme celle d'un casque, pour le récepteur de Media3. */
        private fun touche(contexte: Context, code: Int): PendingIntent {
            val intention = Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(ComponentName(contexte, MediaButtonReceiver::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, code))
            return PendingIntent.getBroadcast(contexte, code, intention, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
