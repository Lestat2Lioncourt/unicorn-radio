# 038 — Ce qui est programmé, dans les notifications

Le 2026-11-12. « On pourrait avoir une notification dans les statuts du
téléphone quand une alarme ou un countdown est programmé ? Avec l'heure
ou le temps restant ? »

## Ce qui change

**Des notifications discrètes** (`lecture/Annonces.kt`), sur leur propre
canal « Réveils et comptes à rebours programmés », priorité basse, sans
son ni vibration ; on les garde (`ongoing`) tant que c'est programmé :

- **un compte à rebours lancé** : son nom, « Fin à 14:32 · FIP », et le
  temps qui reste, qu'Android fait défiler lui-même
  (`setChronometerCountDown`), sans que l'application tourne ; « Annuler »
  ;
- **le prochain réveil en marche**, le plus proche s'il y en a plusieurs :
  « Prochain réveil : Semaine », « jeu. 07:00 · FIP » ; « Ne pas sonner
  cette fois ».

**« Ne pas sonner cette fois »** saute la prochaine sonnerie, le réveil
garde les suivantes : l'heure dite sautée est gardée dans l'alarme
(`saute`), la programmation passe à celle d'après
(`Reveil.quandApresLeSaut`). Un réveil qui ne sonne qu'une fois s'arrête ;
sans nom, il disparaît. Changer l'heure ou les jours, ou le remettre en
marche, oublie le saut.

**Un appui** ouvre l'application sur la liste qui va avec, réveils ou
comptes à rebours.

**Refaites à chaque changement** (`Annonces.rafraichir`, appelé par
`Reveil` quand une alarme est gardée, supprimée, programmée après une
sonnerie, reprogrammée au démarrage) et au lancement de l'application :
rien ne tourne entre-temps ; ce qui n'est plus programmé s'en va.

L'icône de réveil d'Android, dans la barre d'état, reste aussi : une
heure seulement, une minute en avance avec une station.

Version 1.3.0.20, code 53, en essai.

## Fichiers

`lecture/Annonces.kt` (nouveau), `lecture/Reveil.kt`, `Plantages.kt`
(l'application), `MainActivity.kt`, `AndroidManifest.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.20.

- Un réveil en marche : la notification « Prochain réveil : … », le jour
  et l'heure ; sans son.
- « Ne pas sonner cette fois » : elle passe à la sonnerie d'après (le
  lendemain, ou le jour choisi suivant) ; ce jour-là, rien ne sonne.
- Un compte à rebours de 3 minutes : sa notification, le temps qui
  défile ; « Annuler » : elle disparaît, l'interrupteur est retombé.
- À la fin d'un compte à rebours : sa notification s'en va.
- Un appui sur une notification : l'application s'ouvre sur la bonne
  liste.
- Tout arrêter : plus aucune de ces notifications.
