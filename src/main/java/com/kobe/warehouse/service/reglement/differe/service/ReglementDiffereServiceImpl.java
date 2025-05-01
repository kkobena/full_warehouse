package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReglementDiffereServiceImpl implements ReglementDiffereService {

    private final SalesRepository salesRepository;

    public ReglementDiffereServiceImpl(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDiffere> getClientDiffere() {
        return this.salesRepository.getClientDiffere(Pageable.unpaged());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiffereItem> getDiffereItems(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    ) {
        return this.salesRepository.getDiffereItems(buildSpecification(customerId, search, startDate, endDate, paymentStatuses), pageable);
    }

    private Specification<Sales> buildSpecification(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses
    ) {
        startDate = Objects.requireNonNullElse(startDate, LocalDate.now());
        endDate = Objects.requireNonNullElse(endDate, LocalDate.now());
        Specification<Sales> specification = Specification.where(this.salesRepository.filterByPeriode(startDate, endDate));
        specification = specification.and(this.salesRepository.filterByCustomerId(customerId));
        specification = specification.and(this.salesRepository.filterNumberTransaction(search));
        specification = specification.and(this.salesRepository.filterByPaymentStatus(paymentStatuses));
        return specification;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiffereDTO> getDiffere(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    ) {
        return getAllDiffere(buildSpecification(customerId, search, startDate, endDate, paymentStatuses), pageable).map(differe -> {
            List<DiffereItem> differeItems =
                this.salesRepository.getDiffereItems(
                        Specification.where(this.salesRepository.filterByCustomerId(differe.customerId())),
                        Pageable.unpaged()
                    ).getContent();
            return new DiffereDTO(
                differe.customerId(),
                differe.firstName(),
                differe.lastName(),
                differe.saleAmount(),
                differe.paidAmount(),
                differe.rest(),
                differeItems
            );
        });
    }

    private Page<DiffereDTO> getAllDiffere(Specification<Sales> specification, Pageable pageable) {
        return this.salesRepository.getDiffere(specification, pageable).map(differe -> {
                List<DiffereItem> differeItems =
                    this.salesRepository.getDiffereItems(
                            Specification.where(this.salesRepository.filterByCustomerId(differe.customerId())),
                            Pageable.unpaged()
                        ).getContent();
                return new DiffereDTO(
                    differe.customerId(),
                    differe.firstName(),
                    differe.lastName(),
                    differe.saleAmount(),
                    differe.paidAmount(),
                    differe.rest(),
                    differeItems
                );
            });
    }

    @Override
    public Optional<DiffereDTO> getOne(Long id) {
        Specification<Sales> specification = Specification.where(this.salesRepository.filterByCustomerId(id));
        specification = specification.and(this.salesRepository.filterByPaymentStatus(Set.of(PaymentStatus.IMPAYE)));
        return Optional.ofNullable(getAllDiffere(specification, Pageable.unpaged()).getContent().getFirst());
    }
}
