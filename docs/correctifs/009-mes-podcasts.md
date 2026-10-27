# 009 — Mes podcasts, et les épisodes par date

Le 2026-10-27, le deuxième palier des podcasts, enchaîné sur le premier :
la liste suivie, l'étoile pour suivre, le collage d'une adresse de flux.
Et une consigne de présentation donnée en route, faite des deux côtés le
même jour (223 côté PC) : « présenter les épisodes par date, et en dessous
la liste des épisodes pour cette date, avec la possibilité d'enrouler la
date ».

## Ce qui est fait

**« Mes podcasts »** : sous l'onglet Podcasts, quand le champ de recherche
est vide, la liste `podcasts` de ma liste, dans l'ordre du serveur, lue par
le lecteur du fichier (`LectureFichier.podcasts`, `Noeud.podcasts`). Chaque
ligne : image, nom, et le dernier épisode, lu flux par flux en arrière-plan
une fois par vie de l'écran ; un flux qui ne répond pas est dit
« injoignable », sans être retiré. Sans podcast suivi, un mot le dit et
renvoie à la recherche ou au collage.

**L'étoile** sur un podcast, résultat ou suivi : un geste `podcast.ajouter`
avec flux, nom, auteur et image, ou `podcast.retirer` ; appliqué sur
place à ma liste (`MesRadios.ajouterPodcast`, `retirerPodcast`), noté au
journal apparié, envoyé dans la foulée, comme les favoris. L'appui long
sur un podcast suivi donne « Monter » et « Descendre », un geste
`podcasts.ordonner` avec toutes les adresses dans le nouvel ordre.

**Le collage d'une adresse** : « Coller l'adresse d'un flux… » à droite de
l'état, une boîte, `https://` d'office si le schéma manque ; le flux est
ouvert tel quel et, s'il a du son et n'est pas suivi, « Suivre ce
podcast ? » est proposé.

**Les épisodes par jour** (`ListeAdaptateur.montrerEpisodes`,
`ligne_date.xml`) : un en-tête par jour, « lundi 27/10/2026 », le jour en
toutes lettres dans la langue de l'application, un cran plus gros que le
texte secondaire, du plus récent au plus ancien, avec le nombre
d'épisodes et un chevron ; les épisodes sans date en bas sous « Sans
date ». La ligne d'un épisode ne répète plus la date, elle garde la durée
et « Reprendre à » ou « Écouté ».

**Repliés par défaut, et la recherche dans les épisodes**, précisés par
l'utilisateur après le 0.9.0 et faits des deux côtés (0.9.1, code 13) :
tous les jours repliés à l'ouverture, un appui déplie ou replie, le pli
n'est pas gardé. Un champ « Chercher dans les épisodes… » en tête de la
liste filtre sur le titre et le résumé, sans égard aux accents ni aux
majuscules ; le résumé de chaque épisode est lu dans le flux,
`description`, `itunes:summary` ou `content:encoded`, ramené à du texte
nu. Pendant une recherche, les jours restants sont tous dépliés et ceux
sans épisode retenu disparaissent ; rien ne correspond, « Aucun
épisode. » ; le champ vidé, tout revient replié. Les mêmes choix que le PC
(223 là-bas), convenus avec lui.

**Tout déplier, tout replier** (0.9.2, code 14), demandé le même jour pour
les deux côtés : « un icône bistable à gauche du champ de recherche qui
déroule / enroule tous les podcasts ». Un bouton à gauche du champ, qui dit
ce que le prochain appui fera, deux chevrons qui s'écartent tant qu'un jour
au moins est replié, qui se rejoignent quand tout est ouvert ; il se relit
après chaque appui sur un jour. Le choix tient le temps du podcast : un
jour peut encore être plié ou déplié à la main contre lui, une recherche
déplie les jours trouvés comme avant, et le champ vidé rend la liste comme
elle était. Un autre podcast s'ouvre replié. Éteint sans épisode.

Version 0.9.2, code 14. Aucune dépendance ajoutée.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/MainActivity.kt`,
`ListeAdaptateur.kt`, `fichier/LectureFichier.kt`, `modele/Modele.kt`,
`serveur/MesRadios.kt`, `serveur/Journal.kt`,
`res/layout/activity_main.xml`, `res/layout/ligne_date.xml`,
`res/drawable/ic_chevron.xml`, `ic_deplier.xml`, `ic_replier.xml`
(nouveaux), `res/values*/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.9.2-debug.apk` par-dessus
l'ancien, apparié.

- Onglet Podcasts, champ vide : « Mes podcasts », les huit du PC dans son
  ordre, avec leur image ; après quelques secondes, « Dernier épisode : … »
  sur chaque ligne.
- Un podcast suivi : ses jours, « lundi 27/10/2026 », du plus récent au
  plus ancien, le nombre à droite, tous repliés ; appuyer sur un jour le
  déplie, le chevron tourne ; encore, il se replie. Rouvrir le podcast :
  tout replié.
- Le bouton à gauche du champ, chevrons écartés : un appui, tous les jours
  se déplient et les chevrons se rejoignent ; un appui sur un jour le
  replie et les chevrons s'écartent à nouveau ; un appui sur le bouton,
  tout se déplie ; encore un, tout se replie.
- « Chercher dans les épisodes… », un mot d'un titre : seuls les jours qui
  ont un épisode retenu restent, tous dépliés ; un mot sans accent trouve
  le titre accentué ; un mot absent : « Aucun épisode. » et le bouton
  éteint ; le champ vidé, la liste revient comme elle était, dépliée si on
  l'avait dépliée, repliée sinon.
- Recherche, étoile sur un résultat : « Suivi : … », il apparaît en fin de
  « Mes podcasts » et sur le PC après le rafraîchissement.
- Étoile sur un podcast suivi, ou appui long → « Ne plus suivre » : retiré
  des deux côtés.
- Appui long sur un podcast suivi → « Descendre » : l'ordre change ici et
  sur le PC.
- « Coller l'adresse d'un flux… », une adresse RSS : le podcast s'ouvre,
  « Suivre ce podcast ? », Suivre : il est dans « Mes podcasts ».
- Une adresse qui n'est pas un flux : « Flux injoignable : … », retour à
  la liste.
