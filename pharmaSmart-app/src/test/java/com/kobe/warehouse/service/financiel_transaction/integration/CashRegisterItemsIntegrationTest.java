package com.kobe.warehouse.service.financiel_transaction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashRegisterItem;
import com.kobe.warehouse.domain.PaymentFournisseur;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashFundService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.impl.CashRegisterServiceImpl;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le détail d'une caisse — le « ticket Z » — est reconstitué à la clôture par deux requêtes : l'une
 * pour les ventes, l'autre pour tout le reste. La seconde filtre sur {@code dtype NOT IN (…)}, une
 * liste d'<strong>exclusion</strong> que son nom de paramètre ne laisse pas deviner.
 *
 * <p>{@code PaymentFournisseur} y figurait : un règlement fournisseur passé par la caisse
 * n'atteignait jamais le détail, et la branche {@code REGLMENT_FOURNISSEUR} du {@code switch} qui
 * suit était inatteignable. Réglé en espèces, c'était une sortie de tiroir que le ticket Z
 * n'expliquait pas.
 *
 * <p>Le contrôle ne vaut qu'exécuté sur un vrai PostgreSQL : c'est la requête native, et elle seule,
 * qui décide de ce qui remonte. Un test sur doublure prouve le {@code switch}, pas le filtre.
 */
@DisplayName("Détail de caisse — mouvements hors ventes sur PostgreSQL")
class CashRegisterItemsIntegrationTest extends AbstractFinancialTransactionIntegrationTest {

    private CashRegisterService service;

    @BeforeEach
    void cablerLeService() {
        service = new CashRegisterServiceImpl(
            IntegrationPostgresDatabase.bean(CashRegisterRepository.class),
            mock(AppConfigurationService.class),
            mock(CashFundService.class),
            mock(UserService.class)
        );
    }

    @Test
    @DisplayName("un règlement fournisseur remonte au détail de la caisse")
    void reglementFournisseurRemonte() {
        CashRegister caisse = caisseOuverte();
        reglementFournisseur(caisse, "CASH", 5_000);

        service.buildCashRegisterItems(caisse);

        assertThat(caisse.getCashRegisterItems())
            .extracting(CashRegisterItem::getTypeFinancialTransaction, CashRegisterItem::getAmount)
            .contains(org.assertj.core.api.Assertions.tuple(TypeFinancialTransaction.REGLMENT_FOURNISSEUR, 5_000L));
    }

    /** Le mode de règlement du mouvement doit suivre, sinon la ligne ne s'impute sur rien. */
    @Test
    @DisplayName("le mode de règlement du mouvement est celui de la ligne")
    void modeDeReglementConserve() {
        CashRegister caisse = caisseOuverte();
        reglementFournisseur(caisse, "VIREMENT", 12_000);

        service.buildCashRegisterItems(caisse);

        assertThat(caisse.getCashRegisterItems())
            .filteredOn(item -> item.getTypeFinancialTransaction() == TypeFinancialTransaction.REGLMENT_FOURNISSEUR)
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getPaymentMode().getCode()).isEqualTo("VIREMENT");
                assertThat(item.getAmount()).isEqualTo(12_000L);
            });
    }

    @Test
    @DisplayName("une caisse sans mouvement hors vente ne porte aucune ligne")
    void caisseSansMouvement() {
        CashRegister caisse = caisseOuverte();

        service.buildCashRegisterItems(caisse);

        assertThat(caisse.getCashRegisterItems()).isEmpty();
    }

    /**
     * Un règlement fournisseur encaissé sur la caisse. Sans commande rattachée : le lien est
     * facultatif, et c'est le mouvement qui nous intéresse, pas son origine.
     */
    private void reglementFournisseur(CashRegister caisse, String codeMode, int montant) {
        PaymentFournisseur paiement = new PaymentFournisseur();
        paiement.setId(transactionIdGeneratorService.nextId());
        paiement.setCashRegister(caisse);
        paiement.setPaymentMode(modePaiement(codeMode));
        paiement.setPaidAmount(montant);
        paiement.setExpectedAmount(montant);
        paiement.setMontantVerse(montant);
        paiement.setReelAmount(montant);
        paiement.setTransactionDate(aujourdHui());
        paiement.setCreatedAt(LocalDateTime.now());
        paiement.setCategorieChiffreAffaire(CategorieChiffreAffaire.CA);
        paiement.setTypeFinancialTransaction(TypeFinancialTransaction.REGLMENT_FOURNISSEUR);
        paiement.setCredit(false);
        em.persist(paiement);
        em.flush();
    }
}
