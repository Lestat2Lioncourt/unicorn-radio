# 005 — Apparier et tirer

Le 2026-10-26, le premier chantier de la synchronisation décrite dans
`format-d-echange.md`, « La synchronisation avec un serveur », dans
l'ordre convenu avec Unicorn-TV : apparier le téléphone à son Unicorn-TV,
et tirer sa liste. Le côté serveur existe depuis le même jour (222
là-bas). Les gestes du téléphone vers le serveur sont le chantier suivant.

## Ce qui est fait

**L'écran Serveur** (`ServeurActivity`, `layout/activity_serveur.xml`),
par l'icône de lien dans la barre ou « Apparier un serveur… » sur l'écran
vide. Non apparié : « Scanner le QR code », et en dessous, pour la saisie
à la main, l'adresse, le code et « Apparier ». Apparié : le nom du
serveur, l'utilisateur, l'adresse qui répond, l'heure de la dernière
copie, « Mettre à jour » et « Se détacher » avec confirmation, qui
prévient le serveur si on le voit, puis oublie tout de lui sauf
l'identifiant d'appareil.

**Le QR code** par ZXing (`zxing-android-embedded`, libre, hors ligne,
sans services Google), la caméra demandée au moment du scan ; refusée ou
scan abandonné, un mot renvoie à la saisie. Le JSON du QR code est
vérifié, `application` et `appariement`, sinon « Ce QR code n'est pas un
appariement Unicorn-TV ». Une adresse tapée sans schéma reçoit `http://`.

**Le client** (`serveur/ClientServeur.kt`) : les routes du contrat, en
`HttpURLConnection` comme l'annuaire, avec le jeton en en-tête
`Authorization: Bearer`. `trouver` essaie les adresses dans l'ordre, la
dernière qui a répondu d'abord, et garde la première qui rend l'identité
attendue en trois secondes : un autre Unicorn-TV sur la même adresse est
écarté et journalisé. Un `404` à l'appariement est « code inconnu ou
périmé », un `429` « trop d'essais », un `401` partout « révoqué ».

**Ce qui est gardé** (`serveur/Serveur.kt`) : identité, nom, adresses,
adresse active, jeton, utilisateur, instant de la dernière copie, et
l'identifiant d'appareil tiré une fois pour toutes ; le nom d'appareil
est celui des réglages du téléphone, sinon son modèle. Dans les
préférences privées de l'application, pas chiffrées : la bibliothèque de
préférences chiffrées est abandonnée par Google, le bac à sable protège
déjà ce fichier, et le jeton n'ouvre que la liste de radios.

**La copie** (`serveur/MesRadios.kt`) : la section seule rendue par
`GET /api/radio/export`, vérifiée lisible puis écrite dans
`mes-radios.json`, lue par le même lecteur que les fichiers. Tirée à
l'appariement, à chaque retour de l'application au premier plan, donc
aussi au retour de l'écran Serveur, et sur « Mettre à jour ».

**La racine, apparié** : « Mes favoris » du serveur, toujours présent,
puis ses dossiers ; les fichiers importés et les favoris locaux ne sont
plus montrés mais restent sur le disque, et reviennent tels quels au
détachement. L'entrée « Ouvrir un fichier » disparaît. Le sous-titre de
la barre dit le nom du serveur, « injoignable » quand il ne répond pas,
et la version. Un `401` oublie le serveur, efface la copie, ramène aux
fichiers et le dit dans un dialogue.

**Pas encore là, exprès** : apparié, l'étoile des résultats et l'appui
long dans « Mes favoris » sont retirés, puisque les favoris sont ceux du
serveur et qu'un changement local serait écrasé au tirage suivant. Ils
reviennent au chantier suivant, comme gestes du journal, avec les
favoris locaux versés au serveur.

Version 0.5.0, code 5. Dépendance ajoutée : ZXing. Permission ajoutée :
la caméra, déclarée non requise.

## Après le premier essai : 0.5.1

L'appariement a réussi au second clic sur le serveur réel, le 2026-10-26
au soir, et l'essai a montré quatre choses, corrigées dans le 0.5.1,
code 6 :

- **Le premier contact dépassait trois secondes** : le wifi du téléphone
  se réveille, et la sonde d'identité concluait trop tôt, à l'appariement
  comme au retour au premier plan, d'où un « injoignable » sous le nom du
  serveur alors que tout marchait. Cinq secondes par adresse, un second
  essai quand elle se tait, et l'accueil ne dit « injoignable » qu'après
  deux tirages manqués de suite.
- **« Serveur injoignable » ne disait rien** : le message porte désormais
  l'adresse essayée et la raison, délai, connexion refusée, adresse
  inconnue, code HTTP, pas un Unicorn-TV, un autre serveur. Les raisons
  sont des codes dans le client, des textes dans l'écran, dans les deux
  langues.
- **Une adresse tapée sans port** frappait au port 80 : le port 3000,
  celui d'Unicorn-TV, est ajouté d'office quand il manque. Et « Apparier »
  avec un champ vide le dit au lieu de se taire.
- **Un logo qui débordait d'une ligne sur l'autre** : une ligne recyclée
  de la liste gardait le chargement en cours du logo précédent, qui
  arrivait sur une station sans logo. Le chargement est annulé avant de
  vider la vue.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/serveur/Serveur.kt`,
`ClientServeur.kt`, `MesRadios.kt` (nouveaux),
`app/src/main/kotlin/org/unicornradio/app/ServeurActivity.kt` (nouveau),
`MainActivity.kt`, `res/layout/activity_serveur.xml` (nouveau),
`res/layout/activity_main.xml`, `res/menu/principal.xml`,
`res/drawable/ic_serveur.xml` (nouveau), `res/values*/strings.xml`,
`AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.5.1-debug.apk` par-dessus
l'ancien ; côté PC, Unicorn-TV redémarré sur son 222, page radio ouverte.

- Barre : la loupe, le fichier, l'icône de lien, les drapeaux.
- Icône de lien : l'écran Serveur, non apparié, avec « Scanner le QR
  code » et les champs à la main.
- Sur le PC, clic droit dans le vide de l'arbre → « Apparier un
  téléphone… ». « Scanner le QR code » : la caméra demandée la première
  fois, le QR code lu, puis « Copie à jour : N stations » et l'écran
  passe en apparié, avec le nom du serveur, l'utilisateur et l'adresse.
  La fenêtre du serveur dit « Téléphone apparié ».
- Retour à la liste : « Mes favoris » puis les dossiers du PC ; le
  sous-titre porte le nom du serveur ; une station joue.
- Les réglages du PC listent le téléphone, sous son nom.
- Un code faux à la main : « Code inconnu ou périmé ». Un code de plus de
  dix minutes : pareil.
- Wifi coupé, application relancée : la liste reste, le sous-titre dit
  « injoignable ».
- Sur le PC, « Révoquer » le téléphone, puis l'application au premier
  plan : le dialogue « … ne connaît plus ce téléphone », retour aux
  fichiers d'avant.
- Réapparier, puis « Se détacher » : confirmation, retour aux fichiers,
  et le téléphone a disparu des réglages du PC.
- Apparié, la recherche ne montre pas d'étoile.
