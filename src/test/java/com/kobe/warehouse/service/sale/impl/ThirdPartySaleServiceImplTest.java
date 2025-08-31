package com.kobe.warehouse.service.sale.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kobe.warehouse.config.IdGeneratorService;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
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
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.errors.ThirdPartySalesTiersPayantException;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.calculation.TiersPayantCalculationService;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.sale.dto.UpdateSale;
import com.kobe.warehouse.service.utils.AfficheurPosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartySaleServiceImplTest {

    @Mock
    private ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    @Mock
    private ClientTiersPayantRepository clientTiersPayantRepository;
    @Mock
    private TiersPayantRepository tiersPayantRepository;
    @Mock
    private SalesLineService salesLineService;
    @Mock
    private StorageService storageService;
    @Mock
    private ThirdPartySaleRepository thirdPartySaleRepository;
    @Mock
    private AssuredCustomerRepository assuredCustomerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ReferenceService referenceService;
    @Mock
    private CashRegisterService cashRegisterService;
    @Mock
    private PosteRepository posteRepository;
    @Mock
    private CashSaleRepository cashSaleRepository;
    @Mock
    private UtilisationCleSecuriteService utilisationCleSecuriteService;
    @Mock
    private RemiseRepository remiseRepository;
    @Mock
    private PrixRererenceService prixRererenceService;
    @Mock
    private LogsService logService;
    @Mock
    private TiersPayantCalculationService tiersPayantCalculationService;
    @Mock
    private SaleLineServiceFactory saleLineServiceFactory;
    @Mock
    private AfficheurPosService afficheurPosService;
    @Mock
    private  IdGeneratorService idGeneratorService;

    @InjectMocks
    private ThirdPartySaleServiceImpl thirdPartySaleService;

    private static java.util.List<ThirdPartySaleLine> mockThirdPartySaleLineList() {
        // Premier tiers payant : ASSURANCE
        TiersPayant tiersPayant1 = new TiersPayant();
        tiersPayant1.setId(1L);
        tiersPayant1.setName("TP_ASSURANCE");
        tiersPayant1.setFullName("Tiers Payant Assurance");
        tiersPayant1.setCategorie(TiersPayantCategorie.ASSURANCE);

        ClientTiersPayant clientTiersPayant1 = new ClientTiersPayant();
        clientTiersPayant1.setId(1L);
        clientTiersPayant1.setNum("CTP001");
        clientTiersPayant1.setPriorite(PrioriteTiersPayant.R0);
        clientTiersPayant1.setTiersPayant(tiersPayant1);

        ThirdPartySaleLine line1 = new ThirdPartySaleLine();
        line1.setId(1L);
        line1.setNumBon("BON001");
        line1.setMontant(500);
        line1.setStatut(ThirdPartySaleStatut.ACTIF);
        line1.setCreated(java.time.LocalDateTime.now());
        line1.setUpdated(java.time.LocalDateTime.now());
        line1.setClientTiersPayant(clientTiersPayant1);

        // Deuxième tiers payant : MUTUELLE
        TiersPayant tiersPayant2 = new TiersPayant();
        tiersPayant2.setId(2L);
        tiersPayant2.setName("TP_MUTUELLE");
        tiersPayant2.setFullName("Tiers Payant Mutuelle");
        tiersPayant2.setCategorie(TiersPayantCategorie.ASSURANCE);

        ClientTiersPayant clientTiersPayant2 = new ClientTiersPayant();
        clientTiersPayant2.setId(2L);
        clientTiersPayant2.setNum("CTP002");
        clientTiersPayant2.setPriorite(PrioriteTiersPayant.R1);
        clientTiersPayant2.setTiersPayant(tiersPayant2);

        ThirdPartySaleLine line2 = new ThirdPartySaleLine();
        line2.setId(2L);
        line2.setNumBon("BON002");
        line2.setMontant(600);
        line2.setStatut(ThirdPartySaleStatut.ACTIF);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
        thirdPartySales.setStatut(SalesStatut.CLOSED);
        thirdPartySales.setPartAssure(200);
        thirdPartySales.setPartTiersPayant(300);
        thirdPartySales.setDiffere(false);
        thirdPartySales.setCreatedAt(java.time.LocalDateTime.now());
        thirdPartySales.setUpdatedAt(java.time.LocalDateTime.now());
        line2.setSale(thirdPartySales);

        return new ArrayList<>(List.of(line1, line2));
    }

    private static java.util.Set<SalesLine> mockSalesLineSet() {
        SalesLine salesLine = new SalesLine();
        salesLine.setId(1L);
        salesLine.setSalesAmount(1000);
        salesLine.setRegularUnitPrice(1000);
        salesLine.setQuantityRequested(2);
        salesLine.setQuantitySold(2);
        salesLine.setCreatedAt(java.time.LocalDateTime.now());
        salesLine.setUpdatedAt(salesLine.getCreatedAt());

        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
        thirdPartySales.setStatut(SalesStatut.CLOSED);
        thirdPartySales.setPartAssure(200);
        thirdPartySales.setPartTiersPayant(800);
        thirdPartySales.setDiffere(false);
        thirdPartySales.setCreatedAt(java.time.LocalDateTime.now());
        thirdPartySales.setUpdatedAt(java.time.LocalDateTime.now());
        salesLine.setSales(thirdPartySales);

        return java.util.Collections.singleton(salesLine);
    }

    @BeforeEach
    void setUp() {
      //  MockitoAnnotations.openMocks(this);
        when(saleLineServiceFactory.getService(any())).thenReturn(salesLineService);
        AppUser user = new AppUser();
        user.setId(1L);
        user.setLogin("test");
        when(storageService.getUser()).thenReturn(user);
        when(userRepository.findOneByLogin(any())).thenReturn(Optional.of(user));
        thirdPartySaleService = new ThirdPartySaleServiceImpl(
            thirdPartySaleLineRepository,
            clientTiersPayantRepository,
            tiersPayantRepository,
            saleLineServiceFactory,
            storageService,
            thirdPartySaleRepository,
            assuredCustomerRepository,
            userRepository,
            paymentService,
            referenceService,
            cashRegisterService,
            posteRepository,
            cashSaleRepository,
            utilisationCleSecuriteService,
            remiseRepository,
            afficheurPosService,
            prixRererenceService,
            logService,
            tiersPayantCalculationService,idGeneratorService
        );
    }

    @Test
    void testCreateSale() throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO();
        thirdPartySaleDTO.setCustomerId(1L);
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setProduitId(1L);
        saleLineDTO.setQuantityRequested(1);
        saleLineDTO.setRegularUnitPrice(1000);
        thirdPartySaleDTO.setSalesLines(Collections.singletonList(saleLineDTO));
        thirdPartySaleDTO.setNatureVente(NatureVente.ASSURANCE);

        Storage storage = new Storage();
        storage.setId(1L);
        when(storageService.getDefaultConnectedUserPointOfSaleStorage()).thenReturn(storage);

        SalesLine salesLine = new SalesLine();
        salesLine.setSalesAmount(1000);
        when(salesLineService.createSaleLineFromDTO(any(SaleLineDTO.class), any(Long.class))).thenReturn(salesLine);

        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setId(1L);
        when(assuredCustomerRepository.getReferenceById(1L)).thenReturn(assuredCustomer);

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.valueOf(200));
        calculationResult.setTotalTiersPayant(BigDecimal.valueOf(800));
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setPartAssure(200);
        when(thirdPartySaleRepository.saveAndFlush(any(ThirdPartySales.class))).thenReturn(thirdPartySales);

        ThirdPartySaleDTO result = thirdPartySaleService.createSale(thirdPartySaleDTO);

        assertNotNull(result);
    }

    @Test
    void testCreateSale_plafondVenteException() throws GenericError, NumBonAlreadyUseException {
        ThirdPartySaleDTO thirdPartySaleDTO = new ThirdPartySaleDTO();
        thirdPartySaleDTO.setCustomerId(1L);
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setProduitId(1L);
        saleLineDTO.setQuantityRequested(1);
        saleLineDTO.setRegularUnitPrice(1000);
        thirdPartySaleDTO.setSalesLines(Collections.singletonList(saleLineDTO));
        thirdPartySaleDTO.setNatureVente(NatureVente.ASSURANCE);

        Storage storage = new Storage();
        storage.setId(1L);
        when(storageService.getDefaultConnectedUserPointOfSaleStorage()).thenReturn(storage);

        SalesLine salesLine = new SalesLine();
        salesLine.setSalesAmount(1000);
        when(salesLineService.createSaleLineFromDTO(any(SaleLineDTO.class), any(Long.class))).thenReturn(salesLine);

        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setId(1L);
        when(assuredCustomerRepository.getReferenceById(1L)).thenReturn(assuredCustomer);

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.valueOf(200));
        calculationResult.setTotalTiersPayant(BigDecimal.valueOf(800));
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        calculationResult.setWarningMessage("Plafond atteint");
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setPartAssure(200);
        when(thirdPartySaleRepository.saveAndFlush(any(ThirdPartySales.class))).thenReturn(thirdPartySales);

        assertThrows(PlafondVenteException.class, () -> {
            thirdPartySaleService.createSale(thirdPartySaleDTO);
        });
    }

    @Test
    void testCreateOrUpdateSaleLine_newSaleLine() throws PlafondVenteException {
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setSaleId(1L);
        saleLineDTO.setProduitId(1L);
        saleLineDTO.setQuantityRequested(1);
        saleLineDTO.setRegularUnitPrice(1000);

        when(salesLineService.findBySalesIdAndProduitId(1L, 1L)).thenReturn(Optional.empty());

        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
     //   when(thirdPartySaleRepository.getReferenceById(1L)).thenReturn(thirdPartySales);

        SalesLine salesLine = new SalesLine();
        salesLine.setSalesAmount(1000);
        when(salesLineService.create(any(SaleLineDTO.class), any(Long.class), any(ThirdPartySales.class))).thenReturn(salesLine);

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.valueOf(200));
        calculationResult.setTotalTiersPayant(BigDecimal.valueOf(800));
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        when(thirdPartySaleRepository.save(any(ThirdPartySales.class))).thenReturn(thirdPartySales);

        SaleLineDTO result = thirdPartySaleService.createOrUpdateSaleLine(saleLineDTO);

        assertNotNull(result);
    }

    @Test
    void testCreateOrUpdateSaleLine_existingSaleLine() throws PlafondVenteException {
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setSaleId(1L);
        saleLineDTO.setProduitId(1L);
        saleLineDTO.setQuantityRequested(2);
        saleLineDTO.setRegularUnitPrice(1000);

        SalesLine existingSalesLine = new SalesLine();
        existingSalesLine.setSalesAmount(1000);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
        existingSalesLine.setSales(thirdPartySales);

        when(salesLineService.findBySalesIdAndProduitId(1L, 1L)).thenReturn(Optional.of(existingSalesLine));

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.valueOf(400));
        calculationResult.setTotalTiersPayant(BigDecimal.valueOf(1600));
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        when(thirdPartySaleRepository.save(any(ThirdPartySales.class))).thenReturn(thirdPartySales);

        SaleLineDTO result = thirdPartySaleService.createOrUpdateSaleLine(saleLineDTO);

        assertNotNull(result);
        verify(salesLineService).updateSaleLine(any(SaleLineDTO.class), any(SalesLine.class), anyLong());
    }

    @Test
    void testDeleteSaleLineById() {
        SalesLine salesLine = new SalesLine();
        salesLine.setId(1L);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
        salesLine.setSales(thirdPartySales);
        salesLine.setSalesAmount(1000);

        when(salesLineService.getOneById(1L)).thenReturn(salesLine);

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.ZERO);
        calculationResult.setTotalTiersPayant(BigDecimal.ZERO);
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        thirdPartySaleService.deleteSaleLineById(1L);

        verify(thirdPartySaleRepository).save(any(ThirdPartySales.class));
        verify(salesLineService).deleteSaleLine(any(SalesLine.class));
    }

    @Test
    void testCancelSale() {
        ThirdPartySales sales = new ThirdPartySales();
        sales.setId(1L);
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        ClientTiersPayant clientTiersPayant = new ClientTiersPayant();
        clientTiersPayant.setPriorite(PrioriteTiersPayant.R0);
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        sales.setThirdPartySaleLines(Collections.singletonList(thirdPartySaleLine));
        when(thirdPartySaleRepository.findOneWithEagerSalesLines(1L)).thenReturn(Optional.of(sales));
        when(paymentService.findAllBySalesId(1L)).thenReturn(Collections.emptyList());
        when(thirdPartySaleLineRepository.findAllBySaleId(1L)).thenReturn(Collections.singletonList(thirdPartySaleLine));

        thirdPartySaleService.cancelSale(1L);

        verify(thirdPartySaleRepository, times(2)).save(any(ThirdPartySales.class));
    }

    @Test
    void testCancelSale_notFound() {
        when(thirdPartySaleRepository.findOneWithEagerSalesLines(99L)).thenReturn(Optional.empty());
        // Ne doit pas lever d'exception, juste ignorer
        thirdPartySaleService.cancelSale(99L);
        verify(thirdPartySaleRepository, times(0)).save(any(ThirdPartySales.class));
    }

    @Test
    void testSave() throws PaymentAmountException, SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, NumBonAlreadyUseException {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setId(1L);
        ClientTiersPayantDTO clientTiersPayantDTO = new ClientTiersPayantDTO();
        clientTiersPayantDTO.setId(1L);
        clientTiersPayantDTO.setNumBon("NUMBON001");
        dto.setTiersPayants(Collections.singletonList(clientTiersPayantDTO));

        ThirdPartySales sales = new ThirdPartySales();
        sales.setId(1L);
        sales.setThirdPartySaleLines(mockThirdPartySaleLineList());
        when(thirdPartySaleRepository.findOneWithEagerSalesLines(1L)).thenReturn(Optional.of(sales));
        when(thirdPartySaleLineRepository.findAllBySaleId(1L)).thenReturn(mockThirdPartySaleLineList());

        FinalyseSaleDTO result = thirdPartySaleService.save(dto);

        assertNotNull(result);
    }

    @Test
    void testSave_numBonAlreadyUseException() {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setId(1L);
        ClientTiersPayantDTO clientTiersPayantDTO = new ClientTiersPayantDTO();
        clientTiersPayantDTO.setId(1L);
        clientTiersPayantDTO.setNumBon("NUMBON001");
        dto.setTiersPayants(Collections.singletonList(clientTiersPayantDTO));

        ThirdPartySales sales = new ThirdPartySales();
        sales.setId(1L);
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        ClientTiersPayant clientTiersPayant = new ClientTiersPayant();
        clientTiersPayant.setId(1L);
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        sales.setThirdPartySaleLines(Collections.singletonList(thirdPartySaleLine));
        when(thirdPartySaleRepository.findOneWithEagerSalesLines(1L)).thenReturn(Optional.of(sales));
        when(thirdPartySaleLineRepository.findAllBySaleId(1L)).thenReturn(Collections.singletonList(thirdPartySaleLine));
        when(thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(anyString(), anyLong(), anyLong(), any(SalesStatut.class))).thenReturn(1l);

        assertThrows(NumBonAlreadyUseException.class, () -> {
            thirdPartySaleService.save(dto);
        });
    }

    @Test
    void testAddThirdPartySaleLineToSales() throws GenericError, NumBonAlreadyUseException, PlafondVenteException {
        ClientTiersPayantDTO dto = new ClientTiersPayantDTO();
        dto.setId(1L);
        dto.setNumBon("NUMBON001");

        ClientTiersPayant clientTiersPayant = new ClientTiersPayant();
        clientTiersPayant.setId(1L);
        when(clientTiersPayantRepository.getReferenceById(1L)).thenReturn(clientTiersPayant);

        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
       // when(thirdPartySaleRepository.getReferenceById(1L)).thenReturn(thirdPartySales);

        when(thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(anyString(), anyLong(), any(SalesStatut.class))).thenReturn(0l);

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.ZERO);
        calculationResult.setTotalTiersPayant(BigDecimal.ZERO);
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        when(thirdPartySaleRepository.saveAndFlush(any(ThirdPartySales.class))).thenReturn(thirdPartySales);

        thirdPartySaleService.addThirdPartySaleLineToSales(dto, 1L);

        verify(thirdPartySaleLineRepository).save(any(ThirdPartySaleLine.class));
        verify(thirdPartySaleRepository).saveAndFlush(any(ThirdPartySales.class));
    }

    @Test
    void testAddThirdPartySaleLineToSales_numBonAlreadyUseException() {
        ClientTiersPayantDTO dto = new ClientTiersPayantDTO();
        dto.setId(1L);
        dto.setNumBon("NUMBON001");

        ClientTiersPayant clientTiersPayant = new ClientTiersPayant();
        clientTiersPayant.setId(1L);
        when(clientTiersPayantRepository.getReferenceById(1L)).thenReturn(clientTiersPayant);

        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
       // when(thirdPartySaleRepository.getReferenceById(1L)).thenReturn(thirdPartySales);

        when(thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(anyString(), anyLong(), any(SalesStatut.class))).thenReturn(1l);

        assertThrows(NumBonAlreadyUseException.class, () -> {
            thirdPartySaleService.addThirdPartySaleLineToSales(dto, 1L);
        });
    }

    @Test
    void testRemoveThirdPartySaleLineToSales() throws PlafondVenteException {
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        thirdPartySaleLine.setId(1L);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
        thirdPartySaleLine.setSale(thirdPartySales);
        when(thirdPartySaleLineRepository.findFirstByClientTiersPayantIdAndSaleId(1L, 1L)).thenReturn(Optional.of(thirdPartySaleLine));

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.ZERO);
        calculationResult.setTotalTiersPayant(BigDecimal.ZERO);
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        thirdPartySaleService.removeThirdPartySaleLineToSales(1L, 1L);

        verify(thirdPartySaleLineRepository).delete(any(ThirdPartySaleLine.class));
        verify(thirdPartySaleRepository).saveAndFlush(any(ThirdPartySales.class));
    }

    @Test
    void testDeleteSaleLineById_notFound() {
        when(salesLineService.getOneById(99L)).thenReturn(null);

        thirdPartySaleService.deleteSaleLineById(99L);

        verify(thirdPartySaleRepository, times(0)).save(any(ThirdPartySales.class));
    }

    @Test
    void testUpdateItemQuantityRequested_stockException() throws StockException, DeconditionnementStockOut {
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setId(1L);
        saleLineDTO.setQuantityRequested(10);

        SalesLine salesLine = new SalesLine();
        salesLine.setId(1L);
        when(salesLineService.getOneById(1L)).thenReturn(salesLine);

        doThrow(new StockException()).when(salesLineService).updateItemQuantityRequested(any(SaleLineDTO.class), any(SalesLine.class), anyLong());

        assertThrows(StockException.class, () -> {
            thirdPartySaleService.updateItemQuantityRequested(saleLineDTO);
        });
    }

    @Test
    void testUpdateItemQuantityRequested_deconditionnementStockOut() throws StockException, DeconditionnementStockOut {
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setId(2L);
        saleLineDTO.setProduitId(1l);
        saleLineDTO.setQuantityRequested(5);
        SalesLine salesLine = new SalesLine();
        salesLine.setId(2L);

        when(salesLineService.getOneById(2L)).thenReturn(salesLine);
        doThrow(new DeconditionnementStockOut(saleLineDTO.getProduitId().toString())).when(salesLineService).updateItemQuantityRequested(any(SaleLineDTO.class), any(SalesLine.class), anyLong());
        assertThrows(DeconditionnementStockOut.class, () -> {
            thirdPartySaleService.updateItemQuantityRequested(saleLineDTO);
        });
    }

    @Test
    void testUpdateItemRegularPrice_plafondVenteException() throws PlafondVenteException {
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setId(1L);
        saleLineDTO.setRegularUnitPrice(2000);

        SalesLine salesLine = new SalesLine();
        salesLine.setId(1L);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
        salesLine.setSales(thirdPartySales);
        when(salesLineService.getOneById(1L)).thenReturn(salesLine);

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.valueOf(400));
        calculationResult.setTotalTiersPayant(BigDecimal.valueOf(1600));
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        calculationResult.setWarningMessage("Plafond atteint");
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        when(thirdPartySaleRepository.saveAndFlush(any(ThirdPartySales.class))).thenReturn(thirdPartySales);

        assertThrows(PlafondVenteException.class, () -> {
            thirdPartySaleService.updateItemRegularPrice(saleLineDTO);
        });
    }

    @Test
    void testChangeCustomer_plafondVenteException() {
        KeyValue keyValue = new KeyValue(1L, 2L);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
     //   when(thirdPartySaleRepository.getReferenceById(1L)).thenReturn(thirdPartySales);

        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setId(2L);
        when(assuredCustomerRepository.getReferenceById(2L)).thenReturn(assuredCustomer);

        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.ZERO);
        calculationResult.setTotalTiersPayant(BigDecimal.ZERO);
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        calculationResult.setWarningMessage("Plafond atteint");
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);

        when(thirdPartySaleRepository.saveAndFlush(any(ThirdPartySales.class))).thenReturn(thirdPartySales);

        assertThrows(PlafondVenteException.class, () -> {
            thirdPartySaleService.changeCustomer(keyValue);
        });
    }

    @Test
    void testEditSale_thirdPartySalesTiersPayantException() {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setId(1L);
        dto.setTiersPayants(Collections.emptyList());

        ThirdPartySales sales = new ThirdPartySales();
        sales.setId(1L);
        when(thirdPartySaleRepository.findOneWithEagerSalesLines(1L)).thenReturn(Optional.of(sales));
        when(thirdPartySaleLineRepository.findAllBySaleId(1L)).thenReturn(Collections.emptyList());

        assertThrows(ThirdPartySalesTiersPayantException.class, () -> {
            thirdPartySaleService.editSale(dto);
        });
    }

    @Test
    void testSave_saleNotFoundCustomerException() {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setId(2L);

        // Simule une vente existante mais sans client associé (ou client non valide)
        ThirdPartySales sales = new ThirdPartySales();
        sales.setId(2L);
        sales.setDiffere(true);
        sales.setCustomer(null); // ou un client qui ne correspond pas à la logique métier attendue

        when(thirdPartySaleRepository.findOneWithEagerSalesLines(2L)).thenReturn(Optional.of(sales));
        when(thirdPartySaleLineRepository.findAllBySaleId(2L)).thenReturn(mockThirdPartySaleLineList());
        assertThrows(SaleNotFoundCustomerException.class, () -> {
            thirdPartySaleService.save(dto);
        });
    }

    @Test
    void testAddThirdPartySaleLineToSales_clientTiersPayantNotFound() {
        ClientTiersPayantDTO dto = new ClientTiersPayantDTO();
        dto.setId(2L);
        dto.setNumBon("NUMBON002");
        when(clientTiersPayantRepository.getReferenceById(2L)).thenThrow(new IllegalArgumentException());
        assertThrows(GenericError.class, () -> {
            thirdPartySaleService.addThirdPartySaleLineToSales(dto, 1L);
        });
    }

    @Test
    void testAddThirdPartySaleLineToSales_thirdPartySalesNotFound() {
        ClientTiersPayantDTO dto = new ClientTiersPayantDTO();
        dto.setId(1L);
        dto.setNumBon("NUMBON001");
        ClientTiersPayant clientTiersPayant = new ClientTiersPayant();
        clientTiersPayant.setId(1L);
        when(clientTiersPayantRepository.getReferenceById(1L)).thenReturn(clientTiersPayant);
      //  when(thirdPartySaleRepository.getReferenceById(2L)).thenThrow(new IllegalArgumentException());
        assertThrows(GenericError.class, () -> {
            thirdPartySaleService.addThirdPartySaleLineToSales(dto, 2L);
        });
    }

    @Test
    void testRemoveThirdPartySaleLineToSales_notFound() {
        when(thirdPartySaleLineRepository.findFirstByClientTiersPayantIdAndSaleId(2L, 2L)).thenReturn(Optional.empty());
        // Ne doit pas lever d'exception, juste ignorer
        thirdPartySaleService.removeThirdPartySaleLineToSales(2L, 2L);
        verify(thirdPartySaleLineRepository, times(0)).delete(any(ThirdPartySaleLine.class));
    }

    @Test
    void testUpdateItemRegularPrice_ok() throws PlafondVenteException {
        SaleLineDTO saleLineDTO = new SaleLineDTO();
        saleLineDTO.setId(2L);
        saleLineDTO.setRegularUnitPrice(1500);
        SalesLine salesLine = new SalesLine();
        salesLine.setId(2L);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(2L);
        salesLine.setSales(thirdPartySales);
        when(salesLineService.getOneById(2L)).thenReturn(salesLine);
        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.valueOf(100));
        calculationResult.setTotalTiersPayant(BigDecimal.valueOf(1400));
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);
        when(thirdPartySaleRepository.saveAndFlush(any(ThirdPartySales.class))).thenReturn(thirdPartySales);
        thirdPartySaleService.updateItemRegularPrice(saleLineDTO);
        verify(thirdPartySaleRepository).saveAndFlush(any(ThirdPartySales.class));
    }

    @Test
    void testChangeCustomer_ok() throws PlafondVenteException {
        KeyValue keyValue = new KeyValue(1L, 2L);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
       // when(thirdPartySaleRepository.getReferenceById(1L)).thenReturn(thirdPartySales);
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setId(2L);
        when(assuredCustomerRepository.getReferenceById(2L)).thenReturn(assuredCustomer);
        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setTotalPatientShare(BigDecimal.ZERO);
        calculationResult.setTotalTiersPayant(BigDecimal.ZERO);
        calculationResult.setTiersPayantLines(Collections.emptyList());
        calculationResult.setItemShares(Collections.emptyList());
        when(tiersPayantCalculationService.calculate(any(CalculationInput.class))).thenReturn(calculationResult);
        when(thirdPartySaleRepository.saveAndFlush(any(ThirdPartySales.class))).thenReturn(thirdPartySales);
        thirdPartySaleService.changeCustomer(keyValue);
        verify(thirdPartySaleRepository).saveAndFlush(any(ThirdPartySales.class));
    }

    @Test
    void testEditSale_ok() throws ThirdPartySalesTiersPayantException {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setId(1L);
        ClientTiersPayantDTO clientTiersPayantDTO = new ClientTiersPayantDTO();
        clientTiersPayantDTO.setId(1L);
        clientTiersPayantDTO.setNumBon("NUMBON001");
        dto.setTiersPayants(Collections.singletonList(clientTiersPayantDTO));
        ThirdPartySales sales = new ThirdPartySales();
        sales.setId(1L);
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        ClientTiersPayant clientTiersPayant = new ClientTiersPayant();
        clientTiersPayant.setId(1L);
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        sales.setThirdPartySaleLines(Collections.singletonList(thirdPartySaleLine));
        when(thirdPartySaleRepository.findOneWithEagerSalesLines(1L)).thenReturn(Optional.of(sales));
        when(thirdPartySaleLineRepository.findAllBySaleId(1L)).thenReturn(Collections.singletonList(thirdPartySaleLine));
        thirdPartySaleService.editSale(dto);
        verify(thirdPartySaleRepository).save(any(ThirdPartySales.class));
    }

    @Test
    void testUpdateCustomerInformation_ok() throws InvalidPhoneNumberException, GenericError, JsonProcessingException {
        AssuredCustomerDTO customerDTO = new AssuredCustomerDTO();
        customerDTO.setId(1L);
        customerDTO.setPhone("0123456789");
        UpdateSale updateSale = new UpdateSale(1L, customerDTO, null, Collections.emptySet(), null, null);
        AssuredCustomer assuredCustomer = new AssuredCustomer();
        assuredCustomer.setId(1L);
        when(assuredCustomerRepository.getReferenceById(1L)).thenReturn(assuredCustomer);
        ThirdPartySales thirdPartySales = new ThirdPartySales();
        thirdPartySales.setId(1L);
        thirdPartySales.setCustomer(assuredCustomer);
       // when(thirdPartySaleRepository.getReferenceById(1L)).thenReturn(thirdPartySales);
        when(assuredCustomerRepository.save(any(AssuredCustomer.class))).thenReturn(assuredCustomer);
        thirdPartySaleService.updateCustomerInformation(updateSale);
        verify(assuredCustomerRepository).save(any(AssuredCustomer.class));
    }
}
