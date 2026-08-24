# Plan de migration vers `ChangeDetectionStrategy.OnPush`

**Chantier terminé le 23 août 2026.** Les 436 composants de `pharmaSmart-app/src/main/webapp/app`
sont en `OnPush`, `ChangeDetectionStrategy.Eager` n'apparaît plus nulle part, et le contrôle
d'invariant ne relève aucun couple à risque. Le document est conservé pour deux raisons : la
règle qu'il énonce reste la règle du dépôt, et le relevé de la page décrit comment vérifier
qu'elle tient toujours.

État initial : 22 août 2026, 436 composants dont 10 en `OnPush`.

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

## Ce que la coquille a appris — 23 août 2026

`main`, `navbar` et `sidebar` avaient été passés à `OnPush` ensemble. Les écrans **non migrés** se
sont mis à se comporter de façon erratique : affichages figés, mises à jour qui n'arrivent qu'au
clic suivant, jamais deux fois au même endroit.

**La cause est `main`, pas les deux autres.** Ce composant héberge le `<router-outlet>` principal :
sous `OnPush`, sa vue n'est visitée que lorsqu'elle est salie, et la traversée s'arrête là — avec
elle, *tous* les écrans de l'application. Ceux qui pilotent leur état par des propriétés mutées
dépendent justement de cette traversée.

`main` est donc revenu à `Eager`, avec un commentaire renvoyant à ce document. `navbar` et
`sidebar` restent `OnPush` : ils sont entièrement aux signaux et n'ont aucun écran sous eux.

**La règle qui en découle, et qui gouverne l'ordre du reste :** un composant ne peut passer à
`OnPush` que si **tout ce qui vit sous lui** est déjà indifférent à la stratégie. On migre donc des
feuilles vers la racine, jamais l'inverse. La coquille est le dernier maillon, pas le premier.

## L'invariant à tenir pendant la migration

Un couple **parent `OnPush` / enfant `Eager` qui mute son état** est un bug en attente : le parent
non sali coupe la traversée, l'enfant cesse de se rafraîchir après une réponse HTTP, sans erreur et
de façon intermittente. C'est ce qui est arrivé à l'échelle de l'application avec `main`, et cela
peut se reproduire à l'échelle d'un panneau.

**Après chaque lot, ce couple doit être recherché.** Le contrôle est mécanique : pour chaque
composant `OnPush`, résoudre les sélecteurs présents dans son gabarit, et vérifier qu'aucun enfant
`Eager` n'a de champ muté lu dans son propre gabarit.

Le 23 août 2026, ce contrôle a relevé **12 couples** après la migration des feuilles. Dix parents
sont repassés à `Eager`, le temps que leur enfant migre — puis **tous ont été libérés le jour même**
en convertissant six composants seulement :

| enfant migré | champs | parents libérés |
|---|---:|---:|
| `fournisseur-select` | 2 | 4 |
| `produit-mouvements-tab` | 15 | 1 |
| `avoir-workspace` | 7 | 1 |
| `rayon-produits-tab` | 3 | 1 |
| `quantite-produt-saisie` | 1 | (avec `fournisseur-select`) 3 |
| `product-search`, `produit-search-autocomplete-scanner` | 0 | 2 |

**Il ne reste aucun couple à risque, ni aucun parent en attente.**

Deux enseignements de ce passage.

`product-search-section` était `OnPush` **avant** ce chantier : le couple existait donc déjà, dans
l'écran de vente, et personne ne l'avait vu. C'est exactement le genre de défaut que ce contrôle
attrape.

`product-search` et `produit-search-autocomplete-scanner` étaient des **faux positifs** : ils
exposent un signal privé via un couple `get`/`set`, si bien que `this.x = …` passe par le setter qui
appelle `.set()`. Le détecteur doit donc traiter tout accesseur `get` comme un champ sûr, sans quoi
il réclame des conversions inutiles.

## Ordre de migration, du moins au plus impactant

Le classement ci-dessous mesure l'impact par la **portée** — combien de composants dépendent de
celui-ci pour être atteints — et non par la quantité de travail. Une feuille sans enfant ne peut
couper la traversée de personne : s'y tromper n'affecte qu'elle. Un layout coupe tout ce qu'il
contient.

396 composants restent hors `OnPush`.

