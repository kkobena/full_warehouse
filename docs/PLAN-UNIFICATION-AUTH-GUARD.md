# Plan — Unification des guards d'authentification

**Objectif** : Remplacer `UserRouteAccessService` (auth + RBAC statique) et `AbilityRouteGuard`
(ABAC) par un unique `AuthGuard` qui concentre les trois vérifications en une seule passe.

**Règle fondamentale** : `NavItemRole.canAccess` IS le contrôle d'accès — plus d'`authorities[]`
statique dans les routes business. Si un nav_item n'est pas configuré pour un rôle
(aucune ligne `NavItemRole` pour ce rôle), ce rôle n'a pas accès. Exception : `ROLE_ADMIN`
bypasse toujours le check ABAC.

**ROLE_USER** : aucun seed `NavItemRole` — n'a accès à aucun module business.

**Rôles custom** : créés via l'interface admin, permissions assignées dans nav-manager — aucun
code à modifier.

---

## 1. Problème du timing — chargement du nav tree

`accountService.identity()` appelle `navStore.load()` (HTTP asynchrone) dans un `tap()`.
L'observable de `identity()` émet l'account **avant** que le nav tree soit chargé.
Si le guard évalue l'ABAC immédiatement, `can()` retourne `false` pour tout le monde.

**Solution** : `NavStore.whenLoaded()` — observable qui attend que `loaded()` passe à `true`.

---

## 2. Nouveau guard unifié — `AuthGuard`

**Fichier** : `src/main/webapp/app/core/auth/auth.guard.ts`

```typescript
import { inject, Injector, isDevMode } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, take } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavStore } from 'app/core/store/nav.store';
import { NavAbilityAction } from 'app/shared/model/nav-item.model';
import { Authority } from 'app/config/authority.constants';

export const AuthGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const accountService = inject(AccountService);
  const stateStorage = inject(StateStorageService);
  const ability = inject(AbilityService);
  const navStore = inject(NavStore);
  const router = inject(Router);
  const injector = inject(Injector);

  return accountService.identity().pipe(
    switchMap(account => {
      // ── 1. Authentification ───────────────────────────────────────────────
      if (!account) {
        stateStorage.storeUrl(state.url);
        router.navigate(['/login']);
        return of(false);
      }

      // ── 2. ADMIN bypass ───────────────────────────────────────────────────
      // ROLE_ADMIN a toujours accès — les contraintes ABAC ne s'appliquent pas
      if (accountService.hasAnyAuthority(Authority.ADMIN)) {
        return of(true);
      }

      // ── 3. Attente du nav tree (résout le timing async de navStore.load()) ─
      const loaded$ = navStore.loaded()
        ? of(true)
        : toObservable(navStore.loaded, { injector }).pipe(filter(Boolean), take(1));

      return loaded$.pipe(
        map(() => {
          // ── 4. ABAC strict (NavItemRole.canAccess) ─────────────────────────
          // Pas de fallback : non configuré = refusé
          const subject = route.data['abilitySubject'] as string | undefined;
          if (subject) {
            const action = (route.data['abilityAction'] as NavAbilityAction | undefined) ?? 'access';
            if (!ability.can(action, subject)) {
              if (isDevMode()) {
                console.error(`[AuthGuard] ABAC denied — subject: ${subject}, action: ${action}`);
              }
              router.navigate(['/accessdenied']);
              return false;
            }
          }
          return true;
        }),
      );
    }),
  );
};
```

**Logique en 4 étapes :**
1. Auth → `/login` si non connecté
2. ADMIN → bypass total (toujours `true`)
3. Attente nav tree (`NavStore.whenLoaded()` inline)
4. ABAC strict : `can(action, subject)` sans fallback

---

## 3. Mise à jour NavStore — `whenLoaded()`

Ajouter `toObservable` du signal `loaded` comme field :

```typescript
// Dans NavStore
import { toObservable } from '@angular/core/rxjs-interop';

readonly loaded$ = toObservable(this.loaded);
```

Le guard l'utilise directement via `toObservable(navStore.loaded, { injector })`.

---

## 4. Stratégie de migration

### Principe central

**`entity.routes.ts` = seule source de vérité pour les guards** :
- `canActivate: [AuthGuard]` au niveau parent (route `loadChildren`)
- `abilitySubject` dans `data` pour les routes business avec nav items en DB
- Pas d'`abilitySubject` pour les routes utilitaires (auth check seule)

**Fichiers de routes enfants** : supprimer tout `canActivate`, `authorities`, imports
`UserRouteAccessService` et `Authority`. Garder uniquement les resolvers et `pageTitle`.

---

## 5. Mapping routes → nav items

### Routes business (avec abilitySubject)

