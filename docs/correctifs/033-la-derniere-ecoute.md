# 033 — La dernière écoute, relancée d'un appui

Le 2026-11-12. Premier de la liste « à voir » : « la dernière station
relancée d'un appui à l'ouverture ».

## Ce qui change

**Le service note ce qu'on écoute** à chaque changement d'élément
(`lecture/DerniereEcoute.kt`, préférences « derniere_ecoute ») :

- une station, entière, telle que le lecteur l'avait : identifiant, nom,
  adresse, logo, HLS, pays ;
- un épisode, seulement « un épisode » : la liste de lecture garde déjà
  les épisodes passés, le courant en dernier, et ceux à suivre. On la
  rejoue, l'épisode courant repris à sa position, le fichier téléchargé
  s'il y en a un.

**À l'ouverture**, si le lecteur est vide, la dernière écoute y est mise
en pause : la ligne de lecture la montre, un appui sur lecture la relance.
Rien ne joue sans appui.

**Depuis Android**, sans ouvrir l'application : « lecture » sur un
lecteur vide (le widget, 034 ; un casque ; la carte de lecture des
réglages rapides) reprend la dernière écoute
(`MediaSession.Callback.onPlaybackResumption`, et le
`MediaButtonReceiver` de Media3 déclaré au manifeste, qui réveille le
service).

Une station reprise est seule dans le lecteur : « précédent » et
« suivant » n'ont plus la liste d'où elle venait.

Version 1.3.0.11, code 44, en essai, avec le widget (034).

## Fichiers

`lecture/DerniereEcoute.kt` (nouveau), `lecture/ServiceLecture.kt`,
`MainActivity.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.11.

- Écouter une station, arrêter, fermer l'application tout à fait (la
  balayer des applications récentes), la rouvrir : la ligne de lecture
  montre la station, en pause ; lecture : elle joue.
- Même chose avec un épisode : il revient à sa position, « précédent »
  et « suivant » parcourent la liste de lecture.
- Application fermée, un appui sur lecture d'un casque Bluetooth : la
  dernière écoute repart.
