package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.FamilleProduit_;
import com.kobe.warehouse.domain.FormProduit;
import com.kobe.warehouse.domain.FormProduit_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.GammeProduit;
import com.kobe.warehouse.domain.GammeProduit_;
import com.kobe.warehouse.domain.Laboratoire;
import com.kobe.warehouse.domain.Laboratoire_;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.RayonProduit_;
import com.kobe.warehouse.domain.Rayon_;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.StockProduit_;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Storage_;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.Tableau_;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.Tva_;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.dto.builder.ProduitBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@Transactional
public class CustomizedProductRepository implements CustomizedProductService {

  private final Logger LOG = LoggerFactory.getLogger(CustomizedProductRepository.class);
  private final StockProduitRepository stockProduitRepository;
  private final LogsService logsService;
  private final ProduitRepository produitRepository;
  private final RayonProduitRepository rayonProduitRepository;
  private final StorageService storageService;
  @PersistenceContext private EntityManager em;

  public CustomizedProductRepository(
      StockProduitRepository stockProduitRepository,
      LogsService logsService,
      ProduitRepository produitRepository,
      RayonProduitRepository rayonProduitRepository,
      StorageService storageService) {
    this.stockProduitRepository = stockProduitRepository;
    this.logsService = logsService;
    this.produitRepository = produitRepository;
    this.rayonProduitRepository = rayonProduitRepository;
    this.storageService = storageService;
  }

  @Override
  public void save(StockProduitDTO dto) throws Exception {
    StockProduit stockProduit = buildStockProduitFromStockProduitDTO(dto);
    Produit produit = em.find(Produit.class, dto.getProduitId());
    stockProduit.setProduit(produit);
    em.merge(stockProduit);
    em.refresh(produit);
  }

  @Override
  public void update(StockProduitDTO dto) throws Exception {
    StockProduit stockProduit =
        buildStockProduitFromStockProduitDTO(dto, em.find(StockProduit.class, dto.getId()));
    em.merge(stockProduit);
  }

  @Override
  public void save(FournisseurProduitDTO dto) throws Exception {
    FournisseurProduit fournisseurProduit = buildFournisseurProduitFromFournisseurProduitDTO(dto);
    Produit produit = em.find(Produit.class, dto.getProduitId());
    fournisseurProduit.setProduit(produit);
    em.merge(fournisseurProduit);
    em.refresh(produit);
  }

  @Override
  public void update(FournisseurProduitDTO dto) throws Exception {
    FournisseurProduit fournisseurProduit =
        buildFournisseurProduitFromFournisseurProduitDTO(
            dto, em.find(FournisseurProduit.class, dto.getId()));
    Produit produit = em.find(Produit.class, dto.getProduitId());
    fournisseurProduit.setProduit(produit);
    em.merge(fournisseurProduit);
    em.refresh(produit);
  }

  @Override
  public void removeFournisseurProduit(Long id) throws Exception {
    FournisseurProduit fournisseurProduit = em.find(FournisseurProduit.class, id);
    em.remove(fournisseurProduit);
  }

