# Plan d'Implémentation — Module Facturation (Officine)

> **Contexte :** Pharma-Smart — Spring Boot 4 / Angular 21 / PostgreSQL  18
> **Base existante :** `FactureTiersPayant`, `ThirdPartySaleLine`, `TiersPayant`, `GroupeTiersPayant`,
> `InvoicePayment extends PaymentTransaction`, patterns Specification + CriteriaQuery + PDF Thymeleaf

---

## Table des matières

1. [Récapitulatif mensuel par tiers-payant](#1-récapitulatif-mensuel-par-tiers-payant)
2. [État de rapprochement](#2-état-de-rapprochement)
3. [Avoir / note de crédit](#3-avoir--note-de-crédit)
4. [Facturation périodique automatisée](#4-facturation-périodique-automatisée)
5. [Personnalisation des modèles PDF](#5-personnalisation-des-modèles-pdf--priorité-basse)

> **Note de numérotation :** les sections 2 à 5 dans le document gardent leur numérotation
> d'origine pour éviter des éditions massives. Le tableau des priorités ci-dessous fait foi.

---

## 1. Récapitulatif mensuel par tiers-payant

### Contexte métier (officine)

Le récapitulatif mensuel est le **relevé de compte** envoyé chaque mois à chaque organisme
tiers-payant (mutuelles, CNAM, assurances). Il récapitule :

- Toutes les factures émises sur la période
- Les règlements reçus (via `InvoicePayment`)
- Le solde restant dû
- Le taux de recouvrement
- Le report du solde du mois précédent

### Architecture cible

```
service/facturation/
└── service/
    ├── RecapitulatifMensuelService.java
    └── RecapitulatifMensuelServiceImpl.java

service/facturation/dto/
├── RecapitulatifMensuelDto.java
├── RecapitulatifMensuelRow.java
└── RecapitulatifMensuelParams.java   (record)

web/rest/facturation/
└── RecapitulatifFactureResource.java

resources/templates/facturation/recapitulatif/
├── main.html
├── header.html
└── body.html
```

### DTOs

```java
public record RecapitulatifMensuelParams(
    int annee,
    int mois,
    List<Integer> tiersPayantIds,  // vide = tous
    List<Integer> groupIds,
    TypeFacture typeFacture
) {}

public record RecapitulatifMensuelDto(
    String     tiersPayantName,
    String     tiersPayantCode,
    YearMonth  periode,
    BigDecimal soldePrecedent,     // non-réglé avant debutPeriode
    BigDecimal totalFacture,       // SUM(montant_net) sur la période
    BigDecimal totalRegle,         // SUM(paid_amount) des InvoicePayment liés
    BigDecimal soldeActuel,        // totalFacture - totalRegle
    BigDecimal soldeCumule,        // soldePrecedent + soldeActuel
    int        nombreFactures,
    int        nombreImpayees,
    List<RecapitulatifMensuelRow> lignes
) {}

public record RecapitulatifMensuelRow(
    String        numFacture,
    LocalDate     invoiceDate,
    LocalDate     echeance,         // invoiceDate + delaiReglement
    BigDecimal    montantNet,
    BigDecimal    montantRegle,     // SUM InvoicePayment.paidAmount
    BigDecimal    restantDu,
    InvoiceStatut statut
) {}
```

### Requêtes SQL

```sql
-- Récapitulatif agrégé par tiers-payant sur la période
SELECT
    tp.id,
    tp.name                                      AS tiersPayantName,
    tp.code_organisme                            AS tiersPayantCode,
    tp.delai_reglement,
    COALESCE(SUM(f.montant_net), 0)              AS totalFacture,
    COALESCE(SUM(ip_agg.montant_regle), 0)       AS totalRegle,
    COUNT(f.id)                                  AS nombreFactures,
    COUNT(f.id) FILTER (WHERE f.statut != 'PAID') AS nombreImpayees
FROM tiers_payant tp
LEFT JOIN facture_tiers_payant f
    ON f.tiers_payant_id = tp.id
    AND f.invoice_date BETWEEN :debut AND :fin
    AND f.groupe_facture_tiers_payant_id IS NULL
LEFT JOIN (
    SELECT facture_tierspayant_id, facture_tierspayant_invoice_date,
           SUM(paid_amount) AS montant_regle
    FROM payment_transaction
    WHERE dtype = 'InvoicePayment'
    GROUP BY facture_tierspayant_id, facture_tierspayant_invoice_date
) ip_agg ON ip_agg.facture_tierspayant_id = f.id
         AND ip_agg.facture_tierspayant_invoice_date = f.invoice_date
GROUP BY tp.id, tp.name, tp.code_organisme, tp.delai_reglement
ORDER BY tp.name;

-- Solde précédent (factures non soldées antérieures à la période)
SELECT COALESCE(SUM(f.montant_net) - COALESCE(SUM(ip_agg.montant_regle), 0), 0)
FROM facture_tiers_payant f
LEFT JOIN (
    SELECT facture_tierspayant_id, SUM(paid_amount) AS montant_regle
    FROM payment_transaction WHERE dtype = 'InvoicePayment'
    GROUP BY facture_tierspayant_id
) ip_agg ON ip_agg.facture_tierspayant_id = f.id
WHERE f.tiers_payant_id = :tiersPayantId
  AND f.invoice_date < :debutPeriode
  AND f.statut != 'PAID'
  AND f.groupe_facture_tiers_payant_id IS NULL;
```

### Endpoints REST

```
GET /api/edition-factures/recapitulatif
    ?annee=2025&mois=3&tiersPayantIds=1,2&typeFacture=INDIVIDUAL
    → Page<RecapitulatifMensuelDto>

GET /api/edition-factures/recapitulatif/pdf?annee=2025&mois=3&tiersPayantId=1
    → Blob (PDF)

GET /api/edition-factures/recapitulatif/excel?annee=2025&mois=3
    → Blob (XLSX)
```

### Frontend

- **Composant** : `facturation/recapitulatif/recapitulatif.component.ts`
- **Filtres** : mois/année (`p-select`), tiers-payant (autocomplete), type
- **Tableau p-table** : Organisme | Solde N-1 | Facturé | Réglé | Restant | Taux
- **Actions** : Export PDF par organisme | Export global Excel
- **Chart.js** : Bar chart réglé vs restant par organisme

### Migrations Flyway

Aucune — données issues de `facture_tiers_payant` et `payment_transaction` existants.

---

## 2. Personnalisation des modèles PDF — configuration complète via `facture_template_config`

> **Priorité : BASSE** — à implémenter après les features 1, 3, 4, 5.

### Principe de non-régression — le système existant est CONSERVÉ INTACT

Le mécanisme actuel de génération PDF (`modelFacture` → classpath `body.html`) **ne sera ni modifié
ni supprimé**. Il continue à fonctionner pour tous les tiers-payants qui utilisent les modèles
système (`default`, `0203`, `0903`, `0907`).

Un **service séparé** (`CustomModelFactureReportService`) gère uniquement les modèles personnalisés
stockés en base. La sélection entre les deux services se fait en amont, dans
`FacturationPdfExportServiceImpl`, sans toucher à `FacturationReportServiceImpl`.

```
Génération PDF (FacturationPdfExportServiceImpl.export)
  │
  ├─ facture_template_config.findByCode(modelFilePath)
  │
  ├─ bodyTemplate == null  ou  config introuvable
  │    → FacturationReportServiceImpl (EXISTANT, INCHANGÉ)
  │         templateEngine.process("facturation/main", context)
  │         → classpath model/{code}/body.html
  │
  └─ bodyTemplate != null  (modèle custom créé par le pharmacien)
       → CustomModelFactureReportService (NOUVEAU, SÉPARÉ)
            FactureTemplateDbResolver résout "facturation/model/{code}/body"
            → body_template depuis facture_template_config
```

**Ce qui ne change pas :**
- `FacturationReportServiceImpl` — aucune modification
- `main.html`, `header.html`, `css.html`, `total.html` — aucune modification
- Les 4 `body.html` classpath (`default`, `0203`, `0903`, `0907`) — aucune modification
- `TiersPayant.modelFacture` + `getModelFilePath()` — aucune modification

### Analyse de l'existant

Les templates Thymeleaf sont dans `src/main/resources/templates/facturation/` :

```
facturation/
├── main.html        ← point d'entrée, charge les fragments
├── header.html      ← pharmacie + organisme + N° facture + dates
├── css.html         ← styles communs
├── total.html       ← arrêté en lettres + signature pharmacien
├── group/           ← variante pour factures groupées
│   ├── main.html
│   ├── header.html
│   └── body.html
└── model/           ← SEULE PARTIE qui change entre modèles
    ├── default/body.html
    ├── 0203/body.html
    ├── 0903/body.html
    └── 0907/body.html
```

**Sélection du template** dans `main.html` :
```html
<table th:replace="~{'facturation/model/' + ${entity.tiersPayant.modelFilePath}+'/body'}">
```

**Variables disponibles dans tous les templates :**

| Variable | Type | Contenu |
|----------|------|---------|
| `entity` | `FactureTiersPayant` | Facture complète avec relations |
| `entity.facturesDetails` | `List<ThirdPartySaleLine>` | Lignes de vente |
| `entity.tiersPayant` | `TiersPayant` | Organisme |
| `entity.groupeTiersPayant` | `GroupeTiersPayant` | Groupe si groupée |
| `magasin` | objet Store | Nom, adresse, téléphone pharmacie |
| `grandTotal` | `Pair<e1,e2>` | e1=total bons, e2=total attendu |
| `invoiceTotalAmountLetters` | `String` | Montant en lettres |

**Analyse des différences entre les 4 modèles existants :**

| Colonne | default | 0203 | 0903 | 0907 |
|---------|:-------:|:----:|:----:|:----:|
| Date | ✓ | ✓ (1ère) | ✓ (1ère) | ✓ |
| Nom assuré | ✓ | ✓ | ✓ | ✓ (combiné) |
| Prénom assuré | ✓ | ✓ | ✓ | — (combiné) |
| Nom patient (ayantDroit) | — | ✓ | — | ✓ |
| Matricule | ✓ | ✓ | ✓ | ✓ |
| N° Bon / N° BPC | "N°Bon" | "N°BPC" | "N°Bon" | "N°Bon" |
| Montant bon (salesAmount) | ✓ | — | ✓ | ✓ |
| Montant attendu (montant) | ✓ | ✓ | ✓ | ✓ |

→ **Conclusion** : la personnalisation se réduit à **activer/désactiver des colonnes prédéfinies**,
changer leur libellé, et réordonner. Aucune colonne "libre" n'est nécessaire.

### Faisabilité d'une configuration complète avec Thymeleaf + Flying Saucer

#### Chaîne de génération actuelle

```
SpringTemplateEngine.process("facturation/main", context)
  ├─ ClassLoaderTemplateResolver (classpath) résout :
  │   ├─ th:replace="~{facturation/css}"      → css.html
  │   ├─ th:replace="~{facturation/header}"   → header.html
  │   ├─ th:replace="~{facturation/model/*/body}" → body.html (statique)
  │   └─ th:replace="~{facturation/total}"   → total.html
  └─ HTML String
       └─ ITextRenderer.setDocumentFromString(html) → PDF (Flying Saucer)
```

**Point clé** : `ITextRenderer.setDocumentFromString(String)` accepte n'importe quel XHTML,
quelle que soit son origine (classpath, base de données, génération programmatique).

#### Réponse : OUI, entièrement faisable — via un `ITemplateResolver` DB custom

Thymeleaf supporte plusieurs `ITemplateResolver` enregistrés en **chaîne prioritaire**.
Si on insère un resolver DB avec priorité haute, Thymeleaf l'interroge en premier pour chaque
résolution de template ou de fragment. S'il ne trouve pas, il passe au classpath resolver.

```
SpringTemplateEngine
  ├─ [Ordre 1] FactureTemplateDbResolver  ← nouveau
  │   Gère : "facturation/model/*/body"   → lit facture_template_config en DB
  │   Passe  : tout le reste              → résolu par le classpath resolver
  └─ [Ordre 2] ClassLoaderTemplateResolver (existant)
       Gère : css, header, total, group/*, autres templates
```

**Ce qui change dans `main.html`** : rien. Le `th:replace` existant continue à fonctionner :
```html
<!-- main.html — inchangé -->
<table th:replace="~{'facturation/model/' + ${entity.tiersPayant.modelFilePath}+'/body'}">
```
- Si `modelFilePath = "default"` → DB resolver cherche, ne trouve pas → classpath `model/default/body.html`
- Si `modelFilePath = "custom_1"` → DB resolver trouve → retourne le template de la DB

Les fragments `~{facturation/css}`, `~{facturation/header}`, `~{facturation/total}` continuent
à être résolus par le classpath resolver (non touchés).

#### Ce qui peut être entièrement configuré en base

```
facture_template_config
├── body_template    TEXT        ← HTML Thymeleaf complet du <table> (corps facture)
├── css_overrides    TEXT        ← CSS additionnel injecté dans css.html
├── page_config      JSONB       ← format page, marges, orientation
└── display_config   JSONB       ← options d'affichage (signature, montant lettres…)
```

Les fragments statiques (`header.html`, `css.html`, `total.html`) restent sur le classpath.
Seul le `body` est configurable — c'est la seule partie qui varie entre modèles.

Si le besoin d'un **header entièrement configurable** émerge, le même mécanisme s'applique
en ajoutant un `header_template TEXT` et en enregistrant `facturation/header` dans le resolver DB.

### Architecture retenue (Option B — resolver DB + JSONB config)

#### 2.1 Entité `FactureTemplateConfig`

```java
@Entity
@Table(name = "facture_template_config")
public class FactureTemplateConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;        // "default", "0203", "custom_1" ...

    @Column(nullable = false, length = 100)
    private String libelle;     // "Modèle CNAM personnalisé"

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "systeme", nullable = false)
    private boolean systeme = false;  // true = non supprimable, non éditable en HTML

    // ── Corps du tableau (HTML Thymeleaf complet) ──────────────────────────────
    // Null pour les modèles système → le classpath resolver prend le relais
    // Non-null pour les modèles custom → utilisé par FactureTemplateDbResolver
    @Column(name = "body_template", columnDefinition = "TEXT")
    private String bodyTemplate;

    // ── CSS additionnel (injecté après css.html) ───────────────────────────────
    @Column(name = "css_overrides", columnDefinition = "TEXT")
    private String cssOverrides;

    // ── Configuration de la page ───────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "page_config", columnDefinition = "jsonb")
    private PageConfig pageConfig;

    // ── Configuration d'affichage ──────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "display_config", columnDefinition = "jsonb")
    private DisplayConfig displayConfig;

    // ── Configuration des colonnes (UI drag-and-drop) ─────────────────────────
    // Utilisée pour générer bodyTemplate via TemplateBuilderService
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "colonnes_config", columnDefinition = "jsonb")
    private List<ColonneConfig> colonnes;

    @Column(name = "created", nullable = false) private LocalDateTime created;
    @Column(name = "updated", nullable = false) private LocalDateTime updated;
}

// ColonneConfig — config UI (drag-and-drop), sert à générer bodyTemplate
public record ColonneConfig(
    String  slot,        // NOM_ASSURE, PRENOM_ASSURE, NOM_PATIENT, MATRICULE,
                         // NUM_BON, DATE, MONTANT_BON, MONTANT_ATTENDU
    String  libelle,     // libellé de la colonne dans l'en-tête PDF
    boolean visible,
    int     ordre,
    String  alignement   // "left", "center", "right"
) {}

// PageConfig — configuration de la page PDF
public record PageConfig(
    String  format,        // "A4", "A5", "LETTER"
    String  orientation,   // "PORTRAIT", "LANDSCAPE"
    String  marginTop,     // "30mm"
    String  marginRight,   // "20px"
    String  marginBottom,  // "20mm"
    String  marginLeft     // "20px"
) {}

// DisplayConfig — options d'affichage
public record DisplayConfig(
    boolean showMontantLetters,   // afficher "arrêté la présente facture à la somme de..."
    boolean showSignature,        // afficher la zone signature pharmacien
    boolean showTauxCouverture,   // afficher le taux de couverture par bon
    String  titreFacultatif       // titre personnalisé au-dessus du tableau (ex: "LISTE DES BONS")
) {}
```

#### 2.2 `FactureTemplateDbResolver` — resolver Thymeleaf DB-backed

```java
/**
 * ITemplateResolver prioritaire (ordre 1) qui résout les fragments body
 * "facturation/model/{code}/body" depuis la table facture_template_config.
 * Toute autre résolution (css, header, total) est ignorée → classpath resolver.
 */
@Component
public class FactureTemplateDbResolver implements ITemplateResolver {

    private static final String BODY_PREFIX = "facturation/model/";
    private static final String BODY_SUFFIX = "/body";

    private final FactureTemplateConfigRepository repository;

    @Override
    public String getName() { return "FactureTemplateDbResolver"; }

    @Override
    public Integer getOrder() { return 1; }   // avant ClassLoaderTemplateResolver (ordre 2)

    @Override
    public TemplateResolution resolveTemplate(
        IEngineConfiguration config,
        String ownerTemplate,
        String template,
        Map<String, Object> templateResolutionAttributes
    ) {
        // Ne traite que "facturation/model/*/body"
        if (!template.startsWith(BODY_PREFIX) || !template.endsWith(BODY_SUFFIX)) {
            return null;    // null = passe au resolver suivant (classpath)
        }

        String code = template
            .substring(BODY_PREFIX.length(), template.length() - BODY_SUFFIX.length());

        return repository.findByCodeAndBodyTemplateIsNotNull(code)
            .map(config2 -> new TemplateResolution(
                template,
                template,
                new StringTemplateResource(config2.getBodyTemplate()),
                false,    // pas de cache (la config peut changer)
                TemplateMode.HTML
            ))
            .orElse(null);  // null = passe au classpath resolver → body.html statique
    }
}

// StringTemplateResource — adapte un String en ITemplateResource Thymeleaf
public class StringTemplateResource implements ITemplateResource {
    private final String content;
    public StringTemplateResource(String content) { this.content = content; }

    @Override public String getDescription() { return "DB template"; }
    @Override public String getBaseName()    { return null; }
    @Override public boolean exists()        { return true; }
    @Override public Reader reader() throws IOException {
        return new StringReader(content);
    }
    @Override public ITemplateResource relative(String relativeLocation) { return null; }
}
```

**Enregistrement dans Spring** :

```java
@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringTemplateEngine templateEngine(
        FactureTemplateDbResolver dbResolver,
        ClassLoaderTemplateResolver classpathResolver   // bean existant
    ) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        // Le classpath resolver existant reste order=2 (ou plus)
        classpathResolver.setOrder(2);
        engine.addTemplateResolver(dbResolver);         // order=1
        engine.addTemplateResolver(classpathResolver);  // order=2
        return engine;
    }
}
```

#### 2.3 `TemplateBuilderService` — génère `bodyTemplate` depuis `colonnes_config`

```java
/**
 * Génère le HTML Thymeleaf du <table> depuis la configuration JSONB colonnes.
 * Appelé quand le pharmacien sauvegarde sa configuration via l'UI.
 * Le résultat est stocké dans facture_template_config.body_template.
 */
@Service
public class TemplateBuilderService {

    public String buildBodyTemplate(List<ColonneConfig> colonnes, DisplayConfig display) {
        List<ColonneConfig> visibles = colonnes.stream()
            .filter(ColonneConfig::visible)
            .sorted(Comparator.comparingInt(ColonneConfig::ordre))
            .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"main-table\">\n");

        // En-tête
        sb.append("  <thead><tr>\n");
        for (ColonneConfig col : visibles) {
            String cssClass = switch (col.alignement()) {
                case "right"  -> " class=\"text-right\"";
                case "center" -> " class=\"text-center\"";
                default       -> "";
            };
            sb.append("    <th").append(cssClass).append(">")
              .append(col.libelle()).append("</th>\n");
        }
        sb.append("  </tr></thead>\n");

        // Corps
        sb.append("  <tbody>\n");
        sb.append("    <tr th:each=\"detail : ${entity.facturesDetails}\">\n");
        for (ColonneConfig col : visibles) {
            String tdClass = "right".equals(col.alignement()) ? " class=\"unit\"" : "";
            sb.append("      <td").append(tdClass).append(">");
            sb.append(resolveExpression(col.slot()));
            sb.append("</td>\n");
        }
        sb.append("    </tr>\n  </tbody>\n");

        // Pied
        sb.append("  <tfoot class=\"datatable-footer\"><tr class=\"tr-footer\">\n");
        // ... (totaux dynamiques selon colonnes choisies)
        sb.append("  </tr></tfoot>\n");
        sb.append("</table>\n");

        return sb.toString();
    }

    private String resolveExpression(String slot) {
        return switch (slot) {
            case "DATE"           -> "<span th:text=\"${#temporals.format(detail.created,'dd/MM/yyyy')}\"></span>";
            case "NOM_ASSURE"     -> "<span th:text=\"${detail.clientTiersPayant.assuredCustomer.firstName}\"></span>";
            case "PRENOM_ASSURE"  -> "<span th:text=\"${detail.clientTiersPayant.assuredCustomer.lastName}\"></span>";
            case "NOM_PATIENT"    -> "<span th:text=\"${detail.sale.ayantDroit.firstName + ' ' + detail.sale.ayantDroit.lastName}\"></span>";
            case "MATRICULE"      -> "<span th:text=\"${detail.clientTiersPayant.num}\"></span>";
            case "NUM_BON"        -> "<span th:text=\"${detail.numBon}\"></span>";
            case "MONTANT_BON"    -> "<span th:text=\"${#numbers.formatInteger(detail.sale.salesAmount,3,'WHITESPACE')}\"></span>";
            case "MONTANT_ATTENDU"-> "<span th:text=\"${#numbers.formatInteger(detail.montant,3,'WHITESPACE')}\"></span>";
            default               -> "";
        };
    }
}
```

#### 2.4 `CustomModelFactureReportService` — service séparé pour les modèles custom

```java
/**
 * Service SÉPARÉ pour les modèles PDF personnalisés stockés en base.
 * N'hérite PAS de CommonReportService pour ne pas interférer avec l'existant.
 * FacturationReportServiceImpl reste inchangé.
 */
@Service
public class CustomModelFactureReportService {

    private final SpringTemplateEngine templateEngine;   // bean Spring existant (+ DbResolver enregistré)
    private final StorageService        storageService;
    private final FileStorageProperties fileStorageProperties;
    private final FactureTemplateConfigRepository templateConfigRepository;

    public Resource printToPdf(FactureTiersPayant facture) throws ReportFileExportException {
        FactureTemplateConfig config = templateConfigRepository
            .findByCode(facture.getTiersPayant().getModelFilePath())
            .orElseThrow(ReportFileExportException::new);

        Context context = buildContext(facture, config);
        String html = templateEngine.process(Constant.FACTURATION_TEMPLATE_FILE, context);
        // FACTURATION_TEMPLATE_FILE = "facturation/main" — même template principal, inchangé
        // Le DbResolver intercepte "facturation/model/{code}/body" → body_template depuis DB
        // css/header/total → classpath (inchangés)

        return generatePdf(html, facture.getNumFacture());
    }

    private Context buildContext(FactureTiersPayant facture, FactureTemplateConfig config) {
        Magasin magasin = storageService.getUser().getMagasin();
        Tuple4 total    = buildSummary(facture);

        Context ctx = new Context(Locale.forLanguageTag("fr"));
        ctx.setVariable(Constant.ENTITY,                facture);
        ctx.setVariable(Constant.MAGASIN,               magasin);
        ctx.setVariable(Constant.FOOTER,                "\"" + buildFooter(magasin) + "\"");
        ctx.setVariable(Constant.FACTURE_TOTAL,         total);
        ctx.setVariable(Constant.FACTURE_TOTAL_LETTERS, NumberUtil.getNumberToWords(total.e2()).toUpperCase());
        // Variables additionnelles pour les templates custom
        ctx.setVariable("cssOverrides",   config.getCssOverrides());
        ctx.setVariable("pageConfig",     config.getPageConfig());
        ctx.setVariable("displayConfig",  config.getDisplayConfig());
        return ctx;
    }

    private Resource generatePdf(String html, String numFacture) {
        String filePath = buildDestPath(numFacture);
        try (OutputStream out = new FileOutputStream(filePath)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.getSharedContext().setPrint(true);
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
        } catch (IOException e) {
            log.error("CustomModelFactureReportService.generatePdf", e);
        }
        return getResource(filePath);
    }
}
```

#### 2.5 Dispatch dans `FacturationPdfExportServiceImpl`

```java
// FacturationPdfExportServiceImpl — SEULE modification dans le code existant
// Ajout du dispatch : custom vs existant

@Service
public class FacturationPdfExportServiceImpl implements FacturationPdfExportService {

    private final FacturationReportService        reportService;          // EXISTANT, inchangé
    private final CustomModelFactureReportService customReportService;    // NOUVEAU
    private final FactureTemplateConfigRepository templateConfigRepository;

    @Override
    public byte[]  export(List<FactureTiersPayant> factures) throws ReportFileExportException {
        // Toutes les factures d'un même batch ont le même modèle
        FactureTiersPayant first = factures.getFirst();
        if (hasCustomTemplate(first)) {
            return customReportService.printToPdf(factures);
        }
        return reportService.exportReportToPdf(factures);   // chemin existant, inchangé
    }

    private boolean hasCustomTemplate(FactureTiersPayant facture) {
        return templateConfigRepository
            .findByCode(facture.getTiersPayant().getModelFilePath())
            .map(c -> c.getBodyTemplate() != null)
            .orElse(false);
    }
}
```

#### 2.6 Injection CSS overrides et page config (uniquement dans le service custom)

`css.html` est étendu **avec des variables optionnelles** — les variables `cssOverrides` et
`pageConfig` sont `null` quand le service existant est utilisé, donc aucun effet sur l'existant :

```html
<!-- css.html — ajout à la fin du <style>, inoffensif si variables nulles -->
<style th:inline="css">
  /* ... styles existants inchangés ... */
  [(${cssOverrides != null ? cssOverrides : ''})]

  @page {
    size: [(${pageConfig?.format ?: 'A4'})] [(${pageConfig?.orientation ?: 'PORTRAIT'})];
    margin-top:    [(${pageConfig?.marginTop    ?: '30mm'})];
    margin-right:  [(${pageConfig?.marginRight  ?: '20px'})];
    margin-bottom: [(${pageConfig?.marginBottom ?: '20mm'})];
    margin-left:   [(${pageConfig?.marginLeft   ?: '20px'})];
  }
</style>
```

#### 2.7 Slots de colonnes disponibles

```java
public enum ColonneSlot {
    DATE            ("Date",             "detail.created",                          "dd/MM/yyyy"),
    NOM_ASSURE      ("Nom",              "detail.clientTiersPayant.assuredCustomer.firstName", null),
    PRENOM_ASSURE   ("Prénom(s)",        "detail.clientTiersPayant.assuredCustomer.lastName",  null),
    NOM_PATIENT     ("Patient",          "detail.sale.ayantDroit.firstName + ' ' + ...",       null),
    MATRICULE       ("Matricule",        "detail.clientTiersPayant.num",            null),
    NUM_BON         ("N° Bon",           "detail.numBon",                           null),
    MONTANT_BON     ("Montant Bon",      "detail.sale.salesAmount",                 "integer"),
    MONTANT_ATTENDU ("Montant Attendu",  "detail.montant",                          "integer");
}
```

#### 2.8 Template Thymeleaf dynamique unique (`model/dynamic/body.html`)

```html
<!-- model/dynamic/body.html — remplace les 4 templates statiques -->
<table class="main-table">
  <thead>
    <tr>
      <!-- Itère sur les colonnes visibles triées par ordre -->
      <th th:each="col : ${colonnesVisibles}"
          th:text="${col.libelle}"
          th:class="${col.alignement == 'right' ? 'text-right' :
                     col.alignement == 'center' ? 'text-center' : ''}">
      </th>
    </tr>
  </thead>
  <tbody>
    <tr th:each="detail : ${entity.facturesDetails}">
      <td th:each="col : ${colonnesVisibles}"
          th:class="${col.alignement == 'right' ? 'unit' : ''}"
          th:switch="${col.slot}">
        <span th:case="'DATE'"
              th:text="${#temporals.format(detail.created, 'dd/MM/yyyy')}"></span>
        <span th:case="'NOM_ASSURE'"
              th:text="${detail.clientTiersPayant.assuredCustomer.firstName}"></span>
        <span th:case="'PRENOM_ASSURE'"
              th:text="${detail.clientTiersPayant.assuredCustomer.lastName}"></span>
        <span th:case="'NOM_PATIENT'"
              th:text="${detail.sale.ayantDroit.firstName + ' ' + detail.sale.ayantDroit.lastName}"></span>
        <span th:case="'MATRICULE'"
              th:text="${detail.clientTiersPayant.num}"></span>
        <span th:case="'NUM_BON'"
              th:text="${detail.numBon}"></span>
        <span th:case="'MONTANT_BON'"
              th:text="${#numbers.formatInteger(detail.sale.salesAmount, 3, 'WHITESPACE')}"></span>
        <span th:case="'MONTANT_ATTENDU'"
              th:text="${#numbers.formatInteger(detail.montant, 3, 'WHITESPACE')}"></span>
      </td>
    </tr>
  </tbody>
  <tfoot class="datatable-footer">
    <tr class="tr-footer">
      <!-- ... totaux dynamiques selon colonnes choisies ... -->
    </tr>
  </tfoot>
</table>
```

#### 2.9 Flux complet de génération PDF (service custom uniquement)

```
Requête GET /api/edition-factures/pdf/{id}/{invoiceDate}
  │
  ├─ FacturationReportServiceImpl.printToPdf(facture)
  │    ├─ templateConfigService.findByCode(facture.tiersPayant.modelFilePath)
  │    │    → FactureTemplateConfig (body_template, css_overrides, page_config, display_config)
  │    ├─ buildCommonParameters(magasin, config)
  │    │    → injecte cssOverrides, pageConfig, displayConfig dans le contexte Thymeleaf
  │    └─ getTemplateAsHtml()
  │         → templateEngine.process("facturation/main", context)
  │              │
  │              ├─ [DB Resolver ord.1] "facturation/model/custom_1/body"
  │              │    → trouve body_template en DB → ITemplateResource(String)
  │              │    → Thymeleaf traite les expressions th:* avec le contexte existant
  │              │
  │              ├─ [Classpath ord.2] "facturation/css"   → css.html (+ cssOverrides injectés)
  │              ├─ [Classpath ord.2] "facturation/header" → header.html
  │              └─ [Classpath ord.2] "facturation/total"  → total.html (conditionnel via displayConfig)
  │
  └─ ITextRenderer.setDocumentFromString(html)
       → Flying Saucer → PDF
```

#### 2.10 Migration Flyway

```sql
-- V1.4.3__facture_template_config.sql
CREATE TABLE facture_template_config (
    id              SERIAL        PRIMARY KEY,
    code            VARCHAR(20)   NOT NULL UNIQUE,
    libelle         VARCHAR(100)  NOT NULL,
    description     VARCHAR(300),
    actif           BOOLEAN       NOT NULL DEFAULT TRUE,
    systeme         BOOLEAN       NOT NULL DEFAULT FALSE,
    body_template   TEXT,                    -- HTML Thymeleaf du <table>, null si modèle système
    css_overrides   TEXT,                    -- CSS additionnel
    page_config     JSONB,                   -- format, orientation, marges
    display_config  JSONB,                   -- signature, montant lettres, etc.
    colonnes_config JSONB         NOT NULL,  -- config UI drag-and-drop
    created         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated         TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Modèles système : body_template NULL → le classpath resolver gère (body.html statique)
-- colonnes_config renseigné pour permettre la visualisation/copie dans l'UI
INSERT INTO facture_template_config (code, libelle, systeme, body_template, colonnes_config) VALUES
('default', 'Modèle standard', true, NULL, '[
  {"slot":"NOM_ASSURE",   "libelle":"Nom",             "visible":true, "ordre":0,"alignement":"left"},
  {"slot":"PRENOM_ASSURE","libelle":"Prénom(s)",        "visible":true, "ordre":1,"alignement":"left"},
  {"slot":"MATRICULE",    "libelle":"Matricule",         "visible":true, "ordre":2,"alignement":"left"},
  {"slot":"NUM_BON",      "libelle":"N° Bon",            "visible":true, "ordre":3,"alignement":"center"},
  {"slot":"DATE",         "libelle":"Date",              "visible":true, "ordre":4,"alignement":"left"},
  {"slot":"MONTANT_BON",  "libelle":"Montant Bon",       "visible":true, "ordre":5,"alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu", "visible":true, "ordre":6,"alignement":"right"}
]'),
('0203', 'Modèle CNAM 02/03', true, NULL, '[
  {"slot":"DATE",         "libelle":"Date",              "visible":true, "ordre":0,"alignement":"left"},
  {"slot":"NOM_ASSURE",   "libelle":"Nom",               "visible":true, "ordre":1,"alignement":"left"},
  {"slot":"PRENOM_ASSURE","libelle":"Prénom(s)",          "visible":true, "ordre":2,"alignement":"left"},
  {"slot":"MATRICULE",    "libelle":"Matricule",           "visible":true, "ordre":3,"alignement":"left"},
  {"slot":"NOM_PATIENT",  "libelle":"Bénéficiaire",       "visible":true, "ordre":4,"alignement":"left"},
  {"slot":"NUM_BON",      "libelle":"N° BPC",             "visible":true, "ordre":5,"alignement":"center"},
  {"slot":"MONTANT_BON",  "libelle":"Montant Bon",         "visible":false,"ordre":6,"alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu",  "visible":true, "ordre":7,"alignement":"right"}
]'),
('0903', 'Modèle Assurance 09/03', true, NULL, '[
  {"slot":"DATE",         "libelle":"Date",              "visible":true, "ordre":0,"alignement":"left"},
  {"slot":"NOM_ASSURE",   "libelle":"Nom",               "visible":true, "ordre":1,"alignement":"left"},
  {"slot":"PRENOM_ASSURE","libelle":"Prénom(s)",          "visible":true, "ordre":2,"alignement":"left"},
  {"slot":"MATRICULE",    "libelle":"Matricule",           "visible":true, "ordre":3,"alignement":"left"},
  {"slot":"NUM_BON",      "libelle":"N° Bon",             "visible":true, "ordre":4,"alignement":"center"},
  {"slot":"MONTANT_BON",  "libelle":"Montant Bon",        "visible":true, "ordre":5,"alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu",  "visible":true, "ordre":6,"alignement":"right"}
]'),
('0907', 'Modèle Mutuelle 09/07', true, NULL, '[
  {"slot":"NOM_ASSURE",   "libelle":"Nom/Prénom(s) salarié","visible":true,"ordre":0,"alignement":"left"},
  {"slot":"NOM_PATIENT",  "libelle":"Nom/Prénom(s) patient","visible":true,"ordre":1,"alignement":"left"},
  {"slot":"MATRICULE",    "libelle":"Matricule",             "visible":true,"ordre":2,"alignement":"left"},
  {"slot":"DATE",         "libelle":"Date",                  "visible":true,"ordre":3,"alignement":"left"},
  {"slot":"NUM_BON",      "libelle":"N°Bon",                 "visible":true,"ordre":4,"alignement":"center"},
  {"slot":"MONTANT_BON",  "libelle":"Montant Total",         "visible":true,"ordre":5,"alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu",     "visible":true,"ordre":6,"alignement":"right"}
]');
```

#### 2.11 Endpoints REST

```
GET    /api/facture-template-configs                   → List<FactureTemplateConfigDto>
GET    /api/facture-template-configs/{code}            → FactureTemplateConfigDto
POST   /api/facture-template-configs                   → FactureTemplateConfigDto  (nouveau)
PUT    /api/facture-template-configs/{code}            → FactureTemplateConfigDto  (modifier)
DELETE /api/facture-template-configs/{code}            → 204  (non-système uniquement)
GET    /api/facture-template-configs/{code}/preview    → Blob PDF (données fictives)
POST   /api/facture-template-configs/{code}/clone      → FactureTemplateConfigDto  (copier un modèle)
```

#### 2.12 Limite du mode colonnes — modèles entièrement personnalisés

**Problème** : avec la configuration de colonnes (`colonnes_config` + `TemplateBuilderService`),
le pharmacien ne peut que **réordonner / masquer des slots prédéfinis**. Il ne peut pas :

- Ajouter une colonne avec un champ non prévu (ex: `taux`, `numBon` avec un format différent)
- Changer la structure (grouper par patient, sous-totaux intermédiaires, layout non-tabulaire)
- Utiliser d'autres champs du domaine disponibles dans le contexte Thymeleaf

La solution : permettre l'édition directe de `body_template` via un **éditeur de code** dans l'UI,
avec validation des expressions dangereuses avant la sauvegarde.

#### 2.13 Éditeur de template — mode avancé

##### Deux modes d'édition coexistants

```
Mode Simple  [onglet "Colonnes"]
  ↓ TemplateBuilderService.buildBodyTemplate(colonnes, display)
  ↓ écrit body_template
  flag manually_edited = false

Mode Avancé  [onglet "Code HTML"]
  ↓ éditeur Monaco/CodeMirror
  ↓ TemplateValidatorService.validate(html)     ← bloque les expressions dangereuses
  ↓ écrit body_template directement
  flag manually_edited = true
  (le mode simple affiche un warning "modifié manuellement — régénérer écrasera")
```

**Ajout dans `FactureTemplateConfig`** :

```java
@Column(name = "manually_edited", nullable = false)
private boolean manuallyEdited = false;
// true = body_template a été édité directement → ne pas écraser avec colonnes_config
```

##### Variables disponibles dans un template custom

Le contexte Thymeleaf injecté par `FacturationReportServiceImpl` est **documenté et limité** :

| Variable | Type | Champs utiles |
|----------|------|---------------|
| `entity` | `FactureTiersPayant` | `.facturesDetails`, `.tiersPayant`, `.numFacture`, `.debutPeriode`, `.finPeriode`, `.remiseForfetaire` |
| `entity.facturesDetails` | `List<ThirdPartySaleLine>` | `.clientTiersPayant.assuredCustomer.{firstName,lastName}`, `.clientTiersPayant.num`, `.numBon`, `.montant`, `.taux`, `.tauxVente`, `.sale.salesAmount`, `.sale.ayantDroit.{firstName,lastName}`, `.created` |
| `magasin` | `Magasin` | `.name`, `.phone`, `.address`, `.registre` |
| `grandTotal` | `Pair<e1,e2>` | `e1` = total bons, `e2` = total attendu |
| `invoiceTotalAmountLetters` | `String` | Montant en lettres |
| `displayConfig` | `DisplayConfig` | `.showSignature`, `.showMontantLetters` |

Ces variables sont documentées dans l'UI sous forme d'**aide contextuelle** dans l'éditeur.

##### `TemplateValidatorService` — protection SSTI

```java
@Service
public class TemplateValidatorService {

    // Patterns Thymeleaf dangereux : exécution de code, accès beans Spring, reflection
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
        Pattern.compile("T\\s*\\("),                          // T(Runtime).exec(...)
        Pattern.compile("#\\{[^}]*@"),                        // #{@beanName...}
        Pattern.compile("\\$\\{[^}]*T\\s*\\("),              // ${T(java.lang.Runtime)...}
        Pattern.compile("\\$\\{[^}]*@[a-zA-Z]"),             // ${@myBean.method()}
        Pattern.compile("\\$\\{[^}]*#request"),               // accès HttpServletRequest
        Pattern.compile("\\$\\{[^}]*#session"),               // accès HttpSession
        Pattern.compile("\\$\\{[^}]*#vars\\.getOrDefault"),  // accès variable engine
        Pattern.compile("(?i)exec\\s*\\("),                   // exec(
        Pattern.compile("(?i)getRuntime"),                    // Runtime.getRuntime()
        Pattern.compile("(?i)ProcessBuilder"),                // ProcessBuilder
        Pattern.compile("(?i)ClassLoader"),                   // ClassLoader
        Pattern.compile("(?i)forName\\s*\\(")                // Class.forName(
    );

    /**
     * Valide un template body_template avant sauvegarde en base.
     * @throws TemplateValidationException si un pattern interdit est trouvé
     */
    public void validate(String template) {
        if (template == null || template.isBlank()) return;
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            Matcher matcher = pattern.matcher(template);
            if (matcher.find()) {
                throw new TemplateValidationException(
                    "Expression interdite dans le template : " + matcher.group()
                );
            }
        }
        // Vérification Thymeleaf syntaxique : tenter un parse à vide
        try {
            Context ctx = new Context();
            ctx.setVariable("entity",     new FactureTiersPayant());
            ctx.setVariable("grandTotal", Pair.of(0L, 0L));
            templateEngine.process(
                "<html><body><table>" + template + "</table></body></html>", ctx
            );
        } catch (TemplateInputException e) {
            throw new TemplateValidationException("Syntaxe Thymeleaf invalide : " + e.getMessage());
        }
    }
}
```

##### Prévisualisation avant sauvegarde

```
POST /api/facture-template-configs/preview
     body: { bodyTemplate, cssOverrides, pageConfig, displayConfig }
     → Blob (PDF généré avec données fictives)
```

Le service génère le PDF avec un `FactureTiersPayant` fictif (3–5 lignes de test) — sans toucher
à la base. Permet au pharmacien de voir le rendu avant de sauvegarder.

```java
@Service
public class FactureTemplatePreviewService {

    public byte[] generatePreview(FactureTemplateConfigDto dto) {
        FactureTiersPayant sampleFacture = buildSampleFacture(); // données fictives
        // Injecte temporairement body_template dans le resolver DB (cache non-persistant)
        dbResolver.putInPreviewCache(dto.code(), dto.bodyTemplate());
        try {
            return reportService.printByteArray(sampleFacture, dto);
        } finally {
            dbResolver.clearPreviewCache(dto.code());
        }
    }
}
```

#### 2.14 Menu de personnalisation (Frontend)

**Page admin** : `admin/facture-templates/`

**Composant configurateur** : `FactureTemplateEditorComponent`

```
┌─────────────────────────────────────────────────────────────┐
│  Modèle : [CNAM 02/03 ▾]   [Nouveau]  [Cloner]  [Aperçu PDF]│
├──────────────────────┬──────────────────────────────────────┤
│  Colonnes disponibles│  Colonnes actives (glisser-déposer)   │
│  ┌─────────────────┐│  ┌──────────────────────────────────┐ │
│  │ □ Montant Bon   ││  │ ≡ Date              [Libellé: __]│ │
│  └─────────────────┘│  │ ≡ Nom               [Libellé: __]│ │
│                      │  │ ≡ Prénom(s)         [Libellé: __]│ │
│                      │  │ ≡ Matricule         [Libellé: __]│ │
│                      │  │ ≡ N° Bon            [Libellé: __]│ │
│                      │  │ ≡ Montant Attendu   [Libellé: __]│ │
│                      │  └──────────────────────────────────┘ │
├──────────────────────┴──────────────────────────────────────┤
│                              [Annuler]  [Enregistrer]        │
└─────────────────────────────────────────────────────────────┘
```

```
┌─ Modèle : [CNAM personnalisé ▾]  [Nouveau]  [Cloner]  [Aperçu PDF]  ┐
│                                                                        │
│  ┌─[Colonnes]──────────────────┐  ┌─[Code HTML]─────────────────┐    │
│  │  Mode simple (drag & drop)  │  │  Mode avancé (Monaco Editor) │    │
│  │                             │  │                              │    │
│  │  Colonnes disponibles       │  │  <table class="main-table">  │    │
│  │  □ Montant Bon              │  │    <thead>...                │    │
│  │                             │  │    <tbody>                   │    │
│  │  Colonnes actives           │  │      <tr th:each="detail...  │    │
│  │  ≡ Date        [Libellé __] │  │        <td>...</td>          │    │
│  │  ≡ Nom         [Libellé __] │  │  </table>                    │    │
│  │  ≡ Prénom      [Libellé __] │  │                              │    │
│  │  ≡ Matricule   [Libellé __] │  │  [?] Variables disponibles   │    │
│  │  ≡ N° Bon      [Libellé __] │  │                              │    │
│  │  ≡ Mt. Attendu [Libellé __] │  │  ⚠ modifié manuellement      │    │
│  └─────────────────────────────┘  └──────────────────────────────┘    │
│                                                                        │
│  CSS additionnel : [textarea]                                          │
│  Format page : [A4 ▾]  [Portrait ▾]  Marges : [30mm] [20px] [20mm]   │
│  ☑ Montant en lettres   ☑ Zone signature   □ Taux couverture          │
│                                                                        │
│                              [Annuler]  [Aperçu PDF]  [Enregistrer]   │
└────────────────────────────────────────────────────────────────────────┘
```

**Composant Angular** : `FactureTemplateEditorComponent`

- **Onglet "Colonnes"** : `PrimeNG OrderList` + `p-toggleswitch` + `pInputText` par colonne
- **Onglet "Code HTML"** : Monaco Editor (déjà utilisable via `ngx-monaco-editor`) avec
  colorisation HTML + Thymeleaf, aide contextuelle des variables disponibles
- **Bouton "Aperçu PDF"** : `POST /api/facture-template-configs/preview` → `window.open(blob)`
- **Bouton "Cloner"** : copie un modèle existant (système ou custom) → nouveau code demandé
- **Badge "Modifié manuellement"** : affiché si `manually_edited = true` sur l'onglet Colonnes
- **Modèles système** : lecture seule → seul "Cloner" disponible

**Dans `form-tiers-payant`** : `p-select` chargé depuis `/api/facture-template-configs?actif=true`,
affiche `libelle` (non plus le code brut `modelFacture`).

---

## 3. État de rapprochement

### Contexte métier (officine)

L'état de rapprochement permet de :

- Comparer ce qui a été **facturé** avec ce qui a été **effectivement payé** par l'organisme
- Visualiser l'**historique des paiements** par facture
- Identifier les **rejets partiels** et en retard

### InvoicePayment — entité existante à utiliser

`InvoicePayment extends PaymentTransaction` (table `payment_transaction`, discriminator `dtype='InvoicePayment'`) contient déjà :

| Champ (PaymentTransaction) | Usage rapprochement |
|---------------------------|---------------------|
| `paidAmount` | Montant réglé sur ce paiement |
| `transactionDate` | Date du règlement |
| `transactionNumber` | Référence virement/chèque |
| `paymentMode` | Mode paiement (VIREMENT, CHEQUE…) |
| `banque` | Banque émettrice |
| `commentaire` | Observations / motif rejet |

| Champ (InvoicePayment) | Usage |
|------------------------|-------|
| `factureTiersPayant` | FK composite vers la facture |
| `invoicePaymentItems` | Détail par ligne de vente |
| `parent` / `invoicePayments` | Paiements groupés (parent = paiement global) |
| `grouped` | True si règlement groupé multi-factures |

### Ce qu'il faut ajouter

`InvoicePayment` ne nécessite **pas de nouvelle entité**. Il faut :

1. Un service de rapprochement qui agrège les `InvoicePayment` existants par facture
2. Des endpoints dédiés pour la saisie / consultation des règlements
3. Un frontend dédié

#### 3.1 Service de rapprochement

```java
// RapprochementService.java
public interface RapprochementService {
    Page<EtatRapprochementDto> getEtatRapprochement(RapprochementParams params, Pageable pageable);

    // Saisie d'un règlement reçu d'un organisme
    InvoicePaymentDto enregistrerReglement(ReglementFactureCommand command);

    // Annulation d'un règlement (suppression de l'InvoicePayment)
    void annulerReglement(Long paymentId, LocalDate paymentDate);

    byte[] exportPdf(RapprochementParams params);
    byte[] exportExcel(RapprochementParams params);
}
```

#### 3.2 DTOs

```java
public record EtatRapprochementDto(
    String              tiersPayantName,
    LocalDate           debutPeriode,
    LocalDate           finPeriode,
    BigDecimal          totalFacture,
    BigDecimal          totalRegle,      // SUM(paidAmount) des InvoicePayments liés
    BigDecimal          ecartTotal,
    List<LigneRapprochementDto> lignes
) {}

public record LigneRapprochementDto(
    String              numFacture,
    LocalDate           invoiceDate,
    LocalDate           echeance,        // invoiceDate + delaiReglement
    BigDecimal          montantFacture,  // FactureTiersPayant.montantNet
    BigDecimal          montantRegle,    // SUM InvoicePayment.paidAmount
    BigDecimal          ecart,
    InvoiceStatut       statut,
    List<ReglementDto>  reglements       // historique des InvoicePayments
) {}

// Représente un InvoicePayment (règlement individuel)
public record ReglementDto(
    Long      id,
    LocalDate transactionDate,
    Integer   paidAmount,
    String    transactionNumber,   // référence
    String    paymentMode,
    String    banque,
    String    commentaire
) {}

// Commande pour enregistrer un règlement via InvoicePayment
public record ReglementFactureCommand(
    Long      factureId,
    LocalDate factureDate,
    Integer   montantRegle,
    LocalDate dateReglement,
    String    transactionNumber,
    String    paymentModeCode,    // code PaymentMode existant
    Long      banqueId,
    String    commentaire
) {}
```

#### 3.3 Endpoints REST

```
GET  /api/rapprochement
     ?tiersPayantId=&startDate=&endDate=&statuts=
     → Page<EtatRapprochementDto>

POST /api/rapprochement/reglement
     body: ReglementFactureCommand
     → ReglementDto

DELETE /api/rapprochement/reglement/{id}/{transactionDate}
     → 204

GET  /api/rapprochement/pdf?tiersPayantId=&startDate=&endDate=
     → Blob (PDF)

GET  /api/rapprochement/excel?tiersPayantId=&startDate=&endDate=
     → Blob (XLSX)
```

### Frontend

- **Composant** : `facturation/rapprochement/rapprochement.component.ts`
- **Tableau AG Grid** : Facture | Date | Échéance | Facturé | Réglé | Écart | Statut
- **Détail ligne** : `rowDetailRenderer` — liste des `ReglementDto` avec bouton "Supprimer règlement"
- **Action par ligne** : bouton "+ Saisir règlement" → modal avec `paymentMode` (p-select depuis API) + montant + référence
- **Alerte visuelle** : ligne rouge si `echeance < today && statut != PAID`
- **KPI en tête** : Total facturé | Total réglé | Écart | Taux recouvrement

### Migrations Flyway

Aucune — `InvoicePayment` / `payment_transaction` existent déjà.  
Vérifier que les `PaymentMode` utilisés pour les virements organisme existent (sinon ajouter via migration de données).

---

## 4. Avoir / note de crédit

### Contexte métier (officine)

Un **avoir** (note de crédit) est émis quand l'organisme rejette une ou plusieurs lignes d'une facture
(médicament non remboursable, erreur de taux, dépassement plafond). L'avoir crédite le compte de
l'organisme et vient en déduction de la prochaine facture.

### Approche : Entité `AvoirTiersPayant` distincte

Justification : séparer avoir et facture évite les montants négatifs sur `FactureTiersPayant` et
garantit un suivi comptable propre. Pattern identique à l'avoir en comptabilité française.

### Modèle de données

```java
// AvoirTiersPayant.java
@Entity
@Table(name = "avoir_tiers_payant")
public class AvoirTiersPayant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_avoir", nullable = false, unique = true, length = 30)
    private String numAvoir;              // "AV-2025_0001"

    // Facture d'origine (composite FK)
    @Column(name = "facture_origine_id",   nullable = false) private Long      factureOrigineId;
    @Column(name = "facture_origine_date", nullable = false) private LocalDate  factureOrigineDate;

    @Column(name = "montant_avoir",  nullable = false) private BigDecimal montantAvoir;
    @Column(name = "montant_tva",    nullable = false) private BigDecimal montantTva;
    @Column(name = "montant_ht",     nullable = false) private BigDecimal montantHt;
    @Column(name = "motif",          length = 500)     private String     motif;
    @Column(name = "avoir_date",     nullable = false) private LocalDate  avoirDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 10)
    private AvoirStatut statut;           // DRAFT, EMIS, IMPUTE, ANNULE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tiers_payant_id", nullable = false)
    private TiersPayant tiersPayant;

    @OneToMany(mappedBy = "avoir", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvoirLine> lignes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "created", nullable = false) private LocalDateTime created;
    @Column(name = "updated", nullable = false) private LocalDateTime updated;
}

// AvoirLine.java
@Entity
@Table(name = "avoir_line")
public class AvoirLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avoir_id")
    private AvoirTiersPayant avoir;

    // Ligne de vente concernée (composite FK)
    @Column(name = "sale_line_id",   nullable = false) private Long      saleLineId;
    @Column(name = "sale_line_date", nullable = false) private LocalDate  saleLineDate;

    @Column(name = "montant_avoir",  nullable = false) private BigDecimal montantAvoir;
    @Column(name = "motif_rejet",    length = 200)     private String     motifRejet;  // code rejet CNAM
}

public enum AvoirStatut { DRAFT, EMIS, IMPUTE, ANNULE }
```

### Migration Flyway

```sql
-- V1.4.4__avoir_tiers_payant.sql
CREATE TABLE avoir_tiers_payant (
    id                   BIGSERIAL     PRIMARY KEY,
    num_avoir            VARCHAR(30)   NOT NULL UNIQUE,
    facture_origine_id   BIGINT        NOT NULL,
    facture_origine_date DATE          NOT NULL,
    tiers_payant_id      INTEGER       NOT NULL REFERENCES tiers_payant(id),
    montant_avoir        NUMERIC(15,2) NOT NULL,
    montant_tva          NUMERIC(15,2) NOT NULL DEFAULT 0,
    montant_ht           NUMERIC(15,2) NOT NULL DEFAULT 0,
    motif                VARCHAR(500),
    avoir_date           DATE          NOT NULL,
    statut               VARCHAR(10)   NOT NULL DEFAULT 'DRAFT',
    user_id              INTEGER       NOT NULL REFERENCES app_user(id),
    created              TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated              TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_avoir_facture_origine
        FOREIGN KEY (facture_origine_id, facture_origine_date)
        REFERENCES facture_tiers_payant(id, invoice_date)
);

CREATE TABLE avoir_line (
    id             BIGSERIAL     PRIMARY KEY,
    avoir_id       BIGINT        NOT NULL REFERENCES avoir_tiers_payant(id),
    sale_line_id   BIGINT        NOT NULL,
    sale_line_date DATE          NOT NULL,
    montant_avoir  NUMERIC(15,2) NOT NULL,
    motif_rejet    VARCHAR(200),
    CONSTRAINT fk_avoir_line_sale_line
        FOREIGN KEY (sale_line_id, sale_line_date)
        REFERENCES third_party_sale_line(id, sale_date)
);

CREATE SEQUENCE avoir_numero_seq START 1;
```

### Service & Endpoints

```java
public interface AvoirService {
    AvoirDto creerAvoir(AvoirCommand command);     // DRAFT
    AvoirDto emettre(Long avoirId);               // DRAFT → EMIS + PDF
    void     imputer(Long avoirId, Long factureId, LocalDate factureDate); // EMIS → IMPUTE
    void     annuler(Long avoirId);               // DRAFT/EMIS → ANNULE
    Page<AvoirDto> findAll(AvoirSearchParams params, Pageable pageable);
    byte[]   exportPdf(Long avoirId);
}
```

```
GET    /api/avoirs?tiersPayantId=&startDate=&endDate=&statuts=  → Page<AvoirDto>
POST   /api/avoirs                                              → AvoirDto
POST   /api/avoirs/{id}/emettre                                 → AvoirDto
POST   /api/avoirs/{id}/imputer  body:{factureId, factureDate}  → void
DELETE /api/avoirs/{id}                                         → 204 (DRAFT uniquement)
GET    /api/avoirs/{id}/pdf                                     → Blob (PDF avoir)
```

### Template PDF avoir

```
resources/templates/facturation/avoir/
├── main.html     (entête "NOTE DE CRÉDIT" en rouge, ref. facture origine)
├── header.html
└── body.html     (tableau lignes annulées + total avoir + motif)
```

### Frontend

- **Onglet "Avoirs"** dans `facturation.component`
- **Liste AG Grid** : N° Avoir | Facture Origine | Tiers-payant | Montant | Statut | Date
- **Création** : depuis `facture-detail` — bouton "Créer un avoir" → modal sélection lignes
- **Actions** : Émettre | Imputer | Annuler | Télécharger PDF
- **Badges statut** : DRAFT (gris) | EMIS (bleu) | IMPUTÉ (vert) | ANNULÉ (rouge)

---

## 5. Facturation périodique automatisée

### Contexte métier (officine)

Automatisation de la génération mensuelle pour éliminer la tâche manuelle. Chaque organisme
peut avoir son propre rythme. Standard CNAM : facturation obligatoire avant le 5 du mois suivant.

### Modèle de données

```java
@Entity
@Table(name = "planification_facturation")
public class PlanificationFacturation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicite", nullable = false, length = 20)
    private Periodicite periodicite;       // HEBDOMADAIRE, MENSUEL, BIMENSUEL, TRIMESTRIEL

    @Column(name = "jour_declenchement", nullable = false)
    private Integer jourDeclenchement;     // 1–28 pour mensuel, 1–7 pour hebdomadaire

    @Column(name = "heure_declenchement", nullable = false)
    private LocalTime heureDeclenchement;  // ex : 02:00

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_edition", nullable = false, length = 30)
    private ModeEditionEnum modeEdition;   // réutilise l'enum existant

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tiers_payant_ids", columnDefinition = "jsonb")
    private List<Integer> tiersPayantIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "groupe_ids", columnDefinition = "jsonb")
    private List<Integer> groupeIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_tiers", length = 20)
    private TiersPayantCategorie categorieTiers;

    @Column(name = "facture_provisoire", nullable = false)
    private boolean factureProvisoire = false;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "prochaine_execution")
    private LocalDateTime prochaineExecution;

    @Column(name = "derniere_execution")
    private LocalDateTime derniereExecution;

    @Enumerated(EnumType.STRING)
    @Column(name = "dernier_statut", length = 20)
    private ExecutionStatut dernierStatut;  // SUCCESS, ECHEC, EN_COURS

    @Column(name = "dernier_message", length = 500)
    private String dernierMessage;

    @Column(name = "created", nullable = false) private LocalDateTime created;
    @Column(name = "updated", nullable = false) private LocalDateTime updated;
}

public enum Periodicite    { HEBDOMADAIRE, MENSUEL, BIMENSUEL, TRIMESTRIEL }
public enum ExecutionStatut { SUCCESS, ECHEC, EN_COURS }
```

### Migration Flyway

```sql
-- V1.4.5__planification_facturation.sql
CREATE TABLE planification_facturation (
    id                   SERIAL       PRIMARY KEY,
    libelle              VARCHAR(100) NOT NULL,
    periodicite          VARCHAR(20)  NOT NULL,
    jour_declenchement   INTEGER      NOT NULL,
    heure_declenchement  TIME         NOT NULL DEFAULT '02:00:00',
    mode_edition         VARCHAR(30)  NOT NULL,
    tiers_payant_ids     JSONB,
    groupe_ids           JSONB,
    categorie_tiers      VARCHAR(20),
    facture_provisoire   BOOLEAN      NOT NULL DEFAULT FALSE,
    actif                BOOLEAN      NOT NULL DEFAULT TRUE,
    prochaine_execution  TIMESTAMP,
    derniere_execution   TIMESTAMP,
    dernier_statut       VARCHAR(20),
    dernier_message      VARCHAR(500),
    created              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE historique_planification (
    id               BIGSERIAL   PRIMARY KEY,
    planification_id INTEGER     NOT NULL REFERENCES planification_facturation(id),
    execution_debut  TIMESTAMP   NOT NULL,
    execution_fin    TIMESTAMP,
    statut           VARCHAR(20) NOT NULL,
    generation_code  INTEGER,
    nombre_factures  INTEGER,
    message          VARCHAR(500)
);
```

### Job Spring Scheduler

```java
@Component
@Slf4j
public class FacturationSchedulerJob {

    @Scheduled(cron = "0 * * * * *")   // toutes les minutes
    @Transactional
    public void executerPlanificationsEnAttente() {
        planificationRepository
            .findByActifTrueAndProchaineExecutionBefore(LocalDateTime.now())
            .forEach(this::executerPlanification);
    }

    private void executerPlanification(PlanificationFacturation plan) {
        LocalDateTime debut = LocalDateTime.now();
        plan.setDernierStatut(ExecutionStatut.EN_COURS);
        planificationRepository.save(plan);
        try {
            EditionService service = serviceRegistry.getService(plan.getModeEdition());
            FactureEditionResponse res = service.createFactureEdition(buildParams(plan));

            plan.setDernierStatut(ExecutionStatut.SUCCESS);
            plan.setProchaineExecution(calculerProchaine(plan));
            plan.setDernierMessage("code=" + res.generationCode());
            eventPublisher.publishEvent(new FacturationAutoReussieEvent(plan, res));
        } catch (Exception e) {
            plan.setDernierStatut(ExecutionStatut.ECHEC);
            plan.setDernierMessage(e.getMessage());
            log.error("Échec planification {} : {}", plan.getId(), e.getMessage(), e);
        } finally {
            enregistrerHistorique(plan, debut);
            planificationRepository.save(plan);
        }
    }

    private LocalDateTime calculerProchaine(PlanificationFacturation plan) {
        LocalDateTime base = LocalDateTime.now();
        return switch (plan.getPeriodicite()) {
            case HEBDOMADAIRE -> base.plusWeeks(1);
            case MENSUEL      -> base.plusMonths(1)
                                     .withDayOfMonth(plan.getJourDeclenchement())
                                     .with(plan.getHeureDeclenchement());
            case BIMENSUEL    -> base.plusMonths(2)
                                     .withDayOfMonth(plan.getJourDeclenchement())
                                     .with(plan.getHeureDeclenchement());
            case TRIMESTRIEL  -> base.plusMonths(3)
                                     .withDayOfMonth(plan.getJourDeclenchement())
                                     .with(plan.getHeureDeclenchement());
        };
    }

    private EditionSearchParams buildParams(PlanificationFacturation plan) {
        LocalDate fin   = LocalDate.now().minusDays(1);
        LocalDate debut = switch (plan.getPeriodicite()) {
            case HEBDOMADAIRE -> fin.minusWeeks(1).plusDays(1);
            case MENSUEL      -> fin.withDayOfMonth(1);
            case BIMENSUEL    -> fin.minusMonths(1).withDayOfMonth(1);
            case TRIMESTRIEL  -> fin.minusMonths(2).withDayOfMonth(1);
        };
        return new EditionSearchParams(
            ModeEditionSort.TIERS_NAME_ASC, plan.getModeEdition(), debut, fin,
            orEmpty(plan.getGroupeIds()), orEmpty(plan.getTiersPayantIds()),
            plan.getCategorieTiers() != null ? List.of(plan.getCategorieTiers().name()) : List.of(),
            plan.isFactureProvisoire()
        );
    }
}
```

### Endpoints REST

```
GET    /api/planifications-facturation                     → List<PlanificationDto>
POST   /api/planifications-facturation                     → PlanificationDto
PUT    /api/planifications-facturation/{id}                → PlanificationDto
PATCH  /api/planifications-facturation/{id}/toggle-actif   → void
DELETE /api/planifications-facturation/{id}                → 204
POST   /api/planifications-facturation/{id}/executer-maintenant → FactureEditionResponse
GET    /api/planifications-facturation/{id}/historique     → Page<HistoriquePlanificationDto>
```

### Frontend

- **Onglet "Automatisation"** dans `facturation.component`
- **Tableau AG Grid** : Libellé | Périodicité | Prochain déclenchement | Dernier statut | Actif
- **Formulaire création** : `ngbModal` — libellé, périodicité, jour/heure, mode, cibles
- **Badge statut** : SUCCESS (vert) | ECHEC (rouge) | EN_COURS (orange)
- **Panel historique** : expandable par ligne — log des exécutions passées

---

## Ordre d'implémentation recommandé

| Priorité | Feature | Complexité | Migrations | Impact existant |
|----------|---------|-----------|------------|-----------------|
| 1 | Récapitulatif mensuel | Faible | Aucune | Aucun |
| 2 | État de rapprochement | Faible | Aucune (`InvoicePayment` existant) | Aucun |
| 3 | Avoir / note de crédit | Élevée | V1.4.4 | Aucun |
| 4 | Facturation automatisée | Élevée | V1.4.5 | Aucun |
| 5 | Personnalisation PDF | Élevée | V1.4.3 | **Dispatch uniquement dans `FacturationPdfExportServiceImpl`** — `FacturationReportServiceImpl` inchangé |

> **Règle pour la feature 5** : le système existant (`modelFacture` → classpath `body.html`)
> est préservé intégralement. `CustomModelFactureReportService` est un service **additionnel**
> qui ne s'active que si `facture_template_config.body_template IS NOT NULL` pour le modèle
> du tiers-payant. Toute facture dont le modèle n'est pas en base continue à utiliser le
> chemin classpath existant sans aucune modification.

---

## Migrations Flyway — récapitulatif

| Version | Fichier | Feature | Statut |
|---------|---------|---------|--------|
| V1.4.2 | `add_email_groupe_tiers_payant.sql` | Email groupe tiers-payant | ✓ créé |
| V1.4.3 | `facture_template_config.sql` | **[Priorité 5]** Table modèles PDF custom | À créer |
| V1.4.4 | `avoir_tiers_payant.sql` | **[Priorité 3]** Tables avoir + lignes avoir | À créer |
| V1.4.5 | `planification_facturation.sql` | **[Priorité 4]** Planification + historique | À créer |

> Récapitulatif mensuel (priorité 1) et État de rapprochement (priorité 2) :
> **aucune migration** — données issues de `facture_tiers_payant` et `payment_transaction` existants.
