package com.kobe.warehouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.AjustRepository;
import com.kobe.warehouse.repository.AjustementRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AjustementService")
class AjustementServiceTest {

    private static final int PRODUIT_ID = 500;
    private static final int AJUST_ID = 12;
    private static final int STORAGE_ID = 2;

    @Mock
    private AjustementRepository ajustementRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private AjustRepository ajustRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private LogsService logsService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private SuggestionReassortService suggestionReassortService;

    @Mock
    private LotStockLocationService lotStockLocationService;

    @Mock
    private LotService lotService;

    private AjustementService service;

    private AppUser currentUser;
    private Storage principal;

    @BeforeEach
    void setUp() {
        service = new AjustementService(
            ajustementRepository,
            produitRepository,
            ajustRepository,
            storageService,
            stockProduitRepository,
            logsService,
            inventoryTransactionService,
            suggestionReassortService,
            lotStockLocationService,
            lotService
        );
        currentUser = new AppUser();
        currentUser.setId(9);
        currentUser.setFirstName("Awa");
        currentUser.setLastName("KOUAME");
        principal = storage(STORAGE_ID, StorageType.PRINCIPAL);

        when(storageService.getUser()).thenReturn(currentUser);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(principal);
        when(ajustRepository.save(any(Ajust.class))).thenAnswer(inv -> {
            Ajust a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(AJUST_ID);
            }
            return a;
        });
        when(ajustementRepository.save(any(Ajustement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Storage storage(int id, StorageType type) {
        Storage s = new Storage();
        s.setId(id);
        s.setStorageType(type);
        return s;
    }

    private static Produit produit(String libelle, String codeCip) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(77);
        fp.setCodeCip(codeCip);
        Produit produit = new Produit();
        produit.setId(PRODUIT_ID);
        produit.setLibelle(libelle);
        produit.setFournisseurProduitPrincipal(fp);
        produit.setStockProduits(new HashSet<>());
        return produit;
    }

    /** StockProduit s'identifie par son id : deux stocks du meme produit doivent differer. */
    private static int stockSeq = 60;

    private static StockProduit stock(Produit produit, Storage storage, int qtyStock, int qtyUg) {
        StockProduit sp = new StockProduit();
        sp.setId(stockSeq++);
        sp.setProduit(produit);
        sp.setStorage(storage);
        sp.setQtyStock(qtyStock);
        sp.setQtyUG(qtyUg);
        sp.setQtyVirtual(qtyStock);
        produit.getStockProduits().add(sp);
        return sp;
    }

    private Ajust ajust(AjustementStatut statut) {
        Ajust ajust = new Ajust();
        ajust.setId(AJUST_ID);
        ajust.setStatut(statut);
        ajust.setDateMtv(LocalDateTime.now());
        ajust.setUser(currentUser);
        ajust.setAjustements(new ArrayList<>());
        return ajust;
    }

    private Ajustement ajustement(Ajust ajust, StockProduit stockProduit, int qtyMvt) {
        Ajustement ajustement = new Ajustement();
        ajustement.setId(70);
        ajustement.setAjust(ajust);
        ajustement.setStockProduit(stockProduit);
        ajustement.setQtyMvt(qtyMvt);
        ajustement.setStockBefore(0);
        ajustement.setStockAfter(0);
        ajustement.setDateMtv(LocalDateTime.now());
        return ajustement;
    }

    private static AjustementDTO dto(int qtyMvt) {
        AjustementDTO dto = new AjustementDTO();
        dto.setProduitId(PRODUIT_ID);
        dto.setAjustId(AJUST_ID);
        dto.setQtyMvt(qtyMvt);
        return dto;
    }

    @Nested
    @DisplayName("createAjsut")
    class CreateAjsut {

        private AjustDTO commande(AjustementDTO... ajustements) {
            AjustDTO dto = new AjustDTO();
            dto.setCommentaire("casse en rayon");
            dto.setAjustements(new ArrayList<>(List.of(ajustements)));
            return dto;
        }

        @Test
        @DisplayName("cree l en-tete d ajustement horodate et signe par l utilisateur")
        void enteteDAjustement() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 0);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit);
            LocalDateTime avant = LocalDateTime.now();

            AjustDTO resultat = service.createAjsut(commande(dto(-4)));

            ArgumentCaptor<Ajust> captor = ArgumentCaptor.forClass(Ajust.class);
            verify(ajustRepository).save(captor.capture());
            assertThat(captor.getValue().getCommentaire()).isEqualTo("casse en rayon");
            assertThat(captor.getValue().getUser()).isSameAs(currentUser);
            assertThat(captor.getValue().getDateMtv()).isAfterOrEqualTo(avant);
            assertThat(resultat.getId()).isEqualTo(AJUST_ID);
            assertThat(resultat.getCommentaire()).isEqualTo("casse en rayon");
            assertThat(resultat.getUserId()).isEqualTo(9);
        }

