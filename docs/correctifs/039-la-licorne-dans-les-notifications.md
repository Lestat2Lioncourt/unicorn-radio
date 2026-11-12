# 039 — La licorne dans les notifications

Le 2026-11-12. « Dans les notifications du téléphone, on pourrait avoir
le logo Unicorn-Radio au lieu d'un logo player impersonnel ? »

## Ce qui n'allait pas

Toutes les notifications, celle du lecteur, celle d'une alarme qui
sonne, celles de ce qui est programmé (038), portaient la petite icône de
Media3, un triangle de lecture.

## Ce qui change

**Une licorne d'une seule couleur** (`res/drawable/ic_licorne.xml`) :
Android ne garde de la petite icône que sa forme, le logo en couleurs y
deviendrait un disque blanc. Tirer une silhouette du logo donnait une
tache illisible à 24 points ; elle est dessinée en traits sur la grille
de 24 × 24 : la tête de profil tournée vers la gauche, la corne,
l'oreille, la crinière en dents, l'œil et le naseau évidés.

- **La notification du lecteur** : `DefaultMediaNotificationProvider`
  avec `setSmallIcon(ic_licorne)`, donné au service.
- **Celle d'une alarme qui sonne** et **celles de ce qui est programmé** :
  la licorne en petite icône, et **le logo en couleurs** en grande icône,
  à droite du texte.

**Après l'essai de la 1.3.0.21** : « dans le player Android, on ne voit
plus le logo original mais celui utilisé dans la barre d'état, qui est très
laid en grand » ; « si la station n'a pas de logo, on pourrait prendre le
logo Unicorn-Radio » ; et la licorne « avec le bleu de la crinière ».

- **Une image toujours** : une station sans logo, un épisode sans
  vignette, portent le logo d'Unicorn-Radio en couleurs
  (`LOGO_PAR_DEFAUT`, `android.resource://…/drawable/logo`) ; le lecteur
  d'Android n'a plus à agrandir la petite icône. Sur la ligne de lecture
  de l'application, une station sans logo garde ses initiales, et la
  dernière écoute ne le garde pas comme logo de la station.
- **La licorne teinte en bleu** (`@color/licorne`, #3D5AFE) dans nos
  notifications, alarmes et ce qui est programmé. Un fond violet sous une
  licorne bleue n'est pas possible pour la petite icône : Android n'en
  garde que la forme et une seule teinte.

Version 1.3.0.21, code 54, puis 1.3.0.22, code 55, en essai.

## Fichiers

`res/drawable/ic_licorne.xml` (nouveau), `lecture/ServiceLecture.kt`,
`lecture/Annonces.kt`, `lecture/Elements.kt`, `podcasts/Elements.kt`,
`lecture/DerniereEcoute.kt`, `MainActivity.kt`, `res/values/colors.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.22.

- Une station en lecture : dans la barre d'état et en tête de la
  notification, la licorne.
- Un compte à rebours lancé : sa notification, la licorne en petit, le
  logo en couleurs à droite.
- Un réveil qui sonne : même chose.
- Une station sans logo : dans le lecteur d'Android, le logo
  d'Unicorn-Radio en couleurs ; sur la ligne de lecture, ses initiales.
- La licorne des notifications d'alarme : bleue.
