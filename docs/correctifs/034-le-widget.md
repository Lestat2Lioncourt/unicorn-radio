# 034 — Le widget

Le 2026-11-12. De la liste « à voir » : « un widget ».

## Ce qui change

**Un widget pour l'écran d'accueil**, quatre cases sur une,
élargissable : le nom de ce qui joue, ou de la dernière écoute (033), le
titre en cours dessous, et précédent, lecture ou pause, suivant, arrêter. Un fond
sombre un peu transparent, lisible sur tous les fonds d'écran.

- **Les boutons** envoient des touches multimédia, comme un casque, au
  `MediaButtonReceiver` de Media3 : application fermée, « lecture »
  réveille le service et reprend la dernière écoute ; « précédent » et
  « suivant » n'agissent que pendant une écoute.
- **Arrêter** (une croix, ajoutée à l'essai : « impossible de le fermer si
  on a fermé l'app, on peut juste passer en pause ») : la touche « stop »,
  la lecture s'arrête, la notification s'en va ; le widget garde la
  station, « lecture » la relance.
- **Un appui hors des boutons** ouvre l'application : le fond, la zone
  des textes et chaque texte portent l'intention, certains écrans
  d'accueil ne transmettant l'appui qu'à la vue touchée ; à l'essai de la
  1.3.0.14, un appui sur le nom n'ouvrait rien.
- **Le service le redessine** à chaque changement : élément, titre,
  lecture ou pause ; à son arrêt, la dernière écoute, en pause. Aucune
  mise à jour à intervalles.

Version 1.3.0.11, code 44, en essai, avec la dernière écoute (033), puis
1.3.0.13, code 46, avec « arrêter », puis 1.3.0.15, code 48, l'appui
qui ouvre l'application étendu à tout le widget.

## Fichiers

`lecture/Widget.kt` (nouveau), `res/layout/widget.xml`,
`res/drawable/widget_fond.xml`, `res/xml/widget_info.xml` (nouveaux),
`lecture/ServiceLecture.kt`, `AndroidManifest.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`.

## Vérification

À relancer : installer la 1.3.0.15.

- Appui long sur l'écran d'accueil, Widgets, Unicorn-Radio : le poser.
  Il montre la dernière écoute, ou « rien écouté encore ».
- Lecture depuis le widget, application fermée : la dernière écoute
  joue, le bouton devient pause, la notification apparaît.
- Une station qui annonce ses titres : le titre change sur le widget.
- Pause depuis la notification : le widget suit.
- Suivant dans une liste de stations ou d'épisodes : le widget suit.
- Un appui sur le nom : l'application s'ouvre.
- La croix pendant une écoute, application fermée : le son s'arrête, la
  notification disparaît.
