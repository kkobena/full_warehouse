# Plan de migration vers `ChangeDetectionStrategy.OnPush`

État au 22 août 2026. Portée : la totalité de `pharmaSmart-app/src/main/webapp/app`.

## Pourquoi ce document existe

Le 22 août 2026, les onglets « Balance caisse réelle », « Rapport TVA réel » et « Tableau
pharmacien réel » du module Retraitement du CA restaient bloqués sur un spinner alors que le serveur
répondait en 100 ms. La cause n'était ni le réseau ni le backend :

- ces trois écrans pilotent leur état par des **propriétés mutées** (`this.loading = false`) ;
- une réponse HTTP ne déclenche aucun événement Angular, donc **rien ne marque leur vue à
  rafraîchir** ;
- ils dépendaient d'une traversée complète de l'arbre, que leur parent — un layout `OnPush` — ne
  déclenchait jamais.

**La règle à retenir : une vue `OnPush` non salie est sautée avec toute sa descendance.** Un
composant qui ne se salit pas lui-même ne fonctionne que si *aucun* de ses ancêtres n'est `OnPush`.
Cette dépendance est invisible à la lecture du composant, ne casse rien tant que les layouts restent
`Eager`, et se manifeste le jour où quelqu'un écrit un écran moderne — sous la forme d'un spinner
qui tourne indéfiniment, sans erreur en console.

Les trois écrans ont été convertis aux signaux le même jour. Ce plan traite les 426 autres.

## Rappel : `Eager`, `OnPush`, et ce qui a changé en Angular 22

`ChangeDetectionStrategy.Default` est **déprécié** et vaut désormais `Eager` (même valeur, `1`).
Surtout, **`OnPush` est la stratégie par défaut** quand `changeDetection` n'est pas renseigné —
l'inverse de ce que faisaient les versions précédentes.

| | vérifié quand |
|---|---|
| `Eager` | à chaque cycle où la traversée atteint le composant |
| `OnPush` | seulement si la vue est *sale* |

Une vue devient sale dans quatre cas : un **signal lu dans le gabarit** change, un **`input()`**
reçoit une nouvelle référence, un **événement** part de la vue, ou `markForCheck()` est appelé
(directement ou par `| async`). Les trois premiers marquent aussi tous les ancêtres, ce qui rouvre
le chemin de descente.

Conséquence pratique : **un composant qui n'expose que des signaux est correct sous les deux
stratégies.** La migration ne consiste donc pas à « passer en OnPush », mais à *rendre les
composants indifférents à la stratégie de leurs ancêtres*. Le changement d'annotation n'en est que
la conclusion.

## État des lieux

429 composants — la totalité du dépôt front. Les deux dossiers d'écrans, `app/features` (224) et
`app/entities` (139), plus le socle (66).

| stratégie | `features` | `entities` | socle | total |
|---|---:|---:|---:|---:|
| `Eager` (explicite) | 202 | 122 | 30 | **354** |
| `OnPush` | 17 | 3 | 12 | **32** |
| absente — donc `OnPush` par défaut en v22 | 5 | 14 | 24 | **43** |

« socle » recouvre `app/home`, `app/layouts/{main,navbar,sidebar}`, `app/login`, `app/shared`,
`app/admin` et `app/account`.

`entities` est le dossier historique, `features` le dossier récent : la proportion de composants
déjà aux signaux y est plus faible (3 sur 139 contre 17 sur 224), et la densité d'état muté plus
forte.

### Le socle est déjà sain — sauf trois fichiers

Bonne nouvelle, et elle change l'ordre des priorités : **le Design System n'a rien à migrer.** Les
24 composants de `app/shared` sans annotation — `app-button`, `app-data-table`, `app-input-number`,
`app-select`, `app-badge`, `app-skeleton`… — sont **entièrement construits sur des signaux** et des
`input()`. Ils sont donc corrects sous `OnPush`, qu'ils subissent déjà par défaut. Aucun état muté
lu dans un gabarit n'y a été trouvé. Douze autres composants de `shared` sont explicitement
`OnPush`.

