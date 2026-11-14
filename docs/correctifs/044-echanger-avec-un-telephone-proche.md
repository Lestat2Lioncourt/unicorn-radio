# 044 — Échanger ses radios avec un téléphone proche

Le 2026-11-14. « Si je croise quelqu'un qui est intéressé par ma liste,
je pourrai la lui envoyer en direct pour qu'il la fusionne avec la
sienne ? Certes on peut aussi exporter importer, mais ce serait un plus.
[…] Je pensais uniquement aux radios catégorisées. Ce n'est pas possible
de faire le lien directement entre deux Unicorn-Radio si les téléphones
sont à proximité ? »

## Ce qui n'allait pas

Donner ses radios à quelqu'un passait par un fichier : exporter,
l'envoyer par un moyen ou un autre, l'autre l'ouvre et l'importe.

## Ce qui change

- **« Échanger avec un téléphone proche… »** dans le menu « ⋮ » de
  l'onglet Radio, puis « Envoyer mes radios » ou « Recevoir des radios ».
- **Celui qui reçoit** : Android demande de rendre le téléphone visible
  (et allume le Bluetooth s'il le faut). Une fenêtre dit le nom du
  téléphone et attend, cinq minutes au plus, ou jusqu'à « Annuler ».
- **Celui qui envoie** : la liste des téléphones et tablettes proches se
  remplit au fil de la recherche ; casques, enceintes, montres et
  voitures n'y sont pas. Un appui sur le bon nom envoie.
- **Ce qui part** : les favoris et les dossiers, comme dans le fichier
  d'export ; les podcasts restent chez soi.
- **Le receveur voit** « X vous envoie N radios, dans M dossiers. Les
  ajouter à votre liste ? » : « Ajouter » les verse comme un import, rien
  effacé, rien doublé, les gestes partent au serveur si l'on est apparié ;
  « Refuser » ne garde rien.
- **Rien n'arrive sans « Recevoir »** : hors de cette fenêtre, l'application
  n'écoute pas.

### Un dossier seul

Le même jour : « Pourrait-on l'ouvrir au clic long sur une catégorie ou
sous-catégorie, pour n'exporter si on le désire qu'une partie de
l'arborescence ? […] L'appui long propose les deux options, envoyer et
exporter. »

- **L'appui long sur un de mes dossiers** propose en plus « Envoyer à un
  téléphone proche » et « Exporter ce dossier… ».
- **Ce qui part** : ce dossier et ses sous-dossiers, chacun sous son
  chemin complet ; ni les favoris, ni les autres dossiers, ni les
  podcasts. Envoyer « Jazz / Années 50 » donne, chez l'autre, « Jazz »
  puis « Années 50 » dedans ; s'il a déjà « Jazz », les stations s'y
  ajoutent sans doublon.
- **Le fichier exporté** porte le nom du dossier :
  `unicorn-radios-Années 50-2026-11-14.json`. Il s'importe comme un
  autre, ici ou sur le bureau.
- Un dossier sans station : « Rien à exporter ».

### Comment

En Bluetooth classique (RFCOMM), d'une application à l'autre, sans
Internet. La liaison est sans appairage, pour ne rien avoir à confirmer
entre deux inconnus ; ce qui passe est une liste de radios publiques, que
le receveur voit avant d'accepter. Sur le fil : un nombre magique, la
longueur, le fichier d'échange en UTF-8 (10 Mo au plus) ; le receveur
répond d'un octet quand il a tout lu.

La solution « Nearby » de Google a été écartée : elle impose une
bibliothèque fermée des services Google, qui ne va pas avec la licence.

Permissions : depuis Android 12, « Appareils à proximité » (recherche
déclarée sans localisation, connexion, visibilité), demandée au premier
usage ; avant, la localisation, qui doit aussi être activée pour que la
recherche trouve quelque chose.

Version 1.4.0.3, code 60, en essai ; le dossier seul en 1.4.0.4, code 61.

## Fichiers

`proximite/Proximite.kt` (nouveau), `MainActivity.kt`,
`AndroidManifest.xml`, `res/menu/principal.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.4.0.4-debug.apk` sur **les
deux** téléphones.

- Onglet Radio, « ⋮ » : « Échanger avec un téléphone proche… » ; il
  n'est pas dans l'onglet Podcasts.
- Téléphone B, « Recevoir des radios » : la permission au premier usage,
  la demande de visibilité, puis la fenêtre d'attente avec son nom.
- Téléphone A, « Envoyer mes radios » : la permission au premier usage ;
  le nom de B apparaît dans la liste en quelques secondes.
- Toucher B : « Envoi à B… », puis « N radios envoyées à B ».
- Sur B : « A vous envoie N radios, dans M dossiers » ; « Ajouter » :
  « N radios ajoutées », les dossiers de A sont là, ceux de B intacts.
- Recommencer : « 0 radio ajoutée », rien n'est doublé.
- « Refuser » sur B : rien ne change.
- A envoie à B sans que B soit en attente : « B n'a pas répondu… ».
- B en attente, « Annuler » : la fenêtre se ferme sans message.
- Sur A, appui long sur un sous-dossier (« Jazz / Années 50 ») : « Envoyer
  à un téléphone proche » et « Exporter ce dossier… » en bas du menu.
- L'envoyer à B en attente : B voit le nombre de stations de ce dossier
  seul ; « Ajouter » crée « Jazz » puis « Années 50 », sans les favoris
  ni les autres dossiers de A.
- « Exporter ce dossier… » : le nom proposé contient celui du dossier ;
  le fichier réimporté ailleurs ne donne que ce dossier.
- « Exporter mes radios… » du menu ⋮ exporte toujours tout, podcasts
  compris.
- Un dossier vide : « Rien à exporter » pour les deux.
