package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentItemRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ReglementDataServiceImpl implements ReglementDataService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final ReglementReportService reglementReportService;
    private final InvoicePaymentItemRepository invoicePaymentItemRepository;

    public ReglementDataServiceImpl(
        FacturationRepository facturationRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        ReglementReportService reglementReportService,
        InvoicePaymentItemRepository invoicePaymentItemRepository
    ) {
        this.facturationRepository = facturationRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.reglementReportService = reglementReportService;
        this.invoicePaymentItemRepository = invoicePaymentItemRepository;
    }

    @Override
    @Transactional
    public void deleteReglement(long idReglement) {
        InvoicePayment invoicePayment = invoicePaymentRepository.findById(idReglement).orElseThrow();
        List<InvoicePayment> invoicePayments = invoicePayment.getInvoicePayments();
        if (CollectionUtils.isEmpty(invoicePayments)) {
            deleteInvoicePayment(invoicePayment, null);
        } else {
            FactureTiersPayant groupeFacture = invoicePayment.getFactureTiersPayant();
            int totalAmount = 0;
            for (InvoicePayment payment : invoicePayments) {
                totalAmount += deleteInvoicePayment(payment, groupeFacture);
            }
            updateFactureTiersPayantStatus(groupeFacture, totalAmount);
            facturationRepository.save(groupeFacture);
            paymentTransactionRepository.deleteByOrganismeId(invoicePayment.getId());
            invoicePaymentRepository.delete(invoicePayment);
        }
    }

    @Override
    public void printReceipt(long idReglement) {
        this.reglementReportService.printRecipt(getInvoicePaymentReceipt(idReglement));
    }

    @Override
    public List<InvoicePaymentWrapper> getInvoicePayments(InvoicePaymentParam invoicePaymentParam) {
        return List.of();
    }

    @Override
    public List<InvoicePaymentDTO> fetchInvoicesPayments(InvoicePaymentParam invoicePaymentParam) {
        return fetchInvoicePayments(invoicePaymentParam);
    }

    private List<InvoicePaymentDTO> fetchInvoicePayments(InvoicePaymentParam invoicePaymentParam) {
        var startDate = Objects.isNull(invoicePaymentParam.dateDebut()) ? LocalDate.now() : invoicePaymentParam.dateDebut();
        var endDate = Objects.isNull(invoicePaymentParam.dateFin()) ? startDate : invoicePaymentParam.dateFin();
        Sort sort = Sort.by(Direction.DESC, "created").and(Sort.by(Direction.ASC, "factureTiersPayant.tiersPayant.name"));
        if (invoicePaymentParam.grouped()) {
            sort = Sort.by(Direction.DESC, "created").and(Sort.by(Direction.ASC, "factureTiersPayant.groupeTiersPayant.name"));
        }
        Specification<InvoicePayment> invoicePaymentSpecification = Specification.where(
            this.invoicePaymentRepository.periodeCriteria(startDate, endDate)
        );
        invoicePaymentSpecification = invoicePaymentSpecification.and(
            this.invoicePaymentRepository.invoicesTypePredicats(invoicePaymentParam.grouped())
        );
        if (invoicePaymentParam.organismeId() != null) {
            if (invoicePaymentParam.grouped()) {
                invoicePaymentSpecification = invoicePaymentSpecification.and(
                    this.invoicePaymentRepository.filterByOrganismeId(invoicePaymentParam.organismeId())
                );
            } else {
                invoicePaymentSpecification = invoicePaymentSpecification.and(
                    this.invoicePaymentRepository.filterByTiersPayantId(invoicePaymentParam.organismeId())
                );
            }
        }
        if (StringUtils.hasText(invoicePaymentParam.search())) {
            invoicePaymentSpecification = invoicePaymentSpecification.and(
                this.invoicePaymentRepository.specialisationQueryString(invoicePaymentParam.search() + "%")
            );
        }
        return this.invoicePaymentRepository.findAll(invoicePaymentSpecification, sort).stream().map(InvoicePaymentDTO::new).toList();
    }

    @Override
    public List<InvoicePaymentItemDTO> getInvoicePaymentsItems(long idReglement) {
        return this.invoicePaymentItemRepository.findByInvoicePaymentId(idReglement).stream().map(InvoicePaymentItemDTO::new).toList();
    }

    @Override
    public List<InvoicePaymentDTO> getInvoicePaymentsGroupItems(long idReglement) {
        return this.invoicePaymentRepository.findInvoicePaymentByParentId(idReglement).stream().map(InvoicePaymentDTO::new).toList();
    }

    private InvoicePaymentReceiptDTO getInvoicePaymentReceipt(long idReglement) {
        return new InvoicePaymentReceiptDTO(this.invoicePaymentRepository.getReferenceById(idReglement));
    }

    private int deleteInvoicePayment(InvoicePayment invoicePayment, FactureTiersPayant groupeFacture) {
        FactureTiersPayant factureTiersPayant = invoicePayment.getFactureTiersPayant();
        int totalAmount = 0;
        int paidAmount = 0;
        for (InvoicePaymentItem invoicePaymentItem : invoicePayment.getInvoicePaymentItems()) {
            ThirdPartySaleLine thirdPartySaleLine = invoicePaymentItem.getThirdPartySaleLine();
            thirdPartySaleLine.setMontantRegle(thirdPartySaleLine.getMontantRegle() - invoicePaymentItem.getPaidAmount());
            factureTiersPayant.setMontantRegle(factureTiersPayant.getMontantRegle() - invoicePaymentItem.getPaidAmount());
            totalAmount += thirdPartySaleLine.getMontant();
            paidAmount += invoicePaymentItem.getPaidAmount();
            thirdPartySaleLineRepository.save(thirdPartySaleLine);
        }
        paymentTransactionRepository.deleteByOrganismeId(invoicePayment.getId());
        invoicePaymentRepository.delete(invoicePayment);

        updateFactureTiersPayantStatus(factureTiersPayant, totalAmount);
        facturationRepository.save(factureTiersPayant);
        if (groupeFacture != null) {
            factureTiersPayant.setMontantRegle(groupeFacture.getMontantRegle() - paidAmount);
        }
        return totalAmount;
    }

    private void updateFactureTiersPayantStatus(FactureTiersPayant factureTiersPayant, int totalAmount) {
        if (factureTiersPayant.getMontantRegle() == 0) {
            factureTiersPayant.setStatut(InvoiceStatut.NOT_PAID);
        } else if (factureTiersPayant.getMontantRegle() < totalAmount) {
            factureTiersPayant.setStatut(InvoiceStatut.PARTIALLY_PAID);
        } else {
            factureTiersPayant.setStatut(InvoiceStatut.PAID);
        }
        factureTiersPayant.setUpdated(LocalDateTime.now());
    }
}
