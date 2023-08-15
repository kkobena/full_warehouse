package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.StatistiqueProduit;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.stat.AchatStatService;
import com.kobe.warehouse.service.stat.DashboardService;
import com.kobe.warehouse.service.stat.SaleStatService;
import com.kobe.warehouse.service.utils.ServiceUtil;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
  private final Logger LOG = LoggerFactory.getLogger(DashboardServiceImpl.class);

  private final EntityManager em;
  private final SaleStatService saleStatService;
  private final AchatStatService achatStatService;

  public DashboardServiceImpl(
      EntityManager em, SaleStatService saleStatService, AchatStatService achatStatService) {
    this.em = em;
    this.saleStatService = saleStatService;
    this.achatStatService = achatStatService;
  }

  public List<StatistiqueProduit> statistiqueProduitsQunatityMonthly(
      LocalDate localDate, int maxResult) {
    try {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<StatistiqueProduit> cq = cb.createQuery(StatistiqueProduit.class);
      Root<SalesLine> root = cq.from(SalesLine.class);
      Join<SalesLine, Sales> saleJoin = root.join("sales");
      Join<SalesLine, Produit> produitJoin = root.join("produit");
      cq.select(
              cb.construct(
                  StatistiqueProduit.class,
                  cb.sumAsLong(root.get("quantitySold")),
                  produitJoin.get("libelle")))
          .groupBy(produitJoin.get("id"))
          .orderBy(cb.desc(cb.sumAsLong(root.get("quantitySold"))));
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(
          cb.greaterThanOrEqualTo(
              saleJoin.get("dateDimension").get("dateKey"),
              ServiceUtil.dateDimensionKey(localDate.minusMonths(1))));
      predicates.add(cb.equal(saleJoin.get("statut"), SalesStatut.CLOSED));
      cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
      TypedQuery<StatistiqueProduit> q = em.createQuery(cq);
      q.setMaxResults(maxResult);
      return q.getResultList();
    } catch (Exception e) {
      LOG.debug("statistiqueProduitsQunatityMonthly====>>", e);
      return Collections.emptyList();
    }
  }

  public List<StatistiqueProduit> statistiqueProduitsQunatityYearly(
      LocalDate localDate, int maxResult) {
    try {

      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<StatistiqueProduit> cq = cb.createQuery(StatistiqueProduit.class);
      Root<SalesLine> root = cq.from(SalesLine.class);
      Join<SalesLine, Sales> saleJoin = root.join("sales");
      Join<SalesLine, Produit> produitJoin = root.join("produit");
      cq.select(
              cb.construct(
                  StatistiqueProduit.class,
                  cb.sumAsLong(root.get("quantitySold")),
                  produitJoin.get("libelle")))
          .groupBy(produitJoin.get("id"))
          .orderBy(cb.desc(cb.sumAsLong(root.get("quantitySold"))));
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(saleJoin.get("dateDimension").get("year"), localDate.getYear()));
      predicates.add(cb.equal(saleJoin.get("statut"), SalesStatut.CLOSED));
      cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
      TypedQuery<StatistiqueProduit> q = em.createQuery(cq);
      q.setMaxResults(maxResult);
      return q.getResultList();
    } catch (Exception e) {
      LOG.debug("statistiqueProduitsQunatityYearly====>>", e);
      return Collections.emptyList();
    }
  }

  public List<StatistiqueProduit> statistiqueProduitsAmountYearly(
      LocalDate localDate, int maxResult) {
    try {

      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<StatistiqueProduit> cq = cb.createQuery(StatistiqueProduit.class);
      Root<SalesLine> root = cq.from(SalesLine.class);
      Join<SalesLine, Sales> saleJoin = root.join(SalesLine_.sales);
      Join<SalesLine, Produit> produitJoin = root.join(SalesLine_.produit);
      cq.select(
              cb.construct(
                  StatistiqueProduit.class,
                  produitJoin.get(Produit_.libelle),
                  cb.sumAsLong(root.get(SalesLine_.salesAmount))))
          .groupBy(produitJoin.get(Produit_.id))
          .orderBy(cb.desc(cb.sumAsLong(root.get(SalesLine_.salesAmount))));
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(
          cb.equal(
              cb.function("YEAR", Year.class, saleJoin.get(Sales_.updatedAt)),
              localDate.getYear()));
      predicates.add(cb.equal(saleJoin.get(Sales_.statut), SalesStatut.CLOSED));
      cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
      TypedQuery<StatistiqueProduit> q = em.createQuery(cq);
      q.setMaxResults(maxResult);
      return q.getResultList();
    } catch (Exception e) {
      LOG.debug("statistiqueProduitsAmountYearly====>>", e);
      return Collections.emptyList();
    }
  }

  public List<StatistiqueProduit> statistiqueProduitsAmountMonthly(
      LocalDate localDate, int maxResult) {
    try {

      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<StatistiqueProduit> cq = cb.createQuery(StatistiqueProduit.class);
      Root<SalesLine> root = cq.from(SalesLine.class);
      Join<SalesLine, Sales> saleJoin = root.join("sales");
      Join<SalesLine, Produit> produitJoin = root.join("produit");
      cq.select(
              cb.construct(
                  StatistiqueProduit.class,
                  produitJoin.get("libelle"),
                  cb.sumAsLong(root.get("salesAmount"))))
          .groupBy(produitJoin.get("id"))
          .orderBy(cb.desc(cb.sumAsLong(root.get("salesAmount"))));
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(
          cb.greaterThanOrEqualTo(
              cb.function("MONTH", Month.class, saleJoin.get(Sales_.updatedAt)),
              localDate.getMonth()));
      predicates.add(cb.equal(saleJoin.get("statut"), SalesStatut.CLOSED));
      cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
      TypedQuery<StatistiqueProduit> q = em.createQuery(cq);
      q.setMaxResults(maxResult);
      return q.getResultList();
    } catch (Exception e) {
      LOG.debug("statistiqueProduitsAmountMonthly====>>", e);
      return Collections.emptyList();
    }
  }

  @Override
  public VenteRecordWrapper getPeridiqueCa(VenteRecordParamDTO venteRecordParamDTO) {
    return this.saleStatService.getPeridiqueCa(venteRecordParamDTO);
  }

  @Override
  public AchatRecord getAchatPeriode(AchatRecordParamDTO achatRecordParam) {
    return this.achatStatService.getAchatPeriode(achatRecordParam);
  }

  @Override
  public List<VentePeriodeRecord> getCaGroupingByPeriode(VenteRecordParamDTO venteRecordParamDTO) {
    return this.saleStatService.getCaGroupingByPeriode(venteRecordParamDTO);
  }

  @Override
  public List<VenteByTypeRecord> getCaGroupingByType(VenteRecordParamDTO venteRecordParamDTO) {
    return this.saleStatService.getCaGroupingByType(venteRecordParamDTO);
  }

  @Override
  public List<VenteModePaimentRecord> getCaGroupingByPaimentMode(
      VenteRecordParamDTO venteRecordParamDTO) {
    return this.saleStatService.getCaGroupingByPaimentMode(venteRecordParamDTO);
  }
}
