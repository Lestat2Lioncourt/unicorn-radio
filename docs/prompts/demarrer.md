# Prompt de démarrage de la session Unicorn-Radio

*À coller tel quel dans une session Claude Code ouverte depuis
`D:\apps\unicorn-radio`. Écrit le 2026-10-25 depuis la session Unicorn-TV
qui a posé le socle. Le CLAUDE.md local, hors dépôt, brieffe déjà la session
sur le projet, les conventions et la chaîne de build ; ce prompt dit par
quoi commencer.*

---

Tu es dans Unicorn-Radio, une application Android de radios web, autonome,
née hier soir d'Unicorn-TV. Lis d'abord CLAUDE.md, puis docs/ROADMAP.md et
docs/format-d-echange.md, puis docs/correctifs/001-le-socle.md. Le dépôt est
public : rien d'Unicorn-TV, ni code copié, ni base, ni nom du catalogue d'un
fournisseur, n'y entre jamais.

On commence le palier 1 de la feuille de route, « lire le fichier, jouer »,
chantier 002. Le but du soir : une station de mon fichier qui joue sur mon
téléphone, hors wifi, en `http`, avec son titre, et qui continue écran éteint.

Ce que je te donne : un fichier `unicorn-config-AAAA-MM-JJ.json` exporté
d'Unicorn-TV, que je dépose dans `debug/` (ignoré par git). Ne le commite
jamais, n'en recopie aucun nom dans le dépôt.

Avant d'écrire du code, propose en quelques lignes le découpage du
chantier : les écrans (une liste de favoris et de dossiers, une ligne de
lecture en bas), le service de lecture Media3 avec sa notification, et
l'ouverture du fichier par « Ouvrir avec » et depuis les fichiers du
téléphone. Dis quelles dépendances tu ajoutes et pourquoi, puis attends mon
accord. Pas de Compose si une vue classique suffit ; tranche et dis-le.

Règles de travail, reprises d'Unicorn-TV : discussion en français, simple ;
demander avant de commencer et quand on s'écarte de la demande ; un « non »
à une demande de permission pendant que je tape n'est pas un refus, relis le
message qui suit ; grouper les appels d'outils, je suis en mode auto ;
messages de commit en ASCII, écrits dans un fichier et passés avec `-F`,
terminés par la ligne Co-Authored-By ; jamais de heredoc pour du Markdown ;
reproduire le défaut avant de corriger ; en tête de chaque liste de
vérifications, dire quoi relancer, ici réinstaller l'APK ou relancer
l'application. Chaque chantier a sa fiche dans docs/correctifs/ et sa ligne
dans docs/CHANGELOG.md avant de commiter.

Pour construire : `JAVA_HOME="C:\\Program Files\\Java\\jdk-17" ./gradlew assembleDebug`
depuis Git Bash, l'APK dans app/build/outputs/apk/debug/, à copier dans
pkg/ (ignoré). Mon téléphone est en dernière version d'Android ; s'il est
branché, `adb` est dans le SDK indiqué par CLAUDE.md, sinon je l'installe
moi-même depuis pkg/. Ne touche pas au SDK, il est en lecture seule.
