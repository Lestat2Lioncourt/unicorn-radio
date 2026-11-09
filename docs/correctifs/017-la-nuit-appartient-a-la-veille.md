# 017 — La nuit appartient à la veille

Le 2026-11-09, dans le bloc de sept, une consigne prise le même soir côté
PC (234 là-bas), que le téléphone devait suivre : « les épisodes d'hier
soir mais publiés après minuit sont bien rattachés à la date du dimanche
[ailleurs]. Or, dans Unicorn, on les voit à la date de publication ».

## Le défaut

Un épisode n'a dans son flux qu'une date de publication, en temps
universel. L'intégrale d'une émission du soir est publiée vers 0 h 15 heure
de Paris, et tombait sous l'en-tête du lendemain. Exact à la lettre, faux
pour l'émission ; rien d'autre dans le flux ne dit le jour d'émission.

## La règle

**Publié avant 1 h 30, heure locale, l'épisode est rangé au jour d'avant.**
La borne est celle que l'utilisateur a choisie pour le PC : les soirées
ordinaires finissent avant 0 h 30, la marge est pour les soirées
spéciales ; 5 h avait été refusé comme trop large. La date gardée ne
change pas, seul l'affichage se déplace, et le jour de la veille est pris
à midi, pour que le changement d'heure ne fasse pas glisser la date.

`podcasts/Podcast.kt`, `jourDEpisode(ms)`, `MINUTES_DE_LA_VEILLE = 90` ;
`ListeAdaptateur.montrerEpisodes` groupe les jours par cette fonction. Les
lignes d'épisodes ne portent pas de date, et les listes gardées en portent
la valeur brute : rien d'autre à déplacer.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/podcasts/Podcast.kt`,
`ListeAdaptateur.kt`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.1.0-debug.apk` par-dessus.

- Un podcast d'émission du soir : l'intégrale publiée à 0 h 15 est sous
  l'en-tête de la veille, le même jour que sur le PC.
- Un épisode publié à 1 h 30 ou après reste à sa date ; à 23 h 59 aussi.
