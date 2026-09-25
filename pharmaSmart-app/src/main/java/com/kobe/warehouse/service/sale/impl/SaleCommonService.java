package com.kobe.warehouse.service.sale.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.SaleAlreadyCloseException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.calculation.SaleAmountCalculator;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Socle commun aux quatre services de vente, qui en héritent.
 */
public abstract class SaleCommonService {

    private final ReferenceService referenceService;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final SaleAmountCalculator saleAmountCalculator;
    private final CashRegisterService cashRegisterService;
    private final PosteRepository posteRepository;
    private final CustomerDisplayService afficheurPosService;
    private final SaleIdGeneratorService idGeneratorService;
    private final ObjectMapper objectMapper;
    private final AppConfigurationService appConfigurationService;


    public SaleCommonService(
        ReferenceService referenceService,
        StorageService storageService,
        UserRepository userRepository,
        SaleAmountCalculator saleAmountCalculator,
        CashRegisterService cashRegisterService,
        PosteRepository posteRepository,
        CustomerDisplayService afficheurPosService,
        SaleIdGeneratorService idGeneratorService,
        ObjectMapper objectMapper,
        AppConfigurationService appConfigurationService
    ) {
        this.referenceService = referenceService;

        this.storageService = storageService;
        this.userRepository = userRepository;
        this.saleAmountCalculator = saleAmountCalculator;
        this.cashRegisterService = cashRegisterService;
        this.posteRepository = posteRepository;
        this.afficheurPosService = afficheurPosService;
        this.idGeneratorService = idGeneratorService;
        this.objectMapper = objectMapper;
        this.appConfigurationService = appConfigurationService;

    }

    /**
     * Vérifie que la vente n'est pas trop ancienne pour être annulée.
     * Le délai maximum est configurable via APP_CANCEL_SALE_MAX_DAYS (défaut : 30 jours).
     *
     * @param saleDate date de la vente
     * @throws GenericError si le délai est dépassé
     */
    protected void checkCancellationDelay(LocalDate saleDate) {
        int maxDays = appConfigurationService.getCancelSaleMaxDays();
        long daysSinceSale = ChronoUnit.DAYS.between(saleDate, LocalDate.now());
        if (daysSinceSale > maxDays) {
            throw new GenericError(
                String.format("Annulation impossible : la vente date de %d jour(s). Délai maximum autorisé : %d jour(s).",
                    daysSinceSale, maxDays));
        }
    }

    public void computeSaleEagerAmount(Sales c) {
        saleAmountCalculator.computeSaleEagerAmount(c);
    }

    protected void updateAmounts(Sales c) {
        saleAmountCalculator.updateAmounts(c);
    }


