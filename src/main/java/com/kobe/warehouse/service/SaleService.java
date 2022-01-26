package com.kobe.warehouse.service;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.web.rest.errors.StockException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;

@Service
@Transactional
public class SaleService {
    private final EntityManager em;
    private final SalesRepository salesRepository;
    private final ProduitRepository produitRepository;
    private final SalesLineRepository salesLineRepository;
    private final UserRepository userRepository;
    private final ReferenceService referenceService;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final ReportService reportService;
    private final UninsuredCustomerRepository uninsuredCustomerRepository;
    private final PaymentModeRepository paymentModeRepository;
    private final StorageService storageService;

    public SaleService(EntityManager em, SalesRepository salesRepository, ProduitRepository produitRepository, SalesLineRepository salesLineRepository, UserRepository userRepository, ReferenceService referenceService, InventoryTransactionRepository inventoryTransactionRepository, PaymentRepository paymentRepository, ReportService reportService, UninsuredCustomerRepository uninsuredCustomerRepository, PaymentModeRepository paymentModeRepository, StorageService storageService) {
        this.em = em;
        this.salesRepository = salesRepository;
        this.produitRepository = produitRepository;
        this.salesLineRepository = salesLineRepository;
        this.userRepository = userRepository;
        this.referenceService = referenceService;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.reportService = reportService;
        this.uninsuredCustomerRepository = uninsuredCustomerRepository;
        this.paymentModeRepository = paymentModeRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> customerPurchases(Long customerId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.INNER);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get("updatedAt")));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("customer").get("id"), customerId));
        predicates.add(cb.notEqual(root.get("statut"), SalesStatut.PENDING));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> customerPurchases(Long customerId, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.INNER);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get("updatedAt")));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("customer").get("id"), customerId));
        predicates.add(cb.notEqual(root.get("statut"), SalesStatut.PENDING));
        if (fromDate != null) {
            predicates.add(cb.between(cb.function("DATE", Date.class, root.get("updatedAt")),
                java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate)));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> customerPurchases(String query, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.INNER);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true).orderBy(cb.desc(root.get("updatedAt")));
        List<Predicate> predicates = new ArrayList<>();
        predicates(query, fromDate, toDate, predicates, cb, root);
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return q.getResultList().stream().map(e -> new SaleDTO(e)).collect(Collectors.toList());
    }

    private void predicates(String query, LocalDate fromDate, LocalDate toDate, List<Predicate> predicates, CriteriaBuilder cb, Root<Sales> root) {
        if (!StringUtils.isEmpty(query)) {
            query = "%" + query.toUpperCase() + "%";
            predicates.add(cb.or(cb.like(cb.upper(root.get("customer").get("firstName")), query),
                cb.like(cb.upper(root.get("customer").get("lastName")), query)));
        }

        predicates.add(cb.notEqual(root.get("statut"), SalesStatut.PENDING));
        if (fromDate != null) {
            predicates.add(cb.between(cb.function("DATE", Date.class, root.get("updatedAt")),
                java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate)));
        }
    }

    private long count(String query, LocalDate fromDate, LocalDate toDate) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Sales> root = cq.from(Sales.class);
        predicates(query, fromDate, toDate, predicates, cb, root);
        cq.select(cb.count(root));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Long> q = em.createQuery(cq);
        return q.getSingleResult();
    }

    @Transactional(readOnly = true)
    public SaleDTO purchaseBy(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Sales> cq = cb.createQuery(Sales.class);
        Root<Sales> root = cq.from(Sales.class);
        root.fetch("salesLines", JoinType.LEFT);
        root.fetch("payments", JoinType.LEFT);
        cq.select(root).distinct(true);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("id"), id));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        TypedQuery<Sales> q = em.createQuery(cq);
        return new SaleDTO(q.getSingleResult());
    }

    public SaleDTO createSale(SaleDTO dto) {
        return new SaleDTO(buildSales(dto));
    }

    private Sales buildSales(SaleDTO dto) {
        Sales sale = new Sales();
        sale.setCreatedAt(Instant.now());
        sale.setUpdatedAt(Instant.now());
        sale.setDiscountAmount(0);
        sale.setGrossAmount(0);
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
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(null);
    }

    private User getUserFormImport() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
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
            InventoryTransaction inventoryTransaction = inventoryTransactionRepository
                .buildInventoryTransaction(salesLine, user);
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
        sales.setCostAmount((sales.getCostAmount() - (oldQty * salesLine.getCostAmount()))
            + (salesLine.getQuantitySold() * salesLine.getCostAmount()));
        salesLineRepository.save(salesLine);
        salesRepository.save(sales);
        return saleLine;

    }

    private SalesLine createSaleLineFromDTO(SaleLineDTO saleLine) {
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
        salesLine.setGrossAmount(0);
        salesLine.setTaxAmount(0);
        salesLine.setDiscountUnitPrice(0);
        return salesLine;
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
        salesLine.setGrossAmount(0);
        salesLine.setTaxAmount(0);
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
        payment.setRestToPay(0);
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

    @Transactional(readOnly = true)
    public String printInvoice(Long saleId) throws IOException {
        Optional<Sales> ptSale = salesRepository.findOneWithEagerSalesLines(saleId);
        Sales sales = ptSale.get();
        Map<String, Object> parameters = reportService.buildMagasinInfo();
        // reportService.buildCustomerInfo(parameters, sales.getCustomer());
        reportService.buildSaleInfo(parameters, sales);
        return reportService.buildReportToPDF(parameters, "warehouse_facture", sales.getSalesLines().stream().map(SaleLineDTO::new).collect(Collectors.toList()));

    }

    public void deleteSaleLineById(Long id) {
        SalesLine salesLine = salesLineRepository.getOne(id);
        Sales sales = salesLine.getSales();
        sales.removeSalesLine(salesLine);
        sales.setCostAmount(sales.getCostAmount() - salesLine.getCostAmount());
        sales.setSalesAmount(sales.getSalesAmount() - salesLine.getSalesAmount());
        sales.setUpdatedAt(Instant.now());
        salesRepository.save(sales);
        salesLineRepository.delete(salesLine);
    }

    public CashSale fromDTOOldCashSale(SaleDTO dto) {
        CashSale c = new CashSale();
        c.setAmountToBePaid(dto.getAmountToBePaid());
        c.setDateDimension(DateDimension(dto.getDateDimensionId()));
        c.setCopy(dto.getCopy());
        c.setAmountToBeTakenIntoAccount(dto.getAmountToBeTakenIntoAccount());
        c.setImported(true);
        c.setMarge(dto.getMarge());
        c.setCostAmount(dto.getCostAmount());
        c.setCreatedAt(dto.getCreatedAt());
        c.setUpdatedAt(dto.getUpdatedAt());
        c.setEffectiveUpdateDate(dto.getEffectiveUpdateDate());
        c.setDiscountAmount(dto.getDiscountAmount());
        c.setNetAmount(dto.getNetAmount());
        c.setGrossAmount(dto.getGrossAmount());
        c.setPayrollAmount(dto.getPayrollAmount());
        c.setSalesAmount(dto.getSalesAmount());
        c.setMargeUg(dto.getMargeUg());
        c.setToIgnore(dto.isToIgnore());
        c.setTicketNumber(dto.getTicketNumber());
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
        salesLine.setGrossAmount(dto.getGrossAmount());
        salesLine.setTaxAmount(dto.getTaxAmount());
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
        payment.setReelPaidAmount(dto.getReelPaidAmount());
        payment.setRestToPay(dto.getRestToPay());
        payment.setUser(s.getUser());
        PaymentMode paymentMode = paymentModeRepository.findOneByCode(dto.getPaymentCode()).orElse(paymentModeRepository.getOne(1l));
        payment.setPaymentMode(paymentMode);
        payment.setSales(s);
        payment.setDateDimension(s.getDateDimension());
        return payment;
    }
}
