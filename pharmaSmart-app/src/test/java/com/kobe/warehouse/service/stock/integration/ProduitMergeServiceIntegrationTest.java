package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.Rupture;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.Substitut;
import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeSubstitut;
import com.kobe.warehouse.service.dto.produit.merge.LotConflictAction;
import com.kobe.warehouse.service.dto.produit.merge.LotConflictDTO;
import com.kobe.warehouse.service.dto.produit.merge.LotResolutionDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergePreviewDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeRequestDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeResultDTO;
import com.kobe.warehouse.service.dto.produit.merge.StockConflictDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fusionner deux fiches en doublon, c'est réaffecter une dizaine de tables d'un produit vers un
 * autre. La moitié d'entre elles porte une contrainte d'unicité qui inclut {@code produit_id} —
 * {@code (storage, produit)} sur le stock, {@code (num_lot, produit)} sur les lots,
 * {@code (fournisseur, produit)}, {@code (rayon, produit)}, {@code (produit, mois)} sur les ventes
 * agrégées, {@code (produit, inventaire, emplacement)} sur les lignes d'inventaire. Réaffecter
 * naïvement une ligne du doublon alors que la cible en a déjà une pour la même clé fait échouer
 * toute la fusion sur une violation de contrainte.
 *
 * <p>Le service traite donc chaque table en distinguant deux cas : pas de collision, la ligne est
 * repointée ; collision, la ligne est supprimée, fusionnée ou laissée en place selon ce que
 * l'arbitrage automatique peut se permettre. Cette distinction ne se vérifie que contre les vraies
 * contraintes — et l'ordre feuilles → racine du traitement encore moins : c'est aussi pour cela
 * que le produit source est <b>désactivé</b> et jamais supprimé.
 */
@DisplayName("ProduitMergeService — fusion de fiches en doublon sur PostgreSQL")
class ProduitMergeServiceIntegrationTest extends AbstractStockIntegrationTest {

    // ===== simulation =====

    @Test
    @DisplayName("La simulation compte ce qui sera déplacé sans rien écrire")
    void simulationNeTouchePasALaBase() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        lotSurEmplacement(doublon, unique("LOT"), LocalDate.now().plusMonths(6), reserve, 4);
        rayonProduit(doublon, rayonSupplementaire());
        viderLeCache();

        ProduitMergePreviewDTO apercu = services.produitMergeService.preview(cible.getId(), List.of(doublon.getId()));

        assertEquals(List.of(doublon.getId()), apercu.sourceIds());
        assertTrue(apercu.rejectedSourceIds().isEmpty());
        assertEquals(1, apercu.entityCounts().get("stockProduit"));
        assertEquals(1, apercu.entityCounts().get("lot"));
        assertEquals(1, apercu.entityCounts().get("fournisseurProduit"));
        assertEquals(1, apercu.entityCounts().get("rayonProduit"));
        assertTrue(apercu.lotConflicts().isEmpty());
        assertTrue(apercu.stockConflicts().isEmpty(), "les deux fiches n'ont aucun emplacement en commun");

