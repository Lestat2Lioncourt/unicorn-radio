# 010 — Proposer la mise à jour

Le 2026-10-29, juste après la première version publiée, la 0.9.3, signée
avec la clef créée le même jour. Demandé le 2026-10-26 : « on pourra avoir
une mise à jour automatique si une nouvelle version du pkg est disponible ?
libre à l'utilisateur de l'installer ou pas ». Le cadre décidé le
2026-10-29 va un cran plus loin que la ROADMAP, qui prévoyait d'ouvrir la
page dans le navigateur : l'application télécharge l'APK elle-même.

## Ce qui est fait

**La vérification** (`miseajour/MiseAJour.kt`) : la dernière release du
dépôt, `GET api.github.com/repos/Lestat2Lioncourt/unicorn-radio/releases/latest`,
API publique, sans compte, soixante appels par heure et par adresse, bien
assez. Le numéro de l'étiquette, sans son `v`, est comparé à la version
installée chiffre par chiffre : 1.0.0 après 0.10.0, 0.10.0 après 0.9.3.
D'elle-même à l'ouverture de l'écran, au plus une fois par jour, en
arrière-plan, et sans un mot si rien n'est plus récent ou si GitHub ne
répond pas. « Plus tard » compte comme une vérification : on redemande le
lendemain.

**La boîte** : « Version X.Y.Z disponible », la note de la release dans la
langue de l'application, la section `[FR]` ou `[EN]` du texte publié, le
titre en tête et une puce par ligne, et deux boutons, « Installer » et
« Plus tard ». Rien ne s'installe sans ces gestes.

**Installer** (`miseajour/Installation.kt`) : l'APK joint à la release,
téléchargé par le gestionnaire de téléchargements d'Android dans le
dossier de l'application, avec sa notification ; à la fin, l'installeur
d'Android est ouvert sur le fichier. La première fois, Android demande
d'autoriser Unicorn-Radio à installer des applications, un réglage par
application, puis confirme encore. La permission
`REQUEST_INSTALL_PACKAGES` au manifeste, et rien d'autre. Si l'écran est
parti entre-temps, la notification du téléchargement fini ouvre
l'installeur elle-même. Un téléchargement manqué : une boîte le dit et
propose la page de la version ; pas d'APK joint, la page directement.

**« Mise à jour… »** dans le menu en haut à droite, derrière les trois
points, apparus pour l'occasion et teintés comme les autres icônes :
vérifie tout de suite et répond dans tous les cas, « À jour : 0.10.1 » ou
« GitHub injoignable : … ».

Version 0.10.1, code 17 : depuis le 0.10.1, les APK de débogage construits sur la machine qui a la clef sont signés avec elle, pour s'installer par-dessus une version publiée. Aucune dépendance ajoutée.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/miseajour/MiseAJour.kt`,
`Installation.kt` (nouveaux), `MainActivity.kt`, `AndroidManifest.xml`,
`res/menu/principal.xml`, `res/values*/strings.xml`,
`app/build.gradle.kts`, `docs/ROADMAP.md`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.10.1-debug.apk`.

- Menu ⋮ → « Mise à jour… » : « À jour : 0.10.1 », la 0.9.3 publiée est
  plus ancienne.
- En mode avion, la même entrée : « GitHub injoignable : … ».
- L'éprouvé complet demande une release plus récente que l'installée : la
  1.0.0 publiée, puis sur un téléphone qui a la 0.9.3 de la release, ouvrir
  l'application, ou « Mise à jour… ». La boîte « Version 1.0.0 disponible »
  avec la note en français ; « Installer » : la notification de
  téléchargement, puis la demande d'Android d'autoriser l'installation,
  puis l'installeur ; l'application rouverte dit 1.0.0 sous son nom,
  favoris et appariement en place.
- « Plus tard », fermer et rouvrir l'application : rien ; le lendemain, la
  boîte revient.
- En anglais par le drapeau : la boîte en anglais.
