### Plan d'Amélioration du Module de Vente - PharmaSmart

Ce document détaille l'analyse du module de vente actuel et propose des pistes d'amélioration pour accroître la robustesse, la maintenabilité et la clarté du code.

#### 1. Analyse de l'Existant

Le module de vente est structuré autour de trois services principaux :
*   `SaleService` (implémenté par `SaleServiceImpl`) : Gère les ventes au comptant (VNO).
*   `ThirdPartySaleService` (implémenté par `ThirdPartySaleServiceImpl`) : Gère les ventes avec tiers-payant (Assurance/VO).
*   `SaleDepotExtensionService` : Gère les extensions de vente liées aux dépôts.

Ces services héritent ou utilisent `SaleCommonService`, qui regroupe la logique partagée (arrondis, calculs de base, gestion des remises).

#### 2. Points Faibles Identifiés

*   **Duplication de Logique (Violation DRY) :** Bien que `SaleCommonService` existe, on retrouve beaucoup de logique similaire mais légèrement différente dans `SaleServiceImpl` et `ThirdPartySaleServiceImpl` (ex: `updateItemQuantityRequested`, `addOrUpdateSaleLine`).
*   **Complexité de `ThirdPartySaleServiceImpl` :** Avec plus de 1000 lignes, cette classe devient difficile à maintenir. La logique de calcul des parts (assuré/tiers-payant) mélangée à la gestion de la persistance rend le code dense.
*   **Couplage aux Entités :** Les services manipulent directement beaucoup d'entités JPA (`Sales`, `SalesLine`, `ThirdPartySales`) au lieu de passer systématiquement par des DTOs ou des couches d'abstraction plus fines, ce qui complique les tests unitaires.
*   **Gestion des Erreurs :** Utilisation de `GenericError` pour beaucoup de cas. Un typage plus fin des exceptions permettrait une meilleure gestion côté Front-end.
*   **Synchronisation de l'Affichage Client :** Les appels à `displayNet` ou `displayMonnaie` sont parsemés dans les méthodes métiers, ce qui mélange logique métier et interaction périphérique.

#### 3. Pistes d'Améliorations

##### A. Refactorisation Architecturale
*   **Unification de la Logique de Ligne de Vente :** Créer un composant dédié à la manipulation des `SalesLine` qui soit indépendant du type de vente parent. Actuellement, `SalesLineService` est injecté mais la logique de mise à jour des totaux de la vente est répétée.
*   **Extraction de la Logique de Calcul :** Sortir la logique de calcul complexe de `ThirdPartySaleServiceImpl` vers des services purement fonctionnels (déjà commencé avec `TiersPayantCalculationService`, à poursuivre).
*   **Pattern Command/Strategy :** Utiliser un pattern Strategy pour les différents types de finalisation de vente (Cash vs Assurance vs Dépôt) afin d'alléger les services principaux.

##### B. Robustesse et Performance
*   **Optimisation des Requêtes :** Remplacer certains `getReferenceById` (qui peuvent causer des `LazyInitializationException` si mal gérés) par des chargements explicites (`fetch join`) quand on sait que les données seront nécessaires.
*   **Validation Déclarative :** Utiliser Bean Validation (JSR 303) plus largement sur les DTOs en entrée de service pour éviter les vérifications manuelles de `null` ou de cohérence de base.

##### C. Maintenabilité et Tests
*   **Réduction de la taille des classes :** Découper `ThirdPartySaleServiceImpl` en sous-services (ex: `ThirdPartyClientService`, `ThirdPartyCalculationManager`).
*   **Standardisation des DTOs :** Harmoniser les DTOs entre les ventes comptant et tiers-payant pour faciliter les traitements génériques en Front-end.

#### 4. Plan d'Action Proposé

1.  **Phase 1 (Nettoyage) :** Extraire les méthodes communes de mise à jour de quantités/prix dans un `SalesManager` partagé.
2.  **Phase 2 (Découpage) :** Scinder `ThirdPartySaleServiceImpl` pour isoler la gestion des tiers-payants de la gestion de la vente elle-même.
3.  **Phase 3 (Exceptions) :** Créer une hiérarchie d'exceptions métier claires pour le module vente (ex: `SaleValidationException`, `TiersPayantLimitException`).
4.  **Phase 4 (Interface) :** Isoler le service d'affichage client via un mécanisme d'événements (Spring Events) pour découpler la vente de l'appareil physique.

---
*Document généré le 21/01/2026 pour l'amélioration du module vente.*
