# 047 — Le mode télé

Le 2026-11-14. « Le mode télé pourrait être mis en place sur l'app
téléphone, non ? Si une personne diffuse son téléphone sur un écran
connecté, par exemple. »

## Ce qui n'allait pas

Le spectre en grand (046) se voyait de près : textes moyens, barres
d'Android visibles, et en portrait si le téléphone était tenu droit.

## Ce qui change

- **Le mode télé** remplace le spectre en grand : un appui sur la bande
  du spectre, ou « Mode télé » dans le menu de l'appui long sur le logo.
- **Pour être vu de loin** : le spectre en grand, en bas le logo (ou les
  initiales), le nom en gros et le titre, l'heure en haut à droite, sur
  fond noir. Le titre et le logo suivent quand ils changent.
- **Sur le téléphone** : tout l'écran, les barres d'Android cachées (un
  glissement depuis le bord les montre un instant), en paysage même
  tenu droit ; l'orientation revient en sortant. L'écran reste allumé.
  Un appui ferme, un appui long ouvre les réglages du spectre.
- **Avec un second écran** (câble USB-C vers HDMI, Miracast sur les
  téléphones qui l'ont encore), le mode télé s'ouvre sur cet écran, un
  message le dit, et le téléphone reste libre. Le menu du logo propose
  alors « Fermer le mode télé sur l'autre écran ». Il se ferme aussi
  quand l'application se ferme. Un Chromecast recopie l'écran du
  téléphone : ce n'est pas un second écran, le mode télé s'y voit parce
  qu'il est sur le téléphone.
- La même vue servira à la box Android TV.

Version 1.5.0, code 69, en essai.

## Fichiers

`ModeTele.kt` (nouveau), `ReglagesSon.kt` (le spectre en grand retiré),
`MainActivity.kt`, `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.5.0-debug.apk`.

- Une station joue, téléphone tenu droit : appui sur la bande du
  spectre. L'écran passe en paysage, sans barres d'Android : le spectre,
  le logo, le nom, le titre, l'heure.
- Le titre change sur la station : il suit à la seconde.
- Une station sans logo : ses initiales dans un cercle.
- Appui long : les réglages du spectre ; un changement se voit derrière.
- Appui : fermé, le téléphone revient en portrait, les barres reviennent.
- Appui long sur le logo de la ligne de lecture : « Mode télé » en
  dernier, il ouvre la même chose.
- Diffusé sur un Chromecast : la télé montre le mode télé.
- Si possible, câble USB-C vers HDMI : « Mode télé sur « … » », la télé
  le montre, le téléphone reste utilisable ; le menu du logo dit
  « Fermer le mode télé sur l'autre écran » et le ferme.
