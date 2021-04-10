package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class PaymentModeTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PaymentMode.class);
        PaymentMode paymentMode1 = new PaymentMode();
        paymentMode1.setId(1L);
        PaymentMode paymentMode2 = new PaymentMode();
        paymentMode2.setId(paymentMode1.getId());
        assertThat(paymentMode1).isEqualTo(paymentMode2);
        paymentMode2.setId(2L);
        assertThat(paymentMode1).isNotEqualTo(paymentMode2);
        paymentMode1.setId(null);
        assertThat(paymentMode1).isNotEqualTo(paymentMode2);
    }
}
