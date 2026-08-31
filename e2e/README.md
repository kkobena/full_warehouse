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
| 5 — volume (50 à 70 scénarios) | **objectif dépassé — 403 parcours sur 11 modules ; 32 scénarios masqués, motif documenté dans le modèle (écran non branché, entrée de navigation désactivée, fonctionnalité sans interface) ; le jeu de démonstration est complété au fil des parcours, chaque manque étant doublé d'un contrôle dans `99_verification.sql`** |

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