| rang | portée | composants | bascule directe | conversions | champs |
|---:|---|---:|---:|---:|---:|
| 1 | **feuilles** (`ui/`, `widgets/`) | 156 | 94 | 62 | 160 |
| 2 | **écrans** | 215 | 93 | 122 | 426 |
| 3 | **layouts** (`*-layout`, `*-home`) | 25 | 16 | 9 | 26 |
| 4 | **coquille** (`main`) | 1 | — | — | — |

### 1. Les feuilles — 156 composants, dont 94 sans rien à convertir

Le lot le plus sûr : ces composants n'hébergent aucun écran. Trente d'entre eux sont des composants
du Design System (`app-button`, `app-input`, `app-select`…) **déjà entièrement aux signaux** : ils
n'attendent que le retrait de leur annotation, et rien ne peut casser en dessous.

Commencer par `shared/ui/` : 30 bascules, zéro conversion, zéro risque. C'est la répétition
générale idéale, et elle profite à toute l'application.

### 2. Les écrans — 215 composants, 426 champs

Le gros du travail. Un écran migré prématurément ne casse que lui-même, ce qui reste circonscrit —
mais 122 d'entre eux demandent une conversion réelle. À traiter module par module, en suivant
l'ordre de rentabilité déjà établi plus bas dans ce document.

### 3. Les layouts — 25 composants, 26 champs seulement

Peu de travail, mais chacun commande une famille entière d'écrans : les migrer avant leur contenu
reproduirait exactement le symptôme du 23 août, à l'échelle d'un module. Les plus chargés :
`facturation-home` (7 champs), `bed-home` et `rayon-home` (4), `fournisseur-home` et `produit-home`
(3).

**À ne toucher qu'une fois tous leurs onglets migrés.**

### 4. `main` en dernier

Il ne peut basculer que lorsque plus aucun écran routé ne dépend d'une traversée complète. C'est le
signal de fin du chantier — et le seul moyen d'en tirer le bénéfice de performance.

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

## Journal d'exécution — 23 août 2026

Le chantier a été mené dans l'ordre de portée décrit plus haut, en lots vérifiés un à un par
`ng build` puis par le contrôle d'invariant. Le compte final : **436 composants sur 436**, aucun
`Eager` résiduel, aucun couple à risque.

| lot | portée | composants | champs convertis |
|---|---|---:|---:|
| feuilles restantes | 1 | 17 | 60 |
| bascules directes des écrans | 2 | 96 | — |
| enfants libérant un parent | 2 | 13 | ~90 |
| conversions d'écrans | 2 | 105 | ~340 |
| layouts | 3 | 21 | 21 |
| `app` puis `main` | 2 et 4 | 2 | — |

Les tests front sont restés exactement au même point qu'avant le chantier : **362 passés, 24
échecs répartis sur 12 suites**, tous antérieurs (fournisseurs absents des `TestBed`, `Zone is
needed`). `npm run lint` ne s'exécute pas dans ce dépôt — `prettier-plugin-java` manque à
`node_modules` — ce qui est sans rapport avec cette migration.

### Ce que la conversion automatique ne sait pas voir

La bascule a été outillée par un script de réécriture. Huit familles de défauts sont apparues à
l'usage ; elles valent d'être connues de quiconque reprendra l'outil ou convertira à la main.

- **Le tiret n'est pas une frontière de mot.** Un champ `fournisseurs` réécrit sans précaution
  transforme `<app-produit-fournisseurs-tab>` en `<app-produit-fournisseurs()-tab>`. Angular ne
  proteste pas : il émet un simple avertissement `NG8113` et le composant disparaît de la page.
- **Le guillemet ouvre une expression, pas une chaîne.** Exclure `"` du contexte gauche laisse
  `[class.is-invalid]="form.get('x')"` non converti.
- **`==` n'est pas `=`.** Le garde qui protège les noms d'attribut doit laisser passer les
  comparaisons, sinon `@if (activeStep === 1)` compare un signal à un nombre.
- **Une affectation depuis le gabarit** — `(click)="showGraphs = false"` — doit devenir un
  `.set()` ; sur une propriété `readonly`, elle échoue à la compilation.
- **Le `.set()` qu'on vient d'écrire** ne doit pas être repris par la passe de lecture, sous peine
  de produire `x().set($event)`.
