package com.kobe.warehouse.service.dashboard.widget.provider;

import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.zeroIfNull;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dashboard.ResponsableCommandeDashboardService;
import com.kobe.warehouse.service.dashboard.widget.WidgetData;
import com.kobe.warehouse.service.dashboard.widget.WidgetDataProvider;
import com.kobe.warehouse.service.dashboard.widget.WidgetPeriods;
import com.kobe.warehouse.service.dashboard.widget.WidgetPeriods.Period;
import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import com.kobe.warehouse.service.report.MargeReportService;
import com.kobe.warehouse.service.report.StockValuationReportService;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stat.DashboardService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Widgets composites : une tuile regroupe plusieurs métriques connexes pour réduire la densité de la
 * grille sans perdre d'informations.
 */
@Configuration
public class CompositeWidgets extends AbstractWidgets {

    private static final int SEUIL_MARGE_BASSE = 10;
    private static final int SEUIL_MARGE_HAUTE = 20;

    public CompositeWidgets(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    /**
     * CA net + marge brute + panier moyen + annulations : toute l'activité ventes en une tuile.
     * Remplace avantageusement ca-net, marge-brute et panier-moyen séparés.
     */
    @Bean
    WidgetDataProvider syntheseActiviteWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("synthese-activite", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            VenteRecordWrapper wrapper = dashboardService.getPeridiqueCa(period.toVenteParam());
            VenteRecord current = closed(wrapper);
            VenteRecord previous = closed(dashboardService.getPeridiqueCa(period.previous().toVenteParam()));
            VenteRecord annulees = canceled(wrapper);
            int net = zeroIfNull(current.netAmount());
            double taux = net > 0 ? current.marge() * 100.0 / net : 0;
            return new WidgetData.Kpi(
                net, zeroIfNull(previous.netAmount()), devise(),
                "%d annulation(s) · %s".formatted(zeroIfNull(annulees.saleCount()), amount(annulees.netAmount())),
                null,
                List.of(
                    new WidgetData.Kpi.Secondary("Marge brute", current.marge(), devise()),
                    new WidgetData.Kpi.Secondary("Taux marge", taux, "%"),
                    new WidgetData.Kpi.Secondary("Panier", current.panierMoyen(), devise())
                ),
                null
            );
        });
    }

    /**
     * Stock valorisé + péremptions à risque + marge potentielle : la santé du stock en un coup d'œil.
     * Remplace stock-valorise et peremptions-valorisees séparés.
     */
    @Bean
    WidgetDataProvider santeStockWidget(StockValuationReportService stockService,
                                        ResponsableCommandeDashboardService commandeService) {
        return WidgetDataProvider.of("sante-stock", (context, params) -> {
            StockValuationSummaryDTO stock = stockService.getStockValuationSummary();
            var peremptions = commandeService.getPeremptions();
            if (stock == null) {
                return new WidgetData.Kpi(null, null, devise(), null);
            }
            int nbPeremptions = zeroIfNull(peremptions.unMois())
                + zeroIfNull(peremptions.unATroisMois())
                + zeroIfNull(peremptions.troisASixMois());
            double tauxMarge = stock.averageMarginPercentage() == null ? 0
                : stock.averageMarginPercentage().doubleValue();
            return new WidgetData.Kpi(
                zeroIfNull(stock.totalPurchaseValue()),
                null,
                devise(),
                String.format(Locale.FRANCE, "%d référence(s) · marge potentielle %.1f %%",
                    zeroIfNull(stock.totalProducts()), tauxMarge),
                null,
                List.of(
                    new WidgetData.Kpi.Secondary("Péremptions (6 mois)", zeroIfNull(peremptions.valeurTotale()), devise()),
                    new WidgetData.Kpi.Secondary("Produits à risque", nbPeremptions, null)
                ),
                null
            );
        });
    }

    /**
     * Achats de la période + commandes en attente + qualité fournisseurs.
     * Remplace achats-periode, commandes-en-cours et qualite-fournisseurs séparés.
     */
    @Bean
    WidgetDataProvider syntheseAchatsWidget(DashboardService dashboardService,
                                             ResponsableCommandeDashboardService commandeService,
                                             SupplierPerformanceReportService supplierService) {
        return WidgetDataProvider.of("synthese-achats", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            AchatRecord current = dashboardService.getAchatPeriode(achatParam(period));
            AchatRecord previous = dashboardService.getAchatPeriode(achatParam(period.previous()));
            var commandes = commandeService.getCommandesEnCours();
            SupplierPerformanceSummaryDTO qualite = supplierService.getSupplierPerformanceSummary();
            double conformite = qualite == null || qualite.avgConformityRate() == null ? 0
                : qualite.avgConformityRate().doubleValue();
            return new WidgetData.Kpi(
                receipt(current), receipt(previous), devise(),
                current == null ? null
                    : "%d réception(s) · %d commande(s) à réceptionner".formatted(
                        zeroIfNull(current.achatCount()), zeroIfNull(commandes.aReceptionner())),
                null,
                List.of(
                    new WidgetData.Kpi.Secondary("Commandes en cours", zeroIfNull(commandes.totalMontant()), devise()),
                    new WidgetData.Kpi.Secondary("Qualité fournisseurs", conformite, "%")
                ),
                null
            );
        });
    }

    /**
     * Différés clients + marge 12 mois + créances tiers payant : le bilan financier de l'officine.
     * Remplace differes-encours, marge-12-mois et donne un aperçu des créances TP.
     */
    @Bean
    WidgetDataProvider bilanFinancesWidget(ReglementDiffereService differesService,
                                            MargeReportService margeService,
                                            TiersPayantReportService creancesService) {
        return WidgetDataProvider.of("bilan-finances", (context, params) -> {
            DiffereSummary differes = differesService.getDiffereSummary(null, Set.of(PaymentStatus.IMPAYE));
            MargeSummaryDTO marge = margeService.getMargeSummary(null, SEUIL_MARGE_BASSE, SEUIL_MARGE_HAUTE);
            long totalCreances = safe(creancesService.getCreancesSummary()).stream()
                .mapToLong(c -> zeroIfNull(c.montantTotal())).sum();
            long differesRest = differes == null ? 0L : zeroIfNull(differes.rest());
            double tauxMarge = marge == null || marge.tauxMargeMoyen() == null ? 0
                : marge.tauxMargeMoyen().doubleValue();
            return new WidgetData.Kpi(
                differesRest, null, devise(),
                differes == null ? null
                    : "accordé %s · remboursé %s".formatted(amount(differes.saleAmount()), amount(differes.paidAmount())),
                null,
                List.of(
                    new WidgetData.Kpi.Secondary("Marge 12 mois", marge == null ? null : marge.margeBruteGlobale(), devise()),
                    new WidgetData.Kpi.Secondary("Taux marge", tauxMarge, "%"),
                    new WidgetData.Kpi.Secondary("Créances TP", totalCreances, devise())
                ),
                null
            );
        });
    }

    private static AchatRecordParamDTO achatParam(Period period) {
        AchatRecordParamDTO param = new AchatRecordParamDTO();
        param.setFromDate(period.start());
        param.setToDate(period.end());
        return param;
    }

    private static long receipt(AchatRecord record) {
        return record == null ? 0L : zeroIfNull(record.receiptAmount());
    }

    private static VenteRecord closed(VenteRecordWrapper wrapper) {
        return Optional.ofNullable(wrapper).map(VenteRecordWrapper::close).orElse(WidgetFormat.EMPTY_VENTE);
    }

    private static VenteRecord canceled(VenteRecordWrapper wrapper) {
        return Optional.ofNullable(wrapper).map(VenteRecordWrapper::canceled).orElse(WidgetFormat.EMPTY_VENTE);
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
