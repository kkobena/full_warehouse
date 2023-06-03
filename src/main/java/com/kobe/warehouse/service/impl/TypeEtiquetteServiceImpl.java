package com.kobe.warehouse.service.impl;


import com.kobe.warehouse.domain.TypeEtiquette;
import com.kobe.warehouse.repository.TypeEtiquetteRepository;
import com.kobe.warehouse.service.dto.TypeEtiquetteDTO;
import com.kobe.warehouse.service.referential.TypeEtiquetteService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link TypeEtiquette}.
 */
@Service
@Transactional
public class TypeEtiquetteServiceImpl implements TypeEtiquetteService {
    private final Logger log = LoggerFactory.getLogger(TypeEtiquetteServiceImpl.class);
    private final TypeEtiquetteRepository typeEtiquetteRepository;

    public TypeEtiquetteServiceImpl(TypeEtiquetteRepository typeEtiquetteRepository) {
        this.typeEtiquetteRepository = typeEtiquetteRepository;

    }

    /**
     * Save a typeEtiquette.
     *
     * @param typeEtiquetteDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public TypeEtiquetteDTO save(TypeEtiquetteDTO typeEtiquetteDTO) {
        log.debug("Request to save TypeEtiquette : {}", typeEtiquetteDTO);
        TypeEtiquette typeEtiquette = new TypeEtiquette().setId(typeEtiquetteDTO.getId())
            .setLibelle(typeEtiquetteDTO.getLibelle());
        typeEtiquette = typeEtiquetteRepository.save(typeEtiquette);
        return new TypeEtiquetteDTO(typeEtiquette);
    }

    /**
     * Get all the typeEtiquettes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TypeEtiquetteDTO> findAll(Pageable pageable) {
        log.debug("Request to get all TypeEtiquettes");
        return typeEtiquetteRepository.findAll(pageable)
            .map(TypeEtiquetteDTO::new);
    }


    /**
     * Get one typeEtiquette by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<TypeEtiquetteDTO> findOne(Long id) {
        log.debug("Request to get TypeEtiquette : {}", id);
        return typeEtiquetteRepository.findById(id)
            .map(TypeEtiquetteDTO::new);
    }

    /**
     * Delete the typeEtiquette by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete TypeEtiquette : {}", id);
        typeEtiquetteRepository.deleteById(id);
    }
}