- **Une affectation ne s'arrête pas au premier point-virgule.** `this.x = res.body.map(p => {
  return {...}; })` exige un vrai comptage de parenthèses ; une expression régulière coupe au
  milieu de l'objet littéral.
- **Une variable de boucle peut porter le nom d'un champ.** `@for (tiersPayant of ...)` dans un
  composant qui a aussi un champ `tiersPayant` : le script refuse désormais de trancher et le
  signale.
- **Un `FormGroup` n'est jamais un signal.** L'instance est stable ; l'envelopper casse l'API que
  les tests et les appelants utilisent (`comp.resetRequestForm.patchValue(...)`). Cinq formulaires
  ont dû être ramenés à des propriétés simples.

### Deux composants réclament un accesseur, pas un signal

`showCommonModal` renseigne l'instance du modal par affectation de propriétés. Une propriété
devenue signal y perd : l'affectation écrase le signal par une valeur brute. `assured-customer-list-modal`
expose donc `set search(...)` et `set preloaded(...)`, qui écrivent dans les signaux internes ; les
deux appelants passent `search:` et `preloaded:`. Le même détour vaut pour tout composant dont
l'API d'entrée est renseignée par affectation plutôt que par liaison.
## Les champs restés propriétés simples

Aucun champ muté n'est plus lu directement par un gabarit — c'est ce que vérifie le relevé. Restent
**166 champs** répartis sur 98 composants, qui n'ont volontairement pas été convertis. La ligne de
partage est simple :

> **État de vue** — une donnée que le gabarit pourrait afficher — devient un signal.
> **Poignée** — un objet qu'on garde pour le manipuler ou le libérer — reste une propriété.

Ce qui a été converti le 23 août, en second passage : les 37 champs de pagination (`page`,
`ngbPaginationPage`, `criteria`, `predicate`, `ascending`, `itemsPerPage`, `sortField`…) et les
drapeaux d'état (`isSaving`, `certifying`, `exporting*`, `displayDialog`, `isScanning`,
`disableButton`…) — **68 champs sur 46 composants**. Aucun n'était rendu, donc aucun ne posait de
problème ; ils sont convertis parce qu'ils sont à une modification de gabarit près d'en poser un.

Ce qui reste, et pourquoi :

| famille | champs | raison |
|---|---:|---|
| divers (poignées internes) | 124 | instances Chart.js, écouteurs DOM, gardes anti-rechargement (`currentXxxId`), instantanés (`initialFormValue`, `originalGroups`) |
| abonnements RxJS | 14 | on les garde pour les fermer |
| API AG Grid (`gridApi`, `params`) | 12 | objets pilotés par la grille, jamais rendus |
| formulaires réactifs | 8 | l'instance est stable ; l'envelopper casse l'API des tests et des appelants |
| minuteries | 7 | identifiants de `setTimeout` |
| `ElementRef` | 1 | poignée DOM |

### Les champs morts, que ni l'un ni l'autre relevé ne voit

Un champ **déclaré et jamais touché** — ni écrit, ni lu, ni dans le TS ni dans le gabarit —
n'apparaît dans aucun des deux relevés : ils partent tous deux de `this.x = …`. `tva.component.ts`
portait ainsi un `protected isSaving = false;` que rien n'utilisait. Ce n'est pas un oubli de la
migration, c'est du code mort.

Un troisième relevé en a compté **120**. Ils se répartissent en quatre familles, qui n'appellent
pas le même geste :

| famille | nombre | geste |
|---|---:|---|
| champs `private` / `protected` | 45 | **supprimés** — l'encapsulation TypeScript garantit qu'aucun appelant externe ne les touche, et le compilateur le vérifie |
| `input()` / `output()` / `model()` | 35 | à garder : un parent peut les lier, même si personne ne le fait aujourd'hui |
| `inject(...)` inutilisés | 13 | **supprimés après vérification un par un** — voir ci-dessous |
| champs sans modificateur d'accès | 27 | **22 supprimés, 4 gardés, 1 faux positif** — voir ci-dessous |

#### Les 13 injections inutilisées, vérifiées une par une

Retirer un `inject(...)` n'est pas anodin : **injecter un service l'instancie**, et plusieurs de
ceux-ci démarrent quelque chose à la construction — `TauriPrinterService` lance
`initializeTauri()` et `initializeCustomerDisplay()`, `UserVendeurService` charge la liste des
vendeurs, `GlobalScannerService` restaure son état. La question n'est donc pas « ce service a-t-il
des effets de bord ? » mais **« ce composant est-il le seul à l'injecter ? »**. Tous sont
`providedIn: 'root'` : le singleton naît à la première injection, d'où qu'elle vienne.

| service | injections ailleurs | verdict |
|---|---:|---|
| `NgbModal` (5 composants) | bibliothèque, partout | sans effet de bord |
| `ActivatedRoute` | fourni par le routeur | sans effet de bord |
| `DatePipe` | reste déclaré dans `providers` de l'écran | sans effet de bord |
| `NotificationService` | 178 | le singleton existe de toute façon |
| `TauriPrinterService` | 39, dont `navigation.service` | idem |
| `UserVendeurService` | 5, dont `sales-home` et `vente-depot` | idem |
| `PeremptionAlertService` | 3, dont `navigation.service` et `main` | idem |
| `GlobalScannerService` | 3, dont `SalesScannerService`, que `sales-home` fournit et injecte | idem |

Aucun de ces treize composants n'était le dernier point d'entrée de son service, et aucun des trois
`protected` n'était lu par son gabarit. **Les 13 ont été retirés**, build vert.

Un cas est instructif : le `NgbModal` de `vente-depot` n'est devenu inutile que le jour où l'écran
a cessé d'appeler `showCommonError(this.modalService, …)` pour passer à une notification. Le code
mort naît souvent d'un remplacement fait à moitié.

Le `DatePipe` de `sales-journal` reste dans le tableau `providers` du composant : il n'est plus
injecté par l'écran lui-même, mais un enfant pourrait le résoudre par là — `DatePipe` n'étant pas
`providedIn: 'root'`, le retirer serait un pari.
#### Les 27 champs sans modificateur d'accès

Ceux-là, TypeScript ne les protège pas : `showCommonModal` et `modalRef.componentInstance`
renseignent les propriétés d'instance depuis l'extérieur, et le compilateur ne dira rien si on les
supprime. Le contrôle est donc double — chercher `componentInstance.champ` **et** le champ comme
clé d'un objet littéral, dans tout fichier qui cite la classe.

**Quatre sont bel et bien renseignés de l'extérieur** et restent en place :

| champ | composant | renseigné par |
|---|---|---|
| `produitId` | `semois-exclure-produit` | `componentInstance` depuis `semois-suggestions` |
| `fournisseurId` | `import-suggestion-modal` | `componentInstance` depuis `commande-requested` |
| `commandeId` | `form-lot` | `componentInstance`, et littéral depuis trois écrans |
| `produitLibelle` | `prix-historique` | littéral passé à `showCommonModal` |

**Vingt-deux ont été supprimés** : les drapeaux `isSaving` / `isValid` des étapes du formulaire
assuré, le `header` de `assure-step`, `active`, `appendTo`, `types`, `tiersPayants`, `CASH`, et
surtout dix **alias de façade** dans les écrans de vente — `loading = this.facade.loading`,
`cashier`, `seller`, `plafondIsReached`, `selectedCustomer` — que le gabarit n'utilise pas.

Vérifié au passage que les composants d'étape ne sont pas atteints par `viewChild` : le parent
`assure-form-step` ne leur appelle que des méthodes (`createFromForm()`, `saveFormState()`,
`goBack()`), jamais un champ.

**Un faux positif, instructif.** `amount` a été signalé dans `payment-mode` — mais il appartient à
l'`export interface PaymentModeEntry` déclarée **après** la classe, dans le même fichier. Un relevé
qui lit tout ce qui suit `export class` prend les membres d'interface pour des champs. Restauré
aussitôt, le compilateur l'ayant vu. Les 400 autres fichiers touchés ont été vérifiés : aucun ne
déclare de type après sa classe.

Deux composants méritent un coup d'œil séparé, hors de ce chantier : `remise-list-dialog` n'est
**cité par aucun fichier**, et `reglement-form` porte un `computed` `monnaie` que personne ne lit.
Deux pièges rencontrés en automatisant la suppression, si l'exercice est refait :

- retirer un champ rend parfois un **import inutile** ; ne réécrire que les lignes d'import qui
  perdent effectivement un identifiant, sinon le diff se remplit de reformatage sans rapport ;
- un identifiant peut n'apparaître que dans un **spread** — `Chart.register(...registerables)`. Un
  test d'usage qui exclut le point qui précède le prend pour un accès de propriété et déclare
  l'import mort.
### Le point aveugle du relevé, mesuré

Un champ absent du gabarit peut tout de même être rendu **à travers une méthode** :
`[disabled]="!estValide()"` où `estValide()` lit `this.champ`. Un détecteur écrit pour l'occasion
remonte 112 candidats — mais presque tous sont du bruit, pour une raison qui compte :

> Un champ lu dans un **gestionnaire d'événement** (`(click)="charger()"`) n'a rien à voir avec le
> rafraîchissement : la valeur est utilisée, pas rendue, et l'événement a de toute façon sali la vue
> avant d'exécuter le gestionnaire.

En ne gardant que les appels en **position de rendu** — interpolation, liaison de propriété,
`@if`, `@for` — il reste **deux** cas, tous deux sûrs :

- `importation-new-commande` : `[disabled]="… || !isValidForm()"` lit `fournisseurSelectedId`, qui
  n'est écrit que par `onFournisseurSelected($event)`, lié dans ce même gabarit.
- `customer-edit-modal` : `[disabled]="… || !hasFormChanged()"` lit `initialFormValue`, figé de
  façon synchrone à l'initialisation et comparé au formulaire vivant.

Attention en relisant ce genre de détecteur : `(keyup.enter)` et `(keydown.enter)` contiennent un
point, et un filtre naïf sur `(\w+)=` les prend pour des positions de rendu.
## Le défaut que le compilateur ne voit pas : un signal lu sans parenthèses

`[items]="familleProduits"` — sans les parenthèses — passe **la fonction signal** au lieu du
tableau. La liste arrive vide, aucune erreur nulle part. Ça ne casse la compilation que si l'entrée
cible est typée précisément ; dès qu'elle accepte `any[]`, c'est silencieux.

Origine dans ce chantier : la première version de la réécriture de gabarits excluait les
identifiants précédés d'un guillemet, pour épargner les chaînes de caractères. Or `"` ouvre aussi
une **expression de liaison**. La règle a été corrigée en cours de route, mais les composants
convertis avant gardaient des lectures nues.

