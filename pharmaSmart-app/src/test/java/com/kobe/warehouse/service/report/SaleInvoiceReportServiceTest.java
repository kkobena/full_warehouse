package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.net.MalformedURLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * La facture remise au client est le seul document de vente que l'officine délivre sur demande, et le
 * seul qu'un client puisse présenter à son assurance ou à son comptable. Elle doit donc porter
 * l'identité complète de l'officine et la devise réellement configurée — une facture libellée dans
 * une monnaie qui n'est pas celle de l'encaissement n'est pas opposable.
 */
@DisplayName("SaleInvoiceReportService — facture client")
class SaleInvoiceReportServiceTest {

    /** Gabarit minimal : Flying Saucer rend réellement, il lui faut du XHTML valide. */
    private static final String HTML = "<html><head><title>Facture</title></head><body><p>facture</p></body></html>";

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
    private final StorageService storageService = mock(StorageService.class);

    private final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);

    private final SaleInvoiceReportService service = new SaleInvoiceReportService(
        templateEngine,
        storageService
    );

    private Magasin magasin;

    @BeforeEach
    void officine() {
        magasin = new Magasin();
        magasin.setId(1);
        magasin.setFullName("PHARMACIE DU PLATEAU");
        magasin.setRegistre("RC-001");
        magasin.setPhone("0102030405");
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(utilisateur);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
    }

    @Test
    @DisplayName("la facture produit un PDF")
    void produitUnPdf() throws MalformedURLException {
        byte[] pdf = service.printInvoice(new SaleDTO());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("la facture porte la vente et l'officine qui l'a établie")
    void variablesDuGabarit() throws MalformedURLException {
        SaleDTO vente = new SaleDTO();

        service.printInvoice(vente);

        assertThat(service.getParameters()).containsEntry(Constant.SALE, vente).containsEntry(Constant.MAGASIN, magasin);
    }

    @Test
    @DisplayName("la facture se rend depuis le gabarit de facture client")
    void gabaritUtilise() throws MalformedURLException {
        service.printInvoice(new SaleDTO());

        assertThat(gabaritRendu()).isEqualTo(Constant.INVOICE_TEMPLATE_FILE);
    }

    @Test
    @DisplayName("le pied de page reprend les mentions légales de l'officine")
    void piedDePage() throws MalformedURLException {
        service.printInvoice(new SaleDTO());

        assertThat((String) service.getParameters().get(Constant.FOOTER)).contains("RC-001").contains("0102030405");
    }

    /** La devise vient de la configuration de l'officine, jamais d'une constante du code. */
    @Test
    @DisplayName("la devise de la facture est celle configurée pour l'officine")
    void deviseConfiguree() throws MalformedURLException {
        when(appConfigurationService.getDevise()).thenReturn("EUR");
        service.setAppConfigurationService(appConfigurationService);

        service.printInvoice(new SaleDTO());

        assertThat(contexteRendu().getVariable("devise")).isEqualTo("EUR");
    }

    @Test
    @DisplayName("sans configuration accessible, la devise retombe sur une valeur par défaut")
    void deviseParDefaut() throws MalformedURLException {
        service.printInvoice(new SaleDTO());

        assertThat(contexteRendu().getVariable("devise")).isEqualTo("FCFA");
    }

    // ===== utilitaires =====

    private String gabaritRendu() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(templateEngine, atLeastOnce()).process(captor.capture(), any(Context.class));
        return captor.getValue();
    }

    private Context contexteRendu() {
        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine, atLeastOnce()).process(anyString(), captor.capture());
        return captor.getValue();
    }
}
