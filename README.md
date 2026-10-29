# Unicorn-Radio

*[Version française plus bas.](#unicorn-radio-en-français)*

Unicorn-Radio is a web radio player for Android. It searches the free
[Radio Browser](https://www.radio-browser.info/) directory, keeps your
stations in favourites and folders, plays them in the background with the
current track on the lock screen, and does not care whether a stream is
served over plain `http`, which the browsers of today refuse.

It lives on its own. It can also exchange its favourites and folders with
[Unicorn-TV](https://github.com/Lestat2Lioncourt/unicorn-tv), the desktop
player it was born from, through a small JSON file: what you file on the
computer follows you on the phone, and the other way round. The file format
is documented in [docs/format-d-echange.md](docs/format-d-echange.md).

## Status

Usable, and young. Since 0.9.3, the first published release: directory
search, favourites and nested folders, background playback, export files,
pairing with a Unicorn-TV at home, and podcasts with resume shared with
the PC. Unicorn-TV is not required. The roadmap is in
[docs/ROADMAP.md](docs/ROADMAP.md), the history in
[docs/CHANGELOG.md](docs/CHANGELOG.md).

## Installing

The [Releases](https://github.com/Lestat2Lioncourt/unicorn-radio/releases)
carry a signed APK. Allow installation from unknown sources on the phone,
open the file, done. Later releases install over the previous one. There is
no Play Store listing.

## Building

Android SDK with platform 36 and build-tools 36, a JDK 17 or 21, and the
Gradle wrapper does the rest:

```
./gradlew assembleDebug
```

## Licence

GNU GPL v3.0, like Unicorn-TV. See [LICENSE](LICENSE).

---

# Unicorn-Radio, en français

Unicorn-Radio est un lecteur de radios web pour Android. Il cherche dans
l'annuaire libre [Radio Browser](https://www.radio-browser.info/), garde
vos stations en favoris et en dossiers, joue écran éteint avec le titre en
cours sur l'écran de verrouillage, et ne se soucie pas qu'un flux soit servi
en `http` simple, ce que les navigateurs d'aujourd'hui refusent.

Il vit seul. Il sait aussi échanger ses favoris et ses dossiers avec
[Unicorn-TV](https://github.com/Lestat2Lioncourt/unicorn-tv), le lecteur de
bureau dont il est né, par un petit fichier JSON : ce que vous rangez sur
l'ordinateur vous suit sur le téléphone, et inversement. Le format du
fichier est décrit dans [docs/format-d-echange.md](docs/format-d-echange.md).

## État

Utilisable, et jeune. Depuis la 0.9.3, première version publiée : la
recherche dans l'annuaire, les favoris et les dossiers à plusieurs niveaux,
la lecture en arrière-plan, les fichiers d'export, l'appariement à un
Unicorn-TV de la maison, et les podcasts avec la reprise partagée avec le
PC. Unicorn-TV n'est pas nécessaire. La feuille de route est dans
[docs/ROADMAP.md](docs/ROADMAP.md), l'historique dans
[docs/CHANGELOG.md](docs/CHANGELOG.md).

## Installer

Les [Releases](https://github.com/Lestat2Lioncourt/unicorn-radio/releases)
portent un APK signé. Autoriser les sources inconnues sur le téléphone,
ouvrir le fichier, c'est tout. Les versions suivantes s'installent
par-dessus. Pas de Play Store.

## Construire

Un SDK Android avec la plateforme 36 et les build-tools 36, un JDK 17 ou
21, et le wrapper Gradle fait le reste :

```
./gradlew assembleDebug
```

## Licence

GNU GPL v3.0, comme Unicorn-TV. Voir [LICENSE](LICENSE).
