# Nouvelle stratégie de configuration initiale — Setup Wizard & Config Editor

## Contexte

Problème initial : l'interface NSIS (`installer.nsh`) affichait un formulaire de configuration
pendant l'installation, mais les valeurs saisies n'étaient pas capturées (bug de callback
`PageConfigLeave` non déclenché dans Tauri MUI2). Deux interfaces coexistaient sans coordination
(`installer.nsh` + `configure-database.ps1`).

Inspiration : pattern IntelliJ/VS Code — l'application elle-même affiche la configuration
au premier lancement, pas l'installateur.

---

## Ce qui a été implémenté

### 1. Champ `setup_complete` dans `config.json` (Rust)

**Fichier :** `src-tauri/src/config.rs`

- Ajout du champ `setup_complete: bool` avec `#[serde(default = "default_setup_complete")]`
  (défaut `false` à la désérialisation)
- Ajout de la fonction `default_setup_complete() -> bool { false }`
- Ajout de la méthode `is_setup_complete() -> bool`
- Champ initialisé à `false` dans les deux initialiseurs `Self {}` :
  - `Default::default()`
  - `default_with_data_dir()`

### 2. Commandes Tauri (Rust)

**Fichier :** `src-tauri/src/main.rs`

Toutes les commandes sont sous `#[cfg(feature = "bundled-backend")]`.

| Commande | Rôle |
|---|---|
| `check_needs_setup` | Retourne `bool` — `true` si `setup_complete == false` |
| `get_setup_defaults` | Retourne `SetupDefaults` — valeurs actuelles de `config.json` pour pré-remplir le wizard |
| `complete_initial_setup` | Reçoit les paramètres DB + port serveur, écrit `config.json` avec `setup_complete: true`, démarre le backend |
| `get_app_config_dto` | Retourne `AppConfigDto` — tous les paramètres éditables de `config.json` |
| `save_app_config_dto` | Reçoit `AppConfigDto`, écrit `config.json` (ne redémarre pas le backend) |

**Struct `SetupDefaults`** : `db_host`, `db_port`, `db_name`, `db_username`, `db_schema`, `server_port`

**Struct `AppConfigDto`** : tous les champs éditables — DB, server_port, JVM (heap, metaspace,
direct memory, GC, options supplémentaires), mail (username, email), FNE (url, api_key,
point_of_sale), port_com

**Logique de démarrage** dans `setup()` :
```
si setup_complete == false
  → émettre événement Tauri "setup-required"
  → NE PAS démarrer le backend
sinon
  → démarrer le backend normalement
```

**Correction :** `use tauri::Emitter` rendu inconditionnel (les deux modes utilisent `emit`).

### 3. SetupWizardComponent (Angular)

**Fichiers :**
- `pharmaSmart-app/src/main/webapp/app/core/setup/setup-wizard.component.ts`
- `pharmaSmart-app/src/main/webapp/app/core/setup/setup-wizard.component.html`
- `pharmaSmart-app/src/main/webapp/app/core/setup/setup-wizard.component.scss`

