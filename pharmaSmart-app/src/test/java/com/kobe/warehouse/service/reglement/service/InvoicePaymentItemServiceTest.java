package com.kobe.warehouse.service.reglement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.service.id_generator.TransactionItemIdGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoicePaymentItemService")
class InvoicePaymentItemServiceTest {

    @Mock
    private TransactionItemIdGeneratorService transactionItemIdGeneratorService;

    private InvoicePaymentItemService service;

    @BeforeEach
    void setUp() {
        service = new InvoicePaymentItemService(transactionItemIdGeneratorService);
        when(transactionItemIdGeneratorService.nextId()).thenReturn(77L);
    }

    private static ThirdPartySaleLine ligne(int montant, int montantRegle) {
        return new ThirdPartySaleLine().setMontant(montant).setMontantRegle(montantRegle);
    }

    @Test
    @DisplayName("rattache la ligne de reglement a son paiement et a son bon")
    void rattacheLaLigne() {
        ThirdPartySaleLine ligne = ligne(1000, 0);
        InvoicePayment paiement = new InvoicePayment();

        InvoicePaymentItem item = service.buildInvoicePaymentItem(ligne, paiement, 600);

        assertThat(item.getId().getId()).isEqualTo(77L);
        assertThat(item.getInvoicePayment()).isSameAs(paiement);
        assertThat(item.getThirdPartySaleLine()).isSameAs(ligne);
        assertThat(item.getPaidAmount()).isEqualTo(600);
    }

    @ParameterizedTest(name = "un bon de {0} deja regle a {1} laisse {2} a devoir")
    @CsvSource({ "1000, 0, 1000", "1000, 400, 600", "1000, 1000, 0", "1000, 1200, -200" })
    @DisplayName("le montant du a la creation est le reste du bon, pas le montant verse")
    void montantDu(int montant, int montantRegle, int attendu) {
        InvoicePaymentItem item = service.buildInvoicePaymentItem(ligne(montant, montantRegle), new InvoicePayment(), 1);

        assertThat(item.getAmount()).isEqualTo(attendu);
        assertThat(item.getPaidAmount()).isEqualTo(1);
    }
}
