package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.DateDimension;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Ticket;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.SalesLineService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.ThirdPartySaleService;
import com.kobe.warehouse.service.TicketService;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.Consommation;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.GenericError;
import com.kobe.warehouse.web.rest.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.web.rest.errors.PaymentAmountException;
import com.kobe.warehouse.web.rest.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.web.rest.errors.StockException;
import com.kobe.warehouse.web.rest.errors.ThirdPartySalesTiersPayantException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneId.systemDefault());

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
        PaymentService paymentService) {
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.ticketService = ticketService;
        this.salesLineService = salesLineService;
        this.storageService = storageService;
        this.thirdPartySaleRepository = thirdPartySaleRepository;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    @Override
    public ThirdPartySales computeCarnetAmounts(
        ThirdPartySaleDTO thirdPartySaleDTO, ThirdPartySales thirdPartySales)
        throws GenericError, NumBonAlreadyUseException {
        long netAmount = thirdPartySales.getNetAmount();
        long partTiersPayant = 0;
        List<ClientTiersPayantDTO> clientTiersPayants = thirdPartySaleDTO.getTiersPayants();
        log.info(" size ====>> {}", clientTiersPayants.size());
        if (CollectionUtils.isEmpty(clientTiersPayants))
            throw new GenericError("sale", "Veuillez ajouter un tierpayant ", "tierPayantNotFound");
        for (ClientTiersPayantDTO tp : clientTiersPayants) {
            if (checkIfNumBonIsAlReadyUse(tp.getNumBon(), tp.getId()))
                throw new NumBonAlreadyUseException(tp.getNumBon());
            ClientTiersPayant clientTiersPayant = getOneId(tp.getId());
            partTiersPayant += computeTiersPayantPart(clientTiersPayant, netAmount);
            ThirdPartySaleLine thirdPartySaleLine =
                createThirdPartySaleLine(tp, clientTiersPayant, (int) partTiersPayant);
            thirdPartySaleLine.setSale(thirdPartySales);
            thirdPartySaleLine.setTaux(clientTiersPayant.getTaux().shortValue());
            // thirdPartySaleLineRepository.save(thirdPartySaleLine);
            thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
        }
        thirdPartySales.setPartTiersPayant((int) partTiersPayant);
        thirdPartySales.setPartAssure((int) (netAmount - thirdPartySales.getPartTiersPayant()));
        thirdPartySales.setAmountToBePaid(roundedAmount(thirdPartySales.getPartAssure()));
        thirdPartySales.setRestToPay(thirdPartySales.getAmountToBePaid());
        thirdPartySales.setAmountToBeTakenIntoAccount(0);
        return thirdPartySales;
    }

    @Override
    public ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError {
        SalesLine saleLine =
            salesLineService.createSaleLineFromDTO(
                dto.getSalesLines().get(0),
                storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        ThirdPartySales thirdPartySales = buildThirdPartySale(dto);
        thirdPartySales.getSalesLines().add(saleLine);
        computeSaleEagerAmount(thirdPartySales, saleLine.getSalesAmount(), 0);
        processDiscount(thirdPartySales, saleLine, null);
        NatureVente natureVente = dto.getNatureVente();
        if (natureVente == NatureVente.ASSURANCE) {
            computeAmounts(dto, thirdPartySales);
        } else if (natureVente == NatureVente.CARNET) {
            computeCarnetAmounts(dto, thirdPartySales);
        } else {
            throw new RuntimeException("Not yet implemented");
        }

        upddateThirdPartySaleAmounts(thirdPartySales, saleLine, null);
        ThirdPartySales sale = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        saleLine.setSales(sale);
        salesLineService.saveSalesLine(saleLine);
        return new ThirdPartySaleDTO(sale);
    }

    private int computeTiersPayantPart(ClientTiersPayant clientTiersPayant, long netAmount) {
        long partTiersPayant = 0;
        if (clientTiersPayant.getPlafondConso() != null) {
            if (clientTiersPayant.getConsoMensuelle() != null) {
                long thatConsom = clientTiersPayant.getConsoMensuelle() + netAmount;
                if (thatConsom >= clientTiersPayant.getPlafondConso()) {
                    partTiersPayant += (thatConsom - clientTiersPayant.getPlafondConso());
                } else {
                    partTiersPayant += netAmount;
                }
            }
        } else {
            partTiersPayant += netAmount;
        }
        return (int) partTiersPayant;
    }

    @Override
    public void computeAllAmounts(ThirdPartySales thirdPartySales) {
        int partTiersPayantCarnet = 0;
        Set<ThirdPartySaleLine> newthirdPartySaleLines = new HashSet<>();
        List<ThirdPartySaleLine> thirdPartySaleLines =
            findAllBySaleId(thirdPartySales.getId()).stream()
                .sorted(
                    Comparator.comparing(
                        o -> o.getClientTiersPayant().getPriorite().getValue(),
                        Comparator.naturalOrder()))
                .collect(Collectors.toList());
        long netAmount =
            thirdPartySales.getNatureVente() == NatureVente.ASSURANCE
                ? thirdPartySales.getSalesAmount()
                : thirdPartySales.getNetAmount();
        for (ThirdPartySaleLine o : thirdPartySaleLines) {
            int amount =
                thirdPartySales.getNatureVente() == NatureVente.ASSURANCE
                    ? computeThirdPartyPart(o.getClientTiersPayant(), (int) netAmount)
                    : computeTiersPayantPart(o.getClientTiersPayant(), netAmount);
            o.setMontant(amount);
            newthirdPartySaleLines.add(o);
            partTiersPayantCarnet += amount;
            thirdPartySales.getThirdPartySaleLines().add(o);
        }
        thirdPartySales.getThirdPartySaleLines().clear();
        thirdPartySales.getThirdPartySaleLines().addAll(newthirdPartySaleLines);
        thirdPartySales.setPartTiersPayant(partTiersPayantCarnet);
        thirdPartySales.setPartAssure((int) (netAmount - thirdPartySales.getPartTiersPayant()));
        thirdPartySales.setAmountToBePaid(roundedAmount(thirdPartySales.getPartAssure()));
        thirdPartySales.setRestToPay(thirdPartySales.getAmountToBePaid());
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
        Set<Consommation> consommations =
            CollectionUtils.isEmpty(clientTiersPayant.getConsommations())
                ? new HashSet<>()
                : clientTiersPayant.getConsommations();
        consommations.stream()
            .filter(
                consommation ->
                    consommation.getId()
                        == buildConsommationId(
                        dateTimeFormatter.format(thirdPartySaleLine.getUpdated())))
            .findFirst()
            .ifPresentOrElse(
                conso ->
                    conso.setConsommation(conso.getConsommation() + thirdPartySaleLine.getMontant()),
                () -> consommations.add(buildConsommation(thirdPartySaleLine.getMontant())));

        clientTiersPayant.setConsommations(consommations);
        clientTiersPayant.setConsoMensuelle(
            clientTiersPayant.getConsoMensuelle() != null
                ? clientTiersPayant.getConsoMensuelle() + thirdPartySaleLine.getMontant()
                : thirdPartySaleLine.getMontant());
        clientTiersPayant.setUpdated(Instant.now());
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
        Set<Consommation> consommations =
            CollectionUtils.isEmpty(tiersPayant.getConsommations())
                ? new HashSet<>()
                : tiersPayant.getConsommations();
        consommations.stream()
            .filter(
                consommation ->
                    consommation.getId()
                        == buildConsommationId(
                        dateTimeFormatter.format(thirdPartySaleLine.getUpdated())))
            .findFirst()
            .ifPresentOrElse(
                conso ->
                    conso.setConsommation(conso.getConsommation() + thirdPartySaleLine.getMontant()),
                () -> consommations.add(buildConsommation(thirdPartySaleLine.getMontant())));
        tiersPayant.setConsommations(consommations);
        tiersPayant.setConsoMensuelle(
            tiersPayant.getConsoMensuelle() != null
                ? tiersPayant.getConsoMensuelle() + thirdPartySaleLine.getMontant()
                : thirdPartySaleLine.getMontant());
        tiersPayant.setUpdated(Instant.now());
        tiersPayant.setUpdatedBy(storageService.getUser());
        tiersPayantRepository.save(tiersPayant);
    }

    @Override
    public int buildConsommationId() {
        return Integer.valueOf(LocalDate.now().format(dateTimeFormatter));
    }

    private ThirdPartySaleLine createThirdPartySaleLine(
        ClientTiersPayantDTO clientTiersPayantDTO,
        ClientTiersPayant clientTiersPayant,
        int partTiersPayant) {
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        thirdPartySaleLine.setCreated(Instant.now());
        thirdPartySaleLine.setUpdated(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setEffectiveUpdateDate(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setNumBon(clientTiersPayantDTO.getNumBon());
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        thirdPartySaleLine.setMontant(partTiersPayant);
        return thirdPartySaleLine;
    }

    private ClientTiersPayant getOneId(Long id) {
        return clientTiersPayantRepository.getReferenceById(id);
    }

    @Override
    public void processDiscount(
        ThirdPartySales thirdPartySales, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine != null) {
            thirdPartySales.setDiscountAmount(
                (thirdPartySales.getDiscountAmount() - oldSaleLine.getDiscountAmount())
                    + saleLine.getDiscountAmount());
            thirdPartySales.setDiscountAmountUg(
                (thirdPartySales.getDiscountAmountUg() - oldSaleLine.getDiscountAmountUg())
                    + saleLine.getDiscountAmountUg());
            thirdPartySales.setDiscountAmountHorsUg(
                (thirdPartySales.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg())
                    + saleLine.getDiscountAmountHorsUg());
            thirdPartySales.setNetAmount(
                thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
        } else {
            thirdPartySales.setDiscountAmount(
                thirdPartySales.getDiscountAmount() + saleLine.getDiscountAmount());
            thirdPartySales.setDiscountAmountUg(
                thirdPartySales.getDiscountAmountUg() + saleLine.getDiscountAmountUg());
            thirdPartySales.setDiscountAmountHorsUg(
                thirdPartySales.getDiscountAmountHorsUg() + saleLine.getDiscountAmountHorsUg());
            thirdPartySales.setNetAmount(
                thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
        }
    }

    @Override
    public void processDiscountWhenRemovingItem(ThirdPartySales thirdPartySales, SalesLine saleLine) {
        thirdPartySales.setDiscountAmount(
            thirdPartySales.getDiscountAmount() - saleLine.getDiscountAmount());
        thirdPartySales.setDiscountAmountUg(
            thirdPartySales.getDiscountAmountUg() - saleLine.getDiscountAmountUg());
        thirdPartySales.setDiscountAmountHorsUg(
            thirdPartySales.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg());
        thirdPartySales.setNetAmount(
            thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
    }

    @Override
    public List<ThirdPartySaleLine> findAllBySaleId(Long saleId) {
        return thirdPartySaleLineRepository.findAllBySaleId(saleId);
    }

    private boolean checkIfNumBonIsAlReadyUse(String numBon, Long clientTiersPayantId) {
        if (StringUtils.isEmpty(numBon)) return false;
        return thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            numBon, clientTiersPayantId, SalesStatut.CLOSED)
            > 0;
    }

    @Override
    public SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto) {
        Optional<SalesLine> salesLineOp =
            salesLineService.findBySalesIdAndProduitId(dto.getSaleId(), dto.getProduitId());
        Long storageId = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
        if (salesLineOp.isPresent()) {
            SalesLine salesLine = salesLineOp.get();
            SalesLine OldSalesLine = (SalesLine) salesLine.clone();
            salesLineService.updateSaleLine(dto, salesLine, storageId);
            ThirdPartySales thirdPartySales = (ThirdPartySales) salesLine.getSales();
            computeThirdPartySaleAmounts(thirdPartySales, salesLine, OldSalesLine);
            thirdPartySaleRepository.save(thirdPartySales);
            return new SaleLineDTO(salesLine);
        }
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getSaleId());
        SalesLine salesLine = salesLineService.create(dto, storageId, thirdPartySales);
        computeThirdPartySaleAmounts(thirdPartySales, salesLine, null);
        thirdPartySaleRepository.save(thirdPartySales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public void deleteSaleLineById(Long id) {
        SalesLine salesLine = salesLineService.getOneById(id);
        ThirdPartySales sales = (ThirdPartySales) salesLine.getSales();
        sales.removeSalesLine(salesLine);
        upddateSaleAmountsOnRemovingItem(sales, salesLine);
        sales.setUpdatedAt(Instant.now());
        sales.setEffectiveUpdateDate(sales.getUpdatedAt());
        sales.setLastUserEdit(storageService.getUser());
        thirdPartySaleRepository.save(sales);
        salesLineService.deleteSaleLine(salesLine);
    }

    @Override
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO)
        throws StockException, DeconditionnementStockOut {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        SalesLine OldSalesLine = (SalesLine) salesLine.clone();
        salesLineService.updateItemQuantityRequested(
            saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        Sales sales = salesLine.getSales();
        ThirdPartySales thirdPartySales = (ThirdPartySales) sales;
        computeThirdPartySaleAmounts(thirdPartySales, salesLine, OldSalesLine);
        thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        SalesLine OldSalesLine = (SalesLine) salesLine.clone();
        salesLineService.updateItemRegularPrice(
            saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        Sales sales = salesLine.getSales();
        ThirdPartySales thirdPartySales = (ThirdPartySales) sales;
        computeThirdPartySaleAmounts(thirdPartySales, salesLine, OldSalesLine);
        thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public void cancelSale(Long id) {
        User user = storageService.getUser();
        thirdPartySaleRepository
            .findOneWithEagerSalesLines(id)
            .ifPresent(
                sales -> {
                    ThirdPartySales copy = (ThirdPartySales) sales.clone();
                    copySale(sales, copy);
                    sales.setUpdatedAt(Instant.now());
                    sales.setEffectiveUpdateDate(sales.getUpdatedAt());
                    sales.setCanceled(true);
                    sales.setLastUserEdit(user);
                    thirdPartySaleRepository.save(sales);
                    thirdPartySaleRepository.save(copy);
                    List<Ticket> tickets = ticketService.findAllBySaleId(sales.getId());
                    paymentService
                        .findAllBySalesId(sales.getId())
                        .forEach(
                            payment -> {
                                paymentService.clonePayment(payment, tickets, copy);
                            });
                    salesLineService.cloneSalesLine(
                        sales.getSalesLines(),
                        copy,
                        user,
                        storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
                    findAllBySaleId(id)
                        .forEach(
                            thirdPartySaleLine -> {
                                ThirdPartySaleLine thirdPartySaleLineClone =
                                    clone(thirdPartySaleLine, copy);
                                updateClientTiersPayantAccount(thirdPartySaleLineClone);
                                updateTiersPayantAccount(thirdPartySaleLineClone);
                            });
                });
    }

    @Override
    public ResponseDTO save(ThirdPartySaleDTO dto)
        throws PaymentAmountException, SaleNotFoundCustomerException,
        ThirdPartySalesTiersPayantException {
        ResponseDTO response = new ResponseDTO();
        User user = storageService.getUser();
        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        Long id = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
        ThirdPartySales p =
            thirdPartySaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        p.getSalesLines()
            .forEach(salesLine -> salesLineService.createInventory(salesLine, user, dateD, id));
        p.setStatut(SalesStatut.CLOSED);
        p.setStatutCaisse(SalesStatut.CLOSED);
        p.setDiffere(dto.isDiffere());
        if (!p.isDiffere() && dto.getPayrollAmount() < dto.getAmountToBePaid())
            throw new PaymentAmountException();
        if (p.getCustomer() == null) throw new SaleNotFoundCustomerException();
        p.setPayrollAmount(dto.getPayrollAmount());
        p.setRestToPay(dto.getRestToPay());
        p.setUpdatedAt(Instant.now());
        p.setMonnaie(dto.getMontantRendu());
        p.setEffectiveUpdateDate(p.getUpdatedAt());
        p.setLastUserEdit(user);
        if (p.getRestToPay() == 0) {
            p.setPaymentStatus(PaymentStatus.PAYE);
        } else {
            p.setPaymentStatus(PaymentStatus.IMPAYE);
        }
        buildReference(p);
        Ticket ticket = ticketService.buildTicket(p, dto, user, buildTvaData(p.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(p, dto, ticket, user);
        p.setTvaEmbeded(ticket.getTva());
        List<ThirdPartySaleLine> thirdPartySaleLines = findAllBySaleId(p.getId());
        if (thirdPartySaleLines.isEmpty() && dto.getTiersPayants().isEmpty())
            throw new ThirdPartySalesTiersPayantException();
        thirdPartySaleLines.forEach(
            thirdPartySaleLine -> {
                for (ClientTiersPayantDTO clientTiersPayantDTO : dto.getTiersPayants()) {
                    ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
                    if (clientTiersPayant.getId().compareTo(clientTiersPayantDTO.getId()) == 0) {
                        if ((!StringUtils.isEmpty(clientTiersPayantDTO.getNumBon())
                            && !StringUtils.isEmpty(thirdPartySaleLine.getNumBon()))
                            && (!clientTiersPayantDTO.getNumBon().equals(thirdPartySaleLine.getNumBon()))) {
                            if (checkIfNumBonIsAlReadyUse(
                                clientTiersPayantDTO.getNumBon(), thirdPartySaleLine.getId()))
                                throw new NumBonAlreadyUseException(clientTiersPayantDTO.getNumBon());
                            thirdPartySaleLine.setNumBon(clientTiersPayantDTO.getNumBon());
                            thirdPartySaleLineRepository.save(thirdPartySaleLine);
                        }
                        continue;
                    }
                }
                updateClientTiersPayantAccount(thirdPartySaleLine);
                updateTiersPayantAccount(thirdPartySaleLine);
            });
        p.getThirdPartySaleLines().stream()
            .sorted(
                Comparator.comparing(
                    e -> e.getClientTiersPayant().getPriorite().getValue(), Comparator.naturalOrder()))
            .findFirst()
            .ifPresent(o -> p.setNumBon(o.getNumBon()));
        thirdPartySaleRepository.save(p);
        response.setMessage(ticket.getCode());
        response.setSuccess(true);
        response.setSize(p.getId().intValue());
        return response;
    }

    @Override
    public ResponseDTO putThirdPartySaleOnHold(ThirdPartySaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getId());
        paymentService.buildPaymentFromFromPaymentDTO(
            thirdPartySales, dto, storageService.getUser());
        thirdPartySaleRepository.save(thirdPartySales);
        response.setSuccess(true);
        return response;
    }

    @Override
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        salesLineService.updateItemQuantitySold(
            salesLine, saleLineDTO, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        ThirdPartySales thirdPartySales = (ThirdPartySales) salesLine.getSales();
        thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public void deleteSalePrevente(Long id) {
        thirdPartySaleRepository
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
                    thirdPartySaleRepository.delete(sales);
                });
    }

    @Override
    public ThirdPartySaleDTO addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, Long saleId)
        throws GenericError, NumBonAlreadyUseException {
        ClientTiersPayant clientTiersPayant = clientTiersPayantRepository.getReferenceById(dto.getId());
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(saleId);
        ThirdPartySaleLine thirdPartySaleLine = createThirdPartySaleLine(dto, clientTiersPayant, 0);
        thirdPartySaleLine.setSale(thirdPartySales);
        thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
    /* List<ThirdPartySaleLine> thirdPartySaleLines =
    thirdPartySaleLineRepository.findAllBySaleId(saleId);*/
        recompute(thirdPartySales.getThirdPartySaleLines(), thirdPartySales);
        return new ThirdPartySaleDTO(thirdPartySaleRepository.saveAndFlush(thirdPartySales));
    }

    @Override
    public void removeThirdPartySaleLineToSales(Long clientTiersPayantId, Long saleId) {
        thirdPartySaleLineRepository
            .findFirstByClientTiersPayantIdAndSaleId(clientTiersPayantId, saleId)
            .ifPresent(
                thirdPartySaleLine -> {
                    ThirdPartySales thirdPartySales = thirdPartySaleLine.getSale();
                    thirdPartySales.setLastUserEdit(storageService.getUser());
                    thirdPartySaleLine.setSale(null);
                    thirdPartySales.getThirdPartySaleLines().remove(thirdPartySaleLine);
                    recompute(thirdPartySales.getThirdPartySaleLines(), thirdPartySales);
                    thirdPartySaleRepository.saveAndFlush(thirdPartySales);
                });
    }

    private void upddateThirdPartySaleAmounts(
        ThirdPartySales c, SalesLine saleLine, SalesLine oldSaleLine) {
        computeSaleLazyAmount(c, saleLine, oldSaleLine);
        computeTvaAmount(c, saleLine, oldSaleLine);
        computeUgTvaAmount(c, saleLine, oldSaleLine);
    }

    private void computeThirdPartySaleAmounts(
        ThirdPartySales thirdPartySales, SalesLine salesLine, SalesLine OldSalesLine) {
        computeSaleEagerAmount(
            thirdPartySales,
            salesLine.getSalesAmount(),
            OldSalesLine != null ? OldSalesLine.getSalesAmount() : 0);
        processDiscount(thirdPartySales, salesLine, OldSalesLine);
        computeAllAmounts(thirdPartySales);
        upddateThirdPartySaleAmounts(thirdPartySales, salesLine, OldSalesLine);
    }

    private void upddateSaleAmountsOnRemovingItem(ThirdPartySales c, SalesLine saleLine) {
        computeSaleEagerAmount(c, saleLine.getSalesAmount() * (-1), 0);
        processDiscountSaleOnRemovingItem(c, saleLine);
        processDiscountWhenRemovingItem(c, saleLine);
        computeAllAmounts(c);
        computeSaleLazyAmountOnRemovingItem(c, saleLine);
        computeUgTvaAmountOnRemovingItem(c, saleLine);
        computeTvaAmountOnRemovingItem(c, saleLine);
    }

    private ThirdPartySales buildThirdPartySale(ThirdPartySaleDTO dto) throws GenericError {
        DateDimension dateDimension = Constants.DateDimension(LocalDate.now());
        AssuredCustomer assuredCustomer =
            dto.getCustomer() != null
                ? assuredCustomerRepository.getReferenceById(dto.getCustomer().getId())
                : null;
        if (assuredCustomer == null)
            throw new GenericError("sale", "Veuillez saisir le client", "customerNotFound");
        ThirdPartySales c = new ThirdPartySales();
        c.setDateDimension(dateDimension);
        c.setCustomer(assuredCustomer);
        c.setAyantDroit(assuredCustomer);
        getAyantDroitFromId(dto.getAyantDroitId())
            .ifPresent(
                assuredCustomer1 -> {
                    c.setAyantDroit(assuredCustomer1);
                });
        c.setNatureVente(dto.getNatureVente());
        c.setTypePrescription(dto.getTypePrescription());
        User user = storageService.getUser();
        User caissier = user;
        if (user.getId().compareTo(dto.getCassier().getId()) != 0) {
            caissier = userRepository.getReferenceById(dto.getCassier().getId());
        }
        if (caissier.getId().compareTo(dto.getSeller().getId()) != 0) {
            c.setSeller(caissier);
        } else {
            c.setSeller(userRepository.getReferenceById(dto.getSeller().getId()));
        }
        c.setImported(false);
        c.setUser(user);
        c.setLastUserEdit(c.getUser());
        c.setCassier(caissier);
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

    private ThirdPartySales computeAmounts(
        ThirdPartySaleDTO thirdPartySaleDTO, ThirdPartySales thirdPartySales)
        throws GenericError, NumBonAlreadyUseException {
        long netAmount = thirdPartySales.getSalesAmount();
        long partTiersPayant = 0;
        int totalTaux = 0;
        int montantVariable = (int) netAmount;
        List<ClientTiersPayantDTO> clientTiersPayants = thirdPartySaleDTO.getTiersPayants();
        if (CollectionUtils.isEmpty(clientTiersPayants))
            throw new GenericError("sale", "Veuillez ajouter un tierpayant ", "tierPayantNotFound");
        clientTiersPayants.sort(
            Comparator.comparing(e -> e.getPriorite().getValue(), Comparator.naturalOrder()));
        log.info(" clientTiersPayants ====>> {}", clientTiersPayants.size());
        for (ClientTiersPayantDTO tp : clientTiersPayants) {
            if (checkIfNumBonIsAlReadyUse(tp.getNumBon(), tp.getId()))
                throw new NumBonAlreadyUseException(tp.getNumBon());
            ClientTiersPayant clientTiersPayant = getOneId(tp.getId());
            int partTiersPayantnet = computeThirdPartyPart(clientTiersPayant, (int) netAmount);
            short newTaux = 0;
            if (montantVariable > partTiersPayantnet) {
                montantVariable -= partTiersPayantnet;
                newTaux = clientTiersPayant.getTaux().shortValue();
                totalTaux += newTaux;
            } else if (montantVariable <= partTiersPayantnet) {
                partTiersPayantnet = montantVariable;
                newTaux = (short) (100 - totalTaux);
                totalTaux += newTaux;
            }
            ThirdPartySaleLine thirdPartySaleLine =
                createThirdPartySaleLine(tp, clientTiersPayant, partTiersPayantnet);
            thirdPartySaleLine.setSale(thirdPartySales);
            thirdPartySaleLine.setTaux(newTaux);
            log.info(" thirdPartySaleLine ====>> {}", thirdPartySaleLine);
            // this.thirdPartySaleLineRepository.save(thirdPartySaleLine);
            thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
            partTiersPayant += partTiersPayantnet;
        }
        thirdPartySales.setPartTiersPayant((int) partTiersPayant);
        thirdPartySales.setPartAssure((int) (netAmount - thirdPartySales.getPartTiersPayant()));
        thirdPartySales.setAmountToBePaid(roundedAmount(thirdPartySales.getPartAssure()));
        thirdPartySales.setRestToPay(thirdPartySales.getAmountToBePaid());
        thirdPartySales.setAmountToBeTakenIntoAccount(0);
        return thirdPartySales;
    }

    private void recompute(
        List<ThirdPartySaleLine> thirdPartySaleLines, ThirdPartySales thirdPartySales)
        throws GenericError, NumBonAlreadyUseException {
        if (CollectionUtils.isEmpty(thirdPartySaleLines))
            throw new GenericError("sale", "Veuillez ajouter un tierpayant ", "tierPayantNotFound");
        long netAmount = thirdPartySales.getSalesAmount();
        long partTiersPayant = 0;
        int totalTaux = 0;
        int montantVariable = (int) netAmount;

        thirdPartySaleLines.sort(
            Comparator.comparing(
                e -> e.getClientTiersPayant().getPriorite().getValue(), Comparator.naturalOrder()));
        for (ThirdPartySaleLine thirdPartySaleLine : thirdPartySaleLines) {
            ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
            if (checkIfNumBonIsAlReadyUse(thirdPartySaleLine.getNumBon(), clientTiersPayant.getId()))
                throw new NumBonAlreadyUseException(thirdPartySaleLine.getNumBon());
            int partTiersPayantnet = computeThirdPartyPart(clientTiersPayant, (int) netAmount);
            short newTaux = 0;
            if (montantVariable > partTiersPayantnet) {
                montantVariable -= partTiersPayantnet;
                newTaux = clientTiersPayant.getTaux().shortValue();
                totalTaux += newTaux;
            } else if (montantVariable <= partTiersPayantnet) {
                partTiersPayantnet = montantVariable;
                newTaux = (short) (100 - totalTaux);
                totalTaux += newTaux;
            }
            thirdPartySaleLine.setMontant(partTiersPayantnet);
            thirdPartySaleLine.setTaux(newTaux);
            partTiersPayant += partTiersPayantnet;
        }
        thirdPartySales.setPartTiersPayant((int) partTiersPayant);
        thirdPartySales.setAmountToBePaid((int) (netAmount - thirdPartySales.getPartTiersPayant()));
        thirdPartySales.setPartAssure(thirdPartySales.getAmountToBePaid());
        thirdPartySales.setRestToPay(thirdPartySales.getAmountToBePaid());
        thirdPartySales.setAmountToBeTakenIntoAccount(0);
    }

    private int computeThirdPartyPart(ClientTiersPayant clientTiersPayant, int saleAmount) {
        double tauxToDouble = clientTiersPayant.getTauxValue();
        double partTiersPayant = saleAmount * tauxToDouble;
        int partTiersPayantNet = (int) Math.ceil(partTiersPayant);
        TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();
        partTiersPayantNet =
            computeTiersPayantTauxWithPlafondConso(
                tiersPayant.getPlafondConso(), tiersPayant.getConsoMensuelle(), partTiersPayantNet);
        return computeTiersPayantTauxWithPlafondConso(
            clientTiersPayant.getPlafondConso(),
            clientTiersPayant.getConsoMensuelle(),
            partTiersPayantNet);
    }

    private int computeTiersPayantTauxWithPlafondConso(
        Long plafonConso, Long consoMensuelle, int partTiersPayantNet) {
        return checkPlafondConso(plafonConso, consoMensuelle, partTiersPayantNet);
    }

    private int checkPlafondConso(Long plafonConso, Long consoMensuelle, int partTiersPayantNet) {
        if (plafonConso != null && consoMensuelle != null) {
            if (consoMensuelle >= plafonConso) {
                return 0;
            }
            long consoAndPartTiersPayant = consoMensuelle + partTiersPayantNet;
            if (consoAndPartTiersPayant <= plafonConso) {
                return partTiersPayantNet;
            } else {
                return (int) (plafonConso - consoMensuelle);
            }
        } else {
            return partTiersPayantNet;
        }
    }
}
