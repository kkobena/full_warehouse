package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.utils.DateUtil;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BalanceCaisseServiceImpl implements BalanceCaisseService {


    private final BalanceReportReportService balanceReportService;
    private final SalesRepository salesRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AppConfigurationService appConfigurationService;

    public BalanceCaisseServiceImpl(BalanceReportReportService balanceReportService,
                                    SalesRepository salesRepository,
                                    PaymentTransactionRepository paymentTransactionRepository, AppConfigurationService appConfigurationService) {

        this.balanceReportService = balanceReportService;
        this.salesRepository = salesRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.appConfigurationService = appConfigurationService;
    }

    public BalanceCaisseWrapper getBalanceCaisseNew(MvtParam mvtParam) {
        List<BalanceCaisseDTO> mvt = paymentTransactionRepository.fetchPaymentTransactionsForBalanceCaisse(mvtParam);
        BalanceCaisseWrapper balanceCaisseWrapper = computeBalanceCaisses(salesRepository.fetchSalesForBalanceCaisse(mvtParam));
        updateModePayment(balanceCaisseWrapper, mvt);
        computeMvts(balanceCaisseWrapper, mvt);
        balanceCaisseWrapper.setPeriode("Du " + DateUtil.format(mvtParam.getFromDate()) + " au " + DateUtil.format(mvtParam.getToDate()));
        return balanceCaisseWrapper;
    }

    @Override
    public BalanceCaisseWrapper getBalanceCaisse(MvtParam mvtParam) {
        mvtParam.setExcludeFreeUnit(appConfigurationService.excludeFreeUnit());
        return getBalanceCaisseNew(mvtParam);

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


    @Override
    public Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException {
        return this.balanceReportService.exportToPdf(
            getBalanceCaisse(mvtParam),
            new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate())
        );
    }
}
