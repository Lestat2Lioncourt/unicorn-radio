# 046 — L'égaliseur et le spectre

Le 2026-11-14. « On passe à l'égaliseur + spectre. » Le cadre, décidé le
2026-11-13 dans `ROADMAP.md` : un égaliseur à dix bandes comme sur le
bureau, et le spectre comme sur le bureau, sans MilkDrop.

## Ce qui change

### L'égaliseur

« Égaliseur… » dans le menu « ⋮ ».

- **Dix bandes**, de 31 Hz à 16 kHz, de −12 à +12 dB, un curseur par
  bande avec sa valeur.
- **La courbe de réponse** au-dessus, de 20 Hz à 20 kHz, calculée à partir
  des filtres réels ; pâle quand l'égaliseur est coupé.
- **Les préréglages** : Plat, Rock, Pop, Jazz, Classique, Basses, Voix,
  Perso (le dernier réglage fait à la main), puis les siens.
- **« Enregistrer sous… »** garde le réglage sous un nom ; le même nom
  remplace. **« Supprimer ce préréglage »** n'apparaît que pour un des
  siens.
- **Un interrupteur** l'allume ou le coupe ; bouger une bande ou choisir
  un préréglage l'allume.
- **Pour tout** : stations et épisodes, téléchargés ou non. Le réglage par
  station viendra plus tard.

### Le spectre

- **Une bande sous la ligne de lecture**, dès que quelque chose joue.
- **Un appui** : en grand, sur fond noir, la station et le titre en bas,
  l'écran gardé allumé ; un appui ferme.
- **Un appui long** (sur la bande ou en grand), ou « Spectre… » dans le
  menu « ⋮ » : les réglages, avec un aperçu qui suit chaque choix.
  - Forme : miroir (par défaut), barres à crêtes, courbe, onde, aucune
    (la bande disparaît).
  - Couleurs : arc-en-ciel (par défaut), couleur de l'application,
    vu-mètre.
  - Bandes : 10, 20, 30 (par défaut, un téléphone étant plus étroit que
    l'écran du bureau) ou 60.
- En pause, le spectre retombe et cesse de se dessiner.

### L'appui long sur le logo

Le même jour : « Sur un appui long sur le logo de la radio en cours de
lecture, on pourrait passer du mode actuel à un mode avec le spectre sous
le lecteur, et inversement, ainsi qu'accéder à la configuration
directement. »

- **L'appui long sur le logo** (ou les initiales, ou l'image d'un
  épisode) ouvre un petit menu : « Masquer le spectre » ou « Afficher le
  spectre », « Réglages du spectre… », « Égaliseur… ».
- **Masquer** est gardé d'une fois sur l'autre et ne touche pas aux
  réglages : la bande revient telle qu'elle était. Masquée, rien n'est
  calculé. Afficher depuis la forme « aucune » remet le miroir.
- Au départ, le spectre est affiché.

### La bande qui n'apparaissait pas

Retour d'essai : « Par défaut je vois "Masquer le spectre" alors que je ne
le vois pas, puis si je demande de l'afficher on ne voit rien. On peut
seulement le voir dans les réglages du spectre. »

La cause : au démarrage, `disposer()` vide l'écran et y remet les vues
qu'il connaît, pour le portrait comme pour le paysage (chantier 032). La
bande du spectre n'était pas dans sa liste : elle était retirée de
l'écran dès l'ouverture, et seul l'aperçu des réglages, une autre vue, se
dessinait. Elle y est ajoutée, sous l'avancement, dans les deux sens.

### En paysage, sur deux colonnes

Retour d'essai : « En mode paysage, il ne serait pas mieux d'avoir la
configuration de l'égaliseur sur deux colonnes pour ne pas avoir à
faire défiler ? » Et de même pour le spectre.

- **L'égaliseur** : à gauche l'interrupteur, la courbe, le préréglage,
  « Supprimer » et « Enregistrer sous… » l'un sous l'autre ; à droite
  les dix bandes en deux colonnes de cinq, 31 à 500 Hz puis 1 à 16 kHz.
- **Le spectre** : à gauche la forme ; à droite l'aperçu, les couleurs et
  les bandes.
- **La fenêtre prend 92 % de la largeur** : Android garde sinon les
  boîtes de dialogue étroites.
