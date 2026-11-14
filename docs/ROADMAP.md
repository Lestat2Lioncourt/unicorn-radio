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
  2026-10-27 (« renomme emporter en exporter »). Fait au 015.
- **« Importer une liste… »** : un fichier reçu, exporté par quelqu'un
  d'autre ou fichier de configuration entier dont seule la section radio
  compte, versé dans ma liste comme autant de gestes, `favori.ajouter`,
  `dossier.ajouter` chemin et station, `podcast.ajouter` ; rien d'effacé,
  ce qui manque s'ajoute, un fichier rejoué ne double rien. Apparié, les
  gestes partent au serveur ; sans serveur, la liste locale les reçoit.
  Remplace le fichier ouvert en lecture seule à côté de la liste. Décidé
  le 2026-10-27 avec le bureau, qui a les deux entrées. Fait au 015.

## Les prochains chantiers, dans l'ordre

Décidés le 2026-11-11, en pensant d'abord à qui n'a que le téléphone.

1. **Retrouver une station** (021) : quand une station ne démarre pas, sa
   fiche est redemandée à l'annuaire, une autre adresse fait repartir la
   lecture, un identifiant disparu est suivi par le nom quand il est
   unique, et chaque cas est dit. Appui long, « Retrouver dans
   l'annuaire… » pour choisir soi-même parmi les homonymes. Le pendant du
   232 du bureau. **Fait le 2026-11-11.**
2. **Précédent et suivant dans la liste de lecture** : pendant un épisode,
   « précédent » revient au dernier épisode passé, « suivant » va au
   premier à suivre ; les deux boutons sur la ligne de lecture de l'écran,
   pour les stations aussi. **Fait le 2026-11-11 (022).**
3. **La sauvegarde automatique de la liste** (024, fait le 2026-11-11) : le fichier d'export écrit de
   lui-même à un emplacement choisi une fois, à chaque changement ; un
   téléphone perdu n'emporte plus la liste. Cadre décidé le 2026-11-11 :
   - aucune question imposée, rien de redemandé tant que rien n'est choisi ;
   - une entrée « Sauvegarde automatique… » dans le menu « … », qui ouvre
     le sélecteur d'Android, puis montre où la sauvegarde s'écrit, la date
     de la dernière, et permet de changer ou d'arrêter ;
   - **un emplacement hors du téléphone d'abord** (« si on perd le
     téléphone ») : le sélecteur d'Android propose les espaces en ligne
     dont l'application est installée ; à éprouver, car certains
     n'acceptent pas le choix d'un dossier mais seulement la création
     d'un fichier, d'où peut-être un fichier choisi plutôt qu'un dossier ;
   - un rappel discret, une seule fois, après le premier favori ou dossier
     créé sans sauvegarde réglée ; pas de rappel quand on est apparié, le
     serveur garde déjà la liste ;
   - l'emplacement devenu inaccessible est dit une fois, et l'entrée du
     menu redevient « à régler ».
4. **La vitesse de lecture des podcasts**, et les sauts de quinze et
   trente secondes. **Fait le 2026-11-11 (029).**
5. **Reprendre seule au retour du réseau** : une lecture tombée sur une
   coupure repart quand le réseau revient, sans appui. **Fait le
   2026-11-11 (030).**

Ensuite, à voir. Faits le 2026-11-12 : la dernière écoute relancée d'un
appui (033), un widget (034), le réveil radio (035), devenu des réveils
et des comptes à rebours nommés, une station ou une sonnerie du
téléphone, le volume progressif à cocher (037), annoncés dans les
notifications (038) ; et, en chemin, le paysage (032), la trace des
plantages (036), la licorne des notifications (039). Faits le
2026-11-12 et le 13 : l'OPML des podcasts (040), les titres aimés (041),
la sélection dans la liste de lecture (042). Faits le 2026-11-14 : ranger
la radio en cours d'écoute (043), échanger ses radios avec un téléphone
proche, toutes ou un dossier, et exporter un dossier seul (044).

Décidés le 2026-11-13, dans l'ordre :

1. ~~L'égaliseur~~ et ~~le spectre~~ : faits le 2026-11-14 (046). Reste
   l'égaliseur par station, plus tard.
