package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.stock.LotService} sur un vrai PostgreSQL.
 *
 * <p>Deux façons de créer un lot coexistent, et elles n'obéissent pas aux mêmes règles : à la
 * réception d'une commande, le lot se rattache à une ligne de commande et ne peut pas dépasser la
 * quantité reçue ; à la saisie depuis la fiche produit, il porte sur du stock déjà présent et ne
 * peut pas dépasser le stock de l'emplacement. Les deux plafonds se calculent à partir de ce qui
 * est <em>déjà</em> en base — lots existants d'un côté, {@code stock_produit} de l'autre — et
 * l'unicité {@code (num_lot, produit)} n'existe qu'en base.
 */
@DisplayName("LotService — cycle de vie des lots sur PostgreSQL")
class LotServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Un lot de réception reprend les prix de sa ligne de commande")
    void ajoutSurLigneDeCommande() {
        OrderLine ligne = ligneRecue(30);

        LotDTO cree = services.lotService.addLot(lotDe(ligne, "LOT-A", 30));
        viderLeCache();

        Lot relu = em.find(Lot.class, cree.getId());
        assertEquals(30, relu.getQuantity());
        assertEquals(30, relu.getCurrentQuantity(), "un lot neuf a tout son stock devant lui");
        assertEquals(ligne.getOrderCostAmount(), relu.getPrixAchat());
        assertEquals(ligne.getOrderUnitPrice(), relu.getPrixUnit());
        assertEquals(StatutLot.IN_PROGRESS, relu.getStatut());
        assertNotNull(relu.getProduit(), "le produit se déduit du référencement fournisseur de la ligne");
    }

    @Test
    @DisplayName("Un second lot portant le même numéro cumule sur le premier")
    void memeNumeroDeLotCumule() {
        OrderLine ligne = ligneRecue(50);
        LotDTO premier = services.lotService.addLot(lotDe(ligne, "LOT-B", 20));
        viderLeCache();

        LotDTO second = services.lotService.addLot(lotDe(ligne, "LOT-B", 15));
        viderLeCache();

        assertEquals(premier.getId(), second.getId(), "c'est le même lot qui grossit");
        Lot relu = em.find(Lot.class, premier.getId());
        assertEquals(35, relu.getQuantity());
        assertEquals(35, relu.getCurrentQuantity());
        assertEquals(1, compter("SELECT count(*) FROM lot WHERE num_lot = 'LOT-B'"));
    }

    @Test
    @DisplayName("La somme des lots ne peut pas dépasser la quantité reçue")
    void lotsAuDelaDeLaQuantiteRecue() {
        OrderLine ligne = ligneRecue(10);
        services.lotService.addLot(lotDe(ligne, "LOT-C", 8));
        viderLeCache();

        LotDTO detrop = lotDe(ligne, "LOT-D", 5);

        GenericError erreur = assertThrows(GenericError.class, () -> services.lotService.addLot(detrop));
        assertTrue(erreur.getMessage().contains("13"), "l'erreur chiffre le dépassement : " + erreur.getMessage());
        assertEquals(1, compter("SELECT count(*) FROM lot WHERE order_line_id = " + ligne.getId().getId()));
    }

    @Test
    @DisplayName("Un lot saisi hors commande est disponible et localisé sur son emplacement")
    void ajoutHorsCommande() {
        Produit produit = produitEnStock("KARDEGIC", 60);
        fournisseurProduit(produit);
        viderLeCache();

        LotDTO cree = services.lotService.addLotSurProduit(
            new LotDTO()
                .setProduitId(produit.getId())
                .setNumLot("LOT-E")
                .setExpiryDate(LocalDate.now().plusMonths(8))
                .setQuantityReceived(25)
                .setStorageId(STORAGE_RAYON_ID)
        );
        viderLeCache();

        Lot relu = em.find(Lot.class, cree.getId());
        assertEquals(StatutLot.AVAILABLE, relu.getStatut(), "le stock est déjà là : rien n'est en cours de réception");
        assertEquals(25, relu.getCurrentQuantity());
        assertEquals(25, quantiteSurEmplacement(relu, rayon));
    }

    @Test
    @DisplayName("Un lot hors commande ne peut pas dépasser le stock de l'emplacement")
    void horsCommandeAuDelaDuStock() {
        Produit produit = produitEnStock("PLAVIX", 10);
        viderLeCache();

        LotDTO detrop = new LotDTO()
            .setProduitId(produit.getId())
            .setNumLot("LOT-F")
            .setExpiryDate(LocalDate.now().plusMonths(8))
            .setQuantityReceived(40)
            .setStorageId(STORAGE_RAYON_ID);

        assertThrows(GenericError.class, () -> services.lotService.addLotSurProduit(detrop));
        assertEquals(0, compter("SELECT count(*) FROM lot WHERE num_lot = 'LOT-F'"));
    }

    @Test
    @DisplayName("Numéro de lot et date de péremption sont exigés hors commande")
    void horsCommandeChampsObligatoires() {
        Produit produit = produitEnStock("CRESTOR", 50);
        viderLeCache();

        LotDTO sansNumero = new LotDTO().setProduitId(produit.getId()).setExpiryDate(LocalDate.now().plusMonths(3)).setQuantityReceived(5);
        LotDTO sansDate = new LotDTO().setProduitId(produit.getId()).setNumLot("LOT-G").setQuantityReceived(5);

        assertThrows(GenericError.class, () -> services.lotService.addLotSurProduit(sansNumero));
        assertThrows(GenericError.class, () -> services.lotService.addLotSurProduit(sansDate));
        assertEquals(0, compter("SELECT count(*) FROM lot WHERE produit_id = " + produit.getId()));
    }

    @Test
    @DisplayName("Modifier un lot réécrit sa quantité, son numéro et sa péremption")
    void modificationDUnLot() {
        Produit produit = produitEnStock("XARELTO", 100);
        Lot lot = lot(produit, "LOT-H", LocalDate.now().plusMonths(6), 20);
        viderLeCache();

        services.lotService.editLot(
            new LotDTO()
                .setId(lot.getId())
                .setNumLot("LOT-H-BIS")
                .setQuantityReceived(30)
                .setFreeQty(5)
                .setExpiryDate(LocalDate.now().plusMonths(9))
        );
        viderLeCache();

        Lot relu = em.find(Lot.class, lot.getId());
        assertEquals("LOT-H-BIS", relu.getNumLot());
        assertEquals(35, relu.getQuantity(), "la quantité inclut les unités gratuites");
        assertEquals(5, relu.getFreeQty());
        assertEquals(LocalDate.now().plusMonths(9), relu.getExpiryDate());
    }

    @Test
    @DisplayName("Supprimer un lot l'efface de la base")
    void suppressionDUnLot() {
        Produit produit = produitEnStock("TAHOR", 100);
        Lot lot = lot(produit, "LOT-I", LocalDate.now().plusMonths(6), 20);
        viderLeCache();

        services.lotService.remove(lot.getId());
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM lot WHERE id = " + lot.getId()));
    }

    @Test
    @DisplayName("La vente décrémente le lot et le solde quand il tombe à zéro")
    void venteDesLots() {
        Produit produit = produitEnStock("LEVOTHYROX", 100);
        Lot entame = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 20);
        Lot epuise = lot(produit, unique("L"), LocalDate.now().plusMonths(7), 5);
        viderLeCache();

        services.lotService.updateLots(
            List.of(
                new LotSold(entame.getId(), entame.getNumLot(), 8, entame.getExpiryDate()),
                new LotSold(epuise.getId(), epuise.getNumLot(), 5, epuise.getExpiryDate())
            )
        );
        viderLeCache();

        assertEquals(12, em.find(Lot.class, entame.getId()).getCurrentQuantity());
        assertEquals(StatutLot.AVAILABLE, em.find(Lot.class, entame.getId()).getStatut());
        assertEquals(0, em.find(Lot.class, epuise.getId()).getCurrentQuantity());
        assertEquals(StatutLot.SOLD, em.find(Lot.class, epuise.getId()).getStatut());
    }

    @Test
    @DisplayName("Une vente servie au-delà du lot ne le fait pas passer sous zéro")
    void venteSuperieureAuLot() {
        Produit produit = produitEnStock("INEXIUM", 100);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 3);
        viderLeCache();

        services.lotService.updateLots(List.of(new LotSold(lot.getId(), lot.getNumLot(), 10, lot.getExpiryDate())));
        viderLeCache();

        assertEquals(0, em.find(Lot.class, lot.getId()).getCurrentQuantity());
        assertEquals(StatutLot.SOLD, em.find(Lot.class, lot.getId()).getStatut());
    }

    @Test
    @DisplayName("L'annulation d'une vente rend sa quantité au lot et le remet disponible")
    void annulationDeVente() {
        Produit produit = produitEnStock("VENTOLINE", 100);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 10);
        services.lotService.updateLots(List.of(new LotSold(lot.getId(), lot.getNumLot(), 10, lot.getExpiryDate())));
        viderLeCache();

        services.lotService.restoreLots(List.of(new LotSold(lot.getId(), lot.getNumLot(), 4, lot.getExpiryDate())));
        viderLeCache();

        Lot relu = em.find(Lot.class, lot.getId());
        assertEquals(4, relu.getCurrentQuantity());
        assertEquals(StatutLot.AVAILABLE, relu.getStatut(), "le lot ressort du statut vendu");
    }

    @Test
    @DisplayName("Un ajustement négatif débite les lots en ordre de péremption")
    void ajustementNegatif() {
        Produit produit = produitEnStock("SERETIDE", 100);
        Lot proche = lot(produit, unique("L"), LocalDate.now().plusMonths(1), 6);
        Lot tardif = lot(produit, unique("L"), LocalDate.now().plusMonths(11), 10);
        viderLeCache();

        services.lotService.adjustLots(produit, -9);
        viderLeCache();

        assertEquals(0, em.find(Lot.class, proche.getId()).getCurrentQuantity(), "le lot proche est vidé en premier");
        assertEquals(StatutLot.SOLD, em.find(Lot.class, proche.getId()).getStatut());
        assertEquals(7, em.find(Lot.class, tardif.getId()).getCurrentQuantity());
    }

    @Test
    @DisplayName("Un ajustement positif crédite le lot reçu le plus récemment")
    void ajustementPositif() {
        Produit produit = produitEnStock("SYMBICORT", 100);
        Lot ancien = lot(produit, unique("L"), LocalDate.now().plusMonths(2), 5);
        Lot recent = lot(produit, unique("L"), LocalDate.now().plusMonths(10), 5);
        viderLeCache();

        services.lotService.adjustLots(produit, 12);
        viderLeCache();

        assertEquals(17, em.find(Lot.class, recent.getId()).getCurrentQuantity());
        assertEquals(5, em.find(Lot.class, ancien.getId()).getCurrentQuantity());
    }

    @Test
    @DisplayName("Créditer un lot désigné le réactive s'il était soldé")
    void creditDUnLotDesigne() {
        Produit produit = produitEnStock("FLIXOTIDE", 100);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 4);
        services.lotService.updateLots(List.of(new LotSold(lot.getId(), lot.getNumLot(), 4, lot.getExpiryDate())));
        viderLeCache();

        services.lotService.creditSpecificLot(em.find(Lot.class, lot.getId()), 6);
        viderLeCache();

        Lot relu = em.find(Lot.class, lot.getId());
        assertEquals(6, relu.getCurrentQuantity());
        assertEquals(StatutLot.AVAILABLE, relu.getStatut());
    }

    @Test
    @DisplayName("Les lots d'un produit se relisent triés par péremption, épuisés exclus")
    void lecturesDesLotsDUnProduit() {
        Produit produit = produitEnStock("ZYRTEC", 100);
        lot(produit, unique("L"), LocalDate.now().plusMonths(9), 10);
        Lot proche = lot(produit, unique("L"), LocalDate.now().plusMonths(2), 10);
        Lot vide = lot(produit, unique("L"), LocalDate.now().plusMonths(3), 10);
        services.lotService.updateLots(List.of(new LotSold(vide.getId(), vide.getNumLot(), 10, vide.getExpiryDate())));
        viderLeCache();

        List<Lot> lots = services.lotService.findProduitLots(produit.getId());

        assertEquals(2, lots.size(), "le lot épuisé sort de la liste");
        assertEquals(proche.getId(), lots.getFirst().getId(), "le plus proche de la péremption vient en tête");
    }

    @Test
    @DisplayName("Un lot se retrouve par son numéro pour un produit donné")
    void rechercheParNumeroDeLot() {
        Produit produit = produitEnStock("CLARITYNE", 100);
        Lot lot = lot(produit, "LOT-J", LocalDate.now().plusMonths(6), 10);
        Produit autre = produitEnStock("AERIUS", 100);
        lot(autre, "LOT-J", LocalDate.now().plusMonths(6), 10);
        viderLeCache();

        var trouve = services.lotService.findByProduitIdAndNumLot(produit.getId(), "LOT-J");

        assertTrue(trouve.isPresent());
        assertEquals(lot.getId(), trouve.orElseThrow().getId(), "le même numéro chez deux produits ne se confond pas");
    }

    // ===== outils =====

    private OrderLine ligneRecue(int quantiteRecue) {
        Produit produit = produitEnStock(unique("PRODUIT"), 200);
        FournisseurProduit fournisseurProduit = fournisseurProduit(produit);
        Commande commande = commande(fournisseurProduit.getFournisseur(), OrderStatut.RECEIVED);
        OrderLine ligne = ligneDeCommande(commande, fournisseurProduit, quantiteRecue, quantiteRecue);
        viderLeCache();
        return em.find(OrderLine.class, ligne.getId());
    }

    private LotDTO lotDe(OrderLine ligne, String numLot, int quantite) {
        return new LotDTO()
            .setReceiptItemId(ligne.getId())
            .setNumLot(numLot)
            .setExpiryDate(LocalDate.now().plusMonths(10))
            .setQuantityReceived(quantite)
            .setFreeQty(0);
    }
}
