package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.errors.DefaultFournisseurException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stock.ProduitService;

import java.util.List;
import java.util.Optional;

import com.kobe.warehouse.service.utils.ServiceUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static java.util.Objects.isNull;

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


    private Fournisseur fournisseurFromId(Integer id) {
        if (id == null) {
            return null;
        }
        return new Fournisseur().id(id);
    }

    private String buildCodeCip(String initialCodeCip) {
        return ServiceUtil.buildCodeCip(initialCodeCip);
    }

    public Optional<FournisseurProduitDTO> create(FournisseurProduitDTO dto) throws GenericError {
        return Optional.ofNullable(createNewFournisseurProduit(dto)).map(FournisseurProduitDTO::new);
    }

    public FournisseurProduit createNewFournisseurProduit(FournisseurProduitDTO dto) throws GenericError {
        Produit produit = produitService.findReferenceById(dto.getProduitId());
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
        fournisseurProduit.setProduit(produit);
        fournisseurProduit.setFournisseur(fournisseurRepository.getReferenceById(dto.getFournisseurId()));
        fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        //  Colisage fournisseur
        if (dto.getQteColis() != null && dto.getQteColis() > 1) {
            fournisseurProduit.setQteColis(dto.getQteColis());
        }
        if (dto.getQteMinimaleCommande() != null && dto.getQteMinimaleCommande() > 0) {
            fournisseurProduit.setQteMinimaleCommande(dto.getQteMinimaleCommande());
        }
        setToFournisseurPrincipal(fournisseurProduit, produit, dto.isPrincipal());
        produit.getFournisseurProduits().add(fournisseurProduit);
        fournisseurProduit = fournisseurProduitRepository.save(fournisseurProduit);
        produitService.updateProduit(produit);
        return fournisseurProduit;
    }

    private void setToFournisseurPrincipal(FournisseurProduit fournisseurProduit, Produit produit, boolean setAsPrincipal) {
        FournisseurProduit currentPrincipal = produit.getFournisseurProduitPrincipal();
        if (!setAsPrincipal
            && currentPrincipal != null
            && currentPrincipal.getId().equals(fournisseurProduit.getId())) {
            throw new DefaultFournisseurException(
                "Vous ne pouvez pas désélectionner le fournisseur principal sans en choisir un autre"
            );
        }

        produit.setFournisseurProduitPrincipal(fournisseurProduit);


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


        fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        //Colisage fournisseur
        if (dto.getQteColis() != null) {
            fournisseurProduit.setQteColis(Math.max(1, dto.getQteColis()));
        }
        if (dto.getQteMinimaleCommande() != null) {
            fournisseurProduit.setQteMinimaleCommande(Math.max(0, dto.getQteMinimaleCommande()));
        }
        setToFournisseurPrincipal(fournisseurProduit, fournisseurProduit.getProduit(), dto.isPrincipal());
        fournisseurProduit = fournisseurProduitRepository.save(fournisseurProduit);
        produitService.updateProduit(fournisseurProduit.getProduit());
        return Optional.of(new FournisseurProduitDTO(fournisseurProduit));

    }

    public void delete(Integer id) throws Exception {
        FournisseurProduit fournisseurProduit = fournisseurProduitRepository.getReferenceById(id);
        Produit produit = fournisseurProduit.getProduit();
        fournisseurProduit.setProduit(null);
        produit.getFournisseurProduits().remove(fournisseurProduit);
        fournisseurProduitRepository.delete(fournisseurProduit);
        produitService.updateProduit(produit);
    }

    public void updateDefaultFournisseur(Integer id, boolean checked, Integer produitId) throws DefaultFournisseurException {
        FournisseurProduit fournisseurProduit = fournisseurProduitRepository.getReferenceById(id);
        Produit produit = produitService.findReferenceById(produitId);
        if (!checked) {
            FournisseurProduit currentDefaultFournisseurProduit = produit.getFournisseurProduitPrincipal();
            if (isNull(currentDefaultFournisseurProduit) || currentDefaultFournisseurProduit.getId().equals(fournisseurProduit.getId())) {
                throw new DefaultFournisseurException(
                    "Vous ne pouvez pas désélectionner le fournisseur principal sans en choisir un autre"

                );
            }

        }
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        produitService.update(produit);
    }

    public Optional<FournisseurProduit> findFirstByProduitIdAndFournisseurId(Integer produitId, Integer fournissieurId) {
        return fournisseurProduitRepository.findOneByProduitIdAndFournisseurId(produitId, fournissieurId);
    }

    public void updateCip(String cip, FournisseurProduit fournisseurProduit) {
        if (!StringUtils.hasLength(cip) || fournisseurProduit.getCodeCip().equals(cip)) {
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

    public FournisseurProduit update(FournisseurProduit fournisseurProduit) {
        return fournisseurProduitRepository.save(fournisseurProduit);
    }

    public Optional<FournisseurProduitDTO> findOneById(Integer id) {
        return this.fournisseurProduitRepository.findById(id).map(FournisseurProduitDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<FournisseurProduitDTO> findAllByProduitId(Integer produitId) {
        return fournisseurProduitRepository.findAllByProduitId(produitId)
            .stream()
            .map(FournisseurProduitDTO::fromEntity)
            .toList();
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

    public Optional<FournisseurProduit> getOne(Integer id) {
        return fournisseurProduitRepository.findById(id);
    }

}
