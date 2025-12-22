# Plan d'Implémentation - Modèle SEMOIS
## Stock Économique Mensuel d'Objectif Interne de Sécurité

**Date:** 2025-12-20
**Version:** 1.0
**Spécifique:** Pharmacie d'Officine

---

## 📖 PRÉSENTATION DU MODÈLE SEMOIS

### Définition
Le **SEMOIS** est une méthode française de gestion des stocks spécialement conçue pour les pharmacies d'officine. Elle vise à optimiser le stock en combinant:
- **Performance économique** (minimiser coûts)
- **Sécurité d'approvisionnement** (éviter ruptures)
- **Adaptation aux spécificités pharmaceutiques** (péremption, rotation, saisonnalité)

### Principes Fondamentaux

```
SEMOIS = Ventes Mensuelles Moyennes + Marge de Sécurité

Où:
- Ventes Mensuelles Moyennes (VMM) = Moyenne glissante sur N mois
- Marge de Sécurité = f(Variabilité, Délai, Criticité produit)
```

### Avantages par rapport à EOQ/ABC Classique
✅ **Adapté pharmacie:** Prend en compte spécificités réglementaires
✅ **Simple à comprendre:** Basé sur ventes mensuelles (familier)
✅ **Flexible:** S'adapte facilement aux variations saisonnières
✅ **Sécurité:** Priorité sur disponibilité produits critiques
✅ **Gestion péremption:** Intègre rotation et DLU

---

## 🎯 FORMULATION MATHÉMATIQUE

### 1. Calcul des Ventes Mensuelles Moyennes (VMM)

#### Méthode Recommandée: Moyenne Mobile Pondérée (MMP)

```
VMM = (V₁×P₁ + V₂×P₂ + ... + Vₙ×Pₙ) / (P₁ + P₂ + ... + Pₙ)

Où:
- Vᵢ = Ventes du mois i
- Pᵢ = Poids du mois i (mois récent = poids plus élevé)
- n = Nombre de mois considérés (recommandé: 6 ou 12)

Exemple de pondération (6 mois):
- Mois actuel-1: Poids 6
- Mois actuel-2: Poids 5
- Mois actuel-3: Poids 4
- Mois actuel-4: Poids 3
- Mois actuel-5: Poids 2
- Mois actuel-6: Poids 1
```

### 2. Calcul de la Marge de Sécurité

#### Formule de Base

```
Marge de Sécurité = VMM × (Délai + Coefficient de Sécurité) / 30

Où:
- Délai = Délai de livraison fournisseur (en jours)
- Coefficient de Sécurité = Facteur selon criticité (voir tableau)
```

#### Coefficient de Sécurité par Classe de Produit

| Classe | Description | Coefficient | Exemples |
|--------|-------------|-------------|----------|
| **A+** | Produits vitaux | **1.5** | Insulines, anticoagulants, antibio critiques |
| **A** | Forte rotation | **1.0** | Paracétamol, AINS courants |
| **B** | Rotation moyenne | **0.7** | Vitamines, compléments |
| **C** | Faible rotation | **0.4** | Produits de niche |
| **D** | Très faible rotation | **0.2** | Produits obsolescents |

### 3. Stock Objectif SEMOIS

```
Stock Objectif = VMM + Marge de Sécurité
              = VMM × (1 + (Délai × Coefficient) / 30)

Quantité à Commander = Stock Objectif - Stock Actuel (si > 0)
```

### 4. Ajustements Spécifiques

#### a) Ajustement Saisonnier

```
VMM_Ajusté = VMM × Facteur_Saisonnier

Exemples:
- Décembre (grippe): Facteur = 1.4
- Juillet (vacances): Facteur = 0.7
```

#### b) Ajustement Péremption

```
Si DLU < 6 mois:
    Stock Objectif = MIN(Stock Objectif, VMM × 3)
    # Limiter à 3 mois de stock pour éviter péremption
```

#### c) Ajustement Promotion

```
Si Promotion Prévue:
    VMM_Ajusté = VMM × (1 + Taux_Augmentation_Estimé)
    # Ex: Promo -20% → Taux = 0.5 (hausse 50% ventes)
```

---

