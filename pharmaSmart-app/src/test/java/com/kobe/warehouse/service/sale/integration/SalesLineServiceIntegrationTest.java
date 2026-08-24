package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.QuantitySoldException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.errors.StockInReserveException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.sale.SalesLineService} sur un vrai PostgreSQL.
 *
 * <p>C'est le service qui touche au stock : il lit le disponible rayon par une agrégation SQL,
 * décide de la quantité servie, puis décrémente {@code qty_stock} et {@code qty_ug} séparément.
 * Ces deux compartiments sont la source de la plupart des écarts d'inventaire ; les vérifier sur
 * un double mémoire ne dirait rien de ce que la base contient après coup.
 */
@DisplayName("SalesLineService — lignes de vente et stock sur PostgreSQL")
class SalesLineServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("La ligne créée reprend le prix, la TVA et le coût du produit")
    void creationDeLigne() {
        Produit produit = produitEnStock("KETOPROFENE", 1_200, 700, 18, 10);

        SalesLine ligne = services.salesLineService.createSaleLineFromDTO(ligneDe(produit, 3), STORAGE_RAYON_ID);

        assertEquals(18, ligne.getTaxValue(), "le taux vient de la TVA du produit, pas du DTO");
        assertEquals(700, ligne.getCostAmount());
        assertEquals(3, ligne.getQuantitySold(), "le stock couvre la demande");
        assertEquals(3_600, ligne.getSalesAmount());
        assertEquals(3_600, ligne.getAmountToBeTakenIntoAccount());
    }

    @Test
    @DisplayName("Une demande au-delà du stock rayon est refusée sans forçage")
    void demandeSuperieureAuStock() {
        Produit produit = produitEnStock("BETADINE", 900, 500, 0, 2);
        SaleLineDTO dto = ligneDe(produit, 5);

        assertThrows(StockException.class, () -> services.salesLineService.createSaleLineFromDTO(dto, STORAGE_RAYON_ID));
    }

    @Test
    @DisplayName("Avec forçage, la quantité servie est plafonnée au stock disponible")
    void forcageDuStock() {
        Produit produit = produitEnStock("SERUM PHY", 300, 150, 0, 2);
        SaleLineDTO dto = ligneDe(produit, 5);
        dto.setForceStock(true);

        SalesLine ligne = services.salesLineService.createSaleLineFromDTO(dto, STORAGE_RAYON_ID);

        assertEquals(5, ligne.getQuantityRequested());
        assertEquals(2, ligne.getQuantitySold(), "on ne sert que ce qui existe");
    }

    @Test
    @DisplayName("Le rayon insuffisant alors que la réserve est garnie renvoie l'écart au frontal")
    void stockEnReserve() {
        Produit produit = produitEnStock("AUGMENTIN", 4_000, 2_500, 0, 1);
        stock(produit, reserve, 10, 0);
        em.flush();
        when(services.storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);

        SaleLineDTO dto = ligneDe(produit, 4);
        StockInReserveException echec = assertThrows(
            StockInReserveException.class,
            () -> services.salesLineService.createSaleLineFromDTO(dto, STORAGE_RAYON_ID)
        );

        assertNotNull(echec, "le frontal doit pouvoir proposer le transfert plutôt que d'échouer");
    }

    @Test
    @DisplayName("Le forçage déclenche le transfert implicite réserve → rayon, du strict manquant")
    void transfertImpliciteDepuisLaReserve() {
        Produit produit = produitEnStock("CLAMOXYL", 3_000, 1_800, 0, 1);
        stock(produit, reserve, 10, 0);
        em.flush();
        when(services.storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);

        SaleLineDTO dto = ligneDe(produit, 4);
        dto.setForceStock(true);
        SalesLine ligne = services.salesLineService.createSaleLineFromDTO(dto, STORAGE_RAYON_ID);

        verify(services.repartitionStockService).transfertImpliciteReserveVersRayon(
            eq(produit.getId()),
            eq(STORAGE_RAYON_ID),
            eq(STORAGE_RESERVE_ID),
            eq(3)
        );
        assertEquals(4, ligne.getQuantitySold(), "1 en rayon + 3 transférés couvrent la demande");
    }

    @Test
    @DisplayName("Les unités gratuites du rayon sont servies en priorité et décomptées à part")
    void unitesGratuites() {
        Produit produit = produit("MULTIVITAMINES", 2_000, 1_100, 0);
        StockProduit stockProduit = stock(produit, rayon, 10, 3);
        em.flush();

        SaleLineDTO dto = ligneDe(produit, 5);
        dto.setQuantitySold(5);
        SalesLine ligne = services.salesLineService.createSaleLineFromDTO(dto, STORAGE_RAYON_ID);
        ligne.setSales(venteActive());
        services.salesLineService.save(Set.of(ligne), caissier, STORAGE_RAYON_ID);
        viderLeCache();

        StockProduit relu = em.find(StockProduit.class, stockProduit.getId());
        assertEquals(3, ligne.getQuantityUg(), "les 3 UG disponibles partent d'abord");
        assertEquals(8, relu.getQtyStock(), "10 − (5 servies − 3 UG)");
        assertEquals(0, relu.getQtyUG(), "les UG consommées quittent leur compartiment");
    }

    @Test
    @DisplayName("La clôture inscrit le stock avant/après sur la ligne et ouvre un avoir si besoin")
    void enregistrementEtAvoir() {
        Produit produit = produitEnStock("VOGALENE", 1_500, 900, 0, 2);
        SaleLineDTO dto = ligneDe(produit, 5);
        dto.setForceStock(true);
        SalesLine ligne = services.salesLineService.createSaleLineFromDTO(dto, STORAGE_RAYON_ID);
        ligne.setSales(venteActive());

        services.salesLineService.save(Set.of(ligne), caissier, STORAGE_RAYON_ID);
        viderLeCache();

        SalesLine relue = em.find(SalesLine.class, ligne.getId());
        assertEquals(2, relue.getInitStock());
        assertEquals(-3, relue.getAfterStock(), "5 demandées sur 2 disponibles");
        assertEquals(3, relue.getQuantityAvoir(), "l'écart part en avoir client");
        assertEquals(
            1,
            compter("SELECT count(*) FROM avoir_client WHERE sales_line_id = " + ligne.getId().getId()),
            "et l'avoir correspondant est bien écrit en base"
        );
    }

    @Test
    @DisplayName("Incrémenter la quantité cumule sur l'existant et réécrit la ligne")
    void incrementDeQuantite() {
        Produit produit = produitEnStock("SMECTA", 600, 350, 0, 20);
        CashSale vente = venteActive();
        SalesLine ligne = ligneEnBase(vente, produit, 2);

        SaleLineDTO increment = ligneDe(produit, 3);
        increment.setSaleLineId(ligne.getId());
        services.salesLineService.incrementItemQuantityRequested(increment, ligne, STORAGE_RAYON_ID);
        services.saleService.upddateCashSaleAmounts(vente);
        viderLeCache();

        SalesLine relue = em.find(SalesLine.class, ligne.getId());
        assertEquals(5, relue.getQuantityRequested());
        assertEquals(3_000, relue.getSalesAmount(), "5 × 600, recalculé et persisté");
    }

    @Test
    @DisplayName("Remplacer la quantité ne cumule pas")
    void remplacementDeQuantite() {
        Produit produit = produitEnStock("GAVISCON", 2_200, 1_300, 0, 20);
        CashSale vente = venteActive();
        SalesLine ligne = ligneEnBase(vente, produit, 4);

        SaleLineDTO remplacement = ligneDe(produit, 2);
        remplacement.setSaleLineId(ligne.getId());
        services.salesLineService.updateItemQuantityRequested(remplacement, ligne, STORAGE_RAYON_ID);
        services.saleService.upddateCashSaleAmounts(vente);
        viderLeCache();

        SalesLine relue = em.find(SalesLine.class, ligne.getId());
        assertEquals(2, relue.getQuantityRequested());
        assertEquals(4_400, relue.getSalesAmount());
    }

    /**
     * Une dépendance d'ordre qu'il vaut mieux nommer que découvrir en production.
     *
     * <p>Baisser la quantité ou le prix laisse {@code amount_to_be_taken_into_account} à sa valeur
     * d'avant : le service de ligne ne le recalcule jamais. Or la contrainte
     * {@code sales_line_declarable_ck} exige {@code montant déclarable <= quantité × prix}. Tant que
     * la ligne n'est pas revenue sous les yeux de {@code SaleCommonService#updateAmounts} — ce que
     * fait {@code SalesManager} après chaque modification — l'état en mémoire est refusé par la
     * base. Écrire la ligne seule, sans repasser par le calcul de la vente, produit donc un flush en
     * échec.
     */
    @Test
    @DisplayName("Le montant déclarable d'une ligne n'est réétabli qu'au recalcul de la vente")
    void montantDeclarableReetabliAuNiveauDeLaVente() {
        Produit produit = produitEnStock("TOPLEXIL", 3_000, 1_800, 0, 20);
        CashSale vente = venteActive();
        SalesLine ligne = ligneEnBase(vente, produit, 4);
        assertEquals(12_000, ligne.getAmountToBeTakenIntoAccount());

        SaleLineDTO baisse = ligneDe(produit, 1);
        baisse.setSaleLineId(ligne.getId());
        services.salesLineService.updateItemQuantityRequested(baisse, ligne, STORAGE_RAYON_ID);

        assertEquals(3_000, ligne.getSalesAmount(), "le montant de la ligne suit la nouvelle quantité");
        assertEquals(12_000, ligne.getAmountToBeTakenIntoAccount(), "mais pas encore le montant déclarable");

        services.saleService.upddateCashSaleAmounts(vente);
        viderLeCache();

        assertEquals(3_000, em.find(SalesLine.class, ligne.getId()).getAmountToBeTakenIntoAccount());
    }

    @Test
    @DisplayName("Servir plus que demandé est refusé")
    void quantiteServieSuperieureALaDemande() {
        Produit produit = produitEnStock("NUROFEN", 1_000, 600, 0, 20);
        SalesLine ligne = ligneEnBase(venteActive(), produit, 2);

        SaleLineDTO dto = ligneDe(produit, 2);
        dto.setQuantitySold(5);

        assertThrows(
            QuantitySoldException.class,
            () -> services.salesLineService.updateItemQuantitySold(ligne, dto, STORAGE_RAYON_ID)
        );
    }

    @Test
    @DisplayName("Changer le prix unitaire réécrit le montant de la ligne en base")
    void changementDePrix() {
        Produit produit = produitEnStock("CREME HYDRATANTE", 5_000, 3_000, 0, 10);
        CashSale vente = venteActive();
        SalesLine ligne = ligneEnBase(vente, produit, 2);

        SaleLineDTO dto = ligneDe(produit, 2);
        dto.setRegularUnitPrice(4_000);
        services.salesLineService.updateItemRegularPrice(dto, ligne, STORAGE_RAYON_ID);
        services.saleService.upddateCashSaleAmounts(vente);
        viderLeCache();

        SalesLine relue = em.find(SalesLine.class, ligne.getId());
        assertEquals(4_000, relue.getRegularUnitPrice());
        assertEquals(8_000, relue.getSalesAmount());
    }

    @Test
    @DisplayName("Les lignes d'une vente sont rendues triées par libellé de produit")
    void lecutreTrieeParLibelle() {
        CashSale vente = venteActive();
        Produit zinc = produitEnStock("ZINC", 500, 300, 0, 10);
        Produit aspirine = produitEnStock("ASPIRINE UPSA", 700, 400, 0, 10);
        ligneEnBase(vente, zinc, 1);
        ligneEnBase(vente, aspirine, 1);
        viderLeCache();

        List<SaleLineDTO> lignes = services.salesLineService.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(
            vente.getId().getId(),
            vente.getSaleDate()
        );

        assertEquals(List.of("ASPIRINE UPSA", "ZINC"), lignes.stream().map(SaleLineDTO::getProduitLibelle).toList());
    }

    @Test
    @DisplayName("On retrouve la ligne d'une vente par son produit")
    void rechercheParProduit() {
        CashSale vente = venteActive();
        Produit produit = produitEnStock("PRIMPERAN", 800, 500, 0, 10);
        ligneEnBase(vente, produit, 2);
        viderLeCache();

        assertTrue(services.salesLineService.findBySalesIdAndProduitId(vente.getId(), produit.getId()).isPresent());
    }

    @Test
    @DisplayName("Les identifiants de ligne viennent d'une séquence Postgres et ne se répètent pas")
    void identifiantsIssusDeLaSequence() {
        long premier = services.salesLineService.getNextId();
        long second = services.salesLineService.getNextId();

        assertTrue(second > premier, premier + " puis " + second);
    }

    @Test
    @DisplayName("La suppression d'une ligne l'efface bien de la table partitionnée")
    void suppressionDeLigne() {
        CashSale vente = venteActive();
        Produit produit = produitEnStock("DAFALGAN", 900, 500, 0, 10);
        SalesLine ligne = ligneEnBase(vente, produit, 1);
        viderLeCache();

        services.salesLineService.deleteSaleLine(em.find(SalesLine.class, ligne.getId()));
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + vente.getId().getId()));
    }

    @Test
    @DisplayName("Le clonage produit des lignes neuves, rattachées à la copie")
    void clonageDesLignes() {
        CashSale vente = venteActive();
        Produit produit = produitEnStock("LEVOTHYROX", 1_100, 700, 0, 10);
        SalesLine ligne = ligneEnBase(vente, produit, 2);
        CashSale copie = venteActive();

        Set<SalesLine> clones = services.salesLineService.cloneSalesLine(Set.of(ligne), copie);
        services.salesLineService.saveAll(clones);
        viderLeCache();

        assertEquals(1, clones.size());
        SalesLine clone = clones.iterator().next();
        assertTrue(clone.getId().getId() != ligne.getId().getId(), "le clone a son propre identifiant");
        assertEquals(1, compter("SELECT count(*) FROM sales_line WHERE sales_id = " + copie.getId().getId()));
    }

    // ===== outils =====

    private SalesLine ligneEnBase(CashSale vente, Produit produit, int quantite) {
        SalesLine ligne = services.salesLineService.createSaleLineFromDTO(ligneDe(produit, quantite), STORAGE_RAYON_ID);
        ligne.setSales(vente);
        vente.getSalesLines().add(ligne);
        services.salesLineService.saveSalesLine(ligne);
        em.flush();
        return ligne;
    }

    private CashSale venteActive() {
        CashSale vente = new CashSale();
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("IT" + vente.getId().getId());
        vente.setStatut(SalesStatut.ACTIVE);
        vente.setPaymentStatus(PaymentStatus.IMPAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(LocalDateTime.now());
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setAmountToBeTakenIntoAccount(0);
        vente.setUser(caissier);
        vente.setSeller(caissier);
        vente.setCaissier(caissier);
        vente.setMagasin(magasin);
        em.persist(vente);
        em.flush();
        return vente;
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
