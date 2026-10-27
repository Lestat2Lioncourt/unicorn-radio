# 007 — Ranger sur le téléphone

Le 2026-10-27, le troisième chantier de la synchronisation, et le palier 3
de la feuille de route moins l'écriture du fichier : les dossiers, pour
les radios seulement, « on implémente pour les radios, pas pour les
podcasts ». L'ergonomie reprend celle du bureau, par menus, sans
glisser-déposer au doigt.

## Ce qui est fait

**Une seule liste modifiable** (`serveur/MesRadios.kt`), au format
d'échange : apparié, la copie du serveur, chaque action un geste du
journal comme au 006 ; non apparié, la liste locale `mes-favoris.json`,
qui gagne des dossiers, mêmes actions, sans journal. L'ancienne classe des
favoris locaux disparaît, c'est le même objet avec un autre fichier. Les
fichiers importés restent en lecture seule. Les opérations sur la section
JSON font ce que le serveur fera : créer un dossier et ses parents
manquants, chacun à la fin de ses frères ; renommer le dernier élément du
chemin, descendants compris ; supprimer un dossier et tout ce qu'il
contient ; ajouter à la fin d'un dossier, créé au besoin, rien si déjà là ;
retirer ; déplacer avec la fiche telle quelle ; ordonner, celles de la
liste dans cet ordre puis les autres dans leur ordre d'avant, `ordre`
réécrit.

**Les noeuds savent à qui ils sont** (`modele/Modele.kt`) : un dossier
porte son chemin complet, posé par le lecteur du fichier, et un noeud de
ma liste est marqué `mien`, favoris et dossiers, par opposition aux
fichiers importés. La racine, apparié : « Mes favoris » puis mes
dossiers ; non apparié : « Mes favoris » s'il y en a, mes dossiers, puis
les fichiers importés. Ma liste est toujours sous le nom de l'application,
un seul fichier importé reste la racine lui-même.

**L'appui long sur une station**, partout, remplace le dialogue de retrait
par un menu : ajouter aux favoris ou en retirer ; « Ranger dans… », mes
dossiers par leur chemin, « Musique / Jazz », et « Nouveau dossier… » en
bas, créé dans le dossier où l'on est, sinon à la racine ; dans un de mes
dossiers, « Déplacer vers… » et « Retirer du dossier » ; dans « Mes
favoris » ou un de mes dossiers, « Monter » et « Descendre », qui
envoient un geste « ordonner » sur toute la liste.

**L'appui long sur un de mes dossiers** : « Nouveau sous-dossier… »,
« Renommer », « Supprimer » avec confirmation, le contenu avec. Sur un
fichier importé, le retrait d'avant. Un nom vide ou avec une barre
oblique n'est pas pris.

**Les gestes** (`serveur/Journal.kt`) : les sept du contrat pour les
dossiers et les deux « ordonner », en plus des deux des favoris. Tous
passent par la même fonction `agir` : appliqué à ma liste, noté au
journal si apparié, l'écran relu, le serveur prévenu dans la foulée.

**Pas dans ce chantier** : le glisser-déposer, et l'écriture du fichier
d'échange pour qui n'a pas de serveur, qui reste à part.

Version 0.7.0, code 8. Aucune dépendance ajoutée.

## L'ordre des dossiers : 0.7.1

Le 0.7.0 laissait de côté l'ordre des dossiers entre eux, que le contrat
ne couvrait pas. Le serveur a ajouté le geste `dossiers.ordonner` le jour
même : le chemin du parent, vide à la racine, et les noms des dossiers
frères dans l'ordre. Le 0.7.1, code 9, le branche : « Monter » et
« Descendre » dans le menu d'un de mes dossiers, parmi ses frères du
niveau où l'on est, le geste avec tous les noms dans le nouvel ordre,
appliqué sur place comme les autres, `ordre` réécrit.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/serveur/MesRadios.kt`,
`Journal.kt`, `MainActivity.kt`, `ServeurActivity.kt`,
`modele/Modele.kt`, `fichier/LectureFichier.kt`,
`fichier/MesFavoris.kt` (supprimé), `res/values*/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.7.1-debug.apk` par-dessus
l'ancien, apparié ; côté PC, la page radio ouverte.

- Appui long sur une station des résultats → « Ranger dans… » → « Nouveau
  dossier… » → un nom : « Rangée dans <nom> », le dossier à la racine avec
  la station, et sur le PC après rechargement.
- Depuis « Mes favoris », appui long → « Ranger dans… » → le dossier
  existant : la station y est aussi, et reste favorite.
- Dans le dossier, appui long sur une station → « Déplacer vers… » → un
  autre dossier : partie de l'un, arrivée à la fin de l'autre, des deux
  côtés.
- Dans le dossier, « Retirer du dossier » : retirée ici et sur le PC.
- Dans « Mes favoris », « Monter » et « Descendre » : l'ordre change ici
  et sur le PC. Pareil dans un dossier.
- Appui long sur un dossier → « Nouveau sous-dossier… » : créé dedans ;
  « Renommer » : renommé, sous-dossiers compris, des deux côtés ;
  « Supprimer » : confirmation, puis disparu avec son contenu, des deux
  côtés.
- Wifi coupé, un dossier créé et une station rangée : l'écran Serveur dit
  « 2 changements en attente d'envoi » ; wifi revenu, application au
  premier plan, tout est sur le PC.
- Détaché : les mêmes menus sur la liste locale ; les dossiers créés
  restent après fermeture de l'application ; un fichier importé n'a pas
  ces menus, seulement son retrait.
