# Plan de Centralisation des Raccourcis Clavier - Module Sales

## 1. Diagnostic complet

### 1.1 Etat actuel des 3 composants enfants

| Composant | Approche | Raccourcis | Problemes |
|---|---|---|---|
| `sale-creation` | `if/else` en cascade (90 lignes) | F2, F5-F11, Escape, Ctrl+S, Ctrl+P | Verbose, non maintenable, raccourcis metier POS mal assignes |
| `sale-assurance` | Tableau declaratif (6 lignes) | F2-F4, F9-F10 | Bonne approche mais incomplete, pas de Ctrl/Alt |
| `sale-carnet` | **Aucun** | - | Manque total de raccourcis clavier |

### 1.2 Etat du parent `sales-home`

- **Aucun raccourci clavier** dans `sales-home.component.ts`
- Le **switch entre types de vente** (COMPTANT/ASSURANCE/CARNET) se fait via `NgbNav` + signal `active`
- La methode `onNavChange()` gere la confirmation si une vente est en cours
- **Consequence** : impossible de changer de type de vente au clavier

### 1.3 Incoherences UX critiques pour un POS

En pharmacie, le caissier utilise intensivement le clavier pour la rapidite :

1. **F9 = "Ventes en attente" en COMPTANT mais "Finaliser" en ASSURANCE** - confusion
2. **F6 = "Mise en attente" en COMPTANT mais "Effacer produit" dans le legacy** - incoherence
3. **Carnet n'a aucun raccourci** - le caissier doit prendre la souris
4. **Aucun raccourci pour le client** en COMPTANT (F11 affiche juste un message inutile)
5. **Ctrl+P declenche l'impression navigateur** au lieu de l'impression ticket
6. **Aucun raccourci pour changer de type de vente** - oblige a cliquer dans le sidebar
7. **F-keys bloquees dans les inputs** en `sale-creation` (pas de filtre) mais filtrees en `sale-assurance`

### 1.4 Analyse du `ShortcutsHelpDialogComponent`

**Fichier** : `shared/shortcuts/shortcuts-help-dialog.component.ts`

**Couplage fort avec le legacy** :
- Import direct de `SellingHomeShortcutsService` (legacy) ligne 6
- Import direct de `KeyboardShortcut` depuis le legacy ligne 5
- La propriete `shortcutsService?: SellingHomeShortcutsService` est injectee manuellement par le caller
- `loadDynamicShortcuts()` appelle `shortcutsService.getShortcutsByCategory()` (methode specifique au legacy)

**Points positifs reutilisables** :
- Affichage par categories avec icones et badges
- Detection Tauri/Web pour adapter l'affichage
- Quick Start Flow visuel (F2 > F3 > F5 > F9)
- Section conseils d'utilisation
- Support impression (CSS `@media print`)
- Formatage intelligent des touches (fleches, Escape > "Echap", etc.)

**Ce qu'il faut changer** :
- Decouple du `SellingHomeShortcutsService` via une interface commune
- Le Quick Start Flow doit s'adapter au type de vente actif (ASSURANCE a un flux different)

### 1.5 Reference : Legacy (a ne pas modifier)

Le service legacy `selling-home-shortcuts.service.ts` definit un mapping coherent :
- `KeyboardShortcutsService` : moteur central (signal Map, matching event>key, anti-conflit navigateur)
- `SalesShortcutCallbacks` : interface de 20+ callbacks
- `TauriKeyboardService` : detection plateforme, shortcuts supplementaires en desktop
- Separation Web (F-keys + Alt) vs Desktop (Ctrl)
- Modale d'aide F1

**On ne modifie pas le legacy.** On s'en inspire pour le nouveau module.

---

## 2. Architecture cible

### 2.1 Deux niveaux de raccourcis

Les raccourcis se repartissent sur **deux niveaux** :

| Niveau | Composant | Responsabilite | Exemples |
|--------|-----------|---------------|----------|
| **Global** | `sales-home` | Navigation entre types de vente, vendeur, ventes en attente | Alt+1/2/3, F11 |
| **Contextuel** | `sale-creation/assurance/carnet` | Actions specifiques a la vente en cours | F2-F10, Escape |

