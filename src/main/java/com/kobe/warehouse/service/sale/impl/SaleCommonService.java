package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.SaleAlreadyCloseException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.sale.AvoirService;
import com.kobe.warehouse.service.sale.SalesLineService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class SaleCommonService {

    private final ReferenceService referenceService;
    private final WarehouseCalendarService warehouseCalendarService;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final SalesLineService salesLineService;
    private final CashRegisterService cashRegisterService;
    private final AvoirService avoirService;
    private final PosteRepository posteRepository;

    public SaleCommonService(
        ReferenceService referenceService,
        WarehouseCalendarService warehouseCalendarService,
        StorageService storageService,
        UserRepository userRepository,
        SalesLineService salesLineService,
        CashRegisterService cashRegisterService,
        AvoirService avoirService,
        PosteRepository posteRepository
    ) {
        this.referenceService = referenceService;
        this.warehouseCalendarService = warehouseCalendarService;
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.salesLineService = salesLineService;
        this.cashRegisterService = cashRegisterService;
        this.avoirService = avoirService;
        this.posteRepository = posteRepository;
    }

    public void computeSaleEagerAmount(Sales c, int amount, int oldSalesAmount) {
        c.setSalesAmount((c.getSalesAmount() - oldSalesAmount) + amount);
    }

    public void computeSaleCmu(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine != null) {
            c.setCmuAmount((c.getCmuAmount() - computeCmuAmount(oldSaleLine)) + computeCmuAmount(saleLine));
        } else {
            c.setCmuAmount(c.getCmuAmount() + computeCmuAmount(saleLine));
        }
    }

    public void computeSaleLazyAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine != null) {
            c.setCostAmount(
                (c.getCostAmount() - (oldSaleLine.getQuantityRequested() * oldSaleLine.getCostAmount())) +
                (saleLine.getQuantityRequested() * saleLine.getCostAmount())
            );
        } else {
            c.setCostAmount(c.getCostAmount() + (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
        }
    }

    public void processDiscountCashSale(CashSale c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine != null) {
            c.setDiscountAmount((c.getDiscountAmount() - oldSaleLine.getDiscountAmount()) + saleLine.getDiscountAmount());
            c.setDiscountAmountUg((c.getDiscountAmountUg() - oldSaleLine.getDiscountAmountUg()) + saleLine.getDiscountAmountUg());
            c.setDiscountAmountHorsUg(
                (c.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg()) + saleLine.getDiscountAmountHorsUg()
            );
            c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
        } else {
            c.setDiscountAmount(c.getDiscountAmount() + saleLine.getDiscountAmount());
            c.setDiscountAmountUg(c.getDiscountAmountUg() + saleLine.getDiscountAmountUg());
            c.setDiscountAmountHorsUg(c.getDiscountAmountHorsUg() + saleLine.getDiscountAmountHorsUg());
            c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
        }
    }

    public void computeUgTvaAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (saleLine.getQuantityUg().compareTo(0) == 0) {
            return;
        }
        if (oldSaleLine == null) {
            int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
            int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmountUg(c.getHtAmountUg() + htc);
                c.setMontantttcUg(c.getMontantttcUg() + htc);
            } else {
                double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(htc / valeurTva);
                int montantTva = htc - htAmont;
                c.setMontantTvaUg(c.getMontantTvaUg() + montantTva);
                c.setHtAmountUg(c.getHtAmountUg() + htAmont);
            }
            c.setMargeUg(c.getMargeUg() + (htc - costAmount));
            c.setNetUgAmount(
                c.getMontantttcUg() + ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice()) - saleLine.getDiscountAmountUg())
            );
        } else {
            int htcOld = oldSaleLine.getQuantityUg() * oldSaleLine.getRegularUnitPrice();
            int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
            int costAmountOld = oldSaleLine.getQuantityUg() * oldSaleLine.getCostAmount();
            int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmountUg((c.getHtAmountUg() - htcOld) + htc);
                c.setMontantttcUg((c.getMontantttcUg() - htcOld) + htc);
            } else {
                double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(htc / valeurTva);
                int montantTva = htc - htAmont;
                int htAmontOld = (int) Math.ceil(htcOld / valeurTva);
                int montantTvaOld = htcOld - htAmontOld;

                c.setMontantTvaUg((c.getMontantTvaUg() - montantTvaOld) + montantTva);
                c.setHtAmountUg((c.getHtAmountUg() - htAmontOld) + htAmont);
            }
            c.setMargeUg((c.getMargeUg() - (htcOld - costAmountOld)) + (htc - costAmount));
            c.setNetUgAmount(
                (c.getMontantttcUg() -
                    ((oldSaleLine.getQuantityUg() * oldSaleLine.getRegularUnitPrice()) - oldSaleLine.getDiscountAmountUg())) +
                ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice()) - saleLine.getDiscountAmountUg())
            );
        }
    }

    public void computeTvaAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
        if (oldSaleLine == null) {
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmount(c.getHtAmount() + saleLine.getSalesAmount());
                saleLine.setTaxAmount(0);
                saleLine.setHtAmount(saleLine.getSalesAmount());
            } else {
                Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
                int montantTva = saleLine.getSalesAmount() - htAmont;
                c.setTaxAmount(c.getTaxAmount() + montantTva);
                c.setHtAmount(c.getHtAmount() + htAmont);
                saleLine.setTaxAmount(montantTva);
                saleLine.setHtAmount(htAmont);
            }
        } else {
            if (saleLine.getTaxValue().compareTo(0) == 0) {
                c.setHtAmount((c.getHtAmount() - oldSaleLine.getSalesAmount()) + saleLine.getSalesAmount());
                saleLine.setHtAmount(saleLine.getSalesAmount());
            } else {
                double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
                int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
                int montantTva = saleLine.getSalesAmount() - htAmont;
                int htAmontOld = (int) Math.ceil(oldSaleLine.getSalesAmount() / valeurTva);
                int montantTvaOld = oldSaleLine.getSalesAmount() - htAmontOld;
                c.setTaxAmount((c.getTaxAmount() - montantTvaOld) + montantTva);
                c.setHtAmount((c.getHtAmount() - htAmontOld) + htAmont);
                saleLine.setTaxAmount(montantTva);
                saleLine.setHtAmount(htAmont);
            }
        }
    }

    public void processDiscountSaleOnRemovingItem(Sales c, SalesLine saleLine) {
        c.setDiscountAmount(c.getDiscountAmount() - saleLine.getDiscountAmount());
        c.setDiscountAmountUg(c.getDiscountAmountUg() - saleLine.getDiscountAmountUg());
        c.setDiscountAmountHorsUg(c.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg());
        c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
    }

    public void computeSaleEagerAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        c.setSalesAmount(c.getSalesAmount() - saleLine.getSalesAmount());
        c.setCmuAmount(c.getCmuAmount() - computeCmuAmount(saleLine));
    }

    public void computeSaleLazyAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        c.setCostAmount(c.getCostAmount() - (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
    }

    public void computeUgTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        if (saleLine.getQuantityUg().compareTo(0) == 0) {
            return;
        }
        int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
        int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
        if (saleLine.getTaxValue().compareTo(0) == 0) {
            c.setHtAmountUg(c.getHtAmountUg() - htc);
            c.setMontantttcUg(c.getMontantttcUg() - htc);
        } else {
            double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
            int htAmont = (int) Math.ceil(htc / valeurTva);
            int montantTva = htc - htAmont;
            c.setMontantTvaUg(c.getMontantTvaUg() - montantTva);
            c.setHtAmountUg(c.getHtAmountUg() - htAmont);
        }
        c.setMargeUg(c.getMargeUg() - (htc - costAmount));
        c.setNetUgAmount(
            c.getMontantttcUg() - ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice()) - saleLine.getDiscountAmountUg())
        );
    }

    public void computeTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        if (saleLine.getTaxValue().compareTo(0) == 0) {
            c.setHtAmount(c.getHtAmount() - saleLine.getSalesAmount());
        } else {
            double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
            int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
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
                    } catch (JSONException e) {}
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

    protected void intSale(SaleDTO dto, Sales c) {
        User user = storageService.getUser();
        c.setNatureVente(dto.getNatureVente());
        c.setTypePrescription(dto.getTypePrescription());
        User caissier = user;
        if (user.getId().compareTo(dto.getCassierId()) != 0) {
            caissier = userRepository.getReferenceById(dto.getCassierId());
        }
        if (Objects.nonNull(dto.getSellerId()) && caissier.getId().compareTo(dto.getSellerId()) != 0) {
            c.setSeller(userRepository.getReferenceById(dto.getSellerId()));
        } else {
            c.setSeller(caissier);
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
        this.buildPreventeReference(c);
        c.setStatut(SalesStatut.ACTIVE);
        c.setStatutCaisse(SalesStatut.ACTIVE);
        c.setCalendar(this.warehouseCalendarService.initCalendar());
        this.posteRepository.findFirstByAddress(dto.getCaisseNum()).ifPresent(poste -> {
                c.setCaisse(poste);
                c.setLastCaisse(poste);
            });

        c.setPaymentStatus(PaymentStatus.IMPAYE);
        c.setMagasin(c.getCassier().getMagasin());
    }

    public void save(Sales c, SaleDTO dto) throws SaleAlreadyCloseException {
        if (c.getStatut() == SalesStatut.CLOSED) {
            throw new SaleAlreadyCloseException();
        }
        User user = storageService.getUser();
        c.setUser(user);
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        c.setCalendar(this.warehouseCalendarService.initCalendar());
        c.setCashRegister(cashRegister);
        Long id = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
        salesLineService.save(c.getSalesLines(), user, id);
        c.setStatut(SalesStatut.CLOSED);
        c.setStatutCaisse(SalesStatut.CLOSED);
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
        this.buildReference(c);
        if (dto.isAvoir()) {
            this.avoirService.save(c);
        }
    }

    public int computeCmuAmount(SalesLine salesLine) {
        if (salesLine.getCmuAmount() > 0) {
            return salesLine.getCmuAmount() * salesLine.getQuantityRequested();
        }
        return salesLine.getRegularUnitPrice() * salesLine.getQuantityRequested();
    }

    public void editSale(Sales c, SaleDTO dto) throws SaleAlreadyCloseException {
        /*  if (c.getStatut() == SalesStatut.CLOSED )) {
            throw new SaleAlreadyCloseException();
        }*/
        User user = storageService.getUser();
        c.setUser(user);
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        c.setCalendar(this.warehouseCalendarService.initCalendar());
        c.setCashRegister(cashRegister);
        Long id = storageService.getDefaultConnectedUserPointOfSaleStorage().getId();
        salesLineService.save(c.getSalesLines(), user, id);
        c.setStatut(SalesStatut.CLOSED);
        c.setStatutCaisse(SalesStatut.CLOSED);
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
        this.buildReference(c);
        if (dto.isAvoir()) {
            this.avoirService.save(c);
        }
    }
}