| zone | composants | déjà `OnPush` | sans annotation | bascule directe | conversion requise | champs |
|---|---:|---:|---:|---:|---:|---:|
| `shared` | 50 | 12 | 24 | 30 | 8 | 18 |
| `home` | 4 | 0 | 0 | 3 | 1 | 44 |
| `layouts/navbar` | 1 | 1 ✅ | 0 | 0 | 0 | 0 |
| `layouts/sidebar` | 1 | 1 ✅ | 0 | 0 | 0 | 0 |
| `layouts/main` | 1 | 1 ✅ | 0 | 0 | 0 | 0 |
| `login` | 1 | 0 | 0 | 1 | 0 | 0 |
| `account` | 5 | 0 | 0 | 4 | 1 | 1 |
| `admin` | 3 | 0 | 0 | 2 | 1 | 1 |

Deux points sortent de ce tableau.

**`home/home-base` porte 44 champs à convertir** — le plus gros du dépôt, loin devant les 21 de
`lot-perimes`. C'est un fichier de 621 lignes qui agrège tableaux de bord, graphiques et
classements. À traiter seul, dans une séance dédiée, et sûrement pas en fin de journée.

**`main`, `navbar` et `sidebar` sont faits** (22 août 2026). Ils ne totalisaient que 5 champs —
`navItems`, `version`, `isTauriMode` — mais formaient la coquille de l'application, montée en
permanence autour de tous les écrans : les retirer de l'équation en premier évite d'avoir à se
demander, pour chaque écran migré ensuite, si le problème vient de lui ou de son cadre.

`navItems` méritait le détour : il était affecté depuis un `effect()`, le store de navigation se
chargeant après le premier rendu. Sous `OnPush` et sans signal, le menu serait resté vide.

Reste dans `shared` huit conversions pour 18 champs, dont `app-settings-dialog` (7 champs) et
`backend-splash` (3) — deux composants qui affichent justement des états de progression asynchrones,
exactement le motif qui casse sous `OnPush`.

`admin` et `account` sont anecdotiques : 8 composants, 6 bascules directes et 2 champs signalés,
dont un faux positif. `password-reset-init` expose un `FormGroup` construit une fois dans le
constructeur — les formulaires réactifs se rafraîchissent par leurs propres événements, il n'y a
rien à convertir. Seul `user-management-update` a un vrai cas : `isAdmin`, affecté après la réponse
du service utilisateur, à passer en signal.

### Le point le plus urgent : les 19 composants d'écrans sans annotation

Ils sont **déjà** soumis à `OnPush` sans que personne l'ait décidé — c'est le changement de défaut
d'Angular 22 qui les y a fait basculer, silencieusement. Rien ne garantit qu'ils se salissent
correctement, et ce sont les seuls du parc où le risque est *actuel* plutôt que futur.

Dans `features` (5) :

- `commande/feature/bon-entree-diverse/ui/bed-lignes/bed-lignes.component.ts`
- `commande/feature/semois-suggestions/ui/semois-commander-modal/semois-commander-modal.component.ts`
- `products/ui/produit-historique-tab/produit-historique-tab.component.ts`
- `sales/feature/sales-kpi-dashboard/sales-kpi-dashboard.component.ts`
- `sales/ui/sale-type-selector/sale-type-selector.component.ts`

Dans `entities` (14) : les cinq widgets de `dashboard/widgets/`, `commande/btn/commande-btn`, les
deux `commande/retour_fournisseur/`, les deux `facturation/groupe-facture-detail/`,
`produit/detail-form-dialog`, `sales/remise-list-dialog`, et les deux
`sales/uninsured-customer-list/`.

**Tous ne sont pas cassés pour autant**, et c'est important pour ne pas perdre du temps : deux
familles sortent du lot à l'inspection.

- Les **cell renderers AG Grid** (`commande-btn` et consorts) reçoivent leur état dans `agInit()`,
  appelé *avant* le premier rendu. La vue est peinte une fois avec les bonnes valeurs et n'a rien à
  rafraîchir ensuite. Ils passent — par chance, pas par construction.
- Les **widgets Chart.js** (`pie-chart-widget`…) affectent `this.chart = new Chart(...)`, un objet
  qui n'est jamais lu par Angular : c'est Chart.js qui peint dans le canvas. Faux positif de
  l'audit.

Il reste à vérifier un par un ceux qui chargent des données de façon asynchrone — c'est là que le
symptôme apparaît.

