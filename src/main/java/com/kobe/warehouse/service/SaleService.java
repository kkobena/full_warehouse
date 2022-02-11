package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.*;
import com.kobe.warehouse.web.rest.SalesResource;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.StockException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class SaleService {
    private final Logger log = LoggerFactory.getLogger(SaleService.class);
    private final SalesRepository salesRepository;
    private final ProduitRepository produitRepository;
    private final SalesLineRepository salesLineRepository;
    private final UserRepository userRepository;
    private final ReferenceService referenceService;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final UninsuredCustomerRepository uninsuredCustomerRepository;
    private final PaymentModeRepository paymentModeRepository;
    private final StorageService storageService;
    private final StockProduitRepository stockProduitRepository;
    private final AppConfigurationService appConfigurationService;
    private final TicketRepository ticketRepository;
    private final CashSaleRepository cashSaleRepository;
    private final LogsService logsService;

    public SaleService(SalesRepository salesRepository, ProduitRepository produitRepository, SalesLineRepository salesLineRepository, UserRepository userRepository, ReferenceService referenceService, InventoryTransactionRepository inventoryTransactionRepository, PaymentRepository paymentRepository, UninsuredCustomerRepository uninsuredCustomerRepository, PaymentModeRepository paymentModeRepository, StorageService storageService, StockProduitRepository stockProduitRepository, AppConfigurationService appConfigurationService, TicketRepository ticketRepository, CashSaleRepository cashSaleRepository, LogsService logsService) {
        this.salesRepository = salesRepository;
        this.produitRepository = produitRepository;
        this.salesLineRepository = salesLineRepository;
        this.userRepository = userRepository;
        this.referenceService = referenceService;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
        this.paymentModeRepository = paymentModeRepository;
        this.storageService = storageService;
        this.stockProduitRepository = stockProduitRepository;
        this.appConfigurationService = appConfigurationService;
        this.ticketRepository = ticketRepository;
        this.cashSaleRepository = cashSaleRepository;
        this.logsService = logsService;
    }

    public SaleDTO createSale(SaleDTO dto) {
        return new SaleDTO(buildSales(dto));
    }

    private Sales buildSales(SaleDTO dto) {
        Sales sale = new Sales();
        sale.setCreatedAt(Instant.now());
        sale.setUpdatedAt(Instant.now());
        sale.setDiscountAmount(0);

        sale.setTaxAmount(0);
        sale.setNetAmount(0);
        sale.setDateDimension(Constants.DateDimension(LocalDate.now()));
        sale.setUser(getUser());
        // sale.setCustomer(fromId(dto.getCustomerId()));
        sale.setNumberTransaction(referenceService.buildNumSale());
        sale.setStatut(SalesStatut.PENDING);
        List<SalesLine> listSaleLine = createLineFromDTO(dto.getSalesLines(), sale);
        sale = salesRepository.save(sale);
        salesLineRepository.saveAll(listSaleLine);
        return sale;
    }

    private List<SalesLine> createLineFromDTO(List<SaleLineDTO> saleLines, Sales sales) {
        List<SalesLine> saleItem = new ArrayList<>();
        int costAmount = 0;
        int saleAmount = 0;
        for (SaleLineDTO saleLine : saleLines) {
            SalesLine salesLine = createSaleLineFromDTO(saleLine);
            Produit produit = salesLine.getProduit();
            saleItem.add(salesLine);
            costAmount += (saleLine.getQuantitySold() * produit.getCostAmount());
            saleAmount += salesLine.getSalesAmount();
            sales.addSalesLine(salesLine);
        }
        sales.setCostAmount(costAmount);
        sales.setSalesAmount(saleAmount);
        return saleItem;

    }

    private User getUser() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(null);
    }

    private User getUserFormImport() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(() -> userRepository.findOneByLogin(Constants.SYSTEM_ACCOUNT).get());
    }

    private Customer fromId(Long id) {
        Customer cust = new Customer();
        cust.setId(id);
        return cust;
    }

    private void createInventory(Set<SalesLine> listSaleLine) {
        User user = getUser();
        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        for (SalesLine salesLine : listSaleLine) {
            InventoryTransaction inventoryTransaction = inventoryTransactionRepository.buildInventoryTransaction(salesLine, user);
            Produit p = salesLine.getProduit();
            int quantityBefor = 0;// p.getQuantity();
            int quantityAfter = quantityBefor - salesLine.getQuantitySold();
            inventoryTransaction.setDateDimension(dateD);
            inventoryTransaction.setQuantityBefor(quantityBefor);
            inventoryTransaction.setQuantityAfter(quantityAfter);
            inventoryTransaction.setRegularUnitPrice(salesLine.getRegularUnitPrice());
            inventoryTransaction.setCostAmount(salesLine.getCostAmount());
            inventoryTransactionRepository.save(inventoryTransaction);
            // p.setQuantity(quantityAfter);
            produitRepository.save(p);
        }

    }

    public Sales createSaleLine(SaleLineDTO saleLine) throws StockException {
        Sales sale = salesRepository.getOne(saleLine.getSaleId());
        SalesLine salesLine;
        Optional<SalesLine> optionalSalesLine = salesLineRepository.findBySalesIdAndProduitId(saleLine.getSaleId(), saleLine.getProduitId());
        if (optionalSalesLine.isPresent()) {
            salesLine = optionalSalesLine.get();
            Produit produit = produitRepository.getOne(saleLine.getProduitId());
            if ((salesLine.getQuantitySold() + saleLine.getQuantitySold()) > 0/*produit.getQuantity()*/) {
                throw new StockException();
            } else {
                salesLine.setQuantitySold(salesLine.getQuantitySold() + saleLine.getQuantitySold());
                salesLine.setSalesAmount(salesLine.getQuantitySold() * saleLine.getRegularUnitPrice());
                sale.setCostAmount(sale.getCostAmount() + (salesLine.getQuantitySold() * produit.getCostAmount()));
                sale.setSalesAmount(sale.getSalesAmount() + salesLine.getSalesAmount());
            }
        } else {
            salesLine = createSaleLineFromDTO(saleLine, sale);
            sale.addSalesLine(salesLine);
        }
        salesLineRepository.save(salesLine);
        salesRepository.save(sale);
        return sale;
    }

    public SaleLineDTO updateSaleLine(SaleLineDTO saleLine) {
        SalesLine salesLine = salesLineRepository.getOne(saleLine.getId());
        int oldAmont = salesLine.getSalesAmount();
        int oldQty = salesLine.getQuantitySold();
        salesLine.setQuantitySold(saleLine.getQuantitySold());
        salesLine.setUpdatedAt(Instant.now());
        salesLine.setSalesAmount(saleLine.getQuantitySold() * saleLine.getRegularUnitPrice());
        salesLine.setRegularUnitPrice(saleLine.getRegularUnitPrice());
        Sales sales = salesLine.getSales();
        sales.setSalesAmount((sales.getSalesAmount() - oldAmont) + salesLine.getSalesAmount());
        sales.setCostAmount((sales.getCostAmount() - (oldQty * salesLine.getCostAmount())) + (salesLine.getQuantitySold() * salesLine.getCostAmount()));
        salesLineRepository.save(salesLine);
        salesRepository.save(sales);
        return saleLine;

    }


    private SalesLine createSaleLineFromDTO(SaleLineDTO saleLine, Sales sales) {
        Produit produit = produitRepository.getOne(saleLine.getProduitId());
        SalesLine salesLine = new SalesLine();
        salesLine.setCreatedAt(Instant.now());
        salesLine.setUpdatedAt(Instant.now());
        salesLine.costAmount(produit.getCostAmount());
        salesLine.setProduit(produit);
        salesLine.setNetAmount(0);
        salesLine.setSalesAmount(saleLine.getQuantitySold() * saleLine.getRegularUnitPrice());
        salesLine.setNetAmount(salesLine.getSalesAmount());
        salesLine.setNetUnitPrice(saleLine.getRegularUnitPrice());
        salesLine.setQuantitySold(saleLine.getQuantitySold());
        salesLine.setRegularUnitPrice(saleLine.getRegularUnitPrice());
        salesLine.setDiscountAmount(0);
        salesLine.setDiscountUnitPrice(0);
        sales.costAmount(sales.getCostAmount() + (saleLine.getQuantitySold() * produit.getCostAmount()));
        sales.setSalesAmount(sales.getSalesAmount() + salesLine.getSalesAmount());
        return salesLine;
    }

    private Payment buildPayment(Sales sale) {
        Payment payment = new Payment();
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        payment.setSales(sale);
        payment.setUser(getUser());
        //  payment.setCustomer(sale.getCustomer());
        payment.setDateDimension(sale.getDateDimension());
        payment.setNetAmount(sale.getSalesAmount());
        payment.setPaidAmount(sale.getSalesAmount());
        payment.setPaymentMode(Constants.getPaymentMode(Constants.MODE_ESP));
        return payment;
    }

    public SaleDTO save(SaleDTO dto) {
        Optional<Sales> ptSale = salesRepository.findOneWithEagerSalesLines(dto.getId());
        ptSale.ifPresent(p -> {
            createInventory(p.getSalesLines());
            paymentRepository.save(buildPayment(p));
            p.setStatut(SalesStatut.CLOSED);
            p.setUpdatedAt(Instant.now());
            salesRepository.save(p);
        });
        return dto;

    }


    public CashSale fromDTOOldCashSale(CashSaleDTO dto) {
        CashSale c = new CashSale();
        c.setAmountToBePaid(dto.getAmountToBePaid());
        c.setDateDimension(DateDimension(dto.getDateDimensionId()));
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
            userRepository.findOneByLogin(dto.getUserFullName()).ifPresentOrElse(u -> c.setUser(u), () -> c.setUser(getUserFormImport()));
        } else {
            c.setUser(getUserFormImport());
        }
        if (StringUtils.isNotEmpty(dto.getSellerUserName())) {
            userRepository.findOneByLogin(dto.getSellerUserName()).ifPresentOrElse(u -> c.setSeller(u), () -> c.setSeller(getUserFormImport()));
        } else {
            c.setSeller(getUserFormImport());
        }
        if (StringUtils.isNotEmpty(dto.getCustomerNum())) {
            uninsuredCustomerRepository.findOneByCode(dto.getCustomerNum()).ifPresent(e -> c.setUninsuredCustomer(e));
        }
        c.setMagasin(c.getUser().getMagasin());
        return c;
    }

    private DateDimension DateDimension(int dateKey) {
        DateDimension dateDimension = new DateDimension();
        dateDimension.setDateKey(dateKey);
        return dateDimension;

    }

    public SalesLine buildSaleLineFromDTO(SaleLineDTO dto) {
        Produit produit = produitRepository.findOneByLibelle(dto.getProduitLibelle().trim()).orElseThrow();
        SalesLine salesLine = new SalesLine();
        salesLine.setCreatedAt(dto.getCreatedAt());
        salesLine.setUpdatedAt(dto.getUpdatedAt());
        salesLine.costAmount(dto.getCostAmount());
        salesLine.setProduit(produit);
        salesLine.setNetAmount(dto.getNetAmount());
        salesLine.setSalesAmount(dto.getSalesAmount());
        salesLine.setNetAmount(dto.getSalesAmount());
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLine.setDiscountAmount(dto.getDiscountAmount());
        salesLine.setDiscountUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantitySold(dto.getQuantitySold());
        salesLine.setQuantityRequested(dto.getQuantityRequested());
        salesLine.setQuantiyAvoir(dto.getQuantiyAvoir());
        salesLine.setQuantityUg(dto.getQuantityUg());
        salesLine.setMontantTvaUg(dto.getMontantTvaUg());
        salesLine.setToIgnore(dto.isToIgnore());
        salesLine.setTaxValue(dto.getTaxValue());
        salesLine.setAmountToBeTakenIntoAccount(dto.getAmountToBeTakenIntoAccount());
        salesLine.setEffectiveUpdateDate(dto.getEffectiveUpdateDate());
        return salesLine;

    }

    public Payment buildPaymentFromDTO(PaymentDTO dto, Sales s) {
        Payment payment = new Payment();
        payment.setCreatedAt(dto.getCreatedAt());
        payment.setUpdatedAt(dto.getUpdatedAt());
        payment.setEffectiveUpdateDate(dto.getUpdatedAt());
        if (s instanceof CashSale) {
            CashSale cashSale = (CashSale) s;
            payment.setCustomer(cashSale.getUninsuredCustomer());
        } else if (s instanceof ThirdPartySales) {
            ThirdPartySales t = (ThirdPartySales) s;
            payment.setCustomer(t.getAssuredCustomer());
        }
        payment.setNetAmount(dto.getNetAmount());
        payment.setPaidAmount(dto.getPaidAmount());
        payment.setUser(s.getUser());
        PaymentMode paymentMode = paymentModeRepository.findOneByCode(dto.getPaymentCode()).orElse(paymentModeRepository.getOne(1l));
        payment.setPaymentMode(paymentMode);
        payment.setSales(s);
        payment.setDateDimension(s.getDateDimension());
        return payment;
    }


    private void computeCashSaleAmountToPaid(CashSale c) {
        c.setAmountToBePaid(c.getNetAmount());
        c.setRestToPay(c.getAmountToBePaid());
        c.setAmountToBeTakenIntoAccount(0);
    }

    private void computeCashSaleEagerAmount(CashSale c, SalesLine saleLine, int oldSalesAmount) {
        c.setSalesAmount((c.getSalesAmount() - oldSalesAmount) + saleLine.getSalesAmount());
    }

    private void computeCashSaleLazyAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine != null) {
            c.setCostAmount((c.getCostAmount() - (oldSaleLine.getQuantityRequested() * oldSaleLine.getCostAmount())) + (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
        } else {
            c.setCostAmount(c.getCostAmount() + (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
        }

    }

    private void processDiscountCashSale(CashSale c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine != null) {
            c.setDiscountAmount((c.getDiscountAmount() - oldSaleLine.getDiscountAmount()) + saleLine.getDiscountAmount());
            c.setDiscountAmountUg((c.getDiscountAmountUg() - oldSaleLine.getDiscountAmountUg()) + saleLine.getDiscountAmountUg());
            c.setDiscountAmountHorsUg((c.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg()) + saleLine.getDiscountAmountHorsUg());
            c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
        } else {
            c.setDiscountAmount(c.getDiscountAmount() + saleLine.getDiscountAmount());
            c.setDiscountAmountUg(c.getDiscountAmountUg() + saleLine.getDiscountAmountUg());
            c.setDiscountAmountHorsUg(c.getDiscountAmountHorsUg() + saleLine.getDiscountAmountHorsUg());
            c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
        }


    }

    private void computeUgTvaAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (saleLine.getQuantityUg().compareTo(0) == 0) return;
        if (oldSaleLine == null) {
            int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
            int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmountUg(c.getHtAmountUg() + htc);
                c.setMontantttcUg(c.getMontantttcUg() + htc);
            } else {
                Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(htc / valeurTva);
                int montantTva = htc - htAmont;
                c.setMontantTvaUg(c.getMontantTvaUg() + montantTva);
                c.setHtAmountUg(c.getHtAmountUg() + htAmont);
            }
            c.setMargeUg(c.getMargeUg() + (htc - costAmount));
            c.setNetUgAmount(c.getMontantttcUg() + ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice()) - saleLine.getDiscountAmountUg()));

        } else {
            int htcOld = oldSaleLine.getQuantityUg() * oldSaleLine.getRegularUnitPrice();
            int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
            int costAmountOld = oldSaleLine.getQuantityUg() * oldSaleLine.getCostAmount();
            int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmountUg((c.getHtAmountUg() - htcOld) + htc);
                c.setMontantttcUg((c.getMontantttcUg() - htcOld) + htc);
            } else {
                Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(htc / valeurTva);
                int montantTva = htc - htAmont;
                int htAmontOld = (int) Math.ceil(htcOld / valeurTva);
                int montantTvaOld = htcOld - htAmontOld;


                c.setMontantTvaUg((c.getMontantTvaUg() - montantTvaOld) + montantTva);
                c.setHtAmountUg((c.getHtAmountUg() - htAmontOld) + htAmont);
            }
            c.setMargeUg((c.getMargeUg() - (htcOld - costAmountOld)) + (htc - costAmount));
            c.setNetUgAmount((c.getMontantttcUg() - ((oldSaleLine.getQuantityUg() * oldSaleLine.getRegularUnitPrice()) - oldSaleLine.getDiscountAmountUg())) + ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice()) - saleLine.getDiscountAmountUg()));
        }

    }

    private void computeTvaAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine == null) {
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmount(c.getHtAmount() + saleLine.getSalesAmount());
            } else {
                Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
                int montantTva = saleLine.getSalesAmount() - htAmont;
                c.setTaxAmount(c.getTaxAmount() + montantTva);
                c.setHtAmount(c.getHtAmount() + htAmont);
            }
        } else {
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmount((c.getHtAmount() - oldSaleLine.getSalesAmount()) + saleLine.getSalesAmount());
            } else {
                Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
                int montantTva = saleLine.getSalesAmount() - htAmont;
                int htAmontOld = (int) Math.ceil(oldSaleLine.getSalesAmount() / valeurTva);
                int montantTvaOld = oldSaleLine.getSalesAmount() - htAmontOld;


                c.setTaxAmount((c.getTaxAmount() - montantTvaOld) + montantTva);
                c.setHtAmount((c.getHtAmount() - htAmontOld) + htAmont);
            }
        }


    }

    public CashSaleDTO createCashSale(CashSaleDTO dto) {
        DateDimension dateDimension = Constants.DateDimension(LocalDate.now());
        SalesLine saleLine = newSaleLine(dto.getSalesLines().get(0));
        UninsuredCustomer uninsuredCustomer = dto.getCustomer() != null ? uninsuredCustomerRepository.getOne(dto.getCustomer().getId()) : null;
        CashSale c = new CashSale();
        c.setDateDimension(dateDimension);
        c.setUninsuredCustomer(uninsuredCustomer);
        c.setNatureVente(dto.getNatureVente());
        c.setTypePrescription(dto.getTypePrescription());
        User user = this.storageService.getUser();
        User caissier = user;
        if (user.getId().compareTo(dto.getCassier().getId()) != 0) {
            caissier = this.userRepository.getOne(dto.getCassier().getId());
        }
        if (caissier.getId().compareTo(dto.getSeller().getId()) != 0) {
            c.setSeller(caissier);
        } else {
            c.setSeller(this.userRepository.getOne(dto.getSeller().getId()));
        }
        c.setImported(false);
        c.setUser(user);
        c.setCassier(caissier);
        c.getSalesLines().add(saleLine);
        c.setCopy(dto.getCopy());
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(c.getCreatedAt());
        c.setEffectiveUpdateDate(c.getUpdatedAt());
        c.setPayrollAmount(0);
        c.setToIgnore(dto.isToIgnore());
        c.setDiffere(dto.isDiffere());
        buildPreventeReference(c);
        c.setStatut(SalesStatut.ACTIVE);
        c.setStatutCaisse(SalesStatut.ACTIVE);
        c.setCaisseNum(dto.getCaisseNum());
        c.setCaisseEndNum(c.getCaisseNum());
        c.setPaymentStatus(PaymentStatus.IMPAYE);
        c.setMagasin(c.getCassier().getMagasin());
        upddateCashSaleAmounts(c, saleLine);
        CashSale sale = this.salesRepository.saveAndFlush(c);
        saleLine.setSales(c);

        this.salesLineRepository.saveAndFlush(saleLine);
        return new CashSaleDTO(sale);
    }

    private void upddateCashSaleAmounts(CashSale c, SalesLine saleLine) {
        computeCashSaleEagerAmount(c, saleLine, 0);
        processDiscountCashSale(c, saleLine, null);
        computeCashSaleAmountToPaid(c);
        computeCashSaleLazyAmount(c, saleLine, null);
        computeTvaAmount(c, saleLine, null);
        computeUgTvaAmount(c, saleLine, null);
    }

    private void upddateCashSaleAmounts(CashSale c, SalesLine saleLine, SalesLine oldSaleLine) {
        computeCashSaleEagerAmount(c, saleLine, oldSaleLine.getSalesAmount());
        processDiscountCashSale(c, saleLine, oldSaleLine);
        computeCashSaleAmountToPaid(c);
        computeCashSaleLazyAmount(c, saleLine, oldSaleLine);
        computeTvaAmount(c, saleLine, oldSaleLine);
        computeUgTvaAmount(c, saleLine, oldSaleLine);
    }

    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO) throws StockException, DeconditionnementStockOut {
        SalesLine salesLine = this.salesLineRepository.getOne(saleLineDTO.getId());
        StockProduit stockProduit = this.stockProduitRepository.findOneByProduitIdAndStockageId(saleLineDTO.getProduitId(), storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        int quantity = stockProduit.getQtyStock();
        if (saleLineDTO.getQuantityRequested() > quantity && !saleLineDTO.isForceStock()) {
            if (salesLine.getProduit().getParent() == null) {
                throw new StockException();
            } else {
                throw new DeconditionnementStockOut(salesLine.getProduit().getParent().getId().toString());
            }
        }
        SalesLine OldSalesLine = (SalesLine) salesLine.clone();
        salesLine.setQuantityRequested(saleLineDTO.getQuantityRequested());
        salesLine.setQuantitySold(salesLine.getQuantityRequested());
        salesLine.setUpdatedAt(Instant.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
        processUg(salesLine, saleLineDTO);
        processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
        Sales sales = salesLine.getSales();
        if (sales instanceof CashSale) {
            upddateCashSaleAmounts((CashSale) sales, salesLine, OldSalesLine);
        }
        this.salesRepository.saveAndFlush(sales);
        salesLine = this.salesLineRepository.saveAndFlush(salesLine);
        return new SaleLineDTO(salesLine);
    }

    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = this.salesLineRepository.getOne(saleLineDTO.getId());
        salesLine.setQuantitySold(saleLineDTO.getQuantitySold());
        salesLine.setUpdatedAt(Instant.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        processUg(salesLine, saleLineDTO);
        processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
        Sales sales = salesLine.getSales();
        if (sales instanceof CashSale) {
            upddateCashSaleAmounts((CashSale) sales, salesLine);
        }
        salesLine.setQuantiyAvoir(salesLine.getQuantityRequested() - salesLine.getQuantitySold());
        this.salesRepository.saveAndFlush(sales);
        salesLine = this.salesLineRepository.saveAndFlush(salesLine);
        return new SaleLineDTO(salesLine);
    }

    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = this.salesLineRepository.getOne(saleLineDTO.getId());
        SalesLine OldSalesLine = (SalesLine) salesLine.clone();
        salesLine.setUpdatedAt(Instant.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setRegularUnitPrice(saleLineDTO.getRegularUnitPrice());
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
        processUg(salesLine, saleLineDTO);
        processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
        Sales sales = salesLine.getSales();
        if (sales instanceof CashSale) {
            upddateCashSaleAmounts((CashSale) sales, salesLine, OldSalesLine);

        }
        this.salesRepository.saveAndFlush(sales);
        salesLine = this.salesLineRepository.saveAndFlush(salesLine);
        return new SaleLineDTO(salesLine);
    }

    private SalesLine newSaleLine(SaleLineDTO dto) {
        return createSaleLineFromDTO(dto);

    }


    private void processProductDiscount(RemiseProduit remiseProduit, SalesLine salesLine) {
        if (remiseProduit == null) {
            salesLine.setNetAmount(salesLine.getSalesAmount());
        } else if (remiseProduit.isEnable() && remiseProduit.getPeriod().isAfter(LocalDateTime.now())) {
            int discount = (int) Math.ceil(salesLine.getSalesAmount() * remiseProduit.getRemiseValue());// getRemiseValue set /100 a la creation;
            salesLine.setDiscountAmount(discount);
            salesLine.setNetAmount(salesLine.getSalesAmount() - discount);
            salesLine.setDiscountAmountHorsUg(discount);
            if (salesLine.getQuantityUg() > 0) {
                int discountHUg = (int) Math.ceil(((salesLine.getQuantityRequested() - salesLine.getQuantityUg()) * salesLine.getRegularUnitPrice()) * remiseProduit.getRemiseValue());
                salesLine.setDiscountAmountHorsUg(discountHUg);
                discountHUg = (int) Math.ceil((salesLine.getQuantityUg() * salesLine.getRegularUnitPrice()) * remiseProduit.getRemiseValue());
                salesLine.setDiscountAmountUg(discountHUg);
            }
        }

    }

    private void processUg(SalesLine salesLine, SaleLineDTO dto) {
        StockProduit stockProduit = this.stockProduitRepository.findOneByProduitIdAndStockageId(dto.getProduitId(), storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        if (stockProduit.getQtyUG() > 0) {
            if (salesLine.getQuantitySold() >= stockProduit.getQtyUG()) {
                salesLine.setQuantityUg(stockProduit.getQtyUG());
            } else {
                salesLine.setQuantityUg(dto.getQuantitySold());
            }

        }
    }

    public SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto) {
        return new SaleLineDTO(createOrUpdateSaleLine(dto));
    }

    public SalesLine createOrUpdateSaleLine(SaleLineDTO dto) {
        if (dto.getSaleId() != null) {
            Optional<SalesLine> salesLineOp = this.salesLineRepository.findBySalesIdAndProduitId(dto.getSaleId(), dto.getProduitId());
            if (salesLineOp.isPresent()) {
                SalesLine salesLine = salesLineOp.get();
                salesLine.setUpdatedAt(Instant.now());
                salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
                salesLine.setSalesAmount((salesLine.getQuantityRequested() + dto.getQuantityRequested()) * dto.getRegularUnitPrice());
                salesLine.setNetAmount(salesLine.getSalesAmount());
                salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
                salesLine.setQuantitySold(salesLine.getQuantitySold() + dto.getQuantitySold());
                salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
                salesLine.setQuantityRequested(salesLine.getQuantityRequested() + dto.getQuantityRequested());
                processUg(salesLine, dto);
                processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
                salesLine = this.salesLineRepository.save(salesLine);
                return salesLine;
            }
            return this.salesLineRepository.save(createSaleLineFromDTO(dto));
        } else {
            return createSaleLineFromDTO(dto);
        }

    }

    private void updateSaleWhenAddItem(SaleLineDTO dto, SalesLine salesLine) {
        CashSale sales = this.cashSaleRepository.getOne(dto.getSaleId());
        upddateCashSaleAmounts(sales, salesLine);
        salesLine.setSales(sales);
        this.salesRepository.saveAndFlush(sales);
    }

    private SalesLine createSaleLineFromDTO(SaleLineDTO dto) {
        Produit produit = produitRepository.getOne(dto.getProduitId());
        Tva tva = produit.getTva();
        SalesLine salesLine = new SalesLine();
        salesLine.setTaxValue(tva.getTaux());
        salesLine.setCreatedAt(Instant.now());
        salesLine.setUpdatedAt(Instant.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.costAmount(produit.getCostAmount());
        salesLine.setProduit(produit);
        salesLine.setSalesAmount(dto.getQuantityRequested() * dto.getRegularUnitPrice());
        salesLine.setNetAmount(salesLine.getSalesAmount());
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantitySold(dto.getQuantitySold());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantityRequested(dto.getQuantityRequested());
        salesLine.setDiscountAmount(0);
        salesLine.setDiscountUnitPrice(0);
        processUg(salesLine, dto);
        processProductDiscount(produit.getRemise(), salesLine);
        if (dto.getSaleId() != null) {
            updateSaleWhenAddItem(dto, salesLine);

        }
        return salesLine;
    }

    private Set<Payment> buildPaymentFromFromPaymentDTO(CashSale sales, SaleDTO saleDTO, Ticket ticket) {
        User user = this.storageService.getUser();
        Set<Payment> payments = new HashSet<>();
        saleDTO.getPayments().forEach(paymentDTO -> {
            Payment payment = buildPaymentFromFromPaymentDTO(sales, paymentDTO, user, ticket);
            payments.add(payment);
        });
        return payments;
    }

    private Payment buildPaymentFromFromPaymentDTO(CashSale sales, PaymentDTO paymentDTO, User user, Ticket ticket) {
        Payment payment = new Payment();
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(payment.getCreatedAt());
        payment.setEffectiveUpdateDate(payment.getCreatedAt());
        payment.setSales(sales);
        payment.setUser(user);
        payment.setCustomer(sales.getUninsuredCustomer());
        payment.setDateDimension(sales.getDateDimension());
        ModePaimentCode modePaimentCode = ModePaimentCode.valueOf(paymentDTO.getPaymentCode());
        switch (modePaimentCode) {
            case CB:
                payment.setMontantVerse(paymentDTO.getMontantVerse());
                break;
        }
        payment.setNetAmount(sales.getAmountToBePaid());
        payment.setPaidAmount(paymentDTO.getPaidAmount());
        payment.setPaymentMode(newPaymentMode(modePaimentCode));
        payment.setTicket(ticket);
        return this.paymentRepository.save(payment);
    }

    private PaymentMode newPaymentMode(ModePaimentCode modePaimentCode) {
        PaymentMode paymentMode = new PaymentMode();
        paymentMode.setCode(modePaimentCode.name());
        return paymentMode;

    }

    private Ticket buildTicket(SaleDTO saleDTO, Sales sales) {
        Ticket ticket = new Ticket();
        ticket.setCode(RandomStringUtils.randomNumeric(8));
        ticket.setCreated(Instant.now());
        ticket.setUser(this.storageService.getUser());
        ticket.setSale(sales);
        ticket.setMontantAttendu(sales.getAmountToBePaid());
        ticket.setMontantPaye(saleDTO.getPayrollAmount());
        ticket.setMontantRendu(saleDTO.getMontantRendue());
        if (sales instanceof CashSale) {
            CashSale cashSale = (CashSale) sales;
            ticket.setCustomer(cashSale.getUninsuredCustomer());
            ticket = ticketRepository.save(ticket);
            buildPaymentFromFromPaymentDTO(cashSale, saleDTO, ticket);
        } else {
            ThirdPartySales thirdPartySales = (ThirdPartySales) sales;
            ticket.setCustomer(thirdPartySales.getAssuredCustomer());
        }
        return ticket;

    }

    private void buildReference(Sales sales) {
        String ref = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat(referenceService.buildNumSale());
        sales.setNumberTransaction(ref);
    }

    private void buildPreventeReference(Sales sales) {
        String ref = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat(referenceService.buildNumPreventeSale());
        sales.setNumberTransaction(ref);

    }

    public ResponseDTO save(CashSaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        User user = this.storageService.getUser();
        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        Optional<CashSale> ptSale = cashSaleRepository.findOneWithEagerSalesLines(dto.getId());
        ptSale.ifPresent(p -> {
            p.getSalesLines().forEach(salesLine -> createInventory(salesLine, user, dateD));
            p.setStatut(SalesStatut.CLOSED);
            p.setUpdatedAt(Instant.now());
            if (p.getRestToPay() == 0) {
                p.setPaymentStatus(PaymentStatus.PAYE);
            } else {
                p.setPaymentStatus(PaymentStatus.IMPAYE);
            }
            buildReference(p);
            Ticket ticket = buildTicket(dto, p);
            salesRepository.save(p);
            response.setMessage(ticket.getCode());

        });
        return response;
    }

    private void createInventory(SalesLine salesLine, User user, DateDimension dateD) {
        InventoryTransaction inventoryTransaction = inventoryTransactionRepository.buildInventoryTransaction(salesLine, user);
        Produit p = salesLine.getProduit();
        StockProduit stockProduit = this.stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
        int quantityAfter = quantityBefor - salesLine.getQuantityRequested();
        inventoryTransaction.setDateDimension(dateD);
        inventoryTransaction.setQuantityBefor(quantityBefor);
        inventoryTransaction.setQuantityAfter(quantityAfter);
        inventoryTransactionRepository.save(inventoryTransaction);
        if (quantityBefor < salesLine.getQuantityRequested()) {
            this.logsService.create(TransactionType.FORCE_STOCK, TransactionType.FORCE_STOCK.getValue(), salesLine.getId().toString());
        }
        FournisseurProduit fournisseurProduitPrincipal = p.getFournisseurProduitPrincipal();
        if (fournisseurProduitPrincipal != null && fournisseurProduitPrincipal.getPrixUni() < salesLine.getRegularUnitPrice()) {
            String desc = String.format("Le prix de vente du produit %s %s a été modifié sur la vente %s prix usuel:  %d prix sur la vente %d", fournisseurProduitPrincipal != null ? fournisseurProduitPrincipal.getCodeCip() : "", p.getLibelle(), fournisseurProduitPrincipal != null ? fournisseurProduitPrincipal.getPrixUni() : null, salesLine.getRegularUnitPrice(), salesLine.getSales().getNumberTransaction());
            this.logsService.create(TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE, desc, salesLine.getId().toString());
        }
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
        stockProduit.setUpdatedAt(Instant.now());
        this.stockProduitRepository.save(stockProduit);
    }


    public void deleteSaleLineById(Long id) {
        SalesLine salesLine = salesLineRepository.getOne(id);
        CashSale sales = (CashSale) salesLine.getSales();
        sales.removeSalesLine(salesLine);
        upddateCashSaleAmountsOnRemovingItem(sales, salesLine);
        sales.setUpdatedAt(Instant.now());
        sales.setEffectiveUpdateDate(sales.getUpdatedAt());
        this.cashSaleRepository.save(sales);
        salesLineRepository.delete(salesLine);
    }

    private void upddateCashSaleAmountsOnRemovingItem(CashSale c, SalesLine saleLine) {
        computeCashSaleEagerAmountOnRemovingItem(c, saleLine);
        processDiscountCashSaleOnRemovingItem(c, saleLine);
        computeCashSaleAmountToPaid(c);
        computeCashSaleLazyAmountOnRemovingItem(c, saleLine);
        computeUgTvaAmountOnRemovingItem(c, saleLine);
        computeTvaAmountOnRemovingItem(c, saleLine);
    }

    private void computeCashSaleEagerAmountOnRemovingItem(CashSale c, SalesLine saleLine) {
        c.setSalesAmount(c.getSalesAmount() - saleLine.getSalesAmount());
    }

    private void computeCashSaleLazyAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        c.setCostAmount(c.getCostAmount() - (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
    }

    private void processDiscountCashSaleOnRemovingItem(CashSale c, SalesLine saleLine) {
        c.setDiscountAmount(c.getDiscountAmount() - saleLine.getDiscountAmount());
        c.setDiscountAmountUg(c.getDiscountAmountUg() - saleLine.getDiscountAmountUg());
        c.setDiscountAmountHorsUg(c.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg());
        c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
    }

    private void computeUgTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        if (saleLine.getQuantityUg().compareTo(0) == 0) return;
        int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
        int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
        if (saleLine.getTaxValue().compareTo(0) == 0) {
            c.setHtAmountUg(c.getHtAmountUg() - htc);
            c.setMontantttcUg(c.getMontantttcUg() - htc);
        } else {
            Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
            int htAmont = (int) Math.ceil(htc / valeurTva);
            int montantTva = htc - htAmont;
            c.setMontantTvaUg(c.getMontantTvaUg() - montantTva);
            c.setHtAmountUg(c.getHtAmountUg() - htAmont);
        }
        c.setMargeUg(c.getMargeUg() - (htc - costAmount));
        c.setNetUgAmount(c.getMontantttcUg() - ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice()) - saleLine.getDiscountAmountUg()));


    }

    private void computeTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        if (saleLine.getTaxValue().compareTo(0) == 0) {
            c.setHtAmount(c.getHtAmount() - saleLine.getSalesAmount());
        } else {
            Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
            int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
            int montantTva = saleLine.getSalesAmount() - htAmont;
            c.setTaxAmount(c.getTaxAmount() - montantTva);
            c.setHtAmount(c.getHtAmount() - htAmont);
        }
    }

}
