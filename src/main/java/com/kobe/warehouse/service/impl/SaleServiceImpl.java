package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.CashRegisterService;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.SaleService;
import com.kobe.warehouse.service.SalesLineService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.TicketService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.web.rest.errors.CashRegisterException;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.PaymentAmountException;
import com.kobe.warehouse.web.rest.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.web.rest.errors.StockException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private final CashRegisterService cashRegisterService;
  private final SalesLineService salesLineService;
  private final PaymentService paymentService;
  private final TicketService ticketService;

  public SaleServiceImpl(
      SalesRepository salesRepository,
      UserRepository userRepository,
      UninsuredCustomerRepository uninsuredCustomerRepository,
      PaymentModeRepository paymentModeRepository,
      StorageService storageService,
      CashSaleRepository cashSaleRepository,
      CashRegisterService cashRegisterService,
      SalesLineService salesLineService,
      PaymentService paymentService,
      TicketService ticketService,
      ReferenceService referenceService,
      WarehouseCalendarService warehouseCalendarService) {
    super(referenceService, warehouseCalendarService);
    this.salesRepository = salesRepository;
    this.userRepository = userRepository;
    this.uninsuredCustomerRepository = uninsuredCustomerRepository;
    this.paymentModeRepository = paymentModeRepository;
    this.storageService = storageService;
    this.cashSaleRepository = cashSaleRepository;
    this.cashRegisterService = cashRegisterService;
    this.salesLineService = salesLineService;
    this.paymentService = paymentService;
    this.ticketService = ticketService;
  }

  private User getUserFormImport() {
    Optional<User> user =
        SecurityUtils.getCurrentUserLogin().flatMap(login -> userRepository.findOneByLogin(login));
    return user.orElseGet(() -> userRepository.findOneByLogin(Constants.SYSTEM_ACCOUNT).get());
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
        (sales.getCostAmount() - (oldQty * salesLine.getCostAmount()))
            + (salesLine.getQuantitySold() * salesLine.getCostAmount()));
    salesRepository.save(sales);
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
      userRepository
          .findOneByLogin(dto.getUserFullName())
          .ifPresentOrElse(u -> c.setUser(u), () -> c.setUser(getUserFormImport()));
    } else {
      c.setUser(getUserFormImport());
    }
    if (StringUtils.isNotEmpty(dto.getSellerUserName())) {
      userRepository
          .findOneByLogin(dto.getSellerUserName())
          .ifPresentOrElse(u -> c.setSeller(u), () -> c.setSeller(getUserFormImport()));
    } else {
      c.setSeller(getUserFormImport());
    }
    if (StringUtils.isNotEmpty(dto.getCustomerNum())) {
      uninsuredCustomerRepository
          .findOneByCode(dto.getCustomerNum())
          .ifPresent(e -> c.setCustomer(e));
    }
    c.setMagasin(c.getUser().getMagasin());
    return c;
  }

  @Override
  public Payment buildPaymentFromDTO(PaymentDTO dto, Sales s) {
    Payment payment = new Payment();
    payment.setCreatedAt(dto.getCreatedAt());
    payment.setUpdatedAt(dto.getUpdatedAt());
    payment.setEffectiveUpdateDate(dto.getUpdatedAt());
    if (s instanceof CashSale cashSale) {
      payment.setCustomer(cashSale.getCustomer());
    } else if (s instanceof ThirdPartySales t) {
      payment.setCustomer(t.getCustomer());
    }
    payment.setNetAmount(dto.getNetAmount());
    payment.setPaidAmount(dto.getPaidAmount());
    payment.setUser(s.getUser());
    PaymentMode paymentMode =
        paymentModeRepository
            .findById(dto.getPaymentCode())
            .orElse(paymentModeRepository.getReferenceById("CASH"));
    payment.setPaymentMode(paymentMode);
    payment.setSales(s);
    return payment;
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
    CashSale c = new CashSale();
    c.setCustomer(uninsuredCustomer);
    c.setNatureVente(dto.getNatureVente());
    c.setTypePrescription(dto.getTypePrescription());
    User user = storageService.getUser();
    User caissier = user;
    if (user.getId().compareTo(dto.getCassierId()) != 0) {
      caissier = userRepository.getReferenceById(dto.getCassierId());
    }
    if (caissier.getId().compareTo(dto.getSellerId()) != 0) {
      c.setSeller(caissier);
    } else {
      c.setSeller(userRepository.getReferenceById(dto.getSellerId()));
    }
    c.setImported(false);
    c.setUser(user);
    c.setLastUserEdit(c.getUser());
    c.setCassier(caissier);
    c.setCopy(dto.getCopy());
    c.setCreatedAt(LocalDateTime.now());
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
    SalesLine saleLine =
        salesLineService.createSaleLineFromDTO(
            dto.getSalesLines().get(0),
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
    upddateCashSaleAmounts(c, saleLine);
    c.getSalesLines().add(saleLine);
    CashSale sale = salesRepository.saveAndFlush(c);
    saleLine.setSales(c);
    salesLineService.saveSalesLine(saleLine);
    return new CashSaleDTO(sale);
  }

  private void upddateCashSaleAmounts(CashSale c, SalesLine saleLine) {
    computeSaleEagerAmount(c, saleLine.getSalesAmount(), 0);
    processDiscountCashSale(c, saleLine, null);
    computeCashSaleAmountToPaid(c);
    computeSaleLazyAmount(c, saleLine, null);
    computeTvaAmount(c, saleLine, null);
    computeUgTvaAmount(c, saleLine, null);
  }

  private void upddateCashSaleAmounts(CashSale c, SalesLine saleLine, SalesLine oldSaleLine) {
    computeSaleEagerAmount(c, saleLine.getSalesAmount(), oldSaleLine.getSalesAmount());
    processDiscountCashSale(c, saleLine, oldSaleLine);
    computeCashSaleAmountToPaid(c);
    computeSaleLazyAmount(c, saleLine, oldSaleLine);
    computeTvaAmount(c, saleLine, oldSaleLine);
    computeUgTvaAmount(c, saleLine, oldSaleLine);
  }

  @Override
  public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO)
      throws StockException, DeconditionnementStockOut {
    SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
    SalesLine OldSalesLine = (SalesLine) salesLine.clone();
    salesLineService.updateItemQuantityRequested(
        saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
    Sales sales = salesLine.getSales();
    upddateCashSaleAmounts((CashSale) sales, salesLine, OldSalesLine);
    salesRepository.saveAndFlush(sales);
    return new SaleLineDTO(salesLine);
  }

  @Override
  public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
    SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
    salesLineService.updateItemQuantitySold(
        salesLine, saleLineDTO, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
    Sales sales = salesLine.getSales();
    salesRepository.saveAndFlush(sales);
    return new SaleLineDTO(salesLine);
  }

  @Override
  public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) {
    SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
    SalesLine OldSalesLine = (SalesLine) salesLine.clone();
    salesLineService.updateItemRegularPrice(
        saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
    Sales sales = salesLine.getSales();
    upddateCashSaleAmounts((CashSale) sales, salesLine, OldSalesLine);
    salesRepository.saveAndFlush(sales);
    return new SaleLineDTO(salesLine);
  }

  @Override
  public SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto) {
    return new SaleLineDTO(createOrUpdateSaleLine(dto));
  }

  private SalesLine createOrUpdateSaleLine(SaleLineDTO dto) {
    Optional<SalesLine> salesLineOp =
        salesLineService.findBySalesIdAndProduitId(dto.getSaleId(), dto.getProduitId());
    Long storageId = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
    if (salesLineOp.isPresent()) {
      SalesLine salesLine = salesLineOp.get();
      SalesLine OldSalesLine = (SalesLine) salesLine.clone();
      salesLineService.updateSaleLine(dto, salesLine, storageId);
      CashSale cashSale = (CashSale) salesLine.getSales();
      upddateCashSaleAmounts(cashSale, salesLine, OldSalesLine);
      cashSaleRepository.save(cashSale);
      return salesLine;
    }
    SalesLine salesLine =
        salesLineService.create(
            dto, storageId, cashSaleRepository.getReferenceById(dto.getSaleId()));
    updateSaleWhenAddItem(dto, salesLine);
    return salesLine;
  }

  private void updateSaleWhenAddItem(SaleLineDTO dto, SalesLine salesLine) {
    CashSale sales = cashSaleRepository.getReferenceById(dto.getSaleId());
    upddateCashSaleAmounts(sales, salesLine);
    salesLine.setSales(sales);
    salesRepository.saveAndFlush(sales);
  }

  @Override
  public ResponseDTO save(CashSaleDTO dto)
      throws PaymentAmountException, SaleNotFoundCustomerException, CashRegisterException {
    User user = storageService.getUser();
    cashRegisterService.checkIfCashRegisterIsOpen(user, storageService.getSystemeUser());
    ResponseDTO response = new ResponseDTO();
    Long id = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
    CashSale p = cashSaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
    UninsuredCustomer uninsuredCustomer = getUninsuredCustomerById(dto.getCustomerId());
    if (Objects.nonNull(uninsuredCustomer)) {
      p.setCustomer(uninsuredCustomer);
    }
    salesLineService.save(p.getSalesLines(), user, id);
    p.setStatut(SalesStatut.CLOSED);
    p.setStatutCaisse(SalesStatut.CLOSED);
    p.setDiffere(dto.isDiffere());
    p.setLastUserEdit(storageService.getUser());
    if (!p.isDiffere() && dto.getPayrollAmount() < dto.getAmountToBePaid())
      throw new PaymentAmountException();
    if (p.isDiffere() && p.getCustomer() == null) throw new SaleNotFoundCustomerException();
    p.setPayrollAmount(dto.getPayrollAmount());

    p.setRestToPay(dto.getRestToPay());
    p.setUpdatedAt(LocalDateTime.now());
    p.setMonnaie(dto.getMontantRendu());
    p.setEffectiveUpdateDate(p.getUpdatedAt());
    if (p.getRestToPay() == 0) {
      p.setPaymentStatus(PaymentStatus.PAYE);
    } else {
      p.setPaymentStatus(PaymentStatus.IMPAYE);
    }
    buildReference(p);
    Ticket ticket = ticketService.buildTicket(dto, p, user, buildTvaData(p.getSalesLines()));
    paymentService.buildPaymentFromFromPaymentDTO(p, dto, ticket, user);
    p.setTvaEmbeded(ticket.getTva());
    salesRepository.save(p);
    response.setMessage(ticket.getCode());
    response.setSuccess(true);
    response.setSize(p.getId().intValue());
    initCalendar();
    return response;
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
  }

  @Override
  public void deleteSalePrevente(Long id) {
    salesRepository
        .findOneWithEagerSalesLines(id)
        .ifPresent(
            sales -> {
              paymentService
                  .findAllBySalesId(sales.getId())
                  .forEach(
                      payment -> {
                        paymentService.delete(payment);
                      });
              ticketService
                  .findAllBySaleId(sales.getId())
                  .forEach(
                      ticket -> {
                        ticketService.delete(ticket);
                      });
              sales
                  .getSalesLines()
                  .forEach(
                      salesLine -> {
                        salesLineService.deleteSaleLine(salesLine);
                      });
              salesRepository.delete(sales);
            });
  }

  @Override
  public void cancelCashSale(Long id) {
    User user = storageService.getUser();
    cashSaleRepository
        .findOneWithEagerSalesLines(id)
        .ifPresent(
            sales -> {
              CashSale copy = (CashSale) sales.clone();
              copySale(sales, copy);
              sales.setEffectiveUpdateDate(LocalDateTime.now());
              sales.setCanceled(true);
              copy.setCanceled(true);
              sales.setLastUserEdit(user);
              cashSaleRepository.save(sales);
              cashSaleRepository.save(copy);
              List<Ticket> tickets = ticketService.findAllBySaleId(sales.getId());
              paymentService
                  .findAllBySalesId(sales.getId())
                  .forEach(payment -> paymentService.clonePayment(payment, tickets, copy));
              salesLineService.cloneSalesLine(
                  sales.getSalesLines(),
                  copy,
                  user,
                  storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
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
    copy.setTickets(Collections.emptySet());
    copy.setSalesLines(Collections.emptySet());
  }

  private void upddateCashSaleAmountsOnRemovingItem(CashSale c, SalesLine saleLine) {
    computeSaleEagerAmountOnRemovingItem(c, saleLine);
    processDiscountSaleOnRemovingItem(c, saleLine);
    computeCashSaleAmountToPaid(c);
    computeSaleLazyAmountOnRemovingItem(c, saleLine);
    computeUgTvaAmountOnRemovingItem(c, saleLine);
    computeTvaAmountOnRemovingItem(c, saleLine);
  }
}
