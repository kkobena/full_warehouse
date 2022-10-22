package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class AjustementTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Ajustement.class);
        Ajustement ajustement1 = new Ajustement();
        ajustement1.setId(1L);
        Ajustement ajustement2 = new Ajustement();
        ajustement2.setId(ajustement1.getId());
        assertThat(ajustement1).isEqualTo(ajustement2);
        ajustement2.setId(2L);
        assertThat(ajustement1).isNotEqualTo(ajustement2);
        ajustement1.setId(null);
        assertThat(ajustement1).isNotEqualTo(ajustement2);
    }
}
