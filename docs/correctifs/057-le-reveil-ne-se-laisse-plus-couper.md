# 057 — Le réveil ne se laisse plus couper par le casque

Le 2026-11-27. « Je suis en train de regarder Netflix sur l'ordi, avec mon
casque Bluetooth. Puis le réveil Unicorn-Radio se met en route sur une
radio mais je n'entends rien. La raison : mon casque est aussi défini sur
le tel, et le Bluetooth met automatiquement en pause le device qui n'est
pas celui en cours d'écoute. »

## Le défaut

Lu dans le code (`ServiceLecture.finDuReveil`) : toute pause pendant le
réveil le finissait, comme si on l'avait arrêté. Or la pause ne venait pas
de l'utilisateur. Un casque relié à deux appareils reste sur celui qui
joue déjà : quand le téléphone se met à jouer, Android voit le casque
« débranché » et met la lecture en pause, ou le casque lui envoie
lui-même une pause. Le réveil s'arrêtait sans un mot, haut-parleur
compris : on n'entendait rien.

## Ce qui change

- Pendant le réveil, une pause faite par Android (casque débranché ou
  parti vers un autre appareil, son pris par une autre application) est
  défaite : la lecture repart aussitôt, par le haut-parleur comme toute
  alarme. Au plus cinq fois par réveil, pour ne pas lutter sans fin.
- Pendant le réveil, la pause des boutons du casque est ignorée. Le
  réveil s'arrête par l'écran du réveil ou la notification : Écouter,
  Arrêter, +1 min, +10 min.
- « Arrêter » de la notification d'avant l'heure passe par le service et
  non plus par un faux bouton du casque, qui serait ignoré.
- Chaque pause ignorée laisse une ligne dans le journal, avec sa cause.
- Hors réveil, rien ne change : le casque débranché met en pause, ses
  boutons commandent la lecture.

Version 1.9.0, code 88, en essai.

## Fichiers

`lecture/ServiceLecture.kt`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.9.0-debug.apk`.

- Casque Bluetooth relié au téléphone et à l'ordinateur, une vidéo qui
  joue sur l'ordinateur ; compte à rebours de 3 min sur une station. À
  l'heure, le haut-parleur du téléphone sonne, et le réveil continue.
- Même chose, le bouton pause du casque : le réveil continue ; « Arrêter »
  sur l'écran ou la notification l'arrête.
- Hors réveil, une radio au casque : le bouton pause du casque met en
  pause, couper le casque met en pause.
- Dans le journal, après le premier essai : la ligne « pause d'Android
  ignorée » ou « pause du casque ignorée », qui dit d'où venait la pause.
