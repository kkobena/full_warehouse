package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.ThirdPartyClientManager} sur un vrai PostgreSQL.
 *
 * <p>Ce gestionnaire tient les comptes : il retrouve les comptes d'assurés, refuse un numéro de bon
 * déjà servi et cumule les consommations mensuelles du client comme de l'organisme. Le contrôle du
 * bon interroge trois mois glissants de ventes clôturées, et la consommation vit en {@code jsonb} —
 * deux choses qu'aucun double mémoire ne reproduit.
 */
@DisplayName("ThirdPartyClientManager — comptes et consommations sur PostgreSQL")
class ThirdPartyClientManagerIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Les comptes d'assurés sont retrouvés par lot d'identifiants")
    void rechercheDesComptes() {
        AssuredCustomer assure = assure("BAKAYOKO", "Ismael", "ASS-TPM-1");
        ClientTiersPayant premier = compte(assure, "CNAM TPM", 70, PrioriteTiersPayant.R0);
        ClientTiersPayant second = compte(assure, "MUTUELLE TPM", 20, PrioriteTiersPayant.R1);
        viderLeCache();

        List<ClientTiersPayant> comptes = services.thirdPartyClientManager.getClientTiersPayants(
            Set.of(premier.getId(), second.getId(), 999_999)
        );

        assertEquals(2, comptes.size(), "l'identifiant inconnu est simplement ignoré");
    }

    @Test
    @DisplayName("Un numéro de bon libre le reste tant qu'aucune vente clôturée ne le porte")
    void numeroDeBonLibre() {
        ClientTiersPayant compte = compte("CNAM LIBRE", 80, PrioriteTiersPayant.R0);
        viderLeCache();

        assertFalse(services.thirdPartyClientManager.checkIfNumBonIsAlReadyUse("BON-LIBRE", compte.getId(), null));
        assertFalse(services.thirdPartyClientManager.checkIfNumBonIsAlReadyUse(null, compte.getId(), null), "pas de bon, pas de conflit");
        assertFalse(services.thirdPartyClientManager.checkIfNumBonIsAlReadyUse("", compte.getId(), null));
    }

    @Test
    @DisplayName("Un bon déjà servi par le même compte est détecté ; un autre compte n'est pas concerné")
    void numeroDeBonDejaServi() {
        Produit produit = produitEnStock("BON TPM", 5_000, 3_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM SERVI", 80, PrioriteTiersPayant.R0);
        ClientTiersPayant autreCompte = compte("MUGEF SERVI", 60, PrioriteTiersPayant.R0);
        venteCloturee(compte, produit, "BON-TPM-1");
        viderLeCache();

        assertTrue(services.thirdPartyClientManager.checkIfNumBonIsAlReadyUse("BON-TPM-1", compte.getId(), null));
        assertFalse(
            services.thirdPartyClientManager.checkIfNumBonIsAlReadyUse("BON-TPM-1", autreCompte.getId(), null),
            "le bon appartient au compte, pas au numéro seul"
        );
    }

    @Test
    @DisplayName("La vente en cours ne se voit pas elle-même comme un doublon")
    void bonDeLaVenteCourante() {
        Produit produit = produitEnStock("BON COURANT", 4_000, 2_400, 0, 50);
        ClientTiersPayant compte = compte("CNAM COURANT", 80, PrioriteTiersPayant.R0);
        Long saleId = venteCloturee(compte, produit, "BON-TPM-2");
        viderLeCache();

        assertFalse(
            services.thirdPartyClientManager.checkIfNumBonIsAlReadyUse("BON-TPM-2", compte.getId(), saleId),
            "sinon rééditer une vente deviendrait impossible"
        );
    }

    @Test
    @DisplayName("La consommation du compte et celle de l'organisme se cumulent en jsonb")
    void cumulDesConsommations() {
        Produit produit = produitEnStock("CONSO TPM", 10_000, 6_000, 0, 50);
        ClientTiersPayant compte = compte("CNAM CONSO", 50, PrioriteTiersPayant.R0);
        venteCloturee(compte, produit, "BON-TPM-3");
        venteCloturee(compte, produit, "BON-TPM-4");
        viderLeCache();

        ClientTiersPayant relu = em.find(ClientTiersPayant.class, compte.getId());
        TiersPayant organisme = relu.getTiersPayant();

        assertEquals(1, relu.getConsommations().size(), "un seul cumul mensuel, pas une ligne par vente");
        assertEquals(10_000L, relu.getConsommations().iterator().next().getConsommation(), "2 × 50 % de 10 000");
        assertEquals(10_000, relu.getConsoMensuelle());
        assertEquals(10_000L, organisme.getConsommations().iterator().next().getConsommation(), "l'organisme suit le même cumul");
    }

    @Test
    @DisplayName("Retirer un tiers-payant d'une vente supprime sa ligne et redonne la charge à l'assuré")
    void retraitDUnCompte() {
        Produit produit = produitEnStock("RETRAIT TPM", 10_000, 6_000, 0, 50);
        AssuredCustomer assure = assure("DIABATE", "Fanta", "ASS-TPM-2");
        ClientTiersPayant principal = compte(assure, "CNAM RETRAIT", 60, PrioriteTiersPayant.R0);
        ClientTiersPayant complementaire = compte(assure, "MUTUELLE RETRAIT", 30, PrioriteTiersPayant.R1);
        ThirdPartySaleDTO vente = services.thirdPartySaleService.createSale(venteDe(principal, produit, "BON-TPM-5"));
        services.thirdPartyClientManager.addThirdPartySaleLineToSales(compteDto(complementaire, "BON-TPM-6"), vente.getSaleId());
        viderLeCache();

        services.thirdPartyClientManager.removeThirdPartySaleLineToSales(complementaire.getId(), vente.getSaleId());
        viderLeCache();

        assertEquals(1, compter("SELECT count(*) FROM third_party_sale_line WHERE sale_id = " + vente.getSaleId().getId()));
        assertEquals(6_000, em.find(com.kobe.warehouse.domain.ThirdPartySales.class, vente.getSaleId()).getPartTiersPayant());
    }

    @Test
    @DisplayName("Les lignes tiers-payant d'une vente se retrouvent par son identifiant composite")
    void lignesDUneVente() {
        Produit produit = produitEnStock("LIGNES TPM", 6_000, 3_600, 0, 50);
        ClientTiersPayant compte = compte("CNAM LIGNES", 75, PrioriteTiersPayant.R0);
        ThirdPartySaleDTO vente = services.thirdPartySaleService.createSale(venteDe(compte, produit, "BON-TPM-7"));
        viderLeCache();

        List<ThirdPartySaleLine> lignes = services.thirdPartyClientManager.findAllBySaleId(vente.getSaleId());

        assertEquals(1, lignes.size());
        assertNotNull(lignes.getFirst().getClientTiersPayant());
        assertEquals(4_500, lignes.getFirst().getMontant(), "75 % de 6 000");
    }

    // ===== outils =====

    /** Une vente assurance menée jusqu'à la clôture : c'est elle qui consomme le numéro de bon. */
    private Long venteCloturee(ClientTiersPayant compte, Produit produit, String numBon) {
        ThirdPartySaleDTO cree = services.thirdPartySaleService.createSale(venteDe(compte, produit, numBon));
        viderLeCache();
        int partAssure = em.find(com.kobe.warehouse.domain.ThirdPartySales.class, cree.getSaleId()).getPartAssure();

        ThirdPartySaleDTO cloture = new ThirdPartySaleDTO();
        cloture.setSaleId(cree.getSaleId());
        cloture.setPayrollAmount(partAssure);
        cloture.setAmountToBePaid(partAssure);
        cloture.setRestToPay(0);
        cloture.setMontantRendu(0);
        cloture.setTiersPayants(new ArrayList<>(List.of(compteDto(compte, numBon))));
        PaymentDTO reglement = new PaymentDTO();
        reglement.setPaidAmount(partAssure);
        reglement.setNetAmount(partAssure);
        reglement.setPaymentMode(new PaymentModeDTO().setCode("CASH"));
        cloture.setPayments(List.of(reglement));
        services.thirdPartySaleService.save(cloture);
        viderLeCache();
        return cree.getSaleId().getId();
    }

    private ClientTiersPayantDTO compteDto(ClientTiersPayant compte, String numBon) {
        ClientTiersPayantDTO dto = new ClientTiersPayantDTO();
        dto.setId(compte.getId());
        dto.setTaux(compte.getTaux());
        dto.setNumBon(numBon);
        return dto;
    }

    private ThirdPartySaleDTO venteDe(ClientTiersPayant compte, Produit produit, String numBon) {
        SaleLineDTO ligne = new SaleLineDTO();
        ligne.setProduitId(produit.getId());
        ligne.setQuantityRequested(1);
        ligne.setQuantitySold(1);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());

        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setNatureVente(NatureVente.ASSURANCE);
        dto.setTypePrescription(TypePrescription.PRESCRIPTION);
        dto.setCustomerId(compte.getAssuredCustomer().getId());
        dto.setSalesLines(List.of(ligne));
        dto.setTiersPayants(new ArrayList<>(List.of(compteDto(compte, numBon))));
        return dto;
    }
}
