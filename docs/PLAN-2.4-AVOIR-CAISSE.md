# Plan 2.4 — Application automatique d'un avoir à la caisse

> **Statut** : À implémenter après validation des points 2.3, 2.5, 2.6, 3.2, 3.3, 3.4, 3.5  
> **Priorité** : Haute (flux de vente critique)  
> **Complexité** : Élevée (couplage fort entre le module vente et le module avoir)

---

## 1. Objectif

Permettre au pharmacien d'appliquer le solde d'un avoir client directement lors de l'encaissement d'une nouvelle vente. L'avoir vient en déduction du montant à payer. Il peut être utilisé entièrement ou partiellement (si son montant dépasse la vente, ou si le client ne souhaite l'utiliser que partiellement).

---

## 2. Flux utilisateur

```
1. Vente en cours → Client identifié
        │
        ▼
2. Badge "Avoirs disponibles : X FCFA" affiché dans la barre de paiement
        │
        ▼
3. Pharmacien clique sur le badge → Dialogue "Appliquer un avoir"
        │
        ▼
4. Liste des avoirs OUVERT du client (solde, date expiration)
   Sélection d'un avoir + montant à appliquer
        │
        ▼
5. Le montant de l'avoir est soustrait du total dû
   "Reste à payer : X FCFA"
        │
        ▼
6. Encaissement validé → l'avoir est clôturé (COMPENSATION_VENTE, partiel ou total)
   La vente est liée à l'avoir via une référence
```

---

## 3. Modifications backend

### 3.1 Nouveau DTO : `AvoirApplicationDTO`

**Fichier** : `service/sale/dto/AvoirApplicationDTO.java`

```java
public record AvoirApplicationDTO(
    Integer avoirId,
    String avoirReference,
    int montantDisponible,   // montantRestant de l'avoir
    int montantApplique      // montant effectivement prélevé sur cet avoir
) {}
```

### 3.2 Extension de `SalePaymentDTO` (ou `EncaissementRequest`)

Ajouter un champ optionnel pour la liste des avoirs à appliquer :

```java
// Dans EncaissementRequest ou SalePaymentRequest :
List<AvoirApplicationDTO> avoirs;  // nullable/empty si pas d'avoir
```

### 3.3 Endpoint : avoirs ouverts d'un client

**Existant** (déjà créé en 3.5) :
```
GET /api/sales/avoirs/documents/by-customer/{customerId}
```
Filtrer côté backend par `statut = OUVERT` et `dateExpiration >= today` uniquement.

**Nouveau endpoint dédié POS** (retourne seulement les avoirs utilisables) :
```
GET /api/sales/avoirs/documents/utilisables/{customerId}
```

Retourne uniquement les avoirs `OUVERT` non expirés avec `montantRestant > 0`.

**Fichier** : `AvoirClientResource.java` — ajouter :

```java
@GetMapping("/sales/avoirs/documents/utilisables/{customerId}")
public ResponseEntity<List<AvoirClientDocumentDTO>> getAvoirsUtilisables(
    @PathVariable Integer customerId
) {
    return ResponseEntity.ok(avoirClientDocumentService.findAvoirsUtilisablesByCustomer(customerId));
}
```

**Service** : `AvoirClientDocumentServiceImpl` — ajouter :

```java
public List<AvoirClientDocumentDTO> findAvoirsUtilisablesByCustomer(Integer customerId) {
    return avoirClientRepository
        .findByCustomerIdAndStatutAndDateExpirationGreaterThanEqualOrDateExpirationIsNull(
            customerId, AvoirClientStatut.OUVERT, LocalDate.now())
        .stream()
        .filter(a -> a.getMontantRestant() > 0)
        .map(this::toDocumentDTO)
        .toList();
}
```

### 3.4 Logique d'application dans `SaleService` / `EncaissementService`

**Fichier** : Service d'encaissement existant (à identifier — probablement `SalesService` ou un service payment dédié)

Lors de la validation de l'encaissement :

```java
// Pour chaque avoir demandé
for (AvoirApplicationDTO avoirApp : request.getAvoirs()) {
    AvoirClient avoir = avoirClientRepository.findById(avoirApp.avoirId()).orElseThrow();
    
    // Vérifications
    if (avoir.getStatut() != AvoirClientStatut.OUVERT) throw new ...;
    if (avoir.getMontantRestant() < avoirApp.montantApplique()) throw new ...;
    if (!avoir.getCustomer().getId().equals(sale.getCustomer().getId())) throw new ...;
    
    // Application : crée une utilisation
    AvoirClientUtilisation utilisation = new AvoirClientUtilisation();
    utilisation.setAvoirClient(avoir);
    utilisation.setMontantUtilise(avoirApp.montantApplique());
    utilisation.setUtiliseLe(LocalDate.now());
    utilisation.setCommentaire("Compensation vente " + sale.getNumberTransaction());
    utilisation.setUtilisePar(securityUtils.getCurrentUser());
    avoirClientUtilisationRepository.save(utilisation);
    
    // Mise à jour de l'avoir
    int nouveauMontantUtilise = avoir.getMontantUtilise() + avoirApp.montantApplique();
    avoir.setMontantUtilise(nouveauMontantUtilise);
    if (nouveauMontantUtilise >= avoir.getMontant()) {
        avoir.setStatut(AvoirClientStatut.CLOTURE);
        avoir.setModeCloture(ModeClotureAvoir.COMPENSATION_VENTE);
        avoir.setClotureLe(LocalDate.now());
    }
    avoirClientRepository.save(avoir);
    
    // Lien vente → avoir (colonne à ajouter via migration)
    sale.getAvoirsAppliques().add(avoir);  // ou champ echangeAvoirRef
}
```

