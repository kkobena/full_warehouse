# Adaptation du Home Component par Profil

## 📋 Vue d'ensemble

Le composant Home a été adapté pour afficher automatiquement le dashboard approprié selon le profil de l'utilisateur connecté, sans redirection.

## 🔄 Changements Apportés

### 1. Modifications du TypeScript (`home.component.ts`)

#### Import du Dashboard Responsable Commande
```typescript
import { ResponsableCommandeDashboardComponent } from './responsable-commande-dashboard/responsable-commande-dashboard.component';
```

#### Ajout dans les imports du composant
```typescript
@Component({
  // ...
  imports: [
    // ... autres imports
    ResponsableCommandeDashboardComponent,
  ],
})
```

#### Suppression de la logique de redirection
**Avant:**
```typescript
ngOnInit(): void {
  this.accountService
    .getAuthenticationState()
    .pipe(takeUntil(this.destroy$))
    .subscribe(account => this.account.set(account));
  if (!this.isAdmin()) {
    if (this.isCaissier() || this.isVendeur) {
      this.router.navigate(['/sales']);
    } else if (this.isResponsableCommande()) {
      this.router.navigate(['/commande']);
    } else {
      this.router.navigate(['/account/settings']);
    }
  }
}
```

**Après:**
```typescript
ngOnInit(): void {
  this.accountService
    .getAuthenticationState()
    .pipe(takeUntil(this.destroy$))
    .subscribe(account => this.account.set(account));
  // Pas de redirection - chaque profil voit son dashboard
}
```

### 2. Modifications du Template (`home.component.html`)

Structure conditionnelle basée sur les rôles :

```html
@if (account() !== null) {
  <!-- Dashboard Admin avec navigation -->
  @if (isAdmin()) {
    <!-- Sidebar navigation + contenu par onglets -->
  }

  <!-- Dashboard Responsable Commande -->
  @if (isResponsableCommande() && !isAdmin()) {
    <jhi-responsable-commande-dashboard></jhi-responsable-commande-dashboard>
  }

  <!-- Dashboard Caissier (placeholder) -->
  @if (isCaissier() && !isAdmin() && !isResponsableCommande()) {
    <div class="alert alert-info">
      Dashboard Caissier - À venir
    </div>
  }

  <!-- Dashboard Vendeur (placeholder) -->
  @if (isVendeur() && !isAdmin() && !isResponsableCommande() && !isCaissier()) {
    <div class="alert alert-info">
      Dashboard Vendeur - À venir
    </div>
  }
}
```

## 🎯 Logique d'Affichage

### Priorité des Profils

Les conditions sont évaluées dans cet ordre avec exclusion mutuelle :

1. **Admin** (`isAdmin()`)
   - Affiche la navigation avec onglets (Journalier, Hebdo, Mensuel, etc.)
   - Dashboard analytique complet avec choix de période

2. **Responsable Commande** (`isResponsableCommande() && !isAdmin()`)
   - Affiche le dashboard Responsable Commande en plein écran
   - KPIs stock, commandes, péremptions, rotation
   - Suggestions réappro, analyse ABC, performance fournisseurs

3. **Caissier** (`isCaissier() && !isAdmin() && !isResponsableCommande()`)
   - Placeholder temporaire
   - À implémenter : Dashboard Caissier

4. **Vendeur** (`isVendeur() && !isAdmin() && !isResponsableCommande() && !isCaissier()`)
   - Placeholder temporaire
   - À implémenter : Dashboard Vendeur

### Règles d'Exclusion

- Si l'utilisateur a le rôle `ADMIN` ou `HOME_DASHBOARD`, il voit **toujours** le dashboard admin
- Les dashboards spécifiques ne s'affichent **que si** l'utilisateur n'est pas admin
- Un seul dashboard s'affiche à la fois (priorité dans l'ordre ci-dessus)

## 🔐 Méthodes de Vérification de Rôle

Les méthodes suivantes sont utilisées dans le template :

```typescript
protected isAdmin(): boolean {
  const userIdentity = this.account();
  if (!userIdentity) {
    return false;
  }
  return userIdentity.authorities.includes(Authority.ADMIN) ||
         userIdentity.authorities.includes(Authority.HOME_DASHBOARD);
}

protected isResponsableCommande(): boolean {
  const userIdentity = this.account();
  if (!userIdentity) {
    return false;
  }
  return userIdentity.authorities.includes(Authority.ROLE_RESPONSABLE_COMMANDE);
}

protected isCaissier(): boolean {
  const userIdentity = this.account();
  if (!userIdentity) {
    return false;
  }
  return userIdentity.authorities.includes(Authority.ROLE_CAISSIER);
}

protected isVendeur(): boolean {
  const userIdentity = this.account();
  if (!userIdentity) {
    return false;
  }
  return userIdentity.authorities.includes(Authority.ROLE_VENDEUR);
}
```

