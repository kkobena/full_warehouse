package com.kobe.warehouse.service.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.stock.dto.StockProduitSearchDTO;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockProduitService")
class StockProduitServiceTest {

    private static final int PRODUIT_ID = 500;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private RepartitionStockService repartitionStockService;

    private StockProduitService service;

    private Storage reserve;
    private Produit produit;

    @BeforeEach
    void setUp() {
        service = new StockProduitService(stockProduitRepository, produitRepository, storageService, repartitionStockService);
        reserve = storage(2, "RESERVE", StorageType.SAFETY_STOCK);
        produit = produit();
    }

    private static Storage storage(int id, String name, StorageType type) {
        Storage s = new Storage();
        s.setId(id);
        s.setName(name);
        s.setStorageType(type);
        return s;
    }

    private static Produit produit() {
        Produit p = new Produit();
        p.setId(PRODUIT_ID);
        p.setLibelle("DOLIPRANE 1000MG");
        p.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        p.setUpdatedAt(LocalDateTime.of(2026, 2, 1, 8, 0));
        return p;
    }

    private static StockProduit stockProduit(int id, Storage storage, Produit produit, int qtyStock) {
        StockProduit sp = new StockProduit();
        sp.setId(id);
        sp.setStorage(storage);
        sp.setProduit(produit);
        sp.setQtyStock(qtyStock);
        sp.setQtyVirtual(qtyStock);
        sp.setQtyUG(0);
        return sp;
    }

    private static StockProduitDTO commande(int qtyStock, Integer seuilMini, Integer reassort, Integer maxi) {
        StockProduitDTO dto = new StockProduitDTO();
        dto.setProduitId(PRODUIT_ID);
        dto.setQtyStock(qtyStock);
        dto.setSeuilMini(seuilMini);
        dto.setStockReassort(reassort);
        dto.setStockMaxi(maxi);
        return dto;
    }

    @Nested
    @DisplayName("createStockProduit")
    class CreateStockProduit {

        @Test
        @DisplayName("refuse un produit inconnu")
        void produitInconnu() {
            StockProduitDTO dto = commande(10, 5, 20, 50);
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createStockProduit(dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Produit not found with id: " + PRODUIT_ID);

            verify(stockProduitRepository, never()).save(any());
        }

        @Test
        @DisplayName("cree le stock dans la reserve de l utilisateur connecte")
        void creeDansLaReserve() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit));
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime before = LocalDateTime.now();
            StockProduitDTO result = service.createStockProduit(commande(30, 5, 20, 50));

            ArgumentCaptor<StockProduit> captor = ArgumentCaptor.forClass(StockProduit.class);
            verify(stockProduitRepository).save(captor.capture());
            StockProduit saved = captor.getValue();
            assertThat(saved.getProduit()).isSameAs(produit);
            assertThat(saved.getStorage()).isSameAs(reserve);
            assertThat(saved.getQtyStock()).isEqualTo(30);
            assertThat(saved.getSeuilMini()).isEqualTo(5);
            assertThat(saved.getStockReassort()).isEqualTo(20);
            assertThat(saved.getStockMaxi()).isEqualTo(50);
            assertThat(saved.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
            assertThat(result.getQtyStock()).isEqualTo(30);
            assertThat(result.getStorageId()).isEqualTo(2);
        }