### Ce qui est sûr, et ce qui ne l'est pas

La liaison **bidirectionnelle** est sûre — vérifié dans `@angular/core` :

```js
function ɵɵtwoWayProperty(propName, value, sanitizer) {
  if (isWritableSignal(value)) { value = value(); }   // Angular déballe
}
function ɵɵtwoWayBindingSet(target, value) {
  const canWrite = isWritableSignal(target);
  canWrite && target.set(value);                      // et sait écrire
}
```

`[(ngModel)]="monSignal"` fonctionne donc dans les deux sens, et les 46 occurrences du dépôt ne
sont pas des défauts. C'est la liaison **unidirectionnelle** qui casse : `ɵɵproperty` ne déballe
rien. Corollaire utile : `twoWayBindingSet` renvoie `false` quand la cible n'est pas un signal
writable, et Angular retombe alors sur l'affectation de propriété — d'où l'erreur « Attempt to
assign to const or readonly variable » sur un `readonly`.

### Le relevé, et ce qu'il a trouvé

Chercher le nom d'un signal dans tout le gabarit ne donne rien d'exploitable — 832 résultats, du
bruit. Il faut n'inspecter que les endroits où le gabarit **évalue du TypeScript** : `[prop]="…"`,
`(event)="…"`, `{{ … }}`, `@if` / `@for` / `@switch`. En écartant les liaisons bidirectionnelles,
les écritures `x.set(…)` et les alias de boucle, il restait **39 lectures nues réelles**, toutes
corrigées :