2. **Android TV** : voir « Installation sur Android TV » plus bas.
3. **MilkDrop**, pour les box qui le portent : voir plus bas.

Restent, sans ordre : un écran de diagnostic avec le journal (la trace
des plantages en est un premier morceau). **Android Auto**, mis de côté
le 2026-11-12 : Android Auto cache les applications qui ne viennent pas
du Play Store, et chaque utilisateur devrait passer ses réglages en mode
développeur et cocher « Sources inconnues », rédhibitoire pour le public
visé ; rien à essayer sans voiture non plus.

**Le téléchargement automatique des nouveaux épisodes**, mis de côté le
2026-11-14 : « j'ai peur de télécharger des centaines d'épisodes pour
les podcasts qui publient beaucoup ». Ce qui avait été décidé, pour le
jour où on le reprendrait : podcast par podcast, une case « Télécharger
les nouveaux épisodes » décochée par défaut ; une vérification toutes les
quelques heures, en wifi seulement, pas sur batterie faible ; seuls les
épisodes parus après la coche ; au plus les trois derniers téléchargés
d'office par podcast, les plus anciens effacés, jamais ceux téléchargés à
la main.

## Installation sur Android TV

Discuté le 2026-11-13, pour une Xiaomi TV Box S de deuxième génération
(modèle `MiTV-AFKR0`, aussi `MDZ-28-AA` : Amlogic S905X4, Mali-G31,
OpenGL ES 3.2, Google TV). L'application demande Android 8 ; elle
s'installe telle quelle sur une box Android TV ou Google TV, mais ne se
voit pas sur l'écran d'accueil et se mène mal à la télécommande.

**Installer, la première fois.** Pas de navigateur sur Google TV :
l'application Downloader (AFTVnews, gratuite au Play Store de la box).

1. Mode développeur : *Paramètres* → *Système* → *À propos*, sept appuis
   sur *Build de l'OS Android TV*.
2. *Paramètres* → *Applications* → *Sécurité et restrictions* →
   *Sources inconnues* : autoriser Downloader.
3. Dans Downloader, l'adresse de l'APK ; Android propose l'installation.

Ensuite, la mise à jour proposée par l'application prend le relais.

**Une adresse fixe.** Le nom de l'APK change à chaque version, et une
longue adresse se tape mal à la télécommande. À chaque release, une
copie de l'APK sous le nom fixe `unicorn-radio.apk`, en plus de celle
numérotée, pour que cette adresse mène toujours à la dernière :

`https://github.com/Lestat2Lioncourt/unicorn-radio/releases/latest/download/unicorn-radio.apk`

Plus court encore : un code Downloader (quelques chiffres, gratuit sur
leur site) qui mène à cette adresse, donné dans le README. À ajouter à
`prompts/publier.md` le moment venu.

**Ce qu'il faut changer dans l'application :**

- **Paraître sur l'écran d'accueil de la box** : la catégorie
  `LEANBACK_LAUNCHER` sur l'activité, une bannière de 320 × 180, et
  l'écran tactile déclaré non obligatoire (`android.hardware.touchscreen`
  `required="false"`), sans quoi Android TV la refuse.
- **La télécommande** : listes et boutons se parcourent déjà aux
  flèches ; le travail est de **bien montrer ce qui a le focus**, un
  cadre ou une surbrillance nette vue du canapé, et qu'aucun bouton ne
  soit hors d'atteinte : la ligne de lecture, les onglets, le menu
  « … ». L'appui long est OK maintenu, Android TV le donne.
- **La saisie** : le clavier de la box ou la voix de la télécommande,
  rien à faire.
- **L'écran** : la disposition en paysage (032) sert de base, les textes
  un peu plus grands pour la distance.
- **Les touches lecture et pause** de la télécommande marchent déjà,
  par la session de lecture, comme celles du casque.
- **Un mode télé** : le spectre en plein écran, la station et le titre,
  comme le mode télé du bureau.

