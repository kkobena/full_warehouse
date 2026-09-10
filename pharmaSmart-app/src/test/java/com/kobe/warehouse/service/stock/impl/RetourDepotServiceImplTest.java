package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUserNames;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourDepot;
import com.kobe.warehouse.domain.RetourDepotItem;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RetourDepotItemRepository;
import com.kobe.warehouse.repository.RetourDepotRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.RetourDepotDTO;
import com.kobe.warehouse.service.dto.RetourDepotItemDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetourDepotServiceImpl")
class RetourDepotServiceImplTest {

    private static final int DEPOT_ID = 9;
    private static final int OFFICINE_ID = 1;
    private static final int PRODUIT_ID = 500;

    @Mock
    private RetourDepotRepository retourDepotRepository;

    @Mock
    private RetourDepotItemRepository retourDepotItemRepository;

    @Mock
    private MagasinRepository magasinRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private UserService userService;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    private RetourDepotServiceImpl service;

    private Magasin depot;
    private Magasin officine;
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        service = new RetourDepotServiceImpl(
            retourDepotRepository,
            retourDepotItemRepository,
            magasinRepository,
            produitRepository,
            userService,
            stockProduitRepository,
            inventoryTransactionService
        );
        depot = magasin(DEPOT_ID, "DEPOT CENTRAL");
        officine = magasin(OFFICINE_ID, "OFFICINE PRINCIPALE");
        currentUser = new AppUser();
        currentUser.setLastName("KOUAME");
        currentUser.setFirstName("Awa");
        currentUser.setMagasin(officine);
    }

    private static Magasin magasin(int id, String fullName) {
        Magasin m = new Magasin();
        m.setId(id);
        m.setFullName(fullName);
        return m;
    }

    private static Storage storage(StorageType type) {
        Storage s = new Storage();
        s.setStorageType(type);
        return s;
    }

    private static StockProduit stock(StorageType type, int qtyStock) {
        StockProduit sp = new StockProduit();
        sp.setStorage(storage(type));
        sp.setQtyStock(qtyStock);
        return sp;
    }

    private static Produit produit(int prixUni, int prixAchat, String cip) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setPrixUni(prixUni);
        fp.setPrixAchat(prixAchat);
        fp.setCodeCip(cip);
        Produit p = new Produit();
        p.setId(PRODUIT_ID);
        p.setLibelle("DOLIPRANE 1000MG");
        p.setFournisseurProduitPrincipal(fp);
        return p;
    }

    private static RetourDepotItemDTO itemDTO(int qtyMvt) {
        return new RetourDepotItemDTO().setProduitId(PRODUIT_ID).setProduitCip("1234567").setQtyMvt(qtyMvt).setRegularUnitPrice(999);
    }

    private RetourDepotDTO commande(RetourDepotItemDTO... items) {
        return new RetourDepotDTO().setDepotId(DEPOT_ID).setRetourDepotItems(items.length == 0 ? List.of() : List.of(items));
    }

    private void stubCreationContext() {
        when(userService.getUser()).thenReturn(currentUser);
        when(magasinRepository.findById(DEPOT_ID)).thenReturn(Optional.of(depot));
        when(retourDepotRepository.save(any(RetourDepot.class))).thenAnswer(inv -> ((RetourDepot) inv.getArgument(0)).setId(3));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("refuse un depot inconnu")
        void depotInconnu() {
            when(userService.getUser()).thenReturn(currentUser);
            when(magasinRepository.findById(DEPOT_ID)).thenReturn(Optional.empty());
            RetourDepotDTO dto = commande();

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Dépôt non trouvé");

            verify(retourDepotRepository, never()).save(any());
        }

        @Test
        @DisplayName("horodate le retour et l affecte a l utilisateur courant")
        void enteteDuRetour() {
            stubCreationContext();

            LocalDateTime before = LocalDateTime.now();
            RetourDepotDTO dto = service.create(commande());

            ArgumentCaptor<RetourDepot> captor = ArgumentCaptor.forClass(RetourDepot.class);
            verify(retourDepotRepository).save(captor.capture());
            assertThat(captor.getValue().getDateMtv()).isAfterOrEqualTo(before);
            assertThat(captor.getValue().getUser()).isSameAs(currentUser);
            assertThat(captor.getValue().getDepot()).isSameAs(depot);
            assertThat(dto.getId()).isEqualTo(3);
            assertThat(dto.getDepotId()).isEqualTo(DEPOT_ID);
            assertThat(dto.getDepotName()).isEqualTo("DEPOT CENTRAL");
            assertThat(dto.getUserFullName()).isEqualTo(AppUserNames.fullName(currentUser));
        }

        @Test
        @DisplayName("une liste de lignes nulle ne cree aucun mouvement")
        void lignesNulles() {
            stubCreationContext();

            service.create(new RetourDepotDTO().setDepotId(DEPOT_ID));

            verify(retourDepotItemRepository, never()).save(any());
            verify(inventoryTransactionService, never()).save(any(RetourDepotItem.class));
        }

        @Test
        @DisplayName("une liste de lignes vide ne cree aucun mouvement")
        void lignesVides() {
            stubCreationContext();

            service.create(commande());

            verify(retourDepotItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse une ligne dont le produit est inconnu")
        void produitInconnu() {
            stubCreationContext();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());
            RetourDepotDTO dto = commande(itemDTO(5));

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Produit non trouvé");
        }

        @Test
        @DisplayName("refuse une ligne sans stock au depot")
        void stockDepotAbsent() {
            stubCreationContext();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID)).thenReturn(List.of());
            RetourDepotDTO dto = commande(itemDTO(5));

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Stock non trouvé pour le produit: 1234567");
        }

        @Test
        @DisplayName("transfere le stock du depot vers l officine")
        void transfereLeStock() {
            stubCreationContext();
            StockProduit depotStock = stock(StorageType.PRINCIPAL, 100);
            StockProduit officineStock = stock(StorageType.PRINCIPAL, 20);
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID))
                .thenReturn(List.of(depotStock));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(OFFICINE_ID, PRODUIT_ID))
                .thenReturn(List.of(officineStock));

            service.create(commande(itemDTO(7)));

            assertThat(depotStock.getQtyStock()).isEqualTo(93);
            assertThat(officineStock.getQtyStock()).isEqualTo(27);
            verify(stockProduitRepository).save(depotStock);
            verify(stockProduitRepository).save(officineStock);
        }

        @Test
        @DisplayName("trace le mouvement avec les stocks avant et apres des deux cotes")
        void traceLeMouvement() {
            stubCreationContext();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 100)));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(OFFICINE_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 20)));

            service.create(commande(itemDTO(7)));

            ArgumentCaptor<RetourDepotItem> captor = ArgumentCaptor.forClass(RetourDepotItem.class);
            verify(retourDepotItemRepository).save(captor.capture());
            RetourDepotItem item = captor.getValue();
            assertThat(item.getQtyMvt()).isEqualTo(7);
            assertThat(item.getInitStock()).isEqualTo(100);
            assertThat(item.getAfterStock()).isEqualTo(93);
            assertThat(item.getOfficineInitStock()).isEqualTo(20);
            assertThat(item.getOfficineFinalStock()).isEqualTo(27);
            verify(inventoryTransactionService).save(item);
        }

        @Test
        @DisplayName("valorise la ligne aux prix du fournisseur principal, pas a ceux transmis")
        void valorisationDepuisLeFournisseurPrincipal() {
            stubCreationContext();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 100)));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(OFFICINE_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 20)));

            service.create(commande(itemDTO(7)));

            ArgumentCaptor<RetourDepotItem> captor = ArgumentCaptor.forClass(RetourDepotItem.class);
            verify(retourDepotItemRepository).save(captor.capture());
            assertThat(captor.getValue().getRegularUnitPrice()).isEqualTo(1200);
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(800);
        }

        @Test
        @DisplayName("privilegie l emplacement principal du depot quand il y en a plusieurs")
        void choisitLEmplacementPrincipal() {
            stubCreationContext();
            StockProduit secondaire = stock(StorageType.SAFETY_STOCK, 40);
            StockProduit principal = stock(StorageType.PRINCIPAL, 100);
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID))
                .thenReturn(List.of(secondaire, principal));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(OFFICINE_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 20)));

            service.create(commande(itemDTO(7)));

            assertThat(principal.getQtyStock()).isEqualTo(93);
            assertThat(secondaire.getQtyStock()).isEqualTo(40);
        }

        @Test
        @DisplayName("retombe sur le premier emplacement du depot quand aucun n est principal")
        void retombeSurLePremierEmplacement() {
            stubCreationContext();
            StockProduit premier = stock(StorageType.SAFETY_STOCK, 40);
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID))
                .thenReturn(List.of(premier));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(OFFICINE_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 20)));

            service.create(commande(itemDTO(7)));

            assertThat(premier.getQtyStock()).isEqualTo(33);
        }

        @Test
        @DisplayName("le stock initial officine somme tous les emplacements")
        void stockInitialOfficineSommeTout() {
            stubCreationContext();
            StockProduit officinePrincipal = stock(StorageType.PRINCIPAL, 20);
            StockProduit officineSecondaire = stock(StorageType.SAFETY_STOCK, 5);
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 100)));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(OFFICINE_ID, PRODUIT_ID))
                .thenReturn(List.of(officinePrincipal, officineSecondaire));

            service.create(commande(itemDTO(7)));

            ArgumentCaptor<RetourDepotItem> captor = ArgumentCaptor.forClass(RetourDepotItem.class);
            verify(retourDepotItemRepository).save(captor.capture());
            assertThat(captor.getValue().getOfficineInitStock()).isEqualTo(25);
            assertThat(captor.getValue().getOfficineFinalStock()).isEqualTo(32);
            assertThat(officinePrincipal.getQtyStock()).isEqualTo(27);
            assertThat(officineSecondaire.getQtyStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("traite chaque ligne de la commande")
        void traiteChaqueLigne() {
            stubCreationContext();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit(1200, 800, "1234567")));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(DEPOT_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 100)));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(OFFICINE_ID, PRODUIT_ID))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 20)));

            service.create(commande(itemDTO(3), itemDTO(4)));

            verify(retourDepotItemRepository, org.mockito.Mockito.times(2)).save(any(RetourDepotItem.class));
        }
    }

    @Nested
    @DisplayName("findAllByDateRange")
    class FindAllByDateRange {

        @Test
        @DisplayName("borne la periode du debut de journee a 23:59:59")
        @SuppressWarnings("unchecked")
        void bornesDePeriode() {
            LocalDate from = LocalDate.of(2026, 4, 1);
            LocalDate to = LocalDate.of(2026, 4, 30);
            Specification<RetourDepot> spec = (root, query, cb) -> null;
            when(retourDepotRepository.filterByDateRangeAndDepot(any(), any(), any())).thenReturn(spec);
            when(retourDepotRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

            service.findAllByDateRange(DEPOT_ID, from, to, PageRequest.of(0, 10));

            verify(retourDepotRepository).filterByDateRangeAndDepot(DEPOT_ID, from.atStartOfDay(), to.atTime(23, 59, 59));
        }

        @Test
        @DisplayName("mappe chaque retour de la page")
        @SuppressWarnings("unchecked")
        void mappeLaPage() {
            RetourDepot retour = new RetourDepot().setId(3).setDateMtv(LocalDateTime.now()).setUser(currentUser).setDepot(depot);
            retour.setRetourDepotItems(new ArrayList<>());
            Specification<RetourDepot> spec = (root, query, cb) -> null;
            when(retourDepotRepository.filterByDateRangeAndDepot(any(), any(), any())).thenReturn(spec);
            when(retourDepotRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(retour)));

            Page<RetourDepotDTO> page = service.findAllByDateRange(
                DEPOT_ID,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                PageRequest.of(0, 10)
            );

            assertThat(page).hasSize(1);
            assertThat(page.getContent().getFirst().getId()).isEqualTo(3);
            assertThat(page.getContent().getFirst().getRetourDepotItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOne")
    class FindOne {

        @Test
        @DisplayName("renvoie vide quand le retour est introuvable")
        void introuvable() {
            when(retourDepotRepository.findOneWithItems(3)).thenReturn(Optional.empty());

            assertThat(service.findOne(3)).isEmpty();
        }

        @Test
        @DisplayName("mappe le retour et ses lignes")
        void mappeLeRetourEtSesLignes() {
            RetourDepotItem item = new RetourDepotItem();
            item.setId(11);
            item.setQtyMvt(7);
            item.setRegularUnitPrice(1200);
            item.setInitStock(100);
            item.setAfterStock(93);
            item.setProduit(produit(1200, 800, "1234567"));
            RetourDepot retour = new RetourDepot().setId(3).setDateMtv(LocalDateTime.now()).setUser(currentUser).setDepot(depot);
            retour.setRetourDepotItems(List.of(item));
            when(retourDepotRepository.findOneWithItems(3)).thenReturn(Optional.of(retour));

            RetourDepotDTO dto = service.findOne(3).orElseThrow();

            assertThat(dto.getId()).isEqualTo(3);
            assertThat(dto.getRetourDepotItems()).hasSize(1);
            RetourDepotItemDTO itemDto = dto.getRetourDepotItems().getFirst();
            assertThat(itemDto.getId()).isEqualTo(11);
            assertThat(itemDto.getQtyMvt()).isEqualTo(7);
            assertThat(itemDto.getInitStock()).isEqualTo(100);
            assertThat(itemDto.getAfterStock()).isEqualTo(93);
            assertThat(itemDto.getProduitId()).isEqualTo(PRODUIT_ID);
            assertThat(itemDto.getProduitLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(itemDto.getProduitCip()).isEqualTo("1234567");
        }

        @Test
        @DisplayName("tolere un retour sans collection de lignes")
        void sansCollectionDeLignes() {
            RetourDepot retour = new RetourDepot().setId(3).setDateMtv(LocalDateTime.now()).setUser(currentUser).setDepot(depot);
            retour.setRetourDepotItems(null);
            when(retourDepotRepository.findOneWithItems(3)).thenReturn(Optional.of(retour));

            // le DTO conserve sa liste par defaut : la branche nulle ne l ecrase pas
            assertThat(service.findOne(3).orElseThrow().getRetourDepotItems()).isEmpty();
        }
    }
}
