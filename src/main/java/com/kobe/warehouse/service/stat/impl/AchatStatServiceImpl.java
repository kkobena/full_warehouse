package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.stat.AchatStatService;
import com.kobe.warehouse.service.utils.AchatStatQueryBuilder;
import java.time.LocalDate;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AchatStatServiceImpl implements AchatStatService {
  private final Logger LOG = LoggerFactory.getLogger(AchatStatServiceImpl.class);
  private final EntityManager em;

  public AchatStatServiceImpl(EntityManager em) {
    this.em = em;
  }

  @Override
  public AchatRecord getAchatPeriode(AchatRecordParamDTO achatRecordParam) {
    Pair<LocalDate, LocalDate> periode =
        buildPeriode(achatRecordParam);

    try {

      return AchatStatQueryBuilder.build(
          (Tuple)
              this.em
                  .createNativeQuery(AchatStatQueryBuilder.ACHAT_QUERY, Tuple.class)
                  .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
                  .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
                  .setParameter(
                      3,
                      achatRecordParam.getReceiptStatuts().stream()
                          .map(ReceiptStatut::name)
                          .toList())
                  .getSingleResult());

    } catch (Exception e) {
      LOG.error(null, e);
    }
    return null;
  }
}
