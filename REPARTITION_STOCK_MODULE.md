# Module de Gestion de Répartition de Stock

## Vue d'ensemble

Module complet pour gérer les répartitions de stock entre différents emplacements de stockage (rayon/réserve) avec suggestions automatiques et interface éditable en temps réel.

## Fonctionnalités

### 1. **Historique des Répartitions**
- Consultation de l'historique complet des mouvements de stock
- Filtres avancés: date, utilisateur, produit, emplacement
- Affichage des stocks avant/après mouvement
- Pagination et recherche en temps réel

### 2. **Suggestions de Réassort Rayon**
- Suggestions automatiques pour réapprovisionner les rayons
- Basées sur le stock minimum et le stock disponible en réserve
- Table éditable avec navigation clavier
- Validation en un clic pour exécuter le mouvement

### 3. **Suggestions de Réassort Réserve**
- Suggestions pour réapprovisionner la réserve
- Calcul automatique basé sur le seuil minimum
- Interface identique aux suggestions rayon

## Navigation au Clavier (Tableau Éditable)

### Touches Disponibles:
- **Entrée**: Sauvegarde et passe à la ligne suivante
- **Flèche Bas ↓**: Sauvegarde et descend d'une ligne
- **Flèche Haut ↑**: Sauvegarde et monte d'une ligne
- **Échap**: Annule la modification
- **+/-**: Incrémente/décrémente la quantité (boutons spinner)

### Workflow Rapide:
1. Cliquer sur l'icône crayon ou double-cliquer sur la ligne
2. Modifier la quantité avec les flèches ou saisie directe
3. Appuyer sur Entrée pour sauvegarder et passer à la ligne suivante
4. Continuer avec les flèches haut/bas pour naviguer

## Structure des Fichiers

### Backend (Java/Spring Boot)

#### Domaine
- `LigneReassort.java` - Ligne de suggestion de réassort
- `SuggestionReassort.java` - Suggestion de réassort
- `RepartitionStockProduit.java` - Historique de mouvement

#### Services
- `RepartitionStockService.java` - Interface du service
- `RepartitionStockServiceImpl.java` - Implémentation avec requêtes dynamiques
- `SuggestionReassortService.java` - Gestion des suggestions
- `SuggestionReassortServiceImpl.java` - Implémentation

#### Repositories
- `RepartitionStockProduitRepository.java` - Repository JPA
- `RepartitionStockProduitRepositoryCustom.java` - Interface custom
- `RepartitionStockProduitRepositoryImpl.java` - Implémentation custom avec requêtes natives dynamiques

#### REST Controllers
- `RepartitionStockResource.java` - Endpoints pour l'historique
- `SuggestionReassortResource.java` - Endpoints pour les suggestions

#### DTOs
- `RepartitionStockProduitDto.java` - DTO pour l'historique
- `SuggestionReassortDto.java` - DTO pour les suggestions
- `LigneReassortDto.java` - DTO pour les lignes
- `RepartionSearchQueryDto.java` - DTO pour les filtres de recherche

### Frontend (Angular 20)

#### Composants
```
repartition-stock/
├── repartition-stock.component.ts/html/scss    # Composant principal avec toolbar et tabs
├── repartition-list/
│   ├── repartition-list.component.ts/html/scss # Liste de l'historique
├── suggestion-reassort/
│   ├── suggestion-reassort.component.ts/html/scss # Tableau éditable avec navigation
├── repartition-stock.model.ts                   # Modèles TypeScript
├── repartition-stock.service.ts                 # Service HTTP
└── repartition-stock.route.ts                   # Configuration de routing
```

## Endpoints API

### Historique des Répartitions
```http
GET /api/repartition-stock
```

**Paramètres:**
- `storageId` (optionnel): ID de l'emplacement
- `userId` (optionnel): ID de l'utilisateur
- `searchTerm` (optionnel): Recherche dans nom/code produit
- `dateDebut` (optionnel): Date de début (ISO-8601)
- `dateFin` (optionnel): Date de fin (ISO-8601)
- `typeRepartition` (optionnel): RAYON | RESERVE
- `stockProduitId` (optionnel): ID du stock produit
- `page`, `size`: Pagination

