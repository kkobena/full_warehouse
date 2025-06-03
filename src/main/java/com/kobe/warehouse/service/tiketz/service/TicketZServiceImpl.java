package com.kobe.warehouse.service.tiketz.service;

import static java.util.Objects.isNull;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.PaymentGroup;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.service.PaymentModeService;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.Tuple;
import com.kobe.warehouse.service.receipt.service.TicketZPrinterService;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZCreditProjection;
import com.kobe.warehouse.service.tiketz.dto.TicketZData;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import com.kobe.warehouse.service.tiketz.dto.TicketZProjection;
import com.kobe.warehouse.service.tiketz.dto.TicketZRecap;
import java.awt.print.PrinterException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class TicketZServiceImpl implements TicketZService {

    private final SalePaymentRepository salePaymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SalesRepository salesRepository;
    private final ThirdPartySaleRepository thirdPartySaleRepository;
    private final PaymentModeService paymentModeService;
    private final TicketZPrinterService ticketZPrinterService;
    private final TicketZReportService ticketZReportService;

    public TicketZServiceImpl(
        SalePaymentRepository salePaymentRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        SalesRepository salesRepository,
        ThirdPartySaleRepository thirdPartySaleRepository,
        PaymentModeService paymentModeService,
        TicketZPrinterService ticketZPrinterService,
        TicketZReportService ticketZReportService
    ) {
        this.salePaymentRepository = salePaymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.salesRepository = salesRepository;
        this.thirdPartySaleRepository = thirdPartySaleRepository;
        this.paymentModeService = paymentModeService;
        this.ticketZPrinterService = ticketZPrinterService;
        this.ticketZReportService = ticketZReportService;
    }

    @Override
    public TicketZ getTicketZ(TicketZParam param) {
        return combineAll(param);
    }

    @Override
    public void printTicketZ(String hostName, TicketZParam param) throws PrinterException {
        Pair periode = getPeriode(param);
        this.ticketZPrinterService.printTicketZ(
                hostName,
                getTicketZ(param),
                (LocalDateTime) periode.key(),
                (LocalDateTime) periode.value()
            );
    }

    @Override
    public ResponseEntity<byte[]> generatePdf(TicketZParam param) {
        return this.ticketZReportService.generatePdf(this.getTicketZ(param), getPeriode(param));
    }

    private TicketZ combineAll(TicketZParam param) {
        Pair periode = getPeriode(param);
        List<TicketZProjection> ticketZProjections = param.onlyVente() ? fetchSalesPayment(param, periode) : fetchAllMvts(param, periode);
        List<TicketZCreditProjection> creditProjections = getTicketZCreditProjection(param, periode);
        creditProjections.addAll(getTicketZDifferes(param, periode));

        return combineCreditPerUser(ticketZProjections, creditProjections);
    }

    private TicketZ combineCreditPerUser(List<TicketZProjection> ticketZProjections, List<TicketZCreditProjection> creditProjections) {
        Map<Long, List<TicketZCreditProjection>> creditProjectionsMap = creditProjections
            .stream()
            .collect(Collectors.groupingBy(TicketZCreditProjection::userId));
        AtomicLong montantMobileG = new AtomicLong(0);
        AtomicLong montantMobileG2 = new AtomicLong(0);
        Map<ModePaimentCode, Tuple> summary = new HashMap<>();
        AtomicLong creditAmount = new AtomicLong(0);
        List<TicketZRecap> ticketZRecaps = new ArrayList<>();
        ticketZProjections
            .stream()
            .collect(Collectors.groupingBy(TicketZProjection::userId))
            .forEach((userId, data) -> {
                List<TicketZData> summaryMobile = new ArrayList<>(); //pour mobile
                List<TicketZData> ticketZDataUser = new ArrayList<>();
                TicketZProjection ticketZProjection = data.getFirst();
                var userName = String.format("%s. %s", ticketZProjection.firstName().charAt(0), ticketZProjection.lastName());
                AtomicLong montantMobile = new AtomicLong(0);
                AtomicLong montantMobile2 = new AtomicLong(0);

                data
                    .stream()
                    .collect(Collectors.groupingBy(TicketZProjection::modePaimentCode))
                    .forEach((modePaimentCode, ticketZProjectionList) -> {
                        long montant = 0;
                        long montant1 = 0;
                        TicketZProjection firstProjection = ticketZProjectionList.getFirst();
                        for (TicketZProjection d : ticketZProjectionList) {
                            if (d.credit()) {
                                montant += ((-1) * d.montant());
                                montant1 += ((-1) * d.montantReel());
                            } else {
                                montant += d.montant();
                                montant1 += d.montantReel();
                            }
                            if (modePaimentCode.getPaymentGroup() == PaymentGroup.MOBILE) {
                                montantMobile.addAndGet(montant);
                                montantMobile2.addAndGet(montant1);
                                montantMobileG.addAndGet(montant);
                                montantMobileG2.addAndGet(montant1);
                            }
                        }

                        if (summary.containsKey(modePaimentCode)) {
                            Tuple tuple = summary.get(modePaimentCode);
                            summary.put(modePaimentCode, new Tuple(tuple.e1() + montant, tuple.e2() + montant1, 0L));
                        } else {
                            summary.put(modePaimentCode, new Tuple(montant, montant1, 0L));
                        }
                        if (montantMobile.get() > 0) {
                            summaryMobile.add(new TicketZData("Total Mobile", montantMobile.get(), montantMobile2.get(), 100));
                        }
                        ticketZDataUser.add(new TicketZData(firstProjection.libelle(), montant, montant1, modePaimentCode.getSortOrder()));
                        List<TicketZCreditProjection> userCredit = creditProjectionsMap.remove(userId);
                        if (!CollectionUtils.isEmpty(userCredit)) {
                            long totalCredit = userCredit.stream().mapToLong(TicketZCreditProjection::montant).sum();
                            ticketZDataUser.add(new TicketZData("Crédit(vno/vo)", totalCredit, totalCredit, 101));
                            creditAmount.addAndGet(totalCredit);
                        }
                    });
                ticketZDataUser.sort(Comparator.comparing(TicketZData::sortOrder));
                ticketZRecaps.add(new TicketZRecap(userId, userName, ticketZDataUser, summaryMobile));
            });

        // Combine credit data

        creditProjectionsMap.forEach((userId, creditData) -> {
            TicketZCreditProjection firstCredit = creditData.getFirst();
            String userName = String.format("%s. %s", firstCredit.firstName().charAt(0), firstCredit.lastName());
            long totalCredit = creditData.stream().mapToLong(TicketZCreditProjection::montant).sum();

            creditAmount.addAndGet(totalCredit);

            ticketZRecaps.add(
                new TicketZRecap(userId, userName, List.of(new TicketZData("Crédit(vno/vo)", totalCredit, totalCredit, 101)), List.of())
            );
        });

        // build summary
        List<TicketZData> summaryData = new ArrayList<>();
        List<PaymentMode> paymentModes = this.paymentModeService.fetch();
        summary.forEach((modePaimentCode, tuple) -> {
            String libelleMode = getModePaimentLibelle(modePaimentCode, paymentModes);
            summaryData.add(new TicketZData(libelleMode, tuple.e1(), tuple.e2(), modePaimentCode.getSortOrder()));
        });
        if (montantMobileG.get() > 0) {
            summaryData.add(new TicketZData("Total Mobile", montantMobileG.get(), montantMobileG2.get(), 100));
        }

        if (creditAmount.get() > 0) {
            summaryData.add(new TicketZData("Crédit(vno/vo)", creditAmount.get(), creditAmount.get(), 101));
        }
        summaryData.sort(Comparator.comparing(TicketZData::sortOrder));
        ticketZRecaps.sort(Comparator.comparing(TicketZRecap::userName));
        return new TicketZ(summaryData, ticketZRecaps);
    }

    List<TicketZProjection> fetchAllMvts(TicketZParam param, Pair periode) {
        return this.paymentTransactionRepository.fetchAllMvts(getTicketZAllPaymentSpecification(param, periode));
    }

    private List<TicketZProjection> fetchSalesPayment(TicketZParam param, Pair periode) {
        return this.salePaymentRepository.fetchSalesPayment(getTicketZSalePaymentSpecification(param, periode));
    }

    private List<TicketZCreditProjection> getTicketZDifferes(TicketZParam param, Pair periode) {
        return this.salesRepository.getTicketZDifferes(getTicketZDiffereSpecification(param, periode));
    }

    private List<TicketZCreditProjection> getTicketZCreditProjection(TicketZParam param, Pair periode) {
        return thirdPartySaleRepository.getTicketZCreditProjection(getTicketZCreditSpecification(param, periode));
    }

    private Pair getPeriode(TicketZParam param) {
        if (isNull(param.fromTime()) || isNull(param.toTime())) {
            return new Pair(param.fromDate().atStartOfDay(), param.toDate().atTime(LocalTime.MAX));
        }
        return new Pair(param.fromDate().atTime(param.fromTime()), param.toDate().atTime(param.toTime()));
    }

    private Specification<Sales> getTicketZDiffereSpecification(TicketZParam param, Pair periode) {
        Specification<Sales> specification =
            this.salesRepository.filterByPeriode((LocalDateTime) periode.key(), (LocalDateTime) periode.value());

        if (!CollectionUtils.isEmpty(param.usersId())) {
            specification = specification.and(this.salesRepository.filterByCaissierId(param.usersId()));
        }
        return specification;
    }

    private Specification<ThirdPartySales> getTicketZCreditSpecification(TicketZParam param, Pair periode) {
        Specification<ThirdPartySales> specification =
            this.thirdPartySaleRepository.filterByPeriode((LocalDateTime) periode.key(), (LocalDateTime) periode.value());

        if (!CollectionUtils.isEmpty(param.usersId())) {
            specification = specification.and(this.thirdPartySaleRepository.filterByCaissierId(param.usersId()));
        }
        return specification;
    }

    private Specification<SalePayment> getTicketZSalePaymentSpecification(TicketZParam param, Pair periode) {
        Specification<SalePayment> specification =
            this.salePaymentRepository.filterByPeriode((LocalDateTime) periode.key(), (LocalDateTime) periode.value());

        if (!CollectionUtils.isEmpty(param.usersId())) {
            specification = specification.and(this.salePaymentRepository.filterByCaissierId(param.usersId()));
        }
        return specification;
    }

    private Specification<PaymentTransaction> getTicketZAllPaymentSpecification(TicketZParam param, Pair periode) {
        Specification<PaymentTransaction> specification =
            this.paymentTransactionRepository.filterByPeriode((LocalDateTime) periode.key(), (LocalDateTime) periode.value());

        if (!CollectionUtils.isEmpty(param.usersId())) {
            specification = specification.and(this.paymentTransactionRepository.filterByCaissierId(param.usersId()));
        }
        return specification;
    }

    private String getModePaimentLibelle(ModePaimentCode modePaimentCode, List<PaymentMode> paymentModes) {
        for (PaymentMode paymentMode : paymentModes) {
            if (paymentMode.getCode().equalsIgnoreCase(modePaimentCode.name())) {
                return paymentMode.getLibelle();
            }
        }
        return modePaimentCode.name();
    }
}
