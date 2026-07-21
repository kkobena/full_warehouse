# PLAN — Durcissement du typage front (strictTemplates / strictNullChecks)

> Statut : **proposition, chiffrée sur mesures réelles**
> Origine : bug d'alias d'output découvert pendant la migration PrimeNG → ng-bootstrap
> (`(searchEvent)` au lieu de `(search)` sur `ng-select`), passé inaperçu du build.
> Périmètre : `pharmaSmart-app/src/main/webapp` — chantier **séparé** de la migration.

---

## 1. Constat de départ

`tsconfig.json` déclare `"strict": true`, mais deux options le vident d'une partie de sa
substance :

```jsonc
"strictNullChecks": false,   // ligne 8  — annule les contrôles de nullité
"strictTemplates": false,    // ligne 33 — annule le typage des templates
```

Conséquence : des erreurs de template passent en production sans que `npm run webapp:build`
ne bronche. Les IDE, eux, appliquent leurs propres diagnostics — d'où des erreurs visibles
dans l'éditeur mais absentes du build, et un signal que l'équipe finit par ignorer.

---

## 2. Mesures

Relevés en activant les options puis en lançant `npm run webapp:build`. Erreurs **uniques**
(dédoublonnées par fichier:ligne:colonne), configuration restaurée après chaque mesure.

| Scénario | Erreurs | Fichiers | Ordre de grandeur |
|---|---|---|---|
| Actuel | 0 | 0 | — |
| **A. `strictTemplates` seul** | **38** | **27** | ~1 à 2 jours |
| **B. `strictTemplates` + `strictNullChecks`** | **1387** | **273** | ~3 à 6 semaines |

L'écart d'un facteur 36 s'explique : beaucoup de sous-contrôles de `strictTemplates`
(`strictNullInputTypes`, `strictAttributeTypes`…) ne s'activent réellement que si
`strictNullChecks` est vrai. Activer `strictTemplates` seul est donc **peu coûteux, mais
aussi partiellement inopérant**.

### 2.1 Détail du scénario A — 38 erreurs

| Code | Nb | Nature | Imputable à |
|---|---|---|---|
| `TS2322` | 18 | `Type 'string' is not assignable to type 'TagSeverity'` | PrimeNG (`p-tag`) |
| `NG8007` | 5 | Les deux moitiés du `[(ngModel)]` ne visent pas la même cible | PrimeNG (`pKeyFilter`) |
| `TS2345` | 5 | types d'événements et de sélection | 3 applicatives, 2 PrimeNG |
| `TS2339` | 5 | propriété inexistante sur le composant ou le modèle | applicatif |
| `NG8002` | 4 | `Can't bind to 'severity'` sur `p-progressbar` | PrimeNG |
| `TS4104` | 1 | Tableau `readonly` affecté à un type mutable | applicatif |

**29 des 38 erreurs (76 %) sont imputables à PrimeNG** et disparaîtront d'elles-mêmes avec
la migration. Les traiter aujourd'hui serait du travail jeté.

Le cas des `NG8007` mérite d'être noté, car il n'est pas évident : la directive
`pKeyFilter` de PrimeNG déclare `outputs: { ngModelChange: "ngModelChange" }`. Sur un
`<input pInputText pKeyFilter [(ngModel)]="x">`, la moitié `[ngModel]` vise donc `NgModel`
et la moitié `(ngModelChange)` vise `KeyFilter`. Ce ne sont **pas** des bugs applicatifs.

### 2.1.1 Les 9 erreurs applicatives — corrigées le 2026-07-18

Toutes traitées hors activation de l'option ; corriger un bug ne casse rien.

| Fichier | Erreur | Correctif |
|---|---|---|
| `produit-synthese-tab.html` ×2, `produit-stock-tab.html` ×1 | `datePeremption` n'existe pas sur `ILotProduit` | → `expiryDate` (le champ réel, déjà utilisé par le tri TS), avec `\| date: 'dd/MM/yyyy'` |
| `suggestion-produit-panel.html` ×2 | `totalPages` n'existe pas | `computed` restauré (il était commenté), avec garde `rows > 0` ; garde de bornes rétablie dans `goToPage` |
| `commande-requested.ts` ×2 | `Event` vs `KeyboardEvent` | signatures élargies à `Event`, le type réellement fourni par un host listener |
| `produit-prix-creation.ts` ×1 | tableau `readonly` | copie mutable par spread, types littéraux préservés |
| `home-base.html` ×2 | `ToggleButtonChangeEvent` | corrigé en parallèle par l'équipe |

**Impact réel de ces correctifs** — ce n'étaient pas des broutilles de typage :

- les alertes « lot expirant dans moins de 3 mois » affichaient une **date vide** ;
- l'infobulle de transfert FEFO affichait `undefined` en guise de date d'expiration ;
- le compteur de pagination affichait « Page 1 / » et le bouton « suivant », comparé à
  `undefined`, n'était **jamais désactivé** — on pouvait paginer au-delà de la fin.