| écran | lectures |
|---|---:|
| `produit-form` (familles, rayons, TVA, fournisseurs, laboratoires…) | 14 |
| `authorization-modal` | 5 |
| `avoir-workspace`, `avoir-form-modal` | 5 |
| `list-bons`, `lot-saisie-produit-modal`, `suggestion-produit-actions` | 6 |
| `add-widget-modal`, `reconciliation-facture`, `rayon-produits-tab` | 4 |
| liaisons unidirectionnelles `[prop]="signal"` (1re série) | 5 |

Les 19 résultats restants sont des faux positifs assumés : le nom apparaît dans une **chaîne**
(`['/sales-home/devis']`, `'app-btn-icon '`) ou comme **clé d'objet** (`{ first: first() }`).

### Le défaut jumeau : des parenthèses là où il ne fallait pas

La même réécriture a produit la faute inverse — coller `()` à la valeur d'un attribut **statique**,
qui n'est pas une expression mais une chaîne :

```html
<input formControlName="cashFundAmount()">   <!-- « Cannot find control with name » -->
<div ngbNavItem="avoirs()">                  <!-- l'identifiant d'onglet ne correspond plus -->
<label for="searchTerm()">                   <!-- le label ne pointe plus sur son champ -->
```

**46 attributs** ont été restaurés sur 16 fichiers, dont les plus graves — `formControlName`,
`formGroupName`, `formArrayName`, `ngbNavItem` — qui échouent à l'exécution, et les plus discrets
— `id`, `for`, `inputId` — qui cassent silencieusement l'association label/champ.

