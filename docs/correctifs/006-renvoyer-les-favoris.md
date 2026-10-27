# 006 — Renvoyer les favoris

Le 2026-10-27, le deuxième chantier de la synchronisation, après
l'appariement et le tirage (005) : ce que le téléphone change part au
serveur, par le journal des gestes de `format-d-echange.md`, « Renvoyer ».
D'abord les favoris, ajouter et retirer, les deux gestes que
l'application connaissait déjà. Les dossiers sont le chantier suivant.

## Ce qui est fait

**Le journal** (`serveur/Journal.kt`) : `gestes.json` dans le stockage
privé, un tableau de gestes dans l'ordre, chacun avec son identifiant
(UUID), son horodatage UTC et ses champs, la fiche complète pour un ajout,
l'identifiant de station pour un retrait. Écrit à l'instant même du geste.
Un journal illisible est repris à vide, en le disant au journal système.

**Appliqué tout de suite** (`serveur/MesRadios.kt`) : la copie locale
reçoit le geste sur place, à la fin des favoris ou retiré, comme le
serveur le fera, pour que l'écran suive sans attendre. La copie garde un
cache de sa racine et de ses favoris, invalidé à chaque écriture.

**La synchronisation** (`serveur/Synchro.kt`) : trouver le serveur
attendu, lui envoyer le journal s'il y en a, par paquets de deux mille au
plus, sortir du journal les gestes envoyés, acceptés, déjà vus ou
refusés, puis tirer la copie. Toujours dans cet ordre : un geste fait
hors ligne n'est jamais écrasé par la copie. Un refus est écrit au
journal système avec sa raison, et compté à l'écran. L'écran principal
et l'écran Serveur passent tous deux par là ; un tirage demandé pendant
un autre est refait à la fin.

**Quand** : à chaque retour de l'application au premier plan, juste après
chaque geste, à l'appariement et sur « Mettre à jour ». Hors ligne, le
geste attend dans le journal ; l'écran Serveur dit combien de changements
attendent d'être envoyés.

**À l'écran, apparié** : l'étoile revient dans les résultats de recherche
et l'appui long dans « Mes favoris », retirés exprès au 005, tous deux
devenus des gestes.

**À l'appariement** : les favoris locaux de « Mes favoris » sont versés au
journal comme autant d'ajouts, envoyés avec le premier tirage ; une
station déjà dans les favoris du serveur est acceptée sans rien changer.
Le fichier local n'est pas effacé : il revient tel quel au détachement.
Le détachement et la révocation effacent le journal, dont les gestes
n'auraient plus de destinataire.

Version 0.6.0, code 7. Aucune dépendance ajoutée.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/serveur/Journal.kt`, `Synchro.kt`
(nouveaux), `MesRadios.kt`, `ClientServeur.kt`, `MainActivity.kt`,
`ServeurActivity.kt`, `res/layout/activity_serveur.xml`,
`res/values*/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.6.0-debug.apk` par-dessus
l'ancien, apparié ; côté PC, Unicorn-TV sur son 222, page radio ouverte.

- Recherche, une étoile sur une station : « Ajoutée aux favoris », et sur
  le PC, après rechargement de la page, la station en fin de favoris.
- Appui long sur une station de « Mes favoris », « Retirer » : elle
  disparaît ici et sur le PC.
- Wifi coupé, une étoile : la station apparaît dans « Mes favoris » ;
  l'écran Serveur dit « 1 changement en attente d'envoi ». Wifi revenu,
  application remise au premier plan : le changement est sur le PC, et
  l'écran Serveur dit « Rien en attente d'envoi ».
- Pendant la coupure, un favori ajouté sur le PC : après le retour, les
  deux changements des deux côtés.
- Une station déjà favorite sur le PC, étoilée depuis le téléphone après
  un tirage : l'étoile est déjà pleine, l'appui la retire.
- « Se détacher », puis réapparier avec des favoris locaux d'avant
  l'appariement : ils sont sur le PC après l'appariement, sans doublon.
