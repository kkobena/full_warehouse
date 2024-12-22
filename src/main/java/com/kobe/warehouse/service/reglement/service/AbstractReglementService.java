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
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentItemRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.reglement.dto.BanqueInfoDTO;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
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
    private final WarehouseCalendarService warehouseCalendarService;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final InvoicePaymentItemRepository invoicePaymentItemRepository;
    private final BanqueRepository banqueRepository;

    protected AbstractReglementService(
        CashRegisterService cashRegisterService,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        WarehouseCalendarService warehouseCalendarService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        InvoicePaymentItemRepository invoicePaymentItemRepository,
        BanqueRepository banqueRepository
    ) {
        this.cashRegisterService = cashRegisterService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.userService = userService;
        this.facturationRepository = facturationRepository;
        this.warehouseCalendarService = warehouseCalendarService;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.invoicePaymentItemRepository = invoicePaymentItemRepository;
        this.banqueRepository = banqueRepository;
    }

    protected void savePaymentTransaction(InvoicePayment invoicePayment, String comment) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setAmount(invoicePayment.getPaidAmount());
        paymentTransaction.setPaymentMode(invoicePayment.getPaymentMode());
        paymentTransaction.setUser(userService.getUser());
        paymentTransaction.setTypeFinancialTransaction(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT);
        paymentTransaction.setCreatedAt(invoicePayment.getCreated());
        paymentTransaction.setCalendar(warehouseCalendarService.initCalendar());
        paymentTransaction.setCashRegister(getCashRegister());
        paymentTransaction.setOrganismeId(invoicePayment.getId());
        paymentTransaction.setCommentaire(comment);
        paymentTransactionRepository.save(paymentTransaction);
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
        invoicePaymentItem.setRestToPay(invoicePaymentItem.getAmount() - amount);
        return invoicePaymentItem;
    }

    protected void updateThirdPartyLine(ThirdPartySaleLine thirdPartySaleLine, int amount) {
        thirdPartySaleLine.setMontantRegle(thirdPartySaleLine.getMontantRegle() + amount);
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, ReglementParam reglementParam) {
        InvoicePayment invoicePayment = new InvoicePayment();
        invoicePayment.setBanque(buildBanque(reglementParam.getBanqueInfo()));
        invoicePayment.setFactureTiersPayant(factureTiersPayant);
        invoicePayment.setCashRegister(getCashRegister());
        invoicePayment.setInvoiceDate(reglementParam.getPaymentDate());
        invoicePayment.setPaymentMode(fromCode(reglementParam.getModePaimentCode()));
        return invoicePayment;
    }

    protected InvoicePayment buildInvoicePayment(FactureTiersPayant factureTiersPayant, InvoicePayment paymentParent) {
        InvoicePayment invoicePayment = new InvoicePayment();
        invoicePayment.setBanque(paymentParent.getBanque());
        invoicePayment.setFactureTiersPayant(factureTiersPayant);
        invoicePayment.setCashRegister(paymentParent.getCashRegister());
        invoicePayment.setPaymentMode(paymentParent.getPaymentMode());
        invoicePayment.setInvoiceDate(paymentParent.getInvoiceDate());
        return invoicePayment;
    }

    private PaymentMode fromCode(ModePaimentCode mode) {
        return new PaymentMode().code(mode.name());
    }

    protected void updateFactureTiersPayant(FactureTiersPayant factureTiersPayant, int amount, int paidAmount) {
        factureTiersPayant.setMontantRegle(Objects.requireNonNullElse(factureTiersPayant.getMontantRegle(), 0) + paidAmount);
        if (factureTiersPayant.getMontantRegle() >= amount) {
            factureTiersPayant.setStatut(InvoiceStatut.PAID);
        } else {
            factureTiersPayant.setStatut(InvoiceStatut.PARTIALLY_PAID);
        }
        factureTiersPayant.setUser(userService.getUser());
    }

    protected InvoicePayment saveInvoicePayment(InvoicePayment invoicePayment) {
        return invoicePaymentRepository.save(invoicePayment);
    }

    protected void saveFactureTiersPayant(FactureTiersPayant factureTiersPayant) {
        facturationRepository.save(factureTiersPayant);
    }

    protected void saveThirdPartyLines(List<ThirdPartySaleLine> thirdPartySaleLines) {
        thirdPartySaleLineRepository.saveAll(thirdPartySaleLines);
    }

    protected void saveItems(List<InvoicePaymentItem> items) {
        invoicePaymentItemRepository.saveAll(items);
    }

    protected Banque buildBanque(BanqueInfoDTO banqueInfo) {
        if (Objects.isNull(banqueInfo)) {
            return null;
        }
        return banqueRepository.save(new Banque().setNom(banqueInfo.getNom()).setAdresse(banqueInfo.getAdresse()));
    }

    protected void saveInvoicePayments(List<InvoicePayment> items) {
        invoicePaymentRepository.saveAll(items);
    }
}
