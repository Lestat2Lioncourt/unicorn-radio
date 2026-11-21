# 054 — L'écran du réveil, et la sonnerie par-dessus une écoute

Le 2026-11-19. « Quand l'alarme ou le compte à rebours sonne, à l'heure
prévue, pas à l'heure − 2 min, est-ce qu'on pourrait allumer le tel comme
le ferait une alarme classique, avec l'app en mode TV et des boutons
+1 min, +10 min ? » Et : « il me semble avoir constaté une confusion
lorsqu'une alarme s'est lancée alors que j'étais déjà en train d'écouter
une radio. » Puis : « si une radio est déjà en écoute, alors l'alarme se
lance en mode sonnerie, pour éviter de la louper. »

## Le défaut

Lu dans le code (`ServiceLecture.sonner`) : une alarme qui part pendant
une écoute prenait le lecteur deux minutes avant l'heure, en muet.
L'écoute se taisait sans raison visible, puis une autre station arrivait,
qu'on pouvait prendre pour la suite de l'écoute ; la file du dossier était
perdue (plus de suivante ni de précédente) ; la minuterie d'extinction
était remplacée par celle d'une heure du réveil, qui restait après lui.

## Ce qui change

- **L'écran du réveil** (`EcranReveil`). À l'heure dite, une notification
  d'alarme allume l'écran, par-dessus le verrouillage : le mode télé
  (spectre, heure, logo, station, titre) et trois boutons.
  - « Arrêter » : le réveil s'arrête.
  - « +1 min », « +10 min » : le son se coupe, la station continue en
    muet, l'écran se ferme ; au bout du temps, la montée du son reprend et
    l'écran revient. Mettre en pause entre-temps annule le réveil. (Pour la
    sonnerie par-dessus une écoute, voir plus bas.)
  - Le retour referme l'écran : une station continue, en écoute
    ordinaire ; une sonnerie s'arrête.
  - Toucher l'écran ne fait rien : un geste ensommeillé ne coupe pas le
    réveil.
  - L'écran se ferme de lui-même quand le réveil finit, d'où qu'il
    finisse.
  - Téléphone en main, ou plein écran refusé par Android : la
    notification reste, avec les mêmes boutons ; un appui sur elle ouvre
    l'application, sur ce qui joue.
- **Le plein écran se permet** depuis Android 14 ; une application
  installée à la main ne l'a pas toujours. À l'ouverture de la liste des
  réveils ou des comptes, s'il manque, une fenêtre l'explique, une fois,
  avec le chemin du réglage.
- **Une alarme pendant une écoute** (station ou épisode, hors réveil) :
  rien ne change avant l'heure dite. À l'heure dite, l'écoute se met en
  pause, la sonnerie du téléphone part au plein (celle de l'alarme si elle
  en a une, sinon celle du réveil d'Android, renforcée), l'écran
  s'allume. « Arrêter » : l'écoute reprend, sa file et sa minuterie
  intactes. Un report : l'écoute reste en pause, comme le silence d'un
  réveil, et le report sonne avec la station de l'alarme (choix de
  l'utilisateur, 1.7.0.8, code 84 ; avant, l'écoute reprenait et le report
  sonnait de nouveau par-dessus, ce qui embrouillait). Relancer la lecture à la main arrête aussi la
  sonnerie. Sans réponse, elle s'arrête au bout de dix minutes et
  l'écoute reprend.
- La minuterie d'une heure posée par un réveil s'en va avec lui.

Version 1.7.0.2, code 78, en essai ; corrigée en 1.7.0.3, code 79.

## Le premier essai

En 1.7.0.2, l'écran du réveil plantait dès son ouverture : son thème,
celui d'Android en noir, n'avait pas les couleurs de l'application, que le
spectre lit (`IllegalArgumentException : VueSpectre requires a value for
the colorPrimary attribute`, trace relevée par l'application, Android 16,
Xiaomi). À l'heure d'un compte à rebours, téléphone verrouillé, l'écran
s'ouvrait, plantait, et la lecture mourait avec lui : rien ne sonnait. Un
appui sur la notification, qui ouvrait le même écran, plantait aussi.

