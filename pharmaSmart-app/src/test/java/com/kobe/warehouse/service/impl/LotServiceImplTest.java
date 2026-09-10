package com.kobe.warehouse.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.LotServiceReportService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import com.kobe.warehouse.service.stock.dto.TypeFilter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LotServiceImpl")
class LotServiceImplTest {

    private static final int PRODUIT_ID = 500;
    private static final OrderLineId ORDER_LINE_ID = new OrderLineId(900, LocalDate.of(2026, 4, 18));

    @Mock
    private LotRepository lotRepository;

    @Mock
    private LotStockLocationRepository lotStockLocationRepository;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private LotServiceReportService lotServiceReportService;

    @Mock
    private OrderLineService orderLineService;

    @Mock
    private LotStockLocationService lotStockLocationService;

    @Mock
    private StorageService storageService;

    @Mock
    private RetourBonRepository retourBonRepository;

    private LotServiceImpl service;

    private Magasin magasin;
    private Storage principal;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new LotServiceImpl(
            lotRepository,
            lotStockLocationRepository,
            appConfigurationService,
            produitRepository,
            lotServiceReportService,
            orderLineService,
            lotStockLocationService,
            storageService,
            retourBonRepository
        );
        magasin = new Magasin();
        magasin.setId(1);
        principal = storage(2, "RAYON");

        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(principal);
        when(appConfigurationService.getNombreJourAlertPeremption()).thenReturn(90);
        when(lotRepository.saveAndFlush(any(Lot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lotRepository.save(any(Lot.class))).thenAnswer(inv -> inv.getArgument(0));
        Specification<Lot> spec = (r, q, cb) -> null;
        when(lotRepository.buildCombinedSpecification(any())).thenReturn(spec);
        when(lotRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    }

    private Storage storage(int id, String name) {
        Storage s = new Storage();
        s.setId(id);
        s.setName(name);
        s.setMagasin(magasin);
        return s;
    }

    private static Produit produit() {
        Produit produit = new Produit();
        produit.setId(PRODUIT_ID);
        produit.setLibelle("DOLIPRANE 1000MG");
        produit.setCostAmount(400);
        produit.setRegularUnitPrice(800);
        produit.setStockProduits(new HashSet<>());
        produit.setRayonProduits(new HashSet<>());
        return produit;
    }

    private static FournisseurProduit fournisseurProduit(Produit produit) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(3);
        fournisseur.setLibelle("LABOREX");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(77);
        fp.setCodeCip("1234567");
        fp.setPrixAchat(450);
        fp.setPrixUni(900);
        fp.setFournisseur(fournisseur);
        fp.setProduit(produit);
        return fp;
    }

    private static Lot lot(int id, String numLot, int quantity, int currentQuantity) {
        return new Lot()
            .setId(id)
            .setNumLot(numLot)
            .setQuantity(quantity)
            .setCurrentQuantity(currentQuantity)
            .setFreeQty(0)
            .setStatut(StatutLot.AVAILABLE);
    }

    private static LotDTO dto(String numLot, Integer quantityReceived, Integer freeQty) {
        return new LotDTO()
            .setNumLot(numLot)
            .setQuantityReceived(quantityReceived)
            .setFreeQty(freeQty)
            .setExpiryDate(LocalDate.of(2027, 6, 30))
            .setReceiptItemId(ORDER_LINE_ID);
    }

    @Nested
    @DisplayName("addLot")
    class AddLot {

        private OrderLine orderLine(Integer quantityReceived, int freeQty, Lot... lots) {
            Produit produit = produit();
            OrderLine line = new OrderLine();
            line.setId(900);
            line.setOrderDate(LocalDate.of(2026, 4, 18));
            line.setFournisseurProduit(fournisseurProduit(produit));
            line.setQuantityReceived(quantityReceived);
            line.setFreeQty(freeQty);
            line.setOrderCostAmount(400);
            line.setOrderUnitPrice(800);
            line.setLots(new ArrayList<>(List.of(lots)));
            when(orderLineService.findOneById(ORDER_LINE_ID)).thenReturn(Optional.of(line));
            return line;
        }

        @Test
        @DisplayName("refuse une ligne de commande introuvable")
        void ligneIntrouvable() {
            when(orderLineService.findOneById(ORDER_LINE_ID)).thenReturn(Optional.empty());
            LotDTO lot = dto("L1", 10, 0);

            assertThatThrownBy(() -> service.addLot(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ligne de commande introuvable");
        }

        @Test
        @DisplayName("cree le lot et reprend les prix de la ligne de commande")
        void creeLeLot() {
            orderLine(10, 0);

            LotDTO resultat = service.addLot(dto("L1", 10, 0));

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).saveAndFlush(captor.capture());
            Lot lot = captor.getValue();
            assertThat(lot.getNumLot()).isEqualTo("L1");
            assertThat(lot.getQuantity()).isEqualTo(10);
            assertThat(lot.getCurrentQuantity()).isEqualTo(10);
            assertThat(lot.getPrixAchat()).isEqualTo(400);
            assertThat(lot.getPrixUnit()).isEqualTo(800);
            assertThat(lot.getStatut()).isEqualTo(StatutLot.IN_PROGRESS);
            assertThat(lot.getProduit().getId()).isEqualTo(PRODUIT_ID);
            assertThat(lot.getCreatedDate()).isNotNull();
            assertThat(resultat.getNumLot()).isEqualTo("L1");
        }

        @Test
        @DisplayName("compte les unites gratuites dans la quantite du lot")
        void avecUnitesGratuites() {
            orderLine(10, 2);

            service.addLot(dto("L1", 10, 2));

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(12);
            assertThat(captor.getValue().getFreeQty()).isEqualTo(2);
        }

        @Test
        @DisplayName("refuse un lot qui depasserait la quantite recue")
        void depasseLaQuantiteRecue() {
            orderLine(10, 0);
            LotDTO lot = dto("L1", 12, 0);

            assertThatThrownBy(() -> service.addLot(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("dépasserait la quantité reçue (10)");
        }

        @Test
        @DisplayName("tient compte des lots deja saisis dans le controle de quantite")
        void cumuleAvecLesLotsExistants() {
            orderLine(10, 0, lot(70, "L0", 8, 8));
            LotDTO lot = dto("L1", 5, 0);

            assertThatThrownBy(() -> service.addLot(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("(13)")
                .hasMessageContaining("(10)");
        }

        @Test
        @DisplayName("un lot deja saisi sans quantite compte pour zero")
        void lotExistantSansQuantite() {
            Lot sansQuantite = lot(70, "L0", 8, 8).setQuantity(null);
            orderLine(10, 0, sansQuantite);

            service.addLot(dto("L1", 10, 0));

            verify(lotRepository).saveAndFlush(any(Lot.class));
        }

        @Test
        @DisplayName("les unites gratuites de la ligne elargissent le plafond")
        void plafondAvecUnitesGratuites() {
            orderLine(10, 5);

            service.addLot(dto("L1", 10, 5));

            verify(lotRepository).saveAndFlush(any(Lot.class));
        }

        @Test
        @DisplayName("aucun controle de quantite quand la ligne n a rien recu")
        void sansQuantiteRecue() {
            orderLine(null, 0);

            service.addLot(dto("L1", 999, 0));

            verify(lotRepository).saveAndFlush(any(Lot.class));
        }

        @Test
        @DisplayName("aucun controle de quantite quand la ligne a recu zero")
        void quantiteRecueNulle() {
            orderLine(0, 0);

            service.addLot(dto("L1", 999, 0));

            verify(lotRepository).saveAndFlush(any(Lot.class));
        }

        @Test
        @DisplayName("cumule sur un lot deja saisi portant le meme numero")
        void cumuleSurLeMemeNumero() {
            Lot existant = lot(70, "L1", 4, 4);
            orderLine(20, 0, existant);

            LotDTO resultat = service.addLot(dto("L1", 6, 2));

            assertThat(existant.getQuantity()).isEqualTo(12);
            assertThat(existant.getCurrentQuantity()).isEqualTo(12);
            assertThat(existant.getFreeQty()).isEqualTo(2);
            verify(lotRepository).saveAndFlush(existant);
            assertThat(resultat.getNumLot()).isEqualTo("L1");
        }

        @Test
        @DisplayName("un numero de lot different cree un nouveau lot")
        void numeroDifferent() {
            Lot existant = lot(70, "L0", 4, 4);
            orderLine(20, 0, existant);

            service.addLot(dto("L1", 6, 0));

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue()).isNotSameAs(existant);
        }

        @Test
        @DisplayName("un lot sans numero cree toujours un nouveau lot")
        void sansNumero() {
            orderLine(20, 0, lot(70, "L0", 4, 4));

            service.addLot(dto(null, 6, 0));

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getNumLot()).isNull();
        }

        @Test
        @DisplayName("addLotBatch traite chaque lot de la liste")
        void batch() {
            orderLine(50, 0);

            assertThat(service.addLotBatch(List.of(dto("L1", 10, 0), dto("L2", 5, 0)))).hasSize(2);

            verify(lotRepository, times(2)).saveAndFlush(any(Lot.class));
        }
    }

    @Nested
    @DisplayName("addLotSurProduit")
    class AddLotSurProduit {

        private Produit produitAvecStock(int qtyStock, boolean avecFournisseurPrincipal) {
            Produit produit = produit();
            StockProduit sp = new StockProduit();
            sp.setId(60);
            sp.setStorage(principal);
            sp.setQtyStock(qtyStock);
            sp.setQtyUG(0);
            produit.getStockProduits().add(sp);
            if (avecFournisseurPrincipal) {
                produit.setFournisseurProduitPrincipal(fournisseurProduit(produit));
            }
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit));
            return produit;
        }

        private static LotDTO horsCommande(Integer produitId, String numLot, LocalDate expiry, Integer qty) {
            return new LotDTO().setProduitId(produitId).setNumLot(numLot).setExpiryDate(expiry).setQuantityReceived(qty).setFreeQty(0);
        }

        @Test
        @DisplayName("refuse une saisie sans produit")
        void sansProduit() {
            LotDTO lot = horsCommande(null, "L1", LocalDate.of(2027, 6, 30), 5);

            assertThatThrownBy(() -> service.addLotSurProduit(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Le produitId est obligatoire");
        }

        @ParameterizedTest(name = "refuse un numero de lot [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        void sansNumeroDeLot(String numLot) {
            LotDTO lot = horsCommande(PRODUIT_ID, numLot, LocalDate.of(2027, 6, 30), 5);

            assertThatThrownBy(() -> service.addLotSurProduit(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Le numéro de lot est obligatoire");
        }

        @Test
        @DisplayName("refuse une saisie sans date de peremption")
        void sansDatePeremption() {
            LotDTO lot = horsCommande(PRODUIT_ID, "L1", null, 5);

            assertThatThrownBy(() -> service.addLotSurProduit(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("La date de péremption est obligatoire");
        }

        @Test
        @DisplayName("refuse un produit inconnu")
        void produitInconnu() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());
            LotDTO lot = horsCommande(PRODUIT_ID, "L1", LocalDate.of(2027, 6, 30), 5);

            assertThatThrownBy(() -> service.addLotSurProduit(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Produit introuvable");
        }

        @Test
        @DisplayName("refuse une quantite nulle ou negative")
        void quantiteNonPositive() {
            produitAvecStock(20, true);
            LotDTO lot = horsCommande(PRODUIT_ID, "L1", LocalDate.of(2027, 6, 30), 0);

            assertThatThrownBy(() -> service.addLotSurProduit(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("La quantité doit être supérieure à 0");
        }

        @Test
        @DisplayName("refuse une quantite superieure au stock de l emplacement")
        void quantiteSuperieureAuStock() {
            produitAvecStock(3, true);
            LotDTO lot = horsCommande(PRODUIT_ID, "L1", LocalDate.of(2027, 6, 30), 5);

            assertThatThrownBy(() -> service.addLotSurProduit(lot))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("ne peut pas dépasser le stock disponible sur cet emplacement (3)");
        }

        @Test
        @DisplayName("cree un lot deja disponible et le localise sur l emplacement")
        void creeUnLotDisponible() {
            produitAvecStock(20, true);

            LotDTO resultat = service.addLotSurProduit(horsCommande(PRODUIT_ID, "L1", LocalDate.of(2027, 6, 30), 5));

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).saveAndFlush(captor.capture());
            Lot lot = captor.getValue();
            assertThat(lot.getStatut()).isEqualTo(StatutLot.AVAILABLE);
            assertThat(lot.getQuantity()).isEqualTo(5);
            assertThat(lot.getCurrentQuantity()).isEqualTo(5);
            assertThat(lot.getOrderLine()).isNull();
            assertThat(lot.getProduit().getId()).isEqualTo(PRODUIT_ID);
            verify(lotStockLocationService).credit(lot, principal, 5);
            assertThat(resultat.getNumLot()).isEqualTo("L1");
        }

        @Test
        @DisplayName("reprend les prix du fournisseur principal quand il existe")
        void prixDuFournisseurPrincipal() {
            produitAvecStock(20, true);

            service.addLotSurProduit(horsCommande(PRODUIT_ID, "L1", LocalDate.of(2027, 6, 30), 5));

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(450);
            assertThat(captor.getValue().getPrixUnit()).isEqualTo(900);
        }

        @Test
        @DisplayName("retombe sur les prix du produit sans fournisseur principal")
        void prixDuProduit() {
            produitAvecStock(20, false);

            service.addLotSurProduit(horsCommande(PRODUIT_ID, "L1", LocalDate.of(2027, 6, 30), 5));

            ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
            verify(lotRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(400);
            assertThat(captor.getValue().getPrixUnit()).isEqualTo(800);
        }

        @Test
        @DisplayName("utilise l emplacement transmis quand il est precise")
        void emplacementTransmis() {
            Storage reserve = storage(3, "RESERVE");
            Produit produit = produit();
            StockProduit sp = new StockProduit();
            sp.setId(61);
            sp.setStorage(reserve);
            sp.setQtyStock(20);
            sp.setQtyUG(0);
            produit.getStockProduits().add(sp);
            produit.setFournisseurProduitPrincipal(fournisseurProduit(produit));
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit));
            when(storageService.getOne(3)).thenReturn(reserve);

            service.addLotSurProduit(horsCommande(PRODUIT_ID, "L1", LocalDate.of(2027, 6, 30), 5).setStorageId(3));

            verify(lotStockLocationService).credit(any(Lot.class), org.mockito.ArgumentMatchers.eq(reserve), anyInt());
            verify(storageService, never()).getDefaultConnectedUserMainStorage();
        }
    }

    @Nested
    @DisplayName("editLot")
    class EditLot {

        @Test
        @DisplayName("met a jour le lot et recalcule sa quantite")
        void metAJour() {
            Lot entity = lot(70, "L0", 4, 4);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);
            LotDTO lot = new LotDTO()
                .setId(70)
                .setNumLot("L1")
                .setQuantityReceived(8)
                .setUgQuantityReceived(2)
                .setExpiryDate(LocalDate.of(2027, 6, 30))
                .setManufacturingDate(LocalDate.of(2026, 1, 15));

            LotDTO resultat = service.editLot(lot);

            assertThat(entity.getNumLot()).isEqualTo("L1");
            assertThat(entity.getQuantity()).isEqualTo(10);
            assertThat(entity.getFreeQty()).isEqualTo(2);
            assertThat(entity.getExpiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
            assertThat(entity.getManufacturingDate()).isEqualTo(LocalDate.of(2026, 1, 15));
            assertThat(resultat.getNumLot()).isEqualTo("L1");
        }

        @Test
        @DisplayName("retombe sur les unites gratuites quand la quantite UG n est pas transmise")
        void ugParDefaut() {
            Lot entity = lot(70, "L0", 4, 4);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.editLot(new LotDTO().setId(70).setNumLot("L1").setQuantityReceived(8).setFreeQty(3));

            assertThat(entity.getFreeQty()).isEqualTo(3);
            assertThat(entity.getQuantity()).isEqualTo(11);
        }

        @Test
        @DisplayName("aucune unite gratuite transmise compte pour zero")
        void sansUg() {
            Lot entity = lot(70, "L0", 4, 4);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.editLot(new LotDTO().setId(70).setNumLot("L1").setQuantityReceived(8));

            assertThat(entity.getFreeQty()).isZero();
            assertThat(entity.getQuantity()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("lectures et suppressions")
    class LecturesEtSuppressions {

        @Test
        @DisplayName("remove par DTO supprime le lot")
        void removeParDto() {
            service.remove(new LotDTO().setId(70));

            verify(lotRepository).deleteById(70);
        }

        @Test
        @DisplayName("remove par identifiant supprime le lot")
        void removeParId() {
            service.remove(70);

            verify(lotRepository).deleteById(70);
        }

        @Test
        @DisplayName("findByProduitId et findProduitLots deleguent au depot")
        void lecturesParProduit() {
            List<Lot> lots = List.of(lot(70, "L1", 10, 10));
            when(lotRepository.findByProduitId(PRODUIT_ID)).thenReturn(lots);

            assertThat(service.findByProduitId(PRODUIT_ID)).isSameAs(lots);
            assertThat(service.findProduitLots(PRODUIT_ID)).isSameAs(lots);
        }

        @Test
        @DisplayName("findByProduitIdAndNumLot delegue au depot")
        void lectureParNumero() {
            when(lotRepository.findByNumLotAndProduitId("L1", PRODUIT_ID)).thenReturn(Optional.empty());

            assertThat(service.findByProduitIdAndNumLot(PRODUIT_ID, "L1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateLots")
    class UpdateLots {

        @ParameterizedTest(name = "une liste vide ou nulle ne touche a aucun lot")
        @NullAndEmptySource
        void listeVide(List<LotSold> lots) {
            service.updateLots(lots);

            verifyNoInteractions(lotRepository);
        }

        @Test
        @DisplayName("decremente la quantite courante du lot vendu")
        void decremente() {
            Lot entity = lot(70, "L1", 10, 10);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.updateLots(List.of(new LotSold(70, "L1", 4, LocalDate.of(2027, 6, 30))));

            assertThat(entity.getCurrentQuantity()).isEqualTo(6);
            assertThat(entity.getStatut()).isEqualTo(StatutLot.AVAILABLE);
            verify(lotRepository).save(entity);
        }

        @Test
        @DisplayName("un lot entierement vendu passe au statut vendu")
        void lotEpuise() {
            Lot entity = lot(70, "L1", 10, 10);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.updateLots(List.of(new LotSold(70, "L1", 10, LocalDate.of(2027, 6, 30))));

            assertThat(entity.getCurrentQuantity()).isZero();
            assertThat(entity.getStatut()).isEqualTo(StatutLot.SOLD);
        }

        @Test
        @DisplayName("la quantite courante ne descend jamais sous zero")
        void plancherAZero() {
            Lot entity = lot(70, "L1", 10, 4);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.updateLots(List.of(new LotSold(70, "L1", 10, LocalDate.of(2027, 6, 30))));

            assertThat(entity.getCurrentQuantity()).isZero();
            assertThat(entity.getStatut()).isEqualTo(StatutLot.SOLD);
        }
    }

    @Nested
    @DisplayName("restoreLots")
    class RestoreLots {

        @ParameterizedTest(name = "une liste vide ou nulle ne touche a aucun lot")
        @NullAndEmptySource
        void listeVide(List<LotSold> lots) {
            service.restoreLots(lots);

            verifyNoInteractions(lotRepository);
        }

        @Test
        @DisplayName("recredite le lot et le remet disponible")
        void recredite() {
            Lot entity = lot(70, "L1", 10, 0).setStatut(StatutLot.SOLD);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.restoreLots(List.of(new LotSold(70, "L1", 4, LocalDate.of(2027, 6, 30))));

            assertThat(entity.getCurrentQuantity()).isEqualTo(4);
            assertThat(entity.getStatut()).isEqualTo(StatutLot.AVAILABLE);
            verify(lotRepository).save(entity);
        }

        @Test
        @DisplayName("un retour a zero laisse le statut inchange")
        void retourAZero() {
            Lot entity = lot(70, "L1", 10, 0).setStatut(StatutLot.SOLD);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.restoreLots(List.of(new LotSold(70, "L1", 0, LocalDate.of(2027, 6, 30))));

            assertThat(entity.getStatut()).isEqualTo(StatutLot.SOLD);
        }
    }

    @Nested
    @DisplayName("adjustLots")
    class AdjustLots {

        @Test
        @DisplayName("un delta nul ne touche a aucun lot")
        void deltaNul() {
            service.adjustLots(produit(), 0);

            verifyNoInteractions(lotRepository);
        }

        @Test
        @DisplayName("un delta negatif debite les lots en FEFO")
        void deltaNegatif() {
            Lot l1 = lot(70, "L1", 10, 3);
            Lot l2 = lot(71, "L2", 10, 10);
            when(lotRepository.findByProduitId(PRODUIT_ID)).thenReturn(List.of(l1, l2));

            service.adjustLots(produit(), -8);

            assertThat(l1.getCurrentQuantity()).isZero();
            assertThat(l1.getStatut()).isEqualTo(StatutLot.SOLD);
            assertThat(l2.getCurrentQuantity()).isEqualTo(5);
            assertThat(l2.getStatut()).isEqualTo(StatutLot.AVAILABLE);
            verify(lotRepository).save(l1);
            verify(lotRepository).save(l2);
        }

        @Test
        @DisplayName("s arrete des que le debit demande est servi")
        void sArreteQuandServi() {
            Lot l1 = lot(70, "L1", 10, 10);
            Lot l2 = lot(71, "L2", 10, 10);
            when(lotRepository.findByProduitId(PRODUIT_ID)).thenReturn(List.of(l1, l2));

            service.adjustLots(produit(), -4);

            assertThat(l1.getCurrentQuantity()).isEqualTo(6);
            assertThat(l2.getCurrentQuantity()).isEqualTo(10);
            verify(lotRepository, never()).save(l2);
        }

        @Test
        @DisplayName("saute les lots deja epuises")
        void sauteLesLotsEpuises() {
            Lot epuise = lot(70, "L1", 10, 0);
            Lot disponible = lot(71, "L2", 10, 10);
            when(lotRepository.findByProduitId(PRODUIT_ID)).thenReturn(List.of(epuise, disponible));

            service.adjustLots(produit(), -4);

            assertThat(disponible.getCurrentQuantity()).isEqualTo(6);
            verify(lotRepository, never()).save(epuise);
        }

        @Test
        @DisplayName("un stock de lots insuffisant vide ce qui est disponible")
        void stockInsuffisant() {
            Lot l1 = lot(70, "L1", 10, 3);
            when(lotRepository.findByProduitId(PRODUIT_ID)).thenReturn(List.of(l1));

            service.adjustLots(produit(), -50);

            assertThat(l1.getCurrentQuantity()).isZero();
        }

        @Test
        @DisplayName("un delta positif credite le dernier lot recu")
        void deltaPositif() {
            Lot dernier = lot(70, "L1", 10, 2);
            when(lotRepository.findLastReceivedByProduitId(PRODUIT_ID)).thenReturn(Optional.of(dernier));

            service.adjustLots(produit(), 5);

            assertThat(dernier.getCurrentQuantity()).isEqualTo(7);
            verify(lotRepository).save(dernier);
        }

        @Test
        @DisplayName("un lot vendu recredite repasse disponible")
        void relanceUnLotVendu() {
            Lot dernier = lot(70, "L1", 10, 0).setStatut(StatutLot.SOLD);
            when(lotRepository.findLastReceivedByProduitId(PRODUIT_ID)).thenReturn(Optional.of(dernier));

            service.adjustLots(produit(), 5);

            assertThat(dernier.getStatut()).isEqualTo(StatutLot.AVAILABLE);
        }

        @Test
        @DisplayName("aucun lot recu : rien a crediter")
        void aucunLotRecu() {
            when(lotRepository.findLastReceivedByProduitId(PRODUIT_ID)).thenReturn(Optional.empty());

            service.adjustLots(produit(), 5);

            verify(lotRepository, never()).save(any(Lot.class));
        }
    }

    @Nested
    @DisplayName("creditSpecificLot")
    class CreditSpecificLot {

        @ParameterizedTest(name = "une quantite de {0} ne touche a aucun lot")
        @ValueSource(ints = { 0, -5 })
        void quantiteNonPositive(int qty) {
            service.creditSpecificLot(lot(70, "L1", 10, 10), qty);

            verifyNoInteractions(lotRepository);
        }

        @Test
        @DisplayName("credite le lot designe")
        void credite() {
            Lot entity = lot(70, "L1", 10, 2);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.creditSpecificLot(lot(70, "L1", 10, 2), 5);

            assertThat(entity.getCurrentQuantity()).isEqualTo(7);
            assertThat(entity.getStatut()).isEqualTo(StatutLot.AVAILABLE);
            verify(lotRepository).save(entity);
        }

        @Test
        @DisplayName("un lot vendu recredite repasse disponible")
        void relanceUnLotVendu() {
            Lot entity = lot(70, "L1", 10, 0).setStatut(StatutLot.SOLD);
            when(lotRepository.getReferenceById(70)).thenReturn(entity);

            service.creditSpecificLot(lot(70, "L1", 10, 0), 5);

            assertThat(entity.getStatut()).isEqualTo(StatutLot.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("findLotsPerimes")
    class FindLotsPerimes {

        private Lot lotAvecCommande(String numLot, LocalDate expiry) {
            Produit produit = produit();
            FournisseurProduit fp = fournisseurProduit(produit);
            FamilleProduit famille = new FamilleProduit();
            famille.setLibelle("ANTALGIQUES");
            produit.setFamille(famille);
            Rayon rayon = new Rayon();
            rayon.setId(4);
            rayon.setLibelle("RAYON A");
            produit.getRayonProduits().add(new RayonProduit().setRayon(rayon).setProduit(produit));
            OrderLine line = new OrderLine();
            line.setId(900);
            line.setOrderDate(LocalDate.of(2026, 4, 18));
            line.setFournisseurProduit(fp);
            return lot(70, numLot, 10, 10).setExpiryDate(expiry).setOrderLine(line).setProduit(produit);
        }

        @SuppressWarnings("unchecked")
        private void stubPage(Lot... lots) {
            when(lotRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(lots)));
        }

        @Test
        @DisplayName("mappe un lot issu d une commande avec produit, fournisseur et rayon")
        void lotAvecCommandeMappe() {
            stubPage(lotAvecCommande("L1", LocalDate.of(2026, 5, 1)));

            LotPerimeDTO dto = service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst();

            assertThat(dto.getId()).isEqualTo(70);
            assertThat(dto.getNumLot()).isEqualTo("L1");
            assertThat(dto.getQuantity()).isEqualTo(10);
            assertThat(dto.getProduitId()).isEqualTo(PRODUIT_ID);
            assertThat(dto.getProduitName()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getProduitCode()).isEqualTo("1234567");
            assertThat(dto.getFournisseur()).isEqualTo("LABOREX");
            assertThat(dto.getFamilleProduitName()).isEqualTo("ANTALGIQUES");
            assertThat(dto.getRayonName()).isEqualTo("RAYON A");
            assertThat(dto.getPrixAchat()).isEqualTo(450);
            assertThat(dto.getPrixVente()).isEqualTo(900);
            assertThat(dto.getDatePeremption()).isEqualTo("01/05/2026");
            assertThat(dto.getPeremptionStatut()).isNotNull();
        }

        @Test
        @DisplayName("un produit sans rayon laisse le rayon vide")
        void sansRayon() {
            Lot lot = lotAvecCommande("L1", LocalDate.of(2026, 5, 1));
            lot.getProduit().getRayonProduits().clear();
            stubPage(lot);

            assertThat(service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst().getRayonName()).isNull();
        }

        @Test
        @DisplayName("un lot hors commande est mappe depuis son produit")
        void lotHorsCommande() {
            Produit produit = produit();
            FamilleProduit famille = new FamilleProduit();
            famille.setLibelle("ANTALGIQUES");
            produit.setFamille(famille);
            produit.setFournisseurProduitPrincipal(fournisseurProduit(produit));
            stubPage(lot(70, "L1", 10, 10).setExpiryDate(LocalDate.of(2026, 5, 1)).setProduit(produit));

            LotPerimeDTO dto = service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst();

            assertThat(dto.getProduitName()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getProduitCode()).isEqualTo("1234567");
        }

        @Test
        @DisplayName("un lot hors commande sans fournisseur principal ne porte que le produit")
        void horsCommandeSansFournisseurPrincipal() {
            Produit produit = produit();
            FamilleProduit famille = new FamilleProduit();
            famille.setLibelle("ANTALGIQUES");
            produit.setFamille(famille);
            stubPage(lot(70, "L1", 10, 10).setExpiryDate(LocalDate.of(2026, 5, 1)).setProduit(produit));

            LotPerimeDTO dto = service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst();

            assertThat(dto.getProduitName()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getFamilleProduitName()).isEqualTo("ANTALGIQUES");
            assertThat(dto.getProduitCode()).isNull();
        }

        @Test
        @DisplayName("un lot hors commande sans famille laisse la famille vide")
        void horsCommandeSansFamille() {
            stubPage(lot(70, "L1", 10, 10).setExpiryDate(LocalDate.of(2026, 5, 1)).setProduit(produit()));

            assertThat(service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst().getFamilleProduitName())
                .isNull();
        }

        @Test
        @DisplayName("un lot orphelin produit un DTO minimal")
        void lotOrphelin() {
            stubPage(lot(70, "L1", 10, 10).setExpiryDate(LocalDate.of(2026, 5, 1)));

            LotPerimeDTO dto = service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst();

            assertThat(dto.getId()).isEqualTo(70);
            assertThat(dto.getNumLot()).isEqualTo("L1");
            assertThat(dto.getDatePeremption()).isEqualTo("01/05/2026");
            assertThat(dto.getProduitId()).isNull();
            assertThat(dto.getProduitName()).isNull();
        }

        @Test
        @DisplayName("un lot orphelin sans date de peremption reste sans date")
        void lotOrphelinSansDate() {
            stubPage(lot(70, "L1", 10, 10));

            assertThat(service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst().getDatePeremption())
                .isNull();
        }

        @Test
        @DisplayName("un lot sans date de peremption reste sans date")
        void lotSansDate() {
            Lot lot = lotAvecCommande("L1", null);
            stubPage(lot);

            LotPerimeDTO dto = service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst();

            assertThat(dto.getDatePeremption()).isNull();
            assertThat(dto.getProduitName()).isEqualTo("DOLIPRANE 1000MG");
        }

        @Test
        @DisplayName("filtre par emplacement : la quantite reprend celle de l emplacement")
        void filtreParEmplacement() {
            stubPage(lotAvecCommande("L1", LocalDate.of(2026, 5, 1)));
            LotStockLocation lsl = new LotStockLocation(lot(70, "L1", 10, 10), principal, 4);
            when(lotStockLocationRepository.findByLotIdAndStorageId(70, 2)).thenReturn(Optional.of(lsl));
            LotFilterParam param = new LotFilterParam();
            param.setStorageId(2);

            LotPerimeDTO dto = service.findLotsPerimes(param, Pageable.unpaged()).getContent().getFirst();

            assertThat(dto.getQuantity()).isEqualTo(4);
            assertThat(dto.getLocations()).hasSize(1);
            assertThat(dto.getLocations().getFirst().qty()).isEqualTo(4);
        }

        @Test
        @DisplayName("un lot absent de l emplacement filtre garde sa quantite")
        void absentDeLEmplacement() {
            stubPage(lotAvecCommande("L1", LocalDate.of(2026, 5, 1)));
            when(lotStockLocationRepository.findByLotIdAndStorageId(70, 2)).thenReturn(Optional.empty());
            LotFilterParam param = new LotFilterParam();
            param.setStorageId(2);

            assertThat(service.findLotsPerimes(param, Pageable.unpaged()).getContent().getFirst().getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("sans filtre d emplacement, tous les emplacements disponibles sont listes")
        void tousLesEmplacements() {
            stubPage(lotAvecCommande("L1", LocalDate.of(2026, 5, 1)));
            when(lotStockLocationRepository.findAvailableByLotId(70))
                .thenReturn(List.of(new LotStockLocation(lot(70, "L1", 10, 10), principal, 6)));

            LotPerimeDTO dto = service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged()).getContent().getFirst();

            assertThat(dto.getLocations()).hasSize(1);
            assertThat(dto.getLocations().getFirst().storageName()).isEqualTo("RAYON");
        }

        @Test
        @DisplayName("trie par date de peremption decroissante, page par page")
        @SuppressWarnings("unchecked")
        void triEtPagination() {
            service.findLotsPerimes(new LotFilterParam(), PageRequest.of(2, 15));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(lotRepository).findAll(any(Specification.class), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(captor.getValue().getPageSize()).isEqualTo(15);
            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "expiryDate"));
        }

        @Test
        @DisplayName("une demande non paginee reste non paginee mais triee")
        @SuppressWarnings("unchecked")
        void sansPagination() {
            service.findLotsPerimes(new LotFilterParam(), Pageable.unpaged());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(lotRepository).findAll(any(Specification.class), captor.capture());
            assertThat(captor.getValue().isPaged()).isFalse();
            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "expiryDate"));
        }
    }

    @Nested
    @DisplayName("normalisation du filtre")
    class NormalisationDuFiltre {

        @Test
        @DisplayName("sans magasin ni emplacement, retombe sur le magasin de l utilisateur")
        void magasinParDefaut() {
            LotFilterParam param = new LotFilterParam();

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getMagasinId()).isEqualTo(1);
        }

        @Test
        @DisplayName("un emplacement transmis dispense de resoudre le magasin")
        void emplacementTransmis() {
            LotFilterParam param = new LotFilterParam();
            param.setStorageId(2);

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getMagasinId()).isNull();
        }

        @Test
        @DisplayName("un magasin transmis est conserve")
        void magasinTransmis() {
            LotFilterParam param = new LotFilterParam();
            param.setMagasinId(9);

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getMagasinId()).isEqualTo(9);
        }

        @Test
        @DisplayName("sans horizon ni periode, retombe sur le seuil d alerte configure")
        void horizonParDefaut() {
            LotFilterParam param = new LotFilterParam();

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getDayCount()).isEqualTo(90);
        }

        @Test
        @DisplayName("une date de debut transmise dispense de l horizon par defaut")
        void avecDateDeDebut() {
            LotFilterParam param = new LotFilterParam();
            param.setFromDate(LocalDate.of(2026, 4, 1));

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getDayCount()).isNull();
        }

        @Test
        @DisplayName("une date de fin transmise dispense de l horizon par defaut")
        void avecDateDeFin() {
            LotFilterParam param = new LotFilterParam();
            param.setToDate(LocalDate.of(2026, 4, 30));

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getDayCount()).isNull();
        }

        @Test
        @DisplayName("un horizon transmis est conserve")
        void horizonTransmis() {
            LotFilterParam param = new LotFilterParam();
            param.setDayCount(30);

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getDayCount()).isEqualTo(30);
        }

        @Test
        @DisplayName("sans type, retombe sur le filtre ALL")
        void typeParDefaut() {
            LotFilterParam param = new LotFilterParam();

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getType()).isEqualTo(TypeFilter.ALL);
        }

        @Test
        @DisplayName("un type transmis est conserve")
        void typeTransmis() {
            LotFilterParam param = new LotFilterParam();
            param.setType(TypeFilter.PERIME);

            service.findLotsPerimes(param, Pageable.unpaged());

            assertThat(param.getType()).isEqualTo(TypeFilter.PERIME);
        }
    }

    @Nested
    @DisplayName("agregats et export")
    class AgregatsEtExport {

        @Test
        @DisplayName("findPerimeSum complete l agregat du nombre de retours en cours")
        void findPerimeSum() {
            when(lotRepository.fetchPerimeSum(any())).thenReturn(new LotPerimeValeurSum(45000L, 90000L, 12, 3L, 5L, 0L));
            when(retourBonRepository.countByStatutNot(RetourStatut.CLOSED)).thenReturn(7L);

            LotPerimeValeurSum sum = service.findPerimeSum(new LotFilterParam());

            assertThat(sum.valeurAchat()).isEqualTo(45000L);
            assertThat(sum.retoursFourn()).isEqualTo(7L);
        }

        @Test
        @DisplayName("findPerimeSum normalise aussi le filtre")
        void findPerimeSumNormaliseLeFiltre() {
            when(lotRepository.fetchPerimeSum(any())).thenReturn(new LotPerimeValeurSum(0L, 0L, 0, 0L, 0L, 0L));
            LotFilterParam param = new LotFilterParam();

            service.findPerimeSum(param);

            assertThat(param.getMagasinId()).isEqualTo(1);
            assertThat(param.getDayCount()).isEqualTo(90);
            assertThat(param.getType()).isEqualTo(TypeFilter.ALL);
        }

        @Test
        @DisplayName("generatePdf transmet la liste, la synthese et la periode au rapport")
        void generatePdf() {
            when(lotRepository.fetchPerimeSum(any())).thenReturn(new LotPerimeValeurSum(0L, 0L, 0, 0L, 0L, 0L));
            ResponseEntity<byte[]> attendu = ResponseEntity.ok(new byte[] { 7 });
            when(lotServiceReportService.generatePdf(any(), any(), any(), any())).thenReturn(attendu);
            LotFilterParam param = new LotFilterParam();
            param.setFromDate(LocalDate.of(2026, 4, 1));
            param.setToDate(LocalDate.of(2026, 4, 30));

            assertThat(service.generatePdf(param)).isSameAs(attendu);

            verify(lotServiceReportService).generatePdf(
                any(),
                any(),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 4, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 4, 30))
            );
        }
    }
}
