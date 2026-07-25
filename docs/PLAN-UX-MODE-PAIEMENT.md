# Plan — Améliorations UX du choix des modes de règlement

> Statut : **implémenté** (préalable technique + P1 à P5) — juillet 2026.
> P6 (raccourcis clavier) reste en attente d'un besoin utilisateur identifié.
> Contexte : notées lors de la migration de `payment-mode.component.ts`
> (PrimeNG → ng-bootstrap). Analyse de faisabilité faite sur le code actuel
> (`features/sales/ui/payment-mode/`, `PaymentModeManagerService`).

## Fonctionnement actuel

- L'écran démarre avec une seule ligne « Espèces » (CASH).
- Un bouton « + » ouvre un popover listant les modes non encore utilisés ;
  en choisir un ajoute une ligne.
- Le bouton « x » d'une ligne :
  - **supprime** la ligne directement s'il en reste plusieurs ;
  - **ouvre un popover de remplacement** si c'est la dernière ligne restante
    (impossible de descendre à zéro mode).
- Pas de réordonnancement. Les montants ne se redistribuent que dans deux cas :
  la suppression d'une ligne reverse son montant sur la première ligne restante,
  et l'ajout d'un mode quand `maxPaymentModes` est atteint réattribue
  automatiquement le reste à l'autre ligne.

## Préalable technique — clarifier qui possède l'état

Avant toute piste touchant à la logique (P1, P5).

### Le problème : `PaymentModeEntry` est défini deux fois

Deux définitions exportées, **non identiques** :

- `payment-mode.component.ts` : `isReadonly?: boolean` (**optionnel**) ;
- `payment-mode-manager.service.ts` : `isReadonly: boolean` (**requis**).

Conséquences concrètes :

1. **Ambiguïté d'import silencieuse.** L'auto-import de l'IDE choisit
   parfois l'une, parfois l'autre. Le typage structurel fait que ça compile
   la plupart du temps — jusqu'à ce qu'un objet typé avec la version du
   composant (`isReadonly` possiblement `undefined`) soit passé à du code
   typé avec celle du service (requis). L'erreur résultante est déroutante :
   « `PaymentModeEntry` n'est pas assignable à `PaymentModeEntry` ».
2. **Divergence garantie dans le temps.** Tout champ ajouté pour P1 (ex.
   tracer la dernière ligne modifiée) devrait être ajouté aux deux
   définitions ; personne n'y pensera, et le typage structurel laissera la
   divergence s'installer sans bruit jusqu'à produire le cas 1.
3. **La définition du service est un piège pur.** Elle est exportée mais
   **jamais utilisée dans le service lui-même** : son état `selectedModes`
   est typé `signal<IPaymentMode[]>` et fabrique ses entrées en écrasant
   `amount` sur des objets `IPaymentMode` (`{ ...cashMode, amount:
   undefined }`). Qui l'importe croit typer ce que le service produit —
   aucun code ne produit jamais cette forme.
4. **Symptôme d'une confusion conceptuelle.** La frontière entre un
   **mode** (référentiel : code, libellé, classes CSS) et une **entrée**
   (ligne de règlement d'une vente : mode + montant + montant versé) n'est
   pas assumée — le service stocke des modes enrichis d'un `amount`.

Par ailleurs, le composant n'utilise du service que `modes()` et
`getCashMode()` : tout l'état du service (`selectedModes`, `addMode`,
`removeMode`, `updateModeAmount`, `resetAmounts`, `reset`,
`setSelectedModes`, `getTotalAmount`) est du **code mort** qui duplique
l'état du composant.

### Décision

- L'état vit dans le **composant** ; la définition de `PaymentModeEntry`
  du composant devient la seule source de vérité.
- Supprimer du service : sa définition de `PaymentModeEntry`, l'état
  `selectedModes` et toutes les méthodes mortes listées ci-dessus.
- Réduire le service à son vrai rôle : **chargement et enrichissement du
  référentiel des modes** (`modes()`, `getCashMode()`,
  `enrichPaymentMode`).

Faire P1 avant ce préalable reviendrait à retravailler la logique de
redistribution sur des types dont on ne sait pas lequel fait foi.

## Pistes retenues, par ordre de priorité

### P1 — Fiabiliser la redistribution des montants (bug latent)

> Contrainte métier actuelle : **le nombre maximum de modes de règlement
> sur une vente est de 2** (`maxPaymentModes`). Le plan est dimensionné
> pour ce cas.

La règle UX actuelle (« le solde est reversé sur la première ligne ») est un
effet de bord de l'ordre du tableau, pas un choix assumé. Surtout, le code
sous-jacent est fragile : dans `onRemovePaymentMode`, la redistribution
**mute `first.amount` après le `selectedModes.update()`**, sans nouveau
`set()` — avec OnPush + signals, le rendu de ce changement n'est pas
fiable. C'est un bug réel dès aujourd'hui, même à 2 modes.

