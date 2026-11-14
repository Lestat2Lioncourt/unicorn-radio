package org.unicornradio.app.lecture

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import org.json.JSONObject
import org.unicornradio.app.modele.Station
import org.unicornradio.app.podcasts.Ecoutes
import org.unicornradio.app.podcasts.FileDEcoute
import org.unicornradio.app.podcasts.Telechargements
import org.unicornradio.app.podcasts.estUnEpisode
import org.unicornradio.app.podcasts.versElement

/**
 * Ce qu'on écoutait en dernier, pour le remettre d'un appui : à l'ouverture
 * de l'application, depuis le widget, depuis les commandes de lecture
 * d'Android. Le service le note à chaque changement d'élément.
 *
 * Une station est gardée entière, telle que le lecteur l'avait. Un épisode
 * ne l'est pas : la liste de lecture garde déjà les épisodes passés, le
 * courant en dernier, et ceux à suivre ; on la rejoue, l'épisode courant
 * repris à sa position.
 */
object DerniereEcoute {

    /** Des éléments pour le lecteur, celui où commencer, et à quelle position. */
    class AJouer(val elements: List<MediaItem>, val index: Int, val positionMs: Long)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("derniere_ecoute", Context.MODE_PRIVATE)

    /** Noté par le service quand un élément démarre. */
    fun noter(ctx: Context, element: MediaItem) {
        // La sonnerie d'une alarme n'est pas une écoute.
        if (element.mediaId == "reveil-sonnerie") return
        val o = if (element.estUnEpisode) {
            JSONObject().put("type", "episode")
        } else {
            val fiche = element.mediaMetadata
            val nom = (fiche.station ?: fiche.artist ?: fiche.title)?.toString() ?: return
            val url = element.localConfiguration?.uri?.toString() ?: return
            JSONObject().put("type", "station")
                .put("uuid", element.mediaId)
                .put("nom", nom)
                .put("url", url)
                .put("logo", fiche.artworkUri?.takeIf { it != LOGO_PAR_DEFAUT }?.toString())
                .put("hls", element.localConfiguration?.mimeType == androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                .put("pays", fiche.extras?.getString(CLEF_PAYS))
        }
        prefs(ctx).edit().putString("element", o.toString()).apply()
    }

    /** Le nom à montrer (widget), sans rien construire : la station, ou le titre de l'épisode courant. */
    fun nom(ctx: Context): String? {
        val o = lu(ctx) ?: return null
        return if (o.optString("type") == "station") o.optString("nom").ifBlank { null }
        else FileDEcoute(ctx).passes().lastOrNull()?.titre
    }

    /** De quoi reprendre, ou `null` s'il n'y a rien. Lit des fichiers : hors du fil principal de préférence. */
    fun aJouer(ctx: Context): AJouer? {
        val o = lu(ctx) ?: return null
        return when (o.optString("type")) {
            "station" -> {
                val station = station(o) ?: return null
                AJouer(listOf(station.versElement()), 0, C.TIME_UNSET)
            }
            "episode" -> {
                val file = FileDEcoute(ctx)
                val passes = file.passes()
                val courant = passes.lastOrNull() ?: return null
                val suite = file.aSuivre().filter { it.clef != courant.clef }
                val telechargements = Telechargements(ctx)
                val elements = (passes + suite).map { e ->
                    e.versElement(telechargements.fichierDe(e)?.let { Uri.fromFile(it).toString() } ?: e.url)
                }
                val ecoute = Ecoutes(ctx).pour(courant.flux)[courant.guid]
                // Les écoutes comptent en secondes.
                val position = if (ecoute != null && !ecoute.termine) ecoute.position * 1_000 else 0L
                AJouer(elements, passes.lastIndex, position)
            }
            else -> null
        }
    }

    /** La dernière station écoutée, telle que le lecteur l'avait, ou `null` si c'était un épisode. */
    fun station(ctx: Context): Station? = lu(ctx)?.takeIf { it.optString("type") == "station" }?.let { station(it) }

    private fun station(o: JSONObject): Station? {
        val url = o.optString("url").ifBlank { return null }
        return Station(
            uuid = o.optString("uuid"),
            nom = o.optString("nom"),
            url = url,
            paysCode = o.optString("pays").ifBlank { null }.takeIf { !o.isNull("pays") },
            pays = null, page = null,
            logo = o.optString("logo").ifBlank { null }.takeIf { !o.isNull("logo") },
            etiquettes = null, langue = null, codec = null, debit = null, votes = null,
            hls = o.optBoolean("hls"),
            ordre = 0,
        )
    }

    private fun lu(ctx: Context): JSONObject? =
        prefs(ctx).getString("element", null)?.let { try { JSONObject(it) } catch (e: Exception) { null } }
}
