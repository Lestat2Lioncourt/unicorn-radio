# Le fichier d'échange avec Unicorn-TV

Ce document fait foi pour les deux projets. Unicorn-TV l'écrit par son menu
« Emporter sur une autre machine » et le relit par « Rapporter » ;
Unicorn-Radio le lit, et saura l'écrire. Établi le 2026-10-25 depuis
l'export d'Unicorn-TV tel qu'il est, chantier 218 là-bas.

## Un fichier JSON, un objet

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
  "podcasts": …
}
```

- `format` : le numéro de ce format. **Absent dans les fichiers d'Unicorn-TV
  écrits avant que ce document existe : absent vaut 1.** Un lecteur refuse
  un numéro qu'il ne connaît pas, en le disant.
- `favoris` : les stations favorites, dans l'ordre.
- `dossiers` : un par dossier, **son chemin complet** du premier niveau à
  lui-même, tableau de noms. Un dossier vide a `stations: []`. L'ordre des
  dossiers frères est `ordre` ; les parents n'ont pas à précéder leurs
  enfants dans le tableau, le lecteur les trie du chemin le plus court au
  plus long.
- `podcasts` : les abonnements aux podcasts, réservé ; Unicorn-Radio ne le
  lit pas pour l'instant et le recopie tel quel s'il écrit le fichier.

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

Noms et adresses inventés.

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

## À faire côté Unicorn-TV

Écrire `"format": 1` dans l'export. Rien d'autre ne change.
