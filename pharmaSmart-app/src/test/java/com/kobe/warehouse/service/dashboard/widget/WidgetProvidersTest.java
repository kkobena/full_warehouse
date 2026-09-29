package com.kobe.warehouse.service.dashboard.widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.service.dashboard.CaissierDashboardService;
import com.kobe.warehouse.service.dashboard.widget.provider.CaisseWidgets;
import com.kobe.warehouse.service.dashboard.widget.provider.StockAchatsWidgets;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import com.kobe.warehouse.service.report.StockRotationReportService;
import java.util.List;
import com.kobe.warehouse.service.dashboard.widget.provider.VentesWidgets;
import com.kobe.warehouse.service.dto.dashboard.CaisseStatusDTO;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stat.DashboardService;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Widgets — périodes et fournisseurs")
class WidgetProvidersTest {

    private static final WidgetContext SEPTEMBRE = new WidgetContext(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, "pharmacien");
    private static final AppConfigurationService DEVISE = mock(AppConfigurationService.class);

    static {
        when(DEVISE.getDevise()).thenReturn("FCFA");
    }

    @Nested
    @DisplayName("WidgetPeriods")
    class Periodes {

        @Test
        @DisplayName("la période précédente a la même durée et finit la veille")
        void periodePrecedente() {
            WidgetPeriods.Period precedente = WidgetPeriods.of(SEPTEMBRE).previous();
            assertThat(precedente.start()).isEqualTo(LocalDate.of(2026, 8, 2));
            assertThat(precedente.end()).isEqualTo(LocalDate.of(2026, 8, 31));
        }

        @Test
        @DisplayName("sans dates, la journée en cours ; dates inversées, remises dans l'ordre")
        void bornes() {
            assertThat(WidgetPeriods.of(new WidgetContext(null, null, null, "x")).days()).isEqualTo(1);
            WidgetPeriods.Period inversee = WidgetPeriods.of(new WidgetContext(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), null, "x"));
            assertThat(inversee.start()).isBefore(inversee.end());
        }

