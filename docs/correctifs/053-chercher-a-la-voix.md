# 053 — Chercher à la voix

Le 2026-11-18. « La commande vocale sur tel Android serait
envisageable ? » « Ok Google, mets … sur Unicorn-Radio » est écarté :
Gemini, l'assistant d'aujourd'hui, ne pilote pas une application tierce,
l'utilisateur l'a constaté. « Oui go pour le micro. »

## Ce qui change

- **Un micro à droite du champ de recherche des radios.** Un appui ouvre
  la fenêtre de reconnaissance vocale d'Android, dans la langue du
  téléphone ; ce qu'elle a compris remplit le champ, et la recherche part
  aussitôt, avec le pays, la langue et le style choisis.
- L'application ne demande pas l'accès au micro : c'est la fenêtre
  d'Android qui écoute.
- Sur un téléphone sans reconnaissance vocale : « Pas de reconnaissance
  vocale sur ce téléphone. »

Version 1.7.0.1, code 77, en essai.

## Fichiers

`MainActivity.kt`, `res/layout/activity_main.xml`,
`res/drawable/ic_micro.xml` (nouveau), `res/values/strings.xml`,
`res/values-fr/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer `pkg/unicorn-radio-1.7.0.1-debug.apk`.

- Ouvrir la recherche : le micro à droite du champ.
- Un appui, dire « France Inter » : le champ se remplit, les résultats
  suivent.
- Annuler la fenêtre sans parler : rien ne change.
