package com.kobe.warehouse.service.referential;


import com.kobe.warehouse.service.dto.TableauDTO;
import java.util.List;
import java.util.Optional;

public interface TableauService {

    /**
     * Save a tableau.
     *
     * @param tableauDTO the entity to save.
     * @return the persisted entity.
     */
    TableauDTO save(TableauDTO tableauDTO);

    /**
     * Get all the formProduits.
     *

     * @return the list of entities.
     */
    List<TableauDTO> findAll();


    /**
     * Get the "id" formProduit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TableauDTO> findOne(Long id);

    /**
     * Delete the "id" formProduit.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
