package com.kobe.warehouse.service.stock.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.PrixReferenceRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.SubstitutRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.dto.produit.merge.LotConflictAction;
import com.kobe.warehouse.service.dto.produit.merge.LotResolutionDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergePreviewDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeRequestDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeResultDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.Cache;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Cible les scénarios de collision (contraintes UNIQUE sur produit_id) traités par
 * {@link ProduitMergeServiceImpl} : StockProduit (storage), Lot (num_lot), OptionPrixProduit
 * (tiersPayant/type), SalesLine (vente/date), FournisseurProduit (fournisseur), et le rejet
 * d'un produit parent de déclinaisons.
 */
@ExtendWith(MockitoExtension.class)
class ProduitMergeServiceImplTest {

    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private StockProduitRepository stockProduitRepository;
    @Mock
    private LotRepository lotRepository;
    @Mock
    private PrixReferenceRepository prixReferenceRepository;
    @Mock
    private FournisseurProduitRepository fournisseurProduitRepository;
    @Mock
    private RayonProduitRepository rayonProduitRepository;
    @Mock
    private SalesLineRepository salesLineRepository;
    @Mock
    private SemoisConfigurationRepository semoisConfigurationRepository;
    @Mock
    private VentesMensuellesAgregeesRepository ventesMensuellesAgregeesRepository;
    @Mock
    private StoreInventoryLineRepository storeInventoryLineRepository;
    @Mock
    private SubstitutRepository substitutRepository;
    @Mock
    private LogsService logsService;

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
    @Mock
    private Session session;
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Cache cache;

