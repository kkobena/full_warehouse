package com.kobe.warehouse.service.sale.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.CashRegisterException;
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
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.AssuredCustomerManager;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SalesManager;
import com.kobe.warehouse.service.sale.ThirdPartyCalculationManager;
import com.kobe.warehouse.service.sale.ThirdPartyClientManager;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.sale.dto.UpdateSale;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@Transactional(noRollbackFor = {PlafondVenteException.class})
public class ThirdPartySaleServiceImpl extends SaleCommonService implements ThirdPartySaleService {

    private final ThirdPartySaleLineService thirdPartySaleLineService;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final StorageService storageService;
    private final ThirdPartySaleRepository thirdPartySaleRepository;
    private final AssuredCustomerRepository assuredCustomerRepository;
    private final PaymentService paymentService;
    private final CashSaleRepository cashSaleRepository;
    private final UtilisationCleSecuriteService utilisationCleSecuriteService;
    private final RemiseRepository remiseRepository;
    private final LogsService logService;

    private final ObjectMapper objectMapper;
    private final SalesManager salesManager;
    private final SalesLineService salesLineService;

    // Nouveaux services dédiés (Phase 2)
    private final ThirdPartyClientManager thirdPartyClientManager;
    private final ThirdPartyCalculationManager thirdPartyCalculationManager;
    private final AssuredCustomerManager assuredCustomerManager;

    public ThirdPartySaleServiceImpl(ThirdPartySaleLineService thirdPartySaleLineService,
                                     ClientTiersPayantRepository clientTiersPayantRepository,
                                     SaleLineServiceFactory saleLineServiceFactory, StorageService storageService,
                                     ThirdPartySaleRepository thirdPartySaleRepository,
                                     AssuredCustomerRepository assuredCustomerRepository, UserRepository userRepository,
                                     PaymentService paymentService, ReferenceService referenceService,
                                     CashRegisterService cashRegisterService, PosteRepository posteRepository,
                                     CashSaleRepository cashSaleRepository,
                                     UtilisationCleSecuriteService utilisationCleSecuriteService,
                                     RemiseRepository remiseRepository, CustomerDisplayService afficheurPosService,
                                     LogsService logService, SaleIdGeneratorService idGeneratorService,
                                     ObjectMapper objectMapper, SalesManager salesManager,
                                     ThirdPartyClientManager thirdPartyClientManager,
                                     ThirdPartyCalculationManager thirdPartyCalculationManager,
                                     AssuredCustomerManager assuredCustomerManager) {
        super(referenceService, storageService, userRepository, saleLineServiceFactory,
            cashRegisterService, posteRepository, afficheurPosService, idGeneratorService,
            objectMapper);
        this.thirdPartySaleLineService = thirdPartySaleLineService;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.salesLineService = saleLineServiceFactory.getService(TypeVente.ThirdPartySales);
        this.storageService = storageService;
        this.thirdPartySaleRepository = thirdPartySaleRepository;
        this.assuredCustomerRepository = assuredCustomerRepository;
        this.paymentService = paymentService;
        this.cashSaleRepository = cashSaleRepository;
        this.utilisationCleSecuriteService = utilisationCleSecuriteService;
        this.remiseRepository = remiseRepository;
        this.logService = logService;
        this.objectMapper = objectMapper;
        this.salesManager = salesManager;

        // Nouveaux services dédiés (Phase 2)
        this.thirdPartyClientManager = thirdPartyClientManager;
        this.thirdPartyCalculationManager = thirdPartyCalculationManager;
        this.assuredCustomerManager = assuredCustomerManager;
    }

