package com.kobe.warehouse.service.cash_register;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashRegisterItem;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterTransactionSpecialisation;
import com.kobe.warehouse.service.cash_register.dto.CashRegisterVenteSpecialisation;
import com.kobe.warehouse.service.cash_register.impl.CashRegisterServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Le détail d'une caisse — le « ticket Z » — est ce qu'un pharmacien lit pour expliquer ce que
 * contient son tiroir. Une écriture qui manque à ce détail ne se voit pas : le total reste juste,
 * et c'est la ligne qui l'explique qui est absente.
 *
 * <p>Deux familles d'écritures l'alimentent : les ventes, et tout le reste — règlements différés,
 * règlements de factures, entrées et sorties, règlements fournisseurs. La seconde est lue par une
 * requête dont le dernier paramètre est une liste d'<strong>exclusion</strong>, ce que son nom ne
 * dit pas. Y ajouter un type par méprise le fait disparaître de l'écran sans rien casser ailleurs.
 */
@DisplayName("CashRegisterService — détail de caisse (ticket Z)")
class CashRegisterItemsTest {

    private final CashRegisterRepository repository = mock(CashRegisterRepository.class);

    private final CashRegisterService service = new CashRegisterServiceImpl(
        repository,
        mock(AppConfigurationService.class),
        mock(CashFundService.class),
        mock(UserService.class)
    );

    @Test
    @DisplayName("un règlement fournisseur figure au détail de la caisse")
    void reglementFournisseurAuDetail() {
        CashRegister caisse = caisse();
        when(repository.findCashRegisterSalesDataById(anyInt(), any(), any())).thenReturn(List.<CashRegisterVenteSpecialisation>of());
        when(repository.findCashRegisterMvtDataById(anyInt(), any(), any())).thenReturn(
            List.of(mouvement("REGLMENT_FOURNISSEUR", "VIREMENT", 5_000))
        );

        service.buildCashRegisterItems(caisse);

        assertThat(caisse.getCashRegisterItems())
            .extracting(CashRegisterItem::getTypeFinancialTransaction, CashRegisterItem::getAmount)
            .containsExactly(org.assertj.core.api.Assertions.tuple(TypeFinancialTransaction.REGLMENT_FOURNISSEUR, 5_000L));
    }

    /**
     * Seules les ventes sont exclues, parce qu'une autre requête les a déjà posées. Tout autre type
     * retiré ici disparaît du ticket Z : réglé en espèces, c'est une sortie de tiroir que le détail
     * n'explique plus.
     */
    @Test
    @DisplayName("seules les ventes sont écartées de la requête des mouvements")
    void seulesLesVentesSontEcartees() {
        when(repository.findCashRegisterSalesDataById(anyInt(), any(), any())).thenReturn(List.<CashRegisterVenteSpecialisation>of());
        when(repository.findCashRegisterMvtDataById(anyInt(), any(), any())).thenReturn(List.<CashRegisterTransactionSpecialisation>of());

        service.buildCashRegisterItems(caisse());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> exclusions = ArgumentCaptor.forClass(Set.class);
        verify(repository).findCashRegisterMvtDataById(anyInt(), any(), exclusions.capture());
        assertThat(exclusions.getValue()).containsExactly("SalePayment");
    }

    private static CashRegister caisse() {
        CashRegister caisse = new CashRegister();
        caisse.setId(1);
        return caisse;
    }

    private static CashRegisterTransactionSpecialisation mouvement(String type, String mode, long montant) {
        return new CashRegisterTransactionSpecialisation() {
            @Override
            public String getTypeTransaction() {
                return type;
            }

            @Override
            public BigDecimal getPaidAmount() {
                return BigDecimal.valueOf(montant);
            }

            @Override
            public BigDecimal getReelAmount() {
                return BigDecimal.valueOf(montant);
            }

            @Override
            public String getPaymentModeCode() {
                return mode;
            }

            @Override
            public String getPaymentModeLibelle() {
                return mode;
            }
        };
    }
}
