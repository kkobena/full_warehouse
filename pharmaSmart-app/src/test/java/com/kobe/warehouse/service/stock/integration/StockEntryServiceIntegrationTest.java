package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PutawayMode;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.stock.StockEntryService} sur un vrai PostgreSQL.
 *
 * <p>Finaliser un bon de livraison est l'écriture la plus large de l'application : pour chaque
 * ligne, le stock du produit augmente, le stock initial et final sont figés, le prix moyen pondéré
 * est recalculé, le tarif fournisseur est mis à jour et historisé s'il a bougé, les lots passent
 * disponibles et se localisent en rayon — puis la commande se ferme. Tout cela dépend de ce qui est
 * <em>déjà</em> en base : le stock initial est lu avant l'entrée, l'ancien prix d'achat sert au
 * calcul du prix moyen, et la sélection de l'emplacement à créditer se fait sur le type de stockage
 * et le magasin. Aucune de ces chaînes ne se vérifie sans base.
 *
 * <p>Les contrôles de refus comptent autant que le chemin nominal : ils doivent tomber
 * <em>avant</em> toute écriture, sans quoi un bon incomplet laisse une réception à moitié faite.
 */
@DisplayName("StockEntryService — réception d'un bon de livraison sur PostgreSQL")
class StockEntryServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("La finalisation entre le stock reçu et ferme le bon")
    void receptionNominale() {
        Reception reception = reception(50, 30, 0);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        StockProduit stock = em.find(StockProduit.class, reception.stockId());
        assertEquals(80, stock.getQtyStock(), "50 en rayon + 30 reçues");
        assertEquals(80, stock.getQtyVirtual(), "le stock virtuel suit le stock physique");
        assertEquals(OrderStatut.CLOSED, em.find(Commande.class, reception.commandeId()).getOrderStatus());
    }

    @Test
    @DisplayName("Les unités gratuites entrent en stock sans passer par la quantité facturée")
    void receptionAvecUnitesGratuites() {
        Reception reception = reception(10, 20, 5);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        StockProduit stock = em.find(StockProduit.class, reception.stockId());
        assertEquals(30, stock.getQtyStock(), "les gratuites ne gonflent pas la quantité facturée");
        assertEquals(5, stock.getQtyUG());
    }

    @Test
    @DisplayName("La ligne garde la trace du stock avant et après réception")
    void stockInitialEtFinalFiges() {
        Reception reception = reception(40, 25, 3);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        assertEquals(40, ligne.getInitStock(), "le stock initial est lu avant l'entrée");
        assertEquals(68, ligne.getFinalStock(), "40 + 25 reçues + 3 gratuites");
        assertNotNull(ligne.getReceiptDate());
    }

    @Test
    @DisplayName("Le prix moyen pondéré du produit est recalculé sur l'ancien prix d'achat")
    void prixMoyenPondere() {
        Reception reception = reception(50, 30, 0);
        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        ligne.setOrderCostAmount(6_500);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        // ((50 × 6 000) + (80 × 6 500)) / 80 : l'ancien prix d'achat est celui d'avant la mise à jour.
        assertEquals(10_250, em.find(Produit.class, reception.produitId()).getPrixMnp());
    }

    @Test
    @DisplayName("Un prix d'achat qui bouge est historisé et reporté sur le tarif fournisseur")
    void variationDePrixHistorisee() {
        Reception reception = reception(20, 10, 0);
        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        ligne.setOrderCostAmount(7_200);
        ligne.setOrderUnitPrice(11_500);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        FournisseurProduit fournisseurProduit = em.find(FournisseurProduit.class, reception.fournisseurProduitId());
        assertEquals(7_200, fournisseurProduit.getPrixAchat(), "le tarif fournisseur suit le bon");
        assertEquals(11_500, fournisseurProduit.getPrixUni());
        assertEquals(
            1,
            compter("SELECT count(*) FROM fournisseur_produit_price_history WHERE fournisseur_produit_id = " +
                reception.fournisseurProduitId()),
            "une ligne d'historique garde l'ancien et le nouveau prix"
        );
        assertEquals(
            6_000,
            ((Number) em
                    .createNativeQuery("SELECT old_prix_achat FROM fournisseur_produit_price_history WHERE fournisseur_produit_id = " +
                        reception.fournisseurProduitId())
                    .getSingleResult()).intValue()
        );
    }

    @Test
    @DisplayName("Un prix inchangé n'écrit aucune ligne d'historique")
    void prixInchangeSansHistorique() {
        Reception reception = reception(20, 10, 0);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM fournisseur_produit_price_history"));
    }

    @Test
    @DisplayName("Un bon déjà finalisé ne se finalise pas deux fois")
    void bonDejaFinalise() {
        Reception reception = reception(30, 15, 0);
        viderLeCache();
        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        DeliveryReceiptLiteDTO second = bon(reception);
        GenericError erreur = assertThrows(GenericError.class, () -> services.stockEntryService.finalizeSaisieEntreeStock(second));
        viderLeCache();

        assertTrue(erreur.getMessage().contains("déjà finalisé"), erreur.getMessage());
        assertEquals(45, em.find(StockProduit.class, reception.stockId()).getQtyStock(), "le stock n'a pas été entré deux fois");
    }

    @Test
    @DisplayName("Un code CIP manquant bloque la réception avant toute écriture de stock")
    void codeCipManquant() {
        Reception reception = reception(30, 15, 0);
        em.find(FournisseurProduit.class, reception.fournisseurProduitId()).setCodeCip("");
        viderLeCache();

        DeliveryReceiptLiteDTO bon = bon(reception);
        assertThrows(GenericError.class, () -> services.stockEntryService.finalizeSaisieEntreeStock(bon));
        viderLeCache();

        assertEquals(30, em.find(StockProduit.class, reception.stockId()).getQtyStock(), "le stock est intact");
        assertEquals(OrderStatut.RECEIVED, em.find(Commande.class, reception.commandeId()).getOrderStatus());
    }

    @Test
    @DisplayName("Une ligne saisie sans quantité reçue bloque la réception")
    void quantiteRecueManquante() {
        Reception reception = reception(30, 15, 0);
        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        ligne.setUpdated(true);
        ligne.setQuantityReceived(null);
        viderLeCache();

        DeliveryReceiptLiteDTO bon = bon(reception);
        assertThrows(GenericError.class, () -> services.stockEntryService.finalizeSaisieEntreeStock(bon));
        viderLeCache();

        assertEquals(30, em.find(StockProduit.class, reception.stockId()).getQtyStock());
    }

    @Test
    @DisplayName("Le bon conforme sans lot sert les lignes non saisies à la quantité commandée")
    void conformeSansLot() {
        Reception reception = reception(10, 0, 0);
        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        ligne.setUpdated(false);
        ligne.setQuantityReceived(null);
        ligne.setQuantityRequested(40);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception).setConformeSansLot(true));
        viderLeCache();

        assertEquals(50, em.find(StockProduit.class, reception.stockId()).getQtyStock(), "les 40 commandées sont réputées reçues");
        assertTrue(em.find(OrderLine.class, reception.ligneId()).getUpdated());
    }

    @Test
    @DisplayName("Un produit à gestion de lot sans lot renseigné bloque la réception")
    void lotManquant() {
        lenient().when(services.appConfigurationService.useLot()).thenReturn(Optional.of(true));
        Reception reception = reception(30, 15, 0);
        em.find(Produit.class, reception.produitId()).setGestionLot(true);
        viderLeCache();

        DeliveryReceiptLiteDTO bon = bon(reception);
        GenericError erreur = assertThrows(GenericError.class, () -> services.stockEntryService.finalizeSaisieEntreeStock(bon));
        viderLeCache();

        assertTrue(erreur.getMessage().contains("lots"), erreur.getMessage());
        assertEquals(30, em.find(StockProduit.class, reception.stockId()).getQtyStock());
    }

    @Test
    @DisplayName("Les lots reçus deviennent disponibles, sont tracés et localisés en rayon")
    void receptionAvecLots() {
        lenient().when(services.appConfigurationService.useLot()).thenReturn(Optional.of(true));
        Reception reception = reception(10, 25, 0);
        Lot lot = lotDeReception(reception, "LOT-REC-1", LocalDate.now().plusMonths(18), 25);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        Lot relu = em.find(Lot.class, lot.getId());
        assertEquals(StatutLot.AVAILABLE, relu.getStatut(), "le lot sort de l'état en cours de réception");
        assertEquals(25, quantiteSurEmplacement(relu, rayon), "le lot est crédité sur l'emplacement principal");
        assertEquals(
            1,
            compter("SELECT count(*) FROM lot_reception WHERE lot_id = " + lot.getId()),
            "la réception du lot est tracée pour la traçabilité fournisseur"
        );
    }

    @Test
    @DisplayName("Un lot périmant avant le seuil minimum bloque la réception")
    void lotTropProcheDeLaPeremption() {
        lenient().when(services.appConfigurationService.useLot()).thenReturn(Optional.of(true));
        lenient().when(services.appConfigurationService.getReceptionMinExpiryDays()).thenReturn(180L);
        Reception reception = reception(10, 25, 0);
        em.find(Produit.class, reception.produitId()).setCheckExpiryDate(true);
        lotDeReception(reception, "LOT-COURT", LocalDate.now().plusDays(30), 25);
        viderLeCache();

        DeliveryReceiptLiteDTO bon = bon(reception);
        GenericError erreur = assertThrows(GenericError.class, () -> services.stockEntryService.finalizeSaisieEntreeStock(bon));
        viderLeCache();

        assertTrue(erreur.getMessage().contains("péremption"), erreur.getMessage());
        assertEquals(10, em.find(StockProduit.class, reception.stockId()).getQtyStock());
    }

    @Test
    @DisplayName("Le contrôle de péremption ne s'applique pas aux produits qui en sont dispensés")
    void peremptionNonControlee() {
        lenient().when(services.appConfigurationService.useLot()).thenReturn(Optional.of(true));
        lenient().when(services.appConfigurationService.getReceptionMinExpiryDays()).thenReturn(180L);
        Reception reception = reception(10, 25, 0);
        em.find(Produit.class, reception.produitId()).setCheckExpiryDate(false);
        lotDeReception(reception, "LOT-COURT-2", LocalDate.now().plusDays(30), 25);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        assertEquals(35, em.find(StockProduit.class, reception.stockId()).getQtyStock());
    }

    @Test
    @DisplayName("Avec deux emplacements, c'est le rayon du magasin connecté qui est crédité")
    void creditDuStockPrincipal() {
        Reception reception = reception(40, 20, 0);
        Produit produit = em.find(Produit.class, reception.produitId());
        StockProduit stockReserve = stock(produit, reserve, 100);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        assertEquals(60, em.find(StockProduit.class, reception.stockId()).getQtyStock(), "le rayon prend l'entrée");
        assertEquals(100, em.find(StockProduit.class, stockReserve.getId()).getQtyStock(), "la réserve ne bouge pas");
    }

    @Test
    @DisplayName("Le rangement automatique n'est déclenché que si la politique le prévoit")
    void politiqueDeRangement() {
        lenient().when(services.appConfigurationService.getPutawayMode()).thenReturn(PutawayMode.AUTO);
        Reception reception = reception(30, 10, 0);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));
        viderLeCache();

        verify(services.suggestionReassortService).autoExecuteOverflowForProducts(anySet());
    }

    @Test
    @DisplayName("En mode manuel, le rangement attend la confirmation de l'opérateur")
    void rangementManuelSansConfirmation() {
        lenient().when(services.appConfigurationService.getPutawayMode()).thenReturn(PutawayMode.MANUAL);
        Reception reception = reception(30, 10, 0);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception).setDoTransfer(false));
        viderLeCache();

        verify(services.suggestionReassortService, never()).autoExecuteOverflowForProducts(anySet());
    }

    @Test
    @DisplayName("La finalisation enregistre les mouvements et rattache les avoirs clients")
    void effetsDeBord() {
        Reception reception = reception(30, 10, 0);
        viderLeCache();

        services.stockEntryService.finalizeSaisieEntreeStock(bon(reception));

        verify(services.inventoryTransactionService).saveAll(any());
        verify(services.avoirClientDocumentService).linkCommandeToAvoirs(any(Commande.class));
    }

    // ===== saisie ligne à ligne =====

    @Test
    @DisplayName("Saisir une quantité reçue marque la ligne comme saisie")
    void saisieDeLaQuantiteRecue() {
        Reception reception = reception(30, 0, 0);
        viderLeCache();

        services.stockEntryService.updateQuantityReceived(
            new DeliveryReceiptItemLiteDTO().setOrderLineId(reception.ligneId()).setQuantityReceivedTmp(18)
        );
        viderLeCache();

        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        assertEquals(18, ligne.getQuantityReceived());
        assertTrue(ligne.getUpdated(), "la ligne est marquée saisie : c'est ce qui la rend finalisable");
    }

    @Test
    @DisplayName("Saisir les unités gratuites et le prix unitaire met la ligne à jour")
    void saisieDesAutresChamps() {
        Reception reception = reception(30, 12, 0);
        viderLeCache();

        services.stockEntryService.updateQuantityUG(
            new DeliveryReceiptItemLiteDTO().setOrderLineId(reception.ligneId()).setQuantityUG(4)
        );
        services.stockEntryService.updateOrderUnitPrice(
            new DeliveryReceiptItemLiteDTO().setOrderLineId(reception.ligneId()).setOrderUnitPrice(12_000)
        );
        viderLeCache();

        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        assertEquals(4, ligne.getFreeQty());
        assertEquals(12_000, ligne.getOrderUnitPrice());
        assertTrue(ligne.getUpdated());
    }

    @Test
    @DisplayName("La saisie en lot met à jour chaque ligne avec sa propre quantité")
    void saisieEnLot() {
        Reception premiere = reception(30, 0, 0);
        Reception seconde = receptionSurCommande(premiere.commandeId(), 20, 0);
        viderLeCache();

        services.stockEntryService.batchUpdateQuantityReceived(
            List.of(
                new DeliveryReceiptItemLiteDTO().setOrderLineId(premiere.ligneId()).setQuantityReceivedTmp(11),
                new DeliveryReceiptItemLiteDTO().setOrderLineId(seconde.ligneId()).setQuantityReceivedTmp(7)
            )
        );
        viderLeCache();

        assertEquals(11, em.find(OrderLine.class, premiere.ligneId()).getQuantityReceived());
        assertEquals(7, em.find(OrderLine.class, seconde.ligneId()).getQuantityReceived());
        assertTrue(em.find(OrderLine.class, seconde.ligneId()).getUpdated());
    }

    // ===== écarts de prix et historique =====

    @Test
    @DisplayName("Seules les lignes dont le prix varie au-delà du seuil sont signalées")
    void lignesAvecEcartDePrix() {
        Reception faible = reception(10, 5, 0);
        Reception forte = receptionSurCommande(faible.commandeId(), 5, 0);
        // Le seuil est à 20 % : 10 % passe, 50 % est signalé.
        em.find(OrderLine.class, faible.ligneId()).setOrderCostAmount(6_600);
        em.find(OrderLine.class, forte.ligneId()).setOrderCostAmount(4_000);
        viderLeCache();

        var ecarts = services.stockEntryService.findLignesAvecEcartPrix(
            faible.commandeId().getId(),
            faible.commandeId().getOrderDate()
        );

        assertEquals(1, ecarts.size());
        assertEquals(4_000, ecarts.getFirst().getOrderCostAmount());
        assertEquals(6_000, ecarts.getFirst().getCostAmount(), "le tarif courant sert de référence");
    }

    @Test
    @DisplayName("L'historique de prix se relit du plus récent au plus ancien")
    void historiqueDePrix() {
        Reception premiere = reception(20, 10, 0);
        em.find(OrderLine.class, premiere.ligneId()).setOrderCostAmount(6_500);
        viderLeCache();
        services.stockEntryService.finalizeSaisieEntreeStock(bon(premiere));
        viderLeCache();

        var historique = services.stockEntryService.getPriceHistory(premiere.fournisseurProduitId());

        assertEquals(1, historique.size());
        assertEquals(6_000, historique.getFirst().oldPrixAchat());
        assertEquals(6_500, historique.getFirst().newPrixAchat());
    }

    // ===== bon de livraison =====

    @Test
    @DisplayName("Créer le bon passe la commande en réception et fige ses montants")
    void creationDuBon() {
        Reception reception = reception(20, 0, 0);
        viderLeCache();

        services.stockEntryService.createBon(
            bon(reception).setReceiptReference("BL-2026-42").setReceiptAmount(150_000).setTaxAmount(27_000)
        );
        viderLeCache();

        Commande commande = em.find(Commande.class, reception.commandeId());
        assertEquals(OrderStatut.RECEIVED, commande.getOrderStatus());
        assertEquals("BL-2026-42", commande.getReceiptReference());
        assertEquals(150_000, commande.getGrossAmount());
        assertEquals(27_000, commande.getTaxAmount());
        assertEquals(150_000, commande.getHtAmount());
    }

    @Test
    @DisplayName("Mettre à jour le bon réécrit ses montants sans toucher à son statut")
    void miseAJourDuBon() {
        Reception reception = reception(20, 0, 0);
        viderLeCache();

        services.stockEntryService.updateBon(bon(reception).setReceiptAmount(90_000).setTaxAmount(16_200));
        viderLeCache();

        Commande commande = em.find(Commande.class, reception.commandeId());
        assertEquals(90_000, commande.getGrossAmount());
        assertEquals(OrderStatut.RECEIVED, commande.getOrderStatus(), "le statut de la fixture est conservé");
    }

    // ===== prévisualisation du rangement =====

    @Test
    @DisplayName("La prévisualisation ne retient que les produits débordant du rayon")
    void previsualisationDuRangement() {
        Reception debordant = reception(120, 0, 0);
        Produit produitDebordant = em.find(Produit.class, debordant.produitId());
        em.find(StockProduit.class, debordant.stockId()).setStockMaxi(80);
        stock(produitDebordant, reserve, 10);

        Reception sansReserve = receptionSurCommande(debordant.commandeId(), 0, 0);
        em.find(StockProduit.class, sansReserve.stockId()).setStockMaxi(5);
        viderLeCache();

        var apercu = services.stockEntryService.getPutawayPreview(
            debordant.commandeId().getId(),
            debordant.commandeId().getOrderDate()
        );

        assertEquals(1, apercu.size(), "sans stock de réserve configuré, aucun rangement n'est proposé");
        assertEquals(produitDebordant.getId(), apercu.getFirst().produitId());
        assertEquals(40, apercu.getFirst().qtyOverflow(), "120 en rayon pour un maximum de 80");
    }

    // ===== fabriques du jeu d'essai =====

    /** Les identifiants d'une réception prête à finaliser, relus après vidage du contexte. */
    private record Reception(
        com.kobe.warehouse.domain.CommandeId commandeId,
        com.kobe.warehouse.domain.OrderLineId ligneId,
        Integer produitId,
        Integer stockId,
        Integer fournisseurProduitId
    ) {}

    private Reception reception(int stockRayon, int quantiteRecue, int gratuites) {
        Produit produit = produit(unique("PRODUIT"));
        StockProduit stockProduit = stock(produit, rayon, stockRayon);
        FournisseurProduit fournisseurProduit = fournisseurProduit(produit);
        Commande commande = commande(fournisseurProduit.getFournisseur(), OrderStatut.RECEIVED);
        OrderLine ligne = ligneDeCommande(commande, fournisseurProduit, Math.max(quantiteRecue, 1), quantiteRecue);
        ligne.setFreeQty(gratuites);
        ligne.setUpdated(true);
        em.flush();
        return new Reception(commande.getId(), ligne.getId(), produit.getId(), stockProduit.getId(), fournisseurProduit.getId());
    }

    /** Une seconde ligne sur la même commande : la finalisation les traite toutes. */
    private Reception receptionSurCommande(com.kobe.warehouse.domain.CommandeId commandeId, int quantiteRecue, int gratuites) {
        Commande commande = em.find(Commande.class, commandeId);
        Produit produit = produit(unique("PRODUIT"));
        StockProduit stockProduit = stock(produit, rayon, 0);
        FournisseurProduit fournisseurProduit = fournisseurProduit(produit);
        OrderLine ligne = ligneDeCommande(commande, fournisseurProduit, Math.max(quantiteRecue, 1), quantiteRecue);
        ligne.setFreeQty(gratuites);
        ligne.setUpdated(true);
        em.flush();
        return new Reception(commandeId, ligne.getId(), produit.getId(), stockProduit.getId(), fournisseurProduit.getId());
    }

    private Lot lotDeReception(Reception reception, String numLot, LocalDate peremption, int quantite) {
        OrderLine ligne = em.find(OrderLine.class, reception.ligneId());
        Lot lot = new Lot();
        lot.setNumLot(numLot);
        lot.setProduit(em.find(Produit.class, reception.produitId()));
        lot.setOrderLine(ligne);
        lot.setQuantity(quantite);
        lot.setCurrentQuantity(quantite);
        lot.setFreeQty(0);
        lot.setExpiryDate(peremption);
        lot.setCreatedDate(LocalDateTime.now());
        lot.setUpdated(LocalDateTime.now());
        lot.setPrixAchat(ligne.getOrderCostAmount());
        lot.setPrixUnit(ligne.getOrderUnitPrice());
        lot.setStatut(StatutLot.IN_PROGRESS);
        em.persist(lot);
        ligne.getLots().add(lot);
        em.flush();
        return lot;
    }

    private DeliveryReceiptLiteDTO bon(Reception reception) {
        return new DeliveryReceiptLiteDTO()
            .setCommandeId(reception.commandeId())
            .setReceiptDate(LocalDate.now())
            .setReceiptAmount(0)
            .setTaxAmount(0);
    }
}
