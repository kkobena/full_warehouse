package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class PaymentFournisseurTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PaymentFournisseur.class);
        PaymentFournisseur paymentFournisseur1 = new PaymentFournisseur();
        paymentFournisseur1.setId(1L);
        PaymentFournisseur paymentFournisseur2 = new PaymentFournisseur();
        paymentFournisseur2.setId(paymentFournisseur1.getId());
        assertThat(paymentFournisseur1).isEqualTo(paymentFournisseur2);
        paymentFournisseur2.setId(2L);
        assertThat(paymentFournisseur1).isNotEqualTo(paymentFournisseur2);
        paymentFournisseur1.setId(null);
        assertThat(paymentFournisseur1).isNotEqualTo(paymentFournisseur2);
    }
}
