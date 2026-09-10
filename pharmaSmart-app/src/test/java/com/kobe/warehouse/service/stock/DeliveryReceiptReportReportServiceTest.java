package com.kobe.warehouse.service.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.report.Constant;
import java.time.LocalDate;
import java.util.ArrayList;
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
@DisplayName("DeliveryReceiptReportReportService")
class DeliveryReceiptReportReportServiceTest {

    /** Gabarit minimal : Flying Saucer rend reellement, il lui faut du XHTML valide. */
    private static final String HTML = "<html><head><title>BL</title></head><body><p>bon de livraison</p></body></html>";

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private StorageService storageService;

    @Mock
    private FileStorageProperties fileStorageProperties;

    private DeliveryReceiptReportReportService service;

    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new DeliveryReceiptReportReportService(templateEngine, storageService, fileStorageProperties);
        magasin = new Magasin();
        magasin.setId(1);
        magasin.setFullName("PHARMACIE DU PLATEAU");
        magasin.setRegistre("RC-001");
        magasin.setPhone("0102030405");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
    }

    private static OrderLine orderLine(String codeCip) {
        Produit produit = new Produit();
        produit.setId(500);
        produit.setLibelle("DOLIPRANE 1000MG");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setCodeCip(codeCip);
        fp.setProduit(produit);
        OrderLine line = new OrderLine();
        line.setId(900);
        line.setOrderDate(LocalDate.of(2026, 4, 18));
        line.setFournisseurProduit(fp);
        line.setQuantityRequested(1);
        line.setQuantityReceived(1);
        line.setOrderCostAmount(400);
        line.setOrderUnitPrice(800);
        return line;
    }

    private static Commande commande(List<OrderLine> lines) {
        Commande commande = new Commande();
        commande.setId(12);
        commande.setOrderDate(LocalDate.of(2026, 4, 18));
        commande.setReceiptReference("BL-20260418-001");
        commande.setOrderLines(new ArrayList<>(lines));
        return commande;
    }

    private static List<OrderLine> lignes(int count) {
        List<OrderLine> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(orderLine(String.format("%07d", i)));
        }
        return lines;
    }

    @Test
    @DisplayName("produit un PDF pour un bon tenant sur une seule page")
    void pdfSurUnePage() {
        byte[] pdf = service.export(commande(lignes(3)));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("renseigne les variables du gabarit")
    void variablesDuGabarit() {
        Commande commande = commande(lignes(3));

        service.export(commande);

        assertThat(service.getParameters())
            .containsEntry(Constant.MAGASIN, magasin)
            .containsEntry(Constant.ENTITY, commande)
            .containsEntry(Constant.ITEM_SIZE, 3)
            .containsEntry(Constant.DEVISE, Constant.DEVISE_CONSTANT)
            .containsEntry(Constant.IS_LAST_PAGE, true)
            .containsEntry(Constant.PAGE_COUNT, "1/1");
    }

    @Test
    @DisplayName("le pied de page reprend les mentions legales de l officine")
    void piedDePage() {
        service.export(commande(lignes(1)));

        assertThat((String) service.getParameters().get(Constant.FOOTER))
            .contains("RC N° RC-001")
            .contains("Tel: 0102030405");
    }

    @Test
    @DisplayName("trie les lignes par code CIP")
    void trieParCodeCip() {
        Commande commande = commande(List.of(orderLine("9999999"), orderLine("1111111"), orderLine("5555555")));

        service.export(commande);

        assertThat(commande.getOrderLines())
            .extracting(l -> l.getFournisseurProduit().getCodeCip())
            .containsExactly("1111111", "5555555", "9999999");
    }

    @Test
    @DisplayName("bascule en rendu multipage au-dela de la taille de page")
    void renduMultipage() {
        Commande commande = commande(lignes(Constant.COMMANDE_PAGE_SIZE + 10));

        byte[] pdf = service.export(commande);

        assertThat(pdf).isNotEmpty();
        assertThat(service.getParameters()).containsEntry(Constant.ITEM_SIZE, Constant.COMMANDE_PAGE_SIZE + 10);
        // la derniere page rendue porte le drapeau de fin
        assertThat(service.getParameters()).containsEntry(Constant.IS_LAST_PAGE, true);
        verify(templateEngine, atLeastOnce()).process(anyString(), any(Context.class));
    }

    @Test
    @DisplayName("la premiere page multipage n embarque que la taille de page")
    void premierePageTronquee() {
        Commande commande = commande(lignes(Constant.COMMANDE_PAGE_SIZE + 10));

        service.export(commande);

        @SuppressWarnings("unchecked")
        List<OrderLine> derniere = (List<OrderLine>) service.getParameters().get(Constant.ITEMS);
        assertThat(derniere).hasSize(10);
    }

    @Test
    @DisplayName("le nom de fichier genere reprend la reference du bon")
    void nomDeFichier() {
        service.export(commande(lignes(1)));

        assertThat(service.getGenerateFileName()).isEqualTo("BL-20260418-001");
    }

    @Test
    @DisplayName("la taille de page provient des constantes de rapport")
    void tailleDePage() {
        assertThat(service.getMaxiRowCount()).isEqualTo(Constant.COMMANDE_PAGE_SIZE);
    }

    @Test
    @DisplayName("les lignes exposees sont celles du bon")
    void lignesExposees() {
        Commande commande = commande(lignes(2));

        service.export(commande);

        assertThat(service.getItems()).isSameAs(commande.getOrderLines());
    }

    @Test
    @DisplayName("le rendu sans contexte explicite passe par les variables communes")
    void renduSansContexte() {
        service.export(commande(lignes(1)));

        assertThat(service.getTemplateAsHtml()).isEqualTo(HTML);
    }

    @Test
    @DisplayName("le rendu avec contexte y recopie les parametres")
    void renduAvecContexte() {
        service.export(commande(lignes(1)));
        Context context = new Context();

        assertThat(service.getTemplateAsHtml(context)).isEqualTo(HTML);
        assertThat(context.getVariable(Constant.MAGASIN)).isSameAs(magasin);
    }

    @Test
    @DisplayName("un bon sans ligne reste rendu sur une page")
    void bonSansLigne() {
        byte[] pdf = service.export(commande(List.of()));

        assertThat(pdf).isNotEmpty();
        assertThat(service.getParameters()).containsEntry(Constant.ITEM_SIZE, 0);
    }
}
