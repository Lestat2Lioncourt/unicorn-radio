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
  de glisser-déposer au doigt.
- Écrire le fichier d'échange, qu'Unicorn-TV importe sans rien effacer.

## Plus tard, peut-être

- Les podcasts, qui sont du RSS : le fichier d'échange les porte déjà.
- Aller chercher le fichier directement sur l'Unicorn-TV de la maison, par
  son adresse ou son QR code, sans passer par un envoi.
- Un égaliseur, comme sur le bureau.

## Écarté

- **Emballer la page web d'Unicorn-TV dans une WebView** : la WebView est
  Chrome, donc toujours pas d'AAC, et le son d'une page s'arrête mal écran
  éteint. Il faudrait le lecteur natif derrière de toute façon.
- **Le Play Store** : l'installation par APK suffit à qui veut l'application,
  et évite un compte, des frais et une revue.