**À faire :**

1. Trancher la règle UX : redistribuer vers la **dernière ligne modifiée**
   (plutôt que la première du tableau). À 2 modes, cela revient à
   « l'autre ligne » — comportement simple à expliquer au caissier.
2. Réécrire la redistribution en immutable (nouveau tableau via
   `selectedModes.set`/`update`, pas de mutation d'objets en place).

**Dette assumée (pas de chantier)** : la répartition automatique de
`onAmountInput` cherche « l'autre mode » via `modes.find(m => m !== entry)`
et ne fonctionne que pour exactement 2 modes. Tant que le maximum métier
reste 2, c'est correct ; si `maxPaymentModes` devait passer à 3+, cette
logique serait à généraliser. Documenter l'hypothèse dans le code
(commentaire près de la contrainte) plutôt que de généraliser à vide.

### P2 — Remplacer les popovers par le motif `ngbDropdown`

Aligner la sélection de mode sur le motif `ngbDropdown` utilisé par le reste
de l'application migrée (cf. `sales-journal`, `devis-list`), avec les libellés
de mode stylisés comme icônes-images.

Bonus au-delà de la cohérence visuelle : le popover actuel nécessite un hack
`positionTarget` + `detectChanges()` synchrone parce que les deux popovers
sont déclarés hors de la boucle des lignes. Un `ngbDropdown` par ligne
supprime ce hack, les deux `viewChild` de popover et les deux signaux de
position (~30 lignes en moins). **Meilleur ratio coût/bénéfice du plan.**

### P3 — Tooltips sur les icônes de sélection de mode

L'icône est déjà une image portant le libellé du mode ; ajouter un
`ngbTooltip` (déjà dans le stack) par item du menu de sélection aide un
caissier peu familier des pictogrammes. Quasi gratuit — à faire en même
temps que P2.

### P4 — Remplacer `<input #paymentInput>` par `app-input-number`

Le composant existe (`shared/ui/input-number/`, CVA, formatage français des
milliers — un vrai plus pour des montants FCFA). Trois points d'attention :

- Le focus programmatique repose sur
  `viewChildren<ElementRef<HTMLInputElement>>('paymentInput')` et des
  `document.querySelector('.payment-mode-input input')` → exposer une API
  `focus()` publique sur `app-input-number` (ou adapter les requêtes).
- `onAmountInput` lit `event.target.value` → passer par `ngModelChange`.
- `(keydown.enter)="submit()"` à poser sur l'hôte du composant
  (l'événement bubble, ça fonctionne).

### P5 — Différencier visuellement « supprimer » et « remplacer » sur le bouton

Le « x » supprime ou remplace selon le nombre de lignes restantes, sans
indice visuel.

**Décision : garder un bouton unique, mais conditionner son icône et sa
couleur selon la fonctionnalité qu'il va déclencher.** Le critère est déjà
celui de `onRemovePaymentMode` (`selectedModes().length === 1`) :

- **Plusieurs lignes → suppression** : icône `pi pi-times`,
  sévérité `danger` (comportement et rendu actuels).
- **Dernière ligne → remplacement** : icône de remplacement
  (ex. `pi pi-sync` ou `pi pi-arrow-right-arrow-left`),
  sévérité distincte (ex. `warn`), tooltip « Changer de mode ».

Coût quasi nul : le comportement du composant ne change pas, seul le
template conditionne `icon`/`severity` sur `selectedModes().length === 1`.
Pas besoin de gérer l'état « zéro mode » (le remplacement reste le seul
chemin sur la dernière ligne — l'effect d'initialisation qui ré-ajoute CASH
à zéro mode n'est donc jamais sollicité).

### P6 — Raccourcis clavier (optionnel, sous condition)

`Enter` soumet déjà depuis les inputs. Le mixin
`keyboard-shortcuts.mixin.ts` existe, mais il est utilisé au niveau **page**
par `sale-creation`, `sale-assurance` et `sale-carnet` (capture
`window:keydown`) — les trois parents qui embarquent `app-payment-mode`.
Ajouter des raccourcis dans le composant enfant exige de coordonner avec ces
trois pages pour éviter les collisions.

**Ne faire que si un besoin utilisateur concret est identifié** (ex.
« ajouter un mode » au clavier), pas par principe.

## Piste écartée

- **Confirmation à la suppression d'une ligne** : écartée délibérément.
  Sur un poste de caisse, une confirmation ralentit le flux et l'action est
  réversible en un clic (on rajoute le mode immédiatement). La seule
  alternative propre serait un pattern « undo », dont le coût dépasse
  l'enjeu.

## Prochaine étape

Valider la priorisation avec l'équipe produit, puis implémenter dans
l'ordre : préalable technique → P1 → P2+P3 → P4 → P5, avec tests à chaque
étape (redistribution à 2 et 3 modes, remplacement du dernier mode, focus
après ajout/remplacement).
