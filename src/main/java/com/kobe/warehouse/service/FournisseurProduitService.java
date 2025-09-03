package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.errors.DefaultFournisseurException;
import com.kobe.warehouse.service.errors.GenericError;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class FournisseurProduitService {

    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ProduitService produitService;

    public FournisseurProduitService(
        FournisseurProduitRepository fournisseurProduitRepository,
        FournisseurRepository fournisseurRepository,
        ProduitService produitService
    ) {
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.produitService = produitService;
    }

    private Produit produitFromId(Long idProduit) {
        if (idProduit == null) {
            return null;
        }
        return new Produit().id(idProduit);
    }

    private Fournisseur fournisseurFromId(Long id) {
        if (id == null) {
            return null;
        }
        return new Fournisseur().id(id);
    }

    private String buildCodeCip(String initialCodeCip) {
        if (StringUtils.isEmpty(initialCodeCip)) {
            return RandomStringUtils.randomNumeric(8);
        }
        if (initialCodeCip.length() < 7) {
            return String.format("%s%s", initialCodeCip, RandomStringUtils.randomNumeric(2));
        }

        return initialCodeCip;
    }

    public Optional<FournisseurProduitDTO> create(FournisseurProduitDTO dto) throws GenericError {
        return Optional.ofNullable(createNewFournisseurProduit(dto)).map(FournisseurProduitDTO::new);
    }

    public FournisseurProduit createNewFournisseurProduit(FournisseurProduitDTO dto) throws GenericError {
        long constraint = fournisseurProduitRepository.countFournisseurProduitByProduitIdAndFournisseurId(
            dto.getProduitId(),
            dto.getFournisseurId()
        );
        if (constraint > 0) {
            throw new GenericError("Le produit est déjà rattaché au fournisseur séléctionné", "duplicateProvider");
        }

        constraint = fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
            dto.getCodeCip(),
            dto.getFournisseurId()
        );
        if (constraint > 0) {
            throw new GenericError(
                String.format("Ce code %s est déjà associé à un produit pour ce grossiste", dto.getCodeCip()),
                "duplicateCodeCip"
            );
        }
        constraint = fournisseurProduitRepository.countByCodeCipAndProduitId(dto.getCodeCip(), dto.getProduitId());
        if (constraint > 0) {
            throw new GenericError(String.format("Ce code %s est déjà associé à un produit ", dto.getCodeCip()), "codeCip");
        }
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setProduit(produitFromId(dto.getProduitId()));
        fournisseurProduit.setFournisseur(fournisseurRepository.getReferenceById(dto.getFournisseurId()));
        fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        return fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
    }

    public Optional<FournisseurProduitDTO> update(FournisseurProduitDTO dto) throws GenericError {
        long constraint;
        FournisseurProduit fournisseurProduit = fournisseurProduitRepository.getReferenceById(dto.getId());
        valideCodeCip(dto, fournisseurProduit);
        if (fournisseurProduit.getFournisseur().getId().compareTo(dto.getFournisseurId()) != 0) {
            constraint = fournisseurProduitRepository.countFournisseurProduitByProduitIdAndFournisseurId(
                dto.getProduitId(),
                dto.getFournisseurId()
            );
            if (constraint > 0) {
                throw new GenericError("Le produit est déjà rattaché au fournisseur séléctionné", "duplicateProvider");
            }

            fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        }
        fournisseurProduit.setProduit(produitFromId(dto.getProduitId()));

        fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        return Optional.of(fournisseurProduitRepository.saveAndFlush(fournisseurProduit)).map(FournisseurProduitDTO::new);
    }

    public void delete(long id) throws Exception {
        fournisseurProduitRepository.delete(fournisseurProduitRepository.getReferenceById(id));
    }

    public void updateDefaultFournisseur(long id, long produitId) throws DefaultFournisseurException {
        FournisseurProduit fournisseurProduit = fournisseurProduitRepository.getReferenceById(id);
        Produit produit = produitService.findReferenceById(produitId);
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        produitService.update(produit);
    }

    public Optional<FournisseurProduit> findFirstByProduitIdAndFournisseurId(Long produitId, Long fournissieurId) {
        return fournisseurProduitRepository.findOneByProduitIdAndFournisseurId(produitId, fournissieurId);
    }

    public void updateCip(String cip, FournisseurProduit fournisseurProduit) {
        if (StringUtils.isEmpty(cip) || fournisseurProduit.getCodeCip().equals(cip)) {
            return;
        }
        long constraint = fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
            cip,
            fournisseurProduit.getFournisseur().getId()
        );
        if (constraint > 0) {
            throw new GenericError(String.format("Ce code %s est déjà associé à un produit pour ce grossiste", cip), "duplicateCodeCip");
        }
        constraint = fournisseurProduitRepository.countByCodeCipAndProduitId(cip, fournisseurProduit.getProduit().getId());
        if (constraint > 0) {
            throw new GenericError(String.format("Ce code %s est déjà associé à un produit ", cip), "codeCip");
        }
        fournisseurProduit.setCodeCip(buildCodeCip(cip));
        fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
    }

    public List<FournisseurProduit> getFournisseurProduitsByFournisseur(Long founisseurId, Pageable pageable) {
        return fournisseurProduitRepository.findAllByFournisseurIdAndProduitParentIsNull(founisseurId, pageable);
    }

    public FournisseurProduit update(FournisseurProduit fournisseurProduit) {
        return fournisseurProduitRepository.save(fournisseurProduit);
    }


    public Optional<FournisseurProduitDTO> findOneById(Long id) {
        return this.fournisseurProduitRepository.findById(id).map(FournisseurProduitDTO::fromEntity);
    }

    public void updateProduitFournisseurFromCommande(FournisseurProduitDTO dto) throws GenericError {
        FournisseurProduit fournisseurProduit = this.fournisseurProduitRepository.getReferenceById(dto.getId());
        Produit produit = fournisseurProduit.getProduit();

        valideCodeCip(dto, fournisseurProduit);

        fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        this.fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
        this.produitService.updateFromCommande(dto.getProduit(), produit);
    }

    private void valideCodeCip(FournisseurProduitDTO dto, FournisseurProduit fournisseurProduit) throws GenericError {
        long constraint;
        if (org.springframework.util.StringUtils.hasLength(dto.getCodeCip()) && !dto.getCodeCip().equals(fournisseurProduit.getCodeCip())) {
            constraint = this.fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
                dto.getCodeCip(),
                dto.getFournisseurId()
            );
            if (constraint > 0) {
                throw new GenericError(
                    String.format("Ce code %s est déjà associé à un produit pour ce grossiste", dto.getCodeCip()),
                    "duplicateCodeCip"
                );
            }
            constraint = this.fournisseurProduitRepository.countByCodeCipAndProduitId(dto.getCodeCip(), dto.getProduitId());
            if (constraint > 0) {
                throw new GenericError(String.format("Ce code %s est déjà associé à un produit ", dto.getCodeCip()), "codeCip");
            }
        }
    }

    public List<FournisseurProduit> findByCodeCipOrProduitcodeEan(String codeCip) {
        return fournisseurProduitRepository.findByCodeCipContainingOrCodeEanContaining(codeCip, codeCip);
    }

    public FournisseurProduit save(FournisseurProduit fournisseurProduit) {
        return fournisseurProduitRepository.save(fournisseurProduit);
    }
}