### Répartition du travail restant

Le critère retenu : un champ **muté** (`this.x = …`) **et** lu dans le gabarit, sans être un signal.
C'est exactement le motif qui casse sous `OnPush`.

| module | composants | déjà `OnPush` | bascule directe | conversion requise | champs à convertir |
|---|---:|---:|---:|---:|---:|
| commande | 73 | 0 | 40 | 33 | 103 |
| sales | 36 | 5 | 15 | 16 | 50 |
| facturation | 25 | 0 | 10 | 15 | 59 |
| products | 24 | 0 | 12 | 12 | 48 |
| inventory | 14 | 0 | 9 | 5 | 7 |
| declaration-ca | 11 | 11 | 0 | 0 | 0 |
| rayon | 8 | 0 | 4 | 4 | 12 |
| differes | 7 | 0 | 2 | 5 | 10 |
| finances | 6 | 0 | 6 | 0 | 0 |
| admin | 5 | 0 | 1 | 4 | 5 |
| settings | 5 | 0 | 2 | 3 | 4 |
| ajustement | 4 | 0 | 1 | 3 | 3 |
| partners | 3 | 0 | 0 | 3 | 8 |
| cahier-recette | 1 | 0 | 1 | 0 | 0 |
| comptabilite | 1 | 0 | 1 | 0 | 0 |
| license | 1 | 1 | 0 | 0 | 0 |

#### `app/entities` — 139 composants

| module | composants | déjà `OnPush` | bascule directe | conversion requise | champs à convertir |
|---|---:|---:|---:|---:|---:|
| reports | 34 | 0 | 33 | 1 | 2 |
| customer | 10 | 0 | 1 | 9 | 25 |
| dashboard | 9 | 0 | 4 | 5 | 5 |
| sales | 8 | 0 | 1 | 7 | 18 |
| depot | 7 | 0 | 3 | 4 | 16 |
| mvt-caisse | 7 | 3 | 1 | 3 | 22 |
| commande | 6 | 0 | 3 | 3 | 6 |
| gestion-peremption | 6 | 0 | 0 | 6 | 49 |
| produit | 6 | 0 | 1 | 5 | 17 |
| remise | 5 | 0 | 1 | 4 | 10 |
| categorie | 4 | 0 | 1 | 3 | 4 |
| cash-register, groupe-tiers-payant, magasin, tableau-produit, tiers-payant | 15 | 0 | 5 | 10 | 20 |
| 13 modules à 1 ou 2 composants | 22 | 0 | 3 | 19 | 44 |

Les 3 `OnPush` de `mvt-caisse` sont `balance-mvt-caisse`, `taxe-report` et `tableau-pharmacien`,
convertis le 22 août 2026 — ce sont eux qui ont motivé ce plan.

#### Totaux

| | `features` | `entities` | socle | total |
|---|---:|---:|---:|---:|
| bascule directe | 104 | 57 | 40 | **201** |
| conversion requise | 103 | 79 | 14 | **196** |
| champs à convertir | 309 | 238 | 69 | **616** |

**201 composants basculent sans rien convertir** — un peu plus de la moitié du parc.

Les plus lourds, tous dossiers confondus :

| champs | composant |
|---:|---|
| 44 | `home/home-base` |
| 21 | `entities/gestion-peremption/lot-perimes` |
| 18 | `entities/gestion-peremption/lot-a-detruire` |
| 15 | `features/products/ui/produit-form` |
| 15 | `features/products/ui/produit-mouvements-tab` |
| 14 | `features/facturation/feature/facturation-edition` |
| 12 | `features/sales/feature/sales-journal` |
| 12 | `entities/mvt-caisse/visualisation-mvt-caisse` |
| 9 | `features/commande/feature/commande-received` |
| 8 | `entities/mvt-caisse/gestion-caisse`, `features/commande/feature/repartition-stock`, `features/commande/ui/lot/inline/lot-inline-editor`, `features/facturation/feature/historique-reglements` |

`entities/reports` est le cas le plus favorable du dépôt : 33 composants sur 34 basculent sans
toucher à une ligne de logique. C'est un module de rapports, essentiellement des gabarits alimentés
par des entrées.

### Reproduire ce relevé

