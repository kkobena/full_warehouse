package com.kobe.warehouse.service.facturation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.PlanificationFacturation;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import com.kobe.warehouse.domain.enumeration.OrigineGeneration;
import com.kobe.warehouse.domain.enumeration.Periodicite;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.HistoriquePlanificationDto;
import com.kobe.warehouse.service.facturation.dto.PlanificationDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * {@link com.kobe.warehouse.service.facturation.service.PlanificationFacturationService} sur un
 * vrai PostgreSQL.
 *
 * <p>Une planification édite des factures toute seule, à échéance. Ce qui demande une base : le
 * choix des organismes éligibles, qui croise périodicité, case « inclure à la facturation
 * automatique » et statut actif ; l'édition qu'elle déclenche pour de bon ; et la trace laissée
 * après coup — dernière période facturée, statut, historique — car c'est cette trace qui décide de
 * la période suivante. Une exécution qui n'avancerait pas {@code dernierePeriodeFin} refacturerait
 * indéfiniment le même mois.
 *
 * <p>Les migrations installent six planifications de référence — mensuelle, quinzainière et
 * bimensuelle, en définitif et en provisoire — et une contrainte d'unicité interdit d'en avoir deux
 * pour le même couple. Les tests travaillent donc sur celles-là, comme le fait l'écran, et ne créent
 * que sur la périodicité hebdomadaire, seule laissée libre.
 */
@DisplayName("PlanificationFacturationService — facturation programmée sur PostgreSQL")
class PlanificationFacturationIntegrationTest extends AbstractFacturationIntegrationTest {

    @Test
    @DisplayName("Les six planifications de référence sont livrées avec la base")
    void planificationsDeReference() {
        List<PlanificationDto> toutes = services.planificationFacturationService.findAll();

        assertEquals(6, toutes.size(), "trois périodicités, en définitif et en provisoire");
        assertTrue(toutes.stream().noneMatch(PlanificationDto::actif), "elles arrivent désactivées");
        assertTrue(toutes.stream().allMatch(p -> p.dernierePeriodeFin() != null), "chacune sait d'où repartir");
    }

    @Test
    @DisplayName("Créer une planification la programme et compte les organismes concernés")
    void creation() {
        organismeEligible("HEBDO A", Periodicite.HEBDOMADAIRE);
        organismeEligible("HEBDO B", Periodicite.HEBDOMADAIRE);
        organismeEligible("MENSUEL IGNORE", Periodicite.MENSUEL);
        viderLeCache();

        PlanificationDto cree = services.planificationFacturationService.create(
            planification("Hebdomadaire", Periodicite.HEBDOMADAIRE, LocalDate.of(2026, 1, 4))
        );
        viderLeCache();

        assertNotNull(cree.id());
        assertEquals(LocalDate.of(2026, 1, 12).atTime(7, 0), cree.prochaineExecution());
        assertEquals(2L, cree.nombreOrganismes(), "seuls les organismes hebdomadaires sont comptés");
        assertNotNull(em.find(PlanificationFacturation.class, cree.id()));
    }

    @Test
    @DisplayName("Une seconde planification sur la même périodicité est refusée par la base")
    void doublonDePeriodicite() {
        PlanificationDto doublon = planification("Doublon mensuel", Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));

