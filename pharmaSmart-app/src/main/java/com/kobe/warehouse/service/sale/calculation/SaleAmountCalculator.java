package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.VenteDepot;
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
        int discount = 0;

        for (SalesLine salesLine : c.getSalesLines()) {
            int saleItemAmount = salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice();
            int costAmountItem = salesLine.getQuantityRequested() * salesLine.getCostAmount();
            salesAmount += saleItemAmount;
            costAmount += costAmountItem;
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
        computeTva(c);
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
        computeTva(c);
        if (c instanceof CashSale cashSale) {
            processDiscountCash(cashSale, discountAmount);
        }
    }

    private static int computeHtAmount(Integer amount, Integer taxValue) {
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

    /** La ligne a déjà quitté la vente : HT et TVA se refont sur les lignes restantes. */
    public void computeTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
        computeTva(c);
    }

    /**
     * HT et TVA de la vente, sur ce que la cliente paie : le montant de chaque ligne MOINS sa remise
     * (remise produit, promotion). Une remise accordée à la vente n'entre pas dans la base imposable ;
     * calculée sur le brut, la TVA d'une vente remisée était surévaluée.
     */
    public void computeTva(Sales c) {
        int htAmount = 0;
        int taxAmount = 0;
        for (SalesLine salesLine : c.getSalesLines()) {
            int net = montantNet(salesLine);
            int ht = computeHtAmount(net, salesLine.getTaxValue());
            htAmount += ht;
            taxAmount += net - ht;
        }
        c.setHtAmount(htAmount);
        c.setTaxAmount(taxAmount);
    }

    /** Ce que la cliente paie pour la ligne : quantité × prix, moins la remise de la ligne. */
    public static int montantNet(SalesLine salesLine) {
        return Objects.requireNonNullElse(salesLine.getQuantityRequested(), 0)
                        * Objects.requireNonNullElse(salesLine.getRegularUnitPrice(), 0)
                - Objects.requireNonNullElse(salesLine.getDiscountAmount(), 0);
    }

    /** HT d'un montant TTC (arrondi au franc supérieur, comme à l'enregistrement). */
    public static int montantHt(int ttc, Integer taxValue) {
        return computeHtAmount(ttc, taxValue);
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
        if (sales.getRemise() instanceof RemiseProduit) {
            this.computeRemiseProduit(sales);
        }
    }

    private void computeRemiseProduit(Sales sales) {
        SalesLineService salesLineService = saleLineServiceFor(sales);
        sales.getSalesLines().forEach(salesLineService::processProductDiscount);
        this.processDiscountCommonAmounts(sales);
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