**Comportement :**
- Composant standalone avec `visible = signal(false)`
- `ngOnInit` : écoute l'événement `"setup-required"` via `listen()` ET appelle
  `check_needs_setup` pour détecter un événement manqué (Angular peut charger après l'émission Rust)
- `loadDefaults()` : appelle `get_setup_defaults`, pré-remplit le formulaire
- Formulaire : PostgreSQL (host, port, dbname, username, password, schema) + port serveur
- `onSubmit()` : appelle `complete_initial_setup`, ferme le wizard (`visible.set(false)`)
- Utilise `NgZone.run()` pour les callbacks Tauri (hors zone Angular)
- Sélecteur : `app-setup-wizard`

**Intégration** dans `main.component.html` :
```html
@if (isTauriMode) {
  <app-setup-wizard></app-setup-wizard>
  <jhi-backend-splash></jhi-backend-splash>
}
```

### 4. AppConfigEditorComponent (Angular) — page dédiée

**Fichiers :**
- `pharmaSmart-app/src/main/webapp/app/features/settings/feature/app-config-editor/app-config-editor.component.ts`
- `pharmaSmart-app/src/main/webapp/app/features/settings/feature/app-config-editor/app-config-editor.component.html`
- `pharmaSmart-app/src/main/webapp/app/features/settings/feature/app-config-editor/app-config-editor.component.scss`
- `pharmaSmart-app/src/main/webapp/app/features/settings/feature/app-config-editor/app-config-editor.route.ts`

**Comportement :**
- Page autonome (pas une modale — trop de contenu)
- Route : `/app-config` — protégée par `AuthGuard` (ROLE_ADMIN bypass automatique)
- Vérification admin au `ngOnInit` via `NavigationService.hasAnyAuthority` → redirige vers
  `/accessdenied` si non admin
- Chargement automatique de `config.json` via `get_app_config_dto` au démarrage de la page
- 5 onglets `pharma-nav-tabs-container` / `ngbNav` : PostgreSQL, Serveur, JVM, Mail, FNE
- Sauvegarde via `save_app_config_dto` puis redémarrage du backend via `BackendManagerService`
- SCSS : `@import 'app/shared/scss/pharma-nav'`

**Route enregistrée** dans `entity.routes.ts` :
```ts
{ path: "app-config", canActivate: [AuthGuard], loadChildren: () => import("...app-config-editor.route") }
```

### 5. Points d'entrée navigation (Sidebar + Navbar)

**Fichiers :** `sidebar.component.ts`, `navbar.component.ts`

- Import `faSlidersH` de `@fortawesome/free-solid-svg-icons`
- Getter `isTauriAdmin` : `isRunningInTauri() && NavigationService.hasAnyAuthority(ROLE_ADMIN, account.authorities)`
- Item `"Configuration avancée"` injecté dans `additionalAccountMenuItems` uniquement si `isTauriAdmin`
- `openConfigEditor()` navigue vers `/app-config`
- Bouton flottant dans la sidebar footer (visible uniquement si `isTauriAdmin`)

---

## État des fichiers NSIS/PS1 existants

| Fichier | Rôle actuel | Statut |
|---|---|---|
| `installer-hooks/installer.nsh` | Crée `config.json` avec défauts + appelle le PS1 | Conservé, filet de sécurité |
| `installer-hooks/configure-database.ps1` | Formulaire graphique PS1 + écriture `config.json` | Conservé, filet de sécurité |

Ces fichiers ne sont pas encore modifiés — la nouvelle stratégie n'est pas encore testée.

---

## Ce qui reste à faire si le test fonctionne

### Priorité 1 — Simplification de l'installateur

- **`configure-database.ps1`** : supprimer le formulaire graphique (Form WinForms).
  Garder uniquement la création de `config.json` avec les valeurs par défaut et
  **`"setup_complete": false`** explicitement.
- **`installer.nsh`** : aucun changement nécessaire. Il appelle déjà le PS1.
- Objectif : l'installateur crée juste un `config.json` minimal, le wizard Angular
  prend le relais au premier lancement.

### Priorité 2 — UX du wizard

- Ajouter un indicateur de progression pendant le démarrage du backend après soumission
  (le `BackendSplashComponent` prend le relais, vérifier que la transition est fluide)
- Tester le cas où l'utilisateur ferme la fenêtre sans valider (le backend ne démarre pas —
  prévoir un message d'avertissement ou empêcher la fermeture)

### Priorité 3 — Robustesse

- Tester le cas `setup_complete: false` sur un `config.json` existant (mise à jour depuis
  une version antérieure sans le champ)
- Vérifier que `check_needs_setup` retourne bien `true` même si Angular charge après
  l'émission de `"setup-required"`

### Priorité 4 — Nettoyage (après validation complète)

- Supprimer le formulaire WinForms de `configure-database.ps1`
- Mettre à jour la documentation `TAURI_BACKEND_SETUP.md` pour décrire le nouveau flux
- Mettre à jour `HOW-TO-CONFIGURE-BACKEND.md` pour mentionner le wizard au premier lancement

---

## Flux complet de la nouvelle stratégie

```
Installation NSIS
  └─ installer.nsh
       └─ configure-database.ps1  →  config.json créé (setup_complete: false, valeurs par défaut)

Premier lancement Tauri
  └─ main.rs setup()
       └─ AppConfig::load()  →  setup_complete == false
            └─ émettre "setup-required"  (backend NE démarre PAS)

Angular charge
  └─ SetupWizardComponent.ngOnInit()
       ├─ listen("setup-required")  (événements futurs)
       └─ invoke("check_needs_setup")  (événement manqué)
            └─ visible.set(true)
                 └─ loadDefaults()  →  formulaire pré-rempli depuis config.json

Utilisateur valide le formulaire
  └─ invoke("complete_initial_setup", { ...params })
       └─ main.rs
            ├─ config.json mis à jour (setup_complete: true + nouveaux params)
            └─ backend Spring Boot démarré
                 └─ BackendSplashComponent attend la disponibilité du backend
                      └─ Application opérationnelle

Lancements suivants
  └─ setup_complete == true  →  backend démarre directement, wizard ignoré

Admin veut modifier la config
  └─ Menu "Configuration avancée" (sidebar/navbar, ROLE_ADMIN + Tauri uniquement)
       └─ Navigation vers /app-config
            └─ AppConfigEditorComponent
                 ├─ get_app_config_dto()  →  formulaire pré-rempli
                 └─ save_app_config_dto() + restart backend
```