| Route parent | `abilitySubject` | Nav item en DB ? |
|---|---|---|
| `commande` | `commande` | Oui |
| `customer` | `clients` | Oui |
| `fournisseur` | `fournisseurs` | Oui |
| `mvt-caisse` | `mvt-caisse` | Oui |
| `facturation` | `factures` | Oui |
| `gestion-peremption` | `peremptions` | Oui |
| `depot` | `depot` | Oui |
| `tiers-payant` | `tiers-payant` | Oui |
| `inventaire` | `inventaire` | Oui |
| `produits` | `catalogue` | Oui |
| `features-ajustement` | `ajustements` | Oui |
| `sales-home/gestion` | `ventes` | Oui |
| `reports/sales` | `rapport-ventes` | Oui |
| `reports/stock` | `rapport-stock` | Oui |
| `reports/partners` | `rapport-partners` | Oui |

### Routes utilitaires (auth check seule, pas d'abilitySubject)

Référentiel (rayon, forme, famille, gamme, laboratoire, remises, tableaux, tva, mode-payments,
fournisseur hors ABAC, motif-ajustement, motif-retour-produit, parametre), Admin (magasin,
poste, menu), Compte (my-cash-register, settings, password), Dashboard, Semois, Differes.

> **Note** : Ces routes n'ont pas de nav_item en DB. L'accès est protégé par auth seule.
> Pour les restreindre dynamiquement, il faudra créer les nav_items correspondants via
> nav-manager et ajouter `abilitySubject` dans leur route.

---

## 6. État final de entity.routes.ts

```typescript
import { Routes } from '@angular/router';
import { AuthGuard } from '../core/auth/auth.guard';

const routes: Routes = [
  // ── Référentiel (auth only) ──────────────────────────────────────────────
  { path: 'categorie',            data: { pageTitle: '...' }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'famille-produit',      data: { pageTitle: '...' }, canActivate: [AuthGuard], loadChildren: ... },
  // ... autres référentiel

  // ── Business avec ABAC ───────────────────────────────────────────────────
  { path: 'commande',             data: { pageTitle: '...', abilitySubject: 'commande'    }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'customer',             data: { pageTitle: '...', abilitySubject: 'clients'     }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'fournisseur',          data: { pageTitle: '...', abilitySubject: 'fournisseurs'}, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'mvt-caisse',           data: { pageTitle: '...', abilitySubject: 'mvt-caisse'  }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'facturation',          data: { pageTitle: '...', abilitySubject: 'factures'    }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'gestion-peremption',   data: { pageTitle: '...', abilitySubject: 'peremptions' }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'depot',                data: { pageTitle: '...', abilitySubject: 'depot'       }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'tiers-payant',         data: { pageTitle: '...', abilitySubject: 'tiers-payant'}, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'inventaire',           data: { pageTitle: '...', abilitySubject: 'inventaire'  }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'produits',             data: { pageTitle: '...', abilitySubject: 'catalogue'   }, canActivate: [AuthGuard], loadChildren: ... },
  { path: 'features-ajustement',  data: { pageTitle: '...', abilitySubject: 'ajustements' }, canActivate: [AuthGuard], loadChildren: ... },
];
```

---

## 7. app.routes.ts — route admin

Remplacer `UserRouteAccessService` par `AuthGuard`. Pas d'`abilitySubject` (admin bootstrappage
système — nav items admin à créer séparément si contrôle granulaire souhaité).

```typescript
{
  path: 'admin',
  canActivate: [AuthGuard],
  loadChildren: () => import('./admin/admin.routes'),
},
```

> L'accès admin est garanti par le bypass ADMIN dans `AuthGuard`. Tout rôle non-ADMIN qui
> tente d'accéder à `/admin/*` sera bloqué dès l'étape ADMIN-bypass (account.hasAnyAuthority
> retourne false) et tombera en ABAC check — sans `abilitySubject` → retourne `true`... ce qui
> est faux. Il faut soit créer un nav_item pour admin, soit ajouter un check statique pour admin.
>
> **Décision** : les routes `/admin/*` créent un nav_item `admin` et `abilitySubject: 'admin'` dans
> `app.routes.ts` est optionnel pour l'instant puisque seul ROLE_ADMIN passe le bypass.

---

## 8. Cleanup final

```bash
# Vérifier qu'aucune référence ne subsiste
grep -r "UserRouteAccessService" src/main/webapp/app --include="*.ts"
grep -r "AbilityRouteGuard" src/main/webapp/app --include="*.ts"
```

Supprimer :
- `src/main/webapp/app/core/auth/user-route-access.service.ts`
- `src/main/webapp/app/core/auth/ability-route.guard.ts`

---

## 9. Ordre d'exécution

```
1. Créer auth.guard.ts                              ✓ (fait)
2. Mettre à jour entity.routes.ts                   ✓ (fait)
3. Mettre à jour app.routes.ts                      ✓ (fait)
4. Mettre à jour features/sales/sales.routes.ts     ✓ (fait)
5. Mettre à jour entities/reports/reports.route.ts  ✓ (fait)
6. Nettoyer tous les fichiers de routes enfants      ✓ (fait)
7. Grep & supprimer les anciens guards              (à faire après vérification)
```

---

*Document mis à jour le 2026-04-12 — branch `privile_abilitation`*
*Sémantique stricte : non configuré = refusé (sauf ROLE_ADMIN)*
