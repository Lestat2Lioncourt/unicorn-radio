# Historique

Une entrée par chantier, la plus récente en tête. Le détail de chacun est
dans `correctifs/`.

## 2026-11-12 — 039 · La licorne dans les notifications

Une licorne d'une couleur, dessinée pour la barre d'état, remplace
l'icône de lecture de Media3 dans toutes les notifications ; le logo en
couleurs en grande icône pour les alarmes et ce qui est programmé.

## 2026-11-12 — 038 · Ce qui est programmé, dans les notifications

Une notification discrète par compte à rebours lancé, le temps qui reste
défilant, « Annuler » ; une pour le prochain réveil, son jour et son
heure, « Ne pas sonner cette fois ». Un appui ouvre la liste.

## 2026-11-12 — 037 · Les réveils et les comptes à rebours

Autant de réveils et de comptes à rebours qu'on veut, deux entrées du
menu, chacun son nom, son interrupteur, ses réglages d'un appui. Le son :
une station ou une sonnerie du téléphone ; le volume progressif à
cocher. Sans nom, il sert une fois. Changer l'heure d'un réveil le remet
en marche.

## 2026-11-12 — 036 · La trace des plantages

Un plantage est écrit dans un fichier ; à l'ouverture suivante, sa trace
est montrée une fois, à copier ou envoyer.

## 2026-11-12 — 035 · Le réveil radio

« Réveil radio… » dans le menu : une heure, des jours, une station de mes
listes. Elle démarre une minute avant, en muet, le temps que la
publicité passe ; à l'heure, le son monte du silence au plein en une
minute, au volume du réveil ; la sonnerie du téléphone si elle ne joue
pas ; arrêt seul au bout d'une heure.

## 2026-11-12 — 034 · Le widget

Un widget pour l'écran d'accueil : ce qui joue ou la dernière écoute, le
titre en cours, précédent, lecture ou pause, suivant, arrêter. Application fermée,
« lecture » reprend la dernière écoute.

## 2026-11-12 — 033 · La dernière écoute

À l'ouverture, la dernière station ou le dernier épisode attend en pause
sur la ligne de lecture, un appui la relance. Un casque ou les commandes
d'Android la reprennent aussi, application fermée.

## 2026-11-12 — 032 · Le paysage

En paysage, à gauche les icônes, le titre en entier, les onglets et les panneaux de recherche,
à droite la liste sur toute la hauteur, en bas le lecteur sur toute la
largeur. Tourner le téléphone ne perd plus l'endroit où l'on est. Le
clavier ne couvre plus l'écran en paysage.

## 2026-11-12 — 031 · L'import et le serveur quittent la barre

L'icône « Importer une liste… » de la barre du haut est retirée :
l'entrée du menu « … » et le bouton de l'écran vide restent. L'icône du
serveur devient « Serveur… », en tête du menu « … ». La barre ne garde
que la loupe et les drapeaux, et le nom de l'application y tient.

## 2026-11-11 — 030 · Reprendre au retour du réseau

Une lecture tombée sur une coupure de réseau repart seule quand Android
voit le réseau revenu, à la même position pour un épisode, avec un
message ; pas après une pause ou un arrêt, ni au-delà de trente minutes.
Hors ligne, l'annuaire n'est plus consulté pour rien.

## 2026-11-11 — 029 · La vitesse des épisodes, reculer 15 s, avancer 30 s

Sur la ligne d'un épisode, « 1× » choisit la vitesse, de 0,75× à 2×, la
même pour tous les épisodes et gardée ; les stations restent à 1×.
Avancer saute désormais 30 secondes, reculer toujours 15 ; les deux
icônes portent leur nombre.

## 2026-11-11 — 028 · Le droit d'auteur

« © 2026 Lestat2Lioncourt » dans « À propos » et dans la section Licence
du README, comme la GPL le recommande.

## 2026-11-11 — 027 · À propos

« À propos », en bas du menu « … » : ce que fait l'application, sans
compte ni identifiant, aucune collecte, à qui elle parle, la licence, le
nombre de téléchargements ; « Code source » ouvre le dépôt.

## 2026-11-11 — 026 · Le nombre de téléchargements

Sous le QR code de l'invitation, « Déjà téléchargée N fois » : le total
des téléchargements des APK de toutes les releases, que GitHub compte.
Rien sans réseau. Le même total en badge dans le README.

## 2026-11-11 — 025 · Ouvrir dans les favoris, ranger en voyant où

Au lancement, l'écran s'ouvre dans « Mes favoris » ; la flèche remonte à
l'arborescence. « Ranger dans… » et « Déplacer vers… » montrent tous les
dossiers, ceux qui contiennent déjà la station en gris et sans effet.

## 2026-11-11 — 024 · La sauvegarde automatique

« Sauvegarde automatique… », dans le menu : un fichier créé une fois à
l'emplacement choisi, de préférence un espace en ligne, puis réécrit de
lui-même à chaque changement de la liste. Un rappel discret une seule
fois ; un emplacement perdu est dit une fois.