- L'écran prend le thème de l'application, fond noir, plein écran
  (`Theme.UnicornRadio.Reveil`).
- Un appui sur la notification ouvre l'application, comme l'attendait
  l'utilisateur ; l'écran mode télé ne vient que de lui-même, téléphone
  verrouillé.
- En 1.7.0.3, l'écran s'ouvrait en portrait : il passe en paysage, comme
  le mode télé (1.7.0.4, code 80).
- En portrait malgré tout (sur l'écran de verrouillage, le téléphone
  peut l'imposer), les trois boutons en ligne manquaient de place,
  « +10 min » écrasé, la rangée par-dessus l'heure : en portrait, ils
  s'empilent, de même largeur, sous l'heure (1.7.0.5, code 81).
- « +1 min » sur une alarme radio, rien en écoute avant : la sonnerie du
  téléphone au lieu de la radio, reproduit deux fois par l'utilisateur. Le
  report mettait la station en pause et programmait un compte à rebours
  qui la relançait ; la cause exacte n'a pas été lue dans le journal.
  L'utilisateur : « le +1 ne devrait pas faire redémarrer la station, mais
  simplement la mettre en mute et rejouer la montée de volume après
  1 minute. » C'est fait : plus de reconnexion, plus de compte à rebours
  pour ce cas (1.7.0.6, code 82). La sonnerie par-dessus une écoute garde
  le compte à rebours sans nom.
- « Écouter » (1.7.0.7, code 83). Au casque Bluetooth, le réveil sonnait
  dans le casque et dans le haut-parleur du téléphone : Android fait sortir
  le son d'une alarme par le haut-parleur, casque branché, pour qu'on
  l'entende. « Comment stopper le son du tel si je veux uniquement garder
  le casque ? » Un bouton « Écouter », quand une station sonne (pas la
  sonnerie) : le réveil finit, la station continue en écoute ordinaire,
  au casque seul, au volume de la musique. Le retour faisait déjà la même
  chose, sans qu'on le devine. Sur l'écran : Écouter, Arrêter, +1 min,
  +10 min. La notification n'a que trois boutons : avec une station,
  Écouter, Arrêter, +10 min ; avec la sonnerie, Arrêter, +1 min,
  +10 min.

## Fichiers

`EcranReveil.kt` (nouveau), `lecture/ServiceLecture.kt`, `ModeTele.kt`
(la vue partagée), `MainActivity.kt`, `AndroidManifest.xml`
(`USE_FULL_SCREEN_INTENT`, l'activité), `res/values/strings.xml`,
`res/values-fr/strings.xml`, `res/values/themes.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.7.0.8-debug.apk`, ouvrir une
fois la liste des réveils (la demande de plein écran, s'il manque).

- Compte à rebours de 3 min sur une station, rien en écoute, téléphone
  verrouillé écran éteint : à l'heure, l'écran s'allume sur le mode télé,
  le son monte. « +1 min » : silence, la radio continue en muet ; une
  minute plus tard, le son remonte et l'écran revient.
- Même chose, « Arrêter » : la radio s'arrête, l'écran se ferme.
- Même chose, retour : la radio continue, l'écran se ferme.
- Casque Bluetooth branché, alarme sur une station : le son dans le
  casque et le haut-parleur. « Écouter » : le haut-parleur se tait, la
  radio continue dans le casque.
- Une radio d'un dossier en écoute, compte à rebours de 3 min : elle
  continue jusqu'à l'heure ; à l'heure, pause, sonnerie au plein, écran.
  « Arrêter » : la radio reprend ; suivante et précédente marchent.
- Minuterie d'extinction de 30 min réglée avant : toujours là après la
  sonnerie par-dessus.
- Même chose, « +1 min » : la sonnerie s'arrête, la radio reste en
  pause ; une minute plus tard, la station de l'alarme monte, l'écran
  revient.
