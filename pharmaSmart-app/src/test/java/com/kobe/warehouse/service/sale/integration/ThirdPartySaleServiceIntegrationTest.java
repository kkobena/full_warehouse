package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.service.errors.ThirdPartySalesTiersPayantException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.ThirdPartySaleService} sur un vrai PostgreSQL.
 *
 * <p>Une vente assurance se répartit entre l'assuré et un ou plusieurs tiers-payants, et cette
 * répartition s'écrit dans trois endroits distincts : la vente ({@code part_assure},
 * {@code part_tiers_payant}), ses lignes tiers-payant dans une table partitionnée à part, et la
 * consommation cumulée du client et de l'organisme, stockée en {@code jsonb}. Vérifier ce trio
 * demande une vraie base : le {@code jsonb} n'existe nulle part ailleurs, et l'unicité du numéro de
 * bon se contrôle par une requête sur trois mois glissants.
 */
@DisplayName("ThirdPartySaleService — ventes assurance sur PostgreSQL")
class ThirdPartySaleServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("La vente assurance répartit le montant entre le tiers-payant et l'assuré")
    void creationVenteAssurance() {
        Produit produit = produitEnStock("INSULINE", 10_000, 6_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM", 80, PrioriteTiersPayant.R0);

        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, 1, "BON-001"));
        viderLeCache();

        ThirdPartySales vente = em.find(ThirdPartySales.class, cree.getSaleId());
        assertNotNull(vente);
        assertEquals(10_000, vente.getSalesAmount());
        assertEquals(8_000, vente.getPartTiersPayant(), "80 % pris en charge");
        assertEquals(2_000, vente.getPartAssure());
        assertEquals(
            1,
            compter("SELECT count(*) FROM third_party_sale_line WHERE sale_id = " + cree.getSaleId().getId()),
            "la ligne tiers-payant vit dans sa propre table partitionnée"
        );
    }

    @Test
    @DisplayName("Sans client, la vente assurance est refusée avant toute écriture")
    void venteSansClient() {
        Produit produit = produitEnStock("METFORMINE", 5_000, 3_000, 0, 50);
        ClientTiersPayant compte = compte("MUGEF", 70, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO dto = venteDe(compte, produit, 1, "BON-002");
        dto.setCustomerId(null);

        assertThrows(GenericError.class, () -> services.thirdPartySaleService.createSale(dto));
    }

    @Test
    @DisplayName("Un second tiers-payant se greffe sur la vente et la répartition est refaite")
    void ajoutDUnSecondTiersPayant() {
        Produit produit = produitEnStock("ANTIBIO VO", 10_000, 6_000, 0, 50);
        AssuredCustomer assure = assure("TRAORE", "Salif", "ASS-TP-2");
        ClientTiersPayant principal = compte(assure, "CNAM 2", 60, PrioriteTiersPayant.R0);
        ClientTiersPayant complementaire = compte(assure, "MUTUELLE 2", 20, PrioriteTiersPayant.R1);

        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(principal, produit, 1, "BON-003"));
        services.thirdPartySaleService.addThirdPartySaleLineToSales(compteDto(complementaire, "BON-004"), cree.getSaleId());
        viderLeCache();

        ThirdPartySales vente = em.find(ThirdPartySales.class, cree.getSaleId());
        assertEquals(2, compter("SELECT count(*) FROM third_party_sale_line WHERE sale_id = " + cree.getSaleId().getId()));
        assertEquals(8_000, vente.getPartTiersPayant(), "60 % + 20 %");
        assertEquals(2_000, vente.getPartAssure());
    }

    @Test
    @DisplayName("Retirer un tiers-payant efface sa ligne et rend la charge à l'assuré")
    void retraitDUnTiersPayant() {
        Produit produit = produitEnStock("ANTIBIO VO 2", 10_000, 6_000, 0, 50);
        AssuredCustomer assure = assure("KONE", "Awa", "ASS-TP-3");
        ClientTiersPayant principal = compte(assure, "CNAM 3", 60, PrioriteTiersPayant.R0);
        ClientTiersPayant complementaire = compte(assure, "MUTUELLE 3", 20, PrioriteTiersPayant.R1);
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(principal, produit, 1, "BON-005"));
        services.thirdPartySaleService.addThirdPartySaleLineToSales(compteDto(complementaire, "BON-006"), cree.getSaleId());

        services.thirdPartySaleService.removeThirdPartySaleLineToSales(complementaire.getId(), cree.getSaleId());
        viderLeCache();

        ThirdPartySales vente = em.find(ThirdPartySales.class, cree.getSaleId());
        assertEquals(1, compter("SELECT count(*) FROM third_party_sale_line WHERE sale_id = " + cree.getSaleId().getId()));
        assertEquals(6_000, vente.getPartTiersPayant());
        assertEquals(4_000, vente.getPartAssure());
    }

    @Test
    @DisplayName("Changer le taux de prise en charge redistribue la vente")
    void changementDeTaux() {
        Produit produit = produitEnStock("ANTIHYPERTENSEUR", 20_000, 12_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM 4", 50, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, 1, "BON-007"));

        services.thirdPartySaleService.updateTiersPayantTaux(compte.getId(), cree.getSaleId(), 90);
        viderLeCache();

        ThirdPartySales vente = em.find(ThirdPartySales.class, cree.getSaleId());
        assertEquals(18_000, vente.getPartTiersPayant());
        assertEquals(2_000, vente.getPartAssure());
    }

    @Test
    @DisplayName("Ajouter un produit refait la répartition sur le nouveau total")
    void ajoutDeProduitRefaitLaRepartition() {
        Produit premier = produitEnStock("SIROP VO", 4_000, 2_400, 0, 50);
        Produit second = produitEnStock("POMMADE VO", 6_000, 3_600, 0, 50);
        ClientTiersPayant compte = compte("CNAM 5", 75, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, premier, 1, "BON-008"));

        SaleLineDTO ajout = ligneDe(second, 1);
        ajout.setSaleCompositeId(cree.getSaleId());
        services.thirdPartySaleService.createOrUpdateSaleLine(ajout);
        viderLeCache();

        ThirdPartySales vente = em.find(ThirdPartySales.class, cree.getSaleId());
        assertEquals(10_000, vente.getSalesAmount());
        assertEquals(7_500, vente.getPartTiersPayant());
        assertEquals(2_500, vente.getPartAssure());
    }

    @Test
    @DisplayName("La clôture ferme la vente, retient le numéro de bon et cumule la consommation")
    void clotureDeLaVente() {
        Produit produit = produitEnStock("TRAITEMENT VO", 10_000, 6_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM 6", 80, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, 1, "BON-009"));
        viderLeCache();

        services.thirdPartySaleService.save(clotureDe(cree.getSaleId(), compte, "BON-009", 2_000));
        viderLeCache();

        ThirdPartySales vente = em.find(ThirdPartySales.class, cree.getSaleId());
        assertEquals(SalesStatut.CLOSED, vente.getStatut());
        assertEquals("BON-009", vente.getNumBon(), "le bon du tiers-payant prioritaire remonte sur la vente");
        assertNotNull(vente.getNumberTransaction());

        ClientTiersPayant relu = em.find(ClientTiersPayant.class, compte.getId());
        assertFalse(relu.getConsommations().isEmpty(), "la consommation mensuelle est écrite en jsonb");
        assertTrue(
            relu.getConsommations().stream().anyMatch(c -> c.getConsommation() == 8_000L),
            "elle porte la part prise en charge : " + relu.getConsommations()
        );
    }

    @Test
    @DisplayName("Une vente assurance sans tiers-payant dans la demande de clôture est refusée")
    void clotureSansTiersPayant() {
        Produit produit = produitEnStock("TRAITEMENT VO 2", 8_000, 5_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM 7", 80, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, 1, "BON-010"));
        viderLeCache();

        ThirdPartySaleDTO cloture = clotureDe(cree.getSaleId(), compte, "BON-010", 1_600);
        cloture.setTiersPayants(new ArrayList<>());

        assertThrows(ThirdPartySalesTiersPayantException.class, () -> services.thirdPartySaleService.save(cloture));
    }

    @Test
    @DisplayName("Un numéro de bon déjà servi par le même compte sur trois mois est rejeté")
    void numeroDeBonDejaUtilise() {
        Produit produit = produitEnStock("BON DOUBLE", 5_000, 3_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM 8", 80, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO premiere = services.thirdPartySaleService.createSale(venteDe(compte, produit, 1, "BON-011"));
        viderLeCache();
        services.thirdPartySaleService.save(clotureDe(premiere.getSaleId(), compte, "BON-011", 1_000));
        viderLeCache();

        ThirdPartySaleDTO seconde = venteDe(compte, produit, 1, "BON-011");
        assertThrows(NumBonAlreadyUseException.class, () -> services.thirdPartySaleService.createSale(seconde));
    }

    @Test
    @DisplayName("Une vente comptant se transforme en vente assurance en gardant ses lignes")
    void transformationDUneVenteComptant() {
        Produit produit = produitEnStock("BASCULE", 3_000, 1_800, 0, 50);
        CashSale comptant = venteComptantEnBase(produit, 2);
        viderLeCache();

        SaleId assuranceId = services.thirdPartySaleService.changeCashSaleToThirdPartySale(comptant.getId(), NatureVente.ASSURANCE);
        viderLeCache();

        assertEquals(
            "ThirdPartySales",
            em.createNativeQuery("SELECT dtype FROM sales WHERE id = " + assuranceId.getId()).getSingleResult()
        );
        assertEquals(0, compter("SELECT count(*) FROM sales WHERE id = " + comptant.getId().getId()));
        assertEquals(
            1,
            compter("SELECT count(*) FROM sales_line WHERE sales_id = " + assuranceId.getId()),
            "la ligne a suivi la vente sur son nouvel identifiant"
        );
    }

    @Test
    @DisplayName("Supprimer une prévente assurance efface la vente et ses lignes")
    void suppressionDUnePrevente() {
        Produit produit = produitEnStock("PREVENTE VO", 2_000, 1_200, 0, 50);
        ClientTiersPayant compte = compte("CNAM 9", 80, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, 1, "BON-012"));
        viderLeCache();

        services.thirdPartySaleService.deleteSalePrevente(cree.getSaleId());
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM sales WHERE id = " + cree.getSaleId().getId()));
        assertEquals(0, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + cree.getSaleId().getId()));
    }

    @Test
    @DisplayName("Les lignes tiers-payant d'une vente se relisent par son identifiant composite")
    void lectureDesLignesTiersPayant() {
        Produit produit = produitEnStock("LECTURE VO", 7_000, 4_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM 10", 65, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, 1, "BON-013"));
        viderLeCache();

        List<ThirdPartySaleLine> lignes = services.thirdPartySaleService.findAllBySaleId(cree.getSaleId());

        assertEquals(1, lignes.size());
        assertEquals(4_550, lignes.getFirst().getMontant(), "65 % de 7 000");
        assertEquals("BON-013", lignes.getFirst().getNumBon());
    }

    // ===== outils =====

    private CashSale venteComptantEnBase(Produit produit, int quantite) {
        CashSale vente = new CashSale();
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("VO" + vente.getId().getId());
        vente.setStatut(SalesStatut.ACTIVE);
        vente.setPaymentStatus(PaymentStatus.IMPAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(LocalDateTime.now());
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(quantite * produit.getRegularUnitPrice());
        vente.setAmountToBeTakenIntoAccount(quantite * produit.getRegularUnitPrice());
        vente.setUser(caissier);
        vente.setSeller(caissier);
        vente.setCaissier(caissier);
        vente.setMagasin(magasin);
        em.persist(vente);

        SalesLine ligne = services.salesLineService.createSaleLineFromDTO(ligneDe(produit, quantite), STORAGE_RAYON_ID);
        ligne.setSales(vente);
        vente.getSalesLines().add(ligne);
        services.salesLineService.saveSalesLine(ligne);
        em.flush();
        return vente;
    }

    private ClientTiersPayantDTO compteDto(ClientTiersPayant compte, String numBon) {
        ClientTiersPayantDTO dto = new ClientTiersPayantDTO();
        dto.setId(compte.getId());
        dto.setTaux(compte.getTaux());
        dto.setNumBon(numBon);
        return dto;
    }

    private ThirdPartySaleDTO venteDe(ClientTiersPayant compte, Produit produit, int quantite, String numBon) {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setNatureVente(NatureVente.ASSURANCE);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        dto.setCustomerId(compte.getAssuredCustomer().getId());
        dto.setSalesLines(List.of(ligneDe(produit, quantite)));
        dto.setTiersPayants(new ArrayList<>(List.of(compteDto(compte, numBon))));
        return dto;
    }

    private ThirdPartySaleDTO clotureDe(SaleId saleId, ClientTiersPayant compte, String numBon, int partAssure) {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setSaleId(saleId);
        dto.setPayrollAmount(partAssure);
        dto.setAmountToBePaid(partAssure);
        dto.setRestToPay(0);
        dto.setMontantRendu(0);
        dto.setTiersPayants(new ArrayList<>(List.of(compteDto(compte, numBon))));
        PaymentDTO reglement = new PaymentDTO();
        reglement.setPaidAmount(partAssure);
        reglement.setNetAmount(partAssure);
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
