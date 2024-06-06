package com.kobe.warehouse.service.remise;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RemiseProduitRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.dto.RemiseClientDTO;
import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.dto.RemiseProduitDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RemiseServiceImpl implements RemiseService {
  private final RemiseRepository remiseRepository;
  private final RemiseProduitRepository remiseProduitRepository;
  private final ProduitRepository produitRepository;
  private final LogsService logsService;

  public RemiseServiceImpl(
      RemiseRepository remiseRepository,
      RemiseProduitRepository remiseProduitRepository,
      ProduitRepository produitRepository,
      LogsService logsService) {
    this.remiseRepository = remiseRepository;
    this.remiseProduitRepository = remiseProduitRepository;
    this.produitRepository = produitRepository;
    this.logsService = logsService;
  }

  @Override
  public RemiseDTO save(RemiseDTO remiseDTO) {
    return toDTO(this.remiseRepository.save(toEntity(remiseDTO)));
  }

  @Override
  public RemiseDTO changeStatus(RemiseDTO remiseDTO) {
    Remise remise = toEntity(remiseDTO);
    remise.setEnable(remiseDTO.isEnable());
    return toDTO(this.remiseRepository.save(remise));
  }

  @Override
  public Optional<RemiseDTO> findOne(Long id) {

    return Optional.ofNullable(toDTO(this.remiseRepository.findById(id).orElse(null)));
  }

  @Override
  public void delete(Long id) {
    this.remiseRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RemiseDTO> findAll() {
    return this.remiseRepository.findAll().stream().map(this::toDTO).toList();
  }

  private Remise toEntity(RemiseDTO remiseDTO) {
    if (remiseDTO instanceof RemiseProduitDTO) {
      return toEntity(remiseDTO, new RemiseProduit());
    }
    return toEntity(remiseDTO, new RemiseClient());
  }

  private Remise toEntity(RemiseDTO remiseDTO, Remise remise) {
    remise.setRemiseValue(remiseDTO.getRemiseValue());
    remise.setValeur(remiseDTO.getValeur());
    remise.setBegin(remiseDTO.getBegin());
    remise.setEnd(remiseDTO.getEnd());
    if (remiseDTO.getId() != null) {
      remise.setId(remiseDTO.getId());
    }
    return remise;
  }

  private RemiseDTO toDTO(Remise remise) {
    if (remise instanceof RemiseProduit) {
      return new RemiseProduitDTO((RemiseProduit) remise);
    }
    return new RemiseClientDTO((RemiseClient) remise);
  }

  @Override
  public void associer(Long id, List<Long> produitIds) {
    RemiseProduit remise = this.remiseProduitRepository.getReferenceById(id);
    produitIds.forEach(
        p -> {
          Produit produit = this.produitRepository.getReferenceById(p);
          try {
            produit.setRemise(remise);
            produit.setUpdatedAt(LocalDateTime.now());
            this.produitRepository.save(produit);
            logsService.create(
                TransactionType.UPDATE_PRODUCT,
                String.format("Modification du produit %s", produit.getLibelle()),
                produit.getId().toString());

          } catch (Exception e) {

            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public void dissocier(List<Long> produitIds) {
    produitIds.forEach(
        p -> {
          Produit produit = this.produitRepository.getReferenceById(p);
          try {
            produit.setRemise(null);
            produit.setUpdatedAt(LocalDateTime.now());
            this.produitRepository.save(produit);
            logsService.create(
                TransactionType.UPDATE_PRODUCT,
                String.format("Modification du produit %s", produit.getLibelle()),
                produit.getId().toString());
          } catch (Exception e) {

            throw new RuntimeException(e);
          }
        });
  }
}
