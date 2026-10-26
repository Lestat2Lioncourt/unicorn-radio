# 002 — Lire le fichier, jouer

Le 2026-10-25 au soir, le palier 1 de `ROADMAP.md`. Le but : « une station
de mon fichier qui joue sur mon téléphone, hors wifi, en `http`, avec son
titre, et qui continue écran éteint. » Deux choix tranchés avant d'écrire :
une ligne par station dans l'ordre du fichier, sans regrouper les
déclinaisons, « comme un dossier d'Unicorn-TV » ; et quand plusieurs fichiers
sont gardés, un dossier par fichier à la racine.

## Ce qu'il y avait

Une activité qui affichait son numéro de version. Rien ne jouait.

## Ce qui est fait

**La lecture du fichier** (`fichier/LectureFichier.kt`), selon
`format-d-echange.md` : le fichier d'export complet, dont seule
la section `radio` est lue, ou la section seule ; `format` absent vaut 1, un
autre numéro est refusé en le disant ; une ligne sans identifiant ou sans
adresse est passée ; l'arbre des dossiers se reconstruit depuis les chemins
complets, un parent absent est créé vide, le même chemin deux fois est un
seul dossier. Le JSON se lit avec `org.json`, fourni par Android : pas de
bibliothèque. Le tout devient des `Noeud` (`modele/Modele.kt`) : un fichier,
un dossier, le même objet, ses sous-dossiers et ses stations ; à la racine
d'un fichier, les stations sont les favoris.

**Les fichiers gardés** (`fichier/Fichiers.kt`) : chaque fichier ouvert est
lu, puis copié dans le stockage privé de l'application sous son nom
d'origine ; le même nom remplace. Relus à chaque démarrage. Deux chemins
pour en ouvrir un : « Ouvrir avec » depuis le gestionnaire de fichiers ou
une pièce jointe, par un filtre d'intention sur les JSON, et le bouton de
la barre, qui lance le sélecteur de documents du système. Aucune permission
de stockage. Un appui long sur un fichier à la racine propose de le retirer.

**L'écran** (`MainActivity.kt`, `ListeAdaptateur.kt`), en vues classiques,
sans Compose : une liste, les sous-dossiers puis les stations, une par ligne
avec son logo ou ses initiales, son nom, son pays et sa pastille de qualité ;
la barre du haut dit où l'on est et revient d'un niveau ; le bouton retour
du système aussi. En bas, la ligne de lecture : logo, station, et dessous
le titre du flux ou l'état, « Connexion… », « En direct », « En pause »,
« Injoignable (code) ». Elle reste pendant qu'on navigue. La station en
cours est marquée dans la liste. Un fichier seul est la racine ; plusieurs,
la racine montre un dossier par fichier.

**Le lecteur** (`lecture/ServiceLecture.kt`) : un ExoPlayer dans une
`MediaSession` de Media3, service en avant-plan de type `mediaPlayback`.
La session donne la notification avec lecture, pause et suivant, l'écran de
verrouillage, les boutons du casque, la pause quand on débranche les
écouteurs, et le verrou réseau pour tourner écran éteint. Le user agent est
« Unicorn-Radio/version ». Les redirections `http` vers `https` sont
suivies. Douze secondes sans réponse ou sans octet valent un échec, plutôt
qu'une mise en mémoire sans fin. Appuyer sur une station envoie toute la
liste où l'on est au lecteur : « suivant » y passe à la station d'après.
Le titre ICY arrive par les métadonnées du lecteur et remplace le `title`
de l'élément, la notification lit « titre — station ». Le journal des essais
va dans Logcat, étiquette `UnicornRadio`.

**Dépendances ajoutées** : Media3 1.11.1 (`exoplayer`, `exoplayer-hls`,
`session`), RecyclerView 1.4.0, Material 1.14.0 pour le thème et la barre,
Lifecycle 2.11.0 pour lire le fichier hors du fil principal, Coil 2.7.0
pour les logos. Le manifeste gagne `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` et `POST_NOTIFICATIONS`,
demandée à la première ouverture. Version 0.2.0, code 2.

## Ce qui reste pour plus tard

- Les déclinaisons d'une station essayées dans l'ordre quand la première
  échoue (la feuille de route le prévoit) : ici, une station qui échoue est
  dite injoignable, et on appuie sur la voisine.
- La reprise après une pause repart de la mémoire tampon, donc en retard
  sur le direct ; relancer la station rattrape.
- Les stations qui exigent un pays ou un user agent précis.

## Fichiers

`app/build.gradle.kts`, `gradle/libs.versions.toml`,
`app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/org/unicornradio/app/`
(`MainActivity.kt`, `ListeAdaptateur.kt`, `modele/Modele.kt`,
`fichier/LectureFichier.kt`, `fichier/Fichiers.kt`,
`lecture/ServiceLecture.kt`, `lecture/Elements.kt`), `app/src/main/res/`
(`layout/`, `menu/`, `drawable/`, `values*/strings.xml`, `values/themes.xml`).

## Vérification

Faite le 2026-10-25 au soir sur le téléphone, hors wifi : « ça joue, avec
le titre, écran éteint aussi. »

À relancer : installer l'APK `pkg/unicorn-radio-0.2.0-debug.apk` par-dessus
l'ancien, wifi coupé.

- Ouvrir l'application : l'écran vide propose « Ouvrir un fichier… ».
- Choisir le fichier d'export : un message dit son nom et son nombre de
  stations ; la liste montre les favoris puis les dossiers, les dossiers
  s'ouvrent, le retour remonte.
- Appuyer sur une station en `http` : la ligne du bas passe par
  « Connexion… » puis montre le titre en cours, ou « En direct » si la
  station n'en envoie pas. La station est marquée dans la liste.
- Éteindre l'écran : le son continue. L'écran de verrouillage montre la
  station et ses boutons ; « suivant » passe à la suivante de la liste.
- La notification est là, avec lecture, pause, suivant.
- Depuis le gestionnaire de fichiers, « Ouvrir avec » sur le JSON : l'application
  le prend et l'affiche.
