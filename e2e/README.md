# Campagne de captures et parcours Playwright

Automatisation du navigateur au service du **manuel utilisateur** d'abord, des tests de
non-régression ensuite. Conception, arbitrages et découpage :
[docs/PLAN-PLAYWRIGHT-E2E-ET-CAPTURES.md](../docs/PLAN-PLAYWRIGHT-E2E-ET-CAPTURES.md).

## Le principe en une phrase

Un parcours ne redécrit pas ses étapes : il se **rattache** à un identifiant de scénario du
cahier de recette (`cahier-recette.model.ts`, 476 scénarios). Les légendes des images sont
prises dans le modèle — le manuel ne peut donc pas décrire autre chose que ce qui a été exécuté.

## Commandes

| Commande | Ce qu'elle fait | Prérequis |
| --- | --- | --- |
| `npm run e2e:liage` | contrôles sur le modèle et la couverture | **aucun** |
| `npm run e2e` | joue les parcours sans prendre d'images | application démarrée |
| `npm run captures` | joue les parcours **et** produit les images | application + base de démo |
| `npm run e2e:droits` | chaque compte non-admin ouvre les écrans et onglets de son menu ; échoue sur tout 403 | application démarrée, backend en `ENFORCE` |
| `npm run e2e:rapport` | ouvre le rapport HTML de la dernière exécution | — |

`npm run e2e:liage` s'exécute sans navigateur, sans serveur et sans base : c'est le contrôle à
lancer en premier quand quelque chose paraît cassé.

## Réglages

Tous par variable d'environnement, tous facultatifs :

