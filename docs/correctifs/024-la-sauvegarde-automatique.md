# 024 — La sauvegarde automatique

Le 2026-11-11. Sans serveur, la liste ne vivait que sur le téléphone : un
téléphone perdu l'emportait, sauf export fait à la main. « La sauvegarde
sur un emplacement réseau est préférable, si on perd le téléphone. Peu
importe mon service, chaque utilisateur aura le sien. »

## Ce qui change

**« Sauvegarde automatique… »** dans le menu « … ». Non réglée, elle
explique et propose « Choisir l'emplacement… » : le sélecteur d'Android
crée un fichier `unicorn-radio-sauvegarde.json` où l'on veut, de
préférence dans un espace en ligne, que le texte recommande. Réglée, elle
dit où et quand, et propose « Sauvegarder maintenant », « Changer
d'emplacement… », « Arrêter ».

**Un fichier plutôt qu'un dossier** : tous les espaces en ligne qui
s'offrent dans le sélecteur savent créer un fichier, pas tous donner un
dossier. L'accès est gardé d'un lancement à l'autre
(`takePersistableUriPermission`). Le contenu est celui de « Exporter mes
radios… » : le fichier d'échange, que « Importer une liste… » relit sur un
autre téléphone.

**Écrite d'elle-même** à chaque rechargement de la liste, donc après
chaque geste, import ou synchronisation, et en quittant l'écran ; jamais
si rien n'a changé depuis la dernière écriture (une empreinte du contenu
est gardée).

**Pas de question imposée.** Sans emplacement et sans serveur, un rappel
discret, une seule fois, dès que la liste a une station ou un podcast.
Apparié, pas de rappel, le serveur garde déjà la liste ; l'entrée reste.

**Un emplacement devenu inaccessible** est dit une fois ; l'entrée du
menu le rappelle et invite à le choisir de nouveau. Les écritures
suivantes réessaient en silence.

**« Importer une liste… » aussi dans le menu « … »**, au-dessus de
« Exporter mes radios… » : le texte de la sauvegarde renvoie à l'import,
qu'on cherchait là, à côté de l'export, et pas dans l'icône de la barre,
qui reste.

Version 1.2.0, code 26, en essai, puis 1.2.0.1, code 27, avec l'import
dans le menu ; publiée en 1.2.1 (voir 025 pour le code).

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/fichier/Sauvegarde.kt`
(nouveau), `MainActivity.kt`, `res/menu/principal.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.2.0.1.

- Onglet Radio, menu « … » : « Importer une liste… » au-dessus de
  « Exporter mes radios… » ; il ouvre le sélecteur de fichiers. Onglet
  Podcasts : absent, comme l'icône.

- Sans serveur, ajouter un premier favori : le rappel vient une fois,
  plus jamais ensuite.
- « Sauvegarde automatique… », « Choisir l'emplacement… », choisir un
  espace en ligne (Drive, OneDrive, Nextcloud…) : « Liste sauvegardée
  dans … » ; le fichier apparaît dans l'espace en ligne.
- Ajouter une station, quitter l'application : le fichier en ligne a
  changé. Rien changé : il n'est pas réécrit.
- Fermer et relancer l'application, ajouter une station : la sauvegarde
  se fait encore, sans redemander.
- Sur un autre téléphone, « Importer une liste… » sur ce fichier : la
  liste revient.
- Supprimer le fichier en ligne, ajouter une station : « Sauvegarde
  impossible » une fois, l'entrée du menu invite à le choisir de nouveau.
- « Arrêter » : plus d'écriture, le fichier reste où il est.
