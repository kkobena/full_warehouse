# Plan d'Implémentation — Personnalisation des Modèles de Factures PDF

> **Contexte :** Pharma-Smart — Spring Boot 4 / Angular 20 / PostgreSQL
> **Base existante :** `FactureTiersPayant`, `ThirdPartySaleLine`, `TiersPayant`,
> `FacturationReportServiceImpl`, templates Thymeleaf classpath (`facturation/model/*/body.html`)
> **Priorité :** BASSE (les features 1–4 du plan principal sont déjà implémentées)

---

## Table des matières

1. [Analyse de l'existant](#1-analyse-de-lexistant)
2. [Principe de non-régression](#2-principe-de-non-régression)
3. [Architecture retenue](#3-architecture-retenue)
   - 3.1 [Entité `FactureTemplateConfig`](#31-entité-facturetemplateconfig)
   - 3.2 [`FactureTemplateDbResolver` — resolver Thymeleaf DB-backed](#32-facturetemplatedbresolver)
   - 3.3 [`TemplateBuilderService` — génération du bodyTemplate](#33-templatebuilderservice)
   - 3.4 [`CustomModelFactureReportService`](#34-custommodelfacturereportservice)
   - 3.5 [Dispatch dans `FacturationPdfExportServiceImpl`](#35-dispatch-dans-facturationpdfexportserviceimpl)
   - 3.6 [Injection CSS overrides et page config](#36-injection-css-overrides-et-page-config)
4. [Slots de colonnes disponibles](#4-slots-de-colonnes-disponibles)
5. [Template Thymeleaf dynamique unique](#5-template-thymeleaf-dynamique-unique)
6. [Mode avancé — éditeur de template](#6-mode-avancé--éditeur-de-template)
   - 6.1 [Deux modes d'édition coexistants](#61-deux-modes-dédition-coexistants)
   - 6.2 [`TemplateValidatorService` — protection SSTI](#62-templatevalidatorservice--protection-ssti)
   - 6.3 [Prévisualisation avant sauvegarde](#63-prévisualisation-avant-sauvegarde)
7. [Migration Flyway](#7-migration-flyway)
8. [Endpoints REST](#8-endpoints-rest)
9. [Frontend — `FactureTemplateEditorComponent`](#9-frontend--facturetemplateditorcomponent)
10. [Flux complet de génération PDF](#10-flux-complet-de-génération-pdf)
11. [Ordre d'implémentation](#11-ordre-dimplémentation)

---

## 1. Analyse de l'existant

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

**Différences entre les 4 modèles existants :**

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
changer leur libellé, et réordonner. Aucune colonne "libre" n'est nécessaire en mode simple.

---

## 2. Principe de non-régression

Le mécanisme actuel de génération PDF (`modelFacture` → classpath `body.html`) **ne sera ni modifié
ni supprimé**. Il continue à fonctionner pour tous les tiers-payants utilisant les modèles
système (`default`, `0203`, `0903`, `0907`).

Un **service séparé** (`CustomModelFactureReportService`) gère uniquement les modèles personnalisés
stockés en base. La sélection entre les deux services se fait dans
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

**Ce qui ne change PAS :**
- `FacturationReportServiceImpl` — aucune modification
- `main.html`, `header.html`, `css.html`, `total.html` — aucune modification
- Les 4 `body.html` classpath (`default`, `0203`, `0903`, `0907`) — aucune modification
- `TiersPayant.modelFacture` + `getModelFilePath()` — aucune modification

---

## 3. Architecture retenue

**Option B — resolver DB + JSONB config**

La chaîne de génération actuelle :
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

**Ajout d'un resolver DB prioritaire :**
```
SpringTemplateEngine
  ├─ [Ordre 1] FactureTemplateDbResolver  ← nouveau
  │   Gère : "facturation/model/*/body"   → lit facture_template_config en DB
  │   Passe  : tout le reste              → résolu par le classpath resolver
  └─ [Ordre 2] ClassLoaderTemplateResolver (existant)
       Gère : css, header, total, group/*, autres templates
```

### 3.1 Entité `FactureTemplateConfig`

**Package :** `com.kobe.warehouse.domain`

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

    // Null pour les modèles système → classpath resolver prend le relais
    // Non-null pour les modèles custom → utilisé par FactureTemplateDbResolver
    @Column(name = "body_template", columnDefinition = "TEXT")
    private String bodyTemplate;

    // CSS additionnel (injecté après css.html)
    @Column(name = "css_overrides", columnDefinition = "TEXT")
    private String cssOverrides;

    // Configuration de la page PDF
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "page_config", columnDefinition = "jsonb")
    private PageConfig pageConfig;

    // Options d'affichage
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "display_config", columnDefinition = "jsonb")
    private DisplayConfig displayConfig;

    // Config UI drag-and-drop → sert à générer bodyTemplate via TemplateBuilderService
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "colonnes_config", columnDefinition = "jsonb")
    private List<ColonneConfig> colonnes;

    // true si body_template a été édité directement (mode avancé)
    @Column(name = "manually_edited", nullable = false)
    private boolean manuallyEdited = false;

    @Column(name = "created", nullable = false) private LocalDateTime created;
    @Column(name = "updated", nullable = false) private LocalDateTime updated;
}

// Config d'une colonne (drag-and-drop)
public record ColonneConfig(
    String  slot,        // NOM_ASSURE, PRENOM_ASSURE, NOM_PATIENT, MATRICULE,
                         // NUM_BON, DATE, MONTANT_BON, MONTANT_ATTENDU
    String  libelle,     // libellé dans l'en-tête PDF
    boolean visible,
    int     ordre,
    String  alignement   // "left", "center", "right"
) {}

// Configuration de la page PDF
public record PageConfig(
    String  format,        // "A4", "A5", "LETTER"
    String  orientation,   // "PORTRAIT", "LANDSCAPE"
    String  marginTop,     // "30mm"
    String  marginRight,   // "20px"
    String  marginBottom,  // "20mm"
    String  marginLeft     // "20px"
) {}

// Options d'affichage
public record DisplayConfig(
    boolean showMontantLetters,   // "arrêté la présente facture à la somme de..."
    boolean showSignature,        // zone signature pharmacien
    boolean showTauxCouverture,   // taux de couverture par bon
    String  titreFacultatif       // titre personnalisé au-dessus du tableau
) {}
```

### 3.2 `FactureTemplateDbResolver`

**Package :** `com.kobe.warehouse.service.facturation`

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

    // Cache de prévisualisation (non-persistant, mémoire uniquement)
    private final Map<String, String> previewCache = new ConcurrentHashMap<>();

    @Override public String  getName()  { return "FactureTemplateDbResolver"; }
    @Override public Integer getOrder() { return 1; }  // avant ClassLoaderTemplateResolver (ordre 2)

    @Override
    public TemplateResolution resolveTemplate(
        IEngineConfiguration config,
        String ownerTemplate,
        String template,
        Map<String, Object> templateResolutionAttributes
    ) {
        if (!template.startsWith(BODY_PREFIX) || !template.endsWith(BODY_SUFFIX)) {
            return null;  // null = passe au resolver suivant (classpath)
        }

        String code = template
            .substring(BODY_PREFIX.length(), template.length() - BODY_SUFFIX.length());

        // Priorité : cache prévisualisation
        String previewContent = previewCache.get(code);
        if (previewContent != null) {
            return buildResolution(template, previewContent);
        }

        return repository.findByCodeAndBodyTemplateIsNotNull(code)
            .map(cfg -> buildResolution(template, cfg.getBodyTemplate()))
            .orElse(null);  // null = classpath resolver → body.html statique
    }

    private TemplateResolution buildResolution(String template, String content) {
        return new TemplateResolution(
            template, template,
            new StringTemplateResource(content),
            false,              // pas de cache (la config peut changer)
            TemplateMode.HTML
        );
    }

    public void putInPreviewCache(String code, String bodyTemplate) {
        previewCache.put(code, bodyTemplate);
    }

    public void clearPreviewCache(String code) {
        previewCache.remove(code);
    }
}

// StringTemplateResource — adapte un String en ITemplateResource Thymeleaf
public class StringTemplateResource implements ITemplateResource {
    private final String content;
    public StringTemplateResource(String content) { this.content = content; }

    @Override public String  getDescription() { return "DB template"; }
    @Override public String  getBaseName()    { return null; }
    @Override public boolean exists()         { return true; }
    @Override public Reader  reader() throws IOException { return new StringReader(content); }
    @Override public ITemplateResource relative(String relativeLocation) { return null; }
}
```

**Enregistrement dans Spring (`ThymeleafConfig.java`) :**

```java
@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringTemplateEngine templateEngine(
        FactureTemplateDbResolver dbResolver,
        ClassLoaderTemplateResolver classpathResolver  // bean existant
    ) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        classpathResolver.setOrder(2);
        engine.addTemplateResolver(dbResolver);         // order=1
        engine.addTemplateResolver(classpathResolver);  // order=2
        return engine;
    }
}
```

### 3.3 `TemplateBuilderService`

**Package :** `com.kobe.warehouse.service.facturation`

```java
/**
 * Génère le HTML Thymeleaf du <table> depuis la configuration JSONB colonnes.
 * Appelé quand le pharmacien sauvegarde sa configuration via l'UI (mode simple).
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
            sb.append("      <td").append(tdClass).append(">")
              .append(resolveExpression(col.slot()))
              .append("</td>\n");
        }
        sb.append("    </tr>\n  </tbody>\n");

        // Pied (totaux dynamiques selon colonnes choisies)
        sb.append("  <tfoot class=\"datatable-footer\"><tr class=\"tr-footer\">\n");
        appendTotals(sb, visibles);
        sb.append("  </tr></tfoot>\n");
        sb.append("</table>\n");

        return sb.toString();
    }

    private String resolveExpression(String slot) {
        return switch (slot) {
            case "DATE"            -> "<span th:text=\"${#temporals.format(detail.created,'dd/MM/yyyy')}\"></span>";
            case "NOM_ASSURE"      -> "<span th:text=\"${detail.clientTiersPayant.assuredCustomer.firstName}\"></span>";
            case "PRENOM_ASSURE"   -> "<span th:text=\"${detail.clientTiersPayant.assuredCustomer.lastName}\"></span>";
            case "NOM_PATIENT"     -> "<span th:text=\"${detail.sale.ayantDroit.firstName + ' ' + detail.sale.ayantDroit.lastName}\"></span>";
            case "MATRICULE"       -> "<span th:text=\"${detail.clientTiersPayant.num}\"></span>";
            case "NUM_BON"         -> "<span th:text=\"${detail.numBon}\"></span>";
            case "MONTANT_BON"     -> "<span th:text=\"${#numbers.formatInteger(detail.sale.salesAmount,3,'WHITESPACE')}\"></span>";
            case "MONTANT_ATTENDU" -> "<span th:text=\"${#numbers.formatInteger(detail.montant,3,'WHITESPACE')}\"></span>";
            default                -> "";
        };
    }

    private void appendTotals(StringBuilder sb, List<ColonneConfig> visibles) {
        for (ColonneConfig col : visibles) {
            if ("MONTANT_BON".equals(col.slot())) {
                sb.append("      <td class=\"unit\" th:text=\"${grandTotal.e1()}\"></td>\n");
            } else if ("MONTANT_ATTENDU".equals(col.slot())) {
                sb.append("      <td class=\"unit\" th:text=\"${grandTotal.e2()}\"></td>\n");
            } else {
                sb.append("      <td></td>\n");
            }
        }
    }
}
```

### 3.4 `CustomModelFactureReportService`

**Package :** `com.kobe.warehouse.service.facturation`

```java
/**
 * Service SÉPARÉ pour les modèles PDF personnalisés stockés en base.
 * N'hérite PAS de CommonReportService pour ne pas interférer avec l'existant.
 * FacturationReportServiceImpl reste INCHANGÉ.
 */
@Service
public class CustomModelFactureReportService {

    private final SpringTemplateEngine            templateEngine;
    private final StorageService                  storageService;
    private final FileStorageProperties           fileStorageProperties;
    private final FactureTemplateConfigRepository templateConfigRepository;

    public Resource printToPdf(FactureTiersPayant facture) throws ReportFileExportException {
        FactureTemplateConfig config = templateConfigRepository
            .findByCode(facture.getTiersPayant().getModelFilePath())
            .orElseThrow(ReportFileExportException::new);

        Context context = buildContext(facture, config);
        // "facturation/main" = même template principal inchangé
        // DbResolver intercepte "facturation/model/{code}/body" → DB
        // css/header/total → classpath (inchangés)
        String html = templateEngine.process(Constant.FACTURATION_TEMPLATE_FILE, context);

        return generatePdf(html, facture.getNumFacture());
    }

    private Context buildContext(FactureTiersPayant facture, FactureTemplateConfig config) {
        Magasin magasin = storageService.getUser().getMagasin();
        Tuple4  total   = buildSummary(facture);

        Context ctx = new Context(Locale.forLanguageTag("fr"));
        ctx.setVariable(Constant.ENTITY,                facture);
        ctx.setVariable(Constant.MAGASIN,               magasin);
        ctx.setVariable(Constant.FOOTER,                buildFooter(magasin));
        ctx.setVariable(Constant.FACTURE_TOTAL,         total);
        ctx.setVariable(Constant.FACTURE_TOTAL_LETTERS, NumberUtil.getNumberToWords(total.e2()).toUpperCase());
        // Variables additionnelles pour les templates custom
        ctx.setVariable("cssOverrides",  config.getCssOverrides());
        ctx.setVariable("pageConfig",    config.getPageConfig());
        ctx.setVariable("displayConfig", config.getDisplayConfig());
        // Colonnes visibles triées (pour le template dynamique)
        List<ColonneConfig> colonnesVisibles = Optional.ofNullable(config.getColonnes())
            .orElse(List.of()).stream()
            .filter(ColonneConfig::visible)
            .sorted(Comparator.comparingInt(ColonneConfig::ordre))
            .toList();
        ctx.setVariable("colonnesVisibles", colonnesVisibles);
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

### 3.5 Dispatch dans `FacturationPdfExportServiceImpl`

> **Seule modification dans le code existant** : ajout du dispatch custom vs existant.

```java
@Service
public class FacturationPdfExportServiceImpl implements FacturationPdfExportService {

    private final FacturationReportService        reportService;          // EXISTANT, inchangé
    private final CustomModelFactureReportService customReportService;    // NOUVEAU
    private final FactureTemplateConfigRepository templateConfigRepository;

    @Override
    public byte[] export(List<FactureTiersPayant> factures) throws ReportFileExportException {
        FactureTiersPayant first = factures.getFirst();
        if (hasCustomTemplate(first)) {
            return customReportService.printToPdf(factures);
        }
        return reportService.exportReportToPdf(factures);  // chemin existant, inchangé
    }

    private boolean hasCustomTemplate(FactureTiersPayant facture) {
        return templateConfigRepository
            .findByCode(facture.getTiersPayant().getModelFilePath())
            .map(c -> c.getBodyTemplate() != null)
            .orElse(false);
    }
}
```

### 3.6 Injection CSS overrides et page config

> Ajout à la fin de `css.html` — **inoffensif si variables nulles** (service existant).

```html
<!-- css.html — ajout à la fin du <style>, variables nulles = aucun effet sur l'existant -->
<style th:inline="css">
  /* ... styles existants inchangés ... */
  [(${cssOverrides != null ? cssOverrides : ''})]

  @page {
    size:          [(${pageConfig?.format      ?: 'A4'})] [(${pageConfig?.orientation ?: 'PORTRAIT'})];
    margin-top:    [(${pageConfig?.marginTop   ?: '30mm'})];
    margin-right:  [(${pageConfig?.marginRight ?: '20px'})];
    margin-bottom: [(${pageConfig?.marginBottom?: '20mm'})];
    margin-left:   [(${pageConfig?.marginLeft  ?: '20px'})];
  }
</style>
```

---

## 4. Slots de colonnes disponibles

```java
public enum ColonneSlot {
    DATE            ("Date",              "detail.created",                                               "dd/MM/yyyy"),
    NOM_ASSURE      ("Nom",               "detail.clientTiersPayant.assuredCustomer.firstName",           null),
    PRENOM_ASSURE   ("Prénom(s)",         "detail.clientTiersPayant.assuredCustomer.lastName",            null),
    NOM_PATIENT     ("Patient",           "detail.sale.ayantDroit.firstName + ' ' + ...lastName",        null),
    MATRICULE       ("Matricule",         "detail.clientTiersPayant.num",                                 null),
    NUM_BON         ("N° Bon",            "detail.numBon",                                                null),
    MONTANT_BON     ("Montant Bon",       "detail.sale.salesAmount",                                      "integer"),
    MONTANT_ATTENDU ("Montant Attendu",   "detail.montant",                                               "integer");
}
```

---

## 5. Template Thymeleaf dynamique unique

> Fichier : `src/main/resources/templates/facturation/model/dynamic/body.html`
> Alternative au `TemplateBuilderService` — utilisable si `colonnes_config` est null mais
> `colonnesVisibles` est injecté dans le contexte.

```html
<table class="main-table">
  <thead>
    <tr>
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
      <td th:each="col : ${colonnesVisibles}"
          th:class="${col.alignement == 'right' ? 'unit' : ''}"
          th:switch="${col.slot}">
        <span th:case="'MONTANT_BON'"
              th:text="${grandTotal.e1()}"></span>
        <span th:case="'MONTANT_ATTENDU'"
              th:text="${grandTotal.e2()}"></span>
        <span th:case="*"></span>
      </td>
    </tr>
  </tfoot>
</table>
```

---

## 6. Niveaux d'édition — UX adaptée au profil utilisateur

> ⚠️ **Monaco Editor n'est PAS adapté à un pharmacien.**
> Monaco est l'éditeur de VS Code : il affiche du HTML/Thymeleaf brut.
> Un pharmacien ne connaît ni HTML ni les expressions `th:each`, `th:text`, etc.
> L'exposer à ce mode serait une source d'erreurs, de frustration et de failles de sécurité.

### 6.1 Architecture en 3 niveaux selon le profil

```
┌──────────────────────────────────────────────────────────────────────┐
│  NIVEAU 1 — Pharmacien / Utilisateur standard  (rôle USER/PHARMACIEN)│
│  ──────────────────────────────────────────────────────────────────  │
│  • Drag & drop des colonnes disponibles                              │
│  • Renommage des libellés de colonnes                                │
│  • Activation/désactivation par p-toggleswitch                       │
│  • Pas de code HTML visible                                          │
│  → Couverture : 90 % des besoins réels (réordonner, masquer)         │
├──────────────────────────────────────────────────────────────────────┤
│  NIVEAU 2 — Responsable pharmacie / Gérant  (rôle MANAGER)          │
│  ──────────────────────────────────────────────────────────────────  │
│  • Tout le niveau 1                                                  │
│  • Configurateur visuel enrichi (formulaire structuré) :             │
│    - Titre personnalisé au-dessus du tableau                         │
│    - Format de page (A4/A5), orientation, marges via sliders         │
│    - Afficher/masquer montant en lettres, signature                  │
│    - CSS simplifié : taille de police, couleur de l'en-tête          │
│  → Couverture : 98 % des besoins sans écrire une ligne de code       │
├──────────────────────────────────────────────────────────────────────┤
│  NIVEAU 3 — Administrateur technique / Intégrateur  (rôle ADMIN)    │
│  ──────────────────────────────────────────────────────────────────  │
│  • Tout le niveau 2                                                  │
│  • Onglet "Code HTML" (Monaco Editor) — accès restreint              │
│    - Édition directe du body_template HTML/Thymeleaf                 │
│    - Validation SSTI avant sauvegarde                                │
│    - Badge "⚠ Modifié manuellement" sur les autres onglets           │
│  → Pour les cas extrêmes non couverts par les niveaux 1 et 2         │
└──────────────────────────────────────────────────────────────────────┘
```

**Règle de contrôle d'accès :**
```typescript
// Dans FactureTemplateEditorComponent
showAdvancedCodeTab = this.accountService.hasAnyAuthority([Authority.ADMIN]);
```

```java
// Dans FactureTemplateConfigResource — endpoint PUT avec body_template non-null
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public ResponseEntity<...> updateTemplate(...) { ... }
```

### 6.2 Niveau 1 — Configurateur Colonnes (pharmacien)

Interface drag-and-drop **sans aucun code** :

```
┌─────────────────────────────────────────────────────────────────┐
│  Modèle : [CNAM 02/03 ▾]   [Nouveau]  [Cloner]  [Aperçu PDF]   │
├─────────────────────────────────────────────────────────────────┤
│  COLONNES DE LA FACTURE                                         │
│  Faites glisser les colonnes pour les réorganiser               │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ≡  ☑  Date              Libellé : [Date          ]       │  │
│  │ ≡  ☑  Nom assuré        Libellé : [Nom           ]       │  │
│  │ ≡  ☑  Prénom            Libellé : [Prénom(s)     ]       │  │
│  │ ≡  ☑  Matricule         Libellé : [Matricule     ]       │  │
│  │ ≡  ☑  N° Bon            Libellé : [N° Bon        ]       │  │
│  │ ≡  ☐  Montant Bon       Libellé : [Montant Bon   ]  [←] │  │
│  │ ≡  ☑  Montant Attendu   Libellé : [Montant Attendu]      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ☑ Afficher le montant en lettres   ☑ Zone de signature         │
│                                                                 │
│                         [Annuler]  [Aperçu PDF]  [Enregistrer] │
└─────────────────────────────────────────────────────────────────┘
```

**Composants PrimeNG utilisés :**
- `p-orderlist` — drag & drop des colonnes
- `p-toggleswitch` — activer/désactiver une colonne
- `pInputText` — modifier le libellé
- `p-button` — aperçu PDF + enregistrer

### 6.3 Niveau 2 — Configurateur Visuel Enrichi (manager)

Formulaire structuré, **toujours sans code HTML** :

```
┌─[Colonnes]─────────────────────────────┬─[Mise en page]──────────────────────┐
│                                         │                                      │
│  (identique niveau 1)                   │  Titre au-dessus du tableau :        │
│                                         │  [LISTE DES BONS DE PRISE EN CHARGE] │
│                                         │                                      │
│                                         │  Format papier : [A4 ▾]              │
│                                         │  Orientation   : [Portrait ▾]        │
│                                         │                                      │
│                                         │  Marges (mm) :                       │
│                                         │  Haut [30] Bas [20] G [20] D [20]   │
│                                         │                                      │
│                                         │  Police du tableau : [10pt ▾]        │
│                                         │  Couleur en-tête   : [████ #336699]  │
│                                         │                                      │
│                                         │  ☑ Montant en lettres                │
│                                         │  ☑ Zone de signature pharmacien      │
│                                         │  ☐ Afficher taux de couverture       │
└─────────────────────────────────────────┴──────────────────────────────────────┘

                              [Annuler]  [Aperçu PDF]  [Enregistrer]
```

**Extension de `DisplayConfig` pour le niveau 2 :**
```java
public record DisplayConfig(
    boolean showMontantLetters,
    boolean showSignature,
    boolean showTauxCouverture,
    String  titreFacultatif,         // texte affiché au-dessus du tableau
    String  tableHeaderColor,        // "#336699" — couleur de fond des <th>
    String  tableFontSize,           // "10pt", "11pt", "12pt"
    String  tableHeaderFontColor     // "#FFFFFF" — couleur texte en-tête
) {}
```

**CSS généré automatiquement depuis `DisplayConfig` (pas saisi manuellement) :**
```java
// Dans TemplateBuilderService — génère le cssOverrides depuis DisplayConfig
public String buildCssFromDisplayConfig(DisplayConfig display) {
    StringBuilder css = new StringBuilder();
    if (display.tableHeaderColor() != null) {
        css.append(".main-table thead th { background-color: ")
           .append(display.tableHeaderColor()).append("; ");
        if (display.tableHeaderFontColor() != null) {
            css.append("color: ").append(display.tableHeaderFontColor()).append(";");
        }
        css.append(" }\n");
    }
    if (display.tableFontSize() != null) {
        css.append(".main-table { font-size: ").append(display.tableFontSize()).append("; }\n");
    }
    return css.toString();
}
```

### 6.4 Niveau 3 — Mode Expert (admin technique uniquement)

Monaco Editor **masqué par défaut**, visible uniquement si `hasAuthority('ROLE_ADMIN')` :

```
┌─[Colonnes]──────┬─[Mise en page]──────┬─[Code HTML ⚠ ADMIN]──────────────────┐
│                 │                     │                                        │
│  (niveau 1)     │  (niveau 2)         │  ⚠ Zone réservée aux administrateurs   │
│                 │                     │  techniques. Toute erreur peut rendre   │
│                 │                     │  le PDF illisible.                      │
│                 │                     │                                        │
│                 │                     │  <table class="main-table">            │
│                 │                     │    <thead>                             │
│                 │                     │      <tr>                              │
│                 │                     │        <th>Date</th>                   │
│                 │                     │        ...                             │
│                 │                     │                                        │
│                 │                     │  [?] Variables disponibles ▾           │
│                 │                     │                                        │
│                 │                     │  Badge : ⚠ Modifié manuellement        │
└─────────────────┴─────────────────────┴────────────────────────────────────────┘
```

**Avertissement à l'activation du mode expert :**
> Une `ngbModal` de confirmation s'affiche : _"Vous allez modifier directement le code HTML
> du template. Cette action est irréversible et réservée aux administrateurs techniques.
> Les niveaux Colonnes et Mise en page ne pourront plus régénérer ce template automatiquement.
> Continuer ?"_

### 6.5 `TemplateValidatorService` — protection SSTI

**Package :** `com.kobe.warehouse.service.facturation`
> Appelé uniquement pour le niveau 3 (body_template édité manuellement).

```java
@Service
public class TemplateValidatorService {

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

### 6.6 Prévisualisation avant sauvegarde

> Disponible pour **tous les niveaux** — le pharmacien peut voir son rendu avant d'enregistrer.

```java
@Service
public class FactureTemplatePreviewService {

    public byte[] generatePreview(FactureTemplateConfigDto dto) {
        FactureTiersPayant sampleFacture = buildSampleFacture(); // données fictives (3–5 lignes)
        dbResolver.putInPreviewCache(dto.code(), dto.bodyTemplate());
        try {
            return customReportService.printByteArray(sampleFacture, dto);
        } finally {
            dbResolver.clearPreviewCache(dto.code());
        }
    }

    private FactureTiersPayant buildSampleFacture() {
        // Facture fictive : organisme "CNAM TEST", 3 lignes de vente demo
        // ...
    }
}
```

### 6.7 Tableau de synthèse — qui peut faire quoi ?

| Fonctionnalité | Pharmacien (USER) | Gérant (MANAGER) | Admin (ADMIN) |
|----------------|:-----------------:|:----------------:|:-------------:|
| Voir la liste des modèles | ✓ | ✓ | ✓ |
| Réordonner / masquer des colonnes | ✓ | ✓ | ✓ |
| Renommer libellés | ✓ | ✓ | ✓ |
| Configurer titre, marges, format papier | — | ✓ | ✓ |
| Configurer couleur / police | — | ✓ | ✓ |
| Cloner un modèle système | ✓ | ✓ | ✓ |
| Aperçu PDF | ✓ | ✓ | ✓ |
| Éditer le HTML directement (Monaco) | ✗ | ✗ | ✓ |
| Supprimer un modèle personnalisé | — | ✓ | ✓ |
| Créer un nouveau modèle (code unique) | — | ✓ | ✓ |

---

## 7. Migration Flyway

**Fichier :** `src/main/resources/db/migration/V1.4.3__facture_template_config.sql`

```sql
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
    manually_edited BOOLEAN       NOT NULL DEFAULT FALSE,
    created         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated         TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Modèles système : body_template NULL → classpath resolver gère (body.html statique)
-- colonnes_config renseigné pour visualisation/copie dans l'UI
INSERT INTO facture_template_config (code, libelle, systeme, body_template, colonnes_config) VALUES
('default', 'Modèle standard', true, NULL, '[
  {"slot":"NOM_ASSURE",     "libelle":"Nom",              "visible":true,  "ordre":0, "alignement":"left"},
  {"slot":"PRENOM_ASSURE",  "libelle":"Prénom(s)",         "visible":true,  "ordre":1, "alignement":"left"},
  {"slot":"MATRICULE",      "libelle":"Matricule",          "visible":true,  "ordre":2, "alignement":"left"},
  {"slot":"NUM_BON",        "libelle":"N° Bon",             "visible":true,  "ordre":3, "alignement":"center"},
  {"slot":"DATE",           "libelle":"Date",               "visible":true,  "ordre":4, "alignement":"left"},
  {"slot":"MONTANT_BON",    "libelle":"Montant Bon",        "visible":true,  "ordre":5, "alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu",    "visible":true,  "ordre":6, "alignement":"right"}
]'),
('0203', 'Modèle CNAM 02/03', true, NULL, '[
  {"slot":"DATE",           "libelle":"Date",               "visible":true,  "ordre":0, "alignement":"left"},
  {"slot":"NOM_ASSURE",     "libelle":"Nom",                "visible":true,  "ordre":1, "alignement":"left"},
  {"slot":"PRENOM_ASSURE",  "libelle":"Prénom(s)",          "visible":true,  "ordre":2, "alignement":"left"},
  {"slot":"MATRICULE",      "libelle":"Matricule",           "visible":true,  "ordre":3, "alignement":"left"},
  {"slot":"NOM_PATIENT",    "libelle":"Bénéficiaire",       "visible":true,  "ordre":4, "alignement":"left"},
  {"slot":"NUM_BON",        "libelle":"N° BPC",             "visible":true,  "ordre":5, "alignement":"center"},
  {"slot":"MONTANT_BON",    "libelle":"Montant Bon",        "visible":false, "ordre":6, "alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu",    "visible":true,  "ordre":7, "alignement":"right"}
]'),
('0903', 'Modèle Assurance 09/03', true, NULL, '[
  {"slot":"DATE",           "libelle":"Date",               "visible":true,  "ordre":0, "alignement":"left"},
  {"slot":"NOM_ASSURE",     "libelle":"Nom",                "visible":true,  "ordre":1, "alignement":"left"},
  {"slot":"PRENOM_ASSURE",  "libelle":"Prénom(s)",          "visible":true,  "ordre":2, "alignement":"left"},
  {"slot":"MATRICULE",      "libelle":"Matricule",           "visible":true,  "ordre":3, "alignement":"left"},
  {"slot":"NUM_BON",        "libelle":"N° Bon",             "visible":true,  "ordre":4, "alignement":"center"},
  {"slot":"MONTANT_BON",    "libelle":"Montant Bon",        "visible":true,  "ordre":5, "alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu",    "visible":true,  "ordre":6, "alignement":"right"}
]'),
('0907', 'Modèle Mutuelle 09/07', true, NULL, '[
  {"slot":"NOM_ASSURE",     "libelle":"Nom/Prénom(s) salarié","visible":true,"ordre":0, "alignement":"left"},
  {"slot":"NOM_PATIENT",    "libelle":"Nom/Prénom(s) patient","visible":true,"ordre":1, "alignement":"left"},
  {"slot":"MATRICULE",      "libelle":"Matricule",             "visible":true,"ordre":2, "alignement":"left"},
  {"slot":"DATE",           "libelle":"Date",                  "visible":true,"ordre":3, "alignement":"left"},
  {"slot":"NUM_BON",        "libelle":"N°Bon",                 "visible":true,"ordre":4, "alignement":"center"},
  {"slot":"MONTANT_BON",    "libelle":"Montant Total",         "visible":true,"ordre":5, "alignement":"right"},
  {"slot":"MONTANT_ATTENDU","libelle":"Montant Attendu",       "visible":true,"ordre":6, "alignement":"right"}
]');
```

---

## 8. Endpoints REST

**Controller :** `com.kobe.warehouse.web.rest.facturation.FactureTemplateConfigResource`

```
GET    /api/facture-template-configs                      → List<FactureTemplateConfigDto>
GET    /api/facture-template-configs?actif=true           → List<FactureTemplateConfigDto> (pour p-select)
GET    /api/facture-template-configs/{code}               → FactureTemplateConfigDto
POST   /api/facture-template-configs                      → FactureTemplateConfigDto  (nouveau custom)
PUT    /api/facture-template-configs/{code}               → FactureTemplateConfigDto  (modifier)
DELETE /api/facture-template-configs/{code}               → 204  (non-système uniquement)
POST   /api/facture-template-configs/{code}/clone         → FactureTemplateConfigDto  (copier un modèle)
POST   /api/facture-template-configs/preview              → Blob PDF (données fictives, avant save)
GET    /api/facture-template-configs/{code}/preview       → Blob PDF (données fictives, depuis DB)
```

**DTO :**
```java
public record FactureTemplateConfigDto(
    Integer             id,
    String              code,
    String              libelle,
    String              description,
    boolean             actif,
    boolean             systeme,
    String              bodyTemplate,       // null si modèle système
    String              cssOverrides,
    PageConfig          pageConfig,
    DisplayConfig       displayConfig,
    List<ColonneConfig> colonnes,
    boolean             manuallyEdited
) {}
```

---

## 9. Frontend — `FactureTemplateEditorComponent`

**Route :** `admin/facture-templates/`
**Fichier :** `src/main/webapp/app/admin/facture-templates/facture-template-editor.component.ts`

### Structure des onglets selon le profil

```typescript
// Onglets affichés selon le rôle :
// USER/PHARMACIEN → [Colonnes]
// MANAGER        → [Colonnes] [Mise en page]
// ADMIN          → [Colonnes] [Mise en page] [Code HTML ⚠]
```

### Spécifications du composant Angular

```typescript
@Component({
  selector: 'app-facture-template-editor',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    // PrimeNG
    SelectModule,
    InputTextModule,
    ButtonModule,
    ToggleSwitchModule,
    OrderListModule,
    TabsModule,          // NgbNav pour les onglets rôle-dépendants
    // ng-bootstrap
    NgbNavModule,
    NgbModalModule,
    // Color picker (pour niveau 2 — couleur en-tête)
    ColorPickerModule,   // ngx-color-picker ou p-colorpicker
    // Monaco Editor (niveau 3 uniquement — chargé en lazy loading)
    // MonacoEditorModule — importé conditionnellement
  ],
  templateUrl: './facture-template-editor.component.html',
})
export class FactureTemplateEditorComponent implements OnInit {

  protected readonly isAdmin    = this.accountService.hasAnyAuthority([Authority.ADMIN]);
  protected readonly isManager  = this.accountService.hasAnyAuthority([Authority.ADMIN, Authority.MANAGER]);

  // Onglets visibles selon profil
  protected get visibleTabs() {
    const tabs = ['columns'];
    if (this.isManager) tabs.push('layout');
    if (this.isAdmin)   tabs.push('code');
    return tabs;
  }

  // ...
}
```

### Onglet "Colonnes" — tous les utilisateurs

```html
<!-- Drag & drop avec p-orderlist, aucun code HTML visible -->
<p-orderList [value]="colonnes" [dragdrop]="true">
  <ng-template pTemplate="item" let-col>
    <div class="d-flex align-items-center gap-3">
      <i class="pi pi-bars text-secondary"></i>
      <p-toggleswitch [(ngModel)]="col.visible" />
      <span class="fw-semibold flex-grow-1">{{ col.slotLabel }}</span>
      <input pInputText [(ngModel)]="col.libelle"
             placeholder="Libellé dans le PDF"
             class="w-50" [disabled]="!col.visible" />
    </div>
  </ng-template>
</p-orderList>

<!-- Options d'affichage simples -->
<div class="mt-3 d-flex gap-4">
  <label><p-toggleswitch [(ngModel)]="displayConfig.showMontantLetters" /> Montant en lettres</label>
  <label><p-toggleswitch [(ngModel)]="displayConfig.showSignature" /> Zone de signature</label>
</div>
```

### Onglet "Mise en page" — manager et admin

```html
<!-- Formulaire structuré, sans code -->
<div class="row g-3">
  <div class="col-12">
    <label class="form-label">Titre au-dessus du tableau</label>
    <input pInputText [(ngModel)]="displayConfig.titreFacultatif"
           placeholder="Ex: LISTE DES BONS DE PRISE EN CHARGE" class="w-100" />
  </div>
  <div class="col-4">
    <label class="form-label">Format papier</label>
    <p-select [options]="formats" [(ngModel)]="pageConfig.format" />
  </div>
  <div class="col-4">
    <label class="form-label">Orientation</label>
    <p-select [options]="orientations" [(ngModel)]="pageConfig.orientation" />
  </div>
  <div class="col-12">
    <label class="form-label">Marges (mm) — Haut / Bas / Gauche / Droite</label>
    <div class="d-flex gap-2">
      <input pInputText type="number" [(ngModel)]="pageConfig.marginTop"    class="w-25" />
      <input pInputText type="number" [(ngModel)]="pageConfig.marginBottom" class="w-25" />
      <input pInputText type="number" [(ngModel)]="pageConfig.marginLeft"   class="w-25" />
      <input pInputText type="number" [(ngModel)]="pageConfig.marginRight"  class="w-25" />
    </div>
  </div>
  <div class="col-4">
    <label class="form-label">Taille de police</label>
    <p-select [options]="fontSizes" [(ngModel)]="displayConfig.tableFontSize" />
  </div>
  <div class="col-4">
    <label class="form-label">Couleur en-tête tableau</label>
    <p-colorpicker [(ngModel)]="displayConfig.tableHeaderColor" format="hex" />
  </div>
  <div class="col-4">
    <label class="form-label">Couleur texte en-tête</label>
    <p-colorpicker [(ngModel)]="displayConfig.tableHeaderFontColor" format="hex" />
  </div>
  <div class="col-12">
    <label><p-toggleswitch [(ngModel)]="displayConfig.showTauxCouverture" /> Afficher taux de couverture</label>
  </div>
</div>
```

### Onglet "Code HTML" — admin uniquement (avec avertissement)

```html
<!-- Onglet visible uniquement pour ADMIN -->
@if (isAdmin) {
  <li ngbNavItem="code">
    <a ngbNavLink>
      Code HTML
      <span class="badge bg-danger ms-1">⚠ ADMIN</span>
    </a>
    <ng-template ngbNavContent>

      <!-- Bannière d'avertissement -->
      <div class="alert alert-warning d-flex align-items-center gap-2 mb-3">
        <i class="pi pi-exclamation-triangle fs-5"></i>
        <div>
          <strong>Zone technique réservée aux administrateurs.</strong>
          Modifier ce code directement peut rendre le PDF illisible.
          Les onglets "Colonnes" et "Mise en page" ne pourront plus régénérer
          ce template automatiquement.
        </div>
      </div>

      <!-- Badge si déjà modifié manuellement -->
      @if (template.manuallyEdited) {
        <div class="alert alert-danger py-1 mb-2">
          ⚠ Ce template a été modifié manuellement. La régénération depuis
          "Colonnes" écrasera ces modifications.
        </div>
      }

      <!-- Monaco Editor — chargé lazily -->
      <ngx-monaco-editor
        [options]="monacoOptions"
        [(ngModel)]="template.bodyTemplate"
        style="height: 450px; border: 1px solid #dee2e6;">
      </ngx-monaco-editor>

      <!-- Aide contextuelle -->
      <details class="mt-2">
        <summary class="text-muted small">Variables disponibles dans le template</summary>
        <table class="table table-sm table-bordered mt-1 small">
          <tr><td><code>entity</code></td><td>FactureTiersPayant — .numFacture, .debutPeriode, .finPeriode</td></tr>
          <tr><td><code>entity.facturesDetails</code></td><td>Liste des lignes — .numBon, .montant, .clientTiersPayant...</td></tr>
          <tr><td><code>magasin</code></td><td>.name, .phone, .address</td></tr>
          <tr><td><code>grandTotal.e1()</code></td><td>Total bons</td></tr>
          <tr><td><code>grandTotal.e2()</code></td><td>Total attendu</td></tr>
        </table>
      </details>

    </ng-template>
  </li>
}
```

### Comportements communs à tous les niveaux

- **Bouton "Aperçu PDF"** : `POST /api/facture-template-configs/preview` → `window.open(blob URL)`
  — fonctionne pour tous les profils, y compris avant sauvegarde
- **Bouton "Cloner"** : ouvre un `ngbModal` pour saisir le nouveau code/libellé — pour tous
- **Modèles système** : onglet "Colonnes" en lecture seule, seul "Cloner" est actif
- **Dans `form-tiers-payant`** : `p-select` chargé depuis `/api/facture-template-configs?actif=true`,
  affiche `libelle` (plus le code brut `modelFacture`)

---

## 10. Flux complet de génération PDF

```
Requête GET /api/edition-factures/pdf/{id}/{invoiceDate}
  │
  ├─ FacturationPdfExportServiceImpl.export(factures)
  │    ├─ hasCustomTemplate(first) ?
  │    │    → templateConfigRepository.findByCode(modelFilePath)
  │    │       → body_template IS NOT NULL ?
  │    │
  │    ├─ NON  → FacturationReportServiceImpl.exportReportToPdf (EXISTANT, INCHANGÉ)
  │    │
  │    └─ OUI  → CustomModelFactureReportService.printToPdf(facture)
  │               ├─ buildContext(facture, config)
  │               │    → injecte cssOverrides, pageConfig, displayConfig, colonnesVisibles
  │               └─ templateEngine.process("facturation/main", context)
  │                    │
  │                    ├─ [DB Resolver ord.1] "facturation/model/{code}/body"
  │                    │    → body_template depuis facture_template_config (DB)
  │                    │    → Thymeleaf traite les expressions th:* avec le contexte
  │                    │
  │                    ├─ [Classpath ord.2] "facturation/css"    → css.html (+ cssOverrides)
  │                    ├─ [Classpath ord.2] "facturation/header" → header.html
  │                    └─ [Classpath ord.2] "facturation/total"  → total.html (conditionnel)
  │
  └─ ITextRenderer.setDocumentFromString(html)
       → Flying Saucer → PDF
```

---

## 11. Ordre d'implémentation

| Étape | Tâche | Complexité | Profil bénéficiaire | Fichier(s) |
|-------|-------|-----------|---------------------|------------|
| 1 | Migration Flyway `V1.4.3__facture_template_config.sql` | Faible | — | `src/main/resources/db/migration/` |
| 2 | Entité `FactureTemplateConfig` + records imbriqués (`DisplayConfig` enrichi) | Faible | — | `domain/` |
| 3 | Repository `FactureTemplateConfigRepository` | Faible | — | `repository/` |
| 4 | `StringTemplateResource` | Faible | — | `service/facturation/` |
| 5 | `FactureTemplateDbResolver` (avec preview cache) | Moyenne | — | `service/facturation/` |
| 6 | `ThymeleafConfig` — enregistrer les 2 resolvers | Faible | — | `config/` |
| 7 | `TemplateBuilderService` — mode simple + génération CSS depuis DisplayConfig | Moyenne | — | `service/facturation/` |
| 8 | `TemplateValidatorService` — protection SSTI (admin uniquement) | Moyenne | Admin | `service/facturation/` |
| 9 | `CustomModelFactureReportService` | Moyenne | — | `service/facturation/` |
| 10 | `FactureTemplatePreviewService` | Faible | Tous | `service/facturation/` |
| 11 | Dispatch dans `FacturationPdfExportServiceImpl` | Faible | — | `service/facturation/` (existant) |
| 12 | `FactureTemplateConfigService` + impl | Moyenne | — | `service/facturation/` |
| 13 | `FactureTemplateConfigResource` (REST + `@PreAuthorize` sur PUT body_template) | Faible | — | `web/rest/facturation/` |
| 14 | Template dynamique `model/dynamic/body.html` (optionnel) | Faible | — | `resources/templates/` |
| 15 | Ajout CSS overrides dans `css.html` (2 lignes) | Faible | — | `resources/templates/facturation/css.html` |
| 16 | **Frontend Niveau 1** — onglet "Colonnes" (drag & drop, tous profils) | Élevée | **Pharmacien** | `app/admin/facture-templates/` |
| 17 | **Frontend Niveau 2** — onglet "Mise en page" (color picker, marges, manager+) | Élevée | **Gérant** | `app/admin/facture-templates/` |
| 18 | **Frontend Niveau 3** — onglet "Code HTML" + Monaco (admin uniquement) | Élevée | **Admin** | `app/admin/facture-templates/` |
| 19 | Mise à jour `form-tiers-payant` : `p-select` → libelle | Faible | Tous | `app/entities/tiers-payant/` |

> **Priorité d'implémentation** : les étapes 1–16 couvrent 90 % des besoins réels et sont
> suffisantes pour un déploiement en production auprès des pharmaciens. Les étapes 17–18
> peuvent être ajoutées dans une version ultérieure.

> **Règle fondamentale** : le système existant (`modelFacture` → classpath `body.html`) est
> préservé intégralement. `CustomModelFactureReportService` est un service **additionnel** qui
> ne s'active que si `facture_template_config.body_template IS NOT NULL` pour le modèle du
> tiers-payant. Toute facture dont le modèle n'est pas en base continue à utiliser le chemin
> classpath existant **sans aucune modification**.