| Variable | Défaut | Rôle |
| --- | --- | --- |
| `E2E_BASE_URL` | `http://localhost:4200` | URL de l'application |
| `E2E_USER` / `E2E_PASSWORD` | `admin` / `admin` | compte du jeu de démonstration |
| `E2E_CAPTURES_DIR` | `e2e/captures` | destination des images |
| `E2E_CAPTURES_RESET` | — | `1` : repart d'un index vide au lieu de le compléter |
| `E2E_JPEG_QUALITY` | `80` | qualité des images |
| `E2E_VIEWPORT_WIDTH` / `E2E_VIEWPORT_HEIGHT` | `1920` / `1080` | **taille de l'écran** que l'application croit avoir |
| `E2E_SCALE` | `1` | densité de pixels — `2` pour une impression plus fine |
| `E2E_FULL_PAGE` | — | `1` : capture le document entier au lieu de la zone visible |
| `E2E_TIMEZONE` | `Europe/Paris` | **doit rester fixe** : sinon les colonnes de date changent d'une machine à l'autre |
| `E2E_COMPTES_DROITS` | `kkone`, `ybrou`, `rkouassi` (mot de passe de l'admin) | comptes du contrôle des droits, `login:motDePasse,…` |
| `E2E_WORKERS` | `1` | voir l'avertissement ci-dessous |

### Taille des captures

Deux réglages distincts, souvent confondus :

- **`E2E_VIEWPORT_*` — la taille de l'écran.** C'est elle qui décide de ce que l'application
  affiche : nombre de colonnes, repliement des barres d'outils, passage en disposition
  compacte. Défaut **1920 × 1080**, soit la définition d'un poste 24 pouces. Élargir montre
  *plus de choses*.
- **`E2E_SCALE` — la densité de pixels.** À `2`, la même mise en page est rendue deux fois
  plus finement : rien de plus n'est visible, c'est plus net à l'impression, et l'image pèse
  environ trois fois plus. Augmenter l'échelle montre *la même chose en plus net*.

Repère de poids, en JPEG qualité 80 : ~140 Ko en 1440 × 900, ~190 Ko en 1920 × 1080.

Un écran plus large n'est pas toujours plus lisible dans le manuel : quand le contenu est
court, le bas de l'image n'est que fond d'écran. Réduire `E2E_VIEWPORT_HEIGHT` (900 à 1000)
resserre l'image sans rien changer à la mise en page, qui ne dépend que de la largeur.

> **Ne pas passer les parcours en parallèle sans y réfléchir.** Ils partagent une seule base de
> démonstration et la modifient (ventes, commandes, inventaires). En parallèle, deux parcours se
> marchent dessus et produisent des captures incohérentes — un défaut qu'on impute alors à tort
> à Playwright.

## Organisation

```
e2e/
├── playwright.config.ts     quatre projets : liage, authentification, parcours, captures
├── setup/auth.setup.ts      connexion unique, session enregistrée pour tous les parcours
├── verifications/           contrôles sur le modèle — sans navigateur
├── parcours/                un fichier par scénario (voir parcours/README.md)
├── droits/                  contrôle des droits par rôle (endpoints en ENFORCE)
└── src/
    ├── config.ts            réglages
    ├── cahier-recette.ts    index des 476 scénarios, résolution par identifiant
    ├── scenario.ts          la fixture scenario() et ses garde-fous
    └── captures-reporter.ts assemble captures/captures.json
```

## Les garde-fous, et pourquoi ils existent

Sans eux, une campagne de captures produit sans broncher un manuel faux — le défaut le plus
coûteux, parce qu'un manuel est cru.

| Garde-fou | Ce qu'il empêche |
| --- | --- |
| Identifiant résolu **au chargement** du fichier | une faute de frappe qui produirait une capture orpheline |
| Collision d'identifiants dans le modèle | des images inattribuables |
| Toutes les étapes du modèle parcourues | un manuel qui saute une étape sans le dire |
| Aucune exception de page ni réponse 5xx | photographier une page d'erreur et l'appeler « écran de vente » |
| Rien n'est indexé pour un parcours en échec | une capture qui survit à l'échec qui l'a produite |

Deux échappatoires, explicites et tracées dans le rapport :

- `etape.horsPortee(n, motif)` — étape non automatisable (geste matériel, imprimante) ;
- `tolererErreurs(motif)` — l'erreur fait partie de ce qu'on veut montrer.

## Où en est le chantier

| Lot | État |
| --- | --- |
| 0 — base de démonstration chargée | fait — `pharma_smart_demo`, 240/242 contrôles, instantané de référence figé |
| 1 — socle Playwright | fait |
| 2 — liage au cahier de recette | fait, vérifié par `npm run e2e:liage` |
| 3 — captures et index | fait |
| 4 — injection des images dans le guide | fait |
| 5 — volume (50 à 70 scénarios) | **objectif dépassé — 402 parcours sur 11 modules ; 35 scénarios masqués, motif documenté dans le modèle (écran non branché, entrée de navigation désactivée, fonctionnalité sans interface) ; le jeu de démonstration est complété au fil des parcours, chaque manque étant doublé d'un contrôle dans `99_verification.sql`** |

## Avant chaque campagne

```powershell
pwsh scripts/demo-data/dump_reference.ps1 -Restore   # base à l'identique
npm run captures
npm run generate:cahier-recette
```

La restauration n'est pas facultative : les scripts de démonstration datent tout par rapport au
jour d'exécution, donc une campagne lancée un autre jour produit des écrans différents. Partir
du même instantané est ce qui rend deux éditions du manuel comparables.

**Depuis que les parcours couvrent la vente, la restauration a une seconde raison d'être :
certains ÉCRIVENT.** Une vente encaissée, une caisse ouverte puis clôturée, un utilisateur
créé — chacun remet ce qu'il peut en état (`ADM-01` supprime son compte, `VTE-01` annule son
panier), mais une vente finalisée ne s'annule pas sans laisser de trace. Après une campagne,
`99_verification.sql` ne passe donc plus : c'est normal, il contrôle un jeu de données
fraîchement chargé, pas une base sur laquelle on a travaillé.

Prérequis : le backend (port **9080**) et le serveur Angular (port 4200) démarrés.

## Vidéos des parcours

Pour montrer un parcours à un utilisateur plutôt que de le lui décrire :

```powershell
npm run videos          # filme les parcours (projet « videos »), mêmes fichiers que « parcours »
npm run generate:cahier-recette
```

Chaque parcours réussi produit `e2e/captures/<ID>/parcours.webm`, indexé dans `e2e/captures/videos.json`.
Comme pour les images, un parcours en échec ne produit aucune vidéo, et une exécution ciblée n'efface pas
les autres. Pour que la vidéo se lise comme une démonstration et non comme un test :

- la **légende de l'étape** (texte du modèle, « étape n/N ») s'affiche en bas de l'écran et survit aux navigations ;
- un **pointeur** orange suit la souris et s'épaissit au clic (Playwright ne dessine pas le curseur) ;
- des **pauses** laissent le temps de lire avant l'étape et de voir son résultat après.

