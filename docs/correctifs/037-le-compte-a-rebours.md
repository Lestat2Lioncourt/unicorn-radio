# 037 — Les réveils et les comptes à rebours

Le 2026-11-12. Après les essais du réveil (035), en plusieurs remarques :

- « un lancement du réveil en mode compte à rebours en plus de réveil
  est possible ? » ;
- « je vois la [station] précédente présélectionnée mais je dois
  recliquer dessus » ; « éventuellement réveil rapide qui ne demande que
  l'heure » ;
- « mémoriser les durées précédentes si on a donné un nom » ;
- « l'emplacement compte à rebours dans le menu réveil est déroutant, il
  lui faut une entrée de menu propre » ;
- « une option volume progressif à cocher […], et aussi la possibilité de
  choisir un son du téléphone plutôt qu'une radio » ;
- « on voit la liste des alarmes existantes, chacune a son option on/off
  et son nom » ;
- « si aucun nom n'est saisi, on considère que l'alarme ou le compte à
  rebours ne doit pas être enregistré ».

Un premier état (1.3.0.17 : un seul réveil réglé ligne par ligne, les
comptes à rebours dans sa fenêtre) a été remplacé par celui-ci.

## Ce qui change

**Deux entrées du menu « … »** : « Réveil radio… » et « Compte à
rebours… ». Chacune ouvre sa liste :

```
Réveils
  Semaine      07:00 · lun. mar. mer. jeu. ven. · FIP     [■]
  Week-end     09:30 · sam. dim. · Sonnerie : Aube         [□]
  Sans nom     06:15 · une fois · FIP                      [■]
  ＋ Nouveau réveil…

Comptes à rebours
  Sieste       20 min · France Culture · fin à 14:32       [■]
  Pause café   15 min · FIP                                [□]
  ＋ Nouveau compte à rebours…
```

- **L'interrupteur** met un réveil en marche ou l'arrête ; lance un
  compte à rebours maintenant ou l'annule.
- **Un appui sur le nom** ouvre ses réglages, chacun d'un appui, la liste
  revenant à jour : Nom, Heure ou Durée, Jours, Son, Volume progressif,
  Supprimer. Changer l'heure d'un réveil le remet en marche : le réveil
  rapide.

**« Nouveau… »** : d'abord le nom, sous « Mémoriser sous ce nom ; sans
nom, il ne servira qu'une fois » ; puis l'heure (des roues) et, s'il est
nommé, les jours, ou la durée (deux roues) ; puis le son. Enregistré en
marche, ou lancé. **Sans nom**, il paraît en italique dans la liste tant
qu'il n'a pas sonné, son interrupteur pour l'annuler, puis disparaît ; un
réveil sans nom sonne une fois, sans demander les jours.

**Le son** : mes stations, puis « Sonnerie du téléphone… », le sélecteur
de sonneries d'Android. Un appui coche, « OK » prend ; celui d'avant est
déjà coché, « OK » suffit. Le dernier choisi est proposé à la création
suivante.

- **Une station** démarre une minute avant l'heure, en muet, le temps
  que la publicité passe ; si elle ne joue pas à l'heure dite, la
  sonnerie du réveil d'Android, en boucle.
- **Une sonnerie** démarre à l'heure, en boucle jusqu'à la pause.

**Le volume progressif**, coché d'office : le son monte du silence au
plein en une minute à l'heure dite. Décoché, il part au plein (du volume
du réveil du téléphone).

**Chacun sa programmation** auprès d'Android, sous son propre numéro :
réveils et comptes à rebours sonnent indépendamment, plusieurs à la fois
s'il le faut. Au départ, l'alarme entière passe au service dans
l'intention (une sans-nom n'est déjà plus dans la liste).

**Les essais d'avant repris une fois** : le réveil devient « Réveil »,
en marche s'il l'était, les comptes à rebours gardés gardent leur nom,
avec la station du réveil.

**Après l'essai de la 1.3.0.18** : « si je clique sur la notification
countdown, rien ne se passe » ; « on pourrait voir le temps restant qui
se décrémente » ; « son du téléphone […] en premier serait plus
intuitif » ; « un interrupteur sur la droite on/off ferait plus pro ».

- **La notification de l'alarme** (celle que le service pose tout de
  suite, et qui peut rester celle qu'on voit) n'avait pas d'action : un
  appui ouvre maintenant l'application, et « Arrêter la lecture » met en
  pause, ce qui finit la sonnerie. L'intention de la session de Media3
  porte aussi `FLAG_ACTIVITY_NEW_TASK`.
- **Le temps qui reste** : la liste des comptes à rebours et les
  réglages d'un compte lancé se relisent chaque seconde, « fin à 14:32 ·
  reste 12:34 ».
- **« Sonnerie du téléphone… » en tête** de la liste des sons, les
  stations ensuite.
- **Les réglages d'une alarme** sont des lignes avec un titre et sa
  valeur dessous ; « En marche » (ou « Lancé », avec le temps qui reste)
  et « Volume progressif » ont un interrupteur à droite ; un appui sur
  leur texte le bascule aussi.

Version 1.3.0.17, code 50, 1.3.0.18, code 51, puis 1.3.0.19, code 52, en
essai.

## Fichiers

`lecture/Reveil.kt` (refait), `lecture/ServiceLecture.kt`,
`lecture/DerniereEcoute.kt`, `MainActivity.kt`,
`res/layout/ligne_alarme.xml` (nouveau), `res/menu/principal.xml`, `lecture/ServiceLecture.kt`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.19.

- « Réveil radio… » : le réveil d'avant, « Réveil », avec son heure, ses
  jours, sa station, son interrupteur comme avant.
- Son interrupteur : arrêté, puis en marche, « Réveil programmé : … ».
- Un appui sur « Réveil », « Heure », une autre heure, « OK » : remis en
  marche à la nouvelle heure.
- « ＋ Nouveau réveil… », sans nom, dans deux minutes, « Sonnerie du
  téléphone… », une sonnerie : « Sans nom » en italique dans la liste ; à
  l'heure, la sonnerie, le son qui monte ; ensuite, il a disparu.
- « Volume progressif : non » sur un réveil : à l'heure, plein son tout
  de suite.
- « Compte à rebours… », « ＋ Nouveau… », « Sieste », 2 minutes, une
  station : « Sieste : fin à … » ; la liste dit « fin à … » ; à la fin, le
  son monte ; ensuite, l'interrupteur est retombé, « Sieste » reste.
- Un réveil et un compte à rebours programmés ensemble : chacun sonne à
  son heure.
- Pendant une sonnerie, un appui sur la notification : l'application
  s'ouvre ; « Arrêter la lecture » : le son s'arrête.
- Un compte à rebours lancé : dans la liste, « reste » décompte chaque
  seconde.
- Redémarrer le téléphone : les réveils en marche sont toujours
  programmés.
