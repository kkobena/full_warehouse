package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.RetourDepotDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.RetourDepot}.
 */
public interface RetourDepotService {
    /**
     * Create a new retour depot.
     *
     * @param retourDepotDTO the entity to create.
     * @return the persisted entity.
     */
    RetourDepotDTO create(RetourDepotDTO retourDepotDTO);


    /**
     * Get all retour depots by date range.
     *
     * @param dtStart  the start date.
     * @param dtEnd    the end date.
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<RetourDepotDTO> findAllByDateRange(Integer depotId, LocalDate dtStart, LocalDate dtEnd, Pageable pageable);


    Optional<RetourDepotDTO> findOne(Integer id);


}
