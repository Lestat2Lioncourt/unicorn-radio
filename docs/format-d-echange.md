# Le fichier d'échange avec Unicorn-TV

Ce document fait foi pour les deux projets. Unicorn-TV l'écrit par son menu
« Emporter sur une autre machine » et le relit par « Rapporter » ;
Unicorn-Radio le lit, et saura l'écrire. Établi le 2026-10-25 depuis
l'export d'Unicorn-TV tel qu'il est, chantier 218 là-bas.

## L'enveloppe : le fichier de configuration d'Unicorn-TV

Ce qu'une personne a sous la main, c'est le fichier que produit
Réglages → « Emporter sur une autre machine » d'Unicorn-TV, nommé
`unicorn-config-AAAA-MM-JJ.json`. La radio y est **une section parmi
d'autres** :

```json
{
  "application": "unicorn-tv",
  "version": 2,
  "exportedAt": "2026-10-25T18:00:00.000Z",
  "local": { … },
  "server": { … },
  "bibliotheque": { … },
  "radio": { … la section décrite ci-dessous … }
}
```

Unicorn-Radio **ne lit que `radio`** et ignore tout le reste : `local` et
`server` portent les réglages et les clefs de l'installation, qui ne
regardent pas le téléphone, et `bibliotheque` les chaînes de télévision.
Depuis le même soir, Unicorn-TV sait aussi produire le fichier **réduit à
la radio**, `unicorn-radios-AAAA-MM-JJ.json` : la même enveloppe,
`application`, `version`, `exportedAt`, et la seule clef `radio`. C'est
celui qu'on donne au téléphone ou à quelqu'un : « Radios seulement » dans
ses réglages, ou « Emporter mes radios… » au clic droit dans le vide de
l'arbre de sa page radio. Unicorn-Radio lit les deux de la même façon, et
accepte encore un objet qui a `favoris` et `dossiers` à sa racine.

Quand Unicorn-Radio écrit, il écrit l'enveloppe réduite, avec
`"application": "unicorn-radio"`, `"version": 2` et `"exportedAt"`.
L'import d'Unicorn-TV accepte ce nom d'application et ne lit que `radio`.

## La section `radio`

```json
{
  "format": 1,
  "favoris": [ { …fiche, "ordre": 0 }, … ],
  "dossiers": [
    {
      "chemin": ["Musique", "Jazz"],
      "ordre": 0,
      "stations": [ { …fiche, "ordre": 0 }, … ]
    },
    …
  ],
  "podcasts": [ { "flux": "https://…/rss", "nom": "…", "auteur": "…", "image": "https://…", "ordre": 0 }, … ]
}
```

- `format` : le numéro de ce format, écrit par Unicorn-TV depuis le
  2026-10-25 au soir. **Absent dans les fichiers écrits avant : absent
  vaut 1.** Un lecteur refuse un numéro qu'il ne connaît pas, en le disant.
- `favoris` : les stations favorites, dans l'ordre.
- `dossiers` : un par dossier, **son chemin complet** du premier niveau à
  lui-même, tableau de noms. Un dossier vide a `stations: []`. L'ordre des
  dossiers frères est `ordre` ; les parents n'ont pas à précéder leurs
  enfants dans le tableau, le lecteur les trie du chemin le plus court au
  plus long.
- `podcasts` : les abonnements aux podcasts, un par ligne avec l'adresse du
  flux RSS, le nom, l'auteur, l'image et l'ordre. Unicorn-Radio ne le lit
  pas pour l'instant et le recopie tel quel s'il écrit le fichier.

Une station peut figurer dans plusieurs dossiers, et dans les favoris en
plus : c'est le même `station_uuid` à chaque fois.

## La fiche d'une station

Les champs de `FICHE` dans `server/services/mesRadios.js` d'Unicorn-TV,
tous présents, à `null` quand inconnus :

| Champ | Type | Sens |
|---|---|---|
| `station_uuid` | chaîne | l'identifiant Radio Browser, **la clef** |
| `pays_code` | chaîne | deux lettres, `??` quand inconnu |
| `pays` | chaîne | le nom du pays |
| `nom` | chaîne | le nom de la station |
| `url` | chaîne | l'adresse du flux, souvent en `http` |
| `page` | chaîne | le site de la station |
| `logo` | chaîne | l'adresse de son logo |
| `etiquettes` | chaîne | les styles, séparés par des virgules |
| `langue` | chaîne | la langue |
| `codec` | chaîne | `MP3`, `AAC`, `AAC+`, … tel que l'annuaire le dit |
| `debit` | entier | en kbit/s |
| `votes` | entier | les votes dans l'annuaire |
| `hls` | 0 ou 1 | le flux est une liste HLS |

Et à côté de la fiche, `ordre`, entier, la place dans la liste.

## Ce qu'un import fait

Du côté d'Unicorn-TV, **rien n'est effacé** : une station inconnue est
ajoutée à sa copie de l'annuaire depuis sa fiche, un dossier absent est
créé, une station absente d'un dossier y est ajoutée à la suite, dans
l'ordre du fichier. Rejouer le même fichier ne fait rien de plus. Une ligne
sans `station_uuid`, ou sans `url` quand la station est inconnue, est
passée. Unicorn-Radio fait de même.

