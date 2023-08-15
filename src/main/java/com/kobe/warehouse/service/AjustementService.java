package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.AjustRepository;
import com.kobe.warehouse.repository.AjustementRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.AjustementDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service Implementation for managing {@link Ajustement}. */
@Service
@Transactional
public class AjustementService {

  private final Logger log = LoggerFactory.getLogger(AjustementService.class);

  private final AjustementRepository ajustementRepository;
  private final ProduitRepository produitRepository;
  private final UserRepository userRepository;
  private final AjustRepository ajustRepository;
  private final StorageService storageService;
  private final StockProduitRepository stockProduitRepository;
  private final WarehouseCalendarService warehouseCalendarService;
  private final LogsService logsService;

  public AjustementService(
      AjustementRepository ajustementRepository,
      ProduitRepository produitRepository,
      UserRepository userRepository,
      AjustRepository ajustRepository,
      StorageService storageService,
      StockProduitRepository stockProduitRepository,
      WarehouseCalendarService warehouseCalendarService,
      LogsService logsService) {
    this.ajustementRepository = ajustementRepository;
    this.produitRepository = produitRepository;
    this.userRepository = userRepository;
    this.ajustRepository = ajustRepository;
    this.storageService = storageService;
    this.stockProduitRepository = stockProduitRepository;
    this.warehouseCalendarService = warehouseCalendarService;
    this.logsService = logsService;
  }

  private User getUser() {
    Optional<User> user =
        SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
    return user.orElseGet(null);
  }

  private Ajust createAjsut(Long id, String comment, Long storageId) {

    if (id == null) {
      Ajust ajust = new Ajust();
      ajust.setCommentaire(comment);
      ajust.setUser(getUser());
      ajust.setDateMtv(LocalDateTime.now());
      if (storageId != null) {
        ajust.setStorage(storageService.getOne(storageId));

      } else {
        ajust.setStorage(storageService.getDefaultConnectedUserMainStorage());
      }
      return ajustRepository.save(ajust);
    }
    return ajustRepository.getReferenceById(id);
  }

  public AjustementDTO save(AjustementDTO ajustementDTO) {
    Ajust ajust =
        createAjsut(
            ajustementDTO.getAjustId(),
            ajustementDTO.getCommentaire(),
            ajustementDTO.getStorageId());
    Produit produit = produitRepository.getReferenceById(ajustementDTO.getProduitId());
    int stock =
        stockProduitRepository
            .findStockProduitByStorageIdAndProduitId(
                ajust.getStorage().getId(), ajustementDTO.getProduitId())
            .get()
            .getQtyStock();
    Ajustement ajustement;
    if (ajustementDTO.getAjustId() == null) {
      ajustement = create(ajustementDTO, ajust, produit, stock);
    } else {
      ajustement = createOrUpdate(ajustementDTO, ajust, produit, stock);
    }

    return new AjustementDTO(ajustementRepository.save(ajustement));
  }

  private Ajustement create(AjustementDTO ajustementDTO, Ajust ajust, Produit produit, int stock) {
    Ajustement ajustement = new Ajustement();
    ajustement.setAjust(ajust);
    ajustement.setMotifAjustement(fromMotifId(ajustementDTO.getMotifAjustementId()));
    ajustement.setProduit(produit);
    ajustement.setDateMtv(LocalDateTime.now());
    ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
    ajustement.setStockBefore(stock);
    ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
    return ajustement;
  }

  private Ajustement createOrUpdate(
      AjustementDTO ajustementDTO, Ajust ajust, Produit produit, int stock) {
    Optional<Ajustement> optionalAjustement =
        ajustementRepository.findFirstByAjustIdAndProduitId(
            ajustementDTO.getAjustId(), ajustementDTO.getProduitId());
    if (optionalAjustement.isEmpty()) return create(ajustementDTO, ajust, produit, stock);
    Ajustement ajustement = optionalAjustement.get();
    ajustement.setDateMtv(LocalDateTime.now());
    ajustement.setQtyMvt(ajustement.getQtyMvt() + ajustementDTO.getQtyMvt());
    ajustement.setStockBefore(stock);
    ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
    return ajustement;
  }

  public void saveAjust(AjustementDTO ajustementDTO) {
    Ajust ajust = ajustRepository.getReferenceById(ajustementDTO.getAjustId());
    List<Ajustement> ajustements = ajustementRepository.findAllByAjustId(ajust.getId());
    save(ajustements);
    ajust.setCommentaire(ajustementDTO.getCommentaire());
    ajust.setStatut(SalesStatut.CLOSED);
  }

  private void save(List<Ajustement> ajustements) {

    for (Ajustement ajustement : ajustements) {
      StockProduit p =
          stockProduitRepository
              .findStockProduitByStorageIdAndProduitId(
                  ajustement.getAjust().getStorage().getId(), ajustement.getProduit().getId())
              .get();
      Produit produit = p.getProduit();
      int initStock = p.getQtyStock();
      AjustType ajustType = AjustType.AJUSTEMENT_OUT;
      TransactionType transactionType = TransactionType.AJUSTEMENT_OUT;
      if (ajustement.getQtyMvt() >= 0) {
        ajustType = AjustType.AJUSTEMENT_IN;
        transactionType = TransactionType.AJUSTEMENT_IN;
      }
      ajustement.setStockBefore(initStock);
      ajustement.setType(ajustType);
      p.setQtyStock(p.getQtyStock() + ajustement.getQtyMvt());
      p.setQtyVirtual(p.getQtyStock());
      ajustement.setStockAfter(p.getQtyStock());
      this.ajustementRepository.save(ajustement);
      stockProduitRepository.save(p);
      FournisseurProduit fournisseurProduitPrincipal = produit.getFournisseurProduitPrincipal();
      String desc =
          String.format(
              "Ajustement du produit %s %s quantité initiale %d quantité ajustéé %d quantité finale %d",
              fournisseurProduitPrincipal != null
                  ? fournisseurProduitPrincipal.getCodeCip()
                  : produit.getCodeEan(),
              produit.getLibelle(),
              initStock,
              ajustement.getQtyMvt(),
              p.getQtyStock());
      logsService.create(transactionType, desc, ajustement.getId().toString(),produit);
    }
    this.warehouseCalendarService.initCalendar();
  }

  public AjustementDTO update(AjustementDTO ajustementDTO) {
    Ajustement ajustement = ajustementRepository.getReferenceById(ajustementDTO.getId());
    int stock = 0;
    Optional<StockProduit> stockProduit =
        stockProduitRepository.findStockProduitByStorageIdAndProduitId(
            ajustementDTO.getStorageId(), ajustementDTO.getProduitId());
    if (stockProduit.isPresent()) {
      stock = stockProduit.get().getQtyStock();
    }
    ajustement.setDateMtv(LocalDateTime.now());
    ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
    ajustement.setStockBefore(stock);
    ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
    return new AjustementDTO(ajustementRepository.save(ajustement));
  }

  private MotifAjustement fromMotifId(Long motifId) {
    if (motifId == null) return null;
    MotifAjustement motifAjustement = new MotifAjustement();
    motifAjustement.setId(motifId);
    return motifAjustement;
  }

  @Transactional
  public List<Ajustement> findAll(Long id) {
    log.debug("Request to get all Ajustements");
    return ajustementRepository.findAllByAjustId(id);
  }

  @Transactional
  public List<Ajustement> findAll() {
    log.debug("Request to get all Ajustements");
    return ajustementRepository.findAll();
  }

  public void delete(Long id) {
    ajustementRepository.deleteById(id);
  }
}
