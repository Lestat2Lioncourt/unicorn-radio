# 032 — Le paysage

Le 2026-11-12. « En mode paysage, l'app est quasi inutilisable : la place
occupée par le menu en haut et le player en bas ne laisse que 2 lignes
pour faire défiler le détail des radios. »

## Ce qui n'allait pas

Tout s'empilait : la barre, les onglets, les panneaux de recherche, la
liste, la ligne de lecture et, pour un épisode, celle d'avancement. En
paysage, la hauteur manque, et la liste n'avait que ce qui restait.

Et tourner le téléphone recréait l'écran : le dossier ouvert, l'onglet, la
recherche en cours se perdaient. Personne ne le voyait, le paysage n'étant
pas utilisable.

## Ce qui change

**En paysage**, les mêmes vues rangées autrement (`disposer()`) :

```
┌──────────────────┬──────────────────────────────┐
│ ←      🔍 🇫🇷🇬🇧 ⋮ │                              │
│ Le nom du        │  la liste, stations          │
│ dossier, entier  │  ou épisodes, sur toute      │
│ [Radio|Podcasts] │  la hauteur                  │
│ (recherche…)     │                              │
├──────────────────┴──────────────────────────────┤
│ ▶ la station, le titre qui défile    ⏱ ⏮ ⏸ ⏭    │
│ 15 ━━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━  -12:34 1× 30 │
└─────────────────────────────────────────────────┘
```

- en haut à gauche, sur deux cinquièmes de la largeur : la barre avec
  seulement le retour ou le logo et les icônes ; dessous, le titre de
  l'écran (le dossier, le podcast, « Annuaire »…) en entier, sur trois
  lignes au besoin, et son sous-titre ; puis les onglets, puis les
  panneaux de recherche (radios, podcasts, épisodes) quand ils sont
  ouverts ;
- en haut à droite, la liste, jusqu'en haut de l'écran ;
- en bas, sur toute la largeur, le lecteur et la ligne d'avancement d'un
  épisode : le titre en cours se lit.

Trois essais pour y arriver : d'abord la liste à gauche et une colonne à
droite avec recherche et lecteur, les onglets dans la barre ; puis la
colonne passée à gauche, « pour voir les chaînes et épisodes sur la
droite » ; enfin le lecteur sur toute la largeur, « sinon on ne voit
quasi pas le nom du morceau », et la barre réduite à la partie gauche
pour compenser. Puis, la colonne ayant de la place en hauteur et le
titre étant tronqué à côté des icônes : la barre ne garde que les
icônes, le titre passe dessous, en entier (`titrer()` le met dans la
barre en portrait, dessous en paysage).

**Le portrait ne change pas.**

**Tourner ne recrée plus l'écran** (`configChanges` sur l'activité) :
`onConfigurationChanged` range les vues pour la nouvelle orientation, et
tout reste, onglet, dossier, recherche, champs, résultats. Un seul
layout : pas de second fichier `layout-land` à tenir à jour. À chaque
rotation, tout est détaché puis rattaché à des conteneurs neufs : le
deuxième essai plantait au second passage en paysage, une colonne
encore accrochée à l'ancien conteneur.

**Le clavier en paysage** ne recouvre plus tout l'écran pour saisir
(`flagNoExtractUi`) : les résultats restent visibles en tapant. Et tant
que le clavier est ouvert, il ne laisse qu'une bande en haut : le lecteur
du bas et le titre de la colonne s'effacent, pour que le champ de saisie
reste visible (`suivreLeClavier()`, `adjustResize`) ; à l'essai, le champ
était caché sous le clavier.

Version 1.3.0.6, code 39, puis 1.3.0.7, code 40, 1.3.0.8, code 41, et
1.3.0.9, code 42, puis 1.3.0.10, code 43, en essai ; publiée en 1.3.1,
code 44.

## Fichiers

`MainActivity.kt`, `res/layout/activity_main.xml`, `res/values/ids.xml`, `AndroidManifest.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.3.0.10.

- Paysage : à gauche les icônes, le titre en entier dessous, les onglets ;
  dans un dossier au nom long, le nom se lit en entier. À droite la liste jusqu'en
  haut.
- Lancer une station : le lecteur en bas sur toute la largeur, le titre
  en cours lisible ; la liste montre bien plus de deux lignes.
- La loupe : le panneau de recherche sous la barre, à gauche ; taper un
  nom : le champ et ce qu'on tape restent visibles, le lecteur du bas
  s'efface le temps de la saisie et revient à la fermeture du clavier.
- Onglet Podcasts, un podcast ouvert, un épisode en lecture : le filtre
  des épisodes à gauche, la ligne d'avancement en bas sur toute la
  largeur.
- Paysage, portrait, paysage, portrait, plusieurs fois : pas de plantage.
- Dans un dossier, ou en pleine recherche : tourner dans les deux sens,
  on reste au même endroit, résultats compris.
- Portrait : comme avant.
