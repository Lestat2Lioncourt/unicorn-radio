# Prompt de publication

*À donner à Claude, ou à relire soi-même, pour publier une version
d'Unicorn-Radio. Repris de celui d'Unicorn-TV le 2026-10-25, avec ce qui
change pour une application : l'APK est la pièce jointe de la release, et la
clef de signature vit hors du dépôt. À compléter à la première publication.*

---

## Prompt

On publie une version. Tout ce qui suit, dans l'ordre, et rien ne part avant
que la note ait été montrée et acceptée.

### 0. La note : la montrer et attendre

- Un brouillon tiré des commits depuis la dernière étiquette
  (`git log vPRÉCÉDENTE..HEAD --format=%s`), proposé spontanément.
- Écrit pour **qui utilise l'application**. Forme du fichier
  `docs/notes-de-version/X.Y.Z.txt` :

  ```
  [FR] Titre en une ligne

  Une ligne par apport visible, courte
  …

  [EN] Title in one line

  One line per visible change, short
  …
  ```

- **Aucun nom du catalogue d'un fournisseur d'Unicorn-TV**, jamais ; le
  dépôt est public.
- **La montrer, puis attendre un accord sur ce texte.**

### 1. Les deux numéros de version

Dans `app/build.gradle.kts` : `versionName = "X.Y.Z"` et `versionCode`,
entier qui **monte de un à chaque publication** ; Android refuse d'installer
par-dessus un `versionCode` égal ou plus petit.

### 2. Le commit de version

Un commit « Version X.Y.Z » qui porte la note et `app/build.gradle.kts`.
Rien d'autre dedans.

### 3. L'APK signé

La clef dans `%USERPROFILE%\.unicorn-radio\signature.jks`, décrite par
`%USERPROFILE%\.unicorn-radio\signature.properties` (`storeFile`,
`storePassword`, `keyAlias`, `keyPassword`), que `app/build.gradle.kts` lit
si le fichier existe. **Ni l'un ni l'autre dans le dépôt.** Perdre la clef,
c'est ne plus pouvoir mettre à jour par-dessus : la sauvegarder.

```
JAVA_HOME="C:\\Program Files\\Java\\jdk-17" ./gradlew assembleRelease
```

→ `app/build/outputs/apk/release/app-release.apk`, copié en
`pkg/unicorn-radio-X.Y.Z.apk`.

### 4. L'étiquette annotée

```
git tag -a vX.Y.Z -F docs/notes-de-version/X.Y.Z.txt
```

### 5. Pousser, en deux commandes

```
git push origin main
git push origin vX.Y.Z
```

### 6. La release GitHub, **avec** l'APK

```
gh release create vX.Y.Z pkg/unicorn-radio-X.Y.Z.apk --title "Unicorn-Radio X.Y.Z" --notes-file docs/notes-de-version/X.Y.Z.txt --verify-tag
```

C'est par là que les gens l'installent : la pièce jointe est voulue ici, à
l'inverse d'Unicorn-TV.

### 7. Éprouver

Installer l'APK de la release par-dessus la version précédente sur un
téléphone : la mise à jour doit passer sans désinstaller, favoris et
dossiers en place.

### Ce qui va avec

- `docs/CHANGELOG.md` et le `correctifs/` du chantier, à jour avant le
  commit de version.
- `Dernière publication` ci-dessous, mis à jour.

---

Dernière publication : 1.0.0, code 18, le 2026-10-29 ; la première, 0.9.3, code 15, le même jour. La
clef `unicorn-radio` créée ce jour-là, empreinte SHA-256 commençant par
`E7:EE:76:0B`.
