package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.service.dto.ProduitRecordParamDTO;
import com.kobe.warehouse.service.dto.records.ProductStatParetoRecord;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import com.kobe.warehouse.service.stat.ProductStatService;
import com.kobe.warehouse.service.utils.ProductStatQueryBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ProductStatServiceImpl implements ProductStatService {
  private final Logger LOG = LoggerFactory.getLogger(ProductStatServiceImpl.class);
  private final EntityManager em;

  public ProductStatServiceImpl(EntityManager em) {
    this.em = em;
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

    Pair<LocalDate, LocalDate> periode =this.buildPeriode(produitRecordParam);
    int totalCount = getTotalCount(produitRecordParam, periode);
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
          quantityAvg += tuple.get("quantity_avg", BigDecimal.class).round(new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
          amountAvg += tuple.get("amount_avg", BigDecimal.class).round(new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
        if(quantityAvg<=quantityRefAvg){
          if(amountAvg <= amountRefAvg){
            results.add(ProductStatQueryBuilder.buildProductStatParetoRecord(tuple));
          }else{
            results.add(ProductStatQueryBuilder.buildProductStatParetoRecord(tuple));
            isNotSatisfied=false;
            break;
          }
        }else{
          if(quantityAvg<quantityRefAvgLimit && amountAvg <= amountRefAvg){
            results.add(ProductStatQueryBuilder.buildProductStatParetoRecord(tuple));
          }else{
            isNotSatisfied=false;
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

  private int getTotalCount(
      ProduitRecordParamDTO produitRecordParam, Pair<LocalDate, LocalDate> periode) {

    try {
      return ((BigInteger)
              this.em
                  .createNativeQuery(this.buildPCountQuey(produitRecordParam))
                  .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
                  .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
                  .getSingleResult())
          .intValue();

    } catch (Exception e) {
      LOG.error(null, e);
      return 0;
    }
  }
}
