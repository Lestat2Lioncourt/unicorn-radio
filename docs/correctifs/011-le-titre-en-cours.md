# 011 — Le titre en cours, enfin affiché

Le 2026-10-29. « Quand la radio annonce le titre en cours de lecture, on
pourrait l'afficher ? » Il était censé l'être depuis le premier palier, et
ne l'a jamais été : sur OÜI FM Alternatif, le PC montre le titre, le
téléphone « En direct ».

## Le défaut

Le flux de la station envoie bien les titres : lu à la main avec
`Icy-MetaData: 1`, « The Middle - JIMMY EAT WORLD » dans ses métadonnées.
Le lecteur Media3 les lit aussi. Mais quand il combine la fiche de
l'élément et ce qui vient du flux, `ExoPlayerImpl.buildUpdatedMediaMetadata`
pose la fiche **par-dessus** le flux, vérifié dans le code décompilé de la
1.11.1. L'application mettait le nom de la station dans le champ `title` de
la fiche, pour que la notification ait un titre avant que le flux parle ;
ce nom cachait donc le titre du flux, sur toutes les stations. Le
commentaire d'`Elements.kt` supposait l'ordre inverse.

Second défaut, plus petit : la ligne du titre était prévue pour défiler
quand il est trop long, mais le défilement d'une `TextView` ne part que si
elle est « sélectionnée », ce qui n'était pas fait ; un titre long était
coupé net.

## La correction

`ServiceLecture.titres`, un écouteur `onMetadata` du lecteur : à chaque
bloc de métadonnées ICY, le titre, nettoyé, est posé dans la fiche de
l'élément en cours par `replaceMediaItem` sur place, même adresse, ce que
Media3 fait sans couper le son (`ProgressiveMediaSource.canUpdateMediaItem`,
HLS pareil). La station qui n'annonce plus rien, titre vide, reprend son
nom, et l'écran redit « En direct ». Les épisodes de podcast ne sont pas
touchés. La barre de lecture, la notification et l'écran de verrouillage
en profitent ensemble, puisque tous lisent la fiche. Et `lectureEtat` est
sélectionnée une fois pour toutes, le défilement part.

Version 1.0.1, code 19. Aucune dépendance ajoutée.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/lecture/ServiceLecture.kt`,
`lecture/Elements.kt`, `MainActivity.kt`, `app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-1.0.1-debug.apk`
par-dessus, ou attendre la 1.0.1 publiée.

- OÜI FM Alternatif : sous le nom, le titre en cours au bout de quelques
  secondes, le même que sur le PC ; il change avec le morceau.
- La notification : « titre — station » ; l'écran de verrouillage pareil.
- Un titre plus long que la ligne défile.
- Une station sans titres, un flux HLS par exemple : « En direct », comme
  avant.
- Un épisode de podcast : inchangé, le nom du podcast sous le titre.
