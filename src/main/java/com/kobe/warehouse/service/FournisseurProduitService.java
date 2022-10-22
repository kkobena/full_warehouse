package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.web.rest.errors.DefaultFournisseurException;
import com.kobe.warehouse.web.rest.errors.GenericError;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FournisseurProduitService {
  private final FournisseurProduitRepository fournisseurProduitRepository;
  private final FournisseurRepository fournisseurRepository;

  public FournisseurProduitService(
      FournisseurProduitRepository fournisseurProduitRepository,
      FournisseurRepository fournisseurRepository) {
    this.fournisseurProduitRepository = fournisseurProduitRepository;
    this.fournisseurRepository = fournisseurRepository;
  }

  private Produit produitFromId(Long idProduit) {
    if (idProduit == null) {
      return null;
    }
    return new Produit().id(idProduit);
  }

  private Fournisseur fournisseurFromId(Long id) {
    if (id == null) return null;
    return new Fournisseur().id(id);
  }

  private String buildCodeCip(String initialCodeCip) {
    if (StringUtils.isEmpty(initialCodeCip)) return RandomStringUtils.randomNumeric(8);
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
    if (constraint > 0)
      throw new GenericError(
          "Le produit est déjà rattaché au fournisseur séléctionné",
          "produit",

          "duplicateProvider");

    constraint =
        fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
            dto.getCodeCip(), dto.getFournisseurId());
    if (constraint > 0)
      throw new GenericError(
          "produit",
          String.format(
              "Ce code %s est déjà associé à un produit pour ce grossiste", dto.getCodeCip()),
          "duplicateCodeCip");
    constraint =
        fournisseurProduitRepository.countByCodeCipAndProduitId(
            dto.getCodeCip(), dto.getProduitId());
    if (constraint > 0)
      throw new GenericError(
          "produit",
          String.format("Ce code %s est déjà associé à un produit ", dto.getCodeCip()),
          "codeCip");
    FournisseurProduit fournisseurProduit = new FournisseurProduit();
    fournisseurProduit.setProduit(produitFromId(dto.getProduitId()));
    fournisseurProduit.setFournisseur(fournisseurRepository.getOne(dto.getFournisseurId()));
    fournisseurProduit.setPrincipal(dto.isPrincipal());
    fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
    fournisseurProduit.setPrixAchat(dto.getPrixAchat());
    fournisseurProduit.setPrixUni(dto.getPrixUni());
    fournisseurProduit.setUpdatedAt(Instant.now());
    return fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
  }

  public Optional<FournisseurProduitDTO> update(FournisseurProduitDTO dto) throws Exception {
    long constraint;
    FournisseurProduit fournisseurProduit = fournisseurProduitRepository.getOne(dto.getId());
    if (!dto.getCodeCip().equals(fournisseurProduit.getCodeCip())) {
      constraint =
          fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
              dto.getCodeCip(), dto.getFournisseurId());
      if (constraint > 0)
        throw new GenericError(
            "produit",
            String.format(
                "Ce code %s est déjà associé à un produit pour ce grossiste", dto.getCodeCip()),
            "duplicateCodeCip");
      constraint =
          fournisseurProduitRepository.countByCodeCipAndProduitId(
              dto.getCodeCip(), dto.getProduitId());
      if (constraint > 0)
        throw new GenericError(
            "produit",
            String.format("Ce code %s est déjà associé à un produit ", dto.getCodeCip()),
            "codeCip");
    }
    if (fournisseurProduit.getFournisseur().getId().compareTo(dto.getFournisseurId()) != 0) {

      constraint =
          fournisseurProduitRepository.countFournisseurProduitByProduitIdAndFournisseurId(
              dto.getProduitId(), dto.getFournisseurId());
      if (constraint > 0)
        throw new GenericError(
            "produit",
            "Le produit est déjà rattaché au fournisseur séléctionné",
            "duplicateProvider");

      fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
    }
    fournisseurProduit.setProduit(produitFromId(dto.getProduitId()));

    fournisseurProduit.setPrincipal(dto.isPrincipal());
    fournisseurProduit.setCodeCip(buildCodeCip(dto.getCodeCip()));
    fournisseurProduit.setPrixAchat(dto.getPrixAchat());
    fournisseurProduit.setPrixUni(dto.getPrixUni());
    fournisseurProduit.setUpdatedAt(Instant.now());
    return Optional.ofNullable(fournisseurProduitRepository.saveAndFlush(fournisseurProduit))
        .map(FournisseurProduitDTO::new);
  }

  public void delete(long id) throws Exception {
    fournisseurProduitRepository.delete(fournisseurProduitRepository.getOne(id));
  }

  public void updateDefaultFournisseur(long id, Boolean isChecked)
      throws DefaultFournisseurException {
    FournisseurProduit fournisseurProduit = fournisseurProduitRepository.getOne(id);
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
    return this.fournisseurProduitRepository.findFirstByProduitIdAndFournisseurId(
        produitId, fournissieurId);
  }

  public void updateCip(String cip, FournisseurProduit fournisseurProduit) {
    if (StringUtils.isEmpty(cip) || fournisseurProduit.getCodeCip().equals(cip)) return;
    long constraint =
        fournisseurProduitRepository.countFournisseurProduitByCodeCipAndFournisseurId(
            cip, fournisseurProduit.getFournisseur().getId());
    if (constraint > 0)
      throw new GenericError(
          "produit",
          String.format("Ce code %s est déjà associé à un produit pour ce grossiste", cip),
          "duplicateCodeCip");
    constraint =
        fournisseurProduitRepository.countByCodeCipAndProduitId(
            cip, fournisseurProduit.getProduit().getId());
    if (constraint > 0)
      throw new GenericError(
          "produit", String.format("Ce code %s est déjà associé à un produit ", cip), "codeCip");
    fournisseurProduit.setCodeCip(buildCodeCip(cip));
    fournisseurProduit.setUpdatedAt(Instant.now());
    fournisseurProduitRepository.saveAndFlush(fournisseurProduit);
  }

  public List<FournisseurProduit> getFournisseurProduitsByFournisseur(Long founisseurId, Pageable pageable) {
    return fournisseurProduitRepository.findAllByFournisseurIdAndProduitParentIsNull(founisseurId,pageable);
  }
}
