# 050 — La radio de secours, et deux minutes d'avance

Le 2026-11-17. « Pourrait-on définir une radio de secours au cas où
simplement le flux de la radio ne fonctionne pas ? » ; « passe la
temporisation d'une minute avant sonnerie à 2 mn, j'ai trouvé des radios
qui envoient beaucoup de pub avant de commencer. Attention, dans le cas
d'un compte à rebours de 1 mn 30 […] » ; la radio de secours proposée
par défaut, comme le son.

## Ce qui n'allait pas

Une station qui ne jouait pas à l'heure dite passait directement à la
sonnerie du téléphone. Et une minute d'avance, en muet, ne suffisait pas
à certaines stations pour passer leur publicité du début.

## Ce qui change

- **« Radio de secours »** dans les réglages d'une alarme dont le son est
  une station : « Aucune : la sonnerie du téléphone » ou une de mes
  stations. Le dernier choix est proposé à une nouvelle alarme, comme le
  son.
- **Le flux tombe avant l'heure dite** : la radio de secours démarre tout
  de suite, en muet, et passe sa publicité dans le temps qui reste.
- **Rien ne joue à l'heure dite** : la radio de secours, si elle n'a pas
  encore été essayée, avec 20 secondes pour partir, le son montant déjà ;
  sinon, ou si elle ne part pas, la sonnerie, renforcée (049).
- **Le flux tombe après l'heure dite** : la radio de secours, sinon la
  sonnerie.
- **Deux minutes d'avance** au lieu d'une. Le départ est le plus tard de
  « l'heure dite moins deux minutes » et « maintenant » : un compte à
  rebours de 1 min 30 démarre la station tout de suite, en muet, et
  sonne à 1 min 30, pas plus tard.

Version 1.6.0.1, code 73, en essai.

## Fichiers

`lecture/Reveil.kt` (le secours de l'alarme et le dernier choisi,
l'avance), `lecture/ServiceLecture.kt`, `MainActivity.kt`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.6.0.1-debug.apk`.

- Réglages d'une alarme sur une station : la ligne « Radio de secours »,
  « Aucune » au départ ; en choisir une. Une nouvelle alarme la propose.
  Avec une sonnerie comme son, la ligne n'apparaît pas.
- Une alarme dans trois minutes : la station démarre en muet deux
  minutes avant, le son vient à l'heure.
- Un compte à rebours de 1 min 30 sur une station : il démarre tout de
  suite en muet, sonne à 1 min 30.
- Une station principale cassée (une station dont le flux ne répond
  plus), une radio de secours qui marche : à l'heure, c'est la radio de
  secours qu'on entend.
- Mode avion pendant l'attente, puis rien : à l'heure, la sonnerie.
