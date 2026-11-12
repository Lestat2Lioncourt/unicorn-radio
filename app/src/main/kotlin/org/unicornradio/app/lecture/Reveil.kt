package org.unicornradio.app.lecture

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import org.unicornradio.app.MainActivity
import org.unicornradio.app.fichier.Fichiers
import org.unicornradio.app.modele.Station
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/**
 * Les réveils et les comptes à rebours : autant qu'on veut, chacun son nom,
 * son heure et ses jours ou sa durée, son son (une station, ou une sonnerie
 * du téléphone), son volume progressif, son interrupteur. Sans nom, il sert
 * une fois et disparaît.
 *
 * Chacun est programmé comme un réveil (`setAlarmClock`), sous son propre
 * numéro : Android l'annonce dans la barre d'état, le déclenche à l'heure
 * même en veille, et permet au service de lecture de démarrer depuis
 * l'arrière-plan. Avec une station, le départ est une minute avant l'heure
 * dite, en muet, le temps que la publicité du début passe ; avec une
 * sonnerie, à l'heure. `Recepteur` lance le service (`ServiceLecture.sonner`)
 * et programme la fois suivante ; au démarrage du téléphone, après une mise
 * à jour ou un changement d'heure, il reprogramme tout.
 */
object Reveil {

    const val ACTION = "org.unicornradio.app.REVEIL"
    /** Dans l'intention : l'heure dite, en millisecondes. */
    const val HEURE_DITE = "heure_dite"
    /** Dans l'intention du service : l'alarme entière, en texte (une sans-nom n'est déjà plus dans la liste). */
    const val ALARME = "alarme"
    /** Dans l'intention : le numéro de l'alarme. */
    const val ID = "id"

    /** Avec une station, le départ une minute avant l'heure, en muet : la publicité du début passe sans être entendue. */
    const val AVANCE_MS = 60_000L

    enum class Genre { REVEIL, COMPTE }

    /** Ce qui sonne : une de mes stations, ou une sonnerie du téléphone (son adresse, son titre). */
    sealed class Son {
        data class DeStation(val station: Station) : Son()
        data class Sonnerie(val uri: String, val titre: String) : Son()
    }

