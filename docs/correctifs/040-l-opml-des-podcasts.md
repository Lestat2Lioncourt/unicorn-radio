# 040 — L'OPML des podcasts

Le 2026-11-12. « Go pour l'OPML » : pouvoir venir d'une autre
application de podcasts avec ses abonnements, et y repartir.

## Ce que c'est

L'OPML est le fichier d'abonnements que presque toutes les applications
de podcasts savent écrire et lire (Pocket Casts, AntennaPod, Podcast
Addict, Overcast, Castbox ; pas Spotify) : un `<outline>` par podcast,
son nom (`text`) et l'adresse de son flux (`xmlUrl`). **Seuls les
abonnements voyagent** : ni les positions, ni les épisodes écoutés, ni
les téléchargements.

## Ce qui change

- **Importer** : dans l'onglet Podcasts, l'entrée du menu « … » devient
  « Importer des podcasts (OPML)… » et propose tous les fichiers, le type
  d'un OPML variant d'une application à l'autre. La lecture reconnaît
  l'OPML à son contenu (`Opml.enEst`), quelle que soit l'entrée : celle de
  l'onglet Radio et « Ouvrir avec » (`text/x-opml`) le prennent aussi.
  Les podcasts sont versés comme ceux d'un fichier d'export : rien
  d'effacé, rien de doublé, les gestes `podcast.ajouter` partent au
  serveur quand on est apparié. Les dossiers que certaines applications
  mettent dans l'OPML sont aplatis.
- **L'image et l'auteur** : l'OPML ne donne que le nom. Le flux de
  chaque podcast nouveau sans image est lu, quatre à la fois, avant le
  versement (« Lecture de N flux… ») ; un flux qui ne répond pas garde ce
  que le fichier disait.
- **Exporter** : « Exporter mes podcasts (OPML)… », dans l'onglet
  Podcasts, écrit `unicorn-podcasts-AAAA-MM-JJ.opml`, les podcasts suivis
  dans l'ordre, avec leur image (`imageUrl`).

Version 1.4.0, code 57, en essai.

## Fichiers

`podcasts/Opml.kt` (nouveau), `fichier/LectureFichier.kt`,
`MainActivity.kt`, `res/menu/principal.xml`, `AndroidManifest.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.4.0-debug.apk`.

- Dans une autre application de podcasts, exporter ses abonnements en
  OPML, l'envoyer sur le téléphone.
- Onglet Podcasts, « … », « Importer des podcasts (OPML)… », choisir le
  fichier : « Lecture de N flux… », puis « Liste importée : 0 stations,
  N podcasts ajoutés » ; ils sont dans « Mes podcasts », avec leur image.
- Ouvrir l'un d'eux qui n'était pas dans l'annuaire : ses épisodes.
- Réimporter le même fichier : 0 podcast ajouté.
- « Exporter mes podcasts (OPML)… » : le fichier s'ouvre dans l'autre
  application, ou se relit par l'import.
- Onglet Radio : le menu montre toujours « Importer une liste… » et
  « Exporter mes radios… », sans l'entrée OPML.
