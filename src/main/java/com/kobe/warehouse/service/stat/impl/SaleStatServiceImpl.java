package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.stat.SaleStatService;
import com.kobe.warehouse.service.utils.VenteStatQueryBuilder;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional(readOnly = true)
public class SaleStatServiceImpl implements SaleStatService {
  private final Logger LOG = LoggerFactory.getLogger(SaleStatServiceImpl.class);
  private final EntityManager em;

  public SaleStatServiceImpl(EntityManager em) {
    this.em = em;
  }

  private VenteRecord getCaByPeriode(
      VenteRecordParamDTO venteRecordParamDTO, Pair<LocalDate, LocalDate> periode) {

    Tuple tuple =
        getCaQuery(
            VenteStatQueryBuilder.PERIOQIQUE_CA_QUERY,
            venteRecordParamDTO,
            periode.getLeft(),
            periode.getRight());
    if (Objects.nonNull(tuple)) {
      return VenteStatQueryBuilder.buildVenteRecord(tuple);
    }
    return null;
  }

  @Override
  public VenteRecordWrapper getPeridiqueCa(VenteRecordParamDTO venteRecordParamDTO) {
    Pair<LocalDate, LocalDate> localDateLocalDatePair = getPeriode(venteRecordParamDTO);
    venteRecordParamDTO.setCanceled(false);
    VenteRecord venteRecord = getCaByPeriode(venteRecordParamDTO, localDateLocalDatePair);
    venteRecordParamDTO.setCanceled(true);
    VenteRecord venteRecordAnnulation = getCaByPeriode(venteRecordParamDTO, localDateLocalDatePair);

    return new VenteRecordWrapper(venteRecord, venteRecordAnnulation);
  }

  private Pair<LocalDate, LocalDate> getPeriode(VenteRecordParamDTO venteRecordParamDTO) {
    return this.buildPeriode(
        venteRecordParamDTO.getFromDate(),
        venteRecordParamDTO.getToDate(),
        venteRecordParamDTO.getDashboardPeriode());
  }

