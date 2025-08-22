package com.kobe.warehouse.service.produit_prix.service;

import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import com.kobe.warehouse.repository.PrixReferenceRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class PrixRererenceServiceImpl implements PrixRererenceService {

    private final PrixReferenceRepository prixReferenceRepository;
    private final UserService userService;

    public PrixRererenceServiceImpl(PrixReferenceRepository prixReferenceRepository, UserService userService) {
        this.prixReferenceRepository = prixReferenceRepository;
        this.userService = userService;
    }

    @Override
    public List<OptionPrixProduit> findByProduitIdAndTiersPayantIds(Long produitId, Set<Long> tiersPayantIds) {
        return this.prixReferenceRepository.findByProduitIdAndTiersPayantIds(produitId, tiersPayantIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OptionPrixProduit> findOneActifByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId) {
        return this.prixReferenceRepository.findOneActifByProduitIdAndTiersPayantId(produitId, tiersPayantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrixReferenceDTO> findActif(Long produitId, Long tiersPayantId) {
        return this.findOneActifByProduitIdAndTiersPayantId(produitId, tiersPayantId)
            .map(prixReference -> {
                PrixReferenceDTO dto = new PrixReferenceDTO();
                dto.setId(prixReference.getId());
                dto.setPrice(prixReference.getPrice());
                dto.setRate(prixReference.getRate());
                dto.setEnabled(prixReference.isEnabled());
                dto.setType(prixReference.getType());
                dto.setProduitId(produitId);
                return Optional.of(dto);
            })
            .orElse(Optional.empty());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrixReferenceDTO> findById(Long id) {
        return this.prixReferenceRepository.findById(id).map(PrixReferenceDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionPrixProduit> findAllByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId) {
        return this.prixReferenceRepository.findAllByProduitIdAndTiersPayantId(produitId, tiersPayantId);
    }

    @Override
    public void add(PrixReferenceDTO dto) {
        OptionPrixProduit optionPrixProduit = new OptionPrixProduit();
        optionPrixProduit.setProduit(new Produit().id(dto.getProduitId()));
        optionPrixProduit.setTiersPayant(new TiersPayant().setId(dto.getTiersPayantId()));
        optionPrixProduit.setPrice(dto.getPrice());
        optionPrixProduit.setRate(dto.getRate());
        optionPrixProduit.setEnabled(dto.isEnabled());
        optionPrixProduit.setType(dto.getType());
        optionPrixProduit.setUser(this.userService.getUser());
        this.prixReferenceRepository.save(optionPrixProduit);
    }

    @Override
    public void update(PrixReferenceDTO dto) {
        this.prixReferenceRepository.findById(dto.getId()).ifPresent(prixReference -> {
            prixReference.setPrice(dto.getPrice());
            prixReference.setEnabled(dto.isEnabled());
            prixReference.setRate(dto.getRate());
            prixReference.setType(dto.getType());
            this.prixReferenceRepository.save(prixReference);
        });
    }

    @Override
    public void delete(Long id) {
        this.prixReferenceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrixReferenceDTO> findAllByProduitIddAndTiersPayantId(Long produitId, Long tiersPayantId) {
        return this.findAllByProduitIdAndTiersPayantId(produitId, tiersPayantId).stream().map(PrixReferenceDTO::new).toList();
    }


    @Override
    public void save(OptionPrixProduit optionPrixProduit) {
        this.prixReferenceRepository.save(optionPrixProduit);
    }

    @Override
    public List<PrixReferenceDTO> findAllByProduitId(Long produitId) {
        return this.prixReferenceRepository.findAllByProduitId(produitId)
            .stream()
            .map(prixReference -> {
                PrixReferenceDTO dto = new PrixReferenceDTO();
                dto.setId(prixReference.getId());
                dto.setPrice(prixReference.getPrice());
                dto.setRate(prixReference.getRate());
                dto.setEnabled(prixReference.isEnabled());
                OptionPrixType type = prixReference.getType();
                dto.setTypeLibelle(type.getLibelle());
                dto.setProduitId(produitId);
                dto.setType(type);
                TiersPayant tiersPayant = prixReference.getTiersPayant();
                dto.setTiersPayantId(tiersPayant.getId());
                dto.setTiersPayantName(tiersPayant.getFullName());

                return dto;
            })
            .toList();
    }
}
