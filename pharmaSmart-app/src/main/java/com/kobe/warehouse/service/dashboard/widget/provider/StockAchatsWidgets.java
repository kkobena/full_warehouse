package com.kobe.warehouse.service.dashboard.widget.provider;

import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.nz;
import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.row;

import com.kobe.warehouse.service.DashboardAlertService;
import com.kobe.warehouse.service.dashboard.ResponsableCommandeDashboardService;
import com.kobe.warehouse.service.dashboard.widget.WidgetData;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.Column;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.SeriesItem;
import com.kobe.warehouse.service.dashboard.widget.WidgetDataProvider;
import com.kobe.warehouse.service.dashboard.widget.WidgetPeriods;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.dashboard.widget.WidgetPeriods.Period;
import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.DashboardAlertCountDTO;
import com.kobe.warehouse.service.dto.dashboard.AnalyseABCDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import com.kobe.warehouse.service.mobile.MobileTodoService;
import com.kobe.warehouse.service.report.StockRotationReportService;
import com.kobe.warehouse.service.report.StockValuationReportService;
import java.util.Comparator;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import com.kobe.warehouse.service.stat.DashboardService;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Widgets « Stock » et « Achats ». Hormis les achats de la période, ce sont des états à l'instant
 * présent : ils ignorent la période du dashboard.
 */
@Configuration
public class StockAchatsWidgets extends AbstractWidgets {

    public StockAchatsWidgets(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    /** Valeur des lots qui périment dans les six mois : la perte à éviter (retour, mise en avant). */
    @Bean
    WidgetDataProvider peremptionsValoriseesWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("peremptions-valorisees", (context, params) -> {
            var p = service.getPeremptions();
            int produits = nz(p.unMois()) + nz(p.unATroisMois()) + nz(p.troisASixMois());
            return new WidgetData.Kpi(
                nz(p.valeurTotale()),
                null,
                devise(),
                "%d produit(s) · dont %d à moins d'1 mois".formatted(produits, nz(p.unMois()))
            );
        });
    }

    /**
     * Produits à rotation lente, classés par valeur immobilisée : l'argent qui dort en rayon.
     * Paramètre : {@code limite} = 10.
     */
    @Bean
    WidgetDataProvider stockDormantWidget(StockRotationReportService stockRotationReportService) {
        return WidgetDataProvider.of("stock-dormant", (context, params) -> {
            List<StockRotationDTO> lents = stockRotationReportService
                .getSlowMovingProducts()
                .stream()
                .filter(s -> s.stockValue() != null && s.stockValue() > 0)
                .sorted(Comparator.comparing(StockRotationDTO::stockValue).reversed())
                .toList();
            long valeurTotale = lents.stream().mapToLong(StockRotationDTO::stockValue).sum();
            return new WidgetData.Table(
                List.of(
                    Column.text("produit", "Produit"),
                    Column.number("stock", "Stock"),
                    Column.amount("valeur", "Valeur"),
                    Column.number("jours", "Jours en stock")
                ),
                lents
                    .stream()
                    .limit(WidgetPeriods.intParam(params, "limite", 10, 1, 50))
                    .map(s -> row("produit", s.libelle(), "stock", s.stockQuantity(), "valeur", s.stockValue(), "jours", s.avgDaysInStock()))
                    .toList(),
                "%d produit(s) dormant(s) · %s immobilisés".formatted(lents.size(), amount(valeurTotale))
            );
        });
    }

    /**
     * Les actions prioritaires du jour — commander, relancer, écouler — reprises du pense-bête de
     * l'application mobile. Paramètre : {@code limite} = 10.
     */
    @Bean
    WidgetDataProvider aFaireWidget(MobileTodoService mobileTodoService) {
        return WidgetDataProvider.of("a-faire", (context, params) ->
            new WidgetData.Table(
                List.of(Column.text("priorite", "Priorité"), Column.text("tache", "Tâche"), Column.text("detail", "Détail")),
                mobileTodoService
                    .getAllTodoItems()
                    .stream()
                    .limit(WidgetPeriods.intParam(params, "limite", 10, 1, 50))
                    .map(t -> row("priorite", PRIORITES.getOrDefault(t.priority().name(), t.priority().name()), "tache", t.title(), "detail", t.description()))
                    .toList(),
                null
            )
        );
    }

    private static final java.util.Map<String, String> PRIORITES = java.util.Map.of("URGENT", "Urgent", "IMPORTANT", "Important", "NORMAL", "Normal");

