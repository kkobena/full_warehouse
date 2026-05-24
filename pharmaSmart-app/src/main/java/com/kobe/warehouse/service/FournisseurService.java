package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.service.dto.FournisseurDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.Fournisseur}.
 */
public interface FournisseurService {
    FournisseurDTO save(FournisseurDTO fournisseurDTO);

    Page<FournisseurDTO> findAll(String search, Pageable pageable);

    Optional<FournisseurDTO> findOne(Integer id);

    void delete(Integer id);

    ResponseDTO importation(InputStream inputStream);

    Fournisseur findOneById(Integer id);

    /** Fournisseurs principaux (parent == null) */
    List<FournisseurDTO> findParents();

    /** Fournisseurs principaux avec pagination et recherche. */
    Page<FournisseurDTO> findParents(String search, Pageable pageable);

    /** Agences d'un fournisseur principal. */
    List<FournisseurDTO> findAgences(Integer parentId);
   Optional<Fournisseur>  getParentByChildId(Integer childId);
    Optional<Integer>  getParentIdByChildId(Integer childId);
}
