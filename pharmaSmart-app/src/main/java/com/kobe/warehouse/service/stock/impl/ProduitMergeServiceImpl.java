package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.Substitut;
import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TransactionType;
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
import com.kobe.warehouse.service.dto.produit.merge.LotConflictDTO;
import com.kobe.warehouse.service.dto.produit.merge.LotResolutionDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergePreviewDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeRequestDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeResultDTO;
import com.kobe.warehouse.service.dto.produit.merge.StockConflictDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.stock.ProduitMergeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fusionne des produits en doublon du catalogue. Traite les entités liées dans l'ordre
 * feuilles -&gt; racine pour ne jamais violer une contrainte UNIQUE, puis désactive les
 * produits source (jamais de suppression physique).
 */
@Service
@Transactional
public class ProduitMergeServiceImpl implements ProduitMergeService {

    private static final String ENTITY_NAME = "produit";

    private final ProduitRepository produitRepository;
    private final StockProduitRepository stockProduitRepository;
    private final LotRepository lotRepository;
    private final PrixReferenceRepository prixReferenceRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final RayonProduitRepository rayonProduitRepository;
    private final SalesLineRepository salesLineRepository;
    private final SemoisConfigurationRepository semoisConfigurationRepository;
    private final VentesMensuellesAgregeesRepository ventesMensuellesAgregeesRepository;
    private final StoreInventoryLineRepository storeInventoryLineRepository;
    private final SubstitutRepository substitutRepository;
    private final LogsService logsService;

    @PersistenceContext
    private EntityManager entityManager;

    /** Entités simples réaffectées par UPDATE JPQL bulk : aucune contrainte UNIQUE portant sur produit_id. */
    private static final List<String> SIMPLE_ENTITIES = List.of(
        "AvoirClient",
        "ClassificationCriticiteLog",
        "Decondition",
        "InventoryTransaction",
        "ProduitPerime",
        "RetourClientLine",
        "RetourDepotItem",
        "Rupture"
    );