## 🏗️ ARCHITECTURE TECHNIQUE

### Schéma de Données

```sql
-- Nouvelle table pour historique ventes mensuelles agrégées
-- Note: Utilise IDENTITY (PostgreSQL 10+) au lieu de SERIAL (déprécié PostgreSQL 18+)
CREATE TABLE ventes_mensuelles_agregees (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    produit_id INTEGER NOT NULL REFERENCES produit(id),
    annee_mois VARCHAR(7) NOT NULL, -- Format: '2025-12'
    quantite_vendue INTEGER NOT NULL DEFAULT 0,
    montant_ca INTEGER NOT NULL DEFAULT 0,
    nombre_ventes INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(produit_id, annee_mois)
);

CREATE INDEX idx_ventes_mensuelles_produit ON ventes_mensuelles_agregees(produit_id);
CREATE INDEX idx_ventes_mensuelles_date ON ventes_mensuelles_agregees(annee_mois);

-- Nouvelle table pour paramètres SEMOIS
-- Note: Utilise IDENTITY (PostgreSQL 10+) au lieu de SERIAL (déprécié PostgreSQL 18+)
CREATE TABLE semois_configuration (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    produit_id INTEGER UNIQUE REFERENCES produit(id),
    classe_criticite VARCHAR(3) NOT NULL DEFAULT 'B', -- A+, A, B, C, D
    coefficient_securite DECIMAL(3,2) NOT NULL DEFAULT 1.0,
    nb_mois_historique INTEGER NOT NULL DEFAULT 6, -- Nombre de mois pour calcul VMM
    delai_livraison_jours INTEGER NOT NULL DEFAULT 7,
    stock_objectif_calcule INTEGER, -- Cache du dernier calcul
    vmm_calcule INTEGER, -- VMM calculé
    date_dernier_calcul TIMESTAMP,
    facteur_saisonnier_actuel DECIMAL(3,2) DEFAULT 1.0,
    limite_peremption BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Vue matérialisée pour calcul SEMOIS temps réel
CREATE MATERIALIZED VIEW mv_semois_suggestion AS
SELECT
    p.id AS produit_id,
    p.libelle,
    fp.code_cip,
    sc.classe_criticite,
    sc.coefficient_securite,
    sc.delai_livraison_jours,

    -- VMM (Ventes Mensuelles Moyennes)
    COALESCE(
        (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / SUM(7 - row_num)
         FROM (
             SELECT quantite_vendue,
                    ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
             FROM ventes_mensuelles_agregees
             WHERE produit_id = p.id
               AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
         ) vma
         WHERE vma.row_num <= 6),
        0
    ) AS vmm,

    -- Marge de sécurité
    COALESCE(
        (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / SUM(7 - row_num)
         FROM (
             SELECT quantite_vendue,
                    ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
             FROM ventes_mensuelles_agregees
             WHERE produit_id = p.id
               AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
         ) vma
         WHERE vma.row_num <= 6),
        0
    ) * (sc.delai_livraison_jours * sc.coefficient_securite / 30.0) AS marge_securite,

    -- Stock objectif
    COALESCE(
        (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / SUM(7 - row_num)
         FROM (
             SELECT quantite_vendue,
                    ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
             FROM ventes_mensuelles_agregees
             WHERE produit_id = p.id
               AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
         ) vma
         WHERE vma.row_num <= 6),
        0
    ) * (1 + (sc.delai_livraison_jours * sc.coefficient_securite / 30.0)) AS stock_objectif,

    -- Stock actuel
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_actuel,

    -- Quantité à commander
    GREATEST(
        0,
        COALESCE(
            (SELECT SUM(vma.quantite_vendue * (7 - row_num)) / SUM(7 - row_num)
             FROM (
                 SELECT quantite_vendue,
                        ROW_NUMBER() OVER (ORDER BY annee_mois DESC) as row_num
                 FROM ventes_mensuelles_agregees
                 WHERE produit_id = p.id
                   AND annee_mois >= TO_CHAR(NOW() - INTERVAL '6 months', 'YYYY-MM')
             ) vma
             WHERE vma.row_num <= 6),
            0
        ) * (1 + (sc.delai_livraison_jours * sc.coefficient_securite / 30.0))
        - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
    ) AS quantite_a_commander

FROM produit p
LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id AND fp.principal = TRUE
LEFT JOIN semois_configuration sc ON sc.produit_id = p.id
LEFT JOIN stock_produit sp ON sp.produit_id = p.id
WHERE p.status = 'ENABLE'
  AND p.type_produit != 'DETAIL'
GROUP BY p.id, p.libelle, fp.code_cip, sc.classe_criticite,
         sc.coefficient_securite, sc.delai_livraison_jours;

CREATE INDEX idx_mv_semois_produit ON mv_semois_suggestion(produit_id);
CREATE INDEX idx_mv_semois_classe ON mv_semois_suggestion(classe_criticite);
```

