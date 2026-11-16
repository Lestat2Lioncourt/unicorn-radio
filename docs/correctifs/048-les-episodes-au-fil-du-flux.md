# 048 — Les épisodes au fil du flux

Le 2026-11-16. « Au niveau des podcasts, je trouve que le chargement de la
liste des épisodes est assez lent avant d'avoir la main, on peut améliorer
ça ? » Puis : « afficher les épisodes dès que leur entête est lue […] à
condition de pouvoir démarrer par les plus récents ! »

## Ce qui n'allait pas

Ouvrir un podcast attendait trois choses l'une après l'autre avant de
montrer quoi que ce soit : le flux téléchargé en entier, analysé en
entier, puis, apparié, les positions d'écoute mises d'accord avec le
serveur. L'utilisateur constate la lenteur près du serveur aussi : le
poids est dans le flux. Pour un podcast ancien, plusieurs mégaoctets, et
dans l'analyse, deux dépenses par épisode : le résumé converti du HTML
en texte (`Html.fromHtml`), et la date essayée format après format, avec
un nouvel objet de format et une exception à chaque essai manqué.

## Ce qui change

- **Le flux est analysé pendant qu'il arrive**, directement sur le
  téléchargement, au lieu d'être d'abord copié en entier en mémoire.
- **Les vingt premiers épisodes s'affichent aussitôt**, puis la liste se
  complète par paquets de deux cents jusqu'à la fin. On a la main tout de
  suite : un épisode se lance pendant que la suite arrive. Les jours
  repliés ou dépliés le restent, la liste ne remonte pas.
- **Les plus récents d'abord.** Presque tous les flux vont du plus récent
  au plus ancien. Au premier paquet, les dates disent le sens : dans
  l'autre sens, rien ne s'affiche avant la fin, pour ne jamais commencer
  par les vieux épisodes.
- **« 120 épisodes, la suite arrive… »** sous le champ de recherche
  pendant le chargement ; une recherche porte sur ce qui est déjà lu et se
  complète avec la liste.
- **L'analyse allégée** : les formats de date préparés une fois par flux,
  sans exception, celui qui a réussi essayé d'abord ; le résumé gardé tel
  quel et converti en texte seulement pour la recherche, en arrière-plan
  une fois la liste complète.
- **Les positions d'écoute** : celles du téléphone marquent les lignes dès
  le premier paquet ; l'accord avec le serveur se fait après l'affichage,
  et les marques suivent s'il change quelque chose.
- Si le flux se coupe en route, les épisodes déjà montrés restent, avec le
  message d'erreur.
- Le journal note les temps : « les 20 premiers en … ms, tout en … ms ».

Version 1.5.0.1, code 70, en essai.

## Fichiers

`podcasts/Flux.kt`, `podcasts/Podcast.kt` (le résumé converti pour la
recherche seulement), `MainActivity.kt`, `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.5.0.1-debug.apk`.

- Ouvrir un podcast qui a des centaines d'épisodes : les premiers jours
  apparaissent vite, les plus récents en tête, la ligne « … épisodes, la
  suite arrive… » compte, puis disparaît.
- Pendant le chargement : déplier un jour, lancer un épisode ; la liste ne
  bouge pas quand la suite arrive.
- Taper dans « Chercher dans les épisodes… » pendant le chargement, puis
  après : la recherche trouve aussi dans les résumés.
- Fermer le podcast pendant le chargement et en ouvrir un autre : aucun
  épisode du premier ne s'y mêle.
- Un épisode commencé : sa marque est là dès l'ouverture.
- Téléphone branché, `adb logcat -s UnicornRadio` : la ligne « flux « … » :
  … épisodes, les 20 premiers en … ms, tout en … ms ».