        @Test
        @DisplayName("ne cree que la premiere ligne transmise")
        void seulementLaPremiereLigne() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 0);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit);

            service.createAjsut(commande(dto(-4), dto(-2)));

            verify(ajustementRepository, times(1)).save(any(Ajustement.class));
        }
    }

    @Nested
    @DisplayName("creation d une ligne d ajustement")
    class CreationDeLigne {

        private Ajustement creerViaCreateOrUpdate(AjustementDTO dto, Produit produit) {
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit);
            when(ajustRepository.getReferenceById(AJUST_ID)).thenReturn(ajust(AjustementStatut.PENDING));
            when(ajustementRepository.findFirstByAjustIdAndStockProduitProduitId(AJUST_ID, PRODUIT_ID)).thenReturn(Optional.empty());

            service.createOrUpdate(dto);

            ArgumentCaptor<Ajustement> captor = ArgumentCaptor.forClass(Ajustement.class);
            verify(ajustementRepository).save(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("prend l emplacement transmis quand il est precise")
        void emplacementTransmis() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            Storage reserve = storage(3, StorageType.SAFETY_STOCK);
            stock(produit, principal, 20, 0);
            StockProduit stockReserve = stock(produit, reserve, 5, 0);

            Ajustement ajustement = creerViaCreateOrUpdate(dto(-2).setStorageId(3), produit);

            assertThat(ajustement.getStockProduit()).isSameAs(stockReserve);
            verify(storageService, never()).getDefaultConnectedUserMainStorage();
        }

        @Test
        @DisplayName("retombe sur l emplacement de vente quand aucun n est precise")
        void emplacementParDefaut() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockPrincipal = stock(produit, principal, 20, 0);

            Ajustement ajustement = creerViaCreateOrUpdate(dto(-2), produit);

            assertThat(ajustement.getStockProduit()).isSameAs(stockPrincipal);
        }

        @Test
        @DisplayName("refuse un produit sans stock dans l emplacement demande")
        void sansStockDansLEmplacement() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, storage(9, StorageType.SAFETY_STOCK), 5, 0);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit);
            when(ajustRepository.getReferenceById(AJUST_ID)).thenReturn(ajust(AjustementStatut.PENDING));
            when(ajustementRepository.findFirstByAjustIdAndStockProduitProduitId(AJUST_ID, PRODUIT_ID)).thenReturn(Optional.empty());
            AjustementDTO dto = dto(-2);

            assertThatThrownBy(() -> service.createOrUpdate(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("n'a pas de stock dans le magasin selectionné");
        }

        @Test
        @DisplayName("calcule le stock avant et apres a partir du stock total")
        void stocksAvantEtApres() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 3);

            Ajustement ajustement = creerViaCreateOrUpdate(dto(-4), produit);

            assertThat(ajustement.getStockBefore()).isEqualTo(23);
            assertThat(ajustement.getStockAfter()).isEqualTo(19);
            assertThat(ajustement.getDateMtv()).isNotNull();
        }

        @ParameterizedTest(name = "un mouvement de {0} est de type {1}")
        @CsvSource({ "5, AJUSTEMENT_IN", "0, AJUSTEMENT_IN", "-1, AJUSTEMENT_OUT", "-5, AJUSTEMENT_OUT" })
        void typeSelonLeSens(int qtyMvt, AjustType attendu) {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 0);

            assertThat(creerViaCreateOrUpdate(dto(qtyMvt), produit).getType()).isEqualTo(attendu);
        }

        @Test
        @DisplayName("rattache le motif transmis")
        void avecMotif() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 0);
            AjustementDTO dto = dto(-2);
            dto.setMotifAjustementId(4);

            Ajustement ajustement = creerViaCreateOrUpdate(dto, produit);

            assertThat(ajustement.getMotifAjustement()).isNotNull();
            assertThat(ajustement.getMotifAjustement().getId()).isEqualTo(4);
        }

        @Test
        @DisplayName("laisse le motif vide quand il n est pas transmis")
        void sansMotif() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 0);

            assertThat(creerViaCreateOrUpdate(dto(-2), produit).getMotifAjustement()).isNull();
        }

        @Test
        @DisplayName("rattache le lot transmis")
        void avecLot() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 0);
            AjustementDTO dto = dto(5);
            dto.setLotId(70);

            Ajustement ajustement = creerViaCreateOrUpdate(dto, produit);

            assertThat(ajustement.getLot()).isNotNull();
            assertThat(ajustement.getLot().getId()).isEqualTo(70);
        }

        @Test
        @DisplayName("laisse le lot vide quand il n est pas transmis")
        void sansLot() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            stock(produit, principal, 20, 0);

            assertThat(creerViaCreateOrUpdate(dto(5), produit).getLot()).isNull();
        }
    }

    @Nested
    @DisplayName("createOrUpdate")
    class CreateOrUpdate {

        @Test
        @DisplayName("cumule la quantite sur une ligne deja saisie pour ce produit")
        void cumuleSurUneLigneExistante() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 20, 3);
            Ajustement existante = ajustement(ajust(AjustementStatut.PENDING), stockProduit, -4);
            when(ajustementRepository.findFirstByAjustIdAndStockProduitProduitId(AJUST_ID, PRODUIT_ID)).thenReturn(Optional.of(existante));
            when(ajustRepository.getReferenceById(AJUST_ID)).thenReturn(ajust(AjustementStatut.PENDING));

            service.createOrUpdate(dto(-2));

            assertThat(existante.getQtyMvt()).isEqualTo(-6);
            assertThat(existante.getStockBefore()).isEqualTo(23);
            assertThat(existante.getStockAfter()).isEqualTo(17);
            verify(ajustementRepository).save(existante);
            verifyNoInteractions(produitRepository);
        }
    }

    @Nested
    @DisplayName("saveAjust")
    class SaveAjust {

        private Ajust ajustAvec(Ajustement... ajustements) {
            Ajust ajust = ajust(AjustementStatut.PENDING);
            when(ajustRepository.getReferenceById(AJUST_ID)).thenReturn(ajust);
            when(ajustementRepository.findAllByAjustId(AJUST_ID)).thenReturn(new ArrayList<>(List.of(ajustements)));
            return ajust;
        }

        private static AjustDTO commande() {
            AjustDTO dto = new AjustDTO();
            dto.setId(AJUST_ID);
            dto.setCommentaire("validation");
            return dto;
        }

        @Test
        @DisplayName("cloture l ajustement et reprend son commentaire")
        void clotureLAjustement() {
            Ajust ajust = ajustAvec();

            service.saveAjust(commande());

            assertThat(ajust.getStatut()).isEqualTo(AjustementStatut.CLOSED);
            assertThat(ajust.getCommentaire()).isEqualTo("validation");
        }

        @Test
        @DisplayName("applique un ajustement negatif au stock et debite les lots en FEFO")
        void ajustementNegatif() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 20, 3);
            Ajust ajust = ajustAvec(ajustement(ajust(AjustementStatut.PENDING), stockProduit, -5));

            service.saveAjust(commande());

            assertThat(stockProduit.getQtyStock()).isEqualTo(15);
            assertThat(stockProduit.getQtyVirtual()).isEqualTo(15);
            assertThat(stockProduit.getQtyUG()).isZero();
            verify(lotService).adjustLots(produit, -5);
            verify(lotStockLocationService).debitFefo(produit, principal, 5);
            verify(lotService, never()).creditSpecificLot(any(), anyInt());
        }

        @Test
        @DisplayName("le debit de lot ne depasse jamais le stock initial")
        void debitPlafonneAuStockInitial() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 5, 0);
            ajustAvec(ajustement(ajust(AjustementStatut.PENDING), stockProduit, -20));

            service.saveAjust(commande());

            verify(lotService).adjustLots(produit, -5);
            verify(lotStockLocationService).debitFefo(produit, principal, 5);
        }

        @Test
        @DisplayName("un stock initial nul ne declenche aucun debit de lot")
        void stockInitialNul() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 0, 0);
            ajustAvec(ajustement(ajust(AjustementStatut.PENDING), stockProduit, -5));

            service.saveAjust(commande());

            verifyNoInteractions(lotService);
            verifyNoInteractions(lotStockLocationService);
        }

        @Test
        @DisplayName("un ajustement positif sur un lot choisi credite ce lot")
        void ajustementPositifSurLotChoisi() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 20, 0);
            Ajustement ajustement = ajustement(ajust(AjustementStatut.PENDING), stockProduit, 5);
            Lot lot = new Lot().setId(70);
            ajustement.setLot(lot);
            ajustAvec(ajustement);

            service.saveAjust(commande());

            assertThat(stockProduit.getQtyStock()).isEqualTo(25);
            verify(lotService).creditSpecificLot(lot, 5);
            verify(lotStockLocationService).credit(lot, principal, 5);
            verify(lotService, never()).adjustLots(any(), anyInt());
        }

        @Test
        @DisplayName("un ajustement positif sans lot choisi credite le dernier lot recu")
        void ajustementPositifSansLot() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 20, 0);
            ajustAvec(ajustement(ajust(AjustementStatut.PENDING), stockProduit, 5));

            service.saveAjust(commande());

            verify(lotService).adjustLots(produit, 5);
            verify(lotStockLocationService).creditLastLot(produit, principal, 5);
            verify(lotService, never()).creditSpecificLot(any(), anyInt());
        }

        @ParameterizedTest(name = "un mouvement de {0} journalise une transaction {1}")
        @CsvSource({ "5, AJUSTEMENT_IN", "0, AJUSTEMENT_IN", "-5, AJUSTEMENT_OUT" })
        void typeDeTransaction(int qtyMvt, TransactionType attendu) {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 20, 0);
            Ajustement ajustement = ajustement(ajust(AjustementStatut.PENDING), stockProduit, qtyMvt);
            ajustAvec(ajustement);

            service.saveAjust(commande());

            assertThat(ajustement.getType()).isEqualTo(qtyMvt >= 0 ? AjustType.AJUSTEMENT_IN : AjustType.AJUSTEMENT_OUT);
            verify(logsService).create(org.mockito.ArgumentMatchers.eq(attendu), anyString(), org.mockito.ArgumentMatchers.eq("70"));
        }

        @Test
        @DisplayName("journalise le detail du mouvement et propose un reassort")
        void journaliseEtPropose() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 20, 3);
            Ajustement ajustement = ajustement(ajust(AjustementStatut.PENDING), stockProduit, -5);
            ajustAvec(ajustement);

            service.saveAjust(commande());

            assertThat(ajustement.getStockBefore()).isEqualTo(23);
            assertThat(ajustement.getStockAfter()).isEqualTo(15);
            ArgumentCaptor<String> desc = ArgumentCaptor.forClass(String.class);
            verify(logsService).create(any(), desc.capture(), anyString());
            assertThat(desc.getValue()).contains("1234567").contains("DOLIPRANE 1000MG").contains("23").contains("-5").contains("15");
            verify(inventoryTransactionService).save(ajustement);
            verify(suggestionReassortService).createRayonSuggestionReassort(stockProduit);
            verify(suggestionReassortService).createReserveSuggestionReassort(stockProduit);
        }

        @Test
        @DisplayName("traite chaque ligne de l ajustement")
        void traiteChaqueLigne() {
            Produit p1 = produit("DOLIPRANE 1000MG", "1234567");
            Produit p2 = produit("EFFERALGAN", "7654321");
            ajustAvec(
                ajustement(ajust(AjustementStatut.PENDING), stock(p1, principal, 20, 0), -5),
                ajustement(ajust(AjustementStatut.PENDING), stock(p2, principal, 10, 0), 3)
            );

            service.saveAjust(commande());

            verify(ajustementRepository, times(2)).save(any(Ajustement.class));
            verify(stockProduitRepository, times(2)).save(any(StockProduit.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("remplace la quantite et recalcule les stocks a partir du stock physique")
        void remplaceLaQuantite() {
            Produit produit = produit("DOLIPRANE 1000MG", "1234567");
            StockProduit stockProduit = stock(produit, principal, 20, 3);
            Ajust ajust = ajust(AjustementStatut.PENDING);
            Ajustement ajustement = ajustement(ajust, stockProduit, -4);
            ajustement.setType(AjustType.AJUSTEMENT_OUT);
            when(ajustementRepository.getReferenceById(70)).thenReturn(ajustement);
            AjustementDTO dto = dto(-6);
            dto.setId(70);
            LocalDateTime avant = LocalDateTime.now();

            AjustementDTO resultat = service.update(dto);

            assertThat(ajustement.getQtyMvt()).isEqualTo(-6);
            // le recalcul se fait sur qtyStock, sans les UG
            assertThat(ajustement.getStockBefore()).isEqualTo(20);
            assertThat(ajustement.getStockAfter()).isEqualTo(14);
            assertThat(ajustement.getDateMtv()).isAfterOrEqualTo(avant);
            assertThat(resultat.getQtyMvt()).isEqualTo(-6);
            assertThat(resultat.getProduitId()).isEqualTo(PRODUIT_ID);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        private Ajustement ligne(String libelle, String codeCip, LocalDateTime dateMtv) {
            Produit produit = produit(libelle, codeCip);
            Ajust ajust = ajust(AjustementStatut.PENDING);
            Ajustement ajustement = ajustement(ajust, stock(produit, principal, 20, 0), -4);
            ajustement.setDateMtv(dateMtv);
            return ajustement;
        }

        @ParameterizedTest(name = "une recherche [{0}] renvoie tout, trie du plus recent au plus ancien")
        @NullAndEmptySource
        void sansRecherche(String search) {
            Ajustement ancienne = ligne("DOLIPRANE 1000MG", "1234567", LocalDateTime.of(2026, 4, 1, 10, 0));
            Ajustement recente = ligne("EFFERALGAN", "7654321", LocalDateTime.of(2026, 4, 18, 10, 0));
            when(ajustementRepository.findAllByAjustId(AJUST_ID)).thenReturn(new ArrayList<>(List.of(ancienne, recente)));

            List<AjustementDTO> resultat = service.findAll(AJUST_ID, search);

            assertThat(resultat).extracting(AjustementDTO::getProduitLibelle).containsExactly("EFFERALGAN", "DOLIPRANE 1000MG");
        }

        @Test
        @DisplayName("filtre sur le libelle du produit, sans tenir compte de la casse")
        void filtreSurLeLibelle() {
            Ajustement l1 = ligne("DOLIPRANE 1000MG", "1234567", LocalDateTime.now());
            Ajustement l2 = ligne("EFFERALGAN", "7654321", LocalDateTime.now().minusDays(1));
            when(ajustementRepository.findAllByAjustId(AJUST_ID)).thenReturn(new ArrayList<>(List.of(l1, l2)));

            assertThat(service.findAll(AJUST_ID, "dolipra"))
                .extracting(AjustementDTO::getProduitLibelle)
                .containsExactly("DOLIPRANE 1000MG");
        }

        @Test
        @DisplayName("filtre sur le code CIP du fournisseur principal")
        void filtreSurLeCodeCip() {
            Ajustement l1 = ligne("DOLIPRANE 1000MG", "1234567", LocalDateTime.now());
            Ajustement l2 = ligne("EFFERALGAN", "7654321", LocalDateTime.now().minusDays(1));
            when(ajustementRepository.findAllByAjustId(AJUST_ID)).thenReturn(new ArrayList<>(List.of(l1, l2)));

            assertThat(service.findAll(AJUST_ID, "76543")).extracting(AjustementDTO::getProduitLibelle).containsExactly("EFFERALGAN");
        }

        @Test
        @DisplayName("une recherche sans correspondance renvoie une liste vide")
        void sansCorrespondance() {
            when(ajustementRepository.findAllByAjustId(AJUST_ID))
                .thenReturn(new ArrayList<>(List.of(ligne("DOLIPRANE 1000MG", "1234567", LocalDateTime.now()))));

            assertThat(service.findAll(AJUST_ID, "ASPIRINE")).isEmpty();
        }

        @Test
        @DisplayName("findAll sans argument remonte tous les ajustements")
        void findAllSansArgument() {
            when(ajustementRepository.findAll())
                .thenReturn(List.of(ligne("DOLIPRANE 1000MG", "1234567", LocalDateTime.now())));

            assertThat(service.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("suppressions")
    class Suppressions {

        @Test
        @DisplayName("deleteItem supprime une ligne d ajustement")
        void deleteItem() {
            service.deleteItem(70);

            verify(ajustementRepository).deleteById(70);
        }

        @Test
        @DisplayName("deleteAll supprime chaque ligne du lot")
        void deleteAll() {
            service.deleteAll(List.of(70, 71));

            verify(ajustementRepository).deleteById(70);
            verify(ajustementRepository).deleteById(71);
        }

        @Test
        @DisplayName("deleteAll tolere une liste vide")
        void deleteAllVide() {
            service.deleteAll(List.of());

            verify(ajustementRepository, never()).deleteById(anyInt());
        }

        @Test
        @DisplayName("delete supprime un ajustement encore en brouillon")
        void deleteEnBrouillon() {
            when(ajustRepository.getReferenceById(AJUST_ID)).thenReturn(ajust(AjustementStatut.PENDING));

            service.delete(AJUST_ID);

            verify(ajustRepository).deleteById(AJUST_ID);
        }

        @ParameterizedTest(name = "delete refuse un ajustement {0}")
        @EnumSource(value = AjustementStatut.class, names = { "PENDING" }, mode = EnumSource.Mode.EXCLUDE)
        void deleteHorsBrouillon(AjustementStatut statut) {
            when(ajustRepository.getReferenceById(AJUST_ID)).thenReturn(ajust(statut));

            service.delete(AJUST_ID);

            verify(ajustRepository, never()).deleteById(anyInt());
        }
    }
}
