# 035 — Le réveil radio

Le 2026-11-12. De la liste « à voir » : « le réveil radio », demandé
après la dernière écoute et le widget.

## Ce qui change

**« Réveil radio… »** dans le menu « … » : ce qu'il fait, son état
(« Réglé : 07:00 · lun. mar. mer. jeu. ven. · FIP », ou « arrêté »),
« Régler… » et, s'il est réglé, « Arrêter ». Régler, c'est trois pas,
le réglage d'avant proposé à chacun :

1. l'heure, sur des roues qu'on fait défiler, heures et minutes (et matin,
   après-midi en 12 h, selon le téléphone) ; un appui sur un nombre le
   fait taper au clavier. D'abord l'horloge de Material, cadran ou
   clavier : « on pourrait pouvoir scroller » ;
2. les jours, du lundi au dimanche, du lundi au vendredi la première
   fois ; aucun jour, une seule fois ;
3. la station, parmi les miennes, favoris puis dossiers.

« Réveil programmé : jeudi 13 novembre, 07:00 ».

**Programmé comme un réveil** (`AlarmManager.setAlarmClock`) : Android
l'annonce dans la barre d'état, le déclenche à l'heure même en veille,
et laisse le service de lecture démarrer depuis l'arrière-plan.
Reprogrammé au démarrage du téléphone, après une mise à jour de
l'application et quand l'heure change (`Reveil.Recepteur`). Les
permissions : `USE_EXACT_ALARM` (Android 13 et après, accordée d'office à
un réveil) et `SCHEDULE_EXACT_ALARM` pour Android 12 ; si Android la
refuse, « Régler… » ouvre le réglage « Alarmes et rappels ».

**Une minute avant l'heure** (`ServiceLecture.sonner`) :

- la station démarre **en muet**, sur le volume du réveil d'Android
  (`USAGE_ALARM`), qui sonne même téléphone en silencieux : la publicité
  du début de flux passe sans être entendue ;
- **à l'heure dite** (`aLHeureDite`), le son monte du silence au plein en
  une minute, recalculé chaque seconde d'après le temps écoulé, au cube
  pour l'oreille : presque rien d'abord, puis vite plus fort ;
- **si la station ne joue pas à l'heure dite** (pas de réseau, station
  muette) : la sonnerie de réveil du téléphone, en boucle, tout de suite ;
  la minute d'avance a servi à la charger ;
- **sans un geste**, arrêt une heure après (la minuterie d'extinction) ;
- **la première pause**, ou une autre écoute, et le lecteur redevient
  ordinaire : volume média, plein son, plus de boucle.

L'icône de réveil d'Android, dans la barre d'état, montre l'heure du
départ, une minute avant l'heure dite ; l'application dit l'heure dite.

**Le premier essai** (1.3.0.12) démarrait à l'heure, au tiers du son,
montant de 5 % toutes les 3 secondes dès le chargement : quand le son
arrivait, la montée était à moitié faite et ne s'entendait plus, et l'on
était « réveillé par la pub, pas vraiment par la radio ». D'où la
minute muette d'avance, l'idée de l'utilisateur, et la montée refaite.

**Le plantage du réveil**, trouvé par la trace (036) :
`ForegroundServiceDidNotStartInTimeException`. Lancé par le réveil avec
`startForegroundService`, le service doit passer au premier plan dans les
secondes qui suivent ; Media3 ne le fait qu'une fois son propre
contrôleur connecté et la station chargée, trop tard parfois, et Android
arrête l'application. Désormais `passerAuPremierPlan()`, tout de suite :
une notification « Réveil radio » sous le numéro de celle de Media3
(1001), qui la remplace dès qu'elle vient. C'était sans doute aussi
l'« application fermée en bougeant le téléphone ».

## Fichiers

`lecture/Reveil.kt` (nouveau), `lecture/ServiceLecture.kt`,
`MainActivity.kt`, `res/layout/heure_du_reveil.xml`, `res/menu/principal.xml`, `AndroidManifest.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

Version 1.3.0.12, code 45, 1.3.0.13, code 46, 1.3.0.14, code 47, puis
1.3.0.16, code 49, les roues, en
essai.

## Vérification

À relancer : installer la 1.3.0.16.

- « … », « Réveil radio… », « Régler… » : l'heure dans deux minutes, le
  jour d'aujourd'hui, une station. « Réveil programmé : … » ; l'icône du
  réveil dans la barre d'état.
- Fermer l'application, éteindre l'écran, téléphone en silencieux : une
  minute avant l'heure, rien ne s'entend ; à l'heure, le son monte du
  silence au plein en une minute.
- Pause depuis la notification : le son s'arrête ; relancer une autre
  station : volume normal.
- Mode avion, un réveil dans deux minutes : à l'heure dite, la sonnerie
  du téléphone ; pause l'arrête.
- Redémarrer le téléphone : le réveil est toujours programmé (l'icône
  dans la barre d'état).
- « Arrêter » : l'icône disparaît, rien ne sonne.
