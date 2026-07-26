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
}