### 3.5 Migration Flyway

**Fichier** : `V1.6.16__sale_avoir_compensation.sql`

```sql
-- Lien entre une vente et les avoirs appliqués lors de l'encaissement
CREATE TABLE IF NOT EXISTS sale_avoir_application (
    id              integer generated by default as identity primary key,
    sale_id         bigint NOT NULL REFERENCES sales(id),
    avoir_client_id integer NOT NULL REFERENCES avoir_client(id),
    montant_applique integer NOT NULL,
    applique_le      date NOT NULL DEFAULT CURRENT_DATE
);

CREATE INDEX IF NOT EXISTS idx_sale_avoir_sale_id ON sale_avoir_application(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_avoir_avoir_id ON sale_avoir_application(avoir_client_id);
```

### 3.6 Extension de `SaleDTO` / `ISales`

Ajouter un champ pour afficher les avoirs appliqués sur une vente :

```java
// Dans SaleDTO ou ISales response :
List<AvoirApplicationDTO> avoirsAppliques;
int montantAvoirTotal;  // somme des montants appliqués
```

---

## 4. Modifications frontend

### 4.1 Nouveau service : `AvoirClientApiService.getAvoirsUtilisables()`

```typescript
getAvoirsUtilisables(customerId: number): Observable<IAvoirClientDocument[]> {
  return this.http.get<IAvoirClientDocument[]>(
    `${this.resourceUrl}/documents/utilisables/${customerId}`
  );
}
```

### 4.2 Nouveau composant : `ApplyAvoirModalComponent`

**Fichier** : `features/sales/ui/apply-avoir-modal/apply-avoir-modal.component.ts`

Responsabilités :
- Afficher la liste des avoirs OUVERT du client (solde, expiration)
- Permettre la sélection d'un avoir
- Saisir le montant à appliquer (par défaut = min(montantRestant, montantVente))
- Retourner `{ avoirId, montantApplique }` à la modal parente

### 4.3 Intégration dans le flux de paiement

Dans la modal de paiement/encaissement existante (identifier le composant — probablement dans `features/sales/ui/` ou `entities/sales/selling-home/`):

```typescript
// Lors de l'affectation d'un client à la vente
private checkAvoirsClient(customerId: number): void {
  this.avoirApi.getAvoirsUtilisables(customerId)
    .subscribe(avoirs => {
      this.avoirsDisponibles.set(avoirs);
      this.totalAvoirsDisponibles.set(avoirs.reduce((s, a) => s + (a.montantRestant ?? 0), 0));
    });
}
```

Dans le template, afficher un badge cliquable :

```html
@if (totalAvoirsDisponibles() > 0) {
  <button class="btn btn-outline-warning btn-sm" (click)="openApplyAvoirModal()">
    <i class="pi pi-ticket me-1"></i>
    Avoir disponible : {{ totalAvoirsDisponibles() | number:'1.0-0' }} FCFA
  </button>
}
```

### 4.4 Extension du `EncaissementRequest` frontend

```typescript
export interface IAvoirApplicationItem {
  avoirId: number;
  montantApplique: number;
}

// Dans EncaissementRequest :
avoirs?: IAvoirApplicationItem[];
```

### 4.5 Affichage sur le ticket de caisse / reçu

- Ajouter une ligne "Avoir appliqué : -X FCFA" dans le reçu ESC/POS
- Modifier `SaleReceiptService` (backend) pour inclure les avoirs appliqués

---

## 5. Contraintes et précautions

| Contrainte | Solution |
|---|---|
| Concurrence (deux postes utilisent le même avoir) | Verrou optimiste sur `montant_utilise` (version column) ou SELECT FOR UPDATE |
| Avoir expiré entre la sélection et la validation | Re-vérifier le statut au moment de la validation côté backend |
| Avoir insuffisant (race condition) | Vérification `montantRestant >= montantApplique` dans la transaction |
| Client sans compte (vente anonyme) | Désactiver le bouton "Avoir disponible" si pas de client identifié |
| Plusieurs avoirs simultanés | Autoriser (liste) mais vérifier que la somme ne dépasse pas le montant dû |
| Remboursement d'une vente avec avoir | Recalculer le remboursement en tenant compte des avoirs appliqués |

---

## 6. Ordre d'implémentation recommandé

1. **Migration SQL** (`V1.6.16`) — table de liaison
2. **Entité `SaleAvoirApplication`** + repository
3. **Service** : `findAvoirsUtilisablesByCustomer()` + logique d'application dans l'encaissement
4. **REST** : endpoint `/utilisables/{customerId}` + extension du payload encaissement
5. **Frontend service** : `getAvoirsUtilisables()`
6. **Frontend** : `ApplyAvoirModalComponent`
7. **Frontend** : intégration dans la modal de paiement
8. **Tests** : cas de rachat partiel, expiration, concurrence

---

## 7. Fichiers à créer / modifier

| Fichier | Type |
|---|---|
| `V1.6.16__sale_avoir_compensation.sql` | Migration |
| `domain/SaleAvoirApplication.java` | Entité |
| `repository/SaleAvoirApplicationRepository.java` | Repository |
| `service/sale/dto/AvoirApplicationDTO.java` | DTO |
| `service/sale/impl/AvoirClientDocumentServiceImpl.java` | Modification |
| `web/rest/sales/AvoirClientResource.java` | Modification |
| Service d'encaissement (à identifier) | Modification |
| `features/sales/data-access/services/avoir-client-api.service.ts` | Modification |
| `features/sales/ui/apply-avoir-modal/` | Création |
| Modal de paiement (à identifier) | Modification |
