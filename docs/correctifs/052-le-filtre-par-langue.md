# 052 — Le filtre par langue

Le 2026-11-18. En regardant les applications de radio présentées sur
Reddit : presque toutes filtrent par genre, langue ou pays. L'application
cherchait par nom, pays et style, pas par langue. « On enchaîne avec le
filtre par langue. »

## Ce qui change

- **Une liste « Toutes les langues »** dans la recherche, à droite du
  pays ; le style passe sur sa propre ligne, dessous : trois listes côte à
  côte seraient trop étroites en portrait.
- **Les langues de l'annuaire** qui ont au moins dix stations et un code
  ISO 639, soixante-dix-huit environ. Les autres sont des variantes mal
  saisies (« francaise », « american english », des noms abîmés) :
  écartées.
- **Leur nom dans la langue du téléphone**, tiré du code : « Français »,
  « Allemand » ; triées par ce nom. La recherche envoie à l'annuaire son
  nom à lui, en anglais, en correspondance exacte.
- **Rien n'est choisi d'avance** : le pays du téléphone l'est déjà, une
  langue en plus écarterait les stations multilingues.
- Une langue seule suffit à lancer la recherche, comme un pays ou un
  style. Si l'annuaire ne donne pas ses langues, la liste reste à
  « Toutes les langues » et la recherche marche quand même.

Version 1.7.0, code 76, en essai.

## Fichiers

`annuaire/Annuaire.kt` (`langues()`, le paramètre `langue` de
`chercher`), `MainActivity.kt`, `res/layout/activity_main.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.7.0-debug.apk`.

- Ouvrir la recherche : pays et langue sur une ligne, le style dessous.
- La liste des langues, en français sur un téléphone en français :
  « Allemand », « Anglais », « Français »…, dans l'ordre alphabétique.
- « Tous les pays » et « Italien » : des stations en italien, d'Italie
  et d'ailleurs.
- Pays « France », langue « Anglais » : les stations françaises qui
  émettent en anglais.
- Le téléphone en anglais : « English », « French », « German »…
