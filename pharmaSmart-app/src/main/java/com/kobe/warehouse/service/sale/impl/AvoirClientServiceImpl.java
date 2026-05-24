package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.sale.AvoirClientService;
import com.kobe.warehouse.service.sale.dto.AvoirClientDTO;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AvoirClientServiceImpl implements AvoirClientService {

    private final SalesLineRepository salesLineRepository;

    public AvoirClientServiceImpl(SalesLineRepository salesLineRepository) {
        this.salesLineRepository = salesLineRepository;
    }

    @Override
    public Page<AvoirClientDTO> findAvoirs(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Specification<SalesLine> spec = salesLineRepository.hasAvoir();
        if (StringUtils.hasText(search)) {
            spec = spec.and(salesLineRepository.filterBySearchTerm(search));
        }
        if (fromDate != null && toDate != null) {
            spec = spec.and(salesLineRepository.filterByPeriode(fromDate, toDate));
        }
        return salesLineRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private AvoirClientDTO toDTO(SalesLine sl) {
        var sale = sl.getSales();
        Customer customer = sale.getCustomer();
        String customerName = customer != null
            ? (customer.getFirstName() + " " + Objects.requireNonNullElse(customer.getLastName(), "")).strip()
            : null;
        String sellerName = sale.getSeller().getFirstName() + " " + sale.getSeller().getLastName();
        FournisseurProduit fp = sl.getProduit().getFournisseurProduitPrincipal();
        String codeCip = fp != null ? fp.getCodeCip() : sl.getProduit().getCodeEanLaboratoire();
        int montant = sl.getQuantityAvoir() * sl.getNetUnitPrice();

        return new AvoirClientDTO(
            sale.getId().getId(),
            sale.getSaleDate(),
            sale.getNumberTransaction(),
            customerName,
            sellerName,
            sl.getId() != null ? sl.getId().getId() : null,
            sl.getProduit().getLibelle(),
            codeCip,
            sl.getQuantityAvoir(),
            sl.getRegularUnitPrice(),
            sl.getNetUnitPrice(),
            montant,
            sl.getUpdatedAt()
        );
    }
}
