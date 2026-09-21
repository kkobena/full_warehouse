package com.kobe.warehouse.service.cash_register;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashRegisterItem;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.enumeration.PaymentGroup;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le montant théorique d'une caisse se déduit des lignes de son détail, et c'est lui qu'on oppose
 * au billetage pour annoncer l'écart. Il ne tolère donc pas d'erreur de sens : une dépense comptée
 * comme une recette creuse un écart apparent du double de son montant, et le caissier cherche un
 * manquant qui n'existe pas.
 *
 * <p>Le montant d'une ligne est toujours positif — c'est son type qui porte le sens, par sa
 * {@code CategorieTransaction}. Règlement fournisseur, sortie de caisse et fonds de caisse sont
 * des sorties.
 */
@DisplayName("CashRegisterDTO — montant théorique de la caisse")
class CashRegisterEstimationTest {

    @Test
    @DisplayName("une recette ajoute au théorique")
    void uneRecetteAjoute() {
        CashRegisterDTO caisse = new CashRegisterDTO(caisseAvec(ligne(TypeFinancialTransaction.CASH_SALE, 120_000L)));

        assertThat(caisse.getEstimateAmount()).isEqualTo(120_000L);
        assertThat(caisse.getCashAmount()).isEqualTo(120_000L);
    }

    @Test
    @DisplayName("un règlement fournisseur retranche du théorique")
    void unReglementFournisseurRetranche() {
        CashRegisterDTO caisse = new CashRegisterDTO(
            caisseAvec(ligne(TypeFinancialTransaction.CASH_SALE, 120_000L), ligne(TypeFinancialTransaction.REGLMENT_FOURNISSEUR, 5_000L))
        );

        assertThat(caisse.getEstimateAmount()).isEqualTo(115_000L);
        assertThat(caisse.getCashAmount()).isEqualTo(115_000L);
    }

    @Test
    @DisplayName("une sortie de caisse retranche du théorique")
    void uneSortieDeCaisseRetranche() {
        CashRegisterDTO caisse = new CashRegisterDTO(
            caisseAvec(ligne(TypeFinancialTransaction.CASH_SALE, 120_000L), ligne(TypeFinancialTransaction.SORTIE_CAISSE, 5_000L))
        );

        assertThat(caisse.getEstimateAmount()).isEqualTo(115_000L);
    }

    /** Un règlement différé est une entrée : il alimente bien le tiroir. */
    @Test
    @DisplayName("un règlement différé ajoute au théorique")
    void unReglementDiffereAjoute() {
        CashRegisterDTO caisse = new CashRegisterDTO(
            caisseAvec(ligne(TypeFinancialTransaction.CASH_SALE, 120_000L), ligne(TypeFinancialTransaction.REGLEMENT_DIFFERE, 5_000L))
        );

        assertThat(caisse.getEstimateAmount()).isEqualTo(125_000L);
    }

    private static CashRegister caisseAvec(CashRegisterItem... lignes) {
        CashRegister caisse = new CashRegister();
        caisse.setId(1);
        AppUser caissier = new AppUser();
        // UserDTO déréférence le drapeau d'activation : un AppUser nu le laisserait à null.
        caissier.setActivated(true);
        caisse.setUser(caissier);
        caisse.setCashRegisterItems(List.of(lignes));
        return caisse;
    }

    private static CashRegisterItem ligne(TypeFinancialTransaction type, long montant) {
        return new CashRegisterItem()
            .setTypeFinancialTransaction(type)
            .setAmount(montant)
            .setPaymentMode(new PaymentMode().code("CASH").group(PaymentGroup.CASH));
    }
}
