# Feuille de route

## D'où ça vient

Unicorn-TV, le lecteur de bureau, joue des radios web depuis l'annuaire
Radio Browser. Pour les emporter sur le téléphone, il a d'abord produit une
page web, puis l'a servie lui-même sur le wifi de la maison (son chantier
220, octobre 2026). Deux murs se sont dressés : un navigateur refuse qu'une
page sécurisée, fichier local ou https, demande un flux `http`, et la
plupart des stations sont en `http` ; et Chrome sur Android ne lit pas
l'AAC brut que beaucoup diffusent. Hors de la maison, la page restait muette
sur une station sur deux.

Une application n'a aucun de ces murs : le `http` se déclare, le lecteur
natif lit l'AAC et le HLS, et le son continue écran éteint. D'où ce projet,
décidé le 2026-10-25 : **une application autonome**, qui cherche elle-même
dans l'annuaire, et qui **échange ses favoris et ses dossiers avec Unicorn-TV**
par un fichier, pour qui se sert des deux.

## Les paliers

Chacun utilisable seul.

### 1. Lire le fichier, jouer

- Ouvrir un fichier d'export d'Unicorn-TV (« Ouvrir avec », ou depuis les
  fichiers du téléphone), en garder plusieurs, le même nom remplace.
- Les favoris et l'arbre des dossiers tels qu'ils sont dans le fichier.
- Lecture par Media3 : MP3, AAC, HLS ; les déclinaisons d'une station
  essayées dans l'ordre ; le titre en cours par les métadonnées ICY.
- Service de lecture en arrière-plan, notification avec pause et suivant,
  écran de verrouillage, boutons du casque.

### 2. Chercher dans l'annuaire

- Recherche par nom, parcours par pays et par style, sur Radio Browser
  directement, avec les mêmes identifiants qu'Unicorn-TV.
- L'étoile met en favori. Le compteur d'écoute de l'annuaire est honoré.

### 3. Ranger, et rendre le fichier

- Dossiers créés et gérés sur le téléphone : « Ranger dans… » par menu, pas
  de glisser-déposer au doigt. Fait au 007, comme gestes de la
  synchronisation.
- **« Exporter mes radios… »** : écrire le fichier d'échange, la section
  radio seule avec `application: "unicorn-radio"`, pour le donner à
  quelqu'un ou le garder ; le même nom que sur le bureau, décidé le
  2026-10-27 (« renomme emporter en exporter »).
- **« Importer une liste… »** : un fichier reçu, exporté par quelqu'un
  d'autre ou fichier de configuration entier dont seule la section radio
  compte, versé dans ma liste comme autant de gestes, `favori.ajouter`,
  `dossier.ajouter` chemin et station, `podcast.ajouter` ; rien d'effacé,
  ce qui manque s'ajoute, un fichier rejoué ne double rien. Apparié, les
  gestes partent au serveur ; sans serveur, la liste locale les reçoit.
  Aujourd'hui, un fichier ouvert s'ajoute en lecture seule à côté de la
  liste, et disparaît de la vue une fois apparié : c'est ce que ce point
  remplace. Décidé le 2026-10-27 avec le bureau, qui a les deux entrées.

## Plus tard, peut-être

- Les podcasts : en cours, par paliers (008, 009), chercher et écouter,
  « Mes podcasts » ; restent la file d'écoute et le téléchargement.
- **La synchronisation avec l'Unicorn-TV de la maison**, décidée le
  2026-10-26 et décrite dans `format-d-echange.md` : l'application s'apparie
  à un serveur précis par QR code, ou code court tapé à la main ; le serveur
  est seul maître de la liste, le téléphone en tire la copie et lui renvoie
  ses gestes, horodatés et rejouables, faits hors ligne compris. Tant qu'elle
  n'est pas appariée, l'application reste en mode fichiers, avec ses favoris
  locaux, versés au serveur à l'appariement. Rien n'existe encore d'aucun
  côté ; Unicorn-TV construit les routes, l'application vient ensuite.
- Un égaliseur, comme sur le bureau.
- **Proposer la mise à jour**, demandé le 2026-10-26, fait au 010 le
  2026-10-29 après la première version publiée : la dernière release du
  dépôt lue au plus une fois par jour, la note dans la langue de
  l'application, « Installer » télécharge l'APK et ouvre l'installeur
  d'Android, qui confirme encore ; « Plus tard » redemande le lendemain.
  La clef de signature, stable et hors du dépôt, et la procédure
  `prompts/publier.md` étaient les prérequis.

## Écarté

- **Emballer la page web d'Unicorn-TV dans une WebView** : la WebView est
  Chrome, donc toujours pas d'AAC, et le son d'une page s'arrête mal écran
  éteint. Il faudrait le lecteur natif derrière de toute façon.
- **Le Play Store** : l'installation par APK suffit à qui veut l'application,
  et évite un compte, des frais et une revue.