    private ProduitMergeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProduitMergeServiceImpl(
            produitRepository,
            stockProduitRepository,
            lotRepository,
            prixReferenceRepository,
            fournisseurProduitRepository,
            rayonProduitRepository,
            salesLineRepository,
            semoisConfigurationRepository,
            ventesMensuellesAgregeesRepository,
            storeInventoryLineRepository,
            substitutRepository,
            logsService
        );
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        // JPQL bulk (Ajustement/OrderLine repoint, entités simples) : chaîne createQuery -> setParameter -> executeUpdate
        lenient().when(entityManager.createQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        lenient().when(query.executeUpdate()).thenReturn(0);

        // Éviction du cache Hibernate en fin de fusion (target + chaque source)
        lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
        lenient().when(session.getSessionFactory()).thenReturn(sessionFactory);
        lenient().when(sessionFactory.getCache()).thenReturn(cache);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Produit produit(int id, String libelle) {
        Produit p = new Produit();
        p.setId(id);
        p.setLibelle(libelle);
        return p;
    }

    private void stubTargetAndSource(Produit target, Produit source) {
        lenient().when(produitRepository.findById(target.getId())).thenReturn(Optional.of(target));
        lenient().when(produitRepository.findById(source.getId())).thenReturn(Optional.of(source));
    }

    private List<String> capturedQueries() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, org.mockito.Mockito.atLeastOnce()).createQuery(captor.capture());
        return captor.getAllValues();
    }

    /**
     * {@link LotRepository} et {@link SalesLineRepository} étendent aussi {@code JpaSpecificationExecutor},
     * qui expose depuis Spring Data JPA 4.x un {@code delete(DeleteSpecification<T>)} en plus du
     * {@code delete(T)} de {@code CrudRepository} — javac ne sait plus résoudre l'appel `.delete(entity)`
     * sur le mock (erreur "reference to delete is ambiguous"). On force la résolution en élargissant
     * le type statique vers {@code CrudRepository} avant de vérifier.
     */
    private org.springframework.data.repository.CrudRepository<Lot, Integer> asCrud(LotRepository repo) {
        return repo;
    }

    private org.springframework.data.repository.CrudRepository<SalesLine, com.kobe.warehouse.domain.SaleLineId> asCrud(SalesLineRepository repo) {
        return repo;
    }

    // ── 1. StockProduit — collision même storage ────────────────────────────

    @Test
    void should_notMergeStockQuantities_whenSameStorageCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Storage storage = new Storage();
        storage.setId(10);
        storage.setName("Réserve");

        StockProduit sourceStock = new StockProduit();
        sourceStock.setId(100);
        sourceStock.setProduit(source);
        sourceStock.setStorage(storage);
        sourceStock.setQtyStock(5);
        sourceStock.setQtyVirtual(2);
        sourceStock.setQtyUG(1);

        StockProduit targetStock = new StockProduit();
        targetStock.setId(200);
        targetStock.setProduit(target);
        targetStock.setStorage(storage);
        targetStock.setQtyStock(3);
        targetStock.setQtyVirtual(1);
        targetStock.setQtyUG(0);

        when(stockProduitRepository.findAllByProduitId(source.getId())).thenReturn(List.of(sourceStock));
        when(stockProduitRepository.findStockProduitByStorageIdAndProduitId(storage.getId(), target.getId()))
            .thenReturn(Optional.of(targetStock));

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of()));

        // Ni la cible ni le doublon ne sont modifiés : aucune fusion de quantité, aucune
        // suppression, aucun repoint de l'historique d'Ajustement (qui fausserait sa lecture).
        assertEquals(3, targetStock.getQtyStock());
        assertEquals(1, targetStock.getQtyVirtual());
        assertEquals(0, targetStock.getQtyUG());
        verify(stockProduitRepository, never()).save(targetStock);
        verify(stockProduitRepository, never()).save(sourceStock);
        verify(stockProduitRepository, never()).delete(any());
        assertTrue(capturedQueries().stream().noneMatch(q -> q.contains("a.stockProduit = :kept")));

        // Le conflit est remonté pour que l'utilisateur fasse un ajustement manuel.
        assertEquals(1, result.stockConflicts().size());
        var conflict = result.stockConflicts().get(0);
        assertEquals(storage.getId(), conflict.storageId());
        assertEquals(5, conflict.sourceQtyStock());
        assertEquals(3, conflict.targetQtyStock());
    }

    @Test
    void should_reassignStockProduit_whenNoStorageCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Storage storageB = new Storage();
        storageB.setId(20);

        StockProduit sourceStock = new StockProduit();
        sourceStock.setId(101);
        sourceStock.setProduit(source);
        sourceStock.setStorage(storageB);
        sourceStock.setQtyStock(4);
        sourceStock.setQtyVirtual(0);
        sourceStock.setQtyUG(0);

        when(stockProduitRepository.findAllByProduitId(source.getId())).thenReturn(List.of(sourceStock));
        when(stockProduitRepository.findStockProduitByStorageIdAndProduitId(storageB.getId(), target.getId()))
            .thenReturn(Optional.empty());

        service.merge(new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of()));

        assertEquals(target, sourceStock.getProduit());
        verify(stockProduitRepository).save(sourceStock);
        verify(stockProduitRepository, never()).delete(any());
    }

    // ── 2. Lot — collision même num_lot ──────────────────────────────────────

    @Test
    void should_mergeLotQuantities_whenSameNumLotCollisionResolvedAsMerge() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Lot sourceLot = new Lot().setId(300).setNumLot("L1").setQuantity(5).setCurrentQuantity(5).setFreeQty(2);
        sourceLot.setProduit(source);
        Lot targetLot = new Lot().setId(400).setNumLot("L1").setQuantity(3).setCurrentQuantity(3).setFreeQty(1);
        targetLot.setProduit(target);

        when(lotRepository.findByProduitId(source.getId())).thenReturn(List.of(sourceLot));
        when(lotRepository.findByProduitId(target.getId())).thenReturn(List.of(targetLot));

        ProduitMergeRequestDTO request = new ProduitMergeRequestDTO(
            target.getId(),
            List.of(source.getId()),
            List.of(new LotResolutionDTO(sourceLot.getId(), LotConflictAction.MERGE))
        );

        service.merge(request);

        assertEquals(8, targetLot.getQuantity());
        assertEquals(8, targetLot.getCurrentQuantity());
        assertEquals(3, targetLot.getFreeQty());
        verify(lotRepository).save(targetLot);
        verify(asCrud(lotRepository)).delete(sourceLot);
        // L'historique d'Ajustement du lot source n'est jamais repointé vers le lot cible
        // (cela fausserait sa lecture) : il est détaché (lot = null), pas réécrit.
        assertTrue(capturedQueries().stream().noneMatch(q -> q.contains("a.lot = :kept")));
        assertTrue(capturedQueries().stream().anyMatch(q -> q.contains("a.lot = null")));
    }

    @Test
    void should_deleteSourceLotWithoutMerging_whenCollisionResolvedAsDelete() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Lot sourceLot = new Lot().setId(301).setNumLot("L2").setQuantity(5).setCurrentQuantity(5).setFreeQty(2);
        sourceLot.setProduit(source);
        Lot targetLot = new Lot().setId(401).setNumLot("L2").setQuantity(3).setCurrentQuantity(3).setFreeQty(1);
        targetLot.setProduit(target);

        when(lotRepository.findByProduitId(source.getId())).thenReturn(List.of(sourceLot));
        when(lotRepository.findByProduitId(target.getId())).thenReturn(List.of(targetLot));

        ProduitMergeRequestDTO request = new ProduitMergeRequestDTO(
            target.getId(),
            List.of(source.getId()),
            List.of(new LotResolutionDTO(sourceLot.getId(), LotConflictAction.DELETE))
        );

        service.merge(request);

        assertEquals(3, targetLot.getQuantity(), "le lot cible ne doit pas être modifié");
        verify(lotRepository, never()).save(targetLot);
        verify(asCrud(lotRepository)).delete(sourceLot);
        assertTrue(capturedQueries().stream().anyMatch(q -> q.contains("a.lot = null")));
    }

    @Test
    void should_rejectMerge_whenLotConflictHasNoResolution() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Lot sourceLot = new Lot().setId(302).setNumLot("L3").setQuantity(5).setCurrentQuantity(5).setFreeQty(2);
        sourceLot.setProduit(source);
        Lot targetLot = new Lot().setId(402).setNumLot("L3").setQuantity(3).setCurrentQuantity(3).setFreeQty(1);
        targetLot.setProduit(target);

        when(lotRepository.findByProduitId(source.getId())).thenReturn(List.of(sourceLot));
        when(lotRepository.findByProduitId(target.getId())).thenReturn(List.of(targetLot));

        ProduitMergeRequestDTO request = new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of());

        BadRequestAlertException ex = assertThrows(BadRequestAlertException.class, () -> service.merge(request));
        assertTrue(ex.getMessage().contains("L3"));

        verifyNoInteractions(stockProduitRepository, prixReferenceRepository, fournisseurProduitRepository, salesLineRepository, rayonProduitRepository, substitutRepository);
        verify(lotRepository, never()).save(any());
        verify(asCrud(lotRepository), never()).delete(any());
        verify(produitRepository, never()).save(any());
    }

    // ── 3. OptionPrixProduit — collision même (tiersPayant, type) ────────────

    @Test
    void should_keepTargetOptionPrix_whenSameTiersPayantAndTypeCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        TiersPayant tiersPayant = new TiersPayant().setId(50);

        OptionPrixProduit sourceOption = new OptionPrixProduit().setId(500).setTiersPayant(tiersPayant).setType(OptionPrixType.REFERENCE);
        sourceOption.setProduit(source);
        OptionPrixProduit targetOption = new OptionPrixProduit().setId(600).setTiersPayant(tiersPayant).setType(OptionPrixType.REFERENCE);
        targetOption.setProduit(target);

        when(prixReferenceRepository.findAllByProduitId(source.getId())).thenReturn(List.of(sourceOption));
        when(prixReferenceRepository.findAllByProduitId(target.getId())).thenReturn(List.of(targetOption));

        service.merge(new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of()));

        verify(prixReferenceRepository).delete(sourceOption);
        verify(prixReferenceRepository, never()).save(sourceOption);
    }

    // ── 4. SalesLine — collision même vente/date ─────────────────────────────

    @Test
    void should_mergeSalesLineQuantities_whenSameSaleAndDateCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        LocalDate saleDate = LocalDate.of(2026, 1, 15);
        Sales sales = mock(Sales.class);
        when(sales.getId()).thenReturn(new SaleId(9L, saleDate));

        SalesLine sourceLine = new SalesLine();
        sourceLine.setSales(sales);
        sourceLine.setSaleDate(saleDate);
        sourceLine.setProduit(source);
        sourceLine.setQuantitySold(2);
        sourceLine.setQuantityRequested(2);
        sourceLine.setQuantityUg(0);
        sourceLine.setQuantityAvoir(0);
        sourceLine.setDiscountAmount(0);
        sourceLine.setSalesAmount(1000);
        sourceLine.setTaxValue(50);
        sourceLine.setCostAmount(500);
        sourceLine.setAmountToBeTakenIntoAccount(1000);
        sourceLine.setLots(new ArrayList<>(List.of(new LotSold(1, "L1", 2, saleDate))));

        SalesLine targetLine = new SalesLine();
        targetLine.setSales(sales);
        targetLine.setSaleDate(saleDate);
        targetLine.setProduit(target);
        targetLine.setQuantitySold(3);
        targetLine.setQuantityRequested(3);
        targetLine.setQuantityUg(0);
        targetLine.setQuantityAvoir(0);
        targetLine.setDiscountAmount(0);
        targetLine.setSalesAmount(1500);
        targetLine.setTaxValue(75);
        targetLine.setCostAmount(750);
        targetLine.setAmountToBeTakenIntoAccount(1500);
        targetLine.setLots(new ArrayList<>(List.of(new LotSold(2, "L2", 3, saleDate))));

        when(salesLineRepository.findAllByProduitId(source.getId())).thenReturn(List.of(sourceLine));
        when(salesLineRepository.findBySalesIdAndProduitIdAndSalesSaleDate(9L, target.getId(), saleDate)).thenReturn(Optional.of(targetLine));

        service.merge(new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of()));

        assertEquals(5, targetLine.getQuantitySold());
        assertEquals(2500, targetLine.getSalesAmount());
        assertEquals(2, targetLine.getLots().size());
        verify(salesLineRepository).save(targetLine);
        verify(asCrud(salesLineRepository)).delete(sourceLine);
    }

    // ── 5. FournisseurProduit — collision même fournisseur ───────────────────

    @Test
    void should_repointOrderLinesAndPrincipal_whenSameFournisseurCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(70);

        FournisseurProduit sourceFp = new FournisseurProduit();
        sourceFp.setId(800);
        sourceFp.setProduit(source);
        sourceFp.setFournisseur(fournisseur);
        source.setFournisseurProduitPrincipal(sourceFp);

        FournisseurProduit targetFp = new FournisseurProduit();
        targetFp.setId(900);
        targetFp.setProduit(target);
        targetFp.setFournisseur(fournisseur);

        when(fournisseurProduitRepository.findAllByProduitId(source.getId())).thenReturn(List.of(sourceFp));
        when(fournisseurProduitRepository.findAllByProduitId(target.getId())).thenReturn(List.of(targetFp));

        service.merge(new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of()));

        // fournisseur_produit_principal_id est UNIQUE : on ne repointe jamais le produit source
        // vers le FournisseurProduit conservé (risque de doublon avec le principal de la cible) ;
        // le produit source étant archivé, son principal est simplement vidé.
        assertEquals(null, source.getFournisseurProduitPrincipal());
        verify(fournisseurProduitRepository).delete(sourceFp);
        assertTrue(capturedQueries().stream().anyMatch(q -> q.contains("ol.fournisseurProduit = :kept")));
    }

    // ── 6. Produit avec plusieurs produits détail — rejet (cas non géré) ─────

    @Test
    void should_rejectSourceInPreview_whenSourceHasMoreThanOneChild() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit child1 = produit(3, "Detail1");
        Produit child2 = produit(4, "Detail2");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(target.getId())).thenReturn(List.of());
        when(produitRepository.findAllByParentId(source.getId())).thenReturn(List.of(child1, child2));

        ProduitMergePreviewDTO preview = service.preview(target.getId(), List.of(source.getId()));

        assertTrue(preview.rejectedSourceIds().contains(source.getId()));
        assertTrue(preview.rejectionReasons().containsKey(source.getId().toString()));
        assertTrue(preview.sourceIds().isEmpty(), "aucune source valide ne doit être retenue");
    }

    @Test
    void should_rejectMerge_whenSourceHasMoreThanOneChild() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit child1 = produit(3, "Detail1");
        Produit child2 = produit(4, "Detail2");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(target.getId())).thenReturn(List.of());
        when(produitRepository.findAllByParentId(source.getId())).thenReturn(List.of(child1, child2));

        ProduitMergeRequestDTO request = new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of());

        BadRequestAlertException ex = assertThrows(BadRequestAlertException.class, () -> service.merge(request));
        assertTrue(ex.getMessage().contains("Source"));

        verifyNoInteractions(stockProduitRepository, prixReferenceRepository, fournisseurProduitRepository, salesLineRepository, rayonProduitRepository, substitutRepository);
        verify(produitRepository, never()).save(any());
    }

    @Test
    void should_rejectMerge_whenTargetHasMoreThanOneChild() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit child1 = produit(3, "Detail1");
        Produit child2 = produit(4, "Detail2");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(target.getId())).thenReturn(List.of(child1, child2));

        ProduitMergeRequestDTO request = new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of());

        assertThrows(BadRequestAlertException.class, () -> service.merge(request));
        verifyNoInteractions(stockProduitRepository, prixReferenceRepository, fournisseurProduitRepository, salesLineRepository, rayonProduitRepository, substitutRepository);
        verify(produitRepository, never()).save(any());
    }

    // ── 7. Boîtes avec déconditionné — fusion des ventes / re-parentage ──────

    @Test
    void should_reparentSourceChild_whenOnlySourceHasDetail() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit sourceChild = produit(10, "SourceDetail");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(target.getId())).thenReturn(List.of());
        when(produitRepository.findAllByParentId(source.getId())).thenReturn(List.of(sourceChild));

        service.merge(new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of()));

        assertEquals(target, sourceChild.getParent());
        verify(produitRepository).save(sourceChild);
        verify(salesLineRepository, never()).findAllByProduitId(sourceChild.getId());
    }

    @Test
    void should_mergeChildSalesOnlyAndArchiveSourceChild_whenBothHaveDetail() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit targetChild = produit(20, "TargetDetail");
        Produit sourceChild = produit(21, "SourceDetail");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(source.getId())).thenReturn(List.of(sourceChild));
        when(produitRepository.findAllByParentId(target.getId())).thenReturn(List.of(targetChild));
        when(salesLineRepository.findAllByProduitId(sourceChild.getId())).thenReturn(List.of());

        ProduitMergeResultDTO result = service.merge(
            new ProduitMergeRequestDTO(target.getId(), List.of(source.getId()), List.of())
        );

        // Ventes fusionnées (ici aucune ligne, mais l'appel a bien eu lieu sur le couple détail).
        verify(salesLineRepository).findAllByProduitId(sourceChild.getId());
        // Le détail source est archivé comme la boîte source, jamais supprimé ni laissé actif en doublon.
        assertEquals(com.kobe.warehouse.domain.enumeration.Status.DISABLE, sourceChild.getStatus());
        verify(produitRepository).save(sourceChild);
        assertEquals(1, result.entityCounts().get("produitDetailFusionne"));
    }

    // ── 12. preview — recensement, rejets et conflits ────────────────────────

    /** Le comptage des entités simples passe par une requête typée : createQuery(String, Long.class). */
    private void stubSimpleEntityCount(long count) {
        @SuppressWarnings("unchecked")
        jakarta.persistence.TypedQuery<Long> typed = mock(jakarta.persistence.TypedQuery.class);
        lenient().when(entityManager.createQuery(anyString(), org.mockito.ArgumentMatchers.eq(Long.class))).thenReturn(typed);
        lenient().when(typed.setParameter(anyString(), any())).thenReturn(typed);
        lenient().when(typed.getSingleResult()).thenReturn(count);
    }

    @Test
    void should_rejectSourceInPreview_whenSourceDoesNotExist() {
        Produit target = produit(1, "Target");
        when(produitRepository.findById(1)).thenReturn(Optional.of(target));
        when(produitRepository.findById(99)).thenReturn(Optional.empty());
        stubSimpleEntityCount(0L);

        ProduitMergePreviewDTO preview = service.preview(1, List.of(99));

        assertTrue(preview.sourceIds().isEmpty());
        assertEquals(List.of(99), preview.rejectedSourceIds());
        assertEquals("Produit introuvable", preview.rejectionReasons().get("99"));
    }

    @Test
    void should_rejectAllSourcesInPreview_whenTargetHasMoreThanOneChild() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(1)).thenReturn(List.of(produit(11, "d1"), produit(12, "d2")));
        stubSimpleEntityCount(0L);

        ProduitMergePreviewDTO preview = service.preview(1, List.of(2));

        assertTrue(preview.sourceIds().isEmpty());
        assertEquals(List.of(2), preview.rejectedSourceIds());
        assertTrue(preview.rejectionReasons().get("2").contains("produit cible"));
    }

    @Test
    void should_ignoreTargetIdAndDuplicates_whenListedAsSource() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        stubSimpleEntityCount(0L);

        ProduitMergePreviewDTO preview = service.preview(1, List.of(1, 2, 2));

        assertEquals(List.of(2), preview.sourceIds());
        assertTrue(preview.rejectedSourceIds().isEmpty());
    }

    @Test
    void should_countRelatedEntities_inPreview() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        when(stockProduitRepository.countByProduitId(2)).thenReturn(3L);
        when(prixReferenceRepository.countByProduitId(2)).thenReturn(2L);
        when(fournisseurProduitRepository.countByProduitId(2)).thenReturn(1L);
        when(rayonProduitRepository.countByProduitId(2)).thenReturn(4L);
        when(salesLineRepository.countByProduitId(2)).thenReturn(10L);
        when(substitutRepository.countByProduitId(2)).thenReturn(1L);
        when(substitutRepository.countBySubstitutId(2)).thenReturn(2L);
        stubSimpleEntityCount(5L);

        ProduitMergePreviewDTO preview = service.preview(1, List.of(2));

        assertEquals(3, preview.entityCounts().get("stockProduit"));
        assertEquals(2, preview.entityCounts().get("optionPrixProduit"));
        assertEquals(1, preview.entityCounts().get("fournisseurProduit"));
        assertEquals(4, preview.entityCounts().get("rayonProduit"));
        assertEquals(10, preview.entityCounts().get("salesLine"));
        assertEquals(3, preview.entityCounts().get("substitut"));
        assertEquals(0, preview.entityCounts().get("lot"));
        // chacune des huit entités simples est comptée par une requête dédiée
        assertEquals(5, preview.entityCounts().get("Rupture"));
        assertEquals(5, preview.entityCounts().get("Decondition"));
    }

    @Test
    void should_reportLotAndStockConflicts_inPreview() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        stubSimpleEntityCount(0L);

        Lot targetLot = new Lot();
        targetLot.setId(200);
        targetLot.setNumLot("L1");
        targetLot.setQuantity(4);
        targetLot.setExpiryDate(LocalDate.of(2027, 1, 31));
        Lot sourceLot = new Lot();
        sourceLot.setId(100);
        sourceLot.setNumLot("L1");
        sourceLot.setQuantity(6);
        sourceLot.setExpiryDate(LocalDate.of(2027, 2, 28));
        when(lotRepository.findByProduitId(1)).thenReturn(List.of(targetLot));
        when(lotRepository.findByProduitId(2)).thenReturn(List.of(sourceLot));

        Storage storage = new Storage();
        storage.setId(10);
        storage.setName("Réserve");
        StockProduit targetStock = new StockProduit();
        targetStock.setStorage(storage);
        targetStock.setQtyStock(3);
        targetStock.setQtyVirtual(1);
        targetStock.setQtyUG(0);
        StockProduit sourceStock = new StockProduit();
        sourceStock.setStorage(storage);
        sourceStock.setQtyStock(5);
        sourceStock.setQtyVirtual(2);
        sourceStock.setQtyUG(1);
        when(stockProduitRepository.findAllByProduitId(1)).thenReturn(List.of(targetStock));
        when(stockProduitRepository.findAllByProduitId(2)).thenReturn(List.of(sourceStock));

        ProduitMergePreviewDTO preview = service.preview(1, List.of(2));

        assertEquals(1, preview.lotConflicts().size());
        assertEquals("L1", preview.lotConflicts().get(0).numLot());
        assertEquals(100, preview.lotConflicts().get(0).sourceLotId());
        assertEquals(200, preview.lotConflicts().get(0).targetLotId());
        assertEquals(1, preview.entityCounts().get("lot"));

        assertEquals(1, preview.stockConflicts().size());
        assertEquals(10, preview.stockConflicts().get(0).storageId());
        assertEquals(5, preview.stockConflicts().get(0).sourceQtyStock());
        assertEquals(3, preview.stockConflicts().get(0).targetQtyStock());
    }

    @Test
    void should_reportNoConflict_inPreview_whenNumLotAndStorageDiffer() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        stubSimpleEntityCount(0L);

        Lot targetLot = new Lot();
        targetLot.setId(200);
        targetLot.setNumLot("L1");
        Lot sourceLot = new Lot();
        sourceLot.setId(100);
        sourceLot.setNumLot("L2");
        when(lotRepository.findByProduitId(1)).thenReturn(List.of(targetLot));
        when(lotRepository.findByProduitId(2)).thenReturn(List.of(sourceLot));

        Storage s1 = new Storage();
        s1.setId(10);
        Storage s2 = new Storage();
        s2.setId(20);
        StockProduit targetStock = new StockProduit();
        targetStock.setStorage(s1);
        StockProduit sourceStock = new StockProduit();
        sourceStock.setStorage(s2);
        when(stockProduitRepository.findAllByProduitId(1)).thenReturn(List.of(targetStock));
        when(stockProduitRepository.findAllByProduitId(2)).thenReturn(List.of(sourceStock));

        ProduitMergePreviewDTO preview = service.preview(1, List.of(2));

        assertTrue(preview.lotConflicts().isEmpty());
        assertTrue(preview.stockConflicts().isEmpty());
    }

    @Test
    void should_announceDetailReparentage_inPreview_whenOnlySourceHasChild() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(1)).thenReturn(List.of());
        when(produitRepository.findAllByParentId(2)).thenReturn(List.of(produit(21, "detail")));
        stubSimpleEntityCount(0L);

        ProduitMergePreviewDTO preview = service.preview(1, List.of(2));

        assertEquals(1, preview.entityCounts().get("produitDetailReparente"));
    }

    @Test
    void should_announceDetailFusion_inPreview_whenBothHaveChild() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        when(produitRepository.findAllByParentId(1)).thenReturn(List.of(produit(11, "detail cible")));
        when(produitRepository.findAllByParentId(2)).thenReturn(List.of(produit(21, "detail source")));
        stubSimpleEntityCount(0L);

        ProduitMergePreviewDTO preview = service.preview(1, List.of(2));

        assertEquals(1, preview.entityCounts().get("produitDetailFusionne"));
    }

    @Test
    void should_rejectPreview_whenTargetDoesNotExist() {
        when(produitRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(BadRequestAlertException.class, () -> service.preview(1, List.of(2)));
    }

    // ── 13. merge — gardes d'entrée ──────────────────────────────────────────

    @Test
    void should_rejectMerge_whenTargetIdIsNull() {
        assertThrows(BadRequestAlertException.class, () -> service.merge(new ProduitMergeRequestDTO(null, List.of(2), List.of())));
        verifyNoInteractions(produitRepository);
    }

    @Test
    void should_rejectMerge_whenSourceIdsAreNull() {
        assertThrows(BadRequestAlertException.class, () -> service.merge(new ProduitMergeRequestDTO(1, null, List.of())));
    }

    @Test
    void should_rejectMerge_whenSourceIdsAreEmpty() {
        assertThrows(BadRequestAlertException.class, () -> service.merge(new ProduitMergeRequestDTO(1, List.of(), List.of())));
    }

    @Test
    void should_rejectMerge_whenOnlySourceIsTheTargetItself() {
        Produit target = produit(1, "Target");
        when(produitRepository.findById(1)).thenReturn(Optional.of(target));

        assertThrows(BadRequestAlertException.class, () -> service.merge(new ProduitMergeRequestDTO(1, List.of(1), List.of())));
    }

    @Test
    void should_rejectMerge_whenTargetDoesNotExist() {
        when(produitRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(BadRequestAlertException.class, () -> service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of())));
    }

    @Test
    void should_rejectMerge_whenSourceDoesNotExist() {
        when(produitRepository.findById(1)).thenReturn(Optional.of(produit(1, "Target")));
        when(produitRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(BadRequestAlertException.class, () -> service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of())));
    }

    @Test
    void should_tolerateNullLotResolutions() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), null));

        assertEquals(List.of(2), result.mergedSourceIds());
    }

    @Test
    void should_mentionStockConflictsInLog_whenAdjustmentIsRequired() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Storage storage = new Storage();
        storage.setId(10);
        storage.setName("Réserve");
        StockProduit sourceStock = new StockProduit();
        sourceStock.setStorage(storage);
        sourceStock.setQtyStock(5);
        sourceStock.setQtyVirtual(2);
        sourceStock.setQtyUG(1);
        StockProduit targetStock = new StockProduit();
        targetStock.setStorage(storage);
        targetStock.setQtyStock(3);
        targetStock.setQtyVirtual(1);
        targetStock.setQtyUG(0);
        when(stockProduitRepository.findAllByProduitId(2)).thenReturn(List.of(sourceStock));
        when(stockProduitRepository.findStockProduitByStorageIdAndProduitId(10, 1)).thenReturn(Optional.of(targetStock));

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        ArgumentCaptor<String> comments = ArgumentCaptor.forClass(String.class);
        verify(logsService).create(any(), comments.capture(), anyString());
        assertTrue(comments.getValue().contains("ajustement de stock manuel requis sur 1 emplacement(s)"));
    }

    // ── 14. Réaffectations sans collision ────────────────────────────────────

    @Test
    void should_reassignLot_whenNoNumLotCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Lot sourceLot = new Lot();
        sourceLot.setId(100);
        sourceLot.setNumLot("L2");
        when(lotRepository.findByProduitId(2)).thenReturn(List.of(sourceLot));
        when(lotRepository.findByProduitId(1)).thenReturn(List.of());

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, sourceLot.getProduit());
        verify(lotRepository).save(sourceLot);
        verify(asCrud(lotRepository), never()).delete(any(Lot.class));
        assertEquals(1, result.entityCounts().get("lot"));
    }

    @Test
    void should_reassignOptionPrix_whenNoCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        TiersPayant tp = new TiersPayant();
        tp.setId(7);
        OptionPrixProduit option = new OptionPrixProduit();
        option.setId(50);
        option.setTiersPayant(tp);
        option.setType(OptionPrixType.REFERENCE);
        when(prixReferenceRepository.findAllByProduitId(2)).thenReturn(List.of(option));
        when(prixReferenceRepository.findAllByProduitId(1)).thenReturn(List.of());

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, option.getProduit());
        verify(prixReferenceRepository).save(option);
        verify(prixReferenceRepository, never()).delete(any());
    }

    @Test
    void should_reassignFournisseurProduit_whenNoCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(3);
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(80);
        fp.setFournisseur(fournisseur);
        when(fournisseurProduitRepository.findAllByProduitId(2)).thenReturn(List.of(fp));
        when(fournisseurProduitRepository.findAllByProduitId(1)).thenReturn(List.of());

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, fp.getProduit());
        verify(fournisseurProduitRepository).save(fp);
        verify(fournisseurProduitRepository, never()).delete(any());
    }

    @Test
    void should_keepSourcePrincipal_whenCollidingFournisseurProduitIsNotThePrincipal() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(3);
        FournisseurProduit sourceFp = new FournisseurProduit();
        sourceFp.setId(80);
        sourceFp.setFournisseur(fournisseur);
        FournisseurProduit targetFp = new FournisseurProduit();
        targetFp.setId(90);
        targetFp.setFournisseur(fournisseur);
        FournisseurProduit autrePrincipal = new FournisseurProduit();
        autrePrincipal.setId(81);
        source.setFournisseurProduitPrincipal(autrePrincipal);

        when(fournisseurProduitRepository.findAllByProduitId(2)).thenReturn(List.of(sourceFp));
        when(fournisseurProduitRepository.findAllByProduitId(1)).thenReturn(List.of(targetFp));

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(autrePrincipal, source.getFournisseurProduitPrincipal());
        verify(fournisseurProduitRepository).delete(sourceFp);
    }

    @Test
    void should_reassignSalesLine_whenNoSaleCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Sales sales = mock(Sales.class);
        when(sales.getId()).thenReturn(new SaleId(500L, LocalDate.of(2026, 4, 18)));
        SalesLine line = new SalesLine();
        line.setSales(sales);
        line.setSaleDate(LocalDate.of(2026, 4, 18));
        when(salesLineRepository.findAllByProduitId(2)).thenReturn(List.of(line));
        when(salesLineRepository.findBySalesIdAndProduitIdAndSalesSaleDate(500L, 1, LocalDate.of(2026, 4, 18)))
            .thenReturn(Optional.empty());

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, line.getProduit());
        verify(salesLineRepository).save(line);
        verify(asCrud(salesLineRepository), never()).delete(any(SalesLine.class));
    }

    // ── 15. RayonProduit ─────────────────────────────────────────────────────

    @Test
    void should_reassignRayonProduit_whenNoRayonCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Rayon rayon = new com.kobe.warehouse.domain.Rayon();
        rayon.setId(4);
        com.kobe.warehouse.domain.RayonProduit rp = new com.kobe.warehouse.domain.RayonProduit();
        rp.setId(60).setRayon(rayon);
        when(rayonProduitRepository.findAllByProduitId(2)).thenReturn(java.util.Set.of(rp));
        when(rayonProduitRepository.findAllByProduitId(1)).thenReturn(java.util.Set.of());

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, rp.getProduit());
        verify(rayonProduitRepository).save(rp);
        assertEquals(1, result.entityCounts().get("rayonProduit"));
    }

    @Test
    void should_deleteRayonProduit_whenSameRayonCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Rayon rayon = new com.kobe.warehouse.domain.Rayon();
        rayon.setId(4);
        com.kobe.warehouse.domain.RayonProduit sourceRp = new com.kobe.warehouse.domain.RayonProduit();
        sourceRp.setId(60).setRayon(rayon);
        com.kobe.warehouse.domain.RayonProduit targetRp = new com.kobe.warehouse.domain.RayonProduit();
        targetRp.setId(61).setRayon(rayon);
        when(rayonProduitRepository.findAllByProduitId(2)).thenReturn(java.util.Set.of(sourceRp));
        when(rayonProduitRepository.findAllByProduitId(1)).thenReturn(java.util.Set.of(targetRp));

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        verify(rayonProduitRepository).delete(sourceRp);
        verify(rayonProduitRepository, never()).save(sourceRp);
    }

    // ── 16. Substituts ───────────────────────────────────────────────────────

    private com.kobe.warehouse.domain.Substitut substitut(int id, Produit produit, Produit remplacant) {
        com.kobe.warehouse.domain.Substitut s = new com.kobe.warehouse.domain.Substitut();
        s.setId(id);
        s.setProduit(produit);
        s.setSubstitut(remplacant);
        return s;
    }

    @Test
    void should_reassignSubstitut_whenNoCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit autre = produit(3, "Autre");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Substitut s = substitut(70, source, autre);
        when(substitutRepository.findAllByProduitId(2)).thenReturn(List.of(s));
        when(substitutRepository.findAllBySubstitutId(2)).thenReturn(List.of());
        when(substitutRepository.existsByProduitAndSubstitut(target, autre)).thenReturn(false);

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, s.getProduit());
        verify(substitutRepository).save(s);
        assertEquals(1, result.entityCounts().get("substitut"));
    }

    @Test
    void should_deleteSubstitut_whenItWouldPointToTheTargetItself() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Substitut s = substitut(70, source, target);
        when(substitutRepository.findAllByProduitId(2)).thenReturn(List.of(s));
        when(substitutRepository.findAllBySubstitutId(2)).thenReturn(List.of());

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        verify(substitutRepository).delete(s);
        verify(substitutRepository, never()).save(s);
    }

    @Test
    void should_deleteSubstitut_whenTargetAlreadyHasTheSamePair() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit autre = produit(3, "Autre");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Substitut s = substitut(70, source, autre);
        when(substitutRepository.findAllByProduitId(2)).thenReturn(List.of(s));
        when(substitutRepository.findAllBySubstitutId(2)).thenReturn(List.of());
        when(substitutRepository.existsByProduitAndSubstitut(target, autre)).thenReturn(true);

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        verify(substitutRepository).delete(s);
    }

    @Test
    void should_reassignReverseSubstitut_whenNoCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit autre = produit(3, "Autre");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Substitut s = substitut(71, autre, source);
        when(substitutRepository.findAllByProduitId(2)).thenReturn(List.of());
        when(substitutRepository.findAllBySubstitutId(2)).thenReturn(List.of(s));
        when(substitutRepository.existsByProduitAndSubstitut(autre, target)).thenReturn(false);

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, s.getSubstitut());
        verify(substitutRepository).save(s);
        assertEquals(1, result.entityCounts().get("substitut"));
    }

    @Test
    void should_deleteReverseSubstitut_whenItWouldPointToTheTargetItself() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Substitut s = substitut(71, target, source);
        when(substitutRepository.findAllByProduitId(2)).thenReturn(List.of());
        when(substitutRepository.findAllBySubstitutId(2)).thenReturn(List.of(s));

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        verify(substitutRepository).delete(s);
    }

    @Test
    void should_deleteReverseSubstitut_whenTargetAlreadyHasTheSamePair() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        Produit autre = produit(3, "Autre");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.Substitut s = substitut(71, autre, source);
        when(substitutRepository.findAllByProduitId(2)).thenReturn(List.of());
        when(substitutRepository.findAllBySubstitutId(2)).thenReturn(List.of(s));
        when(substitutRepository.existsByProduitAndSubstitut(autre, target)).thenReturn(true);

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        verify(substitutRepository).delete(s);
    }

    // ── 17. Configuration SEMOIS ─────────────────────────────────────────────

    @Test
    void should_countNoSemoisConfiguration_whenSourceHasNone() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        when(semoisConfigurationRepository.findByProduitId(2)).thenReturn(Optional.empty());

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(0, result.entityCounts().get("semoisConfiguration"));
        verify(semoisConfigurationRepository, never()).save(any());
        verify(semoisConfigurationRepository, never()).delete(any());
    }

    @Test
    void should_reassignSemoisConfiguration_whenTargetHasNone() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        com.kobe.warehouse.domain.SemoisConfiguration cfg = new com.kobe.warehouse.domain.SemoisConfiguration();
        cfg.setId(30);
        when(semoisConfigurationRepository.findByProduitId(2)).thenReturn(Optional.of(cfg));
        when(semoisConfigurationRepository.findByProduitId(1)).thenReturn(Optional.empty());

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, cfg.getProduit());
        verify(semoisConfigurationRepository).save(cfg);
        assertEquals(1, result.entityCounts().get("semoisConfiguration"));
    }

    @Test
    void should_keepTargetSemoisConfiguration_whenBothHaveOne() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        com.kobe.warehouse.domain.SemoisConfiguration sourceCfg = new com.kobe.warehouse.domain.SemoisConfiguration();
        sourceCfg.setId(30);
        com.kobe.warehouse.domain.SemoisConfiguration targetCfg = new com.kobe.warehouse.domain.SemoisConfiguration();
        targetCfg.setId(31);
        when(semoisConfigurationRepository.findByProduitId(2)).thenReturn(Optional.of(sourceCfg));
        when(semoisConfigurationRepository.findByProduitId(1)).thenReturn(Optional.of(targetCfg));

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        verify(semoisConfigurationRepository).delete(sourceCfg);
        verify(semoisConfigurationRepository, never()).save(any());
        assertEquals(1, result.entityCounts().get("semoisConfiguration"));
    }

    // ── 18. Ventes mensuelles agrégées ───────────────────────────────────────

    private com.kobe.warehouse.domain.VentesMensuellesAgregees vma(String mois, int qte, int ca, int nb) {
        com.kobe.warehouse.domain.VentesMensuellesAgregees v = new com.kobe.warehouse.domain.VentesMensuellesAgregees();
        v.setAnneeMois(mois);
        v.setQuantiteVendue(qte);
        v.setMontantCa(ca);
        v.setNombreVentes(nb);
        return v;
    }

    @Test
    void should_reassignMonthlyAggregate_whenTargetHasNoRowForThatMonth() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        com.kobe.warehouse.domain.VentesMensuellesAgregees row = vma("2026-03", 10, 1000, 5);
        when(ventesMensuellesAgregeesRepository.findAllByProduitIdIn(List.of(2))).thenReturn(List.of(row));
        when(ventesMensuellesAgregeesRepository.findByProduitIdAndAnneeMois(1, "2026-03")).thenReturn(Optional.empty());

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, row.getProduit());
        verify(ventesMensuellesAgregeesRepository).save(row);
        assertEquals(1, result.entityCounts().get("ventesMensuellesAgregees"));
    }

    @Test
    void should_sumMonthlyAggregates_whenTargetAlreadyHasThatMonth() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        com.kobe.warehouse.domain.VentesMensuellesAgregees sourceRow = vma("2026-03", 10, 1000, 5);
        sourceRow.setIsFrozen(Boolean.TRUE);
        sourceRow.setEstRuptureFournisseur(Boolean.FALSE);
        com.kobe.warehouse.domain.VentesMensuellesAgregees targetRow = vma("2026-03", 4, 400, 2);
        targetRow.setIsFrozen(Boolean.FALSE);
        targetRow.setEstRuptureFournisseur(Boolean.TRUE);
        when(ventesMensuellesAgregeesRepository.findAllByProduitIdIn(List.of(2))).thenReturn(List.of(sourceRow));
        when(ventesMensuellesAgregeesRepository.findByProduitIdAndAnneeMois(1, "2026-03")).thenReturn(Optional.of(targetRow));

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(14, targetRow.getQuantiteVendue());
        assertEquals(1400, targetRow.getMontantCa());
        assertEquals(7, targetRow.getNombreVentes());
        // les deux drapeaux sont des OU logiques : un mois figé ou en rupture d'un côté le reste
        assertTrue(targetRow.getIsFrozen());
        assertTrue(targetRow.getEstRuptureFournisseur());
        verify(ventesMensuellesAgregeesRepository).save(targetRow);
        verify(ventesMensuellesAgregeesRepository).delete(sourceRow);
    }

    @Test
    void should_keepFlagsFalse_whenNeitherMonthlyAggregateIsFlagged() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);
        com.kobe.warehouse.domain.VentesMensuellesAgregees sourceRow = vma("2026-03", 1, 100, 1);
        sourceRow.setIsFrozen(Boolean.FALSE);
        sourceRow.setEstRuptureFournisseur(Boolean.FALSE);
        com.kobe.warehouse.domain.VentesMensuellesAgregees targetRow = vma("2026-03", 1, 100, 1);
        targetRow.setIsFrozen(Boolean.FALSE);
        targetRow.setEstRuptureFournisseur(Boolean.FALSE);
        when(ventesMensuellesAgregeesRepository.findAllByProduitIdIn(List.of(2))).thenReturn(List.of(sourceRow));
        when(ventesMensuellesAgregeesRepository.findByProduitIdAndAnneeMois(1, "2026-03")).thenReturn(Optional.of(targetRow));

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(Boolean.FALSE, targetRow.getIsFrozen());
        assertEquals(Boolean.FALSE, targetRow.getEstRuptureFournisseur());
    }

    // ── 19. Lignes d'inventaire ──────────────────────────────────────────────

    private com.kobe.warehouse.domain.StoreInventoryLine inventoryLine(long inventoryId, Storage storage) {
        com.kobe.warehouse.domain.StoreInventory inventory = new com.kobe.warehouse.domain.StoreInventory();
        inventory.setId(Long.valueOf(inventoryId));
        com.kobe.warehouse.domain.StoreInventoryLine line = new com.kobe.warehouse.domain.StoreInventoryLine();
        line.setId(inventoryId * 10);
        line.setStoreInventory(inventory);
        line.setStorage(storage);
        return line;
    }

    @Test
    void should_reassignInventoryLine_whenNoCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Storage storage = new Storage();
        storage.setId(10);
        com.kobe.warehouse.domain.StoreInventoryLine line = inventoryLine(5L, storage);
        when(storeInventoryLineRepository.findAllByProduitId(2)).thenReturn(List.of(line));
        when(storeInventoryLineRepository.existsByProduitIdAndStoreInventoryIdAndStorageId(1, 5L, 10)).thenReturn(false);

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, line.getProduit());
        verify(storeInventoryLineRepository).save(line);
        assertEquals(1, result.entityCounts().get("storeInventoryLine"));
    }

    @Test
    void should_leaveInventoryLineUntouched_whenSameInventoryAndStorageCollision() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        Storage storage = new Storage();
        storage.setId(10);
        com.kobe.warehouse.domain.StoreInventoryLine line = inventoryLine(5L, storage);
        when(storeInventoryLineRepository.findAllByProduitId(2)).thenReturn(List.of(line));
        when(storeInventoryLineRepository.existsByProduitIdAndStoreInventoryIdAndStorageId(1, 5L, 10)).thenReturn(true);

        ProduitMergeResultDTO result = service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        verify(storeInventoryLineRepository, never()).save(any());
        assertEquals(0, result.entityCounts().get("storeInventoryLine"));
    }

    @Test
    void should_tolerateInventoryLineWithoutStorage() {
        Produit target = produit(1, "Target");
        Produit source = produit(2, "Source");
        stubTargetAndSource(target, source);

        com.kobe.warehouse.domain.StoreInventoryLine line = inventoryLine(5L, null);
        when(storeInventoryLineRepository.findAllByProduitId(2)).thenReturn(List.of(line));
        when(storeInventoryLineRepository.existsByProduitIdAndStoreInventoryIdAndStorageId(1, 5L, null)).thenReturn(false);

        service.merge(new ProduitMergeRequestDTO(1, List.of(2), List.of()));

        assertEquals(target, line.getProduit());
    }
}
