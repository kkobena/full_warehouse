# Plan — Gestion dynamique des menus & contrôle d'accès automatisé

> **Pharma-Smart** — Angular 20 / Spring Boot 4
> Date : 2026-04-06 — **Mise à jour : 2026-04-06**
> Auteur : GitHub Copilot
> Périmètre : Remplacement du menu statique hardcodé par une architecture dynamique, orientée
> données, basée sur les patterns Spring ACL, CASL.js, JHipster RBAC et les standards SAP
> Fiori / Odoo / Keycloak.
>
> ⚠️ **Décision 2026-04-06 :** L'entité `Menu` existante est conservée **telle quelle** (utilisée
> pour l'attribution de privilèges aux rôles). Le nouveau système de navigation repose sur un
> **modèle indépendant** : `nav_item` + `nav_permission` + `nav_item_role`.

---

## Table des matières

1. [Diagnostic — État actuel et limites](#1-diagnostic--état-actuel-et-limites)
2. [Référentiels — Standards analysés](#2-référentiels--standards-analysés)
3. [Architecture cible — Vue d'ensemble](#3-architecture-cible--vue-densemble)
4. [Nouveau modèle de données indépendant](#4-nouveau-modèle-de-données-indépendant)
5. [Backend — Plan d'implémentation](#5-backend--plan-dimplémentation)
6. [Frontend — Plan d'implémentation](#6-frontend--plan-dimplémentation)
7. [Réorganisation dynamique des menus (Drag & Drop)](#7-réorganisation-dynamique-des-menus-drag--drop)
8. [Roadmap par phases](#8-roadmap-par-phases)
9. [Fichiers créés / modifiés](#9-fichiers-créés--modifiés)
10. [Estimation des efforts](#10-estimation-des-efforts)

---

## 1. Diagnostic — État actuel et limites

### 1.1 Ce qui existe

| Composant | Localisation | Rôle |
|---|---|---|
| `NavigationService` | `core/config/navigation.service.ts` | Construit le menu navbar **en dur** (416 lignes) |
| `NavItem` | `layouts/navbar/navbar-item.model.ts` | Modèle frontend uniquement, non persisté |
| `Menu` (JPA) | `domain/Menu.java` | Entité DB : `name`, `libelle`, `root`, `parent`, `enable`, `ordre` — **conservée, non modifiée** |
| `AuthorityPrivilege` (JPA) | `domain/AuthorityPrivilege.java` | Table de jointure `privilege ↔ authority` — **conservée** |
| `MenuDetailComponent` | `entities/menu/menu-detail.component.*` | UI d'attribution des privilèges aux rôles — **conservée** |
| `UserRouteAccessService` | `core/auth/user-route-access.service.ts` | `canActivate` : vérifie `data.authorities` |
| `Authority` enum | `shared/constants/authority.constants.ts` | 60+ constantes hardcodées |

### 1.2 Pourquoi ne PAS modifier l'entité `Menu` existante

```
L'entité Menu est déjà utilisée pour :
  ✅ L'attribution de privilèges aux rôles (MenuDetailComponent)
  ✅ La table privilege / authority_privilege (jointure)
  ✅ Données de référence insérées en V1.0.3__menus.sql

Modifier Menu = risque de casser le système de sécurité existant.
→ Décision : créer un modèle PARALLÈLE et INDÉPENDANT dédié à la navigation.
```

### 1.3 Problèmes identifiés

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ PROBLÈME 1 — Menu 100% statique                                             │
│  NavigationService hardcode chaque entrée de menu, chaque autorité,         │
│  chaque routerLink. Ajouter un item = modifier le code source.              │
├─────────────────────────────────────────────────────────────────────────────┤
│ PROBLÈME 2 — Aucune réorganisation possible                                 │
│  L'ordre des menus est figé dans le code. Impossible de déplacer            │
│  un sous-menu ou un onglet sans redéploiement.                              │
├─────────────────────────────────────────────────────────────────────────────┤
│ PROBLÈME 3 — Granularité insuffisante (RBAC only)                           │
│  Seules les routes sont protégées. Aucun contrôle fin sur les boutons,      │
│  colonnes, onglets, actions contextuelles (ABAC manquant).                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ PROBLÈME 4 — Cache inexistant côté frontend                                 │
│  Même si le menu était dynamique, chaque navigation rechargerait les droits  │
│  depuis le serveur.                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Référentiels — Standards analysés

### 2.1 Spring Security ACL

Spring ACL modélise les droits sur des **objets** (Object Identity) avec des **permissions** (READ,
WRITE, CREATE, DELETE, ADMIN) et un **principal** (utilisateur ou rôle).

**Leçon adoptée pour Pharma-Smart :**
- Chaque `NavItem` = une ressource protégée
- Chaque `NavPermission` = une permission (DISPLAY, ACCESS, CREATE, EDIT, DELETE, EXPORT)
- Un rôle (`Authority`) est relié à N `NavItem` via `NavItemRole`

### 2.2 Keycloak / OIDC Resource Access

**Leçon adoptée :** Inclure la liste des `navItem.code` accessibles dans `/api/account` pour
éviter un appel supplémentaire à chaque chargement.

### 2.3 CASL.js (Angular)

**Leçon adoptée :** `AbilityService` Angular : `can('action', 'subject')` évalué depuis un
signal réactif alimenté par le retour de `/api/account`.

### 2.4 SAP Fiori / Odoo / Oracle OEBS

| Standard | Pattern menu dynamique |
|---|---|
| **SAP Fiori** | Tiles depuis catalogue de rôles (FLP). Drag & drop dans l'espace perso. |
| **Odoo** | Menus configurables par groupe ; ordre modifiable depuis l'interface admin |
| **Oracle OEBS** | Responsabilités → fonctions → menus (arbre 3 niveaux + drag) |
| **Winpharma** | Menu configurable par profil |

**Leçon adoptée :**
- Niveau 1 (root) = groupe fonctionnel
- Niveau 2 = fonctionnalité
- Chaque item porte un `ordre` modifiable via API

---

## 3. Architecture cible — Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  BACKEND                                                                                │
│                                                                                         │
│  Tables EXISTANTES (inchangées)          Nouvelles tables (navigation)                 │
│  menu ──── privilege ─── authority       nav_item ──── nav_item_role ─── authority     │
│                                               │                                         │
│                                          nav_permission (action: DISPLAY/ACCESS/...)    │
│                                                                                         │
│  API:  GET /api/nav/my-items      → arbre NavItem filtré pour l'utilisateur courant    │
│        PUT /api/nav/reorder       → réorganisation (drag & drop)                       │
│        GET /api/admin/nav/items   → tous les items (admin)                             │
│        POST /api/admin/nav/items  → créer/modifier un item                             │
│        PUT /api/admin/nav/assign  → assigner item[] à un rôle                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                          │
                    HTTP / Signal
                          │
┌─────────────────────────▼───────────────────────────────────────────────────────────────┐
│  FRONTEND ANGULAR                                                                       │
│                                                                                         │
│  NavApiService ──► NavStore (Signal) ──► NavigationService ──► NavbarComponent         │
│                          │                                                              │
│                   AbilityService (CASL-like)                                            │
│                          │                                                              │
│          ┌───────────────┼────────────────────┐                                        │
│          │               │                    │                                         │
│   *appHasAbility    hasAbility pipe    AbilityRouteGuard                               │
│   (directive)       (template)         (canActivate)                                   │
│                                                                                         │
│  Admin : NavManagerComponent                                                            │
│    ├── Arbre drag & drop (PrimeNG p-tree + cdkDragDrop)                                │
│    ├── Panneau permissions fines par rôle                                              │
│    └── Prévisualisation navbar pour un rôle donné                                      │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Nouveau modèle de données indépendant

> ⚠️ L'entité `Menu` existante **n'est pas modifiée**. Tout ce qui suit est dans de **nouvelles
> tables** préfixées `nav_`.

### 4.1 Schéma SQL — Migration `V1.4.7__nav_dynamic_menu.sql`

```sql
-- ============================================================
-- V1.4.7 — Nouveau modèle de navigation dynamique (nav_*)
-- Indépendant de la table menu existante
-- ============================================================

-- Table principale : items de navigation
CREATE TABLE nav_item (
    id           INTEGER      GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code         VARCHAR(60)  NOT NULL UNIQUE,      -- clé métier ex: "commande", "facturation"
    libelle      VARCHAR(100) NOT NULL,             -- label affiché
    icon         VARCHAR(80),                       -- "pi pi-shopping-bag" ou FA key
    router_link  VARCHAR(150),                      -- route Angular ex: "/commande"
    parent_id    INTEGER      REFERENCES nav_item(id) ON DELETE CASCADE,
    ordre        INTEGER      NOT NULL DEFAULT 0,   -- tri dans le parent
    niveau       SMALLINT     NOT NULL DEFAULT 1,   -- 1=root, 2=enfant, 3=sous-enfant
    badge_type   VARCHAR(20)  DEFAULT 'NONE',       -- NONE|RUPTURE|PEREMPTION|URGENT
    target_type  VARCHAR(20)  DEFAULT 'ROUTE',      -- ROUTE|ACTION|GROUP|DIVIDER
    actif        BOOLEAN      NOT NULL DEFAULT TRUE,
    created      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Permissions fines sur chaque item
CREATE TABLE nav_permission (
    id          INTEGER     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nav_item_id INTEGER     NOT NULL REFERENCES nav_item(id) ON DELETE CASCADE,
    action      VARCHAR(20) NOT NULL,               -- DISPLAY|ACCESS|CREATE|EDIT|DELETE|EXPORT
    UNIQUE (nav_item_id, action)
);

-- Association item ↔ rôle (avec permissions)
CREATE TABLE nav_item_role (
    id              INTEGER     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nav_item_id     INTEGER     NOT NULL REFERENCES nav_item(id) ON DELETE CASCADE,
    role_name       VARCHAR(60) NOT NULL,            -- = Authority.name ex: "ROLE_ADMIN"
    can_display     BOOLEAN     NOT NULL DEFAULT TRUE,
    can_access      BOOLEAN     NOT NULL DEFAULT TRUE,
    can_create      BOOLEAN     NOT NULL DEFAULT FALSE,
    can_edit        BOOLEAN     NOT NULL DEFAULT FALSE,
    can_delete      BOOLEAN     NOT NULL DEFAULT FALSE,
    can_export      BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (nav_item_id, role_name)
);

-- Préférences de réorganisation par utilisateur (drag & drop perso)
CREATE TABLE nav_item_user_order (
    id          INTEGER     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_login  VARCHAR(50) NOT NULL,
    nav_item_id INTEGER     NOT NULL REFERENCES nav_item(id) ON DELETE CASCADE,
    ordre       INTEGER     NOT NULL DEFAULT 0,
    UNIQUE (user_login, nav_item_id)
);

-- Index
CREATE INDEX idx_nav_item_parent   ON nav_item(parent_id);
CREATE INDEX idx_nav_item_role_role ON nav_item_role(role_name);
CREATE INDEX idx_nav_perm_item     ON nav_permission(nav_item_id);
CREATE INDEX idx_nav_user_order    ON nav_item_user_order(user_login);

-- ─── Données initiales ────────────────────────────────────────────────────
-- Niveau 1 : groupes fonctionnels
INSERT INTO nav_item (code, libelle, icon, router_link, ordre, niveau, target_type) VALUES
  ('nouvelle-vente',      'Nouvelle Vente',         'pi pi-shopping-bag',  '/sales-home',         0,  1, 'ROUTE'),
  ('gestion-courante',    'Gestion Courante',        'pi pi-list',          NULL,                  1,  1, 'GROUP'),
  ('gestion-stock',       'Gestion Stock',           'pi pi-truck',         NULL,                  2,  1, 'GROUP'),
  ('facturation',         'Facturation',             'pi pi-wallet',        NULL,                  3,  1, 'GROUP'),
  ('referentiel',         'Référentiel',             'pi pi-book',          NULL,                  4,  1, 'GROUP'),
  ('rapports',            'Rapports & Statistiques', 'pi pi-chart-bar',     NULL,                  5,  1, 'GROUP'),
  ('administration',      'Administration',          'pi pi-cog',           NULL,                  6,  1, 'GROUP');

-- Niveau 2 : enfants de gestion-courante
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type)
  SELECT 'ventes',        'Ventes',             'pi pi-shopping-bag', '/sales-home/gestion', id, 0, 2, 'ROUTE' FROM nav_item WHERE code='gestion-courante';
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type)
  SELECT 'mvt-caisse',    'Mouvements Caisse',  'pi pi-coins',        '/mvt-caisse',         id, 1, 2, 'ROUTE' FROM nav_item WHERE code='gestion-courante';

-- Niveau 2 : enfants de gestion-stock
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type)
  SELECT 'catalogue',     'Catalogue produits', 'pi pi-box',          '/produits',           id, 0, 2, 'ROUTE' FROM nav_item WHERE code='gestion-stock';
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type)
  SELECT 'commande',      'Commandes',          'pi pi-send',         '/commande',           id, 1, 2, 'ROUTE' FROM nav_item WHERE code='gestion-stock';
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type)
  SELECT 'peremptions',   'Péremptions',        'pi pi-calendar-times','/gestion-peremption', id, 2, 2, 'ROUTE' FROM nav_item WHERE code='gestion-stock';
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type)
  SELECT 'ajustements',   'Ajustements',        'pi pi-sliders-h',    '/features-ajustement',id, 3, 2, 'ROUTE' FROM nav_item WHERE code='gestion-stock';
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type)
  SELECT 'inventaire',    'Inventaire',         'pi pi-clipboard',    '/inventaire',         id, 4, 2, 'ROUTE' FROM nav_item WHERE code='gestion-stock';

-- Assignation par défaut ROLE_ADMIN (accès tout)
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export)
  SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE FROM nav_item;
```

### 4.2 Entités JPA — package `domain/nav`

```java
// NavItem.java
@Entity @Table(name = "nav_item")
public class NavItem implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull @Column(unique = true)
    private String code;          // clé métier unique

    @NotNull
    private String libelle;
    private String icon;
    private String routerLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private NavItem parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<NavItem> children = new ArrayList<>();

    private int ordre;
    private int niveau;

    @Enumerated(EnumType.STRING)
    private NavBadgeType badgeType = NavBadgeType.NONE;

    @Enumerated(EnumType.STRING)
    private NavTargetType targetType = NavTargetType.ROUTE;

    private boolean actif = true;
}

// NavItemRole.java
@Entity @Table(name = "nav_item_role",
    uniqueConstraints = @UniqueConstraint(columnNames = {"nav_item_id","role_name"}))
public class NavItemRole implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "nav_item_id")
    private NavItem navItem;

    @NotNull
    private String roleName;

    private boolean canDisplay = true;
    private boolean canAccess  = true;
    private boolean canCreate  = false;
    private boolean canEdit    = false;
    private boolean canDelete  = false;
    private boolean canExport  = false;
}

// NavItemUserOrder.java — préférences perso de tri
@Entity @Table(name = "nav_item_user_order",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_login","nav_item_id"}))
public class NavItemUserOrder implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull private String userLogin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "nav_item_id")
    private NavItem navItem;

    private int ordre;
}

// Enums
public enum NavBadgeType  { NONE, RUPTURE, PEREMPTION, URGENT }
public enum NavTargetType { ROUTE, ACTION, GROUP, DIVIDER }
```

### 4.3 DTO Frontend

```typescript
// shared/model/nav-item.model.ts

export interface INavNode {
  id: number;
  code: string;           // "commande"
  libelle: string;        // "Commandes"
  icon?: string;          // "pi pi-send"
  routerLink?: string;    // "/commande"
  badgeType?: 'RUPTURE' | 'PEREMPTION' | 'URGENT' | 'NONE';
  targetType: 'ROUTE' | 'ACTION' | 'GROUP' | 'DIVIDER';
  ordre: number;
  children?: INavNode[];
  permissions?: INavPermissions;
}

export interface INavPermissions {
  canDisplay: boolean;
  canAccess:  boolean;
  canCreate:  boolean;
  canEdit:    boolean;
  canDelete:  boolean;
  canExport:  boolean;
}

// Ability plate (pour AbilityService)
export interface IAbility {
  action: 'display' | 'access' | 'create' | 'edit' | 'delete' | 'export';
  subject: string; // = NavItem.code
}
```

### 4.4 Relation `Menu` existant ↔ `NavItem` nouveau

```
Table menu (existante, inchangée)          Table nav_item (nouvelle)
──────────────────────────────────         ────────────────────────────────
id, libelle, name, root, enable, ordre     id, code, libelle, router_link, ...

Menu.name  ←──── correspond à ────►  NavItem.code
(ex: "commande")                      (ex: "commande")

Les deux tables utilisent la même CLÉ MÉTIER (code/name)
mais restent physiquement séparées.
Le lien est logique (par valeur), pas de FK physique.
```

---

## 5. Backend — Plan d'implémentation

### Phase B1 — Migration DB

**Fichier :** `V1.4.7__nav_dynamic_menu.sql` (SQL complet en §4.1)

### Phase B2 — Endpoint `GET /api/nav/my-items`

```java
// NavItemResource.java
@GetMapping("/api/nav/my-items")
public ResponseEntity<List<NavNodeDTO>> getMyNavItems() {
    String login = SecurityUtils.getCurrentUserLogin().orElseThrow();
    List<String> roles = userService.getRolesByLogin(login);
    List<NavNodeDTO> tree = navItemService.buildTreeForRoles(roles, login);
    return ResponseEntity.ok(tree);
}
```

**Algorithme `buildTreeForRoles` :**
```
1. Charger tous les NavItem actifs dont au moins un NavItemRole.roleName ∈ roles[]
2. Pour chaque item → merger les permissions (union des rôles : OR logique)
3. Appliquer les ordres personnalisés de l'utilisateur (nav_item_user_order)
4. Reconstruire l'arbre parent/enfant trié par ordre
5. Exclure les groupes sans enfants visibles
```

### Phase B3 — Endpoint `PUT /api/nav/reorder`

```java
// Payload : liste de { navItemId, newOrdre, parentId? }
@PutMapping("/api/nav/reorder")
public ResponseEntity<Void> reorderItems(
    @RequestBody List<NavReorderDTO> reorderList) {
    String login = SecurityUtils.getCurrentUserLogin().orElseThrow();
    navItemService.saveUserOrder(login, reorderList);
    return ResponseEntity.noContent().build();
}
```

### Phase B4 — Endpoint Admin `POST /api/admin/nav/assign`

```java
// Assigne des NavItem à un rôle avec des permissions fines
@PostMapping("/api/admin/nav/assign")
public ResponseEntity<Void> assignItemsToRole(
    @RequestBody NavAssignDTO dto) {
    // dto.roleName + dto.assignments = List<{navItemId, canDisplay, canAccess, ...}>
    navItemService.assignItemsToRole(dto);
    return ResponseEntity.noContent().build();
}
```

### Phase B5 — Cache Caffeine

```java
// Clé: "navTree::" + login (invalidé lors d'un changement de rôle ou reorder)
@Cacheable(cacheNames = "navTree", key = "#login")
public List<NavNodeDTO> buildTreeForRoles(List<String> roles, String login) { ... }
```

---

## 6. Frontend — Plan d'implémentation

### Phase F1 — `NavStore` (Signal)

```typescript
// core/store/nav.store.ts
@Injectable({ providedIn: 'root' })
export class NavStore {
  private readonly api = inject(NavApiService);

  readonly navTree   = signal<INavNode[]>([]);
  readonly loading   = signal(false);
  readonly loaded    = signal(false);

  load(): void {
    if (this.loaded()) return;
    this.loading.set(true);
    this.api.getMyNavItems()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({ next: tree => { this.navTree.set(tree); this.loaded.set(true); } });
  }

  invalidate(): void { this.loaded.set(false); this.navTree.set([]); }

  // Mise à jour optimiste lors du drag & drop
  applyLocalReorder(reordered: INavNode[]): void {
    this.navTree.set(reordered);
  }
}
```

### Phase F2 — `AbilityService`

```typescript
// core/auth/ability.service.ts
@Injectable({ providedIn: 'root' })
export class AbilityService {
  private readonly abilities = signal<IAbility[]>([]);

  setFromNavTree(tree: INavNode[]): void {
    const flat = this.flatten(tree);
    const abilities: IAbility[] = [];
    for (const node of flat) {
      const p = node.permissions;
      if (!p) continue;
      if (p.canDisplay) abilities.push({ action: 'display', subject: node.code });
      if (p.canAccess)  abilities.push({ action: 'access',  subject: node.code });
      if (p.canCreate)  abilities.push({ action: 'create',  subject: node.code });
      if (p.canEdit)    abilities.push({ action: 'edit',    subject: node.code });
      if (p.canDelete)  abilities.push({ action: 'delete',  subject: node.code });
      if (p.canExport)  abilities.push({ action: 'export',  subject: node.code });
    }
    this.abilities.set(abilities);
  }

  can(action: string, subject: string): boolean {
    return this.abilities().some(a => a.subject === subject && a.action === action);
  }

  canSignal(action: string, subject: string): Signal<boolean> {
    return computed(() => this.can(action, subject));
  }

  private flatten(nodes: INavNode[]): INavNode[] {
    return nodes.flatMap(n => [n, ...this.flatten(n.children ?? [])]);
  }
}
```

### Phase F3 — `NavigationService` refactorisé

```typescript
// core/config/navigation.service.ts — REFACTORISÉ
buildNavItems(options: NavigationOptions = {}): NavItem[] {
  const tree = inject(NavStore).navTree();
  const items = this.mapNodesToNavItems(tree);
  items.push(this.buildAccountMenu(options)); // menu Compte toujours en dernier
  return items;
}

private mapNodesToNavItems(nodes: INavNode[]): NavItem[] {
  return nodes
    .filter(n => n.permissions?.canDisplay !== false)
    .sort((a, b) => a.ordre - b.ordre)
    .map(n => ({
      label:      n.libelle,
      routerLink: n.routerLink,
      faIcon:     n.icon,
      children:   n.children?.length ? this.mapNodesToNavItems(n.children) : undefined,
    }));
}
```

### Phase F4 — Directive `*appHasAbility`

```typescript
// shared/directives/has-ability.directive.ts
@Directive({ selector: '[appHasAbility]', standalone: true })
export class HasAbilityDirective {
  private readonly ability = inject(AbilityService);
  private readonly vcr     = inject(ViewContainerRef);
  private readonly tmpl    = inject(TemplateRef);

  @Input() set appHasAbility(cfg: { action: string; subject: string }) {
    this.vcr.clear();
    if (this.ability.can(cfg.action, cfg.subject)) {
      this.vcr.createEmbeddedView(this.tmpl);
    }
  }
}
```

**Utilisation :**
```html
<p-button *appHasAbility="{ action: 'create', subject: 'commande' }" label="Nouvelle commande" />
<th *appHasAbility="{ action: 'edit', subject: 'facturation' }">Actions</th>
```

### Phase F5 — Pipe `hasAbility`

```typescript
@Pipe({ name: 'hasAbility', standalone: true, pure: false })
export class HasAbilityPipe implements PipeTransform {
  private readonly a = inject(AbilityService);
  transform(subject: string, action: string): boolean { return this.a.can(action, subject); }
}
```

### Phase F6 — Guard `AbilityRouteGuard`

```typescript
export const AbilityRouteGuard: CanActivateFn = (route) => {
  const { abilityAction, abilitySubject } = route.data;
  if (!abilityAction || !abilitySubject) return true;
  if (inject(AbilityService).can(abilityAction, abilitySubject)) return true;
  inject(Router).navigate(['/accessdenied']);
  return false;
};
```

---

## 7. Réorganisation dynamique des menus (Drag & Drop)

### 7.1 Principe général

```
L'ordre des menus et sous-menus est stocké dans deux tables :
  • nav_item.ordre          → ordre global (admin, partagé)
  • nav_item_user_order     → ordre personnel (par utilisateur)

Deux niveaux de réorganisation :
  A) Admin reorganise pour tous  → modifie nav_item.ordre
  B) Utilisateur réorganise pour lui seul → modifie nav_item_user_order
```

### 7.2 Ce qu'on peut déplacer

| Objet déplaçable | Périmètre | Persistance |
|---|---|---|
| **Menus racine** (Gestion Stock, Facturation…) | Global (admin) ou perso (user) | `nav_item.ordre` ou `nav_item_user_order` |
| **Sous-menus** (Commandes, Catalogue…) | Global (admin) ou perso (user) | idem |
| **Onglets** (tabs) | Par composant — si les tabs sont des `INavNode` | idem |
| **Widgets dashboard** | À venir — même principe | Table `nav_widget_user_order` à créer |

### 7.3 Implémentation Frontend — Drag & Drop

**Librairie recommandée : `@angular/cdk/drag-drop`** (déjà dans le projet Angular)  
Alternative : **PrimeNG `p-orderList`** ou **`p-tree` avec drag**.

#### 7.3.1 Composant `NavReorderComponent` (admin)

```typescript
// features/admin/nav-manager/nav-reorder/nav-reorder.component.ts
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';

@Component({
  standalone: true,
  imports: [DragDropModule, ...],
  template: `
    <div cdkDropListGroup>
      @for (group of groups(); track group.id) {
        <div class="nav-group">
          <h6 cdkDrag cdkDragHandle (cdkDragEnded)="onGroupMoved($event, group)">
            {{ group.libelle }}
          </h6>
          <ul [cdkDropList]="group.code"
              [cdkDropListData]="group.children"
              (cdkDropListDropped)="onItemDropped($event, group)">
            @for (item of group.children; track item.id) {
              <li cdkDrag>{{ item.libelle }}</li>
            }
          </ul>
        </div>
      }
    </div>
    <p-button label="Enregistrer l'ordre" (onClick)="saveOrder()" />
  `
})
export class NavReorderComponent {
  protected groups = inject(NavStore).navTree;

  onGroupMoved(event: CdkDragDrop<INavNode[]>, group: INavNode): void {
    moveItemInArray(this.groups(), event.previousIndex, event.currentIndex);
    inject(NavStore).applyLocalReorder([...this.groups()]);
  }

  onItemDropped(event: CdkDragDrop<INavNode[]>, targetGroup: INavNode): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      // Déplacement d'un sous-menu vers un autre groupe
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
      // Mettre à jour le parentId côté backend
    }
    this.recalculateOrdres();
  }

  saveOrder(): void {
    const payload = this.buildReorderPayload(this.groups());
    inject(NavApiService).saveAdminReorder(payload).subscribe({
      next: () => inject(NavStore).invalidate() // recharge depuis le serveur
    });
  }

  private recalculateOrdres(): void {
    this.groups().forEach((g, gi) => {
      g.ordre = gi;
      g.children?.forEach((c, ci) => { c.ordre = ci; });
    });
  }
}
```

#### 7.3.2 Réorganisation personnelle (par utilisateur)

```typescript
// Bouton "Mon ordre" dans la navbar — active le mode perso
@Component({ selector: 'app-nav-personal-reorder' })
export class NavPersonalReorderComponent {
  private readonly api   = inject(NavApiService);
  private readonly store = inject(NavStore);

  savePersonalOrder(reordered: INavNode[]): void {
    const payload: NavReorderDTO[] = reordered.map((n, i) => ({
      navItemId: n.id,
      newOrdre:  i,
      parentId:  null
    }));
    this.api.saveUserReorder(payload).subscribe({
      next: () => this.store.applyLocalReorder(reordered)
    });
  }
}
```

### 7.4 API Backend pour le reorder

```java
// NavItemResource.java

// Admin : modifie nav_item.ordre (partagé)
@PutMapping("/api/admin/nav/reorder")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> adminReorder(@RequestBody List<NavReorderDTO> list) {
    navItemService.saveAdminOrder(list);
    navCacheService.evictAll(); // invalide le cache de TOUS les utilisateurs
    return ResponseEntity.noContent().build();
}

// Utilisateur : modifie nav_item_user_order (personnel)
@PutMapping("/api/nav/reorder")
public ResponseEntity<Void> userReorder(@RequestBody List<NavReorderDTO> list) {
    String login = SecurityUtils.getCurrentUserLogin().orElseThrow();
    navItemService.saveUserOrder(login, list);
    navCacheService.evict(login); // invalide uniquement le cache de cet utilisateur
    return ResponseEntity.noContent().build();
}

// DTO
public record NavReorderDTO(
    Integer navItemId,
    Integer newOrdre,
    Integer newParentId  // null si même parent, sinon = déplacement entre groupes
) {}
```

### 7.5 Gestion des onglets (tabs) dynamiques

Les onglets peuvent aussi être des `NavItem` de `niveau=3` avec `targetType=TAB` :

```sql
-- Exemple : onglets de la fiche produit
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau)
  SELECT 'produit-stock-tab',   'Stock',     'TAB', id, 0, 3 FROM nav_item WHERE code='catalogue';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau)
  SELECT 'produit-infos-tab',   'Infos',     'TAB', id, 1, 3 FROM nav_item WHERE code='catalogue';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau)
  SELECT 'produit-lots-tab',    'Lots FEFO', 'TAB', id, 2, 3 FROM nav_item WHERE code='catalogue';
```

**Composant Angular — onglets dynamiques :**
```typescript
// features/products/ui/produit-home/produit-tabs.component.ts
@Component({
  template: `
    <ul class="nav nav-tabs">
      @for (tab of tabs(); track tab.id) {
        <li class="nav-item" cdkDrag (cdkDragEnded)="onTabReorder($event)">
          <a class="nav-link"
             [class.active]="activeTab() === tab.code"
             (click)="setTab(tab.code)">
            {{ tab.libelle }}
          </a>
        </li>
      }
    </ul>
  `
})
export class ProduitTabsComponent {
  protected tabs = computed(() =>
    inject(NavStore).navTree()
      .find(g => g.code === 'catalogue')
      ?.children
      ?.filter(t => t.targetType === 'TAB')
      ?.sort((a, b) => a.ordre - b.ordre)
    ?? []
  );

  onTabReorder(event: CdkDragEnd): void {
    inject(NavApiService).saveUserReorder(this.buildPayload()).subscribe();
  }
}
```

### 7.6 Résumé des règles de déplacement

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  RÈGLES DE RÉORGANISATION                                                    │
│                                                                              │
│  ✅ Autorisé                                                                 │
│     • Réordonner des items au même niveau (même parent)                     │
│     • Déplacer un sous-menu vers un autre groupe du même niveau             │
│     • Réordonner les onglets (tabs) d'une même page                         │
│                                                                              │
│  ⚠️ Sous conditions                                                          │
│     • Déplacer un item vers un parent différent → vérifier la cohérence     │
│       des permissions (le nouveau groupe doit autoriser le rôle)            │
│     • Changer le niveau (niveau 2 → niveau 1) → requiert droits admin       │
│                                                                              │
│  ❌ Interdit                                                                 │
│     • Supprimer un item via drag (bouton dédié dans l'admin)                │
│     • Déplacer un item racine vers un parent (il resterait root)            │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Roadmap par phases

### Phase 0 — DB + entités JPA (1j)

| Tâche | Effort |
|---|---|
| Migration `V1.4.7__nav_dynamic_menu.sql` | 0.5j |
| Entités `NavItem`, `NavItemRole`, `NavItemUserOrder` + enums | 0.5j |

### Phase 1 — Backend dynamique (1.5j)

| Tâche | Effort |
|---|---|
| `NavItemService.buildTreeForRoles()` | 0.5j |
| `GET /api/nav/my-items` | 0.25j |
| `PUT /api/nav/reorder` (perso) | 0.25j |
| `PUT /api/admin/nav/reorder` (global) | 0.25j |
| Cache Caffeine `navTree` | 0.25j |

### Phase 2 — Frontend core (2j)

| Tâche | Effort |
|---|---|
| `NavApiService` | 0.25j |
| `NavStore` (Signal) | 0.5j |
| `AbilityService` (CASL-like) | 0.5j |
| `NavigationService` refactorisé → menu dynamique | 0.75j |

### Phase 3 — Contrôle d'accès fin (1.5j)

| Tâche | Effort |
|---|---|
| Directive `*appHasAbility` | 0.5j |
| Pipe `hasAbility` | 0.25j |
| Guard `AbilityRouteGuard` | 0.5j |
| Intégration sur routes prioritaires | 0.25j |

### Phase 4 — Drag & Drop (2j)

| Tâche | Effort |
|---|---|
| `NavReorderComponent` (admin) avec `cdkDragDrop` | 1j |
| Réorganisation personnelle (perso par user) | 0.5j |
| Onglets dynamiques (`targetType=TAB`) | 0.5j |

### Phase 5 — Admin `NavManagerComponent` (2j)

| Tâche | Effort |
|---|---|
| Arbre nav + matrice permissions par rôle | 1.5j |
| Prévisualisation navbar pour un rôle donné | 0.5j |

---

## 9. Fichiers créés / modifiés

### Backend

| Fichier | Action |
|---|---|
| `db/migration/V1.4.7__nav_dynamic_menu.sql` | ➕ Nouveau |
| `domain/nav/NavItem.java` | ➕ Nouveau |
| `domain/nav/NavItemRole.java` | ➕ Nouveau |
| `domain/nav/NavItemUserOrder.java` | ➕ Nouveau |
| `domain/nav/NavBadgeType.java` | ➕ Enum |
| `domain/nav/NavTargetType.java` | ➕ Enum |
| `repository/nav/NavItemRepository.java` | ➕ Nouveau |
| `repository/nav/NavItemRoleRepository.java` | ➕ Nouveau |
| `service/nav/NavItemService.java` | ➕ Interface |
| `service/nav/impl/NavItemServiceImpl.java` | ➕ Implémentation |
| `service/dto/nav/NavNodeDTO.java` | ➕ Record DTO |
| `service/dto/nav/NavReorderDTO.java` | ➕ Record DTO |
| `web/rest/nav/NavItemResource.java` | ➕ Endpoints |

> ✅ **Aucun fichier existant modifié** (`Menu.java`, `AuthorityPrivilege.java`, etc.)

### Frontend

| Fichier | Action |
|---|---|
| `shared/model/nav-item.model.ts` | ➕ `INavNode`, `INavPermissions`, `IAbility` |
| `core/data-access/nav-api.service.ts` | ➕ `getMyNavItems()`, `saveUserReorder()` |
| `core/store/nav.store.ts` | ➕ Signal store |
| `core/auth/ability.service.ts` | ➕ CASL-like service |
| `core/auth/account.service.ts` | ✏️ Init `NavStore` + `AbilityService` au login |
| `core/auth/ability-route.guard.ts` | ➕ `AbilityRouteGuard` |
| `core/config/navigation.service.ts` | ✏️ Consomme `NavStore` à la place du code dur |
| `shared/directives/has-ability.directive.ts` | ➕ `*appHasAbility` |
| `shared/pipes/has-ability.pipe.ts` | ➕ `hasAbility` pipe |
| `features/admin/nav-manager/nav-reorder/` | ➕ Drag & drop admin |
| `features/admin/nav-manager/nav-manager.component.*` | ➕ Admin complet |

---

## 10. Estimation des efforts

| Phase | Description | Backend | Frontend | Total |
|---|:---|:---:|:---:|:---:|
| **Phase 0** | DB + entités JPA | 1j | — | **1j** |
| **Phase 1** | Backend dynamique + reorder API | 1.5j | — | **1.5j** |
| **Phase 2** | Frontend core (store + nav dynamique) | — | 2j | **2j** |
| **Phase 3** | Directive + Pipe + Guard | — | 1.5j | **1.5j** |
| **Phase 4** | Drag & Drop + onglets dynamiques | — | 2j | **2j** |
| **Phase 5** | Admin NavManager + prévisualisation | 0.5j | 1.5j | **2j** |
| **TOTAL** | | **3j** | **7j** | **10j** |

> **Priorité recommandée :** Phases 0 → 1 → 2 → 3 (fonctionnel complet en ~6j, sans drag & drop).
> Phase 4 (drag & drop) en option UX premium.

---

## Annexe A — Comparaison modèles

| Critère | Modifier `Menu` existant | Nouveau modèle `nav_*` |
|---|:---:|:---:|
| Risque de régression sur sécurité existante | 🔴 Élevé | 🟢 Nul |
| Séparation des responsabilités | 🔴 Faible | 🟢 Parfaite |
| Rollback possible | 🔴 Difficile | 🟢 DROP TABLE nav_* |
| Migration DB simple | 🟡 ALTER TABLE | 🟢 CREATE TABLE |
| **Recommandation** | ❌ | ✅ |

## Annexe B — Comparaison approches d'accès

| Approche | Flexibilité | Sécurité | Complexité | Statut |
|---|:---:|:---:|:---:|:---:|
| RBAC pur (état actuel) | 🔴 | 🟡 | 🟢 | ✅ Actuel |
| RBAC + Nav DB dynamique | 🟡 | 🟡 | 🟡 | ✅ Phase 0-2 |
| RBAC + ABAC (abilities) | 🟢 | 🟢 | 🟡 | ✅ Phase 3-4 |
| Full Spring ACL | 🟢 | 🟢 | 🔴 | ❌ Overkill |
| Keycloak externalisé | 🟢 | 🟢 | 🔴 | ❌ Hors scope |

---

## Annexe C — Actions métier & `IAbility` : cas concret de la validation définitive d'une vente

> Ce chapitre répond à la question : **"Comment la validation définitive d'une vente est-elle
> gérée avec `IAbility` ?"**

### C.1 État actuel — deux couches de permissions ventes

Le projet a déjà une couche de permissions métier fine via les constantes `PR_*` :

```typescript
// Authority.constants.ts — PR_* existants
PR_FORCE_STOCK            = 'PR_FORCE_STOCK'           // Forcer le stock insuffisant
PR_MODIFIER_PRIX          = 'PR_MODIFIER_PRIX'          // Modifier un prix en caisse
PR_MODIFICATION_VENTE     = 'PR_MODIFICATION_VENTE'     // Modifier une vente clôturée
PR_ANNULATION_VENTE       = 'PR_ANNULATION_VENTE'       // Annuler une vente
PR_AJOUTER_REMISE_VENTE   = 'PR_AJOUTER_REMISE_VENTE'   // Appliquer une remise
PR_SUPPRIME_PRODUIT_VENTE = 'PR_SUPPRIME_PRODUIT_VENTE' // Supprimer une ligne vente
```

Ces permissions sont vérifiées **manuellement dans chaque composant** :

```typescript
// Actuel — éparpillé dans comptant.component.ts, product-table.component.ts, etc.
this.canForceStock    = this.hasAuthorityService.hasAuthorities(Authority.PR_FORCE_STOCK);
this.canEdit          = this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFICATION_VENTE);
this.canCancel        = this.hasAuthorityService.hasAuthorities(Authority.PR_ANNULATION_VENTE);
this.canApplyDiscount = this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE);
```

**Problème :** chaque composant duplique la logique de permission. Aucune centralisation.
L'`AuthorizationService` existant ouvre en plus un modal de **délégation par PIN** pour les
actions protégées demandées par un utilisateur qui n'a pas directement le droit.

---

### C.2 La différence fondamentale — Navigation vs Action métier

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  NavItem  targetType = ROUTE / GROUP                                             │
│  → Contrôle SI l'utilisateur VOIT et ACCÈDE au menu/page                        │
│  → Exemple : est-ce que "Gestion Courante" apparaît dans la navbar ?            │
│  → IAbility : can('display', 'gestion-courante')                                │
│                                                                                  │
│  NavItem  targetType = ACTION                                                    │
│  → Contrôle SI l'utilisateur peut EXÉCUTER une action métier précise            │
│  → Exemple : peut-il cliquer sur "Valider définitivement" ?                     │
│  → IAbility : can('execute', 'vente-validation-definitive')                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**La clé :** les `PR_*` existants deviennent des `NavItem` avec `targetType = 'ACTION'`.
Ils ne sont pas des entrées de menu — ce sont des **capacités métier** persistées en base,
gouvernées par les mêmes mécanismes que les menus de navigation.

---

### C.3 Extension du modèle — action `execute`

#### C.3.1 Étendre `NavAction` côté frontend

```typescript
// shared/model/nav-item.model.ts — ÉTENDU
export type NavAction =
  // Actions UI standard (navigation)
  | 'display' | 'access' | 'create' | 'edit' | 'delete' | 'export'
  // Actions métier (capacités business binaires)
  | 'execute';   // ← une seule action pour "peut exécuter cette capacité"

export interface IAbility {
  action: NavAction;
  subject: string; // = NavItem.code  ex: 'vente-validation-definitive'
}
```

> **Pourquoi une seule action `execute` ?**
> Les `PR_*` sont des capacités binaires (peut / ne peut pas). On n'a pas besoin de granularité
> create/edit/delete. `execute` = "peut déclencher cette action métier".

#### C.3.2 Ajouter `EXECUTE` dans `nav_item_role`

```sql
-- Ajouter la colonne dans nav_item_role
ALTER TABLE nav_item_role ADD COLUMN IF NOT EXISTS can_execute BOOLEAN NOT NULL DEFAULT FALSE;
```

#### C.3.3 Données SQL — `PR_*` migrés en `nav_item`

```sql
-- PR_* comme ACTION items enfants de 'ventes'
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau, actif)
  SELECT 'pr-force-stock',              'Forcer le stock',           'ACTION', id, 0, 3, TRUE FROM nav_item WHERE code='ventes';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau, actif)
  SELECT 'pr-modifier-prix',            'Modifier le prix',          'ACTION', id, 1, 3, TRUE FROM nav_item WHERE code='ventes';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau, actif)
  SELECT 'pr-remise-vente',             'Appliquer une remise',      'ACTION', id, 2, 3, TRUE FROM nav_item WHERE code='ventes';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau, actif)
  SELECT 'pr-supprimer-ligne-vente',    'Supprimer une ligne vente', 'ACTION', id, 3, 3, TRUE FROM nav_item WHERE code='ventes';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau, actif)
  SELECT 'pr-modifier-vente',           'Modifier une vente',        'ACTION', id, 4, 3, TRUE FROM nav_item WHERE code='ventes';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau, actif)
  SELECT 'pr-annuler-vente',            'Annuler une vente',         'ACTION', id, 5, 3, TRUE FROM nav_item WHERE code='ventes';
INSERT INTO nav_item (code, libelle, target_type, parent_id, ordre, niveau, actif)
  SELECT 'vente-validation-definitive', 'Validation définitive',     'ACTION', id, 6, 3, TRUE FROM nav_item WHERE code='ventes';

-- ROLE_ADMIN : tout
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_execute)
  SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE
  FROM nav_item WHERE target_type = 'ACTION' AND code LIKE 'pr-%'
     OR code = 'vente-validation-definitive';

-- ROLE_CAISSIER : validation définitive + force stock + remise
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_execute)
  SELECT id, 'ROLE_CAISSIER', TRUE, TRUE, TRUE
  FROM nav_item WHERE code IN ('vente-validation-definitive','pr-force-stock','pr-remise-vente');

-- ROLE_VENDEUR : aucune action définitive → pas d'INSERT → can_execute = FALSE par défaut
```

---

### C.4 Flux complet — Validation définitive d'une vente

```
Utilisateur (ROLE_CAISSIER) — écran de vente
         │
         ▼
① AbilityService chargé au login depuis NavStore (tree includes ACTION items)
   → abilities contient : { action: 'execute', subject: 'vente-validation-definitive' }
         │
         ▼
② Template vérifie :
   @if ('vente-validation-definitive' | hasAbility: 'execute') { ... }
   → TRUE  → bouton "Valider définitivement" visible et cliquable
   → FALSE → bouton absent OU bouton "délégation" (cf. §C.5)
         │
         ▼
③ Clic → SaleComponent.onValidateClick()
   → can('execute', 'vente-validation-definitive') = TRUE
   → appel direct : salesService.validateDefinitive(saleId)
         │
         ▼
④ Backend : POST /api/sales/{id}/validate
   @PreAuthorize("hasAuthority('ROLE_CAISSIER') or hasAuthority('ROLE_ADMIN')")
   // Double sécurité : le frontend filtre, le backend valide indépendamment
```

---

### C.5 Cas spécial — Autorisation déléguée (modal PIN existant préservé)

L'`AuthorizationService` actuel supporte un pattern puissant :
**un caissier sans droit direct peut demander l'autorisation d'un responsable via PIN**.
Ce pattern est **préservé et cohabite avec `IAbility`** :

```typescript
// SalesComponent (version cible)
protected readonly canValidateDirectly = inject(AbilityService)
  .canSignal('execute', 'vente-validation-definitive');

onValidateClick(): void {
  if (this.canValidateDirectly()) {
    // ① Accès direct — l'utilisateur a le droit lui-même
    this.salesService.validateDefinitive(this.saleId());
  } else {
    // ② Délégation → modal PIN (AuthorizationService EXISTANT, inchangé)
    this.authorizationService
      .requestAuthorization(this.saleId(), 'vente-validation-definitive')
      .pipe(filter(Boolean))
      .subscribe(() => this.salesService.validateDefinitive(this.saleId()));
  }
}
```

```html
<!-- Template correspondant -->
@if (canValidateDirectly()) {
  <!-- Accès direct sans PIN -->
  <p-button label="Valider définitivement" icon="pi pi-check-circle"
            severity="success" (onClick)="onValidateClick()" />

} @else if (canRequestDelegation()) {
  <!-- Accès délégué — nécessite PIN d'un responsable -->
  <p-button label="Valider (autorisation requise)" icon="pi pi-lock"
            severity="warning" [outlined]="true" (onClick)="onValidateClick()" />
}
<!-- Aucun des deux = bouton totalement absent -->
```

---

### C.6 Migration progressive — sans rupture

```
Étape 1 : Tables nav_* créées, PR_* insérés comme ACTION items
          → Aucun changement dans les composants existants

Étape 2 : NavStore charge le tree (inclut les ACTION items)
          AbilityService génère les abilities incluant { action:'execute', subject:'pr-*' }
          → Les composants continuent d'utiliser hasAuthorities() — ça fonctionne encore

Étape 3 : HasAuthorityService BRIDGÉ → délègue à AbilityService
          → Migration transparente, composant par composant

Étape 4 : Remplacer hasAuthorities(Authority.PR_*) par can('execute', 'pr-*') progressivement
```

**Bridge de compatibilité** (étape 3) :

```typescript
// Remplacer has-authority.service.ts
@Injectable({ providedIn: 'root' })
export class HasAuthorityService {
  private readonly ability  = inject(AbilityService);
  private readonly account  = inject(AccountService);

  hasAuthorities(authorities: string | string[]): boolean {
    const list = Array.isArray(authorities) ? authorities : [authorities];
    // Nouveau : cherche dans les abilities (PR_FORCE_STOCK → pr-force-stock)
    const viaAbility = list.some(a =>
      this.ability.can('execute', a.toLowerCase().replace(/_/g, '-'))
    );
    if (viaAbility) return true;
    // Fallback : RBAC existant (pendant la transition)
    return this.account.hasAnyAuthority(list);
  }
}
```

---

### C.7 Tableau de correspondance `PR_*` → `IAbility`

| Ancien `Authority.*` | Nouveau `NavItem.code` | Vérification `IAbility` |
|---|---|---|
| `PR_FORCE_STOCK` | `pr-force-stock` | `can('execute', 'pr-force-stock')` |
| `PR_MODIFIER_PRIX` | `pr-modifier-prix` | `can('execute', 'pr-modifier-prix')` |
| `PR_AJOUTER_REMISE_VENTE` | `pr-remise-vente` | `can('execute', 'pr-remise-vente')` |
| `PR_SUPPRIME_PRODUIT_VENTE` | `pr-supprimer-ligne-vente` | `can('execute', 'pr-supprimer-ligne-vente')` |
| `PR_MODIFICATION_VENTE` | `pr-modifier-vente` | `can('execute', 'pr-modifier-vente')` |
| `PR_ANNULATION_VENTE` | `pr-annuler-vente` | `can('execute', 'pr-annuler-vente')` |
| *(nouveau)* | `vente-validation-definitive` | `can('execute', 'vente-validation-definitive')` |
| `PR_VOIR_STOCK_INVENTAIRE` | `pr-voir-stock-inventaire` | `can('execute', 'pr-voir-stock-inventaire')` |

---

*Document créé le 2026-04-06 — mis à jour le 2026-04-06*
*Statut : 📋 Plan v3 — nav_* indépendant + Drag & Drop + Actions métier (PR_* → IAbility)*

<!-- Colonne Actions : visible seulement si edit OU delete -->
@if (('commande' | hasAbility: 'edit') || ('commande' | hasAbility: 'delete')) {
  <th>Actions</th>
}

<!-- Chaque bouton protégé individuellement -->
<p-button *appHasAbility="{ action: 'edit',   subject: 'commande' }" icon="pi pi-pencil" />
<p-button *appHasAbility="{ action: 'delete', subject: 'commande' }" icon="pi pi-trash" severity="danger" />
