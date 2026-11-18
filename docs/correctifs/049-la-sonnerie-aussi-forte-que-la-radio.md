# 049 — La sonnerie aussi forte que la radio

Le 2026-11-17. « J'avais mis un radio réveil mais la radio n'était pas
joignable donc bascule sur la sonnerie ok. Cependant le volume progressif
de la sonnerie tel ne monte pas aussi haut que celui avec la musique. »

## Ce qui n'allait pas

Le code traite les deux de la même façon : la station et la sonnerie de
secours passent par le même lecteur, sur le volume du réveil d'Android,
avec la même montée jusqu'au plein. L'écart est dans le son : une radio
est diffusée très compressée, à un niveau élevé ; une sonnerie d'Android
est enregistrée bien plus bas. Au même volume, elle sonnait moins fort.

La radio ne répondait pas ce matin-là parce que le VPN du téléphone
tournait en boucle de reconnexion ; l'application n'y est pour rien et
n'en a pas le moyen : elle ne touche pas au VPN, elle écoute seulement
les changements de réseau.

## Ce qui change

- **Le renfort de la sonnerie** : pendant que la sonnerie du téléphone
  joue, et seulement là, le renfort d'Android (`LoudnessEnhancer`) est
  posé sur la session audio du lecteur, à +10 dB ; son limiteur évite la
  saturation. Il est retiré dès que la sonnerie s'arrête.
- La montée progressive ne change pas : une minute, presque rien au
  début, plein à la fin, qui finit maintenant au niveau d'une radio.
- Vaut pour la sonnerie de secours, quand la station ne joue pas, et pour
  une sonnerie choisie comme son d'une alarme.

Version 1.6.0, code 72, en essai. Les +10 dB sont à ajuster à l'oreille.

## Fichiers

`lecture/ServiceLecture.kt`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.6.0-debug.apk`.

- Une alarme dans deux minutes, son « une sonnerie du téléphone »,
  volume progressif : la montée, puis au bout d'une minute un niveau
  comparable à une radio sur le même volume du réveil, sans grésillement.
- Une alarme sur une station, mode avion activé une minute avant : la
  sonnerie de secours, renforcée de même.
- Arrêter la sonnerie puis lancer une radio : son volume habituel, pas
  renforcé.
