package com.kobe.warehouse.service.reassort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.domain.enumeration.TypeRepartition;
import com.kobe.warehouse.repository.RepartitionStockProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.projection.RepartitionStockProduitProjection;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.dto.RepartionQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import com.kobe.warehouse.service.reassort.impl.RepartitionStockServiceImpl;
import com.kobe.warehouse.service.report.pdf.RepartitionStockPdfReportService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * Répartir du stock, c'est écrire au même instant dans quatre endroits : les deux
 * {@code stock_produit} concernés, la trace {@code repartition_stock_produit}, le journal de
 * mouvement et la localisation des lots. Ce qui se joue ici est l'accord entre ces écritures — le
 * sens du mouvement, les quantités avant/après de chaque côté, et le fait qu'aucune des quatre ne
 * parte sans les trois autres.
 *
 * <p>Les garde-fous comptent autant : une source introuvable, une destination absente, une
 * quantité qui viderait l'emplacement d'origine. Chacun doit refuser ou passer son tour
 * <b>avant</b> d'avoir touché au stock, faute de quoi la base garde la moitié d'un transfert.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RepartitionStockService — transfert de stock entre emplacements")
class RepartitionStockServiceImplTest {

    private static final int RAYON_ID = 1;
    private static final int RESERVE_ID = 3;
    private static final AtomicInteger COMPTEUR_LIGNE = new AtomicInteger();

    @Mock
    private StorageService storageService;

    @Mock
    private RepartitionStockProduitRepository repartitionStockProduitRepository;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private RepartitionStockPdfReportService repartitionStockPdfReportService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private LotStockLocationService lotStockLocationService;

    private RepartitionStockService service;

    private Storage rayon;
    private Storage reserve;
    private AppUser operateur;

    @BeforeEach
    void init() {
        service = new RepartitionStockServiceImpl(
            storageService,
            repartitionStockProduitRepository,
            stockProduitRepository,
            repartitionStockPdfReportService,
            inventoryTransactionService,
            lotStockLocationService
        );
        rayon = storage(RAYON_ID, "Stock rayon", StorageType.PRINCIPAL);
        reserve = storage(RESERVE_ID, "Stock réserve", StorageType.SAFETY_STOCK);
        operateur = new AppUser();
        operateur.setId(1);
    }

    @Nested
    @DisplayName("Validation d'une suggestion")
    class ValidationDUneSuggestion {

        @Test
        @DisplayName("Une suggestion absente, close ou vide ne déclenche aucune écriture")
        void suggestionSansEffet() {
            service.process((SuggestionReassort) null);

            SuggestionReassort close = suggestion(TypeReassort.RAYON, StatutReassort.CLOSED);
            close.getLigneReassorts().add(new LigneReassort());
            service.process(close);

            service.process(suggestion(TypeReassort.RAYON, StatutReassort.OPEN));

            verifyNoInteractions(
                repartitionStockProduitRepository,
                inventoryTransactionService,
                lotStockLocationService,
                stockProduitRepository
            );
        }

