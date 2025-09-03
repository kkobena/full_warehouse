package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
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
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.PrivilegeException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.utils.AfficheurPosService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
        AfficheurPosService afficheurPosService, SaleIdGeneratorService idGeneratorService
    ) {
        super(
            referenceService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
            posteRepository,
            afficheurPosService, idGeneratorService
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
    }

    private AppUser getUserFormImport() {
        Optional<AppUser> user = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin);
        return user.orElseGet(() -> userRepository.findOneByLogin(Constants.SYSTEM).orElse(null));
    }

    @Override
    public SaleLineDTO updateSaleLine(SaleLineDTO saleLine) {
        SalesLine salesLine = salesLineService.getOneById(saleLine.getId());
        int oldAmont = salesLine.getSalesAmount();
        int oldQty = salesLine.getQuantitySold();
        salesLineService.updateSaleLine(saleLine, salesLine);
        Sales sales = salesLine.getSales();
        sales.setSalesAmount((sales.getSalesAmount() - oldAmont) + salesLine.getSalesAmount());
        sales.setCostAmount(
            (sales.getCostAmount() - (oldQty * salesLine.getCostAmount())) + (salesLine.getQuantitySold() * salesLine.getCostAmount())
        );
        salesRepository.save(sales);
        this.displayNet(sales.getNetAmount());
        return new SaleLineDTO(salesLine);
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
    private CashSale findOneById(Long id) {
        return this.cashSaleRepository.findOneById(id);
    }

    @Override
    public void setCustomer(KeyValue keyValue) {
        CashSale cashSale = findOneById(keyValue.key());
        cashSale.setCustomer(getUninsuredCustomerById(keyValue.value()));
        this.cashSaleRepository.save(cashSale);
    }

    @Override
    public void removeCustomer(Long saleId) {
        CashSale cashSale = findOneById(saleId);
        cashSale.setCustomer(null);
        this.cashSaleRepository.save(cashSale);
    }

    private void computeCashSaleAmountToPaid(CashSale c) {
        c.setAmountToBePaid(c.getNetAmount());
        c.setRestToPay(c.getAmountToBePaid());
        c.setAmountToBeTakenIntoAccount(0);
    }

    private UninsuredCustomer getUninsuredCustomerById(Long id) {
        return id != null ? uninsuredCustomerRepository.getReferenceById(id) : null;
    }

    @Override
    public CashSaleDTO createCashSale(CashSaleDTO dto) {
        UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
        CashSale cashSale = new CashSale();
        this.intSale(dto, cashSale);
        cashSale.setCustomer(uninsuredCustomer);
        cashSale.setOrigineVente(OrigineVente.DIRECT);
        SalesLine saleLine = salesLineService.createSaleLineFromDTO(
            dto.getSalesLines().getFirst(),
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        cashSale.getSalesLines().add(saleLine);
        upddateCashSaleAmounts(cashSale);

        CashSale sale = salesRepository.save(cashSale);
        saleLine.setSales(cashSale);

        salesLineService.saveSalesLine(saleLine);
        this.displayNet(cashSale.getNetAmount());
        return new CashSaleDTO(sale);
    }

    private void upddateCashSaleAmounts(CashSale c) {
        computeSaleEagerAmount(c);
        this.proccessDiscount(c);
        computeCashSaleAmountToPaid(c);
        arrondirMontantCaisse(c);
    }

    @Override
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO) throws StockException, DeconditionnementStockOut {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        salesLineService.updateItemQuantityRequested(
            saleLineDTO,
            salesLine,
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
         finalizeSaleLineUpdate(salesLine);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        salesLineService.updateItemQuantitySold(salesLine, saleLineDTO, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
         finalizeSaleLineUpdate(salesLine);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        salesLineService.updateItemRegularPrice(saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
         finalizeSaleLineUpdate(salesLine);
        return new SaleLineDTO(salesLine);
    }

    private void finalizeSaleLineUpdate(SalesLine salesLine) {
        CashSale sales =(CashSale) salesLine.getSales();
        upddateCashSaleAmounts(sales);
        cashSaleRepository.saveAndFlush(sales);
        this.displayNet(sales.getNetAmount());

    }

    @Override
    public SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto) {
        return new SaleLineDTO(createOrUpdateSaleLine(dto));
    }

    private SalesLine createOrUpdateSaleLine(SaleLineDTO dto) {
        Optional<SalesLine> salesLineOp = salesLineService.findBySalesIdAndProduitId(dto.getSaleId(), dto.getProduitId());
        Long storageId = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
        if (salesLineOp.isPresent()) {
            SalesLine salesLine = salesLineOp.get();
            salesLineService.updateSaleLine(dto, salesLine, storageId);
            CashSale cashSale = (CashSale) salesLine.getSales();
            upddateCashSaleAmounts(cashSale);
            cashSaleRepository.save(cashSale);
            return salesLine;
        }
        SalesLine salesLine = salesLineService.create(dto, storageId, findOne(dto.getSaleId()));
        updateSaleWhenAddItem(dto, salesLine);
        return salesLine;
    }

    private CashSale findOne(Long id) {
        return this.cashSaleRepository.findOneById(id);
    }

    private void updateSaleWhenAddItem(SaleLineDTO dto, SalesLine salesLine) {
        CashSale sales = findOne(dto.getSaleId());
        upddateCashSaleAmounts(sales);
        salesLine.setSales(sales);
        salesRepository.save(sales);
    }

    @Override
    public FinalyseSaleDTO save(CashSaleDTO dto) throws PaymentAmountException, SaleNotFoundCustomerException, CashRegisterException {
        CashSale cashSale = cashSaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        this.save(cashSale, dto);
        UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
        cashSale.setCustomer(uninsuredCustomer);
        cashSale.setTvaEmbeded(buildTvaData(cashSale.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(cashSale, dto);
        salesRepository.save(cashSale);
        displayMonnaie(dto.getMontantRendu());

        return new FinalyseSaleDTO(cashSale.getId().getId(), true);
    }

    /*
    Sauvegarder l etat de la vente
     */
    @Override
    public ResponseDTO putCashSaleOnHold(CashSaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        AppUser user = storageService.getUser();
        CashSale cashSale = findOne(dto.getId());
        cashSale.setLastUserEdit(user);
        paymentService.buildPaymentFromFromPaymentDTO(cashSale, dto, user);
        UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
        cashSale.setCustomer(uninsuredCustomer);
        salesRepository.save(cashSale);
        response.setSuccess(true);
        return response;
    }

    @Override
    public void deleteSaleLineById(Long id) {
        SalesLine salesLine = salesLineService.getOneById(id);
        CashSale sales = (CashSale) salesLine.getSales();
        sales.removeSalesLine(salesLine);
        upddateCashSaleAmountsOnRemovingItem(sales, salesLine);
        sales.setUpdatedAt(LocalDateTime.now());
        sales.setLastUserEdit(storageService.getUser());
        sales.setEffectiveUpdateDate(sales.getUpdatedAt());
        cashSaleRepository.save(sales);
        salesLineService.deleteSaleLine(salesLine);
        this.displayNet(sales.getNetAmount());
    }

    @Override
    public void deleteSalePrevente(Long id) {
        salesRepository
            .findOneWithEagerSalesLines(id)
            .ifPresent(sales -> {
                paymentService.findAllBySalesId(sales.getId().getId()).forEach(paymentService::delete);
                sales.getSalesLines().forEach(salesLineService::deleteSaleLine);
                salesRepository.delete(sales);
            });
    }

    @Override
    public void cancelCashSale(Long id) {
        AppUser user = storageService.getUser();
        cashSaleRepository
            .findOneWithEagerSalesLines(id)
            .ifPresent(sales -> {
                CashSale copy = (CashSale) sales.clone();
                copySale(sales, copy);
                setId(copy);
                copy.setSaleDate(LocalDate.now());
                sales.setEffectiveUpdateDate(LocalDateTime.now());
                sales.setCanceled(true);
                copy.setCanceled(true);
                sales.setLastUserEdit(user);
                cashSaleRepository.save(sales);
                cashSaleRepository.save(copy);
                paymentService.findAllBySalesId(sales.getId().getId()).forEach(payment -> paymentService.clonePayment(payment, copy));
                salesLineService.cloneSalesLine(
                    sales.getSalesLines(),
                    copy,
                    user,
                    storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
                );
            });
    }

    private void copySale(Sales sales, Sales copy) {
        copy.setId(null);
        copy.setUpdatedAt(LocalDateTime.now());
        copy.setCreatedAt(copy.getUpdatedAt());
        copy.setEffectiveUpdateDate(copy.getUpdatedAt());
        buildReference(copy);
        copy.setCanceledSale(sales);
        copy.setStatut(SalesStatut.CANCELED);
        copy.setCostAmount(copy.getCostAmount() * (-1));
        copy.setNetAmount(copy.getNetAmount() * (-1));
        copy.setSalesAmount(copy.getSalesAmount() * (-1));
        copy.setHtAmount(copy.getHtAmount() * (-1));
        copy.setPayrollAmount(copy.getPayrollAmount() * (-1));
        copy.setRestToPay(copy.getRestToPay() * (-1));
        copy.setCopy(true);
        copy.setDiscountAmount(copy.getDiscountAmount() * (-1));
        copy.setTaxAmount(copy.getTaxAmount() * (-1));
        copy.setUser(sales.getUser());
        copy.setLastUserEdit(storageService.getUser());
        copy.setPayments(Collections.emptySet());
        copy.setSalesLines(Collections.emptySet());
    }

    private void upddateCashSaleAmountsOnRemovingItem(CashSale c, SalesLine saleLine) {
        computeSaleEagerAmountOnRemovingItem(c, saleLine);
        this.proccessDiscount(c);
        computeCashSaleAmountToPaid(c);
        computeSaleLazyAmountOnRemovingItem(c, saleLine);
        computeTvaAmountOnRemovingItem(c, saleLine);
    }

    // java -jar your-application.jar
    // --spring.config.location=file:/path/to/your/additional-config1.yml,file:/path/to/your/additional-config2.yml
    @Override
    public void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO) throws PrivilegeException {
        this.utilisationCleSecuriteService.authorizeAction(utilisationCleSecuriteDTO, ThirdPartySaleService.class);
    }

    @Override
    public void processDiscount(KeyValue keyValue) {
        cashSaleRepository
            .findCashSaleById(keyValue.key())
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
    public void removeRemiseFromCashSale(Long salesId) {
        CashSale sales = findOne(salesId);
        this.removeRemise(sales);
        this.cashSaleRepository.save(sales);
        this.displayNet(sales.getNetAmount());
    }
}
