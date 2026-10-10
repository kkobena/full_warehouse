package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;
import com.kobe.warehouse.service.report.Constant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementDiffereReportServiceImpl")
class ReglementDiffereReportServiceImplTest {

    private static final String HTML = "<html><head><title>Differes</title></head><body><p>differes</p></body></html>";


    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private StorageService storageService;

    private ReglementDiffereReportServiceImpl service;

    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new ReglementDiffereReportServiceImpl(templateEngine, storageService);
        magasin = new Magasin();
        magasin.setId(1);
        magasin.setFullName("PHARMACIE DU PLATEAU");
        magasin.setRegistre("RC-001");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
    }

    private static DiffereDTO differe() {
        return new DiffereDTO(3, "Kofi", "YAO", 10000L, 4000L, 6000L, List.of());
    }

    private static DiffereSummary summary() {
        return new DiffereSummary(10000L, 4000L, 6000L);
    }

    private static ReglementDiffereWrapperDTO reglement() {
        return new ReglementDiffereWrapperDTO(3, "Kofi", "YAO", 4000L, 6000L, List.of());
    }

    @Test
    @DisplayName("la liste des differes produit un PDF")
    void listeDesDifferes() throws Exception {
        byte[] pdf = service.printListToPdf(List.of(differe()), summary());

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("la liste des differes date son titre du jour et transmet la synthese")
    void variablesDeLaListe() throws Exception {
        DiffereSummary summary = summary();
        List<DiffereDTO> differes = List.of(differe());

        service.printListToPdf(differes, summary);

        assertThat((String) service.getParameters().get(Constant.REPORT_TITLE)).startsWith("LISTE DES DIFFERES  AU ");
        assertThat(service.getParameters()).containsEntry(Constant.ITEMS, differes).containsEntry(Constant.REPORT_SUMMARY, summary);
        assertThat(service.getParameters()).containsEntry(Constant.MAGASIN, magasin);
    }

    @Test
    @DisplayName("la liste des reglements produit un PDF")
    void listeDesReglements() throws Exception {
        byte[] pdf = service.printReglementToPdf(
            List.of(reglement()),
            new DifferePaymentSummaryDTO(4000L, 6000L),
            new ReportPeriode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
        );

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("le titre des reglements borne la periode demandee")
    void titreDesReglements() throws Exception {
        service.printReglementToPdf(
            List.of(reglement()),
            new DifferePaymentSummaryDTO(4000L, 6000L),
            new ReportPeriode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
        );

        assertThat((String) service.getParameters().get(Constant.REPORT_TITLE))
            .startsWith("LISTE DES REGLEMENTS DIFFERES PERIODE DU ")
            .contains(" AU ");
    }

    @Test
    @DisplayName("le nom de fichier depend du dernier rapport demande")
    void nomDeFichierContextuel() throws Exception {
        service.printListToPdf(List.of(differe()), summary());
        assertThat(service.getGenerateFileName()).isEqualTo("liste_des_differes");

        service.printReglementToPdf(
            List.of(reglement()),
            new DifferePaymentSummaryDTO(4000L, 6000L),
            new ReportPeriode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
        );
        assertThat(service.getGenerateFileName()).isEqualTo("liste_des_reglements_differes");
    }

    @Test
    @DisplayName("le rapport n est pas pagine et n expose aucun element")
    void contratDeRapport() {
        assertThat(service.getMaxiRowCount()).isZero();
        assertThat(service.getItems()).isEmpty();
    }

    @Test
    @DisplayName("le rendu sans contexte explicite passe par les variables communes")
    void renduSansContexte() throws Exception {
        service.printListToPdf(List.of(differe()), summary());

        assertThat(service.getTemplateAsHtml()).isEqualTo(HTML);
    }

    @Test
    @DisplayName("le rendu avec contexte y recopie les parametres")
    void renduAvecContexte() throws Exception {
        service.printListToPdf(List.of(differe()), summary());
        Context context = new Context();

        assertThat(service.getTemplateAsHtml(context)).isEqualTo(HTML);
        assertThat(context.getVariable(Constant.MAGASIN)).isSameAs(magasin);
    }
}