## 2026-11-11 — 023 · Invitation QRCode

Dans le menu « … », « Invitation QRCode » montre le QR code de la page de
la dernière version, pour donner l'application à quelqu'un qu'on croise ;
« Envoyer le lien » ouvre le partage d'Android.

## 2026-11-11 — 022 · Précédent et suivant

Pendant un épisode, le lecteur porte toute la liste de lecture :
précédent et suivant passent d'un épisode à l'autre, depuis l'écran, la
notification et le casque, et l'épisode atteint reprend à sa position. La
liste gardée suit l'ordre du lecteur. Deux boutons sur la ligne de
lecture, pour les stations aussi.

## 2026-11-11 — 021 · Retrouver une station

Une station qui ne démarre pas est redemandée à l'annuaire : une autre
adresse de flux fait repartir la lecture, un identifiant disparu est suivi
par le nom quand il est unique, favoris et dossiers suivent ; sinon le
pourquoi est dit. Appui long, « Retrouver dans l'annuaire… » pour choisir
soi-même parmi les homonymes. Le pendant du 232 du bureau, pour qui n'a
pas de serveur ; apparié, les gestes du contrat portent le remplacement.

## 2026-11-10 — 020 · Le serveur gardé en mémoire

L'adresse du serveur qui a répondu sert telle quelle, sans lui redemander
son identité avant chaque poussée de position ; la recherche ne reprend
qu'en cas d'échec, puis un second essai, ou quand Android signale un autre
réseau. Le PC ne voit plus une demande d'identité toutes les dix secondes.

## 2026-11-09 — 019 · La mise à jour vérifiée à chaque ouverture

Plus d'attente d'un jour : la dernière release est lue à chaque retour au
premier plan, et proposée si elle est plus récente. « Plus tard » écarte
cette version-là jusqu'au lendemain ; le menu propose toujours.

## 2026-11-09 — 018 · Une vignette par épisode

Chaque ligne d'épisode a sa vignette, l'image propre de l'épisode quand le
flux en donne une, sinon celle du podcast, les initiales si rien ne charge ;
l'épisode en cours montre la même. Comme sur le bureau le même jour.

## 2026-11-09 — 017 · La nuit appartient à la veille

Un épisode publié avant 1 h 30, heure locale, est rangé au jour d'avant :
l'intégrale d'une émission du soir, publiée après minuit, retrouve son
jour. La date gardée ne change pas. La même règle que le bureau.

## 2026-11-09 — 016 · Télécharger un épisode

Par appui long, « Télécharger » : Android télécharge, la ligne dit
« Téléchargé », et l'épisode joue sans réseau, à la main ou à la suite
dans la liste de lecture. « Supprimer le téléchargement » le reprend.

## 2026-11-09 — 015 · Exporter mes radios, importer une liste

« Exporter mes radios… » écrit le fichier d'échange, favoris, dossiers et
podcasts, là où l'on veut. « Importer une liste… » remplace « Ouvrir un
fichier… » : le fichier est versé dans ma liste, rien d'effacé, rien
doublé, et apparié, les ajouts partent au serveur comme autant de gestes.
Les fichiers ouverts par les versions d'avant sont versés une fois.

## 2026-11-09 — 014 · Précédemment

Un appui sur la ligne de lecture, pendant qu'une station joue : les titres
qu'elle a annoncés, avec leur heure, notés par le service au fil de
l'écoute ; apparié, ceux que le serveur de la maison a vus s'y ajoutent.

## 2026-11-09 — 013 · La minuterie d'extinction, le site, l'adresse du flux

Une icône dans la ligne de lecture : 15 à 90 minutes, puis la pause ; les
minutes qui restent dessous. Et dans le menu d'une station, « Ouvrir le
site de la station » et « Copier l'adresse du flux », comme sur le bureau.

## 2026-11-09 — 012 · La liste de lecture des podcasts

« Lire ensuite » par appui long sur un épisode ; la liste de lecture en
tête de mes podcasts, précédents, en cours, à suivre, gardée sur le
téléphone ; l'enchaînement tout seul à la fin de chaque épisode ; « Vider
la liste ». Le troisième palier des podcasts.

## 2026-10-29 — 011 · Le titre en cours, enfin affiché

Le titre que la station annonce dans son flux ne s'était jamais affiché :
le lecteur garde la fiche de la station par-dessus ce qui vient du flux, et
la fiche portait le nom en guise de titre. Le service pose maintenant le
titre du flux dans la fiche lui-même, sans couper le son ; barre,
notification et écran de verrouillage le montrent. Les titres longs
défilent.

## 2026-10-29 — 010 · Proposer la mise à jour

Après la première version publiée, la 0.9.3 : à l'ouverture, au plus une
fois par jour, l'application regarde la dernière release du dépôt GitHub
et, s'il y a plus récent, montre la note dans sa langue avec « Installer »
et « Plus tard ». Installer télécharge l'APK et ouvre l'installeur
d'Android, qui demande encore. « Mise à jour… » dans le menu vérifie tout
de suite.

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
