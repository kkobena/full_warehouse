package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.repository.MobileProductRepository;
import com.kobe.warehouse.repository.MobileProductRepository.LotInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.PriceInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.ProductBasicInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.ProductSalesStatsProjection;
import com.kobe.warehouse.repository.MobileProductRepository.StockInfoProjection;
import com.kobe.warehouse.repository.MobileProductRepository.StorageStockProjection;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.LotInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.StockInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.StorageStock;
import com.kobe.warehouse.service.stock.ProduitService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * La fiche produit rapide est ce qu'on ouvre au comptoir, client devant soi : « en ai-je, où, et
 * jusqu'à quand ». Elle rassemble en un appel le stock par emplacement, le prix, les lots et les
 * ventes récentes.
 *
 * <p>Deux qualifications y sont faites, et ce sont elles qui colorent la fiche. Le <b>statut de
 * stock</b>, qui doit distinguer une rupture franche d'un stock sous seuil — un produit à zéro et un
 * produit à deux exemplaires n'appellent pas la même décision. Et l'<b>autonomie en jours</b>, qui
 * rapporte le stock au rythme de sortie : elle n'a de sens que si le produit se vend, et ne doit
 * jamais se calculer en divisant par zéro.
 */
@DisplayName("MobileProductService — fiche produit rapide")
class MobileProductServiceTest {

    private static final int PRODUIT = 42;

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final MobileProductRepository productRepository = mock(MobileProductRepository.class);
    private final ProduitService produitService = mock(ProduitService.class);

    private final MobileProductService service = new MobileProductService(productRepository, produitService);

    @BeforeEach
    void produitOrdinaire() {
        when(productRepository.getBasicProductInfo(anyInt())).thenReturn(
            new ProductBasicInfoProjection(42L, "DOLIPRANE 1000", "CIP123", "EAN456", "LABOREX", 3L, "ANTALGIQUES", 9L)
        );
        when(productRepository.getStockInfo(anyInt())).thenReturn(stock(50, 0, 10));
        when(productRepository.getPriceInfo(anyInt())).thenReturn(new PriceInfoProjection(800, 1_000, 20.0, 18));
        when(productRepository.getLotInfo(anyInt())).thenReturn(List.of());
        when(productRepository.getSalesStats(anyInt(), any(), any(), any())).thenReturn(
            new ProductSalesStatsProjection(0, 0L, 0, 0L, 0, 0L, 0.0)
        );
        when(productRepository.getAverageDailyQuantitySold(anyInt(), anyInt())).thenReturn(0.0);
    }

    // ===== identité =====

    @Nested
    @DisplayName("Identité du produit")
    class IdentiteDuProduit {

        @Test
        @DisplayName("la fiche porte les identifiants et le rattachement du produit")
        void identifiants() {
            MobileProductQuickInfoDTO fiche = service.getProductQuickInfo(PRODUIT);

            assertThat(fiche.id()).isEqualTo(42L);
            assertThat(fiche.name()).isEqualTo("DOLIPRANE 1000");
            assertThat(fiche.codeCip()).isEqualTo("CIP123");
            assertThat(fiche.codeEan()).isEqualTo("EAN456");
            assertThat(fiche.supplierName()).isEqualTo("LABOREX");
            assertThat(fiche.supplierId()).isEqualTo(3L);
            assertThat(fiche.familyName()).isEqualTo("ANTALGIQUES");
            assertThat(fiche.familyId()).isEqualTo(9L);
        }

        /**
         * Un code-barres inconnu, un produit supprimé : l'appel ne rend rien, plutôt qu'une fiche
         * vide qui laisserait croire à un produit sans stock.
         */
        @Test
        @DisplayName("un produit introuvable ne rend aucune fiche")
        void produitIntrouvable() {
            when(productRepository.getBasicProductInfo(anyInt())).thenReturn(null);

            assertThat(service.getProductQuickInfo(PRODUIT)).isNull();
        }

        @Test
        @DisplayName("un produit introuvable n'interroge pas le reste")
        void produitIntrouvableSansSuite() {
            when(productRepository.getBasicProductInfo(anyInt())).thenReturn(null);

            service.getProductQuickInfo(PRODUIT);

            verify(productRepository, never()).getStockInfo(anyInt());
            verify(productRepository, never()).getLotInfo(anyInt());
        }
    }

    // ===== stock =====

    @Nested
    @DisplayName("Stock")
    class Stock {

        @Test
        @DisplayName("les quantités se distinguent du stock payant et des unités gratuites")
        void quantites() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(40, 10, 5));

            StockInfo stockInfo = service.getProductQuickInfo(PRODUIT).stock();

