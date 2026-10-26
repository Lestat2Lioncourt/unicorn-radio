# 003 — Chercher dans l'annuaire

Le 2026-10-26, le palier 2 de `ROADMAP.md` : « recherche par nom, parcours
par pays et par style, sur Radio Browser directement, avec les mêmes
identifiants ; l'étoile met en favori ; le compteur d'écoute de l'annuaire
est honoré. » Trois choix posés avant d'écrire et acceptés : la recherche
dans le même écran que la liste, pour garder la ligne de lecture ; les
favoris du téléphone dans un fichier à part des fichiers importés ; une
ligne par résultat, sans regrouper les déclinaisons.

## Ce qu'il y avait

L'application lisait des fichiers d'export et jouait leurs stations
(chantier 002). Elle ne connaissait que ce qu'on lui apportait.

## Ce qui est fait

**L'annuaire** (`annuaire/Annuaire.kt`), sans bibliothèque : `HttpURLConnection`
et `org.json`. Ce que Radio Browser demande est fait : un user agent qui
nomme l'application et son dépôt ; les serveurs trouvés par le DNS de
`all.api.radio-browser.info`, par leurs noms inverses, puis le nom collectif
lui-même, puis un serveur de secours, chacun essayé à son tour, retenus une
heure ; délai de vingt secondes. Trois appels : la recherche, nom, pays et
étiquette en une requête, triée par votes, stations cassées exclues, deux
cents au plus ; les pays avec leur nombre de stations ; les étiquettes les
plus portées, quatre-vingts. Les fiches deviennent des `Station` avec les
champs du fichier d'échange, `url_resolved` pour l'adresse.

**L'écoute comptée** : dans le service, à chaque station lancée, un appel
`/json/url/<uuid>` dans un fil à part, pour toute station, du fichier ou de
l'annuaire. L'adresse à jour qu'il rend est notée dans le journal quand
elle diffère ; la lecture déjà partie n'est pas interrompue.

**La recherche à l'écran** : la loupe de la barre fait passer la liste en
mode recherche. Un champ pour le nom, qui cherche trois cents millisecondes
après la dernière lettre et à partir de deux ; dessous, deux listes
déroulantes, le pays, celui du téléphone choisi d'avance, et le style, avec
« Tous » en tête de chacune ; un filtre relance tout de suite. Une ligne
d'état dit où on en est : quoi faire, « Recherche… », le nombre trouvé,
« Aucune station », ou l'annuaire injoignable et pourquoi. Une réponse qui
arrive après une recherche plus récente est ignorée. Les résultats sont des
lignes de station comme ailleurs, avec une étoile au bout ; appuyer joue et
la liste des résultats devient la file du lecteur. Le retour rend la liste
où l'on était. L'écran vide propose aussi la recherche.

**Mes favoris** (`fichier/MesFavoris.kt`, `fichier/EcritureFichier.kt`) :
la liste choisie sur le téléphone, écrite dans le stockage privé au format
d'échange, section seule, `application` à `unicorn-radio`, `format` à 1,
une fiche complète par station, dossiers vides pour l'instant. L'étoile
pleine ou vide dit si la station y est ; appuyer l'ajoute ou la retire, un
message le confirme. Dès qu'elle n'est pas vide, la liste apparaît à la
racine sous « Mes favoris », avec une étoile pour icône, à côté des
fichiers importés, qui ne sont jamais modifiés. Dans cette liste, un appui
long sur une station propose de la retirer, avec confirmation : demandé au
premier essai, « sans avoir à rechercher la chaîne », et sans étoile dans
les lignes, « lourdingue ». L'écriture servira telle quelle au palier 3.

Version 0.3.0, code 3. Aucune dépendance ni permission ajoutée.

## Ce qui reste pour plus tard

- Grouper les résultats par nom avec les déclinaisons en pastilles, comme
  la recherche du bureau.
- L'étoile sur les stations des fichiers importés, et les dossiers du
  téléphone : palier 3.
- Les étiquettes sont les plus portées du monde entier, pas celles du pays
  choisi.

## Fichiers

`app/src/main/kotlin/org/unicornradio/app/annuaire/Annuaire.kt`,
`fichier/MesFavoris.kt`, `fichier/EcritureFichier.kt`, `MainActivity.kt`,
`ListeAdaptateur.kt`, `modele/Modele.kt`, `lecture/ServiceLecture.kt`,
`app/src/main/res/layout/activity_main.xml`, `layout/ligne_station.xml`,
`menu/principal.xml`, `drawable/ic_loupe.xml`, `ic_etoile.xml`,
`ic_etoile_vide.xml`, `values*/strings.xml`, `app/build.gradle.kts`.

## Vérification

À relancer : installer l'APK `pkg/unicorn-radio-0.3.0-debug.apk` par-dessus
l'ancien.

- La loupe : le champ et les deux listes apparaissent, le pays du téléphone
  est choisi, la ligne d'état dit quoi faire.
- Sans rien taper, le pays seul liste ses stations les plus votées ; choisir
  un style les filtre ; « Tous les pays » avec un style donne le monde.
- Taper un nom : rien avant deux lettres, puis les résultats au bout d'un
  instant ; une frappe rapide ne fait pas clignoter la liste.
- Appuyer sur un résultat : il joue, la ligne du bas le montre ; « suivant »
  sur l'écran de verrouillage passe au résultat d'après.
- L'étoile : pleine, un message « Ajoutée aux favoris » ; le retour montre
  « Mes favoris » à la racine avec la station ; l'étoile à nouveau la retire.
- Dans « Mes favoris », un appui long sur une station propose de la retirer ;
  la dernière retirée, la liste disparaît de la racine. Ailleurs, l'appui
  long sur une station ne fait rien.
- Wifi coupé et données coupées : la ligne d'état dit « Annuaire
  injoignable » et la liste des dossiers marche toujours.
- Dans Logcat, `annuaire : serveurs [...]` au premier appel, et à chaque
  lecture lancée rien, ou « l'annuaire donne une autre adresse ».
