package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.SaleService} sur un vrai PostgreSQL.
 *
 * <p>Le service ne calcule presque rien seul : il compose des écritures sur des tables partitionnées
 * par date, avec un identifiant composite {@code (id, sale_date)}, une contrainte d'unicité sur
 * {@code (produit_id, sales_id, sale_date)} et un stock modifié en marge de la vente. Un double
 * mémoire des repositories accepterait tout cela sans broncher ; Postgres, non.
 */
@DisplayName("SaleService — vente comptant sur PostgreSQL")
class SaleServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("La vente créée et sa ligne sont écrites en base, montants calculés")
    void creationVenteComptant() {
        Produit produit = produitEnStock("DOLIPRANE 1000", 1_000, 600, 0, 50);

        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 2));
        viderLeCache();

        CashSale vente = em.find(CashSale.class, cree.getSaleId());
        assertNotNull(vente, "la vente est bien en base, pas seulement en mémoire");
        assertEquals(2_000, vente.getSalesAmount());
        assertEquals(2_000, vente.getNetAmount());
        assertEquals(1_200, vente.getCostAmount(), "2 × 600 de prix d'achat");
        assertEquals(SalesStatut.ACTIVE, vente.getStatut());
        assertEquals(1, vente.getSalesLines().size());
        assertEquals(
            1,
            compter("SELECT count(*) FROM sales_line WHERE sales_id = " + cree.getSaleId().getId()),
            "la ligne est rattachée à la vente par le couple (sales_id, sales_sale_date)"
        );
    }

    @Test
    @DisplayName("Le stock n'est débité qu'à la clôture, pas à la saisie de la ligne")
    void leStockNestDebiteQuALaCloture() {
        Produit produit = produitEnStock("EFFERALGAN", 500, 300, 0, 20);

        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 3));
        viderLeCache();
        assertEquals(20, stockRayon(produit), "la saisie ne touche pas encore au stock");

        cloturer(cree.getSaleId(), 1_500);
        viderLeCache();

        assertEquals(17, stockRayon(produit), "le débit a lieu au moment de la clôture");
    }

    @Test
    @DisplayName("Ajouter deux fois le même produit met à jour la ligne au lieu de la dupliquer")
    void memeProduitDeuxFois() {
        Produit produit = produitEnStock("AMOXICILLINE", 800, 500, 0, 40);

        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 1));
        SaleLineDTO ajout = ligneDe(produit, 2);
        ajout.setSaleCompositeId(cree.getSaleId());
        services.saleService.addOrUpdateSaleLine(ajout);
        viderLeCache();

        assertEquals(
            1,
            compter("SELECT count(*) FROM sales_line WHERE sales_id = " + cree.getSaleId().getId()),
            "la contrainte d'unicité (produit_id, sales_id, sale_date) interdit le doublon"
        );
        CashSale vente = em.find(CashSale.class, cree.getSaleId());
        assertEquals(3, vente.getSalesLines().iterator().next().getQuantityRequested());
        assertEquals(2_400, vente.getSalesAmount(), "3 × 800, recalculé depuis les lignes");
    }

    @Test
    @DisplayName("Un second produit s'ajoute et la vente totalise les deux lignes")
    void deuxProduitsDifferents() {
        Produit premier = produitEnStock("VITAMINE C", 1_000, 600, 0, 30);
        Produit second = produitEnStock("SIROP TOUX", 2_500, 1_500, 18, 30);

        CashSaleDTO cree = services.saleService.createCashSale(venteDe(premier, 2));
        SaleLineDTO ajout = ligneDe(second, 1);
        ajout.setSaleCompositeId(cree.getSaleId());
        services.saleService.addOrUpdateSaleLine(ajout);
        viderLeCache();

        CashSale vente = em.find(CashSale.class, cree.getSaleId());
        assertEquals(2, vente.getSalesLines().size());
        assertEquals(4_500, vente.getSalesAmount());
        // 2 000 exonérés + 2 500 à 18 % : seul le second porte de la TVA.
        assertEquals(381, vente.getTaxAmount(), "2 500 − ceil(2 500 / 1,18) = 2 500 − 2 119");
    }

    @Test
    @DisplayName("La clôture ferme la vente, lui donne sa référence et enregistre le règlement")
    void clotureDeLaVente() {
        Produit produit = produitEnStock("SPASFON", 1_500, 900, 0, 10);
        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 2));

        services.saleService.save(clotureDe(cree.getSaleId(), 3_000));
        viderLeCache();

        CashSale vente = em.find(CashSale.class, cree.getSaleId());
        assertEquals(SalesStatut.CLOSED, vente.getStatut());
        assertNotNull(vente.getNumberTransaction());
        assertEquals(0, vente.getRestToPay());
        assertEquals(
            1,
            compter("SELECT count(*) FROM payment_transaction WHERE sale_id = " + cree.getSaleId().getId()),
            "le règlement espèces est écrit dans la table partitionnée des transactions"
        );
    }

    @Test
    @DisplayName("Une vente non différée réglée en deçà du dû est refusée")
    void reglementInsuffisant() {
        Produit produit = produitEnStock("ASPIRINE", 1_000, 600, 0, 10);
        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 2));

        CashSaleDTO cloture = clotureDe(cree.getSaleId(), 2_000);
        cloture.setPayrollAmount(500);

        assertThrows(PaymentAmountException.class, () -> services.saleService.save(cloture));
    }

    /**
     * L'annulation est réversible par construction : elle n'efface rien, elle écrit une vente
     * miroir aux montants négatifs qui neutralise la première, et rend le stock. La contrainte
     * {@code sales_line_declarable_ck} refusait ces lignes négatives jusqu'à la migration V1.9.3 ;
     * ce test la garde honnête.
     */
    @Test
    @DisplayName("L'annulation écrit une contrepassation négative et rend le stock")
    void annulationDUneVente() {
        Produit produit = produitEnStock("IBUPROFENE", 1_000, 700, 0, 25);
        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 4));
        cloturer(cree.getSaleId(), 4_000);
        viderLeCache();
        assertEquals(21, stockRayon(produit));

        services.saleService.cancelCashSale(cree.getSaleId(), "erreur de saisie");
        viderLeCache();

        CashSale originale = em.find(CashSale.class, cree.getSaleId());
        assertTrue(originale.isCanceled());
        assertEquals("erreur de saisie", originale.getCancelComment());
        assertEquals(
            1,
            compter("SELECT count(*) FROM sales WHERE canceled_sale_id = " + cree.getSaleId().getId()),
            "la copie pointe vers la vente qu'elle annule"
        );
        assertEquals(
            -4_000,
            compter("SELECT sales_amount FROM sales WHERE canceled_sale_id = " + cree.getSaleId().getId()),
            "la contrepassation porte le montant en négatif"
        );
        assertEquals(
            -4_000,
            compter(
                "SELECT amount_to_be_taken_into_account FROM sales_line" +
                " WHERE sales_id = (SELECT id FROM sales WHERE canceled_sale_id = " + cree.getSaleId().getId() + ")"
            ),
            "le montant déclarable est négatif lui aussi : c'est ce qui annule le chiffre d'affaires"
        );
        assertEquals(25, stockRayon(produit), "le stock revient à son niveau d'avant la vente");
    }

    @Test
    @DisplayName("Supprimer une ligne retire son montant de la vente et la ligne de la base")
    void suppressionDeLigne() {
        Produit premier = produitEnStock("PARACETAMOL", 1_000, 600, 0, 30);
        Produit second = produitEnStock("MAGNESIUM", 2_000, 1_200, 0, 30);
        CashSaleDTO cree = services.saleService.createCashSale(venteDe(premier, 1));
        SaleLineDTO ajout = ligneDe(second, 1);
        ajout.setSaleCompositeId(cree.getSaleId());
        SaleLineDTO ligneAjoutee = services.saleService.addOrUpdateSaleLine(ajout);

        services.saleService.deleteSaleLineById(new SaleLineId(ligneAjoutee.getId(), cree.getSaleId().getSaleDate()));
        viderLeCache();

        assertEquals(1, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + cree.getSaleId().getId()));
        assertEquals(1_000, em.find(CashSale.class, cree.getSaleId()).getSalesAmount());
    }

    @Test
    @DisplayName("Une remise client s'applique aux montants puis se retire proprement")
    void remiseClient() {
        Produit produit = produitEnStock("CREME SOLAIRE", 10_000, 6_000, 0, 10);
        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 1));
        RemiseClient remise = remiseClientDe(0.10f);

        services.saleService.processDiscount(new UpdateSaleInfo(cree.getSaleId(), remise.getId()));
        viderLeCache();

        CashSale avecRemise = em.find(CashSale.class, cree.getSaleId());
        assertEquals(1_000, avecRemise.getDiscountAmount(), "10 % de 10 000");
        assertEquals(9_000, avecRemise.getNetAmount());
        assertNotNull(avecRemise.getRemise());

        services.saleService.removeRemiseFromCashSale(cree.getSaleId());
        viderLeCache();

        CashSale sansRemise = em.find(CashSale.class, cree.getSaleId());
        assertEquals(0, sansRemise.getDiscountAmount());
        assertEquals(10_000, sansRemise.getNetAmount());
        assertNull(sansRemise.getRemise());
    }

    @Test
    @DisplayName("Mettre en attente une vente sans ligne la supprime de la base")
    void miseEnAttenteDUneVenteVide() {
        Produit produit = produitEnStock("COMPRESSES", 300, 150, 0, 10);
        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 1));
        // Comme en production, la suppression arrive dans une requête distincte : la ligne est
        // déjà en base quand on la retire.
        viderLeCache();
        SalesLine ligne = em.find(CashSale.class, cree.getSaleId()).getSalesLines().iterator().next();
        services.saleService.deleteSaleLineById(ligne.getId());

        CashSaleDTO attente = new CashSaleDTO();
        attente.setSaleId(cree.getSaleId());
        services.saleService.putCashSaleOnHold(attente);
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM sales WHERE id = " + cree.getSaleId().getId()));
    }

    @Test
    @DisplayName("Le client rattaché à la vente est relu depuis la base")
    void rattachementDuClient() {
        Produit produit = produitEnStock("PANSEMENTS", 400, 200, 0, 10);
        CashSaleDTO cree = services.saleService.createCashSale(venteDe(produit, 1));
        UninsuredCustomer client = client("KOUASSI", "Ama", "CLI-INT-1");

        services.saleService.setCustomer(new UpdateSaleInfo(cree.getSaleId(), client.getId()));
        viderLeCache();

        assertEquals(client.getId(), em.find(CashSale.class, cree.getSaleId()).getCustomer().getId());

        services.saleService.removeCustomer(cree.getSaleId());
        viderLeCache();
        assertNull(em.find(CashSale.class, cree.getSaleId()).getCustomer());
    }

    // ===== outils =====

    private int stockRayon(Produit produit) {
        StockProduit stock = services.stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), STORAGE_RAYON_ID);
        return stock.getQtyStock() + stock.getQtyUG();
    }

    private RemiseClient remiseClientDe(float taux) {
        RemiseClient remise = new RemiseClient();
        remise.setTauxRemise(taux);
        remise.setRemiseValue(taux * 100);
        remise.setValeur(String.valueOf((int) (taux * 100)));
        em.persist(remise);
        em.flush();
        return remise;
    }

    private void cloturer(SaleId saleId, int montant) {
        services.saleService.save(clotureDe(saleId, montant));
    }

    private CashSaleDTO clotureDe(SaleId saleId, int montant) {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(saleId);
        dto.setPayrollAmount(montant);
        dto.setAmountToBePaid(montant);
        dto.setRestToPay(0);
        dto.setMontantRendu(0);
        PaymentDTO reglement = new PaymentDTO();
        reglement.setPaidAmount(montant);
        reglement.setNetAmount(montant);
        reglement.setPaymentMode(new PaymentModeDTO().setCode("CASH"));
        dto.setPayments(List.of(reglement));
        return dto;
    }

    private CashSaleDTO venteDe(Produit produit, int quantite) {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setNatureVente(NatureVente.COMPTANT);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        dto.setSalesLines(List.of(ligneDe(produit, quantite)));
        return dto;
    }

    private SaleLineDTO ligneDe(Produit produit, int quantite) {
        SaleLineDTO ligne = new SaleLineDTO();
        ligne.setProduitId(produit.getId());
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        return ligne;
    }

}
