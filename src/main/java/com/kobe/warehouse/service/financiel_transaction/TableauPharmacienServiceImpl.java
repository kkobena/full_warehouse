package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.domain.GroupeFournisseur_;
import com.kobe.warehouse.domain.PaymentTransaction_;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.repository.GroupeFournisseurRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.excel.ExcelExportService;
import com.kobe.warehouse.service.excel.GenericExcelDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
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

import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class TableauPharmacienServiceImpl implements TableauPharmacienService {

    private static final Logger LOG = LoggerFactory.getLogger(TableauPharmacienServiceImpl.class);
    private static final long GROUP_OTHER_ID = -1L;
    private final EntityManager entityManager;
    private final GroupeFournisseurRepository groupeFournisseurRepository;
    private final Set<Long> groupeFournisseurs = new HashSet<>();
    private final TableauPharmacienReportReportService reportService;
    private final ExcelExportService excelExportService;
    private final TableauPharmacienSpecification tableauPharmacienSpecification;
    private final AppConfigurationService appConfigurationService;

    public TableauPharmacienServiceImpl(
        EntityManager entityManager,
        GroupeFournisseurRepository groupeFournisseurRepository,
        TableauPharmacienReportReportService reportService,
        ExcelExportService excelExportService, TableauPharmacienSpecification tableauPharmacienSpecification, AppConfigurationService appConfigurationService
    ) {
        this.entityManager = entityManager;
        this.groupeFournisseurRepository = groupeFournisseurRepository;
        this.reportService = reportService;
        this.excelExportService = excelExportService;
        this.tableauPharmacienSpecification = tableauPharmacienSpecification;
        this.appConfigurationService = appConfigurationService;
        groupeFournisseurs.addAll(fetchGroupGrossisteToDisplay().stream().map(GroupeFournisseurDTO::getId).collect(Collectors.toSet()));
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

    private void buildAchatsFromProjection( List<AchatDTO> projections , TableauPharmacienWrapper tableauPharmacienWrapper) {
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
            .forEach(tableauPharmacienDTO -> updateTableauPharmacienWrapper(tableauPharmacienWrapper, tableauPharmacienDTO));
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
                Long topKey = null;
                for (Long groupeFournisseur : groupeFournisseurs) {
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
                    achatFournisseur = newGroupFournisseurAchat(topKey, value.get(0).getGroupeGrossiste());
                } else {
                    FournisseurAchat f = null;
                    for (FournisseurAchat fournisseurAchat : fournisseurAchats) {
                        if (fournisseurAchat.getId() == topKey) {
                            f = fournisseurAchat;
                            break;
                        }
                    }
                    if (f == null) {
                        achatFournisseur = newGroupFournisseurAchat(topKey, value.get(0).getGroupeGrossiste());
                    } else {
                        achatFournisseur = f;
                    }
                }
                achatFournisseur.setAchat(getAchatDTO(value, achatFournisseur));
                fournisseurAchats.add(achatFournisseur);
            });
        return fournisseurAchats.stream().toList();
    }

    private FournisseurAchat newGroupFournisseurAchat(Long key, String libelle) {
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
        List<AchatDTO> achats  = fetchAchatData(mvtParam);
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
                    fournisseurAchat.setLibelle(v.get(0).getLibelle());
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
            LOG.warn("Error {}", e.getMessage());
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
            LOG.warn("Error {}", e.getMessage());
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
            LOG.warn("Error {}", e.getMessage());
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
            LOG.warn("Error {}", e.getMessage());
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


        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TableauPharmacienDTO> cq = cb.createQuery(TableauPharmacienDTO.class);
        Root<Sales> root = cq.from(Sales.class);
        SetJoin<Sales, SalePayment> payments = root.joinSet(Sales_.PAYMENTS, JoinType.LEFT);
        SetJoin<Sales, SalesLine> salesLineSetJoin = root.joinSet(Sales_.SALES_LINES);
        Path<ThirdPartySales> thirdPartySalesPath = cb.treat(root, ThirdPartySales.class);
        Expression<Integer> quantityExpression = mvtParam.isExcludeFreeUnit() ? cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)) : salesLineSetJoin.get(SalesLine_.quantityRequested);
        Expression<Long> montantTtcExpression = mvtParam.isExcludeFreeUnit() ? cb.sumAsLong(cb.prod(cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)), salesLineSetJoin.get(SalesLine_.regularUnitPrice))) : cb.sumAsLong(cb.prod(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.regularUnitPrice)));
        Expression<Long> montantTtcAcahtExpression = mvtParam.isExcludeFreeUnit() ? cb.sumAsLong(cb.prod(cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)), salesLineSetJoin.get(SalesLine_.costAmount))) : cb.sumAsLong(cb.prod(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.costAmount)));
        Expression<?> discountExpression = mvtParam.isExcludeFreeUnit() ? cb.sum(cb.prod(quantityExpression, salesLineSetJoin.get(SalesLine_.tauxRemise))) : cb.sumAsLong(root.get(Sales_.discountAmount));
        Expression<Integer> montantHtQtyExpression = mvtParam.isExcludeFreeUnit() ? cb.prod(cb.diff(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.quantityUg)), salesLineSetJoin.get(SalesLine_.regularUnitPrice)) : cb.prod(salesLineSetJoin.get(SalesLine_.quantityRequested), salesLineSetJoin.get(SalesLine_.regularUnitPrice));
        cq.where(tableauPharmacienSpecification.buildSalesSpecification(mvtParam).toPredicate(root, cq, cb));

        Expression<LocalDate> mvtDate = cb.function("DATE", LocalDate.class, root.get(Sales_.updatedAt));
        if ("month".equals(mvtParam.getGroupeBy())) {
            mvtDate = cb.function("DATE_FORMAT", LocalDate.class, root.get(Sales_.updatedAt), cb.literal("%Y-%m"));
        }
        cq.select(createTableauPharmacienDTOResult(cb, root, payments, salesLineSetJoin, thirdPartySalesPath, montantTtcExpression, montantTtcAcahtExpression, discountExpression, montantHtQtyExpression, mvtDate));
        cq.groupBy(mvtDate);
        cq.orderBy(cb.asc(mvtDate));

        return entityManager.createQuery(cq).getResultList();
    }


    private CompoundSelection<TableauPharmacienDTO> createTableauPharmacienDTOResult(CriteriaBuilder cb, Root<Sales> root, SetJoin<Sales, SalePayment> payments, SetJoin<Sales, SalesLine> salesLineSetJoin, Path<ThirdPartySales> thirdPartySalesPath, Expression<Long> montantTtcExpression, Expression<Long> montantTtcAcahtExpression, Expression<?> discountExpression, Expression<Integer> montantHtQtyExpression, Expression<LocalDate> mvtDate) {
        return cb.construct(TableauPharmacienDTO.class,
            mvtDate,
            cb.count(root.get(Sales_.id)),
            discountExpression,
            montantTtcExpression,
            cb.sumAsLong(payments.get(PaymentTransaction_.paidAmount)),
            cb.sumAsLong(payments.get(PaymentTransaction_.reelAmount)),
            cb.ceiling(
                cb.sum(
                    cb.quot(
                        montantHtQtyExpression,
                        cb.sum(1, cb.quot(salesLineSetJoin.get(SalesLine_.taxValue), 100.0d))
                    )
                )
            ),
            montantTtcAcahtExpression,
            cb.sum(root.get(Sales_.restToPay)),
            cb.sumAsLong(root.get(Sales_.amountToBeTakenIntoAccount)),
            cb.sumAsLong(thirdPartySalesPath.get(ThirdPartySales_.partTiersPayant)),
            cb.sumAsLong(thirdPartySalesPath.get(ThirdPartySales_.partAssure))
        );
    }

    private List<AchatDTO> fetchAchatData(MvtParam mvtParam) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AchatDTO> cq = cb.createQuery(AchatDTO.class);
        Root<Commande> root = cq.from(Commande.class);
        Join<Commande, Fournisseur> fournisseur = root.join(Commande_.fournisseur);
        Join<Fournisseur, GroupeFournisseur> groupeFournisseur = fournisseur.join(Fournisseur_.groupeFournisseur);
        cq.where(tableauPharmacienSpecification.buildAchatSpecification(mvtParam).toPredicate(root, cq, cb));

        Expression<LocalDate> mvtDate = cb.function("DATE", LocalDate.class, root.get(Commande_.updatedAt));
        if ("month".equals(mvtParam.getGroupeBy())) {
            mvtDate = cb.function("DATE_FORMAT", LocalDate.class, root.get(Commande_.updatedAt), cb.literal("%Y-%m"));
        }

        cq.select(cb.construct(AchatDTO.class,
            mvtDate,
            cb.sumAsLong(cb.diff(root.get(Commande_.grossAmount), root.get(Commande_.taxAmount))),
            cb.sumAsLong(root.get(Commande_.taxAmount)),
            cb.sumAsLong(root.get(Commande_.grossAmount)),
            cb.sumAsLong(root.get(Commande_.discountAmount)),
            groupeFournisseur.get(GroupeFournisseur_.id),
            groupeFournisseur.get(GroupeFournisseur_.libelle),
            groupeFournisseur.get(GroupeFournisseur_.odre)
        ));

        cq.groupBy(mvtDate, groupeFournisseur.get(GroupeFournisseur_.id));
        cq.orderBy(cb.asc(mvtDate));

        return entityManager.createQuery(cq).getResultList();
    }
}