**Justification** : Le switch de type de vente est gere par `sales-home` (NgbNav + `active` signal).
Les composants enfants ne connaissent pas les autres onglets. Il est donc naturel que les raccourcis
de navigation inter-onglets soient au niveau parent.

### 2.2 Structure des fichiers

```
features/sales/
  shared/
    mixins/
      keyboard-shortcuts.mixin.ts    <-- NOUVEAU : mixin raccourcis (composants enfants)
      index.ts                       <-- MAJ : re-export
  feature/
    sales-home/
      sales-home.component.ts        <-- MAJ : ajout raccourcis globaux (switch type vente)

shared/
  shortcuts/
    shortcuts-provider.interface.ts   <-- NOUVEAU : interface commune pour decouplage
    shortcuts-help-dialog.component.ts <-- MAJ : utiliser l'interface au lieu du service legacy
```

---

## 3. Mapping unifie des raccourcis POS

### 3.1 Philosophie UX POS pharmacie

**Flux caissier type** (inspire des terminaux de pharmacie Winpharma, LGPI, LEO) :

```
Chercher produit (F2) > Quantite (F3) > Ajouter (F5) > [repeter]
  > Client si necessaire (F4)
  > Finaliser/Payer (F9)
  > [Ticket imprime automatiquement]
  > Nouveau client...
```

**Principes** :
- Les **F-keys** suivent l'ordre du flux de travail (F2>F3>F4>F5 = chercher>quantite>client>ajouter)
- **F9 = Finaliser TOUJOURS** (standard POS, jamais un autre role)
- Les F-keys fonctionnent **meme dans les inputs** (le caissier ne doit pas cliquer hors du champ)
- **Alt+chiffre** pour changer de type de vente (mnemonique : 1=Comptant, 2=Assurance, 3=Carnet)
- **Escape** = annuler/retour (standard universel)

### 3.2 Raccourcis contextuels (dans les composants enfants)

#### F-Keys : Flux de vente (identiques pour COMPTANT, ASSURANCE, CARNET)

| Touche | Action | Detail | Badge |
|--------|--------|--------|-------|
| **F1** | Aide raccourcis | Ouvre la modale d'aide | Essentiel |
| **F2** | Focus recherche produit | Focus sur le champ autocomplete produit | Essentiel |
| **F3** | Focus quantite | Focus sur le champ quantite | Essentiel |
| **F4** | Focus client | COMPTANT: ouvre overlay client / ASSURANCE+CARNET: focus barre recherche client | Essentiel |
| **F5** | Ajouter produit | Ajoute le produit selectionne avec la quantite | Essentiel |
| **F6** | Effacer selection produit | Vide le champ produit et la quantite | |
| **F7** | Voir stock produit | Affiche le detail stock du produit selectionne | |
| **F8** | Appliquer remise | Ouvre le selecteur de remise | |
| **F9** | **Finaliser (Payer)** | Declenche la validation du paiement | Important |
| **F10** | Mettre en attente | Sauvegarde la vente en attente | |

**Regles specifiques prevente** :
- F9 = "Enregistrer prevente" (meme touche, action adaptee)
- F10 = desactive (pas de mise en attente en mode prevente)

#### Touches speciales

| Touche | Action | Condition |
|--------|--------|-----------|
| **Escape** | Annuler la vente | Demande confirmation si lignes presentes |
| **Delete** | Supprimer ligne selectionnee | Hors d'un input uniquement |

#### Alt+Lettre : Actions rapides (Web-safe)

| Touche | Action | Mnemonique |
|--------|--------|------------|
| **Alt+P** | Focus produit (=F2) | **P**roduit |
| **Alt+Q** | Focus quantite (=F3) | **Q**uantite |
| **Alt+C** | Focus client (=F4) | **C**lient |
| **Alt+V** | Focus vendeur | **V**endeur |
| **Alt+I** | Imprimer facture | **I**mprimer |
| **Alt+T** | Imprimer ticket | **T**icket |
| **Alt+R** | Appliquer remise | **R**emise |
| **Alt+Shift+R** | Retirer remise | Retirer **R**emise |
| **Alt+S** | Mettre en attente | **S**tandby |
| **Alt+F** | Finaliser (=F9) | **F**inaliser |