        @Test
        @DisplayName("la granularité de la courbe suit la durée")
        void granularite() {
            assertThat(WidgetPeriods.of(SEPTEMBRE).evolutionGranularity()).isEqualTo("daily");
            assertThat(new WidgetPeriods.Period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31)).evolutionGranularity()).isEqualTo("weekly");
            assertThat(new WidgetPeriods.Period(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)).evolutionGranularity()).isEqualTo("monthly");
        }

        @Test
        @DisplayName("un paramètre numérique est borné, et remplacé s'il est invalide")
        void parametres() {
            assertThat(WidgetPeriods.intParam(Map.of("limite", "500"), "limite", 10, 1, 50)).isEqualTo(50);
            assertThat(WidgetPeriods.intParam(Map.of("limite", "abc"), "limite", 10, 1, 50)).isEqualTo(10);
            assertThat(WidgetPeriods.intParam(Map.of(), "limite", 10, 1, 50)).isEqualTo(10);
        }
    }

    @Test
    @DisplayName("marge brute : la valeur de la période et celle de la période précédente")
    void margeBrute() throws Exception {
        DashboardService dashboardService = mock(DashboardService.class);
        when(dashboardService.getPeridiqueCa(argThat(p -> p != null && p.getFromDate().equals(LocalDate.of(2026, 9, 1))))).thenReturn(
            new VenteRecordWrapper(vente(100_000, 60_000), null)
        );
        when(dashboardService.getPeridiqueCa(argThat(p -> p != null && p.getFromDate().equals(LocalDate.of(2026, 8, 2))))).thenReturn(
            new VenteRecordWrapper(vente(80_000, 50_000), null)
        );

        WidgetData.Kpi kpi = (WidgetData.Kpi) provider(new VentesWidgets(DEVISE), "margeBruteWidget", dashboardService).load(SEPTEMBRE, Map.of());

        assertThat(kpi.value()).isEqualTo(40_000);
        assertThat(kpi.previousValue()).isEqualTo(30_000);
        assertThat(kpi.detail()).startsWith("40,0 %").endsWith("FCFA");
        assertThat(kpi.unit()).isEqualTo("FCFA");
    }

    @Test
    @DisplayName("CA vs N-1 : comparé aux mêmes dates un an plus tôt, pas à la période précédente")
    void caVsN1() throws Exception {
        DashboardService dashboardService = mock(DashboardService.class);
        when(dashboardService.getPeridiqueCa(argThat(p -> p != null && p.getFromDate().equals(LocalDate.of(2026, 9, 1))))).thenReturn(
            new VenteRecordWrapper(vente(120_000, 0), null)
        );
        when(dashboardService.getPeridiqueCa(argThat(p -> p != null && p.getFromDate().equals(LocalDate.of(2025, 9, 1)) && p.getToDate().equals(LocalDate.of(2025, 9, 30))))).thenReturn(
            new VenteRecordWrapper(vente(100_000, 0), null)
        );

        WidgetData.Kpi kpi = (WidgetData.Kpi) provider(new VentesWidgets(DEVISE), "caVsN1Widget", dashboardService).load(SEPTEMBRE, Map.of());

        assertThat(kpi.value()).isEqualTo(120_000);
        assertThat(kpi.previousValue()).isEqualTo(100_000);
        assertThat(kpi.previousLabel()).isEqualTo("vs l'an dernier");
    }

    @Test
    @DisplayName("stock dormant : classé par valeur immobilisée, sans les produits à valeur nulle")
    void stockDormant() throws Exception {
        StockRotationReportService service = mock(StockRotationReportService.class);
        when(service.getSlowMovingProducts()).thenReturn(List.of(lent("Petit", 1_000L), lent("Gros", 50_000L), lent("Vide", 0L)));

        WidgetData.Table table = (WidgetData.Table) provider(new StockAchatsWidgets(DEVISE), "stockDormantWidget", service).load(SEPTEMBRE, Map.of());

        assertThat(table.rows()).extracting(r -> r.get("produit")).containsExactly("Gros", "Petit");
        assertThat(table.footer()).startsWith("2 produit(s) dormant(s)").endsWith("FCFA immobilisés");
    }

    private static StockRotationDTO lent(String libelle, Long valeur) {
        return new StockRotationDTO(1, libelle, "CIP", null, 10, 100, valeur, 0, 0, 0, 0, 0, null, 400, null);
    }

    @Test
    @DisplayName("ventes annulées : un service qui ne renvoie rien donne zéro, pas une erreur")
    void serviceVide() throws Exception {
        DashboardService dashboardService = mock(DashboardService.class);
        WidgetData.Kpi kpi = (WidgetData.Kpi) provider(new VentesWidgets(DEVISE), "ventesAnnuleesWidget", dashboardService).load(SEPTEMBRE, Map.of());
        assertThat(kpi.value()).isEqualTo(0);
    }

    @Test
    @DisplayName("ma caisse : fermée, aucune valeur n'est affichée")
    void caisseFermee() throws Exception {
        CaissierDashboardService service = mock(CaissierDashboardService.class);
        when(service.getCaisseStatus()).thenReturn(new CaisseStatusDTO(10_000L, 0L, 10_000L, null, "FERMEE", null));
        WidgetData.Kpi kpi = (WidgetData.Kpi) provider(new CaisseWidgets(DEVISE), "maCaisseWidget", service).load(SEPTEMBRE, Map.of());
        assertThat(kpi.value()).isNull();
        assertThat(kpi.detail()).isEqualTo("Caisse fermée");
    }

    private static VenteRecord vente(int netAmount, int costAmount) {
        return new VenteRecord(netAmount, 0, 0, costAmount, 0, netAmount, (double) netAmount, 0, 0, 0, 0, 0, 0, 0, 10L, null, null, null);
    }

    private static WidgetDataProvider provider(Object configuration, String beanMethod, Object service) throws Exception {
        for (Method method : configuration.getClass().getDeclaredMethods()) {
            if (method.getName().equals(beanMethod)) {
                method.setAccessible(true);
                return (WidgetDataProvider) method.invoke(configuration, service);
            }
        }
        throw new IllegalArgumentException(beanMethod);
    }
}