        @Test
        @DisplayName("aligne la quantite virtuelle sur le stock et met les UG a zero")
        void quantiteVirtuelleEtUg() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit));
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));

            StockProduitDTO result = service.createStockProduit(commande(30, 5, 20, 50));

            assertThat(result.getQtyVirtual()).isEqualTo(30);
            assertThat(result.getQtyUG()).isZero();
        }

        @Test
        @DisplayName("ne declenche aucune repartition par defaut")
        void sansRepartition() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit));
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));

            service.createStockProduit(commande(30, 5, 20, 50));

            verifyNoInteractions(repartitionStockService);
        }

        @Test
        @DisplayName("declenche la repartition entre emplacements quand le transfert est demande")
        void avecRepartition() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit));
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));
            StockProduitDTO dto = commande(30, 5, 20, 50);
            dto.setWithTransfer(true);

            service.createStockProduit(dto);

            ArgumentCaptor<StockProduit> captor = ArgumentCaptor.forClass(StockProduit.class);
            verify(repartitionStockService).transferStockBetweenStorages(captor.capture());
            assertThat(captor.getValue().getQtyStock()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("updateStockProduit")
    class UpdateStockProduit {

        @Test
        @DisplayName("refuse un stock inconnu")
        void stockInconnu() {
            StockProduitDTO dto = commande(0, 5, 20, 50).setId(77);
            when(stockProduitRepository.findById(77)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStockProduit(dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("StockProduit not found with id: 77");
        }

        @Test
        @DisplayName("met a jour les trois seuils modifiables")
        void metAJourLesSeuils() {
            StockProduit existing = stockProduit(77, reserve, produit, 30);
            existing.setSeuilMini(1);
            existing.setStockReassort(2);
            existing.setStockMaxi(3);
            when(stockProduitRepository.findById(77)).thenReturn(Optional.of(existing));
            when(stockProduitRepository.save(existing)).thenReturn(existing);

            LocalDateTime before = LocalDateTime.now();
            StockProduitDTO result = service.updateStockProduit(commande(0, 8, 25, 60).setId(77));

            assertThat(existing.getSeuilMini()).isEqualTo(8);
            assertThat(existing.getStockReassort()).isEqualTo(25);
            assertThat(existing.getStockMaxi()).isEqualTo(60);
            assertThat(existing.getUpdatedAt()).isAfterOrEqualTo(before);
            assertThat(result.getSeuilMini()).isEqualTo(8);
        }

        @Test
        @DisplayName("ne touche pas au stock physique")
        void neTouchePasAuStock() {
            StockProduit existing = stockProduit(77, reserve, produit, 30);
            when(stockProduitRepository.findById(77)).thenReturn(Optional.of(existing));
            when(stockProduitRepository.save(existing)).thenReturn(existing);

            service.updateStockProduit(commande(999, 8, 25, 60).setId(77));

            assertThat(existing.getQtyStock()).isEqualTo(30);
        }

        @Test
        @DisplayName("laisse chaque seuil intact quand il est absent de la commande")
        void seuilsAbsentsIgnores() {
            StockProduit existing = stockProduit(77, reserve, produit, 30);
            existing.setSeuilMini(1);
            existing.setStockReassort(2);
            existing.setStockMaxi(3);
            when(stockProduitRepository.findById(77)).thenReturn(Optional.of(existing));
            when(stockProduitRepository.save(existing)).thenReturn(existing);

            service.updateStockProduit(commande(0, null, null, null).setId(77));

            assertThat(existing.getSeuilMini()).isEqualTo(1);
            assertThat(existing.getStockReassort()).isEqualTo(2);
            assertThat(existing.getStockMaxi()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getStockProduit")
    class GetStockProduit {

        @Test
        @DisplayName("refuse un stock inconnu")
        void stockInconnu() {
            when(stockProduitRepository.findById(77)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStockProduit(77))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("StockProduit not found with id: 77");
        }

        @Test
        @DisplayName("mappe le stock trouve")
        void mappeLeStock() {
            StockProduit existing = stockProduit(77, reserve, produit, 30);
            when(stockProduitRepository.findById(77)).thenReturn(Optional.of(existing));

            StockProduitDTO dto = service.getStockProduit(77);

            assertThat(dto.getId()).isEqualTo(77);
            assertThat(dto.getQtyStock()).isEqualTo(30);
            assertThat(dto.getProduitId()).isEqualTo(PRODUIT_ID);
            assertThat(dto.getProduitLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getStorageName()).isEqualTo("RESERVE");
        }
    }

    @Nested
    @DisplayName("searchStockProduitsForRepartition")
    class SearchStockProduitsForRepartition {

        private void stubMagasin() {
            Magasin magasin = new Magasin();
            magasin.setId(1);
            AppUser user = new AppUser();
            user.setMagasin(magasin);
            when(storageService.getUser()).thenReturn(user);
        }

        @Test
        @DisplayName("restreint la recherche au magasin de l utilisateur connecte")
        void restreintAuMagasin() {
            stubMagasin();
            when(stockProduitRepository.searchStockProduitsForRepartition(2, 1, "dolipra")).thenReturn(List.of());

            assertThat(service.searchStockProduitsForRepartition(2, "dolipra")).isEmpty();

            verify(stockProduitRepository).searchStockProduitsForRepartition(2, 1, "dolipra");
        }

        @Test
        @DisplayName("mappe l emplacement et le produit")
        void mappeEmplacementEtProduit() {
            stubMagasin();
            FournisseurProduit fp = new FournisseurProduit();
            fp.setCodeCip("1234567");
            produit.setFournisseurProduitPrincipal(fp);
            StockProduit sp = stockProduit(77, reserve, produit, 30);
            sp.setSeuilMini(4);
            produit.setStockProduits(Set.of(sp));
            when(stockProduitRepository.searchStockProduitsForRepartition(2, 1, null)).thenReturn(List.of(sp));

            StockProduitSearchDTO dto = service.searchStockProduitsForRepartition(2, null).getFirst();

            assertThat(dto.getId()).isEqualTo(77);
            assertThat(dto.getQtyStock()).isEqualTo(30);
            assertThat(dto.getSeuilMini()).isEqualTo(4);
            assertThat(dto.getStorageId()).isEqualTo(2);
            assertThat(dto.getStorageName()).isEqualTo("RESERVE");
            assertThat(dto.getStorageType()).isEqualTo(StorageType.SAFETY_STOCK.name());
            assertThat(dto.getProduitId()).isEqualTo(PRODUIT_ID);
            assertThat(dto.getProduitLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getProduitCodeCip()).isEqualTo("1234567");
            assertThat(dto.getAllStocks()).hasSize(1);
        }

        @Test
        @DisplayName("tolere un emplacement absent")
        void emplacementAbsent() {
            stubMagasin();
            StockProduit sp = stockProduit(77, null, produit, 30);
            produit.setStockProduits(new HashSet<>());
            when(stockProduitRepository.searchStockProduitsForRepartition(2, 1, null)).thenReturn(List.of(sp));

            StockProduitSearchDTO dto = service.searchStockProduitsForRepartition(2, null).getFirst();

            assertThat(dto.getStorageId()).isNull();
            assertThat(dto.getStorageName()).isNull();
            assertThat(dto.getStorageType()).isNull();
        }

        @Test
        @DisplayName("tolere un emplacement sans type")
        void emplacementSansType() {
            stubMagasin();
            StockProduit sp = stockProduit(77, storage(3, "AUTRE", null), produit, 30);
            produit.setStockProduits(new HashSet<>());
            when(stockProduitRepository.searchStockProduitsForRepartition(2, 1, null)).thenReturn(List.of(sp));

            assertThat(service.searchStockProduitsForRepartition(2, null).getFirst().getStorageType()).isNull();
        }

        @Test
        @DisplayName("tolere un stock sans produit")
        void produitAbsent() {
            stubMagasin();
            StockProduit sp = stockProduit(77, reserve, null, 30);
            when(stockProduitRepository.searchStockProduitsForRepartition(2, 1, null)).thenReturn(List.of(sp));

            StockProduitSearchDTO dto = service.searchStockProduitsForRepartition(2, null).getFirst();

            assertThat(dto.getProduitId()).isNull();
            assertThat(dto.getProduitLibelle()).isNull();
            assertThat(dto.getAllStocks()).isNull();
        }

        @Test
        @DisplayName("laisse le code CIP vide quand il n y a pas de fournisseur principal")
        void sansFournisseurPrincipal() {
            stubMagasin();
            StockProduit sp = stockProduit(77, reserve, produit, 30);
            produit.setStockProduits(new HashSet<>());
            when(stockProduitRepository.searchStockProduitsForRepartition(2, 1, null)).thenReturn(List.of(sp));

            assertThat(service.searchStockProduitsForRepartition(2, null).getFirst().getProduitCodeCip()).isNull();
        }

        @Test
        @DisplayName("renvoie une liste de stocks vide quand le produit n en porte aucun")
        void sansCollectionDeStocks() {
            stubMagasin();
            StockProduit sp = stockProduit(77, reserve, produit, 30);
            produit.setStockProduits(null);
            when(stockProduitRepository.searchStockProduitsForRepartition(2, 1, null)).thenReturn(List.of(sp));

            assertThat(service.searchStockProduitsForRepartition(2, null).getFirst().getAllStocks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("fromStockProduit")
    class FromStockProduit {

        @Test
        @DisplayName("mappe toutes les colonnes quand emplacement et produit sont presents")
        void mappeTout() {
            StockProduit sp = stockProduit(77, reserve, produit, 30);
            sp.setQtyUG(4);

            StockProduitDTO dto = service.fromStockProduit(sp);

            assertThat(dto.getId()).isEqualTo(77);
            assertThat(dto.getQtyStock()).isEqualTo(30);
            assertThat(dto.getQtyVirtual()).isEqualTo(30);
            assertThat(dto.getQtyUG()).isEqualTo(4);
            assertThat(dto.getStorageId()).isEqualTo(2);
            assertThat(dto.getStorageName()).isEqualTo("RESERVE");
            assertThat(dto.getStorageType()).isEqualTo(StorageType.SAFETY_STOCK.getValue());
            assertThat(dto.getType()).isEqualTo(StorageType.SAFETY_STOCK);
            assertThat(dto.getProduitId()).isEqualTo(PRODUIT_ID);
            assertThat(dto.getProduitLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 8, 0));
            assertThat(dto.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 8, 0));
            assertThat(dto.getTotalStockQuantity()).isEqualTo(34);
        }

        @Test
        @DisplayName("tolere un emplacement absent")
        void emplacementAbsent() {
            StockProduitDTO dto = service.fromStockProduit(stockProduit(77, null, produit, 30));

            assertThat(dto.getStorageId()).isNull();
            assertThat(dto.getStorageName()).isNull();
            assertThat(dto.getStorageType()).isNull();
            assertThat(dto.getType()).isNull();
        }

        @Test
        @DisplayName("tolere un emplacement sans type")
        void emplacementSansType() {
            StockProduitDTO dto = service.fromStockProduit(stockProduit(77, storage(3, "AUTRE", null), produit, 30));

            assertThat(dto.getStorageId()).isEqualTo(3);
            assertThat(dto.getStorageType()).isNull();
            assertThat(dto.getType()).isNull();
        }

        @Test
        @DisplayName("tolere un produit absent")
        void produitAbsent() {
            StockProduitDTO dto = service.fromStockProduit(stockProduit(77, reserve, null, 30));

            assertThat(dto.getProduitId()).isNull();
            assertThat(dto.getProduitLibelle()).isNull();
            assertThat(dto.getCreatedAt()).isNull();
            assertThat(dto.getUpdatedAt()).isNull();
        }
    }
}
