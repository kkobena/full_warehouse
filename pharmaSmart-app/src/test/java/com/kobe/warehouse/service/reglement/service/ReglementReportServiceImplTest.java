package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import com.kobe.warehouse.service.report.Constant;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReglementReportServiceImpl")
class ReglementReportServiceImplTest {

    /** Gabarit minimal : Flying Saucer rend reellement, il lui faut du XHTML valide. */
    private static final String HTML = "<html><head><title>Releve</title></head><body><p>releve</p></body></html>";

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private StorageService storageService;

    private ReglementReportServiceImpl service;

    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new ReglementReportServiceImpl(templateEngine, storageService);
        magasin = new Magasin();
        magasin.setId(1);
        magasin.setFullName("PHARMACIE DU PLATEAU");
        magasin.setRegistre("RC-001");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
    }

    /** Un wrapper porte des montants deja formates : le service les reparse pour totaliser. */
    private static InvoicePaymentWrapper wrapper(String periode, InvoicePaymentDTO... paiements) {
        InvoicePaymentWrapper wrapper = new InvoicePaymentWrapper();
        wrapper.setPeriode(periode);
        wrapper.setInvoicePayments(List.of(paiements));
        return wrapper;
    }

    /** InvoicePaymentDTO ne se construit que depuis une entite complete. */
    private static InvoicePaymentDTO paiement(int totalAmount, int itemsCount) {
        AppUser user = new AppUser();
        user.setFirstName("Awa");
        user.setLastName("KOUAME");
        CashRegister caisse = new CashRegister();
        caisse.setUser(user);
        caisse.setMagasin(caisse.getUser().getMagasin());
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setId(3);
        tiersPayant.setName("CNAM");
        tiersPayant.setFullName("CNAM");
        FactureTiersPayant facture = new FactureTiersPayant().setId(7L).setMontantRegle(0);
        facture.setNumFacture("FA-0007");
        facture.setTiersPayant(tiersPayant);
        InvoicePayment paiement = new InvoicePayment();
        paiement.setId(55L);
        paiement.setTransactionDate(java.time.LocalDate.of(2026, 4, 18));
        paiement.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 18, 10, 0));
        paiement.setFactureTiersPayant(facture);
        paiement.setPaymentMode(new PaymentMode().code("CASH"));
        paiement.setCashRegister(caisse);
        paiement.setExpectedAmount(totalAmount);
        paiement.setPaidAmount(totalAmount);
        paiement.setMontantVerse(totalAmount);
        java.util.List<InvoicePaymentItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < itemsCount; i++) {
            items.add(new InvoicePaymentItem());
        }
        paiement.setInvoicePaymentItems(items);
        return new InvoicePaymentDTO(paiement);
    }

    @Test
    @DisplayName("le releve d un organisme produit un PDF")
    void relevePourUnOrganisme() throws Exception {
        byte[] pdf = service.printToPdf(wrapper("18/04/2026", paiement(1000, 2), paiement(500, 1)));

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("le releve d un organisme totalise le nombre de bons et le montant regle")
    void totauxPourUnOrganisme() throws Exception {
        service.printToPdf(wrapper("18/04/2026", paiement(1000, 2), paiement(500, 1)));

        assertThat(service.getParameters())
                .containsKey(Constant.REGLEMENT_COUNT)
                .containsKey(Constant.REGLEMENT_PAID_AMOUNT);
        assertThat((String) service.getParameters().get(Constant.REGLEMENT_COUNT))
                .contains("3");
        assertThat(service.getParameters()).containsEntry(Constant.MAGASIN, magasin);
    }

    @Test
    @DisplayName("le releve multi-organismes produit un PDF et reprend la periode du premier")
    void relevePourPlusieursOrganismes() throws Exception {
        InvoicePaymentWrapper w1 = wrapper("18/04/2026", paiement(1000, 2));
        InvoicePaymentWrapper w2 = wrapper("18/04/2026", paiement(500, 1));

        byte[] pdf = service.printToPdf(List.of(w1, w2));

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(service.getParameters()).containsEntry(Constant.PERIODE, "18/04/2026");
        assertThat(service.getParameters()).containsEntry(Constant.ITEMS, List.of(w1, w2));
    }

    @Test
    @DisplayName("une liste vide d organismes remonte une erreur d export")
    void listeVide() {
        List<InvoicePaymentWrapper> vide = List.of();

        assertThatThrownBy(() -> service.printToPdf(vide)).isInstanceOf(ReportFileExportException.class);
    }

    @Test
    @DisplayName("le nom de fichier genere est fixe et le rapport n est pas pagine")
    void contratDeRapport() {
        assertThat(service.getGenerateFileName()).isEqualTo("releve_reglement");
        assertThat(service.getMaxiRowCount()).isZero();
        assertThat(service.getItems()).isEmpty();
    }

    @Test
    @DisplayName("le rendu sans contexte explicite passe par les variables communes")
    void renduSansContexte() throws Exception {
        service.printToPdf(wrapper("18/04/2026", paiement(1000, 2)));

        assertThat(service.getTemplateAsHtml()).isEqualTo(HTML);
    }

    @Test
    @DisplayName("le rendu avec contexte y recopie les parametres")
    void renduAvecContexte() throws Exception {
        service.printToPdf(wrapper("18/04/2026", paiement(1000, 2)));
        Context context = new Context();

        assertThat(service.getTemplateAsHtml(context)).isEqualTo(HTML);
        assertThat(context.getVariable(Constant.MAGASIN)).isSameAs(magasin);
    }
}
