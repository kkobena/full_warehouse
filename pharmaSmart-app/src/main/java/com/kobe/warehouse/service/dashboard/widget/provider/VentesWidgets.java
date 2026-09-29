package com.kobe.warehouse.service.dashboard.widget.provider;

import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.nz;

import com.kobe.warehouse.service.dashboard.widget.WidgetData;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.Column;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.SeriesItem;
import com.kobe.warehouse.service.dashboard.widget.WidgetDataProvider;
import com.kobe.warehouse.service.dashboard.widget.WidgetPeriods;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.dashboard.widget.WidgetPeriods.Period;
import com.kobe.warehouse.service.dto.OrderBy;
import com.kobe.warehouse.service.TiersPayantService;
import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.dto.report.DashboardCAEvolutionDTO;
import com.kobe.warehouse.service.report.DashboardCAService;
import com.kobe.warehouse.service.stat.DashboardService;
import com.kobe.warehouse.service.stat.ProductStatService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;

/** Widgets « Ventes » : chiffre d'affaires, marge, panier, répartitions et meilleures ventes. */
@Configuration
public class VentesWidgets extends AbstractWidgets {

    public VentesWidgets(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    private static final Map<String, String> TYPES_VENTE = Map.of(
        "CashSale",
        "Comptant",
        "ThirdPartySales",
        "Tiers payant",
        "VenteDepot",
        "Dépôt"
    );

    @Bean
    WidgetDataProvider caNetWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("ca-net", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            VenteRecord current = closed(dashboardService.getPeridiqueCa(period.toVenteParam()));
            VenteRecord previous = closed(dashboardService.getPeridiqueCa(period.previous().toVenteParam()));
            return new WidgetData.Kpi(nz(current.netAmount()), nz(previous.netAmount()), devise(), "%d vente(s)".formatted(nzl(current.saleCount())));
        });
    }

    /** CA de la période comparé aux mêmes dates de l'année précédente : neutralise la saisonnalité. */
    @Bean
    WidgetDataProvider caVsN1Widget(DashboardService dashboardService) {
        return WidgetDataProvider.of("ca-vs-n1", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            Period anneePrecedente = new Period(period.start().minusYears(1), period.end().minusYears(1));
            VenteRecord current = closed(dashboardService.getPeridiqueCa(period.toVenteParam()));
            VenteRecord previous = closed(dashboardService.getPeridiqueCa(anneePrecedente.toVenteParam()));
            return new WidgetData.Kpi(
                nz(current.netAmount()),
                nz(previous.netAmount()),
                devise(),
                "%s l'an dernier aux mêmes dates".formatted(amount(previous.netAmount())),
                "vs l'an dernier"
            );
        });
    }

    @Bean
    WidgetDataProvider margeBruteWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("marge-brute", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            VenteRecord current = closed(dashboardService.getPeridiqueCa(period.toVenteParam()));
            VenteRecord previous = closed(dashboardService.getPeridiqueCa(period.previous().toVenteParam()));
            int net = nz(current.netAmount());
            // même taux que l'accueil pharmacien : marge rapportée au CA net
            double taux = net > 0 ? current.marge() * 100.0 / net : 0;
            return new WidgetData.Kpi(
                current.marge(),
                previous.marge(),
                devise(),
                String.format(Locale.FRANCE, "%.1f %% · coût %s", taux, amount(current.costAmount()))
            );
        });
    }

    @Bean
    WidgetDataProvider panierMoyenWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("panier-moyen", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            VenteRecord current = closed(dashboardService.getPeridiqueCa(period.toVenteParam()));
            VenteRecord previous = closed(dashboardService.getPeridiqueCa(period.previous().toVenteParam()));
            return new WidgetData.Kpi(current.panierMoyen(), previous.panierMoyen(), devise(), "HT, sur %d vente(s)".formatted(nzl(current.saleCount())));
        });
    }

    @Bean
    WidgetDataProvider ventesAnnuleesWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("ventes-annulees", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            VenteRecord current = canceled(dashboardService.getPeridiqueCa(period.toVenteParam()));
            VenteRecord previous = canceled(dashboardService.getPeridiqueCa(period.previous().toVenteParam()));
            return new WidgetData.Kpi(nz(current.netAmount()), nz(previous.netAmount()), devise(), "%d vente(s) annulée(s)".formatted(nzl(current.saleCount())));
        });
    }

    @Bean
    WidgetDataProvider ventesParTypeWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("ventes-par-type", (context, params) -> {
            var records = dashboardService.getCaGroupingByType(WidgetPeriods.of(context).toVenteParam());
            List<String> labels = records.stream().map(r -> TYPES_VENTE.getOrDefault(r.typeVente(), r.typeVente())).toList();
            List<Integer> values = records.stream().map(r -> nz(r.venteRecord().netAmount())).toList();
            return new WidgetData.Series(labels, List.of(new SeriesItem("CA net", values)), null);
        });
    }

    @Bean
    WidgetDataProvider modesReglementWidget(DashboardService dashboardService) {
        return WidgetDataProvider.of("modes-reglement", (context, params) -> {
            var records = dashboardService.getCaGroupingByPaimentMode(WidgetPeriods.of(context).toVenteParam());
            List<String> labels = records.stream().map(r -> r.libelle()).toList();
            List<Integer> values = records.stream().map(r -> nz(r.paidAmount())).toList();
            return new WidgetData.Series(labels, List.of(new SeriesItem("Montant encaissé", values)), null);
        });
    }

    @Bean
    WidgetDataProvider caEvolutionWidget(DashboardCAService dashboardCAService) {
        return WidgetDataProvider.of("ca-evolution", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            DashboardCAEvolutionDTO evolution = dashboardCAService.getEvolutionData(period.evolutionGranularity(), period.start(), period.end());
            return new WidgetData.Series(
                evolution.labels(),
                List.of(new SeriesItem("CA", evolution.caValues()), new SeriesItem("Période précédente", evolution.caPreviousValues())),
                null
            );
        });
    }

    /** Paramètres : {@code tri} = montant (défaut) | quantite, {@code limite} = 10. */
    @Bean
    WidgetDataProvider topProduitsWidget(ProductStatService productStatService) {
        return WidgetDataProvider.of("top-produits", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            ProduitRecordParamDTO param = new ProduitRecordParamDTO();
            param.setFromDate(period.start());
            param.setToDate(period.end());
            param.setOrder("quantite".equals(params.get("tri")) ? OrderBy.QUANTITY_SOLD : OrderBy.AMOUNT);
            int limite = WidgetPeriods.intParam(params, "limite", 10, 1, 50);

            List<Map<String, Object>> rows = productStatService
                .fetchProductStat(param, PageRequest.of(0, limite))
                .getContent()
                .stream()
                .map(p -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("libelle", p.libelle());
                    row.put("quantite", p.quantitySold());
                    row.put("montant", p.salesAmount());
                    return row;
                })
                .toList();
            return new WidgetData.Table(
                List.of(Column.text("libelle", "Produit"), Column.number("quantite", "Quantité"), Column.amount("montant", "Montant")),
                rows,
                null
            );
        });
    }

    /** Paramètre : {@code limite} = 10. */
    @Bean
    WidgetDataProvider ventesParTiersPayantWidget(TiersPayantService tiersPayantService) {
        return WidgetDataProvider.of("ventes-par-tiers-payant", (context, params) -> {
            VenteRecordParamDTO param = WidgetPeriods.of(context).toVenteParam();
            param.setLimit(WidgetPeriods.intParam(params, "limite", 10, 1, 50));
            return new WidgetData.Table(
                List.of(Column.text("tiersPayant", "Tiers payant"), Column.amount("montant", "Montant TTC")),
                tiersPayantService
                    .fetchAchatTiersPayant(param)
                    .stream()
                    .map(tp -> WidgetFormat.row("tiersPayant", tp.tiersPayantName(), "montant", tp.montantTtc()))
                    .toList(),
                null
            );
        });
    }

    /**
     * Produits qui font 80 % des ventes. Paramètre : {@code tri} = montant (défaut) | quantite. La
     * liste peut être longue ; elle est coupée à {@link #PARETO_MAX_LIGNES} lignes.
     */
    @Bean
    WidgetDataProvider paretoProduitsWidget(ProductStatService productStatService) {
        return WidgetDataProvider.of("pareto-produits", (context, params) -> {
            Period period = WidgetPeriods.of(context);
            ProduitRecordParamDTO param = new ProduitRecordParamDTO();
            param.setFromDate(period.start());
            param.setToDate(period.end());
            boolean parQuantite = "quantite".equals(params.get("tri"));
            param.setOrder(parQuantite ? OrderBy.QUANTITY_SOLD : OrderBy.AMOUNT);

            var produits = productStatService.fetch20x80(param);
            List<Map<String, Object>> rows = produits
                .stream()
                .limit(PARETO_MAX_LIGNES)
                .map(p -> WidgetFormat.row("libelle", p.libelle(), "cip", p.codeCip(), "total", p.total(), "part", p.pourcentage()))
                .toList();
            return new WidgetData.Table(
                List.of(
                    Column.text("libelle", "Produit"),
                    Column.text("cip", "CIP"),
                    parQuantite ? Column.number("total", "Quantité") : Column.amount("total", "Montant"),
                    new Column("part", "Part", "percent")
                ),
                rows,
                "%d produit(s) font 80 %% des ventes".formatted(produits.size())
            );
        });
    }

    private static final int PARETO_MAX_LIGNES = 100;

    private static VenteRecord closed(VenteRecordWrapper wrapper) {
        return Optional.ofNullable(wrapper).map(VenteRecordWrapper::close).orElse(WidgetFormat.EMPTY_VENTE);
    }

    private static VenteRecord canceled(VenteRecordWrapper wrapper) {
        return Optional.ofNullable(wrapper).map(VenteRecordWrapper::canceled).orElse(WidgetFormat.EMPTY_VENTE);
    }

    private static long nzl(Long value) {
        return value == null ? 0 : value;
    }
}