### 3.3 Raccourcis globaux (dans sales-home)

Ces raccourcis sont geres par `sales-home` car ils touchent a la navigation inter-onglets.

| Touche | Action | Justification |
|--------|--------|---------------|
| **Alt+1** | Basculer vers Comptant | Mnemonique : 1er onglet |
| **Alt+2** | Basculer vers Assurance | Mnemonique : 2e onglet |
| **Alt+3** | Basculer vers Carnet | Mnemonique : 3e onglet |
| **F11** | Ouvrir ventes en attente | Action globale (drawer dans sales-home) |
| **Alt+B** | Ouvrir ventes en attente (alt.) | **B** = Back-log |

**Pourquoi dans sales-home ?**
- Le `NgbNav` et le signal `active` sont dans `sales-home`
- La methode `onNavChange()` gere la confirmation "vente en cours, voulez-vous changer ?"
- Les composants enfants ne connaissent pas les autres onglets
- Les ventes en attente (drawer) sont aussi dans `sales-home`

### 3.4 Raccourcis Desktop Tauri (supplementaires)

En mode Tauri, on peut utiliser les Ctrl qui sont reserves par le navigateur en mode Web :

| Touche | Action | Badge |
|--------|--------|-------|
| **Ctrl+S** | Mettre en attente | Desktop |
| **Ctrl+Enter** | Finaliser rapidement | Desktop |
| **Ctrl+N** | Nouvelle vente (annuler) | Desktop |
| **Ctrl+F** | Focus recherche produit | Desktop |
| **Ctrl+P** | Imprimer ticket | Desktop |
| **Ctrl+Shift+P** | Imprimer facture | Desktop |
| **Ctrl+D** | Appliquer remise | Desktop |
| **Ctrl+1** | Basculer vers Comptant | Desktop |
| **Ctrl+2** | Basculer vers Assurance | Desktop |
| **Ctrl+3** | Basculer vers Carnet | Desktop |

---

## 4. Plan d'implementation detaille

### Etape 1 : Interface commune `ShortcutsProvider`

Decouple le `ShortcutsHelpDialogComponent` du legacy.

**Fichier** : `shared/shortcuts/shortcuts-provider.interface.ts`

```typescript
import { KeyboardShortcut } from '../../entities/sales/selling-home/racourci/keyboard-shortcuts.service';

export interface ShortcutsProvider {
  getShortcutsByCategory(): Map<string, KeyboardShortcut[]>;
  isRunningInTauri(): boolean;
}
```

**Modification** : `shortcuts-help-dialog.component.ts`
- Remplacer `shortcutsService?: SellingHomeShortcutsService` par `shortcutsService?: ShortcutsProvider`
- L'import de `SellingHomeShortcutsService` est retire
- Le legacy continue a fonctionner car `SellingHomeShortcutsService` implemente deja les memes methodes

### Etape 2 : Creer le mixin `createKeyboardShortcuts`

**Fichier** : `features/sales/shared/mixins/keyboard-shortcuts.mixin.ts`

```typescript
export interface SaleShortcutCallbacks {
  // Navigation
  focusProductSearch: () => void;
  focusQuantity: () => void;
  focusCustomer: () => void;
  focusVendor?: () => void;

  // Actions Produit
  addProduct: () => void;
  removeSelectedLine: () => void;
  clearProduct: () => void;
  viewProductStock?: () => void;

  // Finalisation
  finalizeSale: () => void;
  putOnStandby: () => void;
  cancelSale: () => void;

  // Impression
  printReceipt?: () => void;
  printInvoice?: () => void;

  // Remises
  applyDiscount?: () => void;
  removeDiscount?: () => void;

  // Prevente
  saveAsPresale?: () => void;
}

export interface KeyboardShortcutsConfig {
  saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET';
  isPresale?: Signal<boolean>;
}

export function createKeyboardShortcuts(config: KeyboardShortcutsConfig, callbacks: SaleShortcutCallbacks) {
  const keyboardService = inject(KeyboardShortcutsService);
  const tauriService = inject(TauriKeyboardService);
  const modalService = inject(NgbModal);

  // Enregistrer les raccourcis communs (F-keys)
  // Enregistrer les raccourcis Alt (Web-safe)
  // Enregistrer les raccourcis Ctrl (Tauri uniquement)

  return {
    handleKeyboardEvent(event: KeyboardEvent): void { ... },
    getShortcutsByCategory(): Map<string, KeyboardShortcut[]> { ... },
    isRunningInTauri(): boolean { ... },
    destroy(): void { ... },
  };
}
```

