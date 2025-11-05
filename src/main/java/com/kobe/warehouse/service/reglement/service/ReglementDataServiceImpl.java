package com.kobe.warehouse.service.reglement.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentItemRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.dto.OrganismeDTO;
import com.kobe.warehouse.service.errors.ReportFileExportException;
import com.kobe.warehouse.service.receipt.service.InvoiceReceiptService;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentWrapper;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReglementDataServiceImpl implements ReglementDataService {

    private final InvoicePaymentRepository invoicePaymentRepository;
    private final FacturationRepository facturationRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final InvoicePaymentItemRepository invoicePaymentItemRepository;
    private final ReglementReportService reglementReportService;
    private final InvoiceReceiptService invoiceReceiptService;

    public ReglementDataServiceImpl(
        FacturationRepository facturationRepository,
        InvoicePaymentRepository invoicePaymentRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        InvoicePaymentItemRepository invoicePaymentItemRepository,
        ReglementReportService reglementReportService,
        InvoiceReceiptService invoiceReceiptService
    ) {
        this.facturationRepository = facturationRepository;

        this.invoicePaymentRepository = invoicePaymentRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.invoicePaymentItemRepository = invoicePaymentItemRepository;
        this.reglementReportService = reglementReportService;
        this.invoiceReceiptService = invoiceReceiptService;
    }

    @Override
    @Transactional
    public void deleteReglement(PaymentId idReglement) {
        InvoicePayment invoicePayment = invoicePaymentRepository.getReferenceById(idReglement);
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
            invoicePaymentRepository.delete(invoicePayment);
        }
    }

    @Override
    @Transactional
    public void deleteReglement(Set<PaymentId> idReglements) {
        idReglements.forEach(this::deleteReglement);
    }

    @Override
    public void printReceipt(PaymentId idReglement) {
        this.invoiceReceiptService.printReceipt(null, getInvoicePaymentReceipt(idReglement));
    }

    @Override
    public List<InvoicePaymentDTO> fetchInvoicesPayments(InvoicePaymentParam invoicePaymentParam) {
        return fetchInvoicePayments(invoicePaymentParam).stream().map(InvoicePaymentDTO::new).toList();
    }

    private List<InvoicePayment> fetchInvoicePayments(InvoicePaymentParam invoicePaymentParam) {
        var startDate = Objects.isNull(invoicePaymentParam.dateDebut()) ? LocalDate.now() : invoicePaymentParam.dateDebut();
        var endDate = Objects.isNull(invoicePaymentParam.dateFin()) ? startDate : invoicePaymentParam.dateFin();
        Sort sort = Sort.by(Direction.ASC, "createdAt").and(Sort.by(Direction.ASC, "factureTiersPayant.tiersPayant.name"));
        if (invoicePaymentParam.grouped()) {
            sort = Sort.by(Direction.ASC, "createdAt").and(Sort.by(Direction.ASC, "factureTiersPayant.groupeTiersPayant.name"));
        }
        Specification<InvoicePayment> invoicePaymentSpecification = this.invoicePaymentRepository.periodeCriteria(startDate, endDate);
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
        return this.invoicePaymentRepository.findAll(invoicePaymentSpecification, sort);
    }

    @Override
    public List<InvoicePaymentItemDTO> getInvoicePaymentsItems(PaymentId paymentId) {
        return this.invoicePaymentItemRepository.findByInvoicePaymentIdAndInvoicePaymentTransactionDate(
                paymentId.getId(),
                paymentId.getTransactionDate()
            )
            .stream()
            .map(InvoicePaymentItemDTO::new)
            .toList();
    }

    @Override
    public List<InvoicePaymentDTO> getInvoicePaymentsGroupItems(PaymentId paymentId) {
        return this.invoicePaymentRepository.findInvoicePaymentByParentIdAndParentTransactionDate(
                paymentId.getId(),
                paymentId.getTransactionDate()
            )
            .stream()
            .map(InvoicePaymentDTO::new)
            .toList();
    }

    @Override
    public Resource printToPdf(InvoicePaymentParam invoicePaymentParam) throws ReportFileExportException {
        List<InvoicePaymentWrapper> invoicePaymentWrappers;
        if (invoicePaymentParam.grouped()) {
            invoicePaymentWrappers = buildGroupInvoicePaymentWrapper(invoicePaymentParam);
        } else {
            invoicePaymentWrappers = buildInvoicePaymentWrapper(invoicePaymentParam);
        }
        if (CollectionUtils.isEmpty(invoicePaymentWrappers)) {
            throw new ReportFileExportException();
        }
        if (invoicePaymentWrappers.size() == 1) {
            return this.reglementReportService.printToPdf(invoicePaymentWrappers.getFirst());
        }
        return this.reglementReportService.printToPdf(invoicePaymentWrappers);
    }

    @Override
    public byte[] generateEscPosReceiptForTauri(PaymentId idReglement) throws IOException {
        return this.invoiceReceiptService.generateEscPosReceiptForTauri(getInvoicePaymentReceipt(idReglement));
    }

    private InvoicePaymentReceiptDTO getInvoicePaymentReceipt(PaymentId idReglement) {
        return new InvoicePaymentReceiptDTO(this.invoicePaymentRepository.getReferenceById(idReglement));
    }

    private int deleteInvoicePayment(InvoicePayment invoicePayment, FactureTiersPayant groupeFacture) {
        FactureTiersPayant factureTiersPayant = invoicePayment.getFactureTiersPayant();
        int totalAmount = 0;
        int paidAmount = 0;
        for (InvoicePaymentItem invoicePaymentItem : invoicePayment.getInvoicePaymentItems()) {
            ThirdPartySaleLine thirdPartySaleLine = invoicePaymentItem.getThirdPartySaleLine();
            thirdPartySaleLine.setMontantRegle(Math.max(thirdPartySaleLine.getMontantRegle() - invoicePaymentItem.getPaidAmount(), 0));
            factureTiersPayant.setMontantRegle(Math.max(factureTiersPayant.getMontantRegle() - invoicePaymentItem.getPaidAmount(), 0));
            totalAmount += thirdPartySaleLine.getMontant();
            paidAmount += invoicePaymentItem.getPaidAmount();
            if (thirdPartySaleLine.getMontantRegle() == 0) {
                thirdPartySaleLine.setStatut(ThirdPartySaleStatut.ACTIF);
            } else {
                thirdPartySaleLine.setStatut(ThirdPartySaleStatut.HALF_PAID);
            }
            thirdPartySaleLineRepository.save(thirdPartySaleLine);
        }
        invoicePaymentRepository.delete(invoicePayment);

        updateFactureTiersPayantStatus(factureTiersPayant, totalAmount);
        facturationRepository.save(factureTiersPayant);
        if (groupeFacture != null) {
            factureTiersPayant.setMontantRegle(Math.max(groupeFacture.getMontantRegle() - paidAmount, 0));
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

    private List<InvoicePaymentWrapper> buildInvoicePaymentWrapper(InvoicePaymentParam invoicePaymentParam) {
        var periode = buildPeriode(invoicePaymentParam);
        List<InvoicePaymentWrapper> invoicePaymentWrappers = new ArrayList<>();
        fetchInvoicePayments(invoicePaymentParam)
            .stream()
            .collect(Collectors.groupingBy(i -> i.getFactureTiersPayant().getTiersPayant()))
            .forEach((k, v) -> {
                InvoicePaymentWrapper invoicePaymentWrapper = new InvoicePaymentWrapper();
                invoicePaymentWrapper.setOrganisme(new OrganismeDTO(k));
                invoicePaymentWrapper.setInvoicePayments(v.stream().map(InvoicePaymentDTO::new).toList());
                invoicePaymentWrapper.setPeriode(periode);
                invoicePaymentWrappers.add(invoicePaymentWrapper);
            });
        return invoicePaymentWrappers;
    }

    private List<InvoicePaymentWrapper> buildGroupInvoicePaymentWrapper(InvoicePaymentParam invoicePaymentParam) {
        List<InvoicePaymentWrapper> invoicePaymentWrappers = new ArrayList<>();
        var periode = buildPeriode(invoicePaymentParam);
        fetchInvoicePayments(invoicePaymentParam)
            .stream()
            .collect(Collectors.groupingBy(i -> i.getFactureTiersPayant().getGroupeTiersPayant()))
            .forEach((k, v) -> {
                InvoicePaymentWrapper invoicePaymentWrapper = new InvoicePaymentWrapper();
                invoicePaymentWrapper.setOrganisme(new OrganismeDTO(k));
                invoicePaymentWrapper.setInvoicePayments(v.stream().map(InvoicePaymentDTO::new).toList());
                invoicePaymentWrapper.setPeriode(periode);
                invoicePaymentWrappers.add(invoicePaymentWrapper);
            });
        return invoicePaymentWrappers;
    }

    private String buildPeriode(InvoicePaymentParam invoicePaymentParam) {
        var startDate = Objects.isNull(invoicePaymentParam.dateDebut()) ? LocalDate.now() : invoicePaymentParam.dateDebut();
        var endDate = Objects.isNull(invoicePaymentParam.dateFin()) ? startDate : invoicePaymentParam.dateFin();
        if (startDate.equals(endDate)) {
            return startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return (
            " DU " +
                startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " AU " +
                endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
    }
}
