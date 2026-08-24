package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import com.kobe.warehouse.domain.enumeration.StatutLegal;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest.RetourLineRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientResultDTO;
import com.kobe.warehouse.service.sale.dto.SaleForRetourDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * {@link com.kobe.warehouse.service.sale.RetourClientService} sur un vrai PostgreSQL.
 *
 * <p>Un retour client décide, produit par produit, s'il rentre en stock, s'il part en destruction
 * ou s'il est purement et simplement refusé — un stupéfiant ne se reprend pas. Chaque décision se
 * lit ensuite dans deux tables : {@code retour_client_line} garde ce qui a été convenu,
 * {@code stock_produit} porte ce qui a été réellement remis en rayon. C'est l'écart entre les deux
 * qui compte, et il n'existe qu'en base.
 */
@DisplayName("RetourClientService — retours client sur PostgreSQL")
class RetourClientServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("La vente est retrouvée par sa référence, avec ses lignes retournables")
    void rechercheDeLaVenteParReference() {
        Produit produit = produitEnStock("DOLIPRANE RET", 1_000, 600, 0, 20);
        fournisseurProduit(produit, "CIP-RET-1");
        CashSale vente = venteFermee(produit, 3, 3);
        viderLeCache();

        SaleForRetourDTO trouvee = services.retourClientService.findSaleByRef(vente.getNumberTransaction());

        assertEquals(vente.getId().getId(), trouvee.saleId());
        assertEquals(1, trouvee.lines().size());
        assertEquals("CIP-RET-1", trouvee.lines().getFirst().codeCip());
        assertEquals(3, trouvee.lines().getFirst().quantitySold());
        assertFalse(trouvee.depasseDelai(), "vente du jour, largement dans le délai de 15 jours");
    }

    @Test
    @DisplayName("Une vente trop ancienne est signalée comme hors délai")
    void venteHorsDelai() {
        Produit produit = produitEnStock("VIEUX STOCK", 1_000, 600, 0, 20);
        CashSale vente = venteFermee(produit, 2, 2, LocalDate.now().minusDays(40));
        viderLeCache();

        SaleForRetourDTO trouvee = services.retourClientService.findSaleByRef(vente.getNumberTransaction());

        assertEquals(40, trouvee.ancienneteJours());
        assertTrue(trouvee.depasseDelai());
    }

    @Test
    @DisplayName("Une référence inconnue est refusée")
    void venteIntrouvable() {
        assertThrows(GenericError.class, () -> services.retourClientService.findSaleByRef("INCONNUE"));
    }

    @Test
    @DisplayName("Un retour conforme remet le produit en rayon et enregistre la reprise")
    void retourConforme() {
        Produit produit = produitEnStock("SIROP RET", 2_000, 1_200, 0, 10);
        CashSale vente = venteFermee(produit, 4, 4);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();

        RetourClientResultDTO resultat = services.retourClientService.validerRetour(
            retourDe(vente, ligne, 3, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, false)
        );
        viderLeCache();

        assertFalse(resultat.partiel());
        RetourClientDTO retour = resultat.retour();
        assertNotNull(retour.reference());
        assertEquals(6_000, retour.montantTotal(), "3 × 2 000 remboursés au patient");
        assertEquals(vente.getNumberTransaction(), retour.originalSaleRef());
        assertEquals(1, retour.lines().size());
        assertEquals(13, stockRayon(produit), "10 en rayon + 3 repris");
        assertEquals(1, compter("SELECT count(*) FROM retour_client_line WHERE retour_client_id = " + retour.id()));
    }

    @Test
    @DisplayName("Un produit thermosensible est repris financièrement mais pas remis en stock")
    void produitThermosensible() {
        Produit produit = produitEnStock("VACCIN", 15_000, 9_000, 0, 5);
        produit.setThermosensible(true);
        CashSale vente = venteFermee(produit, 2, 2);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();

        RetourClientResultDTO resultat = services.retourClientService.validerRetour(
            retourDe(vente, ligne, 1, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, false)
        );
        viderLeCache();

        assertTrue(resultat.partiel(), "le retour est traité, mais avec une anomalie à signaler");
        assertEquals(1, resultat.lignesNonRestockees().size());
        assertEquals(5, stockRayon(produit), "la chaîne du froid est rompue : le rayon ne bouge pas");
        assertEquals(15_000, resultat.retour().montantTotal(), "le patient est remboursé malgré tout");
        assertEquals(
            1,
            compter("SELECT count(*) FROM retour_client_line WHERE retour_client_id = " + resultat.retour().id()),
            "la ligne de reprise est bien enregistrée : seul le mouvement de stock n'a pas eu lieu"
        );
    }

    @Test
    @DisplayName("Un emballage abîmé écarte la ligne du stock sans annuler la reprise")
    void etatNonConforme() {
        Produit produit = produitEnStock("BOITE ABIMEE", 3_000, 1_800, 0, 8);
        CashSale vente = venteFermee(produit, 2, 2);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();

        RetourClientResultDTO resultat = services.retourClientService.validerRetour(
            retourDe(vente, ligne, 2, ModeReglementRetour.REMBOURSEMENT_ESPECES, false, false)
        );
        viderLeCache();

        assertEquals(1, resultat.lignesNonRestockees().size());
        assertEquals(8, stockRayon(produit));
    }

    @Test
    @DisplayName("Un stupéfiant ne se reprend pas : sans autre ligne, la demande entière est refusée")
    void stupefiantRefuse() {
        Produit produit = produitEnStock("MORPHINE", 8_000, 5_000, 0, 5);
        produit.setStatutLegal(StatutLegal.STUPEFIANTS);
        CashSale vente = venteFermee(produit, 2, 2);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();

        RetourClientRequest requete = retourDe(vente, ligne, 1, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, false);
        GenericError echec = assertThrows(GenericError.class, () -> services.retourClientService.validerRetour(requete));

        assertTrue(echec.getMessage().contains("Aucune ligne retournable"), echec.getMessage());
        assertEquals(0, compter("SELECT count(*) FROM retour_client"), "rien n'est écrit quand rien n'est reprenable");
    }

    @Test
    @DisplayName("Le stupéfiant est écarté mais les autres lignes de la demande passent")
    void stupefiantEcarteParmiDautresLignes() {
        Produit stupefiant = produitEnStock("FENTANYL", 20_000, 12_000, 0, 5);
        stupefiant.setStatutLegal(StatutLegal.STUPEFIANTS);
        Produit ordinaire = produitEnStock("PARACETAMOL RET", 1_000, 600, 0, 10);
        CashSale vente = venteFermee(stupefiant, 1, 1);
        SalesLine ligneOrdinaire = ajouterLigne(vente, ordinaire, 2, 2);
        SalesLine ligneStupefiant = vente
            .getSalesLines()
            .stream()
            .filter(l -> l.getProduit().getId().equals(stupefiant.getId()))
            .findFirst()
            .orElseThrow();
        viderLeCache();

        RetourClientResultDTO resultat = services.retourClientService.validerRetour(
            new RetourClientRequest(
                vente.getId().getId(),
                vente.getSaleDate(),
                MotifRetourClient.ERREUR_DISPENSATION,
                ModeReglementRetour.REMBOURSEMENT_ESPECES,
                null,
                List.of(
                    new RetourLineRequest(ligneStupefiant.getId().getId(), ligneStupefiant.getSaleDate(), 1, true, true, true),
                    new RetourLineRequest(ligneOrdinaire.getId().getId(), ligneOrdinaire.getSaleDate(), 2, true, true, true)
                ),
                false
            )
        );
        viderLeCache();

        assertTrue(resultat.partiel());
        assertEquals(1, resultat.lignesRejetees().size());
        assertEquals(StatutLegal.STUPEFIANTS, resultat.lignesRejetees().getFirst().statutLegal());
        assertEquals(1, resultat.retour().lines().size(), "seule la ligne ordinaire est reprise");
        assertEquals(12, stockRayon(ordinaire));
        assertEquals(5, stockRayon(stupefiant));
    }

    @Test
    @DisplayName("Rendre plus que ce qui a été servi est refusé")
    void quantiteSuperieureAuVendu() {
        Produit produit = produitEnStock("QUANTITE RET", 1_000, 600, 0, 10);
        CashSale vente = venteFermee(produit, 2, 2);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();

        RetourClientRequest requete = retourDe(vente, ligne, 5, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, false);
        assertThrows(GenericError.class, () -> services.retourClientService.validerRetour(requete));
    }

    @Test
    @DisplayName("Un règlement en avoir ouvre un avoir client du montant repris")
    void reglementEnAvoir() {
        Produit produit = produitEnStock("AVOIR RET", 2_500, 1_500, 0, 10);
        UninsuredCustomer client = client("CISSE", "Adama", "CLI-RET-1");
        CashSale vente = venteFermee(produit, 2, 2);
        vente.setCustomer(client);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();

        RetourClientResultDTO resultat = services.retourClientService.validerRetour(
            retourDe(vente, ligne, 2, ModeReglementRetour.AVOIR_CLIENT, true, false)
        );
        viderLeCache();

        assertEquals(
            1,
            compter("SELECT count(*) FROM avoir_client WHERE customer_id = " + client.getId()),
            "le crédit du patient est matérialisé par un avoir"
        );
        assertEquals(5_000, compter("SELECT montant FROM avoir_client WHERE customer_id = " + client.getId()));
        assertEquals(ModeReglementRetour.AVOIR_CLIENT, resultat.retour().modeReglement());
    }

    @Test
    @DisplayName("Un retour avec échange force l'avoir et rend le contexte de la vente de remplacement")
    void retourAvecEchange() {
        Produit produit = produitEnStock("ECHANGE", 4_000, 2_400, 0, 10);
        UninsuredCustomer client = client("KEITA", "Mariam", "CLI-RET-2");
        CashSale vente = venteFermee(produit, 1, 1);
        vente.setCustomer(client);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();

        RetourClientResultDTO resultat = services.retourClientService.validerRetour(
            retourDe(vente, ligne, 1, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, true)
        );

        assertEquals(ModeReglementRetour.AVOIR_CLIENT, resultat.retour().modeReglement(), "l'échange impose l'avoir");
        assertNotNull(resultat.echangeContext());
        assertEquals(4_000, resultat.echangeContext().montantCredit());
        assertEquals("Mariam KEITA", resultat.echangeContext().customerName());
        assertEquals(1, resultat.echangeContext().avoirReferences().size());
    }

    @Test
    @DisplayName("La vente de remplacement se rattache au retour, et seulement s'il attend un échange")
    void rattachementDeLaVenteDechange() {
        Produit produit = produitEnStock("LIEN ECHANGE", 3_000, 1_800, 0, 10);
        CashSale vente = venteFermee(produit, 1, 1);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();
        Integer avecEchange = services.retourClientService
            .validerRetour(retourDe(vente, ligne, 1, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, true))
            .retour()
            .id();

        RetourClientDTO lie = services.retourClientService.lierVenteEchange(avecEchange, "VTE-REMPLACEMENT");
        viderLeCache();

        assertEquals("VTE-REMPLACEMENT", lie.echangeSaleRef());
        assertEquals("VTE-REMPLACEMENT", em.find(com.kobe.warehouse.domain.RetourClient.class, avecEchange).getEchangeSaleRef());

        Produit autre = produitEnStock("SANS ECHANGE", 1_000, 600, 0, 10);
        CashSale venteSimple = venteFermee(autre, 1, 1);
        SalesLine ligneSimple = venteSimple.getSalesLines().iterator().next();
        viderLeCache();
        Integer sansEchange = services.retourClientService
            .validerRetour(retourDe(venteSimple, ligneSimple, 1, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, false))
            .retour()
            .id();

        assertThrows(GenericError.class, () -> services.retourClientService.lierVenteEchange(sansEchange, "VTE-X"));
    }

    @Test
    @DisplayName("La liste des retours filtre sur la période et se relit par identifiant")
    void listeDesRetours() {
        Produit produit = produitEnStock("LISTE RET", 1_000, 600, 0, 20);
        CashSale vente = venteFermee(produit, 3, 3);
        SalesLine ligne = vente.getSalesLines().iterator().next();
        viderLeCache();
        Integer retourId = services.retourClientService
            .validerRetour(retourDe(vente, ligne, 1, ModeReglementRetour.REMBOURSEMENT_ESPECES, true, false))
            .retour()
            .id();
        viderLeCache();

        var duJour = services.retourClientService.findAll(null, LocalDate.now(), LocalDate.now(), PageRequest.of(0, 20));
        var anneeDerniere = services.retourClientService.findAll(
            null,
            LocalDate.now().minusYears(1),
            LocalDate.now().minusYears(1),
            PageRequest.of(0, 20)
        );

        assertEquals(1, duJour.getTotalElements());
        assertEquals(0, anneeDerniere.getTotalElements());
        assertEquals(retourId, services.retourClientService.findById(retourId).id());
    }

    // ===== outils =====

    private int stockRayon(Produit produit) {
        StockProduit stock = services.stockProduitRepository.findOneByProduitIdAndStockageId(produit.getId(), STORAGE_RAYON_ID);
        return stock.getQtyStock();
    }

    private RetourClientRequest retourDe(
        CashSale vente,
        SalesLine ligne,
        int quantite,
        ModeReglementRetour mode,
        boolean etatConforme,
        boolean avecEchange
    ) {
        return new RetourClientRequest(
            vente.getId().getId(),
            vente.getSaleDate(),
            MotifRetourClient.ERREUR_DISPENSATION,
            mode,
            "retour au comptoir",
            List.of(new RetourLineRequest(ligne.getId().getId(), ligne.getSaleDate(), quantite, etatConforme, true, true)),
            avecEchange
        );
    }
}
