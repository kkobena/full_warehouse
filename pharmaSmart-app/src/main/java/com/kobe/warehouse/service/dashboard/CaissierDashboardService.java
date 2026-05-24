package com.kobe.warehouse.service.dashboard;

import com.kobe.warehouse.service.dto.dashboard.CaissierDashboardDTO;
import com.kobe.warehouse.service.dto.dashboard.CaisseStatusDTO;
import com.kobe.warehouse.service.dto.dashboard.LivraisonAttendueDTO;
import com.kobe.warehouse.service.dto.dashboard.ResumeDifferesDTO;
import com.kobe.warehouse.service.dto.dashboard.SessionEncaissementsDTO;
import com.kobe.warehouse.service.dto.dashboard.VenteRecenteDTO;

import java.util.List;

/**
 * Service du dashboard préparateur en pharmacie.
 * Toutes les méthodes filtrent sur l'utilisateur connecté via SecurityUtils.
 */
public interface CaissierDashboardService {

    /**
     * Retourne toutes les données du dashboard en un seul appel.
     * Filtré sur l'utilisateur connecté (login courant).
     */
    CaissierDashboardDTO getDashboardData();

    /**
     * État de la caisse de l'utilisateur connecté pour aujourd'hui.
     * Expose : fondOuverture, encaissementsEspeces, especesTheoriques.
     * N'expose PAS l'écart (réservé au manager).
     */
    CaisseStatusDTO getCaisseStatus();

    /**
     * Encaissements de la session courante filtrés sur le cash_register_id
     * de l'utilisateur connecté.
     */
    SessionEncaissementsDTO getSessionEncaissements();

    /**
     * Différés dont sale_date <= aujourd'hui et rest_to_pay > 0.
     */
    ResumeDifferesDTO getDifferesRelance();

    /**
     * Commandes fournisseurs avec order_status=REQUESTED et order_date=aujourd'hui.
     */
    List<LivraisonAttendueDTO> getLivraisonsJour();

    /**
     * Dernières ventes de la session courante (filtrées sur caissier_id du connecté).
     *
     * @param limit nombre max de résultats (défaut : 8)
     */
    List<VenteRecenteDTO> getVentesRecentes(Integer limit);
}
