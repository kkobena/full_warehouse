package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.repository.GroupeFournisseurRepository;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class TableauPharmacienServiceImpl implements TableauPharmacienService {

    private static final Logger LOG = LoggerFactory.getLogger(TableauPharmacienServiceImpl.class);
    private static final long GROUP_OTHER_ID = -1L;
    private final EntityManager entityManager;
    private final GroupeFournisseurRepository groupeFournisseurRepository;
    private final Set<Long> groupeFournisseurs = new HashSet<>();
    private final TableauPharmacienReportService reportService;

    public TableauPharmacienServiceImpl(
        EntityManager entityManager,
        GroupeFournisseurRepository groupeFournisseurRepository,
        TableauPharmacienReportService reportService
    ) {
        this.entityManager = entityManager;
        this.groupeFournisseurRepository = groupeFournisseurRepository;
        this.reportService = reportService;
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

    private List<Tuple> executeQuery(MvtParam mvtParam) {
        return entityManager
            .createNativeQuery(buildQuery(mvtParam), Tuple.class)
            .setParameter(1, mvtParam.getFromDate())
            .setParameter(2, mvtParam.getToDate())
            .getResultList();
    }

    private List<Tuple> executeAchatQuery(MvtParam mvtParam) {
        return entityManager
            .createNativeQuery(buildAchatQuery(mvtParam.getGroupeBy()), Tuple.class)
            .setParameter(1, mvtParam.getFromDate())
            .setParameter(2, mvtParam.getToDate())
            .getResultList();
    }

    private List<AchatDTO> buildAchatsFromTuple(List<Tuple> tuples, TableauPharmacienWrapper tableauPharmacienWrapper) {
        return tuples
            .stream()
            .map(t -> {
                AchatDTO achatDTO = new AchatDTO();
                achatDTO
                    .setMontantNet(t.get("montantNet", BigDecimal.class).longValue())
                    .setMontantTtc(t.get("montantTtc", BigDecimal.class).longValue())
                    .setMontantHt(t.get("montantHt", BigDecimal.class).longValue())
                    .setMontantTaxe(t.get("montantTaxe", BigDecimal.class).longValue())
                    .setMontantRemise(t.get("montantRemise", BigDecimal.class).longValue())
                    .setGroupeGrossiste(t.get("groupeGrossiste", String.class))
                    .setGroupeGrossisteId(t.get("groupeGrossisteId", Long.class))
                    .setOrdreAffichage(t.get("ordreAffichage", Integer.class))
                    .setMvtDate(LocalDate.parse(t.get("mvtDate", String.class)));
                updateTableauPharmacienWrapper(tableauPharmacienWrapper, achatDTO);
                return achatDTO;
            })
            .sorted(Comparator.comparing(AchatDTO::getOrdreAffichage))
            .toList();
    }

    private void updateTableauPharmacienWrapper(TableauPharmacienWrapper tableauPharmacienWrapper, AchatDTO achatDTO) {
        tableauPharmacienWrapper.setMontantAchatTtc(tableauPharmacienWrapper.getMontantAchatTtc() + achatDTO.getMontantTtc());
        tableauPharmacienWrapper.setMontantAchatRemise(tableauPharmacienWrapper.getMontantAchatRemise() + achatDTO.getMontantRemise());
        tableauPharmacienWrapper.setMontantAchatNet(tableauPharmacienWrapper.getMontantAchatNet() + achatDTO.getMontantNet());
        tableauPharmacienWrapper.setMontantAchatTaxe(tableauPharmacienWrapper.getMontantAchatTaxe() + achatDTO.getMontantTaxe());
        tableauPharmacienWrapper.setMontantAchatHt(tableauPharmacienWrapper.getMontantAchatHt() + achatDTO.getMontantHt());
    }

    private List<TableauPharmacienDTO> buildTableauPharmacienFromTuple(
        List<Tuple> tuples,
        TableauPharmacienWrapper tableauPharmacienWrapper,
        boolean groupingByMonth
    ) {
        return tuples
            .stream()
            .map(t -> {
                TableauPharmacienDTO tableauPharmacienDTO = new TableauPharmacienDTO();
                long montantCredit = 0L;
                long partAssure = 0L;
                if (Objects.nonNull(t.get("partTiersPayant", BigDecimal.class))) {
                    montantCredit = t.get("partTiersPayant", BigDecimal.class).longValue();
                }
                if (Objects.nonNull(t.get("montantDiffere", BigDecimal.class))) {
                    montantCredit += t.get("montantDiffere", BigDecimal.class).longValue();
                }
                if (Objects.nonNull(t.get("partAssure", BigDecimal.class))) {
                    partAssure = t.get("partAssure", BigDecimal.class).longValue();
                }
                tableauPharmacienDTO
                    .setAmountToBePaid(t.get("amountToBePaid", BigDecimal.class).longValue())
                    .setMontantNetUg(t.get("montantNetUg", BigDecimal.class).longValue())
                    .setMontantTtcUg(t.get("montantTtcUg", BigDecimal.class).longValue())
                    .setMontantHtUg(t.get("montantHtUg", BigDecimal.class).longValue())
                    .setNombreVente(t.get("numberCount", Long.class).intValue())
                    .setAmountToBeTakenIntoAccount(t.get("amountToBeTakenIntoAccount", BigDecimal.class).longValue())
                    //   .setMontantAchat(t.get("montantAchat", BigDecimal.class).longValue())
                    .setMontantCredit(montantCredit)
                    .setPartAssure(partAssure)
                    .setMvtDate(
                        groupingByMonth
                            ? DateUtil.formaFromYearMonth(t.get("mvtDate", String.class))
                            : LocalDate.parse(t.get("mvtDate", String.class))
                    )
                    .setMontantComptant(
                        Objects.nonNull(t.get("montantPaye", BigDecimal.class)) ? t.get("montantPaye", BigDecimal.class).longValue() : 0L
                    )
                    .setMontantTtc(t.get("montantTtc", BigDecimal.class).longValue())
                    .setMontantHt(t.get("montantHt", BigDecimal.class).longValue())
                    .setMontantTaxe(t.get("montantTaxe", BigDecimal.class).longValue())
                    .setMontantRemise(t.get("montantDiscount", BigDecimal.class).longValue())
                    .setMontantNet(t.get("montantNet", BigDecimal.class).longValue());
                updateTableauPharmacienWrapper(tableauPharmacienWrapper, tableauPharmacienDTO);
                return tableauPharmacienDTO;
            })
            .collect(Collectors.toList());
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
            tableauPharmacien.setAchatFournisseus(computeMapGroupFournisseurAchat(tableauPharmacien.getGroupAchats()));
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
            tableauPharmacienDTO.setMontantBonAchat(
                tableauPharmacienDTO.getGroupAchats().stream().mapToLong(f -> f.getAchat().getMontantNet()).sum()
            );
            tableauPharmaciens.add(tableauPharmacienDTO);
        });
    }

    private TableauPharmacienWrapper computeData(MvtParam mvtParam) {
        TableauPharmacienWrapper tableauPharmacienWrapper = new TableauPharmacienWrapper();
        List<Tuple> tuples = executeQuery(mvtParam);
        List<TableauPharmacienDTO> tableauPharmaciens = buildTableauPharmacienFromTuple(
            tuples,
            tableauPharmacienWrapper,
            "month".equals(mvtParam.getGroupeBy())
        );
        List<Tuple> achatTuples = executeAchatQuery(mvtParam);
        List<AchatDTO> achats = buildAchatsFromTuple(achatTuples, tableauPharmacienWrapper);
        List<TableauPharmacienDTO> result = addAchatsToTableauPharmacien(tableauPharmaciens, new ArrayList<>(achats));

        tableauPharmacienWrapper.setTableauPharmaciens(result);
        computeAchats(tableauPharmacienWrapper);
        computeRatioVenteAchat(tableauPharmacienWrapper);
        computeRatioAchatVente(tableauPharmacienWrapper);
        tableauPharmacienWrapper.setAchatFournisseus(
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
            topN.addLast(new GroupeFournisseurDTO().setId(GROUP_OTHER_ID).setLibelle("Autres").setOdre(1000_1000));
            topN.sort(Comparator.comparing(GroupeFournisseurDTO::getOdre));
            return topN;
        } else {
            return all.stream().sorted(Comparator.comparing(GroupeFournisseur::getOdre)).map(GroupeFournisseurDTO::new).toList();
        }
    }
}
