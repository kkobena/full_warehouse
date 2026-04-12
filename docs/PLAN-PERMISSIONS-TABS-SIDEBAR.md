# Plan — Permissions fines sur tabs et sidebars

**Date :** 2026-04-12
**Contexte :** Étendre le système de navigation dynamique (NavItem / NavItemRole / AbilityService) pour contrôler la visibilité des onglets internes (ngbNav sidebar) et des boutons d'action à l'intérieur des pages, en utilisant **exactement la même mécanique** que nav-manager.

---

## 1. Analyse complète du système existant

### 1.1 Flux de données — attribution de permission (actuel)

```
[Admin]
  └─ nav-manager : sélectionne un rôle
       └─ getAllNavItemsForRole(roleName)
            GET /api/admin/nav/items?roleName=ROLE_X
            └─ NavItemServiceImpl.findAllItemsForRole()
                 → tous NavItem actifs + permMap depuis nav_item_role
                 → buildAdminTree() → arbre imbriqué avec permissions

  └─ p-table affiche la liste aplatie (flattenWithDepth)
       └─ checkbox cochée/décochée → onPermissionChange(item)
            updateSinglePermission(roleName, assignment)
            POST /api/admin/nav/assign  { roleName, assignments: [1 item] }
            └─ NavItemServiceImpl.assignItemsToRole()
                 findByNavItemIdAndRoleName().orElseGet(new NavItemRole)
                 → setCanDisplay/Access/Create/.../Execute
                 → save()  ← upsert, zéro risque de doublon
```

### 1.2 Flux de données — lecture des permissions (actuel, côté utilisateur)

```
[Utilisateur connecté]
  └─ NavStore.load()
       GET /api/nav/my-items
       └─ NavItemServiceImpl.buildTreeForRoles(roles, login)
            findAllActiveByRoles(roles)             ← items où au moins un rôle a accès
            findAllByNavItemIdInAndRoleNameIn()     ← toutes les NavItemRole correspondantes
            buildPermissionsMap()                   ← merge par OR logique (union des rôles)
            buildTree()                             ← filtre sur canDisplay = true

  └─ AbilityService.setFromNavTree(tree)
       aplatit l'arbre → liste IAbility { action, subject=node.code }

  └─ AbilityService.can('display', 'commande')     ← vérification synchrone
  └─ AbilityService.canSignal('export', 'commande') ← Signal réactif
```

### 1.3 Ce qui fonctionne déjà pour n'importe quel targetType