### 📝 Note de Compatibilité PostgreSQL

**PostgreSQL 18+ : IDENTITY vs SERIAL**

Les tables ci-dessus utilisent la syntaxe moderne `GENERATED ALWAYS AS IDENTITY` au lieu de `SERIAL`:

| Ancienne Syntaxe (Déprécié) | Nouvelle Syntaxe (PostgreSQL 10+) |
|------------------------------|-------------------------------------|
| `id SERIAL PRIMARY KEY` | `id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY` |
| `id BIGSERIAL PRIMARY KEY` | `id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY` |

**Avantages de IDENTITY:**
- ✅ Conforme au standard SQL:2003
- ✅ Meilleur contrôle avec `ALWAYS` vs `BY DEFAULT`
- ✅ Syntaxe plus claire et explicite
- ✅ Compatibilité future garantie

**Migration depuis SERIAL (si nécessaire):**
```sql
-- Si vos tables existantes utilisent SERIAL, vous pouvez les migrer:
ALTER TABLE table_name
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;

-- Supprimer l'ancienne séquence (optionnel)
DROP SEQUENCE IF EXISTS table_name_id_seq;
```

**Pour JPA/Hibernate:**
Aucun changement nécessaire côté Java! La stratégie `GenerationType.IDENTITY` fonctionne avec les deux syntaxes:
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Integer id;
```

---

## 💻 IMPLÉMENTATION BACKEND

### 1. Entités JPA

#### SemoisConfiguration.java

```java
package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "semois_configuration")
public class SemoisConfiguration implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @NotNull
    private Produit produit;

    @Enumerated(EnumType.STRING)
    @Column(name = "classe_criticite", length = 3, nullable = false)
    private ClasseCriticite classeCriticite = ClasseCriticite.B;

    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("2.0")
    @Column(name = "coefficient_securite", precision = 3, scale = 2, nullable = false)
    private BigDecimal coefficientSecurite = BigDecimal.ONE;

    @NotNull
    @Min(3)
    @Column(name = "nb_mois_historique", nullable = false)
    private Integer nbMoisHistorique = 6;

    @NotNull
    @Min(1)
    @Column(name = "delai_livraison_jours", nullable = false)
    private Integer delaiLivraisonJours = 7;

    @Column(name = "stock_objectif_calcule")
    private Integer stockObjectifCalcule;

    @Column(name = "vmm_calcule")
    private Integer vmmCalcule;

    @Column(name = "date_dernier_calcul")
    private LocalDateTime dateDernierCalcul;

    @Column(name = "facteur_saisonnier_actuel", precision = 3, scale = 2)
    private BigDecimal facteurSaisonnierActuel = BigDecimal.ONE;

    @Column(name = "limite_peremption")
    private Boolean limitePeremption = false;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters, Setters, equals, hashCode...
}
```

#### VentesMensuellesAgregees.java

```java
package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(
    name = "ventes_mensuelles_agregees",
    uniqueConstraints = @UniqueConstraint(columnNames = {"produit_id", "annee_mois"})
)
public class VentesMensuellesAgregees implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @NotNull
    private Produit produit;

    @NotNull
    @Column(name = "annee_mois", length = 7, nullable = false)
    private String anneeMois; // Format: YYYY-MM

    @NotNull
    @Min(0)
    @Column(name = "quantite_vendue", nullable = false)
    private Integer quantiteVendue = 0;

    @NotNull
    @Min(0)
    @Column(name = "montant_ca", nullable = false)
    private Integer montantCa = 0;

    @NotNull
    @Min(0)
    @Column(name = "nombre_ventes", nullable = false)
    private Integer nombreVentes = 0;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Méthodes utilitaires
    public YearMonth getYearMonth() {
        return YearMonth.parse(anneeMois);
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.anneeMois = yearMonth.toString();
    }

    // Getters, Setters...
}
```

#### ClasseCriticite.java (Enum)

```java
package com.kobe.warehouse.domain.enumeration;

