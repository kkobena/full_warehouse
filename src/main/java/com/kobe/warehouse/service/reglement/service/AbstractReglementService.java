package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.reglement.dto.BanqueInfoDTO;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public abstract class AbstractReglementService implements ReglementService {

    private final CashRegisterService cashRegisterService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final UserService userService;
    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    private final BanqueRepository banqueRepository;

    protected AbstractReglementService(
        CashRegisterService cashRegisterService,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        BanqueRepository banqueRepository
    ) {
        this.cashRegisterService = cashRegisterService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.userService = userService;
        this.facturationRepository = facturationRepository;

        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.banqueRepository = banqueRepository;
    }

    protected CashRegister getCashRegister() {
        var user = userService.getUser();
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        return cashRegister;
    }

    protected InvoicePaymentItem buildInvoicePaymentItem(ThirdPartySaleLine thirdPartySaleLine, InvoicePayment invoicePayment, int amount) {
        InvoicePaymentItem invoicePaymentItem = new InvoicePaymentItem();
        invoicePaymentItem.setAmount(thirdPartySaleLine.getMontant() - thirdPartySaleLine.getMontantRegle());
        invoicePaymentItem.setInvoicePayment(invoicePayment);
        invoicePaymentItem.setThirdPartySaleLine(thirdPartySaleLine);
        invoicePaymentItem.setPaidAmount(amount);
        return invoicePaymentItem;
    }

    protected void updateThirdPartyLine(ThirdPartySaleLine thirdPartySaleLine, int amount) {
        thirdPartySaleLine.setMontantRegle(thirdPartySaleLine.getMontantRegle() + amount);
        thirdPartySaleLine.setEffectiveUpdateDate(LocalDateTime.now());
        thirdPartySaleLine.setUpdated(thirdPartySaleLine.getEffectiveUpdateDate());
        if (thirdPartySaleLine.getMontant() <= thirdPartySaleLine.getMontantRegle()) {
            thirdPartySaleLine.setStatut(ThirdPartySaleStatut.PAID);
        } else {
            thirdPartySaleLine.setStatut(ThirdPartySaleStatut.HALF_PAID);
        }
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, ReglementParam reglementParam) {
        InvoicePayment invoice = new InvoicePayment()
            .setBanque(buildBanque(reglementParam.getBanqueInfo()))
            .setFactureTiersPayant(factureTiersPayant);
        invoice
            .setCashRegister(getCashRegister())
            .setMontantVerse(reglementParam.getAmount())
            .setTransactionDate(Objects.requireNonNullElse(reglementParam.getPaymentDate(), LocalDate.now()));
        invoice.setPaymentMode(fromCode(reglementParam.getModePaimentCode()));
        return invoice;
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, InvoicePayment paymentParent) {
        InvoicePayment invoicePayment = new InvoicePayment();
        invoicePayment.setBanque(paymentParent.getBanque());
        invoicePayment.setFactureTiersPayant(factureTiersPayant);
        invoicePayment.setCashRegister(paymentParent.getCashRegister());
        invoicePayment.setPaymentMode(paymentParent.getPaymentMode());
        invoicePayment.setTransactionDate(paymentParent.getTransactionDate());
        return invoicePayment;
    }

    private PaymentMode fromCode(ModePaimentCode mode) {
        return new PaymentMode().code(mode.name());
    }

    protected void updateFactureTiersPayant(FactureTiersPayant factureTiersPayant, int paidAmount) {
        factureTiersPayant.setMontantRegle(Objects.requireNonNullElse(factureTiersPayant.getMontantRegle(), 0) + paidAmount);
        factureTiersPayant.setUser(userService.getUser());
        factureTiersPayant.setUpdated(LocalDateTime.now());
    }

    protected InvoicePayment saveInvoicePayment(InvoicePayment invoicePayment) {
        invoicePayment.setTypeFinancialTransaction(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT);
        return invoicePaymentRepository.save(invoicePayment);
    }

    protected void saveFactureTiersPayant(FactureTiersPayant factureTiersPayant) {
        facturationRepository.save(factureTiersPayant);
    }

    protected void saveThirdPartyLines(List<ThirdPartySaleLine> thirdPartySaleLines) {
        thirdPartySaleLineRepository.saveAll(thirdPartySaleLines);
    }

    protected Banque buildBanque(BanqueInfoDTO banqueInfo) {
        if (Objects.isNull(banqueInfo)) {
            return null;
        }
        return banqueRepository.save(
            new Banque().setCode(banqueInfo.getCode()).setNom(banqueInfo.getNom()).setBeneficiaire(banqueInfo.getBeneficiaire())
        );
    }

    protected void saveInvoicePayments(List<InvoicePayment> items) {
        invoicePaymentRepository.saveAll(items);
    }

    protected void updateStatut(FactureTiersPayant factureTiersPayant, int montantFacture) {
        if (Objects.requireNonNullElse(factureTiersPayant.getMontantRegle(), 0) < montantFacture) {
            factureTiersPayant.setStatut(InvoiceStatut.PARTIALLY_PAID);
        } else {
            factureTiersPayant.setStatut(InvoiceStatut.PAID);
        }
    }
}