Le script d'audit n'est pas versionné — il se réécrit en dix lignes et se périmerait plus vite qu'il
ne servirait. Sa logique : pour chaque `*.component.ts`, lire la stratégie déclarée, extraire les
champs affectés par `this.x =`, retrancher ceux déclarés via `signal` / `computed` / `input` /
`model` / `linkedSignal` / `toSignal` / `viewChild` / `contentChild`, et ne garder que ceux dont le
nom apparaît dans le gabarit (fichier `.html` ou `template:` en ligne).

Deux limites assumées : la correspondance de nom dans le gabarit est textuelle (quelques faux
positifs sur des noms courts), et un champ muté **non** lu dans le gabarit peut tout de même poser
problème s'il alimente un `@Input()` d'un enfant. Les chiffres donnent un ordre de grandeur fiable,
pas un décompte à l'unité.

## Ordre de marche

### Étape 0 — Ne pas aggraver la dette

À faire avant tout le reste, parce que chaque semaine sans cette règle ajoute des composants à
migrer.

- **Interdire les nouveaux composants `Eager`.** Ne pas écrire `changeDetection` du tout : la valeur
  par défaut de la v22 est déjà la bonne.
- **Ajouter la règle au `CLAUDE.md`** de la section front, à côté des interdits existants
  (`*ngIf`, `styleClass`, `p-dialog`…) : *tout état lu dans un gabarit est un signal*.
- Vérifier les 19 composants d'écrans sans annotation listés plus haut : ils sont déjà soumis à
  `OnPush` sans filet. Les 24 du Design System, eux, sont sains — ne pas perdre de temps dessus.

### Étape 1 — Les 201 bascules directes

Aucun état muté lu dans le gabarit : il suffit de retirer `changeDetection: ChangeDetectionStrategy.Eager`.

Faisable module par module en une passe. Le contrôle est visuel — ouvrir l'écran, vérifier que les
données s'affichent et que les actions répondent —, car aucun test ne couvre aujourd'hui ce
comportement.

**Piège à connaître** : un composant peut n'avoir aucun état muté *et* dépendre d'un enfant qui, lui,
en a. Basculer le parent en `OnPush` coupe alors la descente vers l'enfant, et c'est ce dernier qui
casse. D'où l'ordre suivant : **les feuilles avant les layouts.** Dans chaque module, convertir les
composants `ui/` avant les `feature/`, et les `feature/` avant le layout.

### Étape 2 — Les 196 conversions, par module

Ordre proposé, du plus rentable au plus coûteux. `f/` désigne `app/features`, `e/` `app/entities`.

1. **e/reports** (33 bascules, 1 conversion à 2 champs) — le meilleur ratio du dépôt, et un module
   isolé : idéal pour roder le geste.
2. **f/finances, f/cahier-recette, f/comptabilite** (8 composants, 0 champ) — bascule pure.
3. **f/inventory** (5 conversions, 7 champs) et **e/dashboard** (5 conversions, 5 champs).
4. Les petits modules d'`entities` — `categorie`, `magasin`, `tva`, `famille-produit`,
   `forme-produit`, `gamme-produit`, `laboratoire-produit`, `mode-payments`, `modif-ajustement`,
   `reglement`, `tableau-produit`, `ticketZ`, `raport-gestion` : tous suivent le même moule
   liste + formulaire, ce qui rend la conversion répétitive donc rapide.
5. **f/ajustement, f/settings, f/admin, f/rayon, f/differes, f/partners** (22 conversions,
   42 champs).
6. **e/produit, e/remise, e/depot** (13 conversions, 43 champs).
7. **f/products** (12 conversions, 48 champs) — dont deux gros morceaux.
8. **e/customer** (9 conversions, 25 champs) et **e/sales** (7 conversions, 18 champs).
9. **f/sales** (16 conversions, 50 champs) — 5 composants déjà `OnPush` servent de modèle.
10. **f/facturation** (15 conversions, 59 champs).
11. **e/mvt-caisse** (3 conversions, 22 champs) — `visualisation-mvt-caisse` et `gestion-caisse` ;
    les trois autres écrans du module sont déjà faits et servent de référence directe.
12. **e/gestion-peremption** (6 conversions, 49 champs) — 6 composants seulement, mais la plus forte
    densité d'état muté du dépôt.