            assertThat(stockInfo.totalQtyStock()).isEqualTo(40);
            assertThat(stockInfo.totalQtyUg()).isEqualTo(10);
            assertThat(stockInfo.totalQuantity()).isEqualTo(50);
            assertThat(stockInfo.minThreshold()).isEqualTo(5);
        }

        @Test
        @DisplayName("un stock épuisé est une rupture")
        void rupture() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(0, 0, 10));

            StockInfo stockInfo = service.getProductQuickInfo(PRODUIT).stock();

            assertThat(stockInfo.status()).isEqualTo("RUPTURE");
            assertThat(stockInfo.statusColor()).isEqualTo("red");
        }

        @Test
        @DisplayName("un stock au seuil est déjà faible")
        void stockAuSeuil() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(10, 0, 10));

            assertThat(service.getProductQuickInfo(PRODUIT).stock().status()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("un stock au-dessus du seuil est suffisant")
        void stockSuffisant() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(11, 0, 10));

            StockInfo stockInfo = service.getProductQuickInfo(PRODUIT).stock();

            assertThat(stockInfo.status()).isEqualTo("OK");
            assertThat(stockInfo.statusColor()).isEqualTo("green");
        }

        @Test
        @DisplayName("le stock est détaillé emplacement par emplacement")
        void detailParEmplacement() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(
                new StockInfoProjection(
                    40, 10, 50, 5, 100,
                    List.of(
                        new StorageStockProjection(1L, "Comptoir", "PRINCIPAL", 30, 5, 35),
                        new StorageStockProjection(2L, "Réserve", "RESERVE", 10, 5, 15)
                    )
                )
            );

            List<StorageStock> emplacements = service.getProductQuickInfo(PRODUIT).stock().storageStocks();

            assertThat(emplacements).extracting(StorageStock::storageName).containsExactly("Comptoir", "Réserve");
            assertThat(emplacements.getFirst().storageId()).isEqualTo(1L);
            assertThat(emplacements.getFirst().storageType()).isEqualTo("PRINCIPAL");
            assertThat(emplacements.getFirst().qtyStock()).isEqualTo(30);
            assertThat(emplacements.getFirst().qtyUg()).isEqualTo(5);
            assertThat(emplacements.getFirst().totalQuantity()).isEqualTo(35);
        }
    }

    // ===== autonomie =====

    @Nested
    @DisplayName("Autonomie en jours")
    class Autonomie {

        @Test
        @DisplayName("l'autonomie rapporte le stock au rythme de sortie des trente derniers jours")
        void autonomie() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(50, 0, 10));
            when(productRepository.getAverageDailyQuantitySold(PRODUIT, 30)).thenReturn(4.0);

            assertThat(service.getProductQuickInfo(PRODUIT).stock().daysOfStock()).isEqualTo(12);
        }

        /** On arrondit vers le bas : annoncer treize jours quand on en a douze et demi ferait manquer. */
        @Test
        @DisplayName("l'autonomie s'arrondit vers le bas")
        void arrondiVersLeBas() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(50, 0, 10));
            when(productRepository.getAverageDailyQuantitySold(PRODUIT, 30)).thenReturn(3.0);

            assertThat(service.getProductQuickInfo(PRODUIT).stock().daysOfStock()).isEqualTo(16);
        }

        @Test
        @DisplayName("un stock épuisé n'a aucune autonomie et ne fait rien calculer")
        void stockEpuise() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(0, 0, 10));

            assertThat(service.getProductQuickInfo(PRODUIT).stock().daysOfStock()).isZero();
            verify(productRepository, never()).getAverageDailyQuantitySold(anyInt(), anyInt());
        }

        /** Un produit qui ne sort pas n'a pas d'échéance : la valeur convenue tient lieu d'infini. */
        @Test
        @DisplayName("un produit qui ne se vend pas reçoit une autonomie conventionnelle")
        void produitDormant() {
            when(productRepository.getStockInfo(anyInt())).thenReturn(stock(50, 0, 10));
            when(productRepository.getAverageDailyQuantitySold(PRODUIT, 30)).thenReturn(0.0);

            assertThat(service.getProductQuickInfo(PRODUIT).stock().daysOfStock()).isEqualTo(999);
        }
    }

    // ===== lots =====

    @Nested
    @DisplayName("Lots")
    class Lots {

        @Test
        @DisplayName("chaque lot porte son numéro, sa date et son échéance")
        void contenuDuLot() {
            when(productRepository.getLotInfo(anyInt())).thenReturn(
                List.of(new LotInfoProjection(7L, "LOT-A", LocalDate.of(2026, 12, 31), 120, 200))
            );

            LotInfo lot = service.getProductQuickInfo(PRODUIT).lots().getFirst();

            assertThat(lot.id()).isEqualTo(7L);
            assertThat(lot.lotNumber()).isEqualTo("LOT-A");
            assertThat(lot.expiryDate()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(lot.quantity()).isEqualTo(120);
            assertThat(lot.daysUntilExpiry()).isEqualTo(200);
        }

        /** Les seuils de la fiche produit sont plus larges que ceux des alertes : trente et quatre-vingt-dix jours. */
        @Test
        @DisplayName("l'échéance qualifie le lot par paliers")
        void paliersDEcheance() {
            when(productRepository.getLotInfo(anyInt())).thenReturn(
                List.of(
                    lot(1L, 0),
                    lot(2L, 1),
                    lot(3L, 30),
                    lot(4L, 31),
                    lot(5L, 90),
                    lot(6L, 91)
                )
            );

            assertThat(service.getProductQuickInfo(PRODUIT).lots())
                .extracting(LotInfo::expiryStatus)
                .containsExactly("EXPIRED", "CRITICAL", "CRITICAL", "WARNING", "WARNING", "OK");
        }

        @Test
        @DisplayName("un lot déjà périmé est signalé comme tel")
        void lotPerime() {
            when(productRepository.getLotInfo(anyInt())).thenReturn(List.of(lot(1L, -5)));

            assertThat(service.getProductQuickInfo(PRODUIT).lots().getFirst().expiryStatus()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("un produit sans lot suivi rend une liste vide")
        void aucunLot() {
            assertThat(service.getProductQuickInfo(PRODUIT).lots()).isEmpty();
        }
    }

    // ===== prix et ventes =====

    @Nested
    @DisplayName("Prix et ventes récentes")
    class PrixEtVentes {

        @Test
        @DisplayName("le prix d'achat, le prix de vente, la marge et la TVA sont repris")
        void prix() {
            var prix = service.getProductQuickInfo(PRODUIT).price();

            assertThat(prix.purchasePrice()).isEqualTo(800);
            assertThat(prix.sellingPrice()).isEqualTo(1_000);
            assertThat(prix.marginPercent()).isEqualTo(20.0);
            assertThat(prix.vatRate()).isEqualTo(18);
        }

        @Test
        @DisplayName("les ventes se lisent sur le jour, la semaine et le mois")
        void ventesRecentes() {
            when(productRepository.getSalesStats(anyInt(), any(), any(), any())).thenReturn(
                new ProductSalesStatsProjection(3, 3_000L, 20, 20_000L, 85, 85_000L, 2.8)
            );

            var ventes = service.getProductQuickInfo(PRODUIT).salesStats();

            assertThat(ventes.todayQuantity()).isEqualTo(3);
            assertThat(ventes.todayAmount()).isEqualTo(3_000L);
            assertThat(ventes.weekQuantity()).isEqualTo(20);
            assertThat(ventes.weekAmount()).isEqualTo(20_000L);
            assertThat(ventes.monthQuantity()).isEqualTo(85);
            assertThat(ventes.monthAmount()).isEqualTo(85_000L);
            assertThat(ventes.averageDailyQuantity()).isEqualTo(2.8);
        }

        @Test
        @DisplayName("les fenêtres de vente sont la semaine et le mois écoulés")
        void fenetresDeVente() {
            LocalDate aujourdHui = LocalDate.now();

            service.getProductQuickInfo(PRODUIT);

            verify(productRepository).getSalesStats(PRODUIT, aujourdHui, aujourdHui.minusDays(7), aujourdHui.minusDays(30));
        }
    }

    // ===== recherche =====

    @Nested
    @DisplayName("Recherche de produits")
    class Recherche {

        @Test
        @DisplayName("la recherche est déléguée avec la limite demandée")
        void limiteDemandee() {
            service.searchProducts("dolip", 5, 20);

            verify(produitService).searchProducts("dolip", 5, Pageable.ofSize(20));
        }

        /** Sans magasin précisé, la recherche porte sur le magasin principal. */
        @Test
        @DisplayName("sans magasin précisé, la recherche porte sur le magasin principal")
        void magasinParDefaut() {
            service.searchProducts("dolip", 20);

            verify(produitService).searchProducts(eq("dolip"), eq(1), any(Pageable.class));
        }
    }

    // ===== fabriques =====

    private static StockInfoProjection stock(int qtyStock, int qtyUg, int seuilMini) {
        return new StockInfoProjection(qtyStock, qtyUg, qtyStock + qtyUg, seuilMini, 100, List.of());
    }

    private static LotInfoProjection lot(long id, int joursAvantPeremption) {
        return new LotInfoProjection(id, "LOT-" + id, LocalDate.now().plusDays(joursAvantPeremption), 10, joursAvantPeremption);
    }
}
