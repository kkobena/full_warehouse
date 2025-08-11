package com.kobe.warehouse.service.sale.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.TiersPayantPrix;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.TiersPayantPrixRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.Consommation;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.errors.PrivilegeException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.errors.ThirdPartySalesTiersPayantException;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import com.kobe.warehouse.service.sale.AvoirService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.sale.dto.UpdateSale;
import com.kobe.warehouse.service.utils.AfficheurPosService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ThirdPartySaleServiceImpl extends SaleCommonService implements ThirdPartySaleService {

    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final TiersPayantRepository tiersPayantRepository;
    private final SalesLineService salesLineService;
    private final StorageService storageService;
    private final ThirdPartySaleRepository thirdPartySaleRepository;
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final PaymentService paymentService;
    private final CashSaleRepository cashSaleRepository;
    private final UtilisationCleSecuriteService utilisationCleSecuriteService;
    private final RemiseRepository remiseRepository;
    private final PrixRererenceService prixRererenceService;
    private final TiersPayantPrixRepository tiersPayantPrixRepository;
    private final LogsService logService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneId.systemDefault());

    public ThirdPartySaleServiceImpl(
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        ClientTiersPayantRepository clientTiersPayantRepository,
        TiersPayantRepository tiersPayantRepository,
        SaleLineServiceFactory saleLineServiceFactory,
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
        CashSaleRepository cashSaleRepository,
        UtilisationCleSecuriteService utilisationCleSecuriteService,
        RemiseRepository remiseRepository,
        AfficheurPosService afficheurPosService,
        PrixRererenceService prixRererenceService,
        TiersPayantPrixRepository tiersPayantPrixRepository,
        LogsService logService
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
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.salesLineService = saleLineServiceFactory.getService(TypeVente.ThirdPartySales);
        this.storageService = storageService;
        this.thirdPartySaleRepository = thirdPartySaleRepository;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.paymentService = paymentService;
        this.cashSaleRepository = cashSaleRepository;
        this.utilisationCleSecuriteService = utilisationCleSecuriteService;
        this.remiseRepository = remiseRepository;
        this.prixRererenceService = prixRererenceService;
        this.tiersPayantPrixRepository = tiersPayantPrixRepository;
        this.logService = logService;
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        SalesLine saleLine = salesLineService.createSaleLineFromDTO(
            dto.getSalesLines().getFirst(),
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        ThirdPartySales thirdPartySales = buildThirdPartySale(dto);
        List<ClientTiersPayant> clientTiersPayants =
            this.clientTiersPayantRepository.findAllByIdIn(
                    dto.getTiersPayants().stream().map(ClientTiersPayantDTO::getId).collect(Collectors.toSet())
                );

        computePrixReference(clientTiersPayants, saleLine);
        thirdPartySales.getSalesLines().add(saleLine);
        computeSaleEagerAmount(thirdPartySales, saleLine.getSalesAmount(), 0);

        applRemiseToSale(thirdPartySales);
        String message = computeAmounts(clientTiersPayants, dto.getTiersPayants(), thirdPartySales);
        upddateThirdPartySaleAmounts(thirdPartySales, saleLine, null);
        thirdPartySales.setOrigineVente(OrigineVente.DIRECT);
        ThirdPartySales sale = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        saleLine.setSales(sale);
        salesLineService.saveSalesLine(saleLine);
        this.displayNet(thirdPartySales.getPartAssure());
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
        tiersPayant.setUser(storageService.getUser());
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
        ThirdPartySales thirdPartySales;
        if (salesLineOp.isPresent()) {
            SalesLine salesLine = salesLineOp.get();
            SalesLine oldSalesLine = (SalesLine) salesLine.clone();
            salesLineService.updateSaleLine(dto, salesLine, storageId);
            computePrixReference(salesLine);
            thirdPartySales = (ThirdPartySales) salesLine.getSales();
            var message = computeThirdPartySaleAmounts(thirdPartySales, salesLine, oldSalesLine);
            thirdPartySales = thirdPartySaleRepository.save(thirdPartySales);
            if (StringUtils.hasLength(message)) {
                throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
            }
            return new SaleLineDTO(salesLine);
        }
        thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getSaleId());
        SalesLine salesLine = salesLineService.create(dto, storageId, thirdPartySales);
        computePrixReference(
            thirdPartySales.getThirdPartySaleLines().stream().map(ThirdPartySaleLine::getClientTiersPayant).toList(),
            salesLine
        );
        var message = computeThirdPartySaleAmounts(thirdPartySales, salesLine, null);
        thirdPartySales = thirdPartySaleRepository.save(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
        }
        this.displayNet(thirdPartySales.getPartAssure());
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
        this.displayNet(sales.getPartAssure());
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
        this.displayNet(thirdPartySales.getPartAssure());
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
        computeOptionPrix(salesLine);
        Sales sales = salesLine.getSales();
        ThirdPartySales thirdPartySales = (ThirdPartySales) sales;
        var message = computeThirdPartySaleAmounts(thirdPartySales, salesLine, oldsalesline);
        thirdPartySales = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        this.displayNet(thirdPartySales.getPartAssure());
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
                paymentService.findAllBySalesId(sales.getId()).forEach(payment -> paymentService.clonePayment(payment, copy));
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
        ThirdPartySales p = thirdPartySaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        this.save(p, dto);
        FinalyseSaleDTO response = finalizeSaleProcess(p, dto);
        displayMonnaie(dto.getMontantRendu());
        return response;
    }

    private FinalyseSaleDTO finalizeSaleProcess(ThirdPartySales p, ThirdPartySaleDTO dto) {
        p.setTvaEmbeded(buildTvaData(p.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(p, dto);
        List<ThirdPartySaleLine> thirdPartySaleLines = findAllBySaleId(p.getId());
        if (thirdPartySaleLines.isEmpty() && dto.getTiersPayants().isEmpty()) {
            throw new ThirdPartySalesTiersPayantException();
        }
        Map<Long, String> numBonMap = dto
            .getTiersPayants()
            .stream()
            .collect(Collectors.toMap(ClientTiersPayantDTO::getId, ClientTiersPayantDTO::getNumBon, (a, b) -> b));

        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
            String numBon = numBonMap.get(clientTiersPayant.getId());
            if (numBon != null) {
                if (checkIfNumBonIsAlReadyUse(numBon, clientTiersPayant.getId())) {
                    throw new NumBonAlreadyUseException(numBon);
                }
                thirdPartySaleLine.setNumBon(numBon);
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
    @Transactional
    public void updateDate(ThirdPartySaleDTO dto) {
        this.thirdPartySaleRepository.findById(dto.getId()).ifPresent(sales -> {
                this.logService.create(
                        TransactionType.MODIFICATION_DATE_DE_VENTE,
                        TransactionType.MODIFICATION_DATE_DE_VENTE.getValue(),
                        sales.getId().toString(),
                        sales.getUpdatedAt().toString(),
                        dto.getUpdatedAt().toString()
                    );
                sales.setCreatedAt(dto.getUpdatedAt());
                sales.setUpdatedAt(sales.getCreatedAt());
                sales
                    .getThirdPartySaleLines()
                    .forEach(thirdPartySaleLine -> {
                        thirdPartySaleLine.setUpdated(sales.getUpdatedAt());
                        thirdPartySaleLine.setCreated(sales.getUpdatedAt());
                        thirdPartySaleLineRepository.save(thirdPartySaleLine);
                    });
                thirdPartySaleRepository.save(sales);
            });
    }

    @Override
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        salesLineService.updateItemQuantitySold(salesLine, saleLineDTO, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        ThirdPartySales thirdPartySales = (ThirdPartySales) salesLine.getSales();
        thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        this.displayNet(thirdPartySales.getPartAssure());
        return new SaleLineDTO(salesLine);
    }

    @Override
    public void deleteSalePrevente(Long id) {
        thirdPartySaleRepository
            .findOneWithEagerSalesLines(id)
            .ifPresent(sales -> {
                paymentService.findAllBySalesId(sales.getId()).forEach(paymentService::delete);
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
        applRemiseToSale(thirdPartySales);
        var message = reComputeAmounts(thirdPartySales);
        var tp = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        this.displayNet(thirdPartySales.getPartAssure());
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
                this.displayNet(thirdPartySales.getPartAssure());
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
        this.displayNet(c.getPartAssure());
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
            null,
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
        c.setCostAmount(cashSale.getCostAmount());
        c.setNumberTransaction(cashSale.getNumberTransaction());
        c.setCategorieChiffreAffaire(cashSale.getCategorieChiffreAffaire());
        c.setAvoir(cashSale.getAvoir());
        c.setTypePrescription(cashSale.getTypePrescription());
        c.setSeller(cashSale.getSeller());
        c.setImported(false);
        c.setUser(cashSale.getUser());
        c.setLastUserEdit(this.storageService.getUser());
        c.setCaissier(cashSale.getCaissier());
        c.setCopy(false);
        c.setCreatedAt(cashSale.getCreatedAt());
        c.setUpdatedAt(cashSale.getUpdatedAt());
        c.setEffectiveUpdateDate(cashSale.getEffectiveUpdateDate());
        c.setPayrollAmount(cashSale.getPayrollAmount());
        c.setToIgnore(cashSale.isToIgnore());
        c.setDiffere(cashSale.isDiffere());
        c.setStatut(cashSale.getStatut());
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

    private void computePrixReference(List<ClientTiersPayant> clientTiersPayants, SalesLine salesLine) {
        List<OptionPrixProduit> optionPrixProduits = prixRererenceService.findByProduitIdAndTiersPayantIds(
            salesLine.getProduit().getId(),
            clientTiersPayants.stream().map(c -> c.getTiersPayant().getId()).collect(Collectors.toSet())
        );

        if (!CollectionUtils.isEmpty(optionPrixProduits)) {
            for (OptionPrixProduit optionPrixProduit : optionPrixProduits) {
                TiersPayantPrix tiersPayantPrix1 = new TiersPayantPrix();
                tiersPayantPrix1.setOptionPrixProduit(optionPrixProduit);
                tiersPayantPrix1.setSaleLine(salesLine);
                tiersPayantPrix1.setPrix(prixRererenceService.getSaleLineUnitPrice(optionPrixProduit, salesLine.getRegularUnitPrice()));
                tiersPayantPrix1.setMontant(tiersPayantPrix1.getPrix() * salesLine.getQuantityRequested());
                salesLine.getPrixAssurances().add(tiersPayantPrix1);
            }
        }
    }

    private void computePrixReference(SalesLine salesLine) {
        salesLine
            .getPrixAssurances()
            .forEach(prixAssurance -> {
                prixAssurance.setMontant(prixAssurance.getPrix() * salesLine.getQuantityRequested());
                this.tiersPayantPrixRepository.save(prixAssurance);
            });
    }

    private String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales, SalesLine salesLine, SalesLine oldsalesline) {
        computeSaleEagerAmount(thirdPartySales, salesLine.getSalesAmount(), oldsalesline != null ? oldsalesline.getSalesAmount() : 0);

        applRemiseToSale(thirdPartySales);
        var message = reComputeAmounts(thirdPartySales);
        upddateThirdPartySaleAmounts(thirdPartySales, salesLine, oldsalesline);
        return message;
    }

    private void upddateSaleAmountsOnRemovingItem(ThirdPartySales c, SalesLine saleLine) {
        computeSaleEagerAmount(c, saleLine.getSalesAmount() * (-1), 0);
        applRemiseToSale(c);
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

    private String computeAmounts(
        List<ClientTiersPayant> clttiersPayants,
        List<ClientTiersPayantDTO> tiersPayantsClts,
        ThirdPartySales thirdPartySales
    ) throws GenericError, NumBonAlreadyUseException {
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
        ClientTiersPayant clientTiersPayantPrincipal =
            this.clientTiersPayantRepository.getReferenceById(tiersPayantsClts.getFirst().getId());
        int totalAmountAssurance = getMontantAssurance(thirdPartySales, clientTiersPayantPrincipal.getTiersPayant());
        Map<Long, List<ClientTiersPayant>> tiersPayants = CollectionUtils.isEmpty(clientTiersPayants)
            ? this.clientTiersPayantRepository.findAllByIdIn(
                    clientTiersPayants.stream().map(ClientTiersPayantDTO::getId).collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.groupingBy(ClientTiersPayant::getId))
            : clttiersPayants.stream().collect(Collectors.groupingBy(ClientTiersPayant::getId));
        boolean hasOptionPrixPourcentage = thirdPartySales.hasOptionPrixPourcentage();
        for (ClientTiersPayantDTO tp : clientTiersPayants) {
            if (checkIfNumBonIsAlReadyUse(tp.getNumBon(), tp.getId())) {
                throw new NumBonAlreadyUseException(tp.getNumBon());
            }
            var isLast = counter == (tiersPayantSize - 1);
            ClientTiersPayant clientTiersPayant = tiersPayants.get(tp.getId()).getFirst();
            totalMontantTiersPayant += processTiersPayantAmount(
                totalMontantTiersPayant,
                sb,
                tp,
                thirdPartySales,
                isLast,
                isPourcentageGreather100,
                clientTiersPayant,
                totalAmountAssurance,
                hasOptionPrixPourcentage
            );
            counter++;
        }

        return updateTiersPayantAmounts(thirdPartySales, sb, totalMontantTiersPayant);
    }

    /*
    montant calculé pour l'assurance, quand il n'y a pas de prix assurance, on prend le prix de reference tu tp principal
     */
    private int getMontantAssurance(ThirdPartySales thirdPartySales, TiersPayant tiersPayant) {
        int amount = thirdPartySales
            .getSalesLines()
            .stream()
            .flatMap(e -> e.getPrixAssurances().stream())
            .filter(pr -> pr.getOptionPrixProduit().getTiersPayant().equals(tiersPayant))
            .mapToInt(TiersPayantPrix::getMontant)
            .sum();
        return amount > 0 ? amount : thirdPartySales.getSalesAmount();
    }

    private int processTiersPayantAmount(
        int totalMontantTiersPayant,
        StringBuilder sb,
        ClientTiersPayantDTO clientTiersPayantDTO,
        ThirdPartySales thirdPartySales,
        boolean isLast,
        boolean isPourcentageGreather100,
        ClientTiersPayant clientTiersPayant,
        int totalAmountAssurance,
        boolean hasOptionPrixPourcentage
    ) throws NumBonAlreadyUseException, PlafondVenteException {
        // si non null  nouvelle creation
        if (
            Objects.nonNull(clientTiersPayantDTO) &&
            checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(), clientTiersPayantDTO.getId())
        ) {
            throw new NumBonAlreadyUseException(clientTiersPayantDTO.getNumBon());
        }

        TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();

        double montantTp = hasOptionPrixPourcentage ? totalAmountAssurance : totalAmountAssurance * clientTiersPayant.getTauxValue();
        int montantTiersPayant = hasOptionPrixPourcentage ? totalAmountAssurance : (int) Math.ceil(montantTp);
        int partTiersPayantnet = computeThirdPartyPart(clientTiersPayant, montantTiersPayant);
        if (montantTiersPayant != partTiersPayantnet) {
            sb
                .append(tiersPayant.getFullName())
                .append(", ne peut prendre en compte : ")
                .append(NumberUtil.formatToString(partTiersPayantnet))
                .append(" , car votre plafond est atteint \n");
        }
        if (isPourcentageGreather100 && isLast) {
            int rest = totalAmountAssurance - totalMontantTiersPayant;
            if (rest <= partTiersPayantnet) {
                partTiersPayantnet = rest;
            }
        }
        int newTaux;
        if (montantTiersPayant == partTiersPayantnet) {
            newTaux = clientTiersPayant.getTaux();
        } else {
            newTaux = (int) Math.ceil(((double) partTiersPayantnet * 100) / totalAmountAssurance);
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
        thirdPartySaleLine.setTaux((short) newTaux);
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
        int totalAmountAssurance = getMontantAssurance(
            thirdPartySales,
            thirdPartySaleLines.getFirst().getClientTiersPayant().getTiersPayant()
        );
        boolean hasOptionPrixPourcentage = thirdPartySales.hasOptionPrixPourcentage();
        for (ThirdPartySaleLine tp : thirdPartySaleLines) {
            var isLast = counter == (tiersPayantSize - 1);
            ClientTiersPayant clientTiersPayant = tp.getClientTiersPayant();
            totalMontantTiersPayant += processTiersPayantAmount(
                totalMontantTiersPayant,
                sb,
                null,
                thirdPartySales,
                isLast,
                isPourcentageGreather100,
                clientTiersPayant,
                totalAmountAssurance,
                hasOptionPrixPourcentage
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
                    Math.max(
                        (thirdPartySales.getSalesAmount() - thirdPartySales.getPartTiersPayant()) - thirdPartySales.getDiscountAmount(),
                        0
                    )
                );
            }
            case NatureVente.CARNET -> {
                thirdPartySales.setPartTiersPayant(Math.max(totalMontantTiersPayant - thirdPartySales.getDiscountAmount(), 0));
                thirdPartySales.setPartAssure(Math.max(thirdPartySales.getNetAmount() - thirdPartySales.getPartTiersPayant(), 0));
            }
            case COMPTANT -> throw new RuntimeException("Not yet implemented");
        }

        thirdPartySales.setAmountToBePaid(roundedAmount(thirdPartySales.getPartAssure()));
        return sb.toString();
    }

    private int computeThirdPartyPart(ClientTiersPayant clientTiersPayant, int partTiersPayantNet) {
        TiersPayant tiersPayant = clientTiersPayant.getTiersPayant();
        return computeTiersPayantTauxWithPlafondConso(tiersPayant, clientTiersPayant.getConsoMensuelle(), partTiersPayantNet);
    }

    private int computeTiersPayantTauxWithPlafondConso(TiersPayant tiersPayant, Long consoMensuelle, int partTiersPayantNet) {
        int finalTpAmount = computePlafondTiersPayant(tiersPayant.getPlafondConso(), tiersPayant.getConsoMensuelle(), partTiersPayantNet);

        return computePlafondClient(
            tiersPayant.getPlafondJournalierClient(),
            tiersPayant.getPlafondConsoClient(),
            consoMensuelle,
            finalTpAmount
        );
    }

    private int computePlafondClient(Integer plafondJournalier, Integer plafonConso, Long consoMensuelle, int partTiersPayantNet) {
        int totalNetAmount = computeConsommationMensuelle(consoMensuelle, plafonConso, partTiersPayantNet);
        return computePlafondJournalier(plafondJournalier, totalNetAmount);
    }

    private int computePlafondJournalier(Integer plafondJournalier, int totalNetAmount) {
        if (plafondJournalier == null) {
            return totalNetAmount;
        }
        if (totalNetAmount <= plafondJournalier) {
            return totalNetAmount;
        }
        return plafondJournalier;
    }

    private int computeConsommationMensuelle(Long consoMensuelle, Integer plafondConso, int totalNetAmount) {
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
        String message = computeAmounts(null, buildFromCustomer(assuredCustomer), thirdPartySales);
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
        ThirdPartySales p = thirdPartySaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        this.editSale(p, dto);
        paymentService.buildPaymentFromFromPaymentDTO(p, dto);
        p.setTvaEmbeded(buildTvaData(p.getSalesLines()));
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
    public void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO) throws PrivilegeException {
        this.utilisationCleSecuriteService.authorizeAction(utilisationCleSecuriteDTO, ThirdPartySaleService.class);
    }

    @Override
    public void processDiscount(KeyValue keyValue) {
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(keyValue.key());
        remiseRepository.findById(keyValue.value()).ifPresent(remise -> processDiscount(thirdPartySales, remise));
        this.displayNet(thirdPartySales.getPartAssure());
    }

    @Override
    public void updateCustomerInformation(UpdateSale updateSale) throws InvalidPhoneNumberException, GenericError, JsonProcessingException {
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(updateSale.id());
        AssuredCustomer assuredCustomer = (AssuredCustomer) thirdPartySales.getCustomer();

        AssuredCustomer ayantDroit = thirdPartySales.getAyantDroit();
        List<ThirdPartySaleLine> thirdPartySaleLines = thirdPartySales.getThirdPartySaleLines();
        Set<ThirdPartySaleLineDTO> thirdPartySaleLineNews = updateSale.thirdPartySaleLines();
        if (thirdPartySaleLines.stream().anyMatch(e -> nonNull(e.getFactureTiersPayant()))) {
            throw new GenericError("La vente est déjà facturée");
        }
        int oldTaux = thirdPartySaleLines.stream().mapToInt(ThirdPartySaleLine::getTaux).sum();
        int newTaux = thirdPartySaleLineNews.stream().mapToInt(ThirdPartySaleLineDTO::getTaux).sum();
        if (oldTaux != newTaux) {
            throw new GenericError(String.format("Les taux sont différents:  Ancien taux : %d ,  Nouveau taux:%d", oldTaux, newTaux));
        }

        if (!isSameCustomer(assuredCustomer, updateSale.customer())) {
            assuredCustomer = this.assuredCustomerRepository.getReferenceById(updateSale.customer().getId());
            thirdPartySales.setCustomer(assuredCustomer);
        }
        updateAssuredCustomer(assuredCustomer, updateSale.customer());
        if (nonNull(ayantDroit) && nonNull(updateSale.ayantDroit())) {
            if (!isSameCustomer(ayantDroit, updateSale.ayantDroit())) {
                ayantDroit = this.assuredCustomerRepository.getReferenceById(updateSale.ayantDroit().getId());
                thirdPartySales.setAyantDroit(ayantDroit);
            }
            updateAssuredCustomer(ayantDroit, updateSale.ayantDroit());
        }
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            ThirdPartySaleLineDTO thirdPartySaleLineDTO = thirdPartySaleLineNews
                .stream()
                .filter(e -> e.getId().equals(thirdPartySaleLine.getId()))
                .findFirst()
                .orElseThrow(() -> new GenericError("La ligne n'existe pas"));
            updateThirdPartySaleLine(thirdPartySaleLine, updateSale.customer(), thirdPartySaleLineDTO);
        });
        thirdPartySales.setLastUserEdit(this.storageService.getUser());
        thirdPartySaleRepository.save(thirdPartySales);
        ObjectMapper objectMapper = new ObjectMapper();
        this.logService.create(
                TransactionType.MODIFICATION_INFO_CLIENT,
                TransactionType.MODIFICATION_INFO_CLIENT.getValue(),
                thirdPartySales.getId().toString(),
                objectMapper.writeValueAsString(updateSale.initialValue()),
                objectMapper.writeValueAsString(updateSale.finalValue())
            );
    }

    private void updateThirdPartySaleLine(
        ThirdPartySaleLine thirdPartySaleLine,
        AssuredCustomerDTO assuredCustomerDTO,
        ThirdPartySaleLineDTO thirdPartySaleLineDTO
    ) {
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();

        if (clientTiersPayant.getId().compareTo(thirdPartySaleLineDTO.getClientTiersPayantId()) != 0) {
            clientTiersPayant = clientTiersPayantRepository.getReferenceById(thirdPartySaleLineDTO.getClientTiersPayantId());
            thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        }
        thirdPartySaleLine.setNumBon(thirdPartySaleLineDTO.getNumBon());
        if (clientTiersPayant.getPriorite() == PrioriteTiersPayant.R0 && !clientTiersPayant.getNum().equals(assuredCustomerDTO.getNum())) {
            clientTiersPayant.setNum(assuredCustomerDTO.getNum());
            clientTiersPayantRepository.save(clientTiersPayant);
        }
    }

    private void updateAssuredCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer) throws InvalidPhoneNumberException {
        if (isNull(customer)) return;

        assuredCustomer.setFirstName(customer.getFirstName());
        assuredCustomer.setLastName(customer.getLastName());
        if (StringUtils.hasText(customer.getPhone())) {
            if (!Util.isValidPhoneNumber(customer.getPhone())) {
                throw new InvalidPhoneNumberException();
            }
            assuredCustomer.setPhone(customer.getPhone());
        }
        if (StringUtils.hasLength(customer.getNumAyantDroit())) {
            assuredCustomer.setNumAyantDroit(customer.getNumAyantDroit());
        }
        this.assuredCustomerRepository.save(assuredCustomer);
    }

    private boolean isSameCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer) {
        return assuredCustomer.getId().compareTo(customer.getId()) == 0;
    }

    private void processDiscount(ThirdPartySales thirdPartySales, Remise remise) {
        if (thirdPartySales.getRemise() != null) {
            this.removeRemise(thirdPartySales);
        }
        if (remise instanceof RemiseClient remiseClient) {
            this.applyRemiseClient(thirdPartySales, remiseClient);
        } else {
            // if (thirdPartySales.getNatureVente() == NatureVente.CARNET) {
            this.applyRemiseProduit(thirdPartySales, (RemiseProduit) remise);
            thirdPartySales.setNetAmount(thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
            /*}
            else {
                throw new GenericError("La remise produit n'est pas applicable sur une vente assurance", "notYetImplemented");
            }*/
        }
        reComputeAmounts(thirdPartySales);
        this.thirdPartySaleRepository.save(thirdPartySales);
    }

    private void applRemiseToSale(ThirdPartySales thirdPartySales) {
        Remise remise = thirdPartySales.getRemise();
        if (remise == null) {
            return;
        }
        if (remise instanceof RemiseProduit && thirdPartySales.getNatureVente() == NatureVente.ASSURANCE) {
            throw new GenericError("La remise produit n'est pas applicable sur une vente assurance", "notYetImplemented");
        }
        this.proccessDiscount(thirdPartySales);
    }

    private void computePartAssure(ThirdPartySales thirdPartySales) {
        if (Objects.nonNull(thirdPartySales.getPartAssure()) && thirdPartySales.getPartAssure().compareTo(0) != 0) {
            thirdPartySales.setPartAssure(thirdPartySales.getPartAssure() - thirdPartySales.getDiscountAmount());
        }
    }

    private void computeOptionPrix(SalesLine salesLine) {
        salesLine
            .getPrixAssurances()
            .forEach(prixAssurance -> {
                prixAssurance.setPrix(
                    prixRererenceService.getSaleLineUnitPrice(prixAssurance.getOptionPrixProduit(), salesLine.getRegularUnitPrice())
                );
                prixAssurance.setMontant(prixAssurance.getPrix() * salesLine.getQuantityRequested());
                this.tiersPayantPrixRepository.save(prixAssurance);
            });
    }
}
