# 045 — « Vous êtes le n-ième utilisateur »

Le 2026-11-14. « Un tip inutile mais amusant. La donnée sera faussée,
c'est juste pour le fun. À la première installation de l'app, on
mémorise le nombre de téléchargements à ce moment, et l'installation
conserve la valeur : "Vous êtes le n-ième utilisateur de l'app". Cette
valeur ne doit pas être modifiée si on met à jour l'application. »

## Ce qui change

- **Au premier lancement**, l'application lit sur GitHub le total des
  téléchargements des APK de toutes les releases, comme pour la ligne
  « Déjà téléchargée N fois », et le garde comme rang. Une fenêtre
  « Bienvenue ! Vous êtes le 37e utilisateur d'Unicorn-Radio. » le dit,
  une seule fois.
- **Gardé pour toujours** : une mise à jour ne le touche pas, il n'est
  jamais relu. Sans réseau, rien n'est noté et l'application réessaie aux
  lancements suivants, jusqu'à ce que GitHub réponde.
- **Dans « À propos »**, la phrase reste, au-dessus du total d'aujourd'hui.
- **Ceux qui avaient déjà l'application** reçoivent leur rang à la
  première ouverture de cette version : un peu trop grand, c'est le jeu.
- « 1er », « 37e » en français ; « 1st », « 2nd », « 3rd », « 37th » en
  anglais.

### Le rang de l'auteur

Le même jour : « Comment éditer ou modifier le numéro d'utilisateur ? En
tant qu'auteur, je veux pouvoir avoir le numéro 1. »

- **Dans une version d'essai** (`BuildConfig.DEBUG`), un appui long sur la
  phrase du rang, dans « À propos », ouvre « Changer mon rang » : un
  nombre, gardé comme le premier, et la phrase change aussitôt.
- **Dans une version publiée**, l'appui long ne fait rien : seuls les APK
  d'essai, que l'auteur est seul à installer, le permettent. Le rang
  choisi reste quand on passe ensuite à la version publiée.

Le chiffre est faux par nature : GitHub compte aussi les mises à jour et
les essais. Rien de nouveau ne part : c'est la page des releases que
l'application lit déjà.

Version 1.4.0.5, code 62, en essai ; le rang de l'auteur en 1.4.0.6,
code 63.

## Fichiers

`miseajour/Rang.kt` (nouveau), `MainActivity.kt`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.4.0.6-debug.apk` par-dessus
l'actuelle.

- À l'ouverture : « Bienvenue ! Vous êtes le Ne utilisateur
  d'Unicorn-Radio. », N étant le total des téléchargements GitHub.
- Fermer, rouvrir : la fenêtre ne revient pas.
- « À propos » : la phrase, puis « Déjà téléchargée … fois ».
- Installer la version d'essai suivante par-dessus : pas de fenêtre, le
  même rang dans « À propos », même si le total a monté.
- En anglais (drapeau) : « You are the 37th user of Unicorn-Radio. »
- « À propos », appui long sur la phrase du rang : « Changer mon rang »,
  taper 1, « Valider » : « Vous êtes le 1er utilisateur ». Fermer,
  rouvrir « À propos » : toujours le 1er.
- Après la release, installée par-dessus : toujours le 1er, et l'appui
  long ne fait plus rien.
