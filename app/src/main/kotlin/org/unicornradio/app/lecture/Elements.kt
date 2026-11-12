package org.unicornradio.app.lecture

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import org.unicornradio.app.modele.Station

/** Dans les extras d'une station : le code de son pays, pour la retrouver par le nom si l'annuaire l'a refaite. */
const val CLEF_PAYS = "pays_code"

/**
 * L'image d'une station sans logo, ou d'un épisode sans vignette : le logo
 * d'Unicorn-Radio, en couleurs. Sans image, le lecteur d'Android agrandissait
 * la petite icône d'une couleur, la licorne de la barre d'état.
 */
val LOGO_PAR_DEFAUT: android.net.Uri = android.net.Uri.parse("android.resource://${org.unicornradio.app.BuildConfig.APPLICATION_ID}/drawable/logo")

/**
 * Une station devient un élément du lecteur. Le nom va dans `station` et
 * `artist`, et d'abord dans `title` ; le titre ICY du flux, quand il arrive,
 * est posé dans `title` par le service (`ServiceLecture.titres`), car le
 * lecteur seul garderait la fiche par-dessus le flux. La notification lit
 * alors « titre — station ».
 */
fun Station.versElement(): MediaItem = MediaItem.Builder()
    .setMediaId(uuid)
    .setUri(url)
    .setMimeType(if (hls) MimeTypes.APPLICATION_M3U8 else null)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(nom)
            .setArtist(nom)
            .setStation(nom)
            .setArtworkUri(logo?.toUri() ?: LOGO_PAR_DEFAUT)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(Bundle().apply { paysCode?.let { putString(CLEF_PAYS, it) } })
            .build(),
    )
    .build()
