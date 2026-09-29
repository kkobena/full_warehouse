# Plan — Sécurisation des endpoints de l'API

> État des lieux au 2026-09-28 et plan de réalisation. Né du chantier du dashboard
> personnalisable (cf. [PLAN-DASHBOARD-PERSONNALISABLE.md](PLAN-DASHBOARD-PERSONNALISABLE.md) § 5.2).

## En bref

Aujourd'hui, **l'API ne contrôle presque que l'authentification**. Sur 131 contrôleurs REST, 14
portent une annotation de sécurité. Tout utilisateur connecté, caissier compris, peut appeler
n'importe quel autre endpoint : lire la marge ou les créances, et vraisemblablement modifier un
référentiel (TVA, remises, fournisseurs) s'il connaît l'URL. Les menus masquent les écrans, ils ne
protègent pas les données.

Le plan aligne l'API sur ce que les menus accordent déjà : **un endpoint exige le droit
`nav_item_role` de l'écran qui l'utilise**. Le contrôle est lu en base, comme pour les widgets du
dashboard. Il se déploie d'abord en **mode audit** (on journalise sans bloquer), puis on bascule en
**mode blocage** une fois les journaux lus.

## 1. État des lieux

### 1.1 Chaîne de filtres (`config/SecurityConfiguration.java`)

| Règle | Portée |
|---|---|
| `permitAll` | ressources statiques, `/api/auth/**`, `/api/register`, `/api/activate`, réinitialisation du mot de passe, `/api/updates/**` (relais de mise à jour des postes, documenté), `/management/health`, `/management/info`, **`/swagger-ui/**` et `/v3/api-docs/**`** |
| `hasAuthority(ROLE_ADMIN)` | `/api/admin/**`, `/management/**` (hors health) |
| `authenticated` | tout le reste de `/api/**` |

- Jeton JWT sans état, CSRF désactivé (cohérent avec un jeton en en-tête).
- `@EnableMethodSecurity(securedEnabled = true)` : `@PreAuthorize` et `@Secured` sont actifs.
- **À corriger** : Swagger et la description OpenAPI sont publics, y compris en production.
  Ils donnent la carte complète de l'API à quiconque atteint le poste serveur.
- **À vérifier** : les URL hors `/api/**` et hors ressources statiques ne correspondent à aucune
  règle (pas de `anyRequest()`). Il faut s'assurer qu'elles sont bien refusées.

### 1.2 Contrôleurs et annotations

| Package (`web/rest/`) | Contrôleurs | Annotés |
|---|---:|---:|
| report | 21 | 3 |
| referential | 17 | 0 |
| (racine) | 17 | 2 |
| commande | 12 | 0 |
| stock | 10 | 1 |
| sales | 8 | 0 |
| facturation | 7 | 0 |
| payment_transaction | 5 | 0 |
| mobile | 4 | 1 |
| inventaire | 4 | 1 |
| declaration_ca | 4 | 4 |
| settings | 4 | 0 |
| stat, reglement, reassort, dashboard | 2 chacun | 0 |
| ticketZ, scheduler, product_to_destroy, prix_reference, depot, dci, cash_register, activity_summary | 1 chacun | 0 |
| nav, license | 1 chacun | 1 |
| **Total** | **131** | **14** |

Annotations en place : 5 `hasRole('ADMIN')`, et quelques privilèges
`pr-*` (`pr-gere-licence`, `pr-declaration-ca-*`, `pr-cloture-inventaire`). Le module
`declaration_ca` est le seul entièrement protégé.

### 1.3 Ce qui existe déjà pour les droits

- **Droits par écran** : `nav_item` (39 routes, 91 sections, 27 actions, 24 widgets) et
  `nav_item_role` (`can_display`, `can_access`, `can_create`, `can_edit`, `can_delete`,
  `can_export`, `can_execute`), gérés dans l'écran d'attribution des menus.
- **Côté front**, `AuthGuard` refuse une route dont le code `abilitySubject` n'a pas `can_access`.
  Le serveur n'applique pas cette règle.
- **Côté jeton**, `DomainUserDetailsService` n'y met que le **premier rôle** de l'utilisateur et
  les codes `ACTION` pour lesquels il a `can_execute`. Un `hasAuthority('rapport-…')` sur un code
  `SECTION` ou `ROUTE` serait donc toujours faux. Un utilisateur à deux rôles n'a les actions que
  du premier.
