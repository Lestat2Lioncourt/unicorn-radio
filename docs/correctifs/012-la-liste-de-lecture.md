# 012 — La liste de lecture des podcasts

Le 2026-11-09, dans un bloc de sept chantiers demandé d'un mot : « fais
tout d'un bloc ». Celui-ci était le troisième palier des podcasts, annoncé
depuis le 008 : « pouvoir stacker un autre épisode dans la liste de lecture
des podcasts », comme sur le bureau.

## Ce qui est fait

**« Lire ensuite »** par appui long sur un épisode : il va à la suite de ce
qui joue. Si un épisode joue déjà, il entre aussi dans la file du lecteur,
qui enchaîne tout seul à la fin du précédent ; sinon il attend dans la
liste, et le prochain épisode lancé l'emmène derrière lui. La ligne porte
« À la suite », et l'appui long propose alors « Retirer de la file ».

**La liste de lecture** (`podcasts/FileDEcoute.kt`, `file-d-ecoute.json`) :
en tête de « Mes podcasts », comme la première entrée du volet sur le
bureau, avec son compte ; et d'un appui sur les textes de la ligne de
lecture quand un épisode joue. Trois sections : les **précédents**,
atténués, vingt au plus, du plus ancien au plus récent ; l'épisode **en
cours** ; ceux **à suivre**, dans l'ordre où ils joueront. Chaque ligne
garde sa vignette, sa durée, son trait d'avancement et « Reprendre à » ou
« Écouté ». Appuyer joue ; l'appui long retire ; « Vider la liste », dans
les trois points, enlève tout, l'épisode en cours compris, arrêté d'abord,
après confirmation, comme le bureau le fait depuis le 2026-10-19.

**Le service note les départs** : à chaque épisode qui démarre, à la main
ou à la suite du précédent, `ServiceLecture.suivi.onMediaItemTransition`
le sort de ceux à suivre et le met en fin des précédents, tout de suite,
avant que l'écran ne relise. L'épisode entier voyage dans les extras de
l'élément du lecteur (`CLEF_EPISODE`), pour que le service le retrouve.

**Gardée sur le téléphone** : la liste survit à la fermeture de
l'application ; le lecteur, lui, repart vide, et le prochain épisode lancé
reprend la liste derrière lui. Rien n'est partagé avec le serveur, comme
sur le bureau où elle vit dans le navigateur.

Une seule file : jouer une station remplace la file du lecteur par les
stations de la liste, et la liste de lecture reste intacte sur le disque.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/podcasts/FileDEcoute.kt`
(nouveau), `podcasts/Podcast.kt` (`versJson`, `depuisJson`, `clef`),
`podcasts/Elements.kt`, `lecture/ServiceLecture.kt`, `ListeAdaptateur.kt`
(`Ligne.Liste`, `Ligne.Titre`, `montrerLaFile`), `MainActivity.kt`,
`res/drawable/ic_liste.xml` (nouveau), `res/menu/principal.xml`,
`res/values*/strings.xml`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.1.0-debug.apk` par-dessus.

- Un podcast ouvert, appui long sur un épisode, « Lire ensuite » : la
  ligne dit « À la suite » ; l'appui long propose « Retirer de la file ».
- Onglet Podcasts, champ vide : « Liste de lecture » en tête, « 1 épisode ».
  L'ouvrir : l'épisode sous « À suivre ». Appuyer : il joue.
- Un autre épisode joué à la main, puis « Lire ensuite » sur un troisième :
  à la fin du deuxième, le troisième part tout seul ; la liste montre le
  deuxième sous « Précédents », le troisième « En cours ».
- Pendant qu'un épisode joue, appuyer sur ses textes en bas : la liste.
- Fermer l'application, la rouvrir : la liste est là ; jouer un épisode
  à suivre : il part, les autres derrière.
- « Vider la liste » dans les trois points : confirmation, tout s'en va,
  la lecture s'arrête.
- Une station lancée pendant la liste : elle joue ; la liste est intacte.
