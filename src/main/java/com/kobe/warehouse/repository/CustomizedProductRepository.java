package com.kobe.warehouse.repository;


import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.*;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class CustomizedProductRepository implements CustomizedProductService {
    private final Logger LOG = LoggerFactory.getLogger(CustomizedProductRepository.class);
    @PersistenceContext
    private EntityManager em;
    @Autowired
    private StockProduitRepository stockProduitRepository;
    @Autowired
    private FournisseurProduitRepository fournisseurProduitRepository;
    @Autowired
    private LogsService logsService;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private RayonProduitRepository rayonProduitRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    private RayonRepository rayonRepository;

    private List<Predicate> produitPredicate(CriteriaBuilder cb, Root<Produit> root, ProduitCriteria produitCriteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (!StringUtils.isEmpty(produitCriteria.getSearch())) {
            String search = produitCriteria.getSearch().toUpperCase() + "%";
            SetJoin<Produit, FournisseurProduit> fp = root.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(cb.or(cb.like(cb.upper(root.get(Produit_.libelle)), search),
                cb.like(cb.upper(root.get(Produit_.codeEan)), search),
                cb.like(cb.upper(fp.get(FournisseurProduit_.codeCip)), search)));

        }
        if (!ObjectUtils.isEmpty(produitCriteria.getStatus())) {
            predicates.add(cb.equal(root.get(Produit_.status), produitCriteria.getStatus()));
        }
        if (produitCriteria.getStorageId() != null || produitCriteria.getRayonId() != null) {
            SetJoin<Produit, StockProduit> st = root.joinSet(Produit_.STOCK_PRODUITS, JoinType.INNER);
            if (produitCriteria.getStorageId() != null) {
                predicates.add(cb.equal(st.get(StockProduit_.storage).get(Storage_.id),
                    produitCriteria.getMagasinId()));
            }
            if (produitCriteria.getRayonId() != null) {
                SetJoin<Produit, RayonProduit> rp = root.joinSet(Produit_.RAYON_PRODUITS, JoinType.INNER);
                predicates.add(cb.equal(rp.get(RayonProduit_.rayon).get(Rayon_.id),
                    produitCriteria.getRayonId()));

            }
        }

        if (produitCriteria.getFamilleId() != null) {
            Join<Produit, FamilleProduit> familleJoin = root.join(Produit_.FAMILLE, JoinType.INNER);
            predicates.add(cb.equal(familleJoin.get(FamilleProduit_.id), produitCriteria.getFamilleId()));
        }
        if (produitCriteria.getLaboratoireId() != null) {
            Join<Produit, Laboratoire> laboratoireIdJoin = root.join(Produit_.LABORATOIRE, JoinType.INNER);
            predicates.add(cb.equal(laboratoireIdJoin.get(Laboratoire_.id), produitCriteria.getLaboratoireId()));
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
                predicates.add(cb.isTrue(root.get(Produit_.dateperemption)));
            } else {
                predicates.add(cb.isFalse(root.get(Produit_.dateperemption)));
            }
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
        return q.getResultList().stream().map(ProduitDTO::new).collect(Collectors.toList());
    }


    private List<Predicate> rechercheProduitPredicate(CriteriaBuilder cb, Root<Produit> root,
                                                      ProduitCriteria produitCriteria) {
        List<Predicate> predicates = new ArrayList<>();

        if (!StringUtils.isEmpty(produitCriteria.getSearch())) {
            String search = produitCriteria.getSearch().toUpperCase() + "%";
            SetJoin<Produit, FournisseurProduit> fp = root.joinSet(Produit_.FOURNISSEUR_PRODUITS, JoinType.LEFT);
            predicates.add(cb.or(cb.like(cb.upper(root.get(Produit_.libelle)), search),
                cb.like(root.get(Produit_.codeEan), search), cb.like(fp.get(FournisseurProduit_.codeCip), search)));
        }
        if (!ObjectUtils.isEmpty(produitCriteria.getStatus())) {
            predicates.add(cb.equal(root.get(Produit_.status), produitCriteria.getStatus()));
        }
        if (produitCriteria.getStorageId() != null) {
            SetJoin<Produit, StockProduit> st = root.joinSet(Produit_.STOCK_PRODUITS, JoinType.INNER);
            if (produitCriteria.getStorageId() != null) {
                predicates.add(cb.equal(st.get(StockProduit_.storage).get(Storage_.id),
                    produitCriteria.getStorageId()));
            }
        }
        if (produitCriteria.getRayonId() != null) {
            SetJoin<Produit, RayonProduit> st = root.joinSet(Produit_.RAYON_PRODUITS, JoinType.INNER);
            predicates.add(cb.equal(st.get(RayonProduit_.rayon).get(Rayon_.id),
                produitCriteria.getRayonId()));

        }
        return predicates;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) throws Exception {
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
            q.setFirstResult(pageable.getPageNumber());
            q.setMaxResults(pageable.getPageSize());
            q.getResultList().forEach(p -> {
                ProduitDTO dto = new ProduitDTO(p, magasin,
                    p.getStockProduits().stream().filter(s -> s.getStorage().equals(userStorage)).findFirst().orElse(null));
                SalesLine lignesVente = lastSale(produitCriteria);
                if (lignesVente != null) {
                    dto.setLastDateOfSale(lignesVente.getUpdatedAt());
                }
                StoreInventoryLine detailsInventaire = lastInventory(produitCriteria);
                if (detailsInventaire != null) {
                    dto.setLastInventoryDate(detailsInventaire.getStoreInventory().getUpdatedAt());
                }
                OrderLine commandeItem = lastOrder(produitCriteria);
                if (commandeItem != null) {
                    dto.setLastOrderDate(commandeItem.getUpdatedAt());
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
        return produitRepository.findById(produitId).map(p -> new ProduitDTO(p, magasin,
            p.getStockProduits().stream().filter(s -> s.getStorage().equals(userStorage))
                .findFirst().orElse(null))

        );

    }

    @Override
    public void save(ProduitDTO dto, Rayon rayon) throws Exception {
        Produit produit = ProduitDTO.fromDTO(dto, rayon);
        produit = produitRepository.save(produit);
        stockProduitRepository.saveAll(produit.getStockProduits());
        logsService.create(TransactionType.CREATE_PRODUCT, String.format("Création du produit %s", new Object[]{produit.getLibelle()}), produit.getId());
    }

    @Override
    public void update(ProduitDTO dto) throws Exception {
        Produit produitToUpdate = em.find(Produit.class, dto.getId());
        Set<RayonProduit> rayonProduits = rayonProduitRepository.findAllByProduitId(produitToUpdate.getId());
        boolean mustUpdateRayon = false;
        if (rayonProduits.isEmpty()) {
            mustUpdateRayon = true;
        } else {
            Optional<RayonProduit> optionalRayonProduit = rayonProduits.stream().filter(rayonProduit -> rayonProduit.getRayon().getStorage().getStorageType().name().equalsIgnoreCase(StorageType.PRINCIPAL.getValue())).findFirst();
            if (optionalRayonProduit.isPresent()) {
                RayonProduit rayonProduit = optionalRayonProduit.get();
                if (!dto.getRayonId().equals(rayonProduit.getRayon().getId())) {
                    mustUpdateRayon = true;
                }
            }
        }
        if (mustUpdateRayon) {
            updateProduitDetails(produitToUpdate.getProduits(), dto);
        }
        Produit produit = buildProduitFromProduitDTO(dto, produitToUpdate);
        buildRayonProduits(produit, dto, rayonProduits);
        produit.getFournisseurProduits().stream().filter(f -> f.isPrincipal()).findFirst().ifPresent(f -> em.merge(f));
        em.merge(produit);
        logsService.create(TransactionType.UPDATE_PRODUCT, String.format("Modification du produit %s", new Object[]{produit.getLibelle()}), produit.getId());
    }

    private void updateProduitDetails(List<Produit> produits, ProduitDTO produitDTO) {
        for (Produit p : produits) {
            Set<RayonProduit> rayonProduits = rayonProduitRepository.findAllByProduitId(p.getId());
            buildRayonProduits(p, produitDTO, rayonProduits);
            produitRepository.save(p);
        }
    }

    private void buildRayonProduits(Produit produitToUpdate, ProduitDTO produitDTO, Set<RayonProduit> rayonProduits) {
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
            newSet.add(new RayonProduit().setProduit(produitToUpdate)
                .setRayon(rayonFromId(produitDTO.getRayonId()))
            );
        }
        produitToUpdate.setRayonProduits(newSet);
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
        StockProduit stockProduit = buildStockProduitFromStockProduitDTO(dto, em.find(StockProduit.class, dto.getId()));
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
        FournisseurProduit fournisseurProduit = buildFournisseurProduitFromFournisseurProduitDTO(dto,
            em.find(FournisseurProduit.class, dto.getId()));
        Produit produit = em.find(Produit.class, dto.getProduitId());
        fournisseurProduit.setProduit(produit);
        em.merge(fournisseurProduit);
        em.refresh(produit);
    }

    @Override
    public void removeFournisseurProduit(Long id) throws Exception {
        FournisseurProduit fournisseurProduit = em.find(FournisseurProduit.class, id);
        Produit produit = fournisseurProduit.getProduit();
        //	produit.removeFournisseurProduit(fournisseurProduit);
        em.remove(fournisseurProduit);

    }

    @Override
    @Transactional(readOnly = true)
    public SalesLine lastSale(ProduitCriteria produitCriteria) {
        try {
            TypedQuery<SalesLine> q = em.createQuery(
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
            TypedQuery<StoreInventoryLine> q = em.createQuery(
                "SELECT o FROM StoreInventoryLine o WHERE o.storeInventory.statut=?1 AND o.produit.id=?2 "
                    + " AND o.storeInventory.magasin.id=?3  ORDER BY  o.storeInventory.updatedAt DESC",
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
    public OrderLine lastOrder(ProduitCriteria produitCriteria) {
        try {
            TypedQuery<OrderLine> q = em
                .createQuery(
                    "SELECT o FROM OrderLine o WHERE o.commande.orderStatus=?1 AND o.produit.id=?2 "
                        + " AND o.commande.magasin.id=?3 ORDER BY  o.commande.updatedAt DESC",
                    OrderLine.class);
            q.setMaxResults(1);
            q.setParameter(1, OrderStatut.CLOSED);
            q.setParameter(2, produitCriteria.getId());
            q.setParameter(3, produitCriteria.getMagasinId());
            return q.getSingleResult();
        } catch (Exception e) {
            //  LOG.debug("lastOrder =====>>>> {}", e);
        }
        return null;
    }

    @Override
    public List<ProduitDTO> lite(String query) {
        return null;
    }

    @Override
    public void save(Produit produit) throws Exception {
        produitRepository.save(produit);
    }

    @Override
    public FournisseurProduit fournisseurProduitFromDTO(ProduitDTO dto) {
        FournisseurProduit fournisseurProduit = fournisseurProduitRepository.findFirstByPrincipalIsTrueAndProduitId(dto.getId());
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        fournisseurProduit.setUpdatedAt(Instant.now());
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setPrixAchat(dto.getCostAmount());
        fournisseurProduit.setPrixUni(dto.getRegularUnitPrice());
        return fournisseurProduit;
    }

    @Override
    public FournisseurProduit fournisseurProduitProduit(Produit produit, ProduitDTO dto) {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        fournisseurProduit.setUpdatedAt(Instant.now());
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
    public void updateDetail(ProduitDTO dto) throws Exception {
        final Produit produit = buildDetailProduitFromProduitDTO(dto, em.find(Produit.class, dto.getId()));
        em.merge(produit);
        logsService.create(TransactionType.UPDATE_PRODUCT, String.format("Modification du produit %s", new Object[]{produit.getLibelle()}), produit.getId());
    }
}
