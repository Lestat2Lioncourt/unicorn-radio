# 042 — Sélectionner dans la liste de lecture

Le 2026-11-13. « Quand on est dans la liste de lecture des podcasts, un
menu s'ouvre à l'appui long sur un des podcasts. Je pense qu'il manque
l'option sélectionner, si on veut sélectionner plusieurs podcasts et les
supprimer ensuite, voire une option vider la liste dans ce menu de
sélection. »

## Ce qui n'allait pas

Retirer des épisodes de la liste de lecture se faisait un par un, par
l'appui long. « Vider la liste » existait, mais seulement dans le menu
« … » de la liste ouverte.

## Ce qui change

- **« Sélectionner »** dans le menu de l'appui long, dans la liste de
  lecture seulement : l'épisode est coché, la liste passe en mode
  sélection.
- **En mode sélection**, un appui, court ou long, coche ou décoche ; un
  épisode coché est surligné, une coche à la place de sa vignette.
- **La barre** dit « N sélectionnés » et n'a plus que trois actions :
  « Retirer » (les cochés quittent la liste, et la lecture s'arrête si
  l'un d'eux joue), « Tout sélectionner » et « Vider la liste », dans
  le « … ».
- **La flèche de retour** et le retour du téléphone sortent du mode
  sans rien changer ; « Retirer » et « Vider la liste » en sortent aussi.

Version 1.4.0.2, code 59, en essai.

## Fichiers

`MainActivity.kt`, `ListeAdaptateur.kt`, `res/menu/principal.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.4.0.2-debug.apk`.

- Liste de lecture avec plusieurs épisodes : appui long sur l'un,
  « Sélectionner » ; il est coché, la barre dit « 1 sélectionné ».
- Appuyer sur d'autres : ils se cochent ; appuyer à nouveau : décochés.
  Aucun ne se lance.
- « Retirer » : « N épisodes retirés de la liste », le mode se ferme.
- Cocher l'épisode en cours, « Retirer » : la lecture s'arrête.
- « … », « Tout sélectionner » : tout est coché.
- « … », « Vider la liste » : la confirmation, puis la liste vide.
- Retour du téléphone en mode sélection : le mode se ferme, rien n'est
  retiré ; un second retour ferme la liste comme avant.
- Ailleurs (mes podcasts, un podcast ouvert, l'onglet Radio) : le menu
  « … » est celui d'avant.
