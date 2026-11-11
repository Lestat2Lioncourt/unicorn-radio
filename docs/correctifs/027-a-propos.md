# 027 — À propos

Le 2026-11-11. « Une rubrique à propos dans les options, qui indique
sommairement ce que fait l'app, sans besoin d'identifiant et sans aucune
collecte d'information. »

## Ce qui change

**« À propos »**, en bas du menu « … », sous « Mise à jour ». Une fenêtre
titrée du nom et de la version :

- ce que fait l'application : radios web et podcasts, les deux annuaires,
  favoris et dossiers, l'écoute en arrière-plan, les téléchargements ;
- sans compte ni identifiant, aucune collecte ;
- à qui elle parle, et à personne d'autre : Radio Browser, l'annuaire des
  podcasts, les stations et podcasts écoutés, GitHub pour les mises à
  jour, le serveur Unicorn-TV s'il y en a un d'apparié ;
- logiciel libre, GPL-3.0.

« Code source » ouvre la page du dépôt, « Fermer » ferme.

**Le compteur aussi**, sous le texte : « Déjà téléchargée N fois », la
même ligne que dans l'invitation (026), demandé ensuite.

**Le compteur d'écoutes de Radio Browser est dit**, plutôt que promettre
que rien ne sort : l'application l'appelle à chaque station lancée, comme
l'annuaire le demande ; il ne compte que des écoutes par station, rien sur
la personne.

Version 1.2.0.4, code 30, en essai, puis 1.2.0.5, code 31, avec le
compteur ; publiée en 1.2.1, code 32.

## Fichiers

`MainActivity.kt`, `res/menu/principal.xml`, `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.2.0.5.

- Menu « … », « À propos », en dernier : le titre porte la version, le
  texte en français ; en anglais avec le drapeau.
- Sous le texte, « Déjà téléchargée N fois », le même nombre que dans
  l'invitation ; en mode avion, pas de ligne.
- « Code source » : la page GitHub du dépôt s'ouvre dans le navigateur.