    /**
     * Un réveil (heure, minute, jours ; aucun jour, une fois) ou un compte à
     * rebours (minutes ; `fin` quand il est lancé). `nom` nul : sans nom, il
     * sert une fois. `actif` : le réveil en marche, le compte lancé.
     */
    data class Alarme(
        val id: Int,
        val genre: Genre,
        val nom: String?,
        val heure: Int = 7,
        val minute: Int = 0,
        val jours: Set<Int> = emptySet(),
        val minutes: Int = 20,
        val son: Son,
        val progressif: Boolean = true,
        val actif: Boolean = false,
        val fin: Long = 0L,
        /** Un réveil : la sonnerie sautée (« Ne pas sonner cette fois »), son heure dite ; 0 sinon. */
        val saute: Long = 0L,
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("reveil", Context.MODE_PRIVATE)

    // ---- La liste ----

    fun alarmes(ctx: Context): List<Alarme> {
        migrer(ctx)
        val t = try { JSONArray(prefs(ctx).getString("alarmes", "[]")) } catch (e: Exception) { JSONArray() }
        return (0 until t.length()).mapNotNull { t.optJSONObject(it)?.let(::alarmeDe) }
    }

    fun alarmes(ctx: Context, genre: Genre) = alarmes(ctx).filter { it.genre == genre }

    fun alarme(ctx: Context, id: Int) = alarmes(ctx).firstOrNull { it.id == id }

    /** Ajoute ou remplace (même numéro), puis programme ou déprogramme selon l'interrupteur. */
    fun garder(ctx: Context, a: Alarme): Long? {
        val liste = alarmes(ctx).toMutableList()
        val i = liste.indexOfFirst { it.id == a.id }
        if (i >= 0) liste[i] = a else liste += a
        ecrire(ctx, liste)
        val quand = if (a.actif) programmer(ctx, a) else { annuler(ctx, a.id); null }
        Annonces.rafraichir(ctx)
        return quand
    }

    fun supprimer(ctx: Context, id: Int) {
        annuler(ctx, id)
        ecrire(ctx, alarmes(ctx).filter { it.id != id })
        Annonces.rafraichir(ctx)
    }

    /** Un numéro libre, pour une nouvelle alarme. */
    fun nouvelId(ctx: Context): Int {
        val p = prefs(ctx)
        val id = maxOf(p.getInt("prochain_id", 10), (alarmes(ctx).maxOfOrNull { it.id } ?: 0) + 1)
        p.edit().putInt("prochain_id", id + 1).apply()
        return id
    }

    /**
     * L'interrupteur : un réveil en marche ou arrêté ; un compte à rebours
     * lancé maintenant ou annulé. L'heure dite, ou `null` si rien n'est
     * programmé. Un sans-nom arrêté disparaît.
     */
    fun basculer(ctx: Context, id: Int, enMarche: Boolean): Long? {
        val a = alarme(ctx, id) ?: return null
        if (!enMarche && a.nom == null) {
            supprimer(ctx, id)
            return null
        }
        val nouvelle = when {
            !enMarche -> a.copy(actif = false, fin = 0L)
            a.genre == Genre.COMPTE -> a.copy(actif = true, fin = System.currentTimeMillis() + a.minutes * 60_000L)
            else -> a.copy(actif = true, saute = 0L)
        }
        return garder(ctx, nouvelle)
    }

    /** Le dernier son choisi, proposé pour la nouvelle alarme suivante. */
    fun dernierSon(ctx: Context): Son? =
        prefs(ctx).getString("dernier_son", null)?.let { try { sonDe(JSONObject(it)) } catch (e: Exception) { null } }

    fun noterLeSon(ctx: Context, son: Son) {
        prefs(ctx).edit().putString("dernier_son", versJson(son).toString()).apply()
    }

    private fun ecrire(ctx: Context, liste: List<Alarme>) {
        val t = JSONArray()
        liste.forEach { t.put(versJson(it)) }
        prefs(ctx).edit().putString("alarmes", t.toString()).apply()
    }

    // ---- La programmation ----

    /** Vrai si Android laisse programmer un réveil à la minute (Android 12 permet de le retirer). */
    fun peutProgrammer(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || gestionnaire(ctx).canScheduleExactAlarms()

    /** L'heure dite de la prochaine sonnerie de cette alarme, après `apres`. */
    fun quand(a: Alarme, apres: Long = System.currentTimeMillis()): Long =
        if (a.genre == Genre.COMPTE) a.fin
        else prochaine(a.heure, a.minute, a.jours, Calendar.getInstance().apply { timeInMillis = apres })

    /** Comme `quand`, la sonnerie sautée (« Ne pas sonner cette fois ») passée : celle d'après. */
    fun quandApresLeSaut(a: Alarme, apres: Long = System.currentTimeMillis()): Long {
        val q = quand(a, apres)
        return if (a.genre == Genre.REVEIL && a.saute >= q) quand(a, a.saute + 1_000) else q
    }

    /**
     * « Ne pas sonner cette fois » : la prochaine sonnerie sautée, le réveil
     * garde les suivantes. Un réveil qui ne sonne qu'une fois s'arrête ;
     * sans nom, il disparaît.
     */
    fun sauter(ctx: Context, id: Int) {
        val a = alarme(ctx, id) ?: return
        when {
            a.nom == null -> supprimer(ctx, id)
            a.jours.isEmpty() -> garder(ctx, a.copy(actif = false))
            else -> garder(ctx, a.copy(saute = quandApresLeSaut(a)))
        }
    }

    /** Programme l'alarme auprès d'Android ; l'heure dite, ou `null` si Android refuse ou si c'est passé. */
    fun programmer(ctx: Context, a: Alarme, apres: Long = System.currentTimeMillis()): Long? {
        val quand = quandApresLeSaut(a, apres)
        if (quand <= System.currentTimeMillis()) return null
        val avance = if (a.son is Son.DeStation) AVANCE_MS else 0L
        val depart = maxOf(quand - avance, System.currentTimeMillis() + 1_000)
        val voir = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return try {
            gestionnaire(ctx).setAlarmClock(AlarmManager.AlarmClockInfo(depart, voir), declencheur(ctx, a.id, quand))
            Log.i(Fichiers.JOURNAL, "alarme ${a.id} « ${a.nom ?: "sans nom"} » : programmée pour ${java.util.Date(quand)}")
            quand
        } catch (e: SecurityException) {
            Log.w(Fichiers.JOURNAL, "alarme ${a.id} : Android refuse le réveil à la minute : ${e.message}")
            null
        }
    }

    private fun annuler(ctx: Context, id: Int) = gestionnaire(ctx).cancel(declencheur(ctx, id))

    /** Tout reprogrammer : au démarrage, après une mise à jour, l'heure changée. Les comptes passés s'éteignent. */
    fun toutProgrammer(ctx: Context) {
        for (a in alarmes(ctx).filter { it.actif }) {
            if (a.genre == Genre.COMPTE && a.fin <= System.currentTimeMillis()) {
                if (a.nom == null) supprimer(ctx, a.id) else garder(ctx, a.copy(actif = false, fin = 0L))
            } else {
                programmer(ctx, a)
            }
        }
        Annonces.rafraichir(ctx)
    }

    /** La prochaine heure, aujourd'hui si elle n'est pas passée, un jour choisi s'il y en a. */
    fun prochaine(heure: Int, minute: Int, jours: Set<Int>, maintenant: Calendar): Long {
        val c = (maintenant.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, heure)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!c.after(maintenant)) c.add(Calendar.DAY_OF_YEAR, 1)
        if (jours.isNotEmpty()) {
            repeat(7) { if (c.get(Calendar.DAY_OF_WEEK) !in jours) c.add(Calendar.DAY_OF_YEAR, 1) }
        }
        return c.timeInMillis
    }

    // ---- Les textes ----

    /** « 07:00 ». */
    fun heureTexte(heure: Int, minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", heure, minute)

    /** « lun. mar. mer. jeu. ven. », ou `uneFois` sans jour. */
    fun joursTexte(ctx: Context, jours: Set<Int>, uneFois: String): String {
        if (jours.isEmpty()) return uneFois
        val noms = DateFormatSymbols.getInstance(ctx.resources.configuration.locales[0] ?: Locale.getDefault()).shortWeekdays
        return JOURS.filter { it in jours }.joinToString(" ") { noms[it] }
    }

    /** « 20 min », « 1 h », « 1 h 45 ». */
    fun dureeTexte(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> String.format(Locale.ROOT, "%d h %02d", minutes / 60, minutes % 60)
    }

    fun sonTexte(son: Son): String = when (son) {
        is Son.DeStation -> son.station.nom
        is Son.Sonnerie -> son.titre
    }

    /** Les jours dans l'ordre d'une semaine qui commence le lundi. */
    val JOURS = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
    )

    // ---- Le stockage ----

    private fun gestionnaire(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    /** Une intention par alarme, sous son numéro : toutes se programment côte à côte. */
    private fun declencheur(ctx: Context, id: Int, heureDite: Long = 0L): PendingIntent = PendingIntent.getBroadcast(
        ctx, id,
        Intent(ctx, Recepteur::class.java).setAction(ACTION).putExtra(HEURE_DITE, heureDite).putExtra(ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun versJson(a: Alarme) = JSONObject()
        .put("id", a.id).put("genre", a.genre.name).put("nom", a.nom ?: JSONObject.NULL)
        .put("heure", a.heure).put("minute", a.minute).put("jours", JSONArray(a.jours.sorted()))
        .put("minutes", a.minutes).put("son", versJson(a.son))
        .put("progressif", a.progressif).put("actif", a.actif).put("fin", a.fin).put("saute", a.saute)

    /** L'alarme lue depuis son texte (l'intention du service). */
    fun alarmeDe(texte: String?): Alarme? = texte?.let { try { alarmeDe(JSONObject(it)) } catch (e: Exception) { null } }

    private fun alarmeDe(o: JSONObject): Alarme? {
        val son = o.optJSONObject("son")?.let(::sonDe) ?: return null
        val jours = o.optJSONArray("jours")?.let { t -> (0 until t.length()).map { t.optInt(it) }.toSet() } ?: emptySet()
        return Alarme(
            id = o.optInt("id"),
            genre = if (o.optString("genre") == Genre.COMPTE.name) Genre.COMPTE else Genre.REVEIL,
            nom = if (o.isNull("nom")) null else o.optString("nom").ifBlank { null },
            heure = o.optInt("heure", 7), minute = o.optInt("minute"), jours = jours,
            minutes = o.optInt("minutes", 20).coerceAtLeast(1), son = son,
            progressif = o.optBoolean("progressif", true), actif = o.optBoolean("actif"), fin = o.optLong("fin"),
            saute = o.optLong("saute"),
        )
    }

    private fun versJson(son: Son): JSONObject = when (son) {
        is Son.DeStation -> JSONObject().put("type", "station").put("station", versJson(son.station))
        is Son.Sonnerie -> JSONObject().put("type", "sonnerie").put("uri", son.uri).put("titre", son.titre)
    }

    private fun sonDe(o: JSONObject): Son? = when (o.optString("type")) {
        "station" -> o.optJSONObject("station")?.let(::stationDe)?.let { Son.DeStation(it) }
        "sonnerie" -> o.optString("uri").ifBlank { null }?.let { Son.Sonnerie(it, o.optString("titre")) }
        else -> null
    }

    private fun versJson(s: Station) = JSONObject()
        .put("uuid", s.uuid).put("nom", s.nom).put("url", s.url)
        .put("logo", s.logo ?: JSONObject.NULL).put("hls", s.hls)
        .put("pays", s.paysCode ?: JSONObject.NULL)

    private fun stationDe(o: JSONObject): Station? {
        val url = o.optString("url").ifBlank { return null }
        return Station(
            uuid = o.optString("uuid"), nom = o.optString("nom"), url = url,
            paysCode = if (o.isNull("pays")) null else o.optString("pays"),
            pays = null, page = null,
            logo = if (o.isNull("logo")) null else o.optString("logo"),
            etiquettes = null, langue = null, codec = null, debit = null, votes = null,
            hls = o.optBoolean("hls"), ordre = 0,
        )
    }

    /**
     * Les essais d'avant gardaient un seul réveil (en un objet « reglage »,
     * puis réglage par réglage) et des comptes à rebours nommés à part :
     * repris une fois en alarmes, le réveil sous le nom « Réveil ».
     */
    private fun migrer(ctx: Context) {
        val p = prefs(ctx)
        if (p.contains("alarmes")) return
        val liste = ArrayList<Alarme>()
        var id = 10
        try {
            val ancien = p.getString("reglage", null)?.let { JSONObject(it) }
            val station = (ancien?.optJSONObject("station") ?: p.getString("station", null)?.let { JSONObject(it) })?.let(::stationDe)
            val heure = ancien?.optInt("heure") ?: p.getInt("heure", -1)
            val minute = ancien?.optInt("minute") ?: p.getInt("minute", 0)
            val jours = (ancien?.optJSONArray("jours") ?: p.getString("jours", null)?.let { JSONArray(it) })
                ?.let { t -> (0 until t.length()).map { t.optInt(it) }.toSet() } ?: JOURS.take(5).toSet()
            if (station != null && heure >= 0) {
                liste += Alarme(id++, Genre.REVEIL, "Réveil", heure, minute, jours, son = Son.DeStation(station), actif = p.getBoolean("actif", false))
            }
            if (station != null) {
                val comptes = JSONArray(p.getString("comptes", "[]"))
                for (i in 0 until comptes.length()) {
                    val c = comptes.optJSONObject(i) ?: continue
                    val nom = c.optString("nom").takeIf { it.isNotBlank() } ?: continue
                    liste += Alarme(id++, Genre.COMPTE, nom, minutes = c.optInt("minutes", 20).coerceAtLeast(1), son = Son.DeStation(station))
                }
                p.edit().putString("dernier_son", versJson(Son.DeStation(station)).toString()).apply()
            }
        } catch (e: Exception) {
            Log.w(Fichiers.JOURNAL, "alarmes : reprise des anciens réglages impossible : ${e.message}")
        }
        // Les anciennes intentions, numéros 0 et 1, ne servent plus.
        for (vieux in 0..1) gestionnaire(ctx).cancel(declencheur(ctx, vieux))
        p.edit()
            .remove("reglage").remove("station").remove("heure").remove("minute").remove("jours").remove("actif")
            .remove("comptes").remove("compte_fin").remove("compte_nom")
            .putInt("prochain_id", id)
            .apply()
        ecrire(ctx, liste)
        toutProgrammer(ctx)
    }

    /** Le départ ; ou le téléphone qui redémarre, l'application mise à jour, l'heure changée. */
    class Recepteur : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intention: Intent) {
            if (intention.action != ACTION) {
                toutProgrammer(ctx)
                return
            }
            val id = intention.getIntExtra(ID, -1)
            val a = alarme(ctx, id) ?: return
            Log.i(Fichiers.JOURNAL, "alarme $id « ${a.nom ?: "sans nom"} » : départ")
            try {
                val heureDite = intention.getLongExtra(HEURE_DITE, 0L).takeIf { it > 0 } ?: System.currentTimeMillis()
                ContextCompat.startForegroundService(
                    ctx, Intent(ctx, ServiceLecture::class.java).setAction(ACTION).putExtra(HEURE_DITE, heureDite).putExtra(ALARME, versJson(a).toString()),
                )
            } catch (e: Exception) {
                Log.w(Fichiers.JOURNAL, "alarme $id : service impossible à lancer : ${e.message}")
            }
            when {
                // Sans nom : une fois, puis il disparaît (le service a déjà ce qu'il lui faut).
                a.nom == null -> supprimer(ctx, id)
                a.genre == Genre.COMPTE -> garder(ctx, a.copy(actif = false, fin = 0L))
                a.jours.isEmpty() -> garder(ctx, a.copy(actif = false))
                // La fois suivante, après l'heure dite (le départ a pu être une minute avant).
                else -> programmer(ctx, a, System.currentTimeMillis() + 2 * AVANCE_MS)
            }
            Annonces.rafraichir(ctx)
        }
    }
}
