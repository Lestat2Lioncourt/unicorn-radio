# 041 — Les titres aimés

Le 2026-11-12. Le chantier suivant de la feuille de route : garder un
morceau entendu à la radio pour le retrouver plus tard.

## Ce qui change

- **Le cœur** sur la ligne de lecture, à gauche de la minuterie, quand
  une station annonce un titre (pas pour un épisode, ni entre deux
  morceaux, quand le titre est le nom de la station). Un appui aime le
  titre, un second ne l'aime plus ; plein quand il est aimé.
- **Dans « Précédemment »** : un cœur devant les titres aimés, et un
  appui sur un titre passé l'aime ou ne l'aime plus. Le morceau qu'on a
  aimé est souvent fini quand on prend le téléphone.
- **« Titres aimés… »** dans le menu « … » : le plus récent en tête, la
  station et le jour dessous. Un appui : « Chercher sur le web »,
  « Partager… » (vers une application de musique, une messagerie),
  « Copier », « Retirer ». Vide, une phrase dit comment en ajouter.
- Le même titre sur deux stations fait deux souvenirs.
- **Sur le téléphone seulement** (`titres-aimes.json`) : ni dans le
  fichier d'export, ni dans la sauvegarde automatique, ni au serveur.

Version 1.4.0.1, code 58, en essai.

## Fichiers

`lecture/TitresAimes.kt` (nouveau), `res/drawable/ic_coeur.xml` et
`ic_coeur_vide.xml` (nouveaux), `MainActivity.kt`,
`res/layout/activity_main.xml`, `res/menu/principal.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.4.0.1-debug.apk`.

- Une station qui annonce ses titres : le cœur vide apparaît ; un appui,
  « Aimé : … », le cœur se remplit. Au titre suivant, il redevient vide.
- Un épisode, ou une station sans titre : pas de cœur.
- Appui sur les textes de la ligne de lecture, « Précédemment » : un
  titre passé touché est aimé, un cœur devant à la réouverture.
- « … », « Titres aimés… » : la liste ; un titre, « Chercher sur le
  web », « Partager… », « Copier », « Retirer ».
- Sans titre aimé : la phrase d'explication.
- Le menu reste lisible en français et en anglais.
