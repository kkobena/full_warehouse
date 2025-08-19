package com.kobe.warehouse.service.produit_prix.service;

import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PrixRererenceService {
    Optional<OptionPrixProduit> findOneActifByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);

    Optional<PrixReferenceDTO> findActif(Long produitId, Long tiersPayantId);

    Optional<PrixReferenceDTO> findById(Long id);

    List<OptionPrixProduit> findAllByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);

    void add(PrixReferenceDTO dto);

    void update(PrixReferenceDTO dto);

    void delete(Long id);

    List<PrixReferenceDTO> findAllByProduitIddAndTiersPayantId(Long produitId, Long tiersPayantId);

    List<PrixReferenceDTO> findAllByProduitId(Long produitId);

    int getSaleLineUnitPrice(OptionPrixProduit optionPrixProduit, int incomingPrice);

    List<OptionPrixProduit> findByProduitIdAndTiersPayantIds(Long produitId, Set<Long> tiersPayantIds);

    void save(OptionPrixProduit optionPrixProduit);

    int getSaleLineTotalAmount(OptionPrixProduit optionPrixProduit, int incomingPrice);
}
