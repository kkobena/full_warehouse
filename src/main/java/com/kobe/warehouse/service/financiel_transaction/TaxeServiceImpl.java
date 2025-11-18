package com.kobe.warehouse.service.financiel_transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.dto.DoughnutChart;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaxeServiceImpl implements TaxeService, MvtCommonService {

    private static final Logger LOG = LoggerFactory.getLogger(TaxeServiceImpl.class);
    private final TvaReportReportService tvaReportService;
    private final SalesRepository salesRepository;
    private final JsonMapper objectMapper;

    public TaxeServiceImpl(TvaReportReportService tvaReportService, SalesRepository salesRepository, JsonMapper objectMapper) {
        this.tvaReportService = tvaReportService;

        this.salesRepository = salesRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public TaxeWrapperDTO fetchTaxe(MvtParam mvtParam, boolean toExport) {
        List<TaxeDTO> result = fetchTaxe(mvtParam);
        if (result.isEmpty()) {
            return null;
        }
        TaxeWrapperDTO taxeWrapperDTO = new TaxeWrapperDTO();
        buildFromProjection(result, "daily".equals(mvtParam.getGroupeBy()), taxeWrapperDTO);
        if (toExport) {
            return taxeWrapperDTO;
        }
        taxeWrapperDTO.setChart(buildDoughnutChart(taxeWrapperDTO));
        return taxeWrapperDTO;
    }

    @Override
    public Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException {
        return this.tvaReportService.exportToPdf(
                this.fetchTaxe(mvtParam, true),
                new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate()),
                StringUtils.hasText(mvtParam.getGroupeBy()) && "daily".equals(mvtParam.getGroupeBy())
            );
    }

    private List<TaxeDTO> fetchTaxe(MvtParam mvtParam) {
        try {
            mvtParam.setStatuts(
                Set.of( SalesStatut.CLOSED,SalesStatut.CANCELED)
            );
            String jsonResult;
            if ("daily".equals(mvtParam.getGroupeBy())) {
                jsonResult = salesRepository.fetchSalesTvaReportJournalier(
                    mvtParam.getFromDate(),
                    mvtParam.getToDate(),
                    mvtParam.getStatuts().stream().map(SalesStatut::name).toArray(String[]::new),
                    mvtParam.getCategorieChiffreAffaires().stream().map(CategorieChiffreAffaire::name).toArray(String[]::new),
                    mvtParam.isExcludeFreeUnit(),
                    BooleanUtils.toBoolean(mvtParam.getToIgnore())
                );
            } else {
                jsonResult = salesRepository.fetchSalesTvaReport(
                    mvtParam.getFromDate(),
                    mvtParam.getToDate(),
                    mvtParam.getStatuts().stream().map(SalesStatut::name).toArray(String[]::new),
                    mvtParam.getCategorieChiffreAffaires().stream().map(CategorieChiffreAffaire::name).toArray(String[]::new),
                    mvtParam.isExcludeFreeUnit(),
                    BooleanUtils.toBoolean(mvtParam.getToIgnore())
                );
            }
            return objectMapper.readValue(jsonResult, new TypeReference<>() {});
        } catch (Exception e) {
            LOG.error(null, e);
            return List.of();
        }
    }

    private void buildFromProjection(List<TaxeDTO> projections, boolean groupByDate, TaxeWrapperDTO taxeWrapperDTO) {
        taxeWrapperDTO.setGroupDate(groupByDate);
        projections.forEach(taxeDTO -> updateTaxeWrapper(taxeWrapperDTO, taxeDTO));
    }

    private void updateTaxeWrapper(TaxeWrapperDTO taxeWrapper, TaxeDTO taxe) {
        taxeWrapper.setMontantHt(taxeWrapper.getMontantHt() + taxe.getMontantHt());
        taxeWrapper.setMontantTaxe(taxeWrapper.getMontantTaxe() + taxe.getMontantTaxe());
        taxeWrapper.setMontantTtc(taxeWrapper.getMontantTtc() + taxe.getMontantTtc());
        taxeWrapper.setMontantNet(taxeWrapper.getMontantNet() + taxe.getMontantNet());
        taxeWrapper.setMontantRemise(taxeWrapper.getMontantRemise() + taxe.getMontantRemise());
        taxeWrapper.setMontantAchat(taxeWrapper.getMontantAchat() + taxe.getMontantAchat());
        taxeWrapper.setAmountToBeTakenIntoAccount(taxeWrapper.getAmountToBeTakenIntoAccount() + taxe.getAmountToBeTakenIntoAccount());
        taxeWrapper.getTaxes().add(taxe);
    }

    private DoughnutChart buildDoughnutChart(TaxeWrapperDTO taxeWrapperDTO) {
        List<TaxeDTO> taxes = taxeWrapperDTO.getTaxes();
        taxes.sort(Comparator.comparing(TaxeDTO::getCodeTva));
        List<String> labeles = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        taxes
            .stream()
            .collect(Collectors.groupingBy(TaxeDTO::getCodeTva, Collectors.summingLong(TaxeDTO::getMontantTtc)))
            .forEach((k, v) -> {
                labeles.add(k.toString());
                data.add(v);
            });
        return new DoughnutChart(labeles, data);
    }
}
