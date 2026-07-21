# Design System PharmaSmart (`app/shared/ui`)

Composants réutilisables rendant du **Bootstrap 5 natif**, sans PrimeNG.

Contexte : [docs/PLAN-MIGRATION-PRIMENG-VERS-NGBOOTSTRAP.md](../../../../../../docs/PLAN-MIGRATION-PRIMENG-VERS-NGBOOTSTRAP.md).

---

## Pourquoi le rendu ne change pas

La palette Bootstrap est alignée sur celle du preset Aura de PrimeNG
(`content/scss/_pharma-bootstrap-palette.scss`). `.btn-primary` sort donc en émeraude
`#10b981`, la couleur actuelle de `<p-button>`, et non le bleu-canard de Bootswatch yeti.

C'est ce qui permet de migrer **écran par écran** sans que l'application devienne bicolore.

Les severities PrimeNG absentes de Bootstrap (`help`, `contrast`) sont ajoutées à
`$theme-colors`, donc `.btn-help`, `.btn-outline-help`, `.text-help`… existent comme les autres.

---

## Mémo de substitution

| PrimeNG | Design System | Notes |
|---|---|---|
| `<p-button label="X" (onClick)="f()">` | `<app-button label="X" (clicked)="f()">` | l'event est renommé `clicked` |
| `<p-tag value="X" severity="s">` | `<app-badge label="X" severity="s">` | |
| `<p-badge value="X">` | `<app-badge label="X">` | |
| `<p-chip label="X" [removable]="true">` | `<app-badge label="X" [rounded]="true" [dismissible]="true">` | |
| `<p-checkbox [(ngModel)]="v">` | `<app-checkbox [(ngModel)]="v">` | |
| `<p-radiobutton name="g" [value]="v">` | `<app-radio name="g" [value]="v">` | `name` est requis |
| `<p-toggleswitch [(ngModel)]="v">` | `<app-switch [(ngModel)]="v">` | |
| `<input pInputText>` | `<app-input>` | |
| `<p-inputnumber [(ngModel)]="v">` | `<app-input-number [(ngModel)]="v">` | formatage FR |
| `<p-password [(ngModel)]="v">` | `<app-password [(ngModel)]="v">` | |
| `<p-skeleton width="4rem">` | `<app-skeleton width="4rem">` | |
| `<p-select>` (sans recherche) | `<app-select>` | wrappe `ng-select` en `[searchable]="false"`. Un `<select>` natif a été essayé puis abandonné : ses `<option>` sont dessinées par l'OS, surbrillance système impossible à aligner sur la charte |
| `<p-select>` (recherche, lazy, ≥ 20 options) | `<app-select-search>` | wrappe `ng-select` |
| `<p-autocomplete>` | `<app-select-search>` | idem |
| `<p-multiselect>` | `<app-multi-select>` | idem, en multiple |
| `<p-splitbutton [model]="items">` | `<app-split-button [items]="items">` | `NgbDropdown` + `.btn-group` |
| `<p-selectbutton>` | `<app-pill-selector [items]="opts">` | style `.dashboard-periode-selector` du tableau de bord |
| `<p-fileupload>` | `<app-file-upload>` | choix des fichiers uniquement, l'envoi reste à l'appelant |
| `<p-table>` | `<app-data-table>` | + `[appSortableHeader]`, `[appSelectableRow]`, `[appRowToggler]` |
| `pKeyFilter="alphanum"` | `appKeyFilter="alphanum"` | presets `alpha` / `alphanum` / `int` / `pint`, ou une `RegExp` |
| `<p-toast />` | **rien** — supprimer la balise | un `<app-toast-host />` unique est monté dans le layout racine |
| `MessageService` (injection directe) | `NotificationService` | API `success` / `info` / `warning` / `error` |
| `<p-floatlabel>` | `<app-float-label label="X" [inputId]="'id'">` | le champ projeté doit porter le même `id` **et** un `placeholder` |
| `<p-iconfield><p-inputicon>` | `<app-icon-field icon="pi pi-search" [overlay]="true">` | `overlay` superpose l'icône au champ comme le faisait PrimeNG ; sans lui, elle occupe une case `.input-group` à côté |
| `<p-card>` | `<div class="card data-card">` | pas de composant : Bootstrap natif suffit |
| `<p-divider>` | `<hr class="my-3">` | idem |
| `<p-toolbar>` | `<div class="btn-toolbar">` | idem |

**Severities** — le vocabulaire PrimeNG est conservé (`primary`, `secondary`, `success`,
`info`, `warn`, `danger`, `help`, `contrast`). Noter `warn`, pas `warning` : le composant
le mappe sur `.btn-warning` en interne.

---

## Import

Tout passe par le barrel :

```ts
import { ButtonComponent, BadgeComponent, InputComponent } from 'app/shared/ui';

@Component({
  imports: [ButtonComponent, BadgeComponent, InputComponent],
  // …
})
```

---

## Exemples

