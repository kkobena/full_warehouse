package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BalanceCaisseServiceImpl implements BalanceCaisseService {

    private static final Logger log = LoggerFactory.getLogger(BalanceCaisseServiceImpl.class);
    private final EntityManager entityManager;
    private final BalanceReportReportService balanceReportService;

    public BalanceCaisseServiceImpl(EntityManager entityManager, BalanceReportReportService balanceReportService) {
        this.entityManager = entityManager;
        this.balanceReportService = balanceReportService;
    }

    @Override
    public BalanceCaisseWrapper getBalanceCaisse(MvtParam mvtParam) {
        List<BalanceCaisseDTO> mvt = buildMvt(getMvt(mvtParam));
        BalanceCaisseWrapper balanceCaisseWrapper = computeBalanceCaisses(buildBalanceCaisses(getSales(mvtParam)));
        updateModePayment(balanceCaisseWrapper, mvt);
        computeMvts(balanceCaisseWrapper, mvt);
        balanceCaisseWrapper.setPeriode("Du " + DateUtil.format(mvtParam.getFromDate()) + " au " + DateUtil.format(mvtParam.getToDate()));
        return balanceCaisseWrapper;
    }

    private List<Tuple> getSales(MvtParam mvtParam) {
        try {
            return entityManager
                .createNativeQuery(SALE_QUERY + this.getWhereClause(mvtParam) + SALE_QUERY_GROUP_BY, Tuple.class)
                .setParameter(1, mvtParam.getFromDate())
                .setParameter(2, mvtParam.getToDate())
                .getResultList();
        } catch (Exception e) {
            log.error("Error getSales", e);
        }

        return Collections.emptyList();
    }

    private List<BalanceCaisseDTO> buildBalanceCaisses(List<Tuple> tuples) {
        List<BalanceCaisseDTO> balanceCaisseDTOS = new ArrayList<>();
        for (Tuple tuple : tuples) {
            BalanceCaisseDTO balanceCaisseDTO = new BalanceCaisseDTO();
            balanceCaisseDTO.setTypeVente(TypeVente.fromValue(tuple.get("typeSale", String.class)));
            balanceCaisseDTO.setCount(tuple.get("numberCount", Long.class).intValue());
            balanceCaisseDTO.setMontantDiscount(tuple.get("montantDiscount", BigDecimal.class).longValue());
            balanceCaisseDTO.setMontantTtc(tuple.get("montantTtc", BigDecimal.class).longValue());
            var montantPaye = tuple.get("montantPaye", BigDecimal.class);
            if (Objects.nonNull(montantPaye)) {
                balanceCaisseDTO.setMontantPaye(montantPaye.longValue());
            }
            balanceCaisseDTO.setMontantHt(tuple.get("montantHt", BigDecimal.class).longValue());
            balanceCaisseDTO.setMontantNet(tuple.get("montantNet", BigDecimal.class).longValue());
            balanceCaisseDTO.setModePaiement(tuple.get("modePaiement", String.class));
            balanceCaisseDTO.setLibelleModePaiement(tuple.get("libelleModePaiement", String.class));
            balanceCaisseDTO.setMontantAchat(tuple.get("montantAchat", BigDecimal.class).longValue());
            balanceCaisseDTO.setMontantDiffere(tuple.get("montantDiffere", BigDecimal.class).longValue());
            balanceCaisseDTO.setAmountToBePaid(tuple.get("amountToBePaid", BigDecimal.class).longValue());
            balanceCaisseDTO.setAmountToBeTakenIntoAccount(tuple.get("amountToBeTakenIntoAccount", BigDecimal.class).longValue());
            balanceCaisseDTO.setMontantNetUg(tuple.get("montantNetUg", BigDecimal.class).longValue());
            balanceCaisseDTO.setMontantTtcUg(tuple.get("montantTtcUg", BigDecimal.class).longValue());
            balanceCaisseDTO.setMontantHtUg(tuple.get("montantHtUg", BigDecimal.class).longValue());
            var partAssure = tuple.get("partAssure", BigDecimal.class);
            if (Objects.nonNull(partAssure)) {
                balanceCaisseDTO.setPartAssure(partAssure.longValue());
            }
            balanceCaisseDTO.setMontantTaxe(tuple.get("montantTaxe", BigDecimal.class).longValue());
            var partTiersPayant = tuple.get("partTiersPayant", BigDecimal.class);
            if (Objects.nonNull(partTiersPayant)) {
                balanceCaisseDTO.setPartTiersPayant(partTiersPayant.longValue());
            }
            balanceCaisseDTOS.add(balanceCaisseDTO);
        }
        return balanceCaisseDTOS;
    }

    private BalanceCaisseWrapper computeBalanceCaisses(List<BalanceCaisseDTO> balanceCaisseDTOS) {
        BalanceCaisseWrapper balanceCaisseWrapper = new BalanceCaisseWrapper();
        BalanceCaisseDTO vno = null;
        BalanceCaisseDTO vo = null;
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes = new ArrayList<>();
        List<BalanceCaisseDTO> balanceCaisses = new ArrayList<>();
        for (Entry<TypeVente, List<BalanceCaisseDTO>> typeVenteListEntry : balanceCaisseDTOS
            .stream()
            .collect(Collectors.groupingBy(BalanceCaisseDTO::getTypeVente))
            .entrySet()) {
            switch (typeVenteListEntry.getKey()) {
                case CASH_SALE:
                    if (vno == null) {
                        vno = new BalanceCaisseDTO();
                    }
                    vno.setTypeVeTypeAffichage(TransactionTypeAffichage.VNO);
                    vno.setTypeSale(TransactionTypeAffichage.VNO.name());
                    upadateBalance(vno, typeVenteListEntry.getValue(), mvtCaissesByModes);
                    break;
                case CREDIT_SALE, VENTES_DEPOT_AGREE:
                    if (vo == null) {
                        vo = new BalanceCaisseDTO();
                    }
                    vo.setTypeVeTypeAffichage(TransactionTypeAffichage.VO);
                    vo.setTypeSale(TransactionTypeAffichage.VO.name());
                    upadateBalance(vo, typeVenteListEntry.getValue(), mvtCaissesByModes);
                    break;
                case VENTES_DEPOTS:
                    balanceCaisseWrapper.setMontantDepot(
                        typeVenteListEntry.getValue().stream().mapToLong(BalanceCaisseDTO::getMontantTtc).sum()
                    );
                    break;
            }
        }
        updateBalanceCaisseWrapper(balanceCaisseWrapper, vo);
        updateBalanceCaisseWrapper(balanceCaisseWrapper, vno);
        if (vno != null) {
            vno.setMontantMarge((vno.getMontantNet() - vno.getMontantTaxe()) - vno.getMontantAchat());
            computePanierMoyen(vno);
            computePercent(vno, balanceCaisseWrapper);
            balanceCaisses.add(vno);
        }
        if (vo != null) {
            vo.setMontantMarge((vo.getMontantNet() - vo.getMontantTaxe()) - vo.getMontantAchat());
            computePanierMoyen(vo);
            computePercent(vo, balanceCaisseWrapper);
            balanceCaisses.add(vo);
        }
        balanceCaisseWrapper.setMontantMarge(
            (balanceCaisseWrapper.getMontantNet() - balanceCaisseWrapper.getMontantTaxe()) - balanceCaisseWrapper.getMontantAchat()
        );
        if (balanceCaisseWrapper.getCount() > 0) {
            balanceCaisseWrapper.setPanierMoyen(balanceCaisseWrapper.getMontantTtc() / balanceCaisseWrapper.getCount());
        }
        computeRatioVenteAchat(balanceCaisseWrapper);
        computeRatioAchatVente(balanceCaisseWrapper);
        balanceCaisseWrapper.setMvtCaissesByModes(mvtCaissesByModes);
        balanceCaisseWrapper.setBalanceCaisses(balanceCaisses);
        return balanceCaisseWrapper;
    }

    private void computeRatioVenteAchat(BalanceCaisseWrapper balanceCaisseWrapper) {
        if (balanceCaisseWrapper.getMontantAchat() == 0L) {
            return;
        }
        balanceCaisseWrapper.setRatioVenteAchat(
            BigDecimal.valueOf((double) balanceCaisseWrapper.getMontantTtc() / balanceCaisseWrapper.getMontantAchat())
                .setScale(2, RoundingMode.HALF_UP)
                .floatValue()
        );
    }

    private void computeRatioAchatVente(BalanceCaisseWrapper balanceCaisseWrapper) {
        if (balanceCaisseWrapper.getMontantTtc() == 0L) {
            return;
        }
        balanceCaisseWrapper.setRatioAchatVente(
            BigDecimal.valueOf((double) balanceCaisseWrapper.getMontantAchat() / balanceCaisseWrapper.getMontantTtc())
                .setScale(2, RoundingMode.HALF_UP)
                .floatValue()
        );
    }

    private void computePanierMoyen(BalanceCaisseDTO balanceCaisse) {
        if (balanceCaisse.getCount() > 0) {
            balanceCaisse.setPanierMoyen(balanceCaisse.getMontantTtc() / balanceCaisse.getCount());
        }
    }

    private void computePercent(BalanceCaisseDTO balanceCaisse, BalanceCaisseWrapper balanceCaisseWrapper) {
        var pourcentage = (short) Math.round(
            ((double) balanceCaisse.getMontantNet() * 100) / Math.abs(balanceCaisseWrapper.getMontantNet())
        );
        balanceCaisse.setTypeSalePercent(pourcentage);
    }

    private void updateBalanceCaisseWrapper(BalanceCaisseWrapper balanceCaisseWrapper, BalanceCaisseDTO b) {
        if (Objects.isNull(b)) {
            return;
        }
        balanceCaisseWrapper.setCount(balanceCaisseWrapper.getCount() + b.getCount());
        balanceCaisseWrapper.setMontantDiscount(balanceCaisseWrapper.getMontantDiscount() + b.getMontantDiscount());
        balanceCaisseWrapper.setMontantTtc(balanceCaisseWrapper.getMontantTtc() + b.getMontantTtc());
        balanceCaisseWrapper.setMontantPaye(balanceCaisseWrapper.getMontantPaye() + b.getMontantPaye());
        balanceCaisseWrapper.setMontantHt(balanceCaisseWrapper.getMontantHt() + b.getMontantHt());
        balanceCaisseWrapper.setMontantNet(balanceCaisseWrapper.getMontantNet() + b.getMontantNet());
        balanceCaisseWrapper.setMontantAchat(balanceCaisseWrapper.getMontantAchat() + b.getMontantAchat());
        balanceCaisseWrapper.setMontantMarge(balanceCaisseWrapper.getMontantMarge() + b.getMontantMarge());
        balanceCaisseWrapper.setAmountToBePaid(balanceCaisseWrapper.getAmountToBePaid() + b.getAmountToBePaid());
        balanceCaisseWrapper.setAmountToBeTakenIntoAccount(
            balanceCaisseWrapper.getAmountToBeTakenIntoAccount() + b.getAmountToBeTakenIntoAccount()
        );
        balanceCaisseWrapper.setMontantNetUg(balanceCaisseWrapper.getMontantNetUg() + b.getMontantNetUg());
        balanceCaisseWrapper.setMontantTtcUg(balanceCaisseWrapper.getMontantTtcUg() + b.getMontantTtcUg());
        balanceCaisseWrapper.setMontantHtUg(balanceCaisseWrapper.getMontantHtUg() + b.getMontantHtUg());
        balanceCaisseWrapper.setPartAssure(balanceCaisseWrapper.getPartAssure() + b.getPartAssure());
        balanceCaisseWrapper.setMontantTaxe(balanceCaisseWrapper.getMontantTaxe() + b.getMontantTaxe());
        balanceCaisseWrapper.setPartTiersPayant(balanceCaisseWrapper.getPartTiersPayant() + b.getPartTiersPayant());
        balanceCaisseWrapper.setMontantCash(balanceCaisseWrapper.getMontantCash() + b.getMontantCash());
        balanceCaisseWrapper.setMontantMobileMoney(balanceCaisseWrapper.getMontantMobileMoney() + b.getMontantMobileMoney());
        balanceCaisseWrapper.setMontantCard(balanceCaisseWrapper.getMontantCard() + b.getMontantCard());
        balanceCaisseWrapper.setMontantVirement(balanceCaisseWrapper.getMontantVirement() + b.getMontantVirement());
        balanceCaisseWrapper.setMontantCheck(balanceCaisseWrapper.getMontantCheck() + b.getMontantCheck());
        balanceCaisseWrapper.setMontantDiffere(balanceCaisseWrapper.getMontantDiffere() + b.getMontantDiffere());
    }

    private void upadateBalance(
        BalanceCaisseDTO b,
        List<BalanceCaisseDTO> balanceCaisses,
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes
    ) {
        for (BalanceCaisseDTO e : balanceCaisses) {
            b.setCount(b.getCount() + e.getCount());
            b.setMontantDiscount(b.getMontantDiscount() + e.getMontantDiscount());
            b.setMontantTtc(b.getMontantTtc() + e.getMontantTtc());
            b.setMontantPaye(b.getMontantPaye() + e.getMontantPaye());
            b.setMontantHt(b.getMontantHt() + e.getMontantHt());
            b.setMontantNet(b.getMontantNet() + e.getMontantNet());
            b.setMontantAchat(b.getMontantAchat() + e.getMontantAchat());
            b.setMontantMarge(b.getMontantMarge() + e.getMontantMarge());
            b.setAmountToBePaid(b.getAmountToBePaid() + e.getAmountToBePaid());
            b.setAmountToBeTakenIntoAccount(b.getAmountToBeTakenIntoAccount() + e.getAmountToBeTakenIntoAccount());
            b.setMontantNetUg(b.getMontantNetUg() + e.getMontantNetUg());
            b.setMontantTtcUg(b.getMontantTtcUg() + e.getMontantTtcUg());
            b.setMontantHtUg(b.getMontantHtUg() + e.getMontantHtUg());
            b.setPartAssure(b.getPartAssure() + e.getPartAssure());
            b.setMontantTaxe(b.getMontantTaxe() + e.getMontantTaxe());
            b.setPartTiersPayant(b.getPartTiersPayant() + e.getPartTiersPayant());
            ModePaimentCode modePaimentCode = ModePaimentCode.fromName(e.getModePaiement());
            if (Objects.nonNull(modePaimentCode)) {
                mvtCaissesByModes.add(
                    new com.kobe.warehouse.service.dto.records.Tuple(e.getModePaiement(), e.getLibelleModePaiement(), e.getMontantPaye())
                );
                switch (modePaimentCode) {
                    case CASH:
                        b.setMontantCash(b.getMontantCash() + e.getMontantPaye());
                        break;
                    case OM, MTN, MOOV, WAVE:
                        b.setMontantMobileMoney(b.getMontantMobileMoney() + e.getMontantPaye());
                        break;
                    case CB:
                        b.setMontantCard(b.getMontantCard() + e.getMontantPaye());
                        break;
                    case VIREMENT:
                        b.setMontantVirement(b.getMontantVirement() + e.getMontantPaye());
                        break;
                    case CH:
                        b.setMontantCheck(b.getMontantCheck() + e.getMontantPaye());
                        break;
                }
            }
        }
    }

    private List<Tuple> getMvt(MvtParam mvtParam) {
        Set<CategorieChiffreAffaire> categorieChiffreAffaires = Objects.requireNonNullElse(mvtParam.getCategorieChiffreAffaires(), Set.of());

        if (categorieChiffreAffaires.contains(CategorieChiffreAffaire.CALLEBASE) && categorieChiffreAffaires.size() > 1) {
            categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.CALLEBASE);
        }
        if (categorieChiffreAffaires.contains(CategorieChiffreAffaire.TO_IGNORE) && categorieChiffreAffaires.size() > 1) {
            categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.TO_IGNORE);
        }
        try {
            return entityManager
                .createNativeQuery(buildMvQuery(categorieChiffreAffaires), Tuple.class)
                .setParameter(1, mvtParam.getFromDate())
                .setParameter(2, mvtParam.getToDate())
                .getResultList();
        } catch (Exception e) {
            log.error("Error getMvt", e);
        }
        return Collections.emptyList();
    }

    private String buildMvQuery(Set<CategorieChiffreAffaire> categorieChiffreAffaires) {
        return String.format(
            MVT_QUERY,
            categorieChiffreAffaires.stream().map(e -> String.valueOf(e.ordinal())).collect(Collectors.joining(","))
        );
    }

    private List<BalanceCaisseDTO> buildMvt(List<Tuple> tuples) {
        List<BalanceCaisseDTO> balanceCaisseDTOS = new ArrayList<>();
        for (Tuple tuple : tuples) {
            BalanceCaisseDTO balanceCaisseDTO = new BalanceCaisseDTO();
            balanceCaisseDTO.setMontantPaye(tuple.get("amount", BigDecimal.class).longValue());
            balanceCaisseDTO.setModePaiement(tuple.get("modePaiement", String.class));
            TypeFinancialTransaction typeFinancialTransaction = TypeFinancialTransaction.values()[tuple.get("typeTransaction", Byte.class)];
            balanceCaisseDTO.setLibelleModePaiement(tuple.get("libelleModePaiement", String.class));
            balanceCaisseDTO.setTypeVeTypeAffichage(typeFinancialTransaction.getTransactionTypeAffichage());
            balanceCaisseDTOS.add(balanceCaisseDTO);
        }
        return balanceCaisseDTOS;
    }

    private void computeMvts(BalanceCaisseWrapper balanceCaisseWrapper, List<BalanceCaisseDTO> balanceCaisses) {
        balanceCaisses
            .stream()
            .collect(Collectors.groupingBy(BalanceCaisseDTO::getTypeVeTypeAffichage))
            .forEach((k, v) -> {
                long amount = v.stream().mapToLong(BalanceCaisseDTO::getMontantPaye).sum();
                balanceCaisseWrapper.getMvtCaisses().add(new com.kobe.warehouse.service.dto.records.Tuple(k.name(), k.getValue(), amount));
            });
    }

    private List<com.kobe.warehouse.service.dto.records.Tuple> computeMvtModesPaiment(List<BalanceCaisseDTO> balanceCaisses) {
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes = new ArrayList<>();
        balanceCaisses
            .stream()
            .collect(Collectors.groupingBy(BalanceCaisseDTO::getModePaiement))
            .forEach((k, v) -> {
                long amount = v.stream().mapToLong(BalanceCaisseDTO::getMontantPaye).sum();
                mvtCaissesByModes.add(new com.kobe.warehouse.service.dto.records.Tuple(k, v.getFirst().getLibelleModePaiement(), amount));
            });
        return mvtCaissesByModes;
    }

    private void updateModePayment(BalanceCaisseWrapper balanceCaisseWrapper, List<BalanceCaisseDTO> balanceCaisses) {
        List<com.kobe.warehouse.service.dto.records.Tuple> mvtCaissesByModes = balanceCaisseWrapper.getMvtCaissesByModes();
        mvtCaissesByModes.addAll(computeMvtModesPaiment(balanceCaisses));
        balanceCaisseWrapper.setMvtCaissesByModes(new ArrayList<>());
        mvtCaissesByModes
            .stream()
            .collect(Collectors.groupingBy(com.kobe.warehouse.service.dto.records.Tuple::key))
            .forEach((k, v) -> {
                long amount = v.stream().mapToLong(e -> Long.parseLong(e.value().toString())).sum();
                balanceCaisseWrapper
                    .getMvtCaissesByModes()
                    .add(new com.kobe.warehouse.service.dto.records.Tuple(k, v.getFirst().libelle(), amount));
            });
    }

    private void computeModeAmounts(BalanceCaisseWrapper balanceCaisseWrapper) {
        balanceCaisseWrapper
            .getMvtCaissesByModes()
            .stream()
            .collect(Collectors.groupingBy(com.kobe.warehouse.service.dto.records.Tuple::key))
            .forEach((k, v) -> {
                long amount = v.stream().mapToLong(e -> Long.parseLong(e.value().toString())).sum();
                ModePaimentCode modePaimentCode = ModePaimentCode.fromName(k);
                switch (modePaimentCode) {
                    case CASH:
                        balanceCaisseWrapper.setMontantCash(balanceCaisseWrapper.getMontantCash() + amount);
                        break;
                    case OM, MTN, MOOV, WAVE:
                        balanceCaisseWrapper.setMontantMobileMoney(balanceCaisseWrapper.getMontantMobileMoney() + amount);
                        break;
                    case CB:
                        balanceCaisseWrapper.setMontantCard(balanceCaisseWrapper.getMontantCard() + amount);
                        break;
                    case VIREMENT:
                        balanceCaisseWrapper.setMontantVirement(balanceCaisseWrapper.getMontantVirement() + amount);
                        break;
                    case CH:
                        balanceCaisseWrapper.setMontantCheck(balanceCaisseWrapper.getMontantCheck() + amount);
                        break;
                    case null:
                        break;
                }
            });
    }

    @Override
    public Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException {
        return this.balanceReportService.exportToPdf(
            getBalanceCaisse(mvtParam),
            new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate())
        );
    }
}
