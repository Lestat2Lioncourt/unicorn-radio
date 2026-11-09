# 016 — Télécharger un épisode

Le 2026-11-09, dans le bloc de sept. Le quatrième palier des podcasts,
pour écouter sans réseau : dans le train, en avion.

## Ce qui est fait

**« Télécharger »** par appui long sur un épisode, dans un podcast ou dans
la liste de lecture : Android télécharge (`DownloadManager`), avec sa
notification et sa reprise, dans le dossier Podcasts privé de
l'application, sous un nom stable tiré de la clef de l'épisode
(`podcasts/Telechargements.kt`, `telechargements.json`). La ligne dit
« Téléchargement… », puis « Téléchargé » quand Android l'annonce, par la
diffusion `ACTION_DOWNLOAD_COMPLETE` que l'écran écoute ; les
téléchargements finis pendant qu'on n'était pas là sont revus à chaque
retour au premier plan. Un échec oublie la note.

**Jouer sans réseau** : un épisode téléchargé joue depuis son fichier,
`MainActivity.elementDe` donne au lecteur l'adresse locale à la place de
l'adresse en ligne, à la main comme à la suite dans la liste de lecture.
Le lecteur lit désormais les fichiers autant que le http
(`DefaultDataSource` par-dessus la source http du service). La position
d'écoute se note pareil.

**« Supprimer le téléchargement »**, à la place de « Télécharger » sur un
épisode téléchargé ou en cours : le fichier s'efface, un téléchargement en
route est annulé.

Pas de liste des téléchargements à part : ils se voient sur les lignes,
dans le podcast et dans la liste de lecture. Et pas de téléchargement
d'office des nouveaux épisodes ; peut-être plus tard.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/podcasts/Telechargements.kt`
(nouveau), `podcasts/Elements.kt` (`versElement(adresse)`),
`lecture/ServiceLecture.kt`, `MainActivity.kt`, `ListeAdaptateur.kt`,
`res/values*/strings.xml`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.1.0-debug.apk` par-dessus.

- Appui long sur un épisode, « Télécharger » : « Téléchargement de : … »,
  la notification d'Android, la ligne « Téléchargement… », puis
  « Téléchargé » à la fin sans rien faire.
- Mode avion : l'épisode téléchargé joue ; un autre, non téléchargé, dit
  « Injoignable ».
- « Lire ensuite » sur un épisode téléchargé, puis la fin du précédent :
  il part depuis son fichier.
- Appui long, « Supprimer le téléchargement » : la marque disparaît ; en
  mode avion, il ne joue plus.
- Quitter l'application pendant un téléchargement, la rouvrir après :
  « Téléchargé ».
