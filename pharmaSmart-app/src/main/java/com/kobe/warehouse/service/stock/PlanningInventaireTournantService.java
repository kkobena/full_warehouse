package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.records.PlanningInventaireTournantRecord;
import com.kobe.warehouse.service.dto.records.TournantDashboardRecord;
import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des plannings d'inventaire tournant (Cycle Counting).
 */
public interface PlanningInventaireTournantService {

    /**
     * Créer un nouveau planning.
     */
    PlanningInventaireTournantRecord create(PlanningInventaireTournantRecord record);

    /**
     * Mettre à jour un planning existant.
     */
    PlanningInventaireTournantRecord update(PlanningInventaireTournantRecord record);

    /**
     * Supprimer un planning.
     */
    void delete(Integer id);

    /**
     * Récupérer un planning par id.
     */
    Optional<PlanningInventaireTournantRecord> findById(Integer id);

    /**
     * Lister tous les plannings, triés par prochaine exécution.
     */
    List<PlanningInventaireTournantRecord> findAll();

    /**
     * Activer/désactiver un planning.
     */
    PlanningInventaireTournantRecord toggleActif(Integer id);

    /**
     * Exécuter manuellement un planning : crée immédiatement l'inventaire tournant et avance la
     * rotation.
     *
     * @return l'id du {@link com.kobe.warehouse.domain.StoreInventory} créé
     */
    Long executerManuellement(Integer planningId);

    /**
     * Données du dashboard inventaire tournant pour un storage donné.
     */
    TournantDashboardRecord getDashboard(Integer storageId);

    List<Long> executerTournantsEchus();
}