    public void computeSaleEagerAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        saleAmountCalculator.computeSaleEagerAmountOnRemovingItem(c, saleLine);
    }

    public void computeSaleLazyAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        saleAmountCalculator.computeSaleLazyAmountOnRemovingItem(c, saleLine);
    }

    public void computeTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        saleAmountCalculator.computeTvaAmountOnRemovingItem(c, saleLine);
    }

    public void buildReference(Sales sales) {
        String ref = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            .concat(referenceService.buildNumSale());
        sales.setNumberTransaction(ref);
    }

    public void buildPreventeReference(Sales sales) {
        String ref = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            .concat(referenceService.buildNumPreventeSale());
        sales.setNumberTransaction(ref);
    }

    public String buildTvaData(Set<SalesLine> salesLines) {
        if (salesLines != null && !salesLines.isEmpty()) {

            ArrayNode array = objectMapper.createArrayNode();
            salesLines
                .stream()
                .filter(saleLine -> saleLine.getTaxValue() > 0)
                .collect(Collectors.groupingBy(SalesLine::getTaxValue))
                .forEach((k, v) -> {
                    ObjectNode json = objectMapper.createObjectNode();

                    int totalTva = 0;
                    for (SalesLine item : v) {
                        Double valeurTva = 1 + (Double.valueOf(k) / 100);
                        int htAmont = (int) Math.ceil(item.getSalesAmount() / valeurTva);
                        totalTva += (item.getSalesAmount() - htAmont);
                    }
                    json.put("tva", k);
                    json.put("amount", totalTva);
                    array.add(json);
                });
            if (!array.isEmpty()) {
                return array.toString();
            }
        }
        return null;
    }

    public int roundedAmount(int payrollAmount) {
        return saleAmountCalculator.roundedAmount(payrollAmount);
    }

    protected void setId(Sales c) {
        c.setId(idGeneratorService.nextId());
    }

    protected void intSale(SaleDTO dto, Sales c) {
        setId(c);
        AppUser caissier = storageService.getUser();
        c.setNatureVente(dto.getNatureVente());
        c.setTypePrescription(dto.getTypePrescription());

        if (nonNull(dto.getSellerId()) && caissier.getId().compareTo(dto.getSellerId()) != 0) {
            c.setSeller(userRepository.getReferenceById(dto.getSellerId()));
        } else {
            c.setSeller(caissier);
        }
        c.setImported(false);
        c.setUser(caissier);
        c.setCaissier(caissier);
        c.setCopy(dto.getCopy());
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(c.getCreatedAt());
        c.setEffectiveUpdateDate(c.getUpdatedAt());
        c.setPayrollAmount(0);
        c.setToIgnore(dto.isToIgnore());
        c.setDiffere(dto.isDiffere());
        this.buildPreventeReference(c);
        if (nonNull(dto.getStatut())) {
            c.setStatut(dto.getStatut());
        } else {
            c.setStatut(SalesStatut.ACTIVE);
        }

        this.posteRepository.findFirstByAddressOrName(dto.getCaisseNum(), dto.getCaisseNum())
            .ifPresent(poste -> {
                c.setCaisse(poste);
                c.setLastCaisse(poste);
            });

        c.setPaymentStatus(PaymentStatus.IMPAYE);
        c.setOrigineVente(OrigineVente.DIRECT);
        c.setMagasin(c.getCaissier().getMagasin());
    }

    public void save(Sales c, SaleDTO dto) throws SaleAlreadyCloseException {
        prevalideSale(c);
        finalizeSale(c, dto);
    }

    public void prevalideSale(Sales c) throws SaleAlreadyCloseException {
        if (CollectionUtils.isEmpty(c.getSalesLines())) {
            return;
        }
        if (c.getStatut() == SalesStatut.CLOSED) {
            throw new SaleAlreadyCloseException();
        }
    }

    public void editSale(Sales c, SaleDTO dto) {
        finalizeSale(c, dto);
    }

    protected CashRegister getCashRegister() {
        AppUser user = storageService.getUser();
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        return cashRegister;
    }

    protected void checkOpenningCaisse() throws CashRegisterException {
        AppUser user = storageService.getUser();
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            throw new CashRegisterException();
        }

    }

    private void finalizeSale(Sales c, SaleDTO dto) {
        AppUser user = storageService.getUser();
        c.setUser(user);
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        c.setCashRegister(cashRegister);
        int id = storageService.getDefaultConnectedUserMainStorage().getId();
        getSaleLineService(c).save(c.getSalesLines(), user, id);
        c.setStatut(SalesStatut.CLOSED);
        c.setDiffere(dto.isDiffere());
        c.setCommentaire(dto.getCommentaire());
        if (!c.isDiffere() && dto.getPayrollAmount() < dto.getAmountToBePaid()) {
            throw new PaymentAmountException();
        }
        if (c.isDiffere() && c.getCustomer() == null) {
            throw new SaleNotFoundCustomerException();
        }
        c.setPayrollAmount(dto.getPayrollAmount());
        this.posteRepository.findFirstByAddressOrName(dto.getCaisseEndNum(), dto.getCaisseNum())
            .ifPresent(c::setLastCaisse);
        c.setRestToPay(calculateRestToPay(dto.getPayrollAmount(), dto.getAmountToBePaid()));
        c.setUpdatedAt(LocalDateTime.now());
        c.setMonnaie(dto.getMontantRendu());
        c.setEffectiveUpdateDate(c.getUpdatedAt());
        if (c.getRestToPay() == 0) {
            c.setPaymentStatus(PaymentStatus.PAYE);
        } else {
            c.setPaymentStatus(PaymentStatus.IMPAYE);
        }
        this.buildReference(c);
    }

    private int calculateRestToPay(Integer payrollAmount, Integer amountToBePaid) {
        if (payrollAmount == null || amountToBePaid == null) {
            throw new PaymentAmountException();
        }
        int restToPay = amountToBePaid - payrollAmount;
        if (restToPay < 4) {
            return 0;
        }
        return restToPay;


    }

    public void arrondirMontantCaisse(Sales sales) {
        saleAmountCalculator.arrondirMontantCaisse(sales);
    }

    public void computeCashSaleAmountToPaid(CashSale c) {
        saleAmountCalculator.computeCashSaleAmountToPaid(c);
    }

    public void upddateCashSaleAmounts(CashSale c) {
        computeSaleEagerAmount(c);
        this.proccessDiscount(c);
        computeCashSaleAmountToPaid(c);
        arrondirMontantCaisse(c);
    }

    public void upddateCashSaleAmountsOnRemovingItem(CashSale c, SalesLine saleLine) {
        computeSaleEagerAmountOnRemovingItem(c, saleLine);
        this.proccessDiscount(c);
        computeCashSaleAmountToPaid(c);
        computeSaleLazyAmountOnRemovingItem(c, saleLine);
        computeTvaAmountOnRemovingItem(c, saleLine);
    }

    public void removeRemise(Sales sales) {
        sales.setRemise(null);
        sales.setDiscountAmount(0);
        sales.setNetAmount(sales.getSalesAmount());
        sales.setAmountToBePaid(sales.getSalesAmount());
        sales.setRestToPay(sales.getSalesAmount());
        sales
            .getSalesLines()
            .forEach(salesLine -> {
                salesLine.setDiscountAmount(0);
                getSaleLineService(sales).saveSalesLine(salesLine);
            });
    }

    public void applyRemiseProduit(Sales sales, RemiseProduit remiseProduit) {
        if (remiseProduit != null) {
            sales.setRemise(remiseProduit);
            saleAmountCalculator.proccessDiscount(sales);
        }
    }

    public void applyRemiseClient(Sales sales, RemiseClient remiseClient) {
        if (remiseClient != null) {
            sales.setRemise(remiseClient);
            saleAmountCalculator.proccessDiscount(sales);
        }
    }

    public void proccessDiscount(Sales sales) {
        saleAmountCalculator.proccessDiscount(sales);
    }

    protected void displayMonnaie(Integer monnaie) {
        if (Objects.requireNonNullElse(monnaie, 0) > 0) {
            afficheurPosService.displayMonnaie(monnaie);
        }
    }

    protected void displayNet(Integer net) {
        afficheurPosService.displaySaleTotal(Objects.requireNonNullElse(net, 0));
    }

    private SalesLineService getSaleLineService(Sales sales) {
        return saleAmountCalculator.saleLineServiceFor(sales);
    }

    protected void copySale(Sales sales, Sales copy) {
        copy.setId(getNextId());
        copy.setUpdatedAt(LocalDateTime.now());
        copy.setCreatedAt(copy.getUpdatedAt());
        copy.setEffectiveUpdateDate(copy.getUpdatedAt());
        buildReference(copy);
        copy.setCanceledSale(sales);
        copy.setStatut(SalesStatut.CANCELED);
        copy.setCostAmount(copy.getCostAmount() * (-1));
        copy.setNetAmount(copy.getNetAmount() * (-1));
        copy.setSalesAmount(copy.getSalesAmount() * (-1));
        copy.setHtAmount(copy.getHtAmount() * (-1));
        copy.setPayrollAmount(copy.getPayrollAmount() * (-1));
        copy.setRestToPay(copy.getRestToPay() * (-1));
        copy.setAmountToBePaid(copy.getAmountToBePaid() * (-1));
        copy.setAmountToBeTakenIntoAccount(copy.getAmountToBeTakenIntoAccount() * (-1));
        copy.setCopy(true);
        copy.setDiscountAmount(copy.getDiscountAmount() * (-1));
        copy.setTaxAmount(copy.getTaxAmount() * (-1));
        copy.setUser(storageService.getUser());

        copy.setPayments(Collections.emptySet());
        copy.setSalesLines(Collections.emptySet());
    }

    protected void copySaleCommon(Sales copy, SalesStatut salesStatut) {
        AppUser user = storageService.getUser();
        copy.setId(getNextId());
        copy.setSaleDate(LocalDate.now());
        copy.setUpdatedAt(LocalDateTime.now());
        copy.setCreatedAt(copy.getUpdatedAt());
        copy.setEffectiveUpdateDate(copy.getUpdatedAt());
        buildReference(copy);
        copy.setUser(user);
        copy.setCaissier(user);
        copy.setStatut(salesStatut);

    }

    protected void copyOrigin(Sales sales, Sales copy) {
        copy.setCanceledSale(sales);
    }

    protected long getNextId() {
        return idGeneratorService.nextId();
    }

    protected void finalizeSale(CashSale c, CashSaleDTO dto) {

        c.setDiffere(dto.isDiffere());
        c.setCommentaire(dto.getCommentaire());

        if (c.isDiffere() && c.getCustomer() == null) {
            throw new SaleNotFoundCustomerException();
        }
        c.setPayrollAmount(dto.getPayrollAmount());
        c.setRestToPay(dto.getRestToPay());
        c.setUpdatedAt(LocalDateTime.now());
        c.setMonnaie(dto.getMontantRendu());
        c.setEffectiveUpdateDate(c.getUpdatedAt());
        if (c.getRestToPay() == 0) {
            c.setPaymentStatus(PaymentStatus.PAYE);
        } else {
            c.setPaymentStatus(PaymentStatus.IMPAYE);
        }
        c.setRestToPay(c.getRestToPay() < 0 ? 0 : c.getRestToPay());
        this.buildReference(c);
    }

    protected void preValidatePrevente(Sales c, SalesStatut salesStatut)
        throws GenericError, SaleNotFoundCustomerException {
        Set<SalesStatut> statuts = Set.of(SalesStatut.PROCESSING, SalesStatut.PENDING,
            SalesStatut.DEVIS);
        if (!statuts.contains(c.getStatut())) {
            throw new GenericError("La vente ne peut pas être finalisée dans son état actuel");
        }
        if (SalesStatut.DEVIS != c.getStatut()) {
            c.setStatut(salesStatut);
        }
        if (SalesStatut.DEVIS == c.getStatut()) {
            if (isNull(c.getCustomer())) {
                throw new SaleNotFoundCustomerException();
            }
        }

    }


    protected void preValidateTrasnform(SalesStatut salesStatut, NatureVente natureVente)
        throws GenericError, SaleNotFoundCustomerException {
        Set<SalesStatut> statuts = Set.of(SalesStatut.PROCESSING,
            SalesStatut.DEVIS);
        Set<NatureVente> natureVentes = Set.of(NatureVente.COMPTANT,
            NatureVente.CARNET);
        if (!statuts.contains(salesStatut) || !natureVentes.contains(natureVente)) {
            throw new GenericError("Impossible de transformer la vente");
        }


    }
}
