package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.TicketService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.Consommation;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.errors.ThirdPartySalesTiersPayantException;
import com.kobe.warehouse.service.sale.AvoirService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ThirdPartySaleServiceImpl extends SaleCommonService implements ThirdPartySaleService {

    private final Logger log = LoggerFactory.getLogger(ThirdPartySaleServiceImpl.class);
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final TiersPayantRepository tiersPayantRepository;
    private final SalesLineService salesLineService;
    private final StorageService storageService;
    private final ThirdPartySaleRepository thirdPartySaleRepository;
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final CashSaleRepository cashSaleRepository;

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneId.systemDefault());

    public ThirdPartySaleServiceImpl(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        ClientTiersPayantRepository clientTiersPayantRepository,
        TiersPayantRepository tiersPayantRepository,
        TicketService ticketService,
        SalesLineService salesLineService,
        StorageService storageService,
        ThirdPartySaleRepository thirdPartySaleRepository,
        AssuredCustomerRepository assuredCustomerRepository,
        UserRepository userRepository,
        PaymentService paymentService,
        ReferenceService referenceService,
        WarehouseCalendarService warehouseCalendarService,
        CashRegisterService cashRegisterService,
        AvoirService avoirService,
        PosteRepository posteRepository,
        CashSaleRepository cashSaleRepository
    ) {
        super(
            referenceService,
            warehouseCalendarService,
            storageService,
            userRepository,
            salesLineService,
            cashRegisterService,
            avoirService,
            posteRepository
        );
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.ticketService = ticketService;
        this.salesLineService = salesLineService;
        this.storageService = storageService;
        this.thirdPartySaleRepository = thirdPartySaleRepository;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.paymentService = paymentService;
        this.cashSaleRepository = cashSaleRepository;
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        SalesLine saleLine = salesLineService.createSaleLineFromDTO(
            dto.getSalesLines().getFirst(),
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        ThirdPartySales thirdPartySales = buildThirdPartySale(dto);
        thirdPartySales.getSalesLines().add(saleLine);
        computeSaleEagerAmount(thirdPartySales, saleLine.getSalesAmount(), 0);
        computeSaleCmu(thirdPartySales, saleLine, null);
        processDiscount(thirdPartySales, saleLine, null);
        String message = computeAmounts(dto.getTiersPayants(), thirdPartySales);
        upddateThirdPartySaleAmounts(thirdPartySales, saleLine, null);
        thirdPartySales.setOrigineVente(OrigineVente.DIRECT);
        ThirdPartySales sale = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        saleLine.setSales(sale);
        salesLineService.saveSalesLine(saleLine);
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(sale);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
        return thirdPartySaleDTO;
    }

    @Override
    public ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy) {
        ThirdPartySaleLine clone = (ThirdPartySaleLine) original.clone();
        clone.setStatut(ThirdPartySaleStatut.DELETE);
        clone.setMontant(clone.getMontant() * (-1));
        copy.setLastUserEdit(storageService.getUser());
        clone.setSale(copy);
        thirdPartySaleLineRepository.save(clone);
        original.setStatut(ThirdPartySaleStatut.DELETE);
        thirdPartySaleLineRepository.save(original);
        return clone;
    }

    @Override
    public void copySale(ThirdPartySales sales, ThirdPartySales copy) {
        copy.setPartAssure(sales.getPartAssure() * (-1));
        copy.setPartTiersPayant(sales.getPartTiersPayant() * (-1));
        copy.setLastUserEdit(storageService.getUser());
        copy.getThirdPartySaleLines().clear();
    }

    @Override
    public void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine) {
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
        Set<Consommation> consommations = CollectionUtils.isEmpty(clientTiersPayant.getConsommations())
            ? new HashSet<>()
            : clientTiersPayant.getConsommations();
        consommations
            .stream()
            .filter(consommation -> consommation.getId() == buildConsommationId(dateTimeFormatter.format(thirdPartySaleLine.getUpdated())))
            .findFirst()
            .ifPresentOrElse(
                conso -> conso.setConsommation(conso.getConsommation() + thirdPartySaleLine.getMontant()),
                () -> consommations.add(buildConsommation(thirdPartySaleLine.getMontant()))
            );

        clientTiersPayant.setConsommations(consommations);
        clientTiersPayant.setConsoMensuelle(
            clientTiersPayant.getConsoMensuelle() != null
                ? clientTiersPayant.getConsoMensuelle() + thirdPartySaleLine.getMontant()
                : thirdPartySaleLine.getMontant()
        );
        clientTiersPayant.setUpdated(LocalDateTime.now());
        clientTiersPayantRepository.save(clientTiersPayant);
    }

    private Consommation buildConsommation(Integer montant) {
        LocalDate now = LocalDate.now();
        Consommation consommation = new Consommation();
        consommation.setId(buildConsommationId());
        consommation.setConsommation(montant);
        consommation.setMonth((short) now.getMonthValue());
        consommation.setYear(now.getYear());
        return consommation;
    }

    @Override
    public void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine) {
        TiersPayant tiersPayant = thirdPartySaleLine.getClientTiersPayant().getTiersPayant();
        Set<Consommation> consommations = CollectionUtils.isEmpty(tiersPayant.getConsommations())
            ? new HashSet<>()
            : tiersPayant.getConsommations();
        consommations
            .stream()
            .filter(consommation -> consommation.getId() == buildConsommationId(dateTimeFormatter.format(thirdPartySaleLine.getUpdated())))
            .findFirst()
            .ifPresentOrElse(
                conso -> conso.setConsommation(conso.getConsommation() + thirdPartySaleLine.getMontant()),
                () -> consommations.add(buildConsommation(thirdPartySaleLine.getMontant()))
            );
        tiersPayant.setConsommations(consommations);
        tiersPayant.setConsoMensuelle(
            tiersPayant.getConsoMensuelle() != null
                ? tiersPayant.getConsoMensuelle() + thirdPartySaleLine.getMontant()
                : thirdPartySaleLine.getMontant()
        );
        tiersPayant.setUpdated(LocalDateTime.now());
        tiersPayant.setUpdatedBy(storageService.getUser());
        tiersPayantRepository.save(tiersPayant);
    }

    @Override
    public int buildConsommationId() {
        return Integer.parseInt(LocalDate.now().format(dateTimeFormatter));
    }

    private ThirdPartySaleLine createThirdPartySaleLine(
        ClientTiersPayantDTO clientTiersPayantDTO,
        ClientTiersPayant clientTiersPayant,
        int partTiersPayant
    ) {
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        thirdPartySaleLine.setCreated(LocalDateTime.now());
        thirdPartySaleLine.setUpdated(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setEffectiveUpdateDate(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setNumBon(clientTiersPayantDTO.getNumBon());
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        thirdPartySaleLine.setMontant(partTiersPayant);
        return thirdPartySaleLine;
    }

    private ThirdPartySaleLine updateThirdPartySaleLine(
        ThirdPartySaleLine thirdPartySaleLine,
        ClientTiersPayant clientTiersPayant,
        int partTiersPayant
    ) {
        thirdPartySaleLine.setUpdated(LocalDateTime.now());
        thirdPartySaleLine.setEffectiveUpdateDate(thirdPartySaleLine.getUpdated());
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        thirdPartySaleLine.setMontant(partTiersPayant);
        return thirdPartySaleLine;
    }

    private ClientTiersPayant getOneId(Long id) {
        return clientTiersPayantRepository.getReferenceById(id);
    }

    @Override
    public void processDiscount(ThirdPartySales thirdPartySales, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine != null) {
            thirdPartySales.setDiscountAmount(
                (thirdPartySales.getDiscountAmount() - oldSaleLine.getDiscountAmount()) + saleLine.getDiscountAmount()
            );
            thirdPartySales.setDiscountAmountUg(
                (thirdPartySales.getDiscountAmountUg() - oldSaleLine.getDiscountAmountUg()) + saleLine.getDiscountAmountUg()
            );
            thirdPartySales.setDiscountAmountHorsUg(
                (thirdPartySales.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg()) + saleLine.getDiscountAmountHorsUg()
            );
            thirdPartySales.setNetAmount(thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
        } else {
            thirdPartySales.setDiscountAmount(thirdPartySales.getDiscountAmount() + saleLine.getDiscountAmount());
            thirdPartySales.setDiscountAmountUg(thirdPartySales.getDiscountAmountUg() + saleLine.getDiscountAmountUg());
            thirdPartySales.setDiscountAmountHorsUg(thirdPartySales.getDiscountAmountHorsUg() + saleLine.getDiscountAmountHorsUg());
            thirdPartySales.setNetAmount(thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
        }
    }

    @Override
    public void processDiscountWhenRemovingItem(ThirdPartySales thirdPartySales, SalesLine saleLine) {
        thirdPartySales.setDiscountAmount(thirdPartySales.getDiscountAmount() - saleLine.getDiscountAmount());
        thirdPartySales.setDiscountAmountUg(thirdPartySales.getDiscountAmountUg() - saleLine.getDiscountAmountUg());
        thirdPartySales.setDiscountAmountHorsUg(thirdPartySales.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg());
        thirdPartySales.setNetAmount(thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
    }

    @Override
    public List<ThirdPartySaleLine> findAllBySaleId(Long saleId) {
        return thirdPartySaleLineRepository.findAllBySaleId(saleId);
    }

    private boolean checkIfNumBonIsAlReadyUse(String numBon, Long clientTiersPayantId) {
        if (!StringUtils.hasLength(numBon)) {
            return false;
        }
        return (
            thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
                numBon,
                clientTiersPayantId,
                SalesStatut.CLOSED
            ) >
            0
        );
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto) throws PlafondVenteException {
        Optional<SalesLine> salesLineOp = salesLineService.findBySalesIdAndProduitId(dto.getSaleId(), dto.getProduitId());
        long storageId = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();

        if (salesLineOp.isPresent()) {
            SalesLine salesLine = salesLineOp.get();
            SalesLine oldSalesLine = (SalesLine) salesLine.clone();
            salesLineService.updateSaleLine(dto, salesLine, storageId);
            ThirdPartySales thirdPartySales = (ThirdPartySales) salesLine.getSales();
            var message = computeThirdPartySaleAmounts(thirdPartySales, salesLine, oldSalesLine);
            thirdPartySales = thirdPartySaleRepository.save(thirdPartySales);
            if (StringUtils.hasLength(message)) {
                throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
            }
            return new SaleLineDTO(salesLine);
        }
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getSaleId());
        SalesLine salesLine = salesLineService.create(dto, storageId, thirdPartySales);
        var message = computeThirdPartySaleAmounts(thirdPartySales, salesLine, null);
        thirdPartySales = thirdPartySaleRepository.save(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
        }
        return new SaleLineDTO(salesLine);
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public void deleteSaleLineById(Long id) {
        SalesLine salesLine = salesLineService.getOneById(id);
        ThirdPartySales sales = (ThirdPartySales) salesLine.getSales();
        sales.removeSalesLine(salesLine);
        upddateSaleAmountsOnRemovingItem(sales, salesLine);
        sales.setUpdatedAt(LocalDateTime.now());
        sales.setEffectiveUpdateDate(sales.getUpdatedAt());
        sales.setLastUserEdit(storageService.getUser());
        thirdPartySaleRepository.save(sales);
        salesLineService.deleteSaleLine(salesLine);
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO)
        throws StockException, DeconditionnementStockOut, PlafondVenteException {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        SalesLine oldsalesline = (SalesLine) salesLine.clone();
        salesLineService.updateItemQuantityRequested(
            saleLineDTO,
            salesLine,
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        Sales sales = salesLine.getSales();
        ThirdPartySales thirdPartySales = (ThirdPartySales) sales;
        var message = computeThirdPartySaleAmounts(thirdPartySales, salesLine, oldsalesline);
        thirdPartySales = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
        }
        return new SaleLineDTO(salesLine);
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) throws PlafondVenteException {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        SalesLine oldsalesline = (SalesLine) salesLine.clone();
        salesLineService.updateItemRegularPrice(saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        Sales sales = salesLine.getSales();
        ThirdPartySales thirdPartySales = (ThirdPartySales) sales;
        var message = computeThirdPartySaleAmounts(thirdPartySales, salesLine, oldsalesline);
        thirdPartySales = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
        }
        return new SaleLineDTO(salesLine);
    }

    @Override
    public void cancelSale(Long id) {
        User user = storageService.getUser();
        thirdPartySaleRepository
            .findOneWithEagerSalesLines(id)
            .ifPresent(sales -> {
                ThirdPartySales copy = (ThirdPartySales) sales.clone();
                copySale(sales, copy);
                sales.setUpdatedAt(LocalDateTime.now());
                sales.setEffectiveUpdateDate(sales.getUpdatedAt());
                sales.setCanceled(true);
                sales.setLastUserEdit(user);
                thirdPartySaleRepository.save(sales);
                thirdPartySaleRepository.save(copy);
                List<Ticket> tickets = ticketService.findAllBySaleId(sales.getId());
                paymentService.findAllBySalesId(sales.getId()).forEach(payment -> paymentService.clonePayment(payment, tickets, copy));
                salesLineService.cloneSalesLine(
                    sales.getSalesLines(),
                    copy,
                    user,
                    storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
                );
                findAllBySaleId(id).forEach(thirdPartySaleLine -> {
                    ThirdPartySaleLine thirdPartySaleLineClone = clone(thirdPartySaleLine, copy);
                    updateClientTiersPayantAccount(thirdPartySaleLineClone);
                    updateTiersPayantAccount(thirdPartySaleLineClone);
                });
            });
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public FinalyseSaleDTO save(ThirdPartySaleDTO dto)
        throws PaymentAmountException, SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException {
        User user = storageService.getUser();
        ThirdPartySales p = thirdPartySaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        this.save(p, dto);
        Ticket ticket = ticketService.buildTicket(p, dto, user, buildTvaData(p.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(p, dto, ticket, user);
        p.setTvaEmbeded(ticket.getTva());
        List<ThirdPartySaleLine> thirdPartySaleLines = findAllBySaleId(p.getId());
        if (thirdPartySaleLines.isEmpty() && dto.getTiersPayants().isEmpty()) {
            throw new ThirdPartySalesTiersPayantException();
        }
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            for (ClientTiersPayantDTO clientTiersPayantDTO : dto.getTiersPayants()) {
                ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
                if (clientTiersPayant.getId().compareTo(clientTiersPayantDTO.getId()) == 0) {
                    if (checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(), thirdPartySaleLine.getId())) {
                        throw new NumBonAlreadyUseException(clientTiersPayantDTO.getNumBon());
                    }
                    thirdPartySaleLine.setNumBon(clientTiersPayantDTO.getNumBon());
                    thirdPartySaleLineRepository.save(thirdPartySaleLine);
                }
            }
            updateClientTiersPayantAccount(thirdPartySaleLine);
            updateTiersPayantAccount(thirdPartySaleLine);
        });
        new ArrayList<>(p.getThirdPartySaleLines())
            .stream()
            .min(Comparator.comparing(e -> e.getClientTiersPayant().getPriorite().getValue()))
            .ifPresent(o -> p.setNumBon(o.getNumBon()));
        thirdPartySaleRepository.save(p);
        return new FinalyseSaleDTO(p.getId(), true);
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseDTO putThirdPartySaleOnHold(ThirdPartySaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getId());
        paymentService.buildPaymentFromFromPaymentDTO(thirdPartySales, dto, storageService.getUser());
        thirdPartySaleRepository.save(thirdPartySales);
        response.setSuccess(true);
        return response;
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        salesLineService.updateItemQuantitySold(salesLine, saleLineDTO, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        ThirdPartySales thirdPartySales = (ThirdPartySales) salesLine.getSales();
        thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public void deleteSalePrevente(Long id) {
        thirdPartySaleRepository
            .findOneWithEagerSalesLines(id)
            .ifPresent(sales -> {
                paymentService.findAllBySalesId(sales.getId()).forEach(paymentService::delete);
                ticketService.findAllBySaleId(sales.getId()).forEach(ticketService::delete);
                sales.getSalesLines().forEach(salesLineService::deleteSaleLine);
                thirdPartySaleRepository.delete(sales);
            });
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public void addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, Long saleId)
        throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        ClientTiersPayant clientTiersPayant = clientTiersPayantRepository.getReferenceById(dto.getId());
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(saleId);
        if (checkIfNumBonIsAlReadyUse(dto.getNumBon(), clientTiersPayant.getId())) {
            throw new NumBonAlreadyUseException(dto.getNumBon());
        }
        ThirdPartySaleLine thirdPartySaleLine = createThirdPartySaleLine(dto, clientTiersPayant, 0);
        thirdPartySaleLine.setSale(thirdPartySales);
        thirdPartySaleLineRepository.save(thirdPartySaleLine);
        thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
        var message = reComputeAmounts(thirdPartySales);
        var tp = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(tp);
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public void removeThirdPartySaleLineToSales(Long clientTiersPayantId, Long saleId) throws PlafondVenteException {
        thirdPartySaleLineRepository
            .findFirstByClientTiersPayantIdAndSaleId(clientTiersPayantId, saleId)
            .ifPresent(thirdPartySaleLine -> {
                ThirdPartySales thirdPartySales = thirdPartySaleLine.getSale();
                thirdPartySales.setLastUserEdit(storageService.getUser());
                thirdPartySaleLine.setSale(null);
                thirdPartySales.getThirdPartySaleLines().remove(thirdPartySaleLine);
                thirdPartySaleLineRepository.delete(thirdPartySaleLine);
                reComputeAmounts(thirdPartySales);
                thirdPartySaleRepository.saveAndFlush(thirdPartySales);
            });
    }

    @Override
    public Long changeCashSaleToThirdPartySale(Long saleId, NatureVente natureVente) {
        CashSale cashSale = this.cashSaleRepository.getReferenceById(saleId);
        ThirdPartySales c = copyFromCashSale(cashSale);
        c.setNatureVente(natureVente);
        c = thirdPartySaleRepository.save(c);
        for (SalesLine salesLine : cashSale.getSalesLines()) {
            salesLine.setSales(c);
            salesLineService.saveSalesLine(salesLine);
        }
        this.cashSaleRepository.delete(cashSale);
        return c.getId();
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public void updateTransformedSale(ThirdPartySaleDTO dto) throws PlafondVenteException {
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getId());
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(dto.getCustomerId());
        thirdPartySales.setCustomer(assuredCustomer);
        thirdPartySales.setLastUserEdit(storageService.getUser());
        thirdPartySales.setAyantDroit(assuredCustomer);
        thirdPartySales.setUpdatedAt(LocalDateTime.now());
        thirdPartySales.setEffectiveUpdateDate(thirdPartySales.getUpdatedAt());
        getAyantDroitFromId(dto.getAyantDroitId()).ifPresent(thirdPartySales::setAyantDroit);
        String message = computeAmounts(
            dto.getTiersPayants().stream().peek(e -> e.setNewClientTiersPayant(true)).toList(),
            thirdPartySales
        );
        ThirdPartySales sale = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(sale);
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
    }

    private ThirdPartySales copyFromCashSale(CashSale cashSale) {
        ThirdPartySales c = new ThirdPartySales();
        c.setSalesAmount(cashSale.getSalesAmount());
        c.setOrigineVente(OrigineVente.DIRECT);
        c.setCmuAmount(cashSale.getCmuAmount());
        c.setCostAmount(cashSale.getCostAmount());
        c.setNumberTransaction(cashSale.getNumberTransaction());
        c.setCategorieChiffreAffaire(cashSale.getCategorieChiffreAffaire());
        c.setAvoir(cashSale.getAvoir());
        c.setTypePrescription(cashSale.getTypePrescription());
        c.setSeller(cashSale.getSeller());
        c.setImported(false);
        c.setUser(cashSale.getUser());
        c.setLastUserEdit(this.storageService.getUser());
        c.setCassier(cashSale.getCassier());
        c.setCopy(false);
        c.setCreatedAt(cashSale.getCreatedAt());
        c.setUpdatedAt(cashSale.getUpdatedAt());
        c.setEffectiveUpdateDate(cashSale.getEffectiveUpdateDate());
        c.setPayrollAmount(cashSale.getPayrollAmount());
        c.setToIgnore(cashSale.isToIgnore());
        c.setDiffere(cashSale.isDiffere());
        c.setStatut(cashSale.getStatut());
        c.setStatutCaisse(cashSale.getStatutCaisse());
        c.setCalendar(cashSale.getCalendar());
        c.setCaisse(cashSale.getCaisse());
        c.setLastCaisse(cashSale.getLastCaisse());
        c.setPaymentStatus(cashSale.getPaymentStatus());
        c.setMagasin(cashSale.getMagasin());
        return c;
    }

    private void upddateThirdPartySaleAmounts(ThirdPartySales c, SalesLine saleLine, SalesLine oldSaleLine) {
        computeSaleLazyAmount(c, saleLine, oldSaleLine);
        computeTvaAmount(c, saleLine, oldSaleLine);
        computeUgTvaAmount(c, saleLine, oldSaleLine);
    }

    private String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales, SalesLine salesLine, SalesLine oldsalesline) {
        computeSaleEagerAmount(thirdPartySales, salesLine.getSalesAmount(), oldsalesline != null ? oldsalesline.getSalesAmount() : 0);
        computeSaleCmu(thirdPartySales, salesLine, oldsalesline);
        processDiscount(thirdPartySales, salesLine, oldsalesline);
        var message = reComputeAmounts(thirdPartySales);
        upddateThirdPartySaleAmounts(thirdPartySales, salesLine, oldsalesline);
        return message;
    }

    private void upddateSaleAmountsOnRemovingItem(ThirdPartySales c, SalesLine saleLine) {
        computeSaleEagerAmount(c, saleLine.getSalesAmount() * (-1), 0);
        c.setCmuAmount(c.getCmuAmount() - computeCmuAmount(saleLine));
        processDiscountSaleOnRemovingItem(c, saleLine);
        processDiscountWhenRemovingItem(c, saleLine);
        reComputeAmounts(c);
        computeSaleLazyAmountOnRemovingItem(c, saleLine);
        computeUgTvaAmountOnRemovingItem(c, saleLine);
        computeTvaAmountOnRemovingItem(c, saleLine);
    }

    private ThirdPartySales buildThirdPartySale(ThirdPartySaleDTO dto) throws GenericError {
        if (dto.getCustomerId() == null) {
            throw new GenericError("Veuillez saisir le client", "customerNotFound");
        }
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(dto.getCustomerId());
        ThirdPartySales c = new ThirdPartySales();
        this.intSale(dto, c);
        c.setCustomer(assuredCustomer);
        c.setAyantDroit(assuredCustomer);
        getAyantDroitFromId(dto.getAyantDroitId()).ifPresent(c::setAyantDroit);
        return c;
    }

    private Optional<AssuredCustomer> getAyantDroitFromId(Long ayantDroitId) {
        AssuredCustomer ayantDroit = null;
        if (ayantDroitId != null) {
            ayantDroit = new AssuredCustomer();
            ayantDroit.setId(ayantDroitId);
        }
        return Optional.ofNullable(ayantDroit);
    }

    private String computeAmounts(List<ClientTiersPayantDTO> tiersPayantsClts, ThirdPartySales thirdPartySales)
        throws GenericError, NumBonAlreadyUseException {
        var sb = new StringBuilder();
        int counter = 0;
        int totalMontantTiersPayant = 0;
        List<ClientTiersPayantDTO> clientTiersPayants = new ArrayList<>(tiersPayantsClts);
        int tiersPayantSize = clientTiersPayants.size();
        int pourcentageTotal = clientTiersPayants.stream().mapToInt(ClientTiersPayantDTO::getTaux).sum();
        boolean isPourcentageGreather100 = pourcentageTotal >= 100;
        if (CollectionUtils.isEmpty(clientTiersPayants)) {
            throw new GenericError("Veuillez ajouter un tierpayant ", "tierPayantNotFound");
        }
        clientTiersPayants.sort(Comparator.comparing(ClientTiersPayantDTO::getCategorie));
        Map<Long, List<ClientTiersPayant>> tiersPayants =
            this.clientTiersPayantRepository.findAllByIdIn(
                    clientTiersPayants.stream().map(ClientTiersPayantDTO::getId).collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.groupingBy(ClientTiersPayant::getId));

        for (ClientTiersPayantDTO tp : clientTiersPayants) {
            if (checkIfNumBonIsAlReadyUse(tp.getNumBon(), tp.getId())) {
                throw new NumBonAlreadyUseException(tp.getNumBon());
            }
            var isLast = counter == (tiersPayantSize - 1);
            ClientTiersPayant clientTiersPayant = tiersPayants.get(tp.getId()).getFirst();
            totalMontantTiersPayant +=
            processTiersPayantAmount(totalMontantTiersPayant, sb, tp, thirdPartySales, isLast, isPourcentageGreather100, clientTiersPayant);
            counter++;
        }

        return updateTiersPayantAmounts(thirdPartySales, sb, totalMontantTiersPayant);
    }

    private int processTiersPayantAmount(
        int totalMontantTiersPayant,
        StringBuilder sb,
        ClientTiersPayantDTO clientTiersPayantDTO,
        ThirdPartySales thirdPartySales,
        boolean isLast,
        boolean isPourcentageGreather100,
        ClientTiersPayant clientTiersPayant
    ) throws NumBonAlreadyUseException, PlafondVenteException {
        int netAmount = thirdPartySales.getSalesAmount();
        int cmuAmount = thirdPartySales.getCmuAmount();

        // si non null  nouvelle creation
        if (
            Objects.nonNull(clientTiersPayantDTO) &&
            checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(), clientTiersPayantDTO.getId())
        ) {
            throw new NumBonAlreadyUseException(clientTiersPayantDTO.getNumBon());
        }

        TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();
        int totalAmount = tiersPayant.getCmu() ? netAmount - cmuAmount : netAmount;
        double montantTp = totalAmount * clientTiersPayant.getTauxValue();
        int montantTiersPayant = (int) Math.ceil(montantTp);
        int partTiersPayantnet = computeThirdPartyPart(clientTiersPayant, montantTiersPayant);
        if (montantTiersPayant != partTiersPayantnet) {
            sb
                .append(tiersPayant.getFullName())
                .append(", ne peut prendre en compte : ")
                .append(NumberUtil.formatToString(partTiersPayantnet))
                .append(" , car votre plafond est atteint \n");
        }
        if (isPourcentageGreather100 && isLast) {
            int rest = totalAmount - totalMontantTiersPayant;
            if (rest <= partTiersPayantnet) {
                partTiersPayantnet = rest;
            }
        }
        short newTaux;
        if (montantTiersPayant == partTiersPayantnet) {
            newTaux = clientTiersPayant.getTaux().shortValue();
        } else {
            newTaux = (short) Math.ceil(((double) partTiersPayantnet * 100) / totalAmount);
        }

        ThirdPartySaleLine thirdPartySaleLine = Objects.nonNull(clientTiersPayantDTO)
            ? createThirdPartySaleLine(clientTiersPayantDTO, clientTiersPayant, partTiersPayantnet)
            : updateThirdPartySaleLine(
                thirdPartySaleLineRepository
                    .findFirstByClientTiersPayantIdAndSaleId(clientTiersPayant.getId(), thirdPartySales.getId())
                    .orElseThrow(),
                clientTiersPayant,
                partTiersPayantnet
            );
        thirdPartySaleLine.setSale(thirdPartySales);
        thirdPartySaleLine.setTaux(newTaux);
        if (Objects.nonNull(clientTiersPayantDTO)) {
            thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
            if (clientTiersPayantDTO.isNewClientTiersPayant()) {
                this.thirdPartySaleLineRepository.save(thirdPartySaleLine);
            }
        } else {
            this.thirdPartySaleLineRepository.save(thirdPartySaleLine);
        }

        return partTiersPayantnet;
    }

    private String reComputeAmounts(ThirdPartySales thirdPartySales) throws GenericError, NumBonAlreadyUseException {
        var sb = new StringBuilder();
        int counter = 0;
        int totalMontantTiersPayant = 0;
        List<ThirdPartySaleLine> thirdPartySaleLines = new ArrayList<>(thirdPartySales.getThirdPartySaleLines());
        int tiersPayantSize = thirdPartySaleLines.size();
        int pourcentageTotal = thirdPartySaleLines.stream().mapToInt(e -> e.getClientTiersPayant().getTaux()).sum();
        boolean isPourcentageGreather100 = pourcentageTotal >= 100;
        thirdPartySaleLines.sort(Comparator.comparing(e -> e.getClientTiersPayant().getPriorite().getValue()));

        for (ThirdPartySaleLine tp : thirdPartySaleLines) {
            var isLast = counter == (tiersPayantSize - 1);
            ClientTiersPayant clientTiersPayant = tp.getClientTiersPayant();
            totalMontantTiersPayant +=
            processTiersPayantAmount(
                totalMontantTiersPayant,
                sb,
                null,
                thirdPartySales,
                isLast,
                isPourcentageGreather100,
                clientTiersPayant
            );
            counter++;
        }
        return updateTiersPayantAmounts(thirdPartySales, sb, totalMontantTiersPayant);
    }

    private String updateTiersPayantAmounts(ThirdPartySales thirdPartySales, StringBuilder sb, int totalMontantTiersPayant) {
        switch (thirdPartySales.getNatureVente()) {
            case NatureVente.ASSURANCE -> {
                thirdPartySales.setPartTiersPayant(totalMontantTiersPayant);
                thirdPartySales.setPartAssure(
                    (thirdPartySales.getSalesAmount() - thirdPartySales.getPartTiersPayant()) - thirdPartySales.getDiscountAmount()
                );
            }
            case NatureVente.CARNET -> {
                thirdPartySales.setPartTiersPayant(totalMontantTiersPayant - thirdPartySales.getDiscountAmount());
                thirdPartySales.setPartAssure(thirdPartySales.getNetAmount() - thirdPartySales.getPartTiersPayant());
            }
            case COMPTANT -> throw new RuntimeException("Not yet implemented");
        }

        thirdPartySales.setAmountToBePaid(roundedAmount(thirdPartySales.getPartAssure()));
        return sb.toString();
    }

    private int computeThirdPartyPart(ClientTiersPayant clientTiersPayant, int partTiersPayantNet) {
        TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();
        return computeTiersPayantTauxWithPlafondConso(
            clientTiersPayant.getPlafondJournalier(),
            tiersPayant,
            clientTiersPayant.getPlafondConso(),
            clientTiersPayant.getConsoMensuelle(),
            partTiersPayantNet
        );
    }

    private int computeTiersPayantTauxWithPlafondConso(
        Long plafondJournalier,
        TiersPayant tiersPayant,
        Long plafonConso,
        Long consoMensuelle,
        int partTiersPayantNet
    ) {
        int finalTpAmount = computePlafondTiersPayant(tiersPayant.getPlafondConso(), tiersPayant.getConsoMensuelle(), partTiersPayantNet);

        return computePlafondClient(plafondJournalier, plafonConso, consoMensuelle, finalTpAmount);
    }

    private int computePlafondClient(Long plafondJournalier, Long plafonConso, Long consoMensuelle, int partTiersPayantNet) {
        int totalNetAmount = computeConsommationMensuelle(consoMensuelle, plafonConso, partTiersPayantNet);
        return computePlafondJournalier(plafondJournalier, totalNetAmount);
    }

    private int computePlafondJournalier(Long plafondJournalier, int totalNetAmount) {
        if (plafondJournalier == null) {
            return totalNetAmount;
        }
        if (totalNetAmount <= plafondJournalier) {
            return totalNetAmount;
        }
        return Math.toIntExact(plafondJournalier);
    }

    private int computeConsommationMensuelle(Long consoMensuelle, Long plafondConso, int totalNetAmount) {
        if (plafondConso == null) {
            return totalNetAmount;
        }
        if (consoMensuelle == null) {
            consoMensuelle = 0L;
        }
        int totalConsoMax = (int) (consoMensuelle + plafondConso);
        int totalConso = (int) (consoMensuelle + totalNetAmount);
        if (totalConso <= totalConsoMax) {
            return totalNetAmount;
        }
        return totalConso - totalConsoMax;
    }

    private int computePlafondTiersPayant(Long plafondTiersPayent, Long consoTiersPayant, int totalNetAmount) {
        if (plafondTiersPayent == null) {
            return totalNetAmount;
        }

        if (consoTiersPayant == null) {
            consoTiersPayant = 0L;
        }
        int totalConsoMax = (int) (consoTiersPayant + plafondTiersPayent);
        int totalConso = (int) (consoTiersPayant + totalNetAmount);
        if (totalConso <= totalConsoMax) {
            return totalNetAmount;
        }
        return totalConso - totalConsoMax;
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public void changeCustomer(KeyValue keyValue) throws GenericError, PlafondVenteException {
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(keyValue.key());
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(keyValue.value());
        thirdPartySales.setCustomer(assuredCustomer);
        List<ThirdPartySaleLine> thirdPartySaleLines = thirdPartySales.getThirdPartySaleLines();
        thirdPartySaleLineRepository.deleteAll(thirdPartySaleLines);
        thirdPartySales.getThirdPartySaleLines().clear();
        thirdPartySales.setLastUserEdit(storageService.getUser());
        thirdPartySales.setAyantDroit(assuredCustomer);
        thirdPartySales.setUpdatedAt(LocalDateTime.now());
        thirdPartySales.setEffectiveUpdateDate(thirdPartySales.getUpdatedAt());
        String message = computeAmounts(buildFromCustomer(assuredCustomer), thirdPartySales);
        ThirdPartySales sale = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(sale);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
    }

    private List<ClientTiersPayantDTO> buildFromCustomer(AssuredCustomer assuredCustomer) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOs = new ArrayList<>();
        Set<ClientTiersPayant> clientTiersPayants = assuredCustomer.getClientTiersPayants();
        for (ClientTiersPayant clientTiersPayant : clientTiersPayants) {
            ClientTiersPayantDTO clientTiersPayantDTO = new ClientTiersPayantDTO();
            clientTiersPayantDTO.setId(clientTiersPayant.getId());
            clientTiersPayantDTO.setTaux(clientTiersPayant.getTaux());
            clientTiersPayantDTO.setTiersPayantId(clientTiersPayant.getTiersPayant().getId());
            clientTiersPayantDTO.setPriorite(clientTiersPayant.getPriorite());
            clientTiersPayantDTO.setCategorie(clientTiersPayantDTO.getPriorite().getValue());
            clientTiersPayantDTO.setNewClientTiersPayant(true);
            clientTiersPayantDTOs.add(clientTiersPayantDTO);
        }
        return clientTiersPayantDTOs;
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public FinalyseSaleDTO editSale(ThirdPartySaleDTO dto)
        throws PaymentAmountException, SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException {
        User user = storageService.getUser();
        ThirdPartySales p = thirdPartySaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        this.editSale(p, dto);
        Ticket ticket = ticketService.buildTicket(p, dto, user, buildTvaData(p.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(p, dto, ticket, user);
        p.setTvaEmbeded(ticket.getTva());
        List<ThirdPartySaleLine> thirdPartySaleLines = findAllBySaleId(p.getId());
        if (thirdPartySaleLines.isEmpty() && dto.getTiersPayants().isEmpty()) {
            throw new ThirdPartySalesTiersPayantException();
        }
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            for (ClientTiersPayantDTO clientTiersPayantDTO : dto.getTiersPayants()) {
                ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
                if (clientTiersPayant.getId().compareTo(clientTiersPayantDTO.getId()) == 0) {
                    if (checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(), thirdPartySaleLine.getId())) {
                        throw new NumBonAlreadyUseException(clientTiersPayantDTO.getNumBon());
                    }
                    thirdPartySaleLine.setNumBon(clientTiersPayantDTO.getNumBon());
                    thirdPartySaleLineRepository.save(thirdPartySaleLine);
                }
            }
            updateClientTiersPayantAccount(thirdPartySaleLine);
            updateTiersPayantAccount(thirdPartySaleLine);
        });
        new ArrayList<>(p.getThirdPartySaleLines())
            .stream()
            .min(Comparator.comparing(e -> e.getClientTiersPayant().getPriorite().getValue()))
            .ifPresent(o -> p.setNumBon(o.getNumBon()));
        thirdPartySaleRepository.save(p);
        return new FinalyseSaleDTO(p.getId(), true);
    }
}
