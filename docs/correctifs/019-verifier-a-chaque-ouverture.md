# 019 — La mise à jour vérifiée à chaque ouverture

Le 2026-11-09, juste après la 1.1.1. Le 010 vérifiait la dernière release
au plus une fois par jour : la 1.1.0 du téléphone avait vérifié le matin,
la 1.1.1 publiée le soir n'a pas été proposée, le menu seul la voyait.
« On lance le check à chaque fois, c'est trop pénible, je ne pourrais
jamais tester sinon, j'utilise l'app tous les jours. »

## Ce qui change

**À chaque retour de l'écran au premier plan**, l'application lit la
dernière release et la propose si elle est plus récente. L'appel est
petit, un JSON de l'API publique de GitHub, et silencieux quand rien n'est
plus récent ou que GitHub ne répond pas. La date de dernière vérification
n'est plus gardée.

**« Plus tard »** garde un sens : il écarte **cette version-là** pour un
jour (`MiseAJour.ecarter`, `estEcartee`) ; elle n'est plus proposée
d'elle-même avant le lendemain, mais une version plus récente encore le
serait tout de suite, et le menu propose toujours. Une seule boîte à la
fois : revenir au premier plan pendant qu'elle est ouverte n'en ouvre pas
une seconde (`propositionOuverte`).

Version 1.1.2, code 22.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/miseajour/MiseAJour.kt`,
`MainActivity.kt`, `app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.1.2, puis attendre une release plus récente.

- Une release publiée pendant que l'application est ouverte : la quitter
  et y revenir, la boîte « Version X disponible » s'ouvre d'elle-même.
- « Plus tard », quitter, revenir : rien ; le menu « Mise à jour… » la
  propose quand même. Le lendemain, elle revient d'elle-même.
- Quitter et revenir pendant que la boîte est ouverte : une seule boîte.
