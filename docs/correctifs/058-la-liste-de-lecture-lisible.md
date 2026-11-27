# 058 — La liste de lecture lisible, et le lecteur sur deux rangs

Le 2026-11-27. Trois demandes de l'utilisateur.

- « Dans les podcasts, si on rajoute un épisode à lire ensuite, ça
  n'apparaît pas clairement dans la liste qu'on l'a sélectionné. Regarde
  ce qui se passe dans unicorn tv, c'est plus lisible. »
- « Si on ouvre la liste de lecture des épisodes, […] ce serait top si on
  pouvait avoir “lire la liste à partir de”, […] par exemple si on s'est
  endormi pendant que la liste jouait. »
- « Dans le player, mes 5 boutons alignés tronquent la place pour
  afficher le nom de la radio. On pourrait mettre le cœur et le chrono en
  dessous de précédent, lecture et suivant ? Et éventuellement afficher la
  radio sur 2 lignes si le nom est tronqué ? »

## Le défaut

- Un épisode à la suite ne se signalait que par « À la suite », en petit
  dans la ligne de détail, parmi la durée et « Téléchargé » ; et on ne l'y
  mettait que par l'appui long. Sur le bureau, chaque épisode a un bouton
  rond « + », plein quand l'épisode est dans la file, et un clic suffit.
- Dans la liste de lecture, toucher un épisode passé le jouait, puis
  sautait à « À suivre » : les passés d'après lui, ceux qu'on a manqués en
  s'endormant, n'étaient pas rejoués.
- Le lecteur alignait cœur, minuterie, précédent, lecture et suivant sur
  la ligne du nom, qui tenait sur une ligne : un nom long était coupé.

## Ce qui change

- **Le « + » des épisodes**, comme sur le bureau : un rond au bout de
  chaque ligne d'épisode d'un podcast. Un appui met l'épisode à la suite ;
  il devient plein, de la couleur de l'application, avec une coche ; un
  nouvel appui l'en retire. L'appui long garde son menu. La mention « À la
  suite » quitte la ligne de détail. Pas de « + » dans la liste de lecture
  elle-même, où tout y est.
- **« Lire la liste à partir d'ici »**, dans le menu de l'appui long de la
  liste de lecture : cet épisode, puis tous ceux qui sont sous lui, dans
  l'ordre de la liste, les passés d'après lui compris, celui qui était en
  cours, ceux à suivre. Ceux du dessus restent les précédents. Chacun
  reprend où on l'avait laissé ; un épisode écouté jusqu'au bout repart du
  début. (Depuis 1.9.0.4, c'est l'appui simple : voir plus bas.)
- **Le lecteur sur deux rangs** : précédent, lecture, suivant en haut ; le
  cœur et la minuterie dessous, les minutes à côté de l'icône. Le nom de
  la station ou de l'épisode passe sur deux lignes s'il le faut.

Version 1.9.0.1, code 89, en essai.

## Le premier essai

En 1.9.0.1, après « Lire la liste à partir d'ici », l'épisode lancé
paraissait deux fois : sous « En cours » et en tête d'« À suivre ».
L'écran se redessine dès que le lecteur a changé d'épisode, avant que le
service n'ait réécrit la liste gardée, où il était encore à suivre. La
liste de lecture ne montre plus l'épisode en cours ailleurs que sous « En
cours » (1.9.0.2, code 90). Le même cas se voyait déjà en touchant un
épisode d'« À suivre ».

Puis : « Si je relis un épisode dans la liste et que je fais plusieurs
fois épisode suivant, on ne voit pas l'épisode sélectionné dans la liste
car la sélection est hors écran. » La liste de lecture garde maintenant
l'épisode en cours à l'écran : quand il change, elle défile en douceur
jusqu'à lui, au plus près, seulement s'il est sorti de la vue ; à
l'ouverture de la liste, elle y va d'un coup. Pas dans la liste des
épisodes d'un podcast (1.9.0.3, code 91).

Puis : « Quand je lance un épisode dans la liste, il se lance mais il
passe tout en bas, donc hors écran, dans En cours, au lieu de rester à sa
place. C'est très déroutant. » L'appui simple jouait l'épisode touché
après tous les autres passés : il descendait sous eux. Il fait maintenant
ce que faisait « Lire la liste à partir d'ici » : l'épisode joue à sa
place, ceux du dessus restent les précédents, la lecture continue avec
ceux du dessous, les passés compris. L'entrée du menu, devenue la même
chose, s'en va ; « Lire » du menu fait pareil (1.9.0.4, code 92).

## Fichiers

`ListeAdaptateur.kt`, `MainActivity.kt`, `res/layout/ligne_episode.xml`,
`res/layout/activity_main.xml`, `res/drawable/rond_suite.xml` et
`rond_suite_plein.xml` (nouveaux), `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.9.0.4-debug.apk`.

- Ouvrir un podcast : un « + » au bout de chaque épisode. Un appui : le
  rond se remplit, une coche ; « Liste de lecture » compte un de plus. Un
  second appui : il se vide, l'épisode quitte la liste.
- Liste de lecture de trois épisodes, le premier fini, le deuxième en
  cours : appui sur le premier, qui reste à sa place :
  il repart du début, puis le deuxième reprend où il en était, puis le
  troisième. L'épisode lancé n'est que sous « En cours ».
- Le lecteur avec une station au nom long : précédent, lecture, suivant
  en haut, le cœur (quand un titre est annoncé) et la minuterie dessous ;
  le nom sur deux lignes. Minuterie programmée : les minutes à côté de
  l'icône.
- Pendant un épisode : la barre d'avancement dessous, inchangée.
- Une liste de lecture plus longue que l'écran : « suivant » plusieurs
  fois, la liste suit l'épisode en cours. Fermer et rouvrir la liste :
  elle s'ouvre sur lui.
