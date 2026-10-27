# 008 — Les podcasts : chercher et écouter

Le 2026-10-27, le premier des quatre paliers des podcasts, dans l'ordre
que le bureau a suivi : chercher et écouter. Consigne de l'utilisateur :
« l'interface doit reprendre ce qui se fait pour l'app Windows : 2
boutons radio / podcasts en haut, chacun ses champs de recherche », et
« que les deux interfaces se ressemblent un minimum ». Les paliers
suivants : « Mes podcasts » et le collage d'une adresse de flux, la file
d'écoute, le téléchargement.

## Ce qui est fait

**Deux onglets en tête** (`activity_main.xml`, `MainActivity`), Radio et
Podcasts, sous la barre, à la racine seulement : dans un dossier, la
recherche de l'annuaire ou un podcast ouvert, ils laissent la place. La
loupe et le fichier de la barre sont de l'onglet Radio. Le bouton retour,
dans l'onglet Podcasts, ferme le podcast ouvert, puis revient à Radio.

**La recherche** (`podcasts/AnnuairePodcasts.kt`) dans l'annuaire d'Apple
Podcasts par nom, cinquante résultats au plus, après deux lettres et
quatre cents millisecondes sans frappe : une ligne par podcast, image,
nom, auteur, nombre d'épisodes, la même ligne que les stations. L'annuaire
ne sert qu'à trouver l'adresse du flux, qui fait foi ensuite.

**Le flux RSS** (`podcasts/Flux.kt`), lu et analysé sur le téléphone,
`XmlPullParser` avec les espaces de noms, jusqu'à cinq redirections,
http vers https compris : l'en-tête, titre, auteur et image, et les
épisodes, du plus récent au plus ancien, avec date, durée annoncée et le
fichier audio de leur `<enclosure>`, reconnu au type `audio/` ou à
l'extension. Un flux dont aucun élément n'a de son est dit : « Ce flux n'a
pas de son : il liste des articles, pas des épisodes. » Un XML cassé
après quelques épisodes garde ceux lus.

**Un podcast ouvert** : son nom et son auteur dans la barre, la liste de
ses épisodes (`ligne_episode.xml`), chacun avec sa date et sa durée, un
trait d'avancement et « Reprendre à 12:34 » s'il est commencé, « Écouté »
s'il est terminé.

**La lecture** : le même lecteur que les radios, l'épisode seul dans la
file, repris où l'on en était s'il n'est pas terminé. Sous la ligne de
lecture, pour un épisode, une barre qu'on peut tirer, le temps restant,
quinze secondes en arrière et en avant. La notification et les boutons
du casque font le reste. Le nom du podcast tient lieu de titre de flux.

**Où l'on en est** (`podcasts/Ecoutes.kt`, `ServiceLecture`) : le service
note la position toutes les dix secondes pendant la lecture, à la pause, à
l'arrêt, au changement d'élément et à la fin ; terminé à moins d'une
minute de la fin. Gardé sur le téléphone dans `ecoutes.json` et, apparié,
poussé au serveur par `PUT /api/radio/podcasts/ecoutes` dès qu'il répond,
gardé en attente sinon. À l'ouverture d'un podcast, ce qui attend est
poussé, puis ce que le serveur sait de ce flux est pris quand c'est plus
récent : la reprise est la même sur le PC et le téléphone. Les secondes
entières sont l'unité des positions, des deux côtés, et l'horodatage
`le`, en millisecondes depuis l'époque, est posé par le serveur à la
réception et par le téléphone à la note ; c'est lui qui départage. Le
0.8.0 l'attendait sous un autre nom et en chaîne ISO, corrigé en 0.8.1,
code 11, après vérification côté serveur.

**Le service** reconnaît un épisode à son type dans les métadonnées, et
ne le signale pas à l'annuaire des radios.

Version 0.8.1, code 11. Aucune dépendance ajoutée.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/podcasts/Podcast.kt`,
`AnnuairePodcasts.kt`, `Flux.kt`, `Ecoutes.kt`, `Elements.kt` (nouveaux),
`MainActivity.kt`, `ListeAdaptateur.kt`, `lecture/ServiceLecture.kt`,
`serveur/ClientServeur.kt`, `res/layout/activity_main.xml`,
`res/layout/ligne_episode.xml`, `res/drawable/ic_reculer.xml`,
`ic_avancer.xml` (nouveaux), `res/values*/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.8.1-debug.apk` par-dessus
l'ancien.

- À la racine, les deux onglets Radio et Podcasts sous la barre ; dans un
  dossier ou la recherche, ils disparaissent.
- Onglet Podcasts : le champ « Chercher des podcasts… » ; deux lettres
  d'un nom connu : des résultats avec image, auteur et nombre d'épisodes.
- Un podcast : « Lecture du flux… » dans la barre, puis ses épisodes du
  plus récent au plus ancien, avec date et durée.
- Un épisode : il joue, le nom de l'épisode et celui du podcast en bas,
  la barre d'avancement et le temps restant qui défilent ; les boutons
  quinze secondes ; tirer la barre saute.
- Pause, retour arrière, le même podcast rouvert : l'épisode porte
  « Reprendre à … » et un trait ; appuyer reprend là.
- Apparié, le même épisode sur le PC reprend à la même position, et
  inversement après une écoute sur le PC.
- Écouter jusqu'à moins d'une minute de la fin : « Écouté ».
- Bouton retour depuis les épisodes : la recherche ; encore : l'onglet
  Radio.
- Une radio après un épisode : la barre d'avancement disparaît.