- **Modèle déjà éprouvé** : `WidgetAuthorizationService` (dashboard) lit `nav_item_role` à chaque
  requête, pour l'union de **tous** les rôles de l'utilisateur.

### 1.4 Réponse à un refus

- **Serveur** : `ExceptionTranslator.handleAnyException(Throwable)` intercepte toute exception
  non métier. Une `AccessDeniedException` levée par un `@PreAuthorize` part donc en **HTTP 500**
  (« Une erreur interne est survenue ») au lieu d'un 403. C'est le cas, dès aujourd'hui, des
  endpoints déjà annotés. À confirmer par un test.
- **Front** : seul un **401** déconnecte l'utilisateur (`auth-expired.interceptor.ts`). Un **403**
  sur un appel HTTP produit une simple alerte (`warehouseApp.httpError`). Seule une navigation
  refusée mène à `/accessdenied`. Un 403 est donc la bonne réponse.
  > Le commentaire de `ExceptionTranslator` affirmant qu'« un 401 ou un 403 déconnecterait » est
  > inexact pour le 403 ; le dashboard renvoie ses refus en `GenericError` (400) pour cette
  > raison, ce qui pourra être aligné.

### 1.5 Hors du web

- **Applications mobiles** : `pharma-mobile-report` et `sales-android` passent par `/api/mobile/**`
  et `/api/customers/tiers-payants/…`, jamais par `web/rest/report/*`. Vérifié le 2026-09-28.
- **Module batch** : pas d'appel HTTP, hors périmètre.

## 2. Risques concrets aujourd'hui

| Risque | Exemple | Gravité |
|---|---|---|
| Lecture de chiffres de gestion | un caissier appelle `/api/pnl-analytique`, `/api/marges-profitability`, `/api/tiers-payant/creances` | élevée |
| Modification de référentiels | tout utilisateur connecté peut appeler les `POST/PUT/DELETE` de `referential/*` (TVA, remises, modes de paiement, fournisseurs) | élevée, **à confirmer endpoint par endpoint** |
| Opérations sensibles hors écran | annulations, ajustements de stock, clôtures, facturation appelés sans le droit de l'écran | élevée |
| Carte de l'API publique | `/swagger-ui/**` et `/v3/api-docs/**` en `permitAll` | moyenne |
| Refus mal signalés | `AccessDeniedException` → 500, ce qui cache un refus légitime derrière une « erreur interne » | moyenne |
| Multi-rôle | seul le premier rôle entre dans le jeton | faible (rare), mais source d'incompréhensions |

## 3. Cible

### 3.1 Règle

> Tout endpoint de `/api/**` exige, en plus de l'authentification, **un droit sur un `nav_item`**,
> ou une **exemption déclarée et justifiée**.

- **Le droit se lit en base**, pour l'union des rôles de l'utilisateur, et non dans le jeton. Un
  droit retiré s'applique sans attendre la reconnexion, et le multi-rôle fonctionne.
- **L'action par défaut suit le verbe HTTP** :

  | Verbe | Droit requis |
  |---|---|
  | `GET` | `can_access` |
  | `POST` | `can_create` |
  | `PUT`, `PATCH` | `can_edit` |
  | `DELETE` | `can_delete` |
  | export PDF/Excel/CSV | `can_export` |
  | opération métier (clôture, validation, annulation) | `can_execute` sur un code `ACTION`, quand il existe |

  L'annotation peut surcharger ce défaut (par exemple un `POST` de recherche, qui n'est qu'une
  lecture).
- **Plusieurs codes possibles** : un endpoint partagé par plusieurs écrans liste tous leurs codes,
  et l'un d'eux suffit. Par exemple, la valorisation du stock sert le rapport et l'accueil
  pharmacien.
- **Exemptions** (authentification seule) :
  - le compte de l'utilisateur et l'arbre de navigation ;
  - la licence en lecture ;
  - les **lectures de référentiels** utilisées partout (produits, clients, tiers payants, TVA,
    modes de paiement) : l'écran de vente en a besoin pour tous les rôles.

  Chaque exemption porte une justification dans le code.
