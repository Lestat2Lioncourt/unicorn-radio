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

## Côté Unicorn-TV

`"format": 1` est écrit dans la section depuis le 2026-10-25 au soir
(`server/routes/radio.js`, route `GET /api/radio/export`). Rien d'autre
n'a changé.
