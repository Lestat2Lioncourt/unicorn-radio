# 023 — Invitation QRCode

Le 2026-11-11. « Une option QRCode qui permette de donner simplement
l'app à quelqu'un qu'on croise. » L'application s'installe par l'APK de la
release, sans compte ; encore faut-il donner l'adresse.

## Ce qui change

**« Invitation QRCode »** dans le menu « … », au-dessus de « Mise à
jour… » : une boîte avec le QR code de
`https://github.com/Lestat2Lioncourt/unicorn-radio/releases/latest`, noir
sur blanc pour se lire en thème sombre aussi, l'adresse écrite au-dessus.
La personne qui le scanne arrive sur la page de la dernière version et
touche le fichier APK. L'adresse ne porte pas de numéro : elle ne vieillit
pas d'une publication à l'autre.

**« Envoyer le lien »**, dans la même boîte, ouvre le partage d'Android
avec une phrase et l'adresse, pour qui n'est pas en face.

Le QR code est dessiné par la bibliothèque qui lit déjà celui de
l'appariement (`BarcodeEncoder`) : rien d'ajouté au projet.

Version 1.1.5, code 25.

## Fichiers

`MainActivity.kt` (`montrerLInvitation`), `res/menu/principal.xml`,
`res/values/strings.xml`, `res/values-fr/strings.xml`,
`app/build.gradle.kts`.

## Vérification

À relancer : installer la 1.1.5.

- Menu « … », « Invitation QRCode » : le QR code s'affiche, en thème clair
  comme en sombre ; scanné par un autre téléphone, il ouvre la page de la
  dernière version.
- « Envoyer le lien » : le partage d'Android s'ouvre, le message porte
  l'adresse.
- En anglais : « QR code invitation », « Send the link ».
