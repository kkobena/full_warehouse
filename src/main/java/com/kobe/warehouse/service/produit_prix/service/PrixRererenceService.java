package com.kobe.warehouse.service.produit_prix.service;

import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PrixRererenceService {
    Optional<OptionPrixProduit> findOneActifByProduitIdAndTiersPayantId(Integer produitId, Integer tiersPayantId);

    Optional<PrixReferenceDTO> findActif(Integer produitId, Integer tiersPayantId);

    Optional<PrixReferenceDTO> findById(Integer id);

    List<OptionPrixProduit> findAllByProduitIdAndTiersPayantId(Integer produitId, Integer tiersPayantId);

    void add(PrixReferenceDTO dto);

    void update(PrixReferenceDTO dto);

    void delete(Integer id);

    List<PrixReferenceDTO> findAllByProduitIddAndTiersPayantId(Integer produitId, Integer tiersPayantId);

    List<PrixReferenceDTO> findAllByProduitId(Integer produitId);

    List<OptionPrixProduit> findByProduitIdAndTiersPayantIds(Integer produitId, Set<Integer> tiersPayantIds);

    void save(OptionPrixProduit optionPrixProduit);
}
