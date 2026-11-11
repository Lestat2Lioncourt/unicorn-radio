# 025 — Ouvrir dans les favoris, ranger en voyant où

Le 2026-11-11. Deux demandes du même jour.

**Les favoris au lancement.** Mettre les favoris à la racine a été écarté :
avec cinquante favoris, il faudrait défiler pour atteindre les dossiers.
Garder « Mes favoris » comme dossier, mais y ouvrir l'écran.

**Ranger en voyant où.** Sur le bureau, en rangeant une radio, les
catégories qui la contiennent déjà sont assombries dans la liste. Ici,
« Ranger dans… » omettait le dossier courant et montrait les autres sans
distinction.

## Ce qui change

**Au lancement**, l'écran s'ouvre dans « Mes favoris » quand il existe ;
la flèche de retour remonte à la racine, un second retour quitte. Seulement
au premier chargement : ensuite, l'endroit où l'on est est gardé comme
avant. Une activité recréée (rotation, langue) garde aussi l'endroit.

**« Ranger dans… » et « Déplacer vers… »** listent tous mes dossiers ;
ceux qui contiennent déjà la station, celui où l'on est compris, sont en
gris et un appui n'y fait rien. « Nouveau dossier… » reste en bas.

Version 1.2.0.2, code 28, en essai ; publiée en 1.2.1 (voir 026 pour le code).

## Fichiers

`MainActivity.kt`, `app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.2.0.2.

- Fermer l'application tout à fait, la rouvrir : l'écran est dans « Mes
  favoris ». Flèche de retour : la racine, « Mes favoris » en tête et les
  dossiers dessous.
- Sans aucun favori : l'écran s'ouvre à la racine.
- Entrer dans un dossier, mettre une station en favori depuis la
  recherche, revenir : on reste dans le dossier.
- Une station rangée dans deux dossiers, appui long, « Ranger dans… » :
  ces deux dossiers en gris, un appui ne fait rien ; les autres rangent.
- Dans un dossier, « Déplacer vers… » : le dossier courant en gris.
