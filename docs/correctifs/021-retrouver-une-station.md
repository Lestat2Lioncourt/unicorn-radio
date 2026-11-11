# 021 — Retrouver une station

Le 2026-11-11. La veille, le bureau avait perdu des favoris et des stations
de dossiers parce que Radio Browser avait refait leurs identifiants (son
232). Le téléphone, lui, garde la fiche complète de chaque station et ne
rafraîchit jamais son annuaire : rien n'y disparaît, mais une station dont
l'adresse ou l'identifiant a changé finit par ne plus jouer, sans un mot,
et rien ne suit. « Il faut aussi penser aux utilisateurs qui n'ont que
l'app Android et pas Unicorn-TV. »

## Ce qui change

**Quand une station ne démarre pas**, le service de lecture demande sa
fiche à l'annuaire par son identifiant, une fois par station lancée
(`ServiceLecture.reprise`, `Retrouver.chercher`) :

- **la fiche existe avec une autre adresse de flux** : la lecture repart
  aussitôt à cette adresse, l'élément du lecteur remplacé sur place, et la
  fiche gardée dans mes listes est mise à jour ;
- **l'identifiant n'y est plus** : les stations de ce nom exact, dans le
  même pays quand il départage. Une seule, c'est elle : la lecture repart
  avec elle, et le nouvel identifiant remplace l'ancien partout, sans
  doublon là où il était déjà. « “X” a changé d'identifiant dans
  l'annuaire : favoris et dossiers suivent » ;
- sinon, le pourquoi est dit : même adresse et l'annuaire n'en connaît pas
  d'autre ; plus dans l'annuaire et aucune station de ce nom ; plus dans
  l'annuaire et N homonymes, avec le renvoi à l'appui long ; annuaire
  injoignable.

**Appui long sur une station de mes favoris ou d'un dossier**, « Retrouver
dans l'annuaire… » : les stations de ce nom, l'exact d'abord puis celles
dont le nom le contient, chacune avec son pays, son codec et son débit,
la fiche actuelle cochée. Celle qu'on choisit remplace l'ancienne partout
(`MainActivity.retrouver`, `remplacer`) ; si c'est la station en cours, la
lecture repart avec la nouvelle.

**Le remplacement** (`Retrouver.appliquer`, `MesRadios.remplacerLaStation`)
touche la copie du serveur et la liste locale, favoris et chaque dossier
qui citait l'ancienne, au même rang. Apparié et quand l'identifiant change,
le journal porte au serveur un `favori.retirer` puis `favori.ajouter`, et
par dossier un `dossier.retirer` puis `dossier.ajouter` : les gestes du
contrat, **rien de nouveau dans le format d'échange**. La synchronisation
suivante remet les deux côtés d'accord ; le serveur, qui fait le même
raisonnement chaque jour, n'y voit que des gestes déjà faits. Quand seule
l'adresse change, pas de geste : le serveur met ses fiches à jour lui-même.

**Les instances de la liste se relisent** : écran et service ont chacun
leur `MesRadios` sur le même fichier, et chacun gardait sa lecture en
mémoire. Une génération de processus monte à chaque écriture ; `charger`
relit quand elle a bougé. L'écran s'accroche aussi à
`Retrouver.surRemplacement` pour recharger et synchroniser dès que le
service a remplacé une fiche.

Le code du pays voyage dans les extras de l'élément du lecteur
(`CLEF_PAYS`), pour départager les homonymes depuis le service. L'annuaire
gagne `fiche(uuid)` (`/json/stations/byuuid`) et `chercher(nomExact = true)`.

Pas de suivi d'adresse quand la station joue : `/json/url` rend l'adresse
brute, parfois une liste de lecture que le lecteur ne lit pas ; le journal
seul la note, comme avant. Pas de grisage des stations disparues ni de
vérification de fond.

Version 1.1.4, code 24.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/annuaire/Retrouver.kt` (nouveau),
`Annuaire.kt`, `serveur/MesRadios.kt`, `lecture/Elements.kt`,
`lecture/ServiceLecture.kt`, `MainActivity.kt`, `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.1.4.

- Une station dont le flux a changé : la lancer, elle repart sur la
  nouvelle adresse après l'échec, message « nouvelle adresse du flux » ;
  « Copier l'adresse » donne la nouvelle. Pour l'éprouver sans attendre,
  changer l'adresse d'une station dans un fichier d'export, l'importer dans
  une liste vide, lancer.
- Une station dont l'identifiant a changé, unique par son nom : même
  chose avec le message « a changé d'identifiant » ; dans un dossier et en
  favori, les deux suivent ; apparié, le bureau la voit sous le nouvel
  identifiant après la synchronisation. À éprouver avec un identifiant
  inventé dans le fichier d'export.
- Un nom porté par plusieurs stations : le message compte les homonymes et
  renvoie à l'appui long ; « Retrouver dans l'annuaire… » les liste, pays
  et débit, le choix remplace la fiche et la station repart.
- Une station morte encore dans l'annuaire : « ne répond pas, l'annuaire
  ne connaît pas d'autre adresse », une seule demande, pas de boucle.
- Mode avion : « et l'annuaire non plus ».
- Un épisode qui échoue : rien de tout cela.
