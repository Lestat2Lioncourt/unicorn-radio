# 014 — Précédemment : les titres passés d'une station

Le 2026-11-09, dans le bloc de sept. Sur le bureau, sous le titre en cours,
« Précédemment » liste les morceaux d'avant ; le téléphone, depuis le 011,
montre le titre en cours et rien d'autre.

## Ce qui est fait

Un appui sur les textes de la ligne de lecture, pendant qu'une station
joue : une boîte « Précédemment · Station », une ligne par titre, l'heure
puis le titre, le plus récent en haut. Vide, elle le dit : les titres
s'empilent au fil de l'écoute.

**Le téléphone note lui-même** (`lecture/Precedents.kt`,
`precedents.json`) : à chaque titre que le flux annonce, l'écouteur ICY du
service (`ServiceLecture.titres`) l'ajoute en tête des précédents de la
station, vingt par station, sauf si c'est le nom de la station lui-même,
qu'elle écrit souvent entre deux morceaux, comme le bureau le filtre. Le
titre en cours n'est pas dans la liste, il est déjà sous le nom.

**Apparié**, ce que le serveur de la maison a vu s'y ajoute : la route
`GET /api/radio/titre/:uuid`, ouverte au jeton comme toutes celles de la
radio, rend le titre et les précédents que son relais garde quand le PC
écoute la même station ; rien sinon. Trois secondes au plus, sur la
dernière adresse qui a répondu, sans chercher le serveur : c'est un
complément, pas une attente. Les deux listes sont réunies par titre, à la
date la plus récente.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/lecture/Precedents.kt`
(nouveau), `lecture/ServiceLecture.kt`, `serveur/ClientServeur.kt`
(`titres`), `MainActivity.kt`, `res/layout/activity_main.xml`,
`res/values*/strings.xml`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.1.0-debug.apk` par-dessus.

- OÜI FM Alternatif, quelques morceaux : appuyer sur le nom en bas,
  « Précédemment · OÜI FM Alternatif », les titres passés avec leur heure,
  le titre en cours absent.
- Une station qui n'annonce rien : « Rien encore… ».
- Apparié, la même station sur le PC et le téléphone : les titres vus par
  le PC avant qu'on lance le téléphone sont là aussi.
- Fermer l'application, la rouvrir, la même station : les précédents sont
  gardés.
