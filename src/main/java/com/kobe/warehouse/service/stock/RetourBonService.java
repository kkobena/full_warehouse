package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.dto.ReponseRetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonBatchResultDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonFromLotRequest;
import com.kobe.warehouse.service.dto.RetourBonFromLotsRequest;
import com.kobe.warehouse.service.dto.RetourBonLotResolutionDTO;
import com.kobe.warehouse.service.dto.RetourBonGroupeDTO;
import com.kobe.warehouse.service.dto.RetourCompletCommandeRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.RetourBon}.
 */
public interface RetourBonService {
    /**
     * Create a new retour bon.
     *
     * @param retourBonDTO the entity to create.
     * @return the persisted entity.
     */
    RetourBonDTO create(RetourBonDTO retourBonDTO);

    /**
     * Get all retour bons with optional filters.
     *
     * @param statut   optional status filter.
     * @param dtStart  optional start date filter.
     * @param dtEnd    optional end date filter.
     * @param search   optional text search on fournisseur / reference.
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<RetourBonDTO> findAll(RetourStatut statut, RetourStatut excludeStatut, LocalDate dtStart, LocalDate dtEnd, String search, Pageable pageable);

    /**
     * Get retour bons by commande ID.
     *
     * @param commandeId the commande ID.
     * @param orderDate  the order date.
     * @return the list of entities.
     */
    List<RetourBonDTO> findAllByCommande(Integer commandeId, LocalDate orderDate);

    /**
     * Get the retour bon by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<RetourBonDTO> findOne(Integer id);

    /**
     * Create a supplier response for a retour bon.
     *
     * @param reponseRetourBonDTO the supplier response to create.
     * @return the persisted entity.
     */
    ReponseRetourBonDTO createSupplierResponse(ReponseRetourBonDTO reponseRetourBonDTO);

    /**
     * Update an existing retour bon (only if statut = VALIDATED).
     * Reverses stock of old items and recreates new ones.
     *
     * @param retourBonDTO the updated entity.
     * @return the updated entity.
     */
    RetourBonDTO update(RetourBonDTO retourBonDTO);

    /**
     * Delete a retour bon (only if statut = VALIDATED).
     * Reverses all stock movements before deletion.
     *
     * @param id the id of the entity to delete.
     */
    void delete(Integer id);

    /**
     * Mark a retour bon as processing (sent to supplier).
     *
     * @param id the id of the retour bon.
     * @return the updated entity.
     */
    RetourBonDTO markAsProcessing(Integer id);

    byte[] export(Integer id);

    /**
     * Crée un RetourBon depuis un lot périmé.
     * Résout automatiquement la Commande source via Lot → OrderLine → Commande.
     *
     * @param request la requête contenant lotId, motifRetourId et quantity.
     * @return le RetourBonDTO créé.
     * @throws com.kobe.warehouse.service.errors.RetourBonCommandeNotFoundException
     *         si le lot n'est pas rattaché à une commande de réception.
     */
    RetourBonDTO createFromExpiredLot(RetourBonFromLotRequest request);

    /**
     * Crée plusieurs RetourBon depuis une liste de lots périmés (batch).
     *
     *
     * @param request la requête batch contenant la liste de lots.
     * @return le résultat batch avec les RetourBon créés et les erreurs.
     */
    RetourBonBatchResultDTO createFromExpiredLots(RetourBonFromLotsRequest request);

    /**
     * Pré-résout un lot pour déterminer l'état du formulaire "Retour fournisseur" :
     * COMMANDE_TROUVEE / HORS_COMMANDE_UN_FOURN / HORS_COMMANDE_MULTI / FOURNISSEUR_INCONNU.
     *
     * @param lotId l'identifiant du lot.
     * @return le DTO de résolution.
     */
    RetourBonLotResolutionDTO resolveLot(Integer lotId);

    /**
     * Clôture manuellement un retour partiellement accepté.
     *
     * @param id the id of the retour bon.
     * @return the updated entity.
     */
    RetourBonDTO closeManually(Integer id);

    /**
     * Crée un RetourBon couvrant toutes les lignes d'une commande reçue.
     *
     * @param request la requête contenant commandeId, commandeOrderDate, motifRetourId.
     * @return le RetourBonDTO créé.
     */
    RetourBonDTO createRetourCompletFromCommande(RetourCompletCommandeRequest request);

    /**
     * Retourne les retours non clôturés regroupés par fournisseur.
     */
    List<RetourBonGroupeDTO> findAllGroupedByFournisseur();

    /**
     * Génère un PDF bordereau regroupant plusieurs retours bons pour un fournisseur.
     *
     * @param ids liste des identifiants des RetourBon à inclure.
     * @return PDF en bytes.
     */
    byte[] exportGroupe(List<Integer> ids);
}
