# 029 — La vitesse des épisodes, reculer 15 s, avancer 30 s

Le 2026-11-11. Quatrième des chantiers de la feuille de route : « la
vitesse de lecture des podcasts, et les sauts de quinze et trente
secondes ». Les sauts existaient, de 15 secondes dans les deux sens.

## Ce qui change

**La vitesse.** Sur la ligne d'avancement d'un épisode, entre le temps
restant et « avancer », un bouton « 1× » ; un appui ouvre le choix 0,75×,
1×, 1,25×, 1,5×, 1,75×, 2×. Le lecteur garde la hauteur de la voix.

- Une seule vitesse pour tous les épisodes, gardée d'un lancement à
  l'autre (`lecture/Vitesse.kt`, préférences « lecture »).
- Le service l'applique à chaque changement d'élément : la vitesse
  choisie pour un épisode, 1× pour une station. L'écran l'applique
  aussi tout de suite à l'épisode en cours.
- Le nombre suit la langue : « 1,25× » en français, « 1.25× » en anglais.

**Les sauts** : reculer de 15 secondes, avancer de 30, comme la plupart
des applications de podcasts. Les deux icônes portent « 15 » et « 30 »
au centre de leur flèche.

Version 1.3.0.1, code 34, en essai, puis 1.3.0.2, code 35, avec le
droit d'auteur sur sa propre ligne (028) ; publiée en 1.3.1 (voir 030
pour le code).

## Fichiers

`lecture/Vitesse.kt` (nouveau), `lecture/ServiceLecture.kt`,
`MainActivity.kt`, `res/layout/activity_main.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.2.

- Lancer un épisode : « 1× » sur la ligne d'avancement, les icônes
  portent « 15 » et « 30 ».
- « 1× », choisir 1,5× : l'épisode accélère tout de suite, la voix garde
  sa hauteur, le bouton dit « 1,5× ».
- Passer à l'épisode suivant : toujours 1,5×.
- Lancer une station : vitesse normale. Revenir à un épisode : 1,5×.
- Fermer et relancer l'application, lancer un épisode : 1,5×.
- Avancer : +30 s sur le temps restant ; reculer : -15 s.