**Points cles** :
- `handleKeyboardEvent` inclut le filtre input/textarea (F-keys passent toujours)
- Retourne `ShortcutsProvider` pour pouvoir ouvrir la modale d'aide
- `destroy()` appele par `DestroyRef` pour cleanup

### Etape 3 : Raccourcis globaux dans `sales-home`

**Fichier** : `sales-home.component.ts`

Ajouter le host binding et un handler leger pour les raccourcis de navigation :

```typescript
@Component({
  host: {
    '(window:keydown)': 'handleGlobalKeyboardEvent($event)',
  },
})
export class SalesHomeComponent {
  handleGlobalKeyboardEvent(event: KeyboardEvent): void {
    // Alt+1/2/3 : Switch type de vente
    if (event.altKey && ['1', '2', '3'].includes(event.key)) {
      event.preventDefault();
      const tabMap: Record<string, string> = { '1': 'comptant', '2': 'assurance', '3': 'carnet' };
      this.switchToTab(tabMap[event.key]);
      return;
    }

    // F11 : Ventes en attente
    if (event.key === 'F11') {
      event.preventDefault();
      this.openPendingSales();
      return;
    }
  }

  private switchToTab(tab: string): void {
    const currentSale = this.salesFacade.currentSale();
    if (currentSale?.salesLines?.length > 0) {
      this.confirmDialog().onConfirm(
        () => { this.active.set(tab); this.focusActiveTab(); },
        'Changement de type de vente',
        'Vous avez une vente en cours. Voulez-vous vraiment changer ?',
      );
    } else {
      this.active.set(tab);
      this.focusActiveTab();
    }
  }
}
```

**Note sur la propagation** : `sales-home` gere Alt+1/2/3 et F11 au niveau global.
Les F-keys restantes (F1-F10) sont gerees par les composants enfants via leur propre host binding.
Comme les enfants sont dans le DOM de `sales-home`, l'event se propage naturellement.
Le parent traite les siens, l'enfant actif traite les siens. Pas de conflit car les touches sont differentes.

### Etape 4 : Integrer dans les 3 composants enfants

**Pattern identique pour les 3 composants** :

```typescript
@Component({
  host: { '(window:keydown)': 'handleKeyboardEvent($event)' },
})
export class SaleXxxComponent {
  private keyboardShortcuts = createKeyboardShortcuts(
    { saleType: 'COMPTANT', isPresale: computed(() => this.isPresale()) },
    {
      focusProductSearch: () => this.productHandling.focusProductSearch(),
      focusQuantity: () => this.quantityComponent()?.getFocus(),
      focusCustomer: () => ...,  // specifique a chaque composant
      finalizeSale: () => this.onSave(),
      putOnStandby: () => this.onPutOnHold(),
      cancelSale: () => this.onCancel(),
      addProduct: () => ...,
      removeSelectedLine: () => ...,
      clearProduct: () => this.productHandling.resetProductSelection(),
      printReceipt: () => this.onPrint(),
      applyDiscount: () => ...,
    },
  );

  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardShortcuts.handleKeyboardEvent(event);
  }
}
```

**Modifications specifiques** :

| Composant | Supprimer | Ajouter | `focusCustomer` |
|---|---|---|---|
| `sale-creation` | 90 lignes de `if/else` | ~20 lignes mixin | Ouvre overlay client |
| `sale-assurance` | `keyboardShortcuts[]` + `handleKeyboardEvent()` | ~20 lignes mixin | Focus `insuranceDataBar.searchInput` |
| `sale-carnet` | rien | host binding + ~20 lignes mixin | Focus `insuranceDataBar.searchInput` |

