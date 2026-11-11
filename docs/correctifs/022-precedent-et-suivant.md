# 022 — Précédent et suivant

Le 2026-11-11. « Si on écoute un podcast, les boutons précédent suivant
pour passer au suivant précédent dans la liste de lecture. » Pendant un
épisode, le lecteur ne portait que l'épisode en cours et ceux à suivre :
« suivant » marchait, « précédent » n'avait rien derrière lui. Et la ligne
de lecture de l'écran n'avait que le bouton lecture.

## Ce qui change

**Le lecteur porte toute la liste** pendant un épisode : les passés
devant, l'épisode lancé, ceux à suivre derrière (`jouerLEpisode`).
Précédent et suivant passent donc d'un épisode à l'autre depuis l'écran,
la notification et le casque.

**La liste gardée suit le lecteur** : à chaque changement d'épisode, le
service la réécrit dans l'ordre du lecteur (`FileDEcoute.suivreLeLecteur`,
qui remplace `noterLeDepart`). Ce qui précède l'épisode en cours est
« passés », lui compris à la fin ; ce qui suit est « à suivre ». Reculer
remet donc l'épisode qu'on quitte en tête d'« à suivre ».

**Repris où on l'avait laissé** : un épisode atteint par précédent,
suivant ou à la fin du précédent reprend à sa position gardée, s'il n'est
pas terminé.

**Deux boutons sur la ligne de lecture**, de part et d'autre de la
lecture, pour les épisodes comme pour les stations d'une liste ; grisés
quand il n'y a rien derrière ou devant.

Dans la notification, « précédent » garde la règle d'Android : au-delà
des trois premières secondes, il revient au début de l'épisode, un second
appui passe au précédent. Le bouton de l'écran passe toujours à
l'épisode d'avant.

Version 1.1.4, code 24, avec le 021.

## Fichiers

`podcasts/FileDEcoute.kt`, `lecture/ServiceLecture.kt`, `MainActivity.kt`,
`res/layout/activity_main.xml`, `res/drawable/ic_precedent.xml` et
`ic_suivant.xml` (nouveaux), `res/values/strings.xml`,
`res/values-fr/strings.xml`.

## Vérification

À relancer : installer la 1.1.4.

- Une liste de lecture de trois épisodes, lancer celui du milieu :
  précédent est actif, suivant aussi ; « précédent » joue le premier à sa
  position gardée, et la liste montre l'épisode quitté en tête d'« à
  suivre ».
- « Suivant » jusqu'au dernier : le bouton se grise.
- Depuis la notification et le casque : même chose, sauf la règle des
  trois secondes.
- Une station lancée depuis un dossier : précédent et suivant passent à
  la station d'avant ou d'après ; depuis une recherche, pareil dans les
  résultats.
- Retirer un épisode de la liste pendant la lecture d'un autre : il
  disparaît de la liste et du lecteur.
