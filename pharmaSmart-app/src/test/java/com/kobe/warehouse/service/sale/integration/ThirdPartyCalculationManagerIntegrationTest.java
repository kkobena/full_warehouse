package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.ThirdPartyCalculationManager} sur un vrai PostgreSQL.
 *
 * <p>Le calcul lui-même est en mémoire et déjà couvert par ses tests unitaires. Ce qui se vérifie
 * ici, c'est ce qu'il en reste une fois écrit : les parts sur la vente, le montant et le taux
 * retenu sur chaque ligne tiers-payant, et la répartition par taux de TVA rangée en {@code jsonb}.
 * Le plafond de consommation, lui, est lu sur des entités persistées : c'est la base qui décide si
 * l'avertissement doit sortir.
 */
@DisplayName("ThirdPartyCalculationManager — répartition écrite en base sur PostgreSQL")
class ThirdPartyCalculationManagerIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Le recalcul écrit les parts sur la vente et le montant sur la ligne tiers-payant")
    void recalculApresChangementDeLigne() {
        Produit produit = produitEnStock("CALC A", 10_000, 6_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM CALC", 70, PrioriteTiersPayant.R0);
        ThirdPartySales vente = venteAssurance(compte, produit, 1, "BON-CALC-1");

        SalesLine ligne = vente.getSalesLines().iterator().next();
        ligne.setQuantityRequested(3);
        ligne.setSalesAmount(30_000);
        String avertissement = services.thirdPartyCalculationManager.computeThirdPartySaleAmounts(vente);
        viderLeCache();

        assertTrue(avertissement == null || avertissement.isBlank(), "aucun plafond n'est posé sur ce compte");
        ThirdPartySales relue = em.find(ThirdPartySales.class, vente.getId());
        assertEquals(30_000, relue.getSalesAmount());
        assertEquals(21_000, relue.getPartTiersPayant(), "70 % de 30 000");
        assertEquals(9_000, relue.getPartAssure());
        assertEquals(
            21_000,
            compter("SELECT montant FROM third_party_sale_line WHERE sale_id = " + vente.getId().getId()),
            "la ligne tiers-payant porte le même montant que la part de la vente"
        );
    }

    @Test
    @DisplayName("La répartition par taux de TVA est rangée en jsonb sur la ligne tiers-payant")
    void repartitionParTva() {
        Produit taxe = produitEnStock("CALC TVA", 11_800, 7_000, 18, 50);
        ClientTiersPayant compte = compte("CNAM TVA", 80, PrioriteTiersPayant.R0);
        venteAssurance(compte, taxe, 1, "BON-CALC-2");
        viderLeCache();

        Object repartitions = em
            .createNativeQuery("SELECT repartitions FROM third_party_sale_line WHERE num_bon = 'BON-CALC-2'")
            .getSingleResult();

        assertNotNull(repartitions, "la ventilation par taux est conservée pour la facturation");
        assertTrue(repartitions.toString().contains("18"), repartitions.toString());
    }

    @Test
    @DisplayName("Deux tiers-payants se partagent la prise en charge selon leur priorité")
    void deuxTiersPayants() {
        Produit produit = produitEnStock("CALC DUO", 20_000, 12_000, 0, 50);
        AssuredCustomer assure = assure("OUATTARA", "Sekou", "ASS-CALC-1");
        ClientTiersPayant principal = compte(assure, "CNAM DUO", 60, PrioriteTiersPayant.R0);
        ClientTiersPayant complementaire = compte(assure, "MUTUELLE DUO", 25, PrioriteTiersPayant.R1);
        ThirdPartySaleDTO vente = services.thirdPartySaleService.createSale(venteDe(principal, produit, "BON-CALC-3"));
        services.thirdPartySaleService.addThirdPartySaleLineToSales(compteDto(complementaire, "BON-CALC-4"), vente.getSaleId());
        viderLeCache();

        ThirdPartySales relue = em.find(ThirdPartySales.class, vente.getSaleId());
        assertEquals(17_000, relue.getPartTiersPayant(), "60 % + 25 % de 20 000");
        assertEquals(3_000, relue.getPartAssure());
        assertEquals(12_000, compter("SELECT montant FROM third_party_sale_line WHERE num_bon = 'BON-CALC-3'"));
        assertEquals(5_000, compter("SELECT montant FROM third_party_sale_line WHERE num_bon = 'BON-CALC-4'"));
    }

    /**
     * Le plafond ne bloque pas la vente : il ramène la prise en charge au reste disponible et
     * prévient le pharmacien. C'est bien la base qui tranche — le plafond est porté par l'organisme,
     * la consommation déjà faite par le compte de l'assuré.
     */
    @Test
    @DisplayName("Un plafond de consommation atteint rabote la prise en charge et avertit")
    void plafondDeConsommation() {
        Produit produit = produitEnStock("CALC PLAFOND", 50_000, 30_000, 0, 50);
        AssuredCustomer assure = assure("SYLLA", "Nabintou", "ASS-CALC-2");
        TiersPayant organisme = tiersPayant("CNAM PLAFOND");
        organisme.setPlafondConsoClient(10_000);
        ClientTiersPayant compte = compte(assure, organisme, 80, PrioriteTiersPayant.R0);
        compte.setConsoMensuelle(9_000L);
        em.flush();

        PlafondVenteException alerte = assertThrows(
            PlafondVenteException.class,
            () -> services.thirdPartySaleService.createSale(venteDe(compte, produit, "BON-CALC-5"))
        );

        assertTrue(alerte.getMessage().contains("CNAM PLAFOND"), alerte.getMessage());
        assertEquals(
            1_000,
            compter("SELECT montant FROM third_party_sale_line WHERE num_bon = 'BON-CALC-5'"),
            "il ne restait que 1 000 sous le plafond de 10 000"
        );
    }

    @Test
    @DisplayName("Retirer un article recalcule la répartition sur le reste de la vente")
    void retraitDunArticle() {
        Produit premier = produitEnStock("CALC RESTE A", 4_000, 2_400, 0, 50);
        Produit second = produitEnStock("CALC RESTE B", 6_000, 3_600, 0, 50);
        ClientTiersPayant compte = compte("CNAM RESTE", 50, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO vente = services.thirdPartySaleService.createSale(venteDe(compte, premier, "BON-CALC-6"));
        SaleLineDTO ajout = ligneDe(second, 1);
        ajout.setSaleCompositeId(vente.getSaleId());
        services.thirdPartySaleService.createOrUpdateSaleLine(ajout);
        viderLeCache();

        ThirdPartySales entite = em.find(ThirdPartySales.class, vente.getSaleId());
        SalesLine aRetirer = entite
            .getSalesLines()
            .stream()
            .filter(l -> l.getProduit().getId().equals(second.getId()))
            .findFirst()
            .orElseThrow();
        entite.getSalesLines().remove(aRetirer);
        services.salesLineService.deleteSaleLine(aRetirer);
        services.thirdPartyCalculationManager.upddateSaleAmountsOnRemovingItem(entite);
        viderLeCache();

        ThirdPartySales relue = em.find(ThirdPartySales.class, vente.getSaleId());
        assertEquals(4_000, relue.getSalesAmount());
        assertEquals(2_000, relue.getPartTiersPayant());
        assertEquals(2_000, relue.getPartAssure());
    }

    // ===== outils =====

    private ThirdPartySales venteAssurance(ClientTiersPayant compte, Produit produit, int quantite, String numBon) {
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, numBon, quantite));
        viderLeCache();
        return em.find(ThirdPartySales.class, cree.getSaleId());
    }

    private ClientTiersPayantDTO compteDto(ClientTiersPayant compte, String numBon) {
        ClientTiersPayantDTO dto = new ClientTiersPayantDTO();
        dto.setId(compte.getId());
        dto.setTaux(compte.getTaux());
        dto.setNumBon(numBon);
        return dto;
    }

    private ThirdPartySaleDTO venteDe(ClientTiersPayant compte, Produit produit, String numBon) {
        return venteDe(compte, produit, numBon, 1);
    }

    private ThirdPartySaleDTO venteDe(ClientTiersPayant compte, Produit produit, String numBon, int quantite) {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setNatureVente(NatureVente.ASSURANCE);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        dto.setCustomerId(compte.getAssuredCustomer().getId());
        dto.setSalesLines(List.of(ligneDe(produit, quantite)));
        dto.setTiersPayants(new ArrayList<>(List.of(compteDto(compte, numBon))));
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