  @Override
  @Transactional(readOnly = true)
  public SalesLine lastSale(ProduitCriteria produitCriteria) {
    try {
      TypedQuery<SalesLine> q =
          em.createQuery(
              "SELECT o FROM SalesLine o WHERE o.sales.statut=?1 AND o.produit.id=?2 "
                  + " AND o.sales.magasin.id=?3  ORDER BY  o.sales.updatedAt DESC",
              SalesLine.class);
      q.setMaxResults(1);
      q.setParameter(1, SalesStatut.CLOSED);
      q.setParameter(2, produitCriteria.getId());
      q.setParameter(3, produitCriteria.getMagasinId());
      return q.getSingleResult();
    } catch (Exception e) {
      // LOG.debug("lastSale=====>>>> {}", e);
    }
    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public StoreInventoryLine lastInventory(ProduitCriteria produitCriteria) {
    try {
      TypedQuery<StoreInventoryLine> q =
          em.createQuery(
              "SELECT o FROM StoreInventoryLine o WHERE o.storeInventory.statut=?1 AND o.produit.id=?2 "
                  + " AND o.storeInventory.storage.magasin.id=?3  ORDER BY  o.storeInventory.updatedAt DESC",
              StoreInventoryLine.class);
      q.setMaxResults(1);
      q.setParameter(1, SalesStatut.CLOSED);
      q.setParameter(2, produitCriteria.getId());
      q.setParameter(3, produitCriteria.getMagasinId());
      return q.getSingleResult();
    } catch (Exception e) {
      // LOG.debug("lastInventory =====>>>> {}", e);
    }
    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public DeliveryReceiptItem lastOrder(ProduitCriteria produitCriteria) {
    try {
      TypedQuery<DeliveryReceiptItem> q =
          em.createQuery(
              "SELECT o FROM DeliveryReceiptItem o WHERE o.deliveryReceipt.receiptStatut<>?1 AND o.fournisseurProduit.produit.id=?2 "
                  + " ORDER BY  o.deliveryReceipt.modifiedDate DESC",
              DeliveryReceiptItem.class);
      q.setMaxResults(1);
      q.setParameter(1, ReceiptStatut.PENDING);
      q.setParameter(2, produitCriteria.getId());

      return q.getSingleResult();
    } catch (Exception e) {
      //  LOG.debug("lastOrder =====>>>> {}", e);
    }
    return null;
  }

  @Override
  public void save(Produit produit) throws Exception {
    produitRepository.save(produit);
  }

  @Override
  public FournisseurProduit fournisseurProduitProduit(Produit produit, ProduitDTO dto) {
    FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
    fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
    fournisseurProduit.setUpdatedAt(LocalDateTime.now());
    fournisseurProduit.setCodeCip(dto.getCodeCip());
    fournisseurProduit.setPrixAchat(dto.getCostAmount());
    fournisseurProduit.setPrixUni(dto.getRegularUnitPrice());
    fournisseurProduit.setProduit(produit);
    return fournisseurProduit;
  }

  @Override
  public StockProduit stockProduitFromProduitDTO(ProduitDTO dto) {
    return null;
  }

  @Override
  public void updateDetail(ProduitDTO dto) {
    final Produit produit =
        buildDetailProduitFromProduitDTO(dto, em.find(Produit.class, dto.getId()));
    em.merge(produit);
    logsService.create(
        TransactionType.UPDATE_PRODUCT,
        String.format("Modification du produit %s", produit.getLibelle()),
        produit.getId().toString());
  }

  @Override
  public StockProduit updateTotalStock(Produit produit, int stockIn, int stockUg) {
    StockProduit stockProduit =
        produit.getStockProduits().stream()
            .filter(
                stock ->
                    stock
                        .getStorage()
                        .getId()
                        .equals(storageService.getDefaultConnectedUserPointOfSaleStorage().getId()))
            .findFirst()
            .orElseThrow();

    stockProduit.setUpdatedAt(LocalDateTime.now());
    stockProduit.setQtyStock(stockProduit.getQtyStock() + stockIn);
    stockProduit.setQtyUG(stockProduit.getQtyUG() + stockUg);
    stockProduit.setQtyVirtual(stockProduit.getQtyStock());
    return stockProduitRepository.save(stockProduit);
  }

  @Override
  public void updateFromCommande(ProduitDTO dto, Produit produit) {

    produit.setTva(this.tvaFromId(dto.getTvaId()));
    if (StringUtils.hasLength(dto.getExpirationDate())) {
      produit.setPerimeAt(
          LocalDate.parse(dto.getExpirationDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }
    produit
        .setCmuAmount(dto.getCmuAmount())
        .setTypeEtyquette(this.typeEtiquetteFromId(dto.getTypeEtiquetteId()));

    produit.setUpdatedAt(LocalDateTime.now());
    Set<RayonProduit> rayonProduits = rayonProduitRepository.findAllByProduitId(produit.getId());
    buildRayonProduits(produit, dto, rayonProduits);
    em.merge(produit);
    logsService.create(
        TransactionType.UPDATE_PRODUCT,
        String.format("Modification du produit %s", produit.getLibelle()),
        produit.getId().toString());
  }

  @Override
  public Page<ProduitDTO> lite(ProduitCriteria produitCriteria, Pageable pageable) {
    long total = findAllCount(produitCriteria);
    List<ProduitDTO> list = new ArrayList<>();
    if (total > 0) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Produit> cq = cb.createQuery(Produit.class);
      Root<Produit> root = cq.from(Produit.class);
      cq.select(root).distinct(true).orderBy(cb.asc(root.get(Produit_.libelle)));
      List<Predicate> predicates = produitPredicate(cb, root, produitCriteria);
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
      TypedQuery<Produit> q = em.createQuery(cq);
      q.setFirstResult((int) pageable.getOffset());
      q.setMaxResults(pageable.getPageSize());
      list =
          q.getResultList().stream()
              .map(ProduitBuilder::fromProduitWithRequiredParentRelation)
              .toList();
    }
    return new PageImpl<>(list, pageable, total);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProduitDTO> findAll(ProduitCriteria produitCriteria) throws Exception {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Produit> cq = cb.createQuery(Produit.class);
    Root<Produit> root = cq.from(Produit.class);
    root.fetch(Produit_.stockProduits, JoinType.LEFT);
    cq.select(root).distinct(true).orderBy(cb.asc(root.get(Produit_.libelle)));
    List<Predicate> predicates = rechercheProduitPredicate(cb, root, produitCriteria);
    cq.where(cb.and(predicates.toArray(new Predicate[0])));
    TypedQuery<Produit> q = em.createQuery(cq);
    return q.getResultList().stream().map(ProduitBuilder::fromProduit).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable)
      throws Exception {
    long total = findAllCount(produitCriteria);
    List<ProduitDTO> list = new ArrayList<>();
    Magasin magasin = storageService.getConnectedUserMagasin();
    Storage userStorage = storageService.getDefaultConnectedUserPointOfSaleStorage();
    if (total > 0) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Produit> cq = cb.createQuery(Produit.class);
      Root<Produit> root = cq.from(Produit.class);
      cq.select(root).distinct(true).orderBy(cb.asc(root.get(Produit_.libelle)));
      List<Predicate> predicates = produitPredicate(cb, root, produitCriteria);
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
      TypedQuery<Produit> q = em.createQuery(cq);
      q.setFirstResult((int) pageable.getOffset());
      q.setMaxResults(pageable.getPageSize());
      q.getResultList()
          .forEach(
              p -> {
                ProduitDTO dto =
                    ProduitBuilder.buildFromProduit(
                        p,
                        magasin,
                        p.getStockProduits().stream()
                            .filter(s -> s.getStorage().equals(userStorage))
                            .findFirst()
                            .orElse(null));
                SalesLine lignesVente = lastSale(produitCriteria);
                if (lignesVente != null) {
                  dto.setLastDateOfSale(lignesVente.getUpdatedAt());
                }
                StoreInventoryLine detailsInventaire = lastInventory(produitCriteria);
                if (detailsInventaire != null) {
                  dto.setLastInventoryDate(detailsInventaire.getStoreInventory().getUpdatedAt());
                }
                DeliveryReceiptItem deliveryReceiptItem = lastOrder(produitCriteria);
                if (deliveryReceiptItem != null) {
                  dto.setLastOrderDate(deliveryReceiptItem.getUpdatedDate());
                }
                list.add(dto);
              });
    }
    return new PageImpl<>(list, pageable, total);
  }

  @Override
  public Optional<ProduitDTO> findOneById(Long produitId) {
    Magasin magasin = storageService.getConnectedUserMagasin();
    Storage userStorage = storageService.getDefaultConnectedUserPointOfSaleStorage();
    return produitRepository
        .findById(produitId)
        .map(
            p ->
                ProduitBuilder.buildFromProduit(
                    p,
                    magasin,
                    p.getStockProduits().stream()
                        .filter(s -> s.getStorage().equals(userStorage))
                        .findFirst()
                        .orElse(null)));
  }

  @Override
  public void save(ProduitDTO dto, Rayon rayon) throws Exception {
    Produit produit = ProduitBuilder.fromDTO(dto, rayon);
    produit = produitRepository.save(produit);
    stockProduitRepository.saveAll(produit.getStockProduits());
    logsService.create(
        TransactionType.CREATE_PRODUCT,
        String.format("Création du produit %s", produit.getLibelle()),
        produit.getId().toString());
  }

  @Override
  public void update(ProduitDTO dto) throws Exception {
    Produit produitToUpdate = em.find(Produit.class, dto.getId());
    Set<RayonProduit> rayonProduits =
        rayonProduitRepository.findAllByProduitId(produitToUpdate.getId());
    boolean mustUpdateRayon = false;
    if (rayonProduits.isEmpty()) {
      mustUpdateRayon = true;
    } else {
      Optional<RayonProduit> optionalRayonProduit =
          rayonProduits.stream()
              .filter(
                  rayonProduit ->
                      rayonProduit
                          .getRayon()
                          .getStorage()
                          .getStorageType()
                          .name()
                          .equalsIgnoreCase(StorageType.PRINCIPAL.getValue()))
              .findFirst();
      if (optionalRayonProduit.isPresent()) {
        RayonProduit rayonProduit = optionalRayonProduit.get();
        if (!rayonProduit.getRayon().getId().equals(dto.getRayonId())) {
          mustUpdateRayon = true;
        }
      }
    }
    if (mustUpdateRayon) {
      updateProduitDetails(produitToUpdate.getProduits(), dto);
    }
    Produit produit = ProduitBuilder.buildProduitFromProduitDTO(dto, produitToUpdate);
    buildRayonProduits(produit, dto, rayonProduits);
    produit.getFournisseurProduits().stream()
        .filter(FournisseurProduit::isPrincipal)
        .findFirst()
        .ifPresent(em::merge);
    em.merge(produit);
    logsService.create(
        TransactionType.UPDATE_PRODUCT,
        String.format("Modification du produit %s", produit.getLibelle()),
        produit.getId().toString());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FournisseurProduit> getFournisseurProduitByCriteria(
      String criteteria, Long fournisseurId) {
    try {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<FournisseurProduit> cq = cb.createQuery(FournisseurProduit.class);
      Root<FournisseurProduit> root = cq.from(FournisseurProduit.class);
      cq.select(root);
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(
          cb.or(
              cb.like(root.get(FournisseurProduit_.codeCip), criteteria),
              cb.like(root.get(FournisseurProduit_.produit).get(Produit_.codeEan), criteteria)));
      predicates.add(
          cb.equal(root.get(FournisseurProduit_.fournisseur).get(Fournisseur_.id), fournisseurId));
      predicates.add(cb.isNull(root.get(FournisseurProduit_.produit).get(Produit_.parent)));
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
      TypedQuery<FournisseurProduit> q = em.createQuery(cq);
      q.setMaxResults(1);
      return Optional.ofNullable(q.getSingleResult());
    } catch (Exception e) {
      LOG.debug("getFournisseurProduitByCriteria=====>>>> {}", e);
      return Optional.empty();
    }
  }

  @Override
  public int produitTotalStock(Produit produit) {
    return produit.getStockProduits().stream()
        .map(StockProduit::getQtyStock)
        .reduce(0, Integer::sum);
  }

  @Override
  public int produitTotalStockWithQantityUg(Produit produit) {
    return produit.getStockProduits().stream()
        .map(StockProduit::getTotalStockQuantity)
        .reduce(0, Integer::sum);
  }

  private void updateProduitDetails(List<Produit> produits, ProduitDTO produitDTO) {
    for (Produit p : produits) {
      Set<RayonProduit> rayonProduits = rayonProduitRepository.findAllByProduitId(p.getId());
      buildRayonProduits(p, produitDTO, rayonProduits);
      produitRepository.save(p);
    }
  }

  private void buildRayonProduits(
      Produit produitToUpdate, ProduitDTO produitDTO, Set<RayonProduit> rayonProduits) {
    Set<RayonProduit> newSet = new HashSet<>();
    if (!rayonProduits.isEmpty()) {
      for (RayonProduit r : rayonProduits) {
        if (r.getRayon().getStorage().getStorageType() == StorageType.PRINCIPAL) {
          newSet.add(r.setRayon(rayonFromId(produitDTO.getRayonId())));
        } else {
          newSet.add(r);
        }
      }
    } else {
      newSet.add(
          new RayonProduit()
              .setProduit(produitToUpdate)
              .setRayon(rayonFromId(produitDTO.getRayonId())));
    }
    produitToUpdate.setRayonProduits(newSet);
  }

  private List<Predicate> produitPredicate(
      CriteriaBuilder cb, Root<Produit> root, ProduitCriteria produitCriteria) {
    List<Predicate> predicates = new ArrayList<>();
    if (StringUtils.hasLength(produitCriteria.getSearch())) {
      String search = produitCriteria.getSearch().toUpperCase() + "%";
      SetJoin<Produit, FournisseurProduit> fp =
          root.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
      predicates.add(
          cb.or(
              cb.like(cb.upper(root.get(Produit_.libelle)), search),
              cb.like(cb.upper(root.get(Produit_.codeEan)), search),
              cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), search)));
    }
    if (!ObjectUtils.isEmpty(produitCriteria.getStatus())) {
      predicates.add(cb.equal(root.get(Produit_.status), produitCriteria.getStatus()));
    }
    if (produitCriteria.getStorageId() != null || produitCriteria.getRayonId() != null) {
      SetJoin<Produit, StockProduit> st = root.joinSet(Produit_.STOCK_PRODUITS, JoinType.INNER);
      if (produitCriteria.getStorageId() != null) {
        predicates.add(
            cb.equal(
                st.get(StockProduit_.storage).get(Storage_.id), produitCriteria.getMagasinId()));
      }
      if (produitCriteria.getRayonId() != null) {
        SetJoin<Produit, RayonProduit> rp = root.joinSet(Produit_.RAYON_PRODUITS, JoinType.INNER);
        predicates.add(
            cb.equal(rp.get(RayonProduit_.rayon).get(Rayon_.id), produitCriteria.getRayonId()));
      }
    }

    if (produitCriteria.getFamilleId() != null) {
      Join<Produit, FamilleProduit> familleJoin = root.join(Produit_.FAMILLE, JoinType.INNER);
      predicates.add(cb.equal(familleJoin.get(FamilleProduit_.id), produitCriteria.getFamilleId()));
    }
    if (produitCriteria.getLaboratoireId() != null) {
      Join<Produit, Laboratoire> laboratoireIdJoin =
          root.join(Produit_.LABORATOIRE, JoinType.INNER);
      predicates.add(
          cb.equal(laboratoireIdJoin.get(Laboratoire_.id), produitCriteria.getLaboratoireId()));
    }
    if (produitCriteria.getGammeId() != null) {
      Join<Produit, GammeProduit> gammeJoin = root.join(Produit_.GAMME, JoinType.INNER);
      predicates.add(cb.equal(gammeJoin.get(GammeProduit_.id), produitCriteria.getGammeId()));
    }
    if (produitCriteria.getFormeId() != null) {
      Join<Produit, FormProduit> formeJoin = root.join(Produit_.FORME, JoinType.INNER);
      predicates.add(cb.equal(formeJoin.get(FormProduit_.id), produitCriteria.getFormeId()));
    }
    if (produitCriteria.getTvaId() != null) {
      Join<Produit, Tva> tvaJoin = root.join(Produit_.TVA, JoinType.INNER);
      predicates.add(cb.equal(tvaJoin.get(Tva_.id), produitCriteria.getTvaId()));
    }
    if (produitCriteria.getTypeProduit() != null) {
      predicates.add(cb.equal(root.get(Produit_.typeProduit), produitCriteria.getTypeProduit()));
    }
    if (produitCriteria.getDeconditionne() != null) {
      if (produitCriteria.getDeconditionne()) {
        predicates.add(cb.equal(root.get(Produit_.typeProduit), TypeProduit.DETAIL));
      }
    }
    if (produitCriteria.getDeconditionnable() != null) {
      if (produitCriteria.getDeconditionnable()) {
        predicates.add(cb.isTrue(root.get(Produit_.deconditionnable)));
      } else {
        predicates.add(cb.isFalse(root.get(Produit_.deconditionnable)));
      }
    }
    if (produitCriteria.getDateperemption() != null) {
      if (produitCriteria.getDateperemption()) {
        predicates.add(cb.isTrue(root.get(Produit_.checkExpiryDate)));
      } else {
        predicates.add(cb.isFalse(root.get(Produit_.checkExpiryDate)));
      }
    }
    if (produitCriteria.getTableauId() != null) {
      predicates.add(
          cb.equal(root.get(Produit_.tableau).get(Tableau_.id), produitCriteria.getTableauId()));
    }
    if (produitCriteria.getTableauNot() != null) {
      predicates.add(
          cb.or(
              cb.isNull(root.get(Produit_.tableau).get(Tableau_.id)),
              cb.notEqual(
                  root.get(Produit_.tableau).get(Tableau_.id), produitCriteria.getTableauNot())));
    }
    return predicates;
  }

  private long findAllCount(ProduitCriteria produitCriteria) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<Produit> root = cq.from(Produit.class);
    cq.select(cb.countDistinct(root));
    List<Predicate> predicates = produitPredicate(cb, root, produitCriteria);
    cq.where(cb.and(predicates.toArray(new Predicate[0])));
    TypedQuery<Long> q = em.createQuery(cq);
    Long v = q.getSingleResult();
    return v != null ? v : 0;
  }

  private List<Predicate> rechercheProduitPredicate(
      CriteriaBuilder cb, Root<Produit> root, ProduitCriteria produitCriteria) {
    List<Predicate> predicates = new ArrayList<>();

    if (StringUtils.hasLength(produitCriteria.getSearch())) {
      String search = produitCriteria.getSearch().toUpperCase() + "%";
      SetJoin<Produit, FournisseurProduit> fp =
          root.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
      predicates.add(
          cb.or(
              cb.like(cb.upper(root.get(Produit_.libelle)), search),
              cb.like(root.get(Produit_.codeEan), search),
              cb.like(fp.get(FournisseurProduit_.codeCip), search)));
    }
    if (!ObjectUtils.isEmpty(produitCriteria.getStatus())) {
      predicates.add(cb.equal(root.get(Produit_.status), produitCriteria.getStatus()));
    }
    if (produitCriteria.getStorageId() != null) {
      SetJoin<Produit, StockProduit> st = root.joinSet(Produit_.STOCK_PRODUITS, JoinType.INNER);
      if (produitCriteria.getStorageId() != null) {
        predicates.add(
            cb.equal(
                st.get(StockProduit_.storage).get(Storage_.id), produitCriteria.getStorageId()));
      }
    }
    if (produitCriteria.getRayonId() != null) {
      SetJoin<Produit, RayonProduit> st = root.joinSet(Produit_.RAYON_PRODUITS, JoinType.INNER);
      predicates.add(
          cb.equal(st.get(RayonProduit_.rayon).get(Rayon_.id), produitCriteria.getRayonId()));
    }
    return predicates;
  }

  @Override
  public List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable) {
      Magasin magasin = storageService.getConnectedUserMagasin();
      Storage userStorage = storageService.getDefaultConnectedUserPointOfSaleStorage();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Produit> cq = cb.createQuery(Produit.class);
    Root<Produit> root = cq.from(Produit.class);
    cq.select(root).distinct(true).orderBy(cb.asc(root.get(Produit_.libelle)));
    List<Predicate> predicates = produitLitePredicate(cb, root, produitCriteria);
    cq.where(cb.and(predicates.toArray(new Predicate[0])));
    TypedQuery<Produit> q = em.createQuery(cq);
    q.setFirstResult((int) pageable.getOffset());
    q.setMaxResults(pageable.getPageSize());
    return q.getResultList().stream()
        .map(e->ProduitBuilder.fromProductLiteList(e,e.getStockProduits().stream()
            .filter(s -> s.getStorage().equals(userStorage))
            .findFirst()
            .orElse(null),magasin))
        .toList();
  }

