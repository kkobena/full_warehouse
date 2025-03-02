package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import com.kobe.warehouse.service.ProductActivityService;
import com.kobe.warehouse.service.dto.AbstractProduitActivity;
import com.kobe.warehouse.service.dto.AjustementActivityDTO;
import com.kobe.warehouse.service.dto.DeconditionActivityDTO;
import com.kobe.warehouse.service.dto.InventoryActivityDTO;
import com.kobe.warehouse.service.dto.OrderActivityDTO;
import com.kobe.warehouse.service.dto.ProductActivityDTO;
import com.kobe.warehouse.service.dto.RetourBonActivityDTO;
import com.kobe.warehouse.service.dto.VenteActivityDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
public class ProductActivityServiceImpl implements ProductActivityService {

    private static final String VENTE_QUERY =
        "SELECT  DATE(s.updated_at) as mvt_date, SUM(sl.quantity_requested) AS qty_mvt,MIN(s.updated_at) AS min_mvt_date,MAX(s.updated_at) AS max_mvt_date FROM sales_line sl, sales s WHERE s.id=sl.sales_id AND sl.produit_id=?1  AND   DATE(s.updated_at) BETWEEN ?2 AND ?3 AND s.statut=?4 AND s.copy=0 AND s.imported=0 GROUP BY  DATE(s.updated_at) ORDER BY  DATE(s.updated_at) ";
    private static final String AJUSTEMENT_QUERY =
        "SELECT DATE(a.date_mtv) as mvt_date,a.type_ajust,SUM(a.qty_mvt) as qty_mvt,MIN(a.date_mtv) AS min_mvt_date,MAX(a.date_mtv) AS max_mvt_date  FROM ajustement a WHERE a.produit_id=?1 AND DATE(a.date_mtv) BETWEEN ?2 AND ?3 GROUP BY a.type_ajust," +
        "mvt_date ORDER BY mvt_date";
    private static final String CANCELED_QUERY =
        "SELECT sl.produit_id, DATE(s.updated_at) as mvt_date, SUM(sl.quantity_requested) AS qty_mvt,MIN(s.updated_at) AS min_mvt_date,MAX(s.updated_at) AS max_mvt_date FROM sales_line sl, sales s WHERE s.id=sl.sales_id AND sl.produit_id=?1  AND   DATE(s.updated_at) BETWEEN ?2 AND ?3 AND s.statut=?4  AND s.imported=0 GROUP BY  DATE(s.updated_at) ORDER BY  DATE(s.updated_at)";
    private static final String DECONDITIONNEMENT_QUERY =
        "SELECT SUM(d.qty_mvt) AS qty_mvt,DATE(d.date_mtv) as mvt_date,d.type_deconditionnement AS type_decon,MIN(d.date_mtv) AS min_mvt_date,MAX(d.date_mtv) AS max_mvt_date  FROM decondition d  WHERE  d.produit_id=?1 AND DATE(d.date_mtv) BETWEEN ?2 AND ?3  GROUP BY DATE(d.date_mtv) ,d.type_deconditionnement";
    private static final String ENTRY_QUERY =
        "SELECT  SUM(it.quantity_received)AS qty_mvt,DATE(d.modified_date) as mvt_date ,MIN(it.updated_date) AS min_mvt_date,MAX(it.updated_date) AS max_mvt_date  FROM delivery_receipt_item it ,delivery_receipt d,fournisseur_produit pf ,produit p WHERE d.id=it.delivery_receipt_id AND it.fournisseur_produit_id=pf.id AND pf.produit_id=p.id AND d.receipt_status <> 'PENDING' AND  p.id=?1 AND DATE(d.modified_date) BETWEEN ?2 AND ?3 GROUP BY  p.id ,DATE(d.modified_date)";
    private static final String INVENTORY_QUERY =
        """
        SELECT SUM(it.quantity_on_hand) AS qty_mvt, DATE(s.updated_at) as mvt_date,MIN(it.updated) AS min_mvt_date,MAX(it.updated) AS max_mvt_date  FROM  store_inventory_line it, store_inventory s WHERE s.id=it.store_inventory_id
        AND s.statut=1 AND  it.produit_id=?1 AND DATE(s.updated_at) BETWEEN ?2 AND ?3
        """;
    private static final String RETOUR_BON_QUERY =
        """
        SELECT SUM(it.qty_mvt) AS qty_mvt, DATE(r.date_mtv) as mvt_date,MIN(it.date_mtv) AS min_mvt_date,MAX(it.date_mtv) AS max_mvt_date FROM retour_bon_item it,retour_bon r,delivery_receipt_item di,
        fournisseur_produit p WHERE r.id=it.retour_bon_id AND it.delivery_receipt_item_id=di.id AND di.fournisseur_produit_id=p.id AND p.produit_id=?1 AND DATE(r.date_mtv) BETWEEN ?2 AND ?3  GROUP BY DATE(r.date_mtv)
        """;