| Mécanisme | Dépend de targetType ? | Verdict |
|-----------|----------------------|---------|
| `flattenWithDepth()` | Non — récursif sur tous les enfants | ✅ fonctionne pour SECTION |
| `onPermissionChange()` | Non — appelle updateSinglePermission | ✅ fonctionne pour SECTION |
| `assignItemsToRole()` backend | Non — générique sur navItemId | ✅ fonctionne pour SECTION |
| `buildAdminTree()` backend | Non — tous items actifs | ✅ fonctionne pour SECTION |
| `applyCollapse()` | Logique basée sur la **profondeur**, pas le type | ✅ fonctionne pour SECTION (skip depth-based couvre tous les enfants d'un GROUP replié) |
| `filterTreeForPreview()` | Oui — exclut ACTION | ⚠️ doit exclure SECTION aussi |
| `buildTree()` backend (my-items) | Non — mais filtre canDisplay | ⚠️ SECTION doit être inclus |
| `mapNodesToNavItems()` frontend | Non — mappe tout ROUTE/GROUP | ⚠️ doit exclure SECTION |
| Rendu CSS nav-manager | Oui — pas de classe `.row-section` | ⚠️ à ajouter |

### 1.4 Gaps identifiés

1. **`NavTargetType.SECTION` manquant** dans l'enum Java et dans `INavNode.targetType` TypeScript
2. **`filterTreeForPreview()`** exclut ACTION mais pas SECTION — les sections apparaîtraient dans l'aperçu navbar (incorrect)
3. **`mapNodesToNavItems()`** dans NavigationService mappe tout — les SECTION apparaîtraient dans la top navbar (incorrect)
4. **Rendu visuel nav-manager** — pas de badge "Onglet", pas de classe CSS `.row-section`
5. **`canSignalWithFallback()`** manque dans AbilityService — le fallback permissif est nécessaire pour ne pas casser les composants non encore configurés
7. **Pas de NavItem SECTION** en base pour les onglets existants (sales-gestion, facturation, etc.)

---

## 2. Architecture cible

### Hiérarchie des types de NavItem

```
GROUP (depth=0)     → entrée sidebar principale   ex: "Gestion courante"
  ROUTE (depth=1)   → page/route                  ex: "/sales-home/gestion"
    SECTION (depth=2) ← NOUVEAU → onglet sidebar  ex: "Journal des ventes"
      ACTION (depth=3) → bouton d'action          ex: "Exporter PDF"
    ACTION (depth=2)   → action directe sur la page
```

### Règle de filtrage par contexte

| Contexte | GROUP | ROUTE | SECTION | ACTION |
|----------|-------|-------|---------|--------|
| Top navbar (`mapNodesToNavItems`) | ✅ | ✅ | ❌ | ❌ |
| `/api/nav/my-items` (AbilityService) | ✅ | ✅ | ✅ | ✅ |
| Aperçu prévisualisation nav-manager | ✅ | ✅ | ❌ | ❌ |
| Matrice permissions nav-manager | ✅ | ✅ | ✅ | ✅ |
| Collapse/expand nav-manager | par GROUP | par ROUTE | (enfant) | (enfant) |

---

## 3. Étapes d'implémentation

### Étape 1 — Backend : `NavTargetType.SECTION`

**Fichier :** `NavTargetType.java`

```java
public enum NavTargetType {
    GROUP,
    ROUTE,
    SECTION,  // ← NOUVEAU : onglet / section intra-page (pas de routerLink)
    ACTION,
    DIVIDER
}
```

**Aucun autre changement backend** : `buildAdminTree()`, `assignItemsToRole()`, `buildTreeForRoles()` sont déjà génériques. Les SECTION sont retournés dans `/api/nav/my-items` car ils ont `canDisplay=true` dans `nav_item_role` — le frontend filtre selon le contexte.

---

### Étape 2 — Frontend modèle : ajouter `SECTION` à `INavNode`

**Fichier :** `src/main/webapp/app/shared/model/nav-item.model.ts`

```typescript
export interface INavNode {
  // ...
  targetType: 'ROUTE' | 'ACTION' | 'GROUP' | 'DIVIDER' | 'SECTION'; // ← SECTION ajouté
  // ...
}
```

---

### Étape 3 — `AbilityService` : `hasEntry` + `canSignalWithFallback`

**Fichier :** `ability.service.ts`

```typescript
/** Vérifie si un sujet existe dans l'arbre (au moins une ability). */
hasEntry(subject: string): boolean {
  return this.abilities().some(a => a.subject === subject);
}

/**
 * Signal réactif avec fallback permissif.
 * Si le sujet n'est pas encore configuré en base (pas de NavItemRole),
 * retourne `fallback` (défaut: true = afficher).
 * Utilisé pour les onglets/sections non encore restreints.
 */
canSignalWithFallback(action: NavAbilityAction, subject: string, fallback = true): Signal<boolean> {
  return computed(() => {
    const hasEntry = this.abilities().some(a => a.subject === subject);
    return hasEntry ? this.can(action, subject) : fallback;
  });
}
```

---

### Étape 4 — `NavigationService` : exclure SECTION de la top navbar

**Fichier :** `navigation.service.ts` — méthode `mapNodesToNavItems()`

```typescript
private mapNodesToNavItems(nodes: INavNode[]): NavItem[] {
  return nodes
    .filter(n => n.permissions?.canDisplay !== false)
    .filter(n => n.targetType !== 'SECTION' && n.targetType !== 'ACTION') // ← ajout
    .sort((a, b) => a.ordre - b.ordre)
    .map(n => ({
      label: n.libelle,
      routerLink: n.targetType === 'ROUTE' ? n.routerLink : undefined,
      faIcon: this.primeIconToFa(n.icon) as IconProp,
      children: n.children?.length ? this.mapNodesToNavItems(n.children) : undefined,
    } as NavItem));
}
```

---

### Étape 5 — `nav-manager.component.ts` : correction preview

> `applyCollapse()` **ne nécessite aucune modification** : la logique est basée sur la profondeur (`skipBelowDepth`), pas sur le type. Quand un GROUP (depth=0) est replié, tous les items avec `depth > 0` sont sautés — ROUTE, SECTION et ACTION inclus.

#### 5.1 `filterTreeForPreview()` — exclure SECTION et ACTION

```typescript
private filterTreeForPreview(
  nodes: INavNode[],
  assignments: Map<number, NavItemAssignment>
): INavNode[] {
  const result: INavNode[] = [];
  for (const node of nodes) {
    if (node.targetType === 'ACTION' || node.targetType === 'SECTION') continue; // ← SECTION exclu
    const a = assignments.get(node.id);
    if (a && !a.canDisplay) continue;
    const children = this.filterTreeForPreview(node.children ?? [], assignments);
    if (node.targetType === 'GROUP' && children.length === 0) continue;
    result.push({ ...node, children });
  }
  return result;
}
```

#### 5.3 `indentRem()` — niveau supplémentaire pour SECTION (depth=2)

```typescript
protected indentRem(depth: number): string {
  // depth 0=GROUP, 1=ROUTE, 2=SECTION, 3=ACTION dans SECTION
  const rem = depth === 0 ? 0.5 : depth * 1.5 + 0.5;
  return `${rem}rem`;
}
```

---

### Étape 6 — `nav-manager.component.html` : rendu des SECTION

#### 6.1 Classe CSS sur les lignes TR

```html
<tr
  [class.row-group]="item.targetType === 'GROUP'"
  [class.row-route]="item.targetType === 'ROUTE' && item.depth === 1"
  [class.row-subroute]="item.targetType === 'ROUTE' && item.depth > 1"
  [class.row-section]="item.targetType === 'SECTION'"   <!-- ← NOUVEAU -->
  [class.row-action]="item.targetType === 'ACTION'"
>
```

#### 6.2 Toggle collapse sur les lignes ROUTE (comme GROUP)

```html
@if (item.targetType === 'GROUP' || item.targetType === 'ROUTE') {
  <button class="group-toggle" type="button" (click)="toggleGroup(item.id)">
    <i class="pi" [class.pi-chevron-down]="!collapsedGroups().has(item.id)"
                  [class.pi-chevron-right]="collapsedGroups().has(item.id)"></i>
  </button>
}
```

#### 6.3 Icône et badge SECTION

```html
@if (item.targetType === 'SECTION') {
  <span class="tree-branch"></span>
  <i class="pi pi-window-maximize section-icon"></i>
}
<!-- badge -->
@if (item.targetType === 'SECTION') {
  <span class="section-badge">Onglet</span>
}
```

#### 6.4 Colonnes de permissions pour SECTION

Les SECTION utilisent `canDisplay` (visible/caché) et `canAccess` (accessible).  
`canCreate`, `canEdit`, `canDelete`, `canExport` sont N/A pour SECTION (sauf si l'onglet lui-même a des actions).  
`canExecute` = N/A.

```html
<!-- Afficher -->
<td class="text-center perm-cell" [class.cell-na]="item.targetType === 'ACTION'">
  @if (item.targetType !== 'ACTION') {
    <p-checkbox [(ngModel)]="item.assignment.canDisplay" [binary]="true"
                (onChange)="onPermissionChange(item)" />
  } @else {
    <span class="na-dash">—</span>
  }
</td>
<!-- Les colonnes Create/Edit/Delete/Export : na si GROUP ou SECTION sans enfants ACTION -->
```

> Les checkboxes existantes fonctionnent sans modification — `onPermissionChange()` est générique.

---

### Étape 7 — `nav-manager.component.scss` : style `.row-section`

```scss
::ng-deep .row-section {
  background: #f5f3ff !important;
  border-left: 4px solid #a78bfa !important;

  td { color: #5b21b6; font-size: 0.82rem; }
}

.section-icon {
  color: #7c3aed;
  font-size: 0.75rem;
  margin-right: 4px;
}

.section-badge {
  display: inline-flex;
  align-items: center;
  margin-left: 6px;
  font-size: 0.6rem;
  font-weight: 700;
  color: #5b21b6;
  background: #ede9fe;
  border: 1px solid #c4b5fd;
  border-radius: 4px;
  padding: 1px 5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
```

---

### Étape 8 — Directive structurelle `*appCan`

**Nouveau fichier :** `src/main/webapp/app/shared/directives/can.directive.ts`

```typescript
import { computed, Directive, effect, inject, input, TemplateRef, ViewContainerRef } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavAbilityAction } from 'app/shared/model/nav-item.model';

/**
 * Directive structurelle — affiche l'élément si l'utilisateur a la permission.
 *
 * Usage :
 *   <ng-container *appCan="'display'; on: 'sales-gestion.journal'">
 *   <p-button *appCan="'export'; on: 'sales-gestion.journal.export'" />
 *
 * Si le code n'est pas encore configuré en base, [fallback] contrôle le comportement
 * (défaut: true = afficher — comportement permissif, zéro régression).
 */
@Directive({ selector: '[appCan]', standalone: true })
export class CanDirective {
  private readonly ability = inject(AbilityService);
  private readonly tpl     = inject(TemplateRef<any>);
  private readonly vc      = inject(ViewContainerRef);

  readonly appCan         = input.required<NavAbilityAction>();
  readonly appCanOn       = input.required<string>();
  readonly appCanFallback = input<boolean>(true);

  constructor() {
    effect(() => {
      const hasEntry = this.ability.hasEntry(this.appCanOn());
      const allowed  = hasEntry
        ? this.ability.can(this.appCan(), this.appCanOn())
        : this.appCanFallback();

      this.vc.clear();
      if (allowed) this.vc.createEmbeddedView(this.tpl);
    });
  }
}
```

---

### Étape 9 — Wiring dans les composants (pilote : sales-management-home)

**`sales-management-home.component.ts`**

```typescript
import { AbilityService } from 'app/core/auth/ability.service';
import { CanDirective } from 'app/shared/directives/can.directive';

@Component({ imports: [..., CanDirective] })
export class SalesManagementHomeComponent {
  private readonly ability = inject(AbilityService);

  // Chaque signal utilise le fallback permissif → zéro régression si non configuré
  protected readonly showJournal  = this.ability.canSignalWithFallback('display', 'sales-gestion.journal');
  protected readonly showEnCours  = this.ability.canSignalWithFallback('display', 'sales-gestion.en-cours');
  protected readonly showPresales = this.ability.canSignalWithFallback('display', 'sales-gestion.presales');
  protected readonly showDevis    = this.ability.canSignalWithFallback('display', 'sales-gestion.devis');
}
```

**`sales-management-home.component.html`**

```html
@if (showJournal()) {
  <ng-container ngbNavItem="journal">
    <a class="pharma-nav-vertical-link" ngbNavLink>
      <i class="pi pi-book"></i>
      <span>Journal des ventes</span>
      <span class="link-arrow">›</span>
    </a>
    <ng-template ngbNavContent>
      <app-sales-journal />
    </ng-template>
  </ng-container>
}

@if (showEnCours()) {
  <ng-container ngbNavItem="en-cours">...</ng-container>
}
@if (showPresales()) {
  <ng-container ngbNavItem="presales">...</ng-container>
}
@if (showDevis()) {
  <ng-container ngbNavItem="devis">...</ng-container>
}
```

Ou version avec directive structurelle :

```html
<ng-container *appCan="'display'; on: 'sales-gestion.journal'" ngbNavItem="journal">
  ...
</ng-container>
```

> **Note :** `*appCan` et `ngbNavItem` sur le même élément peuvent créer un conflit de directives structurelles. Préférer le wrapper `@if (showXxx())`.

---

### Étape 10 — Migration Flyway

**Fichier :** `V1.4.8__nav_sections.sql`

```sql
-- ── Sections de sales-gestion ──────────────────────────────────────────────
DO $$
DECLARE
  p_id INT;
  j_id INT;
BEGIN
  SELECT id INTO p_id FROM warehouse.nav_item WHERE code = 'sales-gestion';

  INSERT INTO warehouse.nav_item (code, libelle, icon, router_link, target_type, niveau, ordre, actif, parent_id, created, updated)
  VALUES
    ('sales-gestion.journal',   'Journal des ventes', 'pi pi-book',      NULL, 'SECTION', 3, 10, true, p_id, NOW(), NOW()),
    ('sales-gestion.en-cours',  'Ventes en cours',    'pi pi-clock',     NULL, 'SECTION', 3, 20, true, p_id, NOW(), NOW()),
    ('sales-gestion.presales',  'Pré-ventes',         'pi pi-bookmark',  NULL, 'SECTION', 3, 30, true, p_id, NOW(), NOW()),
    ('sales-gestion.devis',     'Proformas',          'pi pi-file-edit', NULL, 'SECTION', 3, 40, true, p_id, NOW(), NOW())
  ON CONFLICT (code) DO NOTHING;

  -- Action dans journal
  SELECT id INTO j_id FROM warehouse.nav_item WHERE code = 'sales-gestion.journal';
  INSERT INTO warehouse.nav_item (code, libelle, icon, router_link, target_type, niveau, ordre, actif, parent_id, created, updated)
  VALUES
    ('sales-gestion.journal.export', 'Exporter PDF', 'pi pi-file-pdf', NULL, 'ACTION', 4, 10, true, j_id, NOW(), NOW())
  ON CONFLICT (code) DO NOTHING;
END $$;

-- Convention de nommage :
--   <route-code>.<section-id>               pour les onglets
--   <route-code>.<section-id>.<action-id>   pour les actions dans un onglet
```

---

## 4. Résumé des fichiers à créer / modifier

### Nouveaux fichiers
| Fichier | Contenu |
|---------|---------|
| `shared/directives/can.directive.ts` | Directive `*appCan` structurelle |
| `db/migration/V1.4.8__nav_sections.sql` | NavItems SECTION pour sales-gestion |

### Fichiers modifiés
| Fichier | Modification |
|---------|-------------|
| `NavTargetType.java` | + `SECTION` |
| `nav-item.model.ts` | targetType + `'SECTION'` |
| `ability.service.ts` | + `hasEntry()` + `canSignalWithFallback()` |
| `navigation.service.ts` | `mapNodesToNavItems()` filtre SECTION/ACTION |
| `nav-manager.component.ts` | `applyCollapse()` sur ROUTE + `filterTreeForPreview()` exclut SECTION |
| `nav-manager.component.html` | CSS row-section, toggle sur ROUTE, badge "Onglet" |
| `nav-manager.component.scss` | `.row-section`, `.section-badge`, `.section-icon` |
| `sales-management-home.component.ts/.html` | Pilote d'intégration (premier composant migré) |

---

## 5. Points de décision confirmés

| Question | Décision | Raison |
|----------|----------|--------|
| Fallback non configuré | Afficher (`true`) | Zéro régression sur composants existants |
| ROLE_ADMIN | Bypass via `buildTree()` — retourne tout (déjà le cas) | Admin ne peut pas se verrouiller |
| Nommage des codes | `route-code.section-id` | Lisible par l'admin dans nav-manager |
| `*appCan` vs `@if (signal())` | `@if (signal())` pour les ngbNavItem | Évite le conflit de directives structurelles |
| SECTION dans `/api/nav/my-items` | Oui — inclus, même traitement que ROUTE | AbilityService doit les connaître |
| SECTION dans top navbar | Non — filtré dans `mapNodesToNavItems()` | Pas de lien de navigation |
| Collapse dans nav-manager | GROUP + ROUTE | Les ROUTE ont des enfants SECTION |

---

## 6. Ordre d'implémentation recommandé

```
1. NavTargetType.SECTION (Java)         → 5 min
2. INavNode.targetType (TypeScript)     → 2 min
3. AbilityService extensions            → 15 min
4. NavigationService filtre SECTION     → 5 min
5. nav-manager.component corrections    → 30 min
6. CanDirective                         → 20 min
7. V1.4.9 migration Flyway              → 20 min
8. sales-management-home (pilote)       → 20 min
9. Autres composants (commande, fact.)  → par itération
```
C:\Users\k.kobena\Documents\dev\full_warehouse\src\main\webapp\app\core\auth\ability-route.guard.ts

🔴 P0    │ Bug fallback hasEntry (point 1)            │ Sécurité incorrecte            │
├──────────┼────────────────────────────────────────────┼────────────────────────────────┤
│ 🔴 P0    │ Câbler les boutons ACTION (point 3)        │ Plan inutile sans ça           │
├──────────┼────────────────────────────────────────────┼────────────────────────────────┤
│ 🟠 P1    │ Performance Map O(1) (point 2)             │ Montée en charge               │
├──────────┼────────────────────────────────────────────┼────────────────────────────────┤
│ 🟠 P1    │ Héritage parent→enfant (point 4)           │ Cohérence métier               │
├──────────┼────────────────────────────────────────────┼────────────────────────────────┤
│ 🟡 P2    │ AbilityRouteGuard sur les routes (point 5) │ Sécurité défense en profondeur │
├──────────┼────────────────────────────────────────────┼────────────────────────────────┤
│ 🟡 P2    │ Refresh en temps réel (point 6)            │ UX     
