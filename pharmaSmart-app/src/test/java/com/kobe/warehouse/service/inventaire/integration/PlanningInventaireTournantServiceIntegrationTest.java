package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import com.kobe.warehouse.service.dto.records.PlanningInventaireTournantRecord;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le planning tournant est le seul chemin par lequel un inventaire se crée sans opérateur devant
 * l'écran. Ce qui doit tenir de bout en bout : le planning se persiste, son exécution crée
 * vraiment un inventaire au bon périmètre, et la rotation avancée est bien celle qu'on relira à
 * l'échéance suivante.
 */
@DisplayName("PlanningInventaireTournantService — rotation programmée sur PostgreSQL")
class PlanningInventaireTournantServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("Un planning créé se relit avec sa rotation initialisée")
    void creationEtRelecture() {
        PlanningInventaireTournantRecord cree = services.planningInventaireTournantService.create(
            demande("Rotation ABC", "CLASSIFICATION_ABC", "MENSUEL"));
        viderLeCache();

        PlanningInventaireTournantRecord relu = services.planningInventaireTournantService
            .findById(cree.id())
            .orElseThrow();

        assertEquals("Rotation ABC", relu.libelle());
        assertEquals("A_PLUS", relu.classeParetoCourante());
        assertEquals(0, relu.critereIndexCourant());
        assertEquals(0, relu.nbExecutions());
        assertTrue(relu.actif());
        assertEquals(utilisateur.getId(), relu.userId());
    }

    @Test
    @DisplayName("Activer puis désactiver un planning se voit en base")
    void bascule() {
        PlanningInventaireTournantRecord cree = services.planningInventaireTournantService.create(
            demande("Rotation rayon", "RAYON", "HEBDO"));
        viderLeCache();

        assertFalse(services.planningInventaireTournantService.toggleActif(cree.id()).actif());
        viderLeCache();
        assertFalse(services.planningInventaireTournantService.findById(cree.id()).orElseThrow().actif());
    }

    @Test
    @DisplayName("Supprimer un planning le retire de la liste")
    void suppression() {
        PlanningInventaireTournantRecord cree = services.planningInventaireTournantService.create(
            demande("A supprimer", "RAYON", "HEBDO"));
        viderLeCache();

        services.planningInventaireTournantService.delete(cree.id());
        viderLeCache();

        assertTrue(services.planningInventaireTournantService.findById(cree.id()).isEmpty());
    }

    @Test
    @DisplayName("Exécuter un planning par rayon crée l'inventaire du rayon courant et avance la rotation")
    void executionParRayon() {
        Rayon antibiotiques = rayon(rayon, "AAA ANTIBIOTIQUES");
        Produit range = produitEnStock(unique("RANGE"), 12);
        ranger(range, antibiotiques);
        PlanningInventaireTournantRecord planning = services.planningInventaireTournantService.create(
            demande("Rotation rayon", "RAYON", "HEBDO", STORAGE_RAYON_ID));
        viderLeCache();

        Long inventaireId = services.planningInventaireTournantService.executerManuellement(planning.id());
        viderLeCache();

        assertNotNull(inventaireId);
        StoreInventory inventaire = services.storeInventoryRepository.findById(inventaireId).orElseThrow();
        assertEquals(InventoryType.PROGRAMME, inventaire.getInventoryType());
        assertEquals(antibiotiques.getId(), inventaire.getRayon().getId());
        assertTrue(inventaire.getDescription().contains("AAA ANTIBIOTIQUES"));
        assertEquals(1, lignesDe(inventaireId));

        PlanningInventaireTournantRecord relu = services.planningInventaireTournantService
            .findById(planning.id())
            .orElseThrow();
        assertEquals(1, relu.critereIndexCourant(), "la rotation a avancé d'un cran");
        assertEquals(1, relu.nbExecutions());
        assertEquals(LocalDate.now(), relu.derniereExecution());
        assertEquals(LocalDate.now().plusWeeks(1), relu.prochaineExecution());
    }

    @Test
    @DisplayName("Exécuter un planning par famille crée l'inventaire de la famille courante")
    void executionParFamille() {
        produit(unique("DANS LA PREMIERE FAMILLE"), 1_000, 600, premiereFamille());
        PlanningInventaireTournantRecord planning = services.planningInventaireTournantService.create(
            demande("Rotation famille", "FAMILLE", "MENSUEL"));
        viderLeCache();

        Long inventaireId = services.planningInventaireTournantService.executerManuellement(planning.id());
        viderLeCache();

        StoreInventory inventaire = services.storeInventoryRepository.findById(inventaireId).orElseThrow();
        assertTrue(inventaire.getDescription().startsWith("Inventaire tournant — Famille : "));
        assertEquals(LocalDate.now().plusMonths(1),
            services.planningInventaireTournantService.findById(planning.id()).orElseThrow().prochaineExecution());
    }

    @Test
    @DisplayName("Seuls les plannings actifs et échus partent à l'échéance")
    void executionDesEchus() {
        produit(unique("A COMPTER"), 1_000, 600, premiereFamille());
        PlanningInventaireTournantRecord echu = services.planningInventaireTournantService.create(
            demande("Echu", "FAMILLE", "MENSUEL", null, LocalDate.now().minusDays(1), true));
        services.planningInventaireTournantService.create(
            demande("Pas encore", "FAMILLE", "MENSUEL", null, LocalDate.now().plusDays(10), true));
        services.planningInventaireTournantService.create(
            demande("Desactive", "FAMILLE", "MENSUEL", null, LocalDate.now().minusDays(1), false));
        viderLeCache();

        List<Long> crees = services.planningInventaireTournantService.executerTournantsEchus();
        viderLeCache();

        assertEquals(1, crees.size());
        assertEquals(1, services.planningInventaireTournantService.findById(echu.id()).orElseThrow().nbExecutions());
    }

    @Test
    @DisplayName("Le tableau de bord d'un emplacement ne voit que ses plannings")
    void tableauDeBordParEmplacement() {
        services.planningInventaireTournantService.create(
            demande("Sur le rayon", "RAYON", "HEBDO", STORAGE_RAYON_ID));
        services.planningInventaireTournantService.create(
            demande("Sur la reserve", "RAYON", "HEBDO", STORAGE_RESERVE_ID));
        viderLeCache();

        assertEquals(1, services.planningInventaireTournantService.getDashboard(STORAGE_RAYON_ID).nbPlanningsActifs());
        assertEquals(2, services.planningInventaireTournantService.getDashboard(null).nbPlanningsActifs());
    }

    @Test
    @DisplayName("La liste des plannings est ordonnée par échéance")
    void listeOrdonnee() {
        services.planningInventaireTournantService.create(
            demande("Plus tard", "RAYON", "HEBDO", null, LocalDate.now().plusDays(20), true));
        services.planningInventaireTournantService.create(
            demande("Bientot", "RAYON", "HEBDO", null, LocalDate.now().plusDays(2), true));
        viderLeCache();

        List<PlanningInventaireTournantRecord> plannings = services.planningInventaireTournantService.findAll();

        assertEquals(2, plannings.size());
        assertEquals("Bientot", plannings.getFirst().libelle());
    }

    // ===== outils =====

    private PlanningInventaireTournantRecord demande(String libelle, String critere, String frequence) {
        return demande(libelle, critere, frequence, null);
    }

    private PlanningInventaireTournantRecord demande(String libelle, String critere, String frequence, Integer storageId) {
        return demande(libelle, critere, frequence, storageId, LocalDate.now(), true);
    }

    private PlanningInventaireTournantRecord demande(
        String libelle,
        String critere,
        String frequence,
        Integer storageId,
        LocalDate prochaineExecution,
        boolean actif
    ) {
        return new PlanningInventaireTournantRecord(
            null, libelle, frequence, critere, storageId, null, utilisateur.getId(), null,
            prochaineExecution, actif, null, null, 0, null
        );
    }
}