| Variable | Défaut | Rôle |
| --- | --- | --- |
| `E2E_PAUSE_AVANT_MS` | 1500 | pause après affichage de la légende, avant le geste |
| `E2E_PAUSE_APRES_MS` | 1200 | pause après le geste, pour voir le résultat |
| `E2E_SLOWMO` | 200 | délai entre deux gestes (déplacement visible du pointeur) |
| `E2E_VIDEO_WIDTH` / `E2E_VIDEO_HEIGHT` | 1280 / 720 | définition de la vidéo |

Une vidéo dure ainsi de une à trois minutes ; les 400 parcours représentent plusieurs heures de campagne
et quelques centaines de Mo. Filmer un seul parcours : `npx playwright test -c e2e --project=videos -g VTE-58`.

### Une vidéo par fonctionnalité, puis par module

```powershell
npm run videos:assembler        # lit e2e/captures/videos.json, écrit target/videos-guide/
```

Les vidéos des parcours sont mises bout à bout : une vidéo **par fonctionnalité**, puis une **par module** (les
fonctionnalités à la suite). Chacune a ses chapitres (`.vtt`), qui portent les titres du guide — jamais les
références techniques comme VTE-01 : ni dans la légende de l'écran, ni dans les chapitres, ni dans la page.

```
target/videos-guide/
├── index.html                       page autonome : un lecteur par module et par fonctionnalité, chapitres cliquables
├── guide-videos.json                la même structure, pour un autre support
├── modules/<module>.mp4 + .vtt
└── fonctionnalites/<module>--<fonctionnalité>.mp4 + .vtt
```

**Vitesse.** Un test va bien plus vite qu'un utilisateur : l'assemblage ralentit donc les vidéos d'un facteur
2 par défaut (`VIDEOS_RALENTI=3` pour aller plus lentement, `1` pour garder la vitesse filmée). Cela allonge
d'autant les pauses — le temps de lire la légende — et les gestes, sans refilmer : seul `npm run videos:assembler`
est à relancer. Pour un rythme plus lent *à la source*, augmenter `E2E_PAUSE_AVANT_MS`, `E2E_PAUSE_APRES_MS` et
`E2E_SLOWMO` avant `npm run videos`.

Ce dossier se copie tel quel sur un serveur de fichiers, une clé USB ou un partage : **il n'est pas dans le jar**,
quel que soit `guide.media`. Les MP4 sont en H.264, lisibles partout. Seuls les parcours dont la vidéo existe
sont repris ; un fichier déjà à jour n'est pas refait (`--force` pour tout refaire). L'encodage utilise
`ffmpeg-static` (installé avec `npm install`) ; sur un poste où ses scripts d'installation sont bloqués,
renseigner `FFMPEG` avec le chemin d'un ffmpeg.

### Les embarquer dans le build (jar final)

Le contenu du guide embarqué dans le jar se règle par la propriété Maven `guide.media` :

| Valeur | Contenu du jar | Comment |
| --- | --- | --- |
| `none` | parcours (textes) seuls | `-Dguide.media=none` |
| `captures` | + images **(défaut)** | rien à ajouter |
| `all` | + images et vidéos | profil `avec-guide` |

```powershell
mvnw.cmd clean package -Pfull-dist,avec-guide -DskipTests              # avec JRE
mvnw.cmd clean package -Pfull-dist,sans-jre,avec-guide -DskipTests     # sans JRE
```

Les parcours (titre, besoin, étapes) sont dans `cahier-recette.json`, toujours embarqué. Les médias, eux,
viennent du dossier `e2e/captures/` : **la campagne (`npm run captures`, `npm run videos`) doit donc avoir
tourné avant le build**, sans quoi le guide est livré sans illustration ni vidéo. Le guide affiche alors,
sous chaque scénario filmé, un lecteur « Voir le parcours en vidéo » ; il n'affiche rien si le build
n'embarque pas les vidéos.

## Ce que produit une campagne

```
npm run captures
        │
        ├─ e2e/captures/VTE-01/etape-1.jpg …        images
        └─ e2e/captures/captures.json                index légendé

npm run generate:cahier-recette   (aussi lancé par chaque build Maven)
        │
        ├─ pharmaSmart-app/src/main/resources/data/cahier-recette.json   modèle + captures
        └─ pharmaSmart-app/src/main/webapp/content/captures/…            images servies
```

Les images sont alors visibles dans l'écran « Guide des fonctionnalités » et dans le PDF
téléchargeable. Le dossier `content/captures/` est un **miroir** : vidé et reconstruit à chaque
génération, jamais versionné. Un build sans campagne préalable produit donc un guide sans
illustrations — conséquence assumée du choix de ne pas versionner les captures.
