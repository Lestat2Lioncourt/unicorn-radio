# Historique

Une entrée par chantier, la plus récente en tête. Le détail de chacun est
dans `correctifs/`.

## 2026-10-27 — 009 · Mes podcasts, et les épisodes par date

Le deuxième palier des podcasts : la liste suivie du serveur sous l'onglet
Podcasts, avec le dernier épisode de chacun ; l'étoile pour suivre ou ne
plus suivre, monter et descendre, chacun un geste du journal ; le collage
d'une adresse de flux. Et les épisodes présentés par date, chaque date
repliable, avec un bouton pour tout déplier ou tout replier et une
recherche dans les épisodes, comme sur le bureau le même jour.

## 2026-10-27 — 008 · Les podcasts : chercher et écouter

Deux onglets en tête, Radio et Podcasts, comme sur le bureau. L'onglet
Podcasts cherche dans l'annuaire d'Apple, ouvre un podcast en lisant son
flux RSS sur le téléphone, et joue ses épisodes avec barre d'avancement,
temps restant, quinze secondes en arrière et en avant, et la reprise où
l'on en était, gardée sur le téléphone et, apparié, partagée avec le PC.
Premier des quatre paliers ; « Mes podcasts », la file d'écoute et le
téléchargement suivent.

## 2026-10-27 — 007 · Ranger sur le téléphone

Les dossiers, pour les radios : par appui long, ranger une station dans un
dossier, l'en retirer, la déplacer, monter et descendre ; créer, renommer,
supprimer un dossier. Apparié, chaque action est un geste du journal ; sans
serveur, la liste locale gagne des dossiers, mêmes menus. Les fichiers
importés restent en lecture seule.

## 2026-10-27 — 006 · Renvoyer les favoris

Le journal des gestes : chaque étoile ou retrait, apparié, est appliqué
sur place, écrit sur le disque, et envoyé au serveur dès qu'il répond,
toujours avant de tirer sa copie, pour qu'un geste fait hors ligne ne soit
jamais écrasé. Les favoris locaux sont versés au serveur à l'appariement.
Les dossiers viennent ensuite.

## 2026-10-26 — 005 · Apparier et tirer

Le premier chantier de la synchronisation : l'écran Serveur apparie le
téléphone à son Unicorn-TV, par le QR code ou l'adresse et le code tapés,
et tire sa liste, qui prend la place des fichiers ; mise à jour à chaque
retour au premier plan, « Se détacher », et le retour aux fichiers quand
le serveur révoque. Les gestes du téléphone vers le serveur viennent
ensuite.

## 2026-10-26 — 004 · Les deux langues

Comme sur le bureau : deux drapeaux en haut à droite, français et anglais,
l'actif net et l'autre atténué ; appuyer change la langue de l'écran sans
couper le son, et le choix est gardé. Et le logo devant le nom, en haut à
gauche.

## 2026-10-26 — 003 · Chercher dans l'annuaire

Le palier 2 : la loupe cherche dans Radio Browser, par nom, pays et style,
directement depuis le téléphone, avec les mêmes identifiants que le fichier
d'échange ; l'étoile met une station dans « Mes favoris », un fichier à part
au format d'échange ; chaque écoute est comptée par l'annuaire.

## 2026-10-25 — 002 · Lire le fichier, jouer

Le palier 1 : l'application ouvre un fichier d'export, par « Ouvrir
avec » ou depuis les fichiers du téléphone, montre ses favoris et ses
dossiers, une ligne par station, et joue : Media3 en service d'arrière-plan,
avec notification, écran de verrouillage, boutons du casque, et le titre en
cours par les métadonnées ICY. Le `http` en clair joue, l'AAC brut et le HLS
aussi.

## 2026-10-25 — 001 · Le socle

Le dépôt, public sous GPL-3.0 comme Unicorn-TV, et une application vide qui
se construit et s'installe : la chaîne Gradle, Kotlin et le SDK Android en
ligne de commande, sans Android Studio. Le format d'échange avec Unicorn-TV
est décrit dans `format-d-echange.md`. Pourquoi ce projet existe : voir
`ROADMAP.md`.
