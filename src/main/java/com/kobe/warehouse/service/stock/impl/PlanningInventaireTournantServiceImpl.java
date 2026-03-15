package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.PlanningInventaireTournant;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.CritereTournant;
import com.kobe.warehouse.domain.enumeration.FrequenceTournant;
import com.kobe.warehouse.repository.FamilleProduitRepository;
import com.kobe.warehouse.repository.PlanningInventaireTournantRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StorageRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.dto.records.PlanningInventaireTournantRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.dto.records.TournantDashboardRecord;
import com.kobe.warehouse.service.stock.InventaireCreationService;
import com.kobe.warehouse.service.stock.PlanningInventaireTournantService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanningInventaireTournantServiceImpl implements PlanningInventaireTournantService {

    private static final Logger log = LoggerFactory.getLogger(
        PlanningInventaireTournantServiceImpl.class);

    private static final List<String> ABC_CLASSES = List.of("A", "B", "C");

    private final PlanningInventaireTournantRepository planningRepository;
    private final RayonRepository rayonRepository;
    private final FamilleProduitRepository familleProduitRepository;
    private final StorageRepository storageRepository;
    private final UserRepository userRepository;
    private final InventaireCreationService inventaireCreationService;

    public PlanningInventaireTournantServiceImpl(
        PlanningInventaireTournantRepository planningRepository,
        RayonRepository rayonRepository,
        FamilleProduitRepository familleProduitRepository,
        StorageRepository storageRepository,
        UserRepository userRepository,
        InventaireCreationService inventaireCreationService
    ) {
        this.planningRepository = planningRepository;
        this.rayonRepository = rayonRepository;
        this.familleProduitRepository = familleProduitRepository;
        this.storageRepository = storageRepository;
        this.userRepository = userRepository;
        this.inventaireCreationService = inventaireCreationService;
    }

    @Override
    public PlanningInventaireTournantRecord create(PlanningInventaireTournantRecord record) {
        PlanningInventaireTournant entity = new PlanningInventaireTournant();
        fillFromRecord(entity, record);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        // Initialise classeParetoCourante pour ABC
        if (entity.getCritere() == CritereTournant.CLASSIFICATION_ABC) {
            entity.setClasseParetoCourante(ABC_CLASSES.getFirst());
        }
        return toRecord(planningRepository.saveAndFlush(entity));
    }

    @Override
    public PlanningInventaireTournantRecord update(PlanningInventaireTournantRecord record) {
        PlanningInventaireTournant entity = planningRepository.getReferenceById(record.id());
        fillFromRecord(entity, record);
        entity.setUpdatedAt(LocalDateTime.now());
        return toRecord(planningRepository.saveAndFlush(entity));
    }

    @Override
    public void delete(Integer id) {
        planningRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanningInventaireTournantRecord> findById(Integer id) {
        return planningRepository.findById(id).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanningInventaireTournantRecord> findAll() {
        return planningRepository.findAllByOrderByProchaineExecutionAsc()
            .stream()
            .map(this::toRecord)
            .toList();
    }

    @Override
    public PlanningInventaireTournantRecord toggleActif(Integer id) {
        PlanningInventaireTournant entity = planningRepository.getReferenceById(id);
        entity.setActif(!Boolean.TRUE.equals(entity.getActif()));
        entity.setUpdatedAt(LocalDateTime.now());
        return toRecord(planningRepository.saveAndFlush(entity));
    }

    @Override
    public Long executerManuellement(Integer planningId) {
        PlanningInventaireTournant planning = planningRepository.getReferenceById(planningId);
        Long inventoryId = doExecuter(planning);
        planningRepository.saveAndFlush(planning);
        return inventoryId;
    }

    @Override
    @Transactional(readOnly = true)
    public TournantDashboardRecord getDashboard(Integer storageId) {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate in7Days = today.plusDays(7);

        List<PlanningInventaireTournant> all = storageId != null
            ? planningRepository.findAllByStorageIdOrderByLibelle(storageId)
            : planningRepository.findAllByOrderByProchaineExecutionAsc();

        int actifs = (int) all.stream().filter(p -> Boolean.TRUE.equals(p.getActif())).count();

        // Inventaires créés ce mois (basé sur derniere_execution)
        int ceMois = (int) all.stream()
            .filter(p -> p.getDerniereExecution() != null && !p.getDerniereExecution()
                .isBefore(firstOfMonth))
            .count();

        // Taux de couverture : plannings exécutés ce mois / plannings actifs
        int tauxCouverture = actifs == 0 ? 0 : Math.min(100, ceMois * 100 / actifs);

        List<PlanningInventaireTournantRecord> prochaines = all.stream()
            .filter(p -> Boolean.TRUE.equals(p.getActif()))
            .filter(p -> !p.getProchaineExecution().isAfter(in7Days))
            .map(this::toRecord)
            .toList();

        LocalDate prochaineTournant = all.stream()
            .filter(p -> Boolean.TRUE.equals(p.getActif()))
            .map(PlanningInventaireTournant::getProchaineExecution)
            .min(LocalDate::compareTo)
            .orElse(null);

        return new TournantDashboardRecord(actifs, ceMois, tauxCouverture, prochaines,
            prochaineTournant);
    }

    // ── Package-level : appelé par le scheduler ──────────────────────────────

    /**
     * Exécute tous les plannings échus et sauvegarde les avancements. Appelé par
     */
    public List<Long> executerTournantsEchus() {
        LocalDate today = LocalDate.now();
        List<PlanningInventaireTournant> echus = planningRepository.findEchus(today);

        return echus.stream()
            .map(planning -> {
                try {
                    Long id = doExecuter(planning);
                    planningRepository.saveAndFlush(planning);
                    return id;
                } catch (Exception e) {
                    log.error("Erreur lors de l'exécution du planning tournant id={}",
                        planning.getId(), e);
                    return null;
                }
            })
            .filter(id -> id != null)
            .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Exécute un planning : détermine le critère courant, crée l'inventaire, et avance la rotation.
     * L'inventaire est assigné à l'employé du planning (ou au premier utilisateur actif en fallback
     * si aucun n'est configuré).
     */
    private Long doExecuter(PlanningInventaireTournant planning) {
        Storage storage = planning.getStorage();
        Integer storageId = storage != null ? storage.getId() : null;
        Integer userId = resolvePlanningUserId(planning);

        StoreInventoryRecord record = buildInventoryRecord(planning, storageId, userId);
        Long inventoryId = inventaireCreationService.create(record).getId();

        // Avancer la rotation
        advanceRotation(planning);

        // Mettre à jour les stats
        planning.setNbExecutions(planning.getNbExecutions() + 1);
        planning.setDerniereExecution(LocalDate.now());
        planning.setProchaineExecution(nextExecutionDate(planning.getFrequence()));

        log.info("Planning tournant id={} exécuté → inventaire id={}, prochaine={}",
            planning.getId(), inventoryId, planning.getProchaineExecution());
        return inventoryId;
    }

    private StoreInventoryRecord buildInventoryRecord(PlanningInventaireTournant planning,
        Integer storageId, Integer userId) {
        return switch (planning.getCritere()) {
            case RAYON -> buildRayonRecord(planning, storageId, userId);
            case FAMILLE -> buildFamilleRecord(planning, storageId, userId);
            case CLASSIFICATION_ABC -> buildAbcRecord(planning, storageId, userId);
        };
    }

    private StoreInventoryRecord buildRayonRecord(PlanningInventaireTournant planning,
        Integer storageId, Integer userId) {
        List<Rayon> rayons = storageId != null
            ? rayonRepository.findAllByStorageIdOrderByLibelle(storageId)
            : List.of();
        if (rayons.isEmpty()) {
            throw new IllegalStateException("Aucun rayon trouvé pour storage=" + storageId);
        }
        int idx = planning.getCritereIndexCourant() % rayons.size();
        Rayon rayon = rayons.get(idx);
        String description = "Inventaire tournant — Rayon : " + rayon.getLibelle();
        return new StoreInventoryRecord(null, storageId, rayon.getId(), "RAYON", null, description,
            null, null, null, null, userId);
    }

    private StoreInventoryRecord buildFamilleRecord(PlanningInventaireTournant planning,
        Integer storageId, Integer userId) {
        List<FamilleProduit> familles = familleProduitRepository.findAllByOrderByLibelleAsc();
        if (familles.isEmpty()) {
            throw new IllegalStateException("Aucune famille de produits trouvée");
        }
        int idx = planning.getCritereIndexCourant() % familles.size();
        FamilleProduit famille = familles.get(idx);
        String description = "Inventaire tournant — Famille : " + famille.getLibelle();
        return new StoreInventoryRecord(null, storageId, null, "FAMILLY", famille.getId(),
            description,
            null, null, null, null, userId);
    }

    private StoreInventoryRecord buildAbcRecord(PlanningInventaireTournant planning,
        Integer storageId, Integer userId) {
        String classePareto = planning.getClasseParetoCourante() != null
            ? planning.getClasseParetoCourante()
            : ABC_CLASSES.get(planning.getCritereIndexCourant() % 3);
        String description = "Inventaire tournant — Classe Pareto : " + classePareto;
        return new StoreInventoryRecord(null, storageId, null, "ABC", null, description,
            null, null, null, classePareto, userId);
    }

    private void advanceRotation(PlanningInventaireTournant planning) {
        int newIndex = planning.getCritereIndexCourant() + 1;
        planning.setCritereIndexCourant(newIndex);
        // Pour ABC : mettre à jour classeParetoCourante
        if (planning.getCritere() == CritereTournant.CLASSIFICATION_ABC) {
            planning.setClasseParetoCourante(ABC_CLASSES.get(newIndex % 3));
        }
    }

    private LocalDate nextExecutionDate(FrequenceTournant frequence) {
        LocalDate today = LocalDate.now();
        return switch (frequence) {
            case QUOTIDIEN -> today.plusDays(1);
            case HEBDO -> today.plusWeeks(1);
            case MENSUEL -> today.plusMonths(1);
            case TRIMESTRIEL -> today.plusMonths(3);
        };
    }

    /**
     * Résout l'userId à utiliser pour créer l'inventaire : 1. L'employé affecté au planning si
     * configuré 2. Sinon, le premier utilisateur actif du système (fallback scheduler)
     */
    private Integer resolvePlanningUserId(PlanningInventaireTournant planning) {
        if (planning.getUser() != null) {
            return planning.getUser().getId();
        }
        return userRepository.findAll(PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(AppUser::getId)
            .orElseThrow(() -> new IllegalStateException("Aucun utilisateur système trouvé"));
    }

    private void fillFromRecord(PlanningInventaireTournant entity,
        PlanningInventaireTournantRecord record) {
        entity.setLibelle(record.libelle());
        entity.setFrequence(FrequenceTournant.valueOf(record.frequence()));
        entity.setCritere(CritereTournant.valueOf(record.critere()));
        entity.setProchaineExecution(record.prochaineExecution());
        entity.setActif(record.actif());
        entity.setStorage(record.storageId() != null
            ? storageRepository.getReferenceById(record.storageId())
            : null);
        entity.setUser(record.userId() != null
            ? userRepository.getReferenceById(record.userId())
            : null);
        if (record.critereIndexCourant() != null) {
            entity.setCritereIndexCourant(record.critereIndexCourant());
        }
        if (record.classeParetoCourante() != null) {
            entity.setClasseParetoCourante(record.classeParetoCourante());
        }
    }

    private PlanningInventaireTournantRecord toRecord(PlanningInventaireTournant e) {
        Storage storage = e.getStorage();
        AppUser user = e.getUser();
        return new PlanningInventaireTournantRecord(
            e.getId(),
            e.getLibelle(),
            e.getFrequence().name(),
            e.getCritere().name(),
            storage != null ? storage.getId() : null,
            storage != null ? storage.getName() : null,
            user != null ? user.getId() : null,
            user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : null,
            e.getProchaineExecution(),
            Boolean.TRUE.equals(e.getActif()),
            e.getCritereIndexCourant(),
            e.getClasseParetoCourante(),
            e.getNbExecutions(),
            e.getDerniereExecution()
        );
    }
}
