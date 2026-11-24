# Unicorn-Radio

[![Downloads](https://img.shields.io/github/downloads/Lestat2Lioncourt/unicorn-radio/total)](https://github.com/Lestat2Lioncourt/unicorn-radio/releases)
[![Latest release](https://img.shields.io/github/v/release/Lestat2Lioncourt/unicorn-radio)](https://github.com/Lestat2Lioncourt/unicorn-radio/releases/latest)
[![Licence: GPL-3.0-only](https://img.shields.io/badge/licence-GPL--3.0--only-blue)](LICENSE)

**Web radios and podcasts for Android. Free, open source, no ads, no
tracking.**

*[Version française plus bas.](#unicorn-radio-en-français)*

## What it does

**Radios**
- Search more than 50,000 stations of the free
  [Radio Browser](https://www.radio-browser.info/) directory, by name,
  country or style.
- Favourites and folders on several levels; next and previous loop through
  a folder.
- Plays in the background, with the current track on the lock screen, in
  the notification, on a widget and with the headset buttons.
- Plays what browsers now refuse: plain `http` streams, raw AAC, HLS.
- Like the track playing, find it later in a list; the last tracks played.
- Send your stations to a nearby phone over Bluetooth, without Internet;
  export and import them as a file.

**Sound**
- A 10-band equalizer with presets and your own, a configurable spectrum.
- A TV mode, readable from the couch: logo, name, track, time, full screen;
  on a screen connected to the phone, it shows there.

**Wake up and fall asleep**
- Radio alarms with a progressive volume, a backup radio if the station
  does not play, then the phone ringtone. The station starts two minutes
  early, muted, to get past its ads.
- Countdowns and a sleep timer.

**Podcasts**
- Search the Apple Podcasts directory, follow podcasts, episodes grouped by
  date, search inside episodes, shown as soon as the feed starts arriving.
- A play queue, downloads for offline listening, playback speed.
- Import and export your subscriptions as OPML.

**And**
- English and French.
- Updates offered from this page, installed over the previous version.

## Installing

Download the APK of the
[latest release](https://github.com/Lestat2Lioncourt/unicorn-radio/releases/latest),
open it on the phone and allow installation from this source when Android
asks. Later releases are offered inside the app and install over the
previous one. There is no Play Store listing.

## Building

Android SDK with platform 36 and build-tools 36, a JDK 17 or 21, and the
Gradle wrapper does the rest:

```
./gradlew assembleDebug
```

The history is in [docs/CHANGELOG.md](docs/CHANGELOG.md), the roadmap in
[docs/ROADMAP.md](docs/ROADMAP.md).

## Licence

© 2026 Lestat2Lioncourt. GNU GPL v3.0 only. See [LICENSE](LICENSE).

---

# Unicorn-Radio, en français

**Radios web et podcasts pour Android. Gratuit, libre, sans pub, sans
pistage.**

## Ce qu'elle fait

**Radios**
- Chercher parmi plus de 50 000 stations de l'annuaire libre
  [Radio Browser](https://www.radio-browser.info/), par nom, pays ou style.
- Favoris et dossiers à plusieurs niveaux ; suivante et précédente
  tournent en boucle dans un dossier.
- Lecture en arrière-plan, avec le titre en cours sur l'écran de
  verrouillage, dans la notification, sur un widget et aux boutons du
  casque.
- Lit ce que les navigateurs refusent désormais : les flux en `http`
  simple, l'AAC brut, le HLS.
- Aimer le titre qui passe et le retrouver dans une liste ; les derniers
  titres passés.
- Envoyer ses radios à un téléphone proche en Bluetooth, sans Internet ;
  les exporter et les importer en fichier.

**Le son**
- Un égaliseur dix bandes, avec des préréglages et les siens, un spectre
  configurable.
- Un mode télé, lisible depuis le canapé : logo, nom, titre, heure, plein
  écran ; sur un écran branché au téléphone, il s'y affiche.

**Se réveiller, s'endormir**
- Des réveils radio au volume progressif, une radio de secours si la
  station ne joue pas, puis la sonnerie du téléphone. La station démarre
  deux minutes avant, en silence, pour passer sa pub.
- Des comptes à rebours et une minuterie d'extinction.

**Podcasts**
- Chercher dans l'annuaire d'Apple Podcasts, suivre des podcasts, les
  épisodes groupés par date, chercher dans les épisodes, affichés dès que
  le flux commence à arriver.
- Une liste de lecture, le téléchargement pour écouter hors ligne, la
  vitesse de lecture.
- Importer et exporter ses abonnements en OPML.

**Et aussi**
- En français et en anglais.
- Les mises à jour proposées depuis cette page, installées par-dessus.

## Installer

Télécharger l'APK de la
[dernière version](https://github.com/Lestat2Lioncourt/unicorn-radio/releases/latest),
l'ouvrir sur le téléphone et autoriser l'installation depuis cette source
quand Android le demande. Les versions suivantes sont proposées dans
l'application et s'installent par-dessus. Pas de Play Store.

## Construire

Un SDK Android avec la plateforme 36 et les build-tools 36, un JDK 17 ou
21, et le wrapper Gradle fait le reste :

```
./gradlew assembleDebug
```

L'historique est dans [docs/CHANGELOG.md](docs/CHANGELOG.md), la feuille
de route dans [docs/ROADMAP.md](docs/ROADMAP.md).

## Licence

© 2026 Lestat2Lioncourt. GNU GPL v3.0 seulement. Voir [LICENSE](LICENSE).
