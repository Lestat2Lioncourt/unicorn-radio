# 018 — Une vignette par épisode

Le 2026-11-09, dans le bloc de sept, la seconde consigne du 234 côté PC :
« le rappel vignette devant chaque épisode et devant l'épisode en cours de
lecture », à la manière des lecteurs de podcasts courants, sans reprendre
leur mise en page.

## Ce qui est fait

Chaque ligne d'épisode a sa vignette à gauche, quarante points, en carré
plein comme une pochette, coins à peine arrondis : l'image propre de
l'épisode quand le flux en donne une, la balise `itunes:image` de
l'élément, lue désormais par `Flux` (`Episode.imagePropre`) ; sinon
l'image du podcast ; les initiales du titre si rien ne charge. La liste de
lecture et l'épisode en cours, dans la ligne de lecture, la notification et
l'écran de verrouillage, montrent la même (`Episode.vignette`, posée dans
l'élément du lecteur).

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/podcasts/Podcast.kt`,
`podcasts/Flux.kt`, `podcasts/Elements.kt`, `ListeAdaptateur.kt`,
`res/layout/ligne_episode.xml`, `res/drawable/carre.xml` (nouveau).

## Vérification

À relancer : installer `pkg/unicorn-radio-1.1.0-debug.apk` par-dessus.

- Un podcast ouvert : chaque épisode a sa vignette ; un épisode qui a sa
  propre image, un best-of par exemple, la montre, les autres celle du
  podcast.
- Un épisode qui joue : la même image en bas et dans la notification.
- Sans réseau : les initiales du titre dans un carré.
