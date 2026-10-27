package org.unicornradio.app.podcasts

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Un épisode devient un élément du lecteur : le titre de l'épisode, le nom
 * du podcast en artiste, l'image, et dans les extras ce qu'il faut pour
 * noter où l'on en est, le flux et l'identifiant.
 */
fun Episode.versElement(): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setUri(url)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(titre)
            .setArtist(podcast)
            .setAlbumTitle(podcast)
            .setArtworkUri(image?.toUri())
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .setExtras(bundleOf(CLEF_FLUX to flux, CLEF_GUID to guid, CLEF_DUREE to (duree ?: -1L)))
            .build(),
    )
    .build()

const val CLEF_FLUX = "flux"
const val CLEF_GUID = "guid"
const val CLEF_DUREE = "duree"

/** Un élément du lecteur est-il un épisode de podcast ? */
val MediaItem.estUnEpisode: Boolean
    get() = mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
