package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.config.IdGeneratorService;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementGroupeFactureService extends AbstractReglementService {

    private final FacturationRepository facturationRepository;
    private final ReglementFactureModeAllService reglementFactureModeAllService;

    public ReglementGroupeFactureService(
        CashRegisterService cashRegisterService,
        InvoicePaymentRepository invoicePaymentRepository,
        UserService userService,
        FacturationRepository facturationRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        BanqueRepository banqueRepository,
        ReglementFactureModeAllService reglementFactureModeAllService,
        IdGeneratorService idGeneratorService,
        InvoicePaymentItemService invoicePaymentItemService
    ) {
        super(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            idGeneratorService,
            invoicePaymentItemService
        );
        this.facturationRepository = facturationRepository;
        this.reglementFactureModeAllService = reglementFactureModeAllService;
    }

    @Override
    public ResponseReglementDTO doReglement(ReglementParam reglementParam) throws CashRegisterException, PaymentAmountException {
        FactureTiersPayant factureTiersPayant = this.facturationRepository.findFactureTiersPayantById(reglementParam.getId()).orElseThrow();

        InvoicePayment invoicePayment = super.buildInvoicePayment(factureTiersPayant, reglementParam);
        invoicePayment.setGrouped(true);
        int montantPaye = 0;

        List<InvoicePayment> invoicePayments = new ArrayList<>();
        int totalAmount = reglementParam.getTotalAmount();
        if (totalAmount > reglementParam.getAmount()) {
            throw new PaymentAmountException();
        }

        for (FactureTiersPayant item : factureTiersPayant.getFactureTiersPayants()) {
            var invoicePaymentItem = this.reglementFactureModeAllService.doReglement(invoicePayment, item);
            montantPaye += invoicePaymentItem.getPaidAmount();
            invoicePayments.add(invoicePaymentItem);
        }

        super.updateFactureTiersPayant(factureTiersPayant, montantPaye);
        factureTiersPayant.setStatut(
            factureTiersPayant.getMontantRegle() < reglementParam.getMontantFacture() ? InvoiceStatut.PARTIALLY_PAID : InvoiceStatut.PAID
        );
        super.saveFactureTiersPayant(factureTiersPayant);
        invoicePayment.setExpectedAmount(totalAmount);
        invoicePayment.setPaidAmount(montantPaye);
        invoicePayment.setReelAmount(montantPaye);
        invoicePayment.setCommentaire(reglementParam.getComment());
        invoicePayment = super.saveInvoicePayment(invoicePayment);
        for (InvoicePayment item : invoicePayments) {
            item.setParent(invoicePayment);
        }
        super.saveInvoicePayments(invoicePayments);
        return new ResponseReglementDTO(invoicePayment.getId().getId(), factureTiersPayant.getStatut() == InvoiceStatut.PAID);
    }
}