public enum ClasseCriticite {
    A_PLUS("Produits vitaux", 1.5),
    A("Forte rotation", 1.0),
    B("Rotation moyenne", 0.7),
    C("Faible rotation", 0.4),
    D("Très faible rotation", 0.2);

    private final String description;
    private final double coefficientDefaut;

    ClasseCriticite(String description, double coefficientDefaut) {
        this.description = description;
        this.coefficientDefaut = coefficientDefaut;
    }

    public String getDescription() {
        return description;
    }

    public double getCoefficientDefaut() {
        return coefficientDefaut;
    }
}
```

### 2. Service SEMOIS

#### SemoisCalculationService.java

```java
package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.SemoisSuggestionDTO;

import java.util.List;

public interface SemoisCalculationService {

    /**
     * Calculer VMM (Ventes Mensuelles Moyennes) avec pondération
     */
    int calculateVMM(Integer produitId, int nbMois);

    /**
     * Calculer la marge de sécurité
     */
    int calculateMargeSecurite(Integer produitId);

    /**
     * Calculer le stock objectif SEMOIS
     */
    int calculateStockObjectif(Integer produitId);

    /**
     * Calculer la quantité à commander
     */
    int calculateQuantiteACommander(Integer produitId);

    /**
     * Obtenir suggestion SEMOIS complète pour un produit
     */
    SemoisSuggestionDTO getSuggestionForProduct(Integer produitId);

    /**
     * Obtenir toutes les suggestions SEMOIS
     */
    List<SemoisSuggestionDTO> getAllSuggestions(String search, String classeCriticite);

    /**
     * Recalculer toutes les configurations SEMOIS
     * (À exécuter quotidiennement via scheduler)
     */
    void recalculateAllConfigurations();

