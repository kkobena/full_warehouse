package com.kobe.warehouse.service.reglement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.DifferePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.NewDifferePaymentDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereResponse;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * {@link com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService} sur un vrai
 * PostgreSQL.
 *
 * <p>Le différé, c'est le crédit accordé au comptoir : la vente est close mais garde un reste à
 * payer, et le client vient l'apurer plus tard, en une ou plusieurs fois. Tout ce que fait ce
 * service se lit dans des agrégats SQL — solde par client, total encaissé sur une période — ou dans
 * une imputation qui répartit un versement sur plusieurs ventes. Ni l'un ni l'autre ne survit à une
 * base simulée.
 */
@DisplayName("ReglementDiffereService — règlement des ventes à crédit sur PostgreSQL")
class ReglementDiffereServiceIntegrationTest extends AbstractReglementIntegrationTest {

    @Test
    @DisplayName("Un versement couvrant tout le crédit solde chaque vente")
    void reglementTotal() {
        UninsuredCustomer client = clientDiffere("KOUAME", "Ali");
        CashSale premiere = venteDiffere(client, 12_000, 0);
        CashSale seconde = venteDiffere(client, 8_000, 0);
        viderLeCache();

        ReglementDiffereResponse reponse = services.reglementDiffereService.doReglement(
            versement(client, Set.of(premiere.getId().getId(), seconde.getId().getId()), 20_000, 20_000)
        );
        viderLeCache();

        assertEquals(PaymentStatus.PAYE, em.find(Sales.class, premiere.getId()).getPaymentStatus());
        assertEquals(0, em.find(Sales.class, premiere.getId()).getRestToPay());
        assertEquals(PaymentStatus.PAYE, em.find(Sales.class, seconde.getId()).getPaymentStatus());

        DifferePayment paiement = em.find(DifferePayment.class, reponse.idReglement());
        assertEquals(20_000, paiement.getPaidAmount());
        assertEquals(20_000, paiement.getMontantVerse());
        assertNotNull(paiement.getTransactionNumber());
        assertEquals(
            2,
            compter("SELECT count(*) FROM differe_payment_item WHERE differe_payment_id = " + reponse.idReglement().getId()),
            "un élément par vente apurée"
        );
    }

    @Test
    @DisplayName("Un versement partiel laisse la vente impayée avec son reliquat")
    void reglementPartiel() {
        UninsuredCustomer client = clientDiffere("BAMBA", "Fanta");
        CashSale vente = venteDiffere(client, 30_000, 0);
        viderLeCache();

        services.reglementDiffereService.doReglement(versement(client, Set.of(vente.getId().getId()), 30_000, 18_000));
        viderLeCache();

        Sales relue = em.find(Sales.class, vente.getId());
        assertEquals(PaymentStatus.IMPAYE, relue.getPaymentStatus());
        assertEquals(12_000, relue.getRestToPay(), "30 000 dus, 18 000 versés");
    }

    @Test
    @DisplayName("Un second versement apure le reliquat laissé par le premier")
    void reglementEnDeuxFois() {
        UninsuredCustomer client = clientDiffere("DIALLO", "Moussa");
        CashSale vente = venteDiffere(client, 25_000, 0);
        viderLeCache();
        services.reglementDiffereService.doReglement(versement(client, Set.of(vente.getId().getId()), 25_000, 10_000));
        viderLeCache();

        services.reglementDiffereService.doReglement(versement(client, Set.of(vente.getId().getId()), 15_000, 15_000));
        viderLeCache();

        Sales relue = em.find(Sales.class, vente.getId());
        assertEquals(PaymentStatus.PAYE, relue.getPaymentStatus());
        assertEquals(0, relue.getRestToPay());
        assertEquals(2, compter("SELECT count(*) FROM payment_transaction WHERE dtype = 'DifferePayment'"));
    }

    @Test
    @DisplayName("Le solde d'un client agrège ses ventes impayées")
    void soldeDUnClient() {
        UninsuredCustomer client = clientDiffere("TRAORE", "Awa");
        venteDiffere(client, 15_000, 5_000);
        venteDiffere(client, 10_000, 0);
        viderLeCache();

        DiffereSummary resume = services.reglementDiffereService.getDiffereSummary(client.getId(), Set.of(PaymentStatus.IMPAYE));

        assertEquals(25_000L, resume.saleAmount());
        assertEquals(5_000L, resume.paidAmount());
        assertEquals(20_000L, resume.rest());
    }

