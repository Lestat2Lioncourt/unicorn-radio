# 026 — Le nombre de téléchargements

Le 2026-11-11. « Si je veux un jour faire la promo de l'app, la somme des
téléchargements est un chiffre, complètement biaisé mais pas faux non
plus, significatif. » Puis : « On pourrait le voir dans l'app ? »

## Ce qui change

**Dans le README**, un badge shields.io affiche le total, et mène à la
page des releases (poussé à part, commit `c6790dd`).

**Dans « Invitation QRCode »**, sous le QR code : « Déjà téléchargée N
fois ». C'est l'écran qu'on montre pour donner l'application. Le total est
demandé à l'API publique de GitHub à l'ouverture de la fenêtre, sans
compte : la somme des `download_count` des APK de toutes les releases. La
ligne apparaît quand la réponse arrive ; sans réseau, ou si GitHub ne
répond pas, elle n'apparaît pas.

**Ce que le chiffre compte** : des téléchargements, pas des personnes.
Chaque mise à jour installée depuis l'application en est un, les
téléchargements du développeur aussi.

Version 1.2.0.3, code 29, en essai ; publiée en 1.2.1 (voir 027 pour le code).

## Fichiers

`miseajour/MiseAJour.kt` (`telechargements()`), `MainActivity.kt`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`, `README.md`.

## Vérification

À relancer : installer la 1.2.0.3.

- Menu « … », « Invitation QRCode » : sous le QR code, « Déjà téléchargée
  N fois », le même nombre que le badge du README.
- En anglais : « Already downloaded N times ».
- En mode avion : la fenêtre s'ouvre, sans la ligne.
