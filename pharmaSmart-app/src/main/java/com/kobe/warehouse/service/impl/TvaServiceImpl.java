package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.repository.TvaRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.TvaService;
import com.kobe.warehouse.service.dto.TvaDTO;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Tva}.
 */
@Service
@Transactional
public class TvaServiceImpl implements TvaService {

    private final Logger log = LoggerFactory.getLogger(TvaServiceImpl.class);

    private final TvaRepository tvaRepository;
    private final ProduitRepository produitRepository;

    public TvaServiceImpl(TvaRepository tvaRepository, ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
        this.tvaRepository = tvaRepository;
    }

    /**
     * Save a tva.
     *
     * @param tvaDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public TvaDTO save(TvaDTO tvaDTO) {
        Tva tva = new Tva();
        tva.setTaux(tvaDTO.getTaux());
        try {
            tva = tvaRepository.save(tva);
        } catch (Exception e) {
            log.error("Error save Tva : ", e);
            throw new RuntimeException(e);
        }

        return new TvaDTO(tva);
    }

    /**
     * Get all the tvas.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TvaDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Tvas");
        return tvaRepository.findAll(pageable).map(TvaDTO::new);
    }

    /**
     * Get one tva by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<TvaDTO> findOne(Integer id) {
        log.debug("Request to get Tva : {}", id);
        return tvaRepository.findById(id).map(TvaDTO::new);
    }

    /**
     * Delete the tva by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Integer id) {
        // Un référentiel encore porté par des fiches ne peut pas disparaître : la
        // requête échouerait sur la contrainte de clé étrangère, en HTTP 500 et sans
        // rien dire d'utile. On compte d'abord, et l'on explique.
        long rattaches = this.produitRepository.countByTvaId(id);
        if (rattaches > 0) {
            throw new GenericError(
                "Le taux de TVA est encore utilisé par %d produit(s) : suppression impossible.".formatted(rattaches),
                "referentielUtilise"
            );
        }
        log.debug("Request to delete Tva : {}", id);

        tvaRepository.deleteById(id);
    }
}