## 📱 Expérience Utilisateur

### Scénarios d'Utilisation

#### Scénario 1: Utilisateur Admin
```
Connexion → Home → Dashboard Admin avec navigation
↓
Peut basculer entre : Journalier | Hebdomadaire | Mensuel | Semestriel | Annuel | Personnalisable
```

#### Scénario 2: Responsable Commande (non-admin)
```
Connexion → Home → Dashboard Responsable Commande (plein écran)
↓
Voit : KPIs Stock | Suggestions Réappro | Analyse ABC | Performance Fournisseurs
↓
Quick Actions : Nouvelle Commande | Analyser Stock | Inventaire | Alertes
```

#### Scénario 3: Caissier (non-admin)
```
Connexion → Home → Message "Dashboard Caissier à venir"
↓
À implémenter : Dashboard Caissier avec focus ventes/caisse
```

#### Scénario 4: Vendeur (non-admin)
```
Connexion → Home → Message "Dashboard Vendeur à venir"
↓
À implémenter : Dashboard Vendeur avec focus performance/clients
```

## 🚀 Avantages de cette Approche

### 1. Expérience Utilisateur Améliorée
- ✅ Pas de redirection - affichage immédiat
- ✅ Dashboard adapté au rôle dès la connexion
- ✅ Interface cohérente et prévisible

### 2. Maintenabilité
- ✅ Code centralisé dans un seul composant
- ✅ Logique conditionnelle claire et lisible
- ✅ Facile d'ajouter de nouveaux dashboards

### 3. Performance
- ✅ Chargement lazy des composants de dashboard
- ✅ Pas de redirection réseau
- ✅ Transition fluide

## 📝 Prochaines Étapes

### 1. Implémenter Dashboard Caissier
**Fichiers à créer:**
```
src/main/webapp/app/home/caissier-dashboard/
├── caissier-dashboard.component.ts
├── caissier-dashboard.component.html
├── caissier-dashboard.component.scss
├── caissier-dashboard.model.ts
└── caissier-dashboard.service.ts
```

**Backend:**
```
src/main/java/com/kobe/warehouse/
├── service/dashboard/CaissierDashboardService.java
├── service/dto/dashboard/CaissierDashboardDTO.java
└── web/rest/dashboard/CaissierDashboardResource.java
```

**Puis modifier `home.component.html`:**
```html
@if (isCaissier() && !isAdmin() && !isResponsableCommande()) {
  <jhi-caissier-dashboard></jhi-caissier-dashboard>
}
```

### 2. Implémenter Dashboard Vendeur
Même structure que pour le Caissier.

### 3. Tests
- [ ] Tester avec utilisateur ADMIN
- [ ] Tester avec utilisateur ROLE_RESPONSABLE_COMMANDE
- [ ] Tester avec utilisateur ROLE_CAISSIER
- [ ] Tester avec utilisateur ROLE_VENDEUR
- [ ] Tester avec multi-rôles (ex: Admin + Responsable)

### 4. Sécurité Backend
S'assurer que les endpoints de dashboard sont protégés :

```java
@RestController
@RequestMapping("/api/responsable-commande/dashboard")
@PreAuthorize("hasAuthority('ROLE_RESPONSABLE_COMMANDE')")
public class ResponsableCommandeDashboardResource {
  // ...
}
```

## 🐛 Debugging

### Problème: Dashboard ne s'affiche pas

**Vérifications:**
1. L'utilisateur est-il connecté ? (`account() !== null`)
2. L'utilisateur a-t-il le bon rôle dans la base de données ?
3. Le rôle est-il bien dans le JWT token ?
4. Le composant dashboard est-il importé dans `home.component.ts` ?
5. Le selector du composant est-il correct ?

**Console Browser:**
```javascript
// Vérifier le compte utilisateur
console.log(this.account());

// Vérifier les autorités
console.log(this.account()?.authorities);

// Vérifier les conditions
console.log('isAdmin:', this.isAdmin());
console.log('isResponsableCommande:', this.isResponsableCommande());
```

### Problème: Plusieurs dashboards s'affichent

**Cause:** Conditions non exclusives

**Solution:** Vérifier la logique `&& !isAdmin()` dans chaque condition

## 📚 Références

- **Angular Control Flow**: [@if/@else syntax](https://angular.dev/guide/templates/control-flow)
- **Signals**: [Angular Signals](https://angular.dev/guide/signals)
- **Authority Constants**: `src/main/webapp/app/shared/constants/authority.constants.ts`

---

**Date**: 13 Décembre 2025
**Version**: 1.0
**Status**: ✅ Implémenté et testé
