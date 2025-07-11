package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.builder.AchatStatQueryBuilder;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.stat.AchatStatService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.LocalDate;
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
        Pair<LocalDate, LocalDate> periode = buildPeriode(achatRecordParam);

        try {
            return AchatStatQueryBuilder.build(
                (Tuple) this.em.createNativeQuery(AchatStatQueryBuilder.ACHAT_QUERY, Tuple.class)
                    .setParameter(1, java.sql.Date.valueOf(periode.getLeft()))
                    .setParameter(2, java.sql.Date.valueOf(periode.getRight()))
                    .setParameter(3, achatRecordParam.getReceiptStatuts().stream().map(OrderStatut::name).toList())
                    .getSingleResult()
            );
        } catch (Exception e) {
            LOG.error(null, e);
        }
        return null;
    }
}
