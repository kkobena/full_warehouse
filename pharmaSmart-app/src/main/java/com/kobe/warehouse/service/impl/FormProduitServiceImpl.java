package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.FormProduit;
import com.kobe.warehouse.repository.FormProduitRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.dto.FormProduitDTO;
import com.kobe.warehouse.service.referential.FormProduitService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FormProduitServiceImpl implements FormProduitService {

    private final Logger log = LoggerFactory.getLogger(FormProduitServiceImpl.class);

    private final FormProduitRepository formProduitRepository;
    private final ProduitRepository produitRepository;

    public FormProduitServiceImpl(FormProduitRepository formProduitRepository, ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
        this.formProduitRepository = formProduitRepository;
    }

    /**
     * Save a formProduit.
     *
     * @param formProduitDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public FormProduitDTO save(FormProduitDTO formProduitDTO) {
        log.debug("Request to save FormProduit : {}", formProduitDTO);
        FormProduit formProduit = new FormProduit().libelle(formProduitDTO.getLibelle());
        formProduit.setId(formProduitDTO.getId());
        formProduit = formProduitRepository.save(formProduit);
        return new FormProduitDTO(formProduit);
    }

    /**
     * Get all the formProduits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FormProduitDTO> findAll(Pageable pageable) {
        log.debug("Request to get all FormProduits");
        return formProduitRepository.findAll(pageable).map(FormProduitDTO::new);
    }

    /**
     * Get one formProduit by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<FormProduitDTO> findOne(Integer id) {
        log.debug("Request to get FormProduit : {}", id);
        return formProduitRepository.findById(id).map(FormProduitDTO::new);
    }

    /**
     * Delete the formProduit by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Integer id) {
        // Un référentiel encore porté par des fiches ne peut pas disparaître : la
        // requête échouerait sur la contrainte de clé étrangère, en HTTP 500 et sans
        // rien dire d'utile. On compte d'abord, et l'on explique.
        long rattaches = this.produitRepository.countByFormeId(id);
        if (rattaches > 0) {
            throw new GenericError(
                "La forme est encore utilisée par %d produit(s) : suppression impossible.".formatted(rattaches),
                "referentielUtilise"
            );
        }
        log.debug("Request to delete FormProduit : {}", id);
        formProduitRepository.deleteById(id);
    }
}