### Etape 5 : Adapter `ShortcutsHelpDialogComponent`

**Modification minimale** : Remplacer le type de `shortcutsService` par `ShortcutsProvider`.

```diff
- import { SellingHomeShortcutsService } from '../../entities/sales/selling-home/racourci/selling-home-shortcuts.service';
+ import { ShortcutsProvider } from './shortcuts-provider.interface';

  export class ShortcutsHelpDialogComponent implements OnInit {
-   public shortcutsService?: SellingHomeShortcutsService;
+   public shortcutsService?: ShortcutsProvider;
```

Le legacy `SellingHomeShortcutsService` implemente deja `getShortcutsByCategory()` et `isRunningInTauri()`,
donc il est compatible sans modification.

**Adapter le Quick Start Flow** selon le type de vente :

```
COMPTANT :  F2 > Produit > F3 > Quantite > F5 > Ajouter > F9 > Finaliser
ASSURANCE : F4 > Client  > F2 > Produit  > F5 > Ajouter > F9 > Finaliser
CARNET :    F4 > Client  > F2 > Produit  > F5 > Ajouter > F9 > Finaliser
```

### Etape 6 : Filtre intelligent input/textarea

Integre dans le mixin, ce filtre est la cle de l'UX POS :

```typescript
function shouldHandleEvent(event: KeyboardEvent): boolean {
  const target = event.target as HTMLElement;
  const isInInput = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'
                    || target.getAttribute('role') === 'combobox';  // p-select, p-autocomplete

  // F-keys : TOUJOURS actives (standard POS - le caissier tape F9 meme dans le champ produit)
  if (/^F\d{1,2}$/.test(event.key)) {
    return true;
  }

  // Alt+Lettre : TOUJOURS actifs (ne conflictent pas avec la saisie)
  if (event.altKey && !event.ctrlKey) {
    return true;
  }

  // Escape : TOUJOURS actif (annuler = universel)
  if (event.key === 'Escape') {
    return true;
  }

  // Ctrl+X dans un input : laisser le navigateur gerer (copier/coller/couper)
  if (event.ctrlKey && isInInput) {
    return false;
  }

  // Delete, Backspace dans un input : laisser la saisie
  if ((event.key === 'Delete' || event.key === 'Backspace') && isInInput) {
    return false;
  }

  // Hors input : tout le reste est gere
  return !isInInput;
}
```

---

## 5. Ordre d'execution

| # | Tache | Fichiers concernes |
|---|-------|--------------------|
| 1 | Creer `ShortcutsProvider` interface | `shared/shortcuts/shortcuts-provider.interface.ts` |
| 2 | Creer `keyboard-shortcuts.mixin.ts` | `features/sales/shared/mixins/keyboard-shortcuts.mixin.ts` |
| 3 | Exporter depuis le barrel | `features/sales/shared/mixins/index.ts` |
| 4 | Ajouter raccourcis globaux dans `sales-home` | `sales-home.component.ts` |
| 5 | Refactoriser `sale-creation` | Supprimer 90 lignes, ajouter ~20 lignes |
| 6 | Refactoriser `sale-assurance` | Supprimer array+handler, ajouter ~20 lignes |
| 7 | Ajouter raccourcis a `sale-carnet` | Ajouter host binding + ~20 lignes |
| 8 | Adapter `ShortcutsHelpDialogComponent` | Remplacer type, adapter Quick Start |

---

## 6. Comparaison avant/apres

### Mapping des touches

