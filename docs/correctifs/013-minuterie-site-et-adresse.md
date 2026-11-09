# 013 — La minuterie d'extinction, le site, l'adresse du flux

Le 2026-11-09, dans le bloc de sept. Trois choses que le bureau fait depuis
longtemps et que le téléphone n'avait pas, relevées en regardant « ce qui
se fait dans les options dans l'app radio ».

## La minuterie

Une icône dans la ligne de lecture, à gauche de lecture/pause : un menu de
durées, 15, 30, 45, 60, 90 minutes, les mêmes que le bureau, et « Annuler
la minuterie » quand elle court. À l'échéance, la lecture se met en pause,
station ou épisode. L'icône se colore et les minutes qui restent
s'affichent dessous, relues toutes les trente secondes ; une au moins tant
que c'est programmé.

`lecture/Extinction.kt` : un objet partagé par l'écran et le service, qui
vivent dans le même processus. L'écran programme, le service pose le geste
de pause à sa création et retire tout à sa destruction. Le `Handler` du
fil principal porte l'échéance ; le service en avant-plan, avec son verrou
réseau, garde le processus éveillé tant que ça joue. Une seule minuterie,
la dernière programmée gagne.

## Le site et l'adresse

Dans le menu d'appui long d'une station, après le rangement et l'ordre :
**« Ouvrir le site de la station »**, quand la fiche en a un, dans le
navigateur ; **« Copier l'adresse du flux »**, dans le presse-papiers,
pour l'écouter ailleurs ou la donner. Depuis Android 13, le système
confirme la copie lui-même ; avant, un mot le dit.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/lecture/Extinction.kt`
(nouveau), `lecture/ServiceLecture.kt`, `MainActivity.kt`,
`res/layout/activity_main.xml`, `res/drawable/ic_minuterie.xml`
(nouveau), `res/values*/strings.xml`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.1.0-debug.apk` par-dessus.

- Une station joue : l'icône de minuterie à gauche de pause. Appuyer,
  « 15 min » : « Arrêt dans 15 min », l'icône se colore, « 15 min »
  dessous ; au bout d'une minute, « 14 min ».
- Écran éteint, quinze minutes plus tard : le son s'arrête, la
  notification montre la lecture en pause.
- Appuyer encore : « Annuler la minuterie » en dernier ; l'icône redevient
  grise.
- Appui long sur une station qui a un site : « Ouvrir le site de la
  station » ouvre le navigateur. « Copier l'adresse du flux » : collée
  dans une note, c'est l'adresse du flux.
