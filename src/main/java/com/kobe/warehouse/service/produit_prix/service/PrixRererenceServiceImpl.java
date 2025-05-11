package com.kobe.warehouse.service.produit_prix.service;

import com.kobe.warehouse.domain.PrixReference;
import com.kobe.warehouse.domain.PrixReferenceType;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.repository.PrixReferenceRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<PrixReference> findByProduitIdAndTiersPayantIds(Long produitId, Set<Long> tiersPayantIds) {
        return this.prixReferenceRepository.findByProduitIdAndTiersPayantIds(produitId, tiersPayantIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrixReference> findOneActifByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId) {
        return this.prixReferenceRepository.findOneActifByProduitIdAndTiersPayantId(produitId, tiersPayantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrixReferenceDTO> findActif(Long produitId, Long tiersPayantId) {
        return this.findOneActifByProduitIdAndTiersPayantId(produitId, tiersPayantId)
            .map(prixReference -> {
                PrixReferenceDTO dto = new PrixReferenceDTO();
                dto.setId(prixReference.getId());
                dto.setValeur(prixReference.getValeur());
                dto.setEnabled(prixReference.isEnabled());
                dto.setType(prixReference.getType());
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
    public List<PrixReference> findAllByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId) {
        return this.prixReferenceRepository.findAllByProduitIdAndTiersPayantId(produitId, tiersPayantId);
    }

    @Override
    public void add(PrixReferenceDTO dto) {
        PrixReference prixReference = new PrixReference();
        prixReference.setProduit(new Produit().id(dto.getProduitId()));
        prixReference.setTiersPayant(new TiersPayant().setId(dto.getTiersPayantId()));
        prixReference.setValeur(dto.getValeur());
        prixReference.setEnabled(dto.isEnabled());
        prixReference.setType(dto.getType());
        prixReference.setUser(this.userService.getUser());
        this.prixReferenceRepository.save(prixReference);
    }

    @Override
    public void update(PrixReferenceDTO dto) {
        this.prixReferenceRepository.findById(dto.getId()).ifPresent(prixReference -> {
                prixReference.setValeur(dto.getValeur());
                prixReference.setEnabled(dto.isEnabled());
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
    @Transactional(readOnly = true)
    public int getSaleLineUnitPrice(PrixReference prixReference, int incomingPrice) {
        if (prixReference == null) {
            return incomingPrice;
        }
        if (prixReference.getType() == PrixReferenceType.POURCENTAGE) {
            return Math.round(incomingPrice * prixReference.getTaux());
        } else {
            return prixReference.getValeur();
        }
    }

    @Override
    public void save(PrixReference prixReference) {
        this.prixReferenceRepository.save(prixReference);
    }

    @Override
    public List<PrixReferenceDTO> findAllByProduitId(Long produitId) {
        return this.prixReferenceRepository.findAllByProduitId(produitId)
            .stream()
            .map(prixReference -> {
                PrixReferenceDTO dto = new PrixReferenceDTO();
                dto.setId(prixReference.getId());
                dto.setValeur(prixReference.getValeur());
                dto.setEnabled(prixReference.isEnabled());
                dto.setType(prixReference.getType());
                TiersPayant tiersPayant = prixReference.getTiersPayant();
                dto.setTiersPayantId(tiersPayant.getId());
                dto.setTiersPayantName(tiersPayant.getFullName());
                return dto;
            })
            .toList();
    }
}
