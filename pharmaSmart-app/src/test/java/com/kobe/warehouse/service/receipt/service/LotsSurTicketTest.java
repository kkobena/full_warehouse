package com.kobe.warehouse.service.receipt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LotsSurTicketTest {

    @Test
    void ticket_unSeulLotSansQuantiteEtPeremptionAuMois() {
        SaleLineDTO ligne = new SaleLineDTO();
        ligne.setLots(List.of(new LotSold(1, "A123", 2, LocalDate.of(2027, 3, 15))));

        assertEquals(List.of("Lot A123 exp. 03/2027"), AbstractSaleReceiptService.formaterLots(ligne));
    }

    @Test
    void ticket_plusieursLotsAvecLeurQuantite() {
        SaleLineDTO ligne = new SaleLineDTO();
        ligne.setLots(List.of(new LotSold(1, "A123", 2, LocalDate.of(2027, 3, 15)), new LotSold(2, "B456", 1, null)));

        assertEquals(List.of("Lot A123 (2) exp. 03/2027", "Lot B456 (1)"), AbstractSaleReceiptService.formaterLots(ligne));
    }

    @Test
    void ticket_sansLotRienAImprimer() {
        assertTrue(AbstractSaleReceiptService.formaterLots(new SaleLineDTO()).isEmpty());
    }
}
