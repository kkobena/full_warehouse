package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.impl.SaleLineServiceFactory;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Arithmétique des montants d'une vente : totaux, TVA, remises, arrondi de caisse.
 */
@Service
public class SaleAmountCalculator {

    private final SaleLineServiceFactory saleLineServiceFactory;

    public SaleAmountCalculator(SaleLineServiceFactory saleLineServiceFactory) {
        this.saleLineServiceFactory = saleLineServiceFactory;
    }

    public void computeSaleEagerAmount(Sales c) {
        updateAmounts(c);
    }

    public void updateAmounts(Sales c) {
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
            // Le montant déclarable de la ligne n'était renseigné qu'à sa création : il dérivait
            // ensuite silencieusement de quantity_requested × regular_unit_price à chaque
            // changement de quantité ou de prix. Le réétablir ici — la seule méthode qui recalcule
            // déjà tous les montants à partir des lignes — garantit par construction que la somme
            // des lignes égale le montant de la vente.
            salesLine.setAmountToBeTakenIntoAccount(saleItemAmount);
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
        c.setCostAmount(
            c.getCostAmount() - (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
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

    public void arrondirMontantCaisse(Sales sales) {
        sales.setAmountToBePaid(roundedAmount(sales.getNetAmount()));
    }

    public void computeCashSaleAmountToPaid(CashSale c) {
        c.setAmountToBePaid(c.getNetAmount());
        c.setRestToPay(c.getAmountToBePaid());
        c.setAmountToBeTakenIntoAccount(0);
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

    private void computeRemiseProduit(Sales sales) {
        sales
            .getSalesLines()
            .forEach(salesLine -> {
                saleLineServiceFor(sales).processProductDiscount(salesLine);
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

    /**
     * Le service de ligne correspondant au type de vente. Exposé parce que la fabrique vit ici : la
     * classe mère y passe pour ses propres besoins plutôt que d'en garder une seconde référence.
     */
    public SalesLineService saleLineServiceFor(Sales sales) {
        return this.saleLineServiceFactory.getService(getTypeVente(sales));
    }

    private TypeVente getTypeVente(Sales sales) {
        if (sales instanceof CashSale) {
            return TypeVente.CashSale;
        } else if (sales instanceof ThirdPartySales) {
            return TypeVente.ThirdPartySales;
        } else if (sales instanceof VenteDepot) {
            return TypeVente.VenteDepot;
        }
        return null;
    }
}
