package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.RetourBonItemRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFromBonLignesCommand;
import com.kobe.warehouse.service.dto.AvoirFromBonLignesCommand.BonLigneItem;
import com.kobe.warehouse.service.dto.RetourBonBatchResultDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonFromLotRequest;
import com.kobe.warehouse.service.dto.RetourBonFromLotsRequest;
import com.kobe.warehouse.service.dto.RetourBonGroupeDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.dto.RetourBonLotResolutionDTO;
import com.kobe.warehouse.service.dto.RetourCompletCommandeRequest;
import com.kobe.warehouse.service.errors.FournisseurIntrouvableException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.MultipleFournisseursException;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.report.pdf.RetourBonPdfReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RetourBonServiceImpl")
class RetourBonServiceImplTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 4, 18);
    private static final int MAGASIN_ID = 1;
    private static final int PRODUIT_ID = 500;

    @Mock
    private RetourBonRepository retourBonRepository;

    @Mock
    private RetourBonItemRepository retourBonItemRepository;

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private UserService userService;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private LotRepository lotRepository;

    @Mock
    private RetourBonPdfReportService retourBonPdfReportService;

    @Mock
    private FournisseurProduitRepository fournisseurProduitRepository;

    @Mock
    private FournisseurRepository fournisseurRepository;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private AvoirFournisseurService avoirFournisseurService;

    private RetourBonServiceImpl service;

    private AppUser currentUser;
    private Magasin magasin;
    private Commande commande;

    @BeforeEach
    void setUp() {
        service = new RetourBonServiceImpl(
            retourBonRepository,
            retourBonItemRepository,
            commandeRepository,
            orderLineRepository,
            userService,
            stockProduitRepository,
            inventoryTransactionService,
            lotRepository,
            retourBonPdfReportService,
            fournisseurProduitRepository,
            fournisseurRepository,
            appConfigurationService,
            avoirFournisseurService
        );

        magasin = new Magasin();
        magasin.setId(MAGASIN_ID);
        currentUser = new AppUser();
        currentUser.setId(9);
        currentUser.setLogin("awa");
        currentUser.setActivated(true);
        currentUser.setFirstName("Awa");
        currentUser.setLastName("KOUAME");
        currentUser.setMagasin(magasin);
        when(userService.getUser()).thenReturn(currentUser);
        when(appConfigurationService.getDelaiRetourFournisseur()).thenReturn(30);

        commande = commande();
        when(commandeRepository.findById(new CommandeId(12, ORDER_DATE))).thenReturn(Optional.of(commande));
        when(retourBonRepository.save(any(RetourBon.class))).thenAnswer(inv -> {
            RetourBon bon = inv.getArgument(0);
            if (bon.getId() == null) {
                bon.setId(31);
            }
            return bon;
        });
        when(retourBonItemRepository.save(any(RetourBonItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Fournisseur fournisseur(int id, String libelle) {
        Fournisseur f = new Fournisseur();
        f.setId(id);
        f.setLibelle(libelle);
        return f;
    }

    private static Commande commande() {
        Commande c = new Commande();
        c.setId(12);
        c.setOrderDate(ORDER_DATE);
        c.setReceiptReference("BL-0012");
        c.setFournisseur(fournisseur(3, "LABOREX"));
        c.setOrderLines(new ArrayList<>());
        return c;
    }

    private static Produit produit() {
        Produit p = new Produit();
        p.setId(PRODUIT_ID);
        p.setLibelle("DOLIPRANE 1000MG");
        return p;
    }

    private static StockProduit stock(StorageType type, int qty) {
        Storage storage = new Storage();
        storage.setStorageType(type);
        StockProduit sp = new StockProduit();
        sp.setStorage(storage);
        sp.setQtyStock(qty);
        sp.setQtyUG(0);
        return sp;
    }

    private static OrderLine orderLine(int id, Integer received, Integer returned) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(77);
        fp.setCodeCip("1234567");
        fp.setProduit(produit());
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setOrderDate(ORDER_DATE);
        line.setFournisseurProduit(fp);
        line.setQuantityReceived(received);
        line.setQuantityReturned(returned);
        line.setOrderCostAmount(400);
        return line;
    }

    private static Lot lot(int id, int quantity) {
        Lot lot = new Lot().setId(id).setNumLot("L" + id).setQuantity(quantity).setProduit(produit());
        lot.setPrixAchat(400);
        return lot;
    }

    private RetourBon retourBon(RetourStatut statut) {
        RetourBon bon = new RetourBon();
        bon.setId(31);
        bon.setStatut(statut);
        bon.setDateMtv(LocalDateTime.now());
        bon.setUser(currentUser);
        bon.setCommande(commande);
        bon.setRetourBonItems(new ArrayList<>());
        return bon;
    }

    private void stubStock(int qty) {
        when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID))
            .thenReturn(List.of(stock(StorageType.PRINCIPAL, qty)));
    }

    private static RetourBonItemDTO itemDTO(int qty) {
        RetourBonItemDTO dto = new RetourBonItemDTO();
        dto.setProduitId(PRODUIT_ID);
        dto.setProduitCip("1234567");
        dto.setQtyMvt(qty);
        dto.setMotifRetourId(4);
        return dto;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("refuse une commande inconnue")
        void commandeInconnue() {
            when(commandeRepository.findById(any(CommandeId.class))).thenReturn(Optional.empty());
            RetourBonDTO dto = new RetourBonDTO().setCommandeId(99).setCommandeOrderDate(ORDER_DATE);

            assertThatThrownBy(() -> service.create(dto)).isInstanceOf(RuntimeException.class).hasMessageContaining("Commande not found");
        }

        @Test
        @DisplayName("cree un retour valide, horodate, rattache a la commande")
        void creeUnRetour() {
            LocalDateTime before = LocalDateTime.now();

            RetourBonDTO dto = service.create(
                new RetourBonDTO().setCommandeId(12).setCommandeOrderDate(ORDER_DATE).setCommentaire("perimes")
            );

            ArgumentCaptor<RetourBon> captor = ArgumentCaptor.forClass(RetourBon.class);
            verify(retourBonRepository, times(2)).save(captor.capture());
            RetourBon saved = captor.getValue();
            assertThat(saved.getStatut()).isEqualTo(RetourStatut.VALIDATED);
            assertThat(saved.getDateMtv()).isAfterOrEqualTo(before);
            assertThat(saved.getUser()).isSameAs(currentUser);
            assertThat(saved.getCommande()).isSameAs(commande);
            assertThat(saved.getCommentaire()).isEqualTo("perimes");
            assertThat(dto.getId()).isEqualTo(31);
        }

        @Test
        @DisplayName("attribue une reference annuelle sur quatre chiffres")
        void reference() {
            service.create(new RetourBonDTO().setCommandeId(12).setCommandeOrderDate(ORDER_DATE));

            ArgumentCaptor<RetourBon> captor = ArgumentCaptor.forClass(RetourBon.class);
            verify(retourBonRepository, times(2)).save(captor.capture());
            assertThat(captor.getValue().getReference()).isEqualTo("RET-" + Year.now().getValue() + "-0031");
        }

        @Test
        @DisplayName("une liste de lignes nulle ne cree aucun mouvement")
        void lignesNulles() {
            service.create(new RetourBonDTO().setCommandeId(12).setCommandeOrderDate(ORDER_DATE));

            verify(retourBonItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("une liste de lignes vide ne cree aucun mouvement")
        void lignesVides() {
            service.create(
                new RetourBonDTO().setCommandeId(12).setCommandeOrderDate(ORDER_DATE).setRetourBonItems(List.of())
            );

            verify(retourBonItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createRetourBonItem")
    class CreateRetourBonItem {

        private RetourBonDTO avecLigne(RetourBonItemDTO item) {
            return new RetourBonDTO().setCommandeId(12).setCommandeOrderDate(ORDER_DATE).setRetourBonItems(List.of(item));
        }

        @Test
        @DisplayName("refuse une quantite superieure au stock disponible")
        void stockInsuffisant() {
            stubStock(2);
            RetourBonDTO dto = avecLigne(itemDTO(5));

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Stock insuffisant pour le produit: 1234567");
        }

        @Test
        @DisplayName("decremente le stock principal du magasin")
        void decrementeLeStock() {
            StockProduit sp = stock(StorageType.PRINCIPAL, 20);
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID)).thenReturn(List.of(sp));

            service.create(avecLigne(itemDTO(5)));

            ArgumentCaptor<RetourBonItem> captor = ArgumentCaptor.forClass(RetourBonItem.class);
            verify(retourBonItemRepository).save(captor.capture());
            assertThat(captor.getValue().getInitStock()).isEqualTo(20);
            assertThat(captor.getValue().getAfterStock()).isEqualTo(15);
            assertThat(sp.getQtyStock()).isEqualTo(15);
            verify(stockProduitRepository).save(sp);
            verify(inventoryTransactionService).save(captor.getValue());
        }

        @Test
        @DisplayName("privilegie l emplacement principal quand il y en a plusieurs")
        void emplacementPrincipal() {
            StockProduit secondaire = stock(StorageType.SAFETY_STOCK, 4);
            StockProduit principal = stock(StorageType.PRINCIPAL, 20);
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID))
                .thenReturn(List.of(secondaire, principal));

            service.create(avecLigne(itemDTO(5)));

            // le stock initial somme tous les emplacements, le mouvement porte sur le principal
            assertThat(principal.getQtyStock()).isEqualTo(19);
            verify(stockProduitRepository).save(principal);
        }

        @Test
        @DisplayName("retombe sur le premier emplacement quand aucun n est principal")
        void premierEmplacement() {
            StockProduit premier = stock(StorageType.SAFETY_STOCK, 20);
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID))
                .thenReturn(List.of(premier));

            service.create(avecLigne(itemDTO(5)));

            assertThat(premier.getQtyStock()).isEqualTo(15);
        }

        @Test
        @DisplayName("rattache la ligne de commande et reprend son prix d achat")
        void avecLigneDeCommande() {
            stubStock(20);
            OrderLine line = orderLine(900, 10, 0);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            RetourBonItemDTO item = itemDTO(5);
            item.setOrderLineId(900);
            item.setOrderLineOrderDate(ORDER_DATE);

            service.create(avecLigne(item));

            ArgumentCaptor<RetourBonItem> captor = ArgumentCaptor.forClass(RetourBonItem.class);
            verify(retourBonItemRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderLine()).isSameAs(line);
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(400);
        }

        @Test
        @DisplayName("refuse une ligne de commande inconnue")
        void ligneDeCommandeInconnue() {
            stubStock(20);
            when(orderLineRepository.findById(any(OrderLineId.class))).thenReturn(Optional.empty());
            RetourBonItemDTO item = itemDTO(5);
            item.setOrderLineId(900);
            item.setOrderLineOrderDate(ORDER_DATE);
            RetourBonDTO dto = avecLigne(item);

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ligne de commande introuvable");
        }

        @Test
        @DisplayName("refuse un retour depassant la quantite retournable de la ligne")
        void quantiteNonRetournable() {
            stubStock(50);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(orderLine(900, 10, 0)));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(8);
            RetourBonItemDTO item = itemDTO(5);
            item.setOrderLineId(900);
            item.setOrderLineOrderDate(ORDER_DATE);
            RetourBonDTO dto = avecLigne(item);

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("dépasse la quantité retournable (2)")
                .hasMessageContaining("déjà retourné : 8");
        }

        @Test
        @DisplayName("le message de depassement retombe sur le numero de ligne sans code CIP")
        void messageSansCodeCip() {
            stubStock(50);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(orderLine(900, 10, null)));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            RetourBonItemDTO item = itemDTO(50);
            item.setProduitCip(null);
            item.setOrderLineId(900);
            item.setOrderLineOrderDate(ORDER_DATE);
            RetourBonDTO dto = avecLigne(item);

            assertThatThrownBy(() -> service.create(dto)).isInstanceOf(GenericError.class).hasMessageContaining("ligne 900");
        }

        @Test
        @DisplayName("une ligne sans quantite recue ni retournee est traitee comme zero")
        void quantitesNulles() {
            stubStock(50);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(orderLine(900, null, null)));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            RetourBonItemDTO item = itemDTO(1);
            item.setOrderLineId(900);
            item.setOrderLineOrderDate(ORDER_DATE);
            RetourBonDTO dto = avecLigne(item);

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("quantité retournable (0)");
        }

        @Test
        @DisplayName("hors commande, le prix d achat vient du DTO")
        void prixAchatHorsCommande() {
            stubStock(20);
            RetourBonItemDTO item = itemDTO(5);
            item.setPrixAchat(650);

            service.create(avecLigne(item));

            ArgumentCaptor<RetourBonItem> captor = ArgumentCaptor.forClass(RetourBonItem.class);
            verify(retourBonItemRepository).save(captor.capture());
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(650);
        }

        @Test
        @DisplayName("decremente le lot rattache")
        void decrementeLeLot() {
            stubStock(20);
            Lot lot = lot(70, 10);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot));
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(0);
            RetourBonItemDTO item = itemDTO(4);
            item.setLotId(70);

            service.create(avecLigne(item));

            assertThat(lot.getQuantity()).isEqualTo(6);
            verify(lotRepository).save(lot);
        }

        @Test
        @DisplayName("refuse un lot inconnu")
        void lotInconnu() {
            stubStock(20);
            when(lotRepository.findById(70)).thenReturn(Optional.empty());
            RetourBonItemDTO item = itemDTO(4);
            item.setLotId(70);
            RetourBonDTO dto = avecLigne(item);

            assertThatThrownBy(() -> service.create(dto)).isInstanceOf(GenericError.class).hasMessageContaining("Le lot n'existe pas");
        }

        @Test
        @DisplayName("refuse une quantite superieure au lot")
        void quantiteSuperieureAuLot() {
            stubStock(50);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 3)));
            RetourBonItemDTO item = itemDTO(4);
            item.setLotId(70);
            RetourBonDTO dto = avecLigne(item);

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Quantité insuffisante dans le lot: L70");
        }

        @Test
        @DisplayName("refuse un lot deja partiellement retourne")
        void lotDejaRetourne() {
            stubStock(50);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(8);
            RetourBonItemDTO item = itemDTO(4);
            item.setLotId(70);
            RetourBonDTO dto = avecLigne(item);

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("quantité retournable = 2")
                .hasMessageContaining("déjà retourné : 8");
        }
    }

    @Nested
    @DisplayName("findAll et sa specification")
    class FindAll {

        @Test
        @DisplayName("mappe la page de retours")
        @SuppressWarnings("unchecked")
        void mappeLaPage() {
            when(retourBonRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(retourBon(RetourStatut.VALIDATED))));

            Page<RetourBonDTO> page = service.findAll(null, null, null, null, null, PageRequest.of(0, 20));

            assertThat(page).hasSize(1);
            assertThat(page.getContent().getFirst().getId()).isEqualTo(31);
        }

        /** Rejoue la specification capturee sur des mocks Criteria pour couvrir son corps. */
        @SuppressWarnings("unchecked")
        private void appliquerSpecification(
            RetourStatut statut,
            RetourStatut excludeStatut,
            LocalDate dtStart,
            LocalDate dtEnd,
            String search,
            CriteriaBuilder cb
        ) {
            when(retourBonRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
            service.findAll(statut, excludeStatut, dtStart, dtEnd, search, PageRequest.of(0, 20));

            ArgumentCaptor<Specification<RetourBon>> captor = ArgumentCaptor.forClass(Specification.class);
            verify(retourBonRepository).findAll(captor.capture(), any(Pageable.class));

            Root<RetourBon> root = mock(Root.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            Path<Object> path = mock(Path.class);
            Join<Object, Object> join = mock(Join.class);
            Predicate predicate = mock(Predicate.class);
            Expression<String> lower = mock(Expression.class);
            when(root.get(anyString())).thenReturn(path);
            when(root.join(anyString(), any(JoinType.class))).thenReturn(join);
            when(join.join(anyString(), any(JoinType.class))).thenReturn(join);
            when(join.get(anyString())).thenReturn(path);
            when(cb.equal(nullable(Expression.class), any(Object.class))).thenReturn(predicate);
            when(cb.notEqual(nullable(Expression.class), any(Object.class))).thenReturn(predicate);
            when(cb.greaterThanOrEqualTo(nullable(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
            when(cb.lessThanOrEqualTo(nullable(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
            when(cb.lower(nullable(Expression.class))).thenReturn(lower);
            when(cb.like(nullable(Expression.class), anyString())).thenReturn(predicate);
            when(cb.or(any(Predicate[].class))).thenReturn(predicate);
            when(cb.and(any(Predicate[].class))).thenReturn(predicate);
            when(cb.desc(nullable(Expression.class))).thenReturn(mock(jakarta.persistence.criteria.Order.class));

            assertThat(captor.getValue().toPredicate((Root<RetourBon>) root, (CriteriaQuery<?>) query, cb)).isSameAs(predicate);
        }

        @Test
        @DisplayName("filtre sur un statut precis")
        void filtreSurStatut() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            appliquerSpecification(RetourStatut.VALIDATED, RetourStatut.CLOSED, null, null, null, cb);

            verify(cb).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(RetourStatut.VALIDATED));
            verify(cb, never()).notEqual(nullable(Expression.class), any(Object.class));
        }

        @Test
        @DisplayName("exclut un statut quand aucun statut precis n est demande")
        void excludeStatut() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            appliquerSpecification(null, RetourStatut.CLOSED, null, null, null, cb);

            verify(cb).notEqual(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(RetourStatut.CLOSED));
        }

        @Test
        @DisplayName("aucun predicat de statut quand rien n est demande")
        void sansStatut() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            appliquerSpecification(null, null, null, null, null, cb);

            verify(cb, never()).equal(nullable(Expression.class), any(Object.class));
            verify(cb, never()).notEqual(nullable(Expression.class), any(Object.class));
        }

        @Test
        @DisplayName("borne la periode du debut de journee a 23:59:59")
        void bornesDePeriode() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            LocalDate from = LocalDate.of(2026, 4, 1);
            LocalDate to = LocalDate.of(2026, 4, 30);

            appliquerSpecification(null, null, from, to, null, cb);

            verify(cb).greaterThanOrEqualTo(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(from.atStartOfDay()));
            verify(cb).lessThanOrEqualTo(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(to.atTime(23, 59, 59)));
        }

        @Test
        @DisplayName("cherche sur fournisseur, reference de BL et reference de retour")
        void rechercheLibre() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            appliquerSpecification(null, null, null, null, "LABOREX", cb);

            verify(cb, times(4)).like(nullable(Expression.class), org.mockito.ArgumentMatchers.eq("%laborex%"));
        }

        @ParameterizedTest(name = "une recherche [{0}] n ajoute aucun predicat de texte")
        @ValueSource(strings = { "", "   " })
        void rechercheBlanche(String search) {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            appliquerSpecification(null, null, null, null, search, cb);

            verify(cb, never()).like(nullable(Expression.class), anyString());
        }
    }

    @Nested
    @DisplayName("lectures")
    class Lectures {

        @Test
        @DisplayName("findAllByCommande mappe les retours de la commande")
        void findAllByCommande() {
            when(retourBonRepository.findAllByCommandeId(12)).thenReturn(List.of(retourBon(RetourStatut.VALIDATED)));

            assertThat(service.findAllByCommande(12, ORDER_DATE)).hasSize(1);
        }

        @Test
        @DisplayName("findOne renvoie vide quand le retour est introuvable")
        void findOneIntrouvable() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.empty());

            assertThat(service.findOne(31)).isEmpty();
        }

        @Test
        @DisplayName("findOne mappe le retour trouve")
        void findOneTrouve() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));

            assertThat(service.findOne(31)).get().extracting(RetourBonDTO::getId).isEqualTo(31);
        }

        @Test
        @DisplayName("countEnAttente compte tout ce qui n est pas cloture")
        void countEnAttente() {
            when(retourBonRepository.countByStatutNot(RetourStatut.CLOSED)).thenReturn(5L);

            assertThat(service.countEnAttente()).isEqualTo(5L);
        }

        @Test
        @DisplayName("createSupplierResponse delegue au service d avoirs")
        void createSupplierResponse() {
            AvoirFournisseurCommand command = new AvoirFournisseurCommand(31, null, List.of());
            AvoirFournisseurDTO expected = new AvoirFournisseurDTO();
            when(avoirFournisseurService.create(command)).thenReturn(expected);

            assertThat(service.createSupplierResponse(command)).isSameAs(expected);
        }

        @Test
        @DisplayName("export produit le PDF du retour")
        void export() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(retourBonPdfReportService.export(any(RetourBonDTO.class))).thenReturn(new byte[] { 7 });

            assertThat(service.export(31)).containsExactly(7);
        }

        @Test
        @DisplayName("export echoue quand le retour est introuvable")
        void exportIntrouvable() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.export(31)).isInstanceOf(RuntimeException.class).hasMessageContaining("RetourBon not found");
        }

        @Test
        @DisplayName("exportGroupe produit le bordereau regroupe")
        void exportGroupe() {
            when(retourBonRepository.findAllById(List.of(31))).thenReturn(List.of(retourBon(RetourStatut.VALIDATED)));
            when(retourBonPdfReportService.exportGroupe(any())).thenReturn(new byte[] { 8 });

            assertThat(service.exportGroupe(List.of(31))).containsExactly(8);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("refuse un retour introuvable")
        void introuvable() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.empty());
            RetourBonDTO dto = new RetourBonDTO().setId(31);

            assertThatThrownBy(() -> service.update(dto)).isInstanceOf(GenericError.class).hasMessageContaining("RetourBon not found");
        }

        @ParameterizedTest(name = "refuse la modification d un retour {0}")
        @EnumSource(value = RetourStatut.class, names = { "VALIDATED" }, mode = EnumSource.Mode.EXCLUDE)
        void statutNonModifiable(RetourStatut statut) {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(statut)));
            RetourBonDTO dto = new RetourBonDTO().setId(31);

            assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Seuls les retours en attente peuvent être modifiés");
        }

        @Test
        @DisplayName("annule les mouvements existants avant de recreer les lignes")
        void annuleEtRecree() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(bon));
            RetourBonItem existant = new RetourBonItem();
            existant.setId(60);
            existant.setQtyMvt(4);
            existant.setOrderLine(orderLine(900, 10, 0));
            when(retourBonItemRepository.findAllByRetourBonId(31)).thenReturn(List.of(existant));
            StockProduit sp = stock(StorageType.PRINCIPAL, 20);
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID)).thenReturn(List.of(sp));

            service.update(new RetourBonDTO().setId(31).setCommentaire("corrige"));

            assertThat(sp.getQtyStock()).isEqualTo(24);
            verify(retourBonItemRepository).deleteAllByRetourBonId(31);
            assertThat(bon.getCommentaire()).isEqualTo("corrige");
        }

        @Test
        @DisplayName("recree les lignes transmises")
        void recreeLesLignes() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(bon));
            when(retourBonItemRepository.findAllByRetourBonId(31)).thenReturn(List.of());
            stubStock(20);

            service.update(new RetourBonDTO().setId(31).setRetourBonItems(List.of(itemDTO(5))));

            verify(retourBonItemRepository).save(any(RetourBonItem.class));
        }

        @Test
        @DisplayName("une liste de lignes vide ne recree rien")
        void lignesVides() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(retourBonItemRepository.findAllByRetourBonId(31)).thenReturn(List.of());

            service.update(new RetourBonDTO().setId(31).setRetourBonItems(List.of()));

            verify(retourBonItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("reverseRetourBonItem")
    class ReverseRetourBonItem {

        private void stubUpdateAvec(RetourBonItem item) {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(retourBonItemRepository.findAllByRetourBonId(31)).thenReturn(List.of(item));
        }

        @Test
        @DisplayName("resout le produit via le lot quand la ligne de commande manque")
        void produitViaLeLot() {
            Lot lot = lot(70, 6);
            RetourBonItem item = new RetourBonItem();
            item.setId(60);
            item.setQtyMvt(4);
            item.setLot(lot);
            stubUpdateAvec(item);
            StockProduit sp = stock(StorageType.PRINCIPAL, 20);
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID)).thenReturn(List.of(sp));

            service.update(new RetourBonDTO().setId(31));

            assertThat(sp.getQtyStock()).isEqualTo(24);
            assertThat(lot.getQuantity()).isEqualTo(10);
            verify(lotRepository).save(lot);
        }

        @Test
        @DisplayName("refuse une ligne sans commande ni lot")
        void sansCommandeNiLot() {
            RetourBonItem item = new RetourBonItem();
            item.setId(60);
            item.setQtyMvt(4);
            stubUpdateAvec(item);
            RetourBonDTO dto = new RetourBonDTO().setId(31);

            assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Impossible de retrouver le produit");
        }

        @Test
        @DisplayName("retombe sur le premier emplacement quand aucun n est principal")
        void premierEmplacement() {
            RetourBonItem item = new RetourBonItem();
            item.setId(60);
            item.setQtyMvt(4);
            item.setOrderLine(orderLine(900, 10, 0));
            stubUpdateAvec(item);
            StockProduit sp = stock(StorageType.SAFETY_STOCK, 20);
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID)).thenReturn(List.of(sp));

            service.update(new RetourBonDTO().setId(31));

            assertThat(sp.getQtyStock()).isEqualTo(24);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("refuse un retour introuvable")
        void introuvable() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(31)).isInstanceOf(GenericError.class).hasMessageContaining("RetourBon not found");
        }

        @ParameterizedTest(name = "refuse la suppression d un retour {0}")
        @EnumSource(value = RetourStatut.class, names = { "VALIDATED" }, mode = EnumSource.Mode.EXCLUDE)
        void statutNonSupprimable(RetourStatut statut) {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(statut)));

            assertThatThrownBy(() -> service.delete(31))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Seuls les retours en attente peuvent être supprimés");
        }

        @Test
        @DisplayName("annule les mouvements puis supprime")
        void supprime() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            RetourBonItem item = new RetourBonItem();
            item.setId(60);
            item.setQtyMvt(4);
            item.setOrderLine(orderLine(900, 10, 0));
            when(retourBonItemRepository.findAllByRetourBonId(31)).thenReturn(List.of(item));
            StockProduit sp = stock(StorageType.PRINCIPAL, 20);
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, PRODUIT_ID)).thenReturn(List.of(sp));

            service.delete(31);

            assertThat(sp.getQtyStock()).isEqualTo(24);
            // Suppression par entite et non en bulk : voir RetourBonServiceImpl.delete
            verify(retourBonItemRepository).deleteAll(List.of(item));
            verify(retourBonRepository).deleteById(31);
        }
    }

    @Nested
    @DisplayName("changements de statut")
    class ChangementsDeStatut {

        @Test
        @DisplayName("markAsProcessing passe le retour en cours")
        void markAsProcessing() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(bon));

            assertThat(service.markAsProcessing(31).getStatut()).isEqualTo(RetourStatut.PROCESSING);
        }

        @Test
        @DisplayName("markAsProcessing refuse un retour introuvable")
        void markAsProcessingIntrouvable() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsProcessing(31)).isInstanceOf(GenericError.class);
        }

        @ParameterizedTest(name = "markAsProcessing refuse un retour {0}")
        @EnumSource(value = RetourStatut.class, names = { "VALIDATED" }, mode = EnumSource.Mode.EXCLUDE)
        void markAsProcessingStatutInvalide(RetourStatut statut) {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(statut)));

            assertThatThrownBy(() -> service.markAsProcessing(31))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("marqués en cours");
        }

        @Test
        @DisplayName("closeManually cloture un retour partiellement accepte")
        void closeManually() {
            RetourBon bon = retourBon(RetourStatut.PARTIALLY_ACCEPTED);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(bon));

            assertThat(service.closeManually(31).getStatut()).isEqualTo(RetourStatut.CLOSED);
        }

        @Test
        @DisplayName("closeManually refuse un retour introuvable")
        void closeManuallyIntrouvable() {
            when(retourBonRepository.findById(31)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.closeManually(31)).isInstanceOf(GenericError.class);
        }

        @ParameterizedTest(name = "closeManually refuse un retour {0}")
        @EnumSource(value = RetourStatut.class, names = { "PARTIALLY_ACCEPTED" }, mode = EnumSource.Mode.EXCLUDE)
        void closeManuallyStatutInvalide(RetourStatut statut) {
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(statut)));

            assertThatThrownBy(() -> service.closeManually(31))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("partiellement acceptés");
        }
    }

    @Nested
    @DisplayName("createFromExpiredLot")
    class CreateFromExpiredLot {

        private RetourBonFromLotRequest requete(int lotId, int quantity) {
            return new RetourBonFromLotRequest().setLotId(lotId).setQuantity(quantity).setMotifRetourId(4).setCommentaire("perime");
        }

        @Test
        @DisplayName("refuse un lot inconnu")
        void lotInconnu() {
            when(lotRepository.findById(70)).thenReturn(Optional.empty());
            RetourBonFromLotRequest request = requete(70, 4);

            assertThatThrownBy(() -> service.createFromExpiredLot(request))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Lot non trouvé: 70");
        }

        @Test
        @DisplayName("refuse une quantite superieure au lot")
        void quantiteSuperieureAuLot() {
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 3)));
            RetourBonFromLotRequest request = requete(70, 4);

            assertThatThrownBy(() -> service.createFromExpiredLot(request))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("dépasse le stock disponible dans le lot (3)");
        }

        @Test
        @DisplayName("chemin nominal : resout la commande via la ligne de commande du lot")
        void cheminNominal() {
            OrderLine line = orderLine(900, 10, 0);
            line.setCommande(commande);
            Lot lot = lot(70, 10).setOrderLine(line);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot));
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(0);
            stubStock(20);

            RetourBonDTO dto = service.createFromExpiredLot(requete(70, 4));

            assertThat(dto.getId()).isEqualTo(31);
            ArgumentCaptor<RetourBonItem> captor = ArgumentCaptor.forClass(RetourBonItem.class);
            verify(retourBonItemRepository).save(captor.capture());
            assertThat(captor.getValue().getLot()).isSameAs(lot);
            assertThat(captor.getValue().getOrderLine()).isSameAs(line);
        }

        @Test
        @DisplayName("fallback manuel : utilise la commande choisie par l utilisateur")
        void fallbackCommandeManuelle() {
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(0);
            stubStock(20);
            RetourBonFromLotRequest request = requete(70, 4).setCommandeId(12).setCommandeOrderDate(ORDER_DATE);

            RetourBonDTO dto = service.createFromExpiredLot(request);

            assertThat(dto.getId()).isEqualTo(31);
            ArgumentCaptor<RetourBonItem> captor = ArgumentCaptor.forClass(RetourBonItem.class);
            verify(retourBonItemRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderLine()).isNull();
        }

        @Test
        @DisplayName("fallback manuel : refuse une commande inconnue")
        void fallbackCommandeInconnue() {
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(commandeRepository.findById(new CommandeId(99, ORDER_DATE))).thenReturn(Optional.empty());
            RetourBonFromLotRequest request = requete(70, 4).setCommandeId(99).setCommandeOrderDate(ORDER_DATE);

            assertThatThrownBy(() -> service.createFromExpiredLot(request))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Commande override non trouvée: 99");
        }

        @Test
        @DisplayName("hors commande : utilise le fournisseur explicitement choisi")
        void horsCommandeAvecFournisseurChoisi() {
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(fournisseurRepository.findById(3)).thenReturn(Optional.of(fournisseur(3, "LABOREX")));
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(0);
            stubStock(20);

            service.createFromExpiredLot(requete(70, 4).setFournisseurId(3));

            ArgumentCaptor<RetourBon> captor = ArgumentCaptor.forClass(RetourBon.class);
            verify(retourBonRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getValue().isHorsCommande()).isTrue();
            assertThat(captor.getValue().getFournisseur().getId()).isEqualTo(3);
        }

        @Test
        @DisplayName("hors commande : refuse un fournisseur choisi inconnu")
        void horsCommandeFournisseurChoisiInconnu() {
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(fournisseurRepository.findById(3)).thenReturn(Optional.empty());
            RetourBonFromLotRequest request = requete(70, 4).setFournisseurId(3);

            assertThatThrownBy(() -> service.createFromExpiredLot(request))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Fournisseur non trouvé: 3");
        }

        @Test
        @DisplayName("hors commande : resout via le fournisseur principal du produit")
        void horsCommandeFournisseurPrincipal() {
            Lot lot = lot(70, 10);
            FournisseurProduit fp = new FournisseurProduit();
            fp.setFournisseur(fournisseur(3, "LABOREX"));
            lot.getProduit().setFournisseurProduitPrincipal(fp);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot));
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(0);
            stubStock(20);

            service.createFromExpiredLot(requete(70, 4));

            ArgumentCaptor<RetourBon> captor = ArgumentCaptor.forClass(RetourBon.class);
            verify(retourBonRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getValue().getFournisseur().getId()).isEqualTo(3);
        }

        @Test
        @DisplayName("hors commande : resout via l unique fournisseur reference du produit")
        void horsCommandeFournisseurUnique() {
            Lot lot = lot(70, 10);
            FournisseurProduit fp = new FournisseurProduit();
            fp.setFournisseur(fournisseur(3, "LABOREX"));
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot));
            when(fournisseurProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(List.of(fp));
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(0);
            stubStock(20);

            service.createFromExpiredLot(requete(70, 4));

            verify(retourBonItemRepository).save(any(RetourBonItem.class));
        }

        @Test
        @DisplayName("hors commande : plusieurs fournisseurs imposent un choix")
        void horsCommandePlusieursFournisseurs() {
            FournisseurProduit fp1 = new FournisseurProduit();
            fp1.setFournisseur(fournisseur(3, "LABOREX"));
            FournisseurProduit fp2 = new FournisseurProduit();
            fp2.setFournisseur(fournisseur(4, "COPHARMED"));
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(fournisseurProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(List.of(fp1, fp2));
            RetourBonFromLotRequest request = requete(70, 4);

            assertThatThrownBy(() -> service.createFromExpiredLot(request)).isInstanceOf(MultipleFournisseursException.class);
        }

        @Test
        @DisplayName("hors commande : aucun fournisseur connu")
        void horsCommandeAucunFournisseur() {
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(fournisseurProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(List.of());
            RetourBonFromLotRequest request = requete(70, 4);

            assertThatThrownBy(() -> service.createFromExpiredLot(request)).isInstanceOf(FournisseurIntrouvableException.class);
        }
    }

    @Nested
    @DisplayName("createFromExpiredLots")
    class CreateFromExpiredLots {

        @Test
        @DisplayName("agrege les retours crees et les erreurs")
        void agregeCreesEtErreurs() {
            RetourBonFromLotRequest ok = new RetourBonFromLotRequest().setLotId(70).setQuantity(4).setMotifRetourId(4);
            RetourBonFromLotRequest ko = new RetourBonFromLotRequest().setLotId(71).setQuantity(4).setMotifRetourId(4);
            Lot lot = lot(70, 10);
            OrderLine line = orderLine(900, 10, 0);
            line.setCommande(commande);
            lot.setOrderLine(line);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot));
            when(lotRepository.findById(71)).thenReturn(Optional.empty());
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            when(retourBonItemRepository.sumQtyMvtByLotId(70, 31, RetourStatut.CLOSED)).thenReturn(0);
            stubStock(20);

            RetourBonBatchResultDTO result = service.createFromExpiredLots(new RetourBonFromLotsRequest().setLots(List.of(ok, ko)));

            assertThat(result.getTotalCreated()).isEqualTo(1);
            assertThat(result.getTotalErrors()).isEqualTo(1);
            assertThat(result.getErrors().getFirst().getLotId()).isEqualTo(71);
        }

        @Test
        @DisplayName("reprend le numero de lot dans le message d erreur quand il est lisible")
        void numeroDeLotDansLErreur() {
            RetourBonFromLotRequest ko = new RetourBonFromLotRequest().setLotId(70).setQuantity(99).setMotifRetourId(4);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 3)));

            RetourBonBatchResultDTO result = service.createFromExpiredLots(new RetourBonFromLotsRequest().setLots(List.of(ko)));

            assertThat(result.getErrors().getFirst().getLotNumero()).isEqualTo("L70");
        }

        @Test
        @DisplayName("un lot vide ne produit aucun retour")
        void batchVide() {
            RetourBonBatchResultDTO result = service.createFromExpiredLots(new RetourBonFromLotsRequest().setLots(List.of()));

            assertThat(result.getTotalCreated()).isZero();
            assertThat(result.getTotalErrors()).isZero();
        }
    }

    @Nested
    @DisplayName("resolveLot")
    class ResolveLot {

        @Test
        @DisplayName("refuse un lot inconnu")
        void lotInconnu() {
            when(lotRepository.findById(70)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolveLot(70)).isInstanceOf(GenericError.class).hasMessageContaining("Lot non trouvé: 70");
        }

        @Test
        @DisplayName("commande trouvee via la ligne de commande du lot")
        void commandeTrouvee() {
            OrderLine line = orderLine(900, 10, 0);
            line.setCommande(commande);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10).setOrderLine(line)));

            RetourBonLotResolutionDTO dto = service.resolveLot(70);

            assertThat(dto.getCommandeId()).isEqualTo(12);
            assertThat(dto.getCommandeOrderDate()).isEqualTo(ORDER_DATE);
            assertThat(dto.getCommandeReference()).isEqualTo("BL-0012");
        }

        @Test
        @DisplayName("hors commande : fournisseur principal du produit")
        void fournisseurPrincipal() {
            Lot lot = lot(70, 10);
            FournisseurProduit fp = new FournisseurProduit();
            fp.setFournisseur(fournisseur(3, "LABOREX"));
            lot.getProduit().setFournisseurProduitPrincipal(fp);
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot));

            RetourBonLotResolutionDTO dto = service.resolveLot(70);

            assertThat(dto.getFournisseurId()).isEqualTo(3);
            assertThat(dto.getFournisseurLibelle()).isEqualTo("LABOREX");
        }

        @Test
        @DisplayName("hors commande : unique fournisseur reference")
        void fournisseurUnique() {
            FournisseurProduit fp = new FournisseurProduit();
            fp.setFournisseur(fournisseur(3, "LABOREX"));
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(fournisseurProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(List.of(fp));

            assertThat(service.resolveLot(70).getFournisseurId()).isEqualTo(3);
        }

        @Test
        @DisplayName("hors commande : plusieurs fournisseurs a proposer")
        void plusieursFournisseurs() {
            FournisseurProduit fp1 = new FournisseurProduit();
            fp1.setFournisseur(fournisseur(3, "LABOREX"));
            FournisseurProduit fp2 = new FournisseurProduit();
            fp2.setFournisseur(fournisseur(4, "COPHARMED"));
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(fournisseurProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(List.of(fp1, fp2));

            assertThat(service.resolveLot(70).getFournisseurs()).hasSize(2);
        }

        @Test
        @DisplayName("hors commande : aucun fournisseur connu")
        void aucunFournisseur() {
            when(lotRepository.findById(70)).thenReturn(Optional.of(lot(70, 10)));
            when(fournisseurProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(List.of());

            assertThat(service.resolveLot(70).getFournisseurs()).isNull();
            assertThat(service.resolveLot(70).getFournisseurId()).isNull();
        }
    }

    @Nested
    @DisplayName("createFromBonLignes")
    class CreateFromBonLignes {

        private AvoirFromBonLignesCommand commande(Integer prixAchat) {
            return new AvoirFromBonLignesCommand(
                12,
                ORDER_DATE,
                "retour BL",
                List.of(new BonLigneItem(900, ORDER_DATE, PRODUIT_ID, "1234567", 4, 4, prixAchat))
            );
        }

        @Test
        @DisplayName("refuse une commande inconnue")
        void commandeInconnue() {
            when(commandeRepository.findById(any(CommandeId.class))).thenReturn(Optional.empty());
            AvoirFromBonLignesCommand cmd = commande(null);

            assertThatThrownBy(() -> service.createFromBonLignes(cmd))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Commande introuvable: 12");
        }

        @Test
        @DisplayName("cree le retour puis l avoir a partir de ses lignes")
        void creeRetourEtAvoir() {
            OrderLine line = orderLine(900, 10, 0);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            stubStock(20);
            RetourBon saved = retourBon(RetourStatut.VALIDATED);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(saved));
            RetourBonItem item = new RetourBonItem();
            item.setId(60);
            item.setQtyMvt(4);
            item.setPrixAchat(400);
            when(retourBonItemRepository.findAllByRetourBonId(31)).thenReturn(List.of(item));
            AvoirFournisseurDTO expected = new AvoirFournisseurDTO();
            when(avoirFournisseurService.createFromRetourBon(any(), any(), anyString())).thenReturn(expected);

            assertThat(service.createFromBonLignes(commande(null))).isSameAs(expected);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<AvoirFournisseurCommand.AvoirLigneCommand>> captor = ArgumentCaptor.forClass(List.class);
            verify(avoirFournisseurService).createFromRetourBon(org.mockito.ArgumentMatchers.eq(saved), captor.capture(), anyString());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().getFirst().qtyAcceptee()).isEqualTo(4);
            assertThat(captor.getValue().getFirst().prixAchat()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("createFromReception")
    class CreateFromReception {

        private AvoirFromBonLignesCommand commande(Integer prixAchat) {
            return new AvoirFromBonLignesCommand(
                12,
                ORDER_DATE,
                "retour reception",
                List.of(new BonLigneItem(900, ORDER_DATE, PRODUIT_ID, "1234567", 4, 4, prixAchat))
            );
        }

        private void stubAvoir() {
            RetourBon saved = retourBon(RetourStatut.VALIDATED);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(saved));
            RetourBonItem item = new RetourBonItem();
            item.setId(60);
            item.setQtyMvt(4);
            item.setPrixAchat(400);
            when(retourBonItemRepository.findAllByRetourBonId(31)).thenReturn(List.of(item));
            when(avoirFournisseurService.createFromRetourBon(any(), any(), anyString())).thenReturn(new AvoirFournisseurDTO());
        }

        @Test
        @DisplayName("valorise l avoir a partir des lignes du retour enregistre")
        void valoriseLAvoir() {
            OrderLine line = orderLine(900, 10, 0);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            stubAvoir();

            service.createFromReception(commande(null));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<AvoirFournisseurCommand.AvoirLigneCommand>> captor = ArgumentCaptor.forClass(List.class);
            verify(avoirFournisseurService).createFromRetourBon(any(), captor.capture(), anyString());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().getFirst().retourBonItemId()).isEqualTo(60);
            assertThat(captor.getValue().getFirst().qtyAcceptee()).isEqualTo(4);
            assertThat(captor.getValue().getFirst().prixAchat()).isEqualTo(400);
        }

        @Test
        @DisplayName("refuse une commande inconnue")
        void commandeInconnue() {
            when(commandeRepository.findById(any(CommandeId.class))).thenReturn(Optional.empty());
            AvoirFromBonLignesCommand cmd = commande(null);

            assertThatThrownBy(() -> service.createFromReception(cmd))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Commande introuvable: 12");
        }

        @Test
        @DisplayName("refuse une ligne de commande inconnue")
        void ligneInconnue() {
            when(orderLineRepository.findById(any(OrderLineId.class))).thenReturn(Optional.empty());
            AvoirFromBonLignesCommand cmd = commande(null);

            assertThatThrownBy(() -> service.createFromReception(cmd))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ligne de commande introuvable: 900");
        }

        @Test
        @DisplayName("marque le retour hors stock et ne touche pas au stock")
        void horsStock() {
            OrderLine line = orderLine(900, 10, 0);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            stubAvoir();

            service.createFromReception(commande(null));

            ArgumentCaptor<RetourBon> captor = ArgumentCaptor.forClass(RetourBon.class);
            verify(retourBonRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getValue().isHorsStock()).isTrue();
            verify(stockProduitRepository, never()).save(any());
            verify(inventoryTransactionService, never()).save(any(RetourBonItem.class));
        }

        @Test
        @DisplayName("decremente la quantite recue et cumule la quantite retournee")
        void ajusteLaLigneDeCommande() {
            OrderLine line = orderLine(900, 10, 2);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            stubAvoir();

            service.createFromReception(commande(null));

            assertThat(line.getQuantityReceived()).isEqualTo(6);
            assertThat(line.getQuantityReturned()).isEqualTo(6);
            verify(orderLineRepository).save(line);
        }

        @Test
        @DisplayName("des quantites nulles sur la ligne sont traitees comme zero")
        void quantitesNulles() {
            OrderLine line = orderLine(900, null, null);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            AvoirFromBonLignesCommand cmd = new AvoirFromBonLignesCommand(
                12,
                ORDER_DATE,
                "retour",
                List.of(new BonLigneItem(900, ORDER_DATE, PRODUIT_ID, "1234567", 0, 4, null))
            );
            stubAvoir();

            service.createFromReception(cmd);

            assertThat(line.getQuantityReceived()).isZero();
            assertThat(line.getQuantityReturned()).isZero();
        }

        @Test
        @DisplayName("le prix transmis prime sur celui de la ligne de commande")
        void prixTransmis() {
            OrderLine line = orderLine(900, 10, 0);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            stubAvoir();

            service.createFromReception(commande(650));

            ArgumentCaptor<RetourBonItem> captor = ArgumentCaptor.forClass(RetourBonItem.class);
            verify(retourBonItemRepository).save(captor.capture());
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(650);
            assertThat(captor.getValue().getInitStock()).isEqualTo(10);
            assertThat(captor.getValue().getAfterStock()).isEqualTo(6);
        }

        @Test
        @DisplayName("refuse une quantite superieure au retournable")
        void quantiteNonRetournable() {
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(orderLine(900, 10, 0)));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(8);
            AvoirFromBonLignesCommand cmd = commande(null);

            assertThatThrownBy(() -> service.createFromReception(cmd))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("dépasse la quantité retournable (2)");
        }
    }

    @Nested
    @DisplayName("createRetourCompletFromCommande")
    class CreateRetourCompletFromCommande {

        private RetourCompletCommandeRequest requete() {
            return new RetourCompletCommandeRequest().setCommandeId(12).setCommandeOrderDate(ORDER_DATE).setMotifRetourId(4);
        }

        @Test
        @DisplayName("refuse une commande inconnue")
        void commandeInconnue() {
            when(commandeRepository.findById(any(CommandeId.class))).thenReturn(Optional.empty());
            RetourCompletCommandeRequest request = requete();

            assertThatThrownBy(() -> service.createRetourCompletFromCommande(request))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Commande introuvable: 12");
        }

        @Test
        @DisplayName("refuse une commande sans ligne recue")
        void aucuneLigneRecue() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(orderLine(900, 0, 0), orderLine(901, null, 0)));
            RetourCompletCommandeRequest request = requete();

            assertThatThrownBy(() -> service.createRetourCompletFromCommande(request))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Aucune ligne reçue trouvée");
        }

        @Test
        @DisplayName("retourne le solde retournable de chaque ligne")
        void retourneLeSolde() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(orderLine(900, 10, 0)));
            when(retourBonItemRepository.sumQtyMvtByOrderLineId(900, ORDER_DATE, null, RetourStatut.CLOSED)).thenReturn(4);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(orderLine(900, 10, 0)));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            stubStock(20);

            service.createRetourCompletFromCommande(requete());

            ArgumentCaptor<RetourBonItem> captor = ArgumentCaptor.forClass(RetourBonItem.class);
            verify(retourBonItemRepository).save(captor.capture());
            assertThat(captor.getValue().getQtyMvt()).isEqualTo(6);
        }

        @Test
        @DisplayName("saute les lignes deja entierement retournees")
        void sauteLesLignesEpuisees() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(orderLine(900, 10, 0)));
            when(retourBonItemRepository.sumQtyMvtByOrderLineId(900, ORDER_DATE, null, RetourStatut.CLOSED)).thenReturn(10);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));

            service.createRetourCompletFromCommande(requete());

            verify(retourBonItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("tolere une ligne sans produit fournisseur")
        void ligneSansProduitFournisseur() {
            OrderLine sansFp = orderLine(900, 10, 0);
            sansFp.setFournisseurProduit(null);
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE)).thenReturn(List.of(sansFp));
            when(retourBonItemRepository.sumQtyMvtByOrderLineId(900, ORDER_DATE, null, RetourStatut.CLOSED)).thenReturn(0);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(sansFp));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, null))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 20)));

            service.createRetourCompletFromCommande(requete());

            verify(retourBonItemRepository).save(any(RetourBonItem.class));
        }

        @Test
        @DisplayName("tolere un produit fournisseur sans produit")
        void produitFournisseurSansProduit() {
            OrderLine ligne = orderLine(900, 10, 0);
            ligne.getFournisseurProduit().setProduit(null);
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE)).thenReturn(List.of(ligne));
            when(retourBonItemRepository.sumQtyMvtByOrderLineId(900, ORDER_DATE, null, RetourStatut.CLOSED)).thenReturn(0);
            when(orderLineRepository.findById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(ligne));
            when(retourBonItemRepository.sumAllReturnedQtyByOrderLineId(900, ORDER_DATE, 31)).thenReturn(0);
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(retourBon(RetourStatut.VALIDATED)));
            when(stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(MAGASIN_ID, null))
                .thenReturn(List.of(stock(StorageType.PRINCIPAL, 20)));

            service.createRetourCompletFromCommande(requete());

            verify(retourBonItemRepository).save(any(RetourBonItem.class));
        }
    }

    @Nested
    @DisplayName("findAllGroupedByFournisseur")
    class FindAllGroupedByFournisseur {

        @Test
        @DisplayName("regroupe les retours ouverts par fournisseur, tries par libelle")
        @SuppressWarnings("unchecked")
        void regroupeParFournisseur() {
            Commande autre = commande();
            autre.setFournisseur(fournisseur(4, "COPHARMED"));
            RetourBon bon1 = retourBon(RetourStatut.VALIDATED);
            RetourBon bon2 = retourBon(RetourStatut.PROCESSING);
            bon2.setCommande(autre);
            when(retourBonRepository.findAll(any(Specification.class))).thenReturn(List.of(bon1, bon2));

            List<RetourBonGroupeDTO> groupes = service.findAllGroupedByFournisseur();

            assertThat(groupes).extracting(RetourBonGroupeDTO::getFournisseurLibelle).containsExactly("COPHARMED", "LABOREX");
        }

        @Test
        @DisplayName("ecarte les retours sans fournisseur identifiable")
        @SuppressWarnings("unchecked")
        void ecarteLesRetoursSansFournisseur() {
            RetourBon sansFournisseur = retourBon(RetourStatut.VALIDATED);
            sansFournisseur.setCommande(null);
            when(retourBonRepository.findAll(any(Specification.class))).thenReturn(List.of(sansFournisseur));

            assertThat(service.findAllGroupedByFournisseur()).isEmpty();
        }

        @Test
        @DisplayName("aucun retour ouvert : liste vide")
        @SuppressWarnings("unchecked")
        void aucunRetourOuvert() {
            when(retourBonRepository.findAll(any(Specification.class))).thenReturn(List.of());

            assertThat(service.findAllGroupedByFournisseur()).isEmpty();
        }
    }
}
