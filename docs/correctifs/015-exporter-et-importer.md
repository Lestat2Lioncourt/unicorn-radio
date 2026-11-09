# 015 — Exporter mes radios, importer une liste

Le 2026-11-09, dans le bloc de sept. Le palier 3 de la feuille de route,
décidé le 2026-10-27 avec le bureau, qui a les deux entrées : rendre le
fichier d'échange, et verser un fichier reçu dans ma liste au lieu de
l'ouvrir en lecture seule à côté.

## Ce qui est fait

**« Exporter mes radios… »**, dans les trois points de l'onglet Radio :
le fichier d'échange, l'enveloppe réduite du format, `application:
"unicorn-radio"`, `version: 2`, `exportedAt`, et la section `radio`
entière, favoris, dossiers et podcasts, telle qu'elle est sur le disque
(`MesRadios.section`, `EcritureFichier.enveloppe`). Le sélecteur d'Android
demande où l'écrire, sous le nom `unicorn-radios-AAAA-MM-JJ.json`, le même
que sur le bureau. Rien à exporter, un mot le dit.

**« Importer une liste… »**, à la place de « Ouvrir un fichier… », l'icône
de la barre et le bouton de l'écran vide, et « Ouvrir avec » depuis les
fichiers du téléphone : le fichier est lu, vérifié, puis **versé dans ma
liste** (`MesRadios.verser`) en une seule écriture, comme l'import du
bureau : rien d'effacé, ce qui manque s'ajoute à la suite, favoris,
dossiers avec leurs parents, stations des dossiers, podcasts ; un fichier
rejoué ne double rien. Apparié, chaque ajout est un geste du journal,
`favori.ajouter`, `dossier.creer`, `dossier.ajouter`, `podcast.ajouter`,
dans l'ordre, envoyés au serveur dans la foulée ; sans serveur, la liste
locale les reçoit. Le mot de la fin dit combien de stations et de podcasts
sont entrés. Un fichier de configuration entier passe aussi, seule sa
section `radio` compte.

**Les fichiers déjà ouverts** par les versions d'avant, gardés dans
`fichiers/` et affichés en lecture seule à côté de la liste, sont versés
une fois au premier démarrage, puis oubliés ; un mot le dit. Apparié, ils
partent au serveur comme un import ; les stations qu'il a déjà n'y changent
rien. La racine n'a plus que « Mes favoris » et mes dossiers, apparié ou
non.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/MainActivity.kt`,
`fichier/Fichiers.kt` (réécrit : `lire`, `anciens`, `oublier`),
`fichier/EcritureFichier.kt` (`enveloppe`), `serveur/MesRadios.kt`
(`section`, `verser`), `res/menu/principal.xml`,
`res/layout/activity_main.xml`, `res/values*/strings.xml`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.1.0-debug.apk` par-dessus.

- Au premier démarrage, si un fichier avait été ouvert avant : « Fichiers
  ouverts avant, versés dans ma liste : N stations », et il n'apparaît
  plus à la racine ; apparié, les stations qui manquaient sont sur le PC
  après le rafraîchissement.
- Trois points, « Exporter mes radios… », un dossier du téléphone : un
  fichier `unicorn-radios-2026-11-09.json`, « Fichier écrit : N stations ».
  Ouvert dans Réglages d'Unicorn-TV, l'import l'accepte.
- L'icône fichier de la barre, « Importer une liste… », un export du PC :
  « Liste importée : N stations, M podcasts ajoutés », les nouveaux à la
  suite de leurs listes, rien de doublé ; le même fichier une seconde
  fois : « 0 stations, 0 podcasts ajoutés ».
- « Ouvrir avec » depuis le gestionnaire de fichiers : pareil.
- Un fichier qui n'est pas un export : « Fichier illisible : … ».