    private static final String VENTE_STOCK_QUERY =
        "SELECT sl.init_stock AS init_stock,sl.after_stock AS after_stock FROM sales_line sl, sales s WHERE s.id=sl.sales_id AND sl.produit_id=?1  AND  DATE(s.updated_at) = ?2  AND s.statut IN('CLOSED','REMOVE') AND s.imported=0 ORDER BY s.updated_at %s LIMIT 1";
    private static final String RETOUR_BON_STOCK_QUERY =
        """
        SELECT it.init_stock AS init_stock,it.after_stock AS after_stock FROM retour_bon_item it,retour_bon r,delivery_receipt_item di,
        fournisseur_produit p WHERE r.id=it.retour_bon_id AND it.delivery_receipt_item_id=di.id AND di.fournisseur_produit_id=p.id AND p.produit_id=?1 AND DATE(r.date_mtv) = ?2   ORDER BY r.date_mtv %s LIMIT 1
        """;
    private static final String INVENTORY_STOCK_QUERY =
        """
          SELECT it.quantity_init AS init_stock,it.quantity_on_hand AS after_stock  FROM  store_inventory_line it, store_inventory s WHERE s.id=it.store_inventory_id
           AND s.statut=1 AND  it.produit_id=?1 AND DATE(s.updated_at) = ?2 ORDER BY s.updated_at %s LIMIT 1
        """;
    private static final String ENTRY_STOCK_QUERY =
        """
          SELECT  it.init_stock AS init_stock,it.after_stock AS after_stock  FROM delivery_receipt_item it ,delivery_receipt d,fournisseur_produit pf ,produit p
          WHERE d.id=it.delivery_receipt_id AND it.fournisseur_produit_id=pf.id AND pf.produit_id=p.id AND d.receipt_status <> 'PENDING' AND  p.id=?1 AND DATE(d.modified_date) = ?2  ORDER BY d.modified_date %s  LIMIT 1
        """;
    private static final String DECONDITIONNEMENT_STOCK_QUERY =
        """
        SELECT  d.stock_before AS init_stock,d.stock_after AS after_stock  FROM decondition d  WHERE  d.produit_id=?1 AND DATE(d.date_mtv)  =?2 ORDER BY d.date_mtv %s  LIMIT 1
        """;
    private static final String AJUSTEMENT_STOCK_QUERY =
        """
        SELECT a.stock_before AS init_stock,a.stock_after AS after_stock  FROM ajustement a WHERE a.produit_id=?1 AND DATE(a.date_mtv) = ?2 ORDER BY a.date_mtv %s  LIMIT 1
        """;

    private final EntityManager em;
    private final List<AbstractProduitActivity> productActivities = new ArrayList<>();

    public ProductActivityServiceImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<ProductActivityDTO> getProductActivity(Long produitId, LocalDate fromDate, LocalDate toDate) {
        getAll(produitId, fromDate, toDate);
        List<ProductActivityDTO> activities = new ArrayList<>();
        productActivities
            .stream()
            .collect(Collectors.groupingBy(AbstractProduitActivity::getDateMvt))
            .forEach((e, k) -> activities.add(buildByDate(e, k, produitId)));
        activities.sort(Comparator.comparing(ProductActivityDTO::getMvtDate));
        return activities;
    }

