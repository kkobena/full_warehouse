package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.SaleAlreadyCloseException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.utils.AfficheurPosService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SaleCommonService {
    private final ReferenceService referenceService;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final SaleLineServiceFactory saleLineServiceFactory;
    private final CashRegisterService cashRegisterService;
    private final PosteRepository posteRepository;
    private final AfficheurPosService afficheurPosService;
    private final SaleIdGeneratorService idGeneratorService;

    public SaleCommonService(
        ReferenceService referenceService,
        StorageService storageService,
        UserRepository userRepository,
        SaleLineServiceFactory saleLineServiceFactory,
        CashRegisterService cashRegisterService,

        PosteRepository posteRepository,
        AfficheurPosService afficheurPosService, SaleIdGeneratorService idGeneratorService
    ) {
        this.referenceService = referenceService;

        this.storageService = storageService;
        this.userRepository = userRepository;
        this.saleLineServiceFactory = saleLineServiceFactory;
        this.cashRegisterService = cashRegisterService;
        this.posteRepository = posteRepository;
        this.afficheurPosService = afficheurPosService;
        this.idGeneratorService = idGeneratorService;

    }

    public void computeSaleEagerAmount(Sales c) {
        updateAmounts(c);
    }

    protected void updateAmounts(Sales c) {
        int salesAmount = 0;
        int costAmount = 0;
        int taxableAmount = 0;
        int htAmount = 0;
        int discount = 0;


        for (SalesLine salesLine : c.getSalesLines()) {
            int saleItemAmount = salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice();
            int costAmountItem = salesLine.getQuantityRequested() * salesLine.getCostAmount();
            salesAmount += saleItemAmount;
            costAmount += costAmountItem;
            int htAmont = computeHtAmount(saleItemAmount, salesLine.getTaxValue());
            htAmount += htAmont;
            int montantTva = saleItemAmount - htAmont;
            taxableAmount += montantTva;
            discount += Objects.requireNonNullElse(salesLine.getDiscountAmount(), 0);

        }

        c.setSalesAmount(salesAmount);
        c.setCostAmount(costAmount);
        c.setTaxAmount(taxableAmount);
        c.setHtAmount(htAmount);
        c.setDiscountAmount(discount);
        c.setNetAmount(salesAmount - discount);
        c.setAmountToBeTakenIntoAccount(salesAmount);


    }


    public void processDiscountCash(CashSale c, int discountAmount) {
        c.setNetAmount(c.getSalesAmount() - discountAmount);
    }

    public void processDiscountCommonAmounts(Sales c) {
        int discountAmount = 0;

        for (SalesLine saleLine : c.getSalesLines()) {
            discountAmount += saleLine.getDiscountAmount();

        }
        c.setDiscountAmount(discountAmount);
        if (c instanceof CashSale cashSale) {
            processDiscountCash(cashSale, discountAmount);
        }
    }


    private int computeHtAmount(Integer amount, Integer taxValue) {
        int tax = Objects.requireNonNullElse(taxValue, 0);
        int ttc = Objects.requireNonNullElse(amount, 0);
        if (tax == 0) {
            return ttc;
        }
        double valeurTva = 1 + ((double) tax / 100);
        return (int) Math.ceil(ttc / valeurTva);
    }


    public void computeSaleEagerAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        c.setSalesAmount(c.getSalesAmount() - saleLine.getSalesAmount());
    }

    public void computeSaleLazyAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        c.setCostAmount(c.getCostAmount() - (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
    }


    public void computeTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        if (saleLine.getTaxValue().compareTo(0) == 0) {
            c.setHtAmount(c.getHtAmount() - saleLine.getSalesAmount());
        } else {
            int htAmont = computeHtAmount(saleLine.getSalesAmount(), saleLine.getTaxValue());
            int montantTva = saleLine.getSalesAmount() - htAmont;
            c.setTaxAmount(c.getTaxAmount() - montantTva);
            c.setHtAmount(c.getHtAmount() - htAmont);
        }
    }

    public void buildReference(Sales sales) {
        String ref = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat(referenceService.buildNumSale());
        sales.setNumberTransaction(ref);
    }

    public void buildPreventeReference(Sales sales) {
        String ref = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat(referenceService.buildNumPreventeSale());
        sales.setNumberTransaction(ref);
    }

    public String buildTvaData(Set<SalesLine> salesLines) {
        if (salesLines != null && !salesLines.isEmpty()) {
            JSONArray array = new JSONArray();
            salesLines
                .stream()
                .filter(saleLine -> saleLine.getTaxValue() > 0)
                .collect(Collectors.groupingBy(SalesLine::getTaxValue))
                .forEach((k, v) -> {
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
                    } catch (JSONException _) {
                    }
                });
            if (!array.isEmpty()) {
                return array.toString();
            }
        }
        return null;
    }

    public int roundedAmount(int payrollAmount) {
        int rest = payrollAmount % 5;
        if (rest == 0) {
            return payrollAmount;
        } else {
            if (rest >= 3) {
                return payrollAmount + (5 - rest);
            } else {
                return payrollAmount - rest;
            }
        }
    }

    protected void setId(Sales c) {
        c.setId(idGeneratorService.nextId());
    }

    protected void intSale(SaleDTO dto, Sales c) {
        setId(c);
        AppUser user = storageService.getUser();
        c.setNatureVente(dto.getNatureVente());
        c.setTypePrescription(dto.getTypePrescription());
        AppUser caissier = user;
        c.setSeller(caissier);
       /* if (user.getId().compareTo(dto.getCassierId()) != 0) {
            caissier = userRepository.getReferenceById(dto.getCassierId());
        }
        if (Objects.nonNull(dto.getSellerId()) && caissier.getId().compareTo(dto.getSellerId()) != 0) {
            c.setSeller(userRepository.getReferenceById(dto.getSellerId()));
        } else {
            c.setSeller(caissier);
        }*/
        c.setImported(false);
        c.setUser(user);
        c.setLastUserEdit(c.getUser());
        c.setCaissier(caissier);
        c.setCopy(dto.getCopy());
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(c.getCreatedAt());
        c.setEffectiveUpdateDate(c.getUpdatedAt());
        c.setPayrollAmount(0);
        c.setToIgnore(dto.isToIgnore());
        c.setDiffere(dto.isDiffere());
        this.buildPreventeReference(c);
        c.setStatut(SalesStatut.ACTIVE);
        this.posteRepository.findFirstByAddress(dto.getCaisseNum()).ifPresent(poste -> {
            c.setCaisse(poste);
            c.setLastCaisse(poste);
        });

        c.setPaymentStatus(PaymentStatus.IMPAYE);
        c.setMagasin(c.getCaissier().getMagasin());
    }

    public void save(Sales c, SaleDTO dto) throws SaleAlreadyCloseException {
        if (CollectionUtils.isEmpty(c.getSalesLines())) {
            return;
        }
        if (c.getStatut() == SalesStatut.CLOSED) {
            throw new SaleAlreadyCloseException();
        }
        finalizeSale(c, dto);
    }

    public void editSale(Sales c, SaleDTO dto) {
        finalizeSale(c, dto);
    }

    private void finalizeSale(Sales c, SaleDTO dto) {
        AppUser user = storageService.getUser();
        c.setUser(user);
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        c.setCashRegister(cashRegister);
        Long id = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
        getSaleLineService(c).save(c.getSalesLines(), user, id);
        c.setStatut(SalesStatut.CLOSED);
        c.setDiffere(dto.isDiffere());
        c.setLastUserEdit(storageService.getUser());
        c.setCommentaire(dto.getCommentaire());
        if (!c.isDiffere() && dto.getPayrollAmount() < dto.getAmountToBePaid()) {
            throw new PaymentAmountException();
        }
        if (c.isDiffere() && c.getCustomer() == null) {
            throw new SaleNotFoundCustomerException();
        }
        c.setPayrollAmount(dto.getPayrollAmount());
        this.posteRepository.findFirstByAddress(dto.getCaisseEndNum()).ifPresent(c::setLastCaisse);
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

    public void arrondirMontantCaisse(Sales sales) {
        sales.setAmountToBePaid(roundedAmount(sales.getNetAmount()));
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
            this.computeRemiseProduit(sales);
        }
    }

    public void applyRemiseClient(Sales sales, RemiseClient remiseClient) {
        if (remiseClient != null) {
            sales.setRemise(remiseClient);
            computeRemisableAmount(remiseClient, sales);
        }
    }

    private void computeRemiseProduit(Sales sales) {
        sales
            .getSalesLines()
            .forEach(salesLine -> {
                getSaleLineService(sales).processProductDiscount(salesLine);
                this.processDiscountCommonAmounts(sales);
            });
    }

    private void computeRemisableAmount(RemiseClient remiseClient, Sales sales) {
        int totalAmount = sales
            .getSalesLines()
            .stream()
            .filter(e -> e.getProduit().getCodeRemise() != CodeRemise.NONE)
            .mapToInt(SalesLine::getSalesAmount)
            .sum();
        if (totalAmount == 0) {
            return;
        }
        int discount = (int) Math.ceil(totalAmount * remiseClient.getTauxRemise());
        sales.setDiscountAmount(discount);
        sales.setNetAmount(sales.getSalesAmount() - discount);
    }

    public void proccessDiscount(Sales sales) {
        Remise remise = sales.getRemise();
        if (remise != null) {
            if (remise instanceof RemiseProduit) {
                this.computeRemiseProduit(sales);
            } else {
                this.computeRemisableAmount((RemiseClient) remise, sales);
            }
        }
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
        return this.saleLineServiceFactory.getService(getTypeVente(sales));
    }

    private TypeVente getTypeVente(Sales sales) {
        if (sales instanceof CashSale) {
            return TypeVente.CashSale;
        } else if (sales instanceof ThirdPartySales) {
            return TypeVente.ThirdPartySales;
        }
        return null;
    }
}
