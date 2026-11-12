# 036 — La trace des plantages

Le 2026-11-12. Pendant un essai du réveil radio : « en bougeant le
téléphone pendant que le réveil jouait, l'app s'est fermée, visiblement
sans erreur ». Android n'annonce pas toujours un plantage, et le
téléphone n'est jamais branché à l'ordinateur : impossible d'en lire le
journal.

## Ce qui change

**Une classe `UnicornRadio` pour l'application**, déclarée au manifeste,
installe avant tout le reste un gardien des plantages (`Plantages.kt`),
pour l'écran comme pour le service de lecture. Une erreur non rattrapée
est écrite dans `plantage.txt`, dans les fichiers privés de
l'application : la version, la date, Android et le modèle du téléphone,
le fil, la pile. Puis Android fait comme d'habitude.

**À l'ouverture suivante**, une fenêtre « L'application s'est arrêtée »
montre la trace, une fois, avec « Copier », « Envoyer… » (le partage
d'Android) et « Fermer ». Rien ne part tout seul : la trace ne quitte le
téléphone que si on l'envoie.

Le plantage du réveil n'est pas corrigé : il attend sa trace.

Version 1.3.0.13, code 46, en essai.

## Fichiers

`Plantages.kt` (nouveau), `MainActivity.kt`, `AndroidManifest.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`.

## Vérification

À relancer : installer la 1.3.0.13.

- Rien ne s'est arrêté : aucune fenêtre à l'ouverture.
- Après un plantage, rouvrir l'application : la fenêtre, avec la trace ;
  « Copier » la met dans le presse-papiers. La rouvrir encore : plus de
  fenêtre.
