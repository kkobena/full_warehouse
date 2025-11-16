package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.ReponseRetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
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
     * Get all retour bons.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<RetourBonDTO> findAll(Pageable pageable);

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
}