**MilkDrop**, après : projectM, la reprise libre de MilkDrop (LGPL),
demande OpenGL ES 3. Les Mi Box 3 et Mi Box S de première génération
(Mali-450, OpenGL ES 2.0) ne le portent pas ; la deuxième génération
oui, puce modeste, des préréglages légers et un rendu en 720p ou 1080p
agrandi ; la Nvidia Shield TV, très à l'aise. Il faut le NDK pour
construire projectM, le son pris dans le lecteur comme pour le spectre,
un plein écran aux flèches pour changer de préréglage, des préréglages
dont la licence permet la distribution, et le proposer seulement là où
OpenGL ES 3 est présent.

## Plus tard, peut-être

- Les podcasts : les quatre paliers sont faits, chercher et écouter (008),
  « Mes podcasts » (009), la liste de lecture (012), le téléchargement
  (016). Reste, peut-être, le téléchargement d'office des nouveaux
  épisodes des podcasts suivis, mis de côté le 2026-11-14 (plus haut).
- **La synchronisation avec l'Unicorn-TV de la maison**, décidée le
  2026-10-26 et décrite dans `format-d-echange.md` : l'application s'apparie
  à un serveur précis par QR code, ou code court tapé à la main ; le serveur
  est seul maître de la liste, le téléphone en tire la copie et lui renvoie
  ses gestes, horodatés et rejouables, faits hors ligne compris. Tant qu'elle
  n'est pas appariée, l'application reste en mode fichiers, avec ses favoris
  locaux, versés au serveur à l'appariement. **Faite** : l'appariement
  par QR code ou code court, la copie tirée du serveur, les gestes poussés
  et rejoués, l'adresse du serveur gardée et retrouvée au changement de
  réseau (020), les écoutes des podcasts partagées.
- L'égaliseur et le spectre, faits le 2026-11-14 (046). La minuterie d'extinction, les
  titres passés, le site et l'adresse d'une station, eux, sont faits
  (013, 014).
- **Proposer la mise à jour**, demandé le 2026-10-26, fait au 010 le
  2026-10-29 après la première version publiée : la dernière release du
  dépôt lue à chaque ouverture depuis le 019, la note dans la langue de
  l'application, « Installer » télécharge l'APK et ouvre l'installeur
  d'Android, qui confirme encore ; « Plus tard » redemande le lendemain.
  La clef de signature, stable et hors du dépôt, et la procédure
  `prompts/publier.md` étaient les prérequis.
- **Une version premium à prix modique**, évoquée le 2026-11-12, rien de
  décidé. Ce qui pèse :
  - la GPL permet de vendre, mais quiconque peut redistribuer le code et
    l'APK gratuitement, et retirer une vérification de licence d'un code
    public ; seul auteur, on peut garder des fonctions premium hors du
    dépôt public, sous une autre licence ;
  - payer dans l'application (Google Play Billing) suppose le Play Store,
    écarté plus bas : compte, frais, revue, 15 % de commission ;
  - vendre hors du Play Store : une plateforme qui encaisse et déclare la
    TVA européenne à notre place (« merchant of record »), et une clef
    vérifiée hors ligne, pour tenir « aucune collecte » ;
  - en France, des ventes régulières demandent un statut (la
    micro-entreprise suffit) ; des dons sont aussi un revenu à déclarer ;
  - le plus simple et le plus fidèle à la GPL : rester gratuit, avec un
    lien « Soutenir le projet » (Liberapay, Ko-fi, GitHub Sponsors) dans
    « À propos » et le README ;
  - la variante discutée le même jour, un **soutien annuel** (par exemple
    5 € par an) : reste un don, récurrent, tant que rien n'est donné en
    échange, ni fonction ni badge ; dire « soutien annuel », pas
    « abonnement ». Le prélèvement et ses rappels, c'est la plateforme
    (Liberapay, GitHub Sponsors). Dans l'application, sur l'honneur et
    sans réseau : « J'ai fait un don » note la date sur le téléphone, un
    an après un rappel discret propose de renouveler ; sans cela, au plus
    un rappel discret par an. À reprendre dans quelques semaines.

## Écarté

- **Emballer la page web d'Unicorn-TV dans une WebView** : la WebView est
  Chrome, donc toujours pas d'AAC, et le son d'une page s'arrête mal écran
  éteint. Il faudrait le lecteur natif derrière de toute façon.
- **Le Play Store** : l'installation par APK suffit à qui veut l'application,
  et évite un compte, des frais et une revue.
