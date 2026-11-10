# 020 — Le serveur gardé en mémoire

Le 2026-11-10. Pendant un podcast, le service de lecture note la position
toutes les dix secondes et la pousse au serveur ; avant chaque poussée, il
recherchait le serveur, donc lui demandait son identité. De 6 h 20 à 6 h 28
ce matin, le PC a reçu une demande d'identité toutes les dix secondes, et
sa fenêtre s'est remplie de « Identité demandée depuis … ». Même chose à
l'ouverture d'un podcast et à chaque synchronisation. Consigne : garder
l'adresse trouvée, et ne refaire la recherche qu'en cas d'échec ou de
changement de réseau.

## Ce qui change

**L'adresse gardée sert telle quelle** (`Serveur.adresse`) : la dernière
qui a répondu avec la bonne identité, déjà notée dans les préférences.
La recherche complète (`Serveur.retrouver`, qui essaie chaque adresse,
l'active d'abord) n'a lieu que si aucune adresse n'est gardée ou si elle
est **à retrouver**.

**En cas d'échec, une recherche puis un second essai** (`Serveur.avecLAdresse`) :
la poussée des positions, la mise d'accord des écoutes à l'ouverture d'un
podcast et la synchronisation du journal passent par là. Un envoi qui
échoue autrement que par une révocation déclenche la recherche, et l'envoi
est refait une fois à l'adresse qui répond. Si rien ne répond, les
positions attendent sur le téléphone, comme avant. Dans la synchro du
journal, les paquets déjà acceptés ont quitté le journal : ils ne
repartent pas.

**Au changement de réseau** (`Reseau`), l'adresse est marquée à retrouver :
Android signale un autre réseau par défaut ou la perte du réseau
(`ConnectivityManager.registerDefaultNetworkCallback`), et la demande
suivante refait la recherche sans attendre un échec. Cela couvre la box
qui redémarre ou un autre wifi de la liste. L'écran et le service
s'abonnent tous deux ; un seul abonnement au système. Le réseau courant,
signalé à l'abonnement, n'est pas un changement. Permission
`ACCESS_NETWORK_STATE`, accordée sans question.

Scénario wifi → données mobiles → wifi : les adresses connues sont
locales, hors du wifi rien ne répond, les positions attendent ; au retour
l'adresse gardée est à retrouver, la recherche la confirme, la poussée
passe. Format d'échange inchangé ; l'écran d'appariement et son bouton
d'épreuve recherchent toujours le serveur, c'est leur rôle.

Version 1.1.3, code 23.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/serveur/Serveur.kt`, `Reseau.kt`
(nouveau), `Synchro.kt`, `podcasts/Ecoutes.kt`, `lecture/ServiceLecture.kt`,
`MainActivity.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.1.3, apparié, le serveur allumé.

- Lire un podcast quelques minutes : la fenêtre du PC ne dit l'identité
  demandée au plus qu'une fois ; les positions arrivent (reprise au même
  endroit sur le bureau).
- Couper le wifi du téléphone pendant la lecture, puis le remettre : la
  poussée suivante passe, le journal du téléphone (`Unicorn-Radio` dans
  Logcat) montre « un autre réseau, le serveur sera recherché » puis la
  recherche.
- Serveur éteint pendant la lecture : une recherche à la poussée suivante,
  rien ne passe, pas d'erreur visible ; serveur rallumé, les positions en
  attente partent à la poussée suivante.
- Ouvrir un podcast, synchroniser depuis le menu : une seule demande
  d'identité au plus, pas une par geste.