  private Tuple getCaQuery(
      String sql, VenteRecordParamDTO venteRecordParamDTO, LocalDate fromDate, LocalDate toDate) {
    try {

      return (Tuple)
          this.em
              .createNativeQuery(buildQuery(sql, venteRecordParamDTO), Tuple.class)
              .setParameter(1, java.sql.Date.valueOf(fromDate))
              .setParameter(2, java.sql.Date.valueOf(toDate))
              .setParameter(
                  3,
                  List.of(
                      venteRecordParamDTO.isCanceled()
                          ? SalesStatut.CANCELED.name()
                          : SalesStatut.CLOSED.name()))
              .getSingleResult();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return null;
  }

  @Override
  public List<VentePeriodeRecord> getCaGroupingByPeriode(VenteRecordParamDTO venteRecordParamDTO) {

    return buildGroupingByPeriode(venteRecordParamDTO);
  }

  @Override
  public List<VenteByTypeRecord> getCaGroupingByType(VenteRecordParamDTO venteRecordParamDTO) {
    return buildGroupingByType(venteRecordParamDTO);
  }

  @Override
  public List<VenteModePaimentRecord> getCaGroupingByPaimentMode(
      VenteRecordParamDTO venteRecordParamDTO) {
    return buildModePaimentQuery(venteRecordParamDTO);
  }

  private String buildQuery(String sql, VenteRecordParamDTO venteRecordParamDTO) {

    String diff = "";

    if (venteRecordParamDTO.isDiffereOnly()) {
      diff = VenteStatQueryBuilder.DIFFERE;
    }

    return String.format(
        sql,
        buildChiffreAffaire(venteRecordParamDTO.getCategorieChiffreAffaire()),
        diff,
        buildType(venteRecordParamDTO.getTypeVente()));
  }

  private String buildChiffreAffaire(CategorieChiffreAffaire categorieChiffreAffaire) {
    return switch (categorieChiffreAffaire) {
      case CA -> VenteStatQueryBuilder.CA;
      case CALLEBASE -> VenteStatQueryBuilder.CALLEBASE;
      case CA_DEPOT -> VenteStatQueryBuilder.CA_DEPOT;
      case TO_IGNORE -> VenteStatQueryBuilder.TO_IGNORE;
    };
  }

  private String buildType(TypeVente typeVente) {
    if (Objects.nonNull(typeVente)) {
      return String.format(VenteStatQueryBuilder.TYPE_VENTE, typeVente);
    }
    return "";
  }

  private List<Tuple> getGroupingByType(
      VenteRecordParamDTO venteRecordParamDTO, Pair<LocalDate, LocalDate> periode) {
    try {
      return this.em
          .createNativeQuery(
              buildQuery(VenteStatQueryBuilder.PERIOQIQUE_CA_QUERY_BY_TYPE, venteRecordParamDTO),
              Tuple.class)
          .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
          .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
          .setParameter(
              3,
              List.of(
                  venteRecordParamDTO.isCanceled()
                      ? SalesStatut.CANCELED.name()
                      : SalesStatut.CLOSED.name()))
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return null;
  }

  private List<VenteByTypeRecord> buildGroupingByType(VenteRecordParamDTO venteRecordParamDTO) {
    Pair<LocalDate, LocalDate> periode = getPeriode(venteRecordParamDTO);

    List<Tuple> list = getGroupingByType(venteRecordParamDTO, periode);
    if (!CollectionUtils.isEmpty(list)) {
      return list.stream().map(VenteStatQueryBuilder::buildVenteByTypeRecord).toList();
    }
    return Collections.emptyList();
  }

  private List<Tuple> getCaGroupingByPeriode(
      VenteRecordParamDTO venteRecordParamDTO, Pair<LocalDate, LocalDate> periode) {
    try {
      return this.em
          .createNativeQuery(
              buildPeriodeQuery(
                  VenteStatQueryBuilder.PERIOQIQUE_CA_QUERY_GROUPING_BY_PERIODE,
                  venteRecordParamDTO),
              Tuple.class)
          .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
          .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
          .setParameter(
              3,
              List.of(
                  venteRecordParamDTO.isCanceled()
                      ? SalesStatut.CANCELED.name()
                      : SalesStatut.CLOSED.name()))
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return null;
  }

  private String buildPeriodeQuery(String sql, VenteRecordParamDTO venteRecordParamDTO) {

    String diff = "";

    if (venteRecordParamDTO.isDiffereOnly()) {
      diff = VenteStatQueryBuilder.DIFFERE;
    }
    return String.format(
        sql,
        this.buildGroupBy(venteRecordParamDTO.getVenteStatGroupBy()),
        buildChiffreAffaire(venteRecordParamDTO.getCategorieChiffreAffaire()),
        diff,
        buildType(venteRecordParamDTO.getTypeVente()));
  }

  private List<VentePeriodeRecord> buildGroupingByPeriode(VenteRecordParamDTO venteRecordParamDTO) {
    Pair<LocalDate, LocalDate> periode = getPeriode(venteRecordParamDTO);

    List<Tuple> list = getCaGroupingByPeriode(venteRecordParamDTO, periode);
    if (!CollectionUtils.isEmpty(list)) {
      return list.stream()
          .map(
              e ->
                  VenteStatQueryBuilder.buildVentePeriodeRecord(
                      e, venteRecordParamDTO.getVenteStatGroupBy()))
          .toList();
    }
    return Collections.emptyList();
  }

  private List<Tuple> getModePaimentQuery(VenteRecordParamDTO venteRecordParamDTO) {
    try {
      Pair<LocalDate, LocalDate> periode = getPeriode(venteRecordParamDTO);
      return this.em
          .createNativeQuery(
              buildQuery(VenteStatQueryBuilder.MODE_PAIMENT, venteRecordParamDTO), Tuple.class)
          .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
          .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
          .setParameter(3, List.of(SalesStatut.CANCELED.name(), SalesStatut.CLOSED.name()))
          .getResultList();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return null;
  }

  private List<VenteModePaimentRecord> buildModePaimentQuery(
      VenteRecordParamDTO venteRecordParamDTO) {
    List<Tuple> list = getModePaimentQuery(venteRecordParamDTO);
    if (!CollectionUtils.isEmpty(list)) {
      return list.stream().map(VenteStatQueryBuilder::buildModePaiment).toList();
    }
    return Collections.emptyList();
  }
}
