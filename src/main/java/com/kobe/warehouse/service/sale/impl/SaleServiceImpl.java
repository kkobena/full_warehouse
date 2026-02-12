package com.kobe.warehouse.service.sale.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.PrivilegeException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SalesManager;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SaleServiceImpl extends SaleCommonService implements SaleService {

    private final SalesRepository salesRepository;
    private final UserRepository userRepository;
    private final UninsuredCustomerRepository uninsuredCustomerRepository;
    private final PaymentModeRepository paymentModeRepository;
    private final StorageService storageService;
    private final CashSaleRepository cashSaleRepository;
    private final SalesLineService salesLineService;
    private final PaymentService paymentService;
    private final UtilisationCleSecuriteService utilisationCleSecuriteService;
    private final RemiseRepository remiseRepository;
    private final SalesManager salesManager;

    public SaleServiceImpl(
        SalesRepository salesRepository,
        UserRepository userRepository,
        UninsuredCustomerRepository uninsuredCustomerRepository,
        PaymentModeRepository paymentModeRepository,
        StorageService storageService,
        CashSaleRepository cashSaleRepository,
        CashRegisterService cashRegisterService,
        SaleLineServiceFactory saleLineServiceFactory,
        PaymentService paymentService,
        ReferenceService referenceService,
        PosteRepository posteRepository,
        UtilisationCleSecuriteService utilisationCleSecuriteService,
        RemiseRepository remiseRepository,
        CustomerDisplayService afficheurPosService,
        SaleIdGeneratorService idGeneratorService, ObjectMapper objectMapper,
        SalesManager salesManager
    ) {
        super(
            referenceService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
            posteRepository,
            afficheurPosService,
            idGeneratorService, objectMapper
        );
        this.salesRepository = salesRepository;
        this.userRepository = userRepository;
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
        this.paymentModeRepository = paymentModeRepository;
        this.storageService = storageService;
        this.cashSaleRepository = cashSaleRepository;
        this.salesLineService = saleLineServiceFactory.getService(TypeVente.CashSale);
        this.paymentService = paymentService;
        this.utilisationCleSecuriteService = utilisationCleSecuriteService;
        this.remiseRepository = remiseRepository;
        this.salesManager = salesManager;
    }

    private AppUser getUserFormImport() {
        Optional<AppUser> user = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin);
        return user.orElseGet(() -> userRepository.findOneByLogin(Constants.SYSTEM).orElse(null));
    }



    @Override
    public CashSale fromDTOOldCashSale(CashSaleDTO dto) {
        CashSale c = new CashSale();
        c.setAmountToBePaid(dto.getAmountToBePaid());
        c.setCopy(dto.getCopy());
        c.setAmountToBeTakenIntoAccount(dto.getAmountToBeTakenIntoAccount());
        c.setImported(true);
        c.setCostAmount(dto.getCostAmount());
        c.setCreatedAt(dto.getCreatedAt());
        c.setUpdatedAt(dto.getUpdatedAt());
        c.setEffectiveUpdateDate(dto.getEffectiveUpdateDate());
        c.setDiscountAmount(dto.getDiscountAmount());
        c.setNetAmount(dto.getNetAmount());
        c.setPayrollAmount(dto.getPayrollAmount());
        c.setSalesAmount(dto.getSalesAmount());
        c.setToIgnore(dto.isToIgnore());
        c.setNumberTransaction(dto.getNumberTransaction());
        c.setTaxAmount(dto.getTaxAmount());
        c.setStatut(SalesStatut.CLOSED);
        c.setSalesAmount(dto.getSalesAmount());
        c.setRestToPay(dto.getRestToPay());
        if (StringUtils.isNotEmpty(dto.getUserFullName())) {
            userRepository.findOneByLogin(dto.getUserFullName()).ifPresentOrElse(c::setUser, () -> c.setUser(getUserFormImport()));
        } else {
            c.setUser(getUserFormImport());
        }
        if (StringUtils.isNotEmpty(dto.getSellerUserName())) {
            userRepository.findOneByLogin(dto.getSellerUserName()).ifPresentOrElse(c::setSeller, () -> c.setSeller(getUserFormImport()));
        } else {
            c.setSeller(getUserFormImport());
        }
        if (StringUtils.isNotEmpty(dto.getCustomerNum())) {
            uninsuredCustomerRepository.findOneByCode(dto.getCustomerNum()).ifPresent(c::setCustomer);
        }
        c.setMagasin(c.getUser().getMagasin());
        return c;
    }

    @Override
    public SalePayment buildPaymentFromDTO(PaymentDTO dto, Sales s) {
        SalePayment payment = new SalePayment();
        payment.setExpectedAmount(dto.getNetAmount());
        payment.setCreatedAt(dto.getCreatedAt());
        payment.setReelAmount(dto.getNetAmount());
        payment.setPaidAmount(dto.getPaidAmount());
        payment.setCashRegister(s.getCashRegister());
        PaymentMode paymentMode = paymentModeRepository
            .findById(dto.getPaymentCode())
            .orElse(paymentModeRepository.getReferenceById(Constants.MODE_ESP));
        payment.setPaymentMode(paymentMode);
        payment.setSale(s);
        if (s instanceof CashSale) {
            payment.setTypeFinancialTransaction(TypeFinancialTransaction.CASH_SALE);
        } else if (s instanceof ThirdPartySales) {
            payment.setTypeFinancialTransaction(TypeFinancialTransaction.CREDIT_SALE);
        }
        return payment;
    }

    private CashSale findOneById(SaleId id) {
        return this.cashSaleRepository.getReferenceById(id);
    }

    @Override
    public void setCustomer(UpdateSaleInfo keyValue) {
        CashSale cashSale = findOneById(keyValue.id());
        cashSale.setCustomer(getUninsuredCustomerById(keyValue.value()));
        this.cashSaleRepository.save(cashSale);
    }

    @Override
    public void removeCustomer(SaleId saleId) {
        CashSale cashSale = findOneById(saleId);
        cashSale.setCustomer(null);
        this.cashSaleRepository.save(cashSale);
    }

    private UninsuredCustomer getUninsuredCustomerById(Integer id) {
        return id != null ? uninsuredCustomerRepository.getReferenceById(id) : null;
    }

    @Override
    public CashSaleDTO createCashSale(CashSaleDTO dto) throws StockException, DeconditionnementStockOut {
        UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
        CashSale cashSale = new CashSale();
        this.intSale(dto, cashSale);
        cashSale.setCustomer(uninsuredCustomer);
        cashSale.setOrigineVente(OrigineVente.DIRECT);
        SalesLine saleLine = salesLineService.createSaleLineFromDTO(
            dto.getSalesLines().getFirst(),
            storageService.getDefaultConnectedUserMainStorage().getId()
        );
        cashSale.getSalesLines().add(saleLine);
        upddateCashSaleAmounts(cashSale);

        CashSale sale = salesRepository.save(cashSale);
        saleLine.setSales(sale);

        salesLineService.saveSalesLine(saleLine);
        this.displayNet(sale.getNetAmount());
        return new CashSaleDTO(sale);
    }

    @Override
    public void upddateCashSaleAmounts(CashSale c) {
        computeSaleEagerAmount(c);
        this.proccessDiscount(c);
        computeCashSaleAmountToPaid(c);
        arrondirMontantCaisse(c);
    }

    @Override
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO, boolean increment) throws StockException, DeconditionnementStockOut {
        return salesManager.updateItemQuantityRequested(saleLineDTO, findOneById(saleLineDTO.getSaleCompositeId()), increment);
    }

    @Override
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        return salesManager.updateItemQuantitySold(saleLineDTO, findOneById(saleLineDTO.getSaleCompositeId()));
    }

    @Override
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) {
        return salesManager.updateItemRegularPrice(saleLineDTO, findOneById(saleLineDTO.getSaleCompositeId()));
    }


    @Override
    public SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto) {
        return salesManager.addOrUpdateSaleLine(dto, findOneById(dto.getSaleCompositeId()));
    }


    private CashSale findOne(SaleId id) {
        return this.cashSaleRepository.getReferenceById(id);
    }


    @Override
    public FinalyseSaleDTO save(CashSaleDTO dto) throws PaymentAmountException, SaleNotFoundCustomerException, CashRegisterException {
        CashSale cashSale = cashSaleRepository
            .findOneWithEagerSalesLines(dto.getSaleId().getId(), dto.getSaleId().getSaleDate())
            .orElseThrow();
        UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
        cashSale.setCustomer(uninsuredCustomer);
        this.save(cashSale, dto);
        cashSale.setTvaEmbeded(buildTvaData(cashSale.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(cashSale, dto);
        salesRepository.save(cashSale);
        displayMonnaie(dto.getMontantRendu());

        return new FinalyseSaleDTO(cashSale.getId(), true);
    }

    /*
    Sauvegarder l etat de la vente
     */
    @Override
    public ResponseDTO putCashSaleOnHold(CashSaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        CashSale cashSale = findOne(dto.getSaleId());
        if (CollectionUtils.isEmpty(cashSale.getSalesLines())) {
            response.setSuccess(true);
            salesRepository.delete(cashSale);
            return response;
        }
        //  paymentService.buildPaymentFromFromPaymentDTO(cashSale, dto, user);
        UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
        cashSale.setCustomer(uninsuredCustomer);
        salesRepository.save(cashSale);
        response.setSuccess(true);
        return response;
    }

    @Override
    public void deleteSaleLineById(SaleLineId id) {
        salesManager.deleteSaleLineById(salesLineService.getOneById(id));
    }

    @Override
    public void deleteSalePrevente(SaleId id) {
        salesRepository
            .findOneWithEagerSalesLines(id.getId(), id.getSaleDate())
            .ifPresent(sales -> {
                paymentService.findAllBySales(sales.getId()).forEach(paymentService::delete);
                sales.getSalesLines().forEach(salesLineService::deleteSaleLine);
                salesRepository.delete(sales);
            });
    }

    @Override
    public void cancelCashSale(SaleId id) {
        AppUser user = storageService.getUser();
        cashSaleRepository
            .findOneWithEagerSalesLines(id.getId(), id.getSaleDate())
            .ifPresent(sales -> {
                if (sales.isCanceled()) {
                    throw new GenericError("La vente est déjà annulée");
                }
                CashSale copy = (CashSale) sales.clone();
                copySale(sales, copy);
                setId(copy);
                copy.setSaleDate(LocalDate.now());
                sales.setEffectiveUpdateDate(LocalDateTime.now());
                sales.setCanceled(true);
                copy.setCanceled(true);

                cashSaleRepository.save(sales);
                cashSaleRepository.save(copy);
                paymentService.findAllBySale(sales).forEach(payment -> paymentService.clonePayment(payment, copy));
                salesLineService.cloneSalesLine(
                    sales.getSalesLines(),
                    copy,
                    user,
                    storageService.getDefaultConnectedUserMainStorage().getId()
                );
            });
    }


    @Override
    public void upddateCashSaleAmountsOnRemovingItem(CashSale c, SalesLine saleLine) {
        computeSaleEagerAmountOnRemovingItem(c, saleLine);
        this.proccessDiscount(c);
        computeCashSaleAmountToPaid(c);
        computeSaleLazyAmountOnRemovingItem(c, saleLine);
        computeTvaAmountOnRemovingItem(c, saleLine);
    }

    @Override
    public void savePrevente(CashSaleDTO dto) {
        cashSaleRepository
            .findById(dto.getSaleId()).ifPresent(s -> {
                preValidatePrevente(s);
                cashSaleRepository.save(s);
            });
    }

    // java -jar your-application.jar
    // --spring.config.location=file:/path/to/your/additional-config1.yml,file:/path/to/your/additional-config2.yml
    @Override
    public void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO) throws PrivilegeException {
        this.utilisationCleSecuriteService.authorizeAction(utilisationCleSecuriteDTO, ThirdPartySaleService.class);
    }

    @Override
    public void processDiscount(UpdateSaleInfo keyValue) {
        cashSaleRepository
            .findById(keyValue.id())
            .ifPresent(cashSale -> {
                remiseRepository
                    .findById(keyValue.value())
                    .ifPresent(remise -> {
                        if (cashSale.getRemise() != null) {
                            this.removeRemise(cashSale);
                        }
                        if (remise instanceof RemiseProduit remiseProduit) {
                            this.applyRemiseProduit(cashSale, remiseProduit);
                        } else {
                            this.applyRemiseClient(cashSale, (RemiseClient) remise);
                        }
                        computeCashSaleAmountToPaid(cashSale);
                        arrondirMontantCaisse(cashSale);
                        this.cashSaleRepository.save(cashSale);
                        this.displayNet(cashSale.getNetAmount());
                    });
            });
    }

    @Override
    public void removeRemiseFromCashSale(SaleId salesId) {
        CashSale sales = findOne(salesId);
        this.removeRemise(sales);
        this.cashSaleRepository.save(sales);
        this.displayNet(sales.getNetAmount());
    }

    @Override
    public List<SaleLineDTO> findBySalesIdAndSalesSaleDateOrderByProduitLibelle(Long salesId, LocalDate saleDate) {
        return salesLineService.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(salesId, saleDate);
    }


}
