package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.builder.ProductStatQueryBuilder;
import com.kobe.warehouse.service.dto.produit.AuditType;
import com.kobe.warehouse.service.dto.produit.ProduitAuditing;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import com.kobe.warehouse.service.stat.ProductStatService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ProductStatServiceImpl implements ProductStatService {
  private static final String SALE_LINE_AUDIT_SQL_QUERY =
      "SELECT s.canceled, s.dtype AS sale_type,sl.quantity_requested,sl.quantity_sold,sl.quantity_avoir,sl.init_stock,sl.after_stock,s.updated_at, DATE(s.updated_at) AS mvt_date from  sales s join sales_line sl on s.id = sl.sales_id WHERE sl.produit_id=?1 AND s.statut=?2 AND DATE(s.updated_at) BETWEEN ?3 AND ?4 ORDER BY s.updated_at";
  private static final String INVENTORY_LINE_AUDIT_SQL_QUERY =
      "SELECT sl.quantity_init,sl.quantity_on_hand, DATE(s.updated_at) AS mvt_date ,s.updated_at AS mvt_date FROM  store_inventory_line sl join store_inventory s on sl.store_inventory_id = s.id WHERE s.statut=2 AND sl.produit_id=?1 AND DATE(s.updated_at) BETWEEN ?2 AND ?3 ORDER BY s.updated_at";

  private static final String DECON_AUDIT_SQL_QUERY =
      "SELECT decon.qty_mvt,decon.stock_before,decon.stock_after, DATE(decon.date_mtv) as mvt_date,decon.date_mtv as updated_at ,decon.type_deconditionnement from  decondition decon where decon.produit_id=?1  AND DATE(decon.date_mtv) BETWEEN ?2 AND ?3 ORDER BY decon.date_mtv";

  private static final String AJUSTEMENT_AUDIT_SQL_QUERY =
      "SELECT  DATE(a.date_mtv) as mvt_date, a.type_ajust,a.date_mtv as updated_at,a.stock_after,a.stock_before,a.qty_mvt FROM  ajustement a JOIN ajust a2 on a.ajust_id = a2.id WHERE a2.statut=1 AND a.produit_id=?1  AND DATE(a.date_mtv) BETWEEN ?2 AND ?3 ORDER BY a.date_mtv";
  private static final String DELEVERY_AUDIT_SQL_QUERY =
      """
  SELECT d.after_stock,d.init_stock,d.quantity_requested,d.quantity_received,d.quantity_ug,DATE(d.updated_date)  as mvt_date ,d.updated_date as updated_at from delivery_receipt_item d JOIN delivery_receipt dr on d.delivery_receipt_id = dr.id
  JOIN fournisseur_produit fp on d.fournisseur_produit_id = fp.id join produit p on fp.produit_id = p.id WHERE dr.receipt_status='CLOSE' AND p.id=?1 AND DATE(d.updated_date) BETWEEN ?3 AND ?4 ORDER BY d.updated_date
""";
  private static final String RETOUR_AUDIT_SQL_QUERY =
      """
SELECT DATE(rt.date_mtv) as mvt_date,rt.date_mtv as updated_at,rt.init_stock,rt.after_stock,rt.qty_mvt FROM retour_bon_item rt JOIN retour_bon rb on rt.retour_bon_id = rb.id
join delivery_receipt_item it on rt.delivery_receipt_item_id = it.id
join fournisseur_produit fp on it.fournisseur_produit_id = fp.id
join produit p on fp.produit_id = p.id WHERE p.id=?1 AND rb.statut=1
 AND DATE(rt.date_mtv) BETWEEN ?3 AND ?4 ORDER BY rt.date_mtv
""";
  private static final String PERIME_AUDIT_SQL_QUERY =
      """
SELECT pp.created as updated_at,DATE(pp.created) as mvt_date,pp.quantity,pp.init_stock,pp.after_stock FROM
produit_perime pp join produit p on pp.produit_id = p.id WHERE p.id=?1 AND DATE(pp.created) BETWEEN ?2 AND ?3 order by pp.created
""";
  private final Logger LOG = LoggerFactory.getLogger(ProductStatServiceImpl.class);
  private final EntityManager em;
  private final StockProduitRepository stockProduitRepository;
  private final StorageService storageService;

  public ProductStatServiceImpl(
      EntityManager em,
      StockProduitRepository stockProduitRepository,
      StorageService storageService) {
    this.em = em;
    this.stockProduitRepository = stockProduitRepository;
    this.storageService = storageService;
  }

  @Override
  public List<ProductStatRecord> fetchProductStat(ProduitRecordParamDTO produitRecordParam) {
    return buildProductStatRecord(produitRecordParam);
  }

  @Override
  public List<ProductStatParetoRecord> fetch20x80(ProduitRecordParamDTO produitRecordParam) {
    return buildProductStat20x80Record(produitRecordParam);
  }

  private List<Tuple> getExecQuery(ProduitRecordParamDTO produitRecordParam) {
    Pair<LocalDate, LocalDate> periode = this.buildPeriode(produitRecordParam);
    try {
      return this.em
          .createNativeQuery(this.buildPrduduitQuery(produitRecordParam), Tuple.class)
          .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
          .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return Collections.emptyList();
  }

  private List<ProductStatRecord> buildProductStatRecord(ProduitRecordParamDTO produitRecordParam) {
    List<Tuple> list = getExecQuery(produitRecordParam);
    if (!CollectionUtils.isEmpty(list)) {
      return list.stream().map(ProductStatQueryBuilder::buildProductStatRecord).toList();
    }
    return Collections.emptyList();
  }

  private List<Tuple> getExec20X80Query(
      ProduitRecordParamDTO produitRecordParam, Pair<LocalDate, LocalDate> periode) {

    try {
      return this.em
          .createNativeQuery(this.buildPerotoQuery(produitRecordParam), Tuple.class)
          .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
          .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
          .setFirstResult(produitRecordParam.getStart())
          .setMaxResults(produitRecordParam.getLimit())
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return Collections.emptyList();
  }

  private List<ProductStatParetoRecord> buildProductStat20x80Record(
      ProduitRecordParamDTO produitRecordParam) {

    Pair<LocalDate, LocalDate> periode = this.buildPeriode(produitRecordParam);
    long totalCount = getTotalCount(produitRecordParam, periode);
    int count = 0;
    List<ProductStatParetoRecord> results = new ArrayList<>();
    double quantityAvg = 0.0;
    double amountAvg = 0.0;
    double quantityRefAvg = 20.0;
    double quantityRefAvgLimit = 50.0;
    double amountRefAvg = 80.0;
    int start = 0;
    int limit = 1000;
    List<Tuple> list;
    produitRecordParam.setStart(start);
    produitRecordParam.setLimit(limit);
    boolean isNotSatisfied = true;
    while ((count <= totalCount) && isNotSatisfied) {
      list = getExec20X80Query(produitRecordParam, periode);
      for (Tuple tuple : list) {
        quantityAvg +=
            tuple
                .get("quantity_avg", BigDecimal.class)
                .round(new MathContext(2, RoundingMode.HALF_UP))
                .doubleValue();
        amountAvg +=
            tuple
                .get("amount_avg", BigDecimal.class)
                .round(new MathContext(2, RoundingMode.HALF_UP))
                .doubleValue();
        if (quantityAvg <= quantityRefAvg) {
          if (amountAvg <= amountRefAvg) {
            results.add(ProductStatQueryBuilder.buildProductStatParetoRecord(tuple));
          } else {
            results.add(ProductStatQueryBuilder.buildProductStatParetoRecord(tuple));
            isNotSatisfied = false;
            break;
          }
        } else {
          if (quantityAvg < quantityRefAvgLimit && amountAvg <= amountRefAvg) {
            results.add(ProductStatQueryBuilder.buildProductStatParetoRecord(tuple));
          } else {
            isNotSatisfied = false;
            break;
          }
        }
      }
      start += limit;
      count += limit;
      produitRecordParam.setStart(start);
      produitRecordParam.setLimit(limit);
    }

    return results;
  }

  private long getTotalCount(
      ProduitRecordParamDTO produitRecordParam, Pair<LocalDate, LocalDate> periode) {

    try {
      return (Long)
          this.em
              .createNativeQuery(this.buildPCountQuey(produitRecordParam))
              .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
              .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
              .getSingleResult();

    } catch (Exception e) {
      LOG.error(null, e);
      return 0;
    }
  }

  private List<Tuple> fetchProduitSaleAuditing(ProduitAuditingParam produitAuditingParam) {

    try {
      return this.em
          .createNativeQuery(SALE_LINE_AUDIT_SQL_QUERY, Tuple.class)
          .setParameter(1, produitAuditingParam.produitId())
          .setParameter(2, SalesStatut.CLOSED.name())
          .setParameter(3, produitAuditingParam.fromDate())
          .setParameter(4, produitAuditingParam.toDate())
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return Collections.emptyList();
  }

  private ProduitAuditing buildProduitSaleAuditingFromTuple(Tuple tuple) {

    return new ProduitAuditing(
        AuditType.SALE,
        tuple.get("quantity_sold", Integer.class),
        tuple.get("init_stock", Integer.class),
        tuple.get("after_stock", Integer.class),
        tuple.get("updated_at", LocalDateTime.class),
        tuple.get("mvt_date", LocalDate.class),
        tuple.get("canceled", Boolean.class),
        tuple.get("sale_type", String.class),
        null,
        null);
  }

  private List<Tuple> fetchProduitAuditing(
      String query, ProduitAuditingParam produitAuditingParam) {
    try {
      return this.em
          .createNativeQuery(query, Tuple.class)
          .setParameter(1, produitAuditingParam.produitId())
          .setParameter(2, produitAuditingParam.fromDate())
          .setParameter(3, produitAuditingParam.toDate())
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return Collections.emptyList();
  }

  private ProduitAuditing buildProduitInventoryAuditingFromTuple(Tuple tuple) {

    return new ProduitAuditing(
        AuditType.INVENTORY,
        tuple.get("quantity_on_hand", Integer.class),
        tuple.get("quantity_init", Integer.class),
        tuple.get("quantity_on_hand", Integer.class),
        tuple.get("updated_at", LocalDateTime.class),
        tuple.get("mvt_date", LocalDate.class),
        null,
        null,
        null,
        null);
  }

  private List<ProduitAuditing> buildFromProduitRetourAuditing(
      ProduitAuditingParam produitAuditingParam) {
    return fetchProduitAuditing(RETOUR_AUDIT_SQL_QUERY, produitAuditingParam).stream()
        .map(this::buildProduitRetourAuditingFromTuple)
        .toList();
  }

  private List<ProduitAuditing> buildProduitInventoryAuditing(
      ProduitAuditingParam produitAuditingParam) {
    return fetchProduitAuditing(INVENTORY_LINE_AUDIT_SQL_QUERY, produitAuditingParam).stream()
        .map(this::buildProduitInventoryAuditingFromTuple)
        .toList();
  }

  private List<ProduitAuditing> buildFromProduitSaleAuditing(
      ProduitAuditingParam produitAuditingParam) {
    return fetchProduitSaleAuditing(produitAuditingParam).stream()
        .map(this::buildProduitSaleAuditingFromTuple)
        .toList();
  }

  private List<ProduitAuditing> buildFromProduitAjustementAuditing(
      ProduitAuditingParam produitAuditingParam) {
    return fetchProduitAuditing(AJUSTEMENT_AUDIT_SQL_QUERY, produitAuditingParam).stream()
        .map(this::buildProduitAjustementAuditingFromTuple)
        .toList();
  }

  private List<ProduitAuditing> buildFromProduitDeconditionAuditing(
      ProduitAuditingParam produitAuditingParam) {
    return fetchProduitAuditing(DECON_AUDIT_SQL_QUERY, produitAuditingParam).stream()
        .map(this::buildProduitDeconditionAuditingFromTuple)
        .toList();
  }

  private List<ProduitAuditing> buildFromProduitDeleveryAuditing(
      ProduitAuditingParam produitAuditingParam) {
    return fetchProduitAuditing(DELEVERY_AUDIT_SQL_QUERY, produitAuditingParam).stream()
        .map(this::buildProduitDeleveryAuditingFromTuple)
        .toList();
  }

  private List<ProduitAuditing> buildFromPerimesAuditing(
      ProduitAuditingParam produitAuditingParam) {
    return fetchProduitAuditing(PERIME_AUDIT_SQL_QUERY, produitAuditingParam).stream()
        .map(this::buildPerimeyAuditing)
        .toList();
  }

  private ProduitAuditing buildProduitDeconditionAuditingFromTuple(Tuple tuple) {
    TypeDeconditionnement typeDeconditionnement =
        TypeDeconditionnement.values()[tuple.get("type_deconditionnement", Integer.class)];
    return new ProduitAuditing(
        AuditType.DECONDITIONNEMENT,
        tuple.get("qty_mvt", Integer.class),
        tuple.get("stock_before", Integer.class),
        tuple.get("stock_after", Integer.class),
        tuple.get("updated_at", LocalDateTime.class),
        tuple.get("mvt_date", LocalDate.class),
        null,
        null,
        typeDeconditionnement,
        null);
  }

  private ProduitAuditing buildProduitAjustementAuditingFromTuple(Tuple tuple) {
    AjustType ajustType = AjustType.values()[tuple.get("type_ajust", Integer.class)];
    return new ProduitAuditing(
        AuditType.AJUSTEMENT,
        tuple.get("qty_mvt", Integer.class),
        tuple.get("stock_before", Integer.class),
        tuple.get("stock_after", Integer.class),
        tuple.get("updated_at", LocalDateTime.class),
        tuple.get("mvt_date", LocalDate.class),
        null,
        null,
        null,
        ajustType);
  }

  private ProduitAuditing buildProduitDeleveryAuditingFromTuple(Tuple tuple) {
    return new ProduitAuditing(
        AuditType.DELIVERY,
        tuple.get("quantity_received", Integer.class),
        tuple.get("init_stock", Integer.class),
        tuple.get("after_stock", Integer.class),
        tuple.get("updated_at", LocalDateTime.class),
        tuple.get("mvt_date", LocalDate.class),
        null,
        null,
        null,
        null);
  }

  private ProduitAuditing buildProduitRetourAuditingFromTuple(Tuple tuple) {

    return new ProduitAuditing(
        AuditType.RETOUR,
        tuple.get("qty_mvt", Integer.class),
        tuple.get("init_stock", Integer.class),
        tuple.get("after_stock", Integer.class),
        tuple.get("updated_at", LocalDateTime.class),
        tuple.get("mvt_date", LocalDate.class),
        null,
        null,
        null,
        null);
  }

  private ProduitAuditing buildPerimeyAuditing(Tuple tuple) {
    return new ProduitAuditing(
        AuditType.PERIME,
        tuple.get("quantity", Integer.class),
        tuple.get("init_stock", Integer.class),
        tuple.get("after_stock", Integer.class),
        tuple.get("updated_at", LocalDateTime.class),
        tuple.get("mvt_date", LocalDate.class),
        null,
        null,
        null,
        null);
  }

  @Override
  public List<ProduitAuditingState> fetchProduitDailyTransaction(
      ProduitAuditingParam produitAuditingParam) {
    List<ProduitAuditingState> produitAuditingStates = new ArrayList<>();
    groupingProduitAuditingByDay(produitAuditingParam)
        .forEach(
            (k, value) ->
                produitAuditingStates.add(
                    buildProduitAuditingState(k, value, produitAuditingParam.produitId())));

    return produitAuditingStates;
  }

  private ProduitAuditingState buildProduitAuditingState(
      LocalDate mvtDate, List<ProduitAuditing> produitAuditings, Long produitId) {
    ProduitAuditingState produitAuditingState = new ProduitAuditingState();
    produitAuditings.sort(Comparator.comparing(ProduitAuditing::updated));
    ProduitAuditing produitAuditingFirst = produitAuditings.get(0);
    produitAuditingState.setInitStock(produitAuditingFirst.beforeStock());
    produitAuditingState.setMvtDate(mvtDate);

    int currentStock =
        this.stockProduitRepository
            .findStockProduitByStorageIdAndProduitId(
                this.storageService.getDefaultConnectedUserMainStorage().getId(), produitId)
            .orElseThrow()
            .getQtyStock();
    produitAuditingState.setCurrentStock(currentStock);
    Map<AuditType, List<ProduitAuditing>> groupingByType =
        produitAuditings.stream().collect(Collectors.groupingBy(ProduitAuditing::auditType));
    for (Map.Entry<AuditType, List<ProduitAuditing>> entry : groupingByType.entrySet()) {
      switch (entry.getKey()) {
        case SALE -> {
          Map<Boolean, Integer> venteQuantity =
              entry.getValue().stream()
                  .collect(
                      Collectors.groupingBy(
                          ProduitAuditing::canceled,
                          Collectors.summingInt(ProduitAuditing::qtyMvt)));
          produitAuditingState.setSaleQuantity(venteQuantity.get(false));
          produitAuditingState.setCanceledQuantity(venteQuantity.get(true));
        }
        case PERIME -> produitAuditingState.setPerimeQuantity(
            entry.getValue().stream().mapToInt(ProduitAuditing::qtyMvt).sum());

        case INVENTORY -> {
          ProduitAuditing produitAuditingInven = entry.getValue().get(0);
          produitAuditingState.setStoreInventoryQuantity(produitAuditingInven.qtyMvt());
          produitAuditingState.setInventoryGap(
              produitAuditingInven.beforeStock() - produitAuditingInven.qtyMvt());
        }
        case DELIVERY -> produitAuditingState.setDeleveryQuantity(
            entry.getValue().stream().mapToInt(ProduitAuditing::qtyMvt).sum());
        case RETOUR -> produitAuditingState.setRetourFournisseurQuantity(
            entry.getValue().stream().mapToInt(ProduitAuditing::qtyMvt).sum());
        case AJUSTEMENT -> {
          Map<AjustType, Integer> ajustTypeInteger =
              entry.getValue().stream()
                  .collect(
                      Collectors.groupingBy(
                          ProduitAuditing::ajustType,
                          Collectors.summingInt(ProduitAuditing::qtyMvt)));
          produitAuditingState.setAjustementPositifQuantity(
              ajustTypeInteger.get(AjustType.AJUSTEMENT_IN));
          produitAuditingState.setAjustementNegatifQuantity(
              ajustTypeInteger.get(AjustType.AJUSTEMENT_OUT));
        }
        case DECONDITIONNEMENT -> {
          Map<TypeDeconditionnement, Integer> typeDeconditionnementIntegerMap =
              entry.getValue().stream()
                  .collect(
                      Collectors.groupingBy(
                          ProduitAuditing::typeDeconditionnement,
                          Collectors.summingInt(ProduitAuditing::qtyMvt)));
          produitAuditingState.setDeconPositifQuantity(
              typeDeconditionnementIntegerMap.get(TypeDeconditionnement.DECONDTION_OUT));
          produitAuditingState.setDeconNegatifQuantity(
              typeDeconditionnementIntegerMap.get(TypeDeconditionnement.DECONDTION_IN));
        }
      }
    }

    return produitAuditingState;
  }

  private Map<LocalDate, List<ProduitAuditing>> groupingProduitAuditingByDay(
      ProduitAuditingParam produitAuditingParam) {
    return collectAudits(produitAuditingParam).stream()
        .sorted(Comparator.comparing(ProduitAuditing::updated))
        .collect(Collectors.groupingBy(ProduitAuditing::mvtDate));
  }

  private List<ProduitAuditing> collectAudits(ProduitAuditingParam produitAuditingParam) {
    List<ProduitAuditing> produitAuditings = new ArrayList<>();

    List<ProduitAuditing> sales = buildFromProduitSaleAuditing(produitAuditingParam);
    if (!CollectionUtils.isEmpty(sales)) {
      produitAuditings.addAll(sales);
    }
    List<ProduitAuditing> inventories = buildProduitInventoryAuditing(produitAuditingParam);
    if (!CollectionUtils.isEmpty(inventories)) {
      produitAuditings.addAll(inventories);
    }
    List<ProduitAuditing> ajustements = buildFromProduitAjustementAuditing(produitAuditingParam);
    if (!CollectionUtils.isEmpty(ajustements)) {
      produitAuditings.addAll(ajustements);
    }

    List<ProduitAuditing> deconditionnements =
        buildFromProduitDeconditionAuditing(produitAuditingParam);
    if (!CollectionUtils.isEmpty(deconditionnements)) {
      produitAuditings.addAll(deconditionnements);
    }
    List<ProduitAuditing> deleveries = buildFromProduitDeleveryAuditing(produitAuditingParam);
    if (!CollectionUtils.isEmpty(deleveries)) {
      produitAuditings.addAll(deleveries);
    }

    List<ProduitAuditing> retours = buildFromProduitRetourAuditing(produitAuditingParam);
    if (!CollectionUtils.isEmpty(retours)) {
      produitAuditings.addAll(retours);
    }
    List<ProduitAuditing> perimes = buildFromPerimesAuditing(produitAuditingParam);
    if (!CollectionUtils.isEmpty(perimes)) {
      produitAuditings.addAll(perimes);
    }
    return produitAuditings;
  }
}