        // Le service ne contrôle pas l'unicité : c'est la contrainte
        // uq_planification_periodicite_provisoire qui l'impose.
        assertThrows(DataIntegrityViolationException.class, () -> services.planificationFacturationService.create(doublon));
    }

    @Test
    @DisplayName("Modifier une planification reprogramme son échéance")
    void modification() {
        PlanificationFacturation mensuelle = planificationDeReference(Periodicite.MENSUEL);
        viderLeCache();

        PlanificationDto modifiee = services.planificationFacturationService.update(
            mensuelle.getId(),
            new PlanificationDto(
                mensuelle.getId(),
                "Mensuelle renommée",
                Periodicite.MENSUEL,
                LocalTime.of(5, 15),
                false,
                true,
                LocalDate.of(2026, 1, 31),
                null,
                null,
                null,
                null,
                0L
            )
        );
        viderLeCache();

        assertEquals("Mensuelle renommée", modifiee.libelle());
        assertEquals(LocalDate.of(2026, 3, 1).atTime(5, 15), modifiee.prochaineExecution());
        assertEquals("Mensuelle renommée", em.find(PlanificationFacturation.class, mensuelle.getId()).getLibelle());
    }

    @Test
    @DisplayName("Activer une planification dont l'échéance est passée la reprogramme")
    void activation() {
        PlanificationFacturation mensuelle = planificationDeReference(Periodicite.MENSUEL);
        mensuelle.setDernierePeriodeFin(LocalDate.of(2026, 1, 31));
        mensuelle.setHeureDeclenchement(LocalTime.of(7, 0));
        mensuelle.setProchaineExecution(null);
        viderLeCache();

        services.planificationFacturationService.toggleActif(mensuelle.getId());
        viderLeCache();

        PlanificationFacturation relue = em.find(PlanificationFacturation.class, mensuelle.getId());
        assertTrue(relue.isActif());
        assertEquals(LocalDate.of(2026, 3, 1).atTime(7, 0), relue.getProchaineExecution());
    }

    @Test
    @DisplayName("Supprimer une planification l'efface")
    void suppression() {
        PlanificationDto hebdomadaire = services.planificationFacturationService.create(
            planification("À supprimer", Periodicite.HEBDOMADAIRE, LocalDate.of(2026, 1, 4))
        );
        viderLeCache();

        services.planificationFacturationService.delete(hebdomadaire.id());
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM planification_facturation WHERE id = " + hebdomadaire.id()));
    }

    @Test
    @DisplayName("Exécuter maintenant édite les factures des organismes éligibles")
    void executionManuelle() {
        TiersPayant organisme = organismeEligible("CNAM EXEC", Periodicite.MENSUEL);
        dossier(compte(organisme), 40_000, LocalDate.now().withDayOfMonth(5));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();

        FactureEditionResponse reponse = services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        assertNotNull(reponse.generationCode(), "une édition a bien eu lieu");
        List<FactureTiersPayant> factures = facturesDe(reponse.generationCode());
        assertEquals(1, factures.size());
        assertEquals(organisme.getId(), factures.getFirst().getTiersPayant().getId());
        assertEquals(OrigineGeneration.AUTO, factures.getFirst().getOrigineGeneration(), "la facture porte son origine");
    }

    @Test
    @DisplayName("Après exécution, la planification retient la période facturée et vise la suivante")
    void traceApresExecution() {
        TiersPayant organisme = organismeEligible("TRACE", Periodicite.MENSUEL);
        dossier(compte(organisme), 30_000, LocalDate.now().withDayOfMonth(3));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();

        services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        PlanificationFacturation relue = em.find(PlanificationFacturation.class, plan.getId());
        LocalDate finDuMoisCourant = LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1);
        assertEquals(finDuMoisCourant, relue.getDernierePeriodeFin(), "la période facturée est le mois courant");
        assertEquals(ExecutionStatut.SUCCESS, relue.getDernierStatut());
        assertNotNull(relue.getDerniereExecution());
        assertNull(relue.getDernierMessage());
        // La période suivante court du 1er au dernier jour du mois prochain ; l'exécution a lieu
        // le lendemain de sa fin, soit le 1er du mois d'après.
        LocalDate finDuMoisProchain = finDuMoisCourant.plusDays(1).withDayOfMonth(1).plusMonths(1).minusDays(1);
        assertEquals(finDuMoisProchain.plusDays(1).atTime(7, 0), relue.getProchaineExecution());
    }

    @Test
    @DisplayName("Chaque exécution laisse une trace dans l'historique")
    void historique() {
        TiersPayant organisme = organismeEligible("HISTO", Periodicite.MENSUEL);
        dossier(compte(organisme), 20_000, LocalDate.now().withDayOfMonth(4));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();
        FactureEditionResponse reponse = services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        List<HistoriquePlanificationDto> historique = services.planificationFacturationService
            .getHistorique(plan.getId(), page())
            .getContent();

        assertEquals(1, historique.size());
        HistoriquePlanificationDto trace = historique.getFirst();
        assertEquals(ExecutionStatut.SUCCESS, trace.statut());
        assertEquals(reponse.generationCode(), trace.generationCode());
        assertNotNull(trace.executionDebut());
        assertNotNull(trace.executionFin());
    }

    @Test
    @DisplayName("L'historique d'une planification ne rend que ses propres exécutions")
    void historiqueParPlanification() {
        TiersPayant organisme = organismeEligible("HISTO 2", Periodicite.MENSUEL);
        dossier(compte(organisme), 20_000, LocalDate.now().withDayOfMonth(4));
        PlanificationFacturation mensuelle = planificationDuMoisCourant();
        PlanificationFacturation bimensuelle = planificationDeReference(Periodicite.BIMENSUEL);
        viderLeCache();
        services.planificationFacturationService.executerMaintenant(mensuelle.getId());
        viderLeCache();

        assertEquals(1, services.planificationFacturationService.getHistorique(mensuelle.getId(), page()).getTotalElements());
        assertEquals(0, services.planificationFacturationService.getHistorique(bimensuelle.getId(), page()).getTotalElements());
    }

    @Test
    @DisplayName("Une exécution sans dossier à facturer est consignée en échec")
    void executionSansDossier() {
        organismeEligible("VIDE PLAN", Periodicite.MENSUEL);
        PlanificationFacturation plan = planificationDuMoisCourant();
        LocalDate periodeAvant = plan.getDernierePeriodeFin();
        viderLeCache();

        FactureEditionResponse reponse = services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        assertNull(reponse.generationCode());
        PlanificationFacturation relue = em.find(PlanificationFacturation.class, plan.getId());
        assertEquals(ExecutionStatut.ECHEC, relue.getDernierStatut(), "aucun dossier facturable remonte comme un échec");
        assertEquals(periodeAvant, relue.getDernierePeriodeFin(), "la période n'avance pas tant que rien n'a été facturé");
        assertEquals(1, compter("SELECT count(*) FROM historique_planification WHERE statut = 'ECHEC'"));
    }

    @Test
    @DisplayName("Un organisme exclu de la facturation automatique n'est pas facturé")
    void organismeExclu() {
        TiersPayant exclu = organismeEligible("EXCLU PLAN", Periodicite.MENSUEL);
        exclu.setInclureFacturationAutoDefinitive(false);
        dossier(compte(exclu), 25_000, LocalDate.now().withDayOfMonth(6));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();

        FactureEditionResponse reponse = services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        assertNull(reponse.generationCode());
        assertEquals(0, compter("SELECT count(*) FROM facture_tiers_payant"));
    }

    @Test
    @DisplayName("Un organisme inactif est laissé de côté même s'il est coché")
    void organismeInactif() {
        TiersPayant inactif = organismeEligible("INACTIF PLAN", Periodicite.MENSUEL);
        inactif.setStatut(com.kobe.warehouse.domain.enumeration.TiersPayantStatut.DISABLED);
        dossier(compte(inactif), 25_000, LocalDate.now().withDayOfMonth(6));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();

        FactureEditionResponse reponse = services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        assertNull(reponse.generationCode());
        assertEquals(0, compter("SELECT count(*) FROM facture_tiers_payant"));
    }

    @Test
    @DisplayName("Un organisme d'une autre périodicité est laissé de côté")
    void autrePeriodicite() {
        TiersPayant mensuel = organismeEligible("MENSUEL SEUL", Periodicite.MENSUEL);
        TiersPayant hebdomadaire = organismeEligible("HEBDO SEUL", Periodicite.HEBDOMADAIRE);
        dossier(compte(mensuel), 10_000, LocalDate.now().withDayOfMonth(7));
        dossier(compte(hebdomadaire), 90_000, LocalDate.now().withDayOfMonth(7));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();

        FactureEditionResponse reponse = services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        List<FactureTiersPayant> factures = facturesDe(reponse.generationCode());
        assertEquals(1, factures.size());
        assertEquals(mensuel.getId(), factures.getFirst().getTiersPayant().getId());
    }

    @Test
    @DisplayName("Les organismes réunis en groupe sont facturés ensemble, les autres à part")
    void facturationGroupeeEtIndividuelle() {
        GroupeTiersPayant groupe = groupeEligible("GROUPE PLAN", Periodicite.MENSUEL);
        TiersPayant adherent = organismeEligible("ADHERENT PLAN", Periodicite.MENSUEL, groupe);
        TiersPayant isole = organismeEligible("ISOLE PLAN", Periodicite.MENSUEL);
        dossier(compte(adherent), 30_000, LocalDate.now().withDayOfMonth(8));
        dossier(compte(isole), 40_000, LocalDate.now().withDayOfMonth(8));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();

        services.planificationFacturationService.executerMaintenant(plan.getId());
        viderLeCache();

        assertEquals(
            1,
            compter("SELECT count(*) FROM facture_tiers_payant WHERE groupe_tiers_payant_id IS NOT NULL"),
            "une facture porteuse pour le groupe"
        );
        assertEquals(
            1,
            compter("SELECT count(*) FROM facture_tiers_payant WHERE groupe_facture_tiers_payant_id IS NOT NULL"),
            "l'adhérent est rattaché à la porteuse"
        );
        assertEquals(3, compter("SELECT count(*) FROM facture_tiers_payant"), "la porteuse, sa fille, et l'organisme isolé");
    }

    @Test
    @DisplayName("Le scheduler avance l'échéance même quand l'exécution échoue")
    void executionParLeScheduler() {
        organismeEligible("SCHEDULER", Periodicite.MENSUEL);
        PlanificationFacturation plan = planificationDeReference(Periodicite.MENSUEL);
        plan.setDernierePeriodeFin(LocalDate.of(2026, 1, 31));
        plan.setHeureDeclenchement(LocalTime.of(7, 0));
        viderLeCache();

        services.planificationFacturationService.executerPlanificationScheduled(
            em.find(PlanificationFacturation.class, plan.getId())
        );
        viderLeCache();

        PlanificationFacturation relue = em.find(PlanificationFacturation.class, plan.getId());
        assertEquals(ExecutionStatut.ECHEC, relue.getDernierStatut(), "aucun dossier sur février 2026");
        assertEquals(
            LocalDate.of(2026, 3, 1).atTime(7, 0),
            relue.getProchaineExecution(),
            "l'échéance avance quand même : sans cela le planificateur rejouerait sans fin"
        );
        assertEquals(1, compter("SELECT count(*) FROM historique_planification"));
    }

    @Test
    @DisplayName("Le scheduler enregistre le succès et la période facturée")
    void succesParLeScheduler() {
        TiersPayant organisme = organismeEligible("SCHEDULER OK", Periodicite.MENSUEL);
        dossier(compte(organisme), 15_000, LocalDate.now().withDayOfMonth(9));
        PlanificationFacturation plan = planificationDuMoisCourant();
        viderLeCache();

        services.planificationFacturationService.executerPlanificationScheduled(
            em.find(PlanificationFacturation.class, plan.getId())
        );
        viderLeCache();

        PlanificationFacturation relue = em.find(PlanificationFacturation.class, plan.getId());
        assertEquals(ExecutionStatut.SUCCESS, relue.getDernierStatut());
        assertEquals(LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1), relue.getDernierePeriodeFin());
        assertEquals(1, compter("SELECT count(*) FROM historique_planification WHERE statut = 'SUCCESS'"));
    }

    // ===== outils =====

    private Pageable page() {
        return PageRequest.of(0, 20);
    }

    private PlanificationDto planification(String libelle, Periodicite periodicite, LocalDate dernierePeriodeFin) {
        return new PlanificationDto(
            null,
            libelle,
            periodicite,
            LocalTime.of(7, 0),
            false,
            true,
            dernierePeriodeFin,
            null,
            null,
            null,
            null,
            0L
        );
    }

    /** La planification définitive livrée par les migrations pour cette périodicité. */
    private PlanificationFacturation planificationDeReference(Periodicite periodicite) {
        return em
            .createQuery(
                "SELECT p FROM PlanificationFacturation p WHERE p.periodicite = :p AND p.factureProvisoire = false",
                PlanificationFacturation.class
            )
            .setParameter("p", periodicite)
            .getSingleResult();
    }

    /**
     * La planification mensuelle calée sur le mois en cours : sa dernière période s'arrête la
     * veille du 1er, la prochaine à facturer est donc le mois courant, celui des dossiers du test.
     */
    private PlanificationFacturation planificationDuMoisCourant() {
        PlanificationFacturation plan = planificationDeReference(Periodicite.MENSUEL);
        plan.setDernierePeriodeFin(LocalDate.now().withDayOfMonth(1).minusDays(1));
        plan.setHeureDeclenchement(LocalTime.of(7, 0));
        em.flush();
        return plan;
    }

    private TiersPayant organismeEligible(String nom, Periodicite periodicite) {
        return organismeEligible(nom, periodicite, null);
    }

    /** Un organisme que la facturation automatique retiendra : périodicité, case cochée, actif. */
    private TiersPayant organismeEligible(String nom, Periodicite periodicite, GroupeTiersPayant groupe) {
        TiersPayant organisme = tiersPayant(nom, TiersPayantCategorie.ASSURANCE, groupe);
        organisme.setPeriodiciteFactureDefinitive(periodicite);
        organisme.setInclureFacturationAutoDefinitive(true);
        em.flush();
        return organisme;
    }

    private GroupeTiersPayant groupeEligible(String nom, Periodicite periodicite) {
        GroupeTiersPayant groupe = groupe(nom);
        groupe.setPeriodiciteFactureDefinitive(periodicite);
        groupe.setInclureFacturationAutoDefinitive(true);
        em.flush();
        return groupe;
    }
}
