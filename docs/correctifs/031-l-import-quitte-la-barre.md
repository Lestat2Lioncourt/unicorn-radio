# 031 — L'import quitte la barre

Le 2026-11-12. « Maintenant qu'on a mis la fonction importer dans le menu
"…", est-ce que le bouton fichier dans la barre de menu est toujours
utile ? »

## Ce qui change

L'icône fichier de la barre du haut, « Importer une liste… », est
retirée. On importe rarement : en arrivant, ou pour reprendre la liste de
quelqu'un ; l'entrée du menu « … », à côté de « Exporter mes radios… »
(024), suffit, et c'est là qu'on la cherchait. La barre garde ce qui sert
tout le temps : la loupe, le serveur, les drapeaux.

Restent : l'entrée du menu « … », dans l'onglet Radio, et le bouton
« Importer une liste… » de l'écran vide, quand la liste n'a encore rien.

**Le serveur aussi**, demandé ensuite : « on ne voit jamais le nom de
l'app dans la barre à cause de la place occupée par les icônes ». L'icône
ne montrait aucun état, elle ouvrait l'écran du serveur, dont on se sert
pour s'apparier puis presque plus ; l'état est déjà dans le sous-titre de
la racine (le nom du serveur, « injoignable »). Elle devient « Serveur… »,
en tête du menu « … ». La barre ne garde que la loupe et les drapeaux.

Version 1.3.0.4, code 37, en essai, puis 1.3.0.5, code 38, avec le
serveur ; publiée en 1.3.1 (voir 032 pour le code).

## Fichiers

`res/menu/principal.xml`, `MainActivity.kt`, `app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.5.

- La barre : la loupe, les drapeaux, « … » ; le nom de l'application se
  lit en entier.
- « … », « Serveur… » en premier : l'écran du serveur s'ouvre ; dans
  l'onglet Podcasts aussi.

- Onglet Radio : plus d'icône fichier dans la barre ; « … », « Importer
  une liste… » ouvre le sélecteur de fichiers.
- Liste vide : le bouton « Importer une liste… » est toujours là.