    @Test
    @DisplayName("Le différé d'un client rend ses ventes impayées, ligne à ligne")
    void differeDUnClient() {
        UninsuredCustomer client = clientDiffere("SANOGO", "Ibrahim");
        venteDiffere(client, 9_000, 0);
        venteDiffere(client, 6_000, 0);
        viderLeCache();

        List<DiffereDTO> differes = services.reglementDiffereService
            .getDiffere(client.getId(), Set.of(PaymentStatus.IMPAYE), Pageable.unpaged())
            .getContent();

        assertEquals(1, differes.size(), "un regroupement par client");
        DiffereDTO differe = differes.getFirst();
        assertEquals(15_000L, differe.saleAmount());
        assertEquals(15_000L, differe.rest());
        assertEquals(2, differe.differeItems().size());
        assertEquals(differe.rest(), services.reglementDiffereService.getOne(client.getId()).orElseThrow().rest());
    }

    @Test
    @DisplayName("Les ventes soldées sortent du différé")
    void ventesSoldeesExclues() {
        UninsuredCustomer client = clientDiffere("KONE", "Salif");
        CashSale impayee = venteDiffere(client, 7_000, 0);
        venteDiffere(client, 4_000, 4_000);
        viderLeCache();

        List<DiffereItem> lignes = services.reglementDiffereService
            .getDiffereItems(client.getId(), null, null, null, Set.of(PaymentStatus.IMPAYE), Pageable.unpaged())
            .getContent();

        assertEquals(1, lignes.size());
        assertEquals(impayee.getId().getId(), lignes.getFirst().saleId());
        assertEquals(7_000, lignes.getFirst().restAmount());
    }

    @Test
    @DisplayName("Les clients à crédit se listent depuis les ventes différées closes")
    void listeDesClientsACredit() {
        UninsuredCustomer client = clientDiffere("YAO", "Adjoua");
        venteDiffere(client, 5_000, 0);
        viderLeCache();

        var clients = services.reglementDiffereService.getClientDiffere().getContent();

        assertEquals(1, clients.size());
        assertEquals(client.getId().longValue(), clients.getFirst().getId());
        assertEquals("Adjoua", clients.getFirst().getFirsName());
    }

    @Test
    @DisplayName("Le reçu d'un règlement porte le caissier, le client et le solde restant")
    void recuDeReglement() {
        UninsuredCustomer client = clientDiffere("OUATTARA", "Hamed");
        CashSale premiere = venteDiffere(client, 20_000, 0);
        venteDiffere(client, 6_000, 0);
        viderLeCache();
        ReglementDiffereResponse reponse = services.reglementDiffereService.doReglement(
            versement(client, Set.of(premiere.getId().getId()), 20_000, 20_000)
        );
        viderLeCache();

        ReglementDiffereReceiptDTO recu = services.reglementDiffereService.getReglementDiffereReceipt(reponse.idReglement());

        assertEquals("Hamed", recu.firstName());
        assertEquals(20_000, recu.paidAmount());
        assertEquals(20_000, recu.montantVerse());
        assertEquals(ModePaimentCode.CASH.name(), recu.mode());
        assertEquals(6_000, recu.solde(), "la seconde vente reste due");
        assertNotNull(recu.reference());
    }

    @Test
    @DisplayName("Les règlements d'une période se regroupent par client avec leur solde")
    void reglementsDUnePeriode() {
        UninsuredCustomer client = clientDiffere("COULIBALY", "Nadia");
        CashSale premiere = venteDiffere(client, 14_000, 0);
        venteDiffere(client, 11_000, 0);
        viderLeCache();
        services.reglementDiffereService.doReglement(versement(client, Set.of(premiere.getId().getId()), 14_000, 14_000));
        viderLeCache();

        List<ReglementDiffereWrapperDTO> reglements = services.reglementDiffereService
            .getReglementsDifferes(client.getId(), LocalDate.now(), LocalDate.now(), Pageable.unpaged())
            .getContent();
        DifferePaymentSummaryDTO resume = services.reglementDiffereService.getDifferePaymentSummary(
            client.getId(),
            LocalDate.now(),
            LocalDate.now()
        );

        assertEquals(1, reglements.size());
        assertEquals(14_000L, reglements.getFirst().paidAmount());
        assertEquals(11_000L, reglements.getFirst().solde(), "il reste la seconde vente à payer");
        assertEquals(1, reglements.getFirst().items().size());
        assertEquals(14_000L, resume.paidAmount());
    }

    @Test
    @DisplayName("L'export Excel du différé rend un classeur non vide")
    void exportExcel() {
        UninsuredCustomer client = clientDiffere("ZOUMANA", "Cissé");
        venteDiffere(client, 13_000, 0);
        viderLeCache();

        byte[] classeur = services.reglementDiffereService.exportDifferesToExcel(client.getId(), Set.of(PaymentStatus.IMPAYE));

        assertTrue(classeur.length > 0);
    }

    private NewDifferePaymentDTO versement(UninsuredCustomer client, Set<Long> ventes, int attendu, int verse) {
        return new NewDifferePaymentDTO(client.getId(), ventes, attendu, verse, ModePaimentCode.CASH, LocalDate.now(), null);
    }
}
