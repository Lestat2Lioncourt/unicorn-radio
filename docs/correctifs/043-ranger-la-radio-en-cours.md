# 043 — Ranger la radio en cours d'écoute

Le 2026-11-14. « Pour une radio en cours d'écoute, l'appui long sur la
radio en cours d'écoute devrait aussi permettre de l'inscrire dans une
catégorie. »

## Ce qui n'allait pas

La ligne de lecture ne réagissait qu'à l'appui court (« Précédemment »).
Pour ranger la station qu'on écoute, il fallait la retrouver dans une
liste ou dans la recherche.

## Ce qui change

- **Un appui long sur les textes de la ligne de lecture**, quand c'est une
  station, ouvre le menu de la station, celui de l'appui long dans les
  listes : ajouter aux favoris ou les retirer, « Ranger dans… », le site,
  copier l'adresse du flux.
- **Sans ce qui dépend du dossier affiché** : ni « Déplacer vers… », ni
  « Retirer du dossier », ni monter et descendre, qui n'auraient pas de
  sens depuis la ligne de lecture.
- **La fiche de la station** vient de ma liste si elle y est, sinon des
  résultats de la recherche, sinon de ce que le lecteur jouait (nom,
  adresse, logo, pays).
- Pour un épisode de podcast, l'appui long ne fait rien.

Version 1.4.0.3, code 60, en essai.

## Fichiers

`MainActivity.kt`, `lecture/DerniereEcoute.kt` (la station gardée,
lisible à part).

## Vérification

À relancer : installer `pkg/unicorn-radio-1.4.0.3-debug.apk`.

- Écouter une station trouvée par la recherche, qui n'est dans aucun
  dossier : appui long sur son nom dans la ligne de lecture, le menu
  s'ouvre à son nom.
- « Ranger dans… », choisir un dossier : « rangée dans … » ; le dossier
  la contient.
- Rouvrir le menu : ce dossier est grisé dans « Ranger dans… ».
- « Nouveau dossier » depuis ce menu : le dossier est créé avec la station.
- Ouvrir un dossier, écouter une station d'ailleurs, appui long sur la
  ligne de lecture : pas de « Déplacer vers… » ni de « Retirer du
  dossier ».
- L'appui court ouvre toujours « Précédemment ».
- Pendant un épisode de podcast : l'appui long ne fait rien, l'appui
  court ouvre la liste de lecture.
