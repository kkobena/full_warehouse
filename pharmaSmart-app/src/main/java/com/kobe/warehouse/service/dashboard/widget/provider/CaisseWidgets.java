package com.kobe.warehouse.service.dashboard.widget.provider;

import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.nz;
import static com.kobe.warehouse.service.dashboard.widget.provider.WidgetFormat.row;

import com.kobe.warehouse.service.dashboard.CaissierDashboardService;
import com.kobe.warehouse.service.dashboard.widget.WidgetData;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.Column;
import com.kobe.warehouse.service.dashboard.widget.WidgetData.SeriesItem;
import com.kobe.warehouse.service.dashboard.widget.WidgetDataProvider;
import com.kobe.warehouse.service.dashboard.widget.WidgetPeriods;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.dto.dashboard.CaisseStatusDTO;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Widgets « Caisse ». Ceux marqués « moi » lisent la caisse de l'utilisateur connecté :
 * {@link CaissierDashboardService} filtre lui-même sur le contexte de sécurité.
 */
@Configuration
public class CaisseWidgets extends AbstractWidgets {

    public CaisseWidgets(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    /** Moi : état de ma caisse. */
    @Bean
    WidgetDataProvider maCaisseWidget(CaissierDashboardService service) {
        return WidgetDataProvider.of("ma-caisse", (context, params) -> {
            CaisseStatusDTO caisse = service.getCaisseStatus();
            if (caisse == null || !"OUVERTE".equals(caisse.etat())) {
                return new WidgetData.Kpi(null, null, devise(), "Caisse fermée");
            }
            return new WidgetData.Kpi(
                nz(caisse.especesTheoriques()),
                null,
                devise(),
                "Espèces théoriques · fond %s · ouverte à %s".formatted(amount(caisse.fondOuverture()), caisse.heureOuverture())
            );
        });
    }

    /** Moi : encaissements de ma session, par mode. */
    @Bean
    WidgetDataProvider mesEncaissementsWidget(CaissierDashboardService service) {
        return WidgetDataProvider.of("mes-encaissements", (context, params) -> {
            var session = service.getSessionEncaissements();
            if (session == null || session.lignes() == null) {
                return new WidgetData.Series(List.of(), List.of(new SeriesItem("Montant", List.of())), null);
            }
            return new WidgetData.Series(
                session.lignes().stream().map(l -> l.libelle()).toList(),
                List.of(new SeriesItem("Montant", session.lignes().stream().map(l -> nz(l.montant())).toList())),
                "Total encaissé : %s · %d transaction(s)".formatted(amount(session.totalEncaisse()), nz(session.nombreTransactions()))
            );
        });
    }

    /** Moi : mes dernières ventes. Paramètre : {@code limite} = 8. */
    @Bean
    WidgetDataProvider mesVentesRecentesWidget(CaissierDashboardService service) {
        return WidgetDataProvider.of("mes-ventes-recentes", (context, params) ->
            new WidgetData.Table(
                List.of(
                    new Column("date", "Heure", "datetime"),
                    Column.text("recu", "Reçu"),
                    Column.text("client", "Client"),
                    Column.text("type", "Type"),
                    Column.amount("montant", "Montant")
                ),
                service
                    .getVentesRecentes(WidgetPeriods.intParam(params, "limite", 8, 1, 30))
                    .stream()
                    .map(v ->
                        row("date", v.dateVente(), "recu", v.numeroRecu(), "client", v.clientNom(), "type", v.typeVente(), "montant", v.montant())
                    )
                    .toList(),
                null
            )
        );
    }

    @Bean
    WidgetDataProvider differesARelancerWidget(CaissierDashboardService service) {
        return WidgetDataProvider.of("differes-a-relancer", (context, params) -> {
            var resume = service.getDifferesRelance();
            List<java.util.Map<String, Object>> rows = resume == null || resume.differes() == null
                ? List.of()
                : resume
                    .differes()
                    .stream()
                    .map(d ->
                        row(
                            "client",
                            d.clientNom(),
                            "telephone",
                            d.clientTelephone(),
                            "montant",
                            d.montantDu(),
                            "echeance",
                            d.dateEcheance(),
                            "retard",
                            d.joursRetard()
                        )
                    )
                    .toList();
            return new WidgetData.Table(
                List.of(
                    Column.text("client", "Client"),
                    Column.text("telephone", "Téléphone"),
                    Column.amount("montant", "Montant dû"),
                    new Column("echeance", "Échéance", "date"),
                    Column.number("retard", "Retard (j)")
                ),
                rows,
                resume == null
                    ? null
                    : "%d échéance(s) aujourd'hui · total dû %s".formatted(nz(resume.nombreEcheancesAujourdhui()), amount(resume.montantTotalDu()))
            );
        });
    }

    @Bean
    WidgetDataProvider livraisonsDuJourWidget(CaissierDashboardService service) {
        return WidgetDataProvider.of("livraisons-du-jour", (context, params) ->
            new WidgetData.Table(
                List.of(Column.text("fournisseur", "Fournisseur"), Column.number("references", "Références")),
                service.getLivraisonsJour().stream().map(l -> row("fournisseur", l.fournisseurNom(), "references", l.nombreReferences())).toList(),
                null
            )
        );
    }
}
