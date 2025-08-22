package com.kobe.warehouse.service.sale.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.Util;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
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
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
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
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.calculation.TiersPayantCalculationService;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.calculation.dto.SaleItemInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantLineOutput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantPrixInput;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.sale.dto.UpdateSale;
import com.kobe.warehouse.service.utils.AfficheurPosService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
    private final LogsService logService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneId.systemDefault());
    private final TiersPayantCalculationService tiersPayantCalculationService;

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
        CashRegisterService cashRegisterService,
              PosteRepository posteRepository,
        CashSaleRepository cashSaleRepository,
        UtilisationCleSecuriteService utilisationCleSecuriteService,
        RemiseRepository remiseRepository,
        AfficheurPosService afficheurPosService,
        PrixRererenceService prixRererenceService,
        LogsService logService,
        TiersPayantCalculationService tiersPayantCalculationService
    ) {
        super(
            referenceService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
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
        this.logService = logService;
        this.tiersPayantCalculationService = tiersPayantCalculationService;
    }

    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        SalesLine saleLine = salesLineService.createSaleLineFromDTO(
            dto.getSalesLines().getFirst(),
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        ThirdPartySales thirdPartySales = buildThirdPartySale(dto);
        thirdPartySales.getSalesLines().add(saleLine);
        computeSaleEagerAmount(thirdPartySales);

        applRemiseToSale(thirdPartySales);
        thirdPartySales.setOrigineVente(OrigineVente.DIRECT);
        thirdPartySales = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        saleLine.setSales(thirdPartySales);
        salesLineService.saveSalesLine(saleLine);
        String message = saveTiersPayantLines(dto, thirdPartySales, saleLine);
        this.displayNet(thirdPartySales.getPartAssure());
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
        return thirdPartySaleDTO;
    }

    private String saveTiersPayantLines(ThirdPartySaleDTO dto, ThirdPartySales thirdPartySales, SalesLine saleLine) {
        List<ClientTiersPayant> clientTiersPayants = getClientTiersPayants(dto.getTiersPayants().stream().map(ClientTiersPayantDTO::getId).collect(Collectors.toSet()));
        for (ClientTiersPayant clientTiersPayant : clientTiersPayants) {
            ClientTiersPayantDTO clientTiersPayantDTO = dto
                .getTiersPayants()
                .stream()
                .filter(ctpdto -> Objects.equals(ctpdto.getId(), clientTiersPayant.getId()))
                .findFirst()
                .orElseThrow(() -> new GenericError("Client tiers payant introuvable"));
            if (dto.getNumBon() != null) {
                if (checkIfNumBonIsAlReadyUse(dto.getNumBon(), clientTiersPayant.getId(), null)) {
                    throw new NumBonAlreadyUseException(dto.getNumBon());
                }

            }
            ThirdPartySaleLine thirdPartySaleLine = createThirdPartySaleLine(dto.getNumBon(), clientTiersPayant, 0);

            thirdPartySaleLine.setSale(thirdPartySales);
            thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
        }
        return upddateThirdPartySaleAmounts(thirdPartySales, true, clientTiersPayants);
    }

    private List<ClientTiersPayant> getClientTiersPayants(Set<Long> ids) {
        return clientTiersPayantRepository.findAllByIdIn(ids);
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
        if (montant == null) {
            throw new GenericError("Unexpected null value for montant in buildConsommation");
        }
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
        String numNon,
        ClientTiersPayant clientTiersPayant,
        int partTiersPayant
    ) {
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        thirdPartySaleLine.setCreated(LocalDateTime.now());
        thirdPartySaleLine.setUpdated(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setEffectiveUpdateDate(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setNumBon(numNon);
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

    @Override
    public List<ThirdPartySaleLine> findAllBySaleId(Long saleId) {
        return thirdPartySaleLineRepository.findAllBySaleId(saleId);
    }

    private boolean checkIfNumBonIsAlReadyUse(String numBon, Long clientTiersPayantId, Long currentSaleId) {
        if (!StringUtils.hasLength(numBon)) {
            return false;
        }
        if (isNull(currentSaleId)) return (
            thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
                numBon,
                clientTiersPayantId,
                SalesStatut.CLOSED
            ) >
                0
        );

        return (
            thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
                numBon,
                currentSaleId,
                clientTiersPayantId,
                SalesStatut.CLOSED
            ) >
                0
        );
    }

    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto) throws PlafondVenteException {
        Optional<SalesLine> salesLineOp = salesLineService.findBySalesIdAndProduitId(dto.getSaleId(), dto.getProduitId());
        long storageId = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
        ThirdPartySales thirdPartySales;
        if (salesLineOp.isPresent()) {
            SalesLine salesLine = salesLineOp.get();
            salesLineService.updateSaleLine(dto, salesLine, storageId);
            thirdPartySales = (ThirdPartySales) salesLine.getSales();
            updateAmounts(thirdPartySales);
            var message = reComputeAndApplyAmounts(thirdPartySales, null, true);
            // thirdPartySales = thirdPartySaleRepository.save(thirdPartySales);
            if (StringUtils.hasLength(message)) {
                throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
            }
            return new SaleLineDTO(salesLine);
        }
        thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getSaleId());
        SalesLine salesLine = salesLineService.create(dto, storageId, thirdPartySales);
        updateAmounts(thirdPartySales);
        var message = reComputeAndApplyAmounts(thirdPartySales, null, true);
        //   thirdPartySales = thirdPartySaleRepository.save(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
        }
        this.displayNet(thirdPartySales.getPartAssure());
        return new SaleLineDTO(salesLine);
    }

    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public void deleteSaleLineById(Long id) {
        SalesLine salesLine = salesLineService.getOneById(id);
        ThirdPartySales sales = (ThirdPartySales) salesLine.getSales();
        sales.removeSalesLine(salesLine);
        sales.setUpdatedAt(LocalDateTime.now());
        sales.setEffectiveUpdateDate(sales.getUpdatedAt());
        sales.setLastUserEdit(storageService.getUser());
        salesLineService.deleteSaleLine(salesLine);
        upddateSaleAmountsOnRemovingItem(sales);
        this.displayNet(sales.getPartAssure());
    }

    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
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
        //   thirdPartySales = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        this.displayNet(thirdPartySales.getPartAssure());
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
        }
        return new SaleLineDTO(salesLine);
    }

    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) throws PlafondVenteException {
        SalesLine salesLine = salesLineService.getOneById(saleLineDTO.getId());
        SalesLine oldsalesline = (SalesLine) salesLine.clone();
        salesLineService.updateItemRegularPrice(saleLineDTO, salesLine, storageService.getDefaultConnectedUserPointOfSaleStorage().getId());

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
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public FinalyseSaleDTO save(ThirdPartySaleDTO dto)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, NumBonAlreadyUseException {
        ThirdPartySales p = thirdPartySaleRepository.findOneWithEagerSalesLines(dto.getId()).orElseThrow();
        this.save(p, dto);
        FinalyseSaleDTO response = finalizeSaleProcess(p, dto);
        displayMonnaie(dto.getMontantRendu());
        return response;
    }

    private FinalyseSaleDTO finalizeSaleProcess(ThirdPartySales p, ThirdPartySaleDTO dto) throws NumBonAlreadyUseException {
        p.setTvaEmbeded(buildTvaData(p.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(p, dto);
        List<ThirdPartySaleLine> thirdPartySaleLines = findAllBySaleId(p.getId());
        if (thirdPartySaleLines.isEmpty() && dto.getTiersPayants().isEmpty()) {
            throw new ThirdPartySalesTiersPayantException();
        }
        Map<Long, String> numBonMap = dto
            .getTiersPayants()
            .stream()
            .collect(Collectors.toMap(ClientTiersPayantDTO::getId, ClientTiersPayantDTO::getNumBon, (_, b) -> b));

        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
            String numBon = numBonMap.get(clientTiersPayant.getId());
            if (numBon != null) {
                if (checkIfNumBonIsAlReadyUse(numBon, clientTiersPayant.getId(), p.getId())) {
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
    @Transactional(noRollbackFor = {PlafondVenteException.class})
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
    @Transactional(noRollbackFor = {PlafondVenteException.class})
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
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public void addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, Long saleId)
        throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        ClientTiersPayant clientTiersPayant = clientTiersPayantRepository.getReferenceById(dto.getId());
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(saleId);
        if (checkIfNumBonIsAlReadyUse(dto.getNumBon(), clientTiersPayant.getId(), null)) {
            throw new NumBonAlreadyUseException(dto.getNumBon());
        }
        ThirdPartySaleLine thirdPartySaleLine = createThirdPartySaleLine(dto.getNumBon(), clientTiersPayant, 0);
        thirdPartySaleLine.setSale(thirdPartySales);
        thirdPartySaleLineRepository.save(thirdPartySaleLine);
        thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
        applRemiseToSale(thirdPartySales);
        String message = reComputeAndApplyAmounts(thirdPartySales, null, true);
        //  var tp = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        this.displayNet(thirdPartySales.getPartAssure());
        if (StringUtils.hasLength(message)) {
            ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
    }

    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public void removeThirdPartySaleLineToSales(Long clientTiersPayantId, Long saleId) throws PlafondVenteException {
        thirdPartySaleLineRepository
            .findFirstByClientTiersPayantIdAndSaleId(clientTiersPayantId, saleId)
            .ifPresent(thirdPartySaleLine -> {
                ThirdPartySales thirdPartySales = thirdPartySaleLine.getSale();
                thirdPartySales.setLastUserEdit(storageService.getUser());
                thirdPartySaleLine.setSale(null);
                thirdPartySales.getThirdPartySaleLines().remove(thirdPartySaleLine);
                thirdPartySaleLineRepository.delete(thirdPartySaleLine);
                reComputeAndApplyAmounts(thirdPartySales, null, true);

                //  thirdPartySaleRepository.saveAndFlush(thirdPartySales);
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
    @Transactional(noRollbackFor = {PlafondVenteException.class})
    public void updateTransformedSale(ThirdPartySaleDTO dto) throws PlafondVenteException {
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(dto.getId());
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(dto.getCustomerId());
        thirdPartySales.setCustomer(assuredCustomer);
        thirdPartySales.setLastUserEdit(storageService.getUser());
        thirdPartySales.setAyantDroit(assuredCustomer);
        thirdPartySales.setUpdatedAt(LocalDateTime.now());
        thirdPartySales.setEffectiveUpdateDate(thirdPartySales.getUpdatedAt());
        getAyantDroitFromId(dto.getAyantDroitId()).ifPresent(thirdPartySales::setAyantDroit);
        String message = reComputeAndApplyAmounts(thirdPartySales, null, true);
        if (StringUtils.hasLength(message)) {
            ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
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
        c.setCaisse(cashSale.getCaisse());
        c.setLastCaisse(cashSale.getLastCaisse());
        c.setPaymentStatus(cashSale.getPaymentStatus());
        c.setMagasin(cashSale.getMagasin());
        return c;
    }

    private String upddateThirdPartySaleAmounts(ThirdPartySales c, boolean isUpdate, List<ClientTiersPayant> clientTiersPayants) {
        updateAmounts(c);
        return reComputeAndApplyAmounts(c, clientTiersPayants, isUpdate);
    }

    private String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales, SalesLine salesLine, SalesLine oldsalesline) {
        computeSaleEagerAmount(thirdPartySales);

        applRemiseToSale(thirdPartySales);
        //  var message = reComputeAmounts(thirdPartySales);
        return upddateThirdPartySaleAmounts(thirdPartySales, true, null);

    }

    private void upddateSaleAmountsOnRemovingItem(ThirdPartySales c) {
        computeSaleEagerAmount(c);
        applRemiseToSale(c);
        reComputeAndApplyAmounts(c, null, true);
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


    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
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
        String message = saveTiersPayantLinesOnChangeCustomer(thirdPartySales);
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
    }


    private String saveTiersPayantLinesOnChangeCustomer(ThirdPartySales thirdPartySales) {
        List<ClientTiersPayant> clientTiersPayants = ((AssuredCustomer) thirdPartySales.getCustomer()).getClientTiersPayants().stream().sorted(Comparator.comparingInt(c -> c.getPriorite().getValue())).collect(Collectors.toList());
        for (ClientTiersPayant clientTiersPayant : clientTiersPayants) {
            ThirdPartySaleLine thirdPartySaleLine = createThirdPartySaleLine(null, clientTiersPayant, 0);
            thirdPartySaleLine.setSale(thirdPartySales);
            thirdPartySales.getThirdPartySaleLines().add(thirdPartySaleLine);
        }
        return reComputeAndApplyAmounts(thirdPartySales, clientTiersPayants, true);
    }

    @Override
    @Transactional(noRollbackFor = {PlafondVenteException.class})
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
                    if (checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(), thirdPartySaleLine.getId(), p.getId())) {
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
        reComputeAndApplyAmounts(thirdPartySales, null, true);

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


    private String reComputeAndApplyAmounts(ThirdPartySales thirdPartySales, List<ClientTiersPayant> clientTiersPayants, boolean isUpdate) {
        // CalculationInput input = buildCalculationInput(thirdPartySales);
        if (CollectionUtils.isEmpty(clientTiersPayants)) {
            clientTiersPayants = thirdPartySales.getThirdPartySaleLines()
                .stream()
                .map(ThirdPartySaleLine::getClientTiersPayant)
                .collect(Collectors.toList());
        }
        CalculationInput input = buildCalculationInput(thirdPartySales, clientTiersPayants);
        CalculationResult output = tiersPayantCalculationService.calculate(input);
        int totalPatientShare = output.getTotalPatientShare().intValue();
        thirdPartySales.setPartTiersPayant(output.getTotalTiersPayant().intValue());
        thirdPartySales.setPartAssure(totalPatientShare);
        thirdPartySales.setAmountToBePaid(roundedAmount(totalPatientShare));

        for (TiersPayantLineOutput lineResult : output.getTiersPayantLines()) {
            findSaleLineByClientTiersPayantId(thirdPartySales, lineResult.getClientTiersPayantId())
                .ifPresent(saleLine -> {
                    saleLine.setMontant(lineResult.getMontant().intValue());
                    saleLine.setTaux((short) lineResult.getFinalTaux());
                    if (isUpdate) {
                        this.thirdPartySaleLineRepository.save(saleLine);
                    }

                });
        }
        for (SalesLine saleLine : thirdPartySales.getSalesLines()) {
            output.getItemShares().stream().filter(s -> s.getSaleLineId().equals(saleLine.getId()))
                .findFirst()
                .ifPresent(itemShare -> {
                    saleLine.setCalculationBasePrice(itemShare.getCalculationBasePrice());
                    saleLine.setRates(itemShare.getRates());
                    if (isUpdate) {
                        this.salesLineService.saveSalesLine(saleLine);
                    }


                });
        }
        if (isUpdate) {
            thirdPartySales = this.thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        }

        return output.getWarningMessage();
    }

    private List<SaleItemInput> buildSaleItemInputs(ThirdPartySales sale, CalculationInput input, Set<Long> tiersPayantIds, List<TiersPayantInput> tiersPayantInputs) {
        return sale.getSalesLines().stream().map(sl -> {
            SaleItemInput si = new SaleItemInput();
            Produit produit = sl.getProduit();
            si.setSalesLineId(sl.getId());
            si.setTotalSalesAmount(BigDecimal.valueOf(sl.getSalesAmount()));
            si.setQuantity(sl.getQuantityRequested());
            si.setRegularUnitPrice(BigDecimal.valueOf(sl.getRegularUnitPrice()));
            input.setTotalSalesAmount(Objects.requireNonNullElse(input.getTotalSalesAmount(), BigDecimal.ZERO).add(si.getTotalSalesAmount()));
            this.prixRererenceService.findByProduitIdAndTiersPayantIds(produit.getId(), tiersPayantIds).forEach(prixRef ->
                tiersPayantInputs.forEach(cl -> {
                    if (cl.getTiersPayantId().compareTo(prixRef.getTiersPayant().getId()) == 0) {
                        TiersPayantPrixInput pi = new TiersPayantPrixInput();
                        pi.setCompteTiersPayantId(cl.getClientTiersPayantId());
                        pi.setPrice(prixRef.getPrice());
                        pi.setRate(prixRef.getRate());
                        pi.setOptionPrixType(prixRef.getType());
                        si.getPrixAssurances().add(pi);
                    }

                })
            );
            return si;
        }).collect(Collectors.toList());
    }

    private CalculationInput buildCalculationInput(ThirdPartySales sale, List<ClientTiersPayant> clientTiersPayants) {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(sale.getNatureVente());
        input.setDiscountAmount(BigDecimal.valueOf(sale.getDiscountAmount()));

        Set<Long> tiersPayantIds = new HashSet<>();
        List<TiersPayantInput> tiersPayantInputs = buildTiersPayantInputs(clientTiersPayants, tiersPayantIds);
        input.setTiersPayants(tiersPayantInputs);

        List<SaleItemInput> saleItemInputs = buildSaleItemInputs(sale, input, tiersPayantIds, tiersPayantInputs);
        input.setSaleItems(saleItemInputs);

        return input;
    }

    private List<TiersPayantInput> buildTiersPayantInputs(List<ClientTiersPayant> clientTiersPayants, Set<Long> tiersPayantIds) {
        if (CollectionUtils.isEmpty(clientTiersPayants)) {
            return Collections.emptyList();
        }
        return clientTiersPayants.stream().
            map(ctp -> {
                TiersPayantInput ti = new TiersPayantInput();
                TiersPayant tiersPayant = ctp.getTiersPayant();
                tiersPayantIds.add(tiersPayant.getId());
                ti.setClientTiersPayantId(ctp.getId());
                ti.setTiersPayantId(tiersPayant.getId());
                ti.setTiersPayantFullName(tiersPayant.getFullName());
                ti.setTaux(ctp.getTaux() / 100.0f);
                ti.setPriorite(ctp.getPriorite());
                Optional.ofNullable(tiersPayant.getPlafondConso()).ifPresent(v -> ti.setPlafondConso(BigDecimal.valueOf(v)));
                Optional.ofNullable(ctp.getConsoMensuelle()).ifPresent(v -> ti.setConsoMensuelle(BigDecimal.valueOf(v)));
                Optional.ofNullable(tiersPayant.getPlafondJournalierClient()).ifPresent(v -> ti.setPlafondJournalierClient(BigDecimal.valueOf(v)));
                return ti;
            }).collect(Collectors.toList());

    }

    private CalculationInput buildCalculationInput(ThirdPartySales sale) {
        CalculationInput input = new CalculationInput();
        input.setNatureVente(sale.getNatureVente());
        input.setDiscountAmount(BigDecimal.valueOf(sale.getDiscountAmount()));

        Set<Long> tiersPayantIds = new HashSet<>();
        List<TiersPayantInput> tiersPayantInputs = sale.getThirdPartySaleLines().stream()
            .map(ThirdPartySaleLine::getClientTiersPayant)
            .map(ctp -> {
                TiersPayantInput ti = new TiersPayantInput();
                TiersPayant tiersPayant = ctp.getTiersPayant();
                tiersPayantIds.add(tiersPayant.getId());
                ti.setClientTiersPayantId(ctp.getId());
                ti.setTiersPayantId(tiersPayant.getId());
                ti.setTiersPayantFullName(tiersPayant.getFullName());
                ti.setTaux(ctp.getTaux());
                ti.setPriorite(ctp.getPriorite());
                Optional.ofNullable(tiersPayant.getPlafondConso()).ifPresent(v -> ti.setPlafondConso(BigDecimal.valueOf(v)));
                Optional.ofNullable(ctp.getConsoMensuelle()).ifPresent(v -> ti.setConsoMensuelle(BigDecimal.valueOf(v)));
                Optional.ofNullable(tiersPayant.getPlafondJournalierClient()).ifPresent(v -> ti.setPlafondJournalierClient(BigDecimal.valueOf(v)));
                return ti;
            }).collect(Collectors.toList());
        input.setTiersPayants(tiersPayantInputs);

        List<SaleItemInput> saleItemInputs = buildSaleItemInputs(sale, input, tiersPayantIds, tiersPayantInputs);
        input.setSaleItems(saleItemInputs);

        return input;
    }

    private Optional<ThirdPartySaleLine> findSaleLineByClientTiersPayantId(ThirdPartySales sale, Long clientTiersPayantId) {
        return sale.getThirdPartySaleLines().stream()
            .filter(line -> line.getClientTiersPayant().getId().equals(clientTiersPayantId))
            .findFirst();
    }


}