    /** Valeur d'achat du stock au jour, et marge potentielle à la revente. */
    @Bean
    WidgetDataProvider stockValoriseWidget(StockValuationReportService stockValuationReportService) {
        return WidgetDataProvider.of("stock-valorise", (context, params) -> {
            StockValuationSummaryDTO stock = stockValuationReportService.getStockValuationSummary();
            if (stock == null) {
                return new WidgetData.Kpi(null, null, devise(), null);
            }
            return new WidgetData.Kpi(
                nz(stock.totalPurchaseValue()),
                null,
                devise(),
                String.format(
                    Locale.FRANCE,
                    "%d référence(s) · marge potentielle %.1f %%",
                    stock.totalProducts() == null ? 0 : stock.totalProducts(),
                    stock.averageMarginPercentage() == null ? 0 : stock.averageMarginPercentage().doubleValue()
                )
            );
        });
    }

    /** Paramètre : {@code limite} = 10. */
    @Bean
    WidgetDataProvider achatsParFournisseurWidget(SupplierPerformanceReportService supplierService) {
        return WidgetDataProvider.of("achats-par-fournisseur", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            return new WidgetData.Table(
                List.of(
                    Column.text("fournisseur", "Fournisseur"),
                    Column.number("commandes", "Commandes"),
                    Column.amount("montant", "Montant"),
                    Column.number("delai", "Délai (j)"),
                    Column.number("score", "Score")
                ),
                supplierService
                    .getTopSuppliersByPeriode(period.start(), period.end(), WidgetPeriods.intParam(params, "limite", 10, 1, 50))
                    .stream()
                    .map(f ->
                        row(
                            "fournisseur",
                            f.fournisseurName(),
                            "commandes",
                            f.nbCommandes(),
                            "montant",
                            f.montantAchat(),
                            "delai",
                            f.avgDeliveryDays(),
                            "score",
                            f.performanceScore()
                        )
                    )
                    .toList(),
                null
            );
        });
    }

    /** Qualité des livraisons sur douze mois : conformité moyenne et délai. */
    @Bean
    WidgetDataProvider qualiteFournisseursWidget(SupplierPerformanceReportService supplierService) {
        return WidgetDataProvider.of("qualite-fournisseurs", (context, params) -> {
            SupplierPerformanceSummaryDTO s = supplierService.getSupplierPerformanceSummary();
            if (s == null) {
                return new WidgetData.Kpi(null, null, "%", null);
            }
            return new WidgetData.Kpi(
                s.avgConformityRate(),
                null,
                "%",
                String.format(
                    Locale.FRANCE,
                    "Conformité · délai moyen %.1f j · %d fournisseur(s) · 12 mois",
                    s.avgDeliveryDays() == null ? 0 : s.avgDeliveryDays().doubleValue(),
                    s.totalSuppliers() == null ? 0 : s.totalSuppliers()
                )
            );
        });
    }

    @Bean
    WidgetDataProvider alertesOfficineWidget(DashboardAlertService dashboardAlertService) {
        return WidgetDataProvider.of("alertes-officine", (context, params) -> {
            DashboardAlertCountDTO a = dashboardAlertService.getAlertCounts();
            return new WidgetData.Table(
                List.of(Column.text("alerte", "Alerte"), Column.number("nombre", "Nombre")),
                List.of(
                    row("alerte", "Péremptions proches", "nombre", nz(a.peremptionCount())),
                    row("alerte", "Ruptures de stock", "nombre", nz(a.ruptureCount())),
                    row("alerte", "À commander en urgence", "nombre", nz(a.urgentCount())),
                    row("alerte", "Ajustements (24 h)", "nombre", nz(a.ajustementCount())),
                    row("alerte", "Modifications de prix (24 h)", "nombre", nz(a.prixModifCount())),
                    row("alerte", "Factures tiers payant échues", "nombre", nz(a.facturationOverdueCount()))
                ),
                null
            );
        });
    }

    @Bean
    WidgetDataProvider alertesStockWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("alertes-stock", (context, params) -> {
            var a = service.getStockAlerts();
            return new WidgetData.Series(
                List.of("Rupture", "Stock critique", "Bientôt en rupture", "Réassort rayon"),
                List.of(new SeriesItem("Produits", List.of(nz(a.rupture()), nz(a.stockCritique()), nz(a.bientotEnRupture()), nz(a.reassortStockRayon())))),
                null
            );
        });
    }

    @Bean
    WidgetDataProvider peremptionsWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("peremptions", (context, params) -> {
            var p = service.getPeremptions();
            return new WidgetData.Series(
                List.of("Moins d'1 mois", "1 à 3 mois", "3 à 6 mois"),
                List.of(new SeriesItem("Produits", List.of(nz(p.unMois()), nz(p.unATroisMois()), nz(p.troisASixMois())))),
                "Valeur totale : " + amount(p.valeurTotale())
            );
        });
    }

    @Bean
    WidgetDataProvider rotationStockWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("rotation-stock", (context, params) -> {
            var r = service.getRotationStock();
            return new WidgetData.Series(
                List.of("Rapide (> 4×)", "Normale (2 à 4×)", "Lente (< 2×)"),
                List.of(new SeriesItem("Produits", List.of(nz(r.rapide()), nz(r.normal()), nz(r.lent())))),
                r.rotationMoyenne() == null ? null : String.format(Locale.FRANCE, "Rotation moyenne : %.1f×", r.rotationMoyenne())
            );
        });
    }

    @Bean
    WidgetDataProvider analyseAbcWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("analyse-abc", (context, params) -> {
            AnalyseABCDTO abc = service.getAnalyseABC();
            return new WidgetData.Table(
                List.of(
                    Column.text("classe", "Classe"),
                    Column.number("produits", "Produits"),
                    new Column("partProduits", "% produits", "percent"),
                    new Column("partCa", "% CA", "percent"),
                    Column.amount("valeur", "Valeur")
                ),
                List.of(abcRow("A", abc.classeA()), abcRow("B", abc.classeB()), abcRow("C", abc.classeC())),
                null
            );
        });
    }

    @Bean
    WidgetDataProvider achatsPeriodeWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("achats-periode", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            AchatRecord current = dashboardService.getAchatPeriode(achatParam(period));
            AchatRecord previous = dashboardService.getAchatPeriode(achatParam(period.previous()));
            return new WidgetData.Kpi(
                receipt(current),
                receipt(previous),
                devise(),
                current == null ? null : "%d commande(s) · HT %s".formatted(nz(current.achatCount()), amount(current.netAmount()))
            );
        });
    }

    @Bean
    WidgetDataProvider commandesEnCoursWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("commandes-en-cours", (context, params) -> {
            var c = service.getCommandesEnCours();
            return new WidgetData.Kpi(
                nz(c.totalMontant()),
                null,
                devise(),
                "%d à réceptionner · %d en attente".formatted(nz(c.aReceptionner()), nz(c.enAttente()))
            );
        });
    }

    @Bean
    WidgetDataProvider suggestionsReapproWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("suggestions-reappro", (context, params) ->
            new WidgetData.Table(
                List.of(
                    Column.text("produit", "Produit"),
                    Column.number("stock", "Stock"),
                    Column.number("quantite", "À commander"),
                    Column.text("fournisseur", "Fournisseur")
                ),
                service
                    .getSuggestionsReappro()
                    .stream()
                    .map(s -> row("produit", s.produitLibelle(), "stock", s.stockActuel(), "quantite", s.quantiteSuggeree(), "fournisseur", s.fournisseurName()))
                    .toList(),
                null
            )
        );
    }

    /** Paramètre : {@code limite} = 5. */
    @Bean
    WidgetDataProvider performanceFournisseursWidget(ResponsableCommandeDashboardService service) {
        return WidgetDataProvider.of("performance-fournisseurs", (context, params) ->
            new WidgetData.Table(
                List.of(
                    Column.text("fournisseur", "Fournisseur"),
                    Column.number("commandes", "Commandes"),
                    Column.number("delai", "Délai moyen (j)"),
                    new Column("conformite", "Conformité", "percent"),
                    Column.number("note", "Note /5")
                ),
                service
                    .getPerformanceFournisseurs(WidgetPeriods.intParam(params, "limite", 5, 1, 20))
                    .stream()
                    .map(f ->
                        row(
                            "fournisseur",
                            f.fournisseurName(),
                            "commandes",
                            f.nombreCommandes(),
                            "delai",
                            f.delaiMoyenJours(),
                            "conformite",
                            f.tauxConformite(),
                            "note",
                            f.note()
                        )
                    )
                    .toList(),
                null
            )
        );
    }

    private static java.util.Map<String, Object> abcRow(String classe, AnalyseABCDTO.ClasseABCItem item) {
        if (item == null) {
            return row("classe", classe, "produits", 0, "partProduits", 0, "partCa", 0, "valeur", 0);
        }
        return row(
            "classe",
            classe,
            "produits",
            item.nombreProduits(),
            "partProduits",
            item.pourcentageProduits(),
            "partCa",
            item.pourcentageCA(),
            "valeur",
            item.valeur()
        );
    }

    private static AchatRecordParamDTO achatParam(Period period) {
        AchatRecordParamDTO param = new AchatRecordParamDTO();
        param.setFromDate(period.start());
        param.setToDate(period.end());
        return param;
    }

    private static Long receipt(AchatRecord record) {
        return record == null ? 0L : nz(record.receiptAmount());
    }
}