  private List<Predicate> produitLitePredicate(
      CriteriaBuilder cb, Root<Produit> root, ProduitCriteria produitCriteria) {
    List<Predicate> predicates = new ArrayList<>();
    if (StringUtils.hasLength(produitCriteria.getSearch())) {
      String search = produitCriteria.getSearch().toUpperCase() + "%";
      SetJoin<Produit, FournisseurProduit> fp =
          root.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
      predicates.add(
          cb.or(
              cb.like(cb.upper(root.get(Produit_.libelle)), search),
              cb.like(cb.upper(root.get(Produit_.codeEan)), search),
              cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), search)));
    }
    if (!ObjectUtils.isEmpty(produitCriteria.getStatus())) {
      predicates.add(cb.equal(root.get(Produit_.status), produitCriteria.getStatus()));
    }
    if (produitCriteria.getStorageId() != null || produitCriteria.getRayonId() != null) {
      SetJoin<Produit, StockProduit> st = root.joinSet(Produit_.STOCK_PRODUITS, JoinType.INNER);
      if (produitCriteria.getStorageId() != null) {
        predicates.add(
            cb.equal(
                st.get(StockProduit_.storage).get(Storage_.id), produitCriteria.getMagasinId()));
      }
      if (produitCriteria.getRayonId() != null) {
        SetJoin<Produit, RayonProduit> rp = root.joinSet(Produit_.RAYON_PRODUITS, JoinType.INNER);
        predicates.add(
            cb.equal(rp.get(RayonProduit_.rayon).get(Rayon_.id), produitCriteria.getRayonId()));
      }
    }


    if (produitCriteria.getTypeProduit() != null) {
      predicates.add(cb.equal(root.get(Produit_.typeProduit), produitCriteria.getTypeProduit()));
    }


    return predicates;
  }
}