        @Test
        @DisplayName("Un réassort rayon puise dans la réserve et journalise le mouvement des deux côtés")
        void reassortRayonPuiseDansLaReserve() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 4);
            StockProduit stockReserve = stock(200, produit, reserve, 30);
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);

            SuggestionReassort suggestion = suggestion(TypeReassort.RAYON, StatutReassort.OPEN);
            suggestion.getLigneReassorts().add(ligne(stockRayon, null, 12));

            service.process(suggestion);

            RepartitionStockProduit trace = capturerLaTrace();
            assertSame(stockReserve, trace.getStockProduitSource(), "le rayon se réassortit depuis la réserve");
            assertSame(stockRayon, trace.getStockProduitDestination());
            assertEquals(12, trace.getQtyMvt());
            assertEquals(30, trace.getSourceInitStock());
            assertEquals(18, trace.getSourceFinalStock());
            assertEquals(4, trace.getDestInitStock());
            assertEquals(16, trace.getDestFinalStock());
            assertEquals(TypeRepartition.MANUEL, trace.getTypeRepartition());
            assertSame(operateur, trace.getUser(), "la trace porte l'auteur de la validation");

            assertEquals(18, stockReserve.getQtyStock());
            assertEquals(16, stockRayon.getQtyStock());
            verify(inventoryTransactionService).saveRepartition(trace);
            verify(lotStockLocationService).transferFefo(produit, reserve, rayon, 12);
        }

        @Test
        @DisplayName("Un réassort réserve puise à l'inverse dans le rayon")
        void reassortReservePuiseDansLeRayon() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 50);
            StockProduit stockReserve = stock(200, produit, reserve, 2);
            when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);

            SuggestionReassort suggestion = suggestion(TypeReassort.RESERVE, StatutReassort.OPEN);
            suggestion.getLigneReassorts().add(ligne(stockReserve, null, 20));

            service.process(suggestion);

            RepartitionStockProduit trace = capturerLaTrace();
            assertSame(stockRayon, trace.getStockProduitSource());
            assertSame(stockReserve, trace.getStockProduitDestination());
            assertEquals(30, stockRayon.getQtyStock());
            assertEquals(22, stockReserve.getQtyStock());
            verify(lotStockLocationService).transferFefo(produit, rayon, reserve, 20);
        }

        @Test
        @DisplayName("La source portée par la ligne prime sur l'emplacement déduit du type")
        void sourceExpliciteDeLaLigne() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 5);
            Storage toxiques = storage(7, "Toxiques", StorageType.TOXIQUE);
            StockProduit stockToxiques = stock(300, produit, toxiques, 40);

            SuggestionReassort suggestion = suggestion(TypeReassort.RAYON, StatutReassort.OPEN);
            suggestion.getLigneReassorts().add(ligne(stockRayon, stockToxiques, 10));

            service.process(suggestion);

            assertSame(stockToxiques, capturerLaTrace().getStockProduitSource());
            assertEquals(30, stockToxiques.getQtyStock());
            verify(lotStockLocationService).transferFefo(produit, toxiques, rayon, 10);
        }

        @Test
        @DisplayName("Sans emplacement source pour le produit, la validation échoue au lieu de créer du stock")
        void sourceIntrouvable() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 5);
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);

            SuggestionReassort suggestion = suggestion(TypeReassort.RAYON, StatutReassort.OPEN);
            suggestion.getLigneReassorts().add(ligne(stockRayon, null, 10));

            GenericError erreur = assertThrows(GenericError.class, () -> service.process(suggestion));
            assertEquals("Stock source inconnu", erreur.getMessage());
            assertEquals(5, stockRayon.getQtyStock(), "rien n'a bougé avant le refus");
            verifyNoInteractions(repartitionStockProduitRepository, lotStockLocationService);
        }
    }

    @Nested
    @DisplayName("Réassort rayon en lot")
    class ReassortRayonEnLot {

        @Test
        @DisplayName("Une liste vide ne consulte même pas l'emplacement de réserve")
        void listeVide() {
            service.processReassortStockRayon(Set.of());

            verifyNoInteractions(storageService, repartitionStockProduitRepository);
        }

        @Test
        @DisplayName("Un produit sans stock en réserve est passé, les autres sont servis")
        void produitSansReserveIgnore() {
            when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            when(storageService.getUser()).thenReturn(operateur);

            Produit sansReserve = produit(10);
            StockProduit rayonSansReserve = stock(100, sansReserve, rayon, 3);

            Produit avecReserve = produit(11);
            StockProduit rayonServi = stock(101, avecReserve, rayon, 3);
            StockProduit reserveServie = stock(201, avecReserve, reserve, 25);

            service.processReassortStockRayon(
                new LinkedHashSet<>(List.of(ligne(rayonSansReserve, null, 5), ligne(rayonServi, null, 5)))
            );

            verify(repartitionStockProduitRepository, times(1)).save(any(RepartitionStockProduit.class));
            assertEquals(3, rayonSansReserve.getQtyStock(), "le produit sans réserve est resté intact");
            assertEquals(8, rayonServi.getQtyStock());
            assertEquals(20, reserveServie.getQtyStock());
            verify(lotStockLocationService).transferFefo(avecReserve, reserve, rayon, 5);
        }
    }

    @Nested
    @DisplayName("Répartition manuelle")
    class RepartitionManuelle {

        @BeforeEach
        void emplacementsParDefaut() {
            lenient().when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
            lenient().when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
            lenient().when(storageService.getUser()).thenReturn(operateur);
            lenient()
                .when(stockProduitRepository.save(any(StockProduit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("Une liste vide ne fait rien")
        void listeVide() {
            service.process(List.<RepartionQueryDto>of());

            verifyNoInteractions(repartitionStockProduitRepository, lotStockLocationService);
        }

        @Test
        @DisplayName("Depuis un emplacement vendable, le stock part vers la réserve")
        void duRayonVersLaReserve() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 40);
            StockProduit stockReserve = stock(200, produit, reserve, 5);
            when(stockProduitRepository.getReferenceById(100)).thenReturn(stockRayon);

            service.process(List.of(new RepartionQueryDto(100, 200, 15, null, false)));

            RepartitionStockProduit trace = capturerLaTrace();
            assertSame(stockRayon, trace.getStockProduitSource());
            assertSame(stockReserve, trace.getStockProduitDestination());
            assertEquals(40, trace.getSourceInitStock());
            assertEquals(25, trace.getSourceFinalStock());
            assertEquals(5, trace.getDestInitStock());
            assertEquals(20, trace.getDestFinalStock());
            assertEquals(25, stockRayon.getQtyStock());
            assertEquals(20, stockReserve.getQtyStock());
            verify(lotStockLocationService).transferFefo(produit, rayon, reserve, 15);
        }

        @Test
        @DisplayName("Depuis la réserve, le stock revient au rayon")
        void deLaReserveVersLeRayon() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 2);
            StockProduit stockReserve = stock(200, produit, reserve, 40);
            when(stockProduitRepository.getReferenceById(200)).thenReturn(stockReserve);

            service.process(List.of(new RepartionQueryDto(200, 100, 10, null, false)));

            RepartitionStockProduit trace = capturerLaTrace();
            assertSame(stockReserve, trace.getStockProduitSource());
            assertSame(stockRayon, trace.getStockProduitDestination());
            assertEquals(12, stockRayon.getQtyStock());
            verify(lotStockLocationService).transferFefo(produit, reserve, rayon, 10);
        }

        @Test
        @DisplayName("Vider entièrement l'emplacement source est refusé sans erreur : la ligne est passée")
        void quantiteEgaleAuStockSource() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 15);
            stock(200, produit, reserve, 0);
            when(stockProduitRepository.getReferenceById(100)).thenReturn(stockRayon);

            service.process(List.of(new RepartionQueryDto(100, 200, 15, null, false)));

            verify(repartitionStockProduitRepository, never()).save(any());
            verifyNoInteractions(lotStockLocationService);
            assertEquals(15, stockRayon.getQtyStock());
        }

        @Test
        @DisplayName("Sans emplacement de réserve et sans demande de création, la répartition est refusée")
        void destinationAbsenteEtNonDemandee() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 40);
            when(stockProduitRepository.getReferenceById(100)).thenReturn(stockRayon);

            List<RepartionQueryDto> demandes = List.of(new RepartionQueryDto(100, 200, 15, null, false));

            GenericError erreur = assertThrows(GenericError.class, () -> service.process(demandes));
            assertTrue(erreur.getMessage().contains("Cochez 'Créer la réserve'"));
            assertEquals(40, stockRayon.getQtyStock(), "rien n'a bougé avant le refus");
        }

        @Test
        @DisplayName("La création à la volée ouvre un emplacement de réserve à zéro avant d'y verser")
        void creationDeLEmplacementDestination() {
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 40);
            when(stockProduitRepository.getReferenceById(100)).thenReturn(stockRayon);

            service.process(List.of(new RepartionQueryDto(100, null, 15, null, true)));

            ArgumentCaptor<StockProduit> capteur = ArgumentCaptor.forClass(StockProduit.class);
            verify(stockProduitRepository, atLeastOnce()).save(capteur.capture());
            StockProduit cree = capteur.getAllValues().getFirst();
            assertSame(reserve, cree.getStorage(), "la destination ouverte est bien la réserve");
            assertSame(produit, cree.getProduit());

            RepartitionStockProduit trace = capturerLaTrace();
            assertEquals(0, trace.getDestInitStock());
            assertEquals(15, trace.getDestFinalStock());
            assertEquals(15, cree.getQtyStock());
        }

        @Test
        @DisplayName("Une quantité nulle ou négative, ou une destination indécise, est rejetée dès la saisie")
        void demandeInvalide() {
            assertThrows(GenericError.class, () -> new RepartionQueryDto(100, 200, 0, null, false));
            assertThrows(GenericError.class, () -> new RepartionQueryDto(100, 200, -3, null, false));
            assertThrows(GenericError.class, () -> new RepartionQueryDto(100, null, 5, null, false));
        }
    }

    @Nested
    @DisplayName("Transferts automatiques")
    class TransfertsAutomatiques {

        @Test
        @DisplayName("Créer un stock de réserve prélève d'autant le rayon, sans recréditer la destination")
        void creationDuStockDeReserve() {
            when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
            when(storageService.getUser()).thenReturn(operateur);
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 40);
            StockProduit stockReserve = stock(200, produit, reserve, 12);

            service.transferStockBetweenStorages(stockReserve);

            RepartitionStockProduit trace = capturerLaTrace();
            assertEquals(12, trace.getQtyMvt());
            assertEquals(40, trace.getSourceInitStock());
            assertEquals(28, trace.getSourceFinalStock());
            assertEquals(0, trace.getDestInitStock());
            assertEquals(12, trace.getDestFinalStock());
            assertEquals(28, stockRayon.getQtyStock());
            assertEquals(12, stockReserve.getQtyStock(), "la réserve porte déjà la quantité saisie");
            verify(lotStockLocationService).transferFefo(produit, rayon, reserve, 12);
        }

        @Test
        @DisplayName("Sans quantité saisie ou sans stock rayon, il n'y a rien à prélever")
        void rienAPrelever() {
            Produit produit = produit(10);
            service.transferStockBetweenStorages(stock(200, produit, reserve, 0));

            when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
            Produit isole = produit(11);
            service.transferStockBetweenStorages(stock(201, isole, reserve, 8));

            verifyNoInteractions(repartitionStockProduitRepository, lotStockLocationService);
        }

        @Test
        @DisplayName("La vente urgente rapatrie la réserve en rayon et marque le mouvement comme automatique")
        void transfertImplicitePourVenteUrgente() {
            when(storageService.getUser()).thenReturn(operateur);
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 0);
            StockProduit stockReserve = stock(200, produit, reserve, 30);
            when(stockProduitRepository.findOneByProduitIdAndStockageId(10, RAYON_ID)).thenReturn(stockRayon);
            when(stockProduitRepository.findOneByProduitIdAndStockageId(10, RESERVE_ID)).thenReturn(stockReserve);

            service.transfertImpliciteReserveVersRayon(10, RAYON_ID, RESERVE_ID, 6);

            RepartitionStockProduit trace = capturerLaTrace();
            assertEquals(TypeRepartition.AUTO, trace.getTypeRepartition(), "le mouvement n'est pas le fait d'une saisie");
            assertSame(stockReserve, trace.getStockProduitSource());
            assertSame(stockRayon, trace.getStockProduitDestination());
            assertEquals(30, trace.getSourceInitStock());
            assertEquals(24, trace.getSourceFinalStock());
            assertEquals(0, trace.getDestInitStock());
            assertEquals(6, trace.getDestFinalStock());
            assertEquals(24, stockReserve.getQtyStock());
            assertEquals(6, stockRayon.getQtyStock());
            verify(inventoryTransactionService).saveRepartition(trace);
            verify(lotStockLocationService).transferFefo(produit, reserve, rayon, 6);
        }

        @Test
        @DisplayName("Le surplus d'une réception descend en réserve et est marqué automatique")
        void versementDuSurplusEnReserve() {
            when(storageService.getUser()).thenReturn(operateur);
            Produit produit = produit(10);
            StockProduit stockRayon = stock(100, produit, rayon, 80);
            StockProduit stockReserve = stock(200, produit, reserve, 5);

            service.autoPutawayRayonToReserve(stockRayon, stockReserve, 30);

            RepartitionStockProduit trace = capturerLaTrace();
            assertEquals(TypeRepartition.AUTO, trace.getTypeRepartition());
            assertSame(stockRayon, trace.getStockProduitSource());
            assertSame(stockReserve, trace.getStockProduitDestination());
            assertEquals(50, stockRayon.getQtyStock());
            assertEquals(35, stockReserve.getQtyStock());
            verify(lotStockLocationService).transferFefo(produit, rayon, reserve, 30);
        }

        @Test
        @DisplayName("Un surplus nul ne produit ni trace ni mouvement de lot")
        void surplusNul() {
            Produit produit = produit(10);
            service.autoPutawayRayonToReserve(stock(100, produit, rayon, 80), stock(200, produit, reserve, 5), 0);

            verifyNoInteractions(
                repartitionStockProduitRepository,
                inventoryTransactionService,
                lotStockLocationService,
                storageService
            );
        }
    }

    /**
     * Les unités gratuites ne sont pas du stock comme les autres : elles ne se facturent pas et
     * doivent sortir en dernier. Le report du nouveau total sur le couple
     * {@code qty_ug} / {@code qty_stock} est donc la seule chose qui empêche une répartition de
     * transformer du gratuit en payant, ou l'inverse.
     */
    @Nested
    @DisplayName("Report sur les unités gratuites")
    class UnitesGratuites {

        @BeforeEach
        void utilisateur() {
            when(storageService.getUser()).thenReturn(operateur);
        }

        @Test
        @DisplayName("Le stock payant absorbe le mouvement tant que les unités gratuites tiennent")
        void lesUnitesGratuitesSontPreservees() {
            Produit produit = produit(10);
            StockProduit stockRayon = stockAvecUg(100, produit, rayon, 20, 5);
            StockProduit stockReserve = stock(200, produit, reserve, 0);

            service.autoPutawayRayonToReserve(stockRayon, stockReserve, 10);

            assertEquals(5, stockRayon.getQtyUG(), "les 5 unités gratuites restent en rayon");
            assertEquals(10, stockRayon.getQtyStock(), "25 - 10 = 15 au total, dont 5 gratuites");
            assertEquals(15, stockRayon.getQtyVirtual());
        }

        @Test
        @DisplayName("Quand le reste passe sous les unités gratuites, elles sont rabotées et le payant tombe à zéro")
        void lesUnitesGratuitesSontRabotees() {
            Produit produit = produit(10);
            StockProduit stockRayon = stockAvecUg(100, produit, rayon, 20, 5);
            StockProduit stockReserve = stock(200, produit, reserve, 0);

            service.autoPutawayRayonToReserve(stockRayon, stockReserve, 22);

            assertEquals(3, stockRayon.getQtyUG());
            assertEquals(0, stockRayon.getQtyStock());
            assertEquals(3, stockRayon.getQtyVirtual());
        }
    }

    @Nested
    @DisplayName("Historique des répartitions")
    class Historique {

        @Test
        @DisplayName("Le type de répartition est transmis à la requête sous forme d'ordinal")
        void typeTransmisEnOrdinal() {
            RepartionSearchQueryDto recherche = new RepartionSearchQueryDto(
                RAYON_ID,
                1,
                "DOLI",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                TypeRepartition.AUTO,
                100
            );
            Pageable page = PageRequest.of(0, 20);
            Page<RepartitionStockProduitProjection> retour = new PageImpl<>(List.of(projection()));
            when(
                repartitionStockProduitRepository.findRepartitionStockProduits(
                    1,
                    TypeRepartition.AUTO.ordinal(),
                    RAYON_ID,
                    100,
                    "DOLI",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 31),
                    page
                )
            ).thenReturn(retour);

            Page<RepartitionStockProduitDto> resultat = service.fetchRepartitionStockProduits(recherche, page);

            assertEquals(1, resultat.getTotalElements());
            RepartitionStockProduitDto dto = resultat.getContent().getFirst();
            assertEquals(55, dto.getId());
            assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30), dto.getCreated());
            assertEquals(12, dto.getMvtQty());
            assertEquals(40, dto.getSourceInitStock());
            assertEquals(28, dto.getSourceFinalStock());
            assertEquals(5, dto.getDestInitStock());
            assertEquals(17, dto.getDestFinalStock());
            assertEquals("DOLIPRANE 500", dto.getProduitName());
            assertEquals("CIP-1", dto.getCodeCip());

            StockProduitDTO source = dto.getStockProduitSrc();
            assertEquals(100, source.getId());
            assertEquals(RAYON_ID, source.getStorageId());
            assertEquals("Stock rayon", source.getStorageName());
            assertEquals("Stock réserve", dto.getStockProduitDest().getStorageName());
        }

        @Test
        @DisplayName("Sans filtre de type, la requête reçoit un ordinal nul plutôt qu'une valeur par défaut")
        void sansFiltreDeType() {
            RepartionSearchQueryDto recherche = new RepartionSearchQueryDto(
                null,
                null,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                null,
                null
            );
            Pageable page = PageRequest.of(0, 20);
            when(
                repartitionStockProduitRepository.findRepartitionStockProduits(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 31),
                    page
                )
            ).thenReturn(Page.empty());

            assertTrue(service.fetchRepartitionStockProduits(recherche, page).isEmpty());
        }

        @Test
        @DisplayName("Un mouvement sans source n'a pas de stock source dans le résultat")
        void mouvementSansSource() {
            RepartitionStockProduitProjection sansSource = mock(RepartitionStockProduitProjection.class);
            when(sansSource.getSrcStockId()).thenReturn(null);
            when(sansSource.getDestStockId()).thenReturn(200);
            RepartionSearchQueryDto recherche = new RepartionSearchQueryDto(
                null,
                null,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                null,
                null
            );
            Pageable page = PageRequest.of(0, 20);
            Page<RepartitionStockProduitProjection> retour = new PageImpl<>(List.of(sansSource));
            when(
                repartitionStockProduitRepository.findRepartitionStockProduits(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(page)
                )
            ).thenReturn(retour);

            RepartitionStockProduitDto dto = service.fetchRepartitionStockProduits(recherche, page).getContent().getFirst();

            assertNull(dto.getStockProduitSrc());
            assertEquals(200, dto.getStockProduitDest().getId());
        }

        @Test
        @DisplayName("L'export reprend la même recherche sans pagination et passe la main au rapport PDF")
        void exportSansPagination() {
            RepartionSearchQueryDto recherche = new RepartionSearchQueryDto(
                null,
                null,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                TypeRepartition.MANUEL,
                null
            );
            Page<RepartitionStockProduitProjection> retour = new PageImpl<>(List.of(projection()));
            when(
                repartitionStockProduitRepository.findRepartitionStockProduits(
                    null,
                    TypeRepartition.MANUEL.ordinal(),
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 31),
                    Pageable.unpaged()
                )
            ).thenReturn(retour);
            when(repartitionStockPdfReportService.export(any(), eq(recherche))).thenReturn(new byte[] { 1, 2, 3 });

            assertEquals(3, service.exportRepartitionStockProduits(recherche).length);

            ArgumentCaptor<List<RepartitionStockProduitDto>> capteur = ArgumentCaptor.captor();
            verify(repartitionStockPdfReportService).export(capteur.capture(), eq(recherche));
            assertEquals(1, capteur.getValue().size());
            assertEquals("DOLIPRANE 500", capteur.getValue().getFirst().getProduitName());
        }
    }

    // ===== fabriques du jeu d'essai =====

    private RepartitionStockProduit capturerLaTrace() {
        ArgumentCaptor<RepartitionStockProduit> capteur = ArgumentCaptor.forClass(RepartitionStockProduit.class);
        verify(repartitionStockProduitRepository).save(capteur.capture());
        return capteur.getValue();
    }

    private static Storage storage(int id, String nom, StorageType type) {
        Storage storage = new Storage();
        storage.setId(id);
        storage.setName(nom);
        storage.setStorageType(type);
        Magasin magasin = new Magasin();
        magasin.setId(1);
        storage.setMagasin(magasin);
        return storage;
    }

    private static Produit produit(int id) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setLibelle("PRODUIT " + id);
        return produit;
    }

    private static StockProduit stock(int id, Produit produit, Storage storage, int quantite) {
        return stockAvecUg(id, produit, storage, quantite, 0);
    }

    private static StockProduit stockAvecUg(int id, Produit produit, Storage storage, int quantite, int unitesGratuites) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setId(id);
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite + unitesGratuites);
        stockProduit.setQtyUG(unitesGratuites);
        produit.getStockProduits().add(stockProduit);
        return stockProduit;
    }

    private SuggestionReassort suggestion(TypeReassort type, StatutReassort statut) {
        SuggestionReassort suggestion = new SuggestionReassort();
        suggestion.setId(1);
        suggestion.setTypeReassort(type);
        suggestion.setStatut(statut);
        suggestion.setLastUserEdit(operateur);
        return suggestion;
    }

    /**
     * L'identifiant n'est pas décoratif : {@code LigneReassort} s'égale par son id, et deux lignes
     * sans id se confondraient dans le {@code Set} porté par la suggestion.
     */
    private static LigneReassort ligne(StockProduit destination, StockProduit source, int quantite) {
        LigneReassort ligne = new LigneReassort();
        ligne.setId(COMPTEUR_LIGNE.incrementAndGet());
        ligne.setStockProduit(destination);
        ligne.setStockProduitSrc(source);
        ligne.setQuantity(quantite);
        return ligne;
    }

    private static RepartitionStockProduitProjection projection() {
        RepartitionStockProduitProjection projection = mock(RepartitionStockProduitProjection.class);
        when(projection.getId()).thenReturn(55);
        when(projection.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 15, 9, 30));
        when(projection.getQtyMvt()).thenReturn(12);
        when(projection.getSourceInitStock()).thenReturn(40);
        when(projection.getSourceFinalStock()).thenReturn(28);
        when(projection.getDestInitStock()).thenReturn(5);
        when(projection.getDestFinalStock()).thenReturn(17);
        when(projection.getProduitName()).thenReturn("DOLIPRANE 500");
        when(projection.getCodeCip()).thenReturn("CIP-1");
        when(projection.getSrcStockId()).thenReturn(100);
        when(projection.getSrcStorageId()).thenReturn(RAYON_ID);
        when(projection.getSrcStorageName()).thenReturn("Stock rayon");
        when(projection.getDestStockId()).thenReturn(200);
        when(projection.getDestStorageId()).thenReturn(RESERVE_ID);
        when(projection.getDestStorageName()).thenReturn("Stock réserve");
        return projection;
    }
}
