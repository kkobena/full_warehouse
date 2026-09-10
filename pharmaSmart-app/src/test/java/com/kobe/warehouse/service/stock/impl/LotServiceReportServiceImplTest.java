package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LotServiceReportServiceImpl")
class LotServiceReportServiceImplTest {

    private static final String HTML = "<html><head><title>Perimes</title></head><body><p>perimes</p></body></html>";

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private FileStorageProperties fileStorageProperties;

    @Mock
    private StorageService storageService;

    private LotServiceReportServiceImpl service;

    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new LotServiceReportServiceImpl(templateEngine, fileStorageProperties, storageService);
        magasin = new Magasin();
        magasin.setId(1);
        magasin.setFullName("PHARMACIE DU PLATEAU");
        magasin.setRegistre("RC-001");
        magasin.setCompteContribuable("CC-002");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
    }

    private static LotPerimeValeurSum somme() {
        return new LotPerimeValeurSum(45000L, 90000L, 12, 3L, 5L, 2L);
    }

    @Test
    @DisplayName("produit une reponse PDF en piece jointe")
    void reponsePdf() {
        ResponseEntity<byte[]> response = service.generatePdf(
            List.of(),
            somme(),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31)
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .startsWith("attachment; filename=produits_perimes_")
            .endsWith(".pdf");
        assertThat(response.getBody()).isNotEmpty();
        assertThat(new String(response.getBody(), 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("compose un titre borne par la periode demandee")
    void titreAvecPeriode() {
        service.generatePdf(List.of(), somme(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertThat((String) service.getParameters().get(Constant.REPORT_TITLE))
            .startsWith("Liste des produits périmés du ")
            .contains(" au ");
    }

    @Test
    @DisplayName("une borne de fin absente est remplacee par la date du jour")
    void borneDeFinAbsente() {
        service.generatePdf(List.of(), somme(), LocalDate.of(2026, 1, 1), null);

        assertThat((String) service.getParameters().get(Constant.REPORT_TITLE)).contains(" au ");
    }

    @Test
    @DisplayName("transmet la liste et la synthese au gabarit")
    void transmetListeEtSynthese() {
        LotPerimeValeurSum sum = somme();
        List<LotPerimeDTO> lots = List.of();

        service.generatePdf(lots, sum, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertThat(service.getParameters())
            .containsEntry(Constant.ITEMS, lots)
            .containsEntry(Constant.REPORT_SUMMARY, sum);
    }

    @Test
    @DisplayName("renseigne l officine et son pied de page")
    void officineEtPiedDePage() {
        service.generatePdf(List.of(), somme(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertThat(service.getParameters()).containsEntry(Constant.MAGASIN, magasin);
        assertThat((String) service.getParameters().get(Constant.FOOTER))
            .contains("RC N° RC-001")
            .contains("CC N° CC-002");
    }

    @Test
    @DisplayName("le nom de fichier genere est fixe")
    void nomDeFichier() {
        assertThat(service.getGenerateFileName()).isEqualTo("produits_perimes_");
    }

    @Test
    @DisplayName("le rapport n est pas pagine et n expose aucun element")
    void sansPaginationNiElements() {
        assertThat(service.getMaxiRowCount()).isZero();
        assertThat(service.getItems()).isEmpty();
    }

    @Test
    @DisplayName("le rendu sans contexte explicite passe par les variables communes")
    void renduSansContexte() {
        service.generatePdf(List.of(), somme(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertThat(service.getTemplateAsHtml()).isEqualTo(HTML);
    }

    @Test
    @DisplayName("le rendu avec contexte y recopie les parametres")
    void renduAvecContexte() {
        service.generatePdf(List.of(), somme(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        Context context = new Context();

        assertThat(service.getTemplateAsHtml(context)).isEqualTo(HTML);
        assertThat(context.getVariable(Constant.MAGASIN)).isSameAs(magasin);
    }
}
