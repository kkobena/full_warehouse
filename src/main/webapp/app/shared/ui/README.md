# Composants UI Partagés (app/shared/ui)

Design System de composants réutilisables wrappant PrimeNG et NgBootstrap pour une API simplifiée et cohérente.

## 📦 Composants Disponibles

### 1. **ButtonComponent** (`app-button`)

Wrapper autour de PrimeNG Button avec API signals.

**Usage :**
```typescript
import { ButtonComponent } from '../../shared/ui';

@Component({
  imports: [ButtonComponent]
})
```

```html
<!-- Bouton simple -->
<app-button 
  label="Sauvegarder" 
  icon="pi pi-save"
  severity="primary"
  (onClick)="save()" />

<!-- Bouton avec loading -->
<app-button 
  label="Enregistrer"
  icon="pi pi-check"
  [loading]="isSaving()"
  [disabled]="!isValid()"
  severity="success" />

<!-- Bouton outlined -->
<app-button 
  label="Annuler"
  icon="pi pi-times"
  [outlined]="true"
  severity="danger" />
```

**Props :**
- `label`: Texte du bouton
- `icon`: Classe d'icône (ex: 'pi pi-save')
- `loading`: Affiche spinner (boolean)
- `disabled`: Désactive le bouton (boolean)
- `severity`: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'danger'
- `size`: 'small' | 'large'
- `text`: Bouton texte uniquement (boolean)
- `outlined`: Bouton avec bordure (boolean)
- `rounded`: Bouton arrondi (boolean)

**Events :**
- `onClick`: Émis au clic

---

### 2. **CardComponent** (`app-card`)

Wrapper autour de PrimeNG Card.

**Usage :**
```html
<!-- Card simple -->
<app-card header="Détails Produit">
  <p>Contenu de la carte</p>
</app-card>

<!-- Card avec subheader -->
<app-card 
  header="Résumé Vente" 
  subheader="Totaux et montants"
  [customClass]="'sale-summary-card'">
  <div>
    <p>Total: 15 000 F</p>
    <p>Remise: 1 000 F</p>
  </div>
</app-card>
```

**Props :**
- `header`: Titre de la carte
- `subheader`: Sous-titre
- `customClass`: Classes CSS personnalisées
- `style`: Styles inline

---

### 3. **InputComponent** (`app-input`)

Wrapper autour de PrimeNG InputText avec two-way binding.

**Usage :**
```typescript
// Dans le composant
searchTerm = signal('');
```

```html
<!-- Input simple -->
<app-input 
  [(value)]="searchTerm"
  placeholder="Rechercher..." />

<!-- Input disabled -->
<app-input 
  [(value)]="name"
  placeholder="Nom"
  [disabled]="isLoading()"
  [required]="true" />

<!-- Input readonly -->
<app-input 
  [(value)]="reference"
  [readonly]="true" />
```

**Props :**
- `value`: Valeur (two-way binding avec model())
- `placeholder`: Texte placeholder
- `disabled`: Désactiver (boolean)
- `required`: Champ requis (boolean)
- `readonly`: Lecture seule (boolean)
- `size`: Taille en caractères (number)
- `customClass`: Classes CSS

---

### 4. **ModalComponent** (`app-modal`)

Wrapper autour de NgBootstrap Modal pour dialogs réutilisables.

**Usage :**
```typescript
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

modalService = inject(NgbModal);

openConfirmDialog(content: TemplateRef<any>) {
  this.modalService.open(content, { 
    size: 'lg', 
    centered: true,
    backdrop: 'static' 
  });
}
```

```html
<!-- Template de modal -->
<ng-template #confirmDialog let-modal>
  <div class="modal-header">
    <h4 class="modal-title">Confirmer l'action</h4>
    <button type="button" class="btn-close" (click)="modal.dismiss()"></button>
  </div>
  <div class="modal-body">
    <p>Êtes-vous sûr de vouloir continuer ?</p>
  </div>
  <div class="modal-footer">
    <button class="btn btn-secondary" (click)="modal.close('cancel')">
      Annuler
    </button>
    <button class="btn btn-primary" (click)="modal.close('confirm')">
      Confirmer
    </button>
  </div>
</ng-template>

<button (click)="openConfirmDialog(confirmDialog)">Ouvrir Dialog</button>
```

**Options NgbModal :**
- `size`: 'sm' | 'lg' | 'xl'
- `centered`: Centrer verticalement (boolean)
- `backdrop`: 'static' | true | false
- `keyboard`: Fermer avec ESC (boolean)