**Réponse:** Liste paginée de `RepartitionStockProduitDto`

### Suggestions Ouvertes
```http
GET /api/suggestion-reassort/open?typeReassort=RAYON
```

**Réponse:** Liste de `SuggestionReassortDto`

### Mettre à Jour une Ligne
```http
PUT /api/suggestion-reassort/ligne/{id}
Content-Type: application/json

{
  "quantity": 50
}
```

### Supprimer une Ligne
```http
DELETE /api/suggestion-reassort/ligne/{id}
```

### Valider une Suggestion
```http
POST /api/suggestion-reassort/{id}/validate
```

### Supprimer une Suggestion
```http
DELETE /api/suggestion-reassort/{id}
```

## Requêtes SQL Dynamiques

Le module utilise des requêtes SQL natives construites dynamiquement pour optimiser les performances:

```java
// Exemple de clause WHERE dynamique
WHERE 1=1
  AND (userId IS NOT NULL ? r.user_id = :userId)
  AND (searchTerm IS NOT NULL ? UPPER(p.libelle) LIKE UPPER(CONCAT('%', :searchTerm, '%')))
  AND (dateDebut IS NOT NULL ? CAST(r.created_at AS date) >= :dateDebut)
  ...
```

**Avantages:**
- Pas de paramètres null dans les requêtes
- WHERE clause optimisée selon les filtres actifs
- Performance maximale avec index appropriés

## Charte Graphique

Le module respecte la charte commune du projet:

### Classes CSS Utilisées:
- `.pharma-toolbar` - Barre d'outils avec filtres
- `.pharma-table` - Tables avec style uniforme
- `.pharma-table-head` - En-têtes de table
- `.pharma-entity-name` - Noms d'entités
- `.pharma-code` - Codes produits
- `.pharma-qty-value` - Valeurs de quantité
- `.pharma-qty-highlight` - Quantités en édition (bleu, gras)
- `.pharma-suggestion-card` - Cartes de suggestion
- `.pharma-suggestion-header` - En-tête avec gradient violet

### Composants PrimeNG:
- `p-table` avec `editMode="row"`
- `p-inputNumber` avec `showButtons`
- `p-button` avec variantes raised/rounded/text
- `p-tag` pour les badges
- `p-tabs` pour les onglets
- `p-toast` pour les notifications
- `p-confirmDialog` pour les confirmations

## Intégration dans le Projet

### 1. Ajouter la Route
Dans `src/main/webapp/app/entities/entity-routing.module.ts`:

```typescript
{
  path: 'repartition-stock',
  loadChildren: () => import('./repartition-stock/repartition-stock.route'),
  data: { pageTitle: 'Répartition de Stock' },
}
```

### 2. Ajouter au Menu
Dans la base de données, table `menu`:

```sql
INSERT INTO menu (libelle, root, menu_group_id, position, authority_name)
VALUES ('Répartition Stock', false, (SELECT id FROM menu_group WHERE libelle = 'Gestion Stock'), 5, 'ROLE_USER');
```

## Tests

### Tests Backend (JUnit)
```bash
./mvnw test -Dtest=RepartitionStockServiceImplTest
./mvnw test -Dtest=RepartitionStockResourceTest
```

### Tests Frontend (Jest)
```bash
npm test -- repartition-stock
```

## Optimisations Futures

1. **Cache Redis** pour les suggestions fréquentes
2. **WebSocket** pour mise à jour en temps réel
3. **Export Excel/PDF** de l'historique
4. **Graphiques** de tendances de répartition
5. **Suggestions ML** basées sur l'historique

## Support et Maintenance

Pour toute question ou bug:
1. Vérifier les logs: `tail -f logs/application.log`
2. Vérifier la console navigateur (F12)
3. Tester les endpoints avec Postman/curl

---

**Version:** 1.0.0
**Dernière mise à jour:** 2025-12-13
**Auteur:** Généré par Claude Code