    /**
     * Agréger les ventes du mois écoulé
     * (À exécuter en début de mois via scheduler)
     */
    void aggregateMonthlySales();
}
```

#### SemoisCalculationServiceImpl.java

```java
package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.dto.SemoisSuggestionDTO;
import com.kobe.warehouse.service.stock.SemoisCalculationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
@Transactional
public class SemoisCalculationServiceImpl implements SemoisCalculationService {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisCalculationServiceImpl.class);

    private final SemoisConfigurationRepository semoisConfigRepository;
    private final VentesMensuellesAgregeesRepository ventesRepository;
    private final ProduitRepository produitRepository;
    private final EntityManager entityManager;

    public SemoisCalculationServiceImpl(
        SemoisConfigurationRepository semoisConfigRepository,
        VentesMensuellesAgregeesRepository ventesRepository,
        ProduitRepository produitRepository,
        EntityManager entityManager
    ) {
        this.semoisConfigRepository = semoisConfigRepository;
        this.ventesRepository = ventesRepository;
        this.produitRepository = produitRepository;
        this.entityManager = entityManager;
    }

    @Override
    public int calculateVMM(Integer produitId, int nbMois) {
        // Récupérer ventes des N derniers mois
        YearMonth now = YearMonth.now();
        List<String> mois = IntStream.range(1, nbMois + 1)
            .mapToObj(i -> now.minusMonths(i).toString())
            .toList();

        List<VentesMensuellesAgregees> ventes = ventesRepository
            .findByProduitIdAndAnneeMoisInOrderByAnneeMoisDesc(produitId, mois);

        if (ventes.isEmpty()) {
            return 0;
        }

        // Calcul moyenne mobile pondérée
        // Poids: mois le plus récent = nbMois, le plus ancien = 1
        BigDecimal sommePonderee = BigDecimal.ZERO;
        BigDecimal sommePoids = BigDecimal.ZERO;

        int poids = nbMois;
        for (VentesMensuellesAgregees vente : ventes) {
            BigDecimal quantite = BigDecimal.valueOf(vente.getQuantiteVendue());
            BigDecimal poidsDecimal = BigDecimal.valueOf(poids);

            sommePonderee = sommePonderee.add(quantite.multiply(poidsDecimal));
            sommePoids = sommePoids.add(poidsDecimal);

            poids--;
        }

        if (sommePoids.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        return sommePonderee
            .divide(sommePoids, RoundingMode.HALF_UP)
            .intValue();
    }

    @Override
    public int calculateMargeSecurite(Integer produitId) {
        SemoisConfiguration config = semoisConfigRepository
            .findByProduitId(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Configuration SEMOIS non trouvée pour produit " + produitId));

        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        if (vmm == 0) {
            return 0;
        }

        int delaiJours = config.getDelaiLivraisonJours();
        BigDecimal coefficient = config.getCoefficientSecurite();
        BigDecimal facteurSaisonnier = Objects.requireNonNullElse(
            config.getFacteurSaisonnierActuel(),
            BigDecimal.ONE
        );

        // Marge = VMM × (Délai × Coefficient / 30) × Facteur Saisonnier
        BigDecimal marge = BigDecimal.valueOf(vmm)
            .multiply(BigDecimal.valueOf(delaiJours))
            .multiply(coefficient)
            .divide(BigDecimal.valueOf(30), RoundingMode.HALF_UP)
            .multiply(facteurSaisonnier);

        return marge.intValue();
    }

    @Override
    public int calculateStockObjectif(Integer produitId) {
        SemoisConfiguration config = semoisConfigRepository
            .findByProduitId(produitId)
            .orElseThrow();

        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        int margeSecurite = calculateMargeSecurite(produitId);

        int stockObjectif = vmm + margeSecurite;

        // Ajustement péremption
        if (Boolean.TRUE.equals(config.getLimitePeremption())) {
            // Limiter à 3 mois de VMM pour produits périssables
            int limitePeremption = vmm * 3;
            stockObjectif = Math.min(stockObjectif, limitePeremption);
        }

        return stockObjectif;
    }

    @Override
    public int calculateQuantiteACommander(Integer produitId) {
        int stockObjectif = calculateStockObjectif(produitId);

        // Récupérer stock actuel
        String sql = "SELECT COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) " +
                     "FROM stock_produit sp " +
                     "WHERE sp.produit_id = :produitId";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("produitId", produitId);

        Integer stockActuel = ((Number) query.getSingleResult()).intValue();

        int quantite = stockObjectif - stockActuel;

        return Math.max(0, quantite);
    }

    @Override
    @Transactional(readOnly = true)
    public SemoisSuggestionDTO getSuggestionForProduct(Integer produitId) {
        SemoisConfiguration config = semoisConfigRepository
            .findByProduitId(produitId)
            .orElseThrow();

        Produit produit = config.getProduit();

        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        int margeSecurite = calculateMargeSecurite(produitId);
        int stockObjectif = calculateStockObjectif(produitId);
        int quantiteACommander = calculateQuantiteACommander(produitId);

        // Récupérer stock actuel
        String sql = "SELECT COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) " +
                     "FROM stock_produit sp " +
                     "WHERE sp.produit_id = :produitId";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("produitId", produitId);
        Integer stockActuel = ((Number) query.getSingleResult()).intValue();

        return new SemoisSuggestionDTO(
            produitId,
            produit.getLibelle(),
            produit.getFournisseurProduitPrincipal().getCodeCip(),
            config.getClasseCriticite(),
            vmm,
            margeSecurite,
            stockObjectif,
            stockActuel,
            quantiteACommander,
            config.getDelaiLivraisonJours(),
            config.getCoefficientSecurite(),
            config.getFacteurSaisonnierActuel(),
            config.getDateDernierCalcul()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SemoisSuggestionDTO> getAllSuggestions(String search, String classeCriticite) {
        // Utiliser la vue matérialisée pour performance optimale
        String sql = "SELECT * FROM mv_semois_suggestion WHERE 1=1";

        if (search != null && !search.isBlank()) {
            sql += " AND (LOWER(libelle) LIKE LOWER(:search) OR LOWER(code_cip) LIKE LOWER(:search))";
        }

        if (classeCriticite != null && !classeCriticite.isBlank()) {
            sql += " AND classe_criticite = :classe";
        }

        sql += " ORDER BY quantite_a_commander DESC";

        Query query = entityManager.createNativeQuery(sql);

        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }

        if (classeCriticite != null && !classeCriticite.isBlank()) {
            query.setParameter("classe", classeCriticite);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(this::mapToDTO)
            .toList();
    }

    private SemoisSuggestionDTO mapToDTO(Object[] row) {
        // Mapper résultats de la vue matérialisée vers DTO
        // Implementation selon structure de mv_semois_suggestion
        return null; // TODO: Implement mapping
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *") // Tous les jours à 2h du matin
    public void recalculateAllConfigurations() {
        LOG.info("Début du recalcul SEMOIS pour tous les produits");

        List<SemoisConfiguration> configs = semoisConfigRepository.findAll();

        for (SemoisConfiguration config : configs) {
            try {
                int vmm = calculateVMM(config.getProduit().getId(), config.getNbMoisHistorique());
                int stockObjectif = calculateStockObjectif(config.getProduit().getId());

                config.setVmmCalcule(vmm);
                config.setStockObjectifCalcule(stockObjectif);
                config.setDateDernierCalcul(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());

                semoisConfigRepository.save(config);
            } catch (Exception e) {
                LOG.error("Erreur recalcul SEMOIS pour produit {}", config.getProduit().getId(), e);
            }
        }

        // Rafraîchir vue matérialisée
        entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW mv_semois_suggestion").executeUpdate();

        LOG.info("Fin du recalcul SEMOIS - {} produits traités", configs.size());
    }

    @Override
    @Scheduled(cron = "0 0 1 1 * *") // 1er de chaque mois à 1h du matin
    public void aggregateMonthlySales() {
        LOG.info("Début agrégation ventes mensuelles");

        YearMonth moisPrecedent = YearMonth.now().minusMonths(1);
        String anneeMois = moisPrecedent.toString();

        // Requête pour agréger ventes du mois écoulé
        String sql = """
            INSERT INTO ventes_mensuelles_agregees (produit_id, annee_mois, quantite_vendue, montant_ca, nombre_ventes, created_at)
            SELECT
                sli.produit_id,
                :anneeMois,
                COALESCE(SUM(sli.quantity_sold), 0),
                COALESCE(SUM(sli.sales_amount), 0),
                COUNT(DISTINCT s.id)
            FROM sales_line sli
            JOIN sales s ON s.id = sli.sales_id
            WHERE s.updated_at >= :debut
              AND s.updated_at < :fin
              AND s.statut = 'CLOSED'
            GROUP BY sli.produit_id
            ON CONFLICT (produit_id, annee_mois)
            DO UPDATE SET
                quantite_vendue = EXCLUDED.quantite_vendue,
                montant_ca = EXCLUDED.montant_ca,
                nombre_ventes = EXCLUDED.nombre_ventes
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("anneeMois", anneeMois);
        query.setParameter("debut", moisPrecedent.atDay(1).atStartOfDay());
        query.setParameter("fin", YearMonth.now().atDay(1).atStartOfDay());

        int rows = query.executeUpdate();

        LOG.info("Agrégation terminée - {} produits agrégés pour {}", rows, anneeMois);
    }
}
```

### 3. DTO

```java
package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SemoisSuggestionDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    ClasseCriticite classeCriticite,
    Integer vmm, // Ventes Mensuelles Moyennes
    Integer margeSecurite,
    Integer stockObjectif,
    Integer stockActuel,
    Integer quantiteACommander,
    Integer delaiLivraisonJours,
    BigDecimal coefficientSecurite,
    BigDecimal facteurSaisonnier,
    LocalDateTime dateDernierCalcul
) {
    public double getTauxCouverture() {
        if (vmm == null || vmm == 0) {
            return 0;
        }
        return (double) stockActuel / vmm;
    }

    public boolean estEnRupture() {
        return stockActuel < margeSecurite;
    }

    public boolean estSurstock() {
        return stockActuel > stockObjectif * 1.5;
    }
}
```

---

## 🎨 INTERFACE UTILISATEUR

### Écran de Configuration SEMOIS

```typescript
// semois-config.component.ts
export class SemoisConfigComponent {
  // Configuration globale
  globalConfig = {
    nbMoisHistorique: 6,
    delaiLivraisonDefaut: 7,
    activerAjustementSaisonnier: true,
    activerLimitePeremption: true
  };

  // Configuration par classe
  classesConfig = [
    { classe: 'A+', coefficient: 1.5, couleur: '#dc3545', description: 'Produits vitaux' },
    { classe: 'A', coefficient: 1.0, couleur: '#fd7e14', description: 'Forte rotation' },
    { classe: 'B', coefficient: 0.7, couleur: '#0d6efd', description: 'Rotation moyenne' },
    { classe: 'C', coefficient: 0.4, couleur: '#6c757d', description: 'Faible rotation' },
    { classe: 'D', coefficient: 0.2, couleur: '#adb5bd', description: 'Très faible' }
  ];

  // Tableau de bord SEMOIS
  dashboard = {
    produitsARisque: 0,     // quantiteACommander > 0
    produitsEnSurstock: 0,  // stockActuel > stockObjectif * 1.5
    stockObjectifTotal: 0,
    ecartMoyen: 0           // (stockActuel - stockObjectif) / stockObjectif
  };
}
```

### Tableau de Suggestions SEMOIS

```html
<div class="semois-suggestions">
  <div class="filters">
    <input type="text" [(ngModel)]="searchTerm" placeholder="Rechercher produit..." />
    <select [(ngModel)]="classeFilter">
      <option value="">Toutes classes</option>
      <option value="A+">A+ - Produits vitaux</option>
      <option value="A">A - Forte rotation</option>
      <option value="B">B - Rotation moyenne</option>
      <option value="C">C - Faible rotation</option>
      <option value="D">D - Très faible</option>
    </select>
    <button (click)="exporterSuggestions()">Exporter CSV</button>
  </div>

  <p-table [value]="suggestions" [paginator]="true" [rows]="20">
    <ng-template pTemplate="header">
      <tr>
        <th>Classe</th>
        <th>Produit</th>
        <th>VMM</th>
        <th>Stock Objectif</th>
        <th>Stock Actuel</th>
        <th>À Commander</th>
        <th>Taux Couverture</th>
        <th>Actions</th>
      </tr>
    </ng-template>
    <ng-template pTemplate="body" let-sugg>
      <tr [class]="getRowClass(sugg)">
        <td>
          <span class="badge" [style.background-color]="getClasseColor(sugg.classeCriticite)">
            {{ sugg.classeCriticite }}
          </span>
        </td>
        <td>
          <strong>{{ sugg.libelle }}</strong><br />
          <small>{{ sugg.codeCip }}</small>
        </td>
        <td>{{ sugg.vmm }}</td>
        <td>
          {{ sugg.stockObjectif }}
          <small class="text-muted">(VMM + {{ sugg.margeSecurite }})</small>
        </td>
        <td>{{ sugg.stockActuel }}</td>
        <td>
          @if (sugg.quantiteACommander > 0) {
            <strong class="text-danger">{{ sugg.quantiteACommander }}</strong>
          } @else {
            <span class="text-success">OK</span>
          }
        </td>
        <td>
          <div class="progress">
            <div
              class="progress-bar"
              [class]="getTauxCouvertureClass(sugg)"
              [style.width.%]="getTauxCouverturePct(sugg)"
            >
              {{ getTauxCouverture(sugg) | number: '1.1-1' }} mois
            </div>
          </div>
        </td>
        <td>
          <button p-button icon="pi pi-shopping-cart" (click)="commander(sugg)"></button>
          <button p-button icon="pi pi-cog" (click)="configurer(sugg)"></button>
        </td>
      </tr>
    </ng-template>
  </p-table>
</div>
```

---

## 📊 MONITORING ET KPIs

### Indicateurs à Suivre

1. **Taux de Service:**
   - % de produits avec stock > marge de sécurité
   - Objectif: > 95% pour classe A+, > 90% pour A

2. **Taux de Rotation:**
   - VMM / Stock Moyen
   - Objectif: > 12 (rotation mensuelle) pour classe A

3. **Taux de Couverture:**
   - Stock Actuel / VMM (en mois)
   - Objectif: 1-2 mois pour A, 2-4 mois pour B, 3-6 mois pour C

4. **Coût de Possession:**
   - Valeur Stock Moyen × Taux de Possession
   - Objectif: Réduction de 15-20% vs système actuel

5. **Taux de Rupture:**
   - % de produits avec stock = 0
   - Objectif: < 1% pour A+/A, < 3% pour B, < 5% pour C

### Tableau de Bord Directeur

```sql
-- Vue pour KPIs SEMOIS
CREATE VIEW v_semois_kpis AS
SELECT
    classe_criticite,
    COUNT(*) as nb_produits,
    SUM(CASE WHEN stock_actuel > marge_securite THEN 1 ELSE 0 END) as nb_produits_ok,
    AVG(stock_actuel::DECIMAL / NULLIF(vmm, 0)) as taux_couverture_moyen,
    SUM(stock_actuel * unit_cost) as valeur_stock_total,
    SUM(CASE WHEN stock_actuel = 0 THEN 1 ELSE 0 END) as nb_ruptures
FROM mv_semois_suggestion
JOIN stock_produit sp ON sp.produit_id = mv_semois_suggestion.produit_id
GROUP BY classe_criticite;
```

---

## 🚀 PLAN DE DÉPLOIEMENT

### Phase 1: Préparation (Semaine 1-2)
- [ ] Migration Flyway (tables + vue matérialisée)
- [ ] Création entités JPA
- [ ] Import données historiques ventes (12 derniers mois)
- [ ] Classification initiale produits (A+, A, B, C, D)

### Phase 2: Développement Backend (Semaine 3-4)
- [ ] Service de calcul SEMOIS
- [ ] Scheduler agrégation mensuelle
- [ ] Scheduler recalcul quotidien
- [ ] API REST endpoints
- [ ] Tests unitaires + intégration

### Phase 3: Développement Frontend (Semaine 5-6)
- [ ] Écran configuration globale
- [ ] Écran configuration par produit
- [ ] Tableau suggestions SEMOIS
- [ ] Tableau de bord KPIs
- [ ] Export CSV

### Phase 4: Tests & Validation (Semaine 7-8)
- [ ] Tests avec données réelles
- [ ] Comparaison SEMOIS vs système actuel
- [ ] Ajustement coefficients
- [ ] Formation utilisateurs

### Phase 5: Déploiement Progressif (Semaine 9-12)
- [ ] Pilote sur classe A (20% produits)
- [ ] Extension classe B
- [ ] Extension classe C
- [ ] Déploiement complet

---

## 🎯 COMPARAISON SEMOIS vs EOQ/ABC

| Critère | SEMOIS | EOQ/ABC |
|---------|--------|---------|
| **Complexité** | ⭐⭐ Moyenne | ⭐⭐⭐ Élevée |
| **Adaptation Pharma** | ⭐⭐⭐ Excellent | ⭐⭐ Bon |
| **Facilité Compréhension** | ⭐⭐⭐ Simple | ⭐⭐ Moyen |
| **Précision Prévision** | ⭐⭐ Bon | ⭐⭐⭐ Excellent |
| **Gestion Saisonnalité** | ⭐⭐⭐ Intégré | ⭐⭐ Optionnel |
| **Coût Calcul** | ⭐⭐⭐ Faible | ⭐⭐ Moyen |
| **Adoption Utilisateur** | ⭐⭐⭐ Rapide | ⭐⭐ Lente |

### Recommandation

**💡 Approche Hybride Recommandée:**

1. **SEMOIS** pour gestion quotidienne:
   - Suggestions automatiques
   - Suivi taux de couverture
   - Alertes rupture/surstock

2. **ABC/EOQ** pour optimisation trimestrielle:
   - Révision classification produits
   - Ajustement coefficients sécurité
   - Optimisation coûts globaux

---

**Document créé par:** Claude Code (Sonnet 4.5)
**Contact:** Équipe Pharma-Smart
**Dernière Mise à Jour:** 2025-12-20