Le repérage tient en une expression : un nom d'attribut précédé d'une espace (donc ni `[prop]=`, ni
`(event)=`) dont la valeur est exactement `identifiant()`.

**À retenir pour la suite** : après toute réécriture automatique de gabarits, repasser ce relevé.
Le build ne rattrapera pas ces défauts — l'écran, lui, les montre tout de suite.
## La suite : retirer `zone.js`

Maintenant que les 436 composants sont `OnPush`, Zone ne sert presque plus à rien. Elle déclenche
un cycle de détection après chaque minuterie, chaque requête, chaque événement — et ce cycle ne
trouve pratiquement jamais rien de sale, puisque ce qui salit désormais, ce sont les signaux. On
paie un polyfill et un tick par tâche asynchrone pour un travail que les signaux font déjà, mieux
ciblé.

**Ce que ça rapporte, mesuré sur ce dépôt** : `polyfills.js` passe de **94,56 ko à 1,65 ko** en
build de développement — il ne reste que `@angular/localize/init`. Rien d'autre ne bouge dans le
bundle.

Le geste : `provideZonelessChangeDetection()` à la place de
`provideZoneChangeDetection({eventCoalescing: true})` dans `app.config.ts`, et retrait de
`"zone.js"` de `polyfills` dans `angular.json`. Tant que Zone est là, garder `eventCoalescing` :
il fusionne les événements d'une même tâche en un seul cycle, il n'y a aucune raison de s'en priver.

### Inventaire des 17 appels à l'API Zone

| catégorie | sites | verdict |
|---|---:|---|
| écrit un signal dans le `run` | 6 | sans objet — le `.set()` planifie le cycle |
| `form.patchValue(...)` | 2 | sûr — les formulaires réactifs écrivent le DOM par le `ControlValueAccessor`, hors détection |
| pousse dans un `Subject` | 5 | sûr — le consommateur (`backend-splash`) écrit des signaux |
| appelle une méthode `async` | 1 | sans objet — la méthode finit sur `visible.set(true)` |
| `runOutsideAngular` (Chart.js) | 2 | garde de performance, reste valide, devient inutile |
| ouverture d'un modal depuis un `effect()` | 1 | **le seul à tester en exécution** |

Le détail par fichier :

- `core/setup/setup-wizard.component.ts` — 5 sites. Quatre écrivent `visible` / `errorMessage` /
  `submitting` ; le cinquième patche le formulaire depuis un `invoke` Tauri, puis `openWizard()`
  enchaîne sur `visible.set(true)`, ce qui salit la vue de toute façon.
- `core/tauri/backend-status.service.ts` — 5 sites, tous des `next()` sur `backendStatus$`.
- `features/settings/feature/app-config-editor/app-config-editor.component.ts` — 4 sites. Le
  `patchValue` est suivi d'un `loading.set(false)` dans le même `run`, donc la liaison
  `[disabled]="form.invalid || …"` est bien réévaluée.
- `shared/chart/chart.component.ts` — 2 `runOutsideAngular` autour de `chart.update()`.
- `shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive.ts` — 1 site, commenté
  « garantit la change detection quand appelé depuis un `effect()` ». C'est le point à vérifier :
  ouvrir la boîte de confirmation depuis un `effect()`, sans Zone, et regarder si elle s'affiche.

**Aucun de ces sites ne bloque le passage.** Quinze deviennent des enrobages inutiles à retirer,
un demande un test manuel.

### Ce qu'il reste à faire avant de basculer

1. Vérifier le site `ngb-confirm-dialog` — le seul doute réel.
2. Reprendre **3 specs** qui utilisent `waitForAsync()` : `home`, `login`, `alert`. Le harnais
   Jest est **déjà zoneless** (`NoopNgZone`), et ces trois-là échouent aujourd'hui sur
   « Zone is needed for the `waitForAsync()` test helper ». Une partie du chemin est donc déjà
   faite, à l'envers.
3. Tester à la main les écrans qui parlent à Tauri — assistant d'installation, éditeur de
   configuration, bandeau d'état du backend — puisque ce sont eux qui repassaient par `ngZone.run`.
4. Retirer les 15 `ngZone.run` devenus inutiles, garder les 2 `runOutsideAngular`.

À faire **après** une période de stabilisation de la migration `OnPush`, pas dans la foulée : les
deux chantiers touchent au même mécanisme, et les diagnostiquer ensemble serait pénible.
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
