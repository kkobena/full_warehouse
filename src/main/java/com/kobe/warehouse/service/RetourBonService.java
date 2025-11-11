package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
     * Update a retour bon.
     *
     * @param retourBonDTO the entity to update.
     * @return the persisted entity.
     */
    RetourBonDTO update(RetourBonDTO retourBonDTO);

    /**
     * Get all retour bons.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<RetourBonDTO> findAll(Pageable pageable);

    /**
     * Get all retour bons by status.
     *
     * @param statut   the status filter.
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<RetourBonDTO> findAllByStatut(RetourStatut statut, Pageable pageable);

    /**
     * Get retour bons by commande ID.
     *
     * @param commandeId the commande ID.
     * @param orderDate  the order date.
     * @return the list of entities.
     */
    List<RetourBonDTO> findAllByCommande(Integer commandeId, LocalDate orderDate);

    /**
     * Get retour bons within date range.
     *
     * @param startDate the start date.
     * @param endDate   the end date.
     * @param pageable  the pagination information.
     * @return the list of entities.
     */
    Page<RetourBonDTO> findAllByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Get the retour bon by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<RetourBonDTO> findOne(Integer id);

    /**
     * Delete the retour bon by id.
     *
     * @param id the id of the entity.
     */
    void delete(Integer id);

    /**
     * Validate a retour bon (change status to VALIDATED).
     *
     * @param id the id of the entity.
     * @return the updated entity.
     */
    RetourBonDTO validate(Integer id);
}