## Exemple

Noms et adresses inventés. La section seule ; dans le fichier de
configuration, le même objet est sous la clef `radio`.

```json
{
  "format": 1,
  "favoris": [
    {
      "station_uuid": "0f1e2d3c-0000-4000-8000-000000000001",
      "pays_code": "FR", "pays": "France",
      "nom": "Radio Exemple", "url": "http://exemple.invalid/flux.mp3",
      "page": "https://exemple.invalid/", "logo": null,
      "etiquettes": "jazz, soul", "langue": "french",
      "codec": "MP3", "debit": 128, "votes": 12, "hls": 0,
      "ordre": 0
    }
  ],
  "dossiers": [
    { "chemin": ["Musique"], "ordre": 0, "stations": [] },
    {
      "chemin": ["Musique", "Jazz"], "ordre": 0,
      "stations": [
        {
          "station_uuid": "0f1e2d3c-0000-4000-8000-000000000001",
          "pays_code": "FR", "pays": "France",
          "nom": "Radio Exemple", "url": "http://exemple.invalid/flux.mp3",
          "page": "https://exemple.invalid/", "logo": null,
          "etiquettes": "jazz, soul", "langue": "french",
          "codec": "MP3", "debit": 128, "votes": 12, "hls": 0,
          "ordre": 0
        }
      ]
    }
  ],
  "podcasts": []
}
```

## La synchronisation avec un serveur

Décidé le 2026-10-26. Le fichier reste le moyen d'échange pour qui n'a pas
de serveur, ou pour donner une liste à quelqu'un. Pour la personne qui a
son Unicorn-TV, l'application peut s'**apparier** à lui : le serveur est
alors **le seul maître** de la liste de cet utilisateur, favoris, dossiers
et podcasts ; l'application en garde une copie, la modifie hors ligne en
notant ses **gestes**, et se remet d'accord avec le serveur dès qu'elle le
voit. Aucun fichier hébergé ailleurs, aucun compte tiers.

Chaque personne a son serveur, et deux serveurs peuvent cohabiter sur le
même wifi : l'application ne cherche donc jamais « le » serveur du réseau,
elle est appariée à un serveur précis, reconnu par son identité.

### L'identité du serveur

`GET /api/radio/serveur`, sans authentification, répond :

```json
{
  "application": "unicorn-tv",
  "serveur": "6c1f…",
  "nom": "Salon",
  "format": 1
}
```

- `serveur` : un identifiant créé à l'installation, stable, jamais
  réutilisé. C'est lui que l'application compare, pas l'adresse.
- `nom` : le nom que l'installation se donne, pour l'affichage.
- `format` : le format de section que ce serveur parle.

### L'appariement

Dans Unicorn-TV, le bloc « Écouter sur le téléphone… » d'un dossier (ou
des réglages) montre un **QR code d'appariement** et, à côté, ce qu'il
porte en clair pour une saisie à la main. Le QR code contient ce JSON :

```json
{
  "application": "unicorn-tv",
  "appariement": 1,
  "serveur": "6c1f…",
  "nom": "Salon",
  "adresses": ["http://192.168.1.20:3000", "http://10.5.0.2:3000"],
  "code": "7KD4PM"
}
```

- `adresses` : toutes les adresses locales du serveur, câble et wifi, comme
  la page téléphone les liste déjà. L'application essaie dans l'ordre et
  garde la première qui répond à `GET /api/radio/serveur` avec le bon
  `serveur`.
- `code` : un code d'appariement court, six caractères, valable dix
  minutes, à usage unique. C'est lui qu'on tape si la caméra manque, avec
  l'une des adresses.

L'application échange ensuite le code contre un jeton :

```
POST /api/radio/appariement
{ "code": "7KD4PM", "appareil": { "id": "a3b2…", "nom": "Téléphone de Lou" } }
```

Réponse :

```json
{ "jeton": "…", "serveur": "6c1f…", "nom": "Salon", "utilisateur": "lou" }
```

- `appareil.id` : un identifiant que l'application crée une fois pour
  toutes et garde ; `appareil.nom`, ce qu'elle affiche.
- `jeton` : secret, long, propre à ce couple appareil–utilisateur. Le
  serveur ne le garde que haché, et le montre une seule fois. Il n'expire
  pas ; il se révoque.

L'application retient `serveur`, `adresses`, `jeton`, `utilisateur`.
Toutes les routes qui suivent portent `Authorization: Bearer <jeton>` ;
une réponse `401` veut dire que le jeton a été révoqué : l'application
repasse en mode fichiers et le dit.

`DELETE /api/radio/appariement`, avec le jeton, le révoque depuis le
téléphone. Dans Unicorn-TV, les réglages listent les appareils appariés,
par nom et date, avec un bouton pour révoquer.

**Au moment de l'appariement**, l'application verse sa liste « Mes
favoris » locale au serveur, comme autant de gestes `favori.ajouter`, puis
tire la copie du serveur, qui remplace les fichiers importés. Tant qu'elle
n'est pas appariée, elle reste en mode fichiers, avec ses favoris locaux.

