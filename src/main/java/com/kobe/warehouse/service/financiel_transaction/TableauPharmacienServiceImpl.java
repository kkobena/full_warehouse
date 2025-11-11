package com.kobe.warehouse.service.financiel_transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.GroupeFournisseurRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.excel.ExcelExportService;
import com.kobe.warehouse.service.excel.GenericExcelDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.PaymentDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.stock.CommandeDataService;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TableauPharmacienServiceImpl implements TableauPharmacienService {

    private static final Logger LOG = LoggerFactory.getLogger(TableauPharmacienServiceImpl.class);
    private static final int GROUP_OTHER_ID = -1;

    private final GroupeFournisseurRepository groupeFournisseurRepository;
    private final Set<Integer> groupeFournisseurs = new HashSet<>();
    private final TableauPharmacienReportReportService reportService;
    private final ExcelExportService excelExportService;
    private final AppConfigurationService appConfigurationService;
    private final JsonMapper objectMapper;
    private final SalesRepository salesRepository;
    private final CommandeDataService commandeDataService;


    public TableauPharmacienServiceImpl(
        GroupeFournisseurRepository groupeFournisseurRepository,
        TableauPharmacienReportReportService reportService,
        ExcelExportService excelExportService, AppConfigurationService appConfigurationService, JsonMapper objectMapper, SalesRepository salesRepository, CommandeDataService commandeDataService
    ) {

        this.groupeFournisseurRepository = groupeFournisseurRepository;
        this.reportService = reportService;
        this.excelExportService = excelExportService;
        this.appConfigurationService = appConfigurationService;
        this.objectMapper = objectMapper;
        this.salesRepository = salesRepository;
        this.commandeDataService = commandeDataService;
    }

    private static AchatDTO getAchatDTO(List<AchatDTO> value, FournisseurAchat achatFournisseur) {
        AchatDTO achatDTO = achatFournisseur.getAchat();
        for (AchatDTO dto : value) {
            achatDTO.setMontantNet(achatDTO.getMontantNet() + dto.getMontantNet());
            achatDTO.setMontantTtc(achatDTO.getMontantTtc() + dto.getMontantTtc());
            achatDTO.setMontantHt(achatDTO.getMontantHt() + dto.getMontantHt());
            achatDTO.setMontantTaxe(achatDTO.getMontantTaxe() + dto.getMontantTaxe());
            achatDTO.setMontantRemise(achatDTO.getMontantRemise() + dto.getMontantRemise());
        }
        return achatDTO;
    }

    public Set<Integer> getGroupeFournisseurs() {
        groupeFournisseurs.addAll(fetchGroupGrossisteToDisplay().stream().map(GroupeFournisseurDTO::getId).collect(Collectors.toSet()));//TODO a revoir
        return groupeFournisseurs;
    }

    @Override
    public TableauPharmacienWrapper getTableauPharmacien(MvtParam mvtParam) {
        mvtParam.setExcludeFreeUnit(appConfigurationService.excludeFreeUnit());
        return computeData(mvtParam);
    }

    @Override
    public Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException {
        return this.reportService.exportToPdf(
            this.getTableauPharmacien(mvtParam),
            this.fetchGroupGrossisteToDisplay(),
            new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate()),
            mvtParam.getGroupeBy()
        );
    }

    @Override
    public Resource exportToExcel(MvtParam mvtParam) throws IOException {
        GenericExcelDTO genericExcel = new GenericExcelDTO();
        TableauPharmacienWrapper tableauPharmacienWrapper = this.getTableauPharmacien(mvtParam);
        List<GroupeFournisseurDTO> groupeFournisseurs = this.fetchGroupGrossisteToDisplay();
        List<String> columns = new ArrayList<>(List.of("Date", "Comptant", "Crédit", "Remise", "Montant Net", "Nbre de Clients"));
        for (GroupeFournisseurDTO groupeFournisseur : groupeFournisseurs) {
            columns.add(groupeFournisseur.getLibelle());
        }
        columns.addAll(List.of("Avoirs", "Achats Nets", "Ratios V/A", "Ratios A/V"));

        genericExcel.addColumn(columns.toArray(new String[0]));
        tableauPharmacienWrapper
            .getTableauPharmaciens()
            .forEach(t -> {
                List<Object> row = new ArrayList<>();
                row.add(t.getMvtDate());
                row.add(t.getMontantComptant());
                row.add(t.getMontantCredit());
                row.add(t.getMontantRemise());
                row.add(t.getMontantNet());
                row.add(t.getNombreVente());
                for (GroupeFournisseurDTO groupeFournisseur : groupeFournisseurs) {
                    row.add(
                        t
                            .getGroupAchats()
                            .stream()
                            .filter(f -> f.getId() == groupeFournisseur.getId())
                            .mapToLong(f -> f.getAchat().getMontantNet())
                            .sum()
                    );
                }
                row.add(t.getMontantAvoirFournisseur());
                row.add(t.getMontantBonAchat());
                row.add(t.getRatioVenteAchat());
                row.add(t.getRatioAchatVente());
                genericExcel.addRow(row.toArray());
            });

        return this.excelExportService.generate(genericExcel, "Tableau pharmacien", "tableau_pharmacien");
    }

    private void buildAchatsFromProjection(List<AchatDTO> projections, TableauPharmacienWrapper tableauPharmacienWrapper) {
        projections
            .forEach(achatDTO -> {
                updateTableauPharmacienWrapper(tableauPharmacienWrapper, achatDTO);

            });
    }

    private void updateTableauPharmacienWrapper(TableauPharmacienWrapper tableauPharmacienWrapper, AchatDTO achatDTO) {
        tableauPharmacienWrapper.setMontantAchatTtc(tableauPharmacienWrapper.getMontantAchatTtc() + achatDTO.getMontantTtc());
        tableauPharmacienWrapper.setMontantAchatRemise(tableauPharmacienWrapper.getMontantAchatRemise() + achatDTO.getMontantRemise());
        tableauPharmacienWrapper.setMontantAchatNet(tableauPharmacienWrapper.getMontantAchatNet() + achatDTO.getMontantNet());
        tableauPharmacienWrapper.setMontantAchatTaxe(tableauPharmacienWrapper.getMontantAchatTaxe() + achatDTO.getMontantTaxe());
        tableauPharmacienWrapper.setMontantAchatHt(tableauPharmacienWrapper.getMontantAchatHt() + achatDTO.getMontantHt());
    }

    private void buildTableauPharmacienFromProjection(
        List<TableauPharmacienDTO> projections,
        TableauPharmacienWrapper tableauPharmacienWrapper
    ) {
        projections
            .forEach(tableauPharmacien -> {
                List<PaymentDTO> payments = tableauPharmacien.getPayments();
                for (PaymentDTO paymentDTO : payments) {
                    tableauPharmacien.setMontantReel(tableauPharmacien.getMontantReel() + paymentDTO.realAmount());
                    tableauPharmacien.setMontantComptant(tableauPharmacien.getMontantComptant() + paymentDTO.paidAmount());
                }

                tableauPharmacien.setMontantNet(tableauPharmacien.getMontantTtc() + tableauPharmacien.getMontantRemise() - tableauPharmacien.getMontantRemiseUg());
                tableauPharmacien.setMontantComptant(tableauPharmacien.getMontantComptant() - tableauPharmacien.getMontantTtcUg());
                updateTableauPharmacienWrapper(tableauPharmacienWrapper, tableauPharmacien);
            });
    }

    private void updateTableauPharmacienWrapper(
        TableauPharmacienWrapper tableauPharmacienWrapper,
        TableauPharmacienDTO tableauPharmacienDTO
    ) {
        tableauPharmacienWrapper.setMontantVenteCredit(
            tableauPharmacienWrapper.getMontantVenteCredit() + tableauPharmacienDTO.getMontantCredit()
        );
        tableauPharmacienWrapper.setMontantVenteComptant(
            tableauPharmacienWrapper.getMontantVenteComptant() + tableauPharmacienDTO.getMontantComptant()
        );
        tableauPharmacienWrapper.setMontantVenteHt(tableauPharmacienWrapper.getMontantVenteHt() + tableauPharmacienDTO.getMontantHt());
        tableauPharmacienWrapper.setMontantVenteTtc(tableauPharmacienWrapper.getMontantVenteTtc() + tableauPharmacienDTO.getMontantTtc());
        tableauPharmacienWrapper.setMontantVenteTaxe(
            tableauPharmacienWrapper.getMontantVenteTaxe() + tableauPharmacienDTO.getMontantTaxe()
        );
        tableauPharmacienWrapper.setMontantVenteRemise(
            tableauPharmacienWrapper.getMontantVenteRemise() + tableauPharmacienDTO.getMontantRemise()
        );
        tableauPharmacienWrapper.setMontantVenteNet(tableauPharmacienWrapper.getMontantVenteNet() + tableauPharmacienDTO.getMontantNet());
        tableauPharmacienWrapper.setNumberCount(tableauPharmacienWrapper.getNumberCount() + tableauPharmacienDTO.getNombreVente());
    }

    private List<TableauPharmacienDTO> addAchatsToTableauPharmacien(List<TableauPharmacienDTO> tableauPharmaciens, List<AchatDTO> achats) {
        if (achats.isEmpty()) {
            return tableauPharmaciens;
        }
        achats.sort(Comparator.comparing(AchatDTO::getOrdreAffichage));
        Map<LocalDate, List<AchatDTO>> map = achats.stream().collect(Collectors.groupingBy(AchatDTO::getMvtDate));

        if (tableauPharmaciens.isEmpty()) {
            updateTableauPharmaciens(map, tableauPharmaciens);
            return tableauPharmaciens;
        }

        for (TableauPharmacienDTO tableauPharmacien : tableauPharmaciens) {
            if (!map.isEmpty()) {
                List<AchatDTO> achatDTOS = map.remove(tableauPharmacien.getMvtDate());
                if (Objects.nonNull(achatDTOS)) {
                    tableauPharmacien.setGroupAchats(computeGroupFournisseurAchatPerDay(achatDTOS));
                    tableauPharmacien.setMontantBonAchat(achatDTOS.stream().mapToLong(AchatDTO::getMontantNet).sum());
                }
            }

            computeRatioVenteAchat(tableauPharmacien);
            computeRatioAchatVente(tableauPharmacien);
            tableauPharmacien.setAchatFournisseurs(computeMapGroupFournisseurAchat(tableauPharmacien.getGroupAchats()));
        }
        if (!map.isEmpty()) {
            updateTableauPharmaciens(map, tableauPharmaciens);
        }

        return tableauPharmaciens;
    }

    Map<Long, Long> computeMapGroupFournisseurAchat(List<FournisseurAchat> groupAchats) {
        if (CollectionUtils.isEmpty(groupAchats)) {
            return Collections.emptyMap();
        }

        return groupAchats
            .stream()
            .collect(Collectors.groupingBy(FournisseurAchat::getId, Collectors.summingLong(f -> f.getAchat().getMontantNet())));
    }

    private List<FournisseurAchat> computeGroupFournisseurAchatPerDay(List<AchatDTO> achatsParJour) {
        Set<FournisseurAchat> fournisseurAchats = new HashSet<>();
        achatsParJour
            .stream()
            .collect(Collectors.groupingBy(AchatDTO::getGroupeGrossisteId))
            .forEach((key, value) -> {
                Integer topKey = null;
                for (Integer groupeFournisseur : getGroupeFournisseurs()) {
                    if (Objects.equals(groupeFournisseur, key)) {
                        topKey = key;
                        break;
                    }
                }
                if (Objects.isNull(topKey)) {
                    topKey = GROUP_OTHER_ID;
                }
                FournisseurAchat achatFournisseur;

                if (fournisseurAchats.isEmpty()) {
                    achatFournisseur = newGroupFournisseurAchat(topKey, value.getFirst().getGroupeGrossiste());
                } else {
                    FournisseurAchat f = null;
                    for (FournisseurAchat fournisseurAchat : fournisseurAchats) {
                        if (fournisseurAchat.getId() == topKey) {
                            f = fournisseurAchat;
                            break;
                        }
                    }
                    if (f == null) {
                        achatFournisseur = newGroupFournisseurAchat(topKey, value.getFirst().getGroupeGrossiste());
                    } else {
                        achatFournisseur = f;
                    }
                }
                achatFournisseur.setAchat(getAchatDTO(value, achatFournisseur));
                fournisseurAchats.add(achatFournisseur);
            });
        return fournisseurAchats.stream().toList();
    }

    private FournisseurAchat newGroupFournisseurAchat(Integer key, String libelle) {
        FournisseurAchat fournisseurAchat = new FournisseurAchat();
        fournisseurAchat.setId(key);
        fournisseurAchat.setLibelle(libelle);
        fournisseurAchat.setAchat(new AchatDTO());
        return fournisseurAchat;
    }

    private void updateTableauPharmaciens(Map<LocalDate, List<AchatDTO>> map, List<TableauPharmacienDTO> tableauPharmaciens) {


        map.forEach((k, v) -> {
            TableauPharmacienDTO tableauPharmacienDTO = new TableauPharmacienDTO();
            tableauPharmacienDTO.setMvtDate(k);
            tableauPharmacienDTO.setGroupAchats(computeGroupFournisseurAchatPerDay(v));
            if (tableauPharmaciens.stream().noneMatch(t -> t.getMvtDate().equals(k))) {
                tableauPharmacienDTO.setAchatFournisseurs(computeMapGroupFournisseurAchat(tableauPharmacienDTO.getGroupAchats()));
            }
            tableauPharmacienDTO.setMontantBonAchat(
                tableauPharmacienDTO.getGroupAchats().stream().mapToLong(f -> f.getAchat().getMontantNet()).sum()
            );
            tableauPharmaciens.add(tableauPharmacienDTO);
        });
    }

    private TableauPharmacienWrapper computeData(MvtParam mvtParam) {
        TableauPharmacienWrapper tableauPharmacienWrapper = new TableauPharmacienWrapper();
        List<TableauPharmacienDTO> tableauPharmaciens = fetchSalesData(mvtParam);
        buildTableauPharmacienFromProjection(
            tableauPharmaciens,
            tableauPharmacienWrapper
        );
        List<AchatDTO> achats = commandeDataService.fetchReportTableauPharmacienData(mvtParam);
        buildAchatsFromProjection(achats, tableauPharmacienWrapper);
        List<TableauPharmacienDTO> result = addAchatsToTableauPharmacien(tableauPharmaciens, new ArrayList<>(achats));

        tableauPharmacienWrapper.setTableauPharmaciens(result);
        computeAchats(tableauPharmacienWrapper);
        computeRatioVenteAchat(tableauPharmacienWrapper);
        computeRatioAchatVente(tableauPharmacienWrapper);
        tableauPharmacienWrapper.setAchatFournisseurs(
            computeMapGroupFournisseurAchat(
                tableauPharmacienWrapper.getTableauPharmaciens().stream().flatMap(t -> t.getGroupAchats().stream()).toList()
            )
        );

        return tableauPharmacienWrapper;
    }

    private void computeAchats(TableauPharmacienWrapper tableauPharmacienWrapper) {
        List<FournisseurAchat> groupAchats = new ArrayList<>();
        AtomicLong montantAchat = new AtomicLong(0L);
        if (tableauPharmacienWrapper.getTableauPharmaciens() != null) {
            tableauPharmacienWrapper
                .getTableauPharmaciens()
                .stream()
                .flatMap(t -> t.getGroupAchats().stream())
                .collect(Collectors.groupingBy(FournisseurAchat::getId))
                .forEach((k, v) -> {
                    FournisseurAchat fournisseurAchat = new FournisseurAchat();
                    fournisseurAchat.setAchat(new AchatDTO());
                    fournisseurAchat.setId(k);
                    fournisseurAchat.setLibelle(v.getFirst().getLibelle());
                    fournisseurAchat.setAchat(getAchatDTO(v.stream().map(FournisseurAchat::getAchat).toList(), fournisseurAchat));
                    montantAchat.addAndGet(fournisseurAchat.getAchat().getMontantNet());
                    groupAchats.add(fournisseurAchat);
                });
            tableauPharmacienWrapper.setGroupAchats(groupAchats);
            tableauPharmacienWrapper.setMontantAchatNet(montantAchat.get());
        }
    }

    private void computeRatioVenteAchat(TableauPharmacienWrapper tableauPharmacienWrapper) {
        if (tableauPharmacienWrapper.getMontantAchatNet() == 0) {
            return;
        }
        try {
            tableauPharmacienWrapper.setRatioVenteAchat(
                BigDecimal.valueOf(tableauPharmacienWrapper.getMontantVenteNet())
                    .divide(
                        BigDecimal.valueOf(
                            tableauPharmacienWrapper.getMontantAchatNet() - tableauPharmacienWrapper.getMontantAvoirFournisseur()
                        ),
                        2,
                        RoundingMode.FLOOR
                    )
                    .floatValue()
            );
        } catch (Exception e) {
            LOG.info( e.getLocalizedMessage());
        }
    }

    private void computeRatioVenteAchat(TableauPharmacienDTO tableauPharmacien) {
        if (tableauPharmacien.getMontantBonAchat() == 0) {
            return;
        }
        try {
            tableauPharmacien.setRatioVenteAchat(
                BigDecimal.valueOf(tableauPharmacien.getMontantNet())
                    .divide(
                        BigDecimal.valueOf(tableauPharmacien.getMontantBonAchat() - tableauPharmacien.getMontantAvoirFournisseur()),
                        2,
                        RoundingMode.FLOOR
                    )
                    .floatValue()
            );
        } catch (Exception e) {
            LOG.info( e.getLocalizedMessage());
        }
    }

    private void computeRatioAchatVente(TableauPharmacienDTO tableauPharmacien) {
        if (tableauPharmacien.getMontantNet() == 0) {
            return;
        }
        try {
            tableauPharmacien.setRatioAchatVente(
                BigDecimal.valueOf(tableauPharmacien.getMontantBonAchat() - tableauPharmacien.getMontantAvoirFournisseur())
                    .divide(BigDecimal.valueOf(tableauPharmacien.getMontantNet()), 2, RoundingMode.FLOOR)
                    .floatValue()
            );
        } catch (Exception e) {
            LOG.info( e.getLocalizedMessage());
        }
    }

    private void computeRatioAchatVente(TableauPharmacienWrapper tableauPharmacienWrapper) {
        if (tableauPharmacienWrapper.getMontantVenteNet() == 0) {
            return;
        }
        try {
            tableauPharmacienWrapper.setRatioAchatVente(
                BigDecimal.valueOf(tableauPharmacienWrapper.getMontantAchatNet() - tableauPharmacienWrapper.getMontantAvoirFournisseur())
                    .divide(BigDecimal.valueOf(tableauPharmacienWrapper.getMontantVenteNet()), 2, RoundingMode.FLOOR)
                    .floatValue()
            );
        } catch (Exception e) {
            LOG.info( e.getLocalizedMessage());
        }
    }

    @Override
    public List<GroupeFournisseurDTO> fetchGroupGrossisteToDisplay() {
        var all = groupeFournisseurRepository.findAllByOrderByOdreAsc();
        if (all.size() > 4) {
            List<GroupeFournisseurDTO> topN = new ArrayList<>(all.stream().limit(4).map(GroupeFournisseurDTO::new).toList());
            topN.add(new GroupeFournisseurDTO().setId(GROUP_OTHER_ID).setLibelle("Autres").setOdre(1000_1000));
            topN.sort(Comparator.comparing(GroupeFournisseurDTO::getOdre));
            return topN;
        } else {
            return all.stream().sorted(Comparator.comparing(GroupeFournisseur::getOdre)).map(GroupeFournisseurDTO::new).toList();
        }
    }


    private List<TableauPharmacienDTO> fetchSalesData(MvtParam mvtParam) {
        try {
            String jsonResult;
            if ("month".equals(mvtParam.getGroupeBy())) {
                jsonResult = salesRepository.fetchTableauPharmacienReportMensuel(mvtParam.getFromDate(), mvtParam.getToDate(), mvtParam.getStatuts().stream().map(SalesStatut::name).toArray(String[]::new), mvtParam.getCategorieChiffreAffaires().stream().map(CategorieChiffreAffaire::name).toArray(String[]::new), mvtParam.isExcludeFreeUnit(), BooleanUtils.toBoolean(mvtParam.getToIgnore()));
            } else {
                jsonResult = salesRepository.fetchTableauPharmacienReport(mvtParam.getFromDate(), mvtParam.getToDate(), mvtParam.getStatuts().stream().map(SalesStatut::name).toArray(String[]::new), mvtParam.getCategorieChiffreAffaires().stream().map(CategorieChiffreAffaire::name).toArray(String[]::new), mvtParam.isExcludeFreeUnit(), BooleanUtils.toBoolean(mvtParam.getToIgnore()));
            }
            return objectMapper.readValue(jsonResult, new TypeReference<>() {
            });

        } catch (Exception e) {
            LOG.info( e.getLocalizedMessage());
            return  new ArrayList<>();
        }
    }

}
