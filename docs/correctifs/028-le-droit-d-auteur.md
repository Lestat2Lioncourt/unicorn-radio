# 028 — Le droit d'auteur

Le 2026-11-11. « C'est pertinent de rajouter un (c) Lestat2Lioncourt, ou
ça ne se fait pas avec le type de licence choisi ? »

## Ce qui change

La GPL ne retire rien du droit d'auteur : l'auteur le garde et accorde à
tous le droit d'utiliser, de modifier et de redistribuer sous la même
licence. Sa notice d'application recommande même la mention
« Copyright (C) année nom ». Le projet n'en avait aucune, le seul
copyright du dépôt étant celui de la Free Software Foundation dans le
texte de la licence.

- **« À propos »** : « Logiciel libre, licence GPL-3.0. », puis à la
  ligne « © 2026 Lestat2Lioncourt », pour qu'elle ne soit pas coupée ; et
  en anglais.
- **README**, section Licence, en anglais et en français : « © 2026
  Lestat2Lioncourt », et « GPL v3.0 only », la licence exacte.

Pas de notice en tête de chaque fichier de code : permise, pas exigée.

Version 1.3.0, code 33, en essai ; publiée en 1.3.1 (voir 029 pour le
code).

## Fichiers

`res/values/strings.xml`, `res/values-fr/strings.xml`, `README.md`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.

- Menu « … », « À propos » : « © 2026 Lestat2Lioncourt » après la
  licence, en français et en anglais.
- Sur GitHub, après le push : la section Licence du README.
