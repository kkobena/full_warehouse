package com.kobe.warehouse.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.kobe.warehouse.web.rest.TestUtil;

public class InventoryTransactionTest {

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(InventoryTransaction.class);
        InventoryTransaction inventoryTransaction1 = new InventoryTransaction();
        inventoryTransaction1.setId(1L);
        InventoryTransaction inventoryTransaction2 = new InventoryTransaction();
        inventoryTransaction2.setId(inventoryTransaction1.getId());
        assertThat(inventoryTransaction1).isEqualTo(inventoryTransaction2);
        inventoryTransaction2.setId(2L);
        assertThat(inventoryTransaction1).isNotEqualTo(inventoryTransaction2);
        inventoryTransaction1.setId(null);
        assertThat(inventoryTransaction1).isNotEqualTo(inventoryTransaction2);
    }
}
