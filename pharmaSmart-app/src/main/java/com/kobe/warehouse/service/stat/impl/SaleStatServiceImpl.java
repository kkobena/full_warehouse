package com.kobe.warehouse.service.stat.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.stat.SaleStatService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Transactional(readOnly = true)
public class SaleStatServiceImpl implements SaleStatService {

    private final Logger LOG = LoggerFactory.getLogger(SaleStatServiceImpl.class);
    private final SalesRepository salesRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final ObjectMapper objectMapper;

    public SaleStatServiceImpl(SalesRepository salesRepository, SalePaymentRepository salePaymentRepository, ObjectMapper objectMapper) {
        this.salesRepository = salesRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public VenteRecordWrapper getPeridiqueCa(VenteRecordParamDTO venteRecordParamDTO) {
        Pair<LocalDate, LocalDate> localDateLocalDatePair = getPeriode(venteRecordParamDTO);
        venteRecordParamDTO.setCanceled(false);
        VenteRecord venteRecord = fetchVenteRecord(
            venteRecordParamDTO,
            localDateLocalDatePair
        )/*getCaByPeriode(venteRecordParamDTO, localDateLocalDatePair)*/;
        venteRecordParamDTO.setCanceled(true);
        VenteRecord venteRecordAnnulation = fetchVenteRecord(venteRecordParamDTO, localDateLocalDatePair); // getCaByPeriode(venteRecordParamDTO, localDateLocalDatePair);

        return new VenteRecordWrapper(venteRecord, venteRecordAnnulation);
    }

    private Pair<LocalDate, LocalDate> getPeriode(VenteRecordParamDTO venteRecordParamDTO) {
        return this.buildPeriode(venteRecordParamDTO);
    }

    @Override
    public List<VentePeriodeRecord> getCaGroupingByPeriode(VenteRecordParamDTO venteRecordParamDTO) {
        Pair<LocalDate, LocalDate> periode = getPeriode(venteRecordParamDTO);
        return this.salesRepository.fetchVentePeriodeRecords(
            buildSpecification(venteRecordParamDTO, periode),
            venteRecordParamDTO.getVenteStatGroupBy()
        );
    }

    @Override
    public List<VenteByTypeRecord> getCaGroupingByType(VenteRecordParamDTO venteRecordParamDTO) {
        Pair<LocalDate, LocalDate> periode = getPeriode(venteRecordParamDTO);
        return getCaGroupingByType(venteRecordParamDTO, periode)
            .stream()
            .map(venteRecord -> new VenteByTypeRecord(venteRecord.type(), venteRecord))
            .toList();
        //  return this.salesRepository.fetchVenteByTypeRecords(buildSpecification(venteRecordParamDTO, periode));
    }

    @Override
    public List<VenteModePaimentRecord> getCaGroupingByPaimentMode(VenteRecordParamDTO venteRecordParamDTO) {
        Pair<LocalDate, LocalDate> periode = getPeriode(venteRecordParamDTO);
        return salePaymentRepository.fetchVenteModePaimentRecords(buildSalePayementSpecification(venteRecordParamDTO, periode));
    }

    private VenteRecord fetchVenteRecord(VenteRecordParamDTO venteRecordParam, Pair<LocalDate, LocalDate> periode) {
        try {
            String jsonResult = salesRepository.fetchSalesSummary(
                periode.getLeft(),
                periode.getRight(),
                new String[]{venteRecordParam.isCanceled() ? SalesStatut.CANCELED.name() : SalesStatut.CLOSED.name()},
                new String[]{venteRecordParam.getCategorieChiffreAffaire().name()}, venteRecordParam.isCanceled()
            );
            return objectMapper.readValue(jsonResult, new TypeReference<>() {
            });
        } catch (Exception e) {
            LOG.error(null, e);
            return null;
        }
    }

    private List<VenteRecord> getCaGroupingByType(VenteRecordParamDTO venteRecordParam, Pair<LocalDate, LocalDate> periode) {
        try {
            String jsonResult = salesRepository.fetchSalesSummaryByTypeVente(
                periode.getLeft(),
                periode.getRight(),
                new String[]{ SalesStatut.CLOSED.name()},
                categoriesVentilees(venteRecordParam)
            );
            return objectMapper.readValue(jsonResult, new TypeReference<>() {
            });
        } catch (Exception e) {
            LOG.info(e.getMessage());
            return List.of();
        }
    }

    /**
     * Catégories de chiffre d'affaires retenues pour une VENTILATION PAR TYPE.
     *
     * <p>Une ventilation par type de vente qui laisserait un type dehors ne ventilerait rien :
     * la vente dépôt porte la catégorie {@code CA_DEPOT} — c'est un transfert vers un
     * dépositaire, hors chiffre d'affaires déclaré — et n'apparaissait donc jamais dans la
     * répartition, alors que c'est précisément l'écran fait pour la montrer. On l'ajoute ici,
     * et seulement ici : le chiffre d'affaires global, lui, continue de se calculer sur la
     * seule catégorie demandée. Chaque type reste sur sa propre ligne, distinguable.
     */
    private String[] categoriesVentilees(VenteRecordParamDTO venteRecordParam) {
        CategorieChiffreAffaire demandee = venteRecordParam.getCategorieChiffreAffaire();
        if (demandee == CategorieChiffreAffaire.CA) {
            return new String[]{CategorieChiffreAffaire.CA.name(), CategorieChiffreAffaire.CA_DEPOT.name()};
        }
        return new String[]{demandee.name()};
    }

    private Specification<SalePayment> buildSalePayementSpecification(
        VenteRecordParamDTO venteRecordParam,
        Pair<LocalDate, LocalDate> periode
    ) {
        Specification<SalePayment> specification = this.salePaymentRepository.between(periode.getLeft(), periode.getRight());
        specification = specification.and(this.salePaymentRepository.paymentBetween(periode.getLeft(), periode.getRight()));
        specification = specification.and(this.salePaymentRepository.notImported());
        if (venteRecordParam.isDiffereOnly()) {
            specification = specification.and(this.salePaymentRepository.isDiffere());
        }
        if (venteRecordParam.isCanceled()) {
            specification = specification.and(this.salePaymentRepository.hasStatut(EnumSet.of(SalesStatut.CANCELED)));
        } else {
            specification = specification.and(this.salePaymentRepository.hasStatut(EnumSet.of(SalesStatut.CLOSED)));
        }
        if (nonNull(venteRecordParam.getCategorieChiffreAffaire())) {
            specification = specification.and(
                this.salePaymentRepository.hasCategorieCa(EnumSet.of(venteRecordParam.getCategorieChiffreAffaire()))
            );
        }
        if (nonNull(venteRecordParam.getTypeVente())) {
            specification = specification.and(this.salePaymentRepository.hasType(venteRecordParam.getTypeVente()));
        }
        return specification;
    }

    private Specification<Sales> buildSpecification(
        VenteRecordParamDTO venteRecordParam,
        Pair<LocalDate, LocalDate> periode
    ) {
        Specification<Sales> specification = this.salesRepository.between(periode.getLeft(), periode.getRight());
        specification = specification.and(this.salesRepository.notImported());
        if (venteRecordParam.isDiffereOnly()) {
            specification = specification.and(this.salesRepository.isDiffere());
        }
        if (venteRecordParam.isCanceled()) {
            specification = specification.and(this.salesRepository.hasStatut(EnumSet.of(SalesStatut.CANCELED)));
        } else {
            specification = specification.and(this.salesRepository.hasStatut(EnumSet.of(SalesStatut.CLOSED)));
        }
        if (nonNull(venteRecordParam.getCategorieChiffreAffaire())) {
            specification = specification.and(
                this.salesRepository.hasCategorieCa(EnumSet.of(venteRecordParam.getCategorieChiffreAffaire()))
            );
        }
        if (nonNull(venteRecordParam.getTypeVente())) {
            specification = specification.and(this.salesRepository.hasType(venteRecordParam.getTypeVente()));
        }
        return specification;
    }
}