Reste après correction : **28 erreurs, toutes imputables à PrimeNG.**

### 2.2 Détail du scénario B — 1387 erreurs

| Code | Nb | Nature |
|---|---|---|
| `TS2531` / `TS2532` | 397 | `Object is possibly 'null' / 'undefined'` |
| `TS2564` | 319 | Propriété sans initialiseur ni assignation certaine |
| `TS2322` | 274 | `Type 'null' is not assignable to type 'Date \| undefined'` |
| `TS2345` | 249 | `string \| undefined` passé où `string` est attendu |
| `TS18048` / `TS18047` / `TS18049` | 110 | Accès à une valeur potentiellement absente |
| autres | 38 | dont les 38 du scénario A |

Ce volume ne se traite pas en une passe : c'est un chantier à part entière, avec un vrai
risque de régression si on « fait taire » les erreurs à coups de `!` plutôt que de corriger
la logique.

---

## 3. Ce que le durcissement n'apporte **pas**

> ⚠ Vérifié empiriquement, contre l'intuition initiale.

Le bug qui a motivé ce plan — `(searchEvent)` au lieu de `(search)` — **n'est PAS détecté
par `strictTemplates`**. Test : le mauvais alias réintroduit avec `strictTemplates: true`
ne produit aucune erreur de compilation.

Raison : Angular autorise **tout** nom d'événement sur un élément. Un nom inconnu devient
un écouteur DOM natif, ce qui est légal. Il n'existe aucune option de compilation pour
l'interdire, contrairement aux propriétés inconnues (`NG8002`).

Corollaires :

- Le diagnostic *« Event X is not emitted by any applicable directives »* vient du
  **langage service de l'IDE**, plus strict que le compilateur. C'est un signal précieux :
  **une erreur affichée dans l'éditeur ne remonte pas forcément au build.**
- La seule protection fiable contre cette classe de bug reste le **test qui émet depuis le
  vrai output** (`instance.searchEvent.emit(...)`), et non via `triggerEventHandler`, qui
  se contente de l'écouteur DOM et passe au vert à tort.

Ce plan ne doit donc pas être vendu comme « ça aurait évité le bug ». Il faut le justifier
sur ce qu'il apporte réellement : les 9 erreurs non-PrimeNG du scénario A, et à terme la
solidité du scénario B.

---

## 4. Recommandation

**Ne rien activer maintenant.** Trois raisons :

1. 47 % des erreurs du scénario A concernent PrimeNG et s'évaporeront avec la migration —
   les corriger aujourd'hui, c'est payer deux fois.
2. `pretest` enchaîne sur `lint`, et le build est déjà dans la chaîne CI : passer `false`
   à `true` bloque tout le monde tant que les 38 (ou 1387) erreurs ne sont pas soldées.
3. La migration PrimeNG recâble massivement les templates ; durcir en même temps ferait se
   télescoper deux sources de rupture, et rendrait chaque échec plus difficile à imputer.

### Séquence proposée

| Étape | Quand | Contenu | Effort |
|---|---|---|---|
| **0** | ~~maintenant~~ | ✅ **Fait le 2026-07-18** — 9 erreurs applicatives corrigées (cf. §2.1.1), sans activer l'option | 0,5 j |
| **1** | après Phase 3 de la migration | Re-mesurer. Les 28 erreurs PrimeNG restantes doivent être tombées à zéro. Activer `strictTemplates: true` | 0,5 j |
| **2** | chantier dédié, après Phase 4 | `strictNullChecks` : découper par dossier via des `tsconfig` de périmètre, corriger dossier par dossier, activer globalement en dernier | 3-6 sem. |

L'étape 0 est indépendante et sans risque : corriger un bug ne casse rien, et ces
liaisons-là ne font rien aujourd'hui.

### Garde-fou à mettre en place en parallèle

Puisque le compilateur ne protège pas des alias d'outputs, inscrire la règle dans le
`README` du Design System (fait) et dans la revue :

> Tout branchement sur un composant tiers doit être testé en émettant depuis l'output réel
> de l'instance, jamais via `triggerEventHandler` avec le nom du binding.

---

## 5. Journal des mesures

Reproductible :

```bash
# Scénario A
#   tsconfig.json : strictTemplates -> true
npm run webapp:build 2>&1 | grep -cE "error (TS|NG)"

# Scénario B
#   tsconfig.json : strictTemplates -> true ET strictNullChecks -> true
npm run webapp:build 2>&1 | grep -cE "error (TS|NG)"
```

Dédoublonner par `fichier:ligne:colonne` : le build répète chaque diagnostic plusieurs fois,
un simple `grep -c` surestime d'un facteur ~3.

| Date | Scénario | Erreurs uniques | Fichiers |
|---|---|---|---|
| 2026-07-18 | A — `strictTemplates` | 38 | 27 |
| 2026-07-18 | B — A + `strictNullChecks` | 1387 | 273 |
| 2026-07-18 | A, après correction des 9 erreurs applicatives | **28** | 21 |