| Touche | AVANT (creation) | AVANT (assurance) | AVANT (carnet) | **APRES (unifie)** |
|--------|------------------|-------------------|----------------|---------------------|
| F1 | - | - | - | **Aide raccourcis** |
| F2 | Focus produit | Focus produit | - | **Focus produit** |
| F3 | - | Focus client | - | **Focus quantite** |
| F4 | - | Mise en attente | - | **Focus client** |
| F5 | Ajouter produit | - | - | **Ajouter produit** |
| F6 | Mise en attente | - | - | **Effacer selection** |
| F7 | Annuler vente | - | - | **Voir stock produit** |
| F8 | Enregistrer | - | - | **Appliquer remise** |
| F9 | Ventes en attente | Finaliser | - | **Finaliser (Payer)** |
| F10 | Focus paiement | Annuler | - | **Mettre en attente** |
| F11 | Message inutile | - | - | **Ventes en attente** |
| Escape | Annuler | - | - | **Annuler vente** |
| Alt+1 | - | - | - | **> Comptant** |
| Alt+2 | - | - | - | **> Assurance** |
| Alt+3 | - | - | - | **> Carnet** |
| Ctrl+S | Enregistrer | - | - | *Desktop: Attente* |
| Ctrl+P | Imprimer | - | - | *Desktop: Ticket* |

### Lignes de code

| Composant | Avant | Apres | Delta |
|-----------|-------|-------|-------|
| `sale-creation` handleKeyboard | ~90 lignes | ~5 lignes | **-85** |
| `sale-assurance` handleKeyboard | ~15 lignes | ~5 lignes | **-10** |
| `sale-carnet` handleKeyboard | 0 lignes | ~5 lignes | +5 |
| `sales-home` handleKeyboard | 0 lignes | ~20 lignes | +20 |
| `keyboard-shortcuts.mixin.ts` | - | ~200 lignes | +200 (partage) |
| **Total** | ~105 (duplique) | ~235 (centralise) | Maintenabilite ++ |

---

## 7. Benefices attendus

1. **Coherence UX** : F9 = Finaliser partout, Alt+1/2/3 = changer de type partout
2. **Completude** : CARNET beneficie enfin de tous les raccourcis
3. **Navigation clavier** : Switch entre types de vente sans souris (Alt+1/2/3)
4. **Standard POS** : F-keys actives meme dans les inputs (flux caissier ininterrompu)
5. **Aide accessible** : F1 partout pour afficher les raccourcis disponibles
6. **Maintenabilite** : Un seul endroit pour modifier le mapping
7. **Pattern existant** : Meme approche mixin que product-handling, payment-handling
8. **Pas de regression** : Le legacy n'est pas touche
9. **Decouplage** : `ShortcutsHelpDialogComponent` utilisable par les deux modules

---

## 8. Audit du legacy et corrections appliquees

### 8.1 Problemes identifies dans `selling-home-shortcuts.service.ts`

| # | Probleme | Impact | Correction dans le nouveau module |
|---|----------|--------|-----------------------------------|
| 1 | **Alt+O/A/K** pour switch type de vente - non mnemonique, conflit potentiel avec access keys navigateur | Confusion utilisateur, erreur de frappe | **Alt+1/2/3** : mnemonique (position onglet), pas de conflit access key |
| 2 | **51 raccourcis** dont beaucoup de doublons (F5=Ajouter ET Ctrl+Enter=Ajouter) | Surcharge cognitive (NNGroup recommande < 20 raccourcis essentiels) | ~25 raccourcis (10 F-keys + 10 Alt + 5 Ctrl Desktop) |
| 3 | **Alt+N** utilise en Web - conflit avec Firefox (ouvre menu Fichier) | Raccourci non fonctionnel sur Firefox | Supprime, remplace par **Escape** (standard universel) |
| 4 | **Ctrl+-/=** pour gestion quantite - conflit avec zoom navigateur | Bloque le zoom utilisateur | Supprime en Web, conserve uniquement en mode Tauri |
| 5 | **Pas de filtre input/textarea** dans `KeyboardShortcutsService.handleKeyboardEvent()` | Delete/Escape interceptes meme dans les champs de saisie | Filtre intelligent dans le mixin (F-keys passent toujours, Delete/Backspace bloques dans inputs) |
| 6 | **Switch type de vente** ne passe pas par `onNavChange()` | Pas de confirmation si vente en cours, risque de perte de donnees | Switch via `sales-home.switchToTab()` qui reutilise la logique de confirmation existante |
| 7 | **Service singleton** (`providedIn: 'root'`) avec callbacks dynamiques | Fuite de callbacks si le composant est detruit sans cleanup | Mixin par composant avec cycle de vie lie au `DestroyRef` |

