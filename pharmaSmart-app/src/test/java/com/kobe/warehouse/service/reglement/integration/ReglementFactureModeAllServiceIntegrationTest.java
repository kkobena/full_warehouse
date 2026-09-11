package com.kobe.warehouse.service.reglement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.reglement.dto.BanqueInfoDTO;
import com.kobe.warehouse.service.reglement.dto.ModeEditionReglement;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.reglement.service.ReglementFactureModeAllService} sur un vrai
 * PostgreSQL.
 *
 * <p>Régler une facture en totalité, c'est écrire au même instant dans quatre tables : la facture
 * (montant réglé, statut), chacun de ses dossiers ({@code third_party_sale_line}), la transaction
 * de paiement ({@code payment_transaction}, partitionnée par date) et son détail
 * ({@code invoice_payment_item}). Rien de tout cela ne se vérifie sans base : les identifiants sont
 * composites, les montants sont des reliquats calculés à partir de ce qui est déjà en base, et le
 * paiement se range dans la partition de sa date de règlement.
 */
@DisplayName("ReglementFactureModeAllService — règlement total d'une facture sur PostgreSQL")
class ReglementFactureModeAllServiceIntegrationTest extends AbstractReglementIntegrationTest {

    @Test
    @DisplayName("Le règlement total solde la facture, ses dossiers, et écrit un paiement par dossier")
    void reglementTotal() throws Exception {
        TiersPayant organisme = tiersPayant("CNAM");
        ClientTiersPayant compte = compte(organisme);
        List<ThirdPartySaleLine> dossiers = List.of(dossier(compte, 40_000), dossier(compte, 60_000));
        FactureTiersPayant facture = facture(organisme, dossiers);
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureModeAllService.doReglement(parametre(facture, 100_000));
        viderLeCache();

        assertTrue(reponse.total(), "la facture est soldée");
        FactureTiersPayant relue = em.find(FactureTiersPayant.class, facture.getId());
        assertEquals(100_000, relue.getMontantRegle());
        assertEquals(InvoiceStatut.PAID, relue.getStatut());
        assertTrue(
            relue.getFacturesDetails().stream().allMatch(d -> d.getStatut() == ThirdPartySaleStatut.PAID),
            "chaque dossier est marqué payé"
        );
        assertEquals(
            2,
            compter("SELECT count(*) FROM invoice_payment_item WHERE invoice_payment_id = " + reponse.id().getId()),
            "un détail par dossier réglé"
        );
    }

