package com.kobe.warehouse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.web.rest.errors.DefaultFournisseurException;
import com.kobe.warehouse.web.rest.errors.GenericError;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FournisseurProduitService {

  private final FournisseurProduitRepository fournisseurProduitRepository;
  private final FournisseurRepository fournisseurRepository;
  private final ProduitService produitService;

  public FournisseurProduitService(
      FournisseurProduitRepository fournisseurProduitRepository,
      FournisseurRepository fournisseurRepository,
      ProduitService produitService) {
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

  public FournisseurProduit createNewFournisseurProduit(FournisseurProduitDTO dto)
      throws GenericError {
    long constraint =
        fournisseurProduitRepository.countFournisseurProduitByProduitIdAndFournisseurId(
            dto.getProduitId(), dto.getFournisseurId());
    if (constraint > 0) {
      throw new GenericError(
          "Le produit est déjà rattaché au fournisseur séléctionné",
          "produit",
          "duplicateProvider");
    }

    constraint =
        fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
            dto.getCodeCip(), dto.getFournisseurId());
    if (constraint > 0) {
      throw new GenericError(
          "produit",
          String.format(
              "Ce code %s est déjà associé à un produit pour ce grossiste", dto.getCodeCip()),
          "duplicateCodeCip");
    }
    constraint =
        fournisseurProduitRepository.countByCodeCipAndProduitId(
            dto.getCodeCip(), dto.getProduitId());
    if (constraint > 0) {
      throw new GenericError(
          "produit",
          String.format("Ce code %s est déjà associé à un produit ", dto.getCodeCip()),
          "codeCip");
    }
    FournisseurProduit fournisseurProduit = new FournisseurProduit();
    fournisseurProduit.setProduit(produitFromId(dto.getProduitId()));
    fournisseurProduit.setFournisseur(
        fournisseurRepository.getReferenceById(dto.getFournisseurId()));
    fournisseurProduit.setPrincipal(dto.isPrincipal());
    fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
    fournisseurProduit.setPrixAchat(dto.getPrixAchat());
    fournisseurProduit.setPrixUni(dto.getPrixUni());
    fournisseurProduit.setUpdatedAt(Instant.now());
    return fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
  }

  public Optional<FournisseurProduitDTO> update(FournisseurProduitDTO dto) throws GenericError {
    long constraint;
    FournisseurProduit fournisseurProduit =
        fournisseurProduitRepository.getReferenceById(dto.getId());
    valideCodeCip(dto, fournisseurProduit);
    if (fournisseurProduit.getFournisseur().getId().compareTo(dto.getFournisseurId()) != 0) {

      constraint =
          fournisseurProduitRepository.countFournisseurProduitByProduitIdAndFournisseurId(
              dto.getProduitId(), dto.getFournisseurId());
      if (constraint > 0) {
        throw new GenericError(
            "produit",
            "Le produit est déjà rattaché au fournisseur séléctionné",
            "duplicateProvider");
      }

      fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
    }
    fournisseurProduit.setProduit(produitFromId(dto.getProduitId()));

    fournisseurProduit.setPrincipal(dto.isPrincipal());
    fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
    fournisseurProduit.setPrixAchat(dto.getPrixAchat());
    fournisseurProduit.setPrixUni(dto.getPrixUni());
    fournisseurProduit.setUpdatedAt(Instant.now());
    return Optional.of(fournisseurProduitRepository.saveAndFlush(fournisseurProduit))
        .map(FournisseurProduitDTO::new);
  }

  public void delete(long id) throws Exception {
    fournisseurProduitRepository.delete(fournisseurProduitRepository.getReferenceById(id));
  }

  public void updateDefaultFournisseur(long id, boolean isChecked)
      throws DefaultFournisseurException {
    FournisseurProduit fournisseurProduit = fournisseurProduitRepository.getReferenceById(id);
    long count =
        fournisseurProduitRepository.principalAlreadyExiste(
            fournisseurProduit.getProduit().getId(), id);

    if (isChecked && count > 0) {
      throw new DefaultFournisseurException();
    }
    fournisseurProduit.setPrincipal(isChecked);
  }

  public Optional<FournisseurProduit> findFirstByProduitIdAndFournisseurId(
      Long produitId, Long fournissieurId) {
    return fournisseurProduitRepository.findFirstByProduitIdAndFournisseurId(
        produitId, fournissieurId);
  }

  public void updateCip(String cip, FournisseurProduit fournisseurProduit) {
    if (StringUtils.isEmpty(cip) || fournisseurProduit.getCodeCip().equals(cip)) {
      return;
    }
    long constraint =
        fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
            cip, fournisseurProduit.getFournisseur().getId());
    if (constraint > 0) {
      throw new GenericError(
          "produit",
          String.format("Ce code %s est déjà associé à un produit pour ce grossiste", cip),
          "duplicateCodeCip");
    }
    constraint =
        fournisseurProduitRepository.countByCodeCipAndProduitId(
            cip, fournisseurProduit.getProduit().getId());
    if (constraint > 0) {
      throw new GenericError(
          "produit", String.format("Ce code %s est déjà associé à un produit ", cip), "codeCip");
    }
    fournisseurProduit.setCodeCip(buildCodeCip(cip));
    fournisseurProduit.setUpdatedAt(Instant.now());
    fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
  }

  public List<FournisseurProduit> getFournisseurProduitsByFournisseur(
      Long founisseurId, Pageable pageable) {
    return fournisseurProduitRepository.findAllByFournisseurIdAndProduitParentIsNull(
        founisseurId, pageable);
  }

  public FournisseurProduit update(FournisseurProduit fournisseurProduit) {
    return fournisseurProduitRepository.save(fournisseurProduit);
  }

  public FournisseurProduit createNewFournisseurProduitDuringCommande(FournisseurProduitDTO dto)
      throws GenericError {
    long constraint =
        fournisseurProduitRepository.countFournisseurProduitByProduitIdAndFournisseurId(
            dto.getProduitId(), dto.getFournisseurId());
    if (constraint > 0) {
      throw new GenericError(
          "Le produit est déjà rattaché au fournisseur séléctionné",
          "produit",
          "duplicateProvider");
    }

    constraint =
        fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
            dto.getCodeCip(), dto.getFournisseurId());
    if (constraint > 0) {
      throw new GenericError(
          "produit",
          String.format(
              "Ce code %s est déjà associé à un produit pour ce grossiste", dto.getCodeCip()),
          "duplicateCodeCip");
    }
    constraint =
        fournisseurProduitRepository.countByCodeCipAndProduitId(
            dto.getCodeCip(), dto.getProduitId());
    if (constraint > 0) {
      throw new GenericError(
          "produit",
          String.format("Ce code %s est déjà associé à un produit ", dto.getCodeCip()),
          "codeCip");
    }
    FournisseurProduit fournisseurProduit = new FournisseurProduit();
    fournisseurProduit.setProduit(produitFromId(dto.getProduitId()));
    fournisseurProduit.setFournisseur(
        fournisseurRepository.getReferenceById(dto.getFournisseurId()));
    fournisseurProduit.setPrincipal(dto.isPrincipal());
    fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
    fournisseurProduit.setPrixAchat(dto.getPrixAchat());
    fournisseurProduit.setPrixUni(dto.getPrixUni());
    fournisseurProduit.setUpdatedAt(Instant.now());
    return fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
  }

  public Optional<FournisseurProduitDTO> findOneById(Long id) {
    return this.fournisseurProduitRepository.findById(id).map(FournisseurProduitDTO::fromEntity);
  }

  public void updateProduitFournisseurFromCommande(FournisseurProduitDTO dto)
      throws GenericError, JsonProcessingException {

    FournisseurProduit fournisseurProduit =
        this.fournisseurProduitRepository.getReferenceById(dto.getId());
    valideCodeCip(dto, fournisseurProduit);
    fournisseurProduit.setPrincipal(dto.isPrincipal());
    fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
    fournisseurProduit.setPrixAchat(dto.getPrixAchat());
    fournisseurProduit.setPrixUni(dto.getPrixUni());
    fournisseurProduit.setUpdatedAt(Instant.now());
    this.fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
    this.produitService.updateFromCommande(dto.getProduit());
  }

  private void valideCodeCip(FournisseurProduitDTO dto, FournisseurProduit fournisseurProduit)
      throws GenericError {
    long constraint;
    if (org.springframework.util.StringUtils.hasLength(dto.getCodeCip())
        && !dto.getCodeCip().equals(fournisseurProduit.getCodeCip())) {
      constraint =
          this.fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
              dto.getCodeCip(), dto.getFournisseurId());
      if (constraint > 0) {
        throw new GenericError(
            "produit",
            String.format(
                "Ce code %s est déjà associé à un produit pour ce grossiste", dto.getCodeCip()),
            "duplicateCodeCip");
      }
      constraint =
          this.fournisseurProduitRepository.countByCodeCipAndProduitId(
              dto.getCodeCip(), dto.getProduitId());
      if (constraint > 0) {
        throw new GenericError(
            "produit",
            String.format("Ce code %s est déjà associé à un produit ", dto.getCodeCip()),
            "codeCip");
      }
    }
  }
}
