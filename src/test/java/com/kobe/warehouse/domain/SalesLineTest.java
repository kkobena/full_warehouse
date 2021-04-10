package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class SalesLineTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SalesLine.class);
        SalesLine salesLine1 = new SalesLine();
        salesLine1.setId(1L);
        SalesLine salesLine2 = new SalesLine();
        salesLine2.setId(salesLine1.getId());
        assertThat(salesLine1).isEqualTo(salesLine2);
        salesLine2.setId(2L);
        assertThat(salesLine1).isNotEqualTo(salesLine2);
        salesLine1.setId(null);
        assertThat(salesLine1).isNotEqualTo(salesLine2);
    }
}
