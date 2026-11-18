# 051 — Les stations d'un dossier en boucle

Le 2026-11-18. « Quand on écoute une radio dans un groupe, et qu'on fait
radio suivante, on bute si on est à la fin de la liste des radios du
groupe, on devrait revenir sur la première. Et inversement si on fait
chaîne précédente. »

## Ce qui n'allait pas

La liste où l'on appuie devient la file du lecteur, sans répétition :
sur la dernière station, « suivante » n'avait plus rien devant elle et
le bouton était grisé ; de même « précédente » sur la première.

## Ce qui change

- **Une liste de stations tourne en boucle** : « suivante » sur la
  dernière revient à la première, « précédente » sur la première va à la
  dernière. Les boutons ne sont plus grisés aux bouts.
- Partout où l'on commande le lecteur : la ligne de lecture, la
  notification, le widget, le casque.
- **Pas les épisodes** : la file d'écoute ne recommence pas une fois
  finie. **Pas une station seule.** Le réveil garde sa propre règle.
- Le service règle la boucle à chaque changement de file ou de station,
  selon ce qui joue : une station d'une liste, un épisode.

Version 1.6.0.2, code 74, en essai.

## Fichiers

`lecture/ServiceLecture.kt`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.6.0.2-debug.apk`.

- Dans un dossier, lancer la dernière station, « suivante » : la
  première du dossier. Depuis la première, « précédente » : la dernière.
- Même chose depuis la notification ou le casque.
- Un podcast, dernier épisode de la file : « suivant » reste grisé.
- Une station seule (la dernière écoute remise à l'ouverture) : les
  boutons restent grisés.