### Tirer : la copie du serveur

`GET /api/radio/export`, avec le jeton, rend **la section seule** telle
que décrite plus haut, `format` à 1. C'est la même route que le fichier
« Radios seulement », qui accepte désormais le jeton en plus de la session
du navigateur.

L'application remplace sa copie par ce qu'elle reçoit. Elle tire au
démarrage, après chaque rejeu de gestes, et quand on lui demande.

### Renvoyer : le journal des gestes

Tout ce que l'utilisateur fait sur le téléphone est un **geste**, écrit sur
le disque à l'instant même, dans l'ordre. En ligne, le journal part tout
de suite ; hors ligne, il attend. Jamais l'application ne renvoie sa liste
entière : elle écraserait ce que la télé a fait entre-temps.

```
POST /api/radio/gestes
{
  "appareil": "a3b2…",
  "gestes": [
    { "id": "f0a1…", "quand": "2026-10-26T09:12:03.120Z", "type": "favori.ajouter", "station": { …fiche… } },
    { "id": "f0a2…", "quand": "2026-10-26T09:12:40.004Z", "type": "dossier.ajouter", "chemin": ["Musique", "Jazz"], "station": { …fiche… } },
    { "id": "f0a3…", "quand": "2026-10-26T09:13:01.900Z", "type": "dossier.retirer", "chemin": ["Musique"], "station_uuid": "0f1e…0001" }
  ]
}
```

Chaque geste porte :

- `id` : un identifiant unique, créé par l'application. Le serveur garde
  ceux qu'il a vus, par appareil : **un geste déjà vu est accepté et
  ignoré**. Rejouer deux fois le même journal après une coupure ne change
  rien.
- `quand` : l'horodatage du geste, ISO 8601 en UTC. Le serveur l'enregistre
  avec le geste ; il n'en fait pas d'autre usage aujourd'hui.
- `type`, et les champs du type :

| Type | Champs | Effet |
|---|---|---|
| `favori.ajouter` | `station` (fiche) | ajoute à la fin des favoris ; déjà là, rien |
| `favori.retirer` | `station_uuid` | retire des favoris ; absent, rien |
| `favoris.ordonner` | `station_uuids` | met dans cet ordre celles qui y sont ; les autres suivent |
| `dossier.creer` | `chemin` | crée le dossier et ses parents manquants ; existe, rien |
| `dossier.renommer` | `chemin`, `nom` | renomme le dernier élément du chemin ; absent, refusé |
| `dossier.supprimer` | `chemin` | supprime le dossier et ce qu'il contient ; absent, rien |
| `dossier.ajouter` | `chemin`, `station` (fiche) | ajoute à la fin du dossier, créé au besoin ; déjà là, rien |
| `dossier.retirer` | `chemin`, `station_uuid` | retire du dossier ; absent, rien |
| `dossier.deplacer` | `chemin`, `station_uuid`, `vers` (chemin) | retire d'un dossier, ajoute à la fin de l'autre |
| `dossier.ordonner` | `chemin`, `station_uuids` | comme `favoris.ordonner`, dans ce dossier |

Une `station` est une fiche complète, comme dans le fichier : le serveur
l'ajoute à sa copie de l'annuaire s'il ne la connaît pas. Un geste refusé
l'est seul, les autres passent.

Réponse :

```json
{ "acceptes": 2, "deja_vus": 1, "refuses": [ { "id": "f0a2…", "raison": "dossier inconnu" } ] }
```

Après la réponse, l'application efface du journal les gestes acceptés,
déjà vus ou refusés, puis tire la copie du serveur.

**Les conflits.** Le serveur applique les gestes dans l'ordre où ils
arrivent, sans les replacer dans sa propre histoire : un journal rejoué
après coup s'applique **par-dessus** ce que la télé a fait entre-temps.
Deux gestes sur des éléments différents ne se gênent pas ; sur le même
élément, le dernier appliqué gagne, donc celui du téléphone. C'est le prix
d'une règle simple, et le cadre convenu, une seule personne qui modifie,
le rend rare.

**Le rejeu.** À l'ouverture de l'application d'abord, dès qu'un serveur
répond avec la bonne identité : c'est la garantie. Une tâche de fond, qui
rejouerait au retour du réseau même application fermée, est un confort à
décider plus tard.

### Ce que ça demande à Unicorn-TV

À faire là-bas, rien n'existe encore : l'identité du serveur et son nom,
la route `GET /api/radio/serveur`, le QR code d'appariement et le code
court, les routes d'appariement et de révocation, le jeton accepté par
`GET /api/radio/export`, la route `POST /api/radio/gestes` avec la mémoire
des gestes vus, et la liste des appareils dans les réglages.

## Côté Unicorn-TV

`"format": 1` est écrit dans la section depuis le 2026-10-25 au soir
(`server/routes/radio.js`, route `GET /api/radio/export`), et le fichier
réduit « Radios seulement » existe depuis le 2026-10-26 (221 là-bas). La
synchronisation décrite ci-dessus n'est pas encore construite d'aucun
côté ; ce contrat la précède.
