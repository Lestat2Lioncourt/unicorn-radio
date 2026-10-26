# 004 — Les deux langues

Le 2026-10-26, une consigne à prendre « dès à présent » : l'application a
des accointances avec Unicorn-TV, et la règle du double langage de
celui-ci, français et anglais avec basculement au clic sur le drapeau en
haut à droite, doit être propagée ici.

## Ce qu'il y avait

Les textes existaient déjà dans les deux langues, `values/strings.xml` en
anglais et `values-fr/strings.xml`, et l'application suivait la langue du
téléphone. Rien pour en changer depuis l'application.

## Ce qui est fait

**Les drapeaux** (`res/layout/drapeaux.xml`, `drawable/drapeau_fr.xml`,
`drapeau_en.xml`) : deux petits drapeaux vectoriels à droite de la barre,
après la loupe et le fichier, portés par un élément de menu à vue propre.
Celui de la langue active est net, l'autre à quarante pour cent, pour que
la paire se lise comme un état et non comme deux boutons, le même parti
que sur le bureau.

**Le basculement** (`MainActivity.poserLesDrapeaux`, `choisirLaLangue`) :
appuyer passe par `AppCompatDelegate.setApplicationLocales`, le mécanisme
officiel. Il relance l'écran dans l'autre langue ; le lecteur est dans le
service, le son continue. Le choix est gardé par le système depuis
Android 13, et par AppCompat avant, grâce au service
`AppLocalesMetadataHolderService` déclaré dans le manifeste avec
`autoStoreLocales`. Sans choix, la langue du téléphone, et l'anglais pour
toute autre langue que le français, comme Unicorn-TV. Changer de langue en
pleine recherche referme la recherche.

**La règle, pour la suite** : tout texte visible ajouté va dans les deux
fichiers de ressources, jamais en dur dans le code ou les mises en page.
Les noms des langues, « Français » et « English », sont écrits dans leur
langue et ne se traduisent pas.

**Et le logo dans la barre**, demandé dans la foulée : à la racine, le
logo de l'application devant son nom, en haut à gauche ; dans un dossier
ou dans la recherche, la flèche retour prend sa place. Les bitmaps
`res/drawable-*/logo.png`, trente-six dp aux cinq densités, viennent du
PNG en haute définition fourni ce jour, réduits par un petit programme
Java du scratchpad, javax.imageio suffisant.

**Et les icônes de la barre éclaircies**, à la vue du résultat : la flèche
retour, la loupe et le fichier étaient teintés de la couleur du texte,
presque noire. Une couleur à elles, `icone_barre`, gris moyen en thème
clair et gris clair en thème sombre (`values/colors.xml`,
`values-night/colors.xml`), posée par `navigationIconTint` sur la barre et
`iconTint` sur les éléments du menu. Les drapeaux gardent leurs couleurs.

Version 0.4.0, code 4. Aucune dépendance ajoutée.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/MainActivity.kt`,
`app/src/main/AndroidManifest.xml`, `app/src/main/res/menu/principal.xml`,
`res/layout/drapeaux.xml`, `res/drawable/drapeau_fr.xml`,
`res/drawable/drapeau_en.xml`, `res/drawable-*/logo.png`,
`res/layout/activity_main.xml`, `res/values*/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.4.0-debug.apk` par-dessus
l'ancien.

- Les deux drapeaux en haut à droite ; sur un téléphone en français, le
  tricolore est net et l'Union Jack atténué.
- Appuyer sur l'Union Jack : l'écran se relance en anglais, barre, écran
  vide, ligne d'état de la recherche, messages ; les drapeaux s'inversent.
- Pendant qu'une station joue, changer de langue : le son ne s'interrompt
  pas, la ligne du bas est toujours là.
- Fermer l'application et la rouvrir : la langue choisie est gardée.
- Appuyer sur le drapeau déjà actif ne fait rien.
- À la racine, le logo devant le nom ; dans un dossier ou la recherche, la
  flèche retour à sa place et pas de logo.