    private void getAll(Long produitId, LocalDate fromDate, LocalDate toDate) {
        addActivities(getProduitVente(produitId, fromDate, toDate));
        addActivities(getCanceledProduitVente(produitId, fromDate, toDate));
        addActivities(getProduitAjustement(produitId, fromDate, toDate));
        addActivities(getDeconditionActivities(produitId, fromDate, toDate));
        addActivities(getOrderActivities(produitId, fromDate, toDate));
        addActivities(getInventoriesActivities(produitId, fromDate, toDate));
        addActivities(getRetourBonActivities(produitId, fromDate, toDate));
    }

    private ProductActivityDTO buildByDate(LocalDate mvtDate, List<AbstractProduitActivity> activities, Long produitId) {
        ProductActivityDTO productActivity = new ProductActivityDTO();

        productActivity.setMvtDate(mvtDate);

        AbstractProduitActivity[] max = new AbstractProduitActivity[1];
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        activities
            .stream()
            .sorted(Comparator.comparing(AbstractProduitActivity::getMin))
            .forEach(abstractProduitActivity -> {
                atomicBoolean.compareAndSet(false, true);
                if (abstractProduitActivity instanceof VenteActivityDTO venteActivity) {
                    if (venteActivity.isCanceled()) {
                        productActivity.setCanceledQuantity(venteActivity.getQtyMvt());
                    } else {
                        productActivity.setSoldQuantity(venteActivity.getQtyMvt());
                    }
                } else if (abstractProduitActivity instanceof OrderActivityDTO orderActivity) {
                    productActivity.setReceivedQuantity(orderActivity.getQtyMvt());
                } else if (abstractProduitActivity instanceof DeconditionActivityDTO deconditionActivity) {
                    if (deconditionActivity.getTypeDeconditionnement() == TypeDeconditionnement.DECONDTION_IN) {
                        productActivity.setDeconInQuantity(deconditionActivity.getQtyMvt());
                    } else {
                        productActivity.setDeconOutQuantity(deconditionActivity.getQtyMvt());
                    }
                } else if (abstractProduitActivity instanceof AjustementActivityDTO ajustementActivity) {
                    if (ajustementActivity.getAjustType() == AjustType.AJUSTEMENT_IN) {
                        productActivity.setAjustInQuantity(ajustementActivity.getQtyMvt());
                    } else {
                        productActivity.setAjustOutQuantity(ajustementActivity.getQtyMvt());
                    }
                } else if (abstractProduitActivity instanceof InventoryActivityDTO inventoryActivity) {
                    productActivity.setInventoryQuantity(inventoryActivity.getQtyMvt());
                } else if (abstractProduitActivity instanceof RetourBonActivityDTO retourBonActivity) {
                    productActivity.setRetourFourQuantity(retourBonActivity.getQtyMvt());
                }
                if (atomicBoolean.get()) {
                    Pair<Integer, Integer> integerIntegerPair = getInitAfterStock(abstractProduitActivity, produitId, mvtDate, "ASC");
                    if (Objects.nonNull(integerIntegerPair)) {
                        productActivity.setInitSock(integerIntegerPair.getLeft());
                    }
                }
                if (max.length == 1) {
                    max[0] = abstractProduitActivity;
                } else {
                    AbstractProduitActivity prev = max[0];
                    if (prev.getMax().isBefore(abstractProduitActivity.getMax())) {
                        max[0] = abstractProduitActivity;
                    }
                }
            });

        Pair<Integer, Integer> pairMax = getInitAfterStock(max[0], produitId, mvtDate, "DESC");
        if (Objects.nonNull(pairMax)) {
            productActivity.setCurrentStock(pairMax.getRight());
        }
        return productActivity;
    }

    private List<VenteActivityDTO> getProduitVente(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return getProduitVente(VENTE_QUERY, produitId, fromDate, toDate, false, SalesStatut.CLOSED);
    }

    // a revoir
    private List<VenteActivityDTO> getCanceledProduitVente(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return getProduitVente(CANCELED_QUERY, produitId, fromDate, toDate, true, SalesStatut.REMOVE);
    }