- **`/api/admin/**`** reste réservé à `ROLE_ADMIN` au niveau de la chaîne de filtres.

### 3.2 Mécanisme

1. **`@RequiresNavAccess`** sur la classe ou la méthode :

   ```java
   @RequiresNavAccess({"rapport-stock.stock-valuation"})           // action déduite du verbe
   @RequiresNavAccess(value = {"factures"}, action = NavAction.EXPORT)
   @NavAccessExempt("lecture du catalogue, nécessaire à la vente pour tous les rôles")
   ```

2. **`NavAccessAspect`**, sur le modèle de `LicenseEnforcementAspect` : il intercepte les méthodes
   des `@RestController`, résout l'action, puis interroge `NavAccessService`.
3. **`NavAccessService`** :
   - il factorise la lecture de `nav_item_role` aujourd'hui dans `WidgetAuthorizationService`, qui
     s'appuiera dessus ;
   - il garde un cache par ensemble de rôles, vidé par l'écran d'attribution des menus
     (`assignItemsToRole` vide déjà `NAV_TREE_CACHE`) ;
   - il tient compte de `actif` et de `required_feature` (licence).
4. **Refus** : une `NavAccessDeniedException`, traduite par `ExceptionTranslator` en **HTTP 403**
   `ProblemDetail`, avec un message lisible (« Accès refusé : Valorisation du stock »). Le même
   gestionnaire traite `AccessDeniedException`, ce qui corrige aussi les 500 actuels.
5. **Mode** : `pharma-smart.security.nav-access.mode` vaut `AUDIT` (journalise le refus et laisse
   passer), `ENFORCE` (bloque) ou `OFF`. Une bascule sans redéploiement de code, et un retour
   arrière immédiat.
6. **Garde-fou de complétude** : un test parcourt par réflexion toutes les méthodes des
   `@RestController`. Il échoue si l'une n'a ni `@RequiresNavAccess`, ni `@NavAccessExempt`, ni
   `@PreAuthorize`. Un nouvel endpoint ne peut plus être oublié.

### 3.3 Correctifs annexes

- **Swagger / OpenAPI** : public en profil `dev` seulement, `ROLE_ADMIN` sinon.
- **Jeton multi-rôle** : `DomainUserDetailsService` met tous les rôles de l'utilisateur. Ce qui
  reste lu dans le jeton (les `@PreAuthorize` sur `pr-*`) devient juste pour le multi-rôle.
- **Commentaire inexact** de `ExceptionTranslator` sur le 403, à corriger.

## 4. Cartographie à établir

Le cœur du travail est de rattacher chaque endpoint à ses codes. C'est un travail d'inventaire,
écran par écran :

1. Pour chaque route Angular, lister les appels HTTP de ses services (par recherche dans les
   `*.service.ts` et `*-api.service.ts`).
2. Pour chaque endpoint, relever les écrans appelants, donc ses codes `nav_item`.
3. Classer : code unique, codes multiples, exemption, admin.

Rattachement de départ, par grands domaines, à affiner lors de l'inventaire :

| Domaine (`web/rest/`) | Codes `nav_item` visés |
|---|---|
| `report/*` | `rapport-ventes.*`, `rapport-stock.*`, `rapport-finance.*`, `rapport-partners.*` |
| `dashboard/*`, `DashboardResource` | accueils par rôle, `ventes.kpi`, widgets (déjà couverts par `/api/dashboard-widgets`) |
| `sales/*`, `cash_register`, `payment_transaction` | `ventes`, `nouvelle-vente`, `nouvelle-prevente`, `mvt-caisse` et leurs sections |
| `facturation/*`, `reglement/*` | `factures`, `differes`, `tiers-payant`, `customer` |
| `commande/*`, `reassort` | `commande` et ses sections |
| `stock/*`, `inventaire/*`, `product_to_destroy` | `catalogue`, `peremptions`, `ajustements`, `inventaire`, `depot` |
| `referential/*`, `dci`, `prix_reference` | `referentiel` et ses routes (`fournisseurs`, `tva`, `remises`…) : **écritures** ; lectures exemptées |
| `settings/*`, `license`, `nav`, `scheduler` | `parametres`, `gestion-licence`, `nav-manager` (admin) |
| `declaration_ca/*` | déjà protégé par `pr-declaration-ca-*` : à laisser tel quel |
| `mobile/*` | autorités mobiles existantes (`PR_MOBILE_ADMIN`, `PR_MOBILE_USER`) : à vérifier |

