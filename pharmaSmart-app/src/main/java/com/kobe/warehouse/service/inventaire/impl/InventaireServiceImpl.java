package com.kobe.warehouse.service.inventaire.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.inventaire.InventaireService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.CreateInventoryFromProduitIds;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLotGroupExport;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.projection.IdProjection;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.errors.InventoryException;
import com.kobe.warehouse.service.mobile.dto.RayonRecord;
import com.kobe.warehouse.service.report.InventoryReportReportService;
import com.kobe.warehouse.service.inventaire.InventaireQueryService;
import com.kobe.warehouse.service.inventaire.InventoryStockService;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.Rayon_;
import com.kobe.warehouse.domain.Storage_;
import com.kobe.warehouse.domain.StoreInventory_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class InventaireServiceImpl implements InventaireService {

    private final Logger log = LoggerFactory.getLogger(InventaireServiceImpl.class);

    private final UserService userService;
    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreInventoryLineRepository storeInventoryLineRepository;
    private final StorageService storageService;
    private final RayonRepository rayonRepository;
    private final InventoryReportReportService inventoryReportService;
    private final EntityManager em;
    private final InventaireQueryService inventaireQueryService;

    public InventaireServiceImpl(
        UserService userService,
        StoreInventoryRepository storeInventoryRepository,
        StoreInventoryLineRepository storeInventoryLineRepository,
        StorageService storageService,
        RayonRepository rayonRepository,
        InventoryReportReportService inventoryReportService,
        EntityManager em,
        InventaireQueryService inventaireQueryService
    ) {
        this.userService = userService;
        this.storeInventoryRepository = storeInventoryRepository;
        this.storeInventoryLineRepository = storeInventoryLineRepository;
        this.storageService = storageService;
        this.rayonRepository = rayonRepository;
        this.inventoryReportService = inventoryReportService;
        this.em = em;
        this.inventaireQueryService = inventaireQueryService;
    }

    @Override
    public byte[] printToPdf(StoreInventoryExportRecord filterRecord) {
        InventoryExportWrapper wrapper = inventaireQueryService.exportInventory(filterRecord);
        if (wrapper == null) {
            return null;
        }
        if (filterRecord.isGestionLot()) {
            List<StoreInventoryLotGroupExport> lotGroups =
                inventaireQueryService.getLotGroupsForExport(filterRecord.filterRecord().storeInventoryId());
            return this.inventoryReportService.printLotToPdf(wrapper, lotGroups);
        }
        return this.inventoryReportService.printToPdf(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreInventoryGroupExport> getStoreInventoryToExport(
        StoreInventoryExportRecord filterRecord) {
        return inventaireQueryService.getStoreInventoryToExport(filterRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreInventoryLineDTO> getAllItems(Long storeInventoryId) {
        return storeInventoryLineRepository.findAllByStoreInventoryId(storeInventoryId).stream()
            .map(StoreInventoryLineDTO::new).toList();
    }

    @Override
    public void synchronizeStoreInventoryLine(List<StoreInventoryLineDTO> storeInventoryLines) {
        if (!CollectionUtils.isEmpty(storeInventoryLines)) {
            storeInventoryLines.forEach(storeInventoryLineDTO -> {
                StoreInventoryLine storeInventoryLine = this.storeInventoryLineRepository.getReferenceById(
                    storeInventoryLineDTO.getId());
                updateStoreInventoryLine(storeInventoryLineDTO, storeInventoryLine);
                this.storeInventoryLineRepository.saveAndFlush(storeInventoryLine);
            });
        }
    }

    private void updateStoreInventoryLine(StoreInventoryLineDTO storeInventoryLineDTO,
        StoreInventoryLine storeInventoryLine) {
        storeInventoryLine.applyCount(
            Objects.requireNonNullElse(storeInventoryLineDTO.getQuantityInit(), 0),
            Objects.requireNonNullElse(storeInventoryLineDTO.getQuantityOnHand(), 0),
            userService.getUser()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public Page<StoreInventoryDTO> storeInventoryList(
        StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable) {
        List<StoreInventory> inventories = this.fetchInventories(storeInventoryFilterRecord,
            pageable);
        Set<Long> countedIds = fetchIdsWithCountedLines(inventories);

        List<StoreInventoryDTO> content = inventories
            .stream()
            .map(inventory ->
                StoreInventoryDTO.builder(inventory)
                    .statut(resolveStatut(inventory, countedIds.contains(inventory.getId())))
                    .build()
            )
            .toList();

        return new PageImpl<>(content, pageable,
            this.fetchInventoriesCount(storeInventoryFilterRecord));
    }

    private Set<Long> fetchIdsWithCountedLines(List<StoreInventory> inventories) {
        if (CollectionUtils.isEmpty(inventories)) {
            return Set.of();
        }
        return this.storeInventoryLineRepository.findStoreInventoryIdsWithCountedLines(
            inventories.stream().map(StoreInventory::getId).toList());
    }

    /**
     * Un inventaire dont au moins une ligne est comptée est en cours, même si son statut en
     * base est resté {@code CREATE} : la saisie d'une ligne ne repasse pas par un service qui
     * ferait progresser l'entête. Un inventaire clôturé n'est jamais réévalué.
     */
    private InventoryStatut resolveStatut(StoreInventory inventory, boolean hasCountedLine) {
        InventoryStatut statut = inventory.getStatut();
        if (statut != InventoryStatut.CLOSED && hasCountedLine) {
            return InventoryStatut.PROCESSING;
        }
        return statut;
    }

    @Override
    public Optional<StoreInventoryDTO> getProccessingStoreInventory(Long id) {
        StoreInventory storeInventory = this.storeInventoryRepository.getReferenceById(id);
        if (storeInventory.getStatut() == InventoryStatut.CLOSED) {
            return Optional.empty();
        }
        return Optional.of(new StoreInventoryDTO(storeInventory));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreInventoryDTO> fetchActifs() {
        return this.storeInventoryRepository.findActif().stream().map(StoreInventoryDTO::new)
            .toList();
    }

    @Override
    public List<RayonRecord> fetchRayonsByStoreInventoryId(Long storeInventoryId) {
        return this.storeInventoryLineRepository.findAllRayons(storeInventoryId)
            .stream()
            .map(rayon -> {
                Storage storage = rayon.getStorage();
                return new RayonRecord(
                    rayon.getId(),
                    rayon.getCode(),
                    rayon.getLibelle(),
                    storage.getId(),
                    storage.getName(),
                    storeInventoryId
                );
            })
            .toList();
    }

    @Override
    public InventoryExportWrapper exportInventory(StoreInventoryExportRecord inventoryExportRecord) {
        return inventaireQueryService.exportInventory(inventoryExportRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreInventoryLotGroupExport> getLotGroupsForExport(Long inventoryId) {
        return inventaireQueryService.getLotGroupsForExport(inventoryId);
    }

    @Override
    public int createInventoryFromFrom(CreateInventoryFromProduitIds createInventoryFromProduitIds)
        throws InventoryException {
        StoreInventory storeInventory = buildStoreInventory(
            createInventoryFromProduitIds.storeInventoryRecord());
        storeInventory = this.storeInventoryRepository.save(storeInventory);
        StoreInventory finalStoreInventory = storeInventory;
        createInventoryFromProduitIds.produitIds()
            .forEach(id -> buildStoreInventoryLineFromProduit(
                em.getReference(Produit.class, id), finalStoreInventory));
        storeInventoryLineRepository.saveAll(storeInventory.getStoreInventoryLines());
        return storeInventory.getStoreInventoryLines().size();
    }

    private StoreInventory buildStoreInventory(StoreInventoryRecord storeInventoryRecord) {
        StoreInventory storeInventory = new StoreInventory();
        storeInventory.createdAt(LocalDateTime.now());
        storeInventory.updatedAt(storeInventory.getCreatedAt());
        storeInventory.setUser(userService.getUser());
        storeInventory.setInventoryCategory(
            InventoryCategory.valueOf(storeInventoryRecord.inventoryCategory()));
        storeInventory.setInventoryAmountAfter(0L);
        storeInventory.setInventoryAmountBegin(0L);
        storeInventory.setInventoryValueCostBegin(0L);
        storeInventory.setInventoryValueCostAfter(0L);
        storeInventory.setDescription(storeInventoryRecord.description());
        storeInventory.setGapAmount(0);
        storeInventory.setGapCost(0);
        if (Objects.isNull(storeInventoryRecord.storage())) {
            storeInventory.setStorage(this.storageService.getDefaultConnectedUserMainStorage());
        }

        if (storeInventory.getInventoryCategory() == InventoryCategory.RAYON) {
            Rayon rayon = this.rayonRepository.getReferenceById(storeInventoryRecord.rayon());
            storeInventory.setRayon(rayon);
            storeInventory.setStorage(rayon.getStorage());
        }
        if (Objects.isNull(storeInventory.getStorage())) {
            storeInventory.setStorage(this.storageService.getOne(storeInventoryRecord.storage()));
        }
        return storeInventory;
    }

    /**
     * Une ligne à compter, posée comme le font les requêtes d'insertion du service de création.
     *
     * <p>L'emplacement manquait, alors que {@code storage_id} est {@code NOT NULL} : la
     * création d'un inventaire depuis une sélection de produits échouait au flush. C'est aussi lui
     * qui rattache la ligne à la grille de saisie.
     *
     * <p>{@code quantityInit} reste vide : le stock théorique est lu au comptage, par
     * {@code InventoryStockService}. Le figer à zéro ferait passer chaque comptage pour un écart
     * positif du total compté.
     */
    private void buildStoreInventoryLineFromProduit(Produit produit,
        StoreInventory storeInventory) {
        StoreInventoryLine storeInventoryLine = new StoreInventoryLine();
        storeInventoryLine.setStoreInventory(storeInventory);
        storeInventoryLine.setProduit(produit);
        storeInventoryLine.setStorage(storeInventory.getStorage());
        storeInventoryLine.setUpdated(false);
        storeInventoryLine.setUpdatedAt(LocalDateTime.now());
        storeInventory.getStoreInventoryLines().add(storeInventoryLine);
    }

    /**
     * Un inventaire clôturé a déjà été appliqué au stock et a laissé une ligne dans
     * {@code historique_inventaire}. Le supprimer effacerait la justification d'un ajustement de
     * stock tout en laissant son historique orphelin : rien n'expliquerait plus l'écart.
     */
    @Override
    public void remove(Long id) {
        refuserSiCloture(storeInventoryRepository.getReferenceById(id),
            "Un inventaire clôturé ne peut pas être supprimé", "inventoryClosedCannotBeDeleted");
        storeInventoryLineRepository.deleteAllByStoreInventoryId(id);
        storeInventoryRepository.deleteById(id);
    }

    /**
     * Garde commune aux gestes qui modifient un inventaire. Le statut fait foi : une fois
     * {@code CLOSED}, l'inventaire est un document comptable, plus un brouillon.
     */
    private void refuserSiCloture(StoreInventory inventory, String message, String cleErreur) {
        if (inventory.getStatut() == InventoryStatut.CLOSED) {
            throw new BadRequestAlertException(message, cleErreur);
        }
    }

    @Override
    public StoreInventoryLineRecord updateQuantityOnHand(
        StoreInventoryLineDTO storeInventoryLineDTO) {
        StoreInventoryLine storeInventoryLine = storeInventoryLineRepository.getReferenceById(
            storeInventoryLineDTO.getId());
        // Recompter une ligne après la clôture ferait diverger l'inventaire de l'historique qu'il
        // a produit, sans que le stock bouge : la ligne ne dirait plus ce qui a été appliqué.
        refuserSiCloture(storeInventoryLine.getStoreInventory(),
            "Cet inventaire est clôturé : ses lignes ne peuvent plus être comptées",
            "inventoryClosedCannotBeCounted");
        // Verrou optimiste : la ligne a-t-elle été recomptée depuis la lecture du client ?
        // Version nulle côté client = pas de contrôle (appelants legacy).
        if (storeInventoryLineDTO.getVersion() != null
            && storeInventoryLine.getVersion() != null
            && !storeInventoryLineDTO.getVersion().equals(storeInventoryLine.getVersion())) {
            throw new OptimisticLockException(
                "Cette ligne a été comptée par un autre opérateur entre-temps");
        }
        updateStoreInventoryLine(storeInventoryLineDTO, storeInventoryLine);

        storeInventoryLine = storeInventoryLineRepository.saveAndFlush(storeInventoryLine);
        int lotCount =
            storeInventoryLine.getLots() != null ? storeInventoryLine.getLots().size() : 0;
        return new StoreInventoryLineRecord(
            storeInventoryLineDTO.getProduitId(),
            storeInventoryLineDTO.getProduitCip(),
            storeInventoryLineDTO.getProduitEan(),
            storeInventoryLineDTO.getProduitLibelle(),
            storeInventoryLine.getId(),
            storeInventoryLine.getGap(),
            storeInventoryLine.getQuantityOnHand(),
            storeInventoryLine.getQuantityInit(),
            storeInventoryLine.getUpdated(),
            storeInventoryLine.getInventoryValueCost(),
            storeInventoryLine.getLastUnitPrice(),
            storeInventoryLine.getStorage() != null ? storeInventoryLine.getStorage().getId()
                : null,
            null,
            lotCount, null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoreInventoryDTO> getStoreInventory(Long id) {
        return storeInventoryRepository.findById(id).map(StoreInventoryDTO::new);
    }

    private List<Predicate> predicates(
        StoreInventoryFilterRecord storeInventoryFilterRecord,
        CriteriaBuilder cb,
        Root<StoreInventory> root
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (!CollectionUtils.isEmpty(storeInventoryFilterRecord.inventoryCategories())) {
            In<InventoryCategory> inventoryCategoryIn = cb.in(
                root.get(StoreInventory_.inventoryCategory));
            storeInventoryFilterRecord.inventoryCategories().forEach(inventoryCategoryIn::value);
            predicates.add(inventoryCategoryIn);
        }

        if (!CollectionUtils.isEmpty(storeInventoryFilterRecord.statuts())) {
            In<InventoryStatut> inventoryStatutIn = cb.in(root.get(StoreInventory_.statut));
            storeInventoryFilterRecord.statuts().forEach(inventoryStatutIn::value);
            predicates.add(inventoryStatutIn);
        }
        if (Objects.nonNull(storeInventoryFilterRecord.rayonId())) {
            predicates.add(cb.equal(root.get(StoreInventory_.rayon).get(Rayon_.id),
                storeInventoryFilterRecord.rayonId()));
        }
        if (Objects.nonNull(storeInventoryFilterRecord.storageId())) {
            predicates.add(cb.equal(root.get(StoreInventory_.storage).get(Storage_.id),
                storeInventoryFilterRecord.storageId()));
        }
        if (Objects.nonNull(storeInventoryFilterRecord.userId())) {
            predicates.add(cb.equal(root.get(StoreInventory_.user).get(AppUser_.id),
                storeInventoryFilterRecord.userId()));
        }

        return predicates;
    }

    private List<StoreInventory> fetchInventories(
        StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StoreInventory> cq = cb.createQuery(StoreInventory.class);
        Root<StoreInventory> root = cq.from(StoreInventory.class);
        cq.select(root).orderBy(cb.desc(root.get(StoreInventory_.updatedAt)));
        List<Predicate> predicates = predicates(storeInventoryFilterRecord, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<StoreInventory> q = em.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return q.getResultList();
    }

    public long fetchInventoriesCount(StoreInventoryFilterRecord storeInventoryFilterRecord) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<StoreInventory> root = cq.from(StoreInventory.class);
        cq.select(cb.count(root));
        List<Predicate> predicates = predicates(storeInventoryFilterRecord, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Long> q = em.createQuery(cq);
        Long v = q.getSingleResult();
        return v != null ? v : 0;
    }

    // @EventListener(ApplicationReadyEvent.class)
    private void updateAll() {
        this.storeInventoryLineRepository.findAllByStoreInventoryId(1L).forEach(storeInventory -> {
            StoreInventoryLineDTO dto = new StoreInventoryLineDTO();
            dto.setId(storeInventory.getId());
            dto.setQuantitySold(0);
            dto.setQuantityInit(0);
            dto.setProduitId(0);
            dto.setQuantityOnHand(5);
            updateQuantityOnHand(dto);
        });
    }

    // @EventListener(ApplicationReadyEvent.class)
    public void clean() {
        List<IdProjection> ids = this.storeInventoryRepository.findByStatutEquals(
            LocalDateTime.now().minusMonths(4));
        if (!CollectionUtils.isEmpty(ids)) {
            ids.forEach(idProjection -> {
                this.storeInventoryLineRepository.deleteAllByStoreInventoryId(
                    idProjection.getId().longValue());
                this.storeInventoryRepository.deleteById(idProjection.getId().longValue());
            });
        }
    }
}