### 8.2 Points positifs du legacy conserves

- Separation Web (F-keys + Alt) vs Desktop (Ctrl) - bonne pratique
- Modale d'aide F1 avec categories et badges
- Detection plateforme Tauri/Web
- Liste `BROWSER_SHORTCUTS` pour eviter les conflits

---

## 9. Sources et references UX

### 9.1 Principes UX fondateurs

| Source | Principe applique | Application dans ce plan |
|--------|-------------------|--------------------------|
| **Nielsen Norman Group - 10 Usability Heuristics** | Heuristique #7 : "Flexibility and efficiency of use - Accelerators" | Les raccourcis sont des accelerateurs : invisibles pour les debutants, indispensables pour les experts |
| **Nielsen Norman Group - Keyboard-Only Navigation** | "Keyboard shortcuts should be discoverable but unobtrusive" | F1 = aide, raccourcis non affiches dans l'UI principale |
| **Fitts's Law** | Le temps pour atteindre une cible depend de la distance et de la taille | F-keys eliminent le deplacement souris (distance = 0) |

### 9.2 Standards et guidelines

| Source | Guideline | Application |
|--------|-----------|-------------|
| **Microsoft - Guidelines for Keyboard UI Design** | "Keystrokes required for a task should not change unexpectedly" | F9 = Finaliser TOUJOURS, quel que soit l'onglet actif |
| **Microsoft - Guidelines for Keyboard UI Design** | "Use Escape to stop or cancel an operation" | Escape = Annuler la vente (standard universel) |
| **Microsoft - Guidelines for Keyboard UI Design** | "Avoid Alt+letter conflicts with access keys" | Alt+1/2/3 au lieu de Alt+O/A/K (pas de conflit avec menus navigateur) |
| **W3C WAI-ARIA - Keyboard Interaction** | "Function keys (F1-F12) should be available regardless of focus context" | Filtre input : F-keys actives meme dans les champs de saisie |

### 9.3 Benchmarks POS reels

| Systeme POS | Observation | Influence sur notre mapping |
|-------------|-------------|-----------------------------|
| **SooPOS** | F-keys pour flux de vente, Ctrl pour actions systeme | Separation F-keys (vente) vs Ctrl (systeme/Desktop) |
| **LS Retail** | Raccourcis contextuels selon l'ecran actif | Architecture 2 niveaux (global parent / contextuel enfant) |
| **Winpharma** (pharmacie FR) | F2=Recherche, Escape=Annuler | F2=Recherche produit, Escape=Annuler vente |
| **LGPI** (pharmacie FR) | Flux lineaire clavier optimise | Flux F2>F3>F5>F9 sans interruption souris |

### 9.4 Ce qui est fonde vs extrapole

**Fonde (principes universels)** :
- Coherence des raccourcis entre contextes (Microsoft Guidelines)
- F-keys actives dans les inputs (standard POS observe)
- Escape = annuler (convention universelle)
- Alt+chiffre pour navigation onglets (Chrome, VS Code, terminals)
- Accelerateurs pour utilisateurs experts (NNGroup Heuristique #7)

**Extrapole (choix de conception)** :
- Le mapping specifique F2=Produit, F3=Quantite, F4=Client, F5=Ajouter, F9=Finaliser
- Il n'existe PAS de standard universel pour le mapping F-key en POS
- Chaque editeur (SooPOS, Winpharma, LGPI) a son propre mapping
- Notre choix s'appuie sur : le legacy existant + un flux logique F2>F3>F4>F5 = chercher>quantite>client>ajouter

### 9.5 Liens

- Nielsen Norman Group - 10 Usability Heuristics : https://www.nngroup.com/articles/ten-usability-heuristics/
- Microsoft - Guidelines for Keyboard UI Design : https://learn.microsoft.com/en-us/windows/apps/design/input/keyboard-interactions
- W3C WAI-ARIA Authoring Practices : https://www.w3.org/WAI/ARIA/apg/practices/keyboard-interface/
- Fitts's Law : https://www.interaction-design.org/literature/topics/fitts-law
