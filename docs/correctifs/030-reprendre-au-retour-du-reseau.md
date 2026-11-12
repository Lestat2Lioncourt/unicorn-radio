# 030 — Reprendre au retour du réseau

Le 2026-11-11. Dernier des chantiers de la feuille de route : « une
lecture tombée sur une coupure repart quand le réseau revient, sans
appui ». Un tunnel, un ascenseur, le wifi de la maison quitté avant que
les données mobiles prennent le relais : la lecture s'arrêtait en erreur
et attendait un appui.

## Ce qui change

**L'erreur de réseau est notée.** Connexion impossible, délai dépassé,
entrée-sortie sans détail : le service note l'heure de la chute.

**Hors ligne, l'annuaire n'est plus consulté.** Le 021 redemande la
fiche d'une station qui ne démarre pas ; sans réseau, il aurait répondu
« annuaire injoignable » pour rien. Il reste consulté quand le réseau est
là.

**Le retour du réseau** est le moment où Android le déclare vérifié
(`NET_CAPABILITY_VALIDATED`), pas seulement branché : `Reseau.surRetour`.
La lecture repart alors (`prepare`, `play`), un épisode à sa position,
avec le message « Réseau revenu : « … » reprend », si :

- le lecteur est toujours en erreur de réseau ;
- la lecture est toujours voulue : ni pause ni arrêt depuis ;
- la chute date de moins de trente minutes, pour ne pas surprendre une
  heure plus tard.

## Fichiers

`serveur/Reseau.kt`, `lecture/ServiceLecture.kt`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

Version 1.3.0.3, code 36, en essai ; publiée en 1.3.1 (voir 031 pour le
code).

## Vérification

À relancer : installer la 1.3.0.3.

- Une station en lecture, mode avion : après quelques secondes, l'erreur.
  Quitter le mode avion : la station repart seule, le message s'affiche.
- Même chose avec un épisode : il reprend là où il s'était arrêté.
- Mode avion, attendre l'erreur, appuyer sur pause, quitter le mode
  avion : rien ne repart.
- Changer de wifi, ou passer du wifi aux données, pendant la lecture :
  pas de coupure, ou une reprise.
- En mode avion, aucun message « annuaire injoignable ».
