package com.kobe.warehouse.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le clone d'une vente (annulation, clone de devis) est une vente nouvelle. S'il gardait la version
 * de l'original, Hibernate le prendrait pour une entité détachée et refuserait de l'insérer.
 */
@DisplayName("Sales.clone — verrou optimiste")
class SalesCloneTest {

    @Test
    @DisplayName("Le clone repart sans version et comme nouvelle vente")
    void cloneSansVersion() {
        CashSale originale = new CashSale();
        originale.setVersion(7L);
        originale.markNotNew();

        CashSale copie = (CashSale) originale.clone();

        assertNull(copie.getVersion());
        assertTrue(copie.isNew());
        assertEquals(7L, originale.getVersion(), "l'original garde sa version");
    }
}
