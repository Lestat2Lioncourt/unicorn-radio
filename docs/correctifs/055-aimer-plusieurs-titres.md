# 055 — Aimer plusieurs titres de « Précédemment »

Le 2026-11-21. « Quand on regarde la liste des morceaux précédemment
joués, il serait pratique de pouvoir rajouter un titre dans la liste des
titres aimés en maintenant appuyé dessus. » Le geste existait déjà, en
appui simple, mais rien ne le disait, et la fenêtre se fermait à chaque
titre aimé : pour en aimer trois, il fallait la rouvrir trois fois.

## Ce qui change

- Toucher un titre l'aime, ou ne l'aime plus, et la fenêtre reste : le
  cœur apparaît ou disparaît sur la ligne. On ferme par OK, le retour,
  ou un appui hors de la fenêtre.
- Sous le titre de la fenêtre : « Touchez un titre pour l'aimer, ou ne
  plus l'aimer. »
- L'appui simple est gardé, pas un appui long : on ne le devinerait pas
  davantage.

Version 1.7.0.8, code 84, en essai.

## Fichiers

`MainActivity.kt` (`montrerLesPrecedents`), `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.7.0.8-debug.apk`.

- Une station qui annonce ses titres, quelques morceaux passés :
  ouvrir « Précédemment ». La phrase sous le titre.
- Toucher deux titres : le cœur apparaît sur chacun, la fenêtre reste.
- Retoucher l'un : le cœur s'en va.
- OK : la fenêtre se ferme ; « Titres aimés » montre le titre gardé.
