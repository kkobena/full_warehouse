package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduitPriceHistory;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotReception;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PutawayMode;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurProduitPriceHistoryRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.LotReceptionRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.PriceHistoryDTO;
import com.kobe.warehouse.service.dto.PutawayPreviewItemDTO;
import com.kobe.warehouse.service.dto.ReceptionScanResultDTO;
import com.kobe.warehouse.service.dto.StockEntryResultDTO;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.rupture.service.RuptureService;
import com.kobe.warehouse.service.sale.AvoirClientDocumentService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import com.kobe.warehouse.service.stock.DataMatrixParserService.BarcodeType;
import com.kobe.warehouse.service.stock.ImportationEchoueService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.stock.ProduitService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockEntryServiceImpl")
class StockEntryServiceImplTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 4, 18);
    private static final CommandeId COMMANDE_ID = new CommandeId(12, ORDER_DATE);
    private static final int PRODUIT_ID = 500;

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private ProduitService produitService;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private StorageService storageService;

    @Mock
    private FournisseurProduitService fournisseurProduitService;

    @Mock
    private LogsService logsService;

    @Mock
    private FournisseurRepository fournisseurRepository;

    @Mock
    private OrderLineService orderLineService;

    @Mock
    private CommandeIdGeneratorService commandeIdGeneratorService;

    @Mock
    private OrderLineIdGeneratorService orderLineIdGeneratorService;

    @Mock
    private ImportationEchoueService importationEchoueService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private LotRepository lotRepository;

    @Mock
    private LotReceptionRepository lotReceptionRepository;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private FournisseurProduitPriceHistoryRepository priceHistoryRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private LotStockLocationService lotStockLocationService;

    @Mock
    private SuggestionReassortService suggestionReassortService;

    @Mock
    private RuptureService ruptureService;

    @Mock
    private DataMatrixParserService dataMatrixParserService;

    @Mock
    private AvoirClientDocumentService avoirClientDocumentService;

    @Mock
    private MultipartFile multipartFile;

    private StockEntryServiceImpl service;

    private Magasin magasin;
    private Storage principal;
    private Storage reserve;
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        service = new StockEntryServiceImpl(
            commandeRepository,
            produitService,
            referenceService,
            storageService,
            fournisseurProduitService,
            logsService,
            fournisseurRepository,
            orderLineService,
            commandeIdGeneratorService,
            orderLineIdGeneratorService,
            importationEchoueService,
            inventoryTransactionService,
            lotRepository,
            lotReceptionRepository,
            appConfigurationService,
            priceHistoryRepository,
            orderLineRepository,
            lotStockLocationService,
            suggestionReassortService,
            ruptureService,
            dataMatrixParserService,
            avoirClientDocumentService
        );

        magasin = new Magasin();
        magasin.setId(1);
        principal = storage(2, StorageType.PRINCIPAL);
        reserve = storage(3, StorageType.SAFETY_STOCK);
        currentUser = new AppUser();
        currentUser.setId(9);
        currentUser.setLogin("awa");

        when(storageService.getUser()).thenReturn(currentUser);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(principal);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
        when(appConfigurationService.useLot()).thenReturn(Optional.of(false));
        when(appConfigurationService.getReceptionMinExpiryDays()).thenReturn(90L);
        when(appConfigurationService.getPutawayMode()).thenReturn(PutawayMode.ALL_RAYON);
        when(appConfigurationService.getSeuilVariationPrix()).thenReturn(10);
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandeRepository.saveAndFlush(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandeRepository.getReferenceById(any(CommandeId.class))).thenAnswer(inv -> commande(OrderStatut.RECEIVED));
        when(orderLineService.save(any(OrderLine.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lotRepository.save(any(Lot.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Storage storage(int id, StorageType type) {
        Storage s = new Storage();
        s.setId(id);
        s.setStorageType(type);
        s.setMagasin(magasin);
        return s;
    }

    private static Produit produit() {
        Produit p = new Produit();
        p.setId(PRODUIT_ID);
        p.setLibelle("DOLIPRANE 1000MG");
        p.setGestionLot(false);
        p.setCheckExpiryDate(false);
        p.setStockProduits(new HashSet<>());
        return p;
    }

    private static FournisseurProduit fournisseurProduit(Produit produit) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(77);
        fp.setCodeCip("1234567");
        fp.setPrixAchat(400);
        fp.setPrixUni(800);
        fp.setProduit(produit);
        return fp;
    }

    private Commande commande(OrderStatut statut) {
        Commande c = new Commande();
        c.setId(12);
        c.setOrderDate(ORDER_DATE);
        c.setReceiptDate(ORDER_DATE);
        c.setReceiptReference("BL-0012");
        c.setOrderStatus(statut);
        c.setGrossAmount(1000);
        c.setTaxAmount(100);
        c.setHtAmount(900);
        c.setOrderLines(new ArrayList<>());
        Fournisseur f = new Fournisseur();
        f.setId(3);
        f.setLibelle("LABOREX");
        c.setFournisseur(f);
        return c;
    }

    private static OrderLine orderLine(Commande commande, Produit produit, int requested, Integer received) {
        OrderLine line = new OrderLine();
        line.setId(900);
        line.setOrderDate(ORDER_DATE);
        line.setCommande(commande);
        line.setFournisseurProduit(fournisseurProduit(produit));
        line.setQuantityRequested(requested);
        line.setQuantityReceived(received);
        line.setOrderCostAmount(400);
        line.setOrderUnitPrice(800);
        line.setFreeQty(0);
        line.setTaxAmount(0);
        line.setInitStock(0);
        line.setUpdated(Boolean.TRUE);
        line.setLots(new ArrayList<>());
        commande.getOrderLines().add(line);
        return line;
    }

    private static StockProduit stockProduit(int qtyStock, Integer qtyUg) {
        StockProduit sp = new StockProduit();
        sp.setQtyStock(qtyStock);
        sp.setQtyUG(qtyUg);
        return sp;
    }

    private static Lot lot(String numLot, LocalDate expiry, int quantity) {
        Lot lot = new Lot().setNumLot(numLot).setExpiryDate(expiry).setQuantity(quantity).setCurrentQuantity(quantity).setFreeQty(0);
        lot.setPrixAchat(400);
        return lot;
    }

    private static DeliveryReceiptLiteDTO liteDTO() {
        return new DeliveryReceiptLiteDTO()
            .setCommandeId(COMMANDE_ID)
            .setReceiptReference("BL-0012")
            .setReceiptDate(ORDER_DATE)
            .setReceiptAmount(1000)
            .setTaxAmount(100);
    }

    private static DeliveryReceiptItemLiteDTO itemDTO() {
        return new DeliveryReceiptItemLiteDTO().setOrderLineId(new OrderLineId(900, ORDER_DATE));
    }

    /** Prépare une réception finalisable : une ligne conforme, stock et produit résolus. */
    private OrderLine prepareFinalisable(Commande commande) {
        Produit produit = produit();
        OrderLine line = orderLine(commande, produit, 10, 10);
        when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(commande);
        when(produitService.updateTotalStock(any(Produit.class), anyInt(), anyInt())).thenReturn(stockProduit(20, 0));
        when(produitService.getProductTotalStock(PRODUIT_ID)).thenReturn(10);
        return line;
    }

    @Nested
    @DisplayName("finalizeSaisieEntreeStock")
    class FinalizeSaisie {

        @Test
        @DisplayName("refuse un bon deja finalise")
        void bonDejaFinalise() {
            when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(commande(OrderStatut.CLOSED));
            DeliveryReceiptLiteDTO dto = liteDTO();

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("est déjà finalisé");
        }

        @Test
        @DisplayName("cloture le bon, journalise l entree et rattache les avoirs")
        void clotureLeBon() {
            Commande commande = commande(OrderStatut.RECEIVED);
            prepareFinalisable(commande);

            StockEntryResultDTO result = service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
            assertThat(result.getCommandeId()).isEqualTo(commande.getId());
            assertThat(result.getPendingRetourBons()).isEmpty();
            verify(logsService).create(
                org.mockito.ArgumentMatchers.eq(TransactionType.ENTREE_STOCK),
                org.mockito.ArgumentMatchers.eq("order.entry"),
                any(Object[].class),
                org.mockito.ArgumentMatchers.eq("12")
            );
            verify(inventoryTransactionService).saveAll(commande.getOrderLines());
            verify(avoirClientDocumentService).linkCommandeToAvoirs(commande);
        }

        @Test
        @DisplayName("horodate chaque ligne a la date de reception")
        void horodateLesLignes() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(line.getReceiptDate()).isEqualTo(ORDER_DATE.atStartOfDay());
        }

        @Test
        @DisplayName("une reception conforme sans lot sert la quantite commandee")
        void conformeSansLot() {
            Commande commande = commande(OrderStatut.RECEIVED);
            Produit produit = produit();
            OrderLine line = orderLine(commande, produit, 10, null);
            line.setUpdated(Boolean.FALSE);
            when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(commande);
            when(produitService.updateTotalStock(any(Produit.class), anyInt(), anyInt())).thenReturn(stockProduit(20, 0));

            service.finalizeSaisieEntreeStock(liteDTO().setConformeSansLot(true));

            assertThat(line.getQuantityReceived()).isEqualTo(10);
            assertThat(line.getUpdated()).isTrue();
        }

        @Test
        @DisplayName("une ligne deja saisie n est pas ecrasee par le mode conforme")
        void conformeSansLotNEcrasePas() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.setQuantityReceived(4);

            service.finalizeSaisieEntreeStock(liteDTO().setConformeSansLot(true));

            assertThat(line.getQuantityReceived()).isEqualTo(4);
        }

        @Test
        @DisplayName("refuse une ligne sans code CIP")
        void codeCipManquant() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().setCodeCip(null);

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(liteDTO()))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Code cip non renseigné");
        }

        @Test
        @DisplayName("refuse une ligne saisie sans quantite recue")
        void quantiteRecueManquante() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.setQuantityReceived(null);

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(liteDTO()))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("La reception de certains produits");
        }

        @Test
        @DisplayName("refuse une quantite recue negative")
        void quantiteRecueNegative() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.setQuantityReceived(-1);

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(liteDTO()))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("La reception de certains produits");
        }

        @Test
        @DisplayName("gestion de lot active mais produit hors suivi de lot : aucun controle")
        void produitHorsSuiviDeLot() {
            Commande commande = commande(OrderStatut.RECEIVED);
            prepareFinalisable(commande);
            when(appConfigurationService.useLot()).thenReturn(Optional.of(true));

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
        }

        @Test
        @DisplayName("un produit a controle de peremption sans lot passe le seuil")
        void controleDePeremptionSansLot() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setCheckExpiryDate(true);

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
        }

        @Test
        @DisplayName("accepte une reception partielle a zero")
        void receptionPartielleAZero() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.setQuantityReceived(0);

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
        }

        @Test
        @DisplayName("accepte une ligne non saisie quand la reception n est pas modifiee")
        void ligneNonModifiee() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.setUpdated(Boolean.FALSE);
            line.setQuantityReceived(null);

            service.finalizeSaisieEntreeStock(liteDTO());

            // saveItem retombe sur la quantite commandee
            assertThat(line.getQuantityReceived()).isEqualTo(10);
        }

        @Test
        @DisplayName("refuse un produit a suivi de lot sans lot renseigne")
        void lotManquant() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setGestionLot(true);
            when(appConfigurationService.useLot()).thenReturn(Optional.of(true));

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(liteDTO()))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Tous les lots ne sont renseignés");
        }

        @Test
        @DisplayName("refuse un lot sans date de peremption")
        void lotSansDatePeremption() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setGestionLot(true);
            line.getLots().add(lot("L1", null, 10));
            when(appConfigurationService.useLot()).thenReturn(Optional.of(true));

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(liteDTO()))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Tous les lots ne sont renseignés");
        }

        @Test
        @DisplayName("refuse des lots dont la somme est inferieure a la quantite recue")
        void lotsIncomplets() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setGestionLot(true);
            line.getLots().add(lot("L1", LocalDate.now().plusYears(2), 4));
            when(appConfigurationService.useLot()).thenReturn(Optional.of(true));

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(liteDTO()))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Tous les lots ne sont renseignés");
        }

        @Test
        @DisplayName("accepte des lots complets et les rend disponibles")
        void lotsComplets() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setGestionLot(true);
            Lot lot = lot("L1", LocalDate.now().plusYears(2), 10);
            line.getLots().add(lot);
            when(appConfigurationService.useLot()).thenReturn(Optional.of(true));

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(lot.getStatut()).isEqualTo(StatutLot.AVAILABLE);
            assertThat(lot.getUpdated()).isNotNull();
            verify(lotRepository).save(lot);
            verify(lotStockLocationService).credit(lot, principal, 10);
            ArgumentCaptor<LotReception> captor = ArgumentCaptor.forClass(LotReception.class);
            verify(lotReceptionRepository).save(captor.capture());
            assertThat(captor.getValue().getQuantityReceived()).isEqualTo(10);
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(400);
            assertThat(captor.getValue().getReceiptDate()).isEqualTo(ORDER_DATE);
        }

        @Test
        @DisplayName("un lot sans prix d achat est receptionne a zero")
        void lotSansPrixAchat() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            Lot lot = lot("L1", LocalDate.now().plusYears(2), 10);
            lot.setPrixAchat(null);
            line.getLots().add(lot);

            service.finalizeSaisieEntreeStock(liteDTO());

            ArgumentCaptor<LotReception> captor = ArgumentCaptor.forClass(LotReception.class);
            verify(lotReceptionRepository).save(captor.capture());
            assertThat(captor.getValue().getPrixAchat()).isZero();
        }

        @Test
        @DisplayName("le controle de lot est ignore quand la gestion de lot est inactive")
        void gestionLotInactive() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setGestionLot(true);
            when(appConfigurationService.useLot()).thenReturn(Optional.empty());

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
        }

        @Test
        @DisplayName("refuse un lot dont la peremption est sous le seuil minimum")
        void peremptionTropProche() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setCheckExpiryDate(true);
            line.getLots().add(lot("L1", LocalDate.now().plusDays(10), 10));

            assertThatThrownBy(() -> service.finalizeSaisieEntreeStock(liteDTO()))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("seuil minimum de 90 jours");
        }

        @Test
        @DisplayName("un lot sans date de peremption ne declenche pas le controle de seuil")
        void lotSansDateIgnoreLeSeuil() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getFournisseurProduit().getProduit().setCheckExpiryDate(true);
            line.getLots().add(lot("L1", null, 10));

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
        }

        @Test
        @DisplayName("le controle de seuil est ignore quand le produit ne suit pas la peremption")
        void sansControleDePeremption() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.getLots().add(lot("L1", LocalDate.now().plusDays(1), 10));

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
        }

        @Test
        @DisplayName("calcule le stock initial et final de la ligne")
        void stocksDeLaLigne() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.setFreeQty(2);

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(line.getInitStock()).isEqualTo(10);
            assertThat(line.getFinalStock()).isEqualTo(22);
        }

        @Test
        @DisplayName("reporte la TVA de la ligne sur le produit")
        void reporteLaTva() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            Tva tva = new Tva().setId(5);
            line.setTva(tva);

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(line.getFournisseurProduit().getProduit().getTva()).isSameAs(tva);
        }

        @Test
        @DisplayName("calcule le prix moyen pondere et debloque les ruptures")
        void prixMoyenPondereEtRuptures() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            Produit produit = line.getFournisseurProduit().getProduit();
            when(produitService.calculPrixMoyenPondereReception(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(450);

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(produit.getPrixMnp()).isEqualTo(450);
            verify(ruptureService).markProductAsBackInStock(produit);
            verify(produitService).update(produit);
        }

        @Test
        @DisplayName("un stock final nul ne recalcule pas le prix moyen")
        void stockFinalNul() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            when(produitService.updateTotalStock(any(Produit.class), anyInt(), anyInt())).thenReturn(stockProduit(0, 0));

            service.finalizeSaisieEntreeStock(liteDTO());

            assertThat(line.getFournisseurProduit().getProduit().getPrixMnp()).isZero();
            verifyNoInteractions(ruptureService);
            verify(produitService, never()).calculPrixMoyenPondereReception(anyInt(), anyInt(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("des UG nulles sur le stock comptent pour zero")
        void stockSansUg() {
            Commande commande = commande(OrderStatut.RECEIVED);
            prepareFinalisable(commande);
            when(produitService.updateTotalStock(any(Produit.class), anyInt(), anyInt())).thenReturn(stockProduit(5, null));
            when(produitService.calculPrixMoyenPondereReception(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(450);

            service.finalizeSaisieEntreeStock(liteDTO());

            verify(produitService).calculPrixMoyenPondereReception(anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(5), anyInt());
        }
    }

    @Nested
    @DisplayName("historique de prix fournisseur")
    class HistoriqueDePrix {

        private Commande prepare(int orderCost, int orderUnit) {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            line.setOrderCostAmount(orderCost);
            line.setOrderUnitPrice(orderUnit);
            return commande;
        }

        @Test
        @DisplayName("aucun historique quand les prix sont inchanges")
        void prixInchanges() {
            prepare(400, 800);

            service.finalizeSaisieEntreeStock(liteDTO());

            verify(priceHistoryRepository, never()).save(any());
            verify(fournisseurProduitService).update(any(FournisseurProduit.class));
        }

        @Test
        @DisplayName("trace et applique un changement de prix d achat")
        void prixAchatChange() {
            Commande commande = prepare(450, 800);

            service.finalizeSaisieEntreeStock(liteDTO());

            ArgumentCaptor<FournisseurProduitPriceHistory> captor = ArgumentCaptor.forClass(FournisseurProduitPriceHistory.class);
            verify(priceHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getOldPrixAchat()).isEqualTo(400);
            assertThat(captor.getValue().getNewPrixAchat()).isEqualTo(450);
            assertThat(captor.getValue().getOldPrixUni()).isEqualTo(800);
            assertThat(captor.getValue().getNewPrixUni()).isEqualTo(800);
            assertThat(captor.getValue().getChangedBy()).isSameAs(currentUser);
            assertThat(captor.getValue().getReceiptReference()).isEqualTo("BL-0012");
            assertThat(commande.getOrderLines().getFirst().getFournisseurProduit().getPrixAchat()).isEqualTo(450);
        }

        @Test
        @DisplayName("trace et applique un changement de prix de vente")
        void prixVenteChange() {
            Commande commande = prepare(400, 900);

            service.finalizeSaisieEntreeStock(liteDTO());

            ArgumentCaptor<FournisseurProduitPriceHistory> captor = ArgumentCaptor.forClass(FournisseurProduitPriceHistory.class);
            verify(priceHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getNewPrixUni()).isEqualTo(900);
            assertThat(commande.getOrderLines().getFirst().getFournisseurProduit().getPrixUni()).isEqualTo(900);
        }

        @Test
        @DisplayName("le montant du tableau du produit est neutre sur la comparaison")
        void avecTableau() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = prepareFinalisable(commande);
            com.kobe.warehouse.domain.Tableau tableau = new com.kobe.warehouse.domain.Tableau();
            tableau.setValue(50);
            line.getFournisseurProduit().getProduit().setTableau(tableau);

            service.finalizeSaisieEntreeStock(liteDTO());

            verify(priceHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("applyPutawayPolicy")
    class ApplyPutawayPolicy {

        @Test
        @DisplayName("le mode AUTO declenche le transfert vers la reserve")
        void modeAuto() {
            Commande commande = commande(OrderStatut.RECEIVED);
            prepareFinalisable(commande);
            when(appConfigurationService.getPutawayMode()).thenReturn(PutawayMode.AUTO);

            service.finalizeSaisieEntreeStock(liteDTO());

            verify(suggestionReassortService).autoExecuteOverflowForProducts(Set.of(PRODUIT_ID));
        }

        @Test
        @DisplayName("le mode MANUAL ne transfere que sur confirmation")
        void modeManuelConfirme() {
            Commande commande = commande(OrderStatut.RECEIVED);
            prepareFinalisable(commande);
            when(appConfigurationService.getPutawayMode()).thenReturn(PutawayMode.MANUAL);

            service.finalizeSaisieEntreeStock(liteDTO().setDoTransfer(true));

            verify(suggestionReassortService).autoExecuteOverflowForProducts(Set.of(PRODUIT_ID));
        }

        @Test
        @DisplayName("le mode MANUAL sans confirmation ne transfere rien")
        void modeManuelSansConfirmation() {
            Commande commande = commande(OrderStatut.RECEIVED);
            prepareFinalisable(commande);
            when(appConfigurationService.getPutawayMode()).thenReturn(PutawayMode.MANUAL);

            service.finalizeSaisieEntreeStock(liteDTO());

            verifyNoInteractions(suggestionReassortService);
        }

        @Test
        @DisplayName("le mode ALL_RAYON ne transfere jamais")
        void modeAllRayon() {
            Commande commande = commande(OrderStatut.RECEIVED);
            prepareFinalisable(commande);

            service.finalizeSaisieEntreeStock(liteDTO());

            verifyNoInteractions(suggestionReassortService);
        }
    }

    @Nested
    @DisplayName("findLignesAvecEcartPrix")
    class FindLignesAvecEcartPrix {

        private OrderLine ligneAvecPrix(int orderCost, int prixAchatActuel) {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = orderLine(commande, produit(), 10, 10);
            line.setOrderCostAmount(orderCost);
            line.getFournisseurProduit().setPrixAchat(prixAchatActuel);
            return line;
        }

        @Test
        @DisplayName("remonte les lignes dont l ecart depasse le seuil")
        void ecartAuDelaDuSeuil() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(ligneAvecPrix(400, 500)));

            List<OrderLineDTO> lignes = service.findLignesAvecEcartPrix(12, ORDER_DATE);

            assertThat(lignes).hasSize(1);
            assertThat(lignes.getFirst().getOrderCostAmount()).isEqualTo(400);
            assertThat(lignes.getFirst().getCostAmount()).isEqualTo(500);
        }

        @Test
        @DisplayName("ecarte les lignes sous le seuil")
        void ecartSousLeSeuil() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(ligneAvecPrix(400, 420)));

            assertThat(service.findLignesAvecEcartPrix(12, ORDER_DATE)).isEmpty();
        }

        @Test
        @DisplayName("ecarte les lignes au prix identique")
        void prixIdentique() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(ligneAvecPrix(400, 400)));

            assertThat(service.findLignesAvecEcartPrix(12, ORDER_DATE)).isEmpty();
        }

        @Test
        @DisplayName("ecarte les lignes sans prix de commande")
        void prixDeCommandeNul() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(ligneAvecPrix(0, 400)));

            assertThat(service.findLignesAvecEcartPrix(12, ORDER_DATE)).isEmpty();
        }

        @Test
        @DisplayName("un ecart a la baisse est detecte aussi")
        void ecartALaBaisse() {
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE))
                .thenReturn(List.of(ligneAvecPrix(400, 300)));

            assertThat(service.findLignesAvecEcartPrix(12, ORDER_DATE)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getPriceHistory")
    class GetPriceHistory {

        private FournisseurProduitPriceHistory historique(int id, AppUser user) {
            return new FournisseurProduitPriceHistory()
                .setOldPrixAchat(400)
                .setNewPrixAchat(450)
                .setOldPrixUni(800)
                .setNewPrixUni(900)
                .setChangedAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .setReceiptReference("BL-0012")
                .setChangedBy(user);
        }

        @Test
        @DisplayName("mappe l historique et son auteur")
        void mappeLHistorique() {
            when(priceHistoryRepository.findByFournisseurProduitIdOrderByChangedAtDesc(77))
                .thenReturn(List.of(historique(1, currentUser)));

            List<PriceHistoryDTO> historique = service.getPriceHistory(77);

            assertThat(historique).hasSize(1);
            assertThat(historique.getFirst().oldPrixAchat()).isEqualTo(400);
            assertThat(historique.getFirst().newPrixAchat()).isEqualTo(450);
            assertThat(historique.getFirst().receiptReference()).isEqualTo("BL-0012");
            assertThat(historique.getFirst().changedBy()).isEqualTo("awa");
        }

        @Test
        @DisplayName("tolere un historique sans auteur")
        void sansAuteur() {
            when(priceHistoryRepository.findByFournisseurProduitIdOrderByChangedAtDesc(77))
                .thenReturn(List.of(historique(1, null)));

            assertThat(service.getPriceHistory(77).getFirst().changedBy()).isNull();
        }

        @Test
        @DisplayName("borne l historique aux 24 derniers changements")
        void limiteA24() {
            List<FournisseurProduitPriceHistory> lignes = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                lignes.add(historique(i, currentUser));
            }
            when(priceHistoryRepository.findByFournisseurProduitIdOrderByChangedAtDesc(77)).thenReturn(lignes);

            assertThat(service.getPriceHistory(77)).hasSize(24);
        }
    }

    @Nested
    @DisplayName("createBon et updateBon")
    class CreateEtUpdateBon {

        @Test
        @DisplayName("createBon passe le bon en RECEIVED et enregistre ses lignes")
        void createBon() {
            Commande commande = commande(OrderStatut.REQUESTED);
            when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(commande);

            DeliveryReceiptLiteDTO dto = service.createBon(liteDTO());

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.RECEIVED);
            assertThat(commande.getUser()).isSameAs(currentUser);
            assertThat(commande.getReceiptReference()).isEqualTo("BL-0012");
            assertThat(commande.getGrossAmount()).isEqualTo(1000);
            assertThat(commande.getTaxAmount()).isEqualTo(100);
            assertThat(commande.getHtAmount()).isEqualTo(1000);
            assertThat(commande.getFinalAmount()).isEqualTo(1000);
            assertThat(commande.getDiscountAmount()).isZero();
            verify(orderLineService).saveAll(commande.getOrderLines());
            assertThat(dto.getId()).isEqualTo(12);
            assertThat(dto.getReceiptAmount()).isEqualTo(1000);
        }

        @Test
        @DisplayName("updateBon met a jour le bon sans changer son statut")
        void updateBon() {
            Commande commande = commande(OrderStatut.RECEIVED);
            when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(commande);

            DeliveryReceiptLiteDTO dto = service.updateBon(liteDTO().setReceiptReference("BL-NEW"));

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.RECEIVED);
            assertThat(commande.getReceiptReference()).isEqualTo("BL-NEW");
            assertThat(dto.getReceiptReference()).isEqualTo("BL-NEW");
            verify(orderLineService, never()).saveAll(org.mockito.ArgumentMatchers.<List<OrderLine>>any());
        }
    }

    @Nested
    @DisplayName("mises a jour de ligne")
    class MisesAJourDeLigne {

        private OrderLine ligneEnregistree() {
            OrderLine line = orderLine(commande(OrderStatut.RECEIVED), produit(), 10, 5);
            when(orderLineService.findOneById(new OrderLineId(900, ORDER_DATE))).thenReturn(Optional.of(line));
            return line;
        }

        @Test
        @DisplayName("updateQuantityUG enregistre les unites gratuites")
        void updateQuantityUG() {
            OrderLine line = ligneEnregistree();

            service.updateQuantityUG(itemDTO().setQuantityUG(3));

            assertThat(line.getFreeQty()).isEqualTo(3);
            assertThat(line.getUpdated()).isTrue();
            assertThat(line.getUpdatedAt()).isNotNull();
            verify(orderLineService).save(line);
        }

        @Test
        @DisplayName("updateQuantityReceived enregistre la quantite saisie")
        void updateQuantityReceived() {
            OrderLine line = ligneEnregistree();

            service.updateQuantityReceived(itemDTO().setQuantityReceivedTmp(8));

            assertThat(line.getQuantityReceived()).isEqualTo(8);
            verify(orderLineService).save(line);
        }

        @Test
        @DisplayName("updateOrderUnitPrice enregistre le prix de vente")
        void updateOrderUnitPrice() {
            OrderLine line = ligneEnregistree();

            service.updateOrderUnitPrice(itemDTO().setOrderUnitPrice(950));

            assertThat(line.getOrderUnitPrice()).isEqualTo(950);
        }

        @Test
        @DisplayName("updateTva rattache la TVA transmise")
        void updateTva() {
            OrderLine line = ligneEnregistree();

            service.updateTva(itemDTO().setTvaId(5));

            assertThat(line.getTva().getId()).isEqualTo(5);
        }

        @Test
        @DisplayName("updateDatePeremption marque simplement la ligne comme saisie")
        void updateDatePeremption() {
            OrderLine line = ligneEnregistree();

            service.updateDatePeremption(itemDTO().setDatePeremptionTmp(LocalDate.now().plusYears(1)));

            assertThat(line.getUpdated()).isTrue();
            verify(orderLineService).save(line);
        }

        @Test
        @DisplayName("batchUpdateQuantityReceived applique chaque quantite a sa ligne")
        void batchUpdate() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine l1 = orderLine(commande, produit(), 10, 0);
            OrderLine l2 = orderLine(commande, produit(), 10, 0);
            l2.setId(901);
            when(orderLineService.findAllByOrderLineIdIn(Set.of(900, 901), ORDER_DATE)).thenReturn(List.of(l1, l2));

            service.batchUpdateQuantityReceived(
                List.of(
                    itemDTO().setQuantityReceivedTmp(4),
                    new DeliveryReceiptItemLiteDTO().setOrderLineId(new OrderLineId(901, ORDER_DATE)).setQuantityReceivedTmp(6)
                )
            );

            assertThat(l1.getQuantityReceived()).isEqualTo(4);
            assertThat(l2.getQuantityReceived()).isEqualTo(6);
            assertThat(l1.getUpdated()).isTrue();
            verify(orderLineService).saveAll(List.of(l1, l2));
        }

        @Test
        @DisplayName("batchUpdateQuantityReceived tolere une liste vide")
        void batchUpdateVide() {
            when(orderLineService.findAllByOrderLineIdIn(Set.of(), null)).thenReturn(List.of());

            service.batchUpdateQuantityReceived(List.of());

            verify(orderLineService).saveAll(List.of());
        }
    }

    @Nested
    @DisplayName("processScanReception")
    class ProcessScanReception {

        private OrderLine ligneDeCommande() {
            Commande commande = commande(OrderStatut.RECEIVED);
            OrderLine line = orderLine(commande, produit(), 10, 2);
            when(orderLineRepository.findFirstByCommandeIdAndCip(12, "3400930000000")).thenReturn(Optional.of(line));
            return line;
        }

        private void stubScan(DataMatrixInfo info, BarcodeType type) {
            when(dataMatrixParserService.parse(anyString())).thenReturn(Optional.ofNullable(info));
            when(dataMatrixParserService.detectBarcodeType(anyString())).thenReturn(type);
        }

        @Test
        @DisplayName("code illisible : aucun resultat")
        void codeIllisible() {
            stubScan(null, BarcodeType.UNKNOWN);

            ReceptionScanResultDTO result = service.processScanReception(12, "????");

            assertThat(result.found()).isFalse();
            assertThat(result.warningMessage()).isEqualTo("Format de code non reconnu");
            assertThat(result.fmdStatus()).isEqualTo(ReceptionScanResultDTO.FmdStatus.ABSENT);
            assertThat(result.scannedQty()).isEqualTo(1);
        }

        @Test
        @DisplayName("code sans identifiant produit : aucun resultat")
        void sansCodeProduit() {
            stubScan(DataMatrixInfo.builder().batchNumber("LOT1").build(), BarcodeType.DATAMATRIX);

            ReceptionScanResultDTO result = service.processScanReception(12, "10LOT1");

            assertThat(result.found()).isFalse();
            assertThat(result.warningMessage()).isEqualTo("Aucun code produit identifié dans le scan");
        }

        @Test
        @DisplayName("code produit blanc : aucun resultat")
        void codeProduitBlanc() {
            stubScan(DataMatrixInfo.builder().cip13("   ").build(), BarcodeType.DATAMATRIX);

            ReceptionScanResultDTO result = service.processScanReception(12, "…");

            assertThat(result.found()).isFalse();
            assertThat(result.warningMessage()).isEqualTo("Aucun code produit identifié dans le scan");
        }

        @Test
        @DisplayName("CIP absent de la commande")
        void cipAbsentDeLaCommande() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").build(), BarcodeType.CIP_13);
            when(orderLineRepository.findFirstByCommandeIdAndCip(12, "3400930000000")).thenReturn(Optional.empty());

            ReceptionScanResultDTO result = service.processScanReception(12, "3400930000000");

            assertThat(result.found()).isFalse();
            assertThat(result.produitCip()).isEqualTo("3400930000000");
            assertThat(result.warningMessage()).contains("absent de la commande");
        }

        @Test
        @DisplayName("incremente d une unite par defaut")
        void incrementParDefaut() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").scannedQty(0).build(), BarcodeType.CIP_13);
            OrderLine line = ligneDeCommande();

            ReceptionScanResultDTO result = service.processScanReception(12, "3400930000000");

            assertThat(result.found()).isTrue();
            assertThat(result.scannedQty()).isEqualTo(1);
            assertThat(line.getQuantityReceived()).isEqualTo(3);
            assertThat(result.produitLibelle()).isEqualTo("DOLIPRANE 1000MG");
            verify(orderLineService).save(line);
        }

        @Test
        @DisplayName("respecte la quantite portee par le DataMatrix")
        void quantiteDuDataMatrix() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").scannedQty(5).build(), BarcodeType.DATAMATRIX);
            OrderLine line = ligneDeCommande();

            ReceptionScanResultDTO result = service.processScanReception(12, "…");

            assertThat(result.scannedQty()).isEqualTo(5);
            assertThat(line.getQuantityReceived()).isEqualTo(7);
        }

        @Test
        @DisplayName("une ligne sans quantite recue part de zero")
        void ligneSansQuantiteRecue() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").scannedQty(0).build(), BarcodeType.CIP_13);
            OrderLine line = ligneDeCommande();
            line.setQuantityReceived(null);

            service.processScanReception(12, "3400930000000");

            assertThat(line.getQuantityReceived()).isEqualTo(1);
        }

        @Test
        @DisplayName("sans numero de serie, la tracabilite FMD est absente")
        void fmdAbsent() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").scannedQty(0).build(), BarcodeType.CIP_13);
            ligneDeCommande();

            ReceptionScanResultDTO result = service.processScanReception(12, "3400930000000");

            assertThat(result.fmdStatus()).isEqualTo(ReceptionScanResultDTO.FmdStatus.ABSENT);
            assertThat(result.warningMessage()).isNull();
        }

        @Test
        @DisplayName("un numero de serie blanc vaut absence de tracabilite")
        void fmdSerieBlanche() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").serialNumber("  ").scannedQty(0).build(), BarcodeType.DATAMATRIX);
            ligneDeCommande();

            assertThat(service.processScanReception(12, "…").fmdStatus()).isEqualTo(ReceptionScanResultDTO.FmdStatus.ABSENT);
        }

        @Test
        @DisplayName("un numero de serie inedit vaut tracabilite assuree")
        void fmdPresent() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").serialNumber("SN1").scannedQty(0).build(), BarcodeType.DATAMATRIX);
            ligneDeCommande();
            when(lotRepository.existsBySerialNumberAndProduitId("SN1", PRODUIT_ID)).thenReturn(false);

            ReceptionScanResultDTO result = service.processScanReception(12, "…");

            assertThat(result.fmdStatus()).isEqualTo(ReceptionScanResultDTO.FmdStatus.PRESENT);
            assertThat(result.serialNumber()).isEqualTo("SN1");
            assertThat(result.warningMessage()).isNull();
        }

        @Test
        @DisplayName("un numero de serie deja vu declenche une alerte de contrefacon")
        void fmdDuplicate() {
            stubScan(DataMatrixInfo.builder().cip13("3400930000000").serialNumber("SN1").scannedQty(0).build(), BarcodeType.DATAMATRIX);
            ligneDeCommande();
            when(lotRepository.existsBySerialNumberAndProduitId("SN1", PRODUIT_ID)).thenReturn(true);

            ReceptionScanResultDTO result = service.processScanReception(12, "…");

            assertThat(result.fmdStatus()).isEqualTo(ReceptionScanResultDTO.FmdStatus.DUPLICATE);
            assertThat(result.warningMessage()).contains("déjà enregistré");
        }

        @Test
        @DisplayName("cree le lot quand le DataMatrix est complet et la gestion de lot active")
        void creeLeLot() {
            stubScan(
                DataMatrixInfo
                    .builder()
                    .cip13("3400930000000")
                    .batchNumber("LOT1")
                    .expiryDate(LocalDate.of(2027, 6, 30))
                    .manufacturingDate(LocalDate.of(2026, 1, 15))
                    .serialNumber("SN1")
                    .scannedQty(2)
                    .build(),
                BarcodeType.DATAMATRIX
            );
            ligneDeCommande();
            when(appConfigurationService.useLot()).thenReturn(Optional.of(true));
            when(lotRepository.existsBySerialNumberAndProduitId("SN1", PRODUIT_ID)).thenReturn(false);

            ReceptionScanResultDTO result = service.processScanReception(12, "…");

            assertThat(result.lotAutoCreated()).isTrue();
            assertThat(result.lotNumero()).isEqualTo("LOT1");
            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).save(captor.capture());
            Lot lot = captor.getValue();
            assertThat(lot.getNumLot()).isEqualTo("LOT1");
            assertThat(lot.getExpiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
            assertThat(lot.getManufacturingDate()).isEqualTo(LocalDate.of(2026, 1, 15));
            assertThat(lot.getQuantity()).isEqualTo(2);
            assertThat(lot.getCurrentQuantity()).isEqualTo(2);
            assertThat(lot.getFreeQty()).isZero();
            assertThat(lot.getStatut()).isEqualTo(StatutLot.IN_PROGRESS);
            assertThat(lot.getSerialNumber()).isEqualTo("SN1");
            assertThat(lot.getPrixAchat()).isEqualTo(400);
        }

        @Test
        @DisplayName("ne recopie pas un numero de serie deja enregistre sur le lot")
        void lotSansSerieDupliquee() {
            stubScan(
                DataMatrixInfo
                    .builder()
                    .cip13("3400930000000")
                    .batchNumber("LOT1")
                    .expiryDate(LocalDate.of(2027, 6, 30))
                    .serialNumber("SN1")
                    .scannedQty(0)
                    .build(),
                BarcodeType.DATAMATRIX
            );
            ligneDeCommande();
            when(appConfigurationService.useLot()).thenReturn(Optional.of(true));
            when(lotRepository.existsBySerialNumberAndProduitId("SN1", PRODUIT_ID)).thenReturn(true);

            service.processScanReception(12, "…");

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).save(captor.capture());
            assertThat(captor.getValue().getSerialNumber()).isNull();
        }

        @Test
        @DisplayName("gestion de lot inactive : le lot est seulement pre-rempli pour l ecran")
        void lotPreRempli() {
            stubScan(
                DataMatrixInfo
                    .builder()
                    .cip13("3400930000000")
                    .batchNumber("LOT1")
                    .expiryDate(LocalDate.of(2027, 6, 30))
                    .scannedQty(0)
                    .build(),
                BarcodeType.DATAMATRIX
            );
            ligneDeCommande();

            ReceptionScanResultDTO result = service.processScanReception(12, "…");

            assertThat(result.lotAutoCreated()).isFalse();
            assertThat(result.lotNumero()).isEqualTo("LOT1");
            assertThat(result.lot()).isNotNull();
            assertThat(result.lot().getNumLot()).isEqualTo("LOT1");
            assertThat(result.lot().getExpiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
            verify(lotRepository, never()).save(any(Lot.class));
        }

        @Test
        @DisplayName("aucun lot quand la date de peremption manque")
        void sansDatePeremption() {
            stubScan(
                DataMatrixInfo.builder().cip13("3400930000000").batchNumber("LOT1").scannedQty(0).build(),
                BarcodeType.DATAMATRIX
            );
            ligneDeCommande();

            ReceptionScanResultDTO result = service.processScanReception(12, "…");

            assertThat(result.lot()).isNull();
            assertThat(result.lotNumero()).isNull();
        }
    }

    @Nested
    @DisplayName("getPutawayPreview")
    class GetPutawayPreview {

        private Produit produitAvecStocks(int id, Integer maxiRayon, int qtyRayon, int qtyReserve, ClasseCriticite classe) {
            Produit p = produit();
            p.setId(id);
            p.setClasseCriticite(classe);
            p.setFournisseurProduitPrincipal(fournisseurProduit(p));
            StockProduit rayonSp = new StockProduit();
            rayonSp.setStorage(principal);
            rayonSp.setQtyStock(qtyRayon);
            rayonSp.setQtyUG(0);
            rayonSp.setStockMaxi(maxiRayon);
            StockProduit reserveSp = new StockProduit();
            reserveSp.setStorage(reserve);
            reserveSp.setQtyStock(qtyReserve);
            reserveSp.setQtyUG(0);
            p.getStockProduits().add(rayonSp);
            p.getStockProduits().add(reserveSp);
            return p;
        }

        private void stubCommandeAvec(Produit... produits) {
            Commande commande = commande(OrderStatut.CLOSED);
            for (Produit p : produits) {
                orderLine(commande, p, 10, 10);
            }
            when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(commande);
        }

        @Test
        @DisplayName("remonte le debordement du rayon vers la reserve")
        void debordement() {
            stubCommandeAvec(produitAvecStocks(PRODUIT_ID, 20, 30, 5, ClasseCriticite.A));

            List<PutawayPreviewItemDTO> preview = service.getPutawayPreview(12, ORDER_DATE);

            assertThat(preview).hasSize(1);
            PutawayPreviewItemDTO item = preview.getFirst();
            assertThat(item.produitId()).isEqualTo(PRODUIT_ID);
            assertThat(item.produitLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(item.codeCip()).isEqualTo("1234567");
            assertThat(item.qtyRayon()).isEqualTo(30);
            assertThat(item.stockMaxiRayon()).isEqualTo(20);
            assertThat(item.qtyOverflow()).isEqualTo(10);
            assertThat(item.qtyReserveActuelle()).isEqualTo(5);
            assertThat(item.classePareto()).isEqualTo("A");
        }

        @Test
        @DisplayName("ignore un produit sans debordement")
        void sansDebordement() {
            stubCommandeAvec(produitAvecStocks(PRODUIT_ID, 50, 30, 5, ClasseCriticite.A));

            assertThat(service.getPutawayPreview(12, ORDER_DATE)).isEmpty();
        }

        @Test
        @DisplayName("ignore un produit sans stock maximum de rayon")
        void sansStockMaxi() {
            stubCommandeAvec(produitAvecStocks(PRODUIT_ID, null, 30, 5, ClasseCriticite.A));

            assertThat(service.getPutawayPreview(12, ORDER_DATE)).isEmpty();
        }

        @Test
        @DisplayName("ignore un stock maximum de rayon non positif")
        void stockMaxiNonPositif() {
            stubCommandeAvec(produitAvecStocks(PRODUIT_ID, 0, 30, 5, ClasseCriticite.A));

            assertThat(service.getPutawayPreview(12, ORDER_DATE)).isEmpty();
        }

        @Test
        @DisplayName("ignore un produit sans emplacement de rayon ou de reserve")
        void emplacementManquant() {
            Produit p = produit();
            p.setFournisseurProduitPrincipal(fournisseurProduit(p));
            StockProduit rayonSp = new StockProduit();
            rayonSp.setStorage(principal);
            rayonSp.setQtyStock(30);
            rayonSp.setQtyUG(0);
            rayonSp.setStockMaxi(20);
            p.getStockProduits().add(rayonSp);
            stubCommandeAvec(p);

            assertThat(service.getPutawayPreview(12, ORDER_DATE)).isEmpty();
        }

        @Test
        @DisplayName("un produit sans classe de criticite est classe B")
        void classeParDefaut() {
            stubCommandeAvec(produitAvecStocks(PRODUIT_ID, 20, 30, 5, null));

            assertThat(service.getPutawayPreview(12, ORDER_DATE).getFirst().classePareto()).isEqualTo("B");
        }

        @Test
        @DisplayName("tolere un produit sans fournisseur principal")
        void sansFournisseurPrincipal() {
            Produit p = produitAvecStocks(PRODUIT_ID, 20, 30, 5, ClasseCriticite.A);
            p.setFournisseurProduitPrincipal(null);
            stubCommandeAvec(p);

            assertThat(service.getPutawayPreview(12, ORDER_DATE).getFirst().codeCip()).isNull();
        }

        @Test
        @DisplayName("trie les produits par classe Pareto")
        void triParClassePareto() {
            stubCommandeAvec(
                produitAvecStocks(501, 20, 30, 5, ClasseCriticite.C),
                produitAvecStocks(502, 20, 30, 5, ClasseCriticite.A_PLUS),
                produitAvecStocks(503, 20, 30, 5, ClasseCriticite.B),
                produitAvecStocks(504, 20, 30, 5, ClasseCriticite.D),
                produitAvecStocks(505, 20, 30, 5, ClasseCriticite.A)
            );

            assertThat(service.getPutawayPreview(12, ORDER_DATE))
                .extracting(PutawayPreviewItemDTO::classePareto)
                .containsExactly("A+", "A", "B", "C", "D");
        }
    }

    @Nested
    @DisplayName("importNewBon")
    class ImportNewBon {

        private UploadDeleiveryReceiptDTO upload(CommandeModel model) {
            return new UploadDeleiveryReceiptDTO().setModel(model).setFournisseurId(3).setDeliveryReceipt(liteDTO());
        }

        private void stubFichier(String nom, String contenu) throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn(nom);
            when(multipartFile.getInputStream())
                .thenAnswer(inv -> new ByteArrayInputStream(contenu.getBytes(StandardCharsets.UTF_8)));
        }

        @BeforeEach
        void stubCreation() {
            when(commandeIdGeneratorService.getNextIdAsInt()).thenReturn(12);
            when(referenceService.buildNumCommande()).thenReturn("CMD-0001");
            Fournisseur f = new Fournisseur();
            f.setId(3);
            when(fournisseurRepository.getReferenceById(3)).thenReturn(f);
            when(orderLineService.buildDeliveryReceiptItemFromRecord(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(Commande.class)
            )).thenAnswer(inv -> {
                OrderLine line = new OrderLine();
                line.setId(900);
                line.setOrderDate(ORDER_DATE);
                line.setFournisseurProduit(inv.getArgument(0));
                line.setQuantityRequested(inv.getArgument(1));
                line.setQuantityReceived(inv.getArgument(2));
                line.setOrderCostAmount(inv.getArgument(3));
                line.setOrderUnitPrice(inv.getArgument(4));
                line.setFreeQty(inv.getArgument(5));
                line.setTaxAmount(inv.getArgument(7));
                line.setCommande(inv.getArgument(8));
                line.setLots(new ArrayList<>());
                return line;
            });
        }

        @Test
        @DisplayName("cree l en-tete du bon importe et deduit le HT du montant de taxe")
        void enteteDuBon() throws IOException {
            stubFichier("bl.csv", "CIP;QUANTITE\n");

            service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository, atLeastOnce()).save(captor.capture());
            Commande saved = captor.getAllValues().getFirst();
            assertThat(saved.getId().getId()).isEqualTo(12);
            assertThat(saved.getOrderDate()).isEqualTo(LocalDate.now());
            assertThat(saved.getOrderReference()).isEqualTo("CMD-0001");
            assertThat(saved.getUser()).isSameAs(currentUser);
            assertThat(saved.getGrossAmount()).isEqualTo(1000);
            assertThat(saved.getTaxAmount()).isEqualTo(100);
            assertThat(saved.getHtAmount()).isEqualTo(900);
            assertThat(saved.getDiscountAmount()).isZero();
        }

        @Test
        @DisplayName("refuse une extension de fichier non prise en charge")
        void extensionNonSupportee() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("bl.xlsx");
            UploadDeleiveryReceiptDTO upload = upload(CommandeModel.CIP_QTE);

            assertThatThrownBy(() -> service.importNewBon(upload, multipartFile))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("n'est pas pris en charche");
        }

        @ParameterizedTest(name = "le format CSV {0} est pris en charge")
        @EnumSource(CommandeModel.class)
        void tousLesFormatsCsv(CommandeModel model) throws IOException {
            stubFichier("bl.csv", "");

            CommandeResponseDTO response = service.importNewBon(upload(model), multipartFile);

            assertThat(response.getTotalItemCount()).isZero();
            assertThat(response.getReference()).isEqualTo("BL-0012");
        }

        @Test
        @DisplayName("CSV : cree une ligne pour chaque produit reconnu")
        void csvProduitReconnu() throws IOException {
            stubFichier("bl.csv", "CIP;QUANTITE\n1234567;8\n");
            Produit produit = produit();
            FournisseurProduit fp = fournisseurProduit(produit);
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3)).thenReturn(Optional.of(fp));
            when(orderLineService.produitTotalStockWithQantitUg(produit)).thenReturn(20);

            CommandeResponseDTO response = service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            assertThat(response.getSuccesCount()).isEqualTo(1);
            assertThat(response.getFailureCount()).isZero();
            assertThat(response.getTotalItemCount()).isEqualTo(1);
            verify(orderLineService).save(any(OrderLine.class));
        }

        @Test
        @DisplayName("CSV : retombe sur les prix du fournisseur quand le fichier ne les porte pas")
        void csvPrixDuFournisseur() throws IOException {
            stubFichier("bl.csv", "CIP;QUANTITE\n1234567;8\n");
            Produit produit = produit();
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3)).thenReturn(Optional.of(fournisseurProduit(produit)));

            service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            verify(orderLineService).buildDeliveryReceiptItemFromRecord(
                any(),
                anyInt(),
                anyInt(),
                org.mockito.ArgumentMatchers.eq(400),
                org.mockito.ArgumentMatchers.eq(800),
                anyInt(),
                anyInt(),
                anyInt(),
                any(Commande.class)
            );
        }

        @Test
        @DisplayName("CSV : cumule les lignes portant le meme produit")
        void csvCumuleLeMemeProduit() throws IOException {
            stubFichier("bl.csv", "CIP;QUANTITE\n1234567;8\n1234567;4\n");
            Produit produit = produit();
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3)).thenReturn(Optional.of(fournisseurProduit(produit)));

            CommandeResponseDTO response = service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            assertThat(response.getSuccesCount()).isEqualTo(2);
            // une seule ligne creee, la seconde vient s y cumuler
            verify(orderLineService, times(1)).save(any(OrderLine.class));
        }

        @Test
        @DisplayName("CSV : cree le lot porte par le fichier")
        void csvAvecLot() throws IOException {
            stubFichier("bl.csv", "1;1234567;400;8;x;800;LOT-A;20270630\n");
            Produit produit = produit();
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3)).thenReturn(Optional.of(fournisseurProduit(produit)));

            service.importNewBon(upload(CommandeModel.TEDIS), multipartFile);

            ArgumentCaptor<OrderLine> captor = ArgumentCaptor.forClass(OrderLine.class);
            verify(orderLineService).save(captor.capture());
            assertThat(captor.getValue().getLots()).hasSize(1);
            Lot lot = captor.getValue().getLots().getFirst();
            assertThat(lot.getNumLot()).isEqualTo("LOT-A");
            assertThat(lot.getExpiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
            assertThat(lot.getQuantity()).isEqualTo(8);
            assertThat(lot.getStatut()).isEqualTo(StatutLot.IN_PROGRESS);
        }

        @Test
        @DisplayName("CSV : cumule aussi les lots du meme produit")
        void csvCumuleLesLots() throws IOException {
            stubFichier("bl.csv", "1;1234567;400;8;x;800;LOT-A;20270630\n2;1234567;400;4;x;800;LOT-B;20280630\n");
            Produit produit = produit();
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3)).thenReturn(Optional.of(fournisseurProduit(produit)));

            service.importNewBon(upload(CommandeModel.TEDIS), multipartFile);

            ArgumentCaptor<OrderLine> captor = ArgumentCaptor.forClass(OrderLine.class);
            verify(orderLineService).save(captor.capture());
            assertThat(captor.getValue().getLots()).hasSize(2);
            assertThat(captor.getValue().getQuantityReceived()).isEqualTo(12);
        }

        @Test
        @DisplayName("CSV : les produits inconnus sont mis de cote pour rejeu")
        void csvProduitInconnu() throws IOException {
            stubFichier("bl.csv", "CIP;QUANTITE\n9999999;8\n");
            when(orderLineService.getFournisseurProduitByCriteria("9999999", 3)).thenReturn(Optional.empty());

            CommandeResponseDTO response = service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            assertThat(response.getFailureCount()).isEqualTo(1);
            assertThat(response.getSuccesCount()).isZero();
            assertThat(response.getItems().getFirst().getProduitCip()).isEqualTo("9999999");
            verify(importationEchoueService).save(org.mockito.ArgumentMatchers.eq(12), org.mockito.ArgumentMatchers.eq(false), any());
        }

        @Test
        @DisplayName("CSV : aucune ligne rejetee, aucune importation echouee enregistree")
        void csvSansRejet() throws IOException {
            stubFichier("bl.csv", "CIP;QUANTITE\n1234567;8\n");
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3))
                .thenReturn(Optional.of(fournisseurProduit(produit())));

            service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            verifyNoInteractions(importationEchoueService);
        }

        @Test
        @DisplayName("TXT : cree une ligne pour chaque produit reconnu")
        void txtProduitReconnu() throws IOException {
            stubFichier("bl.txt", "1234567\tDOLIPRANE\t400\t8\tx\t800\n");
            Produit produit = produit();
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3)).thenReturn(Optional.of(fournisseurProduit(produit)));
            when(orderLineService.produitTotalStockWithQantitUg(produit)).thenReturn(20);

            CommandeResponseDTO response = service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            assertThat(response.getSuccesCount()).isEqualTo(1);
            assertThat(response.getTotalItemCount()).isEqualTo(1);
            verify(orderLineService).save(any(OrderLine.class));
        }

        @Test
        @DisplayName("TXT : cumule les lignes portant le meme produit")
        void txtCumuleLeMemeProduit() throws IOException {
            stubFichier("bl.txt", "1234567\tD\t400\t8\tx\t800\n1234567\tD\t400\t4\tx\t800\n");
            when(orderLineService.getFournisseurProduitByCriteria("1234567", 3))
                .thenReturn(Optional.of(fournisseurProduit(produit())));

            CommandeResponseDTO response = service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            assertThat(response.getSuccesCount()).isEqualTo(2);
            verify(orderLineService, times(1)).save(any(OrderLine.class));
        }

        @Test
        @DisplayName("TXT : les produits inconnus sont mis de cote pour rejeu")
        void txtProduitInconnu() throws IOException {
            stubFichier("bl.txt", "9999999\tINCONNU\t400\t8\tx\t800\n");
            when(orderLineService.getFournisseurProduitByCriteria("9999999", 3)).thenReturn(Optional.empty());

            CommandeResponseDTO response = service.importNewBon(upload(CommandeModel.CIP_QTE), multipartFile);

            assertThat(response.getFailureCount()).isEqualTo(1);
            assertThat(response.getItems().getFirst().getProduitEan()).isEqualTo("9999999");
            assertThat(response.getItems().getFirst().getMontant()).isEqualTo(800d);
        }

        @Test
        @DisplayName("TXT : une erreur de lecture remonte a l appelant")
        void txtErreurDeLecture() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("bl.txt");
            InputStream flux = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("flux interrompu");
                }
            };
            when(multipartFile.getInputStream()).thenReturn(flux);
            UploadDeleiveryReceiptDTO upload = upload(CommandeModel.CIP_QTE);

            assertThatThrownBy(() -> service.importNewBon(upload, multipartFile)).isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("CSV : une erreur de lecture remonte enveloppee par commons-csv")
        void csvErreurDeLecture() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("bl.csv");
            InputStream flux = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("flux interrompu");
                }
            };
            when(multipartFile.getInputStream()).thenReturn(flux);
            UploadDeleiveryReceiptDTO upload = upload(CommandeModel.CIP_QTE);

            // commons-csv enveloppe les erreurs de flux : le catch(IOException) du service ne les voit pas
            assertThatThrownBy(() -> service.importNewBon(upload, multipartFile))
                .isInstanceOf(java.io.UncheckedIOException.class)
                .hasRootCauseInstanceOf(IOException.class);
        }
    }
}