---

### 5. **DataTableComponent** (`app-data-table`)

Wrapper autour de PrimeNG Table avec configuration simplifiée.

**Usage :**
```typescript
products = signal<Product[]>([...]);
columns = signal([
  { field: 'name', header: 'Nom' },
  { field: 'price', header: 'Prix' },
  { field: 'stock', header: 'Stock' }
]);
```

```html
<app-data-table
  [data]="products()"
  [columns]="columns()"
  [loading]="isLoading()"
  [paginator]="true"
  [rows]="10"
  (rowSelect)="onRowSelect($event)" />
```

---

### 6. **FormFieldComponent** (`app-form-field`)

Wrapper pour champs de formulaire avec label et validation.

**Usage :**
```html
<app-form-field 
  label="Nom du produit"
  [required]="true"
  [error]="nameError()">
  <app-input [(value)]="productName" placeholder="Entrez le nom" />
</app-form-field>
```

---

## 🎯 Quand les utiliser ?

### ✅ **À UTILISER dans ces cas :**

1. **Nouvelles features** - Toutes les nouvelles fonctionnalités doivent utiliser ces composants
2. **Formulaires** - ButtonComponent + InputComponent + FormFieldComponent
3. **Listes/Tableaux** - DataTableComponent pour affichage de données
4. **Dialogs/Confirmations** - ModalComponent pour toutes les popups
5. **Cartes d'information** - CardComponent pour regrouper du contenu

### 🔄 **Migration progressive :**

Les composants existants (comme `sales/ui/*`) utilisent **directement PrimeNG** - c'est OK pour l'instant. La migration se fera progressivement.

**Exemple de migration future :**
```html
<!-- AVANT (actuel) -->
<p-button 
  label="Sauvegarder"
  [loading]="isSaving()"
  severity="success"
  (onClick)="save()" />

<!-- APRÈS (cible future) -->
<app-button 
  label="Sauvegarder"
  [loading]="isSaving()"
  severity="success"
  (onClick)="save()" />
```

---

## 🏗️ Architecture

```
app/
├── shared/
│   └── ui/                    # Design System
│       ├── button/            # ✅ Wrapper PrimeNG Button
│       ├── card/              # ✅ Wrapper PrimeNG Card
│       ├── input/             # ✅ Wrapper PrimeNG Input
│       ├── modal/             # ✅ Wrapper NgBootstrap Modal
│       ├── data-table/        # ✅ Wrapper PrimeNG Table
│       ├── form-field/        # ✅ Label + Validation
│       └── index.ts           # Barrel export
│
└── features/
    └── sales/
        └── ui/                # Composants métier Sales
            ├── product-search/     # ⚠️ Utilise PrimeNG directement
            ├── product-list/       # ⚠️ Utilise PrimeNG directement
            └── sale-summary/       # ⚠️ Utilise PrimeNG directement
```

**Principe :**
- `shared/ui` = **Composants génériques réutilisables** (Design System)
- `features/*/ui` = **Composants métier spécifiques** (domaine business)

---

## 📋 Import

```typescript
// Import depuis shared/ui
import { ButtonComponent, CardComponent, InputComponent } from '../../shared/ui';

// Dans le composant
@Component({
  imports: [ButtonComponent, CardComponent, InputComponent]
})
```

---

## 🎨 Avantages

1. **API cohérente** - Tous les composants utilisent signals + input()/output()
2. **Type-safe** - TypeScript avec types stricts
3. **OnPush** - Performance optimale avec ChangeDetection.OnPush
4. **Standalone** - Pas besoin de NgModule
5. **Wrapper léger** - Juste une couche au-dessus de PrimeNG/NgBootstrap
6. **Documentation** - Exemples d'usage intégrés

---

## 🚀 Prochaines étapes (Phase 5+)

1. **Créer plus de composants** : Dropdown, Checkbox, Radio, DatePicker
2. **Migrer progressivement** les composants existants vers shared/ui
3. **Thématique** : Ajouter support de thèmes personnalisés
4. **Storybook** : Documentation interactive des composants
5. **Tests** : Unit tests pour chaque composant

---

## 💡 Philosophie

> "Construire un Design System progressivement, pas tout refactoriser d'un coup."

Les composants shared/ui existent pour les **futures features**. Les composants existants (sales/ui) restent en PrimeNG direct - **pas de refactoring massif nécessaire**.

Migration = **opportuniste**, pas **systématique**.
