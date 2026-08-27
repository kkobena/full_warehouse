package com.kobe.warehouse.service.dci.service;

import com.kobe.warehouse.service.dci.dto.DciDTO;
import com.kobe.warehouse.service.dci.dto.DciProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DciService {
    Page<DciDTO> findAll(String search, Pageable pageable);

    Optional<DciDTO> findOne(Integer id);

    DciDTO create(DciDTO dto);

    DciDTO update(DciDTO dto);

    void delete(Integer id);

    /** Produits portant cette substance active, pour le détail de l'écran. */
    List<DciProduitDTO> findProduits(Integer dciId);

    /**
     * Importe un fichier CSV « code;libelle ».
     *
     * <p>Le libellé est obligatoire ; le code est facultatif et sera dérivé du libellé lorsqu'il
     * est absent.
     */
    ResponseDTO importation(InputStream inputStream);
}