- En portrait, rien ne change. Le défilement reste possible si l'écran
  est très bas.

### Comment

Le son est traité **dans le lecteur de l'application** : deux étapes sont
posées dans la chaîne audio de Media3 (`DefaultAudioSink`), l'égaliseur
puis une dérivation qui copie le son pour le spectre.

- L'égaliseur est une suite de dix filtres en cloche (formules du
  « cookbook » de R. Bristow-Johnson), canal par canal, en PCM 16 bits.
  Un gain d'entrée baisse le tout d'autant que la bande la plus poussée,
  pour ne pas saturer ; ce qui dépasserait est écrêté. Coupé ou plat, il
  recopie le son tel quel.
- Le spectre est une FFT de 2048 points, fenêtre de Hann, en bandes
  logarithmiques de 31 Hz à 16 kHz, de −70 à 0 dB. Le son arrive au
  lecteur un quart de seconde avant d'être entendu : on le lit d'autant
  en arrière.
- **Pas de permission du micro**, que demanderait le `Visualizer`
  d'Android, et pas l'égaliseur d'Android, dont le nombre de bandes
  change d'un téléphone à l'autre.
- **Rien n'est copié ni calculé** tant qu'aucun spectre n'est à l'écran :
  écran éteint ou application cachée, la dérivation ne fait que passer.

Version 1.4.0.7, code 64, en essai ; l'appui long sur le logo en
1.4.0.8, code 65 ; la bande remise à l'écran en 1.4.0.9, code 66 ; les
deux colonnes en paysage en 1.4.0.10, code 67.

## Fichiers

`lecture/Egaliseur.kt`, `lecture/Spectre.kt`, `VueSpectre.kt`,
`ReglagesSon.kt` (nouveaux), `lecture/ServiceLecture.kt`,
`MainActivity.kt`, `res/layout/activity_main.xml`,
`res/menu/principal.xml`, `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.4.0.10-debug.apk`, puis
fermer l'application de la liste des applications récentes (le service de
lecture doit repartir pour prendre l'égaliseur).

- Lancer une station : la bande du spectre s'anime sous la ligne de
  lecture, en miroir, arc-en-ciel ; elle suit la musique, sans avance
  visible.
- Pause : le spectre retombe ; reprise : il repart.
- Appui sur la bande : en grand, station et titre en bas ; le titre suit
  quand il change ; l'écran ne s'éteint pas. Appui : fermé.
- Appui long : les réglages ; chaque forme, couleur et nombre de bandes
  change l'aperçu et la bande aussitôt. « Aucune » : la bande disparaît ;
  « Spectre… » dans le menu la fait revenir.
- « Égaliseur… » : coupé au départ, la courbe pâle et plate.
- « Basses » : l'égaliseur s'allume, la courbe monte à gauche, les basses
  s'entendent, sans saturation.
- Bouger une bande : le préréglage devient « Perso », la courbe suit.
- « Enregistrer sous… » « Soirée » : « Préréglage : Soirée » ;
  « Supprimer ce préréglage » apparaît ; le supprimer : retour à Perso.
- Couper l'interrupteur : le son redevient d'origine aussitôt.
- Un épisode de podcast, à vitesse 1,5× : l'égaliseur et le spectre
  marchent aussi.
- Appui long sur le logo de la station : le menu à trois lignes ;
  « Masquer le spectre » : la bande disparaît. Appui long à nouveau :
  « Afficher le spectre », elle revient avec la même forme et les mêmes
  couleurs.
- Depuis ce menu, « Réglages du spectre… » et « Égaliseur… » ouvrent
  leurs fenêtres.
- Pendant un épisode : le même menu sur l'image de l'épisode.
- En paysage, « Égaliseur… » : deux colonnes, les bandes à droite en
  deux colonnes de cinq, sans défilement ; « Spectre… » : la forme à
  gauche, le reste à droite. En portrait : comme avant.
- Tourner le téléphone en paysage : la bande est sous le lecteur, sur
  toute la largeur ; revenir en portrait : elle y est toujours.
- Fermer, rouvrir l'application : les réglages sont gardés, le spectre
  masqué le reste.
- Écran éteint pendant la lecture, longtemps : pas plus de batterie
  qu'avant (rien n'est calculé).
