package com.kobe.warehouse.service.dashboard.widget.provider;

import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.nz;
import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.row;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dashboard.widget.WidgetData;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.Column;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.SeriesItem;
import com.kobe.warehouse.service.dashboard.widget.WidgetDataProvider;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import com.kobe.warehouse.service.report.MargeReportService;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Widgets « Finances » : marge sur douze mois, créances tiers payant, différés clients. Ce sont des
 * états à l'instant présent, calculés par les rapports financiers : ils ignorent la période.
 */
@Configuration
public class FinancesWidgets extends AbstractWidgets {

    /** Seuils du rapport de rentabilité, repris de l'accueil pharmacien. */
    private static final int SEUIL_MARGE_BASSE = 10;
    private static final int SEUIL_MARGE_HAUTE = 20;

    public FinancesWidgets(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    @Bean
    WidgetDataProvider marge12MoisWidget(MargeReportService margeReportService) {
        return WidgetDataProvider.of("marge-12-mois", (context, params) -> {
            MargeSummaryDTO marge = margeReportService.getMargeSummary(null, SEUIL_MARGE_BASSE, SEUIL_MARGE_HAUTE);
            if (marge == null) {
                return new WidgetData.Kpi(null, null, devise(), null);
            }
            return new WidgetData.Kpi(
                nz(marge.margeBruteGlobale()),
                null,
                devise(),
                String.format(
                    Locale.FRANCE,
                    "%.1f %% · %d référence(s) · 12 mois glissants",
                    marge.tauxMargeMoyen() == null ? 0 : marge.tauxMargeMoyen().doubleValue(),
                    nz(marge.totalProduits())
                )
            );
        });
    }

    /** Encours tiers payant par ancienneté, tous organismes confondus. */
    @Bean
    WidgetDataProvider creancesTpWidget(TiersPayantReportService tiersPayantReportService) {
        return WidgetDataProvider.of("creances-tp", (context, params) -> {
            List<TiersPayantCreancesSummaryDTO> creances = tiersPayantReportService.getCreancesSummary();
            return new WidgetData.Series(
                List.of("Moins de 30 j", "30 à 60 j", "60 à 90 j", "Plus de 90 j"),
                List.of(
                    new SeriesItem(
                        "Encours",
                        List.of(
                            sum(creances, TiersPayantCreancesSummaryDTO::montantMoinsDe30Jours),
                            sum(creances, TiersPayantCreancesSummaryDTO::montantEntre30Et60Jours),
                            sum(creances, TiersPayantCreancesSummaryDTO::montantEntre60Et90Jours),
                            sum(creances, TiersPayantCreancesSummaryDTO::montantPlusDe90Jours)
                        )
                    )
                ),
                "Encours total : %s · %d facture(s)".formatted(
                    amount(sum(creances, TiersPayantCreancesSummaryDTO::montantTotal)),
                    sum(creances, TiersPayantCreancesSummaryDTO::nombreFactures)
                )
            );
        });
    }

    @Bean
    WidgetDataProvider creancesParOrganismeWidget(TiersPayantReportService tiersPayantReportService) {
        return WidgetDataProvider.of("creances-par-organisme", (context, params) ->
            new WidgetData.Table(
                List.of(
                    Column.text("organisme", "Organisme"),
                    Column.number("factures", "Factures"),
                    Column.amount("encours", "Encours"),
                    Column.amount("plus90", "Plus de 90 j")
                ),
                safe(tiersPayantReportService.getCreancesSummary())
                    .stream()
                    .map(c ->
                        row(
                            "organisme",
                            c.groupeTiersPayantLibelle(),
                            "factures",
                            c.nombreFactures(),
                            "encours",
                            c.montantTotal(),
                            "plus90",
                            c.montantPlusDe90Jours()
                        )
                    )
                    .toList(),
                null
            )
        );
    }

    /** Différés clients encore dus, pour toute l'officine. */
    @Bean
    WidgetDataProvider differesEncoursWidget(ReglementDiffereService reglementDiffereService) {
        return WidgetDataProvider.of("differes-encours", (context, params) -> {
            DiffereSummary differes = reglementDiffereService.getDiffereSummary(null, Set.of(PaymentStatus.IMPAYE));
            if (differes == null) {
                return new WidgetData.Kpi(0L, null, devise(), null);
            }
            return new WidgetData.Kpi(
                nz(differes.rest()),
                null,
                devise(),
                "accordé %s · remboursé %s".formatted(amount(differes.saleAmount()), amount(differes.paidAmount()))
            );
        });
    }

    private static int sum(List<TiersPayantCreancesSummaryDTO> creances, Function<TiersPayantCreancesSummaryDTO, Integer> montant) {
        return safe(creances).stream().mapToInt(c -> nz(montant.apply(c))).sum();
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
