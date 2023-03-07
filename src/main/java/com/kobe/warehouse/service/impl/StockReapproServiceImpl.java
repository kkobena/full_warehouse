package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.DateUtil;
import com.kobe.warehouse.service.stock.StockReapproService;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StockReapproServiceImpl implements StockReapproService {
  private final Logger log = LoggerFactory.getLogger(StockReapproServiceImpl.class);
  private final EntityManager entityManager;
  private final AppConfigurationService appConfigurationService;

  private final TransactionTemplate transactionTemplate;

  public StockReapproServiceImpl(
      EntityManager entityManager,
      AppConfigurationService appConfigurationService,
      TransactionTemplate transactionTemplate) {
    this.entityManager = entityManager;
    this.appConfigurationService = appConfigurationService;

    this.transactionTemplate = transactionTemplate;
  }

  @Override
  // @Scheduled(cron = "0 0 8-19 * * *")
  @Scheduled(cron = "0 0/10 * * * *")
  public void computeStockReapprovisionnement() {
    Optional<AppConfiguration> appConfiguration = getLastReapproDate();
    AppConfiguration configuration = appConfiguration.get();
    boolean computeReappro = true;

    if (StringUtils.isNotEmpty(configuration.getValue())) {
      LocalDate date = LocalDate.parse(configuration.getValue());
      computeReappro = date.getMonthValue() != LocalDate.now().getMonthValue();
    }
    if (computeReappro) {
      LocalDateTime start = LocalDateTime.now();
      compute();
      configuration.setUpdated(LocalDateTime.now());
      configuration.setValue(LocalDate.now().toString());
      appConfigurationService.update(configuration);
      LocalDateTime end = LocalDateTime.now();
      long period = TimeUnit.SECONDS.convert(Duration.between(start, end));
      log.info("Temps passé en seconde {} app {}", period, appConfiguration);
    }
  }

  private void compute() {

    final LocalDate lastMonth = DateUtil.getLastMonthFromNow();
    final LocalDate threeMonthAgo = DateUtil.getNthLastMonthFromNow(3);
    final int dailyNumberStock =
        Integer.parseInt(
            appConfigurationService
                .findOneById(EntityConstant.APP_DAY_STOCK)
                .map(AppConfiguration::getValue)
                .orElse(EntityConstant.APP_DAY_STOCK_DEFAULT_VALUE + ""));
    final int nbLimit =
        Integer.parseInt(
            appConfigurationService
                .findOneById(EntityConstant.APP_LIMIT_NBR_DAY_REAPPRO)
                .map(AppConfiguration::getValue)
                .orElse(EntityConstant.APP_LIMIT_NBR_DAY_REAPPRO_DEFAULT_VALUE + ""));
    final double denominateur =
        Double.parseDouble(
            appConfigurationService
                .findOneById(EntityConstant.APP_DENOMINATEUR_REAPPRO)
                .map(AppConfiguration::getValue)
                .orElse(EntityConstant.APP_DENOMINATEUR_REAPPRO_DEFAULT_VALUE + ""));
    fetchProductAndQuantitySold(threeMonthAgo, lastMonth).parallelStream()
        .forEach(tuple -> computeProduitQtyReappro(tuple, dailyNumberStock, nbLimit, denominateur));
  }

  private List<Tuple> fetchProductAndQuantitySold(LocalDate threeMonthAgo, LocalDate lastMonth) {
    String sqlQuery =
        """
SELECT p.id,p.item_qty,(SELECT COALESCE(SUM(sl.quantity_sold),0) FROM sales_line sl,sales s WHERE sl.produit_id=p.id

 AND  s.id=sl.sales_id AND s.statut='CLOSED' AND DATE(s.updated_at) BETWEEN ?1 AND ?2

) AS qtySold, (SELECT COALESCE(SUM(sl.quantity_sold),0) FROM sales_line sl,sales s,produit pd WHERE sl.produit_id=pd.id

 AND  s.id=sl.sales_id AND s.statut='CLOSED' AND pd.type_produit=0 AND pd.parent_id=p.id AND DATE(s.updated_at) BETWEEN ?3 AND ?4
) AS itemQtySold FROM produit p where p.status=0 AND p.type_produit=1
          """;
    try {
      Query q = entityManager.createNativeQuery(sqlQuery, Tuple.class);
      q.setParameter(1, java.sql.Date.valueOf(threeMonthAgo), TemporalType.DATE);
      q.setParameter(2, java.sql.Date.valueOf(lastMonth), TemporalType.DATE);
      q.setParameter(3, java.sql.Date.valueOf(threeMonthAgo), TemporalType.DATE);
      q.setParameter(4, java.sql.Date.valueOf(lastMonth), TemporalType.DATE);
      return q.getResultList();
    } catch (Exception e) {
      log.debug(null, e);
      return Collections.emptyList();
    }
  }

  private void computeProduitQtyReappro(
      Tuple tuple, int dayStock, int delayReappro, double denominateurReappro) {
    long id = tuple.get("id", BigInteger.class).intValue();
    int qtySold = tuple.get("qtySold", BigDecimal.class).intValue();
    int itemQtySold = tuple.get("itemQtySold", BigDecimal.class).intValue();
    int itemQty = tuple.get("item_qty", Integer.class);
    if (itemQtySold > 0) {
      qtySold = qtySold + ((int) Math.ceil(itemQtySold / Double.valueOf(itemQty)));
    }
    double soldQuantityAvg = qtySold / denominateurReappro;
    int seuilMin = (int) Math.ceil(soldQuantityAvg * dayStock);
    int quantityReappro = (int) Math.ceil(soldQuantityAvg * delayReappro);
    updateProduit(id, seuilMin, quantityReappro);
  }

  private void updateProduit(long id, int seuilMin, int quantityReappro) {
    transactionTemplate.execute(
        new TransactionCallbackWithoutResult() {
          @Override
          protected void doInTransactionWithoutResult(TransactionStatus status) {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaUpdate<Produit> update = cb.createCriteriaUpdate(Produit.class);
            Root<Produit> root = update.from(Produit.class);
            update.set(Produit_.qtyAppro, quantityReappro);
            update.set(Produit_.qtySeuilMini, seuilMin);
            update.set(Produit_.updatedAt, Instant.now());
            update.where(cb.equal(root.get(Produit_.id), id));
            entityManager.createQuery(update).executeUpdate();
          }
        });
  }

  private Optional<AppConfiguration> getLastReapproDate() {
    return appConfigurationService.findOneById(EntityConstant.APP_LAST_DAY_REAPPRO);
  }
}
