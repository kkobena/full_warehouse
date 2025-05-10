package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.User;
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
import com.kobe.warehouse.service.WarehouseCalendarService;
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
import com.kobe.warehouse.service.sale.AvoirService;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.utils.AfficheurPosService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SaleServiceImpl extends SaleCommonService implements SaleService {

    private final Logger log = LoggerFactory.getLogger(SaleServiceImpl.class);
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
        WarehouseCalendarService warehouseCalendarService,
        AvoirService avoirService,
        PosteRepository posteRepository,
        UtilisationCleSecuriteService utilisationCleSecuriteService,
        RemiseRepository remiseRepository,
        AfficheurPosService afficheurPosService
    ) {
        super(
            referenceService,
            warehouseCalendarService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
            avoirService,
            posteRepository,
            afficheurPosService
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

    private User getUserFormImport() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin);
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
        c.setMargeUg(dto.getMargeUg());
        c.setToIgnore(dto.isToIgnore());
        c.setNumberTransaction(dto.getNumberTransaction());
        c.setTaxAmount(dto.getTaxAmount());
        c.setMontantnetUg(dto.getMontantnetUg());
        c.setMargeUg(dto.getMargeUg());
        c.setMontantTvaUg(dto.getMontantTvaUg());
        c.setMontantttcUg(dto.getMontantttcUg());
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

    @Override
    public void setCustomer(KeyValue keyValue) {
        CashSale cashSale = this.cashSaleRepository.getReferenceById(keyValue.key());
        cashSale.setCustomer(getUninsuredCustomerById(keyValue.value()));
        this.cashSaleRepository.save(cashSale);
    }

    @Override
    public void removeCustomer(Long saleId) {
        CashSale cashSale = this.cashSaleRepository.getReferenceById(saleId);
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
        upddateCashSaleAmounts(cashSale, saleLine, null);
        cashSale.getSalesLines().add(saleLine);
        CashSale sale = salesRepository.saveAndFlush(cashSale);
        saleLine.setSales(cashSale);

        salesLineService.saveSalesLine(saleLine);
        this.displayNet(cashSale.getNetAmount());
        return new CashSaleDTO(sale);
    }

    private void upddateCashSaleAmounts(CashSale c, SalesLine saleLine, SalesLine oldSaleLine) {
        computeSaleEagerAmount(c, saleLine.getSalesAmount(), Objects.nonNull(oldSaleLine) ? oldSaleLine.getSalesAmount() : 0);
        this.proccessDiscount(c);
        computeCashSaleAmountToPaid(c);
        computeSaleLazyAmount(c, saleLine, oldSaleLine);
        computeTvaAmount(c, saleLine, oldSaleLine);
        computeUgTvaAmount(c, saleLine, oldSaleLine);
        arrondirMontantCaisse(c);
    }

    @Override
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO) throws StockException, DeconditionnementStockOut {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        SalesLine oldSalesLine = (SalesLine) salesLine.clone();
        salesLineService.updateItemQuantityRequested(
            saleLineDTO,
            salesLine,
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        CashSale sales = (CashSale) salesLine.getSales();
        //   this.proccessDiscount(sales);
        upddateCashSaleAmounts(sales, salesLine, oldSalesLine);
        cashSaleRepository.saveAndFlush(sales);
        this.displayNet(sales.getNetAmount());
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        salesLineService.updateItemQuantitySold(salesLine, saleLineDTO, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        CashSale sales = (CashSale) salesLine.getSales();
        this.proccessDiscount(sales);
        cashSaleRepository.saveAndFlush(sales);
        this.displayNet(sales.getNetAmount());
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        SalesLine oldSalesLine = (SalesLine) salesLine.clone();
        salesLineService.updateItemRegularPrice(saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        Sales sales = salesLine.getSales();
        upddateCashSaleAmounts((CashSale) sales, salesLine, oldSalesLine);
        salesRepository.saveAndFlush(sales);
        this.displayNet(sales.getNetAmount());
        return new SaleLineDTO(salesLine);
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
            SalesLine oldSalesLine = (SalesLine) salesLine.clone();
            salesLineService.updateSaleLine(dto, salesLine, storageId);
            CashSale cashSale = (CashSale) salesLine.getSales();
            upddateCashSaleAmounts(cashSale, salesLine, oldSalesLine);
            cashSaleRepository.save(cashSale);
            return salesLine;
        }
        SalesLine salesLine = salesLineService.create(dto, storageId, cashSaleRepository.getReferenceById(dto.getSaleId()));
        updateSaleWhenAddItem(dto, salesLine);
        return salesLine;
    }

    private void updateSaleWhenAddItem(SaleLineDTO dto, SalesLine salesLine) {
        CashSale sales = cashSaleRepository.getReferenceById(dto.getSaleId());
        upddateCashSaleAmounts(sales, salesLine, null);
        salesLine.setSales(sales);
        salesRepository.saveAndFlush(sales);
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

        return new FinalyseSaleDTO(cashSale.getId(), true);
    }

    /*
    Sauvegarder l etat de la vente
     */
    @Override
    public ResponseDTO putCashSaleOnHold(CashSaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        User user = storageService.getUser();
        CashSale cashSale = cashSaleRepository.getReferenceById(dto.getId());
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
                paymentService.findAllBySalesId(sales.getId()).forEach(paymentService::delete);
                sales.getSalesLines().forEach(salesLineService::deleteSaleLine);
                salesRepository.delete(sales);
            });
    }

    @Override
    public void cancelCashSale(Long id) {
        User user = storageService.getUser();
        cashSaleRepository
            .findOneWithEagerSalesLines(id)
            .ifPresent(sales -> {
                CashSale copy = (CashSale) sales.clone();
                copySale(sales, copy);
                sales.setEffectiveUpdateDate(LocalDateTime.now());
                sales.setCanceled(true);
                copy.setCanceled(true);
                sales.setLastUserEdit(user);
                cashSaleRepository.save(sales);
                cashSaleRepository.save(copy);
                paymentService.findAllBySalesId(sales.getId()).forEach(payment -> paymentService.clonePayment(payment, copy));
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
        copy.setMontantttcUg(copy.getMontantttcUg() * (-1));
        copy.setHtAmountUg(copy.getHtAmountUg() * (-1));
        copy.setCostAmount(copy.getCostAmount() * (-1));
        copy.setNetAmount(copy.getNetAmount() * (-1));
        copy.setSalesAmount(copy.getSalesAmount() * (-1));
        copy.setHtAmount(copy.getHtAmount() * (-1));
        copy.setPayrollAmount(copy.getPayrollAmount() * (-1));
        copy.setMargeUg(copy.getMargeUg() * (-1));
        copy.setRestToPay(copy.getRestToPay() * (-1));
        copy.setCopy(true);
        copy.setDiscountAmount(copy.getDiscountAmount() * (-1));
        copy.setDiscountAmountUg(copy.getDiscountAmountUg() * (-1));
        copy.setDiscountAmountHorsUg(copy.getDiscountAmountHorsUg() * (-1));
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
        computeUgTvaAmountOnRemovingItem(c, saleLine);
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
            .findById(keyValue.key())
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
        CashSale sales = cashSaleRepository.getReferenceById(salesId);
        this.removeRemise(sales);
        this.cashSaleRepository.save(sales);
        this.displayNet(sales.getNetAmount());
    }
}
