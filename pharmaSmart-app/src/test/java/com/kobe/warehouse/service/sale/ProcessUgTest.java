package com.kobe.warehouse.service.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.sale.impl.SalesLineServiceBaseImpl;
import com.kobe.warehouse.service.sale.impl.StockUpdateService;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les unités gratuites reçues d'un fournisseur sont vendues comme les autres, mais elles ne coûtent
 * rien : la part d'UG consommée par une ligne de vente sort d'un stock distinct et pèse sur la marge.
 * Elle vaut donc {@code min(quantité vendue, UG disponibles)} — jamais davantage.
 *
 * <p>La quantité vendue qui fait foi est celle que le serveur calcule, écrêtée au stock réellement
 * disponible, et non celle que porte la requête : le champ est facultatif côté client, et le
 * serveur le recalcule à chaque modification de ligne.
 */
@DisplayName("SalesLineService — part d'unités gratuites d'une ligne de vente")
class ProcessUgTest {

    private final StockProduitRepository stockProduitRepository = mock(StockProduitRepository.class);

    private final SalesLineService service = new SalesLineServiceBaseImpl(
        mock(ProduitRepository.class),
        mock(SalesLineRepository.class),
        stockProduitRepository,
        mock(LotService.class),
        mock(InventoryTransactionService.class),
        mock(SaleLineIdGeneratorService.class),
        mock(StockUpdateService.class),
        mock(StorageService.class),
        mock(RepartitionStockService.class),
        mock(LotStockLocationService.class),
        mock(AvoirClientDocumentService.class),
        mock(DataMatrixParserService.class),
        mock(com.kobe.warehouse.service.AjustementService.class)
    );

    @Test
    @DisplayName("on ne consomme jamais plus d'UG qu'il n'y en a en stock")
    void bornePaLesUgDisponibles() {
        stockAvecUg(3);
        SalesLine ligne = ligneVendue(10);

        service.processUg(ligne, dto(null), 1);

        assertThat(ligne.getQuantityUg()).isEqualTo(3);
    }

    @Test
    @DisplayName("on ne consomme pas plus d'UG que d'unités vendues")
    void bornePaLaQuantiteVendue() {
        stockAvecUg(10);
        SalesLine ligne = ligneVendue(3);

        service.processUg(ligne, dto(null), 1);

        assertThat(ligne.getQuantityUg()).isEqualTo(3);
    }

    /**
     * Le cas qui cassait : le champ est facultatif côté client et absent à la création d'une ligne.
     * On écrivait alors {@code null} dans une colonne non nulle, et l'insertion échouait au flush.
     */
    @Test
    @DisplayName("une quantité absente de la requête ne trouble pas le calcul")
    void quantiteAbsenteDeLaRequete() {
        stockAvecUg(10);
        SalesLine ligne = ligneVendue(4);

        service.processUg(ligne, dto(null), 1);

        assertThat(ligne.getQuantityUg()).isEqualTo(4);
    }

    /**
     * Le serveur écrête la quantité vendue au stock disponible ; une requête qui en annonce
     * davantage ne doit pas faire consommer d'UG au-delà.
     */
    @Test
    @DisplayName("une quantité surévaluée par la requête est ignorée")
    void quantiteSurevalueeParLaRequete() {
        stockAvecUg(10);
        SalesLine ligne = ligneVendue(2);

        service.processUg(ligne, dto(9), 1);

        assertThat(ligne.getQuantityUg()).isEqualTo(2);
    }

    @Test
    @DisplayName("sans UG en stock, la ligne n'en porte aucune")
    void sansUgEnStock() {
        stockAvecUg(0);
        SalesLine ligne = ligneVendue(5);

        service.processUg(ligne, dto(5), 1);

        assertThat(ligne.getQuantityUg()).isZero();
    }

    private void stockAvecUg(int qtyUg) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setQtyUG(qtyUg);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(anyInt(), anyInt())).thenReturn(stockProduit);
    }

    private static SalesLine ligneVendue(int quantiteVendue) {
        SalesLine salesLine = new SalesLine();
        salesLine.setQuantitySold(quantiteVendue);
        return salesLine;
    }

    private static SaleLineDTO dto(Integer quantiteVendueAnnoncee) {
        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(1);
        dto.setQuantitySold(quantiteVendueAnnoncee);
        return dto;
    }
}