    @Test
    @DisplayName("Le paiement porte le montant versé, un numéro de transaction et son type")
    void paiementEcrit() throws Exception {
        TiersPayant organisme = tiersPayant("MUGEF");
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte(organisme), 25_000)));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureModeAllService.doReglement(
            parametre(facture, 25_000).setComment("règlement du mois")
        );
        viderLeCache();

        InvoicePayment paiement = em.find(InvoicePayment.class, reponse.id());
        assertEquals(25_000, paiement.getMontantVerse());
        assertEquals(25_000, paiement.getPaidAmount());
        assertEquals(25_000, paiement.getReelAmount());
        assertEquals(25_000, paiement.getExpectedAmount());
        assertEquals("règlement du mois", paiement.getCommentaire());
        assertEquals(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT, paiement.getTypeFinancialTransaction());
        assertNotNull(paiement.getTransactionNumber(), "le numéro vient de la table des références");
        assertEquals(caisse.getId(), paiement.getCashRegister().getId());
    }

    @Test
    @DisplayName("Sur une facture déjà entamée, seul le reliquat de chaque dossier est réglé")
    void reglementDuReliquat() throws Exception {
        TiersPayant organisme = tiersPayant("CNPS");
        ClientTiersPayant compte = compte(organisme);
        ThirdPartySaleLine dossier = dossier(compte, 50_000);
        dossier.setMontantRegle(20_000);
        dossier.setStatut(ThirdPartySaleStatut.HALF_PAID);
        FactureTiersPayant facture = facture(organisme, List.of(dossier));
        facture.setMontantRegle(20_000);
        facture.setStatut(InvoiceStatut.PARTIALLY_PAID);
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureModeAllService.doReglement(parametre(facture, 30_000));
        viderLeCache();

        FactureTiersPayant relue = em.find(FactureTiersPayant.class, facture.getId());
        assertEquals(50_000, relue.getMontantRegle(), "20 000 déjà réglés + 30 000 de reliquat");
        assertEquals(InvoiceStatut.PAID, relue.getStatut());
        assertEquals(50_000, relue.getFacturesDetails().getFirst().getMontantRegle());

        InvoicePayment paiement = em.find(InvoicePayment.class, reponse.id());
        assertEquals(30_000, paiement.getPaidAmount(), "seul le reliquat est encaissé");
    }

    @Test
    @DisplayName("Quand le montant facturé dépasse ce qui a été réglé, la facture reste partiellement payée")
    void factureIncompletementReglee() throws Exception {
        TiersPayant organisme = tiersPayant("MUTUELLE");
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte(organisme), 10_000)));
        viderLeCache();

        // Le montant facturé annoncé (15 000) excède la somme des dossiers rattachés (10 000) :
        // c'est le cas d'une facture dont tous les dossiers n'ont pas encore été rapatriés.
        ResponseReglementDTO reponse = services.reglementFactureModeAllService.doReglement(
            parametre(facture, 10_000).setMontantFacture(15_000)
        );
        viderLeCache();

        assertEquals(InvoiceStatut.PARTIALLY_PAID, em.find(FactureTiersPayant.class, facture.getId()).getStatut());
        assertFalse(reponse.total());
    }

    @Test
    @DisplayName("La banque saisie au règlement est créée et rattachée au paiement")
    void banqueEnregistree() throws Exception {
        TiersPayant organisme = tiersPayant("ASSUR CHEQUE");
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte(organisme), 12_000)));
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureModeAllService.doReglement(
            parametre(facture, 12_000)
                .setModePaimentCode(ModePaimentCode.CH)
                .setBanqueInfo(new BanqueInfoDTO().setNom("SGBCI").setCode("SG").setBeneficiaire("PHARMACIE"))
        );
        viderLeCache();

        InvoicePayment paiement = em.find(InvoicePayment.class, reponse.id());
        assertNotNull(paiement.getBanque(), "la banque est persistée avant le paiement");
        assertEquals("SGBCI", paiement.getBanque().getNom());
        assertEquals("CH", paiement.getPaymentMode().getCode());
    }

    @Test
    @DisplayName("Un règlement antidaté se range dans la partition de sa date de transaction")
    void reglementAntidate() throws Exception {
        TiersPayant organisme = tiersPayant("ANTIDATE");
        FactureTiersPayant facture = facture(organisme, List.of(dossier(compte(organisme), 8_000)));
        LocalDate veille = LocalDate.now().minusDays(1);
        viderLeCache();

        ResponseReglementDTO reponse = services.reglementFactureModeAllService.doReglement(
            parametre(facture, 8_000).setPaymentDate(veille)
        );
        viderLeCache();

        assertEquals(veille, reponse.id().getTransactionDate());
        assertEquals(
            1,
            compter(
                "SELECT count(*) FROM payment_transaction WHERE id = " +
                reponse.id().getId() +
                " AND transaction_date = '" +
                veille +
                "'"
            )
        );
    }

    private ReglementParam parametre(FactureTiersPayant facture, int montant) {
        return new ReglementParam()
            .setId(facture.getId())
            .setMode(ModeEditionReglement.FACTURE_TOTAL)
            .setModePaimentCode(ModePaimentCode.CASH)
            .setAmount(montant)
            .setTotalAmount(montant)
            .setMontantFacture(montant);
    }
}
