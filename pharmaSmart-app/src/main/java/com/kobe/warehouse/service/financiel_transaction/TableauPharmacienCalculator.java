package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.PaymentDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Calculator for TableauPharmacien ratios and aggregations
 */
@Component
public class TableauPharmacienCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(TableauPharmacienCalculator.class);
    private static final int DECIMAL_SCALE = 2;

    /**
     * Calculate ratio Vente/Achat for wrapper
     */
    public void calculateRatioVenteAchat(TableauPharmacienWrapper wrapper) {
        long netPurchase = wrapper.getMontantAchatNet() - wrapper.getMontantAvoirFournisseur();
        if (netPurchase == 0) {
            wrapper.setRatioVenteAchat(0f);
            return;
        }

        try {
            float ratio = BigDecimal.valueOf(wrapper.getMontantVenteNet())
                .divide(BigDecimal.valueOf(netPurchase), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            wrapper.setRatioVenteAchat(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio V/A for wrapper: {}", e.getMessage());
            wrapper.setRatioVenteAchat(0f);
        }
    }

    /**
     * Calculate ratio Achat/Vente for wrapper
     */
    public void calculateRatioAchatVente(TableauPharmacienWrapper wrapper) {
        if (wrapper.getMontantVenteNet() == 0) {
            wrapper.setRatioAchatVente(0f);
            return;
        }

        try {
            long netPurchase = wrapper.getMontantAchatNet() - wrapper.getMontantAvoirFournisseur();
            float ratio = BigDecimal.valueOf(netPurchase)
                .divide(BigDecimal.valueOf(wrapper.getMontantVenteNet()), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            wrapper.setRatioAchatVente(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio A/V for wrapper: {}", e.getMessage());
            wrapper.setRatioAchatVente(0f);
        }
    }

    /**
     * Calculate ratio Vente/Achat for daily/monthly entry
     */
    public void calculateRatioVenteAchat(TableauPharmacienDTO dto) {
        long netPurchase = dto.getMontantBonAchat() - dto.getMontantAvoirFournisseur();
        if (netPurchase == 0) {
            dto.setRatioVenteAchat(0f);
            return;
        }

        try {
            float ratio = BigDecimal.valueOf(dto.getMontantNet())
                .divide(BigDecimal.valueOf(netPurchase), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            dto.setRatioVenteAchat(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio V/A for DTO: {}", e.getMessage());
            dto.setRatioVenteAchat(0f);
        }
    }

    /**
     * Calculate ratio Achat/Vente for daily/monthly entry
     */
    public void calculateRatioAchatVente(TableauPharmacienDTO dto) {
        if (dto.getMontantNet() == 0) {
            dto.setRatioAchatVente(0f);
            return;
        }

        try {
            long netPurchase = dto.getMontantBonAchat() - dto.getMontantAvoirFournisseur();
            float ratio = BigDecimal.valueOf(netPurchase)
                .divide(BigDecimal.valueOf(dto.getMontantNet()), DECIMAL_SCALE, RoundingMode.FLOOR)
                .floatValue();
            dto.setRatioAchatVente(ratio);
        } catch (ArithmeticException e) {
            LOG.warn("Error calculating ratio A/V for DTO: {}", e.getMessage());
            dto.setRatioAchatVente(0f);
        }
    }

    /**
     * Calculate payment totals for a TableauPharmacienDTO
     * Optimized to use single loop instead of multiple iterations
     */
    public void calculatePaymentTotals(TableauPharmacienDTO dto) {
        List<PaymentDTO> payments = dto.getPayments();
        if (payments == null || payments.isEmpty()) {
            return;
        }

        // Single loop to calculate both totals
        long montantReel = 0;
        long montantComptant = 0;

        for (PaymentDTO payment : payments) {
            montantReel += payment.realAmount();
            montantComptant += payment.paidAmount();
        }

        dto.setMontantReel(montantReel);
        dto.setMontantComptant(montantComptant);
    }

    /**
     * Calculate net amount considering remises
     */
    public void calculateNetAmount(TableauPharmacienDTO dto, boolean exclureUnitesGratuites) {
        // La remise se retranche du TTC, elle ne s'y ajoute pas : le TTC rendu par la fonction
        // stockee est le brut (quantite x prix de vente), et le net est ce qui reste a encaisser.
        // Le signe inverse faisait depasser la colonne « Montant Net » le chiffre d'affaires brut,
        // de deux fois la remise accordee, et faussait d'autant les deux ratios V/A et A/V.
        //
        // La remise est celle de l'en-tete de vente : elle porte deja sur toutes les lignes, unites
        // gratuites comprises. La retirer une seconde fois sous la forme de montantRemiseUg
        // comptait la remise des UG deux fois.
        long montantNet = dto.getMontantTtc() - dto.getMontantRemise();
        if (exclureUnitesGratuites) {
            montantNet -= montantUgNet(dto);
        }
        dto.setMontantNet(montantNet);
    }

    /**
     * Retire du comptant ce que les unités gratuites ont rapporté, quand l'officine a choisi de les
     * exclure de son chiffre d'affaires.
     *
     * <p>Les unités gratuites sont vendues au prix normal : {@code quantity_ug} est un sous-ensemble
     * de {@code quantity_requested} (cf. {@code StockUpdateService}, qui décrémente le stock de la
     * différence et le stock d'UG du reste). Leur valeur est donc comprise dans le TTC <em>et</em>
     * dans l'encaissement — d'où la nécessité de la retirer des deux à la fois, ou d'aucun des deux,
     * sous peine de rompre l'égalité entre la colonne « Montant Net » et le comptant plus le crédit.
     *
     * <p>Le retrait était inconditionnel : le paramètre d'officine
     * {@code AppConfigurationService.excludeFreeUnit()} n'était lu par personne.
     */
    public void adjustCashAmountForUnitGratuite(TableauPharmacienDTO dto, boolean exclureUnitesGratuites) {
        if (!exclureUnitesGratuites) {
            return;
        }
        dto.setMontantComptant(dto.getMontantComptant() - montantUgNet(dto));
    }

    /** Ce que les unités gratuites ont réellement rapporté : leur valeur brute, remise déduite. */
    private long montantUgNet(TableauPharmacienDTO dto) {
        return dto.getMontantTtcUg() - dto.getMontantRemiseUg();
    }

    /**
     * Aggregate AchatDTO amounts
     */
    public AchatDTO aggregateAchats(List<AchatDTO> achats, AchatDTO initialAchat) {
        for (AchatDTO achat : achats) {
            initialAchat.setMontantNet(initialAchat.getMontantNet() + achat.getMontantNet());
            initialAchat.setMontantTtc(initialAchat.getMontantTtc() + achat.getMontantTtc());
            initialAchat.setMontantHt(initialAchat.getMontantHt() + achat.getMontantHt());
            initialAchat.setMontantTaxe(initialAchat.getMontantTaxe() + achat.getMontantTaxe());
            initialAchat.setMontantRemise(initialAchat.getMontantRemise() + achat.getMontantRemise());
        }
        return initialAchat;
    }
}
