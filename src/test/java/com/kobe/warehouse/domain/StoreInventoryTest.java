package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class StoreInventoryTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StoreInventory.class);
        StoreInventory storeInventory1 = new StoreInventory();
        storeInventory1.setId(1L);
        StoreInventory storeInventory2 = new StoreInventory();
        storeInventory2.setId(storeInventory1.getId());
        assertThat(storeInventory1).isEqualTo(storeInventory2);
        storeInventory2.setId(2L);
        assertThat(storeInventory1).isNotEqualTo(storeInventory2);
        storeInventory1.setId(null);
        assertThat(storeInventory1).isNotEqualTo(storeInventory2);
    }
}