    private List<VenteActivityDTO> getProduitVente(
        String sql,
        Long produitId,
        LocalDate fromDate,
        LocalDate toDate,
        boolean canceled,
        SalesStatut salesStatut
    ) {
        List<Tuple> query =
            this.em.createNativeQuery(sql, Tuple.class)
                .setParameter(1, produitId)
                .setParameter(2, java.sql.Date.valueOf(fromDate))
                .setParameter(3, java.sql.Date.valueOf(toDate))
                .setParameter(4, salesStatut.name())
                .getResultList();
        return query.stream().map(e -> buildVenteActivity(e, canceled)).filter(Objects::nonNull).toList();
    }

    private List<AjustementActivityDTO> getProduitAjustement(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return execQuery(AJUSTEMENT_QUERY, produitId, fromDate, toDate)
            .stream()
            .map(this::buildAjustementActivity)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<DeconditionActivityDTO> getDeconditionActivities(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return execQuery(DECONDITIONNEMENT_QUERY, produitId, fromDate, toDate)
            .stream()
            .map(this::buildDeconditionActivity)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<OrderActivityDTO> getOrderActivities(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return execQuery(ENTRY_QUERY, produitId, fromDate, toDate).stream().map(this::buildOrderActivity).filter(Objects::nonNull).toList();
    }

    private List<InventoryActivityDTO> getInventoriesActivities(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return execQuery(INVENTORY_QUERY, produitId, fromDate, toDate)
            .stream()
            .map(this::buildInventoryActivityActivity)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<RetourBonActivityDTO> getRetourBonActivities(Long produitId, LocalDate fromDate, LocalDate toDate) {
        return execQuery(RETOUR_BON_QUERY, produitId, fromDate, toDate)
            .stream()
            .map(this::buildRetourBonActivity)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<Tuple> execQuery(String sqlQuery, Long produitId, LocalDate fromDate, LocalDate toDate) {
        return this.em.createNativeQuery(sqlQuery, Tuple.class)
            .setParameter(1, produitId)
            .setParameter(2, java.sql.Date.valueOf(fromDate))
            .setParameter(3, java.sql.Date.valueOf(toDate))
            .getResultList();
    }

    private AjustementActivityDTO buildAjustementActivity(Tuple tuple) {
        Pair<LocalDate, Integer> localDateIntegerPair = build(tuple);

        if (Objects.isNull(localDateIntegerPair)) return null;
        Pair<LocalDateTime, LocalDateTime> localDateTimePair = buildMixMax(tuple);
        return new AjustementActivityDTO(
            localDateIntegerPair.getLeft(),
            localDateIntegerPair.getRight(),
            AjustType.values()[tuple.get("type_ajust", Integer.class)],
            localDateTimePair.getLeft(),
            localDateTimePair.getRight()
        );
    }

    private VenteActivityDTO buildVenteActivity(Tuple tuple, boolean canceled) {
        Pair<LocalDate, Integer> localDateIntegerPair = build(tuple);

        if (Objects.isNull(localDateIntegerPair)) return null;
        Pair<LocalDateTime, LocalDateTime> localDateTimePair = buildMixMax(tuple);
        return new VenteActivityDTO(
            localDateIntegerPair.getLeft(),
            localDateIntegerPair.getRight(),
            canceled,
            localDateTimePair.getLeft(),
            localDateTimePair.getRight()
        );
    }

    private DeconditionActivityDTO buildDeconditionActivity(Tuple tuple) {
        Pair<LocalDate, Integer> localDateIntegerPair = build(tuple);

        if (Objects.isNull(localDateIntegerPair)) return null;
        Pair<LocalDateTime, LocalDateTime> localDateTimePair = buildMixMax(tuple);
        return new DeconditionActivityDTO(
            localDateIntegerPair.getLeft(),
            localDateIntegerPair.getRight(),
            TypeDeconditionnement.values()[tuple.get("type_decon", Integer.class)],
            localDateTimePair.getLeft(),
            localDateTimePair.getRight()
        );
    }

    private OrderActivityDTO buildOrderActivity(Tuple tuple) {
        Pair<LocalDate, Integer> localDateIntegerPair = build(tuple);

        if (Objects.isNull(localDateIntegerPair)) return null;
        Pair<LocalDateTime, LocalDateTime> localDateTimePair = buildMixMax(tuple);
        return new OrderActivityDTO(
            localDateIntegerPair.getLeft(),
            localDateIntegerPair.getRight(),
            localDateTimePair.getLeft(),
            localDateTimePair.getRight()
        );
    }

    private InventoryActivityDTO buildInventoryActivityActivity(Tuple tuple) {
        Pair<LocalDate, Integer> localDateIntegerPair = build(tuple);

        if (Objects.isNull(localDateIntegerPair)) return null;
        Pair<LocalDateTime, LocalDateTime> localDateTimePair = buildMixMax(tuple);
        return new InventoryActivityDTO(
            localDateIntegerPair.getLeft(),
            localDateIntegerPair.getRight(),
            localDateTimePair.getLeft(),
            localDateTimePair.getRight()
        );
    }

    private RetourBonActivityDTO buildRetourBonActivity(Tuple tuple) {
        Pair<LocalDate, Integer> localDateIntegerPair = build(tuple);

        if (Objects.isNull(localDateIntegerPair)) return null;
        Pair<LocalDateTime, LocalDateTime> localDateTimePair = buildMixMax(tuple);
        return new RetourBonActivityDTO(
            localDateIntegerPair.getLeft(),
            localDateIntegerPair.getRight(),
            localDateTimePair.getLeft(),
            localDateTimePair.getRight()
        );
    }

    private void addActivities(List<? extends AbstractProduitActivity> list) {
        productActivities.addAll(list);
    }

    private Pair<LocalDate, Integer> build(Tuple tuple) {
        Date date = tuple.get("mvt_date", Date.class);
        if (Objects.isNull(date)) return null;
        BigDecimal bigDecimal = tuple.get("qty_mvt", BigDecimal.class);
        Integer mvtQty = Objects.nonNull(bigDecimal) ? bigDecimal.intValue() : null;
        return new MutablePair<>(date.toLocalDate(), mvtQty);
    }

    private Pair<LocalDateTime, LocalDateTime> buildMixMax(Tuple tuple) {
        return new MutablePair<>(
            tuple.get("min_mvt_date", Timestamp.class).toLocalDateTime(),
            tuple.get("max_mvt_date", Timestamp.class).toLocalDateTime()
        );
    }

    private Pair<Integer, Integer> getInitAfterStock(Tuple tuple) {
        return new MutablePair<>(tuple.get("init_stock", Integer.class), tuple.get("after_stock", Integer.class));
    }

    private Pair<Integer, Integer> getInitAfterStock(String sql, Long produitId, LocalDate fromDate) {
        return Optional.of(
            (Tuple) this.em.createNativeQuery(sql, Tuple.class)
                .setParameter(1, produitId)
                .setParameter(2, java.sql.Date.valueOf(fromDate))
                .getSingleResult()
        )
            .map(this::getInitAfterStock)
            .orElseThrow();
    }

    private Pair<Integer, Integer> getInitAfterStock(
        AbstractProduitActivity abstractProduit,
        Long produitId,
        LocalDate fromDate,
        String order
    ) {
        if (abstractProduit instanceof VenteActivityDTO) {
            return getInitAfterStock(String.format(VENTE_STOCK_QUERY, order), produitId, fromDate);
        } else if (abstractProduit instanceof OrderActivityDTO) {
            return getInitAfterStock(String.format(ENTRY_STOCK_QUERY, order), produitId, fromDate);
        } else if (abstractProduit instanceof DeconditionActivityDTO) {
            return getInitAfterStock(String.format(DECONDITIONNEMENT_STOCK_QUERY, order), produitId, fromDate);
        } else if (abstractProduit instanceof AjustementActivityDTO) {
            return getInitAfterStock(String.format(AJUSTEMENT_STOCK_QUERY, order), produitId, fromDate);
        } else if (abstractProduit instanceof RetourBonActivityDTO) {
            return getInitAfterStock(String.format(RETOUR_BON_STOCK_QUERY, order), produitId, fromDate);
        } else if (abstractProduit instanceof InventoryActivityDTO) {
            return getInitAfterStock(String.format(INVENTORY_STOCK_QUERY, order), produitId, fromDate);
        }
        return null;
    }
}
