# 056 — Les noms de stations en entier

Le 2026-11-22. « Quand on lance une recherche, beaucoup de radios ont un
nom tronqué dans le résultat, surtout celles qui dépendent d'une
franchise, par exemple Radio Caprice, qui prend déjà pas mal de place, et
qui est suivi d'un nom de style. »

## Le défaut

Lu dans `ligne_station.xml` : le nom d'une station tenait sur une seule
ligne, coupé par « … » au-delà. Dans les résultats de recherche, l'étoile
des favoris réduit encore la place.

## Ce qui change

- Le nom passe sur deux lignes s'il le faut ; « … » seulement au-delà.
  Un nom court ne change rien. La ligne de détail (pays, style) reste sur
  une ligne.
- Le même modèle de ligne sert aux favoris et aux dossiers : ils en
  profitent aussi.

Version 1.8.0, code 86, en essai.

## Fichiers

`res/layout/ligne_station.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.8.0-debug.apk`.

- Chercher « Radio Caprice » : les noms longs sur deux lignes, entiers.
- Un nom court : la ligne garde sa hauteur.
- Favoris et dossiers : même chose, rien ne déborde sur l'étoile ou le
  logo.