```html
<!-- Boutons -->
<app-button label="Enregistrer" icon="pi pi-save" (clicked)="save()" />
<app-button label="Supprimer" severity="danger" [outlined]="true" (clicked)="remove()" />
<app-button icon="pi pi-trash" [iconOnly]="true" severity="danger" ariaLabel="Supprimer" />
<app-button label="Annuler" severity="secondary" [text]="true" />
<app-button label="Valider" [loading]="saving()" [disabled]="form.invalid" type="submit" />

<!-- Badges -->
<app-badge label="Actif" severity="success" />
<app-badge label="Périmé" severity="danger" icon="pi pi-exclamation-triangle" />
<app-badge label="Filtre : Paracétamol" [rounded]="true" [dismissible]="true" (dismissed)="clear()" />

<!-- Formulaire -->
<app-form-field label="Nom du produit" [required]="true" [error]="nomErreur()">
  <app-input placeholder="Ex. Paracétamol 500mg" [(ngModel)]="nom" />
</app-form-field>

<app-input-number [(ngModel)]="prix" [min]="0" [maxFractionDigits]="2" suffix=" F CFA" />
<app-checkbox label="Produit remboursable" [(ngModel)]="remboursable" />
<app-radio name="paiement" [value]="'especes'" label="Espèces" [(ngModel)]="mode" />
<app-switch label="Alerte de péremption" [(ngModel)]="alerte" />

<app-icon-field icon="pi pi-search" [overlay]="true">
  <input class="form-control" placeholder="Rechercher" [(ngModel)]="recherche" />
</app-icon-field>

<!-- Liste déroulante simple, sans recherche -->
<app-select [items]="categories" bindLabel="libelle" bindValue="id" placeholder="Catégorie" formControlName="categorieId" />

<!-- Listes déroulantes riches -->
<app-select-search [items]="produits()" bindLabel="libelle" bindValue="id" [(ngModel)]="produitId" />
<app-multi-select [items]="rayons()" bindLabel="nom" bindValue="id" [(ngModel)]="rayonIds" />

<!-- Dans une modale ou un conteneur overflow:hidden, sans quoi le panneau est rogné -->
<app-select-search [items]="produits()" appendTo="body" [(ngModel)]="produitId" />

<!-- Recherche côté serveur -->
<app-select-search [items]="resultats()" [typeahead]="recherche$" [loading]="chargement()" (searched)="chercher($event)" />

<!-- Bouton scindé -->
<app-split-button
  label="Enregistrer"
  icon="pi pi-save"
  [items]="[{ label: 'Enregistrer et fermer', icon: 'pi pi-check', command: saveAndClose }]"
  (clicked)="save()" />

<!-- Import de fichier -->
<app-file-upload accept=".csv,.xlsx" [maxSizeMb]="5" (filesSelected)="importer($event)" (rejected)="notif.error($event)" />

<!-- Chargement -->
@if (loading()) { <app-skeleton width="12rem" height="1.5rem" /> }
```

---

## Conventions (plan §5)

Toute contribution au Design System doit respecter :

- `input()` / `output()` — jamais les décorateurs `@Input` / `@Output`
- **pas** de `standalone: true` ni de `changeDetection` — ce sont les défauts depuis Angular 20/22
- `host: { … }` — jamais `@HostBinding` / `@HostListener`
- `[class.x]` / `[style.x]` — jamais `ngClass` / `ngStyle`
- `signal()` pour l'état local, `computed()` pour l'état dérivé
- template inline sous ~60 lignes, fichier séparé au-delà
- les composants de formulaire étendent `ControlValueAccessorBase` (`forms/control-value-accessor.base.ts`)

⚠ **Backticks** : à l'intérieur d'un bloc `template:` ou `styles:`, un backtick non échappé
ferme le littéral. Ne pas écrire de `` `code` `` dans les commentaires SCSS inline — l'erreur
n'apparaît qu'à la compilation du composant, donc pas au build tant que rien ne l'importe.

---

## État

| Composant | Statut |
|---|---|
| `app-button` | ✅ Bootstrap natif |
| `app-badge` | ✅ tokens Aura |
| `app-checkbox` / `app-radio` / `app-switch` | ✅ `.form-check` Bootstrap |
| `app-input` / `app-input-number` / `app-password` | ✅ `.form-control` Bootstrap |
| `app-form-field` / `app-float-label` / `app-icon-field` | ✅ Bootstrap natif |
| `app-skeleton` | ✅ `.placeholder` Bootstrap |
| `app-select-search` / `app-multi-select` | ✅ wrappers `ng-select` |
| `app-split-button` | ✅ `NgbDropdown` |
| `app-file-upload` | ✅ `<input type="file">` |
| `app-data-table` | ✅ Bootstrap + `NgbPagination` |
| `app-toast-host` | ✅ `NgbToast` |
| `app-card` | ⚠ wrappe encore PrimeNG — à supprimer (Bootstrap natif suffit) |
| `app-modal` | ⚠ à ajuster (`NgbModal`) |

## Notifications

`NotificationService` a gardé son API ; seule son implémentation a changé (signal interne
au lieu de `MessageService`). Les 514 appels existants n'ont pas bougé.

Un `<app-toast-host />` unique est monté dans `layouts/main/main.component.html`. Il
remplace les `<p-toast>` que chaque écran déclarait.

**Reste à nettoyer** (mécanique, sans risque — ces éléments ne rendent plus rien) :

- **73 fichiers** portent encore une balise `<p-toast>` devenue inerte : supprimer la
  balise et l'import `ToastModule`
- **13 composants** injectent `MessageService` directement au lieu de passer par
  `NotificationService` : remplacer `messageService.add({severity, summary, detail})`
  par `notificationService.<severity>(detail, summary)`

Tant que ces deux points subsistent, le provider `MessageService` doit rester dans
`app.config.ts` — `p-toast` l'injecte et planterait sans lui.

## Confirmations

Rien à créer : `NgbConfirmDialogService` (`shared/dialog/ngb-confirm-dialog/`) existe déjà
et sert dans **70 fichiers**. Le `ConfirmationService` de PrimeNG n'a **aucun appelant** —
son provider dans `app.config.ts` est mort et peut partir en Phase 4.

```ts
readonly confirmDialog = inject(NgbConfirmDialogService);
this.confirmDialog.onConfirm(() => this.supprimer(), 'Suppression', 'Confirmer ?');
```
