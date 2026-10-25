# 001 — Le socle

Le 2026-10-25, au terme d'une réflexion menée dans Unicorn-TV sur la façon
d'écouter dehors des stations servies en `http` : « est-ce que ce ne serait
pas mieux que je crée un dossier dédié pour Unicorn-Radio, qui serait une
app autonome mais également capable d'échanger des fichiers de
configuration avec Unicorn-TV ? […] l'app aurait son dépôt git dédié, qui
serait lui public. » Licence : « la même que celle du projet d'origine
d'Unicorn-TV », la GPL-3.0.

## Ce qu'il y avait

Rien. Unicorn-TV avait produit une page web pour le téléphone (son chantier
220) et buté sur deux murs du navigateur : pas de flux `http` depuis une
page sécurisée, pas d'AAC brut dans Chrome sur Android. Le pourquoi de ce
projet est en tête de `docs/ROADMAP.md`.

## Ce qui est fait

**Le dépôt.** `README.md` en anglais puis en français, `LICENSE` repris tel
quel d'Unicorn-TV, `.gitignore` qui écarte la clef de signature, les builds,
et tout ce qui brieffe Claude Code sur cette machine. Le dossier `docs/` sur
le modèle d'Unicorn-TV, avec `format-d-echange.md`, le contrat du fichier
JSON partagé avec Unicorn-TV, établi depuis son export tel qu'il est ; il y
manque un champ `format` que l'autre projet doit se mettre à écrire. La
procédure de publication, adaptée : l'APK est la pièce jointe de la release.

**L'application vide.** Nom de paquet `org.unicornradio.app`, une activité
qui affiche son numéro de version, l'icône adaptative provisoire, et dans le
manifeste la ligne qui justifie le projet : `usesCleartextTraffic="true"`.
Gradle 8.11.1 par le wrapper, AGP 8.10.1, Kotlin 2.1.21, compileSdk 36,
minSdk 26. Pas d'Android Studio : le SDK installé par Visual Studio dans
`C:\Program Files (x86)\Android\android-sdk` suffit, et `local.properties`,
hors dépôt, pointe dessus. Le build de release lit une clef de signature
dans `%USERPROFILE%\.unicorn-radio\signature.properties` si elle existe.

## Fichiers

Tout le dépôt.

## Vérification

- `./gradlew assembleDebug` avec `JAVA_HOME` sur le JDK 17 : réussi du
  premier coup, 3 min 51 s, le temps de télécharger Gradle et les
  dépendances ; `app/build/outputs/apk/debug/app-debug.apk`.
- À faire sur un téléphone : installer l'APK, ouvrir, lire
  « Unicorn-Radio 0.1.0 · Rien ne joue encore. »