13. **f/commande** (33 conversions, 103 champs) — le plus gros volume, à traiter en dernier.

Le socle s'intercale hors de cette progression, parce qu'il ne suit pas la même logique :

- **`app/shared` (30 bascules)** peut se faire dès maintenant, en même temps que l'étape 1 : le
  Design System est déjà aux signaux, ces bascules ne sont que du ménage d'annotations.
- ~~**`main`, `navbar`, `sidebar` (5 champs)**~~ — fait le 22 août 2026, en même temps que la
  correction du menu manquant dans la sidebar.
- **`shared/app-settings-dialog` et `shared/backend-splash` (10 champs)** : à traiter avec les
  écrans, pas avec le Design System — ce sont de vrais écrans asynchrones.
- **`admin` et `account` (8 composants, 1 vraie conversion)** : à faire n'importe quand, c'est
  l'affaire d'une demi-heure.
- **`home/home-base` (44 champs)** : en dernier, ou dans une séance qui ne fait que cela.

### Étape 3 — Vérification

Rechercher les `ChangeDetectionStrategy.Eager` restants et exiger qu'ils portent un commentaire
justifiant le cas d'usage (intégration tierce qui modifie l'état hors d'Angular, typiquement).

## Le geste de conversion

Modèle repris de `entities/mvt-caisse/balance-mvt-caisse`, converti le 22 août 2026.

**Composant** — la propriété devient un signal en lecture seule, l'affectation devient un `.set()` :

```ts
// avant
protected loading = false;
protected wrapper: Wrapper | null = null;
// this.loading = true; … this.wrapper = data || null;

// après
protected readonly loading = signal(false);
protected readonly wrapper = signal<Wrapper | null>(null);
// this.loading.set(true); … this.wrapper.set(data ?? null);
```

**Gabarit** — appeler le signal, et capturer l'objet une fois plutôt que de le rappeler à chaque
ligne :

```html
<!-- avant -->
@if (wrapper) { {{ wrapper.montantTtc }} … {{ wrapper.montantHt }} }
<!-- après -->
@if (wrapper(); as w) { {{ w.montantTtc }} … {{ w.montantHt }} }
```

**`[(ngModel)]`** ne se lie pas à un `signal()` — la syntaxe raccourcie n'existe que pour les
`model()` de composants. Décomposer :

```html
<!-- avant -->
<pharma-date-picker [(ngModel)]="fromDate" [maxDate]="toDate" />
<!-- après -->
<pharma-date-picker [ngModel]="fromDate()" (ngModelChange)="fromDate.set($event)" [maxDate]="toDate()" />
```

**Points d'attention**

- Un objet passé à `.set()` sur plusieurs lignes : ne pas oublier de fermer par `});` et non `};`.
  Le compilateur le signale, mais le message (`')' expected`) pointe loin de la cause.
- Les champs mutés **non** lus dans le gabarit peuvent rester des propriétés. Les convertir « pour
  faire propre » allonge la revue sans rien apporter.
- `mode`, `titre` et autres entrées sont déjà des `input()` : ils salissent la vue tout seuls, rien
  à faire.

## Ce que la migration apporte, et ce qu'elle ne règle pas

Le gain de performance est réel mais secondaire : sur ces écrans, la détection de changement n'a
jamais été le facteur limitant. **Le vrai gain est la suppression d'un couplage invisible** — un
composant aux signaux se comporte de la même façon quel que soit l'écran qui l'accueille, et le
symptôme « spinner infini alors que le backend répond » disparaît de la classe des bugs possibles.

Ce que ce plan ne traite pas :

- **rien, côté relevé** : les 429 composants du dépôt sont couverts ;
- **l'absence de tests sur ce comportement** : rien ne détecterait aujourd'hui une régression de ce
  type, et chaque étape repose sur une vérification manuelle. Un test qui monte un composant sous un
  hôte `OnPush` et vérifie l'affichage après une réponse simulée verrouillerait le geste — c'est le
  meilleur complément à ce plan, et il n'existe pas encore ;
- **les cas légitimes de `Eager`** : une intégration qui modifie l'état hors d'Angular sans appeler
  `markForCheck()` restera `Eager`, et c'est correct. L'objectif n'est pas zéro `Eager`, mais zéro
  `Eager` non justifié.
