package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.SimplifiedSaleService} sur un vrai PostgreSQL.
 *
 * <p>C'est la vente du mobile : elle arrive complète et se clôture d'un seul appel — lignes,
 * montants, TVA embarquée, débit du stock et règlement dans la même transaction. Là où le comptoir
 * construit la vente en plusieurs requêtes, celle-ci n'a droit qu'à une, ce qui rend l'ordre des
 * écritures et l'intégrité référentielle bien plus faciles à casser.
 */
@DisplayName("SimplifiedSaleService — vente mobile en un appel sur PostgreSQL")
class SimplifiedSaleServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("La vente arrive complète et ressort clôturée, stock débité")
    void venteEnUnAppel() {
        Produit produit = produitEnStock("MOBILE A", 1_500, 900, 0, 40);

        FinalyseSaleDTO finalisee = services.simplifiedSaleService.createCashSale(venteDe(3_000, ligneDe(produit, 2)));
        viderLeCache();

        assertTrue(finalisee.success());
        CashSale vente = em.find(CashSale.class, finalisee.saleId());
        assertNotNull(vente);
        assertEquals(SalesStatut.CLOSED, vente.getStatut());
        assertNotNull(vente.getNumberTransaction());
        assertEquals(3_000, vente.getSalesAmount());
        assertEquals(38, stockRayon(produit), "40 − 2 vendues");
        assertEquals(1, compter("SELECT count(*) FROM payment_transaction WHERE sale_id = " + finalisee.saleId().getId()));
    }

    @Test
    @DisplayName("Plusieurs lignes sont écrites d'un coup, avec la TVA embarquée")
    void plusieursLignes() {
        Produit exonere = produitEnStock("MOBILE EXO", 2_000, 1_200, 0, 40);
        Produit taxe = produitEnStock("MOBILE TVA", 5_000, 3_000, 18, 40);

        FinalyseSaleDTO finalisee = services.simplifiedSaleService.createCashSale(
            venteDe(9_000, ligneDe(exonere, 2), ligneDe(taxe, 1))
        );
        viderLeCache();

        CashSale vente = em.find(CashSale.class, finalisee.saleId());
        assertEquals(2, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + finalisee.saleId().getId()));
        assertEquals(9_000, vente.getSalesAmount());
        assertNotNull(vente.getTvaEmbeded(), "le détail de TVA est figé sur la vente");
        assertTrue(vente.getTvaEmbeded().contains("18"), vente.getTvaEmbeded());
    }

    @Test
    @DisplayName("Le client rattaché à la vente est retrouvé en base")
    void venteAvecClient() {
        Produit produit = produitEnStock("MOBILE CLIENT", 1_000, 600, 0, 40);
        UninsuredCustomer client = client("KOUAME", "Yao", "CLI-MOB-1");

        CashSaleDTO dto = venteDe(1_000, ligneDe(produit, 1));
        dto.setCustomerId(client.getId());
        FinalyseSaleDTO finalisee = services.simplifiedSaleService.createCashSale(dto);
        viderLeCache();

        assertEquals(client.getId(), em.find(CashSale.class, finalisee.saleId()).getCustomer().getId());
    }

    @Test
    @DisplayName("La liste du mobile ne rend que les ventes du jour, clôturées, du caissier")
    void listeDuJour() {
        Produit produit = produitEnStock("MOBILE LISTE", 1_000, 600, 0, 40);
        FinalyseSaleDTO premiere = services.simplifiedSaleService.createCashSale(venteDe(1_000, ligneDe(produit, 1)));
        services.simplifiedSaleService.createCashSale(venteDe(2_000, ligneDe(produit, 2)));
        venteFermee(produit, 1, 1, java.time.LocalDate.now().minusDays(3));
        viderLeCache();

        var liste = services.simplifiedSaleService.getList(null);

        assertEquals(2, liste.getContent().size(), "la vente d'avant-hier reste dehors");
        assertTrue(liste.getContent().stream().allMatch(v -> v.getStatut() == SalesStatut.CLOSED));

        var parReference = services.simplifiedSaleService.getList(
            em.find(CashSale.class, premiere.saleId()).getNumberTransaction()
        );
        assertEquals(1, parReference.getContent().size(), "la recherche porte sur la référence de vente");
    }

    // ===== outils =====

    private int stockRayon(Produit produit) {
        return services.stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), STORAGE_RAYON_ID).getQtyStock();
    }

    private CashSaleDTO venteDe(int montant, SaleLineDTO... lignes) {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setNatureVente(NatureVente.COMPTANT);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        dto.setSalesLines(List.of(lignes));
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

    private SaleLineDTO ligneDe(Produit produit, int quantite) {
        SaleLineDTO ligne = new SaleLineDTO();
        ligne.setProduitId(produit.getId());
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        return ligne;
    }
}
