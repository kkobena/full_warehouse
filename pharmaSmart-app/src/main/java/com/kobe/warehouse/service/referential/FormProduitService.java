package com.kobe.warehouse.service.referential;

import com.kobe.warehouse.service.dto.FormProduitDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FormProduitService {
    /**
     * Save a formProduit.
     *
     * @param formProduitDTO the entity to save.
     * @return the persisted entity.
     */
    FormProduitDTO save(FormProduitDTO formProduitDTO);

    /**
     * Get all the formProduits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<FormProduitDTO> findAll(Pageable pageable);

    /**
     * Get the "id" formProduit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<FormProduitDTO> findOne(Integer id);

    /**
     * Delete the "id" formProduit.
     *
     * @param id the id of the entity.
     */
    void delete(Integer id);
}