    public ProduitMergeServiceImpl(
        ProduitRepository produitRepository,
        StockProduitRepository stockProduitRepository,
        LotRepository lotRepository,
        PrixReferenceRepository prixReferenceRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        RayonProduitRepository rayonProduitRepository,
        SalesLineRepository salesLineRepository,
        SemoisConfigurationRepository semoisConfigurationRepository,
        VentesMensuellesAgregeesRepository ventesMensuellesAgregeesRepository,
        StoreInventoryLineRepository storeInventoryLineRepository,
        SubstitutRepository substitutRepository,
        LogsService logsService
    ) {
        this.produitRepository = produitRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.lotRepository = lotRepository;
        this.prixReferenceRepository = prixReferenceRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.rayonProduitRepository = rayonProduitRepository;
        this.salesLineRepository = salesLineRepository;
        this.semoisConfigurationRepository = semoisConfigurationRepository;
        this.ventesMensuellesAgregeesRepository = ventesMensuellesAgregeesRepository;
        this.storeInventoryLineRepository = storeInventoryLineRepository;
        this.substitutRepository = substitutRepository;
        this.logsService = logsService;
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitMergePreviewDTO preview(Integer targetId, List<Integer> sourceIds) {
        Produit target = getProduitOrThrow(targetId);
        List<Integer> candidates = distinctSourceIds(targetId, sourceIds);

        List<Integer> rejected = new ArrayList<>();
        Map<String, String> rejectionReasons = new LinkedHashMap<>();
        List<Integer> valid = new ArrayList<>();

        List<Produit> targetChildren = produitRepository.findAllByParentId(targetId);
        boolean targetHasTooManyChildren = targetChildren.size() > 1;

        for (Integer sourceId : candidates) {
            Optional<Produit> source = produitRepository.findById(sourceId);
            if (source.isEmpty()) {
                rejected.add(sourceId);
                rejectionReasons.put(sourceId.toString(), "Produit introuvable");
            } else if (targetHasTooManyChildren) {
                rejected.add(sourceId);
                rejectionReasons.put(
                    sourceId.toString(),
                    "Le produit cible a plusieurs produits détail (déconditionnés) rattachés : cas non géré automatiquement, à corriger avant fusion"
                );
            } else if (produitRepository.findAllByParentId(sourceId).size() > 1) {
                rejected.add(sourceId);
                rejectionReasons.put(
                    sourceId.toString(),
                    "Le produit a plusieurs produits détail (déconditionnés) rattachés : cas non géré automatiquement, à corriger avant fusion"
                );
            } else {
                valid.add(sourceId);
            }
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        List<LotConflictDTO> lotConflicts = new ArrayList<>();
        List<StockConflictDTO> stockConflicts = new ArrayList<>();
        Map<String, Lot> targetLotsByNumLot = lotsByNumLot(target.getId());
        Map<Integer, StockProduit> targetStocksByStorage = stocksByStorage(target.getId());
        boolean targetHasChild = targetChildren.size() == 1;
        for (Integer sourceId : valid) {
            counts.merge("stockProduit", (int) stockProduitRepository.countByProduitId(sourceId), Integer::sum);
            counts.merge("optionPrixProduit", (int) prixReferenceRepository.countByProduitId(sourceId), Integer::sum);
            counts.merge("fournisseurProduit", (int) fournisseurProduitRepository.countByProduitId(sourceId), Integer::sum);
            counts.merge("rayonProduit", (int) rayonProduitRepository.countByProduitId(sourceId), Integer::sum);
            counts.merge("salesLine", (int) salesLineRepository.countByProduitId(sourceId), Integer::sum);
            counts.merge(
                "substitut",
                (int) (substitutRepository.countByProduitId(sourceId) + substitutRepository.countBySubstitutId(sourceId)),
                Integer::sum
            );

            List<Lot> sourceLots = lotRepository.findByProduitId(sourceId);
            counts.merge("lot", sourceLots.size(), Integer::sum);
            lotConflicts.addAll(detectLotConflicts(sourceLots, targetLotsByNumLot));

            stockConflicts.addAll(detectStockConflicts(stockProduitRepository.findAllByProduitId(sourceId), targetStocksByStorage));

            List<Produit> sourceChildren = produitRepository.findAllByParentId(sourceId);
            if (!sourceChildren.isEmpty()) {
                counts.merge(targetHasChild ? "produitDetailFusionne" : "produitDetailReparente", 1, Integer::sum);
            }
        }
        for (String entityName : SIMPLE_ENTITIES) {
            for (Integer sourceId : valid) {
                counts.merge(entityName, countByProduitId(entityName, sourceId), Integer::sum);
            }
        }

        return new ProduitMergePreviewDTO(targetId, valid, rejected, rejectionReasons, counts, lotConflicts, stockConflicts);
    }

    @Override
    public ProduitMergeResultDTO merge(ProduitMergeRequestDTO request) {
        if (request.targetId() == null || request.sourceIds() == null || request.sourceIds().isEmpty()) {
            throw new BadRequestAlertException("Cible et sources sont obligatoires", ENTITY_NAME, "mergemissingargs");
        }
        Produit target = getProduitOrThrow(request.targetId());
        List<Integer> sourceIds = distinctSourceIds(request.targetId(), request.sourceIds());
        if (sourceIds.isEmpty()) {
            throw new BadRequestAlertException("Aucun produit source valide à fusionner", ENTITY_NAME, "mergenoop");
        }

        Map<Integer, LotConflictAction> resolutions = new HashMap<>();
        if (request.lotResolutions() != null) {
            for (LotResolutionDTO r : request.lotResolutions()) {
                resolutions.put(r.lotId(), r.action());
            }
        }

        List<Produit> targetChildren = produitRepository.findAllByParentId(target.getId());
        if (targetChildren.size() > 1) {
            throw new BadRequestAlertException(
                "Le produit cible a plusieurs produits détail (déconditionnés) rattachés : cas non géré automatiquement",
                ENTITY_NAME,
                "mergetoomanychildren"
            );
        }

        List<Produit> sources = new ArrayList<>();
        Map<String, Lot> targetLotsByNumLot = lotsByNumLot(target.getId());
        for (Integer sourceId : sourceIds) {
            Produit source = getProduitOrThrow(sourceId);
            if (produitRepository.findAllByParentId(sourceId).size() > 1) {
                throw new BadRequestAlertException(
                    "Le produit " + source.getLibelle() + " a plusieurs produits détail (déconditionnés) rattachés : cas non géré automatiquement",
                    ENTITY_NAME,
                    "mergetoomanychildren"
                );
            }
            List<LotConflictDTO> conflicts = detectLotConflicts(lotRepository.findByProduitId(sourceId), targetLotsByNumLot);
            for (LotConflictDTO conflict : conflicts) {
                if (!resolutions.containsKey(conflict.sourceLotId())) {
                    throw new BadRequestAlertException(
                        "Conflit de lot non résolu (num_lot=" + conflict.numLot() + ") : indiquez fusionner ou supprimer",
                        ENTITY_NAME,
                        "mergelotconflict"
                    );
                }
            }
            sources.add(source);
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        List<StockConflictDTO> stockConflicts = new ArrayList<>();
        for (Produit source : sources) {
            mergeDeconditionnes(target, source, counts);
            counts.merge("stockProduit", mergeStockProduits(target, source, stockConflicts), Integer::sum);
            counts.merge("lot", mergeLots(target, source, resolutions), Integer::sum);
            counts.merge("optionPrixProduit", mergeOptionPrixProduits(target, source), Integer::sum);
            counts.merge("fournisseurProduit", mergeFournisseurProduits(target, source), Integer::sum);
            counts.merge("rayonProduit", mergeRayonProduits(target, source), Integer::sum);
            counts.merge("salesLine", mergeSalesLines(target, source), Integer::sum);
            counts.merge("substitut", mergeSubstituts(target, source), Integer::sum);
            counts.merge("semoisConfiguration", mergeSemoisConfiguration(target, source), Integer::sum);
            counts.merge("ventesMensuellesAgregees", mergeVentesMensuellesAgregees(target, source), Integer::sum);
            counts.merge("storeInventoryLine", mergeStoreInventoryLines(target, source), Integer::sum);
            for (String entityName : SIMPLE_ENTITIES) {
                counts.merge(entityName, bulkReassign(entityName, target, source), Integer::sum);
            }

            source.setStatus(Status.DISABLE);
            source.setUpdatedAt(java.time.LocalDateTime.now());
            produitRepository.save(source);
            evictProduitCache(source.getId());
        }
        evictProduitCache(target.getId());

        List<Integer> mergedIds = sources.stream().map(Produit::getId).toList();
        String comments =
            "Fusion des produits " +
            mergedIds +
            " dans le produit " +
            target.getId() +
            " (" +
            target.getLibelle() +
            ") : " +
            counts +
            (stockConflicts.isEmpty() ? "" : " — ajustement de stock manuel requis sur " + stockConflicts.size() + " emplacement(s)");
        logsService.create(TransactionType.MERGE_PRODUCT, comments, target.getId().toString());

        return new ProduitMergeResultDTO(target.getId(), mergedIds, counts, stockConflicts);
    }

    // ------------------------------------------------------------------
    // Validation / helpers
    // ------------------------------------------------------------------

    private Produit getProduitOrThrow(Integer id) {
        return produitRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Produit introuvable : " + id, ENTITY_NAME, "idnotfound"));
    }

    private List<Integer> distinctSourceIds(Integer targetId, List<Integer> sourceIds) {
        Set<Integer> distinct = new LinkedHashSet<>(sourceIds);
        distinct.remove(targetId);
        return new ArrayList<>(distinct);
    }

    private Map<String, Lot> lotsByNumLot(Integer produitId) {
        Map<String, Lot> byNumLot = new HashMap<>();
        for (Lot lot : lotRepository.findByProduitId(produitId)) {
            byNumLot.put(lot.getNumLot(), lot);
        }
        return byNumLot;
    }

    private List<LotConflictDTO> detectLotConflicts(List<Lot> sourceLots, Map<String, Lot> targetLotsByNumLot) {
        if (sourceLots.isEmpty()) {
            return List.of();
        }
        List<LotConflictDTO> conflicts = new ArrayList<>();
        for (Lot sourceLot : sourceLots) {
            Lot targetLot = targetLotsByNumLot.get(sourceLot.getNumLot());
            if (targetLot != null) {
                conflicts.add(
                    new LotConflictDTO(
                        sourceLot.getNumLot(),
                        sourceLot.getId(),
                        sourceLot.getQuantity(),
                        sourceLot.getExpiryDate(),
                        targetLot.getId(),
                        targetLot.getQuantity(),
                        targetLot.getExpiryDate()
                    )
                );
            }
        }
        return conflicts;
    }

    private Map<Integer, StockProduit> stocksByStorage(Integer produitId) {
        Map<Integer, StockProduit> byStorage = new HashMap<>();
        for (StockProduit stock : stockProduitRepository.findAllByProduitId(produitId)) {
            byStorage.put(stock.getStorage().getId(), stock);
        }
        return byStorage;
    }

    /**
     * Un {@code StockProduit} du doublon partageant le même {@code storage} qu'une ligne du
     * produit cible n'est jamais fusionné automatiquement (cf. {@link StockConflictDTO}) : on se
     * contente de signaler l'écart, à charge pour l'utilisateur de faire un ajustement manuel.
     */
    private List<StockConflictDTO> detectStockConflicts(List<StockProduit> sourceStocks, Map<Integer, StockProduit> targetStocksByStorage) {
        if (sourceStocks.isEmpty()) {
            return List.of();
        }
        List<StockConflictDTO> conflicts = new ArrayList<>();
        for (StockProduit sourceStock : sourceStocks) {
            StockProduit targetStock = targetStocksByStorage.get(sourceStock.getStorage().getId());
            if (targetStock != null) {
                conflicts.add(
                    new StockConflictDTO(
                        sourceStock.getStorage().getId(),
                        sourceStock.getStorage().getName(),
                        sourceStock.getQtyStock(),
                        sourceStock.getQtyVirtual(),
                        sourceStock.getQtyUG(),
                        targetStock.getQtyStock(),
                        targetStock.getQtyVirtual(),
                        targetStock.getQtyUG()
                    )
                );
            }
        }
        return conflicts;
    }

    private int countByProduitId(String entityName, Integer produitId) {
        Long count = entityManager
            .createQuery("SELECT COUNT(e) FROM " + entityName + " e WHERE e.produit.id = :produitId", Long.class)
            .setParameter("produitId", produitId)
            .getSingleResult();
        return count.intValue();
    }

    private int bulkReassign(String entityName, Produit target, Produit source) {
        return entityManager
            .createQuery("UPDATE " + entityName + " e SET e.produit = :target WHERE e.produit = :source")
            .setParameter("target", target)
            .setParameter("source", source)
            .executeUpdate();
    }

    /**
     * Une "boîte" (produit parent) peut avoir un produit détail (déconditionné) rattaché : ce
     * n'est plus un motif de rejet de la fusion. Trois cas :
     * <ul>
     *   <li>Le doublon n'a pas de détail : rien à faire.</li>
     *   <li>Seule la boîte source a un détail : il est simplement re-parenté vers la boîte
     *       cible (aucune ambiguïté, la cible n'en a pas).</li>
     *   <li>Les deux boîtes ont chacune un détail actif : fusionner les ventes est obligatoire
     *       (jamais une option) pour ne jamais laisser deux produits détail actifs sous la même
     *       boîte cible. Seules les ventes sont fusionnées — les déconditionnés ne sont jamais
     *       commandés, et le stock/les lots ne sont jamais fusionnés automatiquement (cf.
     *       {@code mergeStockProduits}) : un ajustement manuel des quantités de la boîte et du
     *       détail reste nécessaire si besoin. Le détail source est ensuite archivé (DISABLE),
     *       comme la boîte source.
     * </ul>
     */
    private void mergeDeconditionnes(Produit target, Produit source, Map<String, Integer> counts) {
        List<Produit> sourceChildren = produitRepository.findAllByParentId(source.getId());
        if (sourceChildren.isEmpty()) {
            return;
        }
        Produit sourceChild = sourceChildren.get(0);

        List<Produit> targetChildren = produitRepository.findAllByParentId(target.getId());
        if (targetChildren.isEmpty()) {
            sourceChild.setParent(target);
            produitRepository.save(sourceChild);
            counts.merge("produitDetailReparente", 1, Integer::sum);
            return;
        }

        Produit targetChild = targetChildren.get(0);
        counts.merge("salesLineDetail", mergeSalesLines(targetChild, sourceChild), Integer::sum);
        sourceChild.setStatus(Status.DISABLE);
        sourceChild.setUpdatedAt(java.time.LocalDateTime.now());
        produitRepository.save(sourceChild);
        evictProduitCache(sourceChild.getId());
        counts.merge("produitDetailFusionne", 1, Integer::sum);
    }

    private void evictProduitCache(Integer produitId) {
        org.hibernate.Cache cache = entityManager.unwrap(Session.class).getSessionFactory().getCache();
        cache.evictEntityData(Produit.class, produitId);
        cache.evictCollectionData("com.kobe.warehouse.domain.Produit.optionPrixProduit", produitId);
        cache.evictCollectionData("com.kobe.warehouse.domain.Produit.fournisseurProduits", produitId);
        cache.evictCollectionData("com.kobe.warehouse.domain.Produit.rayonProduits", produitId);
    }

    // ------------------------------------------------------------------
    // Par entité
    // ------------------------------------------------------------------

    /**
     * Ne fusionne JAMAIS les quantités automatiquement en cas de collision (même storage des
     * deux côtés) : une somme implicite modifierait le stock de la cible sans passer par le
     * mécanisme d'ajustement audité de l'application. La ligne cible reste inchangée ET la ligne
     * du doublon n'est ni réaffectée ni supprimée : elle reste rattachée au produit source
     * (désormais archivé) avec son historique d'{@code Ajustement} intact — repointer cet
     * historique vers la ligne cible fausserait sa lecture (stock_before/after ne correspondrait
     * plus au produit associé). Le conflit est ajouté à {@code stockConflicts} pour que
     * l'utilisateur soit averti qu'un ajustement manuel de stock reste nécessaire s'il souhaite
     * reporter cette quantité sur la cible.
     */
    private int mergeStockProduits(Produit target, Produit source, List<StockConflictDTO> stockConflicts) {
        List<StockProduit> sourceStocks = stockProduitRepository.findAllByProduitId(source.getId());
        for (StockProduit stock : sourceStocks) {
            Optional<StockProduit> existing = stockProduitRepository.findStockProduitByStorageIdAndProduitId(
                stock.getStorage().getId(),
                target.getId()
            );
            if (existing.isPresent()) {
                StockProduit kept = existing.get();
                stockConflicts.add(
                    new StockConflictDTO(
                        stock.getStorage().getId(),
                        stock.getStorage().getName(),
                        stock.getQtyStock(),
                        stock.getQtyVirtual(),
                        stock.getQtyUG(),
                        kept.getQtyStock(),
                        kept.getQtyVirtual(),
                        kept.getQtyUG()
                    )
                );
            } else {
                stock.setProduit(target);
                stockProduitRepository.save(stock);
            }
        }
        return sourceStocks.size();
    }

    private int mergeLots(Produit target, Produit source, Map<Integer, LotConflictAction> resolutions) {
        List<Lot> sourceLots = lotRepository.findByProduitId(source.getId());
        Map<String, Lot> targetByNumLot = new HashMap<>();
        for (Lot lot : lotRepository.findByProduitId(target.getId())) {
            targetByNumLot.put(lot.getNumLot(), lot);
        }
        for (Lot sourceLot : sourceLots) {
            Lot targetLot = targetByNumLot.get(sourceLot.getNumLot());
            if (targetLot != null) {
                LotConflictAction action = resolutions.get(sourceLot.getId());
                if (action == LotConflictAction.MERGE) {
                    targetLot.setQuantity(targetLot.getQuantity() + sourceLot.getQuantity());
                    targetLot.setCurrentQuantity(targetLot.getCurrentQuantity() + sourceLot.getCurrentQuantity());
                    targetLot.setFreeQty(targetLot.getFreeQty() + sourceLot.getFreeQty());
                    lotRepository.save(targetLot);
                }
                // Le lot source disparaît (fusionné ou supprimé) : son historique d'Ajustement
                // n'est jamais repointé vers le lot cible, ce qui laisserait croire à tort que
                // ces ajustements passés portaient sur le lot cible. Il est détaché (lot = null,
                // colonne nullable) plutôt que réécrit.
                entityManager
                    .createQuery("UPDATE Ajustement a SET a.lot = null WHERE a.lot = :dup")
                    .setParameter("dup", sourceLot)
                    .executeUpdate();
                lotRepository.delete(sourceLot);
            } else {
                sourceLot.setProduit(target);
                lotRepository.save(sourceLot);
                targetByNumLot.put(sourceLot.getNumLot(), sourceLot);
            }
        }
        return sourceLots.size();
    }

    private int mergeOptionPrixProduits(Produit target, Produit source) {
        List<OptionPrixProduit> sourceOptions = prixReferenceRepository.findAllByProduitId(source.getId());
        Map<String, OptionPrixProduit> targetByKey = new HashMap<>();
        for (OptionPrixProduit option : prixReferenceRepository.findAllByProduitId(target.getId())) {
            targetByKey.put(optionPrixKey(option), option);
        }
        for (OptionPrixProduit option : sourceOptions) {
            String key = optionPrixKey(option);
            if (targetByKey.containsKey(key)) {
                // Le produit cible a déjà une tarification pour ce tiers payant/type : elle prévaut.
                prixReferenceRepository.delete(option);
            } else {
                option.setProduit(target);
                prixReferenceRepository.save(option);
                targetByKey.put(key, option);
            }
        }
        return sourceOptions.size();
    }

    private String optionPrixKey(OptionPrixProduit option) {
        return option.getTiersPayant().getId() + "#" + option.getType();
    }

    private int mergeFournisseurProduits(Produit target, Produit source) {
        List<FournisseurProduit> sourceFps = fournisseurProduitRepository.findAllByProduitId(source.getId());
        Map<Integer, FournisseurProduit> targetByFournisseur = new HashMap<>();
        for (FournisseurProduit fp : fournisseurProduitRepository.findAllByProduitId(target.getId())) {
            targetByFournisseur.put(fp.getFournisseur().getId(), fp);
        }
        for (FournisseurProduit fp : sourceFps) {
            Integer fournisseurId = fp.getFournisseur().getId();
            FournisseurProduit kept = targetByFournisseur.get(fournisseurId);
            if (kept != null) {
                if (source.getFournisseurProduitPrincipal() != null && source.getFournisseurProduitPrincipal().getId().equals(fp.getId())) {
                    // fournisseur_produit_principal_id est UNIQUE : pointer le produit source vers
                    // `kept` violerait la contrainte si la cible référence déjà (ou référencera)
                    // ce même FournisseurProduit comme principal. Le produit source étant archivé
                    // juste après, son fournisseur principal n'a plus d'utilité : on le vide.
                    source.setFournisseurProduitPrincipal(null);
                    produitRepository.save(source);
                }
                entityManager
                    .createQuery("UPDATE OrderLine ol SET ol.fournisseurProduit = :kept WHERE ol.fournisseurProduit = :dup")
                    .setParameter("kept", kept)
                    .setParameter("dup", fp)
                    .executeUpdate();
                fournisseurProduitRepository.delete(fp);
            } else {
                fp.setProduit(target);
                fournisseurProduitRepository.save(fp);
                targetByFournisseur.put(fournisseurId, fp);
            }
        }
        return sourceFps.size();
    }

    private int mergeRayonProduits(Produit target, Produit source) {
        List<RayonProduit> sourceRayons = rayonProduitRepository.findAllByProduitId(source.getId()).stream().toList();
        Map<Integer, RayonProduit> targetByRayon = new HashMap<>();
        for (RayonProduit rp : rayonProduitRepository.findAllByProduitId(target.getId())) {
            targetByRayon.put(rp.getRayon().getId(), rp);
        }
        for (RayonProduit rp : sourceRayons) {
            Integer rayonId = rp.getRayon().getId();
            if (targetByRayon.containsKey(rayonId)) {
                rayonProduitRepository.delete(rp);
            } else {
                rp.setProduit(target);
                rayonProduitRepository.save(rp);
                targetByRayon.put(rayonId, rp);
            }
        }
        return sourceRayons.size();
    }

    private int mergeSalesLines(Produit target, Produit source) {
        List<SalesLine> sourceLines = salesLineRepository.findAllByProduitId(source.getId());
        for (SalesLine line : sourceLines) {
            Optional<SalesLine> existing = salesLineRepository.findBySalesIdAndProduitIdAndSalesSaleDate(
                line.getSales().getId().getId(),
                target.getId(),
                line.getSaleDate()
            );
            if (existing.isPresent()) {
                SalesLine kept = existing.get();
                kept.setQuantitySold(kept.getQuantitySold() + line.getQuantitySold());
                kept.setQuantityRequested(kept.getQuantityRequested() + line.getQuantityRequested());
                kept.setQuantityUg(kept.getQuantityUg() + line.getQuantityUg());
                kept.setQuantityAvoir(kept.getQuantityAvoir() + line.getQuantityAvoir());
                kept.setDiscountAmount(kept.getDiscountAmount() + line.getDiscountAmount());
                kept.setSalesAmount(kept.getSalesAmount() + line.getSalesAmount());
                kept.setTaxValue(kept.getTaxValue() + line.getTaxValue());
                kept.setCostAmount(kept.getCostAmount() + line.getCostAmount());
                kept.setAmountToBeTakenIntoAccount(kept.getAmountToBeTakenIntoAccount() + line.getAmountToBeTakenIntoAccount());
                List<LotSold> mergedLots = new ArrayList<>(kept.getLots());
                mergedLots.addAll(line.getLots());
                kept.setLots(mergedLots);
                salesLineRepository.save(kept);
                salesLineRepository.delete(line);
            } else {
                line.setProduit(target);
                salesLineRepository.save(line);
            }
        }
        return sourceLines.size();
    }

    private int mergeSubstituts(Produit target, Produit source) {
        int count = 0;
        for (Substitut s : substitutRepository.findAllByProduitId(source.getId())) {
            Produit other = s.getSubstitut();
            if (other.getId().equals(target.getId()) || substitutRepository.existsByProduitAndSubstitut(target, other)) {
                substitutRepository.delete(s);
            } else {
                s.setProduit(target);
                substitutRepository.save(s);
            }
            count++;
        }
        for (Substitut s : substitutRepository.findAllBySubstitutId(source.getId())) {
            Produit other = s.getProduit();
            if (other.getId().equals(target.getId()) || substitutRepository.existsByProduitAndSubstitut(other, target)) {
                substitutRepository.delete(s);
            } else {
                s.setSubstitut(target);
                substitutRepository.save(s);
            }
            count++;
        }
        return count;
    }

    private int mergeSemoisConfiguration(Produit target, Produit source) {
        Optional<SemoisConfiguration> sourceConfig = semoisConfigurationRepository.findByProduitId(source.getId());
        if (sourceConfig.isEmpty()) {
            return 0;
        }
        if (semoisConfigurationRepository.findByProduitId(target.getId()).isPresent()) {
            // Le produit cible a déjà une configuration SEMOIS : elle prévaut sur celle du doublon.
            semoisConfigurationRepository.delete(sourceConfig.get());
        } else {
            SemoisConfiguration cfg = sourceConfig.get();
            cfg.setProduit(target);
            semoisConfigurationRepository.save(cfg);
        }
        return 1;
    }

    private int mergeVentesMensuellesAgregees(Produit target, Produit source) {
        List<VentesMensuellesAgregees> sourceRows = ventesMensuellesAgregeesRepository.findAllByProduitIdIn(List.of(source.getId()));
        for (VentesMensuellesAgregees row : sourceRows) {
            Optional<VentesMensuellesAgregees> existing = ventesMensuellesAgregeesRepository.findByProduitIdAndAnneeMois(
                target.getId(),
                row.getAnneeMois()
            );
            if (existing.isPresent()) {
                VentesMensuellesAgregees kept = existing.get();
                kept.setQuantiteVendue(kept.getQuantiteVendue() + row.getQuantiteVendue());
                kept.setMontantCa(kept.getMontantCa() + row.getMontantCa());
                kept.setNombreVentes(kept.getNombreVentes() + row.getNombreVentes());
                kept.setIsFrozen(Boolean.TRUE.equals(kept.getIsFrozen()) || Boolean.TRUE.equals(row.getIsFrozen()));
                kept.setEstRuptureFournisseur(
                    Boolean.TRUE.equals(kept.getEstRuptureFournisseur()) || Boolean.TRUE.equals(row.getEstRuptureFournisseur())
                );
                ventesMensuellesAgregeesRepository.save(kept);
                ventesMensuellesAgregeesRepository.delete(row);
            } else {
                row.setProduit(target);
                ventesMensuellesAgregeesRepository.save(row);
            }
        }
        return sourceRows.size();
    }

    /**
     * Les lignes d'inventaire physique en cours ne peuvent pas être fusionnées automatiquement
     * (quelle quantité comptée retenir ?). En cas de collision (même session + même storage sur
     * le produit cible), la ligne du doublon est laissée telle quelle pour arbitrage manuel.
     */
    private int mergeStoreInventoryLines(Produit target, Produit source) {
        List<StoreInventoryLine> sourceLines = storeInventoryLineRepository.findAllByProduitId(source.getId());
        int reassigned = 0;
        for (StoreInventoryLine line : sourceLines) {
            Integer storageId = line.getStorage() == null ? null : line.getStorage().getId();
            boolean collision = storeInventoryLineRepository.existsByProduitIdAndStoreInventoryIdAndStorageId(
                target.getId(),
                line.getStoreInventory().getId(),
                storageId
            );
            if (!collision) {
                line.setProduit(target);
                storeInventoryLineRepository.save(line);
                reassigned++;
            }
        }
        return reassigned;
    }
}
