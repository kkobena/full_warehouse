package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SimplifiedSaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class SimplifiedSaleServiceImpl extends SaleCommonService implements SimplifiedSaleService {
    private final UninsuredCustomerRepository uninsuredCustomerRepository;
    private final CashRegisterService cashRegisterService;

    private final StorageService storageService;
    private final CashSaleRepository cashSaleRepository;
    private final SalesLineService salesLineService;
    private final PaymentService paymentService;

    public SimplifiedSaleServiceImpl(PaymentService paymentService, CashSaleRepository cashSaleRepository, ReferenceService referenceService, StorageService storageService, UserRepository userRepository, SaleLineServiceFactory saleLineServiceFactory, CashRegisterService cashRegisterService, PosteRepository posteRepository, CustomerDisplayService afficheurPosService, SaleIdGeneratorService idGeneratorService, UninsuredCustomerRepository uninsuredCustomerRepository) {
        super(referenceService, storageService, userRepository, saleLineServiceFactory, cashRegisterService, posteRepository, afficheurPosService, idGeneratorService);
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
        this.storageService = storageService;
        this.cashSaleRepository = cashSaleRepository;
        this.salesLineService = saleLineServiceFactory.getService(TypeVente.CashSale);
        this.paymentService = paymentService;
        this.cashRegisterService = cashRegisterService;

    }

    private UninsuredCustomer getUninsuredCustomerById(Integer id) {
        return id != null ? uninsuredCustomerRepository.getReferenceById(id) : null;
    }

    @Override
    public FinalyseSaleDTO createCashSale(CashSaleDTO dto) {
        AppUser user = storageService.getUser();

        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }

        UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
        CashSale cashSale = new CashSale();
        this.intSale(dto, cashSale);
        cashSale.setCashRegister(cashRegister);
        cashSale.setCustomer(uninsuredCustomer);
        List<SalesLine> saleLines = salesLineService.createSaleLinesFromDTO(cashSale,
            dto.getSalesLines(),
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        cashSale.setSalesLines(Set.copyOf(saleLines));
        prevalideSale(cashSale);
        cashSale.setTvaEmbeded(buildTvaData(cashSale.getSalesLines()));
        cashSale.setStatut(SalesStatut.CLOSED);
        computeSaleEagerAmount(cashSale);
        finalizeSale(cashSale, dto);
        CashSale sale = cashSaleRepository.save(cashSale);
        paymentService.buildPaymentFromFromPaymentDTO(cashSale, dto);
        salesLineService.saveAllSalesLines(cashSale.getSalesLines(), user, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());


        return new FinalyseSaleDTO(sale.getId(), true);
    }


    public Slice<CashSaleDTO> getList( //pour le mobile
                                       String search
    ) {


        LocalDate today = LocalDate.now();
        Specification<CashSale> specification = cashSaleRepository.between(today, today);
        specification = specification.and(cashSaleRepository.hasStatut(EnumSet.of(SalesStatut.CLOSED)));
        if (StringUtils.isNotBlank(search)) {
            specification = specification.and(cashSaleRepository.filterNumberTransaction(search));
        }
        specification = specification.and(cashSaleRepository.hasCaissier(storageService.getUser()));
        Pageable page = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        return cashSaleRepository.findAll(specification, page)
            .map(cashSale -> SaleDTO.toSaleDTOConverter(cashSale));
    }


}
