package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.stat.SaleStatService;
import com.kobe.warehouse.service.utils.VenteStatQueryBuilder;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    Pair<LocalDate, LocalDate> localDateLocalDatePair =
        buildPeriode(
            venteRecordParamDTO.getFromDate(),
            venteRecordParamDTO.getToDate(),
            venteRecordParamDTO.getDashboardPeriode());
    venteRecordParamDTO.setCanceled(false);
    VenteRecord venteRecord = getCaByPeriode(venteRecordParamDTO, localDateLocalDatePair);
    venteRecordParamDTO.setCanceled(true);
    VenteRecord venteRecordAnnulation = getCaByPeriode(venteRecordParamDTO, localDateLocalDatePair);

    return new VenteRecordWrapper(venteRecord, venteRecordAnnulation);
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
                  venteRecordParamDTO.isCanceled()
                      ? SalesStatut.CANCELED.name()
                      : SalesStatut.CLOSED.name())
              .getSingleResult();

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return null;
  }

  @Override
  public List<VentePeriodeRecord> getCaGroupingByPeriode(VenteRecordParamDTO venteRecordParamDTO) {
    return null;
  }

  private String buildQuery(String sql, VenteRecordParamDTO venteRecordParamDTO) {

    String diff = "";

    if (venteRecordParamDTO.isDiffereOnly()) {
      diff = VenteStatQueryBuilder.DIFFERE;
    }
    var ca =
        switch (venteRecordParamDTO.getCategorieChiffreAffaire()) {
          case CA -> VenteStatQueryBuilder.CA;
          case CALLEBASE -> VenteStatQueryBuilder.CALLEBASE;
          case CA_DEPOT -> VenteStatQueryBuilder.CA_DEPOT;
          case TO_IGNORE -> VenteStatQueryBuilder.TO_IGNORE;
        };
    return String.format(sql, ca, diff, buildType(venteRecordParamDTO.getTypeVente()));
  }

  private String buildType(TypeVente typeVente) {
    if (Objects.nonNull(typeVente)) {
      return String.format(VenteStatQueryBuilder.TYPE_VENTE, typeVente);
    }
    return "";
  }
}
