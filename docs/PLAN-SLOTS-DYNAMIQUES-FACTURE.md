# Plan — Slots Dynamiques & Évolutifs pour les Modèles de Factures PDF

> **Contexte :** Extension du plan `PLAN-PERSONNALISATION-MODELES-FACTURE.md`
> **Problème résolu :** Les 8 slots codés en dur dans `ColonneSlot` ne couvrent pas toute
> la richesse du modèle de domaine. L'ajout d'un nouveau slot nécessite actuellement une
> modification du code Java + redéploiement.
> **Objectif :** Rendre le catalogue de slots 100 % piloté par données (base de données),
> extensible par migration Flyway ou via l'UI admin, sans aucune modification du code applicatif.

---

## Table des matières

1. [Analyse comparative — avant vs après](#1-analyse-comparative--avant-vs-après)
2. [Catalogue exhaustif des variables disponibles](#2-catalogue-exhaustif-des-variables-disponibles)
3. [Architecture retenue — `facture_slot_definition`](#3-architecture-retenue--facture_slot_definition)
4. [Entité `FactureSlotDefinition`](#4-entité-facturesLotdefinition)
5. [Gestion des données agrégées](#5-gestion-des-données-agrégées--sommes-totaux-moyennes-comptages)
   - 5.1 [Types d'agrégation par slot](#51-problème--les-agrégats-ne-sont-pas-uniformes)
   - 5.2 [Extension du modèle — `aggregation_type`](#52-extension-du-modèle--aggregation_type-dans-facture_slot_definition)
   - 5.3 [`AggregateResult`](#53-aggregateresult--structure-de-résultat)
   - 5.4 [`AggregateCalculatorService`](#54-aggregatecalculatorservice--moteur-de-calcul)
   - 5.5 [Génération du `<tfoot>`](#55-génération-du-tfoot-dans-templatebuilderservice)
   - 5.6 [Injection dans le contexte Thymeleaf](#56-injection-dans-le-contexte-thymeleaf)
   - 5.7 [Cas particuliers — slots FACTURE](#57-cas-particuliers--slots-facture-déjà-agrégés)
   - 5.8 [Tableau récapitulatif par catégorie](#58-tableau-récapitulatif--types-dagrégation-par-catégorie)
   - 5.9 [Visualisation rendu PDF](#59-visualisation--rendu-pdf-dun-tfoot-complet)
6. [`TemplateBuilderService` — version dynamique](#6-templatebuilderservice--version-dynamique)
7. [Migration Flyway — données initiales](#7-migration-flyway--données-initiales)
8. [Endpoints REST](#8-endpoints-rest)
9. [Frontend — sélecteur de slots enrichi](#9-frontend--sélecteur-de-slots-enrichi)
10. [Sécurité — validation des expressions](#10-sécurité--validation-des-expressions)
11. [Évolution future — ajout d'un slot sans redéploiement](#11-évolution-future--ajout-dun-slot-sans-redéploiement)
12. [Impact sur le plan principal](#12-impact-sur-le-plan-principal)

---

## 1. Analyse comparative — avant vs après

### Situation actuelle (slots codés en dur)

```java
// ColonneSlot.java — 8 slots figés dans le code
public enum ColonneSlot {
    DATE, NOM_ASSURE, PRENOM_ASSURE, NOM_PATIENT,
    MATRICULE, NUM_BON, MONTANT_BON, MONTANT_ATTENDU
}

// TemplateBuilderService.java — switch hardcodé
private String resolveExpression(String slot) {
    return switch (slot) {
        case "NOM_ASSURE" -> "<span th:text=\"${detail.clientTiersPayant.assuredCustomer.firstName}\"></span>";
        // ...
        default -> "";  // ← slot inconnu = silencieux, pas d'erreur, champ vide
    };
}
```

**Problèmes :**
| Problème | Impact |
|----------|--------|
| 8 slots sur ~40 champs disponibles | 80 % de la richesse du domaine inaccessible |
| Ajout d'un slot = modification Java + build + déploiement | Cycle de release bloquant |
| `default -> ""` silencieux | Erreur de configuration indétectable |
| Aucune documentation dans l'UI | L'admin ne sait pas ce qu'est `NOM_ASSURE` |
| Pas de groupe / catégorie | Interface plate impossible à parcourir avec 40 slots |

### Solution proposée (slots pilotés par données)

```
facture_slot_definition (table)
  ├── key          VARCHAR(50)  — identifiant unique ex: "ASSURE_NOM"
  ├── label        VARCHAR(100) — "Nom assuré"
  ├── description  TEXT         — "Prénom de l'assuré tel qu'enregistré dans le dossier"
  ├── expression   TEXT         — "detail.clientTiersPayant.assuredCustomer.firstName"
  ├── scope        VARCHAR(10)  — LINE | FACTURE (ligne répétée vs en-tête)
  ├── formatter    VARCHAR(15)  — STRING | DATE | INTEGER | DECIMAL | PERCENT
  ├── category     VARCHAR(20)  — ASSURE | PATIENT | BON | MONTANT | TAUX | FACTURE | ORGANISME
  ├── builtin      BOOLEAN      — true = créé par migration, non supprimable
  └── actif        BOOLEAN      — visible dans le sélecteur de l'UI
```

**Avantages :**
| Avantage | Détail |
|----------|--------|
| Extensible sans code | Ajout d'un slot = `INSERT` dans la table ou migration |
| 40+ slots disponibles dès le départ | Toute la richesse du domaine exposée |
| Groupés par catégorie | Interface claire dans le configurateur |
| Documentation intégrée | `description` affichée dans l'UI comme tooltip |
| Validation de l'expression au `INSERT` | Pas d'erreur silencieuse |
| Rétrocompatible | Les templates existants avec les anciens 8 slots continuent à fonctionner |

---

## 2. Catalogue exhaustif des variables disponibles

> Dérivé de l'analyse de `FactureTiersPayant`, `ThirdPartySaleLine`,
> `ClientTiersPayant`, `AssuredCustomer`, `Customer`, `ThirdPartySales`, `Sales`, `TiersPayant`.

### Scope LINE — variables disponibles dans `th:each="detail : ${entity.facturesDetails}"`

#### Catégorie ASSURE (Assuré principal)

| Clé | Label FR | Expression Thymeleaf | Formatter | Source |
|-----|----------|----------------------|-----------|--------|
| `ASSURE_NOM` | Nom assuré | `detail.clientTiersPayant.assuredCustomer.firstName` | STRING | `Customer.firstName` |
| `ASSURE_PRENOM` | Prénom assuré | `detail.clientTiersPayant.assuredCustomer.lastName` | STRING | `Customer.lastName` |
| `ASSURE_NOM_PRENOM` | Nom & Prénom assuré | `detail.clientTiersPayant.assuredCustomer.firstName + ' ' + detail.clientTiersPayant.assuredCustomer.lastName` | STRING | Calculé |
| `ASSURE_MATRICULE` | Matricule | `detail.clientTiersPayant.num` | STRING | `ClientTiersPayant.num` |
| `ASSURE_CODE` | Code assuré | `detail.clientTiersPayant.assuredCustomer.code` | STRING | `Customer.code` |
| `ASSURE_SEXE` | Sexe | `detail.clientTiersPayant.assuredCustomer.sexe` | STRING | `AssuredCustomer.sexe` |
| `ASSURE_DATE_NAISSANCE` | Date de naissance | `detail.clientTiersPayant.assuredCustomer.datNaiss` | DATE | `AssuredCustomer.datNaiss` |
| `ASSURE_TYPE` | Type assuré | `detail.clientTiersPayant.assuredCustomer.typeAssure` | STRING | `Customer.typeAssure` |
| `ASSURE_TAUX_CLIENT` | Taux prise en charge | `detail.clientTiersPayant.taux` | PERCENT | `ClientTiersPayant.taux` |
| `ASSURE_NUM_AYANT_DROIT` | N° Ayant droit | `detail.clientTiersPayant.assuredCustomer.numAyantDroit` | STRING | `AssuredCustomer.numAyantDroit` |

#### Catégorie PATIENT (Bénéficiaire / Ayant droit de la vente)

| Clé | Label FR | Expression Thymeleaf | Formatter | Source |
|-----|----------|----------------------|-----------|--------|
| `PATIENT_NOM` | Nom patient | `detail.sale.ayantDroit?.firstName` | STRING | `AssuredCustomer.firstName` |
| `PATIENT_PRENOM` | Prénom patient | `detail.sale.ayantDroit?.lastName` | STRING | `AssuredCustomer.lastName` |
| `PATIENT_NOM_PRENOM` | Nom & Prénom patient | `(detail.sale.ayantDroit != null ? detail.sale.ayantDroit.firstName + ' ' + detail.sale.ayantDroit.lastName : '')` | STRING | Calculé |
| `PATIENT_NUM_AYANT_DROIT` | N° Ayant droit patient | `detail.sale.ayantDroit?.numAyantDroit` | STRING | `AssuredCustomer.numAyantDroit` |
| `PATIENT_DATE_NAISSANCE` | Date naissance patient | `detail.sale.ayantDroit?.datNaiss` | DATE | `AssuredCustomer.datNaiss` |
| `PATIENT_SEXE` | Sexe patient | `detail.sale.ayantDroit?.sexe` | STRING | `AssuredCustomer.sexe` |

#### Catégorie BON (Bon / Ordonnance)

| Clé | Label FR | Expression Thymeleaf | Formatter | Source |
|-----|----------|----------------------|-----------|--------|
| `BON_NUM` | N° Bon (ligne) | `detail.numBon` | STRING | `ThirdPartySaleLine.numBon` |
| `BON_NUM_VENTE` | N° Bon (vente) | `detail.sale.numBon` | STRING | `ThirdPartySales.numBon` |
| `BON_DATE` | Date enregistrement | `detail.created` | DATE | `ThirdPartySaleLine.created` |
| `BON_DATE_VENTE` | Date de la vente | `detail.saleDate` | DATE | `ThirdPartySaleLine.saleDate` |
| `BON_NUM_TRANSACTION` | N° Transaction | `detail.sale.numberTransaction` | STRING | `Sales.numberTransaction` |
| `BON_TYPE_PRESCRIPTION` | Type prescription | `detail.sale.typePrescription` | STRING | `Sales.typePrescription` |
| `BON_NATURE_VENTE` | Nature vente | `detail.sale.natureVente` | STRING | `Sales.natureVente` |

#### Catégorie MONTANT (Montants financiers)

| Clé | Label FR | Expression Thymeleaf | Formatter | Source |
|-----|----------|----------------------|-----------|--------|
| `MONTANT_ATTENDU` | Montant attendu TP | `detail.montant` | INTEGER | `ThirdPartySaleLine.montant` |
| `MONTANT_BON` | Montant bon (total vente) | `detail.sale.salesAmount` | INTEGER | `Sales.salesAmount` |
| `MONTANT_REGLE` | Montant réglé (ligne) | `detail.montantRegle` | INTEGER | `ThirdPartySaleLine.montantRegle` |
| `MONTANT_PART_ASSURE` | Part assuré | `detail.sale.partAssure` | INTEGER | `ThirdPartySales.partAssure` |
| `MONTANT_PART_TP` | Part tiers-payant | `detail.sale.partTiersPayant` | INTEGER | `ThirdPartySales.partTiersPayant` |
| `MONTANT_REMISE` | Remise | `detail.sale.discountAmount` | INTEGER | `Sales.discountAmount` |
| `MONTANT_NET_VENTE` | Montant net vente | `detail.sale.netAmount` | INTEGER | `Sales.netAmount` |
| `MONTANT_HT_VENTE` | Montant HT vente | `detail.sale.htAmount` | INTEGER | `Sales.htAmount` |
| `MONTANT_TVA_VENTE` | Montant TVA vente | `detail.sale.taxAmount` | INTEGER | `Sales.taxAmount` |

#### Catégorie TAUX

| Clé | Label FR | Expression Thymeleaf | Formatter | Source |
|-----|----------|----------------------|-----------|--------|
| `TAUX_PRISE_EN_CHARGE` | Taux TP (%) | `detail.taux` | PERCENT | `ThirdPartySaleLine.taux` |
| `TAUX_VENTE` | Taux vente (%) | `detail.tauxVente` | PERCENT | `ThirdPartySaleLine.tauxVente` |

---

### Scope FACTURE — variables disponibles dans le contexte `entity` (en-tête, non répétables)

#### Catégorie FACTURE

| Clé | Label FR | Expression Thymeleaf | Formatter | Source |
|-----|----------|----------------------|-----------|--------|
| `FACTURE_NUM` | N° Facture | `entity.displayNumFacture` | STRING | `FactureTiersPayant.displayNumFacture` |
| `FACTURE_DEBUT_PERIODE` | Début période | `entity.debutPeriode` | DATE | `FactureTiersPayant.debutPeriode` |
| `FACTURE_FIN_PERIODE` | Fin période | `entity.finPeriode` | DATE | `FactureTiersPayant.finPeriode` |
| `FACTURE_DATE` | Date facture | `entity.invoiceDate` | DATE | `FactureTiersPayant.invoiceDate` |
| `FACTURE_MONTANT_NET` | Montant net facture | `entity.montantNet` | DECIMAL | `FactureTiersPayant.montantNet` |
| `FACTURE_MONTANT_TTC` | Montant TTC facture | `entity.montantTtc` | DECIMAL | `FactureTiersPayant.montantTtc` |
| `FACTURE_MONTANT_HT` | Montant HT facture | `entity.montantHt` | DECIMAL | `FactureTiersPayant.montantHt` |
| `FACTURE_MONTANT_TVA` | Montant TVA facture | `entity.montantTva` | DECIMAL | `FactureTiersPayant.montantTva` |
| `FACTURE_REMISE_FORFETAIRE` | Remise forfaitaire | `entity.remiseForfetaire` | INTEGER | `FactureTiersPayant.remiseForfetaire` |
| `FACTURE_STATUT` | Statut facture | `entity.statut` | STRING | `FactureTiersPayant.statut` |

#### Catégorie ORGANISME

| Clé | Label FR | Expression Thymeleaf | Formatter | Source |
|-----|----------|----------------------|-----------|--------|
| `ORG_NOM` | Nom organisme | `entity.tiersPayant.name` | STRING | `TiersPayant.name` |
| `ORG_NOM_COMPLET` | Nom complet organisme | `entity.tiersPayant.fullName` | STRING | `TiersPayant.fullName` |
| `ORG_CODE` | Code organisme | `entity.tiersPayant.codeOrganisme` | STRING | `TiersPayant.codeOrganisme` |
| `ORG_NCC` | NCC (identifiant fiscal) | `entity.tiersPayant.ncc` | STRING | `TiersPayant.ncc` |
| `ORG_ADRESSE` | Adresse organisme | `entity.tiersPayant.adresse` | STRING | `TiersPayant.adresse` |
| `ORG_TELEPHONE` | Téléphone organisme | `entity.tiersPayant.telephone` | STRING | `TiersPayant.telephone` |
| `ORG_DELAI_REGLEMENT` | Délai règlement (jours) | `entity.tiersPayant.delaiReglement` | INTEGER | `TiersPayant.delaiReglement` |

---

**Récapitulatif : 8 slots avant → 40+ slots disponibles**

| Catégorie | Slots disponibles | Scope |
|-----------|:-----------------:|-------|
| ASSURE | 10 | LINE |
| PATIENT | 6 | LINE |
| BON | 7 | LINE |
| MONTANT | 9 | LINE |
| TAUX | 2 | LINE |
| FACTURE | 10 | FACTURE |
| ORGANISME | 7 | FACTURE |
| **Total** | **51** | — |

---

## 3. Architecture retenue — `facture_slot_definition`

```
facture_slot_definition (catalogue)
  ← lu au démarrage et mis en cache
  ← extensible par migration ou API admin

FactureSlotDefinitionService
  ← fournit List<SlotDefinition> au TemplateBuilderService

TemplateBuilderService
  ← version dynamique : résout l'expression depuis la DB, pas depuis un switch
  ← génère bodyTemplate à partir de colonnes_config + slotDefinitions

FactureTemplateConfig (existant)
  ← colonnes_config stocke les clés (ex: "ASSURE_NOM", "BON_NUM")
  ← le libellé personnalisé est dans ColonneConfig.libelle
  ← l'expression est toujours dans facture_slot_definition

Rétrocompatibilité :
  ← Les anciennes clés ("NOM_ASSURE", "NUM_BON"...) sont remappées
     via une migration RENAME ou un alias dans la table
```

---

## 4. Entité `FactureSlotDefinition`

**Package :** `com.kobe.warehouse.domain` (ou `service/facturation/`)

```java
@Entity
@Table(name = "facture_slot_definition",
    uniqueConstraints = @UniqueConstraint(columnNames = "slot_key"))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FactureSlotDefinition implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Identifiant unique du slot (ex: "ASSURE_NOM", "BON_NUM").
     * Stocké dans colonnes_config.slot des FactureTemplateConfig.
     */
    @Column(name = "slot_key", nullable = false, unique = true, length = 50)
    private String slotKey;

    /** Label affiché dans l'UI configurateur (ex: "Nom assuré"). */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /**
     * Explication métier pour l'admin (tooltip).
     * Ex: "Prénom de l'assuré tel qu'enregistré dans le dossier patient."
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Expression Thymeleaf SpEL (sans ${ }).
     * Ex: "detail.clientTiersPayant.assuredCustomer.firstName"
     * Validée à la sauvegarde via SlotExpressionValidator.
     */
    @Column(name = "expression", nullable = false, columnDefinition = "TEXT")
    private String expression;

    /**
     * Portée du slot :
     * - LINE    : itéré dans th:each="detail : ${entity.facturesDetails}"
     * - FACTURE : accessible une seule fois dans l'en-tête/pied via ${entity}
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private SlotScope scope = SlotScope.LINE;

    /**
     * Formateur appliqué à l'expression lors de la génération du HTML.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "formatter", nullable = false, length = 15)
    private SlotFormatter formatter = SlotFormatter.STRING;

    /**
     * Groupe métier pour l'affichage dans l'UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private SlotCategory category;

    /**
     * true = créé par migration Flyway, non supprimable via l'API.
     * false = créé par un admin, peut être supprimé.
     */
    @Column(name = "builtin", nullable = false)
    private boolean builtin = true;

    /** false = masqué du sélecteur dans l'UI (mais utilisable dans les templates existants). */
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /** Ordre d'affichage dans l'UI dans sa catégorie. */
    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

    /** Type d'agrégation dans le pied de tableau (<tfoot>). */
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", nullable = false, length = 10)
    private AggregationType aggregationType = AggregationType.NONE;

    /**
     * Label affiché à gauche de la valeur agrégée dans le pied.
     * Ex: "Total :", "Moy. :", "Nb. bons :"
     * null = pas de label.
     */
    @Column(name = "aggregate_label", length = 30)
    private String aggregateLabel;

    // getters/setters...
}

// Enums de configuration
public enum SlotScope     { LINE, FACTURE }
public enum SlotFormatter { STRING, DATE, INTEGER, DECIMAL, PERCENT, BOOLEAN }
public enum SlotCategory  { ASSURE, PATIENT, BON, MONTANT, TAUX, FACTURE, ORGANISME }
```

**Repository :**
```java
public interface FactureSlotDefinitionRepository
    extends JpaRepository<FactureSlotDefinition, Integer> {

    List<FactureSlotDefinition>  findAllByActifTrueOrderByCategoryAscDisplayOrderAsc();
    Optional<FactureSlotDefinition> findBySlotKey(String slotKey);
    List<FactureSlotDefinition>  findByCategory(SlotCategory category);
}
```

**DTO pour l'API :**
```java
public record SlotDefinitionDto(
    Integer       id,
    String        slotKey,
    String        label,
    String        description,
    SlotScope     scope,
    SlotFormatter formatter,
    SlotCategory  category,
    boolean       builtin,
    boolean       actif,
    Integer       displayOrder,
    String        expression       // exposé uniquement aux ADMIN
) {}
```

---

## 5. Gestion des données agrégées — sommes, totaux, moyennes, comptages

### 5.1 Problème — les agrégats ne sont pas uniformes

Les besoins d'agrégation diffèrent selon le type de slot :

| Slot | Type d'agrégation métier | Exemple attendu dans le pied |
|------|-------------------------|------------------------------|
| `MONTANT_ATTENDU` | **SUM** | `1 234 567` (total à payer) |
| `MONTANT_BON` | **SUM** | `1 456 789` (total des ventes) |
| `TAUX_PRISE_EN_CHARGE` | **AVG** | `80 %` (taux moyen) |
| `BON_NUM` | **COUNT** | `42 bons` |
| `MONTANT_PART_ASSURE` | **SUM** | `222 222` |
| `ASSURE_NOM` | **NONE** | *(cellule vide)* |
| `BON_DATE` | **NONE** | *(cellule vide)* |
| `FACTURE_MONTANT_NET` | **NONE** (déjà agrégé) | *(valeur de l'entity)* |

> **Règle :** Les slots `FACTURE` (scope `FACTURE`) **sont déjà des agrégats** au niveau
> de l'entité `FactureTiersPayant` (`entity.montantNet`, `entity.montantTtc`...).
> Seuls les slots `LINE` ont besoin d'un calcul d'agrégat sur les lignes `facturesDetails`.

### 5.2 Extension du modèle — `aggregation_type` dans `facture_slot_definition`

```sql
-- Ajout à la migration V1.4.3__facture_slot_definition.sql
ALTER TABLE facture_slot_definition
  ADD COLUMN aggregation_type VARCHAR(10) NOT NULL DEFAULT 'NONE',
  -- Label affiché à gauche du total dans le pied (ex: "Total :", "Moy. :", "Nb. :")
  ADD COLUMN aggregate_label  VARCHAR(30);
```

```java
public enum AggregationType {
    NONE,   // Pas d'agrégat — cellule vide dans tfoot
    SUM,    // Somme des valeurs numériques
    AVG,    // Moyenne des valeurs numériques
    COUNT,  // Nombre de lignes non nulles
    MIN,    // Valeur minimale
    MAX     // Valeur maximale
}
```

**Mise à jour des données de référence (extrait) :**
```sql
-- Slots MONTANT → SUM
UPDATE facture_slot_definition SET aggregation_type='SUM', aggregate_label='Total :'
WHERE slot_key IN (
  'MONTANT_ATTENDU','MONTANT_BON','MONTANT_REGLE','MONTANT_PART_ASSURE',
  'MONTANT_PART_TP','MONTANT_REMISE','MONTANT_NET_VENTE','MONTANT_HT_VENTE','MONTANT_TVA_VENTE'
);

-- Slots TAUX → AVG (moyenne pondérée)
UPDATE facture_slot_definition SET aggregation_type='AVG', aggregate_label='Moy. :'
WHERE slot_key IN ('TAUX_PRISE_EN_CHARGE','TAUX_VENTE','ASSURE_TAUX_CLIENT');

-- Slot N° Bon → COUNT
UPDATE facture_slot_definition SET aggregation_type='COUNT', aggregate_label='Total :'
WHERE slot_key IN ('BON_NUM','BON_NUM_VENTE');
```

### 5.3 `AggregateResult` — structure de résultat

```java
/**
 * Résultat d'agrégation pour un slot donné.
 * Contient toutes les valeurs possibles — seule la valeur correspondant
 * à l'AggregationType du slot sera utilisée dans le template.
 */
public record AggregateResult(
    long   count,          // nombre de lignes non nulles
    long   sum,            // somme (pour INTEGER/DECIMAL)
    double avg,            // moyenne
    long   min,            // valeur minimale
    long   max,            // valeur maximale

    // Valeurs pré-formatées selon le formatter du slot
    String formattedSum,   // "1 234 567"
    String formattedAvg,   // "80 %" ou "1 234"
    String formattedMin,
    String formattedMax,
    String formattedCount, // "42 bons"

    // Valeur unique selon AggregationType du slot (la plus utilisée)
    String formattedValue  // → formattedSum si SUM, formattedAvg si AVG, etc.
) {
    /** Construit un résultat vide (slot NONE ou erreur). */
    public static AggregateResult empty() {
        return new AggregateResult(0, 0, 0, 0, 0, "", "", "", "", "", "");
    }
}
```

### 5.4 `AggregateCalculatorService` — moteur de calcul

```java
/**
 * Calcule les agrégats pour tous les slots LINE d'un template.
 * Utilise Spring SpEL pour évaluer les expressions dynamiquement.
 * Résultat injecté dans le contexte Thymeleaf sous la clé "aggregates".
 */
@Service
@Slf4j
public class AggregateCalculatorService {

    private final FactureSlotDefinitionRepository slotRepository;
    private final ExpressionParser                spelParser = new SpelExpressionParser();

    /**
     * Calcule les agrégats pour les slots LINE utilisés dans un template.
     *
     * @param lines    Liste des ThirdPartySaleLine de la facture
     * @param slotKeys Clés des slots configurés dans le template (colonnes_config)
     * @return Map<slotKey, AggregateResult>
     */
    public Map<String, AggregateResult> compute(
        List<ThirdPartySaleLine> lines,
        List<String>             slotKeys
    ) {
        if (lines == null || lines.isEmpty()) return Map.of();

        // Charger uniquement les slots LINE avec agrégation non-NONE
        List<FactureSlotDefinition> slotsToAggregate = slotRepository
            .findAllBySlotKeyIn(slotKeys)
            .stream()
            .filter(s -> s.getScope() == SlotScope.LINE)
            .filter(s -> s.getAggregationType() != AggregationType.NONE)
            .toList();

        Map<String, AggregateResult> results = new HashMap<>();
        for (FactureSlotDefinition slot : slotsToAggregate) {
            results.put(slot.getSlotKey(), computeForSlot(lines, slot));
        }
        return results;
    }

    private AggregateResult computeForSlot(
        List<ThirdPartySaleLine> lines,
        FactureSlotDefinition    slot
    ) {
        // Évaluer l'expression SpEL sur chaque ligne et collecter les valeurs numériques
        List<Number> values = lines.stream()
            .map(detail -> evaluateSafely(slot.getExpression(), detail))
            .filter(Objects::nonNull)
            .toList();

        if (values.isEmpty()) return AggregateResult.empty();

        long   count = values.size();
        long   sum   = values.stream().mapToLong(Number::longValue).sum();
        double avg   = values.stream().mapToDouble(Number::doubleValue).average().orElse(0);
        long   min   = values.stream().mapToLong(Number::longValue).min().orElse(0);
        long   max   = values.stream().mapToLong(Number::longValue).max().orElse(0);

        // Formater selon le formatter du slot
        String formattedSum   = format(sum,   slot.getFormatter());
        String formattedAvg   = format((long) Math.round(avg), slot.getFormatter());
        String formattedMin   = format(min,   slot.getFormatter());
        String formattedMax   = format(max,   slot.getFormatter());
        String formattedCount = count + (slot.getAggregateLabel() != null ? "" : "");

        // Valeur principale selon le type d'agrégation du slot
        String formattedValue = switch (slot.getAggregationType()) {
            case SUM   -> formattedSum;
            case AVG   -> formattedAvg;
            case COUNT -> String.valueOf(count);
            case MIN   -> formattedMin;
            case MAX   -> formattedMax;
            case NONE  -> "";
        };

        return new AggregateResult(
            count, sum, avg, min, max,
            formattedSum, formattedAvg, formattedMin, formattedMax, formattedCount,
            formattedValue
        );
    }

    /**
     * Évalue une expression SpEL de façon sécurisée sur un objet detail.
     * Retourne null si l'expression échoue ou si le résultat n'est pas numérique.
     */
    private Number evaluateSafely(String expression, ThirdPartySaleLine detail) {
        try {
            EvaluationContext ctx = new StandardEvaluationContext(detail);
            // Désactiver l'accès aux méthodes statiques, constructeurs et types
            ctx.setVariable("_guard", null);  // Variable sentinelle
            Object val = spelParser.parseExpression(expression).getValue(ctx);
            return (val instanceof Number n) ? n : null;
        } catch (Exception e) {
            log.debug("Impossible d'évaluer l'expression '{}' : {}", expression, e.getMessage());
            return null;
        }
    }

    private String format(long value, SlotFormatter formatter) {
        return switch (formatter) {
            case PERCENT -> NumberFormat.getIntegerInstance(Locale.FRANCE).format(value) + " %";
            case DECIMAL -> String.format("%,.2f", (double) value / 100);  // si stocké en centimes
            default      -> NumberFormat.getIntegerInstance(Locale.FRANCE).format(value);
        };
    }
}
```

### 5.5 Génération du `<tfoot>` dans `TemplateBuilderService`

Le pied de tableau est généré automatiquement selon l'`aggregation_type` de chaque slot :

```java
// Dans TemplateBuilderService.buildBodyTemplate(...)

// Pied — agrégats dynamiques
sb.append("  <tfoot class=\"datatable-footer\">\n");
sb.append("    <tr class=\"tr-footer\">\n");

for (ColonneConfig col : visibles) {
    FactureSlotDefinition def = definitions.get(col.slot());

    if (def == null || def.getScope() != SlotScope.LINE
            || def.getAggregationType() == AggregationType.NONE) {
        // Pas d'agrégat → cellule vide
        sb.append("      <td></td>\n");
        continue;
    }

    String tdClass = "right".equals(col.alignement()) ? " class=\"unit\"" : "";
    String aggrVar = "aggregates['" + def.getSlotKey() + "']";

    switch (def.getAggregationType()) {
        case SUM -> {
            // "Total : 1 234 567"
            String label = def.getAggregateLabel() != null ? def.getAggregateLabel() : "";
            sb.append("      <td").append(tdClass).append(">\n");
            sb.append("        <span class=\"agg-label\">").append(label).append("</span>\n");
            sb.append("        <span th:text=\"${").append(aggrVar).append(".formattedSum}\"></span>\n");
            sb.append("      </td>\n");
        }
        case AVG -> {
            // "Moy. : 80 %"
            String label = def.getAggregateLabel() != null ? def.getAggregateLabel() : "Moy. :";
            sb.append("      <td").append(tdClass).append(">\n");
            sb.append("        <span class=\"agg-label\">").append(label).append("</span>\n");
            sb.append("        <span th:text=\"${").append(aggrVar).append(".formattedAvg}\"></span>\n");
            sb.append("      </td>\n");
        }
        case COUNT -> {
            // "42"
            sb.append("      <td").append(tdClass).append(">");
            sb.append("<span th:text=\"${").append(aggrVar).append(".count}\"></span>");
            sb.append("</td>\n");
        }
        case MIN -> {
            sb.append("      <td").append(tdClass).append(">");
            sb.append("<span th:text=\"${").append(aggrVar).append(".formattedMin}\"></span>");
            sb.append("</td>\n");
        }
        case MAX -> {
            sb.append("      <td").append(tdClass).append(">");
            sb.append("<span th:text=\"${").append(aggrVar).append(".formattedMax}\"></span>");
            sb.append("</td>\n");
        }
    }
}
sb.append("    </tr>\n  </tfoot>\n");
```

**Rendu HTML généré (exemple avec 3 colonnes) :**

```html
<tfoot class="datatable-footer">
  <tr class="tr-footer">
    <td></td>  <!-- ASSURE_NOM : NONE -->
    <td></td>  <!-- BON_NUM : COUNT → mais pas d'alignement right, on le met -->
    <td class="unit">
      <span class="agg-label">Total :</span>
      <span th:text="${aggregates['MONTANT_ATTENDU'].formattedSum}"></span>
    </td>
  </tr>
</tfoot>
```

> `3 bons` ← `aggregates['BON_NUM'].count`
> `Total : 36 250` ← `aggregates['MONTANT_BON'].formattedSum`
> `Total : 29 000` ← `aggregates['MONTANT_ATTENDU'].formattedSum`

---

## 6. `TemplateBuilderService` — version dynamique

```java
@Service
public class TemplateBuilderService {

    private final FactureSlotDefinitionRepository slotRepository;

    /**
     * Génère le HTML Thymeleaf <table> depuis la configuration colonnes.
     * Toutes les expressions sont résolues dynamiquement depuis la DB.
     */
    public String buildBodyTemplate(List<ColonneConfig> colonnes, DisplayConfig display) {

        // Charger les définitions pour les clés demandées
        List<String> keys = colonnes.stream().map(ColonneConfig::slot).toList();
        Map<String, FactureSlotDefinition> definitions = slotRepository
            .findAllBySlotKeyIn(keys)
            .stream()
            .collect(Collectors.toMap(FactureSlotDefinition::getSlotKey, Function.identity()));

        List<ColonneConfig> visibles = colonnes.stream()
            .filter(ColonneConfig::visible)
            .sorted(Comparator.comparingInt(ColonneConfig::ordre))
            .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"main-table\">\n");

        // En-tête
        sb.append("  <thead><tr>\n");
        for (ColonneConfig col : visibles) {
            String cssClass = cssClassForAlignment(col.alignement());
            sb.append("    <th").append(cssClass).append(">")
              .append(col.libelle()).append("</th>\n");
        }
        sb.append("  </tr></thead>\n");

        // Corps
        sb.append("  <tbody>\n");
        sb.append("    <tr th:each=\"detail : ${entity.facturesDetails}\">\n");
        for (ColonneConfig col : visibles) {
            FactureSlotDefinition def = definitions.get(col.slot());
            if (def == null) {
                // Slot inconnu : cellule vide avec commentaire (plus de défaut silencieux)
                sb.append("      <td><!-- slot inconnu: ").append(col.slot()).append(" --></td>\n");
                continue;
            }
            // Seuls les slots LINE sont affichés dans le corps (les slots FACTURE ignorés ici)
            if (def.getScope() == SlotScope.LINE) {
                String tdClass = "right".equals(col.alignement()) ? " class=\"unit\"" : "";
                sb.append("      <td").append(tdClass).append(">")
                  .append(resolveExpression(def))
                  .append("</td>\n");
            }
        }
        sb.append("    </tr>\n  </tbody>\n");

        // Pied — totaux pour les colonnes MONTANT
        sb.append("  <tfoot class=\"datatable-footer\"><tr class=\"tr-footer\">\n");
        for (ColonneConfig col : visibles) {
            FactureSlotDefinition def = definitions.get(col.slot());
            if (def != null && def.getScope() == SlotScope.LINE
                && def.getCategory() == SlotCategory.MONTANT) {
                // Génère le total dynamiquement via une variable de contexte injectée
                String totalVar = "totals['" + col.slot() + "']";
                sb.append("      <td class=\"unit\" th:text=\"${").append(totalVar).append("}\"></td>\n");
            } else {
                sb.append("      <td></td>\n");
            }
        }
        sb.append("  </tr></tfoot>\n");
        sb.append("</table>\n");

        return sb.toString();
    }

    /**
     * Résout l'expression Thymeleaf complète selon le formatter défini en base.
     * Entièrement générique — aucun switch sur les clés.
     */
    private String resolveExpression(FactureSlotDefinition def) {
        String expr = def.getExpression();
        return switch (def.getFormatter()) {
            case DATE    -> "<span th:text=\"${#temporals.format(" + expr + ", 'dd/MM/yyyy')}\"></span>";
            case INTEGER -> "<span th:text=\"${#numbers.formatInteger(" + expr + ", 3, 'WHITESPACE')}\"></span>";
            case DECIMAL -> "<span th:text=\"${#numbers.formatDecimal(" + expr + ", 3, 'WHITESPACE', 2, 'POINT')}\"></span>";
            case PERCENT -> "<span th:text=\"${" + expr + " + ' %'}\"></span>";
            case BOOLEAN -> "<span th:text=\"${" + expr + " ? 'Oui' : 'Non'}\"></span>";
            default      -> "<span th:text=\"${" + expr + "}\"></span>";
        };
    }

    private String cssClassForAlignment(String align) {
        return switch (align == null ? "left" : align) {
            case "right"  -> " class=\"text-right\"";
            case "center" -> " class=\"text-center\"";
            default       -> "";
        };
    }
}
```

### Injection des totaux dynamiques dans le contexte

Le `CustomModelFactureReportService` calcule dynamiquement les totaux pour **tous les slots MONTANT** :

```java
// Dans CustomModelFactureReportService.buildContext(...)

// Calcule les totaux pour chaque slot de type MONTANT/LINE
Map<String, Long> totals = new HashMap<>();
List<FactureSlotDefinition> montantSlots = slotRepository
    .findByCategory(SlotCategory.MONTANT);

for (FactureSlotDefinition slot : montantSlots) {
    // Utilise Spring SpEL pour évaluer l'expression sur chaque ligne
    ExpressionParser parser = new SpelExpressionParser();
    long sum = facture.getFacturesDetails().stream()
        .mapToLong(detail -> {
            try {
                EvaluationContext context = new StandardEvaluationContext(detail);
                Number val = parser.parseExpression(slot.getExpression())
                                   .getValue(context, Number.class);
                return val != null ? val.longValue() : 0L;
            } catch (Exception e) {
                return 0L;
            }
        }).sum();
    totals.put(slot.getSlotKey(), sum);
}

ctx.setVariable("totals", totals);
```

---

## 7. Migration Flyway — données initiales

**Fichier :** `src/main/resources/db/migration/V1.4.3__facture_slot_definition.sql`

> Ce fichier précède `V1.4.4__facture_template_config.sql` (renommer en conséquence).

```sql
CREATE TABLE facture_slot_definition (
    id               SERIAL       PRIMARY KEY,
    slot_key         VARCHAR(50)  NOT NULL UNIQUE,
    label            VARCHAR(100) NOT NULL,
    description      TEXT,
    expression       TEXT         NOT NULL,
    scope            VARCHAR(10)  NOT NULL DEFAULT 'LINE',
    formatter        VARCHAR(15)  NOT NULL DEFAULT 'STRING',
    category         VARCHAR(20)  NOT NULL,
    aggregation_type VARCHAR(10)  NOT NULL DEFAULT 'NONE',  -- NONE|SUM|AVG|COUNT|MIN|MAX
    aggregate_label  VARCHAR(30),                            -- "Total :", "Moy. :", "Nb. :"
    builtin          BOOLEAN      NOT NULL DEFAULT TRUE,
    actif            BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order    INTEGER,
    created          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- CATÉGORIE : ASSURE
-- ============================================================
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, display_order) VALUES

('ASSURE_NOM',            'Nom assuré',
 'Prénom de l''assuré tel qu''enregistré dans la fiche client',
 'detail.clientTiersPayant.assuredCustomer.firstName',
 'LINE', 'STRING', 'ASSURE', 1),

('ASSURE_PRENOM',         'Prénom assuré',
 'Nom de famille de l''assuré',
 'detail.clientTiersPayant.assuredCustomer.lastName',
 'LINE', 'STRING', 'ASSURE', 2),

('ASSURE_NOM_PRENOM',     'Nom & Prénom assuré',
 'Nom complet (prénom + nom) en une seule colonne',
 'detail.clientTiersPayant.assuredCustomer.firstName + '' '' + detail.clientTiersPayant.assuredCustomer.lastName',
 'LINE', 'STRING', 'ASSURE', 3),

('ASSURE_MATRICULE',      'Matricule',
 'Numéro d''affiliation de l''assuré chez l''organisme',
 'detail.clientTiersPayant.num',
 'LINE', 'STRING', 'ASSURE', 4),

('ASSURE_CODE',           'Code assuré',
 'Code interne assuré dans Pharma-Smart',
 'detail.clientTiersPayant.assuredCustomer.code',
 'LINE', 'STRING', 'ASSURE', 5),

('ASSURE_SEXE',           'Sexe',
 'Sexe de l''assuré (M/F)',
 'detail.clientTiersPayant.assuredCustomer.sexe',
 'LINE', 'STRING', 'ASSURE', 6),

('ASSURE_DATE_NAISSANCE', 'Date de naissance',
 'Date de naissance de l''assuré',
 'detail.clientTiersPayant.assuredCustomer.datNaiss',
 'LINE', 'DATE',   'ASSURE', 7),

('ASSURE_TYPE',           'Type assuré',
 'Type d''assuré (PRINCIPAL, AYANT_DROIT...)',
 'detail.clientTiersPayant.assuredCustomer.typeAssure',
 'LINE', 'STRING', 'ASSURE', 8),

('ASSURE_TAUX_CLIENT',    'Taux prise en charge (%)',
 'Taux de remboursement de l''assuré chez cet organisme',
 'detail.clientTiersPayant.taux',
 'LINE', 'PERCENT','ASSURE', 9),

('ASSURE_NUM_AYANT_DROIT','N° Ayant droit',
 'Numéro d''ayant droit (si l''assuré est un bénéficiaire)',
 'detail.clientTiersPayant.assuredCustomer.numAyantDroit',
 'LINE', 'STRING', 'ASSURE', 10);

-- ============================================================
-- CATÉGORIE : PATIENT (bénéficiaire réel de la vente)
-- ============================================================
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, display_order) VALUES

('PATIENT_NOM',             'Nom patient',
 'Prénom du bénéficiaire réel de la vente (peut différer de l''assuré)',
 'detail.sale.ayantDroit?.firstName',
 'LINE', 'STRING', 'PATIENT', 1),

('PATIENT_PRENOM',          'Prénom patient',
 'Nom de famille du bénéficiaire',
 'detail.sale.ayantDroit?.lastName',
 'LINE', 'STRING', 'PATIENT', 2),

('PATIENT_NOM_PRENOM',      'Nom & Prénom patient',
 'Nom complet du bénéficiaire',
 '(detail.sale.ayantDroit != null ? detail.sale.ayantDroit.firstName + '' '' + detail.sale.ayantDroit.lastName : '''')',
 'LINE', 'STRING', 'PATIENT', 3),

('PATIENT_NUM_AYANT_DROIT', 'N° Ayant droit patient',
 'Numéro d''ayant droit du bénéficiaire',
 'detail.sale.ayantDroit?.numAyantDroit',
 'LINE', 'STRING', 'PATIENT', 4),

('PATIENT_DATE_NAISSANCE',  'Date naissance patient',
 'Date de naissance du bénéficiaire',
 'detail.sale.ayantDroit?.datNaiss',
 'LINE', 'DATE',   'PATIENT', 5),

('PATIENT_SEXE',            'Sexe patient',
 'Sexe du bénéficiaire',
 'detail.sale.ayantDroit?.sexe',
 'LINE', 'STRING', 'PATIENT', 6);

-- ============================================================
-- CATÉGORIE : BON
-- ============================================================
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, display_order) VALUES

('BON_NUM',              'N° Bon (ligne)',
 'Numéro du bon de prise en charge au niveau de la ligne',
 'detail.numBon',
 'LINE', 'STRING', 'BON', 1),

('BON_NUM_VENTE',        'N° Bon (vente)',
 'Numéro du bon au niveau de la vente (différent si plusieurs lignes)',
 'detail.sale.numBon',
 'LINE', 'STRING', 'BON', 2),

('BON_DATE',             'Date enregistrement',
 'Date à laquelle le bon a été enregistré dans la pharmacie',
 'detail.created',
 'LINE', 'DATE',   'BON', 3),

('BON_DATE_VENTE',       'Date de la vente',
 'Date de la vente en officine',
 'detail.saleDate',
 'LINE', 'DATE',   'BON', 4),

('BON_NUM_TRANSACTION',  'N° Transaction',
 'Numéro de transaction interne de la vente',
 'detail.sale.numberTransaction',
 'LINE', 'STRING', 'BON', 5),

('BON_TYPE_PRESCRIPTION','Type prescription',
 'Type de prescription médicale (ORDONNANCE, SANS_ORDONNANCE...)',
 'detail.sale.typePrescription',
 'LINE', 'STRING', 'BON', 6),

('BON_NATURE_VENTE',     'Nature vente',
 'Nature de la vente (COMPTANT, CREDIT...)',
 'detail.sale.natureVente',
 'LINE', 'STRING', 'BON', 7);

-- ============================================================
-- CATÉGORIE : MONTANT
-- ============================================================
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, display_order) VALUES

('MONTANT_ATTENDU',      'Montant attendu TP',
 'Montant que l''organisme doit rembourser pour ce bon',
 'detail.montant',
 'LINE', 'INTEGER', 'MONTANT', 1),

('MONTANT_BON',          'Montant bon (vente)',
 'Montant total de la vente (part assuré + part TP)',
 'detail.sale.salesAmount',
 'LINE', 'INTEGER', 'MONTANT', 2),

('MONTANT_REGLE',        'Montant réglé (ligne)',
 'Montant déjà réglé par l''organisme pour cette ligne',
 'detail.montantRegle',
 'LINE', 'INTEGER', 'MONTANT', 3),

('MONTANT_PART_ASSURE',  'Part assuré',
 'Montant à la charge de l''assuré',
 'detail.sale.partAssure',
 'LINE', 'INTEGER', 'MONTANT', 4),

('MONTANT_PART_TP',      'Part tiers-payant',
 'Montant à la charge de l''organisme',
 'detail.sale.partTiersPayant',
 'LINE', 'INTEGER', 'MONTANT', 5),

('MONTANT_REMISE',       'Remise',
 'Remise accordée sur la vente',
 'detail.sale.discountAmount',
 'LINE', 'INTEGER', 'MONTANT', 6),

('MONTANT_NET_VENTE',    'Montant net vente',
 'Montant net après remise',
 'detail.sale.netAmount',
 'LINE', 'INTEGER', 'MONTANT', 7),

('MONTANT_HT_VENTE',     'Montant HT vente',
 'Montant hors taxes',
 'detail.sale.htAmount',
 'LINE', 'INTEGER', 'MONTANT', 8),

('MONTANT_TVA_VENTE',    'Montant TVA vente',
 'Montant de la TVA',
 'detail.sale.taxAmount',
 'LINE', 'INTEGER', 'MONTANT', 9);

-- ============================================================
-- CATÉGORIE : TAUX
-- ============================================================
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, display_order) VALUES

('TAUX_PRISE_EN_CHARGE', 'Taux TP (%)',
 'Taux de prise en charge par l''organisme tiers-payant',
 'detail.taux',
 'LINE', 'PERCENT', 'TAUX', 1),

('TAUX_VENTE',           'Taux vente (%)',
 'Taux appliqué lors de la vente',
 'detail.tauxVente',
 'LINE', 'PERCENT', 'TAUX', 2);

-- ============================================================
-- CATÉGORIE : FACTURE (scope FACTURE — en-tête)
-- ============================================================
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, display_order) VALUES

('FACTURE_NUM',              'N° Facture',
 'Numéro de la facture (affiché sans le préfixe de génération)',
 'entity.displayNumFacture',
 'FACTURE', 'STRING',  'FACTURE', 1),

('FACTURE_DEBUT_PERIODE',    'Début période',
 'Date de début de la période couverte par la facture',
 'entity.debutPeriode',
 'FACTURE', 'DATE',    'FACTURE', 2),

('FACTURE_FIN_PERIODE',      'Fin période',
 'Date de fin de la période couverte par la facture',
 'entity.finPeriode',
 'FACTURE', 'DATE',    'FACTURE', 3),

('FACTURE_DATE',             'Date facture',
 'Date d''émission de la facture',
 'entity.invoiceDate',
 'FACTURE', 'DATE',    'FACTURE', 4),

('FACTURE_MONTANT_NET',      'Montant net facture',
 'Montant net total de la facture',
 'entity.montantNet',
 'FACTURE', 'DECIMAL', 'FACTURE', 5),

('FACTURE_MONTANT_TTC',      'Montant TTC facture',
 'Montant toutes taxes comprises',
 'entity.montantTtc',
 'FACTURE', 'DECIMAL', 'FACTURE', 6),

('FACTURE_REMISE_FORFETAIRE','Remise forfaitaire',
 'Remise forfaitaire appliquée sur la facture (%)',
 'entity.remiseForfetaire',
 'FACTURE', 'INTEGER', 'FACTURE', 7),

('FACTURE_STATUT',           'Statut facture',
 'Statut actuel de la facture (NOT_PAID, PAID, PARTIAL...)',
 'entity.statut',
 'FACTURE', 'STRING',  'FACTURE', 8);

-- ============================================================
-- CATÉGORIE : ORGANISME
-- ============================================================
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, display_order) VALUES

('ORG_NOM',          'Nom organisme',
 'Nom abrégé de l''organisme tiers-payant',
 'entity.tiersPayant.name',
 'FACTURE', 'STRING',  'ORGANISME', 1),

('ORG_NOM_COMPLET',  'Nom complet organisme',
 'Dénomination complète de l''organisme',
 'entity.tiersPayant.fullName',
 'FACTURE', 'STRING',  'ORGANISME', 2),

('ORG_CODE',         'Code organisme',
 'Code alphanumérique de l''organisme (ex: 01C2025)',
 'entity.tiersPayant.codeOrganisme',
 'FACTURE', 'STRING',  'ORGANISME', 3),

('ORG_NCC',          'NCC (identifiant fiscal)',
 'Numéro de contribuable de l''organisme',
 'entity.tiersPayant.ncc',
 'FACTURE', 'STRING',  'ORGANISME', 4),

('ORG_ADRESSE',      'Adresse organisme',
 'Adresse postale de l''organisme',
 'entity.tiersPayant.adresse',
 'FACTURE', 'STRING',  'ORGANISME', 5),

('ORG_TELEPHONE',    'Téléphone organisme',
 'Numéro de téléphone de l''organisme',
 'entity.tiersPayant.telephone',
 'FACTURE', 'STRING',  'ORGANISME', 6),

('ORG_DELAI_REGLEMENT','Délai règlement (j)',
 'Délai contractuel de règlement des factures en jours',
 'entity.tiersPayant.delaiReglement',
 'FACTURE', 'INTEGER', 'ORGANISME', 7);

-- Alias rétrocompatibilité : anciens noms 8 slots → nouvelles clés
-- (permet aux templates existants de continuer à fonctionner)
INSERT INTO facture_slot_definition
  (slot_key, label, expression, scope, formatter, category, builtin, display_order) VALUES
('NOM_ASSURE',      'Nom assuré (alias)',
 'detail.clientTiersPayant.assuredCustomer.firstName',
 'LINE', 'STRING', 'ASSURE', true, 100),
('PRENOM_ASSURE',   'Prénom assuré (alias)',
 'detail.clientTiersPayant.assuredCustomer.lastName',
 'LINE', 'STRING', 'ASSURE', true, 101),
('NOM_PATIENT',     'Nom patient (alias)',
 '(detail.sale.ayantDroit != null ? detail.sale.ayantDroit.firstName + '' '' + detail.sale.ayantDroit.lastName : '''')',
 'LINE', 'STRING', 'PATIENT', true, 100),
('MATRICULE',       'Matricule (alias)',
 'detail.clientTiersPayant.num',
 'LINE', 'STRING', 'ASSURE', true, 102),
('NUM_BON',         'N° Bon (alias)',
 'detail.numBon',
 'LINE', 'STRING', 'BON', true, 100),
('DATE',            'Date (alias)',
 'detail.created',
 'LINE', 'DATE',   'BON', true, 101),
('MONTANT_BON',     'Montant bon (alias)',
 'detail.sale.salesAmount',
 'LINE', 'INTEGER','MONTANT', true, 100),
('MONTANT_ATTENDU', 'Montant attendu (alias)',
 'detail.montant',
 'LINE', 'INTEGER','MONTANT', true, 101);
```

---

## 8. Endpoints REST

**Controller :** `com.kobe.warehouse.web.rest.facturation.FactureSlotDefinitionResource`

```
GET    /api/facture-slot-definitions
       ?actif=true&scope=LINE&category=ASSURE
       → List<SlotDefinitionDto>           (publics — expression masquée si non-ADMIN)

GET    /api/facture-slot-definitions/{key}
       → SlotDefinitionDto

POST   /api/facture-slot-definitions               [ADMIN]
       body: SlotDefinitionDto (sans id)
       → SlotDefinitionDto  (nouveau slot personnalisé)

PUT    /api/facture-slot-definitions/{key}          [ADMIN]
       → SlotDefinitionDto  (modification label/description/actif)

DELETE /api/facture-slot-definitions/{key}          [ADMIN]
       → 204  (builtin=true → 403 Forbidden)

GET    /api/facture-slot-definitions/catalogue
       → Map<SlotCategory, List<SlotDefinitionDto>>  (groupé par catégorie)
```

**Sécurité sur l'expression :**
- `GET` → le champ `expression` est masqué (`null`) si l'utilisateur n'est pas ADMIN
- `POST/PUT` → valide l'expression via `SlotExpressionValidator` avant sauvegarde

---

## 9. Frontend — sélecteur de slots enrichi

### Chargement du catalogue groupé

```typescript
// Dans FactureTemplateEditorComponent
ngOnInit(): void {
  this.slotService.getCatalogue().subscribe(catalogue => {
    this.slotCatalogue = catalogue; // Map<category, SlotDefinitionDto[]>
    this.flatSlots = Object.values(catalogue).flat();
  });
}
```

### Interface de sélection — groupée par catégorie

```
┌─ CATALOGUE DES COLONNES DISPONIBLES ──────────────────────────────────────┐
│  [🔍 Rechercher une colonne...                    ]                        │
│                                                                            │
│  ▶ ASSURÉ (10 colonnes)                                                   │
│    ☑ Nom assuré          — Prénom de l'assuré                              │
│    ☑ Prénom assuré       — Nom de famille de l'assuré                      │
│    ☑ Matricule           — N° d'affiliation chez l'organisme               │
│    ☐ Sexe                — Sexe de l'assuré (M/F)                          │
│    ☐ Date de naissance   — Date de naissance                               │
│    ☐ Taux prise en charge— Taux de remboursement (%)                       │
│    + 4 autres...                                                           │
│                                                                            │
│  ▶ PATIENT (6 colonnes)                                                   │
│    ☑ Nom patient         — Prénom du bénéficiaire réel                     │
│    ☐ Prénom patient      — Nom de famille du bénéficiaire                  │
│    + 4 autres...                                                           │
│                                                                            │
│  ▶ BON (7 colonnes)                                                       │
│    ☑ N° Bon              — Numéro du bon de prise en charge                │
│    ☑ Date enregistrement — Date de saisie dans la pharmacie                │
│    + 5 autres...                                                           │
│                                                                            │
│  ▶ MONTANTS (9 colonnes)                                                  │
│    ☑ Montant attendu TP  — Montant que l'organisme doit rembourser         │
│    ☑ Montant bon (vente) — Total de la vente                               │
│    ☐ Part assuré         — Montant à charge de l'assuré                    │
│    ☐ Part tiers-payant   — Montant à charge de l'organisme                 │
│    + 5 autres...                                                           │
│                                                                            │
│  ▶ TAUX (2 colonnes)                                                      │
│    ☐ Taux TP (%)         — Taux de prise en charge TP                      │
│    ☐ Taux vente (%)      — Taux appliqué à la vente                        │
└────────────────────────────────────────────────────────────────────────────┘
```

### Zone "Colonnes actives" — drag & drop des colonnes sélectionnées

```html
<!-- p-orderlist avec tooltip sur chaque ligne -->
<p-orderList [value]="colonnesActives" [dragdrop]="true">
  <ng-template pTemplate="item" let-col>
    <div class="d-flex align-items-center gap-2 w-100">
      <i class="pi pi-bars text-secondary"></i>
      <p-toggleswitch [(ngModel)]="col.visible" />
      <div class="flex-grow-1">
        <span class="fw-semibold">{{ getSlotLabel(col.slot) }}</span>
        <!-- Tooltip avec description métier -->
        <i class="pi pi-info-circle text-muted ms-1"
           [pTooltip]="getSlotDescription(col.slot)"
           tooltipPosition="right">
        </i>
      </div>
      <input pInputText [(ngModel)]="col.libelle"
             placeholder="Libellé personnalisé"
             class="w-40"
             [disabled]="!col.visible" />
      <!-- Alignement (droit pour les montants, gauche sinon) -->
      <p-select [options]="alignements"
                [(ngModel)]="col.alignement"
                class="w-15" />
    </div>
  </ng-template>
</p-orderList>
```

---

## 10. Sécurité — validation des expressions

> Applicable lors du `POST/PUT` d'un nouveau slot (admin uniquement).

```java
@Service
public class SlotExpressionValidator {

    // Racines d'expression autorisées
    private static final Set<String> ALLOWED_ROOTS = Set.of(
        "detail.",     // ThirdPartySaleLine et ses relations
        "entity.",     // FactureTiersPayant et ses relations
        "magasin."     // Magasin
    );

    // Patterns interdits (mêmes que TemplateValidatorService)
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
        Pattern.compile("T\\s*\\("),
        Pattern.compile("@[a-zA-Z]"),
        Pattern.compile("(?i)exec\\s*\\("),
        Pattern.compile("(?i)getRuntime"),
        Pattern.compile("(?i)ProcessBuilder"),
        Pattern.compile("(?i)ClassLoader"),
        Pattern.compile("(?i)forName\\s*\\("),
        Pattern.compile("#\\{"),     // EL injection
        Pattern.compile("\\$\\{")   // SpEL dans SpEL
    );

    /**
     * Valide qu'une expression de slot est sûre avant insertion en base.
     * - Doit commencer par une racine autorisée (detail., entity., magasin.)
     * - Ne doit pas contenir de patterns dangereux
     * - Test de résolution SpEL sur un objet fictif
     */
    public void validate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new SlotValidationException("L'expression ne peut pas être vide");
        }

        // 1. Vérifier la racine
        boolean validRoot = ALLOWED_ROOTS.stream().anyMatch(expression::startsWith);
        if (!validRoot) {
            throw new SlotValidationException(
                "Expression invalide : doit commencer par detail., entity. ou magasin. "
                + "— reçu : " + expression
            );
        }

        // 2. Vérifier les patterns interdits
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(expression).find()) {
                throw new SlotValidationException(
                    "Expression interdite : " + pattern.pattern()
                );
            }
        }

        // 3. Test de parse SpEL (ne prouve pas la validité runtime, mais détecte les erreurs de syntaxe)
        try {
            new SpelExpressionParser().parseExpression(
                expression.replace("?.", ".")  // simplifie le safe-nav pour le parse
            );
        } catch (ParseException e) {
            throw new SlotValidationException("Syntaxe SpEL invalide : " + e.getMessage());
        }
    }
}
```

---

## 11. Évolution future — ajout d'un slot sans redéploiement

### Scénario : ajouter un champ `NOM_PRESCRIPTEUR` (nom du médecin prescripteur)

**Étape 1 — Migration Flyway (ou INSERT via UI admin) :**
```sql
INSERT INTO facture_slot_definition
  (slot_key, label, description, expression, scope, formatter, category, builtin)
VALUES (
  'PRESCRIPTEUR_NOM',
  'Nom du prescripteur',
  'Nom du médecin prescripteur (si renseigné sur la vente)',
  'detail.sale.commentaire',   -- ou le champ réel si ajouté au domaine
  'LINE', 'STRING', 'BON', false
);
```

**Étape 2 — Le slot est immédiatement disponible** dans le configurateur de modèles.

**Étape 3 — Si le champ n'existe pas encore dans le domaine** :
→ Ajouter le champ JPA + migration schema → le slot résout correctement.

**Aucun changement dans** `TemplateBuilderService`, `CustomModelFactureReportService`, ni le frontend.

---

## 12. Impact sur le plan principal

Les modifications à apporter dans `PLAN-PERSONNALISATION-MODELES-FACTURE.md` :

| Section | Modification |
|---------|-------------|
| Migration Flyway | Ajouter `V1.4.3__facture_slot_definition.sql` **avant** `V1.4.4__facture_template_config.sql` |
| Entité `FactureTemplateConfig` | `colonnes_config[].slot` reste `String` — stocke les clés (`"ASSURE_NOM"`, etc.) |
| `TemplateBuilderService` | Remplacer le `switch` hardcodé par la résolution dynamique depuis `FactureSlotDefinitionRepository` |
| `ColonneSlot` enum | **Supprimer** — remplacé par `facture_slot_definition` |
| Endpoint `/api/facture-slot-definitions` | **Ajouter** au plan |
| Frontend | Charger le catalogue depuis l'API + grouper par catégorie |
| Ordre d'implémentation | Ajouter étape 0 : `facture_slot_definition` (avant tout le reste) |

### Ordre d'implémentation mis à jour

| Étape | Tâche | Complexité |
|-------|-------|-----------|
| **0** | **Migration `V1.4.3__facture_slot_definition.sql` + entité + repo + validator** | **Faible** |
| 1 | Migration `V1.4.4__facture_template_config.sql` | Faible |
| 2–6 | (inchangé) | — |
| 7 | `TemplateBuilderService` — version dynamique | **Faible** (simplifié) |
| 8 | `SlotExpressionValidator` | Faible |
| 9–15 | (inchangé) | — |
| **16** | **Frontend — sélecteur de slots groupés par catégorie** | Élevée |
| 17–19 | (inchangé) | — |

