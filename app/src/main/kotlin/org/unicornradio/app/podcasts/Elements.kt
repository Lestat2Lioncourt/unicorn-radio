package org.unicornradio.app.podcasts

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONObject

/**
 * Un épisode devient un élément du lecteur : le titre de l'épisode, le nom
 * du podcast en artiste, sa vignette, et dans les extras ce qu'il faut pour
 * noter où l'on en est, le flux et l'identifiant, et l'épisode entier pour
 * la liste de lecture. `adresse` permet de jouer un fichier téléchargé à la
 * place du fichier en ligne.
 */
fun Episode.versElement(adresse: String = url): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setUri(adresse)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(titre)
            .setArtist(podcast)
            .setAlbumTitle(podcast)
            .setArtworkUri(vignette?.toUri())
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .setExtras(
                bundleOf(
                    CLEF_FLUX to flux, CLEF_GUID to guid, CLEF_DUREE to (duree ?: -1L),
                    CLEF_EPISODE to versJson().toString(),
                ),
            )
            .build(),
    )
    .build()

const val CLEF_FLUX = "flux"
const val CLEF_GUID = "guid"
const val CLEF_DUREE = "duree"
const val CLEF_EPISODE = "episode"

/** Un élément du lecteur est-il un épisode de podcast ? */
val MediaItem.estUnEpisode: Boolean
    get() = mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE

/** L'épisode qu'un élément du lecteur porte, tel qu'il a été mis dans la file ; `null` pour une station. */
val MediaItem.episode: Episode?
    get() = mediaMetadata.extras?.getString(CLEF_EPISODE)?.let { runCatching { Episode.depuisJson(JSONObject(it)) }.getOrNull() }
