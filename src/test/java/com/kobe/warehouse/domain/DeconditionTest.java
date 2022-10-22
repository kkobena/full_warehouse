package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class DeconditionTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Decondition.class);
        Decondition decondition1 = new Decondition();
        decondition1.setId(1L);
        Decondition decondition2 = new Decondition();
        decondition2.setId(decondition1.getId());
        assertThat(decondition1).isEqualTo(decondition2);
        decondition2.setId(2L);
        assertThat(decondition1).isNotEqualTo(decondition2);
        decondition1.setId(null);
        assertThat(decondition1).isNotEqualTo(decondition2);
    }
}
