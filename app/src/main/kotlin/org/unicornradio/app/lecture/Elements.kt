package org.unicornradio.app.lecture

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import org.unicornradio.app.modele.Station

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
            .setArtworkUri(logo?.toUri())
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .build(),
    )
    .build()