    @Override
    public ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto)
        throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        SalesLine saleLine = salesLineService.createSaleLineFromDTO(dto.getSalesLines().getFirst(),
            storageService.getDefaultConnectedUserMainStorage().getId());
        ThirdPartySales thirdPartySales = buildThirdPartySale(dto);
        thirdPartySales.getSalesLines().add(saleLine);
        computeSaleEagerAmount(thirdPartySales);

        applRemiseToSale(thirdPartySales);
        thirdPartySales = thirdPartySaleRepository.saveAndFlush(thirdPartySales);
        saleLine.setSales(thirdPartySales);
        salesLineService.saveSalesLine(saleLine);
        String message = saveTiersPayantLines(dto, thirdPartySales);
        this.displayNet(thirdPartySales.getPartAssure());
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
        if (StringUtils.hasLength(message)) {
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
        return thirdPartySaleDTO;
    }

    private String saveTiersPayantLines(ThirdPartySaleDTO dto, ThirdPartySales thirdPartySales) {
        return thirdPartyClientManager.saveTiersPayantLines(dto, thirdPartySales);
    }

    @Override
    public ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy) {
        return thirdPartyClientManager.clone(original, copy);
    }

    @Override
    public void copyThirdPartySales(ThirdPartySales sales, ThirdPartySales copy) {
        copy.setPartAssure(sales.getPartAssure() * (-1));
        copy.setPartTiersPayant(sales.getPartTiersPayant() * (-1));
        copy.getThirdPartySaleLines().clear();
    }

    @Override
    public void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine) {
        thirdPartyClientManager.updateClientTiersPayantAccount(thirdPartySaleLine);
    }

    @Override
    public void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine) {
        thirdPartyClientManager.updateTiersPayantAccount(thirdPartySaleLine);
    }


    @Override
    public List<ThirdPartySaleLine> findAllBySaleId(SaleId saleId) {
        return thirdPartyClientManager.findAllBySaleId(saleId);
    }

    private boolean checkIfNumBonIsAlReadyUse(String numBon, Integer clientTiersPayantId,
                                              Long currentSaleId) {
        return thirdPartyClientManager.checkIfNumBonIsAlReadyUse(numBon, clientTiersPayantId,
            currentSaleId);
    }

    @Override
    public SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto) throws PlafondVenteException {
        return salesManager.addOrUpdateSaleLine(dto, findById(dto.getSaleCompositeId()));
    }

    @Override

    public void deleteSaleLineById(SaleLineId id) {
        salesManager.deleteSaleLineById(salesLineService.getOneById(id));
    }


    @Override
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO, boolean increment)
        throws StockException, DeconditionnementStockOut, PlafondVenteException {
        return salesManager.updateItemQuantityRequested(saleLineDTO,
            findById(saleLineDTO.getSaleCompositeId()), increment);
    }

    @Override
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO)
        throws PlafondVenteException {
        return salesManager.updateItemRegularPrice(saleLineDTO,
            findById(saleLineDTO.getSaleCompositeId()));
    }

    @Override
    public void cancelSale(SaleId id) throws CashRegisterException {
        AppUser user = storageService.getUser();
        thirdPartySaleRepository.findByIdAndSaleDate(id.getId(), id.getSaleDate())
            .ifPresent(sales -> {
                if (sales.isCanceled()) {
                    throw new GenericError("La vente est déjà annulée");
                }
                if (sales.getStatut() != SalesStatut.CLOSED) {
                    throw new GenericError("La vente doit être clôturée pour être modifiée");
                }

                cancelSale(new ArrayList<>(new LinkedHashSet<>(sales.getThirdPartySaleLines())),
                    new HashSet<>(sales.getSalesLines()), new HashSet<>(sales.getPayments()), sales,
                    user);
            });
    }


    private SaleId cloneSale(ThirdPartySales sales, boolean canceledOriginal,
                             SalesStatut salesStatut) throws CashRegisterException {
        List<ThirdPartySaleLine> originalThirdPartySaleLines = new ArrayList<>(new LinkedHashSet<>(
            sales.getThirdPartySaleLines()));// Utiliser LinkedHashSet pour préserver l'ordre des lignes et éviter les doublons
        Set<SalesLine> originalSalesLines = new HashSet<>(sales.getSalesLines());
        Set<SalePayment> originalPayments = new HashSet<>(sales.getPayments());

        ThirdPartySales copy = (ThirdPartySales) sales.clone();
        copy.setThirdPartySaleLines(new ArrayList<>());
        copy.setSalesLines(new HashSet<>());
        copy.setPayments(new HashSet<>());

        copySaleCommon(copy, salesStatut);
        if (canceledOriginal) {
            copyOrigin(sales, copy);
            copy.setCashRegister(getCashRegister());
        }

        // Sauvegarder copy SANS collections pour éviter le dirty checking Hibernate
        copy = thirdPartySaleRepository.saveAndFlush(copy);

        // Créer et sauvegarder les collections indépendamment avec la copie persistée

        paymentService.saveAll(paymentService.clonePayments(originalPayments, copy));

        salesLineService.saveAll(salesLineService.cloneSalesLine(originalSalesLines, copy));

        thirdPartyClientManager.saveAll(
            thirdPartyClientManager.clone(originalThirdPartySaleLines, copy));

        thirdPartySaleRepository.flush();
        if (canceledOriginal) {
            cancelSale(originalThirdPartySaleLines, originalSalesLines, originalPayments, sales,
                copy.getUser());
        }

        return copy.getId();
    }

    private void cancelSale(List<ThirdPartySaleLine> originalThirdPartySaleLines,
                            Set<SalesLine> originalSalesLines, Set<SalePayment> originalPayments, ThirdPartySales sales,
                            AppUser user) throws CashRegisterException {
        checkOpenningCaisse();
        ThirdPartySales copy = (ThirdPartySales) sales.clone();
        copy.setThirdPartySaleLines(new ArrayList<>());
        copy.setSalesLines(new HashSet<>());
        copy.setPayments(new HashSet<>());

        copySale(sales, copy);
        copy.setSaleDate(LocalDate.now());
        copyThirdPartySales(sales, copy);
        sales.setEffectiveUpdateDate(sales.getUpdatedAt());
        sales.setCanceled(true);
        copy.setCanceled(true);
        thirdPartySaleRepository.save(sales);
        thirdPartySaleRepository.save(copy);
        originalPayments.forEach(payment -> paymentService.clonePayment(payment, copy));
        salesLineService.cloneSalesLine(originalSalesLines, copy, user,
            storageService.getDefaultConnectedUserMainStorage().getId());
        originalThirdPartySaleLines.forEach(thirdPartySaleLine -> {
            ThirdPartySaleLine thirdPartySaleLineClone = clone(thirdPartySaleLine, copy);
            updateClientTiersPayantAccount(thirdPartySaleLineClone);
            updateTiersPayantAccount(thirdPartySaleLineClone);
        });
    }

    @Override
    public FinalyseSaleDTO save(ThirdPartySaleDTO dto)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, NumBonAlreadyUseException {
        ThirdPartySales p = thirdPartySaleRepository.findOneWithEagerSalesLines(
            dto.getSaleId().getId(), dto.getSaleId().getSaleDate()).orElseThrow();
        this.save(p, dto);
        FinalyseSaleDTO response = finalizeSaleProcess(p, dto);
        displayMonnaie(dto.getMontantRendu());
        return response;
    }

    private FinalyseSaleDTO finalizeSaleProcess(ThirdPartySales p, ThirdPartySaleDTO dto)
        throws NumBonAlreadyUseException {
        p.setTvaEmbeded(buildTvaData(p.getSalesLines()));
        paymentService.buildPaymentFromFromPaymentDTO(p, dto);
        List<ThirdPartySaleLine> thirdPartySaleLines = findAllBySaleId(p.getId());
        if (CollectionUtils.isEmpty(thirdPartySaleLines) || CollectionUtils.isEmpty(
            dto.getTiersPayants())) {
            throw new ThirdPartySalesTiersPayantException();
        }

        Map<Integer, List<String>> numBonMap = dto.getTiersPayants().stream().collect(
            Collectors.groupingBy(ClientTiersPayantDTO::getId,
                Collectors.mapping(ClientTiersPayantDTO::getNumBon, Collectors.toList())));

        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
            String numBon = numBonMap.get(clientTiersPayant.getId()).getFirst();
            if (numBon != null) {
                if (checkIfNumBonIsAlReadyUse(numBon, clientTiersPayant.getId(),
                    p.getId().getId())) {
                    throw new NumBonAlreadyUseException(numBon);
                }
                thirdPartySaleLine.setNumBon(numBon);
            }
            updateClientTiersPayantAccount(thirdPartySaleLine);
            updateTiersPayantAccount(thirdPartySaleLine);
        });
        new ArrayList<>(p.getThirdPartySaleLines()).stream()
            .min(Comparator.comparing(e -> e.getClientTiersPayant().getPriorite().getValue()))
            .ifPresent(o -> p.setNumBon(o.getNumBon()));
        thirdPartySaleRepository.save(p);
        return new FinalyseSaleDTO(p.getId(), true);
    }

    @Override
    public ResponseDTO putThirdPartySaleOnHold(ThirdPartySaleDTO dto) {
        ResponseDTO response = new ResponseDTO();
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.findOneById(dto.getId());
        if (CollectionUtils.isEmpty(thirdPartySales.getSalesLines())) {
            response.setSuccess(true);
            thirdPartySaleRepository.delete(thirdPartySales);
            return response;
        }
        //  paymentService.buildPaymentFromFromPaymentDTO(thirdPartySales, dto, storageService.getUser());
        thirdPartySaleRepository.save(thirdPartySales);
        response.setSuccess(true);
        return response;
    }

    @Override
    public void updateDate(ThirdPartySaleDTO dto) {
        ThirdPartySales sales = this.thirdPartySaleRepository.getReferenceById(dto.getSaleId());
        this.logService.create(TransactionType.MODIFICATION_DATE_DE_VENTE,
            TransactionType.MODIFICATION_DATE_DE_VENTE.getValue(), sales.getId().getId().toString(),
            sales.getUpdatedAt().toString(), dto.getUpdatedAt().toString());
        sales.setCreatedAt(dto.getUpdatedAt());
        sales.setUpdatedAt(sales.getCreatedAt());
        sales.getThirdPartySaleLines().forEach(thirdPartySaleLine -> {
            thirdPartySaleLine.setUpdated(sales.getUpdatedAt());
            thirdPartySaleLine.setCreated(sales.getUpdatedAt());
            thirdPartySaleLineService.save(thirdPartySaleLine);
        });
        thirdPartySaleRepository.save(sales);
    }

    @Override
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        return salesManager.updateItemQuantitySold(saleLineDTO,
            findById(saleLineDTO.getSaleCompositeId()));
    }

    @Override
    public void deleteSalePrevente(SaleId id) {
        thirdPartySaleRepository.findOneWithEagerSalesLines(id.getId(), id.getSaleDate())
            .ifPresent(sales -> {
                paymentService.findAllBySales(sales.getId()).forEach(paymentService::delete);
                sales.getSalesLines().forEach(salesLineService::deleteSaleLine);
                thirdPartySaleRepository.delete(sales);
            });
    }

    @Override
    public void addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, SaleId saleId)
        throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        ThirdPartySales thirdPartySales = findById(saleId);
        applRemiseToSale(thirdPartySales);

        String message = thirdPartyClientManager.addThirdPartySaleLineToSales(dto, saleId);
        this.displayNet(thirdPartySales.getPartAssure());

        if (StringUtils.hasLength(message)) {
            ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
    }

    @Override
    public void removeThirdPartySaleLineToSales(Integer clientTiersPayantId, SaleId saleId)
        throws PlafondVenteException {
        thirdPartyClientManager.removeThirdPartySaleLineToSales(clientTiersPayantId, saleId);
        ThirdPartySales thirdPartySales = thirdPartySaleRepository.getReferenceById(saleId);
        this.displayNet(thirdPartySales.getPartAssure());
    }

    @Override
    public SaleId changeCashSaleToThirdPartySale(SaleId saleId, NatureVente natureVente)
        throws PlafondVenteException {
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

    private ThirdPartySales findById(SaleId id) {
        return thirdPartySaleRepository.getReferenceById(id);
    }

    @Override
    public void updateTransformedSale(ThirdPartySaleDTO dto) throws PlafondVenteException {
        ThirdPartySales thirdPartySales = findById(dto.getSaleId());
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(
            dto.getCustomerId());
        thirdPartySales.setCustomer(assuredCustomer);
        thirdPartySales.setAyantDroit(assuredCustomer);
        thirdPartySales.setUpdatedAt(LocalDateTime.now());
        thirdPartySales.setEffectiveUpdateDate(thirdPartySales.getUpdatedAt());
        getAyantDroitFromId(dto.getAyantDroitId()).ifPresent(thirdPartySales::setAyantDroit);
        String message = thirdPartyCalculationManager.reComputeAndApplyAmounts(thirdPartySales,
            null, true);
        if (StringUtils.hasLength(message)) {
            ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO(thirdPartySales);
            throw new PlafondVenteException(thirdPartySaleDTO, message);
        }
    }

    private ThirdPartySales copyFromCashSale(CashSale cashSale) {
        ThirdPartySales c = new ThirdPartySales();
        setId(c);
        c.setSalesAmount(cashSale.getSalesAmount());
        c.setOrigineVente(OrigineVente.DIRECT);
        c.setCostAmount(cashSale.getCostAmount());
        c.setNumberTransaction(cashSale.getNumberTransaction());
        c.setCategorieChiffreAffaire(cashSale.getCategorieChiffreAffaire());
        c.setTypePrescription(cashSale.getTypePrescription());
        c.setSeller(cashSale.getSeller());
        c.setImported(false);
        c.setUser(cashSale.getUser());
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


    @Override
    public String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales)
        throws PlafondVenteException {
        applRemiseToSale(thirdPartySales);
        return thirdPartyCalculationManager.computeThirdPartySaleAmounts(thirdPartySales);
    }

    @Override
    public void upddateSaleAmountsOnRemovingItem(ThirdPartySales c) throws PlafondVenteException {
        applRemiseToSale(c);
        thirdPartyCalculationManager.upddateSaleAmountsOnRemovingItem(c);
    }

    @Override
    public void savePrevente(ThirdPartySaleDTO dto, boolean transform)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException {
        thirdPartySaleRepository.findOneWithEagerSalesLines(dto.getSaleId().getId(),
            dto.getSaleId().getSaleDate()).ifPresent(p -> {
            preValidatePrevente(p, transform ? SalesStatut.ACTIVE : SalesStatut.PROCESSING);
            List<ThirdPartySaleLine> thirdPartySaleLines = findAllBySaleId(p.getId());
            if (!CollectionUtils.isEmpty(thirdPartySaleLines)) {
                thirdPartySaleLines.forEach(thirdPartySaleLine -> {
                    for (ClientTiersPayantDTO clientTiersPayantDTO : dto.getTiersPayants()) {
                        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
                        if (clientTiersPayant.getId().compareTo(clientTiersPayantDTO.getId())
                            == 0) {
                            if (checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(),
                                clientTiersPayantDTO.getId(), p.getId().getId())) {
                                throw new NumBonAlreadyUseException(
                                    clientTiersPayantDTO.getNumBon());
                            }
                            thirdPartySaleLine.setNumBon(clientTiersPayantDTO.getNumBon());
                            thirdPartySaleLineService.save(thirdPartySaleLine);
                        }
                    }
                    updateClientTiersPayantAccount(thirdPartySaleLine);
                    updateTiersPayantAccount(thirdPartySaleLine);
                });
                new ArrayList<>(p.getThirdPartySaleLines()).stream().min(
                        Comparator.comparing(e -> e.getClientTiersPayant().getPriorite().getValue()))
                    .ifPresent(o -> p.setNumBon(o.getNumBon()));
            }

            thirdPartySaleRepository.save(p);
        });
    }

    private ThirdPartySales buildThirdPartySale(ThirdPartySaleDTO dto) throws GenericError {
        if (dto.getCustomerId() == null) {
            throw new GenericError("Veuillez saisir le client", "customerNotFound");
        }
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(
            dto.getCustomerId());
        ThirdPartySales c = new ThirdPartySales();
        this.intSale(dto, c);
        c.setCustomer(assuredCustomer);
        c.setAyantDroit(assuredCustomer);
        getAyantDroitFromId(dto.getAyantDroitId()).ifPresent(c::setAyantDroit);
        return c;
    }

    private Optional<AssuredCustomer> getAyantDroitFromId(Integer ayantDroitId) {
        return assuredCustomerManager.getAyantDroitFromId(ayantDroitId);
    }

    @Override
    public void changeCustomer(UpdateSaleInfo updateSaleInfo)
        throws GenericError, PlafondVenteException {
        ThirdPartySales thirdPartySales = findById(updateSaleInfo.id());
        AssuredCustomer assuredCustomer = assuredCustomerRepository.getReferenceById(
            updateSaleInfo.value());
        thirdPartySales.setCustomer(assuredCustomer);
        List<ThirdPartySaleLine> thirdPartySaleLines = thirdPartySales.getThirdPartySaleLines();
        thirdPartySaleLineService.deleteAll(thirdPartySaleLines);
        thirdPartySales.getThirdPartySaleLines().clear();
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
        return thirdPartyClientManager.saveTiersPayantLinesOnChangeCustomer(thirdPartySales);
    }

    @Override

    public FinalyseSaleDTO editSale(ThirdPartySaleDTO dto)
        throws PaymentAmountException, SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException {
        SaleId saleId = dto.getSaleId();

        ThirdPartySales p = thirdPartySaleRepository.findOneWithEagerSalesLines(saleId.getId(),
            saleId.getSaleDate()).orElseThrow();
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
                    if (checkIfNumBonIsAlReadyUse(clientTiersPayantDTO.getNumBon(),
                        clientTiersPayantDTO.getId(), p.getId().getId())) {
                        throw new NumBonAlreadyUseException(clientTiersPayantDTO.getNumBon());
                    }
                    thirdPartySaleLine.setNumBon(clientTiersPayantDTO.getNumBon());
                    thirdPartySaleLineService.save(thirdPartySaleLine);
                }
            }
            updateClientTiersPayantAccount(thirdPartySaleLine);
            updateTiersPayantAccount(thirdPartySaleLine);
        });
        new ArrayList<>(p.getThirdPartySaleLines()).stream()
            .min(Comparator.comparing(e -> e.getClientTiersPayant().getPriorite().getValue()))
            .ifPresent(o -> p.setNumBon(o.getNumBon()));
        thirdPartySaleRepository.save(p);
        return new FinalyseSaleDTO(p.getId(), true);
    }


    @Override
    public SaleId copiePourEdition(SaleId saleId)
        throws PaymentAmountException, SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, CashRegisterException {

        ThirdPartySales p = thirdPartySaleRepository.findByIdAndSaleDate(saleId.getId(),
            saleId.getSaleDate()).orElseThrow(
            () -> new GenericError("Une erreur est survenue lors de la récupération de la vente"));
        if (p.getStatut() != SalesStatut.CLOSED) {
            throw new GenericError("La vente doit être clôturée pour être modifiée");
        }
        if (p.isCanceled()) {
            throw new GenericError("La vente est annulée et ne peut pas être modifiée");
        }
        return cloneSale(p, true, SalesStatut.ACTIVE);
    }

    @Override
    public void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO)
        throws PrivilegeException {
        this.utilisationCleSecuriteService.authorizeAction(utilisationCleSecuriteDTO,
            ThirdPartySaleService.class);
    }

    @Override
    public void processDiscount(UpdateSaleInfo updateSaleInfo) {
        ThirdPartySales thirdPartySales = findById(updateSaleInfo.id());
        remiseRepository.findById(updateSaleInfo.value())
            .ifPresent(remise -> processDiscount(thirdPartySales, remise));
        this.displayNet(thirdPartySales.getPartAssure());
    }

    @Override
    public void removeDiscount(SaleId saleId) {
        ThirdPartySales thirdPartySales = findById(saleId);
        removeRemise(thirdPartySales);
        thirdPartyCalculationManager.reComputeAndApplyAmounts(thirdPartySales, null, true);
        this.displayNet(thirdPartySales.getPartAssure());
    }

    @Override
    public void updateCustomerInformation(UpdateSale updateSale)
        throws InvalidPhoneNumberException, GenericError, JsonProcessingException {
        ThirdPartySales thirdPartySales = findById(updateSale.id());
        AssuredCustomer assuredCustomer = (AssuredCustomer) thirdPartySales.getCustomer();

        AssuredCustomer ayantDroit = thirdPartySales.getAyantDroit();
        List<ThirdPartySaleLine> thirdPartySaleLines = thirdPartySales.getThirdPartySaleLines();
        Set<ThirdPartySaleLineDTO> thirdPartySaleLineNews = updateSale.thirdPartySaleLines();
        if (thirdPartySaleLines.stream().anyMatch(e -> nonNull(e.getFactureTiersPayant()))) {
            throw new GenericError("La vente est déjà facturée");
        }
        int oldTaux = thirdPartySaleLines.stream().mapToInt(ThirdPartySaleLine::getTaux).sum();
        int newTaux = thirdPartySaleLineNews.stream().mapToInt(ThirdPartySaleLineDTO::getTaux)
            .sum();
        if (oldTaux != newTaux) {
            throw new GenericError(
                String.format("Les taux sont différents:  Ancien taux : %d ,  Nouveau taux:%d",
                    oldTaux, newTaux));
        }

        if (!isSameCustomer(assuredCustomer, updateSale.customer())) {
            assuredCustomer = this.assuredCustomerRepository.getReferenceById(
                updateSale.customer().getId());
            thirdPartySales.setCustomer(assuredCustomer);
        }
        updateAssuredCustomer(assuredCustomer, updateSale.customer());
        if (nonNull(ayantDroit) && nonNull(updateSale.ayantDroit())) {
            if (!isSameCustomer(ayantDroit, updateSale.ayantDroit())) {
                ayantDroit = this.assuredCustomerRepository.getReferenceById(
                    updateSale.ayantDroit().getId());
                thirdPartySales.setAyantDroit(ayantDroit);
            }
            updateAssuredCustomer(ayantDroit, updateSale.ayantDroit());
        }
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            ThirdPartySaleLineDTO thirdPartySaleLineDTO = thirdPartySaleLineNews.stream()
                .filter(e -> e.getAssuranceSaleId().equals(thirdPartySaleLine.getId())).findFirst()
                .orElseThrow(() -> new GenericError("La ligne n'existe pas"));
            updateThirdPartySaleLine(thirdPartySaleLine, updateSale.customer(),
                thirdPartySaleLineDTO);
        });

        thirdPartySaleRepository.save(thirdPartySales);

        this.logService.create(TransactionType.MODIFICATION_INFO_CLIENT,
            TransactionType.MODIFICATION_INFO_CLIENT.getValue(),
            thirdPartySales.getId().getId().toString(),
            objectMapper.writeValueAsString(updateSale.initialValue()),
            objectMapper.writeValueAsString(updateSale.finalValue()));
    }

    @Override
    public SaleId transformToVenteEncour(SaleId saleId) {

        ThirdPartySales thirdPartySales = thirdPartySaleRepository.findByIdAndSaleDate(
                saleId.getId(), saleId.getSaleDate())
            .orElseThrow(() -> new GenericError("Une erreur est survenue"));
        preValidateTrasnform(thirdPartySales.getStatut(), thirdPartySales.getNatureVente());
        if (thirdPartySales.getStatut() == SalesStatut.DEVIS) {
            SaleId cloneId = cloneSale(thirdPartySales, false, SalesStatut.ACTIVE);
            thirdPartySales.getSalesLines().forEach(salesLineService::deleteSaleLine);
            thirdPartySales.getThirdPartySaleLines().forEach(thirdPartySaleLineService::delete);
            thirdPartySaleRepository.delete(thirdPartySales);
            return cloneId;
        } else {
            thirdPartySales.setStatut(SalesStatut.ACTIVE);
            thirdPartySaleRepository.save(thirdPartySales);
        }
        return saleId;
    }

    @Override
    public void cloneDevis(SaleId saleId) {
        ThirdPartySales p = thirdPartySaleRepository.findByIdAndSaleDate(saleId.getId(),
            saleId.getSaleDate()).orElseThrow(
            () -> new GenericError("Une erreur est survenue lors de la récupération de la vente"));
        if (p.getStatut() != SalesStatut.DEVIS) {
            throw new GenericError("Une erreur est survenue ");
        }

        cloneSale(p, false, p.getStatut());
    }

    @Override
    public void addAyantDroitToSale(UpdateSaleInfo updateSaleInfo) {
        if (updateSaleInfo == null) {
            return;
        }
        ThirdPartySales p = thirdPartySaleRepository.getReferenceById(updateSaleInfo.id());
        AssuredCustomer ayantDroit = assuredCustomerRepository.getReferenceById(updateSaleInfo.value());
        p.setAyantDroit(ayantDroit);
        thirdPartySaleRepository.save(p);
    }

    private void updateThirdPartySaleLine(ThirdPartySaleLine thirdPartySaleLine,
                                          AssuredCustomerDTO assuredCustomerDTO, ThirdPartySaleLineDTO thirdPartySaleLineDTO) {
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();

        if (clientTiersPayant.getId().compareTo(thirdPartySaleLineDTO.getClientTiersPayantId())
            != 0) {
            clientTiersPayant = clientTiersPayantRepository.getReferenceById(
                thirdPartySaleLineDTO.getClientTiersPayantId());
            thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        }
        thirdPartySaleLine.setNumBon(thirdPartySaleLineDTO.getNumBon());
        if (clientTiersPayant.getPriorite() == PrioriteTiersPayant.R0 && !clientTiersPayant.getNum()
            .equals(assuredCustomerDTO.getNum())) {
            clientTiersPayant.setNum(assuredCustomerDTO.getNum());
            clientTiersPayantRepository.save(clientTiersPayant);
        }
    }

    private void updateAssuredCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer)
        throws InvalidPhoneNumberException {
        assuredCustomerManager.updateAssuredCustomer(assuredCustomer, customer);
    }

    private boolean isSameCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer) {
        return assuredCustomerManager.isSameCustomer(assuredCustomer, customer);
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
            thirdPartySales.setNetAmount(
                thirdPartySales.getSalesAmount() - thirdPartySales.getDiscountAmount());
            /*}
            else {
                throw new GenericError("La remise produit n'est pas applicable sur une vente assurance", "notYetImplemented");
            }*/
        }
        thirdPartyCalculationManager.reComputeAndApplyAmounts(thirdPartySales, null, true);
    }

    private void applRemiseToSale(ThirdPartySales thirdPartySales) {
        Remise remise = thirdPartySales.getRemise();
        if (remise == null) {
            return;
        }
        if (remise instanceof RemiseProduit
            && thirdPartySales.getNatureVente() == NatureVente.ASSURANCE) {
            throw new GenericError("La remise produit n'est pas applicable sur une vente assurance",
                "notYetImplemented");
        }
        this.proccessDiscount(thirdPartySales);
    }


}
