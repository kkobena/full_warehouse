package com.kobe.warehouse.service;

import com.kobe.warehouse.Util;
import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.*;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.PaymentAmountException;
import com.kobe.warehouse.web.rest.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.web.rest.errors.StockException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
            uninsuredCustomerRepository.findOneByCode(dto.getCustomerNum()).ifPresent(e -> c.setCustomer(e));
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
            payment.setCustomer(cashSale.getCustomer());
        } else if (s instanceof ThirdPartySales) {
            ThirdPartySales t = (ThirdPartySales) s;
            payment.setCustomer(t.getCustomer());
        }
        payment.setNetAmount(dto.getNetAmount());
        payment.setPaidAmount(dto.getPaidAmount());
        payment.setUser(s.getUser());
        PaymentMode paymentMode = paymentModeRepository.findById(dto.getPaymentCode()).orElse(paymentModeRepository.getOne("CASH"));
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
        c.setCustomer(uninsuredCustomer);
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
                SalesLine OldSalesLine = (SalesLine) salesLine.clone();
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
                CashSale cashSale = (CashSale) salesLine.getSales();
                upddateCashSaleAmounts(cashSale, salesLine, OldSalesLine);
                this.cashSaleRepository.save(cashSale);
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

    private Set<Payment> buildPaymentFromFromPaymentDTO(CashSale sales, SaleDTO saleDTO) {
        User user = this.storageService.getUser();
        Set<Payment> payments = new HashSet<>();
        saleDTO.getPayments().forEach(paymentDTO -> {
            Payment payment = buildPaymentFromFromPaymentDTO(sales, paymentDTO, user, null);
            payment.setStatut(SalesStatut.PENDING);
            this.paymentRepository.save(payment);
            payments.add(payment);
        });
        return payments;
    }

    private Set<Payment> buildPaymentFromFromPaymentDTO(CashSale sales, SaleDTO saleDTO, Ticket ticket) {
        User user = this.storageService.getUser();
        Set<Payment> payments = new HashSet<>();
        saleDTO.getPayments().forEach(paymentDTO -> {
            Payment payment = buildPaymentFromFromPaymentDTO(sales, paymentDTO, user, ticket);
            this.paymentRepository.save(payment);
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
        payment.setCustomer(sales.getCustomer());
        payment.setDateDimension(sales.getDateDimension());
        if (paymentDTO.getPaymentMode() != null) {
            PaymentMode paymentMode = this.paymentModeRepository.getOne(paymentDTO.getPaymentMode().getCode());
            ModePaimentCode modePaimentCode = ModePaimentCode.valueOf(paymentMode.getCode());
            switch (modePaimentCode) {
                case CASH:
                    payment.setMontantVerse(paymentDTO.getMontantVerse());
                    break;
            }
            payment.setPaymentMode(paymentMode);
        }

        payment.setNetAmount(sales.getAmountToBePaid());
        payment.setPaidAmount(paymentDTO.getPaidAmount());

        payment.setTicketCode(ticket.getCode());
        return payment;
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
        ticket.setRestToPay(saleDTO.getRestToPay());
        ticket.setMontantVerse(saleDTO.getMontantVerse());
        if (sales instanceof CashSale) {
            CashSale cashSale = (CashSale) sales;
            ticket.setCustomer(cashSale.getCustomer());
            ticket = ticketRepository.save(ticket);
            buildPaymentFromFromPaymentDTO(cashSale, saleDTO, ticket);
        } else {
            ThirdPartySales thirdPartySales = (ThirdPartySales) sales;
            ticket.setCustomer(thirdPartySales.getCustomer());
        }
        ticket.setTva(buildTvaData(sales.getSalesLines()));
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

    public ResponseDTO save(CashSaleDTO dto) throws PaymentAmountException, SaleNotFoundCustomerException {
        ResponseDTO response = new ResponseDTO();
        User user = this.storageService.getUser();
        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        CashSale p = cashSaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        p.getSalesLines().forEach(salesLine -> createInventory(salesLine, user, dateD));
        p.setStatut(SalesStatut.CLOSED);
        p.setStatutCaisse(SalesStatut.CLOSED);
        p.setDiffere(dto.isDiffere());
        if (!p.isDiffere() && dto.getPayrollAmount() < dto.getAmountToBePaid()) throw new PaymentAmountException();
        if (p.isDiffere() && p.getCustomer() == null) throw new SaleNotFoundCustomerException();
        p.setPayrollAmount(dto.getPayrollAmount());
        p.setRestToPay(dto.getRestToPay());
        p.setUpdatedAt(Instant.now());
        p.setEffectiveUpdateDate(p.getUpdatedAt());
        if (p.getRestToPay() == 0) {
            p.setPaymentStatus(PaymentStatus.PAYE);
        } else {
            p.setPaymentStatus(PaymentStatus.IMPAYE);
        }
        buildReference(p);
        Ticket ticket = buildTicket(dto, p);
        p.setTvaEmbeded(ticket.getTva());
        salesRepository.save(p);
        response.setMessage(ticket.getCode());
        response.setSuccess(true);
        response.setSize(p.getId().intValue());
        return response;
    }

    /*
    Sauvegarder l etat de la vente
     */
    public ResponseDTO putCashSaleOnHold(CashSaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        CashSale cashSale = cashSaleRepository.getOne(dto.getId());
        buildPaymentFromFromPaymentDTO(cashSale, dto);
        salesRepository.save(cashSale);
        response.setSuccess(true);
        return response;
    }

    private String buildTvaData(Set<SalesLine> salesLines) {
        if (salesLines != null && salesLines.size() > 0) {
            JSONArray array = new JSONArray();
            salesLines.stream().filter(saleLine -> saleLine.getTaxValue() > 0).collect(Collectors.groupingBy(SalesLine::getTaxValue)).forEach((k, v) -> {
                JSONObject json = new JSONObject();

                int totalTva = 0;
                for (SalesLine item : v) {
                    Double valeurTva = 1 + (Double.valueOf(k) / 100);
                    int htAmont = (int) Math.ceil(item.getSalesAmount() / valeurTva);
                    totalTva += (item.getSalesAmount() - htAmont);
                }
                try {
                    json.put("tva", k);
                    json.put("amount", totalTva);
                    array.put(json);
                } catch (JSONException e) {
                    log.debug("{}", e);
                }

            });
            if (array.length() > 0) return array.toString();
        }
        return null;
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
            String desc = String.format("Le prix de vente du produit %s %s a ??t?? modifi?? sur la vente %s prix usuel:  %d prix sur la vente %s", fournisseurProduitPrincipal != null ? fournisseurProduitPrincipal.getCodeCip() : "", p.getLibelle(), fournisseurProduitPrincipal != null ? fournisseurProduitPrincipal.getPrixUni() : null, salesLine.getRegularUnitPrice(), salesLine.getSales().getNumberTransaction());
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

    public void deleteSalePrevente(Long id) {
        this.salesRepository.findOneWithEagerSalesLines(id).ifPresent(sales -> {
            this.paymentRepository.findAllBySalesId(sales.getId()).forEach(payment -> {
                this.paymentRepository.delete(payment);
            });
            this.ticketRepository.findAllBySaleId(sales.getId()).forEach(ticket -> {
                this.ticketRepository.delete(ticket);
            });
            sales.getSalesLines().forEach(salesLine -> {
                this.salesLineRepository.delete(salesLine);
            });
            this.salesRepository.delete(sales);
        });
    }

    public void cancelCashSale(Long id) {
        this.cashSaleRepository.findOneWithEagerSalesLines(id).ifPresent(sales -> {
            CashSale copy = (CashSale) sales.clone();
            copySale(sales, copy);
            sales.setUpdatedAt(Instant.now());
            sales.setEffectiveUpdateDate(sales.getUpdatedAt());
            sales.setCanceled(true);
            this.cashSaleRepository.save(sales);
            this.cashSaleRepository.save(copy);
            List<Ticket> tickets = this.ticketRepository.findAllBySaleId(sales.getId());
            this.paymentRepository.findAllBySalesId(sales.getId()).forEach(payment -> {
                clonePayment(payment, tickets, copy);
            });

            sales.getSalesLines().forEach(salesLine -> {
                salesLine.setUpdatedAt(Instant.now());
                salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
                SalesLine salesLineCopy = cloneSalesLine(salesLine, copy);
                createInventoryAnnulation(salesLineCopy, storageService.getUser(), copy.getDateDimension());
            });
        });
    }

    private void copySale(Sales sales, Sales copy) {
        copy.setId(null);
        copy.setUpdatedAt(Instant.now());
        copy.setCreatedAt(copy.getUpdatedAt());
        copy.setEffectiveUpdateDate(copy.getUpdatedAt());
        buildReference(copy);
        copy.setCanceledSale(sales);
        copy.dateDimension(Constants.DateDimension(LocalDate.now()));
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
        copy.setStatut(SalesStatut.REMOVE);
        copy.setTaxAmount(copy.getTaxAmount() * (-1));
        copy.setUser(storageService.getUser());
        copy.setPayments(Collections.emptySet());
        copy.setTickets(Collections.emptySet());
        copy.setSalesLines(Collections.emptySet());
    }

    private void createInventoryAnnulation(SalesLine salesLine, User user, DateDimension dateD) {
        InventoryTransaction inventoryTransaction = inventoryTransactionRepository.buildInventoryTransaction(salesLine, TransactionType.CANCEL_SALE, user);
        Produit p = salesLine.getProduit();
        StockProduit stockProduit = this.stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
        int quantityAfter = quantityBefor - salesLine.getQuantityRequested();
        inventoryTransaction.setDateDimension(dateD);
        inventoryTransaction.setQuantityBefor(quantityBefor);
        inventoryTransaction.setQuantityAfter(quantityAfter);
        inventoryTransactionRepository.save(inventoryTransaction);
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
        stockProduit.setUpdatedAt(Instant.now());
        this.stockProduitRepository.save(stockProduit);
    }


    private SalesLine cloneSalesLine(SalesLine salesLine, Sales copy) {
        SalesLine salesLineCopy = (SalesLine) salesLine.clone();
        salesLineCopy.setId(null);
        salesLineCopy.setCreatedAt(Instant.now());
        salesLineCopy.setSales(copy);
        salesLineCopy.setUpdatedAt(salesLineCopy.getCreatedAt());
        salesLineCopy.setEffectiveUpdateDate(salesLineCopy.getUpdatedAt());
        salesLineCopy.setMontantTvaUg(salesLineCopy.getMontantTvaUg() * (-1));
        salesLineCopy.setSalesAmount(salesLineCopy.getSalesAmount() * (-1));
        salesLineCopy.setNetAmount(salesLineCopy.getNetAmount() * (-1));
        salesLineCopy.setQuantiyAvoir(salesLineCopy.getQuantiyAvoir() * (-1));
        salesLineCopy.setQuantitySold(salesLineCopy.getQuantitySold() * (-1));
        salesLineCopy.setQuantityUg(salesLineCopy.getQuantityUg() * (-1));
        salesLineCopy.setQuantityRequested(salesLineCopy.getQuantityRequested() * (-1));
        salesLineCopy.setDiscountAmountHorsUg(salesLineCopy.getDiscountAmountHorsUg() * (-1));
        salesLineCopy.setDiscountAmount(salesLineCopy.getDiscountAmount() * (-1));
        salesLineCopy.setDiscountAmountUg(salesLineCopy.getDiscountAmountUg() * (-1));
        salesLineCopy.setAmountToBeTakenIntoAccount(salesLineCopy.getAmountToBeTakenIntoAccount() * (-1));
        this.salesLineRepository.save(salesLineCopy);
        this.salesLineRepository.save(salesLine);
        return salesLineCopy;

    }

    private Ticket cloneTicket(Ticket ticket, Sales copy) {
        Ticket ticketCopy = (Ticket) ticket.clone();
        ticketCopy.setCode(RandomStringUtils.randomNumeric(8));
        ticketCopy.setSale(copy);
        ticketCopy.setCreated(copy.getEffectiveUpdateDate());
        ticketCopy.setMontantVerse(ticketCopy.getMontantVerse() * (-1));
        ticketCopy.setRestToPay(ticketCopy.getRestToPay() * (-1));
        ticketCopy.setMontantAttendu(ticketCopy.getMontantAttendu() * (-1));
        ticketCopy.setMontantPaye(ticketCopy.getMontantPaye() * (-1));
        List<TvaEmbeded> tvaEmbededs = Util.transformTvaEmbeded(ticket.getTva());
        if (tvaEmbededs.size() > 0) {
            for (TvaEmbeded tva : tvaEmbededs) {
                tva.setAmount(tva.getAmount() * (-1));
            }
        }
        ticketCopy.setTva(Util.transformTvaEmbededToString(tvaEmbededs));
        ticketCopy.setCanceled(true);
        this.ticketRepository.save(ticketCopy);
        ticket.setCanceled(true);
        this.ticketRepository.save(ticket);
        return ticketCopy;
    }

    private void clonePayment(Payment payment, List<Ticket> tickets, Sales copy) {
        payment.setUpdatedAt(Instant.now());
        payment.setEffectiveUpdateDate(payment.getUpdatedAt());
        payment.setCanceled(true);
        Payment paymentCopy = (Payment) payment.clone();
        paymentCopy.setId(null);
        paymentCopy.setCanceled(true);
        paymentCopy.setCreatedAt(payment.getUpdatedAt());
        paymentCopy.setUser(copy.getUser());
        paymentCopy.setDateDimension(copy.getDateDimension());
        paymentCopy.setSales(copy);
        paymentCopy.setMontantVerse(paymentCopy.getMontantVerse() * (-1));
        paymentCopy.setNetAmount(paymentCopy.getNetAmount() * (-1));
        paymentCopy.setPaidAmount(paymentCopy.getPaidAmount() * (-1));
        String paymentTicket = payment.getTicketCode();
        if (paymentTicket != null) {
            for (Ticket ticket : tickets) {
                if (ticket.getCode().equals(paymentTicket)) {
                    Ticket ticketCopy = cloneTicket(ticket, copy);
                    paymentCopy.setTicketCode(ticketCopy.getCode());
                    break;
                }
            }
        }
        paymentCopy.setStatut(SalesStatut.REMOVE);
        this.paymentRepository.save(paymentCopy);
        this.paymentRepository.save(payment);
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


    public void venteTest() {
        Customer customer = this.uninsuredCustomerRepository.findAll().get(0);
        CashSale cashSale = this.cashSaleRepository.getOne(143L);
        cashSale.setCustomer(customer);
        this.produitRepository.findAll(PageRequest.of(0, 500))
            .forEach(produit -> {
                SaleLineDTO dto = new SaleLineDTO();
                dto.setProduitId(produit.getId());
                dto.setSaleId(cashSale.getId());
                dto.setRegularUnitPrice(produit.getRegularUnitPrice());
                dto.setQuantityRequested(1);
                dto.setQuantitySold(1);
                dto.setQuantityUg(0);
                SalesLine salesLine = createSaleLineFromDTO(dto);
                salesLine.setSales(cashSale);
                this.salesLineRepository.save(salesLine);
                cashSale.setUpdatedAt(Instant.now());
                this.salesRepository.save(cashSale);
            });

        ;

    }
}