## 5. Plan de réalisation

### Lot 0 — Socle (≈ 2 j)
- `@RequiresNavAccess`, `@NavAccessExempt`, `NavAccessAspect`, `NavAccessService` (avec cache et
  éviction), mode `AUDIT` / `ENFORCE` / `OFF`.
- 403 dans `ExceptionTranslator`, pour `AccessDeniedException` comme pour le nouveau refus.
- `WidgetAuthorizationService` réécrit sur `NavAccessService`.
- Test de complétude, d'abord en **liste d'attente** : les contrôleurs pas encore traités y sont
  tolérés, et la liste doit se vider lot après lot.
- Tests d'intégration : refus d'un caissier, union de deux rôles, droit retiré pris en compte
  sans reconnexion, mode audit qui laisse passer et journalise.

### Lot 1 — Rapports et tableaux de bord (≈ 2 j)
- Les 21 contrôleurs `report/*`, `DashboardResource`, `CaissierDashboardResource` et
  `ResponsableCommandeDashboardResource`.
- Le cas de l'accueil pharmacien est réglé par la migration `V2.1.10`.
- C'est la faille la plus grave : on commence par elle.

### Lot 2 — Référentiels (≈ 2 j)
- **Écritures** : le droit de l'écran du référentiel.
- **Lectures** : exemptées quand l'écran de vente ou de commande en a besoin, sinon le code du
  référentiel.

### Lot 3 — Ventes, caisse, facturation (≈ 3 j)
- Le plus délicat : le parcours de vente appelle beaucoup d'endpoints, pour tous les rôles de
  comptoir.
- Les opérations sensibles (annulation, remise hors barème, forçage de stock) s'appuient sur
  les `ACTION` existantes (`ventes.en-cours.delete`…).

### Lot 4 — Stock, commandes, inventaire (≈ 2 j)

### Lot 5 — Administration, paramètres, mobile (≈ 1 j)
- Swagger restreint.
- Jeton multi-rôle.
- Vérification des autorités mobiles.

### Lot 6 — Bascule et nettoyage (≈ 1 j, après la période d'audit)
- Lecture des journaux d'audit : chaque refus relevé est soit un droit oublié (migration), soit
  un vrai accès à bloquer.
- Passage en `ENFORCE`.
- Le test de complétude devient strict : liste d'attente vide.

**Total : ≈ 13 jours**, hors période d'audit en conditions réelles.

## 6. Tests et recette

- **Intégration (Testcontainers)** : un test par lot, qui prend pour chaque rôle (admin,
  pharmacien, caissier, vendeur, responsable commande) quelques endpoints représentatifs et
  vérifie autorisé / 403.
- **Complétude** : le test du § 3.2, qui échoue sur tout endpoint non classé.
- **E2E Playwright** : les campagnes existantes, jouées **en mode `ENFORCE`** avec chaque compte
  de démonstration. Un écran qui casse révèle un code manquant dans la cartographie. Réinitialiser
  la base de démonstration avant la campagne.
- **Recette métier** : une journée par rôle sur un poste pilote, en mode audit, puis lecture des
  journaux.

## 7. Déploiement

1. Livrer en **`AUDIT`** : aucun changement visible, les refus potentiels sont journalisés avec
   l'utilisateur, le rôle, l'endpoint et les codes attendus.
2. Laisser tourner une à deux semaines d'activité normale ; corriger les droits oubliés par
   migration.
3. Basculer en **`ENFORCE`** par la propriété, sans redéploiement de code.
4. Retour arrière : repasser en `AUDIT` le temps de corriger.

## 8. Décisions à prendre

1. **Lectures de référentiels** : les exempter (proposé), ou exiger un code même en lecture, au
   prix d'une cartographie plus fine du parcours de vente ?
2. **Durée de la période d'audit** avant la bascule.
3. **Swagger en production** : réservé à l'admin (proposé), ou supprimé ?
4. **Refus du dashboard** : aligner ses `GenericError` (400) sur le 403 commun, maintenant qu'on
   sait qu'un 403 ne déconnecte pas ?
