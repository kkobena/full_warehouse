package com.kobe.warehouse.service.inventaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.records.PlanningInventaireTournantRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.dto.records.TournantDashboardRecord;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.inventaire.impl.PlanningInventaireTournantServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

/**
 * L'inventaire tournant compte un morceau de l'officine à chaque échéance, en avançant d'un cran
 * dans une liste ordonnée. Tout le service tient dans cette rotation : elle doit couvrir la liste
 * entière — les cinq classes ABC, extrémités comprises — et repartir au début une fois le tour
 * bouclé. Le reste, ce sont les cas où la rotation n'a rien à parcourir, qui doivent se dire
 * plutôt que de tomber en erreur technique.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanningInventaireTournantService — rotation des inventaires tournants")
class PlanningInventaireTournantServiceImplTest {

    @Mock
    private PlanningInventaireTournantRepository planningRepository;

    @Mock
    private RayonRepository rayonRepository;

    @Mock
    private FamilleProduitRepository familleProduitRepository;

    @Mock
    private StorageRepository storageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventaireCreationService inventaireCreationService;

    private PlanningInventaireTournantService service;

    @BeforeEach
    void init() {
        service = new PlanningInventaireTournantServiceImpl(
            planningRepository,
            rayonRepository,
            familleProduitRepository,
            storageRepository,
            userRepository,
            inventaireCreationService
        );
    }

    @Test
    @DisplayName("Un planning ABC démarre sur la première classe du cycle")
    void creationAbc() {
        when(planningRepository.saveAndFlush(any(PlanningInventaireTournant.class)))
            .thenAnswer(i -> i.getArgument(0));

        PlanningInventaireTournantRecord cree = service.create(
            record(null, "CLASSIFICATION_ABC", "MENSUEL", null));

        assertEquals("A_PLUS", cree.classeParetoCourante(), "les 60 premiers % du CA sont comptés en premier");
    }

    @Test
    @DisplayName("Un planning par rayon n'initialise aucune classe Pareto")
    void creationRayon() {
        when(planningRepository.saveAndFlush(any(PlanningInventaireTournant.class)))
            .thenAnswer(i -> i.getArgument(0));

        assertNull(service.create(record(null, "RAYON", "HEBDO", null)).classeParetoCourante());
    }

    @Test
    @DisplayName("Le cycle ABC parcourt les cinq classes puis revient au début")
    void cycleAbcComplet() {
        PlanningInventaireTournant planning = planning(CritereTournant.CLASSIFICATION_ABC, FrequenceTournant.QUOTIDIEN, null);
        planning.setClasseParetoCourante("A_PLUS");
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        List<String> parcourues = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            parcourues.add(planning.getClasseParetoCourante());
            service.executerManuellement(1);
        }

        assertEquals(List.of("A_PLUS", "A", "B", "C", "D", "A_PLUS"), parcourues,
            "aucune extrémité n'est sautée et le cycle boucle");
    }

    @Test
    @DisplayName("La description d'un inventaire ABC affiche « A+ », pas la valeur stockée")
    void libelleClassePareto() {
        PlanningInventaireTournant planning = planning(CritereTournant.CLASSIFICATION_ABC, FrequenceTournant.MENSUEL, null);
        planning.setClasseParetoCourante("A_PLUS");
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        service.executerManuellement(1);

        StoreInventoryRecord demande = capturerLaDemande();
        assertEquals("A_PLUS", demande.classePareto(), "le filtre SQL garde la valeur stockée");
        assertTrue(demande.description().endsWith("A+"), demande.description());
    }

    @Test
    @DisplayName("Un planning par rayon sans emplacement de référence balaie tous les rayons")
    void rayonsDeToutLOfficine() {
        PlanningInventaireTournant planning = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, null);
        planning.setCritereIndexCourant(1);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(rayonRepository.findAll(any(Sort.class)))
            .thenReturn(List.of(rayon(10, "ANTIBIOTIQUES"), rayon(11, "DERMATOLOGIE")));
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        service.executerManuellement(1);

        StoreInventoryRecord demande = capturerLaDemande();
        assertEquals(11, demande.rayon(), "l'index courant désigne le deuxième rayon de la liste");
        assertEquals("RAYON", demande.inventoryCategory());
        assertNull(demande.storage());
        verify(rayonRepository, never()).findAllByStorageIdOrderByLibelle(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("Un planning rattaché à un emplacement ne balaie que ses rayons")
    void rayonsDUnEmplacement() {
        PlanningInventaireTournant planning = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, storage(4));
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(rayonRepository.findAllByStorageIdOrderByLibelle(4)).thenReturn(List.of(rayon(10, "ANTIBIOTIQUES")));
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        service.executerManuellement(1);

        StoreInventoryRecord demande = capturerLaDemande();
        assertEquals(4, demande.storage());
        assertEquals(10, demande.rayon());
    }

    @Test
    @DisplayName("Sans aucun rayon, l'exécution se refuse par un message métier")
    void aucunRayonDefini() {
        PlanningInventaireTournant planning = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, null);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(rayonRepository.findAll(any(Sort.class))).thenReturn(List.of());

        GenericError erreur = assertThrows(GenericError.class, () -> service.executerManuellement(1));

        assertTrue(erreur.getMessage().contains("Aucun rayon"), erreur.getMessage());
        verify(inventaireCreationService, never()).create(any(StoreInventoryRecord.class));
    }

    @Test
    @DisplayName("Sans aucune famille, l'exécution se refuse par un message métier")
    void aucuneFamilleDefinie() {
        PlanningInventaireTournant planning = planning(CritereTournant.FAMILLE, FrequenceTournant.MENSUEL, null);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(familleProduitRepository.findAllByOrderByLibelleAsc()).thenReturn(List.of());

        assertThrows(GenericError.class, () -> service.executerManuellement(1));
    }

    @Test
    @DisplayName("L'exécution avance la rotation, compte le passage et reporte l'échéance")
    void avancementApresExecution() {
        PlanningInventaireTournant planning = planning(CritereTournant.FAMILLE, FrequenceTournant.TRIMESTRIEL, null);
        planning.setNbExecutions(2);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(familleProduitRepository.findAllByOrderByLibelleAsc())
            .thenReturn(List.of(famille(1, "ANTALGIQUES"), famille(2, "VITAMINES")));
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        Long inventoryId = service.executerManuellement(1);

        assertEquals(100L, inventoryId);
        assertEquals(1, planning.getCritereIndexCourant());
        assertEquals(3, planning.getNbExecutions());
        assertEquals(LocalDate.now(), planning.getDerniereExecution());
        assertEquals(LocalDate.now().plusMonths(3), planning.getProchaineExecution());
        verify(planningRepository).saveAndFlush(planning);
    }

    @Test
    @DisplayName("Chaque fréquence reporte l'échéance à son propre pas")
    void reportSelonLaFrequence() {
        assertEquals(LocalDate.now().plusDays(1), echeanceApresExecution(FrequenceTournant.QUOTIDIEN));
        assertEquals(LocalDate.now().plusWeeks(1), echeanceApresExecution(FrequenceTournant.HEBDO));
        assertEquals(LocalDate.now().plusMonths(1), echeanceApresExecution(FrequenceTournant.MENSUEL));
        assertEquals(LocalDate.now().plusMonths(3), echeanceApresExecution(FrequenceTournant.TRIMESTRIEL));
    }

    @Test
    @DisplayName("Sans employé affecté, l'inventaire revient au premier utilisateur du système")
    void utilisateurParDefaut() {
        PlanningInventaireTournant planning = planning(CritereTournant.FAMILLE, FrequenceTournant.MENSUEL, null);
        planning.setUser(null);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(familleProduitRepository.findAllByOrderByLibelleAsc()).thenReturn(List.of(famille(1, "ANTALGIQUES")));
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(utilisateur(8))));
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        service.executerManuellement(1);

        assertEquals(8, capturerLaDemande().userId());
    }

    @Test
    @DisplayName("L'employé affecté au planning reçoit l'inventaire")
    void employeAffecte() {
        PlanningInventaireTournant planning = planning(CritereTournant.FAMILLE, FrequenceTournant.MENSUEL, null);
        planning.setUser(utilisateur(15));
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(familleProduitRepository.findAllByOrderByLibelleAsc()).thenReturn(List.of(famille(1, "ANTALGIQUES")));
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        service.executerManuellement(1);

        assertEquals(15, capturerLaDemande().userId());
        verify(userRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Une base sans aucun utilisateur interdit l'exécution automatique")
    void aucunUtilisateurSysteme() {
        PlanningInventaireTournant planning = planning(CritereTournant.FAMILLE, FrequenceTournant.MENSUEL, null);
        planning.setUser(null);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        assertThrows(IllegalStateException.class, () -> service.executerManuellement(1));
    }

    @Test
    @DisplayName("Un planning en échec n'empêche pas les autres échéances de partir")
    void unEchecNArretePasLaTournee() {
        PlanningInventaireTournant fautif = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, null);
        fautif.setId(1);
        PlanningInventaireTournant valide = planning(CritereTournant.FAMILLE, FrequenceTournant.HEBDO, null);
        valide.setId(2);

        when(planningRepository.findEchus(LocalDate.now())).thenReturn(List.of(fautif, valide));
        when(rayonRepository.findAll(any(Sort.class))).thenReturn(List.of());
        when(familleProduitRepository.findAllByOrderByLibelleAsc()).thenReturn(List.of(famille(1, "ANTALGIQUES")));
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(200L));

        List<Long> crees = service.executerTournantsEchus();

        assertEquals(List.of(200L), crees);
        assertEquals(0, fautif.getNbExecutions(), "le planning en échec n'avance pas");
        assertEquals(1, valide.getNbExecutions());
    }

    @Test
    @DisplayName("Activer/désactiver bascule l'état du planning")
    void bascule() {
        PlanningInventaireTournant planning = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, null);
        planning.setActif(true);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(planningRepository.saveAndFlush(planning)).thenReturn(planning);

        assertFalse(service.toggleActif(1).actif());
        assertTrue(service.toggleActif(1).actif());
    }

    @Test
    @DisplayName("Un employé sans nom de famille n'empêche pas de lire le planning")
    void employeSansNomDeFamille() {
        AppUser sansNom = new AppUser();
        sansNom.setId(6);
        sansNom.setFirstName("Awa");
        PlanningInventaireTournant planning = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, null);
        planning.setUser(sansNom);
        when(planningRepository.findById(1)).thenReturn(java.util.Optional.of(planning));

        PlanningInventaireTournantRecord relu = service.findById(1).orElseThrow();

        assertEquals("Awa", relu.userFullName(), "l'abréviation se réduit au prénom, elle n'échoue pas");
    }

    @Test
    @DisplayName("Le tableau de bord ne retient que les plannings actifs échéant sous sept jours")
    void tableauDeBord() {
        PlanningInventaireTournant imminent = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, storage(4));
        imminent.setProchaineExecution(LocalDate.now().plusDays(2))
            .setDerniereExecution(LocalDate.now().withDayOfMonth(1));
        PlanningInventaireTournant lointain = planning(CritereTournant.FAMILLE, FrequenceTournant.TRIMESTRIEL, storage(4));
        lointain.setProchaineExecution(LocalDate.now().plusMonths(2));
        PlanningInventaireTournant inactif = planning(CritereTournant.RAYON, FrequenceTournant.HEBDO, storage(4));
        inactif.setActif(false).setProchaineExecution(LocalDate.now());
        when(planningRepository.findAllByStorageIdOrderByLibelle(4))
            .thenReturn(List.of(imminent, lointain, inactif));

        TournantDashboardRecord tableau = service.getDashboard(4);

        assertEquals(2, tableau.nbPlanningsActifs(), "le planning désactivé ne compte pas");
        assertEquals(1, tableau.nbInventairesCeMois());
        assertEquals(50, tableau.tauxCouverturePct(), "un planning exécuté sur deux actifs");
        assertEquals(1, tableau.prochainesExecutions().size());
        assertEquals(LocalDate.now().plusDays(2), tableau.prochaineTournant(),
            "l'échéance la plus proche parmi les plannings actifs");
    }

    @Test
    @DisplayName("Un tableau de bord sans planning actif n'invente pas de taux de couverture")
    void tableauDeBordVide() {
        when(planningRepository.findAllByOrderByProchaineExecutionAsc()).thenReturn(List.of());

        TournantDashboardRecord tableau = service.getDashboard(null);

        assertEquals(0, tableau.nbPlanningsActifs());
        assertEquals(0, tableau.tauxCouverturePct());
        assertNull(tableau.prochaineTournant());
    }

    // ===== jeu d'essai =====

    private LocalDate echeanceApresExecution(FrequenceTournant frequence) {
        PlanningInventaireTournant planning = planning(CritereTournant.FAMILLE, frequence, null);
        when(planningRepository.getReferenceById(1)).thenReturn(planning);
        when(familleProduitRepository.findAllByOrderByLibelleAsc()).thenReturn(List.of(famille(1, "ANTALGIQUES")));
        when(inventaireCreationService.create(any(StoreInventoryRecord.class))).thenReturn(inventaire(100L));

        service.executerManuellement(1);
        return planning.getProchaineExecution();
    }

    private StoreInventoryRecord capturerLaDemande() {
        ArgumentCaptor<StoreInventoryRecord> demande = ArgumentCaptor.forClass(StoreInventoryRecord.class);
        verify(inventaireCreationService).create(demande.capture());
        return demande.getValue();
    }

    private PlanningInventaireTournant planning(CritereTournant critere, FrequenceTournant frequence, Storage storage) {
        return new PlanningInventaireTournant()
            .setLibelle("Rotation")
            .setCritere(critere)
            .setFrequence(frequence)
            .setStorage(storage)
            .setUser(utilisateur(1))
            .setActif(true)
            .setCritereIndexCourant(0)
            .setNbExecutions(0)
            .setProchaineExecution(LocalDate.now())
            .setCreatedAt(LocalDateTime.now())
            .setUpdatedAt(LocalDateTime.now());
    }

    private PlanningInventaireTournantRecord record(Integer id, String critere, String frequence, Integer storageId) {
        return new PlanningInventaireTournantRecord(
            id, "Rotation", frequence, critere, storageId, null, null, null,
            LocalDate.now().plusDays(1), true, null, null, 0, null
        );
    }

    private StoreInventoryDTO inventaire(Long id) {
        StoreInventoryDTO dto = new StoreInventoryDTO();
        dto.setId(id);
        return dto;
    }

    private Rayon rayon(int id, String libelle) {
        Rayon rayon = new Rayon();
        rayon.setId(id);
        rayon.setLibelle(libelle);
        return rayon;
    }

    private FamilleProduit famille(int id, String libelle) {
        FamilleProduit famille = new FamilleProduit();
        famille.setId(id);
        famille.setLibelle(libelle);
        return famille;
    }

    private Storage storage(int id) {
        Storage storage = new Storage();
        storage.setId(id);
        storage.setName("Emplacement " + id);
        return storage;
    }

    private AppUser utilisateur(int id) {
        AppUser user = new AppUser();
        user.setId(id);
        // Nom et prénom renseignés : le libellé du planning les abrège, et l'abréviation lit
        // le premier caractère du nom sans le vérifier.
        user.setFirstName("Awa");
        user.setLastName("Kone");
        return user;
    }
}