        viderLeCache();
        assertEquals(Status.ENABLE, em.find(Produit.class, doublon.getId()).getStatus(), "la simulation n'archive rien");
        assertEquals(
            doublon.getId().intValue(),
            compter("SELECT produit_id FROM stock_produit WHERE storage_id = %d AND produit_id = %d".formatted(STORAGE_RESERVE_ID, doublon.getId()))
        );
    }

    @Test
    @DisplayName("La simulation signale les collisions de lot et de stock à arbitrer")
    void simulationSignaleLesCollisions() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        lotSurEmplacement(cible, "LOT-COMMUN", LocalDate.now().plusMonths(6), rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        StockProduit stockDoublon = stock(doublon, rayon, 4);
        Lot lotDoublon = lotSurEmplacement(doublon, "LOT-COMMUN", LocalDate.now().plusMonths(9), rayon, 4);
        viderLeCache();

        ProduitMergePreviewDTO apercu = services.produitMergeService.preview(cible.getId(), List.of(doublon.getId()));

        assertEquals(1, apercu.lotConflicts().size());
        LotConflictDTO conflitLot = apercu.lotConflicts().getFirst();
        assertEquals("LOT-COMMUN", conflitLot.numLot());
        assertEquals(lotDoublon.getId(), conflitLot.sourceLotId());
        assertEquals(4, conflitLot.sourceQuantity());
        assertEquals(10, conflitLot.targetQuantity());

        assertEquals(1, apercu.stockConflicts().size());
        StockConflictDTO conflitStock = apercu.stockConflicts().getFirst();
        assertEquals(STORAGE_RAYON_ID, conflitStock.storageId());
        assertEquals(stockDoublon.getQtyStock(), conflitStock.sourceQtyStock());
        assertEquals(10, conflitStock.targetQtyStock());
    }

    @Test
    @DisplayName("Un produit introuvable ou la cible elle-même sont écartés de la simulation")
    void simulationEcarteLesSourcesInvalides() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        viderLeCache();

        ProduitMergePreviewDTO apercu = services.produitMergeService.preview(
            cible.getId(),
            List.of(cible.getId(), 999_999)
        );

        assertTrue(apercu.sourceIds().isEmpty(), "on ne fusionne pas un produit avec lui-même");
        assertEquals(List.of(999_999), apercu.rejectedSourceIds());
        assertTrue(apercu.rejectionReasons().get("999999").contains("introuvable"));
    }

    // ===== fusion : réaffectation sans collision =====

    @Test
    @DisplayName("Sans collision, tout le rattachement du doublon bascule sur la cible")
    void fusionSansCollision() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        Rayon rayonCible = rayonSupplementaire();
        rayonProduit(cible, rayonCible);

        Produit doublon = produit(unique("DOUBLON"));
        FournisseurProduit referenceDoublon = fournisseurProduit(doublon);
        StockProduit stockDoublon = stock(doublon, reserve, 4);
        Lot lotDoublon = lotSurEmplacement(doublon, unique("LOT"), LocalDate.now().plusMonths(6), reserve, 4);
        Rayon rayonDoublon = rayonSupplementaire();
        RayonProduit rangementDoublon = rayonProduit(doublon, rayonDoublon);
        venteMensuelle(doublon, "2026-01", 12, 120_000);
        semois(doublon);
        viderLeCache();

        ProduitMergeResultDTO resultat = services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(cible.getId(), resultat.targetId());
        assertEquals(List.of(doublon.getId()), resultat.mergedSourceIds());
        assertTrue(resultat.stockConflicts().isEmpty());

        assertEquals(cible.getId().intValue(), compter("SELECT produit_id FROM stock_produit WHERE id = " + stockDoublon.getId()));
        assertEquals(cible.getId().intValue(), compter("SELECT produit_id FROM lot WHERE id = " + lotDoublon.getId()));
        assertEquals(cible.getId().intValue(), compter("SELECT produit_id FROM fournisseur_produit WHERE id = " + referenceDoublon.getId()));
        assertEquals(cible.getId().intValue(), compter("SELECT produit_id FROM rayon_produit WHERE id = " + rangementDoublon.getId()));
        assertEquals(1, compter("SELECT COUNT(*) FROM ventes_mensuelles_agregees WHERE produit_id = " + cible.getId()));
        assertEquals(1, compter("SELECT COUNT(*) FROM semois_configuration WHERE produit_id = " + cible.getId()));

        assertEquals(Status.DISABLE, em.find(Produit.class, doublon.getId()).getStatus(), "le doublon est archivé, jamais supprimé");
        assertEquals(1, compter("SELECT COUNT(*) FROM produit WHERE id = " + doublon.getId()));
    }

    @Test
    @DisplayName("Les entités sans contrainte d'unicité sont réaffectées en bloc")
    void reaffectationEnBlocDesEntitesSimples() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        rupture(doublon);
        rupture(doublon);
        viderLeCache();

        ProduitMergeResultDTO resultat = services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(2, resultat.entityCounts().get("Rupture"));
        assertEquals(2, compter("SELECT COUNT(*) FROM rupture WHERE produit_id = " + cible.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM rupture WHERE produit_id = " + doublon.getId()));
    }

    // ===== fusion : collisions =====

    @Test
    @DisplayName("Un emplacement commun laisse les deux stocks en place et remonte un écart à arbitrer")
    void collisionDeStockSignaleeSansFusion() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        StockProduit stockCible = stock(cible, rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        StockProduit stockDoublon = stock(doublon, rayon, 4);
        viderLeCache();

        ProduitMergeResultDTO resultat = services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(1, resultat.stockConflicts().size());
        assertEquals(10, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockCible.getId()), "la cible n'est jamais créditée en douce");
        assertEquals(
            doublon.getId().intValue(),
            compter("SELECT produit_id FROM stock_produit WHERE id = " + stockDoublon.getId()),
            "la ligne du doublon reste chez lui, avec son historique d'ajustement"
        );
    }

    @Test
    @DisplayName("Un lot de même numéro est additionné sur demande, et le lot du doublon disparaît")
    void collisionDeLotResolueParFusion() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        Lot lotCible = lotSurEmplacement(cible, "LOT-COMMUN", LocalDate.now().plusMonths(6), rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        Lot lotDoublon = lotSurEmplacement(doublon, "LOT-COMMUN", LocalDate.now().plusMonths(9), reserve, 4);
        viderLeCache();

        services.produitMergeService.merge(
            new ProduitMergeRequestDTO(
                cible.getId(),
                List.of(doublon.getId()),
                List.of(new LotResolutionDTO(lotDoublon.getId(), LotConflictAction.MERGE))
            )
        );
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM lot WHERE id = " + lotDoublon.getId()), "le lot du doublon a disparu");
        assertEquals(14, compter("SELECT quantity FROM lot WHERE id = " + lotCible.getId()));
        assertEquals(14, compter("SELECT current_quantity FROM lot WHERE id = " + lotCible.getId()));
        assertEquals(1, compter("SELECT COUNT(*) FROM lot WHERE num_lot = 'LOT-COMMUN'"), "la contrainte (num_lot, produit) tient");
    }

    @Test
    @DisplayName("Un lot de même numéro peut aussi être jeté sans reporter sa quantité")
    void collisionDeLotResolueParSuppression() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        Lot lotCible = lotSurEmplacement(cible, "LOT-COMMUN", LocalDate.now().plusMonths(6), rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        Lot lotDoublon = lotSurEmplacement(doublon, "LOT-COMMUN", LocalDate.now().plusMonths(9), reserve, 4);
        viderLeCache();

        services.produitMergeService.merge(
            new ProduitMergeRequestDTO(
                cible.getId(),
                List.of(doublon.getId()),
                List.of(new LotResolutionDTO(lotDoublon.getId(), LotConflictAction.DELETE))
            )
        );
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM lot WHERE id = " + lotDoublon.getId()));
        assertEquals(10, compter("SELECT quantity FROM lot WHERE id = " + lotCible.getId()), "la quantité du doublon n'est pas reportée");
    }

    @Test
    @DisplayName("Un conflit de lot laissé sans arbitrage bloque la fusion avant toute écriture")
    void conflitDeLotNonArbitre() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        lotSurEmplacement(cible, "LOT-COMMUN", LocalDate.now().plusMonths(6), rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        StockProduit stockDoublon = stock(doublon, reserve, 4);
        lotSurEmplacement(doublon, "LOT-COMMUN", LocalDate.now().plusMonths(9), reserve, 4);
        viderLeCache();

        ProduitMergeRequestDTO demande = fusion(cible, doublon);

        BadRequestAlertException erreur = assertThrows(
            BadRequestAlertException.class,
            () -> services.produitMergeService.merge(demande)
        );
        assertTrue(erreur.getMessage().contains("Conflit de lot non résolu"));

        viderLeCache();
        assertEquals(Status.ENABLE, em.find(Produit.class, doublon.getId()).getStatus(), "rien n'a été engagé");
        assertEquals(doublon.getId().intValue(), compter("SELECT produit_id FROM stock_produit WHERE id = " + stockDoublon.getId()));
    }

    @Test
    @DisplayName("Un même fournisseur des deux côtés : la référence du doublon part et ses commandes suivent")
    void collisionDeFournisseur() {
        Fournisseur fournisseur = fournisseurPartage();
        Produit cible = produit(unique("CIBLE"));
        FournisseurProduit referenceCible = referenceChez(cible, fournisseur);
        stock(cible, rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        FournisseurProduit referenceDoublon = referenceChez(doublon, fournisseur);
        stock(doublon, reserve, 4);
        ligneDeCommande(commande(fournisseur, OrderStatut.REQUESTED), referenceDoublon, 10, 0);
        viderLeCache();

        services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM fournisseur_produit WHERE id = " + referenceDoublon.getId()));
        assertEquals(
            1,
            compter("SELECT COUNT(*) FROM order_line WHERE fournisseur_produit_id = " + referenceCible.getId()),
            "la commande passée sur le doublon pointe désormais la référence conservée"
        );
        assertNull(
            em.find(Produit.class, doublon.getId()).getFournisseurProduitPrincipal(),
            "le fournisseur principal du doublon est vidé : la colonne est unique"
        );
    }

    @Test
    @DisplayName("Un rangement au même rayon des deux côtés : celui du doublon est jeté")
    void collisionDeRayon() {
        Rayon rayonCommun = rayonSupplementaire();
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        rayonProduit(cible, rayonCommun);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        RayonProduit rangementDoublon = rayonProduit(doublon, rayonCommun);
        viderLeCache();

        services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM rayon_produit WHERE id = " + rangementDoublon.getId()));
        assertEquals(1, compter("SELECT COUNT(*) FROM rayon_produit WHERE produit_id = %d AND rayon_id = %d".formatted(cible.getId(), rayonCommun.getId())));
    }

    @Test
    @DisplayName("Les ventes agrégées du même mois sont additionnées, les autres mois sont repris")
    void fusionDesVentesMensuelles() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        venteMensuelle(cible, "2026-01", 10, 100_000);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        venteMensuelle(doublon, "2026-01", 3, 30_000);
        venteMensuelle(doublon, "2026-02", 7, 70_000);
        viderLeCache();

        services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(2, compter("SELECT COUNT(*) FROM ventes_mensuelles_agregees WHERE produit_id = " + cible.getId()));
        assertEquals(
            13,
            compter("SELECT quantite_vendue FROM ventes_mensuelles_agregees WHERE produit_id = %d AND annee_mois = '2026-01'".formatted(cible.getId()))
        );
        assertEquals(
            130_000,
            compter("SELECT montant_ca FROM ventes_mensuelles_agregees WHERE produit_id = %d AND annee_mois = '2026-01'".formatted(cible.getId()))
        );
        assertEquals(
            7,
            compter("SELECT quantite_vendue FROM ventes_mensuelles_agregees WHERE produit_id = %d AND annee_mois = '2026-02'".formatted(cible.getId()))
        );
        assertEquals(0, compter("SELECT COUNT(*) FROM ventes_mensuelles_agregees WHERE produit_id = " + doublon.getId()));
    }

    @Test
    @DisplayName("La configuration SEMOIS de la cible prévaut sur celle du doublon")
    void configurationSemoisDeLaCiblePrevaut() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        SemoisConfiguration configurationCible = semois(cible);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        SemoisConfiguration configurationDoublon = semois(doublon);
        viderLeCache();

        services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(1, compter("SELECT COUNT(*) FROM semois_configuration WHERE id = " + configurationCible.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM semois_configuration WHERE id = " + configurationDoublon.getId()));
    }

    @Test
    @DisplayName("Une ligne d'inventaire en collision reste au doublon pour arbitrage manuel")
    void collisionDeLigneDInventaire() {
        StoreInventory inventaire = inventaire();

        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        ligneDInventaire(inventaire, cible, rayon);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        StoreInventoryLine ligneEnCollision = ligneDInventaire(inventaire, doublon, rayon);
        StoreInventoryLine ligneLibre = ligneDInventaire(inventaire, doublon, reserve);
        viderLeCache();

        ProduitMergeResultDTO resultat = services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(1, resultat.entityCounts().get("storeInventoryLine"), "seule la ligne sans collision est reprise");
        assertEquals(cible.getId().intValue(), compter("SELECT produit_id FROM store_inventory_line WHERE id = " + ligneLibre.getId()));
        assertEquals(
            doublon.getId().intValue(),
            compter("SELECT produit_id FROM store_inventory_line WHERE id = " + ligneEnCollision.getId()),
            "la ligne en collision n'est pas déplacée : quelle quantité comptée retenir ?"
        );
    }

    @Test
    @DisplayName("Les substituts croisés entre les deux fiches sont supprimés, les autres suivent la cible")
    void fusionDesSubstituts() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);

        Produit tiers = produit(unique("TIERS"));
        fournisseurProduit(tiers);
        stock(tiers, rayon, 5);

        Substitut croise = substitut(doublon, cible);
        Substitut versLeTiers = substitut(doublon, tiers);
        viderLeCache();

        services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM substitut WHERE id = " + croise.getId()), "un produit n'est pas son propre substitut");
        assertEquals(cible.getId().intValue(), compter("SELECT produit_id FROM substitut WHERE id = " + versLeTiers.getId()));
    }

    // ===== refus =====

    @Test
    @DisplayName("Une demande sans cible ou sans source est refusée")
    void demandeIncomplete() {
        assertThrows(
            BadRequestAlertException.class,
            () -> services.produitMergeService.merge(new ProduitMergeRequestDTO(null, List.of(1), List.of()))
        );
        assertThrows(
            BadRequestAlertException.class,
            () -> services.produitMergeService.merge(new ProduitMergeRequestDTO(1, List.of(), List.of()))
        );
    }

    @Test
    @DisplayName("Fusionner un produit avec lui-même ne laisse aucune source valide")
    void fusionAvecSoiMeme() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        viderLeCache();

        ProduitMergeRequestDTO demande = new ProduitMergeRequestDTO(cible.getId(), List.of(cible.getId()), List.of());

        BadRequestAlertException erreur = assertThrows(
            BadRequestAlertException.class,
            () -> services.produitMergeService.merge(demande)
        );
        assertTrue(erreur.getMessage().contains("Aucun produit source valide"));
    }

    @Test
    @DisplayName("Une cible portant plusieurs produits détail est refusée d'emblée")
    void cibleAvecPlusieursDetails() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        detailDe(cible);
        detailDe(cible);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        viderLeCache();

        ProduitMergeRequestDTO demande = fusion(cible, doublon);

        BadRequestAlertException erreur = assertThrows(
            BadRequestAlertException.class,
            () -> services.produitMergeService.merge(demande)
        );
        assertTrue(erreur.getMessage().contains("plusieurs produits détail"));

        ProduitMergePreviewDTO apercu = services.produitMergeService.preview(cible.getId(), List.of(doublon.getId()));
        assertTrue(apercu.sourceIds().isEmpty());
        assertEquals(List.of(doublon.getId()), apercu.rejectedSourceIds());
    }

    // ===== produits détail =====

    @Test
    @DisplayName("Le produit détail d'un doublon est re-parenté quand la cible n'en a pas")
    void detailReparente() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        Produit detail = detailDe(doublon);
        viderLeCache();

        ProduitMergeResultDTO resultat = services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(1, resultat.entityCounts().get("produitDetailReparente"));
        assertEquals(cible.getId().intValue(), compter("SELECT parent_id FROM produit WHERE id = " + detail.getId()));
        assertEquals(Status.ENABLE, em.find(Produit.class, detail.getId()).getStatus(), "le détail re-parenté reste actif");
    }

    @Test
    @DisplayName("Quand les deux fiches ont un détail, celui du doublon est archivé sous la cible")
    void detailFusionne() {
        Produit cible = produit(unique("CIBLE"));
        fournisseurProduit(cible);
        stock(cible, rayon, 10);
        Produit detailCible = detailDe(cible);

        Produit doublon = produit(unique("DOUBLON"));
        fournisseurProduit(doublon);
        stock(doublon, reserve, 4);
        Produit detailDoublon = detailDe(doublon);
        viderLeCache();

        ProduitMergeResultDTO resultat = services.produitMergeService.merge(fusion(cible, doublon));
        viderLeCache();

        assertEquals(1, resultat.entityCounts().get("produitDetailFusionne"));
        assertEquals(
            Status.DISABLE,
            em.find(Produit.class, detailDoublon.getId()).getStatus(),
            "jamais deux produits détail actifs sous la même boîte"
        );
        assertEquals(Status.ENABLE, em.find(Produit.class, detailCible.getId()).getStatus());
    }

    // ===== fabriques du jeu d'essai propres à la fusion =====

    private ProduitMergeRequestDTO fusion(Produit cible, Produit doublon) {
        return new ProduitMergeRequestDTO(cible.getId(), List.of(doublon.getId()), List.of());
    }

    private Fournisseur fournisseurPartage() {
        Fournisseur fournisseur = new Fournisseur();
        String code = unique("FRS");
        fournisseur.setLibelle("FOURNISSEUR " + code);
        fournisseur.setCode(code);
        em.persist(fournisseur);
        em.flush();
        return fournisseur;
    }

    /** Une référence chez un fournisseur imposé : c'est ce qui crée la collision à la fusion. */
    private FournisseurProduit referenceChez(Produit produit, Fournisseur fournisseur) {
        FournisseurProduit reference = new FournisseurProduit();
        reference.setProduit(produit);
        reference.setFournisseur(fournisseur);
        reference.setCodeCip(unique("CIP"));
        reference.setPrixAchat(produit.getCostAmount());
        reference.setPrixUni(produit.getRegularUnitPrice());
        em.persist(reference);
        produit.setFournisseurProduitPrincipal(reference);
        em.flush();
        return reference;
    }

    private Rayon rayonSupplementaire() {
        Rayon nouveauRayon = new Rayon();
        nouveauRayon.setCode(unique("RAY"));
        nouveauRayon.setLibelle("RAYON " + unique("L"));
        nouveauRayon.setStorage(rayon);
        nouveauRayon.setExclude(false);
        em.persist(nouveauRayon);
        em.flush();
        return nouveauRayon;
    }

    private RayonProduit rayonProduit(Produit produit, Rayon rayonCible) {
        RayonProduit rangement = new RayonProduit().setProduit(produit).setRayon(rayonCible);
        em.persist(rangement);
        em.flush();
        return rangement;
    }

    private Substitut substitut(Produit produit, Produit remplacant) {
        Substitut substitut = new Substitut();
        substitut.setProduit(produit);
        substitut.setSubstitut(remplacant);
        substitut.setType(TypeSubstitut.GENERIQUE);
        em.persist(substitut);
        em.flush();
        return substitut;
    }

    private VentesMensuellesAgregees venteMensuelle(Produit produit, String anneeMois, int quantite, int montant) {
        VentesMensuellesAgregees ventes = new VentesMensuellesAgregees();
        ventes.setProduit(produit);
        ventes.setAnneeMois(anneeMois);
        ventes.setQuantiteVendue(quantite);
        ventes.setMontantCa(montant);
        ventes.setNombreVentes(1);
        em.persist(ventes);
        em.flush();
        return ventes;
    }

    private SemoisConfiguration semois(Produit produit) {
        SemoisConfiguration configuration = new SemoisConfiguration();
        configuration.setProduit(produit);
        em.persist(configuration);
        em.flush();
        return configuration;
    }

    /** Une rupture : l'archétype des entités réaffectées en bloc, sans contrainte d'unicité. */
    private void rupture(Produit produit) {
        Rupture rupture = new Rupture();
        rupture.setProduit(produit);
        rupture.setFournisseur(fournisseurPartage());
        rupture.setQty(5);
        rupture.setDateMtv(LocalDateTime.now());
        em.persist(rupture);
        em.flush();
    }

    private StoreInventory inventaire() {
        StoreInventory inventaire = new StoreInventory();
        inventaire.setUser(utilisateur);
        inventaire.setCreatedAt(LocalDateTime.now());
        inventaire.setUpdatedAt(LocalDateTime.now());
        inventaire.setInventoryValueCostBegin(0L);
        inventaire.setInventoryAmountBegin(0L);
        inventaire.setInventoryValueCostAfter(0L);
        inventaire.setInventoryAmountAfter(0L);
        em.persist(inventaire);
        em.flush();
        return inventaire;
    }

    private StoreInventoryLine ligneDInventaire(StoreInventory inventaire, Produit produit, Storage storage) {
        StoreInventoryLine ligne = new StoreInventoryLine();
        ligne.setStoreInventory(inventaire);
        ligne.setProduit(produit);
        ligne.setStorage(storage);
        ligne.setQuantityInit(0);
        ligne.setQuantityOnHand(0);
        ligne.setInventoryValueCost(0);
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        em.flush();
        return ligne;
    }

    /** Un produit détail (déconditionné) rattaché à sa boîte. */
    private Produit detailDe(Produit boite) {
        Produit detail = produit(unique("DETAIL"));
        detail.setParent(boite);
        em.flush();
        return detail;
    }
}
