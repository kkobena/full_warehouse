package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.config.IdGeneratorService;
import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.UserService;
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
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final UserService userService;
    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final BanqueRepository banqueRepository;
    private final IdGeneratorService idGeneratorService;
    private final InvoicePaymentItemService invoicePaymentItemService;

    protected AbstractReglementService(
        CashRegisterService cashRegisterService,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        BanqueRepository banqueRepository,
        IdGeneratorService idGeneratorService,
        InvoicePaymentItemService invoicePaymentItemService
    ) {
        this.cashRegisterService = cashRegisterService;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.userService = userService;
        this.facturationRepository = facturationRepository;

        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.banqueRepository = banqueRepository;
        this.idGeneratorService = idGeneratorService;
        this.invoicePaymentItemService = invoicePaymentItemService;
        this.idGeneratorService.setSequenceName("id_transaction_seq");
    }

    protected CashRegister getCashRegister() {
        return cashRegisterService.getCashRegister();
    }

    protected InvoicePaymentItem buildInvoicePaymentItem(ThirdPartySaleLine thirdPartySaleLine, InvoicePayment invoicePayment, int amount) {
        return this.invoicePaymentItemService.buildInvoicePaymentItem(thirdPartySaleLine, invoicePayment, amount);
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

    private InvoicePayment getNew() {
        InvoicePayment invoice = new InvoicePayment();
        invoice.setId(this.idGeneratorService.nextId());
        return invoice;
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, ReglementParam reglementParam) {
        InvoicePayment invoice = getNew().setFactureTiersPayant(factureTiersPayant);
        invoice
            .setCashRegister(getCashRegister())
            .setMontantVerse(reglementParam.getAmount())
            .setTransactionDate(Objects.requireNonNullElse(reglementParam.getPaymentDate(), LocalDate.now()));
        invoice.setPaymentMode(fromCode(reglementParam.getModePaimentCode()));
        invoice.setBanque(buildBanque(reglementParam.getBanqueInfo()));
        return invoice;
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, InvoicePayment paymentParent) {
        InvoicePayment invoicePayment = getNew();
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
