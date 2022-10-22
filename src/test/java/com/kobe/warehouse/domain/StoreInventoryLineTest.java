package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class StoreInventoryLineTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StoreInventoryLine.class);
        StoreInventoryLine storeInventoryLine1 = new StoreInventoryLine();
        storeInventoryLine1.setId(1L);
        StoreInventoryLine storeInventoryLine2 = new StoreInventoryLine();
        storeInventoryLine2.setId(storeInventoryLine1.getId());
        assertThat(storeInventoryLine1).isEqualTo(storeInventoryLine2);
        storeInventoryLine2.setId(2L);
        assertThat(storeInventoryLine1).isNotEqualTo(storeInventoryLine2);
        storeInventoryLine1.setId(null);
        assertThat(storeInventoryLine1).isNotEqualTo(storeInventoryLine2);
    }
}
